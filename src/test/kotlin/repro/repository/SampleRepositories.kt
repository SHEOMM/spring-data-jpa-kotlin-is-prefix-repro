package repro.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import repro.model.FieldAccessSample
import repro.model.PropertyAccessSample

interface ActiveSpellingRepository : JpaRepository<PropertyAccessSample, Long> {
    fun findByActiveTrue(): List<PropertyAccessSample>
}

interface IsActiveSpellingRepository : JpaRepository<PropertyAccessSample, Long> {
    fun findByIsActiveTrue(): List<PropertyAccessSample>
}

interface IsVerifiedSpellingRepository : JpaRepository<PropertyAccessSample, Long> {
    fun findByIsVerifiedTrue(): List<PropertyAccessSample>
}

interface FieldAccessRepository : JpaRepository<FieldAccessSample, Long> {
    fun findByIsActiveTrue(): List<FieldAccessSample>
}

interface ExplicitQueryRepository : JpaRepository<PropertyAccessSample, Long> {
    @Query("select e from PropertyAccessSample e where e.active = true")
    fun findActiveUsingJpql(): List<PropertyAccessSample>
}
