package com.sksamuel.elastic4s.search

import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl
import com.sksamuel.elastic4s.testkit.DualClientTests
import com.sksamuel.elastic4s.testkit.ResponseConverterImplicits._
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Try

class BoolQueryTest extends FlatSpec with Matchers with ElasticDsl with DualClientTests {

  override protected def beforeRunTests(): Unit = {

    Try {
      execute {
        deleteIndex("fonts")
      }.await
    }

    execute {
      createIndex("fonts")
    }.await

    execute {
      bulk(
        indexInto("fonts/family").fields("name" -> "helvetica", "style" -> "sans"),
        indexInto("fonts/family").fields("name" -> "helvetica modern", "style" -> "serif"),
        indexInto("fonts/family").fields("name" -> "arial", "style" -> "serif"),
        indexInto("fonts/family").fields("name" -> "verdana", "style" -> "serif"),
        indexInto("fonts/family").fields("name" -> "times new roman", "style" -> "serif"),
        indexInto("fonts/family").fields("name" -> "roman comic", "style" -> "comic"),
        indexInto("fonts/family").fields("name" -> "comic sans", "style" -> "comic")
      ).refresh(RefreshPolicy.Immediate)
    }.await
  }

  "bool query" should "support must and not" in {
    val resp = execute {
      search("fonts/family").query {
        boolQuery().must("helvetica").not("serif")
      }
    }.await

    resp.totalHits shouldBe 1
    resp.hits.hits.head.sourceField("style") shouldBe "sans"
  }

  it should "support multiple must queries" in {
    val resp = execute {
      search("fonts/family").query {
        boolQuery().must("times", "new")
      }
    }.await

    resp.totalHits shouldBe 1
    resp.hits.hits.head.sourceField("name") shouldBe "times new roman"
  }

  it should "support not" in {
    val resp = execute {
      search("fonts/family").query {
        boolQuery().not("sans")
      }
    }.await

    resp.totalHits shouldBe 5
    resp.hits.hits.map(_.sourceField("style")).toSet shouldBe Set("comic", "serif")
  }

  it should "support must" in {
    val resp = execute {
      search("fonts/family").query {
        boolQuery().must("roman")
      }
    }.await

    resp.totalHits shouldBe 2
    resp.hits.hits.map(_.sourceField("name")).toSet shouldBe Set("times new roman", "roman comic")
  }

  it should "support or using should" in {
    val resp = execute {
      search("fonts/family").query {
        boolQuery().should(
          matchPhraseQuery("name", "times new roman"),
          matchPhraseQuery("name", "comic sans")
        )
      }
    }.await

    resp.totalHits shouldBe 2
    resp.hits.hits.map(_.sourceField("name")).toSet shouldBe Set("times new roman", "comic sans")
  }
}
