
import shapeless.syntax.std.product._
import io.voklim.internal.common._
import io.voklim.validen.ast._
import shapeless.{HList, LabelledGeneric}
import io.voklim.validen.parser.VParser


case class Baz(c: String)
case class Bar(z: Int, a: Boolean)
case class Foo(x: String, d: Baz, y: Vector[Bar])

def test(a: Product): Unit = {
  a.productIterator.foreach( _ match {
    case s: String => println("string")
    case b: Boolean => println("boolean")
    case i: Int => println("int")
    case p: Product => test(p)
  })
}

val f= Foo("hello", Baz("hell1"), Vector(Bar(5, true), Bar(6, false)))

f.toMapNested

//test(f)

def validate[A <: Product, R <: HList](vo: VObj, a: A)(implicit
  ev1: LabelledGeneric.Aux[A, R],
  ev2: ToNestedMap[R]
) = {
  val aMap = a.toMapNested
  _validate(aMap, vo)


}

def _validate(aMap: Map[String, Any], vo: VObj): Unit = {
  vo.fields.foreach{ case (vk ,vv) =>
    aMap.get(vk).cata(
      println("Missing"),
      av => (vv, av) match {
        case (vs: VStr, os: String) => println("string")
        case (vb: VBool, ob: Boolean) => println("boolean")
        case (vf: VDouble, of: Double) => println("float")
        case (va: VArr, oa: List[_]) => println("list")
        case (vi: VInt, oi: Int) => println("int")
        case (vo: VObj, cc: Map[String, _] @ unchecked) =>
          _validate(cc, vo)
        case _ => println("Invalid")
      }
    )
  }
}

/*def _validate1(a: Any, v: VValue) = {

}*/

//val vo = VObj(Map("x" -> VStr(None, None, true), "y" -> VObj(Map("z" -> VInt(None, true)), true)), true)

/*val o = VParser.unsafeParse(
  """
    |{
    | x: string,
    | y: {
    |   z: int
    | }
    |}
  """.stripMargin)*/

//validate(vo, f)
