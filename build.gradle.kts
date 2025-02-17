import org.spongepowered.gradle.plugin.config.PluginLoaders

plugins {
    val spongeGradleVersion = "2.2.0"

    `java-library`
    id("org.spongepowered.gradle.plugin") version spongeGradleVersion
    id("org.spongepowered.gradle.ore") version spongeGradleVersion // for Ore publishing
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
        release.set(javaTarget)
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

// Optional: configure publication to Ore
// Publish using the publishToOre task
// An API token is needed for this, by default read from the ORE_TOKEN environment variable
oreDeployment {
    // The default publication here is automatically configured by SpongeGradle
    // using the first-created plugin's ID as the project ID
    // A version body is optional, to provide additional information about the release
    /*
    defaultPublication {
        // Read the version body from the file whose path is provided to the changelog gradle property
        versionBody.set(providers.gradleProperty("changelog").map { file(it).readText(Charsets.UTF_8) }.orElse(""))
    }*/
}
