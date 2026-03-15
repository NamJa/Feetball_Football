# Stage 3 / Optimization — 성능 최적화

> **목표:** R8 최적화, Baseline Profiles, 메모리 최적화, 앱 크기 축소
> **선행 조건:** Stage 3 Testing 완료
> **Git 브랜치:** `feature/renewal-optimization`
> **참조:** Android Performance 가이드라인

---

## Step 1 — R8 / ProGuard 설정

### 1.1 app/build.gradle.kts에서 R8 활성화

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

> **현재 상태:** 기존 `build.gradle`에서 `minifyEnabled false`로 비활성화 상태

### 1.2 ProGuard 규칙 작성

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

### 1.3 각 모듈별 consumer-rules.pro

```proguard
# core/core-network/consumer-rules.pro
# 이 모듈의 DTO가 R8에 의해 제거/난독화되지 않도록 보호
-keep class com.chase1st.feetballfootball.core.network.dto.** { *; }
```

### 1.4 검증

```bash
./gradlew assembleRelease

# APK 분석
# Android Studio → Build → Analyze APK → release APK 선택
# 확인 항목: DEX 크기, resources 크기, 총 APK 크기
```

---

## Step 2 — Baseline Profiles

### 2.1 의존성 추가

```toml
# libs.versions.toml
[versions]
profileinstaller = "1.4.1"
benchmark = "1.3.4"

[libraries]
androidx-profileinstaller = { group = "androidx.profileinstaller", name = "profileinstaller", version.ref = "profileinstaller" }
androidx-benchmark-macro-junit4 = { group = "androidx.benchmark", name = "benchmark-macro-junit4", version.ref = "benchmark" }
```

### 2.2 app 모듈에 ProfileInstaller 추가

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(libs.androidx.profileinstaller)
}
```

### 2.3 Baseline Profile Generator 모듈 생성

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

### 2.4 Baseline Profile 생성 테스트

```kotlin
// benchmark/src/main/kotlin/.../BaselineProfileGenerator.kt
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

### 2.5 Baseline Profile 생성 실행

```bash
# 에뮬레이터 또는 실제 디바이스에서 실행
./gradlew :benchmark:pixel6Api31BenchmarkAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.chase1st.feetballfootball.benchmark.BaselineProfileGenerator

# 생성된 프로필을 app 모듈에 복사
# app/src/main/baseline-prof.txt
```

---

## Step 3 — Compose 성능 최적화

### 3.1 Stability 확인

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

### 3.2 불안정(Unstable) 클래스 해결

```kotlin
// 문제: List<T>는 기본적으로 Unstable → 불필요한 Recomposition 발생

// 해결 1: kotlinx.collections.immutable 사용
// libs.versions.toml
// [libraries]
// kotlinx-collections-immutable = { group = "org.jetbrains.kotlinx", name = "kotlinx-collections-immutable", version = "0.4.0" }

// 해결 2: UiState에서 ImmutableList 사용
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

data class FixtureUiState(
    val fixtures: ImmutableList<Fixture> = persistentListOf(),
)
```

### 3.3 LazyColumn 최적화

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

### 3.4 이미지 로딩 최적화 (Coil)

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

---

## Step 4 — 메모리 최적화

### 4.1 WebView 메모리 누수 방지 (뉴스 화면)

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

### 4.2 ViewModel 스코프 확인

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

### 4.3 이미지 메모리 캐시 크기 설정

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

---

## Step 5 — 앱 크기 최적화

### 5.1 리소스 축소 확인

```kotlin
// app/build.gradle.kts (Step 1에서 이미 설정)
buildTypes {
    release {
        isShrinkResources = true  // 사용하지 않는 리소스 자동 제거
    }
}
```

### 5.2 불필요한 리소스 설정 제거

```xml
<!-- app/src/main/res/raw/keep.xml (필요 시) -->
<!-- 동적으로 참조하는 리소스가 있으면 여기에 명시 -->
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools"
    tools:keep="@drawable/ic_launcher_*" />
```

### 5.3 언어 리소스 제한

```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        resourceConfigurations += listOf("ko", "en")  // 한국어, 영어만 포함
    }
}
```

### 5.4 ABI 분할 (선택사항)

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

---

## Step 6 — 네트워크 최적화

### 6.1 OkHttp 캐시 설정

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
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("x-mas", xMasTokenProvider.generate())
                    .build()
            )
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
}
```

### 6.2 API 호출 최적화

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

---

## Step 7 — 앱 시작 속도 최적화

### 7.1 Splash Screen API

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(libs.androidx.core.splashscreen)
}
```

```toml
# libs.versions.toml
[versions]
splashscreen = "1.2.0"  # stable 졸업 완료

[libraries]
androidx-core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "splashscreen" }
```

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

### 7.2 Hilt 초기화 최적화

Hilt는 Application 생성 시 DI 그래프를 구축합니다. 무거운 초기화는 lazy로 지연시킵니다:

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

---

## ★ Optimization 완료 검증 체크리스트

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
- [ ] `git commit -m "perf: R8 최적화 + Baseline Profile + 메모리/네트워크 최적화"`
