pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        val localSdkRepository = providers.gradleProperty("vrockSdkRepository")
            .orNull?.let { file(it) }
        exclusiveContent {
            forRepository {
                maven {
                    url = localSdkRepository?.toURI()
                        ?: uri("https://bboyv-rock.github.io/vrock-navigation-sdk-distribution/maven/")
                }
            }
            filter { includeGroup("com.vrock.navigation") }
        }
    }
}

rootProject.name = "VRock Navigation Example"
include(":app")
