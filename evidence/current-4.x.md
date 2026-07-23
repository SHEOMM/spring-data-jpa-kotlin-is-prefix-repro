# Current 4.x evidence

Verified on 2026-07-15:

| Spring Boot | Spring Data Commons/JPA | Hibernate ORM | Kotlin |
| --- | --- | --- | --- |
| 4.1.0 | 4.1.0 | 7.4.1.Final | 2.3.21 |
| 4.0.7 | 4.0.6 | 7.2.19.Final | 2.2.21 |

Both builds completed with 6 tests, 0 failures. The exception classes and chain shape below were identical on the two tested Spring Data lines.

Observed `findByActiveTrue()` chain:

```text
org.springframework.data.repository.query.QueryCreationException:
No property 'active' found for type 'PropertyAccessSample'
caused by: org.springframework.data.core.PropertyReferenceException:
No property 'active' found for type 'PropertyAccessSample'
```

Observed `findByIsActiveTrue()` chain after executing the repository method:

```text
org.springframework.data.jpa.repository.query.BadJpqlGrammarException:
org.hibernate.query.sqm.UnknownPathException:
Could not resolve attribute 'isActive' of 'repro.model.PropertyAccessSample'
[SELECT p FROM PropertyAccessSample p WHERE p.isActive = TRUE]
caused by: java.lang.IllegalArgumentException
caused by: org.hibernate.query.sqm.UnknownPathException
caused by: org.hibernate.query.sqm.PathElementException
```

The nullable `Boolean?` query produced the same chain with the generated path `p.isVerified` and root message `Could not resolve attribute 'isVerified'`.

The tests also assert the `isActive`/`active` and `isVerified`/`verified` name pairs, strict `BadJpqlGrammarException` and root `PathElementException` types, and successful field-access plus explicit-JPQL controls.
