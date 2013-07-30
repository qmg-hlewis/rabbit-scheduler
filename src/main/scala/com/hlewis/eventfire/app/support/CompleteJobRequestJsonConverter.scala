package com.hlewis.eventfire.app.support

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods._
import com.hlewis.eventfire.domain.CompleteJobRequest

class CompleteJobRequestJsonConverter {
  implicit val formats = DefaultFormats

  def from(json: String) = {
    parse(json).extract[CompleteJobRequest]
  }
}
