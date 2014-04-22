package org.huwtl.penfold.command

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import org.huwtl.penfold.domain.model.{Task, AggregateId}
import org.huwtl.penfold.domain.store.DomainRepository

class TriggerTaskHandlerTest extends Specification with Mockito {
  val expectedAggregateId = AggregateId("a1")

  val domainRepository = mock[DomainRepository]

  val createdTask = mock[Task]
  val readyTask = mock[Task]

  val handler = new TriggerTaskHandler(domainRepository)

  "trigger waiting task" in {
    domainRepository.getById[Task](expectedAggregateId) returns createdTask
    createdTask.trigger returns readyTask

    handler.handle(new TriggerTask(expectedAggregateId))

    there was one(domainRepository).add(readyTask)
  }
}