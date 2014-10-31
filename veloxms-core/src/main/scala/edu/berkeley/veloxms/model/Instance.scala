package edu.berkeley.veloxms.model

/** An instantiated model.  For now it is really just a function, but at some
  * point it may be useful to distinguish it from Function.  Some models are
  * mutable and may expose an additional interface like observe() to their
  * managers.  It may also be important to have a notion of invertibility for
  * some instances.  Also, probably a cache context will need to be passed in
  * to the instance somewhere, either as an extra argument to apply() or as a
  * member.
  */
trait Instance[I, O] extends Function[I,O] {
  def apply(in: I): O
}

/** For now this is just a marker interface. */
trait StaticInstance[I, O] extends Instance[I, O]

case class ComposedInstance[I, N, O](first: Instance[I, N], andThen: Instance[N, O]) extends Instance[I, O] {
  def apply(in: I): O = andThen(first(in))
}

case class ConcatenatedInstance[I, O1, O2](firstOutput: Instance[I, O1], secondOutput: Instance[I, O2]) extends Instance[I, (O1, O2)] {
  def apply(in: I): (O1, O2) = (firstOutput(in), secondOutput(in))
}