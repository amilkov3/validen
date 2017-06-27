package internal.common

trait CategoryImports extends {}
  with cats.syntax.EitherSyntax
  with cats.syntax.OptionSyntax
  with cats.syntax.ValidatedSyntax
  with cats.syntax.CartesianSyntax
{
  type NonEmptyList[+A] = cats.data.NonEmptyList[A]
  val NonEmptyList = cats.data.NonEmptyList

  type ValidatedNel[+E, +A] = cats.data.ValidatedNel[E, A]
  val Validated = cats.data.Validated
}
