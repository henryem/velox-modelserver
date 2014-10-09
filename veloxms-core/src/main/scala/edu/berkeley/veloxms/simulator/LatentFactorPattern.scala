import edu.berkeley.veloxms.UserID
import edu.berkeley.veloxms.simulator._
import org.jblas.DoubleMatrix
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.util.Random

case class RecommendationRequest(requestTime: Duration, userId: UserID) extends VeloxRequest

case class RecommendationResponse(itemId: Long) extends VeloxResponse

case class TrueScores(responseToScore: (RecommendationResponse => Double))

case class RecommendationObservation(
    observationTime: Duration,
    userId: UserID,
    responseToScore: (RecommendationResponse => Double))
    extends VeloxObservation

/**
 * The world works as follows:
 *   - Users arrive according to a homogeneous Poisson process with rate
 *     @arrivalRate.
 *   - When a user-arrival happens, the user's ID is drawn uniformly at random
 *     from among the possible IDs.
 *   - A user can be assigned to any item.
 *   - When a user is assigned an item, she gives it a score.  A user's score
 *     for an item is the inner product of that user's latent factor vector
 *     with the item's latent factor vector, plus 0-mean Gaussian noise with
 *     variance @noiseSigma^2.  If a user is assigned several times to an item,
 *     she draws IID from this distribution each time.
 *   - Velox assigns items to users.
 */
case class IidLatentFactorPattern(
    override val arrivalRate: Duration,
    observationDelay: Duration,
    /** The ith row is the factor for the ith user. */
    userFactor: DoubleMatrix,
    /** The jth column is the factor for the jth item. */
    itemFactor: DoubleMatrix,
    noiseSigma: Double)
    extends PoissonWorldPattern[RecommendationRequest, TrueScores, RecommendationObservation]
    with HomogeneousArrivals {
  @transient lazy val numUsers = userFactor.rows

  override def sample(time: Duration, rand: Random): (RecommendationRequest, TrueScores, RecommendationObservation) = {
    val user = rand.nextInt(numUsers)
    //FIXME: unsafe long to int conversion...
    //FIXME: Is it okay to dot a row vector with a column vector?
    val trueScores = (response: RecommendationResponse) => userFactor.getRow(user).dot(itemFactor.getColumn(response.itemId.toInt))
    val noise = rand.nextGaussian() * noiseSigma
    val observedScores = (response: RecommendationResponse) => trueScores(response) + noise
    (
      RecommendationRequest(time, user),
      TrueScores(trueScores),
      RecommendationObservation(time + observationDelay, user, observedScores))
  }
}

case class RecommendationSummarizer(
    numUsers: Long,
    numItems: Long)
    extends SimulationSummarizer[Regret, RecommendationRequest, RecommendationResponse, TrueScores] {
  override def summarize(data: Seq[(RecommendationRequest, RecommendationResponse, TrueScores)]): Regret = {
    // For each request, compute the score of the best item and subtract the
    // score for the item Velox picked.
    //FIXME: Not sure whether noise is observation noise or noise in the true
    // loss function.  If it is observation noise we should not include it in
    // the regret calculation.  Currently we do.
    data.map(t => t._3)
    0.0 //FIXME
  }
}