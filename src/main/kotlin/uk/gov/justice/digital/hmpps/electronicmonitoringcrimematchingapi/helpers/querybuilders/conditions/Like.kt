package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Expression

class Like<T>(left: Expression<T>, right: Expression<T>) : ComparisonCondition<T>(left, right, "LIKE")
