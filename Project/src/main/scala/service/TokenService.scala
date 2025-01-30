package service

import cats.MonadThrow
import cats.data.EitherT
import cats.data.EitherT.liftF
import cats.effect.{Clock, Sync}
import cats.syntax.all._
import domain.authentication.{JWTToken, UserClaims}
import domain.errors.{AppError, ExpirationTokenError, InvalidTokenError}
import domain.role.Role
import domain.user.{UserId, UserName}
import io.circe.syntax.EncoderOps
import tsec.jws.mac.{JWSMacCV, JWTMac}
import tsec.jwt.JWTClaims
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.mac.jca.MacSigningKey

import java.time.Instant

trait TokenService[F[_]] {

  def jwtEncode(claims: UserClaims): F[JWTToken]
  def verifyUser(token: JWTToken): EitherT[F, AppError, UserClaims]

}

object TokenService {

  trait JWTWrapper[F[_], JA] {

    def jwtEncode(claims: JWTClaims): F[JWTMac[JA]]
    def jwtDecode(token: String): F[JWTMac[JA]]

  }

  object JWTWrapper {

    def make[F[_]: Sync, JA: JWTMacAlgo](
      jwtSecretKey: MacSigningKey[JA]
    )(implicit
      JWSMacCV: JWSMacCV[F, JA]
    ): JWTWrapper[F, JA] = new JWTWrapper[F, JA] {

      override def jwtEncode(claims: JWTClaims): F[JWTMac[JA]] =
        JWTMac.build[F, JA](claims, jwtSecretKey)

      override def jwtDecode(token: String): F[JWTMac[JA]] =
        JWTMac.verifyAndParse[F, JA](token, jwtSecretKey)
    }

  }

  def make[F[_]: MonadThrow: Clock, JA: JWTMacAlgo](
    jwtWrapper: JWTWrapper[F, JA]
  ): TokenService[F] =
    new TokenService[F] {

      override def jwtEncode(claims: UserClaims): F[JWTToken] = for {
        now <- Clock[F].realTime
        token <- jwtWrapper
          .jwtEncode(
            JWTClaims(
              issuer = Some("SimpleSocialNetwork"),
              subject = Some(claims.id.value.toString),
              expiration = Some(Instant.ofEpochSecond(now.toSeconds + 3600)),
              customFields =
                Seq("username" -> claims.name.value.asJson, "roles" -> claims.roles.asJson)
            )
          )
          .map(jwtMac => JWTToken(jwtMac.toEncodedString))
      } yield token

      private def jwtDecode(token: JWTToken): F[Option[(UserClaims, Instant)]] =
        jwtWrapper
          .jwtDecode(token.token)
          .map(jwt =>
            for {
              userId   <- jwt.body.subject.flatMap(_.toLongOption).map(UserId(_))
              exp      <- jwt.body.expiration
              userName <- jwt.body.getCustom[String]("username").toOption.map(UserName(_))
              userRoles <- jwt.body
                .getCustom[Set[Role]]("roles")
                .toOption
            } yield (UserClaims(userId, userName, userRoles), exp)
          )
          .handleError(_ => None)

      override def verifyUser(token: JWTToken): EitherT[F, AppError, UserClaims] = {
        for {
          claims <- EitherT(jwtDecode(token).map(_.toRight(InvalidTokenError(token))))
            .leftMap(identity[AppError])

          now <- liftF(Clock[F].realTime)
          _ <- EitherT.cond[F](
            now.toSeconds < claims._2.getEpochSecond,
            (),
            identity[AppError](ExpirationTokenError(token))
          )
        } yield claims._1
      }
    }

}
