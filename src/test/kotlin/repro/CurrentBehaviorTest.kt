package repro

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.hibernate.query.sqm.PathElementException
import org.junit.jupiter.api.Test
import org.springframework.beans.BeanUtils
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.core.PropertyPath
import org.springframework.data.core.PropertyReferenceException
import org.springframework.data.jpa.repository.query.BadJpqlGrammarException
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory
import org.springframework.transaction.annotation.Transactional
import repro.model.PropertyAccessSample
import repro.repository.ActiveSpellingRepository
import repro.repository.IsActiveSpellingRepository
import repro.repository.IsVerifiedSpellingRepository

@SpringBootTest(properties = ["spring.jpa.show-sql=false"])
@Transactional
class CurrentBehaviorTest {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Test
    fun `Spring Data and the JPA metamodel expose different is-prefix names`() {
        val beanProperties = BeanUtils.getPropertyDescriptors(PropertyAccessSample::class.java)
            .map { it.name }
        val jpaAttributes = entityManager.metamodel.entity(PropertyAccessSample::class.java)
            .attributes
            .map { it.name }

        assertThat(beanProperties).contains("isActive", "isVerified").doesNotContain("active", "verified")
        assertThat(jpaAttributes).contains("active", "verified").doesNotContain("isActive", "isVerified")
        assertThat(PropertyAccessSample::class.java.declaredMethods.map { it.name })
            .contains("isActive", "setActive", "isVerified", "setVerified")
            .doesNotContain("getIsActive", "setIsActive", "getIsVerified", "setIsVerified")

        assertThat(PropertyPath.from("isActive", PropertyAccessSample::class.java).toDotPath())
            .isEqualTo("isActive")
        assertThat(PropertyPath.from("isVerified", PropertyAccessSample::class.java).toDotPath())
            .isEqualTo("isVerified")
        assertThatThrownBy { PropertyPath.from("active", PropertyAccessSample::class.java) }
            .isInstanceOf(PropertyReferenceException::class.java)
        assertThatThrownBy { PropertyPath.from("verified", PropertyAccessSample::class.java) }
            .isInstanceOf(PropertyReferenceException::class.java)
    }

    @Test
    fun `findByActiveTrue fails during Spring Data property resolution`() {
        val thrown = catchThrowable {
            JpaRepositoryFactory(entityManager).getRepository(ActiveSpellingRepository::class.java)
        }

        assertThat(thrown).isNotNull
        assertThat(rootCause(thrown)).isInstanceOf(PropertyReferenceException::class.java)
        println("findByActiveTrue exception chain:\n${exceptionChain(thrown)}")
    }

    @Test
    fun `findByIsActiveTrue fails during JPQL path validation on execution`() {
        entityManager.persist(PropertyAccessSample(id = 1, isActive = true))
        entityManager.flush()

        val thrown = catchThrowable {
            JpaRepositoryFactory(entityManager)
                .getRepository(IsActiveSpellingRepository::class.java)
                .findByIsActiveTrue()
        }

        assertThat(thrown).isInstanceOf(BadJpqlGrammarException::class.java)
        assertThat(rootCause(thrown)).isInstanceOf(PathElementException::class.java)
        assertThat(rootCause(thrown).message).contains("isActive")
        println("findByIsActiveTrue exception chain:\n${exceptionChain(thrown)}")
    }

    @Test
    fun `findByIsVerifiedTrue fails for a nullable Boolean during JPQL path validation`() {
        entityManager.persist(PropertyAccessSample(id = 1, isVerified = true))
        entityManager.flush()

        val thrown = catchThrowable {
            JpaRepositoryFactory(entityManager)
                .getRepository(IsVerifiedSpellingRepository::class.java)
                .findByIsVerifiedTrue()
        }

        assertThat(thrown).isInstanceOf(BadJpqlGrammarException::class.java)
        assertThat(rootCause(thrown)).isInstanceOf(PathElementException::class.java)
        assertThat(rootCause(thrown).message).contains("isVerified")
        println("findByIsVerifiedTrue exception chain:\n${exceptionChain(thrown)}")
    }

    private fun rootCause(throwable: Throwable): Throwable =
        generateSequence(throwable) { it.cause }.last()

    private fun exceptionChain(throwable: Throwable): String =
        generateSequence(throwable) { it.cause }
            .joinToString("\ncaused by: ") { "${it.javaClass.name}: ${it.message}" }
}
