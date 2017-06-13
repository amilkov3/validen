package io.voklim.internal.common

import scala.util.matching.Regex

trait ExtensionImports {
  implicit def toRichOption[A](repr: Option[A]): RichOption[A] = new RichOption[A](repr)

  implicit def toRichBoolean(repr: Boolean): RichBoolean = new RichBoolean(repr)

  implicit def toRichShapelessOps[A](repr: A): RichShapelessOps[A] = new RichShapelessOps[A](repr)

  implicit def toRichRegex(repr: Regex): RichRegex = new RichRegex(repr)
}

final class RichOption[A](val repr: Option[A]) extends AnyVal {
  def cata[B](f: => B, g: A => B): B = repr match {
    case None => f
    case Some(x) => g(x)
  }
}

final class RichBoolean(val repr: Boolean) extends AnyVal {

  //def either[A, B](r: => B, l: => A) = if (repr) Right(r) else Left(l)

  def validatedNel[S, E](r: => S, l: => E): ValidatedNel[E, S] =
    if (repr) r.validNel else l.invalidNel
}

final class RichRegex(val repr: Regex) extends AnyVal {

  //TODO:
  // There's gotta be some type class out there for this. Probably in cats but should be in scala
  def patternEqualTo(r: Regex) = repr.regex == r.regex
}



