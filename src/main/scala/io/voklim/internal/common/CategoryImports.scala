package io.voklim.internal.common


trait CategoryImports extends {}
  with cats.syntax.EitherSyntax
  with cats.syntax.OptionSyntax
  with cats.syntax.ValidatedSyntax
  with cats.syntax.CartesianSyntax
{

  //type NonEmptyVector[+A] = cats.data.NonEmptyVector[A]
  type NonEmptyList[+A] = cats.data.NonEmptyList[A]
  val NonEmptyList = cats.data.NonEmptyList

  /*type ValidatedNev[+E, +A] = cats.data.Validated[NonEmptyVector[E], A]*/

  type ValidatedNel[+E, +A] = cats.data.ValidatedNel[E, A]
  val Validated = cats.data.Validated
}
