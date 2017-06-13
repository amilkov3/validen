
##### This project is not operational yet but I'm almost at a 1.0

## Validen

 Generic case class validator

#### Supported datatypes and available validations:
* string: regex, length, mandatory
* double: upper/lower bounds, shape (how many digits to the left and right of the decimal place), mandatory
* integer: bounds, mandatory
* boolean: mandatory
* array: length, mandatory

#### Available validators:

* ShapelessValidator: Uses [shapeless](https://github.com/milessabin/shapeless) to convert
arbitrarily nested case class to a `[Map[String, Any]` which is
then validated against a given Validen AST

* ProductIteratorValidator: Uses the `.productIterator` method
provided by the scala `Product` interface to iterate
through the fields of an arbitrarily nested case class 
and validate them against a given Validen AST

### Usage

```scala

import io.voklim.validen._
/** You can use the parser to parse a spec
and then validate your case class against
it */

case class Baz(c: Double)
case class Quux(a: Boolean, e: Baz)
case class Bar(b: Int, z: Vector[Baz])
case class Foo(x: String, d: Quux, y: List[Bar])

val vobj1 = VParser.parse(
  """
    |{
    | x: string! "(foo|bar){1}.+"r > 10 <= 20,
    | d: {
    | 	a: bool,
    |  	e: {
    |   	c: string! < 10
    |   }!
    | },
    | y: [{
    |  	b: int! = 3,
    |   z: [{
    |   	c: double 1_2 <= 5 
		|  	} = 1],
    | } < 3]!
    |}
	""".stripMargin
).valueOr(e => "Failed to parse schema with: $e")

val instance = Foo(
	"foo",
	Quux(true, Baz(4.44)),
		List(
			Bar(3, Vector(Baz(3.44))),
			Bar(3, Vector(Baz(2.44)))
		)
	)

val validator = new ShapelessValidator
val res: ValidatedNel[String, Unit] = validator.validate(vobj1, instance)

/** Or construct an ast directly and 
validate against that */
 
import io.voklim.validen.ast._
 
val vobj2 = VObj(
	Map(
	"x" -> VStr(Some("(foo|bar){1}.+".r), Some(GreaterThanLessThan((10, false), (20, true))), true),
	"d" -> VObj(
		Map(
			"a" -> VBool(false),
			"e" -> VObj(
				Map(
					"c" -> VStr(None, Some(LessThan(10, false)), true)
				),
				true
			)
		),
		true
	),
	"y" -> VArr(
		VObj(
			Map(
				"b" -> VInt(Some(EqualTo(3)), true),
				"z" -> VArr(
					VObj(
						Map(
							"c" -> VDouble(Some((1, 2)), Some(LessThan(5, true)), false)
						),
						false
					),
					Some(EqualTo(1, false)),
					false
				)
			),
			false
		),
		Some(LessThan(3, false)),
		true
		)
	),
	true
)
/** This approach is significantly gnarlier
* for a quite nested case class such as this
 *  */

validator.validate(vobj2, instance)
```

### Motivation

At work, I needed to make a large service 
multitenant (have the ability to serve any
number of different clients generically). As part 
of this undertaking, I moved endpoint payload validation
(among other things) into a backend store so that
when the service started up it could load
all of its endpoints and corresponding payload
validation schemas in memory. So effectively,
I needed to encode validation not in code (as for
example play json might do it), but in a generic
spec language that could be stored in a backend
as a string or what have you.

The only solution I found that addressed this issue
was [play-json-schema-validator](https://github.com/eclipsesource/play-json-schema-validator)
, which was actually quite nice, but it only worked with
play json and so I had to add a 5th json library dependency
to the service in order to use it (the 
service was an incorrigable mess to begin with)
By using case classes Validen works around 
this issue.

### Moving forward

A problem remains however. Case classes are fine 
but what if you just want to have a validation middleware
that validates all payloads to all endpoints generically
without first converting json to a case class? (which you
typically do only when you get to the specific endpoint's 
handler anyway). To mitigate this
and without writing a new json parser (I want this
library to be as performant as possible), I will be 
adding seperate modules containing generic validators 
for the json asts of all of the major Scala json libraries:

* `validen-circe`
* `validen-argonaut`
* `validen-play`
* `validen-spray`
* (i dont think you should be using any other
libraries but maybe if I have time)
