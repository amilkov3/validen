package io.voklim.internal.common

import io.voklim.test._

class RichShapelessOpsTest extends UnitPropertySpec {

  case class Baz(c: String)
  case class Quux(a: Boolean, e: Baz)
  case class Bar(b: Int, z: Vector[Baz])
  case class Foo(x: String, d: Quux, y: List[Bar])

  property("should successfully convert arbitrarily nested case classes into arbitrarily nested maps") {
    val m = Foo(
      "foo",
      Quux(true, Baz("baz")),
      List(
        Bar(5, Vector(Baz("baz1"))),
        Bar(6, Vector(Baz("baz2")))
      )
    ).toMapNested

    m("x") shouldEqual "foo"
    m("d").asInstanceOf[Map[String, Any]]("e").asInstanceOf[Map[String, String]]("c") shouldEqual "baz"
    val l = m("y").asInstanceOf[List[Map[String, Any]]]
    l(0)("z").asInstanceOf[Vector[Map[String, String]]](0)("c") shouldEqual "baz1"
  }

}
