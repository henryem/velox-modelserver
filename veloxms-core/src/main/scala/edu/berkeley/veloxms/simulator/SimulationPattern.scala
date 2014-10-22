package edu.berkeley.veloxms.simulator

import org.apache.commons.math3.distribution.ExponentialDistribution

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

trait WorldState { }

case object EmptyState extends WorldState

trait TimedState extends WorldState {
  val time: Duration
}

case class TimeState(override val time: Duration) extends TimedState


/** When a WorldPattern simulates a single event (e.g. a single user
  * interaction), this is the output of that simulation.  It includes:
  *  - a request made to Velox for the event (e.g. a predicted rating for an
  *    item for the user)
  *  - a ground-truth value related to the request (e.g. the true rating of the
  *    item for the user)
  *  - an observation that will be made available to Velox (e.g. the true
  *    rating), potentially after some delay
  *  - the new state of the world after the event has happened
  */
case class SimulationResult[Q <: VeloxRequest, V, O <: VeloxObservation, S <: WorldState](
    request: Q,
    truth: V,
    observation: O,
    newState: S)


// Questions:
// Should WorldPattern return a batch of queries or just one?
//   A: Just one, for now.  If performance becomes an issue we can revisit
//   this.
// Do queries need time stamps?
//   A: Yes.  So the simplest possible state will be TimeState.
// How can the pattern get feedback from choices?
//   A: For now it will not.
// Who decides when observations happen?
//   A: VeloxObservation will include a time stamp.  The simulator is
//   responsible for making the observation happen at that time.
trait WorldPattern[Q <: VeloxRequest, V, O <: VeloxObservation, S <: WorldState] {
  def initialState(rand: Random): S
  def sampleNext(currentState: S, rand: Random): SimulationResult[Q, V, O, S]
}


/** A simple pattern in which the only state is the current time.  The rate
  * and distribution of events is allowed to depend on the time, but on nothing
  * else.
  *
  * Another way of saying this is that the full vector of events is a
  * sample from a marked nonhomogeneous Poisson process.  One nice implication of
  * this fact is that k independent copies of the same IndependentPoissonWorldPattern
  * generate event vectors from the same distribution as one copy with
  *   singlePattern.nextInterarrivalTime = (1/k) copiedPattern.nextInterarrivalTime
  * Therefore we could run this kind of simulator efficiently in parallel.
  *
  * This interface actually imposes weaker constraints on possible patterns
  * than might be apparent.  Many apparently-stateful patterns can be
  * implemented by sampling the rate functions for the arrival and marking
  * processes from a Markov process (which may itself involve state) as a
  * preprocessing step.
  *
  * The important kind of state we cannot handle efficiently (and which
  * implementors of this interface are barred from using) is state that
  * depends on the responses from Velox.  In active learning parlance, we
  * can only simulate an oblivious or stochastic adversary.  In practical
  * terms, we cannot simulate tasks like load balancing, where the performance
  * of an action taken by Velox depends on previous actions.
  */
trait PoissonWorldPattern[Q <: VeloxRequest, V, O <: VeloxObservation]
    extends WorldPattern[Q, V, O, TimedState] {
  override def initialState(rand: Random) = TimeState(0 millis)
  
  override def sampleNext(currentState: TimedState, rand: Random) = {
    val nextTime = nextInterarrivalTime(rand) + currentState.time
    val (request, truth, observation) = sample(nextTime, rand)
    SimulationResult(request, truth, observation, TimeState(nextTime))
  }

  def nextInterarrivalTime(rand: Random): Duration
  
  def sample(time: Duration, rand: Random): (Q, V, O)
}

/** Mixin for PoissonWorldPatterns with constant arrival rates. */
trait HomogeneousArrivals {
  val arrivalRate: Duration

  @transient lazy val interarrivalDistribution = new ExponentialDistribution(arrivalRate.toNanos)

  def nextInterarrivalTime(rand: Random): Duration = {
    val p = rand.nextDouble()
    interarrivalDistribution.inverseCumulativeProbability(p) nanos
  }
}
