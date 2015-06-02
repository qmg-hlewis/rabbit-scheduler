package com.qmetric.penfold.app.store.jdbc

import scala.slick.driver.JdbcDriver.backend.Database
import Database.dynamicSession
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import Q.interpolation
import com.qmetric.penfold.domain.model.AggregateId
import com.qmetric.penfold.app.support.json.EventSerializer
import com.qmetric.penfold.domain.event.Event
import com.qmetric.penfold.domain.store.EventStore
import java.sql.{SQLIntegrityConstraintViolationException, Timestamp}
import com.qmetric.penfold.domain.exceptions.AggregateConflictException
import scala.util.{Failure, Success, Try}

class JdbcEventStore(database: Database, eventSerializer: EventSerializer) extends EventStore {
  implicit val getEventFromRow = GetResult(row => eventSerializer.deserialize(row.nextString()))

  private val connectionSuccess = true

  override def checkConnectivity = {
    Try(database.withDynSession(sql"""SELECT 1""".as[String].first)) match {
      case Success(_) => Left(connectionSuccess)
      case Failure(e: Exception) => Right(e)
      case Failure(e) => throw e
    }
  }

  override def add(event: Event) = {
    database.withDynSession {
      try {
        sqlu"""
        INSERT INTO events (type, aggregate_id, aggregate_version, aggregate_type, created, data) VALUES (
          ${event.getClass.getSimpleName},
          ${event.aggregateId.value},
          ${event.aggregateVersion.number},
          ${event.aggregateType.name},
          ${new Timestamp(event.created.getMillis).toString},
          ${eventSerializer.serialize(event)}
        )
        """.execute
        event
      } catch {
        case e: SQLIntegrityConstraintViolationException => throw new AggregateConflictException(s"aggregate conflict ${event.aggregateId}")
      }
    }
  }

  override def retrieveBy(aggregateId: AggregateId) = {
    database.withDynSession {
      sql"""
        SELECT data FROM events
          WHERE aggregate_id = ${aggregateId.value}
          ORDER BY aggregate_version
      """.as[Event].list
    }
  }
}