# Play 스토어 등록정보 (ko-KR)

## 앱 이름 (30자 이내)
색상 반전

## 간단한 설명 (80자 이내)
시스템 색상 반전을 버튼 한 번, 빠른 설정 타일, 홈 화면 바로가기로 켜고 끕니다.

## 자세한 설명
안드로이드 접근성 설정 깊숙이 있는 "색상 반전"을 버튼 한 번으로 켜고 끄는 앱입니다.

주요 기능
- 앱을 열고 큰 버튼 한 번으로 색상 반전 켜기/끄기
- 빠른 설정 패널 타일 지원: 알림창을 내려 바로 전환
- 홈 화면 바로가기 지원
- 시스템 설정에서 바꿔도 앱 화면이 실제 상태를 그대로 표시
- 인터넷 권한 없음, 광고 없음, 개인정보 수집 없음

처음 한 번 설정이 필요합니다
안드로이드는 일반 앱이 시스템 접근성 설정을 바꾸지 못하게 막아 둡니다. 그래서 처음 한 번만 컴퓨터에 휴대폰을 연결하고 아래 ADB 명령으로 권한을 부여해야 합니다. 앱 안에 단계별 안내가 있습니다.

adb shell pm grant com.cjsdudwls1.colorinversion android.permission.WRITE_SECURE_SETTINGS

이 권한은 색상 반전 켜짐/꺼짐 값 하나를 바꾸는 데에만 사용합니다.

## 개인정보처리방침 URL
https://cjsdudwls1.github.io/Color-Inversion/privacy.html

## 카테고리
도구

## 연락처 이메일
cjsdudwls1357@gmail.com

## 앱 액세스 (검토자 안내)
로그인은 필요 없습니다. 색상 반전 토글은 WRITE_SECURE_SETTINGS 권한이 있어야 동작하며, 이 권한은 ADB로만 부여할 수 있습니다:
adb shell pm grant com.cjsdudwls1.colorinversion android.permission.WRITE_SECURE_SETTINGS
권한이 없으면 앱이 부여 방법을 단계별로 안내하는 화면을 보여 줍니다.

## 설문 답변 요약
- 광고: 없음
- 데이터 보안: 수집·공유하는 데이터 없음
- 대상 연령: 18세 이상 (아동 대상 아님)
- 콘텐츠 등급: 도구, 폭력/성적/도박 등 해당 없음
- 정부 앱 / 금융 기능 / 건강 앱: 해당 없음
