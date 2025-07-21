package com.example.calllimiter

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CallLimiterApp : Application() {
    // Hilt will generate the necessary code for dependency injection.
    // You usually don't need to add anything else here for basic Hilt setup.
}