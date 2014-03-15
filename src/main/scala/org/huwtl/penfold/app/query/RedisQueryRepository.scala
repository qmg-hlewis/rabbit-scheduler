package org.huwtl.penfold.app.query

import com.redis.RedisClientPool
import org.huwtl.penfold.domain.model.Status
import org.joda.time.format.DateTimeFormat
import org.huwtl.penfold.app.support.json.ObjectSerializer
import org.joda.time.DateTime._
import org.huwtl.penfold.query._
import org.huwtl.penfold.domain.model.QueueName
import org.huwtl.penfold.domain.model.AggregateId
import org.huwtl.penfold.domain.model.Payload
import scala.Some
import org.huwtl.penfold.query.PageRequest
import org.huwtl.penfold.query.JobRecord
import org.huwtl.penfold.query.PageResult

class RedisQueryRepository(redisClientPool: RedisClientPool, indexes: Indexes, objectSerializer: ObjectSerializer,
                           keyFactory: RedisKeyFactory) extends QueryRepository {
  val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  lazy val retrieveJobScript = redisClientPool.withClient(_.scriptLoad(
    """
      | local jobKey = KEYS[1]
      |
      | if redis.call('exists', jobKey) == 0 then
      |   return {}
      | end
      |
      | local created = redis.call('hget', jobKey, 'created')
      | local queue = redis.call('hget', jobKey, 'queue')
      | local status = redis.call('hget', jobKey, 'status')
      | local trigger = redis.call('hget', jobKey, 'trigger')
      | local payload = redis.call('hget', jobKey, 'payload')
      |
      | return {created, queue, status, trigger, payload}
    """.stripMargin
  ))

  override def retrieveBy(queueName: QueueName, status: Status, pageRequest: PageRequest, filters: Filters) = {
    val retrievalKey = indexes.keyFor(filters, queueName, status) getOrElse keyFactory.queueKey(queueName, status)
    retrievePage(retrievalKey, pageRequest)
  }

  override def retrieveBy(filters: Filters, pageRequest: PageRequest) = {
    indexes.keyFor(filters) match {
      case Some(indexKey) => retrievePage(indexKey, pageRequest)
      case None => PageResult(0, List(), previousExists = false, nextExists = false)
    }
  }

  override def retrieveBy(aggregateId: AggregateId) = {
    val jobKeyName = keyFactory.jobKey(aggregateId)

    val jobAttributes = redisClientPool.withClient(_.evalMultiSHA[String](retrieveJobScript.get, List(jobKeyName), Nil).get)

    if (jobAttributes.isEmpty) {
      None
    }
    else {
      val created = jobAttributes(0).get
      val queueName = jobAttributes(1).get
      val status = jobAttributes(2).get
      val triggerDate = jobAttributes(3).get
      val payload = jobAttributes(4).get
      Some(JobRecord(aggregateId, dateFormatter.parseDateTime(created), QueueName(queueName), Status.from(status).get, dateFormatter.parseDateTime(triggerDate), objectSerializer.deserialize[Payload](payload)))
    }
  }

  override def retrieveWithPendingTrigger = {
    val pageSize = 50

    def nextPageOfJobsToTrigger(offset: Int) = {
      val statusKey = keyFactory.statusKey(Status.Waiting)
      val nextPageOfEarliestTriggeredJobs = redisClientPool.withClient(_.zrangebyscore(key = statusKey, max = now().getMillis, limit = Some(offset, pageSize)))
      nextPageOfEarliestTriggeredJobs.getOrElse(Nil).map {
        aggregateId => new JobRecordReference(AggregateId(aggregateId))
      }
    }

    def allPagesOfJobsToTrigger(offset: Int = 0): Stream[List[JobRecordReference]] = {
      val page = nextPageOfJobsToTrigger(offset)
      if (page.isEmpty) Stream.empty else page #:: allPagesOfJobsToTrigger(offset + pageSize)
    }

    val allJobsToTrigger = for {
      pageOfJobsToTrigger <- allPagesOfJobsToTrigger()
      jobToTrigger <- pageOfJobsToTrigger
    } yield jobToTrigger

    allJobsToTrigger
  }

  private def retrievePage(indexKey: String, pageRequest: PageRequest): PageResult = {
    val aggregateIdsWithOverflow = redisClientPool.withClient(_.zrange(indexKey, pageRequest.start, pageRequest.end).get)
    val aggregateIdsWithoutOverflow = aggregateIdsWithOverflow.take(pageRequest.pageSize)

    val previousPageExists = !pageRequest.firstPage
    val nextPageExists = aggregateIdsWithOverflow.size != aggregateIdsWithoutOverflow.size

    PageResult(
      pageRequest.pageNumber,
      aggregateIdsWithoutOverflow.map(id => retrieveBy(AggregateId(id)).get),
      previousPageExists,
      nextPageExists)
  }
}
