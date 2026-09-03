// Override the Kotlin version bundled with AGP 9's built-in Kotlin.
// AGP 9.2.x bundles KGP 2.3.10 by default, but we pin KGP 2.3.21 to match the
// Compose plugin and KSP 2.3.x. Declaring KGP on the buildscript classpath
// upgrades the Kotlin compiler to the desired version.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

plugins {
    // AGP 9.2.1 is the latest version supported by Android Studio 2026.1.1 (Quail 1).
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.11" apply false
    id("com.google.dagger.hilt.android") version "2.60.1" apply false
}