package com.herokuapp.xtend

import scalaz._,Scalaz._
import unfiltered.request._
import unfiltered.response._
import util.Properties
import sbt.{Path=>_,_}
import java.io.File
import org.eclipse.xtext.xtend2.compiler.batch.Xtend2BatchCompiler
import org.apache.log4j.BasicConfigurator
import org.eclipse.xtext.xtend2.Xtend2StandaloneSetup
import net.liftweb.json._

object App extends unfiltered.filter.Plan {

  def xtend2java(src:Seq[SourceFile]) = {
    IO.withTemporaryDirectory{in =>
      src.foreach{f =>
        IO.writeLines(in / f.name ,f.contents.pure[Seq] )
      }
      IO.withTemporaryDirectory{out =>
        compileXtend(out,in,file(".").pure[Seq])
      }
    }
  }


  def compileXtend(out:File,in:File,cp:Seq[File]):Either[String,Seq[SourceFile]] = {
    try{
      val writer = new java.io.CharArrayWriter
      BasicConfigurator.configure()
      val injector = new Xtend2StandaloneSetup().createInjectorAndDoEMFRegistration
      val c = injector.getInstance(classOf[Xtend2BatchCompiler])
      c.setOutputPath(out.toString())
      c.setSourcePath(in.toString())
      c.setOutputWriter(writer)
      c.setErrorWriter(writer)
      c.setVerbose(true)
      c.setClassPath(cp.map{_.getAbsolutePath}.mkString(File.pathSeparator))
      if(c.compile()){
        (out ** "*.java").get.map{f => SourceFile(f.getName,IO.read(f))}.right
      }else{
        ("compile fail\n" + writer.toString).left
      }
    }catch{
      case e => e.toString.left
    }
  }

  val GITHUB = "https://github.com/xuwei-k/xtend.herokuapp.com"

  def intent = {
    case r @ POST(Path("/")) =>
      val str = Body.string(r)
      println(str)
      parseOpt(str).map{ j =>
        println(pretty(render(j)))
      }.getOrElse{
        println("fail parse json. " + str + " is not valid json")
      }

      val sourceFiles = for{
        JObject(List(JField(Common.FILES,JObject(json)))) <- parseOpt(str)
        files = for{
          JField(name,JString(contents)) <- json
        } yield SourceFile(name,contents)
      }yield files

      sourceFiles.map(xtend2java).map{
        case Right(seq)  => Result(false,"",seq)
        case Left(error) => Result(true,error,Nil)
      }.getOrElse(
        Result(true,"invalid params "+ str,Nil)
      ).toJsonResponse

    case GET(Path("/")) => Html(
      <html>
        <head>
          <script type="text/javascript" src="http://code.jquery.com/jquery-1.7.2.js" />
          <script type="text/javascript" src="/xtendheroku.js" />
        </head>
        <body>
          <button id='compile' >compile</button>
          <textarea id='xtendcode' cols='50' rows='20'></textarea>
          <textarea id='javacode' cols='50' rows='20'></textarea>
        </body>
      </html>
    )
/*
          <button text='compile' onClick='submitCode()' />
    case GET(_) => Html(
      <h1><a href={GITHUB}>{GITHUB}</a><br />GUI client will be soon available ... </h1>
    )
*/
  }
}

case class SourceFile(name:String,contents:String)

case class Result(error:Boolean,message:String,result:Seq[SourceFile]){
  import net.liftweb.json.JsonDSL._

  def toJsonResponse = Json(
    (Common.ERROR   -> error) ~
    (Common.MESSAGE -> message) ~
    (Common.RESULT  -> JObject(result.map{f => JField(f.name,JString(f.contents))}.toList) )
  )
}

object Web {
  def main(args: Array[String]) {
    val port = Properties.envOrElse("PORT",Common.DEFAULT_PORT).toInt
    unfiltered.jetty.Http(port).resources(getClass.getResource("/")).filter(App).run
  }
}

