package edu.berkeley.veloxms.simulator

import org.jblas.DoubleMatrix

import scala.concurrent.duration._
import scala.language.postfixOps

object LatentFactorSimulator {
  val simulationRunner = SingleProcessSimulationRunner
  val numUsers = 1000
  val numItems = 100
  val truePreferenceRank = 5
  val ptn = IidLatentFactorPattern(
    arrivalRate = 100 nanos,
    observationDelay = 1 millis,
    userFactor = generateRandomUserFactor(numUsers, truePreferenceRank), //FIXME
    itemFactor = generateRandomItemFactor(numItems, truePreferenceRank), //FIXME
    noiseSigma = 1e-3)
  val duration = 1 second
  val summarizer = RecommendationSummarizer(numUsers, numItems)

  def main(args: Seq[String]): Unit = {
    val result = simulationRunner.simulate(ptn, duration, summarizer)
    println("Simulation result: %s".format(result))
  }

  private def generateRandomUserFactor(numUsers: Int, numFactors: Int): DoubleMatrix = {
    DoubleMatrix.randn(numUsers, numFactors)
  }

  private def generateRandomItemFactor(numItems: Int, numFactors: Int): DoubleMatrix = {
    DoubleMatrix.randn(numFactors, numItems)
  }
}
