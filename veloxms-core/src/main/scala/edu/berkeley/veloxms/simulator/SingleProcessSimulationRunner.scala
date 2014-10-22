package edu.berkeley.veloxms.simulator

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Random

/**
 * Simulates a running Velox system and makes user queries against it
 * according to a user-specified pattern.  All simulation happens in a single
 * process, so this simulator is useful for quickly exploring the statistical
 * properties of an algorithm at medium scale, but not useful for larger scales
 * or for testing the computational performance of an actual Velox system.
 */
object SingleProcessSimulationRunner extends SimulationRunner {
  def simulate[U, Q <: VeloxRequest, R <: VeloxResponse, V, O <: VeloxObservation, S <: TimedState](
      pattern: WorldPattern[Q, V, O, S],
      duration: Duration,
      summarizer: SimulationSummarizer[U, Q, R, V]):
      U = {
    summarizer.summarize(runEpochs(pattern, duration))
  }

  def runEpochs[Q <: VeloxRequest, R <: VeloxResponse, V, O <: VeloxObservation, S <: TimedState](
      pattern: WorldPattern[Q, V, O, S],
      duration: Duration):
      Seq[(Q, R, V)] = {
    val velox = new InProcessVelox[O,Q,R]()
    runEpochs(pattern, velox, duration)
  }

  def runEpochs[Q <: VeloxRequest, R <: VeloxResponse, V, O <: VeloxObservation, S <: TimedState](
      pattern: WorldPattern[Q, V, O, S],
      velox: VeloxFrontend[O, Q, R],
      duration: Duration):
      Seq[(Q, R, V)] = {
    val rand = new Random()
    val initialState = pattern.initialState(rand)
    var currentState = initialState
    val startTime = initialState.time
    val endTime = startTime + duration

    val observationQueue = new mutable.PriorityQueue[O]()(Ordering.by(_.observationTime))

    // This does what
    //   observationQueue.takeWhile(_.observationTime < t).foreach(o => velox.handleObservation(o))
    // ought to do.
    def dequeueObservations(t: Duration) {
      while (observationQueue.nonEmpty && observationQueue.head.observationTime < t) {
        val o = observationQueue.dequeue()
        velox.handleObservation(o)
      }
    }

    var simulationResults = Seq[(Q, R, V)]()
    while (currentState.time < endTime) {
      val nextEvent = pattern.sampleNext(currentState, rand)
      val nextEventTime = nextEvent.newState.time
      dequeueObservations(nextEventTime)
      observationQueue.enqueue(nextEvent.observation)
      val response = Await.result(velox.handleRequest(nextEvent.request), Duration.Inf)
      simulationResults :+= ((nextEvent.request, response, nextEvent.truth))
    }

    simulationResults
  }
}