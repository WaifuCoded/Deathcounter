import org.spongepowered.gradle.plugin.config.PluginLoaders

plugins {
    val spongeGradleVersion = "2.2.0"

    `java-library`
    id("org.spongepowered.gradle.plugin") version spongeGradleVersion
}

group = "org.spongepowered"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

sponge {
    apiVersion("8.0.0")
    license("MIT")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }
    plugin("deathcounter") {
        displayName("Deathcounter")
        entrypoint("org.waifucoded.deathcounter.DeathCounter")
        description("Tracks player deaths per user")
        // ... other config ...
    }
}

val javaTarget = 8
java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
    if (JavaVersion.current() < JavaVersion.toVersion(javaTarget)) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaTarget))
    }
}

tasks.withType(JavaCompile::class).configureEach {
    options.apply {
        encoding = "utf-8" // Consistent source file encoding
    }
}
tasks.processResources {
    from("src/main/resources") {
        include("META-INF/sponge_plugins.json")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

// Make sure all tasks which produce archives (jar, sources jar, javadoc jar, etc) produce more consistent output
tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}