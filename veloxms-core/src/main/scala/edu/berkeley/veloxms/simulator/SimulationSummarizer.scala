package edu.berkeley.veloxms.simulator

trait SimulationSummarizer[S, Q <: VeloxRequest, R <: VeloxResponse, V] {
  def summarize(data: Seq[(Q, R, V)]): S
}
