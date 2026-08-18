package com.elderlycare.app.data.ezviz

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.elderlycare.app.BuildConfig
import com.elderlycare.app.config.EzvizConfig
import com.elderlycare.app.data.local.SettingsStore
import com.elderlycare.app.data.message.AppDatabase
import com.elderlycare.app.data.message.MessageRepository
import com.elderlycare.app.network.ezviz.EZCloudBroadcastManager
import com.elderlycare.app.network.ezviz.EzvizVoiceApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 轻量服务定位器（替代 Hilt）。
 * 在 Application.onCreate 中调用 [init] 完成初始化。
 */
object ServiceLocator {

    private const val TAG = "ServiceLocator"

    lateinit var repository: EzvizRepository
        private set
    lateinit var tokenManager: TokenManager
        private set
    lateinit var deviceBindingStore: DeviceBindingStore
        private set
    lateinit var settingsStore: SettingsStore
        private set

    // ===== 留言模块 =====
    lateinit var sdkManager: EzvizSdkManager
        private set
    lateinit var broadcastManager: EZCloudBroadcastManager
        private set
    lateinit var messageRepository: MessageRepository
        private set

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext

        val logging = HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message)
        }.apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.EZVIZ_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(EzvizApi::class.java)
        tokenManager = TokenManager(appContext)
        repository = EzvizRepository(api, tokenManager)
        deviceBindingStore = DeviceBindingStore(appContext)
        settingsStore = SettingsStore(appContext)

        // ===== 留言模块 =====
        // 云广播 REST：独立 Retrofit（域名/返回结构均与老接口不同）
        val broadcastRetrofit = Retrofit.Builder()
            .baseUrl(EzvizConfig.BROADCAST_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val voiceApi = broadcastRetrofit.create(EzvizVoiceApi::class.java)
        sdkManager = EzvizSdkManager()
        broadcastManager = EZCloudBroadcastManager(
            api = voiceApi,
            // 复用项目登录态：token 过期自动刷新
            tokenProvider = { repository.obtainValidToken() },
            talkCapabilityProvider = { repository.getDeviceSupportTalkRaw(it) }
        )
        messageRepository = MessageRepository(
            context = appContext,
            dao = AppDatabase.getInstance(appContext).messageDao(),
            sdkManager = sdkManager,
            broadcastManager = broadcastManager,
            ezvizRepository = repository
        )

        initialized = true
        Log.d(TAG, "ServiceLocator 初始化完成")
    }
}

/**
 * 已绑定设备信息持久化（家属端绑定后，首页据此进入直播/回放）
 */
class DeviceBindingStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("device_binding", Context.MODE_PRIVATE)

    data class BoundDevice(
        val deviceSerial: String,
        val validateCode: String,
        val deviceName: String
    )

    fun save(serial: String, validateCode: String, deviceName: String) {
        prefs.edit()
            .putString("device_serial", serial)
            .putString("validate_code", validateCode)
            .putString("device_name", deviceName)
            .apply()
    }

    fun load(): BoundDevice? {
        val serial = prefs.getString("device_serial", null) ?: return null
        if (serial.isBlank()) return null
        return BoundDevice(
            deviceSerial = serial,
            validateCode = prefs.getString("validate_code", "") ?: "",
            deviceName = prefs.getString("device_name", "RK3 设备") ?: "RK3 设备"
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
