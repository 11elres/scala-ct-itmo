package controller

import domain.authentication._
import domain.errors.AppError
import domain.role.Role
import domain.user._
import domain.post._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir._

object endpoints {

  private type AuthEndpoint[I, E, O] = Endpoint[JWTToken, I, E, O, Any]

  private val authenticate
    : PublicEndpoint[Unit, Unit, Unit, Any] => AuthEndpoint[Unit, Unit, Unit] =
    _.securityIn(auth.bearer[String]().map(JWTToken(_))(_.token))

  // authentication

  val login: PublicEndpoint[LoginRequest, AppError, JWTToken, Any] =
    endpoint.post
      .in("login")
      .in(jsonBody[LoginRequest])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[JWTToken])
      .tag("Authentication")
      .description("Sign in of existing user")

  val signup: PublicEndpoint[RegisterRequest, AppError, JWTToken, Any] =
    endpoint.post
      .in("signup")
      .in(jsonBody[RegisterRequest])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[JWTToken])
      .tag("Authentication")
      .description("Sign up of new user")

  private val authAll: List[AnyEndpoint] = List(login, signup)

  // users

  val userByName: PublicEndpoint[UserName, AppError, UserInfo, Any] =
    endpoint.get
      .in("user" / path[UserName]("username"))
      .errorOut(jsonBody[AppError])
      .out(jsonBody[UserInfo])
      .tag("Users")
      .description("Get user information")

  val updateUser: AuthEndpoint[(UserName, UserInfo), AppError, UserInfo] =
    authenticate(endpoint.patch)
      .in("user" / path[UserName]("username"))
      .in(jsonBody[UserInfo])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[UserInfo])
      .tag("Users")
      .description("Update user information")

  val deleteUser: AuthEndpoint[UserName, AppError, Unit] =
    authenticate(endpoint.delete)
      .in("user" / path[UserName]("username"))
      .errorOut(jsonBody[AppError])
      .tag("Users")
      .description("Delete user")

  val listUsers: PublicEndpoint[Unit, AppError, List[UserInfo], Any] =
    endpoint.get
      .in("users")
      .errorOut(jsonBody[AppError])
      .out(jsonBody[List[UserInfo]])
      .tag("Users")
      .description("Get user information of all users")

  val assignRole: AuthEndpoint[(UserName, Role), AppError, Unit] =
    authenticate(endpoint.post)
      .in("user" / path[UserName]("username") / "role")
      .in(query[Role]("role"))
      .errorOut(jsonBody[AppError])
      .tag("Users")
      .description("Assign new role to a user")

  val deleteRole: AuthEndpoint[(UserName, Role), AppError, Unit] =
    authenticate(endpoint.delete)
      .in("user" / path[UserName]("username") / "role")
      .in(query[Role]("role"))
      .errorOut(jsonBody[AppError])
      .tag("Users")
      .description("Delete role from a user")

  private val userAll: List[AnyEndpoint] =
    List(userByName, updateUser, deleteUser, listUsers, assignRole, deleteRole)

  // posts

  val createPost: AuthEndpoint[PostInfo, AppError, Post] =
    authenticate(endpoint.post)
      .in("post")
      .in(jsonBody[PostInfo])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[Post])
      .tag("Posts")
      .description("Create post")

  val postById: PublicEndpoint[PostId, AppError, Post, Any] =
    endpoint.get
      .in("post" / path[PostId]("post_id"))
      .errorOut(jsonBody[AppError])
      .out(jsonBody[Post])
      .tag("Posts")
      .description("Get post by id")

  val postByTitle: PublicEndpoint[PostTitle, AppError, List[Post], Any] =
    endpoint.get
      .in("posts" / "find")
      .in(query[PostTitle]("post_title"))
      .errorOut(jsonBody[AppError])
      .out(jsonBody[List[Post]])
      .tag("Posts")
      .description("Get posts by title")

  val deletePost: AuthEndpoint[PostId, AppError, Unit] =
    authenticate(endpoint.delete)
      .in("post" / path[PostId]("post_id"))
      .errorOut(jsonBody[AppError])
      .tag("Posts")
      .description("Delete post")

  val updatePost: AuthEndpoint[(PostId, PostInfo), AppError, Post] =
    authenticate(endpoint.patch)
      .in("post" / path[PostId]("post_id"))
      .in(jsonBody[PostInfo])
      .errorOut(jsonBody[AppError])
      .out(jsonBody[Post])
      .tag("Posts")
      .description("Update post")

  val listUserPosts: PublicEndpoint[UserName, AppError, List[Post], Any] =
    endpoint.get
      .in("user" / path[UserName]("username") / "posts")
      .errorOut(jsonBody[AppError])
      .out(jsonBody[List[Post]])
      .tag("Posts")
      .description("Get user's posts")

  val listPosts: PublicEndpoint[Unit, AppError, List[Post], Any] =
    endpoint.get
      .in("posts")
      .errorOut(jsonBody[AppError])
      .out(jsonBody[List[Post]])
      .tag("Posts")
      .description("Get all posts")

  private val postAll: List[AnyEndpoint] =
    List(createPost, postById, postByTitle, deletePost, updatePost, listUserPosts, listPosts)

  val all: List[AnyEndpoint] = authAll ++ userAll ++ postAll

}
