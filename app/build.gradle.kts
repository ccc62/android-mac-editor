import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "io.github.jqssun.maceditor"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        targetSdk = 37
        versionCode = 8
        versionName = "0.0.8"
        applicationId = "io.github.jqssun.maceditor"
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    signingConfigs {
        if (localProps.containsKey("storeFile")) {
            create("release") {
                storeFile = file(localProps["storeFile"] as String)
                storePassword = localProps["storePassword"] as String
                keyAlias = localProps["keyAlias"] as String
                keyPassword = localProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles("proguard-rules.pro")
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.libxposed.service)
    compileOnly(libs.libxposed.api)
}
