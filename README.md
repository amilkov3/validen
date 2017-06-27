
##### This project is not operational yet but it's getting close to a 1.0

## Validen

_A generic case class validator designed to alleviate programmatic validation
by encoding validator schemas in a simple spec language with a corresponding AST.
This library focuses on performance with a functional implementation under the hood_

### Components:
* A parser which parses a simple spec language into a json-like validation AST
* A validator which validates any arbitrarily nested case class 
against said AST

### Validations and their spec language syntax
* regex: pattern is enclosed in quotes and suffixed with a literal `r` i.e. `"(foo|bar){0, 1}"r`
* bounds: `= <num>`, `< <num>`, `<= <num>`, `>= <num>`, `> <num>`. mathematical coherence of combinations of these 
 is enforced by the parser
* shape: the shape of `22.22` would be expressed as `2_2` i.e. 2 digits to the right of 
the decimal place and 2 to the left. `222.22` would be `3_2` and so forth. Note the upper 
bound on this validation is `9_9`
* mandatory: whether a field, object, or array is required to be present. expressed as:
  * primitives (add at the end of the type declaration): `<string, double, int, bool>!`
  * object or array : `{ ... }!` `[ ... ]!` (suffix `!` after closing brace or bracket )

I apologize if this is archane. See the example usage below

### Supported datatypes and associated validations:
* string: regex, length(expressed as bounds), mandatory
* double: bounds, shape, mandatory 
* integer: bounds, mandatory
* boolean: mandatory
* array: length, mandatory

### Available validators:

* `ShapelessValidator`: Uses [shapeless](https://github.com/milessabin/shapeless) to convert
any case class to a `Map[String, Any]` (a type which uncoincidentally mirrors the AST)

* `ProductIteratorValidator`: Uses the `.productIterator` method
provided by the `Product` interface from the scala standard library

Benchmarks soon to come but I think its pretty intuitive that the 
`ProductIteratorValidator`, though more pedestrian, will have the best performance characteristics 
since to construct the `Map[String, Any]`, shapeless must first construct an 
`HList` representation of the case class. This must then be traversed to create the map
, and then that map has to be recursively traversed to validate
a case class instance against an AST instance. Only the later must be performed in the case
 of the `ProductIteratorValidator`

##### A note about functional library dependencies:

The `.validate` function on a given validator
returns a `ValidatedNel[String, Unit]` [from](http://typelevel.org/cats/datatypes/validated.html)
the [cats](http://typelevel.org/cats/) library. 
Cats is also used throughout internally. 

It could quite trivially be ported to [scalaz](https://github.com/scalaz/scalaz)

### Usage

You can use the parser to parse a spec and then 
validate your case class against it

```scala

import validen._

case class Baz(c: Double)
case class Quux(a: Boolean, e: Baz)
case class Bar(b: Int, z: Vector[Baz])
case class Foo(x: String, d: Quux, y: List[Bar])

val vobj = VParser.parse(
  """
    |{
    | x: string! "(foo|bar){1}.+"r > 10 <= 20,
    | d: {
    | 	a: bool,
    |  	e: {
    |  		c: string! < 10
    |   }!
    | },
    | y: [{
    |  	b: int! = 3,
    |   z: [{
    | 		c: double 1_2 <= 5 
    |    } = 1],
    | } < 3]!
    |}
  """.stripMargin
).valueOr(e => "Failed to parse schema with: $e")

val instance = 
Foo(
  "foo",
  Quux(true, Baz(4.44)),
  List(
    Bar(3, Vector(Baz(3.44))),
    Bar(3, Vector(Baz(2.44)))
  )
)

val validator = new ShapelessValidator

import cats.data.ValidatedNel

val res: ValidatedNel[String, Unit] = validator.validate(vobj, instance)
```

Here's what the parsed AST looks like: 

```scala
 
import validen.ast._
 
VObj(
  Map(
    "x" -> VStr(Some("(foo|bar){1}.+".r), Some(Btw(Gt(10), Lte(20))), true),
    "d" -> VObj(
      Map(
        "a" -> VBool(false),
        "e" -> VObj(
          Map(
            "c" -> VStr(None, Some(Lt(10)), true)
          ),
          true
        )
      ),
      true
    ),
    "y" -> VArr(
      VObj(
        Map(
          "b" -> VInt(Some(Eq(3)), true),
          "z" -> VArr(
            VObj(
              Map(
                "c" -> VDbl(Some((1, 2)), Some(Lte(5)), false)
              ),
              false
            ),
            Some(Eq(1)),
            false
          )
        ),
        false
      ),
      Some(Eq(3)),
      true
    )
  ),
  true
)
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
I needed to express validation not programmatically (as for
example play-json might [do](https://www.playframework.com/documentation/2.5.x/ScalaJsonCombinators#Validation-with-Reads) it), but in terms of a generic
spec language that could be easily encoded in a backend
as a string or what have you.

The only solution I found that addressed this issue
was [play-json-schema-validator](https://github.com/eclipsesource/play-json-schema-validator),
which suited my requirements well, but it only worked with
[play-json](https://github.com/playframework/play-json)
and so I had to add a 5th json library dependency
to the service in order to use it (the 
service was an incorrigible mess to begin with).

Validen provides a tenable solution to the issue of
tight coupling to a particular data domain 
by using case classes which are de facto representations
of heterogeneous generic data structures in scala

### Moving forward

- The problem outlined above remains however, and requires 
a more domain specific solution. Namely one that handles json, but can 
still do so fairly generically. Case classes are fine 
but what if you just want to have a validation middleware
that validates all payloads to all endpoints generically
without first converting json to a case class? (which you
typically do only further downstream, when the handler for 
that specific endpoint is invoked). To mitigate this
and without writing a new json parser for the sake of performance,
I will be adding seperate modules containing generic validators 
for the json ASTs of all of the major Scala json libraries:

  * `validen-circe`
  * `validen-argonaut`
  * `validen-play-json`
  * `validen-spray-json`

- Look how nasty that example AST is ^. I'm sure I'll refine it as
 I come up with new ideas but what we really need is a DSL to 
 construct/manipulate the AST in a programmatically clean way
