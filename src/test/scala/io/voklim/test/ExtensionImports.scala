package io.voklim.test

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

trait ExtensionImports {

  implicit def toRichValidated[E, A](repr: Validated[E, A]) = new RichValidation[E, A](repr)
}

final class RichValidation[E, S](val repr: Validated[E, S]) extends AnyVal {

  def asSuccess =
    repr.valueOr(_ => throw new AssertionError(s"$repr is not a success but a failure"))

  def asFailure = repr match {
    case Valid(_) => throw new AssertionError(s"$repr is not a failure but a success")
    case Invalid(e) => e
  }
}
