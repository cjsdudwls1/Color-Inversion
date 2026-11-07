package com.cjsdudwls1.colorinversion

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/** 위젯·고정 알림에서 누른 전환 요청을 처리합니다. 화면을 띄우지 않습니다. */
class ToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ToggleActivity.ACTION_TOGGLE) return
        val result = Inversion.toggle(context)
        if (result !is Inversion.Result.Ok) {
            Toast.makeText(context.applicationContext, Inversion.message(context, result), Toast.LENGTH_SHORT).show()
        }
        StatusSync.updateAll(context)
    }
}

/** 재부팅·앱 업데이트 뒤에도 고정 알림과 위젯 상태 감시를 되살립니다. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> StatusSync.updateAll(context)
        }
    }
}

/** 홈 화면 1x1 색상 반전 위젯. */
class InversionWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        StatusSync.updateWidgets(context, ids)
        InversionWatchJob.schedule(context)
    }

    override fun onEnabled(context: Context) = StatusSync.updateAll(context)

    override fun onDisabled(context: Context) = StatusSync.updateAll(context)
}
