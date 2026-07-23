# Spring Data JPA Kotlin `is`-prefix property-access reproducer

This standalone project reproduces a derived-query naming gap for a Kotlin `Boolean` property whose name starts with `is` on a JPA property-access entity.

```kotlin
@Entity
class PropertyAccessSample(
    @get:Id
    var id: Long = 0,

    @get:Column(nullable = false)
    var isActive: Boolean = false,

    @get:Column
    var isVerified: Boolean? = null,
)
```

| Layer | Non-null Boolean | Nullable Boolean |
| --- | --- | --- |
| Kotlin property | `isActive` | `isVerified` |
| JVM getter | `isActive(): boolean` | `isVerified(): Boolean` |
| Spring Data property path | `isActive` | `isVerified` |
| JPA property-access metamodel | `active` | `verified` |

As a result, no derived-query spelling is usable:

| Repository method | Current result |
| --- | --- |
| `findByActiveTrue()` | Commons rejects `active` with `PropertyReferenceException` |
| `findByIsActiveTrue()` | Commons accepts `isActive`; the generated JPA path is then rejected as unknown |
| `findByIsVerifiedTrue()` | The same JPA path failure occurs for the boxed nullable `Boolean?` |

## Run

Requirements: Java 17. The Gradle wrapper is included.

Current line:

```shell
./gradlew clean test printVersions
```

Maintenance line:

```shell
./gradlew clean test printVersions \
  -PspringBootVersion=4.0.7 \
  -PkotlinVersion=2.2.21
```

Verified combinations:

| Spring Boot | Spring Data Commons/JPA | Hibernate ORM | Kotlin | Result |
| --- | --- | --- | --- | --- |
| 4.1.0 | 4.1.0 | 7.4.1.Final | 2.3.21 | all 6 tests pass |
| 4.0.7 | 4.0.6 | 7.2.19.Final | 2.2.21 | all 6 tests pass |

The failure assertions instantiate the repository interfaces independently and execute `findByIsActiveTrue()` so deferred JPA query validation cannot hide the issue. The successful controls verify field access and declared JPQL using `e.active`.

## Interpretation

The requested behavior is the Kotlin spelling `findByIsActiveTrue()`, not restoration of the old `findByActiveTrue()` behavior. `KotlinBeanInfoFactory` intentionally exposes the Kotlin declaration name. The integration gap is that Spring Data JPA renders that parsed name without reconciling it with the JPA metamodel attribute selected by property access.

Property access is relevant in legacy Kotlin codebases that consistently use `@get:` mapping annotations. Moving an existing entity to field access can change persistence behavior and is not necessarily a safe one-line workaround.

Accessor `@JvmName` is another valid workaround on Kotlin 1.9 and 2.2. Kotlin 2.3.x has a temporary JPA compiler-plugin regression, [KT-86985](https://youtrack.jetbrains.com/issue/KT-86985), which is marked fixed in 2.4.20-Beta2.

See [ISSUE.md](ISSUE.md) for the proposed issue text and [evidence/current-4.x.md](evidence/current-4.x.md) for the observed exception chains.
