pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        id("org.springframework.boot") version providers.gradleProperty("springBootVersion").orElse("4.1.0").get()
        id("io.spring.dependency-management") version "1.1.7"
        kotlin("jvm") version providers.gradleProperty("kotlinVersion").orElse("2.3.21").get()
        kotlin("plugin.jpa") version providers.gradleProperty("kotlinVersion").orElse("2.3.21").get()
    }
}

rootProject.name = "spring-data-jpa-kotlin-is-prefix-repro"
