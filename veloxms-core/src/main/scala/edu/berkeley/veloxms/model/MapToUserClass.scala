package edu.berkeley.veloxms.model

case class MapToUserClassInstance[O](map: Function[Int, O]) extends StaticInstance[Int, O] {
  def apply(i: Int) = map(i)
}

/** Maps generic integer labels to usable class labels. */
//FIXME: @map should probably be a bijection, e.g. twitter's Bijection class.
case class MapToUserClass[O](map: Function[Int, O]) extends StaticModel[Int, O]{
  override def makeInstance() = MapToUserClassInstance(map)
}
