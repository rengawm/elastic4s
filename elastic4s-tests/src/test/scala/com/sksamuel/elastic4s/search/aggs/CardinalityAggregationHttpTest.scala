package com.sksamuel.elastic4s.search.aggs

import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl
import com.sksamuel.elastic4s.testkit.ClassloaderLocalNodeProvider
import org.scalatest.{FreeSpec, Matchers}

class CardinalityAggregationHttpTest extends FreeSpec with ClassloaderLocalNodeProvider with Matchers with ElasticDsl {

  http.execute {
    createIndex("cardagg") mappings {
      mapping("buildings") fields(
        textField("name").fielddata(true),
        intField("height").stored(true)
      )
    }
  }.await

  http.execute(
    bulk(
      indexInto("cardagg/buildings") fields("name" -> "Willis Tower", "height" -> 1244),
      indexInto("cardagg/buildings") fields("name" -> "Burj Kalifa", "height" -> 2456),
      indexInto("cardagg/buildings") fields("name" -> "Tower of London", "height" -> 169)
    ).refresh(RefreshPolicy.Immediate)
  ).await

  "cardinality agg" - {
    "should return the count of distinct values" in {

      val resp = http.execute {
        search("cardagg").matchAllQuery().aggs {
          cardinalityAgg("agg1", "name")
        }
      }.await

      resp.totalHits shouldBe 3

      val agg = resp.maxAgg("agg1")
      // should be 6 unique terms, the 'of' in tower of london will be filtered out by the analyzer
      agg.value shouldBe 6
    }
  }
}
