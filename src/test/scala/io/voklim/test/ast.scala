package io.voklim.test

import io.voklim.validen.ast._

package object ast {

  val vStrAst = VStr(Some("^(foo|bar){1}.+".r), Some(LessThan(20, true)), true)

  val primitiveValidatorAst = VObj(
    Map(
      "str" -> vStrAst,
      "int" -> VInt(Some(GreaterThanLessThan((10, true), (20, false))), true),
      "float" -> VDouble(Some((2, 2)), Some(GreaterThan(2.0, false)), true),
      "bool" -> VBool(false)
    ),
    true
  )

  case class PrimitiveAstTestClass(
    str: String,
    int: Int,
    float: Double,
    bool: Boolean
  )

  val simpleArrayValidatorAst = VObj(
    Map(
      "arr" -> VArr(vStrAst, Some(EqualTo(2)), true)
    ),
    true
  )

  case class SimpleArrayAstTestClass(
    arr: List[String]
  )

  val nestedArrayObjAst = VObj(
    Map(
      "obj" -> primitiveValidatorAst,
      "arr" -> VArr(simpleArrayValidatorAst, None, true)
    ),
    true
  )

  case class NestedArrayAstTestClass(
    obj: PrimitiveAstTestClass,
    arr: List[SimpleArrayAstTestClass]
  )
}
