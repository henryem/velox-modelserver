
package edu.berkeley.veloxms

import scala.util.Success
import scala.util.Failure
import scala.util.Try
import edu.berkeley.veloxms.storage._

/**
 * Model interface
 * @tparam T The scala type of the item, deserialized from Array[Byte]
 * We defer deserialization to the model interface to encapsulate everything
 * the user must implement into a single class.
 * @tparam U The type of per-item data being stored in the
 * KV store
 */
trait Model[T, U] {

  /** The number of features in this model.
   * Used for pre-allocating arrays/matrices
   */
  val numFeatures: Int

  val modelStorage: ModelStorage[U]

  /** Average user weight vector.
   * Used for warmstart for new users
   */
  val averageUser: WeightVector

  /**
   * User provided implementation for the given model. Will be called
   * by Velox on feature cache miss.
   */
  def computeFeatures(data: T) : FeatureVector

  /** Deserialize object representation from raw bytes to
   * the type of expected
   */
  def deserializeInput(data: Array[Byte]) : T

  // TODO: probably want to elect a leader to initiate the Spark retraining
  // once we are running a Spark cluster
  def retrainInSpark(sparkMaster: String)

  /**
   * Velox implemented - fetch from local Tachyon partition
   */
  def getWeightVector(userId: Long) : WeightVector




}



