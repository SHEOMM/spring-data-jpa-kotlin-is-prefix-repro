package repro

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory
import org.springframework.transaction.annotation.Transactional
import repro.model.FieldAccessSample
import repro.model.PropertyAccessSample
import repro.repository.ExplicitQueryRepository
import repro.repository.FieldAccessRepository

@SpringBootTest(properties = ["spring.jpa.show-sql=false"])
@Transactional
class ControlCasesTest {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Test
    fun `field access supports the Kotlin property spelling`() {
        entityManager.persist(FieldAccessSample(id = 1, isActive = true))
        entityManager.persist(FieldAccessSample(id = 2, isActive = false))
        entityManager.flush()

        val repository = JpaRepositoryFactory(entityManager).getRepository(FieldAccessRepository::class.java)

        assertThat(repository.findByIsActiveTrue().map { it.id }).containsExactly(1)
        assertThat(entityManager.metamodel.entity(FieldAccessSample::class.java).attributes.map { it.name })
            .contains("isActive")
    }

    @Test
    fun `explicit JPQL can use the JPA attribute name`() {
        entityManager.persist(PropertyAccessSample(id = 1, isActive = true))
        entityManager.persist(PropertyAccessSample(id = 2, isActive = false))
        entityManager.flush()

        val repository = JpaRepositoryFactory(entityManager).getRepository(ExplicitQueryRepository::class.java)

        assertThat(repository.findActiveUsingJpql().map { it.id }).containsExactly(1)
    }
}
