package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "crime_batch_email_attachment")
data class CrimeBatchEmailAttachment(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @Schema(hidden = true)
  @ManyToOne
  @JoinColumn(name = "crime_batch_email_id")
  var crimeBatchEmail: CrimeBatchEmail,

//  @OneToOne(mappedBy = "crimeBatchEmailAttachment", cascade = [CascadeType.ALL], orphanRemoval = true)
//  var crimeBatch: CrimeBatch? = null,

  val fileName: String,
  val rowCount: Int,
)