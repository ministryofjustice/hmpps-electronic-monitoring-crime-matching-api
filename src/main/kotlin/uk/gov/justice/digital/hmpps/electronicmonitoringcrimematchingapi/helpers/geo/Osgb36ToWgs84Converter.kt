package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.geo

import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateReferenceSystem
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.Wgs84

class Osgb36ToWgs84Converter {
  private val crsFactory = CRSFactory()
  private val ctFactory = CoordinateTransformFactory()

  private val osgb36Bng: CoordinateReferenceSystem =
    crsFactory.createFromName("EPSG:27700")

  private val wgs84: CoordinateReferenceSystem =
    crsFactory.createFromName("EPSG:4326")

  private val transform = ctFactory.createTransform(osgb36Bng, wgs84)

  fun convert(easting: Double, northing: Double): Wgs84 {
    val src = ProjCoordinate(easting, northing)
    val dst = ProjCoordinate()
    transform.transform(src, dst)
    return Wgs84(longitude = dst.x, latitude = dst.y)
  }
}
