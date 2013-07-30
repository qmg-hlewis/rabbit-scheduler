package com.hlewis.eventfire.app.support.hal

import com.theoryinpractise.halbuilder.api.RepresentationFactory
import com.theoryinpractise.halbuilder.api.RepresentationFactory._
import com.hlewis.eventfire.domain.Job
import java.net.URI

class HalTriggeredJobFeedFormatter(representationFactory: RepresentationFactory, selfLink: URI, jobLink: URI, startedJobLink: URI) {

  def halFrom(job: Job) = {
    createHal(job).toString(HAL_JSON)
  }

  def halFrom(jobs: Iterable[Job]) = {
    val root = representationFactory.newRepresentation(selfLink)
    jobs.foreach(job => {
      root.withRepresentation("jobs", createHal(job))
    })
    root.toString(HAL_JSON)
  }

  private def createHal(job: Job) = {
    representationFactory.newRepresentation(s"${selfLink.toString}/${job.id}")
      .withProperty("jobId", job.id)
      .withLink("job", s"${jobLink.toString}/${job.id}")
      .withLink("start", s"${startedJobLink.toString}")
  }
}
