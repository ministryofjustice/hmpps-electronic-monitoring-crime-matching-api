package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.geo

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.assertj.core.data.Offset
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.geo.Osgb36ToWgs84Converter

@ActiveProfiles("test")
class Osgb36ToWgs84ConverterTest {

  @ParameterizedTest(name = "it should convert - ({0}, {1}) -> ({2}, {3})")
  @MethodSource("locations")
  fun `it should convert from OSGB36 to WGS84`(easting: Double, northing: Double, longitude: Double, latitude: Double) {
    val converter = Osgb36ToWgs84Converter()
    val wgs84 = converter.convert(easting, northing)

    assertThat(wgs84.longitude).isEqualTo(longitude, tolerance)
    assertThat(wgs84.latitude).isEqualTo(latitude, tolerance)
  }

  companion object {
    private val tolerance: Offset<Double> = within(1e-5) // ~= 1 metre

    @JvmStatic
    fun locations() = listOf(
      Arguments.of(529381, 179534, -0.137433, 51.499946), // 102PF
      Arguments.of(537209, 180263, -0.024452, 51.504652), // 10SC
    )
  }
}
