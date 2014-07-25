package com.qmetric.penfold.readstore

import com.qmetric.penfold.domain.model.Status
import com.qmetric.penfold.domain.model.QueueId
import com.qmetric.penfold.domain.model.AggregateId

trait ReadStore {
  def checkConnectivity: Either[Boolean, Exception]

  def retrieveBy(id: AggregateId): Option[TaskRecord]

  def retrieveBy(filters: Filters, pageRequest: PageRequest): PageResult

  def retrieveByQueue(queueId: QueueId, status: Status, pageRequest: PageRequest, sortOrder: SortOrder, filters: Filters = Filters.empty): PageResult

  def retrieveTasksToTrigger: Iterator[TaskRecordReference]

  def retrieveTasksToTimeout(timeoutAttributePath: String, status: Option[Status] = None): Iterator[TaskRecordReference]
}
