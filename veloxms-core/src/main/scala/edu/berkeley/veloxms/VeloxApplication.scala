package edu.berkeley.veloxms

import edu.berkeley.veloxms.resources._
import edu.berkeley.veloxms.storage._
import edu.berkeley.veloxms.misc.WriteModelsResource
import io.dropwizard.Configuration
// import net.nicktelford.dropwizard.scala._
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import tachyon.TachyonURI
import tachyon.r.sorted.ClientStore
import scala.util.{Try, Success, Failure}
import com.fasterxml.jackson.annotation.JsonProperty
import org.hibernate.validator.constraints.NotEmpty
// import com.typesafe.scalalogging._
import com.massrelevance.dropwizard.ScalaApplication
import com.massrelevance.dropwizard.bundles.ScalaBundle
import javax.validation.constraints.NotNull

import edu.berkeley.veloxms.util.Logging

class VeloxConfiguration extends Configuration {
    @NotEmpty val tachyonMaster: String = "NoTachyonMaster"
    @NotEmpty val itemStoreName: String = "item-model"
    @NotEmpty val userStoreName: String = "user-model"
    @NotEmpty val ratingsStoreName: String = "movie-ratings"
    @NotNull val numFactors: Integer = 50
    val sparkMaster: String = "NoSparkMaster"
    // sparkMaster: String
    // whether to do preprocessing of dataset for testing purposes
    // reloadTachyon: Boolean,
    // rawDataLoc: String
}


object VeloxApplication extends ScalaApplication[VeloxConfiguration] with Logging {

    override def getName = "velox model server"

    // TODO I think this is fucked - look at Spark's Logging.scala to fix
    // val logger = LoggerFactory.getLogger(classOf[VeloxApplication])

    override def initialize(bootstrap: Bootstrap[VeloxConfiguration]) {
        bootstrap.addBundle(new ScalaBundle)
        // init global state
    }

    override def run(conf: VeloxConfiguration, env: Environment) {

        val userModel = TachyonUtils.getStore(conf.tachyonMaster, conf.userStoreName) match {
            case Success(s) => s
            case Failure(f) => throw new RuntimeException(
                s"Couldn't initialize use model: ${f.getMessage}")
        }

        val itemModel = TachyonUtils.getStore(conf.tachyonMaster, conf.itemStoreName) match {
            case Success(s) => s
            case Failure(f) => throw new RuntimeException(
                s"Couldn't initialize item model: ${f.getMessage}")
        }

        val ratings = TachyonUtils.getStore(conf.tachyonMaster, conf.ratingsStoreName) match {
            case Success(s) => s
            case Failure(f) => throw new RuntimeException(
                s"Couldn't initialize use model: ${f.getMessage}")
        }
        logInfo("got tachyon stores")

        val modelStorage = 
            new TachyonStorage(userModel, itemModel, ratings, conf.numFactors)
            // new TachyonStorage[Array[Double]](userModel, itemModel, ratings, conf.numFactors)
        val averageUser = Array.fill[Double](conf.numFactors)(1.0)
        val matrixFactorizationModel =
            new MatrixFactorizationModel(conf.numFactors, modelStorage, averageUser, conf)

        val featureCache = new FeatureCache[Long](FeatureCache.tempBudget)

        env.jersey().register(new MatrixFactorizationPredictionResource(
            matrixFactorizationModel, featureCache))

        env.jersey().register(new MatrixFactorizationUpdateResource(
            matrixFactorizationModel, featureCache, conf.sparkMaster))

        env.jersey().register(new WriteModelsResource)
        // env.jersey().register(addRatings)
    }
}











