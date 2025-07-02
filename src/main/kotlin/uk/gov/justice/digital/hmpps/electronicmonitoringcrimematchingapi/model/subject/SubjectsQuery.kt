package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.CreationTimestamp
import java.time.ZonedDateTime

@Entity
@Table(
  name = "subject_query_cache",
  uniqueConstraints = [UniqueConstraint(columnNames = ["nomisId", "subjectName"])]
)
data class SubjectsQuery(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,

  val nomisId: String?,
  val subjectName: String?,

  @Column(nullable = false, unique = true)
  val queryExecutionId: String,

  @Column(nullable = false)
  val queryOwner: String,

  @CreationTimestamp
  val createdAt: ZonedDateTime = ZonedDateTime.now()
)