pluginManagement {
    repositories {
        google()
        // Google's mirror of Maven Central; avoids rate limits on shared CI and cloud runners
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Google's mirror of Maven Central; avoids rate limits on shared CI and cloud runners
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}
rootProject.name = "TinyCam"
include(":app")
