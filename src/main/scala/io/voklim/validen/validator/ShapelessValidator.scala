package io.voklim.validen.validator

import io.voklim.validen.ast._
import shapeless._
import io.voklim.internal.common._

class ShapelessValidator {

  def validate[A <: Product, R <: HList](vo: VObj, a: A)(implicit
    ev1: LabelledGeneric.Aux[A, R],
    ev2: ToNestedMap[R]
  ): ValidatedNel[String, Unit] = {
    val aMap = a.toMapNested
    _validate(aMap, vo)
  }

  private def _validate(aMap: Map[String, Any], vo: VObj): ValidatedNel[String, Unit] = {
    vo.fields.foldLeft(().validNel[String]){ case (accum, (vk ,vv)) =>
      val out = aMap.get(vk).cata(
        vv.req.validatedNel((), s"Req field named $vk is missing in your case class"),
        av => (vv, av) match {
          case (vs: VStr, as: String) => VStr.validate(as, vs)
          case (_: VBool, _: Boolean) => ().validNel
          case (vf: VDouble, af: Double) => VDouble.validate(af, vf)
          case (va: VArr, al: List[_]) => validateArr(al, va)
          case (vi: VInt, ai: Int) => VInt.validate(ai, vi)
          case (vo: VObj, cc: Map[String, _] @ unchecked) => _validate(cc, vo)
          case (v, a) => s"Incompatible or unrecognized type: ${a.getClass().getName()}".invalidNel
        }
      )
      (accum |@| out).tupled.map(_ => ())
    }
  }

  private def validateArr[A](as: List[A], va: VArr): ValidatedNel[String, Unit] = {
    val lenV = va.len.cata(
      ().validNel,
      implicitly[ValidatableBounds[List[_]]].validate(as, _)
    )
    val accumV = as.foldLeft(().validNel[String]){case (accum, a) =>
      val out = (va.elems, a) match {
        case (vs: VStr, as: String) => VStr.validate(as, vs)
        case (_: VBool, _: Boolean) => ().validNel
        case (vf: VDouble, af: Double) => VDouble.validate(af, vf)
        case (va: VArr, al: List[_]) => validateArr(al, va)
        case (vi: VInt, ai: Int) => VInt.validate(ai, vi)
        case (vo: VObj, cc: Map[String, _] @ unchecked) => _validate(cc, vo)
        case (v, a) => s"Incompatible or unrecognized type: ${a.getClass().getName()}".invalidNel
      }
      (accum |@| out).tupled.map(_ => ())
    }
    (accumV |@| lenV).tupled.map(_ => ())
  }
}
