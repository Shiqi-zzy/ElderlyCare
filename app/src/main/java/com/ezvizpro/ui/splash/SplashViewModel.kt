package com.ezvizpro.ui.splash

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 启动页 ViewModel（MVP 简化版）
 *
 * 登录逻辑移至 LoginViewModel：
 *   萤石 AppKey 登录 → 自建后端 sync → Portal 或 角色选择
 */
@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel()
