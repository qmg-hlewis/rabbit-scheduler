package org.huwtl.penfold.query

import org.specs2.mutable.Specification

class FiltersTest extends Specification {
  "ignore filters with empty values" in {
    Filters(Nil).all must beEqualTo(Nil)
    Filters(List(Filter("a", "1"), Filter("b", ""))).all must beEqualTo(List(Filter("a", "1")))
  }

  "retrieve filter by key" in {
    val filters = Filters(List(Filter("a", "1"), Filter("B", "2")))
    filters.get("a") must beEqualTo(Some(Filter("a", "1")))
    filters.get("A") must beEqualTo(Some(Filter("a", "1")))
    filters.get("b") must beEqualTo(Some(Filter("B", "2")))
    filters.get("c") must beNone
  }
}
