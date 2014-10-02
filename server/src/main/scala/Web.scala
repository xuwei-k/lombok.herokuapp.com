package com.herokuapp.lombok

import scalaz._
import syntax.either._
import unfiltered.request._
import unfiltered.response._
import util.Properties
import sbt.{Path=>_, _}
import sbt.Path._
import java.io.{Writer, File}
import org.json4s._
import org.json4s.native.JsonMethods
import lombok.delombok.Delombok

class App(debug:Boolean) extends unfiltered.filter.Plan {

  lazy val jarList = Seq("lib","lib_managed").flatMap{ dir =>
    { new File(dir) ** "*.jar" get }
  } :+ IO.classLocationFile[Predef.type]

  def lombok2java(src: Seq[SourceFile]) = {
    IO.withTemporaryDirectory{in =>
      src.foreach{f =>
        IO.writeLines(in / f.name, Seq(f.contents))
      }
      IO.withTemporaryDirectory{out =>
        delombok(out,in,jarList)
      }
    }
  }

  def delombok(out: File, in: File, cp: Seq[File]): String \/ Seq[SourceFile] = {
    val o = System.out
    val e = System.err
    val buf = new java.io.ByteArrayOutputStream
    val stream = new java.io.PrintStream(buf)
    System.setOut(stream)
    System.setErr(stream)
    try{
      val c = new Delombok
      c.setVerbose(true)
      c.setClasspath(cp.map{_.getAbsolutePath}.mkString(java.io.File.pathSeparator))
      c.setOutput(out)
      c.addDirectory(in)
      c.setCharset("UTF-8")
      if(c.delombok()){
        val generated = (out ** "*.java").get.map{f => SourceFile(f.getName,IO.read(f))}
        if(debug){
          generated.foreach(println)
        }
        generated.right
      }else{
        stream.close
        ("compile fail \n" + buf.toString ).left
      }
    }catch{
      case e: Throwable => e.getStackTrace.mkString(e.getMessage+"\n","\n","").left
    }finally{
      stream.close
      System.setOut(o)
      System.setErr(e)
    }
  }

  val GITHUB = "https://github.com/xuwei-k/lombok.herokuapp.com"
  val lombok_SITE = "http://projectlombok.org"

  def intent = {
    case r @ POST(Path("/")) =>
      val str = Body.string(r)
      if(debug){
        println(str)
        JsonMethods.parseOpt(str).map{ j =>
          println(JsonMethods.pretty(JsonMethods.render(j)))
        }.getOrElse{
          println("fail parse json. " + str + " is not valid json")
        }
      }

      val sourceFiles = for{
        JObject(List(JField(Common.FILES,JObject(json)))) <- JsonMethods.parseOpt(str)
        files = for{
          JField(name,JString(contents)) <- json
        } yield SourceFile(name,contents)
      }yield files

      sourceFiles.map(lombok2java).map{
        case \/-(seq)   => Result(false,"",seq)
        case -\/(error) => Result(true,error,Nil)
      }.getOrElse{
        val msg = "invalid params " + str
        Result(true,msg,Nil)
      }.toJsonResponse

    case GET(Path("/")) =>

      Html(
      <html>
        <head>
          <script type="text/javascript" src="http://code.jquery.com/jquery-2.1.0.js"></script>
          <script type="text/javascript" src={LOMBOK_HEROKU_JS}></script>
          <title>lombok {lombokVersion()} web interface</title>
          <link rel="stylesheet" href={LOMBOK_HEROKU_CSS} type="text/css" />
          <script src="//cdnjs.cloudflare.com/ajax/libs/prettify/r298/prettify.js" type="text/javascript"></script>
          <link href="//cdnjs.cloudflare.com/ajax/libs/prettify/r298/prettify.css" rel="stylesheet" type="text/css"/>
        </head>
        <body>
          <h1><a href={lombok_SITE}>lombok</a> {lombokVersion()} web interface</h1>
          <p><a href={GITHUB}>this program source code</a></p>
          <p>
            <button id='compile' >compile</button>
            <button id='clear_javacode' >clear java code</button>
            <button id='clear_error_message' >clear error message</button>
            <form>
            <input type='radio' name='lombok_edit_type' id='edit_type_auto' value='auto'>auto</input>
            <input type='radio' name='lombok_edit_type' id='edit_type_manual' value='manual'>manual</input>
            </form>
          </p>
          <div class='src_wrap_div'>
            <div>
              <div id="lombokcode_wrap" class="source_code">
                <p class="lombok_class_wrap">class <input id="lombok_class_name" type="text" />{"{"}</p>
                <p id='lombok_file_name_wrap'>file name<input id="lombok_file_name" type="text" /></p>
                <textarea id='lombokcode'></textarea>
                <p class="lombok_class_wrap" >{"}"}</p>
              </div>
            </div>
            <div><pre class="source_code prettyprint" id='javacode'/></div>
          </div>
          <div id='error_message' />
        </body>
      </html>
      )
    case GET(Path(LOMBOK_HEROKU_JS)) =>
      JsResponse
    case GET(Path(LOMBOK_HEROKU_CSS)) =>
      CssResponse
  }

  private[this] final val LOMBOK_HEROKU_JS = "/lombokheroku.js"
  private[this] final val LOMBOK_HEROKU_CSS = "/lombokheroku.css"
  private[this] final val JsResponse =
    Ok ~> ResponseString(IO.readStream(getClass.getResourceAsStream(LOMBOK_HEROKU_JS)))
  private[this] final val CssResponse =
    Ok ~> ResponseString(IO.readStream(getClass.getResourceAsStream(LOMBOK_HEROKU_CSS)))
}

case class SourceFile(name:String,contents:String)

case class Result(error:Boolean,msg:String,result:Seq[SourceFile]){
  import org.json4s.JsonDSL._

  def toJsonResponse = Json(
    (Common.ERROR   -> error) ~
    (Common.MESSAGE -> msg) ~
    (Common.RESULT  -> JObject(result.map{f => JField(f.name,JString(f.contents))}.toList) )
  )
}

object Web {
  def main(args: Array[String]) {
    val debug = Option(args).flatMap{_.headOption.map{java.lang.Boolean.parseBoolean}}.getOrElse(false)
    val port = Properties.envOrElse("PORT",Common.DEFAULT_PORT).toInt
    println("debug mode=" + debug + " port=" + port)
    if(debug){
      unfiltered.util.Browser.open("http://127.0.0.1:" + port)
    }
    unfiltered.jetty.Server.http(port).plan(new App(debug)).run
  }
}

