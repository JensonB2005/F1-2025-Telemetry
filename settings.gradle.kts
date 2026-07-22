pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        // Plugin Portal mirror of Maven Central (reachable when repo.maven.apache.org is blocked).
        maven("https://plugins.gradle.org/m2/")
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Serves Maven Central artifacts via an allowed host; primary because
        // repo.maven.apache.org is blocked (403) on the build network.
        maven("https://plugins.gradle.org/m2/")
        mavenCentral()
    }
}

rootProject.name = "F1 Telemetry Viewer"
include(":app")
