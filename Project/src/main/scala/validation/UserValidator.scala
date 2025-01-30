package validation

import cats.syntax.either._
import domain.errors.{IncorrectUserEmailError, IncorrectUserNameError, WeakUserPasswordError}
import domain.user.{UserEmail, UserName, UserPassword}

trait UserValidator {

  def validateName(name: UserName): Either[IncorrectUserNameError, UserName]
  def validateEmail(email: UserEmail): Either[IncorrectUserEmailError, UserEmail]
  def validatePassword(password: UserPassword): Either[WeakUserPasswordError, UserPassword]

}

// TODO quires?? почему either
object UserValidator {

  def make(): UserValidator = new UserValidator {
    override def validateName(name: UserName): Either[IncorrectUserNameError, UserName] = {
      val usernamePattern = "^[a-zA-Z0-9_]+$".r

      if (usernamePattern.matches(name.value))
        name.asRight
      else
        IncorrectUserNameError("should contains only latin letters, digits or underscores").asLeft
    }

    override def validateEmail(email: UserEmail): Either[IncorrectUserEmailError, UserEmail] = {
      val emailPattern = "^([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9_-]+)$".r

      if (emailPattern.matches(email.value))
        email.asRight
      else
        IncorrectUserEmailError(email).asLeft
    }

    override def validatePassword(
      password: UserPassword
    ): Either[WeakUserPasswordError, UserPassword] = {
      val minimumLength = 8

      if (password.value.length < minimumLength)
        WeakUserPasswordError(
          s"Password should contain not less than $minimumLength symbols"
        ).asLeft
      else
        password.asRight
    }
  }

}
