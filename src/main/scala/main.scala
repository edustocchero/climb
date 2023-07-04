import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import scala.io.StdIn

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import spray.json.DefaultJsonProtocol.*
import spray.json.RootJsonFormat

@main
def main(): Unit = {
  implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "climb-system")
  implicit val executionContext: ExecutionContext = system.executionContext

  final case class Post(id: Long, content: String, userName: String)
  final case class NewPost(userName: String, content: String)

  implicit val postFormat: RootJsonFormat[Post] = jsonFormat3(Post.apply)
  implicit val newPostFormat: RootJsonFormat[NewPost] = jsonFormat2(NewPost.apply)

  var postsCount: Long = 0
  var posts: Map[Long, Post] = Map.empty

  def fetchPost(id: Long): Future[Option[Post]] = Future {
    posts.find(_._2.id.equals(id)).map(_._2)
  }

  def savePost(newPost: NewPost): Future[Done] = {
    postsCount = postsCount + 1
    posts = posts.+(postsCount -> Post(postsCount, newPost.content, newPost.userName))
    Future { Done }
  }

  val route =
    concat(
      get {
        pathPrefix("posts" / LongNumber) { id =>
          val maybePost: Future[Option[Post]] = fetchPost(id)

          onSuccess(maybePost) {
            case Some(post) => complete(post)
            case None       => complete(StatusCodes.NotFound)
          }
        }
      },
      post {
        path("posts" / "new") {
          entity(as[NewPost]) { newPost =>
            val saved: Future[Done] = savePost(newPost)
            onSuccess(saved) { _ =>
              complete(StatusCodes.Created)
            }
          }
        }
      }
    )

  val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

  println(s"Server running at localhost:8080")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
