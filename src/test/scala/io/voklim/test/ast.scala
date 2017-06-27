package io.voklim.test

import io.voklim.validen.ast._

package object ast {

  val vStrAst = VStr(Some("^(foo|bar){1}.+".r), Some(Lte(20)), true)

  val primitiveValidatorAst = VObj(
    Map(
      "str" -> vStrAst,
      "int" -> VInt(Some(Btw(Gte(10), Lt(20))), true),
      "float" -> VDbl(Some((2, 2)), Some(Gt(2.0)), true),
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
      "arr" -> VArr(vStrAst, Some(Eq(2)), true)
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
