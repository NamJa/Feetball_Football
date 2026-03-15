# Stage 2 / Slice 4 — 뉴스 화면

> **목표:** WebView 기반 뉴스 화면. Compose AndroidView로 래핑
> **선행 조건:** Stage 1 완료 (Slice 3과 병렬 작업 가능)
> **Git 브랜치:** `feature/renewal-slice-4-news`
> **참조 파일:** `NewsFragment.kt` (88줄)

---

## 이 Slice가 간단한 이유

외부 웹페이지를 WebView로 보여주는 것이 전부이며, API 호출도 없고 Domain/Data 레이어도 필요 없습니다. Compose의 `AndroidView`로 WebView를 래핑합니다.

---

## Step 1 — libs.versions.toml 의존성 추가

WebKit 의존성을 Version Catalog에 추가합니다:

```toml
[versions]
webkit = "1.15.0"

[libraries]
androidx-webkit = { group = "androidx.webkit", name = "webkit", version.ref = "webkit" }
```

---

## Step 2 — build.gradle.kts 의존성 추가

feature-news 모듈에 WebKit 의존성을 추가합니다:

```kotlin
// feature/feature-news/build.gradle.kts
dependencies {
    // 기존 의존성에 추가
    implementation(libs.androidx.webkit)
}
```

---

## Step 3 — AndroidManifest.xml 확인

인터넷 권한은 app 모듈의 AndroidManifest.xml에 이미 있어야 합니다:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## Step 4 — feature-news: NewsScreen.kt

```kotlin
package com.chase1st.feetballfootball.feature.news

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

// 참조: NewsFragment.kt — WebView 설정, 다크모드, 뒤로가기 핸들링

private const val NEWS_URL = "https://sports.news.naver.com/wfootball/index"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NewsScreen() {
    val isDarkTheme = isSystemInDarkTheme()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    // 뒤로가기 핸들링 (참조: NewsFragment.kt의 OnBackPressedCallback)
    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }

                // 다크모드 지원 (참조: NewsFragment.kt)
                if (isDarkTheme &&
                    WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)
                ) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
                }

                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()

                setOnScrollChangeListener { _, _, _, _, _ ->
                    canGoBack = this.canGoBack()
                }

                loadUrl(NEWS_URL)
                webView = this
            }
        },
        update = { view ->
            // WebView가 재구성될 때의 업데이트 로직
            canGoBack = view.canGoBack()
        },
    )

    // WebView 소멸 시 정리
    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }
}
```

---

## ★ Slice 4 완료 검증

**체크리스트:**

- [ ] 뉴스 탭 선택 시 네이버 스포츠 축구 뉴스 로딩
- [ ] JavaScript 정상 동작 (뉴스 페이지 인터랙션)
- [ ] 뉴스 내부 링크 클릭 시 WebView 내 이동
- [ ] 뒤로가기 버튼: WebView 히스토리 탐색 → 히스토리 없으면 탭 동작
- [ ] 다크모드: 시스템 테마에 따라 WebView 다크모드 적용
- [ ] 화면 회전 시 WebView 상태 유지 (또는 재로딩)
- [ ] `git commit -m "feat: Slice 4 뉴스 화면 구현"`
