/*
 * OAndBackupX: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization").version("1.7.20")
}

val vKotlin = "1.7.20"
val vComposeCompiler = "1.3.2"

val vCompose = "1.3.1"
val vRoom = "2.5.0-beta02"
val vNavigation = "2.5.3"
val vAccompanist = "0.28.0"
val vLibsu = "5.0.4"
//val vIconics = "5.3.4"

val vJunitJupiter = "5.9.1"
val vAndroidxTest = "1.4.0"
val vAndroidxTestExt = "1.1.4"

android {
    namespace = "com.machiav3lli.backup"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.machiav3lli.backup"
        minSdk = 26
        targetSdk = 32
        versionCode = 8208
        versionName = "8.2.5"
        buildConfigField("int", "MAJOR", "8")
        buildConfigField("int", "MINOR", "2")

        testApplicationId = "${applicationId}.tests"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments(
                    mapOf(
                        "room.schemaLocation" to "$projectDir/schemas",
                        "room.incremental" to "true"
                    )
                )
            }
        }

    }

    buildTypes {
        named("release") {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            //versionNameSuffix = "-alpha01"
            isMinifyEnabled = true
        }
        named("debug") {
            applicationIdSuffix = ".debug"
            //versionNameSuffix = "-alpha01"
            isMinifyEnabled = false
        }
        create("neo") {
            applicationIdSuffix = ".neo"
            //versionNameSuffix = "-alpha01"
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        dataBinding = true
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    composeOptions {
        kotlinCompilerExtensionVersion = vComposeCompiler
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = compileOptions.sourceCompatibility.toString()
            freeCompilerArgs = listOf("-Xjvm-default=all")
        }
    }
    lint {
        checkReleaseBuilds = false
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    packagingOptions {
        resources.excludes.add("META-INF/LICENSE.md")
        resources.excludes.add("META-INF/LICENSE-notice.md")
    }
}

dependencies {
    //implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$vKotlin")
    implementation(kotlin("stdlib", vKotlin))
    implementation(kotlin("reflect", vKotlin))

    // Libs
    implementation("androidx.room:room-runtime:$vRoom")
    implementation("androidx.room:room-ktx:$vRoom")
    kapt("androidx.room:room-compiler:$vRoom")
    implementation("androidx.work:work-runtime-ktx:2.8.0-rc01")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha04")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("org.apache.commons:commons-compress:1.22")
    implementation("commons-io:commons-io:2.11.0")      // attention, there is an old 2003 version, that looks like newer
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.github.topjohnwu.libsu:core:$vLibsu")
    implementation("com.github.topjohnwu.libsu:io:$vLibsu")
    //implementation("com.github.topjohnwu.libsu:busybox:$libsu")
    implementation("de.voize:semver4k:4.1.0")
    //implementation("com.github.tony19:named-regexp:0.2.6") // regex named groups

    // UI
    implementation("com.google.android.material:material:1.8.0-alpha03")
    implementation("androidx.preference:preference-ktx:1.2.0")

    // Compose
    implementation("androidx.compose.runtime:runtime:$vCompose")
    implementation("androidx.compose.ui:ui:$vCompose")
    implementation("androidx.compose.ui:ui-tooling:$vCompose")
    implementation("androidx.compose.foundation:foundation:$vCompose")
    implementation("androidx.compose.runtime:runtime-livedata:$vCompose")
    implementation("androidx.navigation:navigation-compose:$vNavigation")
    implementation("io.coil-kt:coil-compose:2.2.2")
    implementation("androidx.compose.material3:material3:1.0.1")
    implementation("com.google.accompanist:accompanist-flowlayout:$vAccompanist")
    implementation("com.google.accompanist:accompanist-systemuicontroller:$vAccompanist")
    implementation("com.google.accompanist:accompanist-navigation-animation:$vAccompanist")
    implementation("com.google.accompanist:accompanist-pager:$vAccompanist")

    // Testing
    androidTestImplementation("org.junit.jupiter:junit-jupiter:$vJunitJupiter")
    androidTestImplementation("androidx.test:runner:$vAndroidxTest")
    implementation("androidx.test:rules:$vAndroidxTest")
    implementation("androidx.test.ext:junit-ktx:$vAndroidxTestExt")

    // compose testing
    //androidTestImplementation("androidx.ui:ui-test:$vCompose")
    // Test rules and transitive dependencies:
    androidTestImplementation("androidx.compose.ui:ui-test:$vCompose")
    //androidTestImplementation("androidx.compose.ui:ui-test-junit4:$vCompose")
    // Needed for createComposeRule, but not createAndroidComposeRule:
    debugImplementation("androidx.compose.ui:ui-test-manifest:$vCompose")
}

// using a task as a preBuild dependency instead of a function that takes some time insures that it runs
task("detectAndroidLocals") {
    val langsList: MutableSet<String> = HashSet()

    // in /res are (almost) all languages that have a translated string is saved. this is safer and saves some time
    fileTree("src/main/res").visit {
        if (this.file.path.endsWith("strings.xml")
            && this.file.canonicalFile.readText().contains("<string")
        ) {
            var languageCode = this.file.parentFile.name.replace("values-", "")
            languageCode = if (languageCode == "values") "en" else languageCode
            langsList.add(languageCode)
        }
    }
    val langsListString = "{${langsList.joinToString(",") { "\"${it}\"" }}}"
    android.defaultConfig.buildConfigField("String[]", "DETECTED_LOCALES", langsListString)
}
tasks.preBuild.dependsOn("detectAndroidLocals")

// tells all test tasks to use Gradle's built-in JUnit 5 support
tasks.withType<Test> {
    useJUnit()
    //useTestNG()
    //useJUnitPlatform()
}
