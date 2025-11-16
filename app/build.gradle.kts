import java.io.FileInputStream
import java.util.Properties

/*
 * ═══════════════════════════════════════════════════════════════════════════
 * WICHTIG FÜR KI-ASSISTENTEN (Gemini, Claude, etc.):
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Diese build.gradle.kts hat SPEZIFISCHE Versionsanforderungen!
 *
 * BEACHTE ALLE KOMMENTARE bei den Versionen:
 * - "DO NOT CHANGE" = Version NIEMALS ändern
 * - "DO NOT UPGRADE" = Version NICHT erhöhen
 * - "DO NOT DOWNGRADE" = Version NICHT verringern
 * - "OK to upgrade" = Version darf aktualisiert werden
 *
 * ⚠️ Das Ignorieren dieser Kommentare führt zu Build-Fehlern! ⚠️
 *
 * minSdk=36 und compileSdk=36 sind ABSICHTLICH so gesetzt (Android 16)!
 * Diese Werte NICHT ändern ohne explizite Anweisung!
 * ═══════════════════════════════════════════════════════════════════════════
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id("jacoco")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
}

// Version-Konstanten
val materialVersion = "1.13.0"  // DO NOT DOWNGRADE !!!
val espressoVersion = "3.5.1"  // DO NOT EVEN THINK ABOUT TO CHANGE !!!
val hiltVersion = "2.57.1"  // DO NOT UPGRADE !!!
val truthVersion = "1.4.5"  // OK to upgrade
val lifecycleVersion = "2.9.4"  // OK to upgrade
val navigationVersion = "2.9.6"  // OK to upgrade
val coroutinesVersion = "1.10.2"  // OK to upgrade
val mockitoKotlinVersion = "5.3.1"  // DO NOT CHANGE !!!
val mockitoCoreVersion = "5.2.0"   // DO NOT CHANGE !!!
val kotlinTestVersion = "2.2.10"  // DO NOT CHANGE !!! (1.9.23 causes compiler errors)
val timberVersion = "5.0.1"  // DO NOT UPGRADE !!! (6.x breaking changes)
val fragmentVersion = "1.8.9"  // DO NOT UPGRADE !!! (keep fragment-ktx and fragment-testing in sync)
val junitVersion = "4.13.2"  // DO NOT UPGRADE !!! (JUnit 5 needs migration)
val coreTestingVersion = "2.2.0"  // DO NOT CHANGE !!!
val turbineVersion = "1.2.1"  // OK to upgrade
val androidxTestJunitVersion = "1.2.1"  // DO NOT CHANGE !!!
val jsonVersion = "20250517"
val androidxTestVersion = "1.6.1"
val androidxTestCoreVersion = "1.6.1"
//val robolectricVersion = "4.16"  // OK to upgrade

// Lädt die sensiblen Daten aus der keystore.properties-Datei
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

// Nur laden wenn Datei existiert (für CI)
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.github.reygnn.json_viewer"
    compileSdk = 36 // DO NOT CHANGE !!!

    defaultConfig {
        applicationId = "com.github.reygnn.json_viewer"
        minSdk = 36 // DO NOT CHANGE !!!
        targetSdk = 36 // DO NOT CHANGE !!!
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.github.reygnn.json_viewer.HiltTestRunner" // HiltTestRunner muss ggf. noch erstellt werden
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }

            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true // Behalten für BuildConfig.DEBUG
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.jvmArgs("-XX:+EnableDynamicAgentLoading")
            }
        }

        unitTests.all {
            it.configure<JacocoTaskExtension> {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }
}

dependencies {
// UI & Material  (MUST be loaded first or use resolutionStrategy below!)
    implementation("com.google.android.material:material:${materialVersion}")  // MUSS VOR androidx.appcompat:appcompat !!!

// Core Android
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.11.0") // Hinzugefügt für activity-Resultate & by viewModels in Activity
    //noinspection NewerVersionAvailable
    implementation("androidx.fragment:fragment-ktx:$fragmentVersion")

// Lifecycle & Navigation
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.navigation:navigation-fragment-ktx:$navigationVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navigationVersion")

// Data & Async
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")

// Utilities
    //noinspection NewerVersionAvailable
    implementation("com.jakewharton.timber:timber:$timberVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    //testImplementation("org.robolectric:robolectric:${robolectricVersion}")

// Hilt
    //noinspection NewerVersionAvailable
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    //noinspection NewerVersionAvailable
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")

// --- LOKALE UNIT-TESTS (laufen auf dem PC/JVM) ---
    //noinspection NewerVersionAvailable
    testImplementation("junit:junit:$junitVersion")
    //noinspection NewerVersionAvailable
    testImplementation("androidx.arch.core:core-testing:$coreTestingVersion")
    //noinspection NewerVersionAvailable
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinTestVersion")
    testImplementation("app.cash.turbine:turbine:$turbineVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("com.google.truth:truth:$truthVersion")
    testImplementation("org.json:json:$jsonVersion")

// Hilt für Unit-Tests
    //noinspection NewerVersionAvailable
    testImplementation("com.google.dagger:hilt-android-testing:$hiltVersion")
    //noinspection NewerVersionAvailable
    kaptTest("com.google.dagger:hilt-compiler:$hiltVersion")

// Mockito für Unit-Tests
    //noinspection NewerVersionAvailable
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
    //noinspection NewerVersionAvailable
    testImplementation("org.mockito:mockito-inline:$mockitoCoreVersion")


// --- INSTRUMENTIERTE TESTS (laufen auf Emulator/Gerät) ---
    //noinspection NewerVersionAvailable
    androidTestImplementation("androidx.test:runner:$androidxTestVersion")
    //noinspection NewerVersionAvailable
    androidTestImplementation("androidx.test:rules:$androidxTestVersion")
    //noinspection NewerVersionAvailable
    androidTestImplementation("androidx.test.ext:junit:$androidxTestJunitVersion")
    //noinspection NewerVersionAvailable
    androidTestImplementation("androidx.test:core-ktx:$androidxTestCoreVersion")
    androidTestImplementation("com.google.truth:truth:$truthVersion")

    //noinspection NewerVersionAvailable
    androidTestImplementation("androidx.arch.core:core-testing:$coreTestingVersion")
    //noinspection NewerVersionAvailable
    debugImplementation("androidx.fragment:fragment-testing:$fragmentVersion")
    //noinspection NewerVersionAvailable
    androidTestImplementation("androidx.test.espresso:espresso-contrib:$espressoVersion")
    //noinspection NewerVersionAvailable
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion")
    //noinspection NewerVersionAvailable
    androidTestImplementation("androidx.test.espresso:espresso-intents:$espressoVersion")
    //noinspection NewerVersionAvailable
    debugImplementation ("androidx.test.espresso:espresso-idling-resource:$espressoVersion")

// Hilt für instrumentierte Tests
    //noinspection NewerVersionAvailable
    androidTestImplementation("com.google.dagger:hilt-android-testing:$hiltVersion")
    //noinspection NewerVersionAvailable
    kaptAndroidTest("com.google.dagger:hilt-compiler:$hiltVersion")

// Mockito für instrumentierte Tests
    //noinspection NewerVersionAvailable
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
}

kapt {
    correctErrorTypes = true
}

configurations.all {
    resolutionStrategy {
        // DO NOT REMOVE !!!
        force("com.google.android.material:material:$materialVersion")
        force("androidx.test:runner:$androidxTestVersion")
        force("androidx.test:monitor:$androidxTestVersion")
    }
}

// JaCoCo-Konfiguration (von Kolibri übernommen)
tasks.register<JacocoReport>("jacocoTestReport") {
    // ... (restlicher Jacoco-Code von oben)
}