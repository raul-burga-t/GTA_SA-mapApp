import java.util.Properties
import java.io.File // Importamos File para arreglar el error

// 1. Cargar el archivo local.properties usando settingsDir
val localProperties = Properties()
val localPropertiesFile = File(settingsDir, "local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
// 2. Extraer el token secreto
val mapboxSecretToken = localProperties.getProperty("MAPBOX_SECRET_TOKEN") ?: ""

// 3. Un solo bloque de pluginManagement
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

// 4. Un solo bloque de dependencyResolutionManagement
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Repositorio de Mapbox
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox" // Siempre es "mapbox"
                password = mapboxSecretToken // Usamos la variable, NO tu token real
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "MinimapaGTA"
include(":app")