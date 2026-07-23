package repro.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "PropertyAccessSample")
@Table(name = "property_access_sample")
class PropertyAccessSample(
    @get:Id
    var id: Long = 0,

    @get:Column(nullable = false)
    var isActive: Boolean = false,

    @get:Column
    var isVerified: Boolean? = null,
)

@Entity(name = "FieldAccessSample")
@Table(name = "field_access_sample")
class FieldAccessSample(
    @field:Id
    var id: Long = 0,

    @field:Column(nullable = false)
    var isActive: Boolean = false,
)
