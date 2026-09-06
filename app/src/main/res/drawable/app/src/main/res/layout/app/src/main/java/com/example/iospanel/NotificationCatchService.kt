package com.example.iospanel

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView

class NotificationCatchService : NotificationListenerService() {

    private var windowManager: WindowManager? = null
    private var floatingBanner: View? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null || sbn.isOngoing || sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        val packageManager = applicationContext.packageManager
        val appIcon = try {
            packageManager.getApplicationIcon(sbn.packageName)
        } catch (e: Exception) {
            null
        }

        handler.post {
            showIosBanner(title, text, appIcon)
        }
    }

    @SuppressLint("InflateParams")
    private fun showIosBanner(title: String, text: String, icon: android.graphics.drawable.Drawable?) {
        removeBanner()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 40
        }

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingBanner = inflater.inflate(R.layout.ios_notification_item, null)

        val titleView = floatingBanner?.findViewById<TextView>(R.id.notif_title)
        val msgView = floatingBanner?.findViewById<TextView>(R.id.notif_message)
        val iconView = floatingBanner?.findViewById<ImageView>(R.id.app_icon)

        titleView?.text = title
        msgView?.text = text
        if (icon != null) iconView?.setImageDrawable(icon)

        floatingBanner?.setOnClickListener {
            removeBanner()
        }

        windowManager?.addView(floatingBanner, params)

        floatingBanner?.translationY = -300f
        ObjectAnimator.ofFloat(floatingBanner, "translationY", 0f).apply {
            duration = 350
            start()
        }

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            removeBanner()
        }, 4500)
    }

    private fun removeBanner() {
        floatingBanner?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
            }
            floatingBanner = null
        }
    }

    override fun onDestroy() {
        removeBanner()
        super.onDestroy()
    }
}
