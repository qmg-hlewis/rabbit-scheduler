package org.huwtl.penfold.domain.event

import org.huwtl.penfold.domain.model.{AggregateId, AggregateVersion}
import org.joda.time.DateTime

case class TaskArchived(aggregateId: AggregateId,
                        aggregateVersion: AggregateVersion,
                        created: DateTime) extends TaskEvent
