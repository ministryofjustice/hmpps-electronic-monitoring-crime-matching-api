package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Expression

class Like(left: Expression<String>, right: Expression<String>) : ComparisonCondition<String>(left, right, "LIKE")
