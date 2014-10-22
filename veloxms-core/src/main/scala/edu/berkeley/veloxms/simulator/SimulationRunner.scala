package edu.berkeley.veloxms.simulator

import scala.concurrent.duration.Duration

trait SimulationRunner {
  def simulate[U, Q <: VeloxRequest, R <: VeloxResponse, V, O <: VeloxObservation, S <: TimedState](
    pattern: WorldPattern[Q, V, O, S],
    duration: Duration,
    summarizer: SimulationSummarizer[U, Q, R, V]):
    U
}

case class SimulatorArgs[U, Q <: VeloxRequest, R <: VeloxResponse, V, O <: VeloxObservation, S <: TimedState](
  sim: SimulationRunner,
  pattern: WorldPattern[Q, V, O, S],
  duration: Duration,
  summarizer: SimulationSummarizer[U, Q, R, V])