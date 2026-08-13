package com.elderlycare.app

import android.app.Application
import com.elderlycare.app.data.ezviz.ServiceLocator

class ElderlyCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
