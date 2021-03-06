package org.huwtl.penfold.domain.event

import org.huwtl.penfold.domain.model._
import org.joda.time.DateTime
import org.huwtl.penfold.domain.model.AggregateId
import org.huwtl.penfold.domain.model.Payload

trait TaskCreatedEvent extends TaskEvent {
  val aggregateId: AggregateId
  val aggregateVersion: AggregateVersion
  val created: DateTime
  val queue: QueueId
  val triggerDate: DateTime
  val payload: Payload
  val score: Long
}