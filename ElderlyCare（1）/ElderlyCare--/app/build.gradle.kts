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

    // ===== 留言模块新增依赖 =====

    // 萤石 EZOpenSDK：核心模块（必须依赖）+ 音视频通话模块（语音对讲/双向呼叫）
    // 注意：ezviz-sdk / videotalk 官方 POM 存在发布缺陷，声明了空的
    // <artifactId>unspecified</artifactId> 依赖条目，Gradle 会报 "Could not find :unspecified:"，
    // 必须在此排除。
    implementation(libs.ezviz.sdk) {
        exclude(group = "", module = "unspecified")
    }
    implementation(libs.ezviz.videotalk) {
        exclude(group = "", module = "unspecified")
    }

    // Room 数据库（留言本地存储）
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
