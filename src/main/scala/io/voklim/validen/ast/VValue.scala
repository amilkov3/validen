package io.voklim.validen.ast

import scala.util.matching.Regex
import io.voklim.internal.common._

trait Requirable { def req: Boolean }
sealed trait VValue extends Requirable
case class VStr(regex: Option[Regex], len: Option[Bounds], req: Boolean) extends VValue
case class VInt(bounds: Option[Bounds], req: Boolean) extends VValue
case class VDouble(shape: Option[VDouble.Shape], bounds: Option[Bounds], req: Boolean) extends VValue
case class VBool(req: Boolean) extends VValue with Requirable
case class VArr(elems: VValue, len: Option[Bounds], req: Boolean) extends VValue
case class VObj(fields: Map[String, VValue], req: Boolean) extends VValue

object VStr {
  def validate(s: String, vstr: VStr): ValidatedNel[String, Unit] = {
    (vstr.len.cata(
      ().validNel,
      s.validateBounds(_)
    ) |@|
    vstr.regex.cata(
      ().validNel,
      r => r.findFirstIn(s).cata(
        NonEmptyList.of(s"$r regex did not match $s").invalid,
        _ => ().valid
      )
    )).tupled.map(_ => ())
  }
}

object VInt {
  def validate(i: Int, vint: VInt)= {
    vint.bounds.cata(
      ().validNel,
      i.validateBounds(_)
    )
  }
}

object VArr {
  def validate(ls: List[_], varr: VArr) = {
    varr.len.cata(
      ().validNel,
      implicitly[ValidatableBounds[List[_]]].validate(ls, _)
    )
  }
}

object VDouble {
  type Shape = (Int, Int)

  def conforms(d: Double, sh: Shape) = {
    val b = BigDecimal(d)
    val left = b.precision - b.scale
    (
      (b.scale == sh._2).validatedNel((), s"${b.scale} places to the right of the decimal, not ${sh._2}") |@|
      (left == sh._1).validatedNel((), s"${left} places to the left of decimal, not ${sh._1}")
    ).tupled.map(_ => ())
  }

  def validate(d: Double, vfl: VDouble) = {
    (vfl.bounds.cata(
      ().validNel,
      d.validateBounds(_)
    ) |@|
    vfl.shape.cata(
      ().validNel,
      conforms(d, _)
    )).tupled.map(_ => ())
  }
}

object Bounds {

  type Bound[A] = (A, Boolean)

  def conforms[A: Numeric](i: A, b: Bounds) = b match {
    case eq: EqualTo[A] @unchecked => EqualTo.conforms(i, eq)
    case gt: GreaterThan[A] @unchecked => GreaterThan.conforms(i, gt)
    case lt: LessThan[A] @unchecked => LessThan.conforms(i, lt)
    case gtlt: GreaterThanLessThan[A] @unchecked =>
      (
        GreaterThan.conforms(i, GreaterThan(gtlt.lower)) |@|
        LessThan.conforms(i, LessThan(gtlt.upper))
      ).tupled.map(_ => ())
  }
}

sealed trait Bounds
case class EqualTo[A: Numeric](num: A) extends Bounds

object EqualTo {

  //def conforms(d: , b: EqualTo) = (d == b.num).validatedNel((), s"$d is not equal to ${b.num}")
  def conforms[A](i: A, b: EqualTo[A])(implicit ev: Numeric[A]) = {
    (ev.equiv(i, b.num)).validatedNel((), s"$i is not equal to ${b.num}")
  }
}

case class GreaterThan[A: Numeric](lower: Bounds.Bound[A]) extends Bounds
object GreaterThan {

  def conforms[A](i: A, b: GreaterThan[A])(implicit ev: Numeric[A]) = {
    val (l, inc) = b.lower
    if (inc){
      (ev.gteq(i, l)).validatedNel((), s"$i is not greater than or equal to $l")
    } else {
      (ev.gt(i, l)).validatedNel((), s"$i is not greater than $l")
    }
  }
}

case class LessThan[A: Numeric](upper: Bounds.Bound[A]) extends Bounds

object LessThan {
  def conforms[A](i: A, b: LessThan[A])(implicit ev: Numeric[A]) = {
    val (u, inc) = b.upper
    if (inc) {
      (ev.lteq(i, u)).validatedNel((), s"$i is not less than or equal to $u")
    } else {
      (ev.lt(i, u)).validatedNel((), s"$i is not less than $u")
    }
  }
}

case class GreaterThanLessThan[A: Numeric](lower: Bounds.Bound[A], upper: Bounds.Bound[A]) extends Bounds
