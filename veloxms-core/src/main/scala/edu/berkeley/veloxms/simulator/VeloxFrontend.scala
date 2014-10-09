package edu.berkeley.veloxms.simulator

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/** All simulated information about a query to Velox, e.g. for a prediction for
  * a particular user.  Note that the simulation may not make Velox itself
  * privy to all of the information in this object. */
trait VeloxRequest {
  val requestTime: Duration
}


/** A response by Velox to a request. */
trait VeloxResponse { }


/** An observation given to Velox, e.g. the true score a user gives to an item.
  */
trait VeloxObservation {
  val observationTime: Duration
}


/** A wrapper around whatever frontend we use for Velox.  For example, it could
  * just be an instance of an in-process class, or it could be behind a REST
  * API.
  *
  * Note that this wrapper uses a static type for requests, responses, and
  * observations.  This means a new instance must be used for each application.
  * That might be a bad design decision.
  */
trait VeloxFrontend[O <: VeloxObservation, Q <: VeloxRequest, R <: VeloxResponse] {
  def handleRequest(req: Q): Future[R]
  def handleObservation(obs: O): Unit
}
