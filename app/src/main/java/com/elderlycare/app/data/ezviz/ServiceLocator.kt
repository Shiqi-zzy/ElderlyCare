package com.elderlycare.app.data.ezviz

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.elderlycare.app.BuildConfig
import com.elderlycare.app.config.EzvizConfig
import com.elderlycare.app.data.binding.BindingDao
import com.elderlycare.app.data.binding.BindingDatabase
import com.elderlycare.app.data.binding.BindingRepository
import com.elderlycare.app.data.binding.SeedData
import com.elderlycare.app.data.local.ElderlyProfileStore
import com.elderlycare.app.data.local.FamilyUserStore
import com.elderlycare.app.data.local.SettingsStore
import com.elderlycare.app.data.local.UserStore
import com.elderlycare.app.data.message.AppDatabase
import com.elderlycare.app.data.message.MessageRepository
import com.elderlycare.app.data.reminder.EzvizReminderApi
import com.elderlycare.app.data.reminder.RemindPlanRepository
import com.elderlycare.app.network.ezviz.EZCloudBroadcastManager
import com.elderlycare.app.network.ezviz.EzvizVoiceApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
    /** 家属账号存储（family_data：家属用户 + 老人档案共用） */
    lateinit var userStore: FamilyUserStore
        private set
    lateinit var profileStore: ElderlyProfileStore
        private set

    // ===== 多端绑定关系 =====
    /** 社区/医院工作人员账号存储（独立 staff_data，与家属 userStore 分离） */
    lateinit var staffUserStore: UserStore
        private set
    /** 多端绑定关系数据库（organization / binding_request / user_elderly_binding / local_alert） */
    lateinit var bindingDatabase: BindingDatabase
        private set
    lateinit var bindingDao: BindingDao
        private set
    /** 多端绑定核心业务层（申请/审批/解除 + 权限过滤 + 设备链路） */
    lateinit var bindingRepository: BindingRepository
        private set

    /** 后台协程作用域（幂等 seed 等轻量初始化任务） */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ===== 留言模块 =====
    lateinit var sdkManager: EzvizSdkManager
        private set
    lateinit var broadcastManager: EZCloudBroadcastManager
        private set
    lateinit var messageRepository: MessageRepository
        private set

    // ===== 提醒计划（萤石 v3 设备本地闹铃 REST + Room）=====
    lateinit var reminderRepository: RemindPlanRepository
        private set

    // ===== 云通话（ERTC）= 自建信令后端 =====
    lateinit var rtcBackendApi: RtcBackendApi
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
        userStore = FamilyUserStore(appContext)
        profileStore = ElderlyProfileStore(appContext)

        // ===== 多端绑定关系 =====
        staffUserStore = UserStore(appContext)
        bindingDatabase = BindingDatabase.getInstance(appContext)
        bindingDao = bindingDatabase.bindingDao()
        bindingRepository = BindingRepository(staffUserStore, userStore, profileStore, bindingDao)
        // 幂等预置演示机构与工作人员账号（不阻塞主线程）
        appScope.launch { SeedData(staffUserStore, bindingDao).ensureSeeded() }

        // ===== 云通话（ERTC）= 自建信令后端 REST =====
        // 注：必须先于 MessageRepository 创建——文字留言走后端接口（云端 TTS + 萤石云广播）
        val rtcRetrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.RTC_BACKEND_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        rtcBackendApi = rtcRetrofit.create(RtcBackendApi::class.java)

        // ===== 留言模块 =====
        // 云广播 REST：独立 Retrofit（域名/返回结构均与老接口不同）
        // 注：文字留言走后端接口（云端 TTS + 萤石云广播）；录音留言双通道发送仍走 broadcastManager 云广播 REST
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
            ezvizRepository = repository,
            rtcBackendApi = rtcBackendApi
        )

        // ===== 提醒计划（萤石 v3 设备本地闹铃）=====
        // 独立 Retrofit：@Header 鉴权 + JSON body（项目首个 @Header/@HTTP/DELETE 用法）
        val reminderRetrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.EZVIZ_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val reminderApi = reminderRetrofit.create(EzvizReminderApi::class.java)
        reminderRepository = RemindPlanRepository(
            context = appContext,
            planDao = AppDatabase.getInstance(appContext).remindPlanDao(),
            messageDao = AppDatabase.getInstance(appContext).messageDao(),
            reminderApi = reminderApi,
            ezvizRepository = repository,
            rtcBackendApi = rtcBackendApi
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
