import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp.gradle.plugin)
    id("com.google.dagger.hilt.android")
    id("io.gitlab.arturbosch.detekt") version "1.23.3"
}

detekt {
    toolVersion = "1.23.3"
    config = files("${project.rootDir}/config/detekt/detekt.yml")
    buildUponDefaultConfig = true

    // TODO: ローカルでのみ自動修正を行い、CIでは行わないようにする
    autoCorrect = true

    // FIXME: 特定のファイルのみを解析対象にしているが、プロジェクト全体を解析するように変更する
    source.setFrom(files("src/main/java/com/example/working_timer/ui/main/MainViewModel.kt"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt> {
    jvmTarget = "1.8"
}

android {
    namespace = "com.example.working_timer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.working_timer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            enableUnitTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }

}

tasks.register<JacocoReport>("testCoverage") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    sourceDirectories.setFrom(files("$projectDir/src/main/java"))
    classDirectories.setFrom(
        fileTree("$buildDir/tmp/kotlin-classes/debug") {
            exclude(
                "**/R.class",
                "**/R$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*"
            )
        }
    )
    executionData.setFrom(file("$buildDir/jacoco/testDebugUnitTest.exec"))
}

dependencies {
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    annotationProcessor(libs.room.compiler)
    ksp(libs.room.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.ui)
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.5")
    debugImplementation("androidx.compose.ui:ui-tooling:1.9.5")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    implementation("com.google.dagger:hilt-android:2.57.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    ksp("com.google.dagger:hilt-android-compiler:2.57.2")

    implementation("androidx.navigation:navigation-compose:2.9.6")

    implementation("androidx.lifecycle:lifecycle-service:2.6.2")

    implementation("androidx.datastore:datastore-preferences:1.2.0")

    testImplementation("io.mockk:mockk:1.14.6")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("app.cash.turbine:turbine:1.2.1")
}