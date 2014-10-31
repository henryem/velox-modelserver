package edu.berkeley.veloxms.model

import org.apache.spark.rdd.RDD

case object BagOfWordsInstance extends StaticInstance[String, Map[String, Int]] {
  override def apply(document: String): Map[String, Int] = {
    null //FIXME: Actually implement bag of words algorithm.
  }
}

case object BagOfWords extends StaticModel[String, Map[String, Int]] {
  override def makeInstance() = BagOfWordsInstance
}
