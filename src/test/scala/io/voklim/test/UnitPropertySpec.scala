package io.voklim.test

import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.PropertyChecks

import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

abstract class UnitPropertySpec extends {}
    with PropSpec
    with PropertyChecks
    with Matchers
