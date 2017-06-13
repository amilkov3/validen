package io.voklim.validen.parser

import io.voklim.test._
import io.voklim.validen.ast._
import io.voklim.test.ast._
import io.voklim.internal.common._

class VParserTest extends UnitPropertySpec {

  val vp = new VParser

  import vp.corecombinators._

  val strSpec = """str: string! "^(foo|bar){1}.+"r <= 20"""
  val dblSpec = """dbl: double! 2_2"""
  val intSpec = """int: int! < 3 >= 2"""
  val boolSpec = """bl: bool!"""

  property("should parse string field into VStr successfully") {
    vp.parse(str, strSpec)
  }

  property("should parse double field into VDouble successfully") {
    vp.parse(double, dblSpec)
  }

  property("should parse int field into VInt successfully") {
    vp.parse(int, intSpec)
  }

  property("should parse boolean field into VBool successfully") {
    vp.parse(bool, boolSpec)
  }

  property("should parse object of primitive fields into VObj successfully") {
    val m = vp.parse(
      obj,
      s"""
        |{
        | $strSpec,
        | $dblSpec,
        | $intSpec,
        | $boolSpec
        |}
      """.stripMargin
    ).get
    //TODO: Comparing these directly fails because Regex is not a case class
    //m.fields("str") shouldBe vStrAst
    val vStr = m.fields("str").asInstanceOf[VStr]
    vStr.len shouldBe vStrAst.len
    vStr.regex.get.patternEqualTo(vStrAst.regex.get) shouldBe true
    vStr.req shouldBe vStrAst.req
  }
}
