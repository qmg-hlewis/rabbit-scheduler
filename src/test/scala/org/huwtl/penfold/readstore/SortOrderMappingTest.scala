package org.huwtl.penfold.readstore

import org.specs2.mutable.Specification
import org.huwtl.penfold.domain.model.Status
import org.huwtl.penfold.readstore.SortOrder.Asc
import org.huwtl.penfold.readstore.SortOrder.Desc

class SortOrderMappingTest extends Specification {

  "map status to suitable sort order" in {
    val mapping = SortOrderMapping(Map(Status.Waiting -> Asc, Status.Ready -> Desc, Status.Started -> Asc, Status.Closed -> Desc))

    mapping.sortOrderFor(Status.Waiting) must beEqualTo(Asc)
    mapping.sortOrderFor(Status.Ready) must beEqualTo(Desc)
    mapping.sortOrderFor(Status.Started) must beEqualTo(Asc)
    mapping.sortOrderFor(Status.Closed) must beEqualTo(Desc)
  }
}