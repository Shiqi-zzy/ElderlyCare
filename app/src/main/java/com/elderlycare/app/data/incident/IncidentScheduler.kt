package com.elderlycare.app.data.incident

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 事件加急/升级周期调度器。
 *
 * 每 [TICK_INTERVAL_MS] 扫描一次等待中的事件，由 [IncidentRepository.tick] 按
 * 「15s×3 加急 → 升级 → 漏接处罚」规则推进（演示可用 IncidentConfig.demoIntervalMs 加速）。
 * 单机演示轨用进程内协程即可；云端由后端定时任务承担同等职责。
 */
class IncidentScheduler(
    private val repository: IncidentRepository,
    private val scope: CoroutineScope
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                runCatching { repository.tick() }
                    .onFailure { Log.e(TAG, "事件调度异常", it) }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    companion object {
        private const val TAG = "IncidentScheduler"
        private const val TICK_INTERVAL_MS = 5_000L
    }
}
