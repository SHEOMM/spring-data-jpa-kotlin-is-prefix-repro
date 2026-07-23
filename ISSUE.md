Title: Derived queries can't reference a Kotlin `is`-prefixed Boolean property on a property-access entity

I ran into a naming mismatch while using a Kotlin `Boolean` property whose name starts with `is`, on an entity that maps through property access (annotations on the getter). I couldn't find a derived-query spelling that works for it, and I wasn't sure whether that's expected or a gap, so I thought I'd write it up. I may well be missing something here.

## Versions

I saw this on two setups:

- Spring Boot 4.1.0, Spring Data 4.1.0, Hibernate 7.4.1.Final, Kotlin 2.3.21
- Spring Boot 4.0.7, Spring Data 4.0.6, Hibernate 7.2.19.Final, Kotlin 2.2.21

Both on Java 17, with H2 and `kotlin-reflect`.

## Reproducer

_(link to be added)_

```shell
./gradlew clean test printVersions
./gradlew clean test printVersions -PspringBootVersion=4.0.7 -PkotlinVersion=2.2.21
```

## What happens

The entity is a plain property-access mapping:

```kotlin
@Entity
class PropertyAccessSample(
    @get:Id var id: Long = 0,
    @get:Column(nullable = false) var isActive: Boolean = false,
    @get:Column var isVerified: Boolean? = null,
)
```

As far as I can tell, the layers end up disagreeing about what that property is called:

| Layer | `isActive` seen as | `isVerified` seen as |
| --- | --- | --- |
| Kotlin property / `KProperty.name` | `isActive` | `isVerified` |
| JVM getter | `isActive()` | `isVerified()` |
| Spring Data (`KotlinBeanInfoFactory`, `PropertyPath`) | `isActive` | `isVerified` |
| JPA property-access metamodel | `active` | `verified` |

Because of that, I couldn't get any of those derived-query names to work all the way through. Spring Data rejects `findByActiveTrue()` up front with a `PropertyReferenceException`, since it has no `active`. `findByIsActiveTrue()` parses fine, but the query it builds still points at `p.isActive`, and Hibernate then rejects that with `UnknownPathException` (a `BadJpqlGrammarException`) when the query runs. The nullable `findByIsVerifiedTrue()` fails in the same way, so it doesn't look specific to primitive `boolean`.

## What I expected

I expected the Kotlin spelling `findByIsActiveTrue()` to line up with the metamodel attribute `active`.

To be clear, I'm not asking to bring back the old `findByActiveTrue()` behavior, or to add an alias in Spring Data Commons. From what I understand, `KotlinBeanInfoFactory` exposes `isActive` on purpose, and that seems right for property parsing. The part I got stuck on is that the parsed name is then rendered into the JPA query without being matched back to the attribute that property access actually produced.

## Why not just use field access

Field access does fix it, and it's in the list below. But moving an existing entity from property to field access changes where JPA reads and writes state, so on an established codebase it isn't always a change I'd feel comfortable making just for this.

## Workarounds

These all work for me:

- Field access, either on the whole entity or on the one property (`@get:Transient` together with `@field:Access(FIELD)`).
- Renaming the property to `active`.
- A declared `@Query` that uses the attribute name `active`.
- `@get:JvmName("getIsActive")` / `@set:JvmName("setIsActive")`, so the getter follows the JavaBeans name. This works on Kotlin 1.9 and 2.2. On Kotlin 2.3.x it's currently rejected for entities processed by the JPA compiler plugin (KT-86985, which looks fixed in 2.4.20-Beta2).

## A possible direction

I don't know this code well enough to be confident about where the right place is, but one narrow option might be to resolve the parsed segment with `ManagedType.getAttribute(segment)` first, and only when that fails and the segment looks like `is` followed by an uppercase letter, fall back to the attribute whose Java member is exactly that no-argument `boolean`/`Boolean` getter and whose name is the decapitalized remainder (`isActive` becomes `active`), accepting it only if exactly one attribute matches. Since that keys off the accessor and the metamodel rather than anything Kotlin-specific, a Java type with the same `isActive()`/`active` shape would behave the same. If something along these lines seems reasonable, I'd be glad to try a PR.

## Prior issues

I went through the Commons and JPA trackers and didn't find an exact match. The closest ones I saw were about projection or accessor-name handling rather than derived-query paths: spring-data-commons#3127, spring-data-jpa#3771, spring-data-commons#3215, spring-data-commons#3249, and the original `KotlinBeanInfoFactory` change in spring-data-commons#1947. Sorry if I missed a duplicate.
