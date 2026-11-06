package com.cjsdudwls1.colorinversion

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** 빠른 설정 패널의 "색상 반전" 타일. */
class InversionTileService : TileService() {

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = render()
    }
    private var observing = false

    override fun onStartListening() {
        super.onStartListening()
        if (!observing) {
            contentResolver.registerContentObserver(Inversion.uri, false, observer)
            observing = true
        }
        render()
    }

    override fun onStopListening() {
        if (observing) {
            contentResolver.unregisterContentObserver(observer)
            observing = false
        }
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (!Inversion.hasPermission(this)) {
            openApp()
            return
        }
        Inversion.toggle(this)
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val permitted = Inversion.hasPermission(this)
        val state = Inversion.readState(this)
        tile.state = when {
            state == null -> Tile.STATE_UNAVAILABLE
            state -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                when {
                    !permitted -> R.string.tile_needs_setup
                    state == true -> R.string.state_on
                    else -> R.string.state_off
                }
            )
        }
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
