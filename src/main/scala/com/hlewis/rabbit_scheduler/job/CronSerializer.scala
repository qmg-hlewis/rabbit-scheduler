package com.hlewis.rabbit_scheduler.job

import net.liftweb.json._
import net.liftweb.json.TypeInfo
import cronish.Cron

class CronSerializer extends Serializer[Cron] {
  private val CronClass = classOf[Cron]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Cron] = {
    case (TypeInfo(CronClass, _), json) => {
      val cronStr = json.extract[String]
      val cronParts = cronStr.split(" ")

      Cron("0", cronParts(0), cronParts(1), cronParts(2), cronParts(3), cronParts(4), "*")
    }
  }

  def serialize(implicit format: Formats) = null
}