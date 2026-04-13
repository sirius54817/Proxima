import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.devtoolsKsp)
}

// Auto-incrementing versionCode — stored in version.properties, bumped on every build
fun getVersionCode(): Int {
    val propsFile = rootProject.file("version.properties")
    val props = Properties()
    if (propsFile.exists()) props.load(propsFile.inputStream())
    val code = (props.getProperty("VERSION_CODE")?.toIntOrNull() ?: 0) + 1
    props["VERSION_CODE"] = code.toString()
    props.store(propsFile.outputStream(), null)
    return code
}

android {
    namespace = "com.sirius.proxima"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sirius.proxima"
        minSdk = 26
        targetSdk = 36
        versionCode = getVersionCode()
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("proxima-release.jks")
            storePassword = "proximapass"
            keyAlias = "proxima"
            keyPassword = "proximapass"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.material.icons.extended)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation
    implementation(libs.navigation.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Google Sign-In
    implementation(libs.play.services.auth)

    // Google Drive API
    implementation(libs.google.api.services.drive)
    implementation(libs.google.api.client.android)
    implementation(libs.google.http.client.gson)

    // Gson
    implementation(libs.gson)

    // OkHttp for SIS scraping
    implementation(libs.okhttp)
    implementation(libs.okhttp.urlconnection)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Auto-install debug APK on connected device after every debug build
afterEvaluate {
    tasks.named("assembleDebug") {
        doLast {
            val apk = file("${layout.buildDirectory.get()}/outputs/apk/debug/app-debug.apk")
            if (!apk.exists()) {
                println("⚠️  APK not found: ${apk.absolutePath}")
                return@doLast
            }
            // Resolve adb path: local.properties → ANDROID_HOME → ANDROID_SDK_ROOT
            val localPropsFile = rootProject.file("local.properties")
            val sdkDir = if (localPropsFile.exists()) {
                localPropsFile.readLines()
                    .firstOrNull { it.startsWith("sdk.dir=") }
                    ?.substringAfter("sdk.dir=")
                    ?.trim()
            } else null
                ?: System.getenv("ANDROID_HOME")
                ?: System.getenv("ANDROID_SDK_ROOT")

            if (sdkDir == null) {
                println("⚠️  Cannot find Android SDK. Set sdk.dir in local.properties.")
                return@doLast
            }
            val adb = "$sdkDir/platform-tools/adb"
            println(" Installing ${apk.name} on connected device...")
            val proc = ProcessBuilder(adb, "install", "-r", "-d", apk.absolutePath)
                .redirectErrorStream(true)
                .start()
            val output = proc.inputStream.bufferedReader().readText().trim()
            val exitCode = proc.waitFor()
            if (output.isNotEmpty()) println(output)
            if (exitCode == 0) {
                println("✅ APK installed successfully.")
            } else {
                // Signature mismatch — uninstall then re-install
                println(" Signature mismatch — uninstalling old version and retrying...")
                ProcessBuilder(adb, "uninstall", "com.sirius.proxima")
                    .redirectErrorStream(true).start().waitFor()
                val proc2 = ProcessBuilder(adb, "install", "-r", "-d", apk.absolutePath)
                    .redirectErrorStream(true).start()
                val out2 = proc2.inputStream.bufferedReader().readText().trim()
                val exit2 = proc2.waitFor()
                if (out2.isNotEmpty()) println(out2)
                if (exit2 == 0) println("✅ APK installed successfully.")
                else println("⚠️  ADB install failed (exit $exit2). Is a device/emulator connected?")
            }
        }
    }
}
