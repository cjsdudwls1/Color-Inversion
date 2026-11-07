package com.cjsdudwls1.colorinversion

import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * 앱 밖에 보이는 표시(홈 화면 위젯, 상태바 고정 알림, 빠른 설정 타일)를
 * 실제 시스템 상태에 맞춰 한꺼번에 다시 그립니다.
 *
 * 색상 반전은 시스템 설정, 다른 앱, 이 앱 어디서든 바뀔 수 있으므로
 * [InversionWatchJob] 이 설정 값 변경을 감지할 때마다 이 함수를 부릅니다.
 */
object StatusSync {

    fun updateAll(context: Context) {
        val app = context.applicationContext
        updateWidgets(app)
        PinnedNotification.refresh(app)
        Inversion.notifyTile(app)
        if (hasWidgets(app) || PinnedNotification.isEnabled(app)) {
            InversionWatchJob.schedule(app)
        } else {
            InversionWatchJob.cancel(app)
        }
    }

    /** 위젯·알림 버튼이 누를 때 보낼 인텐트. 권한이 없으면 설정 안내가 있는 앱 화면을 엽니다. */
    fun tapIntent(context: Context, requestCode: Int): PendingIntent =
        if (Inversion.hasPermission(context)) {
            PendingIntent.getBroadcast(
                context, requestCode,
                Intent(context, ToggleReceiver::class.java).setAction(ToggleActivity.ACTION_TOGGLE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        } else {
            openAppIntent(context, requestCode)
        }

    fun openAppIntent(context: Context, requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context, requestCode,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun hasWidgets(context: Context): Boolean = widgetIds(context).isNotEmpty()

    private fun widgetIds(context: Context): IntArray = try {
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, InversionWidget::class.java))
    } catch (_: RuntimeException) {
        IntArray(0)
    }

    fun updateWidgets(context: Context, ids: IntArray = widgetIds(context)) {
        if (ids.isEmpty()) return
        val state = Inversion.readState(context)
        val permitted = Inversion.hasPermission(context)
        val views = RemoteViews(context.packageName, R.layout.widget_toggle).apply {
            val on = state == true
            setInt(R.id.widgetRoot, "setBackgroundResource",
                if (on) R.drawable.bg_widget_on else R.drawable.bg_widget_off)
            setInt(R.id.widgetIcon, "setColorFilter",
                context.getColor(if (on) R.color.on_accent else R.color.toggle_off_fg))
            setTextColor(R.id.widgetLabel,
                context.getColor(if (on) R.color.on_accent else R.color.text_secondary))
            setTextColor(R.id.widgetState,
                context.getColor(if (on) R.color.on_accent else R.color.toggle_off_fg))
            setTextViewText(R.id.widgetState, context.getString(
                when {
                    !permitted -> R.string.tile_needs_setup
                    state == null -> R.string.state_unknown
                    on -> R.string.state_on
                    else -> R.string.state_off
                }
            ))
            setContentDescription(R.id.widgetRoot, context.getString(
                if (on) R.string.toggle_desc_on else R.string.toggle_desc_off
            ))
            setOnClickPendingIntent(R.id.widgetRoot, tapIntent(context, REQ_WIDGET))
        }
        AppWidgetManager.getInstance(context).updateAppWidget(ids, views)
    }

    private const val REQ_WIDGET = 10
}

/** 설정 값이 바뀌면 깨어나 위젯·알림을 갱신하는 작업. 앱이 꺼져 있어도 시스템이 실행해 줍니다. */
class InversionWatchJob : android.app.job.JobService() {

    override fun onStartJob(params: android.app.job.JobParameters?): Boolean {
        // updateAll 이 다음 변경을 감시하도록 작업을 다시 예약합니다(콘텐츠 트리거 작업은 1회성).
        StatusSync.updateAll(this)
        return false
    }

    override fun onStopJob(params: android.app.job.JobParameters?): Boolean = false

    companion object {
        private const val JOB_ID = 4201

        fun schedule(context: Context) {
            val js = context.getSystemService(JobScheduler::class.java) ?: return
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, InversionWatchJob::class.java))
                .addTriggerContentUri(JobInfo.TriggerContentUri(Inversion.uri, 0))
                .setTriggerContentUpdateDelay(0)
                .setTriggerContentMaxDelay(500)
                .build()
            try {
                js.schedule(job)
            } catch (_: RuntimeException) {
            }
        }

        fun cancel(context: Context) {
            context.getSystemService(JobScheduler::class.java)?.cancel(JOB_ID)
        }
    }
}
