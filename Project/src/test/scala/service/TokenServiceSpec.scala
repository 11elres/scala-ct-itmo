package service

import cats.effect.{Clock, IO}
import cats.effect.unsafe.implicits.global
import domain.authentication.{JWTToken, UserClaims}
import domain.errors.{ExpirationTokenError, InvalidTokenError}
import domain.role._
import domain.user._
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tsec.mac.jca.HMACSHA256

import java.time.Instant
import scala.concurrent.duration.{DurationInt, FiniteDuration, MILLISECONDS}

class TokenServiceSpec extends AnyFlatSpec with Matchers {

  val expiration: FiniteDuration = 3600.seconds // TODO: to config?
  val claims: UserClaims         = UserClaims(UserId(42), UserName("name"), Set(OrdinaryUserRole))
  // jwt depends on issued time from Sync[IO] and banned tokens from future, so will use now as started time point
  val now: FiniteDuration             = FiniteDuration(Instant.now.toEpochMilli, MILLISECONDS)
  implicit val mockClockIO: Clock[IO] = mock[Clock[IO]]

  val tokenService: TokenService[IO] = TokenService.make(
    TokenService.JWTWrapper.make[IO, HMACSHA256](HMACSHA256.generateKey[IO].unsafeRunSync())
  )

  "TokenService" should "correctly encode-decode not expired token" in {
    when(mockClockIO.realTime).thenReturn(IO.pure(now))
    val token = tokenService.jwtEncode(claims).unsafeRunSync()

    when(mockClockIO.realTime).thenReturn(IO.pure(now + 10.seconds))
    val decodedClaims = tokenService.verifyUser(token).value.unsafeRunSync()

    assert(decodedClaims.isRight, "token should be decodable")

    decodedClaims.toOption.get shouldBe claims
  }

  it should "reject expired token" in {
    when(mockClockIO.realTime).thenReturn(IO.pure(now))
    val token = tokenService.jwtEncode(claims).unsafeRunSync()

    when(mockClockIO.realTime).thenReturn(IO.pure(now + expiration + 10.seconds))
    val decodedClaims = tokenService.verifyUser(token).value.unsafeRunSync()

    assert(decodedClaims.isLeft, "token should be correct, but expired")

    decodedClaims.swap.toOption.get shouldBe ExpirationTokenError(token)
  }

  it should "reject incorrect token" in {
    when(mockClockIO.realTime).thenReturn(IO.pure(now))
    val token = tokenService.jwtEncode(claims).unsafeRunSync()
    val invalidToken = JWTToken(
      token.token.dropRight(1) + (if (token.token.charAt(token.token.length - 1) != '0') '0'
                                  else '1')
    )

    when(mockClockIO.realTime).thenReturn(IO.pure(now + 10.seconds))
    val decodedClaims = tokenService.verifyUser(invalidToken).value.unsafeRunSync()

    assert(decodedClaims.isLeft, "token should be incorrect")

    decodedClaims.swap.toOption.get shouldBe InvalidTokenError(invalidToken)
  }

}
