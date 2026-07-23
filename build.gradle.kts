plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.jpa")
}

group = "repro"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.register("printVersions") {
    description = "Prints the resolved versions relevant to this reproducer."
    group = "help"
    doLast {
        val relevantModules = setOf(
            "org.springframework.data:spring-data-commons",
            "org.springframework.data:spring-data-jpa",
            "org.hibernate.orm:hibernate-core",
            "org.jetbrains.kotlin:kotlin-reflect",
        )
        configurations.testRuntimeClasspath.get().resolvedConfiguration.resolvedArtifacts
            .map { "${it.moduleVersion.id.group}:${it.name}" to it.moduleVersion.id.version }
            .filter { it.first in relevantModules }
            .sortedBy { it.first }
            .forEach { (module, version) -> println("$module:$version") }
    }
}
