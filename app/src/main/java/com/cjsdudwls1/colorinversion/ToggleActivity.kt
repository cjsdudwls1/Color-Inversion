package com.cjsdudwls1.colorinversion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * 홈 화면 바로가기 / 앱 아이콘 길게 누르기 메뉴에서 실행됩니다.
 * 화면을 띄우지 않고 색상 반전만 전환한 뒤 즉시 종료합니다.
 * 권한이 없으면 설정 안내가 있는 메인 화면을 엽니다.
 */
class ToggleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val result = Inversion.toggle(this)
        if (result == Inversion.Result.NoPermission) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
        Toast.makeText(applicationContext, Inversion.message(this, result), Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        const val ACTION_TOGGLE = "com.cjsdudwls1.colorinversion.TOGGLE"
    }
}
