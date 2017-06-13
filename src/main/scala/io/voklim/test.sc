import io.voklim.test.ast._
import io.voklim.validen.parser.VParser
import io.voklim.internal.common._

/*
"arr" : [ string ] = 3,
	"bool": bool,
	"fl": float! 2_2 > 4 <= 5
* */

val v = new VParser


v.parse(v.test, "str: ")
v.parse(v.member, """str: string! "(bar|baz)"r""")
//v.parse(v.regex, """"""(bar|baz)"r")

val schema = VObj(v.parse(v.obj, """{
	str: string! "(bar|baz)"r,
	int: int > 100
}""").get, false)

case class Foo(str: String, int: Int)



import shapeless._
import shapeless.syntax._
//import shapeless.ops.record._
import shapeless.ops._
import shapeless.ops.hlist.ToTraversable._
import shapeless.ops.product._
import shapeless.syntax.std.product._

implicitly[ToMap.Aux[Foo, Symbol, Any]]
Foo("string", 1).toMap
//(ToMap.Aux[Foo, Symbol, Any])

//val x1 = LabelledGeneric[Foo]

//ToMap[x1.Repr].apply(x1.to(Foo("hello", 5)))

//ev2: Keys.Aux[R, K],
    //ev3: Values.Aux[R, V],
//K <: HList,

def validate[A <: Product, R <: HList, V <: Any](schema: VObj, a: A)
  (implicit
    ev1: Generic.Aux[A, R],
    ev2: ToMap.Aux[A, Symbol, V]
  )= {
  val m = a.toMap
  schema.fields.foreach { case (k, v) =>
    m.get(Symbol(k)).cata(
      if (checkRequired(v)) throw new Exception("Req field missing"),
        //.validatedNel((), s"Missing"),
      v1 => checkType(v, v1)
    )
  }
}

validate(schema, Foo("hello", 10))

def validateI(v: VValue, o: Any) = (v, o) match {
  case (vs: VStr, os: String) => VStr.validate(os, vs)
  case (vb: VBool, ob: Boolean) => ().validNel
  case (vf: VFloat, of: Float) => VFloat.validate(of, vf)
  case (va: VArr, oa: List[_]) => VArr.validate(oa, va)
  case (vi: VInt, oi: Int) => VInt.validate(oi, vi)
  case (vo: VObj, op: Product) => ().validNel
  case _ => NonEmptyList.of(s"Invalid").invalid[Unit]
}

def checkType(v: VValue, o: Any) = (v, o) match {
  case (_: VStr, _: String) => true
  case (_: VBool, _: Boolean) => true
  case (_: VFloat, _: Float) => true
  case (_: VArr, _: List[_]) => true
  case (_: VInt, _: Int) => true
  case (_: VObj, _: Product) => true
  case _ => false
}

def checkRequired(v: VValue) = v match {
  case s: VStr => s.req
  case b: VBool => b.req
  case f: VFloat => f.req
  case a: VArr => a.req
  case i: VInt => i.req
  case o: VObj => o.req
}