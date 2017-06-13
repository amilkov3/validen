package io.voklim.internal.common

import shapeless._
import shapeless.labelled.FieldType

trait ToNestedMap[L <: HList] { def apply(l: L): Map[String, Any]}

trait LowPriorityToNestedMap {
  implicit def hconsToMapNested1[K <: Symbol, V, T <: HList](implicit
    wit: Witness.Aux[K],
    tmrT: ToNestedMap[T]
  ): ToNestedMap[FieldType[K, V] :: T] = new ToNestedMap[FieldType[K, V] :: T] {
    def apply(l: FieldType[K, V] :: T): Map[String, Any] =
      tmrT(l.tail) + (wit.value.name -> l.head)
  }
}

object ToNestedMap extends LowPriorityToNestedMap {
  implicit val hnilToMapNested: ToNestedMap[HNil] = new ToNestedMap[HNil] {
    def apply(l: HNil): Map[String, Any] = Map.empty
  }

  /* TODO:
  The common parent between Vector and List is Seq which doesn't have a map
  which is why both implicit constructors present */
  implicit def hconsToMapNestedVector[K <: Symbol, V, R <: HList, T <: HList](implicit
    wit: Witness.Aux[K],
    gen: LabelledGeneric.Aux[V, R],
    tmrH: Lazy[ToNestedMap[R]],
    tmrT: Lazy[ToNestedMap[T]]
  ): ToNestedMap[FieldType[K, Vector[V]] :: T] = new ToNestedMap[FieldType[K, Vector[V]] :: T] {
    override def apply(l: FieldType[K, Vector[V]] :: T): Map[String, Any] = {
      tmrT.value(l.tail) + (wit.value.name -> l.head.map(x => tmrH.value(gen.to(x))))
      //tmrH(l.head.map(gen.to(_))) :: tmrT(l.tail)
    }
  }

  implicit def hconsToMapNestedList[K <: Symbol, V, R <: HList, T <: HList, H](implicit
    wit: Witness.Aux[K],
    gen: LabelledGeneric.Aux[V, R],
    tmrH: Lazy[ToNestedMap[R]],
    tmrT: Lazy[ToNestedMap[T]]
  ): ToNestedMap[FieldType[K, List[V]] :: T] = new ToNestedMap[FieldType[K, List[V]] :: T] {
    def apply(l: FieldType[K, List[V]] :: T): Map[String, Any] = {
      tmrT.value(l.tail) + (wit.value.name -> l.head.map(cc => tmrH.value(gen.to(cc))))
    }
  }

  implicit def hconsToMapNested0[K <: Symbol, V, R <: HList, T <: HList, H](implicit
    wit: Witness.Aux[K],
    gen: LabelledGeneric.Aux[V, R],
    tmrH: Lazy[ToNestedMap[R]],
    tmrT: Lazy[ToNestedMap[T]]
  ): ToNestedMap[FieldType[K, V] :: T] = new ToNestedMap[FieldType[K, V] :: T] {
    def apply(l: FieldType[K, V] :: T): Map[String, Any] = {
      tmrT.value(l.tail) + (wit.value.name -> tmrH.value(gen.to(l.head)))
    }
  }
}

final class RichShapelessOps[A](val a: A) extends AnyVal {
  def toMapNested[L <: HList](implicit
    gen: LabelledGeneric.Aux[A, L],
    tmr: ToNestedMap[L]
  ): Map[String, Any] = tmr(gen.to(a))
}

