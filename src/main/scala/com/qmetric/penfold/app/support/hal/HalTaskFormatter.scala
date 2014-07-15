package com.qmetric.penfold.app.support.hal

import com.theoryinpractise.halbuilder.api.RepresentationFactory._
import org.joda.time.format.DateTimeFormat
import java.net.URI
import com.theoryinpractise.halbuilder.DefaultRepresentationFactory
import com.qmetric.penfold.readstore._
import com.qmetric.penfold.domain.model.Status._
import com.theoryinpractise.halbuilder.api.Representation
import com.qmetric.penfold.app.support.JavaMapUtil
import com.qmetric.penfold.readstore.PageRequest
import com.qmetric.penfold.readstore.TaskRecord
import com.qmetric.penfold.domain.model.QueueBinding

class HalTaskFormatter(baseTaskLink: URI, baseQueueLink: URI) extends PaginatedRepresentationProvider {
  private val representationFactory = new DefaultRepresentationFactory

  private val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  def halRepresentationFrom(task: TaskRecord) = {
    val queueIdParam = task.queueBinding.id.value

    val representation: Representation = representationFactory.newRepresentation(s"${baseTaskLink.toString}/${task.id.value}")
      .withProperty("id", task.id.value)
      .withProperty("version", task.version.number)
      .withProperty("status", task.status.name)
      .withProperty("statusLastModified", dateFormatter.print(task.statusLastModified))
      .withProperty("triggerDate", dateFormatter.print(task.triggerDate))
      .withProperty("payload", JavaMapUtil.deepConvertToJavaMap(task.payload.content))
      .withProperty("queueBinding", JavaMapUtil.deepConvertToJavaMap(bindingToMap(task.queueBinding)))
      .withLink("queue", s"${baseQueueLink.toString}/$queueIdParam")

    if (task.assignee.isDefined) {
      representation.withProperty("assignee", task.assignee.get.username)
    }

    if (task.rescheduleType.isDefined) {
      representation.withProperty("rescheduleType", task.rescheduleType.get)
    }

    if (task.conclusionType.isDefined) {
      representation.withProperty("conclusionType", task.conclusionType.get)
    }

    if (task.previousStatus.isDefined) {
      representation.withProperty("previousStatus", JavaMapUtil.deepConvertToJavaMap(previousStatusToMap(task.previousStatus.get)))
    }

    if (task.status != Closed) {
      representation.withLink("updatePayload", s"${baseTaskLink.toString}/${task.id.value}/${task.version.number}/payload")
    }

    representation.withLink("reschedule", s"${baseQueueLink.toString}/$queueIdParam/${Waiting.name}")

    if (task.status != Waiting & task.status != Ready) {
      representation.withLink("requeue", s"${baseQueueLink.toString}/$queueIdParam/${Ready.name}")
    }

    if (task.status == Ready) {
      representation.withLink("start", s"${baseQueueLink.toString}/$queueIdParam/${Started.name}")
    }

    if (task.status != Closed) {
      representation.withLink("close", s"${baseQueueLink.toString}/$queueIdParam/${Closed.name}")
    }

    representation
  }

  def halFrom(task: TaskRecord) = {
    halRepresentationFrom(task).toString(HAL_JSON)
  }

  def halFrom(pageRequest: PageRequest, pageOfTasks: PageResult, filters: Filters = Filters.empty) = {
    val baseSelfLink = s"${baseTaskLink.toString}"

    val root = getRepresentation(pageRequest, pageOfTasks, filters, baseSelfLink, representationFactory)

    pageOfTasks.entries.foreach(task => {
      root.withRepresentation("tasks", halRepresentationFrom(task))
    })

    root.toString(HAL_JSON)
  }

  def bindingToMap(binding: QueueBinding) = {
    Map("id" -> binding.id.value)
  }

  def previousStatusToMap(previousStatus: PreviousStatus) = {
    Map("status" -> previousStatus.status.name, "statusLastModified" -> dateFormatter.print(previousStatus.statusLastModified))
  }
}