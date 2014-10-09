package edu.berkeley.veloxms.simulator

import scala.concurrent.Future

class InProcessVelox[O <: VeloxObservation, Q <: VeloxRequest, R <: VeloxResponse] extends VeloxFrontend[O, Q, R] {
  def handleRequest(req: Q): Future[R] = {
    null //FIXME
  }

  def handleObservation(o: O): Unit = {
    //FIXME
  }
}
