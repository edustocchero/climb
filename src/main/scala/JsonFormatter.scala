package com.github.edustocchero.climb

import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol.*

case class Post(id: Long, content: String, userName: String)
case class NewPost(content: String, userName: String)

trait JsonFormatter {
  implicit val postFormatter: RootJsonFormat[Post] = jsonFormat3(Post.apply)
  implicit val newPostFormatter: RootJsonFormat[NewPost] = jsonFormat2(NewPost.apply)
}
