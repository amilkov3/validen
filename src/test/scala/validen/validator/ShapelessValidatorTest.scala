package validen.validator

import io.voklim.test._
import io.voklim.test.ast._

class ShapelessValidatorTest extends UnitPropertySpec {

  val validator = new ShapelessValidator

  val pat = PrimitiveAstTestClass("foobar", 15, 22.22, true)
  val saat = SimpleArrayAstTestClass(List("foo-two", "bar-elements"))

  property("validate valid case class instance against ast comprised of just primitives") {
    validator.validate(primitiveValidatorAst, pat).asSuccess
  }

  property("validate valid case class instance against ast comprised of a list of a primitive type"){
    validator.validate(simpleArrayValidatorAst, saat).asSuccess
  }

  property("validate valid case class instance against ast comprised of nested obj and case class list") {
    validator.validate(nestedArrayObjAst, NestedArrayAstTestClass(
      pat,
      List(saat)
    )).asSuccess
  }

}
