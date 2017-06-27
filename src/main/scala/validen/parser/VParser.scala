package validen.parser

import validen.ast._

import scala.util.parsing.combinator._
import shapeless.tag
import shapeless.tag._
import internal.common._

import scala.util.matching.Regex

/** Impl to parse the Json-like spec language */
private[parser] class VParser extends JavaTokenParsers {

  import VParser.combinatorutils._
  import corecombinators._

  def topLevelVObj: Parser[VObj] = "{" ~ repsep(field, ",") ~ "}" ^^
    { case _ ~ elems ~ _ => VObj(Map() ++ elems, true)}

  private[parser] object corecombinators {

    /** Nested objects */
    def obj: Parser[VObj] = "{" ~ repsep(field, ",") ~ "}(!{0,1})".r ^^
      { case _ ~ elems ~ typeStr => VObj(Map() ++ elems, isRequired(tag[ReqTag][String](typeStr)))}

    /** An individual field inside of a VObj (an element in the map) */
    def field: Parser[(String, VValue)] = "[a-z]+".r ~ ":" ~ value ^^
      { case keyStr ~ _ ~ v => (keyStr, v) }

    /** All of the datatypes the validator supports */
    def value: Parser[VValue] = str | double | int | bool | arr


    /** Represents a Seq of VValues */
    def arr: Parser[VArr] = "[" ~ value ~ "](!{0,1})".r ~ opt(matchBoundsInt) ^^
      { case _ ~ valueArr ~ reqStr ~ boundsOpt => VArr(valueArr, boundsOpt, isRequired(tag[ReqTag][String](reqStr))) }

    /** String validator (with presence, regex and bounds validation) */
    def str: Parser[VStr] = "string(!{0,1})".r ~ opt(regex) ~ opt(matchBoundsInt) ^^
      { case typeStr ~ rOpt ~ bOpt  => VStr(rOpt, bOpt, isRequired(tag[ReqTag][String](typeStr))) }

    /** Double validator (with presence, shape and bounds validation) */
    def double: Parser[VDbl] = "double(!{0,1})".r ~ opt(doubleShape) ~ opt(matchBoundsDouble) ^^
      { case typeStr ~ shOpt ~ bOpt => VDbl(shOpt, bOpt, isRequired(tag[ReqTag][String](typeStr))) }

    /** Int validator (with bounds validation) */
    def int: Parser[VInt] = "int(!{0,1})".r ~ opt(matchBoundsInt) ^^
      { case typeStr ~ boundsOpt => VInt(boundsOpt, isRequired(tag[ReqTag][String](typeStr))) }

    /** Boolean validator */
    def bool: Parser[VBool] = "bool(!{0,1})".r ^^
      { case typeStr => VBool(isRequired(tag[ReqTag][String](typeStr)))}

  }

  //TODO:: scalac freaks out when a put a lot of these in a utilcombinators object

  /** Regex format used in the spec lang */
  private[parser] def regex: Parser[Regex] =  dequotedStringLiteral ~ "r" ^^ {case rStr ~ _  => new Regex(rStr)}

  /** This library likes to parse string literals like say: "hello" into ""hello"" */
  private[parser] def dequotedStringLiteral = stringLiteral ^^ {case str => str.substring(1, str.length - 1)}

  /** Spec for how many places there should be to the right and left of the decimal point */
  private[parser] def doubleShape: Parser[VDbl.Shape] = "[1-9]{1}_[1-9]{1}".r ^^
    {
      case shapeStr =>
        val shapeArr = shapeStr.split("_")
        (shapeArr(0).toInt, shapeArr(1).toInt)
    }

  private[parser] val upperBoundEquality = "(<=|<)".r
  private[parser] val lowerBoundEquality = "(>=|>)".r

  /** Upper and lower bounds spec for a double */
  private[parser] def matchBoundsDouble : Parser[Bounds] = {
    "=" ~ floatingPointNumber ^^
      {
        case _ ~ i => Eq(i.toInt)
      } |
    upperBoundEquality ~ floatingPointNumber ~ lowerBoundEquality ~ floatingPointNumber ^^
      {
        case upperEq ~ upperS ~ lowerEq ~ lowerS =>
          createBetweenBound(
            lowerS.toDouble,
            tag[InequalityTag][String](lowerEq),
            upperS.toDouble,
            tag[InequalityTag][String](upperEq)
          )
      } |
    lowerBoundEquality ~ floatingPointNumber ~ upperBoundEquality ~ floatingPointNumber ^^
      {
        case lowerEq ~ lowerS ~ upperEq ~ upperS =>
          createBetweenBound(
            lowerS.toDouble,
            tag[InequalityTag][String](lowerEq),
            upperS.toDouble,
            tag[InequalityTag][String](upperEq)
          )
      } |
    upperBoundEquality ~ floatingPointNumber ^^
      {
        case upperEq ~ upperS => createUpperBound(tag[InequalityTag][String](upperEq), upperS.toDouble)
      } |
    lowerBoundEquality ~ floatingPointNumber ^^
      {
        case lowerEq ~ lowerS => createLowerBound(tag[InequalityTag][String](lowerEq), lowerS.toDouble)
      }
  }

  /** Upper and lower bounds spec for an int */
  private[parser] def matchBoundsInt : Parser[Bounds] = {
    "=" ~ wholeNumber ^^
      {
        case _ ~ i => Eq(i.toInt)
      } |
    upperBoundEquality ~ wholeNumber ~ lowerBoundEquality ~ wholeNumber ^^
      {
        case upperEq ~ upperS ~ lowerEq ~ lowerS =>
          createBetweenBound(
            lowerS.toInt,
            tag[InequalityTag][String](lowerEq),
            upperS.toInt,
            tag[InequalityTag][String](upperEq)
          )
      } |
    lowerBoundEquality ~ wholeNumber ~ upperBoundEquality ~ wholeNumber ^^
      {
        case lowerEq ~ lowerS ~ upperEq ~ upperS =>
          createBetweenBound(
            lowerS.toInt,
            tag[InequalityTag][String](lowerEq),
            upperS.toInt,
            tag[InequalityTag][String](upperEq)
          )
      } |
    upperBoundEquality ~ wholeNumber ^^
      {
        case upperEq ~ upperS => createUpperBound(tag[InequalityTag][String](upperEq), upperS.toInt)
      } |
    lowerBoundEquality ~ wholeNumber ^^
      {
        case lowerEq ~ lowerS => createLowerBound(tag[InequalityTag][String](lowerEq), lowerS.toInt)
      }
  }
}

object VParser {

  private val vp = new VParser

  def unsafeParse(schemaStr: String) = vp.parse(vp.topLevelVObj, schemaStr).get

  def parse(schemaStr: String) = Either.catchNonFatal(unsafeParse(schemaStr))

  private[parser] object combinatorutils {
    trait ReqTag
    type ReqStr = String @@ ReqTag

    trait InequalityTag
    type Inequality = String @@ InequalityTag

    def isRequired(reqStr: ReqStr) = reqStr.endsWith("!")

    def isInclusiveBound(ineq: Inequality) = ineq.contains("=")

    def createLowerBound[A: Numeric](ineq: Inequality, lower: A) = {
        if (isInclusiveBound(ineq)) {
          Gte(lower)
        } else {
          Gt(lower)
        }
      }

    def createUpperBound[A: Numeric](ineq: Inequality, upper: A) = {
      if (isInclusiveBound(ineq)) {
        Lte(upper)
      } else {
        Lt(upper)
      }
    }

    def createBetweenBound[A](
      lower: A,
      lowerEq: Inequality,
      upper: A,
      upperEq: Inequality
    )(implicit ev: Numeric[A]) = {
      if (ev.lteq(upper, lower)){
        throw new IllegalArgumentException(
          s"Upper bound: $upper cannot be less than or equal to lower bound: $lower"
        )
      }
      Btw(createLowerBound(lowerEq, lower), createUpperBound(upperEq, upper))
    }
  }
}
