package com.lorfocus.app.detection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.lorfocus.app.R

/**
 * F3.1 / F3.2 — draws the calm block or pause screen on top of the target feed, and keeps
 * the pipeline reliable via a low-priority foreground notification (§11 reliability).
 *
 * The overlay is plain Android Views (not Compose) to avoid ComposeView window-lifecycle
 * boilerplate. ponytail: upgrade to a themed ComposeView only if the overlay grows.
 */
class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlay: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var ticker: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (intent == null || !Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }

        val kind = intent.getStringExtra(EXTRA_KIND) ?: KIND_BLOCK
        val feed = intent.getStringExtra(EXTRA_FEED) ?: "this feed"
        val pause = intent.getIntExtra(EXTRA_PAUSE, 3)
        val goal = intent.getStringExtra(EXTRA_GOAL).orEmpty()
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE)

        removeOverlay()
        when (kind) {
            KIND_PAUSE -> showPause(feed, pause)
            KIND_SHORTS -> showShorts(pause)
            else -> showBlock(feed, goal, subtitle)
        }
        return START_NOT_STICKY
    }

    private fun addOverlay(layout: Int): View {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = LayoutInflater.from(this).inflate(layout, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.OPAQUE,
        )
        wm.addView(view, params)
        windowManager = wm
        overlay = view
        return view
    }

    private fun showBlock(feed: String, goal: String, subtitle: String?) {
        val v = addOverlay(R.layout.overlay_block)
        v.findViewById<TextView>(R.id.overlay_subtitle).text =
            subtitle ?: "You asked me to keep $feed quiet. Nothing to do here."
        v.findViewById<Button>(R.id.overlay_action).setOnClickListener { dismiss() }
    }

    /** Short-form pause: a calm countdown, no buttons — the detection service presses Back
     *  when it ends, returning the user to full-length videos. */
    private fun showShorts(seconds: Int) {
        val v = addOverlay(R.layout.overlay_shorts)
        val counter = v.findViewById<TextView>(R.id.overlay_countdown)
        var remaining = seconds.coerceAtLeast(1)
        counter.text = remaining.toString()
        ticker = object : Runnable {
            override fun run() {
                remaining -= 1
                if (remaining <= 0) { counter.text = "0"; dismiss() }
                else { counter.text = remaining.toString(); handler.postDelayed(this, 1000) }
            }
        }.also { handler.postDelayed(it, 1000) }
    }

    private fun showPause(feed: String, seconds: Int) {
        val v = addOverlay(R.layout.overlay_pause)
        val counter = v.findViewById<TextView>(R.id.overlay_countdown)
        v.findViewById<Button>(R.id.overlay_notnow).setOnClickListener { dismiss() }
        v.findViewById<TextView>(R.id.overlay_continue).setOnClickListener {
            // "Continue anyway" — grant the session by simply stepping aside.
            dismiss()
        }
        var remaining = seconds
        counter.text = remaining.toString()
        ticker = object : Runnable {
            override fun run() {
                remaining -= 1
                if (remaining <= 0) counter.text = "0" else {
                    counter.text = remaining.toString(); handler.postDelayed(this, 1000)
                }
            }
        }.also { handler.postDelayed(it, 1000) }
    }

    private fun dismiss() { removeOverlay(); stopSelf() }

    private fun removeOverlay() {
        ticker?.let { handler.removeCallbacks(it) }; ticker = null
        overlay?.let { runCatching { windowManager?.removeView(it) } }
        overlay = null
    }

    override fun onDestroy() { removeOverlay(); super.onDestroy() }

    private fun startAsForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Lor Focus", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Keeps quiet feeds quiet." }
            )
        }
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Lor Focus is watching over your feeds")
            .setSmallIcon(R.drawable.badger_roundel)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        // 2-arg startForeground picks up foregroundServiceType from the manifest.
        startForeground(NOTIF_ID, notif)
    }

    companion object {
        const val KIND_BLOCK = "block"
        const val KIND_PAUSE = "pause"
        const val KIND_SHORTS = "shorts"
        const val EXTRA_KIND = "kind"
        const val EXTRA_FEED = "feed"
        const val EXTRA_PAUSE = "pause"
        const val EXTRA_GOAL = "goal"
        const val EXTRA_SUBTITLE = "subtitle"
        private const val CHANNEL = "lorfocus_pipeline"
        private const val NOTIF_ID = 42
    }
}
