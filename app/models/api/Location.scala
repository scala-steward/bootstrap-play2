package models.api

import java.util.UUID

import common.utils.OptionUtils._
import org.joda.time.DateTime
import play.api.libs.json._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import play.api.libs.json.Reads

/**
 * Implementation independent aggregate root.
 */
case class Location(
  id: Option[Long],
  company: UUID,
  name: Option[String],
  settings: Option[JsValue],
  lat: Option[Double],
  lon: Option[Double],
  addressLine1: Option[String],
  addressLine2: Option[String],
  zip: Option[String],
  city: Option[String],
  country: Option[String],
  created: Option[DateTime],
  updated: Option[DateTime],
  distance: Option[Float]
) extends ApiBaseModel {
  val writes: Writes[Location] = implicitly
  override def toJson: JsValue = Json.toJson(this)(writes)
}

object Location {
  implicit val reads  = Json.reads[Location]
  implicit val writes = Json.writes[Location]

  def patch(newObject: Location, oldObject: Location): Location =
    newObject.copy(
      id = oldObject.id,
      company = oldObject.company,
      name = newObject.name.getOrElseOld(oldObject.name),
      lat = newObject.lat.getOrElseOld(oldObject.lat),
      lon = newObject.lon.getOrElseOld(oldObject.lon),
      addressLine1 = newObject.addressLine1.getOrElseOld(oldObject.addressLine1),
      addressLine2 = newObject.addressLine2.getOrElseOld(oldObject.addressLine2),
      zip = newObject.zip.getOrElseOld(oldObject.zip),
      city = newObject.city.getOrElseOld(oldObject.city),
      country = newObject.country.getOrElseOld(oldObject.country),
      settings = newObject.settings.getOrElseOld(oldObject.settings),
      created = oldObject.created,
      updated = Some(DateTime.now)
    )

  case class PagedLocationData(
    data: List[Location],
    prev: String,
    next: String,
    count: Long
  )
}
