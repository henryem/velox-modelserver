package edu.berkeley.veloxms.model

import org.apache.spark.rdd.RDD

trait TrainingOutput[Input, Output] {
  val instance: Instance[Input, Output]
  val state: Any
}

case class StatefulOutput[Input, Output](override val instance: Instance[Input, Output], override val state: Any) extends TrainingOutput[Input, Output]

case class StatelessOutput[Input, Output](override val instance: Instance[Input, Output]) extends TrainingOutput[Input, Output]{
  override val state: Any = None
}

trait TrainingExample[Input, Output]

/** A typical TrainingExample for supervised training. */
case class SupervisedExample[Input, Output](in: Input, out: Output) extends TrainingExample[Input, Output]


trait DistributionStrategy

case object Broadcast extends DistributionStrategy

trait Partition[Input] extends DistributionStrategy {
  def hashInput(in: Input): Long
}


/** An abstract description of a model (not instantiated; has no parameters).
 *  Describes how to build instances.  Immutable.
 */
trait Model[Input, Output] {
  //FIXME: Should pass some sort of pointer to an old instance.  This will
  // depend on the interface Velox wants to expose.
  def loadInstance(): TrainingOutput[Input, Output]

  /** Produces a new instance. */
  def buildFresh(trainingSet: RDD[Any]): TrainingOutput[Input, Output]

  /** Produces a new instance as a batch update to @oldState. */
  def batchUpdate(newTrainingSet: RDD[Any], oldState: Any): TrainingOutput[Input, Output]

  /** Updates @instance and @state in place using a single example. */
  def doUpdate(example: Any, instance: Instance[Input, Output], state: Any): Unit

  val distributionStrategy: DistributionStrategy
}

/** A model that never needs to change in response to data. */
trait StaticModel[Input, Output] extends Model[Input, Output] {
  def makeInstance(): Instance[Input, Output]

  def loadInstance() = StatelessOutput(makeInstance())
  def buildFresh(trainingSet: RDD[Any]) = StatelessOutput(makeInstance())
  def batchUpdate(newTrainingSet: RDD[Any], oldState: Any) = StatelessOutput(makeInstance())
  def doUpdate(example: Any, instance: Instance[Input, Output], state: Any) { }
  override val distributionStrategy = Broadcast
}

/** Provides default implementations of methods for a Model decorator. */
trait Decorator[Input, Output]
    extends Model[Input, Output] {
  val decorated: Model[Input, Output]

  def loadInstance() = decorated.loadInstance()

  def buildFresh(trainingSet: RDD[Any]) = decorated.buildFresh(trainingSet)

  def batchUpdate(newTrainingSet: RDD[Any], oldState: Any) =
    decorated.batchUpdate(newTrainingSet, oldState)

  def doUpdate(example: Any, instance: Instance[Input, Output], state: Any): Unit = {
    decorated.doUpdate(example, instance, state)
  }

  override val distributionStrategy = decorated.distributionStrategy
}

case class CompositeInstanceState(firstState: Any, andThenState: Any)

object CompositeTrainingOutput {
  def apply[Input, Intermediate, Output](
      firstOutput: TrainingOutput[Input, Intermediate],
      andThenOutput: TrainingOutput[Intermediate, Output]):
      TrainingOutput[Input, Output] = {
    TrainingOutput(ComposedInstance(firstOutput.instance, andThenOutput.instance), (firstOutput.state, andThenOutput.state))
  }
}

object ComposedModel {
//  def apply[Input, Intermediate, Output](first: Model[Input, Intermediate], andThen: Model[Intermediate, Output]) = {
//    ComposedModel(first, andThen, )
//  }
}

/** Composes one model with another.  Composition of models is complicated, and
  * this does the simplest possible thing in all cases, relying on decorators
  * to provide more intelligent behavior when available in particular cases.
  */
case class ComposedModel[Input, Intermediate, Output](
    first: Model[Input, Intermediate],
    andThen: Model[Intermediate, Output])
    extends Model[Input, Output] {
  def loadInstance() = CompositeTrainingOutput(first.loadInstance(), andThen.loadInstance())

  //NOTE 0: This doesn't make much sense and will rarely work.  Need to map
  // andThen.applyInverse to the training output, or else do some other special
  // thing.  (An example of a "special thing": When we compose an ensemble with
  // a linear model to make an online-trained non-uniform mixture, each member
  // of the ensemble is trained offline on the original training set.)
  def buildFresh(trainingSet: RDD[Any]) = CompositeTrainingOutput(first.buildFresh(trainingSet), andThen.buildFresh(trainingSet))

  //Note 0 on buildFresh() applies here.
  def batchUpdate(newTrainingSet: RDD[Any], oldState: Any) = {
    oldState match {
      case (firstOldState, andThenOldState) => {
        CompositeTrainingOutput(first.batchUpdate(newTrainingSet, firstOldState), andThen.batchUpdate(newTrainingSet, andThenOldState))
      }
      case _ => throw new IllegalArgumentException() //FIXME
    }
  }

  //Note 0 on buildFresh() applies here.
  def doUpdate(example: Any, instance: Instance[Input, Output], state: Any) {
    (instance, state) match {
      case (composedInstance: ComposedInstance[Input, Intermediate, Output], composedState: (Any, Any)) => {
        val firstInstance = composedInstance.first
        val andThenInstance = composedInstance.andThen
        val firstState = composedState._1
        val andThenState = composedState._2
        first.doUpdate(example, firstInstance, firstState)
        andThen.doUpdate(example, andThenInstance, andThenState)
      }
    }
  }

  //FIXME: Weird and possibly unintuitive.
  override val distributionStrategy = andThen.distributionStrategy
}


// Issues encountered:
//   - ComposedModel makes it really tricky to put a compile-time type on
//     training data, trained models, and instance state.  For now I am
//     punting on full generics for all of these things.  We should examine
//     whether it's feasible to add them back in.