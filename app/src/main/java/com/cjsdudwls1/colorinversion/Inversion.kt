package com.cjsdudwls1.colorinversion

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.service.quicksettings.TileService

/**
 * 시스템 색상 반전 설정을 읽고 쓰는 유일한 지점.
 *
 * 안드로이드(삼성 One UI 포함)는 색상 반전 상태를
 * Settings.Secure "accessibility_display_inversion_enabled" (0/1) 에 저장하고,
 * 시스템이 이 값을 감시하다가 즉시 화면에 반영합니다.
 *  - 읽기: 권한 없이 가능
 *  - 쓰기: WRITE_SECURE_SETTINGS 권한 필요 (ADB로 1회 부여)
 */
object Inversion {

    const val KEY = "accessibility_display_inversion_enabled"
    const val PERMISSION = "android.permission.WRITE_SECURE_SETTINGS"

    val uri: Uri get() = Settings.Secure.getUriFor(KEY)

    sealed class Result {
        /** 요청한 상태가 되었음 (이미 그 상태였던 경우 포함). */
        data class Ok(val enabled: Boolean, val changed: Boolean) : Result()
        /** 권한이 없어 변경할 수 없음. */
        data object NoPermission : Result()
        /** 값을 기록했지만 시스템에 반영되지 않았거나 기기가 거부함. */
        data object Failed : Result()
    }

    /** ADB 권한 부여 명령. */
    fun grantCommand(context: Context): String =
        "adb shell pm grant ${context.packageName} $PERMISSION"

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    /**
     * 현재 시스템 상태. 기기가 값을 읽지 못하게 막는 경우 null.
     * 값이 한 번도 설정되지 않은 기기는 기본값 0(꺼짐)으로 간주합니다.
     */
    fun readState(context: Context): Boolean? = try {
        Settings.Secure.getInt(context.contentResolver, KEY, 0) == 1
    } catch (e: SecurityException) {
        null
    } catch (e: RuntimeException) {
        null
    }

    fun isEnabled(context: Context): Boolean = readState(context) == true

    /** 원하는 상태로 설정. 같은 상태를 다시 요청해도 안전합니다(아무것도 바꾸지 않음). */
    fun set(context: Context, enable: Boolean): Result {
        val current = readState(context)
        if (current == enable) return Result.Ok(enable, changed = false)
        if (!hasPermission(context)) return Result.NoPermission

        val written = try {
            Settings.Secure.putInt(context.contentResolver, KEY, if (enable) 1 else 0)
        } catch (e: SecurityException) {
            // 권한이 도중에 취소된 경우
            return Result.NoPermission
        } catch (e: RuntimeException) {
            false
        }
        val after = readState(context)
        return if (written && after == enable) {
            // 타일·위젯·고정 알림을 한꺼번에 새 상태로 맞춥니다.
            StatusSync.updateAll(context)
            Result.Ok(enable, changed = true)
        } else {
            Result.Failed
        }
    }

    fun toggle(context: Context): Result = set(context, !isEnabled(context))

    /** 빠른 설정 타일이 즉시 새 상태를 그리도록 요청. */
    fun notifyTile(context: Context) {
        try {
            TileService.requestListeningState(
                context, ComponentName(context, InversionTileService::class.java)
            )
        } catch (_: RuntimeException) {
            // 타일이 추가되어 있지 않은 경우 등 - 무시해도 무방
        }
    }

    /** 사용자 안내 메시지(토스트 등에 사용). */
    fun message(context: Context, result: Result): String = when (result) {
        is Result.Ok -> context.getString(
            when {
                result.enabled && result.changed -> R.string.msg_turned_on
                !result.enabled && result.changed -> R.string.msg_turned_off
                result.enabled -> R.string.msg_already_on
                else -> R.string.msg_already_off
            }
        )
        Result.NoPermission -> context.getString(R.string.msg_no_permission)
        Result.Failed -> context.getString(R.string.msg_failed)
    }

    /**
     * 시스템의 색상 반전 설정 화면을 엽니다.
     * 기기마다 지원하는 화면이 달라 가능한 경로를 순서대로 시도합니다.
     */
    fun openSystemInversionSettings(context: Context): Boolean = startFirst(
        context,
        Intent("android.settings.COLOR_INVERSION_SETTINGS"),
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
        Intent(Settings.ACTION_SETTINGS),
    )

    fun openDeveloperOptions(context: Context): Boolean = startFirst(
        context,
        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
        Intent(Settings.ACTION_DEVICE_INFO_SETTINGS),
    )

    fun openDeviceInfo(context: Context): Boolean = startFirst(
        context,
        Intent(Settings.ACTION_DEVICE_INFO_SETTINGS),
        Intent(Settings.ACTION_SETTINGS),
    )

    fun isDeveloperOptionsOn(context: Context): Boolean = globalFlag(context, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED)
    fun isUsbDebuggingOn(context: Context): Boolean = globalFlag(context, Settings.Global.ADB_ENABLED)

    private fun globalFlag(context: Context, key: String): Boolean = try {
        Settings.Global.getInt(context.contentResolver, key, 0) == 1
    } catch (_: RuntimeException) {
        false
    }

    private fun startFirst(context: Context, vararg intents: Intent): Boolean {
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        return false
    }
}
