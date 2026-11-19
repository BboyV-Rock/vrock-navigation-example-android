import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.vrock.navigationexample"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.vrock.navigationexample"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        val googleApiKey = providers.fileContents(layout.projectDirectory.file("secrets.properties"))
            .asText
            .orElse("")
            .map { contents ->
                Properties().apply { contents.reader().use { load(it) } }
                    .getProperty("GOOGLE_API_KEY", "")
                    .trim()
            }
            .get()
        manifestPlaceholders["GOOGLE_API_KEY"] = googleApiKey
        val escapedGoogleApiKey = googleApiKey
            .replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
        buildConfigField("String", "GOOGLE_API_KEY", "\"$escapedGoogleApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.vrock.navigation.ui)
    implementation(libs.vrock.navigation.ui.view)
    implementation(libs.vrock.navigation.routing)
    implementation(libs.vrock.navigation.routing.google)
    implementation(libs.vrock.navigation.map.google)
    implementation(libs.vrock.navigation.location)
    implementation(libs.google.maps)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}