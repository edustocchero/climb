package com.github.edustocchero.climb

import akka.Done
import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, ActorRef}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route

import akka.util.Timeout

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object Posts {
  sealed trait Command

  case class Create(content: String, userName: String) extends Command

  case class Find(id: Long, replyTo: ActorRef[Option[Post]]) extends Command

  def apply: Behaviors.Receive[Command] = apply(Map.empty)

  def apply(posts: Map[Long, Post], count: Long = 1): Behaviors.Receive[Command] =
    Behaviors.receive {
      case (ctx, Create(content, userName)) =>
        ctx.log.info(s"creating new post for user $userName")
        val post = Post(count, content, userName)
        apply(posts + (count -> post), count + 1)
      case (ctx, Find(id, replyTo)) =>
        ctx.log.info(s"finding post with id $id")
        replyTo ! posts.get(id)
        Behaviors.same
    }
}

object Main extends JsonFormatter {
  def main(args: Array[String]): Unit = {
    val (host, port) = ("localhost", 8080)

    implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "climb-system")
    implicit val executionContext: ExecutionContext = system.executionContext

    implicit val postsSystem: ActorSystem[Posts.Command] = ActorSystem(Posts.apply, "posts-system")

    val posts: ActorRef[Posts.Command] = postsSystem
    import Posts.*

    val routes =
      pathPrefix("posts") {
        concat(
          post {
            entity(as[NewPost]) { newPost =>
              posts ! Create(newPost.content, newPost.userName)
              complete(StatusCodes.Created)
            }
          },
          get {
            path(LongNumber) { id =>
              implicit val timeout: Timeout = 2.5.seconds
              val maybePost: Future[Option[Post]] = posts.ask(Find(id, _))
              onSuccess(maybePost) {
                case Some(post) => complete(post)
                case None => complete(StatusCodes.NotFound)
              }
            }
          }
        )
      }

    val bindingFuture = Http().newServerAt(host, port).bind(routes)

    println(s"Server running at $host:$port")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
