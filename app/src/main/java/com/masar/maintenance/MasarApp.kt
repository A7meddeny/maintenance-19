package com.masar.maintenance

import android.app.Application
import com.masar.maintenance.data.Net
import com.masar.maintenance.data.Session
import com.masar.maintenance.ui.I18n

class MasarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Net.init(Session(this))
        I18n.init()
    }
}
