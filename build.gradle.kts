// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.ksp.gradle.plugin) apply false
    alias(libs.plugins.compose.compiler) apply false

    id("com.google.dagger.hilt.android") version "2.56.2" apply false
}