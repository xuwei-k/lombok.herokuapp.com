package com.herokuapp.lombok

import scalaz._,Scalaz._
import java.io.File
import scala.io.Source.fromFile
import org.json4s._
import org.json4s.native.JsonMethods._
import scalaj.http._

object Client extends Client("http://lombok.herokuapp.com")

object DebugClient extends Client("http://localhost:" + Common.DEFAULT_PORT)

class Client(val URL:String){

  type SourceStr  = (String,String)
  type SourceFile = (String,File)

  val OPTIONS = List( HttpOptions.connTimeout(30000) , HttpOptions.readTimeout(30000) )

  case class Req private[Client](srcs:Seq[SourceStr]){
    lazy val jsonDocument = {
      val array = JObject(srcs.map{case (k,v) => JField(k,JString(v))}.toList)
      render(JObject(List(JField(Common.FILES,array))))
    }
    def prettyJson:String  = pretty(jsonDocument)
    def compactJson:String = compact(jsonDocument)

    println(pretty(render(parse(prettyJson))))

    def str = Http.postData(URL,compactJson).options(OPTIONS).asString

    def get():Either[String,List[SourceStr]] = {
      val s = str
      println(s)
      val json = parse(s)
      val JBool(isError)   = json \ Common.ERROR
      val JString(message) = json \ Common.MESSAGE
      val JObject(result)  = json \ Common.RESULT
      val list = for{
        JField(name,JString(contents)) <- result
      }yield (name,contents)
      Either.cond(! isError,list,message)
    }

    def add(src:SourceStr,sources:SourceStr*):Req = Req(src +: (sources ++ srcs))

    def add(src:SourceFile,sources:SourceFile*)(implicit e:DummyImplicit):Req =
      Req( srcs ++ (src +: sources).map{case (k,v) => k -> fromFile(v).mkString} )
  }

  def apply(src:SourceStr,sources:SourceStr*):Req = Req(src +: sources)

  def apply(src:SourceFile,sources:SourceFile*)(implicit e:DummyImplicit):Req =
    Req( (src +: sources).map{case (k,v) => k -> fromFile(v).mkString} )

}

