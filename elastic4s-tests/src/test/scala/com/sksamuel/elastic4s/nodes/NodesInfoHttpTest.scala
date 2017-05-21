package com.sksamuel.elastic4s.nodes

import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.{ElasticDsl, HttpClient}
import com.sksamuel.elastic4s.testkit.ClassloaderLocalNodeProvider
import org.scalatest.{Matchers, WordSpec}

class NodesInfoHttpTest extends WordSpec with Matchers with ClassloaderLocalNodeProvider with ElasticDsl {

  "node info request" should {
    "return node information" in {
      val nodes = http.execute {
        nodeInfo()
      }.await

      nodes.nodes.size should be(1)
      nodes.clusterName should be("classloader-node")
      nodes.nodes.values.toSeq.head.os.availableProcessors > 0 shouldBe true
    }
  }
}
