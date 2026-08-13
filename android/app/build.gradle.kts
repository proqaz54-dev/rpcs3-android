plugins {
    id("com.android.application")
}

android {
    namespace = "net.rpcs3.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.rpcs3.android"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1-alpha"

        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DCMAKE_BUILD_TYPE=Release",
                    "-DCMAKE_CXX_STANDARD=23",
                    "-DUSE_NATIVE_INSTRUCTIONS=OFF",
                    "-DUSE_PRECOMPILED_HEADERS=OFF",
                    "-DUSE_SYSTEM_FFMPEG=OFF"
                )
            }
        }
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity:1.9.3")
    implementation("com.google.android.material:material:1.12.0")
}