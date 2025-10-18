plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.lanxin.im"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lanxin.im"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            // Development server (154.40.45.121)
            buildConfigField("String", "API_BASE_URL", "\"http://154.40.45.121:8080/api/v1/\"")
            buildConfigField("String", "WS_BASE_URL", "\"ws://154.40.45.121:8080/ws\"")
            buildConfigField("String", "MINIO_ENDPOINT", "\"http://154.40.45.121:9000\"")
            buildConfigField("String", "MINIO_BUCKET", "\"lanxin-im\"")
            buildConfigField("String", "MINIO_ACCESS_KEY", "\"minioadmin\"")
            buildConfigField("String", "MINIO_SECRET_KEY", "\"minioadmin\"")
        }
        release {
            isMinifyEnabled = true
            buildConfigField("String", "API_BASE_URL", "\"https://api.lanxin168.com/api/v1/\"")
            buildConfigField("String", "WS_BASE_URL", "\"wss://api.lanxin168.com/ws\"")
            buildConfigField("String", "MINIO_ENDPOINT", "\"https://files.lanxin168.com\"")
            buildConfigField("String", "MINIO_BUCKET", "\"lanxin-im\"")
            // TODO: Replace with secure credentials from environment variables or keystore
            buildConfigField("String", "MINIO_ACCESS_KEY", "\"REPLACE_WITH_PRODUCTION_KEY\"")
            buildConfigField("String", "MINIO_SECRET_KEY", "\"REPLACE_WITH_PRODUCTION_SECRET\"")
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // GridLayout support
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    
    // Jetpack Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    
    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // WebSocket (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // 腾讯云 TRTC SDK（音视频通话）- 必须保留
    implementation("com.tencent.liteav:LiteAVSDK_TRTC:latest.release")
    
    // MinIO S3客户端（自建对象存储，不使用腾讯云COS）
    implementation("io.minio:minio:8.5.7")
    
    // Glide 图片加载
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
    
    // DataStore (SharedPreferences替代)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // CardView
    implementation("androidx.cardview:cardview:1.0.0")
    
    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // EncryptedSharedPreferences (安全存储)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

