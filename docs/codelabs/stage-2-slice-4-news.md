# Stage 2 / Slice 4 — 뉴스 화면

> ⏱ 예상 소요 시간: 1시간 | 난이도: ★☆☆ | 선행 조건: Stage 1 완료 (Slice 3과 병렬 작업 가능)

---

## 이 Codelab에서 배우는 것

- **Compose `AndroidView`** 를 사용해 기존 Android View(WebView)를 Compose UI에 래핑하는 방법
- **WebView 다크모드** 지원: `WebSettingsCompat.setAlgorithmicDarkeningAllowed`
- **Compose `BackHandler`** 로 WebView 히스토리 기반 뒤로가기 처리
- **`DisposableEffect`** 로 WebView 생명주기 정리(destroy) 처리
- **AndroidX WebKit** 라이브러리 의존성 추가 및 version catalog 관리

---

## 완성 후 결과물

- 뉴스 탭을 선택하면 네이버 스포츠 축구 뉴스 페이지가 WebView로 로딩됩니다.
- JavaScript가 정상 동작하여 뉴스 페이지 내 인터랙션이 가능합니다.
- 뉴스 내부 링크 클릭 시 WebView 내에서 이동합니다.
- 뒤로가기 버튼으로 WebView 히스토리를 탐색하며, 히스토리가 없으면 기본 탭 동작으로 돌아갑니다.
- 시스템 다크모드에 따라 WebView 콘텐츠에 다크모드가 자동 적용됩니다.

---

## Step 1 — libs.versions.toml에 WebKit 의존성 추가

### 목표

> feature-news 모듈에서 WebView 다크모드를 지원하려면 AndroidX WebKit 라이브러리가 필요합니다. Gradle version catalog에 등록합니다.

### 작업 내용

**파일:** `gradle/libs.versions.toml`

version catalog의 `[versions]` 섹션과 `[libraries]` 섹션에 각각 아래 내용을 추가합니다.

```toml
[versions]
webkit = "1.15.0"

[libraries]
androidx-webkit = { group = "androidx.webkit", name = "webkit", version.ref = "webkit" }
```

**WHY:** `WebSettingsCompat.setAlgorithmicDarkeningAllowed()` API는 AndroidX WebKit 라이브러리에 포함되어 있습니다. 이 라이브러리가 없으면 WebView 다크모드를 지원할 수 없습니다.

**HOW:** Gradle version catalog 방식(`libs.versions.toml`)을 사용하면 모든 모듈에서 동일한 버전을 참조할 수 있어 버전 충돌을 방지합니다.

> 💡 **Tip:** 이미 `libs.versions.toml`에 webkit 관련 항목이 있다면 버전만 확인/업데이트하세요.

### ✅ 검증

- [ ] `libs.versions.toml` 파일에 `webkit` 버전과 `androidx-webkit` 라이브러리가 등록되어 있다
- [ ] Gradle Sync가 정상 완료된다

---

## Step 2 — feature-news 모듈의 build.gradle.kts 의존성 추가

### 목표

> feature-news 모듈이 AndroidX WebKit 라이브러리를 사용할 수 있도록 의존성을 선언합니다.

### 작업 내용

**파일:** `feature/feature-news/build.gradle.kts`

`dependencies` 블록에 아래 한 줄을 추가합니다.

```kotlin
// feature/feature-news/build.gradle.kts
dependencies {
    // 기존 의존성에 추가
    implementation(libs.androidx.webkit) // WebView 다크모드 지원용
}
```

**WHY:** WebView 자체는 Android SDK에 포함되어 있지만, **다크모드 제어용 `WebSettingsCompat` API**는 별도의 AndroidX WebKit 라이브러리에 있습니다. 이 의존성이 없으면 `WebSettingsCompat`과 `WebViewFeature` 클래스를 import할 수 없습니다.

**HOW:** `libs.androidx.webkit`은 Step 1에서 등록한 version catalog 항목을 참조합니다.

### ✅ 검증

- [ ] Gradle Sync 성공
- [ ] `import androidx.webkit.WebSettingsCompat` 가 feature-news 모듈에서 resolve 된다

---

## Step 3 — AndroidManifest.xml 인터넷 권한 확인

### 목표

> WebView가 외부 웹페이지를 로딩하려면 인터넷 권한이 필수입니다. app 모듈의 매니페스트에 이미 선언되어 있는지 확인합니다.

### 작업 내용

**파일:** `app/src/main/AndroidManifest.xml`

다음 권한이 이미 선언되어 있어야 합니다. 없다면 `<manifest>` 태그 바로 아래에 추가합니다.

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

**WHY:** 인터넷 권한 없이 WebView에서 외부 URL을 로딩하면 `net::ERR_CLEARTEXT_NOT_PERMITTED` 또는 빈 화면이 표시됩니다.

**HOW:** Android에서 매니페스트 권한은 **앱 모듈(app)에 선언하면 전체 모듈에 적용**됩니다. feature-news 모듈에 별도로 선언할 필요는 없습니다(매니페스트 병합 규칙).

> ⚠️ **주의:** 이 프로젝트는 기존 앱이 API 호출을 하고 있으므로 인터넷 권한이 이미 있을 가능성이 높습니다. 중복 선언해도 오류는 없지만, 확인 후 필요한 경우에만 추가하세요.

### ✅ 검증

- [ ] `AndroidManifest.xml`에 `INTERNET` 권한이 선언되어 있다
- [ ] `adb shell dumpsys package <패키지명> | grep INTERNET` 으로 권한 확인 가능

---

## Step 4 — NewsScreen.kt 작성

### 목표

> Compose `AndroidView`로 WebView를 래핑하여 뉴스 화면을 구현합니다. 다크모드 지원, 뒤로가기 핸들링, WebView 생명주기 정리까지 포함합니다.

### 작업 내용

**파일:** `feature/feature-news/src/main/kotlin/com/chase1st/feetballfootball/feature/news/NewsScreen.kt`

이 파일은 새로 생성합니다. 기존 `NewsFragment.kt`(88줄)의 WebView 설정, 다크모드, 뒤로가기 핸들링 로직을 Compose 방식으로 재구현합니다.

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

아래에서 각 핵심 부분을 자세히 설명합니다.

---

### 4-1. `AndroidView` — WebView를 Compose에 래핑

```kotlin
AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { context -> WebView(context).apply { ... } },
    update = { view -> canGoBack = view.canGoBack() },
)
```

**WHY:** WebView는 기존 Android View 시스템 컴포넌트입니다. Compose에서 직접 사용할 수 없으므로 `AndroidView`로 래핑해야 합니다.

**HOW:**
- `factory` 블록: Composable이 처음 생성될 때 **한 번만** 호출됩니다. 여기서 WebView를 생성하고 설정합니다.
- `update` 블록: Compose 리컴포지션 시마다 호출됩니다. WebView 상태를 Compose 상태와 동기화합니다.

---

### 4-2. WebView 설정

```kotlin
settings.apply {
    javaScriptEnabled = true      // 뉴스 페이지 인터랙션에 필수
    domStorageEnabled = true      // localStorage/sessionStorage 지원
    loadWithOverviewMode = true   // 콘텐츠를 화면에 맞춤
    useWideViewPort = true        // 뷰포트 메타 태그 지원
}
```

**WHY:** 네이버 스포츠 뉴스 페이지는 JavaScript 기반 SPA에 가깝습니다. JavaScript와 DOM Storage가 활성화되어야 정상 렌더링됩니다.

> ⚠️ **주의:** `javaScriptEnabled = true` 설정 시 Android Studio에서 보안 경고가 발생합니다. 이는 신뢰할 수 있는 URL만 로딩한다는 전제하에 `@SuppressLint("SetJavaScriptEnabled")`로 억제합니다.

---

### 4-3. 다크모드 지원

```kotlin
if (isDarkTheme &&
    WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)
) {
    WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
}
```

**WHY:** 시스템 다크모드가 켜져 있을 때 WebView 콘텐츠도 어둡게 표시하여 눈의 피로를 줄입니다.

**HOW:** `ALGORITHMIC_DARKENING`은 WebView가 웹 콘텐츠의 색상을 알고리즘적으로 반전시켜 다크모드를 구현합니다. 이 기능이 지원되지 않는 기기에서는 조건문으로 건너뜁니다.

> 💡 **Tip:** `isSystemInDarkTheme()`은 Compose 함수로, Compose의 테마 시스템과 연동됩니다. `Configuration.uiMode`를 직접 확인하는 것보다 간결합니다.

---

### 4-4. 뒤로가기 핸들링

```kotlin
BackHandler(enabled = canGoBack) {
    webView?.goBack()
}
```

**WHY:** 사용자가 뉴스 내부 링크를 클릭하여 이동한 후 뒤로가기 버튼을 누르면 WebView 히스토리로 이동해야 합니다. 히스토리가 없을 때는 기본 동작(탭 전환 또는 앱 종료)이 실행됩니다.

**HOW:** `BackHandler`의 `enabled` 파라미터가 `true`일 때만 뒤로가기를 가로챕니다. `canGoBack` 상태는 WebView 스크롤 이벤트에서 업데이트됩니다.

---

### 4-5. WebView 생명주기 정리

```kotlin
DisposableEffect(Unit) {
    onDispose {
        webView?.destroy()
        webView = null
    }
}
```

**WHY:** WebView는 메모리를 많이 사용하는 컴포넌트입니다. 화면을 떠날 때 `destroy()`를 호출하여 메모리 누수를 방지합니다.

**HOW:** `DisposableEffect(Unit)`은 Composable이 Composition에서 제거될 때 `onDispose` 블록을 실행합니다. key가 `Unit`이므로 Composable 생명주기와 1:1로 연동됩니다.

> ⚠️ **주의:** `destroy()` 호출 후에는 해당 WebView 인스턴스를 다시 사용할 수 없습니다. 참조를 `null`로 설정하여 잘못된 접근을 방지합니다.

### ✅ 검증

- [ ] `NewsScreen.kt` 파일이 올바른 패키지 경로에 생성되었다
- [ ] import 문이 모두 정상 resolve 된다
- [ ] 빌드 오류 없이 컴파일 된다

---

## Step 5 — 통합 빌드 및 동작 확인

### 목표

> feature-news 모듈이 앱에 올바르게 통합되어 뉴스 화면이 정상 동작하는지 최종 확인합니다.

### 작업 내용

1. **빌드 확인:**
   ```bash
   ./gradlew :feature:feature-news:assembleDebug
   ```

2. **앱 실행 후 뉴스 탭 테스트:**
   - 뉴스 탭 선택 시 네이버 스포츠 축구 뉴스 페이지가 로딩되는지 확인
   - 뉴스 기사 클릭 후 뒤로가기 버튼으로 이전 페이지로 돌아가는지 확인
   - 시스템 다크모드 전환 후 WebView 콘텐츠 색상이 변경되는지 확인

### ✅ 검증

- [ ] 뉴스 탭 선택 시 네이버 스포츠 축구 뉴스 로딩
- [ ] JavaScript 정상 동작 (뉴스 페이지 인터랙션)
- [ ] 뉴스 내부 링크 클릭 시 WebView 내 이동
- [ ] 뒤로가기 버튼: WebView 히스토리 탐색 → 히스토리 없으면 탭 동작
- [ ] 다크모드: 시스템 테마에 따라 WebView 다크모드 적용
- [ ] 화면 회전 시 WebView 상태 유지 (또는 재로딩)

---

## Step 6 — Git 커밋

### 목표

> 뉴스 화면 구현이 완료되면 변경사항을 커밋합니다.

### 작업 내용

```bash
git add feature/feature-news/
git add gradle/libs.versions.toml  # webkit 의존성 추가된 경우
git commit -m "feat: Slice 4 뉴스 화면 구현"
```

### ✅ 검증

- [ ] 커밋에 필요한 파일만 포함되었다
- [ ] 빌드가 깨지지 않는다

---

## 🎉 Slice 4 완료!

축하합니다! 뉴스 화면 구현이 완료되었습니다.

**이 Slice에서 완성한 것:**

| 모듈 | 파일 | 설명 |
|------|------|------|
| `feature-news` | `NewsScreen.kt` | Compose AndroidView + WebView 뉴스 화면 |
| `gradle` | `libs.versions.toml` | AndroidX WebKit 의존성 추가 |

**핵심 학습 포인트:**
- Compose `AndroidView`는 기존 View를 Compose에 래핑하는 브릿지 역할을 합니다
- `DisposableEffect`로 View 기반 리소스의 생명주기를 관리합니다
- `BackHandler`로 시스템 뒤로가기 동작을 Compose에서 제어합니다

> 💡 **Tip:** 이 Slice는 Domain/Data 레이어가 전혀 필요 없는 순수 UI 작업이었습니다. API 호출 없이 외부 웹페이지를 보여주는 것이 전부이므로, Slice 3과 병렬로 작업할 수 있었습니다. 다음 Slice 5(경기 상세)는 가장 복잡한 화면으로, 전체 레이어를 관통합니다.
