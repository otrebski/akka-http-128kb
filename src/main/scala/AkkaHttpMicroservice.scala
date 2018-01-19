import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContextExecutor


case class User(firstName: String, lastName: String)

case class Response(body: String)


trait Protocols extends DefaultJsonProtocol {
  implicit val userFormat = jsonFormat2(User.apply)
  implicit val responseFormat = jsonFormat1(Response.apply)
}

trait Service extends Protocols {
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  def config: Config

  val logger: LoggingAdapter


  val routes: Route = {

    val singleRoute = (post & entity(as[User])) { user =>
      complete {
        Response(s"Single user $user")
      }
    }
    val listRoute = (post & entity(as[List[User]])) { users =>
      complete {
        Response(s"List of users, size ${users.size}")

      }
    }

    path("u1") {
      listRoute ~
        singleRoute
    } ~ path("u2") {
      singleRoute ~
        listRoute
    }

  }

}

object AkkaHttpMicroservice extends App with Service {


  override implicit val system: ActorSystem = ActorSystem()
  override implicit val executor: ExecutionContextExecutor = system.dispatcher
  override implicit val materializer: ActorMaterializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
