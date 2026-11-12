import java.util.Properties
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
val signingFile = providers.environmentVariable("LUCENT_SIGNING_PROPERTIES").orNull?.let(::file)
val signing = signingFile?.takeIf { it.isFile }?.let { Properties().apply { it.inputStream().use { load(it) } } }
android {
    namespace = "dev.chengeeker.lucent.demo"
    compileSdk = 36
    defaultConfig { applicationId = "dev.chengeeker.lucent.demo"; minSdk = 24; targetSdk = 36; versionCode = 1; versionName = "0.1.0"; ndk { abiFilters += "arm64-v8a" } }
    signingConfigs { if (signing != null) create("release") { storeFile = signingFile!!.parentFile.resolve(signing.getProperty("storeFile")); storePassword = signing.getProperty("storePassword"); keyAlias = signing.getProperty("keyAlias"); keyPassword = signing.getProperty("keyPassword") } }
    buildTypes { release { signingConfig = signingConfigs.findByName("release"); isMinifyEnabled = true; isShrinkResources = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 } }
dependencies {
    implementation(project(":lucent-ui"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("com.google.android.material:material:1.14.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0"); implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2"); testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.compose.ui:ui-test-junit4"); testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

