package com.hlewis.eventfire.app

import javax.servlet.ServletContext
import org.scalatra.LifeCycle
import akka.actor.ActorSystem
import com.hlewis.eventfire.app.support._
import com.hlewis.eventfire.app.web._
import scala.language.postfixOps
import com.hlewis.eventfire.app.store.InMemoryJobStoreFactory
import com.theoryinpractise.halbuilder.DefaultRepresentationFactory
import java.net.URI
import com.hlewis.eventfire.app.support.hal.{HalTriggeredJobFeedFormatter, HalStartedJobFormatter, HalJobFormatter, HalCompletedJobFormatter}
import com.hlewis.eventfire.usecases._

class Main extends LifeCycle with InMemoryJobStoreFactory {

  private implicit val actorSystem = ActorSystem("actor-system")

  override def init(context: ServletContext) {
    val jobStore = initJobStore()

    //val exposeTriggeredJobs = actor(new ExposeTriggeredJobs())
    //actor(new RepeatExecution(repeatDelay = 10 seconds, exposeTriggeredJobs ! Refresh))

    val jsonJobConverter = new JobJsonConverter
    val jsonStartJobRequestConverter = new StartJobRequestJsonConverter
    val jsonCompleteJobRequestConverter = new CompleteJobRequestJsonConverter

    val baseUrl = new URI("http://localhost:8080")

    val jobLink = new URI(s"${baseUrl.toString}/jobs")

    val triggeredJobLink = new URI(s"${baseUrl.toString}/triggered-feed")

    val startedJobLink = new URI(s"${baseUrl.toString}/started-feed")

    val completedJobLink = new URI(s"${baseUrl.toString}/completed-feed")

    val jobFormatter = new HalJobFormatter(new DefaultRepresentationFactory, jobLink, triggeredJobLink)

    val triggeredJobFeedFormatter = new HalTriggeredJobFeedFormatter(new DefaultRepresentationFactory, triggeredJobLink, jobLink, startedJobLink)

    val startedJobFormatter = new HalStartedJobFormatter(new DefaultRepresentationFactory, startedJobLink, jobLink, completedJobLink)

    val completedJobFormatter = new HalCompletedJobFormatter(new DefaultRepresentationFactory, completedJobLink)


    val createJob = new CreateJob(jobStore)
    val retrieveExistingJob = new RetrieveJob(jobStore)

    val retrieveTriggeredJob = new RetrieveTriggeredJob(jobStore)
    val retrieveTriggeredJobs = new RetrieveTriggeredJobs(jobStore)
    val retrieveTriggeredJobsByType = new RetrieveTriggeredJobsByType(jobStore)

    val startJob = new StartJob(jobStore)
    val retrieveStartedJob = new RetrieveStartedJob(jobStore)

    val completeJob = new CompleteJob(jobStore)

    context mount(new JobsResource(retrieveExistingJob, createJob, jsonJobConverter, jobFormatter), "/jobs/*")
    context mount(new TriggeredJobFeedResource(retrieveTriggeredJob, retrieveTriggeredJobs, retrieveTriggeredJobsByType, jsonJobConverter, triggeredJobFeedFormatter), "/triggered-feed/*")
    context mount(new StartedJobFeedResource(startJob, retrieveStartedJob, jsonStartJobRequestConverter, startedJobFormatter), "/started-feed/*")
    context mount(new CompletedJobFeedResource(completeJob, jsonCompleteJobRequestConverter, completedJobFormatter), "/completed-feed/*")
  }

  override def destroy(context: ServletContext) {
    actorSystem.shutdown()
  }
}
