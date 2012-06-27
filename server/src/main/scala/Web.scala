package com.herokuapp.xtend

import scalaz._,Scalaz._
import unfiltered.request._
import unfiltered.response._
import util.Properties
import sbt.{Path=>_,Logger=>_,Level=>_,_}
import java.io.{Writer,File}
import org.eclipse.xtend.core.compiler.batch.XtendBatchCompiler
import org.apache.log4j.BasicConfigurator
import org.eclipse.xtend.core.XtendStandaloneSetup
import net.liftweb.json._
import org.apache.log4j.{Logger => Log4jLogger,Level,WriterAppender,HTMLLayout,SimpleLayout,Layout}

class App(debug:Boolean) extends unfiltered.filter.Plan {

  lazy val libClass = classOf[org.eclipse.xtext.xbase.lib.CollectionLiterals]
  lazy val xtendLibJar = new File(libClass.getProtectionDomain.getCodeSource.getLocation.getFile)

  def xtend2java(src:Seq[SourceFile]) = {
    IO.withTemporaryDirectory{in =>
      src.foreach{f =>
        IO.writeLines(in / f.name ,f.contents.pure[Seq] )
      }
      IO.withTemporaryDirectory{out =>
        compileXtend(out,in,Seq(xtendLibJar))
      }
    }
  }

  def createLoggers():(Writer,Writer) = {
    val logger = Log4jLogger.getLogger("org.eclipse.xtext")
    logger.setAdditivity(false)
    logger.setLevel(Level.DEBUG)
    logger.removeAllAppenders()
    def add(layout:Layout):Writer = {
      val w = new java.io.CharArrayWriter
      val appender = new WriterAppender(layout,w)
      logger.addAppender(appender)
      w
    }
    (new SimpleLayout(),new HTMLLayout()).mapElements(add,add)
  }

  def compileXtend(out:File,in:File,cp:Seq[File]):Either[ErrorMessage,Seq[SourceFile]] = {
    val (simple,html) = createLoggers()
    try{
      val resultWriter = new java.io.CharArrayWriter
      val injector = new XtendStandaloneSetup().createInjectorAndDoEMFRegistration
      val c = injector.getInstance(classOf[XtendBatchCompiler])
      c.setOutputPath(out.toString())
      c.setSourcePath(in.toString())
      c.setOutputWriter(resultWriter)
//      c.setErrorWriter(simple) // TODO invalid ?
      c.setVerbose(true)
      c.setClassPath(cp.map{_.getAbsolutePath}.mkString(File.pathSeparator))
      if(c.compile()){
        (out ** "*.java").get.map{f => SourceFile(f.getName,IO.read(f))}.right
      }else{
        ErrorMessage(("compile fail" + simple.toString),html.toString).left
      }
    }catch{
      case e =>
        ErrorMessage(Seq(simple.toString,e.toString).mkString("\n\n"),html.toString).left
    }
  }

  val GITHUB = "https://github.com/xuwei-k/xtend.herokuapp.com"

  def intent = {
    case r @ POST(Path("/")) =>
      val str = Body.string(r)
      if(debug){
        println(str)
        parseOpt(str).map{ j =>
          println(pretty(render(j)))
        }.getOrElse{
          println("fail parse json. " + str + " is not valid json")
        }
      }

      val sourceFiles = for{
        JObject(List(JField(Common.FILES,JObject(json)))) <- parseOpt(str)
        files = for{
          JField(name,JString(contents)) <- json
        } yield SourceFile(name,contents)
      }yield files

      sourceFiles.map(xtend2java).map{
        case Right(seq)  => Result(false,EmptyError,seq)
        case Left(error) => Result(true,error,Nil)
      }.getOrElse{
        val msg = "invalid params " + str
        Result(true,ErrorMessage(msg,msg),Nil)
      }.toJsonResponse

    case GET(Path("/")) =>
      val initCode = "def hello(){'hello'}"

      Html(
      <html>
        <head>
          <script type="text/javascript" src="http://code.jquery.com/jquery-1.7.2.js" />
          <script type="text/javascript" src="/xtendheroku.js" />
        </head>
        <body>
          <button id='compile' >compile</button>
          <textarea id='xtendcode' cols='50' rows='20'>{initCode}</textarea>
          <textarea id='javacode' cols='50' rows='20'></textarea>
          <div id='error_message' />
        </body>
      </html>
      )
  }
}

case class SourceFile(name:String,contents:String)

case class ErrorMessage(simple:String,html:String)

object EmptyError extends ErrorMessage("","")

case class Result(error:Boolean,msg:ErrorMessage,result:Seq[SourceFile]){
  import net.liftweb.json.JsonDSL._

  def toJsonResponse = Json(
    (Common.ERROR   -> error) ~
    (Common.MESSAGE -> msg.simple) ~
    (Common.HTML_MESSAGE -> msg.html) ~
    (Common.RESULT  -> JObject(result.map{f => JField(f.name,JString(f.contents))}.toList) )
  )
}

object Web {
  def main(args: Array[String]) {
    val debug = Option(args).flatMap{_.headOption.map{java.lang.Boolean.parseBoolean}}.getOrElse(false)
    val port = Properties.envOrElse("PORT",Common.DEFAULT_PORT).toInt
    println("debug mode=" + debug + " port=" + port)
    if(debug){
      unfiltered.util.Browser.open("http://localhost:" + port)
    }
    unfiltered.jetty.Http(port).resources(getClass.getResource("/")).filter(new App(debug)).run
  }
}

