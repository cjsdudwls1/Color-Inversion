package com.cjsdudwls1.colorinversion

import android.app.Activity
import android.Manifest
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var stateText: TextView
    private lateinit var toggle: View
    private lateinit var toggleIcon: ImageView
    private lateinit var toggleText: TextView
    private lateinit var feedback: TextView
    private lateinit var setupCard: View
    private lateinit var steps: List<TextView>
    private lateinit var pinSwitch: Switch

    private val handler = Handler(Looper.getMainLooper())

    /** 이 앱이 직접 바꾼 직후의 변경 알림은 "외부 변경" 문구를 띄우지 않기 위한 표시. */
    private var expectingChange = false

    /** 직전 render 때의 권한 상태. 권한이 생기거나 사라지면 이전 안내 문구를 지우기 위해 씁니다. */
    private var lastPermitted: Boolean? = null

    /** 앱 밖(시스템 설정, 다른 앱, 타일 등)에서 값이 바뀌어도 즉시 화면을 맞춥니다. */
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            if (expectingChange) {
                expectingChange = false
            } else {
                feedback.setTextColor(getColor(R.color.text_secondary))
                feedback.text = getString(R.string.msg_changed_outside)
            }
            render()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBarInsets(findViewById(R.id.root))

        stateText = findViewById(R.id.stateText)
        toggle = findViewById(R.id.toggle)
        toggleIcon = findViewById(R.id.toggleIcon)
        toggleText = findViewById(R.id.toggleText)
        feedback = findViewById(R.id.feedback)
        setupCard = findViewById(R.id.setupCard)
        steps = listOf(R.id.step1, R.id.step2, R.id.step3, R.id.step4).map { findViewById(it) }

        findViewById<TextView>(R.id.command).text = Inversion.grantCommand(this)

        toggle.setOnClickListener { onToggleClicked() }
        findViewById<Button>(R.id.btnDeviceInfo).setOnClickListener { open(Inversion.openDeviceInfo(this)) }
        findViewById<Button>(R.id.btnDevOptions).setOnClickListener { open(Inversion.openDeveloperOptions(this)) }
        findViewById<Button>(R.id.btnSystemSettings).setOnClickListener { open(Inversion.openSystemInversionSettings(this)) }
        findViewById<Button>(R.id.btnCopy).setOnClickListener { copyCommand() }
        findViewById<Button>(R.id.btnRecheck).setOnClickListener { recheck() }
        findViewById<Button>(R.id.btnAddShortcut).setOnClickListener { addHomeShortcut() }
        findViewById<Button>(R.id.btnAddWidget).setOnClickListener { addHomeWidget() }

        pinSwitch = findViewById(R.id.switchPinNotif)
        pinSwitch.setOnClickListener { onPinSwitchClicked() }

        val addTile = findViewById<Button>(R.id.btnAddTile)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addTile.setOnClickListener { requestAddTile() }
        } else {
            // Android 12 이하는 앱이 타일 추가를 요청할 수 없어 수동 안내만 보여 줍니다.
            addTile.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver(Inversion.uri, false, observer)
    }

    override fun onResume() {
        super.onResume()
        // 앱을 다시 열거나 설정 화면에서 돌아올 때마다 실제 시스템 상태를 다시 읽습니다.
        render()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 빠른 설정 패널을 내렸다 올린 경우 등 onResume이 불리지 않는 상황 대비
        if (hasFocus) render()
    }

    override fun onStop() {
        contentResolver.unregisterContentObserver(observer)
        super.onStop()
    }

    private fun onToggleClicked() {
        toggle.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        val target = !Inversion.isEnabled(this)
        expectingChange = true
        val result = Inversion.set(this, target)
        if (result !is Inversion.Result.Ok || !result.changed) expectingChange = false

        feedback.text = Inversion.message(this, result)
        feedback.setTextColor(
            getColor(if (result is Inversion.Result.Ok) R.color.text_secondary else R.color.error)
        )
        if (result == Inversion.Result.NoPermission) {
            setupCard.visibility = View.VISIBLE
            findViewById<View>(R.id.root).post {
                (findViewById<View>(R.id.root) as android.widget.ScrollView)
                    .smoothScrollTo(0, setupCard.top)
            }
        }
        render()
        StatusSync.updateAll(this)
        toggle.announceForAccessibility(feedback.text)
    }

    /** 실제 시스템 값을 기준으로 화면 전체를 그립니다. 앱 내부에 별도 상태를 저장하지 않습니다. */
    private fun render() {
        val state = Inversion.readState(this)
        val permitted = Inversion.hasPermission(this)
        if (lastPermitted != null && lastPermitted != permitted) {
            // 권한이 바뀌면 이전 상황에서 남은 문구(예: "시스템 설정에서 바뀜")는 더 이상 맞지 않습니다.
            feedback.text = ""
        }
        lastPermitted = permitted

        when (state) {
            true -> {
                stateText.text = getString(R.string.state_on)
                stateText.setTextColor(getColor(R.color.accent))
                toggle.setBackgroundResource(R.drawable.bg_toggle_on)
                toggleIcon.setColorFilter(getColor(R.color.on_accent))
                toggleText.setTextColor(getColor(R.color.on_accent))
                toggleText.text = getString(R.string.btn_on)
                toggle.contentDescription = getString(R.string.toggle_desc_on)
            }
            false -> {
                stateText.text = getString(R.string.state_off)
                stateText.setTextColor(getColor(R.color.text))
                toggle.setBackgroundResource(R.drawable.bg_toggle_off)
                toggleIcon.setColorFilter(getColor(R.color.toggle_off_fg))
                toggleText.setTextColor(getColor(R.color.toggle_off_fg))
                toggleText.text = getString(R.string.btn_off)
                toggle.contentDescription = getString(R.string.toggle_desc_off)
            }
            null -> {
                stateText.text = getString(R.string.state_unknown)
                stateText.setTextColor(getColor(R.color.error))
                toggle.setBackgroundResource(R.drawable.bg_toggle_off)
                toggleIcon.setColorFilter(getColor(R.color.toggle_off_fg))
                toggleText.setTextColor(getColor(R.color.toggle_off_fg))
                toggleText.text = "–"
                feedback.text = getString(R.string.msg_unsupported)
            }
        }
        toggle.alpha = if (permitted) 1f else 0.6f

        // 알림 권한을 시스템 설정에서 끈 경우 스위치도 꺼진 것으로 보여 줍니다.
        pinSwitch.isChecked = PinnedNotification.isEnabled(this) && PinnedNotification.canPost(this)

        setupCard.visibility = if (permitted) View.GONE else View.VISIBLE
        if (!permitted) {
            if (feedback.text.isNullOrEmpty()) {
                feedback.setTextColor(getColor(R.color.text_secondary))
                feedback.text = getString(R.string.hint_setup_first)
            }
            val done = listOf(
                Inversion.isDeveloperOptionsOn(this),
                Inversion.isUsbDebuggingOn(this),
                false,
                false,
            )
            steps.zip(done).forEach { (view, ok) ->
                view.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    if (ok) R.drawable.ic_check else R.drawable.ic_pending, 0, 0, 0
                )
            }
        } else if (feedback.text.isNullOrEmpty() ||
            feedback.text == getString(R.string.hint_setup_first)
        ) {
            feedback.setTextColor(getColor(R.color.text_secondary))
            feedback.text = getString(R.string.hint_tap)
        }
    }

    private fun recheck() {
        render()
        val ok = Inversion.hasPermission(this)
        feedback.setTextColor(getColor(if (ok) R.color.success else R.color.error))
        feedback.text = getString(if (ok) R.string.msg_permission_ok else R.string.msg_still_no_permission)
        toast(feedback.text)
        if (ok) StatusSync.updateAll(this)
    }

    private fun copyCommand() {
        val cm = getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("adb", Inversion.grantCommand(this)))
        // Android 13+는 시스템이 자체적으로 복사 알림을 보여 줍니다.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) toast(getString(R.string.msg_copied))
    }

    private fun addHomeShortcut() {
        val sm = getSystemService(ShortcutManager::class.java)
        if (sm == null || !sm.isRequestPinShortcutSupported) {
            toast(getString(R.string.msg_shortcut_unsupported))
            return
        }
        val info = ShortcutInfo.Builder(this, "pinned_toggle")
            .setShortLabel(getString(R.string.shortcut_short))
            .setLongLabel(getString(R.string.shortcut_long))
            .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut))
            .setIntent(
                Intent(ToggleActivity.ACTION_TOGGLE).setClass(this, ToggleActivity::class.java)
            )
            .build()
        try {
            sm.requestPinShortcut(info, null)
        } catch (e: RuntimeException) {
            toast(getString(R.string.msg_shortcut_unsupported))
        }
    }

    private fun onPinSwitchClicked() {
        val want = pinSwitch.isChecked
        if (want && !PinnedNotification.canPost(this) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            pinSwitch.isChecked = false
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
            return
        }
        PinnedNotification.setEnabled(this, want)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_NOTIF) return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            PinnedNotification.setEnabled(this, true)
        } else {
            toast(getString(R.string.msg_notif_denied))
        }
        render()
    }

    private fun addHomeWidget() {
        val awm = AppWidgetManager.getInstance(this)
        val ok = try {
            awm.isRequestPinAppWidgetSupported &&
                awm.requestPinAppWidget(ComponentName(this, InversionWidget::class.java), null, null)
        } catch (e: RuntimeException) {
            false
        }
        if (!ok) toast(getString(R.string.msg_widget_unsupported))
    }

    private fun requestAddTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val sbm = getSystemService(StatusBarManager::class.java) ?: return
        try {
            sbm.requestAddTileService(
                ComponentName(this, InversionTileService::class.java),
                getString(R.string.tile_label),
                Icon.createWithResource(this, R.drawable.ic_tile),
                mainExecutor,
            ) { code ->
                when (code) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> toast(getString(R.string.msg_tile_added))
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> toast(getString(R.string.msg_tile_exists))
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> Unit // 사용자가 취소
                    else -> toast(getString(R.string.tile_manual_hint))
                }
            }
        } catch (e: RuntimeException) {
            toast(getString(R.string.tile_manual_hint))
        }
    }

    /**
     * Android 15(target 35+)부터는 앱 화면이 상태바·내비게이션 바 밑까지 그려집니다.
     * 시스템 바(및 노치) 영역만큼 여백을 줘서 내용이 가려지지 않게 합니다.
     */
    private fun applySystemBarInsets(root: View) {
        val baseLeft = root.paddingLeft
        val baseTop = root.paddingTop
        val baseRight = root.paddingRight
        val baseBottom = root.paddingBottom
        root.setOnApplyWindowInsetsListener { v, insets ->
            val (l, t, r, b) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bars = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                )
                listOf(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                listOf(
                    insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom,
                )
            }
            v.setPadding(baseLeft + l, baseTop + t, baseRight + r, baseBottom + b)
            insets
        }
        root.requestApplyInsets()
    }

    private fun open(success: Boolean) {
        if (!success) toast(getString(R.string.msg_cannot_open))
    }

    private fun toast(text: CharSequence) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    private companion object {
        const val REQ_NOTIF = 1
    }
}
