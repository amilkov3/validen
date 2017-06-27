package io.voklim.validen.ast

import scala.util.matching.Regex
import io.voklim.internal.common._

trait Requirable { def req: Boolean }
sealed trait VValue extends Requirable
case class VStr(regex: Option[Regex], len: Option[Bounds], req: Boolean) extends VValue
case class VInt(bounds: Option[Bounds], req: Boolean) extends VValue
case class VDbl(shape: Option[VDbl.Shape], bounds: Option[Bounds], req: Boolean) extends VValue
case class VBool(req: Boolean) extends VValue
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
        (s"$s did not match regex: $r").invalidNel,
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

object VDbl {
  type Shape = (Int, Int)

  def conforms(d: Double, sh: Shape) = {
    val b = BigDecimal(d)
    val left = b.precision - b.scale
    (
      (b.scale == sh._2).validatedNel((), s"${b.scale} places to the right of the decimal, not ${sh._2}") |@|
      (left == sh._1).validatedNel((), s"${left} places to the left of decimal, not ${sh._1}")
    ).tupled.map(_ => ())
  }

  def validate(d: Double, vfl: VDbl) = {
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

  //TODO
  /** Numeric is convenient to use here but it ends up polluting
    * the Bounds ADT and some methods.*/
  def conforms[A: Numeric](i: A, b: Bounds) = b match {
    case eq: Eq[A] @unchecked => Eq.conforms(i, eq)
    case gt: Gt[A] @unchecked => Gt.conforms(i, gt)
    case lt: Lt[A] @unchecked => Lt.conforms(i, lt)
    case gte: Gte[A] @unchecked => Gte.conforms(i, gte)
    case lte: Lte[A] @unchecked => Lte.conforms(i, lte)
    case bt: Btw[A] @unchecked => Btw.conforms(i, bt)
  }
}

sealed trait Bounds
sealed trait Lower[A]
sealed trait Upper[A]
case class Eq[A: Numeric](num: A) extends Bounds
case class Lt[A: Numeric](upper: A) extends Bounds with Upper[A]
case class Lte[A: Numeric](upper: A) extends Bounds  with Upper[A]
case class Gt[A: Numeric](lower: A) extends Bounds with Lower[A]
case class Gte[A: Numeric](lower: A) extends Bounds with Lower[A]
case class Btw[A: Numeric](lower: Lower[A], upper: Upper[A]) extends Bounds

object Btw {
  def conforms[A](i: A, bt: Btw[A])(implicit ev: Numeric[A]) = {
    bt match {
      case Btw(gt: Gt[A], lt: Lt[A]) =>
        (Gt.conforms(i, gt) |@| Lt.conforms(i, lt)).tupled.map(_ => ())
      case Btw(gte: Gte[A], lt: Lt[A]) =>
        (Gte.conforms(i, gte) |@| Lt.conforms(i, lt)).tupled.map(_ => ())
      case Btw(gt: Gt[A], lte: Lte[A]) =>
        (Gt.conforms(i, gt) |@| Lte.conforms(i, lte)).tupled.map(_ => ())
      case Btw(gte: Gte[A], lte: Lte[A]) =>
        (Gte.conforms(i, gte) |@| Lte.conforms(i, lte)).tupled.map(_ => ())
    }
  }
}

object Eq {
  def conforms[A](i: A, e: Eq[A])(implicit ev: Numeric[A]) = {
    ev.equiv(i, e.num).validatedNel((), s"$i is not equal to ${e.num}")
  }
}

object Lt {
  def conforms[A](i: A, u: Lt[A])(implicit ev: Numeric[A]) = {
    ev.lt(i, u.upper).validatedNel((), s"$i is not less than $u")
  }
}

object Lte {
  def conforms[A](i: A, u: Lte[A])(implicit ev: Numeric[A]) = {
    ev.lteq(i, u.upper).validatedNel((), s"$i is not less than or equal to $u")
  }
}

object Gt {
  def conforms[A](i: A, l: Gt[A])(implicit ev: Numeric[A]) = {
    ev.gt(i, l.lower).validatedNel((), s"$i is not greater than ${l.lower}"
  }
}

object Gte {
  def conforms[A](i: A, l: Gte[A])(implicit ev: Numeric[A]) = {
    ev.gteq(i, l.lower).validatedNel((), s"$i is not greater than or equal to ${l.lower}")
  }
}
