 plugins {
     id("com.android.application")
     id("org.jetbrains.kotlin.android")
     id("org.jetbrains.kotlin.plugin.compose")
 }

 android {
     namespace = "com.wallpaperextend"
     compileSdk = 35

     defaultConfig {
         applicationId = "com.wallpaperextend"
         minSdk = 26
         targetSdk = 35
         versionCode = 2
         versionName = "2.0.0"

         ndk {
             abiFilters += listOf("arm64-v8a", "armeabi-v7a")
         }
         externalNativeBuild {
             cmake {
                 arguments += "-DANDROID_STL=c++_shared"
             }
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
             isMinifyEnabled = true
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

     kotlinOptions {
         jvmTarget = "17"
     }

     buildFeatures {
         compose = true
         viewBinding = false
     }

     packaging {
         resources {
             excludes += "/META-INF/{AL2.0,LGPL2.1}"
         }
         jniLibs {
             useLegacyPackaging = false
         }
     }
 }

 dependencies {
     implementation("androidx.core:core-ktx:1.15.0")
     implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
     implementation("androidx.activity:activity-compose:1.9.3")
     implementation(platform("androidx.compose:compose-bom:2024.12.01"))
     implementation("androidx.compose.ui:ui")
     implementation("androidx.compose.ui:ui-graphics")
     implementation("androidx.compose.ui:ui-tooling-preview")
     implementation("androidx.compose.material3:material3")
     implementation("androidx.compose.material:material-icons-extended")

     implementation("top.yukonga.miuix.kmp:miuix:0.9.3")
     implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.3")
     implementation("top.yukonga.miuix.kmp:miuix-blur:0.9.3")
     implementation("io.github.kyant0:backdrop:2.0.0")

     implementation("androidx.exifinterface:exifinterface:1.4.0")
     implementation("io.coil-kt:coil-compose:2.7.0")

     debugImplementation("androidx.compose.ui:ui-tooling")
     debugImplementation("androidx.compose.ui:ui-test-manifest")
 }