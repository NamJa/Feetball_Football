# Stage 3 / Optimization — 성능 최적화
> ⏱ 예상 소요 시간: 10시간 | 난이도: ★★★ | 선행 조건: Stage 3 Testing 완료

---

## 이 Codelab에서 배우는 것

- **R8 / ProGuard** 설정으로 앱 크기 축소 및 코드 난독화
- 모듈별 **consumer-rules.pro** 작성으로 DTO/Route 클래스 보존
- **Baseline Profiles** 생성으로 앱 시작 속도 향상 (AOT 컴파일)
- **Compose Stability** 리포트 분석 및 불필요한 Recomposition 제거
- **kotlinx.collections.immutable**로 Compose 성능 최적화
- **Coil 3** 이미지 캐시 정책 설정 (메모리/디스크)
- **WebView 메모리 누수** 방지 패턴
- **OkHttp 캐시** 설정으로 네트워크 최적화
- **Repository 인메모리 캐싱**으로 중복 API 호출 방지
- **Splash Screen API** 및 **lazy DI 초기화**로 앱 시작 속도 개선

---

## 완성 후 결과물

| 항목 | 설명 |
|------|------|
| R8 활성화 | Release 빌드에서 코드 축소 + 리소스 축소 + 난독화 |
| ProGuard 규칙 | DTO, Navigation Route, Retrofit, Hilt 관련 클래스 보존 |
| Baseline Profile | 핵심 사용자 시나리오 기반 AOT 프로파일 |
| Compose 최적화 | ImmutableList 사용, LazyColumn key 지정, Stability 확보 |
| 이미지 최적화 | Coil 메모리 캐시 20%, 디스크 캐시 50MB |
| 네트워크 최적화 | OkHttp 10MB 캐시, Repository 인메모리 캐싱 |
| 앱 시작 속도 | Splash Screen API + OkHttpClient lazy 초기화 |
| 앱 크기 축소 | 언어 리소스 제한 (ko, en), ABI 분할 (선택) |

---

## Step 1 — R8 / ProGuard 설정

### 목표
> Release 빌드에서 R8 코드 축소, 리소스 축소, 난독화를 활성화하고, 필요한 클래스를 보존하는 ProGuard 규칙을 작성합니다.

### 작업 내용

#### 1.1 R8 활성화

**파일:** `app/build.gradle.kts`

**이유:** 현재 `minifyEnabled false`로 R8이 비활성화 상태입니다. R8을 활성화하면 사용하지 않는 코드 제거, 메서드 인라이닝, 클래스 이름 난독화가 적용되어 APK 크기가 크게 줄어듭니다.

```kotlin
// app/build.gradle.kts
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}
```

> ⚠️ **주의:** `isMinifyEnabled = true`와 `isShrinkResources = true`는 반드시 함께 사용합니다. `isShrinkResources`만 단독으로 활성화하면 빌드 에러가 발생합니다.

#### 1.2 ProGuard 규칙 작성

**파일:** `app/proguard-rules.pro`

**이유:** R8은 "사용되지 않는" 코드를 제거하는데, 리플렉션으로 접근하는 클래스(DTO, Retrofit 인터페이스 등)는 R8이 "미사용"으로 판단하여 제거할 수 있습니다. 이를 방지하기 위한 보존 규칙입니다.

```proguard
# app/proguard-rules.pro

# ── kotlinx.serialization ──
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# DTO 클래스 보존 (API 응답 역직렬화)
-keep,includedescriptorclasses class com.chase1st.feetballfootball.core.network.dto.**$$serializer { *; }
-keepclassmembers class com.chase1st.feetballfootball.core.network.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.chase1st.feetballfootball.core.network.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Navigation Route 클래스 보존 ──
-keep class com.chase1st.feetballfootball.navigation.** { *; }

# ── Retrofit ──
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# ── OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Coroutines ──
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── Hilt ──
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
```

> 💡 **Tip:** ProGuard 규칙은 최소한으로 유지합니다. 과도한 `-keep` 규칙은 R8의 최적화 효과를 반감시킵니다. 빌드 후 크래시가 발생하면 로그에서 `ClassNotFoundException`이나 `NoSuchMethodException`을 확인하고, 해당 클래스만 보존 규칙에 추가합니다.

#### 1.3 모듈별 consumer-rules.pro

**파일:** `core/core-network/consumer-rules.pro`

**이유:** `consumer-rules.pro`는 라이브러리 모듈이 앱 모듈에 자신의 ProGuard 규칙을 전파하는 파일입니다. core-network 모듈의 DTO 클래스가 R8에 의해 제거/난독화되지 않도록 보호합니다.

```proguard
# core/core-network/consumer-rules.pro
# 이 모듈의 DTO가 R8에 의해 제거/난독화되지 않도록 보호
-keep class com.chase1st.feetballfootball.core.network.dto.** { *; }
```

#### 1.4 검증

```bash
./gradlew assembleRelease

# APK 분석
# Android Studio → Build → Analyze APK → release APK 선택
# 확인 항목: DEX 크기, resources 크기, 총 APK 크기
```

> 💡 **Tip:** Android Studio의 APK Analyzer에서 "DEX" 섹션을 열어 `com.chase1st.feetballfootball.core.network.dto` 패키지가 존재하는지 확인합니다. 해당 패키지가 없으면 ProGuard 규칙이 적용되지 않은 것입니다.

### ✅ 검증
- [ ] `./gradlew assembleRelease` 빌드 성공 (R8 활성화 상태)
- [ ] Release APK에서 DTO 클래스가 보존되어 있음 (APK Analyzer 확인)
- [ ] Release APK에서 Navigation Route 클래스가 보존되어 있음
- [ ] Release APK에서 모든 화면이 정상 동작 (난독화 후 크래시 없음)

---

## Step 2 — Baseline Profiles

### 목표
> 핵심 사용자 시나리오에 대한 Baseline Profile을 생성하여 앱 시작 속도와 스크롤 성능을 향상시킵니다.

### 작업 내용

#### 2.1 의존성 추가

**파일:** `gradle/libs.versions.toml`

**이유:** ProfileInstaller는 앱이 Google Play에서 설치될 때 Baseline Profile을 적용하는 라이브러리입니다. Benchmark 라이브러리는 Baseline Profile을 생성하는 데 사용됩니다.

```toml
[versions]
profileinstaller = "1.4.1"
benchmark = "1.3.4"

[libraries]
androidx-profileinstaller = { group = "androidx.profileinstaller", name = "profileinstaller", version.ref = "profileinstaller" }
androidx-benchmark-macro-junit4 = { group = "androidx.benchmark", name = "benchmark-macro-junit4", version.ref = "benchmark" }
```

#### 2.2 app 모듈에 ProfileInstaller 추가

**파일:** `app/build.gradle.kts`

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(libs.androidx.profileinstaller)
}
```

> 💡 **Tip:** ProfileInstaller는 앱 사이즈에 미치는 영향이 매우 작으면서(~20KB), 첫 실행 시 AOT(Ahead-Of-Time) 컴파일로 앱 시작 속도를 30~50% 개선할 수 있습니다.

#### 2.3 Benchmark 모듈 생성

**파일:** `benchmark/build.gradle.kts`

**이유:** Baseline Profile 생성은 별도의 `com.android.test` 모듈에서 실행합니다. 이 모듈은 앱을 실제 디바이스에서 실행하며 핵심 코드 경로를 수집합니다.

```kotlin
// benchmark/build.gradle.kts
plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.chase1st.feetballfootball.benchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 28  // Benchmark은 API 28 이상 필요
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
}
```

> ⚠️ **주의:** `minSdk = 28`로 설정해야 합니다. Benchmark 라이브러리는 API 28 미만을 지원하지 않습니다. 앱 자체의 `minSdk = 26`과는 다른 값입니다.

**파일:** `settings.gradle.kts`에 `include(":benchmark")` 추가를 잊지 마세요.

#### 2.4 Baseline Profile 생성 테스트

**파일:** `benchmark/src/main/kotlin/com/chase1st/feetballfootball/benchmark/BaselineProfileGenerator.kt`

**이유:** 앱의 핵심 사용자 시나리오(경기 목록 스크롤, 리그 탭 전환, 뉴스 탭 전환)를 자동으로 실행하여, 해당 코드 경로를 AOT 컴파일 대상으로 수집합니다.

```kotlin
package com.chase1st.feetballfootball.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        rule.collect(
            packageName = "com.chase1st.feetballfootball",
        ) {
            // 앱 시작
            pressHome()
            startActivityAndWait()

            // 핵심 사용자 시나리오
            // 1. 경기 목록 스크롤
            device.waitForIdle()

            // 2. 리그 탭 전환
            // Bottom Navigation에서 리그 탭 클릭
            device.waitForIdle()

            // 3. 뉴스 탭 전환
            device.waitForIdle()
        }
    }
}
```

#### 2.5 Baseline Profile 생성 실행

```bash
# 에뮬레이터 또는 실제 디바이스에서 실행
./gradlew :benchmark:pixel6Api31BenchmarkAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.chase1st.feetballfootball.benchmark.BaselineProfileGenerator

# 생성된 프로필을 app 모듈에 복사
# app/src/main/baseline-prof.txt
```

> 💡 **Tip:** 생성된 `baseline-prof.txt` 파일은 `app/src/main/` 디렉토리에 위치해야 합니다. 이 파일은 Git에 커밋합니다 — 바이너리 프로필이 아닌 텍스트 형식의 규칙 파일이므로 버전 관리에 적합합니다.

### ✅ 검증
- [ ] `benchmark` 모듈이 빌드 성공
- [ ] Baseline Profile 생성 테스트가 디바이스에서 실행 성공
- [ ] `app/src/main/baseline-prof.txt` 파일이 생성됨
- [ ] ProfileInstaller가 앱에 포함됨 (APK Analyzer에서 확인)

---

## Step 3 — Compose 성능 최적화

### 목표
> Compose Compiler 리포트를 분석하여 불안정(Unstable) 클래스를 해결하고, LazyColumn과 이미지 로딩을 최적화합니다.

### 작업 내용

#### 3.1 Compose Stability 리포트 활성화

**파일:** 각 feature 모듈 또는 app 모듈의 `build.gradle.kts`

**이유:** Compose Compiler는 각 클래스의 안정성(Stable/Unstable)을 분석합니다. Unstable 클래스가 매개변수로 사용되면 불필요한 Recomposition이 발생합니다. 리포트를 생성하여 어떤 클래스가 Unstable인지 확인합니다.

```kotlin
// build.gradle.kts (app 또는 feature 모듈)
android {
    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }
}
```

```bash
# Compose 컴파일러 리포트 생성
./gradlew assembleRelease

# 결과 확인:
# build/compose_compiler/<variant>/<module>-classes.txt    ← 클래스 안정성
# build/compose_compiler/<variant>/<module>-composables.txt ← Composable 함수 정보
```

> 💡 **Tip:** `-classes.txt` 파일에서 `unstable` 키워드를 검색하면 문제가 되는 클래스를 빠르게 찾을 수 있습니다. 대부분의 경우 `List<T>` 타입의 프로퍼티가 원인입니다.

#### 3.2 불안정(Unstable) 클래스 해결

**이유:** `List<T>`는 Kotlin stdlib에서 `MutableList<T>`의 상위 타입이므로, Compose Compiler가 "변경될 수 있다"고 판단하여 Unstable로 분류합니다. `ImmutableList<T>`를 사용하면 Compose Compiler가 "변경 불가"로 인식하여 Smart Recomposition이 동작합니다.

**파일:** `gradle/libs.versions.toml`

```toml
[libraries]
kotlinx-collections-immutable = { group = "org.jetbrains.kotlinx", name = "kotlinx-collections-immutable", version = "0.4.0" }
```

**파일:** 각 feature 모듈의 UiState 클래스

```kotlin
// 해결: UiState에서 ImmutableList 사용
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

data class FixtureUiState(
    val fixtures: ImmutableList<Fixture> = persistentListOf(),
)
```

> ⚠️ **주의:** `ImmutableList`를 사용하려면 ViewModel에서 데이터를 emit할 때 `.toImmutableList()`로 변환해야 합니다. `List<T>`를 `ImmutableList<T>`에 직접 대입할 수 없습니다.

#### 3.3 LazyColumn 최적화

**파일:** 각 feature 모듈의 LazyColumn 사용 부분

**이유:** `key`를 지정하지 않으면 LazyColumn은 아이템의 위치(index)를 기준으로 Recomposition합니다. 목록이 변경되면 모든 아이템이 Recomposition됩니다. `key`를 지정하면 동일 ID의 아이템은 Recomposition을 건너뜁니다.

```kotlin
// 각 feature 모듈의 LazyColumn에 key 지정
LazyColumn {
    items(
        items = fixtures,
        key = { it.id },  // 고유 키로 불필요한 recomposition 방지
    ) { fixture ->
        FixtureItem(fixture = fixture)
    }
}
```

#### 3.4 이미지 로딩 최적화 (Coil 3)

**파일:** `core/core-designsystem`의 TeamLogo 컴포넌트

**이유:** 축구 앱은 팀 로고를 반복적으로 표시합니다. 정확한 크기로 디코딩하면 메모리 사용량이 줄고, 캐시 정책으로 네트워크 호출을 최소화합니다.

```kotlin
// core-designsystem의 TeamLogo 컴포넌트
@Composable
fun TeamLogo(
    url: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)      // 메모리 캐시
            .diskCachePolicy(CachePolicy.ENABLED)         // 디스크 캐시
            .size(with(LocalDensity.current) { size.roundToPx() }) // 정확한 크기로 디코딩
            .build(),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}
```

> 💡 **Tip:** `.size()`를 지정하면 원본 이미지가 1024x1024라도 32x32로 디코딩됩니다. 이는 메모리 사용량을 **1024배** 줄이는 효과가 있습니다 (1024x1024x4bytes vs 32x32x4bytes).

### ✅ 검증
- [ ] Compose Compiler 리포트에서 UiState 클래스가 Stable로 표시됨
- [ ] `ImmutableList`가 적용된 UiState가 정상 동작
- [ ] 모든 LazyColumn에 `key`가 지정됨
- [ ] TeamLogo 컴포넌트에 캐시 정책이 설정됨
- [ ] Layout Inspector에서 불필요한 Recomposition이 감소했는지 확인

---

## Step 4 — 메모리 최적화

### 목표
> WebView 메모리 누수를 방지하고, ViewModel 스코프를 확인하며, 이미지 캐시 크기를 설정합니다.

### 작업 내용

#### 4.1 WebView 메모리 누수 방지

**파일:** feature-news의 `NewsScreen.kt` (Stage 2에서 이미 구현)

**이유:** WebView는 Android에서 가장 흔한 메모리 누수 원인 중 하나입니다. `DisposableEffect`에서 명시적으로 `destroy()`를 호출하여 WebView가 사용하는 네이티브 메모리를 해제합니다.

```kotlin
// feature-news의 NewsScreen.kt — Stage 2에서 이미 구현
// DisposableEffect에서 WebView.destroy() 호출 확인
DisposableEffect(Unit) {
    onDispose {
        webView?.stopLoading()
        webView?.destroy()
        webView = null
    }
}
```

> ⚠️ **주의:** `webView?.destroy()` 전에 반드시 `webView?.stopLoading()`을 호출합니다. 로딩 중인 WebView를 destroy하면 크래시가 발생할 수 있습니다.

#### 4.2 ViewModel 스코프 확인

**파일:** 각 ViewModel

**이유:** `viewModelScope`는 ViewModel이 소멸될 때 자동으로 모든 코루틴을 취소합니다. 직접 `CoroutineScope`를 생성하면 누수가 발생할 수 있으므로, 반드시 `viewModelScope`를 사용합니다.

```kotlin
// 각 ViewModel에서 viewModelScope 사용 확인
// viewModelScope는 ViewModel 소멸 시 자동으로 취소됨
class FixtureViewModel @Inject constructor(...) : ViewModel() {
    init {
        viewModelScope.launch {
            // API 호출 — ViewModel 소멸 시 자동 취소
        }
    }
}
```

#### 4.3 이미지 메모리 캐시 크기 설정

**파일:** `app/src/main/kotlin/.../FeetballApp.kt` (Application 클래스)

**이유:** Coil의 기본 메모리 캐시는 앱 메모리의 25%입니다. 축구 앱에서는 팀 로고가 반복되므로 20%로 충분하며, 디스크 캐시 50MB로 앱 재시작 시에도 이미지를 빠르게 로딩합니다.

```kotlin
// FeetballApp.kt (Application 클래스)
@HiltAndroidApp
class FeetballApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this@FeetballApp, 0.20)  // 앱 메모리의 20%
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)  // 50MB
                    .build()
            }
            .build()
    }
}
```

> 💡 **Tip:** `ImageLoaderFactory` 인터페이스를 Application에 구현하면, Coil이 자동으로 이 ImageLoader를 사용합니다. 별도의 DI 설정이 필요 없습니다.

### ✅ 검증
- [ ] NewsScreen에서 뒤로가기 후 WebView 메모리가 해제됨 (Android Profiler 확인)
- [ ] 모든 ViewModel이 `viewModelScope`를 사용 (직접 생성한 CoroutineScope 없음)
- [ ] FeetballApp에 ImageLoaderFactory가 구현됨
- [ ] Android Profiler에서 메모리 누수가 없음

---

## Step 5 — 앱 크기 최적화

### 목표
> 사용하지 않는 리소스를 제거하고, 언어 리소스를 제한하며, ABI 분할을 설정합니다.

### 작업 내용

#### 5.1 리소스 축소 확인

**파일:** `app/build.gradle.kts` (Step 1에서 이미 설정)

```kotlin
// app/build.gradle.kts (Step 1에서 이미 설정)
buildTypes {
    release {
        isShrinkResources = true  // 사용하지 않는 리소스 자동 제거
    }
}
```

#### 5.2 불필요한 리소스 보존 설정

**파일:** `app/src/main/res/raw/keep.xml` (필요 시)

**이유:** `isShrinkResources = true`는 코드에서 참조되지 않는 리소스를 자동 제거합니다. 하지만 동적으로 참조하는 리소스(예: 리소스 이름을 문자열로 조합하여 접근)는 오탐으로 제거될 수 있습니다. 이런 리소스는 `keep.xml`에 명시합니다.

```xml
<!-- app/src/main/res/raw/keep.xml (필요 시) -->
<!-- 동적으로 참조하는 리소스가 있으면 여기에 명시 -->
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools"
    tools:keep="@drawable/ic_launcher_*" />
```

#### 5.3 언어 리소스 제한

**파일:** `app/build.gradle.kts`

**이유:** 앱이 한국어와 영어만 지원하므로, 라이브러리가 포함하는 수십 개의 언어 리소스를 제거합니다. 이것만으로 수백 KB를 절약할 수 있습니다.

```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        resourceConfigurations += listOf("ko", "en")  // 한국어, 영어만 포함
    }
}
```

#### 5.4 ABI 분할 (선택사항)

**파일:** `app/build.gradle.kts`

**이유:** 하나의 APK에 모든 CPU 아키텍처(arm64, arm32, x86 등)의 네이티브 라이브러리가 포함됩니다. ABI 분할을 적용하면 각 아키텍처별로 별도의 APK가 생성되어 크기가 줄어듭니다. 단, Google Play에 여러 APK를 업로드해야 합니다.

```kotlin
// app/build.gradle.kts
android {
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }
}
```

> 💡 **Tip:** App Bundle(AAB) 형식으로 Google Play에 배포하면 ABI 분할을 Google Play가 자동으로 처리합니다. AAB를 사용할 경우 이 설정은 불필요합니다.

### ✅ 검증
- [ ] Release APK 크기가 이전 대비 감소 (Android Studio APK Analyzer로 비교)
- [ ] `resourceConfigurations`에 "ko", "en"만 포함
- [ ] APK에 불필요한 언어 리소스가 제거됨 (APK Analyzer → res 폴더 확인)
- [ ] (선택) ABI 분할 APK가 각각 생성됨

---

## Step 6 — 네트워크 최적화

### 목표
> OkHttp 캐시를 설정하고, Repository에 인메모리 캐싱을 추가하여 중복 API 호출을 방지합니다.

### 작업 내용

#### 6.1 OkHttp 캐시 설정

**파일:** `core/core-network`의 NetworkModule (Hilt DI 모듈)

**이유:** SofaScore API는 경기 일정 등의 데이터가 자주 변경되지 않습니다. OkHttp 캐시를 설정하면 서버가 `Cache-Control` 헤더를 반환할 때 로컬 캐시를 사용하여 네트워크 호출을 줄입니다.

```kotlin
// core-network의 NetworkModule
@Provides
@Singleton
fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
    return OkHttpClient.Builder()
        .cache(Cache(
            directory = File(context.cacheDir, "http_cache"),
            maxSize = 10L * 1024 * 1024,  // 10MB
        ))
        // SofaScore API — GET 엔드포인트는 인증 불필요
        // Bearer Token이 필요한 경우 아래 인터셉터를 활성화
        // .addInterceptor { chain ->
        //     chain.proceed(
        //         chain.request().newBuilder()
        //             .header("Authorization", "Bearer $apiToken")
        //             .build()
        //     )
        // }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
}
```

> ⚠️ **주의:** SofaScore API의 응답에 `Cache-Control` 헤더가 없으면, OkHttp 캐시가 동작하지 않습니다. 이 경우 네트워크 인터셉터에서 강제로 캐시 헤더를 추가하는 방법을 고려할 수 있지만, 실시간 데이터의 신선도와 트레이드오프를 고려해야 합니다.

#### 6.2 Repository 인메모리 캐싱

**파일:** `core/core-data`의 `FixtureRepositoryImpl.kt`

**이유:** 사용자가 같은 날짜의 경기 목록을 반복 조회할 때(예: 탭 전환 후 복귀), 매번 API를 호출하는 대신 메모리 캐시를 사용합니다. 불필요한 네트워크 호출을 줄이면 응답 속도와 사용자 경험이 향상됩니다.

```kotlin
// 중복 API 호출 방지: Repository에서 캐싱
class FixtureRepositoryImpl @Inject constructor(
    private val api: FootballApiService,
) : FixtureRepository {

    // 간단한 인메모리 캐시 (같은 날짜 반복 조회 방지)
    private val cache = mutableMapOf<LocalDate, Map<LeagueInfo, List<Fixture>>>()

    override suspend fun getFixturesByDate(date: LocalDate): Result<Map<LeagueInfo, List<Fixture>>> {
        cache[date]?.let { return Result.Success(it) }

        return try {
            val response = api.getFixtures(date.toString(), SeasonUtil.currentSeason())
            val mapped = FixtureMapper.map(response)
            cache[date] = mapped
            Result.Success(mapped)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    fun clearCache() {
        cache.clear()
    }
}
```

> 💡 **Tip:** 이 인메모리 캐시는 프로세스 생존 기간 동안만 유효합니다. 앱이 백그라운드에서 종료되면 캐시도 사라집니다. Pull-to-refresh 기능에서 `clearCache()`를 호출하여 사용자가 강제 새로고침할 수 있도록 합니다.

### ✅ 검증
- [ ] OkHttp에 10MB 캐시가 설정됨
- [ ] 같은 날짜의 경기 목록을 두 번 조회할 때 두 번째는 캐시에서 반환됨
- [ ] `clearCache()` 호출 후 API 재호출이 정상 동작

---

## Step 7 — 앱 시작 속도 최적화

### 목표
> Splash Screen API를 적용하고, DI 그래프의 무거운 초기화를 lazy로 지연시킵니다.

### 작업 내용

#### 7.1 Splash Screen API

**파일:** `gradle/libs.versions.toml`

```toml
[versions]
splashscreen = "1.2.0"  # stable 졸업 완료

[libraries]
androidx-core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "splashscreen" }
```

**파일:** `app/build.gradle.kts`

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(libs.androidx.core.splashscreen)
}
```

**파일:** `app/src/main/kotlin/.../MainActivity.kt`

**이유:** Splash Screen API는 Android 12+의 기본 스플래시 화면 동작을 모든 API 레벨에서 일관되게 제공합니다. `installSplashScreen()`은 반드시 `super.onCreate()` 이전에 호출해야 합니다.

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            FeetballTheme {
                FeetballApp()
            }
        }
    }
}
```

> ⚠️ **주의:** `installSplashScreen()`은 반드시 `super.onCreate(savedInstanceState)` **이전에** 호출해야 합니다. 순서가 바뀌면 크래시가 발생합니다.

#### 7.2 Hilt DI lazy 초기화

**파일:** `core/core-network`의 NetworkModule

**이유:** Hilt는 Application 생성 시 DI 그래프를 구축합니다. OkHttpClient는 내부적으로 ConnectionPool, Dispatcher 등을 초기화하므로 비용이 큽니다. `dagger.Lazy`를 사용하면 실제 네트워크 호출 시점까지 초기화를 지연시킵니다.

```kotlin
// NetworkModule에서 OkHttpClient를 lazy 초기화
@Provides
@Singleton
fun provideRetrofit(okHttpClient: dagger.Lazy<OkHttpClient>): Retrofit {
    return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .callFactory { request -> okHttpClient.get().newCall(request) }  // lazy
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .build()
}
```

> 💡 **Tip:** `dagger.Lazy<T>`는 `get()`이 호출될 때 비로소 객체를 생성합니다. Retrofit의 `callFactory`에 lambda를 전달하면, 첫 번째 API 호출 시점에 OkHttpClient가 초기화됩니다. 앱 시작 시간이 수십 ms 단축됩니다.

### ✅ 검증
- [ ] Splash Screen이 앱 시작 시 정상 표시됨
- [ ] `installSplashScreen()`이 `super.onCreate()` 이전에 호출됨
- [ ] OkHttpClient가 lazy 초기화됨 (첫 API 호출 전까지 생성되지 않음)
- [ ] 앱 시작 시간이 이전 대비 개선됨 (Android Profiler 또는 Macrobenchmark로 측정)

---

## 완료

### 최종 검증 체크리스트

- [ ] R8/ProGuard 활성화 + Release 빌드 성공
- [ ] ProGuard 규칙: DTO, Navigation Route, Retrofit 보존 확인
- [ ] Baseline Profile 생성 완료
- [ ] Compose 안정성 리포트 확인 — Unstable 클래스 해결
- [ ] LazyColumn에 key 지정
- [ ] Coil 메모리/디스크 캐시 설정
- [ ] WebView 메모리 누수 방지 확인
- [ ] OkHttp 캐시 설정 (10MB)
- [ ] 앱 크기 확인 (Release APK 기준)
- [ ] 앱 시작 속도 확인 (Splash Screen + lazy 초기화)
- [ ] Release 빌드에서 모든 화면 정상 동작 (난독화 후에도)

### 커밋

```bash
git commit -m "perf: R8 최적화 + Baseline Profile + 메모리/네트워크 최적화"
```

### 다음 단계

Stage 3 / KMP로 진행하여 core 모듈을 Kotlin Multiplatform으로 전환하고 iOS 앱 기반을 마련합니다.
