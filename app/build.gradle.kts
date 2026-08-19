plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.elderlycare.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.elderlycare.app"
        minSdk = 22
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }

        // 萤石开放平台 AppKey/AppSecret（通过 gradle.properties 注入）
        buildConfigField("String", "EZVIZ_APP_KEY", "\"${project.findProperty("EZVIZ_APP_KEY") ?: ""}\"")
        buildConfigField("String", "EZVIZ_APP_SECRET", "\"${project.findProperty("EZVIZ_APP_SECRET") ?: ""}\"")
        buildConfigField("String", "EZVIZ_BASE_URL", "\"https://open.ys7.com/\"")

        // 云通话(ERTC) 配置
        buildConfigField("String", "EZVIZ_RTC_APP_ID", "\"1aafc61ecfba4b48b4eccdbe7849e4e8\"")
        buildConfigField("String", "RTC_BACKEND_URL", "\"${project.findProperty("RTC_BACKEND_URL") ?: "http://10.0.2.2:8000/"}\"")

        // 萤石 SDK 原生库架构
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)

    // 萤石开放平台网络层
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Media (ExoPlayer)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.exoplayer.hls)

    // Image
    implementation(libs.coil.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // DataStore（家属账号/老人档案持久化）
    implementation(libs.datastore.preferences)

    // 萤石开放平台 SDK（云通话 ERTC + 直播/回放，已内置 com.ezviz.videotalk / com.ezviz.mediarecoder / libezwatch.so）
    implementation(libs.ezviz.sdk)
    // 萤石语音对讲 API（com.ezviz.sdk.videotalk.EzvizVoiceCall）。
    // videotalk AAR 里除了这个新包外，还捆绑了与 ezviz-sdk 完全重复的旧 videotalk/mediarecoder 类与
    // libezwatch.so，直接引入会触发 checkDebugDuplicateClasses / mergeDebugNativeLibs 失败；
    // 故此处仅提取其唯一独有的 classes.jar（com.ezviz.sdk.videotalk.*）作为本地 jar 引入。
    implementation(files("libs/ezviz-videotalk-voice-1.2.1.jar"))

    // Room（留言模块本地存储）
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
