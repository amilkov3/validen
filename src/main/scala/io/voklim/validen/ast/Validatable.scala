package io.voklim.validen.ast

import io.voklim.internal.common._

trait ValidatableImports {

  implicit def toValidatableBoundsOps[A: ValidatableBounds](repr: A): ValidatableBoundsOps[A] = new ValidatableBoundsOps[A](repr)
}

trait Validatable[A, B] {
  def validate(a: A, b: B): ValidatedNel[String, Unit]
}

trait ValidatableBounds[A] extends Validatable[A, Bounds]{
  override def validate(a: A, b: Bounds): ValidatedNel[String, Unit]
}

object ValidatableBounds {

  def apply[A](implicit ev: ValidatableBounds[A]) = ev

  def instance[A](f: (A, Bounds) => ValidatedNel[String, Unit]) = new ValidatableBounds[A] {
    override def validate(a: A, b: Bounds): ValidatedNel[String, Unit] = f(a, b)
  }

  implicit val validatableBoundsString: ValidatableBounds[String] = instance { (a, b) => Bounds.conforms(a.size, b) }

  implicit val validatableBoundsInt: ValidatableBounds[Int] = instance { (a, b) => Bounds.conforms(a, b) }

  implicit val validatableBoundsDouble: ValidatableBounds[Double] = instance { (a, b) => Bounds.conforms(a, b) }

  implicit val validatableBoundsList: ValidatableBounds[List[_]] = instance { (a, b) => Bounds.conforms(a.length, b)}
}

final class ValidatableBoundsOps[A](val repr: A) extends AnyVal {
  def validateBounds(b: Bounds)(implicit ev: ValidatableBounds[A]) = ev.validate(repr, b)
}
