// Override the Kotlin version bundled with AGP 9's built-in Kotlin.
// AGP 9.3.x bundles KGP 2.2.10 by default, but KSP 2.3.x (which is required
// for AGP 9's new android.sourceSets DSL) targets Kotlin 2.3.x. Declaring KGP
// on the buildscript classpath upgrades the Kotlin compiler to match.
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
    id("com.android.application") version "9.3.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.11" apply false
    id("com.google.dagger.hilt.android") version "2.60.1" apply false
}