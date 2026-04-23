# Feetball Football — Renewal Codelabs (통합본)

> **대상 기준:** 2026-04-23 라이브러리 버전 상태
> **구성:** `docs/codelabs/` 하위 11개 파일을 `_sidebar.md` 순서에 맞춰 단일 파일로 통합

## 전체 목차

- [Stage 1 — 프로젝트 셋업](#stage-1--프로젝트-셋업)
- [Stage 2 — Slice 1 · 리그 선택](#stage-2--slice-1--리그-선택)
- [Stage 2 — Slice 2 · 리그 순위](#stage-2--slice-2--리그-순위)
- [Stage 2 — Slice 3 · 경기 일정](#stage-2--slice-3--경기-일정)
- [Stage 2 — Slice 4 · 뉴스](#stage-2--slice-4--뉴스)
- [Stage 2 — Slice 5 · 경기 상세](#stage-2--slice-5--경기-상세)
- [Stage 2 — Navigation 3 통합](#stage-2--navigation-3-통합)
- [Stage 2 — Cleanup · 레거시 제거](#stage-2--cleanup--레거시-제거)
- [Stage 3 — 테스트 인프라](#stage-3--테스트-인프라)
- [Stage 3 — 성능 최적화](#stage-3--성능-최적화)
- [Stage 3 — KMP 전환](#stage-3--kmp-전환)

---

# Stage 1 — 프로젝트 셋업
> ⏱ 예상 소요 시간: 4시간 | 난이도: ★★☆ | 선행 조건: 없음

---

## 이 Codelab에서 배우는 것

- Gradle Version Catalog (`libs.versions.toml`)를 사용한 중앙 집중식 의존성 관리
- Groovy DSL (`build.gradle`)에서 Kotlin DSL (`build.gradle.kts`)로 빌드 파일 전환
- Convention Plugins를 활용한 빌드 로직 재사용 (`build-logic` 모듈)
- 멀티 모듈 아키텍처 설계 (core 7개 + feature 4개 = 11개 모듈)
- Hilt를 활용한 의존성 주입(DI) 그래프 구성
- Jetpack Compose 기반 UI 셋업 및 Material 3 테마 시스템
- Retrofit 3 + kotlinx.serialization 네트워크 레이어 구성
- Room Database 기반 코드 셋업
- SofaScore API 인증 처리 (Bearer Token 기반)

---

## 완성 후 결과물

| 항목 | 상태 |
|------|------|
| 모듈 구조 | 11개 모듈 (core 7 + feature 4) 빌드 성공 |
| 빌드 시스템 | Kotlin DSL + Version Catalog + Convention Plugins |
| 도구 버전 | Kotlin 2.3.20 / AGP 9.1.1 / Gradle 9.4.1 |
| SDK | compileSdk 36, targetSdk 35, minSdk 26 |
| DI | Hilt DI 그래프 런타임 동작 |
| 네트워크 | Retrofit 3.0 + OkHttp 5.x + kotlinx.serialization |
| UI | 빈 Compose 화면 + FeetballTheme 적용 |
| 데이터베이스 | Room Database 연결 (빈 상태) |
| 화면 | "Feetball Football v2.0" 텍스트 표시 |

---

## Step 1 — Version Catalog 생성

### 목표
> 모든 의존성 버전을 `gradle/libs.versions.toml` 한 곳에서 관리하도록 변경한다.

### 작업 내용

현재 프로젝트는 각 `build.gradle` 파일에 의존성 버전이 직접 하드코딩되어 있습니다.

```groovy
// 현재 상태 (app/build.gradle) — 버전이 곳곳에 흩어져 있음
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.google.code.gson:gson:2.9.0'
implementation 'com.squareup.picasso:picasso:2.71828'
```

이 방식은 멀티 모듈로 전환할 때 버전 충돌과 관리 복잡도가 급격히 증가합니다. Gradle Version Catalog를 사용하면 **한 파일에서 모든 버전을 선언**하고, 각 모듈에서 타입 안전하게 참조할 수 있습니다.

**파일 경로:** `gradle/libs.versions.toml` (새로 생성)

> 💡 **Tip:** `gradle/` 디렉토리는 이미 존재합니다 (`gradle/wrapper/` 안에 wrapper 파일이 있음). 그 안에 `libs.versions.toml` 파일을 생성하면 됩니다.

```toml
[versions]
# Core
agp = "9.1.1"                    # 9.1.0 패치 릴리스 (2026-04). R8는 DEX 빌드 시 unnamed 패키지로 repackage가 기본값
kotlin = "2.3.20"                # 2026-03 릴리스. name-based destructuring, Map.Entry immutable copy API, Native C/ObjC 새 interop 모드
ksp = "2.3.6"                    # Kotlin 2.3.20과 매칭. KSP 버전은 Kotlin 컴파일러 버전과 독립적으로 관리됨

# AndroidX Core
core-ktx = "1.18.0"
appcompat = "1.7.0"
activity-compose = "1.13.0"
lifecycle = "2.10.0"
navigation = "2.9.7"

# Compose
compose-bom = "2026.03.00"       # 2026-03-30 릴리스 기준

# DI
hilt = "2.59.2"                  # 2.59부터 AGP 9 / Gradle 9.1+ 필수 (브레이킹). Hilt Gradle Plugin도 AGP 9 호환으로 업데이트
hilt-navigation-compose = "1.3.0"# ⚠ 1.3.0에서 `hiltViewModel()` API가 `androidx.hilt:hilt-lifecycle-viewmodel-compose`로 이동함

# Networking
retrofit = "3.0.0"
okhttp = "5.3.0"                 # 5.x 메이저 업: Android AAR 아티팩트 분리

# Serialization
kotlinx-serialization = "1.10.0"

# Database
room = "2.8.4"                   # 2.x 유지보수 모드. Room 3.0-alpha01 (androidx.room3:room3-*)은 2026-03 공개되었으나 아직 stable 아님

# Image
coil = "3.4.0"

# Async
coroutines = "1.10.2"

# Testing
junit-jupiter = "6.0.3"          # JUnit 6 GA (2026-02-15). 좌표는 그대로(org.junit.jupiter:junit-jupiter), JDK 17+ 필수
mockk = "1.14.7"
turbine = "1.2.1"
truth = "1.4.5"

[libraries]
# AndroidX
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core-ktx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }

# Lifecycle
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }

# Navigation
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

# Compose BOM
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }
# 1.3.0에서 `hiltViewModel()`이 이 아티팩트로 이동 (패키지: androidx.hilt.lifecycle.viewmodel.compose). `hilt-navigation-compose`는 navigation 그래프 scoped ViewModel용으로 남음
hilt-lifecycle-viewmodel-compose = { group = "androidx.hilt", name = "hilt-lifecycle-viewmodel-compose", version.ref = "hilt-navigation-compose" }

# Retrofit
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }

# OkHttp (5.x: Android AAR 아티팩트 분리, KMP 미지원 — KMP에서는 Ktor 사용)
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }

# Serialization
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }

# Coil
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }

# Coroutines
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

# Testing
junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit-jupiter" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }

# Build-logic dependencies
android-gradlePlugin = { group = "com.android.tools.build", name = "gradle", version.ref = "agp" }
kotlin-gradlePlugin = { group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version.ref = "kotlin" }
compose-gradlePlugin = { group = "org.jetbrains.kotlin", name = "compose-compiler-gradle-plugin", version.ref = "kotlin" }
ksp-gradlePlugin = { group = "com.google.devtools.ksp", name = "symbol-processing-gradle-plugin", version.ref = "ksp" }  # KSP 2.3.x 아티팩트명

[bundles]
compose = ["compose-ui", "compose-ui-graphics", "compose-ui-tooling-preview", "compose-material3"]
compose-debug = ["compose-ui-tooling", "compose-ui-test-manifest"]
lifecycle = ["androidx-lifecycle-runtime-compose", "androidx-lifecycle-viewmodel-compose"]
networking = ["retrofit", "retrofit-kotlinx-serialization", "okhttp", "okhttp-logging"]
room = ["room-runtime", "room-ktx"]
testing = ["junit-jupiter", "mockk", "turbine", "truth", "kotlinx-coroutines-test"]

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
room = { id = "androidx.room", version.ref = "room" }
```

### ✅ 검증
- [ ] `gradle/libs.versions.toml` 파일이 생성되었다
- [ ] TOML 문법 오류 없이 저장되었다 (IDE에서 빨간 줄 없음)
- [ ] `[versions]`, `[libraries]`, `[bundles]`, `[plugins]` 4개 섹션이 모두 있다

---

## Step 2 — Kotlin DSL 빌드 파일 전환

### 목표
> 루트 `settings.gradle`와 `build.gradle`을 Groovy에서 Kotlin DSL (`.kts`)로 전환한다.

### 작업 내용

현재 프로젝트의 빌드 파일은 Groovy DSL로 작성되어 있습니다.

```groovy
// 현재 settings.gradle — Groovy DSL
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "FeetballFootball"
include ':app'
```

```groovy
// 현재 build.gradle — Groovy DSL, buildscript 블록 사용
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.13.2'
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0'
    }
}
```

Kotlin DSL로 전환하면 **타입 안전성**, **IDE 자동완성**, **Version Catalog와의 통합**이 가능해집니다. 또한 `buildscript` 블록 대신 `plugins {}` 블록을 사용하는 최신 Gradle 패턴으로 전환합니다.

> ⚠️ **주의:** 기존 `settings.gradle`과 `build.gradle` 파일은 **반드시 삭제**해야 합니다. Groovy와 Kotlin DSL 파일이 동시에 존재하면 Gradle이 혼란을 일으킵니다.

#### 1. 기존 파일 삭제

```bash
# 프로젝트 루트에서 실행
rm settings.gradle
rm build.gradle
```

#### 2. `settings.gradle.kts` 생성

**파일 경로:** 프로젝트 루트 `/settings.gradle.kts`

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FeetballFootball"

include(":app")
// core 모듈 (Step 6에서 추가)
// feature 모듈 (Step 6에서 추가)
```

> 💡 **Tip:** `includeBuild("build-logic")`은 Step 5에서 만들 Convention Plugins 모듈을 포함시키는 선언입니다. 아직 `build-logic/` 디렉토리가 없으므로, Step 5를 완료하기 전까지 이 줄을 주석 처리해두면 중간 검증이 가능합니다.

#### 3. `build.gradle.kts` 생성 (프로젝트 루트)

**파일 경로:** 프로젝트 루트 `/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}
```

> 💡 **Tip:** `apply false`는 "이 플러그인을 루트 프로젝트에는 적용하지 않지만, 서브모듈에서 사용할 수 있도록 클래스패스에 올려둔다"는 의미입니다. Version Catalog의 `[plugins]`에 선언한 것을 여기서 `alias()`로 참조합니다.

### ✅ 검증
- [ ] `settings.gradle` (Groovy) 파일이 삭제되었다
- [ ] `build.gradle` (Groovy) 파일이 삭제되었다
- [ ] `settings.gradle.kts` 파일이 루트에 생성되었다
- [ ] `build.gradle.kts` 파일이 루트에 생성되었다

---

## Step 3 — app/build.gradle.kts 전환

### 목표
> `app` 모듈의 빌드 파일을 Kotlin DSL로 전환하고, Compose + Hilt 의존성을 추가한다.

### 작업 내용

현재 `app/build.gradle`은 Groovy DSL로 작성되어 있으며, 여러 레거시 의존성을 포함하고 있습니다.

```groovy
// 현재 상태 — 제거 대상 의존성들
implementation 'com.squareup.picasso:picasso:2.71828'        // → Coil 3으로 교체
implementation 'com.google.code.gson:gson:2.9.0'            // → kotlinx.serialization으로 교체
implementation 'com.squareup.retrofit2:converter-gson:2.9.0' // → converter-kotlinx-serialization으로 교체
implementation 'com.jakewharton.threetenabp:threetenabp:1.3.0' // → java.time (API 26+)으로 교체
```

> ⚠️ **주의:** 기존 `app/build.gradle` 파일을 **삭제**한 후 새로 `app/build.gradle.kts`를 생성합니다.

```bash
rm app/build.gradle
```

**파일 경로:** `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.chase1st.feetballfootball"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chase1st.feetballfootball"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "2.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // 모듈 의존성 (Step 6 이후 추가)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    // Lifecycle
    implementation(libs.bundles.lifecycle)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)           // NavGraph-scoped ViewModel (hiltNavGraphViewModels)
    implementation(libs.hilt.lifecycle.viewmodel.compose)  // hiltViewModel() — 1.3.0부터 신규 아티팩트

    // Navigation
    implementation(libs.androidx.navigation.compose)
}
```

주요 변경점:

| 항목 | 이전 | 이후 |
|------|------|------|
| namespace | `com.example.feetballfootball` | `com.chase1st.feetballfootball` |
| compileSdk | 34 | 36 |
| targetSdk | 34 | 35 |
| JVM target | 1.8 | 17 |
| UI 프레임워크 | ViewBinding | Compose |
| JSON 파서 | GSON | kotlinx.serialization |
| 이미지 로더 | Picasso | Coil 3 (core-designsystem에서) |
| DI | 없음 | Hilt |
| 어노테이션 프로세서 | KAPT | KSP |

> 💡 **Tip:** `namespace` 변경은 리소스 R 클래스 패키지에 영향을 줍니다. 기존 코드를 마이그레이션할 때 `import com.example.feetballfootball.R`을 `import com.chase1st.feetballfootball.R`로 변경해야 합니다.

### ✅ 검증
- [ ] `app/build.gradle` (Groovy) 파일이 삭제되었다
- [ ] `app/build.gradle.kts` 파일이 생성되었다
- [ ] `libs.plugins.*`와 `libs.*` 참조에 IDE 빨간 줄이 없다

---

## Step 4 — Gradle/Kotlin/AGP 업그레이드

### 목표
> Gradle 8.13 → 9.4.1, Kotlin 2.1.0 → 2.3.20, AGP 8.13.2 → 9.1.1로 업그레이드한다.

### 작업 내용

현재 Gradle wrapper 설정:

```properties
# 현재 상태 (gradle/wrapper/gradle-wrapper.properties)
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip
```

#### 1. `gradle/wrapper/gradle-wrapper.properties` 수정

**파일 경로:** `gradle/wrapper/gradle-wrapper.properties`

`distributionUrl` 줄을 다음으로 변경합니다.

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
```

> ⚠️ **주의:** 나머지 줄(`distributionBase`, `distributionPath`, `zipStorePath`, `zipStoreBase`)은 그대로 유지합니다.

#### 2. `gradle.properties` 수정

**파일 경로:** 프로젝트 루트 `/gradle.properties`

현재 파일에는 레거시 설정(`android.enableJetifier=true`, `android.nonTransitiveRClass=false` 등)이 포함되어 있습니다. 전체 내용을 다음으로 교체합니다.

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

주요 변경점:
- `android.enableJetifier=true` **제거** — Jetifier는 Support Library를 AndroidX로 변환하는 도구인데, 이제 모든 라이브러리가 AndroidX를 사용하므로 불필요합니다.
- `android.nonTransitiveRClass=false` → `true`로 변경 — 각 모듈이 자신의 리소스만 R 클래스에 포함하게 되어 빌드 속도가 향상됩니다.
- `android.defaults.buildfeatures.buildconfig=true` **제거** — 각 모듈의 `build.gradle.kts`에서 개별 설정합니다.
- `android.nonFinalResIds=false` **제거** — 불필요한 레거시 설정입니다.

> 💡 **Tip:** Kotlin 2.3.20과 AGP 9.1.1 버전은 Step 1에서 만든 `libs.versions.toml`의 `[versions]` 섹션에 이미 선언되어 있습니다. Gradle wrapper만 별도로 properties 파일에서 관리됩니다. AGP 9.1.x는 Gradle 9.1 이상을 요구하며, 여기서는 최신 패치인 9.4.1을 사용합니다.

### ✅ 검증
- [ ] `./gradlew --version` 실행 시 `Gradle 9.4.1` 출력 확인
- [ ] `gradle.properties`에서 `enableJetifier` 관련 줄이 제거되었다
- [ ] `android.nonTransitiveRClass=true`로 설정되었다

---

## Step 5 — Convention Plugins

### 목표
> `build-logic` 모듈을 생성하여, 반복되는 빌드 설정을 Convention Plugin으로 추출한다.

### 작업 내용

멀티 모듈 프로젝트에서 각 모듈마다 `compileSdk`, `minSdk`, `jvmTarget` 등을 반복 선언하면 유지보수가 어렵습니다. Convention Plugin을 사용하면 **공통 빌드 설정을 한 곳에서 정의**하고 각 모듈에서 `id("feetball.android.library")` 한 줄로 적용할 수 있습니다.

#### 디렉토리 구조

다음 디렉토리와 파일을 **프로젝트 루트** 기준으로 생성합니다.

```
build-logic/
├── convention/
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       ├── Extensions.kt
│       ├── AndroidApplicationConventionPlugin.kt
│       ├── AndroidLibraryConventionPlugin.kt
│       ├── AndroidComposeConventionPlugin.kt
│       ├── AndroidHiltConventionPlugin.kt
│       └── AndroidTestConventionPlugin.kt
└── settings.gradle.kts
```

#### 1. `build-logic/settings.gradle.kts`

**파일 경로:** `build-logic/settings.gradle.kts`

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
```

> 💡 **Tip:** `from(files("../gradle/libs.versions.toml"))`는 루트 프로젝트의 Version Catalog를 build-logic에서도 참조할 수 있게 합니다. 이 덕분에 Convention Plugin 내에서 `libs.android.gradlePlugin` 같은 참조가 가능합니다.

#### 2. `build-logic/convention/build.gradle.kts`

**파일 경로:** `build-logic/convention/build.gradle.kts`

```kotlin
plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "feetball.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "feetball.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "feetball.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "feetball.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidTest") {
            id = "feetball.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
    }
}
```

> 💡 **Tip:** `compileOnly`를 사용하는 이유는 이 의존성들이 **빌드 시에만** 필요하고 런타임에 포함될 필요가 없기 때문입니다. AGP, Kotlin, Compose 플러그인의 Gradle API를 Convention Plugin 코드에서 호출하기 위해 필요합니다.

#### 3. `Extensions.kt` — Version Catalog 접근 유틸리티

Convention Plugin 코드 내에서 `libs`를 사용하려면 확장 프로퍼티가 필요합니다.

**파일 경로:** `build-logic/convention/src/main/kotlin/Extensions.kt`

```kotlin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
```

#### 4. `AndroidLibraryConventionPlugin.kt`

모든 library 모듈(core/feature)에 공통 적용되는 설정입니다.

**파일 경로:** `build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt`

```kotlin
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<LibraryExtension> {
                compileSdk = 36

                defaultConfig {
                    minSdk = 26
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
        }
    }
}
```

#### 5. `AndroidComposeConventionPlugin.kt`

Compose를 사용하는 모듈에 적용합니다 (core-designsystem, feature 모듈 등).

**파일 경로:** `build-logic/convention/src/main/kotlin/AndroidComposeConventionPlugin.kt`

```kotlin
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            extensions.configure<LibraryExtension> {
                buildFeatures {
                    compose = true
                }
            }

            dependencies {
                val bom = libs.findLibrary("compose-bom").get()
                add("implementation", platform(bom))
                add("implementation", libs.findBundle("compose").get())
                add("debugImplementation", libs.findBundle("compose-debug").get())
            }
        }
    }
}
```

#### 6. `AndroidHiltConventionPlugin.kt`

Hilt DI를 사용하는 모듈에 적용합니다.

**파일 경로:** `build-logic/convention/src/main/kotlin/AndroidHiltConventionPlugin.kt`

```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.dagger.hilt.android")
                apply("com.google.devtools.ksp")
            }

            dependencies {
                add("implementation", libs.findLibrary("hilt-android").get())
                add("ksp", libs.findLibrary("hilt-compiler").get())
            }
        }
    }
}
```

#### 7. `AndroidTestConventionPlugin.kt`

테스트 의존성을 일괄 적용합니다.

**파일 경로:** `build-logic/convention/src/main/kotlin/AndroidTestConventionPlugin.kt`

```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            dependencies {
                add("testImplementation", libs.findBundle("testing").get())
            }
        }
    }
}
```

#### 8. `AndroidApplicationConventionPlugin.kt` (선택)

app 모듈에서 사용할 수 있는 Convention Plugin입니다. 현재 Stage에서는 app 모듈이 직접 설정을 선언하고 있으므로 빈 플레이스홀더로 둡니다.

**파일 경로:** `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt`

```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }
        }
    }
}
```

### ✅ 검증
- [ ] `build-logic/` 디렉토리가 생성되었다
- [ ] `build-logic/settings.gradle.kts`가 존재한다
- [ ] `build-logic/convention/build.gradle.kts`가 존재한다
- [ ] `build-logic/convention/src/main/kotlin/` 아래 6개 Kotlin 파일이 있다
- [ ] 루트 `settings.gradle.kts`에 `includeBuild("build-logic")` 라인이 있다

---

## Step 6 — 멀티 모듈 생성

### 목표
> 11개 모듈(core 7 + feature 4)의 디렉토리 구조와 빌드 파일을 생성한다.

### 작업 내용

현재는 `app` 모듈 하나에 67개의 Kotlin 소스 파일이 모두 들어 있습니다. 멀티 모듈로 분리하면:
- **빌드 속도 향상:** 변경된 모듈만 재빌드
- **의존성 방향 강제:** feature 모듈이 다른 feature를 직접 참조할 수 없음
- **테스트 격리:** 각 모듈을 독립적으로 테스트 가능

#### 모듈 구조 개요

```
프로젝트 루트/
├── app/                          (Application 모듈)
├── core/
│   ├── core-common/              (공통 유틸리티: Result, Dispatchers)
│   ├── core-model/               (도메인 모델)
│   ├── core-domain/              (UseCase)
│   ├── core-network/             (Retrofit API)
│   ├── core-database/            (Room DB)
│   ├── core-data/                (Repository 구현)
│   └── core-designsystem/        (Compose 테마, 공통 컴포넌트)
└── feature/
    ├── feature-fixture/          (경기 일정)
    ├── feature-fixture-detail/   (경기 상세)
    ├── feature-league/           (리그/순위)
    └── feature-news/             (뉴스)
```

#### 1. 소스 디렉토리 생성

각 모듈에 소스 디렉토리와 `AndroidManifest.xml`을 생성합니다.

```bash
# core 모듈 디렉토리
mkdir -p core/core-common/src/main/kotlin/com/chase1st/feetballfootball/core/common/
mkdir -p core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/
mkdir -p core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/
mkdir -p core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/
mkdir -p core/core-database/src/main/kotlin/com/chase1st/feetballfootball/core/database/
mkdir -p core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/
mkdir -p core/core-designsystem/src/main/kotlin/com/chase1st/feetballfootball/core/designsystem/

# feature 모듈 디렉토리
mkdir -p feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/
mkdir -p feature/feature-fixture-detail/src/main/kotlin/com/chase1st/feetballfootball/feature/fixturedetail/
mkdir -p feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/
mkdir -p feature/feature-news/src/main/kotlin/com/chase1st/feetballfootball/feature/news/
```

#### 2. 각 모듈에 빈 `AndroidManifest.xml` 생성

모든 library 모듈(`core/*`, `feature/*`)의 `src/main/` 디렉토리에 다음 파일을 생성합니다.

**파일 경로 패턴:** `{모듈}/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

> 💡 **Tip:** AGP library 모듈에서 `AndroidManifest.xml`은 `namespace`를 `build.gradle.kts`에서 지정하는 경우 최소한의 내용만 있으면 됩니다. 하지만 Convention Plugin에서 `namespace`를 설정하지 않았으므로, 각 모듈의 `build.gradle.kts`에서 설정하거나 manifest에 `package` 속성을 추가해야 합니다. 여기서는 Convention Plugin에서 `namespace`를 설정하지 않는 대신, 각 모듈 빌드 파일에서 필요 시 추가합니다.

#### 3. core 모듈 빌드 파일

##### `core/core-common/build.gradle.kts`

```kotlin
plugins {
    id("feetball.android.library")
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
```

##### `core/core-model/build.gradle.kts`

```kotlin
plugins {
    id("feetball.android.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
```

##### `core/core-domain/build.gradle.kts`

```kotlin
plugins {
    id("feetball.android.library")
    id("feetball.android.hilt")
}

dependencies {
    api(project(":core:core-model"))
    implementation(project(":core:core-common"))
    implementation(libs.kotlinx.coroutines.android)
}
```

> 💡 **Tip:** `core-model`은 `api`로 선언합니다. `core-domain`에 의존하는 모듈이 `core-model`의 클래스도 접근할 수 있어야 하기 때문입니다 (UseCase의 반환 타입이 Model 클래스).

##### `core/core-network/build.gradle.kts`

```kotlin
plugins {
    id("feetball.android.library")
    id("feetball.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(libs.bundles.networking)
    implementation(libs.kotlinx.serialization.json)
}
```

##### `core/core-database/build.gradle.kts`

```kotlin
plugins {
    id("feetball.android.library")
    id("feetball.android.hilt")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-model"))
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
```

##### `core/core-data/build.gradle.kts`

```kotlin
plugins {
    id("feetball.android.library")
    id("feetball.android.hilt")
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-model"))
    implementation(project(":core:core-domain"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-database"))
    implementation(libs.kotlinx.coroutines.android)
}
```

##### `core/core-designsystem/build.gradle.kts`

```kotlin
plugins {
    id("feetball.android.library")
    id("feetball.android.compose")
}

dependencies {
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
```

#### 4. feature 모듈 빌드 파일

모든 feature 모듈은 동일한 패턴을 따릅니다.

##### `feature/feature-fixture/build.gradle.kts`

```kotlin
plugins {
    id("feetball.android.library")
    id("feetball.android.compose")
    id("feetball.android.hilt")
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-domain"))
    implementation(project(":core:core-designsystem"))
    implementation(libs.bundles.lifecycle)
    implementation(libs.hilt.navigation.compose)           // NavGraph-scoped ViewModel (hiltNavGraphViewModels)
    implementation(libs.hilt.lifecycle.viewmodel.compose)  // hiltViewModel() — 1.3.0부터 신규 아티팩트
}
```

##### `feature/feature-fixture-detail/build.gradle.kts`

```kotlin
plugins {
    id("feetball.android.library")
    id("feetball.android.compose")
    id("feetball.android.hilt")
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-domain"))
    implementation(project(":core:core-designsystem"))
    implementation(libs.bundles.lifecycle)
    implementation(libs.hilt.navigation.compose)           // NavGraph-scoped ViewModel (hiltNavGraphViewModels)
    implementation(libs.hilt.lifecycle.viewmodel.compose)  // hiltViewModel() — 1.3.0부터 신규 아티팩트
}
```

##### `feature/feature-league/build.gradle.kts`

```kotlin
plugins {
    id("feetball.android.library")
    id("feetball.android.compose")
    id("feetball.android.hilt")
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-domain"))
    implementation(project(":core:core-designsystem"))
    implementation(libs.bundles.lifecycle)
    implementation(libs.hilt.navigation.compose)           // NavGraph-scoped ViewModel (hiltNavGraphViewModels)
    implementation(libs.hilt.lifecycle.viewmodel.compose)  // hiltViewModel() — 1.3.0부터 신규 아티팩트
}
```

##### `feature/feature-news/build.gradle.kts`

```kotlin
plugins {
    id("feetball.android.library")
    id("feetball.android.compose")
    id("feetball.android.hilt")
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-domain"))
    implementation(project(":core:core-designsystem"))
    implementation(libs.bundles.lifecycle)
    implementation(libs.hilt.navigation.compose)           // NavGraph-scoped ViewModel (hiltNavGraphViewModels)
    implementation(libs.hilt.lifecycle.viewmodel.compose)  // hiltViewModel() — 1.3.0부터 신규 아티팩트
}
```

#### 5. `settings.gradle.kts` 업데이트

**파일 경로:** 프로젝트 루트 `/settings.gradle.kts`

기존 주석 처리된 부분에 모듈을 추가합니다.

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FeetballFootball"

include(":app")

// core 모듈
include(":core:core-common")
include(":core:core-model")
include(":core:core-domain")
include(":core:core-network")
include(":core:core-database")
include(":core:core-data")
include(":core:core-designsystem")

// feature 모듈
include(":feature:feature-fixture")
include(":feature:feature-fixture-detail")
include(":feature:feature-league")
include(":feature:feature-news")
```

#### 6. `app/build.gradle.kts` 모듈 의존성 추가

`app/build.gradle.kts`의 `dependencies` 블록 상단에 다음을 추가합니다.

```kotlin
dependencies {
    // Feature 모듈
    implementation(project(":feature:feature-fixture"))
    implementation(project(":feature:feature-fixture-detail"))
    implementation(project(":feature:feature-league"))
    implementation(project(":feature:feature-news"))

    // Core 모듈 (app에서 직접 필요한 것만)
    implementation(project(":core:core-designsystem"))
    implementation(project(":core:core-data"))   // Hilt Repository 바인딩
    implementation(project(":core:core-model"))

    // ... 기존 의존성 (AndroidX, Compose, Lifecycle, Hilt, Navigation)
}
```

### ✅ 검증
- [ ] 11개 모듈 디렉토리가 모두 생성되었다
- [ ] 각 모듈에 `build.gradle.kts`와 `src/main/AndroidManifest.xml`이 있다
- [ ] `settings.gradle.kts`에 11개 `include()` 선언이 있다
- [ ] `./gradlew assembleDebug` — 11개 빈 모듈 포함 빌드 성공

---

## Step 7 — core-common 기반 코드

### 목표
> 전체 프로젝트에서 공유하는 `Result` 래퍼와 Coroutine Dispatcher 주입 코드를 `core-common`에 작성한다.

### 작업 내용

#### 1. `Result.kt`

네트워크 요청이나 DB 조회 결과를 `Loading → Success | Error`로 래핑하는 sealed interface입니다. Flow 확장함수 `asResult()`를 제공하여 ViewModel에서 편리하게 사용할 수 있습니다.

**파일 경로:** `core/core-common/src/main/kotlin/com/chase1st/feetballfootball/core/common/result/Result.kt`

```kotlin
package com.chase1st.feetballfootball.core.common.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable) : Result<Nothing>
    data object Loading : Result<Nothing>
}

fun <T> Flow<T>.asResult(): Flow<Result<T>> = this
    .map<T, Result<T>> { Result.Success(it) }
    .onStart { emit(Result.Loading) }
    .catch { emit(Result.Error(it)) }
```

> 💡 **Tip:** 현재 프로젝트에서는 `thread {}` 블록으로 비동기 처리를 하고 있습니다. 이 `Result` 타입을 도입하면 `Flow` 기반의 일관된 비동기 패턴을 사용할 수 있습니다.

#### 2. Dispatcher Qualifiers

테스트에서 Dispatcher를 교체하기 위해 Hilt Qualifier를 정의합니다.

**파일 경로:** `core/core-common/src/main/kotlin/com/chase1st/feetballfootball/core/common/dispatcher/Dispatchers.kt`

```kotlin
package com.chase1st.feetballfootball.core.common.dispatcher

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
```

#### 3. DispatcherModule

Hilt가 Dispatcher를 주입할 수 있도록 Module을 정의합니다.

**파일 경로:** `core/core-common/src/main/kotlin/com/chase1st/feetballfootball/core/common/dispatcher/DispatcherModule.kt`

```kotlin
package com.chase1st.feetballfootball.core.common.dispatcher

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @IoDispatcher
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @DefaultDispatcher
    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @MainDispatcher
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
```

> ⚠️ **주의:** `core-common` 모듈에서 Hilt를 사용하므로, `core-common/build.gradle.kts`에 Hilt 의존성을 추가해야 합니다. Convention Plugin `feetball.android.hilt`를 적용하거나, 직접 추가하세요.

`core/core-common/build.gradle.kts`를 다음과 같이 수정:

```kotlin
plugins {
    id("feetball.android.library")
    id("feetball.android.hilt")  // DispatcherModule에서 Hilt 사용
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
```

### ✅ 검증
- [ ] `Result.kt` 파일이 올바른 경로에 생성되었다
- [ ] `Dispatchers.kt` 파일에 3개의 Qualifier 어노테이션이 있다
- [ ] `DispatcherModule.kt`이 `@Module`, `@InstallIn(SingletonComponent::class)`로 선언되어 있다
- [ ] `core-common/build.gradle.kts`에 Hilt 플러그인이 적용되어 있다

---

## Step 8 — core-network 기반 코드

### 목표
> Retrofit + OkHttp 네트워크 레이어를 `core-network` 모듈에 구성한다.

### 작업 내용

현재 프로젝트에서 네트워크 호출은 `FootballDataFetchr` 유틸리티 클래스에서 직접 Retrofit을 생성하고 API 키를 하드코딩하고 있습니다. 이를 Hilt Module 기반의 DI로 전환합니다.

주요 변경점:
- Retrofit 2.9.0 → 3.0.0
- GSON → kotlinx.serialization
- OkHttp 3.x → 5.x
- API-Sports → SofaScore API (Bearer Token 기반 인증)

#### 1. `FootballApiService.kt`

엔드포인트는 각 Slice(Stage)에서 추가합니다. 우선 빈 인터페이스로 생성합니다.

**파일 경로:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/api/FootballApiService.kt`

```kotlin
package com.chase1st.feetballfootball.core.network.api

interface FootballApiService {
    // 엔드포인트는 각 Slice에서 추가
}
```

#### 2. `NetworkModule.kt`

**파일 경로:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/di/NetworkModule.kt`

```kotlin
package com.chase1st.feetballfootball.core.network.di

import com.chase1st.feetballfootball.core.network.api.FootballApiService
import com.chase1st.feetballfootball.core.network.interceptor.AuthInterceptor
import com.chase1st.feetballfootball.core.network.token.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.sofascore.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideFootballApiService(retrofit: Retrofit): FootballApiService =
        retrofit.create(FootballApiService::class.java)
}
```

> 💡 **Tip:** SofaScore API는 Bearer Token 기반 인증을 사용합니다. `AuthInterceptor`가 non-GET/HEAD 요청에 `Authorization: Bearer {token}` 헤더를 자동으로 추가합니다. `ignoreUnknownKeys = true`는 API 응답에 새로운 필드가 추가되어도 앱이 크래시하지 않도록 합니다. `coerceInputValues = true`는 null 값이 non-null 필드에 올 때 기본값으로 대체합니다.

#### 3. `AuthInterceptor.kt`

SofaScore API는 Bearer Token 기반 인증을 사용합니다. non-GET/HEAD 요청에 `Authorization: Bearer {token}` 헤더를 자동으로 추가하고, 서버가 `X-Token-Refresh` 헤더로 토큰 갱신을 요청하면 자동으로 갱신합니다.

**파일 경로:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/interceptor/AuthInterceptor.kt`

```kotlin
package com.chase1st.feetballfootball.core.network.interceptor

import com.chase1st.feetballfootball.core.network.token.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {

    companion object {
        private val AUTH_REQUIRED_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val request = if (original.method in AUTH_REQUIRED_METHODS) {
            val token = tokenManager.getToken()
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        val response = chain.proceed(request)

        // 서버가 토큰 갱신을 요청하는 경우
        if (response.header("X-Token-Refresh") != null) {
            tokenManager.refreshToken()
        }

        return response
    }
}
```

Bearer Token 인증 흐름:
1. 앱 시작 시 `POST api/v1/token/init`으로 초기 토큰 발급
2. non-GET/HEAD 요청에 `Authorization: Bearer {token}` 헤더 추가
3. 서버가 `X-Token-Refresh` 응답 헤더를 반환하면 `POST api/v1/token/refresh`로 토큰 갱신
4. GET/HEAD 요청은 인증 없이 접근 가능

> 💡 **Tip:** SofaScore API는 GET 요청(데이터 조회)에는 인증이 불필요합니다. POST/PUT 등 상태 변경 요청에만 Bearer Token이 필요합니다. 별도의 CAPTCHA나 봇 검증도 필요하지 않습니다.

### ✅ 검증
- [ ] `FootballApiService.kt`가 올바른 패키지에 생성되었다
- [ ] `NetworkModule.kt`이 Hilt `@Module`로 선언되어 있다
- [ ] `provideJson()`, `provideOkHttpClient()`, `provideRetrofit()`, `provideFootballApiService()` 4개 `@Provides` 함수가 있다
- [ ] Base URL이 `https://api.sofascore.com/`으로 설정되어 있다
- [ ] `AuthInterceptor.kt`가 `core/core-network`의 `interceptor` 패키지에 생성되었다
- [ ] `OkHttpClient`에 `AuthInterceptor`가 추가되어 있다

---

## Step 9 — Bearer Token 관리 설계

### 목표
> SofaScore API의 Bearer Token 발급/갱신 시스템을 설계한다.

### 작업 내용

SofaScore API는 Bearer Token 기반 인증을 사용합니다. GET/HEAD 요청은 인증 없이 접근 가능하지만, POST/PUT/PATCH/DELETE 등 상태 변경 요청에는 `Authorization: Bearer {token}` 헤더가 필요합니다. 토큰은 앱 시작 시 발급받고, 서버가 `X-Token-Refresh` 응답 헤더를 반환하면 자동으로 갱신합니다.

> 💡 **Tip:** SofaScore API의 토큰 관리는 두 가지 엔드포인트를 사용합니다. `POST api/v1/token/init`으로 초기 토큰을 발급받고, `POST api/v1/token/refresh`로 만료된 토큰을 갱신합니다. 별도의 CAPTCHA나 봇 검증은 필요하지 않습니다.

#### 1. `TokenManager.kt` — 토큰 상태 관리 (Singleton)

Bearer Token의 발급, 저장, 갱신을 담당합니다. 앱 시작 시 `initToken()`을 호출하여 초기 토큰을 발급받고, 서버 요청에 따라 자동으로 갱신합니다.

**파일 경로:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/token/TokenManager.kt`

```kotlin
package com.chase1st.feetballfootball.core.network.token

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor() {

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    // 토큰 전용 OkHttpClient (AuthInterceptor 없이 — 순환 의존 방지)
    private val tokenClient = OkHttpClient.Builder().build()

    companion object {
        private const val BASE_URL = "https://api.sofascore.com/api/v1"
    }

    fun getToken(): String? = _token.value

    /**
     * 앱 시작 시 호출하여 초기 토큰을 발급받는다.
     * POST api/v1/token/init
     */
    fun initToken() {
        runBlocking {
            val request = Request.Builder()
                .url("$BASE_URL/token/init")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()

            tokenClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    // 응답에서 토큰 추출 (실제 응답 형식에 맞게 파싱)
                    _token.value = parseToken(body)
                }
            }
        }
    }

    /**
     * 서버가 X-Token-Refresh 헤더를 반환한 경우 호출하여 토큰을 갱신한다.
     * POST api/v1/token/refresh
     */
    fun refreshToken() {
        runBlocking {
            val currentToken = _token.value ?: return@runBlocking
            val request = Request.Builder()
                .url("$BASE_URL/token/refresh")
                .header("Authorization", "Bearer $currentToken")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()

            tokenClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    _token.value = parseToken(body)
                }
            }
        }
    }

    fun isAuthenticated(): Boolean = _token.value != null

    private fun parseToken(body: String?): String? {
        // TODO: 실제 응답 JSON 형식에 맞게 파싱 구현
        return body?.let {
            try {
                Json.parseToJsonElement(it)
                    .toString()  // 실제 토큰 필드명에 맞게 수정
            } catch (e: Exception) {
                null
            }
        }
    }
}
```

> 💡 **Tip:** `TokenManager`는 `@Singleton`으로 선언되어 앱 전체에서 하나의 인스턴스를 공유합니다. 토큰 발급용 `tokenClient`는 `AuthInterceptor`를 포함하지 않는 별도의 `OkHttpClient`를 사용하여 순환 의존을 방지합니다.

#### 2. `TokenInitializer.kt` — 앱 시작 시 토큰 초기화

앱 시작 시 자동으로 토큰을 초기화하는 Initializer입니다. `FeetballApp`의 `onCreate()`에서 호출합니다.

**파일 경로:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/token/TokenInitializer.kt`

```kotlin
package com.chase1st.feetballfootball.core.network.token

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenInitializer @Inject constructor(
    private val tokenManager: TokenManager,
) {
    fun initialize() {
        tokenManager.initToken()
    }
}
```

#### 3. `FeetballApp.kt` 업데이트 — 토큰 초기화 호출

Step 10에서 생성하는 `FeetballApp.kt`에 `TokenInitializer`를 주입하고 `onCreate()`에서 호출합니다.

```kotlin
@HiltAndroidApp
class FeetballApp : Application() {

    @Inject
    lateinit var tokenInitializer: TokenInitializer

    override fun onCreate() {
        super.onCreate()
        tokenInitializer.initialize()
    }
}
```

> 💡 **Tip:** Hilt의 `@Inject` 필드 주입은 `super.onCreate()` 호출 시점에 완료됩니다. 따라서 `tokenInitializer.initialize()`를 `super.onCreate()` 이후에 호출해야 합니다. 토큰 발급은 네트워크 요청이므로 실제 프로덕션에서는 비동기로 처리하는 것이 좋습니다.

### ✅ 검증
- [ ] `TokenManager.kt`가 `core-network`의 `token` 패키지에 생성되었다
- [ ] `TokenInitializer.kt`가 `@Singleton`으로 선언되어 있다
- [ ] `TokenManager`가 `initToken()`과 `refreshToken()` 메서드를 포함한다
- [ ] `FeetballApp`의 `onCreate()`에서 `tokenInitializer.initialize()`가 호출된다

---

## Step 10 — Hilt + MainActivity 기반

### 목표
> Hilt Application 클래스와 Compose 기반 MainActivity를 생성한다.

### 작업 내용

현재 `MainActivity`는 `AppCompatActivity`를 상속하고 ViewBinding을 사용하는 Fragment 호스트입니다. 새 버전에서는 `ComponentActivity`를 상속하고 `setContent {}`로 Compose UI를 렌더링합니다.

> ⚠️ **주의:** 이 시점에서 기존 `MainActivity.kt`와 관련 Fragment 파일들은 아직 삭제하지 않습니다. 기존 코드는 Stage 2 이후의 마이그레이션 과정에서 점진적으로 교체됩니다. 새 파일은 별도 경로에 생성합니다.

#### 1. `FeetballApp.kt` — Hilt Application 클래스

**파일 경로:** `app/src/main/kotlin/com/chase1st/feetballfootball/FeetballApp.kt`

```kotlin
package com.chase1st.feetballfootball

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FeetballApp : Application()
```

> 💡 **Tip:** `@HiltAndroidApp`은 Hilt의 코드 생성 진입점입니다. 이 어노테이션이 없으면 `@AndroidEntryPoint`가 붙은 Activity/Fragment에서 DI가 작동하지 않습니다.

#### 2. `AndroidManifest.xml` 업데이트

**파일 경로:** `app/src/main/AndroidManifest.xml`

`<application>` 태그에 `android:name` 속성을 추가합니다.

```xml
<application
    android:name=".FeetballApp"
    android:label="@string/app_name"
    android:theme="@style/Theme.FeetballFootball"
    ...>
    <activity
        android:name=".MainActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

#### 3. `MainActivity.kt` (새 Compose 버전)

**파일 경로:** `app/src/main/kotlin/com/chase1st/feetballfootball/MainActivity.kt`

```kotlin
package com.chase1st.feetballfootball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chase1st.feetballfootball.core.designsystem.theme.FeetballTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FeetballTheme {
                // 임시: 빈 화면 (Stage 2에서 FeetballApp() 호출로 교체)
                androidx.compose.material3.Text("Feetball Football v2.0")
            }
        }
    }
}
```

주요 변경점:
- `AppCompatActivity` → `ComponentActivity` (Compose에 최적화)
- `setContentView(R.layout.activity_main)` → `setContent { }` (Compose)
- ViewBinding 제거
- `@AndroidEntryPoint` 추가 (Hilt DI)
- `enableEdgeToEdge()` 추가 (시스템 바 투명 처리)

### ✅ 검증
- [ ] `FeetballApp.kt`에 `@HiltAndroidApp` 어노테이션이 있다
- [ ] `AndroidManifest.xml`의 `<application>` 태그에 `android:name=".FeetballApp"`이 설정되어 있다
- [ ] `MainActivity.kt`에 `@AndroidEntryPoint` 어노테이션이 있다
- [ ] `setContent { FeetballTheme { ... } }` 구조로 Compose UI가 설정되어 있다
- [ ] 빌드 후 실행 시 "Feetball Football v2.0" 텍스트가 표시된다

---

## Step 11 — core-designsystem

### 목표
> Material 3 테마 시스템(Color, Typography, Theme)과 공통 Compose 컴포넌트를 `core-designsystem` 모듈에 생성한다.

### 작업 내용

현재 프로젝트는 XML 기반 테마(`styles.xml`, `colors.xml`)를 사용하고 있습니다. Compose로 전환하면서 코드 기반의 테마 시스템으로 변경합니다.

#### 1. `Color.kt`

**파일 경로:** `core/core-designsystem/src/main/kotlin/com/chase1st/feetballfootball/core/designsystem/theme/Color.kt`

```kotlin
package com.chase1st.feetballfootball.core.designsystem.theme

import androidx.compose.ui.graphics.Color

object FeetballColors {
    val Primary = Color(0xFFF44336)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFFFDAD5)
    val Surface = Color(0xFFFFFBFF)
    val SurfaceVariant = Color(0xFFF5DDDA)

    // 축구 전용 색상
    val StadiumGreen = Color(0xFF00935C)
    val FormationGreen = Color(0xFFAFF1CE)
    val RatingGood = Color(0xFF4CAF50)     // ≥ 7.0
    val RatingAverage = Color(0xFFFF9800)  // < 7.0
    val YellowCard = Color(0xFFFFEB3B)
    val RedCard = Color(0xFFF44336)
    val SubstitutionIn = Color(0xFF4CAF50)
    val SubstitutionOut = Color(0xFFF44336)
    val GoalGold = Color(0xFFFFD700)
}
```

> 💡 **Tip:** 축구 전용 색상(`StadiumGreen`, `YellowCard` 등)은 Material 3 ColorScheme에 포함되지 않지만, 도메인 특화 UI에서 직접 참조합니다. `FeetballColors.YellowCard`처럼 사용합니다.

#### 2. `Type.kt`

**파일 경로:** `core/core-designsystem/src/main/kotlin/com/chase1st/feetballfootball/core/designsystem/theme/Type.kt`

```kotlin
package com.chase1st.feetballfootball.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val FeetballTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp),
)
```

#### 3. `Theme.kt`

**파일 경로:** `core/core-designsystem/src/main/kotlin/com/chase1st/feetballfootball/core/designsystem/theme/Theme.kt`

```kotlin
package com.chase1st.feetballfootball.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = FeetballColors.Primary,
    onPrimary = FeetballColors.OnPrimary,
    primaryContainer = FeetballColors.PrimaryContainer,
    surface = FeetballColors.Surface,
    surfaceVariant = FeetballColors.SurfaceVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = FeetballColors.Primary,
    onPrimary = FeetballColors.OnPrimary,
)

@Composable
fun FeetballTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = FeetballTypography,
        content = content,
    )
}
```

> 💡 **Tip:** `FeetballTheme`은 Step 10의 `MainActivity.kt`에서 이미 사용하고 있습니다. `core-designsystem` 모듈에 이 파일이 없으면 빌드가 실패합니다.

#### 4. 공통 컴포넌트 — `TeamLogo.kt`

**파일 경로:** `core/core-designsystem/src/main/kotlin/com/chase1st/feetballfootball/core/designsystem/component/TeamLogo.kt`

```kotlin
package com.chase1st.feetballfootball.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun TeamLogo(
    logoUrl: String,
    teamName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    AsyncImage(
        model = logoUrl,
        contentDescription = teamName,
        modifier = modifier.size(size),
    )
}
```

> 💡 **Tip:** 기존에 Picasso로 로고를 로드하던 코드를 Coil 3의 `AsyncImage`로 대체합니다. Coil 3은 Compose에 최적화되어 있으며 `AsyncImage`로 한 줄에 이미지 로딩이 가능합니다.

#### 5. 공통 컴포넌트 — `LoadingIndicator.kt`

**파일 경로:** `core/core-designsystem/src/main/kotlin/com/chase1st/feetballfootball/core/designsystem/component/LoadingIndicator.kt`

```kotlin
package com.chase1st.feetballfootball.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun FeetballLoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
```

#### 6. 공통 컴포넌트 — `ErrorContent.kt`

**파일 경로:** `core/core-designsystem/src/main/kotlin/com/chase1st/feetballfootball/core/designsystem/component/ErrorContent.kt`

```kotlin
package com.chase1st.feetballfootball.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("다시 시도")
        }
    }
}
```

### ✅ 검증
- [ ] `theme/` 디렉토리에 `Color.kt`, `Type.kt`, `Theme.kt` 3개 파일이 있다
- [ ] `component/` 디렉토리에 `TeamLogo.kt`, `LoadingIndicator.kt`, `ErrorContent.kt` 3개 파일이 있다
- [ ] `FeetballTheme` Composable이 `darkTheme` 파라미터를 지원한다
- [ ] `TeamLogo`가 Coil 3의 `AsyncImage`를 사용한다

---

## Step 12 — core-database 기반

### 목표
> Room Database와 Hilt Module을 `core-database` 모듈에 생성한다.

### 작업 내용

현재 프로젝트에는 로컬 DB가 없습니다. Room을 도입하면 오프라인 캐싱이 가능해지고, Single Source of Truth 패턴을 적용할 수 있습니다.

#### 1. `FeetballDatabase.kt`

Entity는 아직 없으므로 빈 상태로 생성합니다. 이후 Slice에서 Entity와 DAO를 추가합니다.

**파일 경로:** `core/core-database/src/main/kotlin/com/chase1st/feetballfootball/core/database/FeetballDatabase.kt`

```kotlin
package com.chase1st.feetballfootball.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [], // Slice에서 Entity 추가
    version = 1,
    exportSchema = true,
)
abstract class FeetballDatabase : RoomDatabase()
```

> ⚠️ **주의:** `entities = []`(빈 배열)인 상태에서 Room이 빌드 에러를 발생시킬 수 있습니다. 이 경우 임시 빈 Entity를 추가하거나, 빌드 시점에 `core-database` 모듈만 임시 제외할 수 있습니다.

#### 2. `DatabaseModule.kt`

**파일 경로:** `core/core-database/src/main/kotlin/com/chase1st/feetballfootball/core/database/di/DatabaseModule.kt`

```kotlin
package com.chase1st.feetballfootball.core.database.di

import android.content.Context
import androidx.room.Room
import com.chase1st.feetballfootball.core.database.FeetballDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FeetballDatabase =
        Room.databaseBuilder(
            context,
            FeetballDatabase::class.java,
            "feetball.db",
        ).build()
}
```

> 💡 **Tip:** `@ApplicationContext`는 Hilt가 자동으로 제공하는 Application Context입니다. `Room.databaseBuilder`에는 Application Context를 사용해야 합니다 (Activity Context를 사용하면 메모리 누수 위험).

### ✅ 검증
- [ ] `FeetballDatabase.kt`가 `@Database` 어노테이션과 함께 생성되었다
- [ ] `DatabaseModule.kt`이 Hilt `@Module`로 선언되어 있다
- [ ] 데이터베이스 이름이 `"feetball.db"`로 설정되어 있다
- [ ] `exportSchema = true`로 스키마 내보내기가 활성화되어 있다

---

## Stage 1 최종 빌드 검증

모든 Step을 완료한 후, 다음 명령으로 전체 빌드를 검증합니다.

```bash
./gradlew assembleDebug
```

> ⚠️ **주의:** 첫 빌드는 Gradle 9.4.1 다운로드, 의존성 다운로드 등으로 시간이 오래 걸릴 수 있습니다 (5-10분). 이후 빌드는 Gradle 캐시 덕분에 빨라집니다.

---

## Stage 1 완료!

### 최종 체크리스트

- [ ] 11개 모듈 (core 7 + feature 4) 빌드 성공
- [ ] Kotlin DSL + Version Catalog 기반
- [ ] Convention Plugins 적용
- [ ] Kotlin 2.3.20 / AGP 9.1.1 / Gradle 9.4.1
- [ ] compileSdk 36, targetSdk 35
- [ ] Hilt DI 그래프 런타임 동작 (FeetballApp + MainActivity)
- [ ] Retrofit + OkHttp 설정 완료 (Bearer Token 인증)
- [ ] FeetballTheme + 공통 컴포넌트 존재
- [ ] Room Database 연결 (빈 상태)
- [ ] 빈 Compose 화면 표시

### 커밋

```bash
git checkout -b feature/renewal-stage-1
git add -A
git commit -m "feat: Stage 1 프로젝트 셋업 완료"
```

### 다음 Stage 미리보기

**Stage 2**에서는 이 기반 위에 다음을 진행합니다:
- Navigation 그래프 설정
- 각 feature 모듈에 빈 Screen 생성
- Bottom Navigation 탭 구성 (Fixtures, Leagues, News)
- 기존 Fragment 코드의 점진적 마이그레이션 시작


---

# Stage 2 / Slice 1 — 리그 선택 화면

> ⏱ 예상 소요 시간: 1시간 | 난이도: ★☆☆ | 선행 조건: Stage 1 완료 (11개 모듈 + Convention Plugins + Hilt DI 설정)

---

## 이 Codelab에서 배우는 것

- **core-model** 모듈에 도메인 상수(리그 목록)를 정의하는 방법
- **feature-league** 모듈에서 Hilt ViewModel을 생성하고 Compose UI와 연결하는 방법
- StateFlow를 사용한 정적 데이터 노출 패턴
- Compose `LazyColumn` + `Card`로 리스트 UI를 구성하는 방법
- Stage 1에서 만든 `FeetballTheme`과 `TeamLogo` 공통 컴포넌트 활용

---

## 완성 후 결과물

- 5대 리그(EPL, LaLiga, Serie A, Bundesliga, Ligue 1)가 카드 형태로 표시되는 화면
- 각 카드에 리그 로고, 이름, 국가가 표시됨
- 카드 클릭 시 리그 정보가 콜백으로 전달됨 (Slice 2에서 순위 화면으로 연결)
- Hilt ViewModel 주입이 정상 동작하는 것을 확인

---

## Step 1 — core-model: 리그 상수 정의

### 목표
> SofaScore API 기준 리그/컵 ID를 **core-model 모듈**의 도메인 객체로 정의합니다.

### 작업 내용

API 호출 없이 정적 데이터만 사용하는 이 Slice의 핵심 데이터입니다. `LeagueInfo` data class와 `SupportedLeagues` object를 SofaScore API의 uniqueTournamentId와 로고 URL 형식에 맞게 만들어 앱 전체에서 참조할 수 있게 합니다.

> 💡 **Tip:** `SupportedLeagues.ALL_LEAGUE_IDS`는 Slice 3(경기 일정)에서 SofaScore API 응답 필터링에 사용됩니다. 지금 미리 정의해 두면 나중에 다시 수정할 필요가 없습니다.

**파일 경로:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/LeagueInfo.kt`

```kotlin
// core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/LeagueInfo.kt
package com.chase1st.feetballfootball.core.model

data class LeagueInfo(
    val id: Int,
    val name: String,
    val country: String,
    val logoUrl: String,
)

// SofaScore API 기준 리그 상수 (uniqueTournamentId)
object SupportedLeagues {
    private fun leagueLogoUrl(id: Int) = "https://img.sofascore.com/api/v1/unique-tournament/$id/image"

    val TOP_5_LEAGUES = listOf(
        LeagueInfo(id = 17, name = "Premier League", country = "England", logoUrl = leagueLogoUrl(17)),
        LeagueInfo(id = 8, name = "LaLiga", country = "Spain", logoUrl = leagueLogoUrl(8)),
        LeagueInfo(id = 23, name = "Serie A", country = "Italy", logoUrl = leagueLogoUrl(23)),
        LeagueInfo(id = 35, name = "Bundesliga", country = "Germany", logoUrl = leagueLogoUrl(35)),
        LeagueInfo(id = 34, name = "Ligue 1", country = "France", logoUrl = leagueLogoUrl(34)),
    )

    // SofaScore uniqueTournamentId (경기 일정 필터링용)
    val ALL_LEAGUE_IDS = listOf(
        17, 8, 23, 35, 34,              // 5대 리그
        29, 21,                          // 잉글랜드 컵 (FA Cup, EFL Cup)
        7, 679,                          // UEFA (Champions League, Europa League)
        16, 1,                           // 국제대회 (World Cup, EURO)
    )
}
```

### ✅ 검증
- [ ] `core-model` 모듈이 빌드 에러 없이 컴파일되는지 확인
- [ ] `LeagueInfo` data class에 `id`, `name`, `country`, `logoUrl` 4개 필드가 있는지 확인
- [ ] `SupportedLeagues.TOP_5_LEAGUES`에 5개 리그가 정의되어 있는지 확인
- [ ] `SupportedLeagues.ALL_LEAGUE_IDS`에 11개 ID가 정의되어 있는지 확인

---

## Step 2 — core-model: 시즌 유틸리티

### 목표
> SofaScore API의 시즌 방식에 맞게 **현재 seasonId를 가져오는 유틸리티**를 만듭니다.

### 작업 내용

SofaScore API는 시즌을 정수형 `seasonId`로 관리합니다. `"2025/2026"` 같은 문자열을 직접 계산하는 것이 아니라, `GET api/v1/unique-tournament/{uniqueTournamentId}/seasons` API를 호출하여 현재 seasonId를 가져와야 합니다. 반환되는 목록의 첫 번째 항목이 현재/최신 시즌입니다.

이 Step에서는 seasonId 개념을 정리하고, 실제 API 호출은 Slice 2의 Repository 계층에서 구현합니다. 여기서는 시즌 관련 참조용 상수만 정의합니다.

> ⚠️ **주의:** SofaScore의 seasonId는 리그마다 다르며, 매 시즌 새로 발급됩니다. 따라서 클라이언트에서 직접 계산할 수 없고 반드시 API로 조회해야 합니다.

**파일 경로:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/Season.kt`

```kotlin
// core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/Season.kt
package com.chase1st.feetballfootball.core.model

/**
 * SofaScore 시즌 정보
 *
 * SofaScore API는 시즌을 정수형 seasonId로 관리합니다.
 * 현재 시즌의 seasonId를 얻으려면 아래 API를 호출합니다:
 *   GET https://api.sofascore.com/api/v1/unique-tournament/{uniqueTournamentId}/seasons
 *
 * 응답의 첫 번째 항목이 현재/최신 시즌입니다.
 * seasonId는 리그마다 다르므로 클라이언트에서 직접 계산할 수 없습니다.
 */
object SeasonUtil {
    /**
     * SofaScore 시즌 API 경로 생성
     * @param uniqueTournamentId SofaScore 리그 ID
     * @return 시즌 목록 API 경로 (예: "unique-tournament/17/seasons")
     */
    fun seasonsPath(uniqueTournamentId: Int): String =
        "unique-tournament/$uniqueTournamentId/seasons"
}
```

### ✅ 검증
- [ ] `SeasonUtil.seasonsPath(17)`이 `"unique-tournament/17/seasons"`을 반환하는지 확인
- [ ] SofaScore seasonId가 API에서 조회되어야 한다는 점을 이해했는지 확인
- [ ] `core-model` 모듈 빌드 성공 확인

---

## Step 3 — feature-league: LeagueListViewModel

### 목표
> Hilt가 주입하는 ViewModel을 만들어 **정적 리그 목록을 StateFlow로 노출**합니다.

### 작업 내용

이 ViewModel은 API 호출 없이 `SupportedLeagues.TOP_5_LEAGUES`를 그대로 노출합니다. Compose UI에서 `collectAsStateWithLifecycle()`로 구독하게 됩니다.

> 💡 **Tip:** `@HiltViewModel` + `@Inject constructor()`가 있으면 Hilt가 자동으로 ViewModel을 생성해 줍니다. `hiltViewModel()` Composable 함수로 주입받을 수 있습니다.

**파일 경로:** `feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/LeagueListViewModel.kt`

```kotlin
// feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/LeagueListViewModel.kt
package com.chase1st.feetballfootball.feature.league

import androidx.lifecycle.ViewModel
import com.chase1st.feetballfootball.core.model.LeagueInfo
import com.chase1st.feetballfootball.core.model.SupportedLeagues
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LeagueListViewModel @Inject constructor() : ViewModel() {

    val leagues: StateFlow<List<LeagueInfo>> =
        MutableStateFlow(SupportedLeagues.TOP_5_LEAGUES)
}
```

> 💡 **Tip:** 지금은 생성자가 비어있지만, 나중에 "최근 본 리그" 같은 기능을 추가하면 Repository를 주입받을 수 있습니다. `@HiltViewModel`이 있으므로 생성자 파라미터만 추가하면 됩니다.

### ✅ 검증
- [ ] `feature-league` 모듈이 `core-model`에 의존성이 있는지 `build.gradle.kts` 확인
- [ ] `@HiltViewModel` 어노테이션이 있는지 확인
- [ ] `leagues` StateFlow가 5개 리그 목록을 반환하는지 확인

---

## Step 4 — feature-league: LeagueListScreen

### 목표
> Compose UI로 리그 카드 리스트를 구현합니다. core-designsystem의 `TeamLogo` 공통 컴포넌트를 사용합니다.

### 작업 내용

`LazyColumn`으로 5개 리그 카드를 표시합니다. 각 카드에는 로고, 리그명, 국가명이 표시되며, 클릭 시 `onLeagueClick` 콜백이 호출됩니다.

**파일 경로:** `feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/LeagueListScreen.kt`

```kotlin
// feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/LeagueListScreen.kt
package com.chase1st.feetballfootball.feature.league

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel  // 1.3.0부터 신규 경로
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chase1st.feetballfootball.core.designsystem.component.TeamLogo
import com.chase1st.feetballfootball.core.model.LeagueInfo

@Composable
fun LeagueListScreen(
    onLeagueClick: (leagueId: Int, leagueName: String) -> Unit,
    viewModel: LeagueListViewModel = hiltViewModel(),
) {
    val leagues by viewModel.leagues.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = leagues,
            key = { it.id },
        ) { league ->
            LeagueCard(
                league = league,
                onClick = { onLeagueClick(league.id, league.name) },
            )
        }
    }
}

@Composable
private fun LeagueCard(
    league: LeagueInfo,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TeamLogo(
                logoUrl = league.logoUrl,
                teamName = league.name,
                size = 48.dp,
            )
            Column {
                Text(
                    text = league.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = league.country,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
```

> ⚠️ **주의:** `TeamLogo`는 core-designsystem에서 제공하는 공통 컴포넌트입니다. Stage 1에서 이미 만들어져 있어야 합니다. 만약 없다면, 먼저 core-designsystem에 `TeamLogo` Composable을 구현하세요.

### ✅ 검증
- [ ] `LeagueListScreen`이 `hiltViewModel()`로 ViewModel을 주입받는지 확인
- [ ] `collectAsStateWithLifecycle()`로 StateFlow를 구독하는지 확인
- [ ] `LazyColumn`의 `items`에 `key = { it.id }`가 설정되어 있는지 확인
- [ ] `LeagueCard`에 로고(48dp), 리그명, 국가명이 표시되는지 확인
- [ ] 카드 클릭 시 `onLeagueClick`에 `leagueId`, `leagueName`이 전달되는지 확인 (seasonId는 순위 화면에서 SofaScore API로 조회)

---

## Step 5 — 임시 연결 (Navigation 전까지)

### 목표
> Navigation 통합 전까지 `MainActivity`에서 직접 `LeagueListScreen`을 호출하여 전체 파이프라인을 검증합니다.

### 작업 내용

Stage 2에서 Navigation 3를 통합하기 전까지, `MainActivity`의 `setContent` 블록에서 `LeagueListScreen`을 직접 호출합니다. 카드 클릭 시 Logcat에 리그 정보가 출력되도록 합니다.

> 💡 **Tip:** 이 코드는 임시 코드입니다. Navigation 통합 시 교체됩니다. 하지만 **Hilt DI → ViewModel → Compose UI** 파이프라인이 정상 동작하는지 확인하는 데 필수적입니다.

**파일 경로:** `app/src/main/kotlin/com/chase1st/feetballfootball/MainActivity.kt` (임시 수정)

```kotlin
// app/src/main/kotlin/com/chase1st/feetballfootball/MainActivity.kt (임시)
setContent {
    FeetballTheme {
        LeagueListScreen(
            onLeagueClick = { id, name ->
                android.util.Log.d("Feetball", "Selected: $name (uniqueTournamentId=$id)")
            }
        )
    }
}
```

> ⚠️ **주의:** `MainActivity`에서 `LeagueListScreen`을 호출하려면 `app` 모듈이 `feature-league` 모듈에 의존해야 합니다. `app/build.gradle.kts`에 `implementation(projects.feature.featureLeague)` 의존성을 추가하세요.

### ✅ 검증
- [ ] 앱 실행 시 5개 리그 카드가 화면에 표시되는지 확인
- [ ] 각 카드에 리그 로고(Coil) + 이름 + 국가가 표시되는지 확인
- [ ] 카드 클릭 시 Logcat에 `Selected: Premier League (uniqueTournamentId=17)` 같은 로그가 출력되는지 확인
- [ ] Hilt ViewModel 주입이 정상 동작하는지 확인 (크래시 없음)

---

## Step 6 — 빌드 및 최종 검증

### 목표
> 전체 빌드를 수행하고 에뮬레이터/기기에서 동작을 확인합니다.

### 작업 내용

```bash
./gradlew assembleDebug
# 앱 실행 → 에뮬레이터/기기
```

### ✅ 검증
- [ ] 앱 실행 시 5개 리그 카드 표시
- [ ] 각 카드에 리그 로고 (Coil) + 이름 + 국가 표시
- [ ] 카드 클릭 시 Logcat에 리그 정보 출력
- [ ] Hilt ViewModel 주입 정상 (크래시 없음)
- [ ] Dark/Light 테마 전환 정상
- [ ] `git commit -m "feat: Slice 1 리그 선택 화면 구현"`

---

## 🎉 Slice 1 완료!

축하합니다! 첫 번째 Slice를 완료했습니다.

**이 Slice에서 달성한 것:**
- `core-model`에 리그 상수와 시즌 유틸리티를 정의했습니다
- `feature-league`에 Hilt ViewModel과 Compose Screen을 구현했습니다
- **Compose + Hilt + 모듈 의존성** 인프라가 정상 동작함을 확인했습니다

**다음 단계:** Slice 2에서는 SofaScore API 첫 연동을 통해 리그 순위 화면을 구현합니다. DTO → Domain → Repository → UseCase → ViewModel → UI 전체 수직 스택을 구축합니다.


---

# Stage 2 / Slice 2 — 리그 순위 화면

> ⏱ 예상 소요 시간: 3시간 | 난이도: ★★☆ | 선행 조건: Slice 1 완료 (리그 선택 화면 + Hilt/Compose 파이프라인 검증)

---

## 이 Codelab에서 배우는 것

- **DTO → Domain Model → Repository → UseCase → ViewModel → UI** 전체 수직 스택 구축
- `kotlinx.serialization`을 사용한 API 응답 DTO 정의
- Retrofit API 엔드포인트 추가 방법
- `@Binds`를 사용한 Hilt Repository 바인딩
- `Flow`를 활용한 비동기 데이터 흐름
- Compose UI로 팀 순위 테이블 구현
- **이 Slice에서 확립한 패턴을 이후 모든 Slice에서 반복 사용합니다**

---

## 완성 후 결과물

- 리그 순위 화면 (팀 순위 탭 + 개인 순위 탭)
- **팀 순위 탭:** 순위, 팀 로고, 팀명, 경기수, 승/무/패, 득실차, 승점 테이블
- **개인 순위 탭:** SofaScore `/api/v1/unique-tournament/{id}/season/{sid}/top-players/{type}` 엔드포인트로 득점왕/도움왕 조회
- Loading → Success / Error 상태 전환
- SofaScore API의 `/api/v1/unique-tournament/{id}/season/{seasonId}/standings/total` 엔드포인트 연동

---

## Step 1 — core-network: DTO 작성

### 목표
> SofaScore API(`/api/v1/unique-tournament/{id}/season/{seasonId}/standings/total`)의 응답 구조에 맞는 DTO(Data Transfer Object)를 정의합니다. `kotlinx.serialization`을 사용합니다.

### 작업 내용

이 Step에서는 1개의 DTO 파일을 만듭니다:
1. **StandingDto.kt** — `/api/v1/unique-tournament/{id}/season/{seasonId}/standings/total` 엔드포인트 응답용

> 💡 **Tip:** SofaScore API는 `standings` 배열을 포함하는 JSON 객체를 반환합니다. 각 standings 항목에 `rows` 배열이 있으며, 이것이 실제 팀 순위 데이터입니다.

> ⚠️ **주의:** SofaScore는 득점/실점을 `scoresFor`, `scoresAgainst`로 각각 별도의 정수 필드로 제공합니다. 골득실차는 `scoresFor - scoresAgainst`로 직접 계산합니다. `promotion` 필드로 진출권/강등권 정보를 제공합니다.

**파일 경로:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/StandingDto.kt`

```kotlin
// core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/StandingDto.kt
package com.chase1st.feetballfootball.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StandingsResponseDto(
    @SerialName("standings") val standings: List<StandingGroupDto> = emptyList(),
)

@Serializable
data class StandingGroupDto(
    @SerialName("tournament") val tournament: StandingTournamentDto? = null,
    @SerialName("type") val type: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("rows") val rows: List<StandingRowDto> = emptyList(),
)

@Serializable
data class StandingTournamentDto(
    @SerialName("uniqueTournament") val uniqueTournament: StandingUniqueTournamentDto? = null,
)

@Serializable
data class StandingUniqueTournamentDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
)

@Serializable
data class StandingRowDto(
    @SerialName("team") val team: StandingTeamDto,
    @SerialName("position") val position: Int = 0,
    @SerialName("matches") val matches: Int = 0,
    @SerialName("wins") val wins: Int = 0,
    @SerialName("draws") val draws: Int = 0,
    @SerialName("losses") val losses: Int = 0,
    @SerialName("scoresFor") val scoresFor: Int = 0,
    @SerialName("scoresAgainst") val scoresAgainst: Int = 0,
    @SerialName("points") val points: Int = 0,
    @SerialName("id") val id: Int = 0,
    @SerialName("promotion") val promotion: StandingPromotionDto? = null,
)

@Serializable
data class StandingTeamDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("shortName") val shortName: String = "",
)

@Serializable
data class StandingPromotionDto(
    @SerialName("text") val text: String = "",
    @SerialName("id") val id: Int = 0,
)

// 시즌 조회용 DTO (현재 시즌 ID를 얻기 위해 사용)
@Serializable
data class SeasonsResponseDto(
    @SerialName("seasons") val seasons: List<SeasonDto> = emptyList(),
)

@Serializable
data class SeasonDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("year") val year: String = "",
)
```

**PlayerStandingDto.kt:**

> SofaScore에서는 선수 통계를 `/api/v1/unique-tournament/{id}/season/{sid}/top-players/{type}` 엔드포인트로 조회합니다. `type`에는 `goals`, `assists`, `rating` 등을 사용할 수 있습니다.

**파일 경로:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/TopPlayersDto.kt`

```kotlin
// core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/TopPlayersDto.kt
package com.chase1st.feetballfootball.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TopPlayersResponseDto(
    @SerialName("topPlayers") val topPlayers: TopPlayersDataDto,
)

@Serializable
data class TopPlayersDataDto(
    @SerialName("goals") val goals: List<TopPlayerItemDto> = emptyList(),
    @SerialName("assists") val assists: List<TopPlayerItemDto> = emptyList(),
)

@Serializable
data class TopPlayerItemDto(
    @SerialName("statistics") val statistics: TopPlayerStatisticsDto,
    @SerialName("player") val player: TopPlayerDto,
    @SerialName("team") val team: TopPlayerTeamDto,
)

@Serializable
data class TopPlayerStatisticsDto(
    @SerialName("goals") val goals: Int = 0,
    @SerialName("assists") val assists: Int = 0,
    @SerialName("appearances") val appearances: Int = 0,
)

@Serializable
data class TopPlayerDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("shortName") val shortName: String = "",
)

@Serializable
data class TopPlayerTeamDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
)
```

**공통 응답 래퍼:**

> SofaScore API는 `ApiResponse<T>` 형태의 공통 래퍼를 사용하지 않습니다. 각 엔드포인트가 고유한 JSON 구조를 반환합니다.

### ✅ 검증
- [ ] `StandingDto.kt`에 `StandingsResponseDto`, `StandingGroupDto`, `StandingRowDto`, `StandingTeamDto`, `StandingPromotionDto`, `SeasonsResponseDto`, `SeasonDto` 클래스가 있는지 확인
- [ ] `StandingRowDto`에 `scoresFor`, `scoresAgainst`, `points`, `position`, `promotion` 필드가 있는지 확인
- [ ] `TopPlayersDto.kt`에 `TopPlayersResponseDto`, `TopPlayerItemDto`, `TopPlayerStatisticsDto` 등이 있는지 확인
- [ ] 모든 DTO에 `@Serializable` 어노테이션이 있는지 확인
- [ ] `core-network` 모듈 빌드 성공 확인

---

## Step 2 — core-network: API 엔드포인트 추가

### 목표
> `FootballApiService` 인터페이스에 SofaScore 순위 엔드포인트를 추가합니다.

### 작업 내용

SofaScore API의 엔드포인트를 사용합니다:
- `/api/v1/unique-tournament/{id}/season/{seasonId}/standings/total` — 리그 팀 순위
- `/api/v1/unique-tournament/{id}/seasons` — 시즌 목록 (현재 시즌 ID 조회용)
- `/api/v1/unique-tournament/{id}/season/{seasonId}/top-players/{type}` — 선수 통계 (득점왕, 도움왕 등)

> 💡 **Tip:** SofaScore는 `uniqueTournamentId`와 `seasonId`를 모두 필요로 합니다. 먼저 `/unique-tournament/{id}/seasons` API로 현재 시즌 ID를 조회한 후, 해당 시즌 ID로 순위를 요청합니다. 선수 통계는 `top-players/{type}`에서 `type`을 `goals`, `assists` 등으로 지정합니다.

**파일 경로:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/api/FootballApiService.kt` (기존 파일에 추가)

```kotlin
// FootballApiService.kt에 추가
import com.chase1st.feetballfootball.core.network.model.*
import retrofit2.http.GET
import retrofit2.http.Path

interface FootballApiService {

    @GET("api/v1/unique-tournament/{id}/season/{seasonId}/standings/total")
    suspend fun getStandings(
        @Path("id") uniqueTournamentId: Int,
        @Path("seasonId") seasonId: Int,
    ): StandingsResponseDto

    @GET("api/v1/unique-tournament/{id}/seasons")
    suspend fun getSeasons(
        @Path("id") uniqueTournamentId: Int,
    ): SeasonsResponseDto

    @GET("api/v1/unique-tournament/{id}/season/{seasonId}/top-players/{type}")
    suspend fun getTopPlayers(
        @Path("id") uniqueTournamentId: Int,
        @Path("seasonId") seasonId: Int,
        @Path("type") type: String,  // "goals", "assists", "rating" 등
    ): TopPlayersResponseDto
}
```

### ✅ 검증
- [ ] `FootballApiService`에 `getStandings()`, `getSeasons()`, `getTopPlayers()` 메서드가 추가되었는지 확인
- [ ] 모든 메서드가 `suspend fun`인지 확인
- [ ] `getStandings()` 반환 타입이 `StandingsResponseDto`인지 확인
- [ ] `@Path("id")`와 `@Path("seasonId")`로 URL 경로 파라미터가 올바른지 확인
- [ ] `getTopPlayers()`의 `type` 파라미터가 `@Path`로 선언되어 있는지 확인
- [ ] `core-network` 모듈 빌드 성공 확인

---

## Step 3 — core-model: Domain Model

### 목표
> 네트워크 DTO와 분리된 순수 도메인 모델을 정의합니다. UI에서는 이 모델만 참조합니다.

### 작업 내용

DTO와 Domain Model을 분리하면:
- UI 레이어가 네트워크 응답 구조에 의존하지 않게 됩니다
- API 응답 구조가 바뀌어도 Mapper만 수정하면 됩니다
- 테스트 시 도메인 모델로 직접 더미 데이터를 만들 수 있습니다

**파일 경로:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/TeamStanding.kt`

```kotlin
// core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/TeamStanding.kt
package com.chase1st.feetballfootball.core.model

data class TeamStanding(
    val rank: Int,
    val team: Team,
    val points: Int,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val form: String?,
    val description: String?,
)

data class Team(
    val id: Int,
    val name: String,
    val logoUrl: String,
)
```

**파일 경로:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/PlayerStanding.kt`

```kotlin
// core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/PlayerStanding.kt
package com.chase1st.feetballfootball.core.model

data class PlayerStanding(
    val rank: Int,
    val player: Player,
    val team: Team,
    val goals: Int,
    val assists: Int,
)

data class Player(
    val id: Int,
    val name: String,
    val photoUrl: String?,
)
```

> 💡 **Tip:** `Team` data class는 `TeamStanding`과 `PlayerStanding` 양쪽에서 사용됩니다. Slice 3(경기 일정)의 `Fixture`에서도 재사용됩니다. 공통 모델은 `core-model`에 두는 것이 좋습니다.

### ✅ 검증
- [ ] `TeamStanding`에 순위표에 필요한 모든 필드가 있는지 확인 (rank, team, points, played, won, drawn, lost, goalsFor, goalsAgainst, goalDifference, form, description)
- [ ] `PlayerStanding`에 rank, player, team, goals, assists 필드가 있는지 확인
- [ ] `Team`과 `Player`가 별도 data class로 분리되어 있는지 확인
- [ ] `core-model` 모듈 빌드 성공 확인

---

## Step 4 — core-domain: Repository 인터페이스 + UseCase

### 목표
> **Repository 인터페이스**를 core-domain에 정의하고, **UseCase**를 만들어 ViewModel이 비즈니스 로직에만 의존하게 합니다.

### 작업 내용

Clean Architecture에서 `core-domain`은:
- Repository **인터페이스**만 정의합니다 (구현은 `core-data`에서)
- UseCase를 통해 비즈니스 로직을 캡슐화합니다
- `core-network`에 의존하지 않습니다 (오직 `core-model`만 참조)

> ⚠️ **주의:** Repository 인터페이스는 `core-domain`에, 구현체는 `core-data`에 있어야 합니다. 이렇게 하면 feature 모듈이 `core-network`에 직접 의존하지 않게 됩니다.

**파일 경로:** `core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/repository/LeagueRepository.kt`

```kotlin
// core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/repository/LeagueRepository.kt
package com.chase1st.feetballfootball.core.domain.repository

import com.chase1st.feetballfootball.core.model.TeamStanding
import com.chase1st.feetballfootball.core.model.PlayerStanding
import kotlinx.coroutines.flow.Flow

interface LeagueRepository {
    fun getLeagueStandings(uniqueTournamentId: Int): Flow<List<TeamStanding>>
    fun getTopScorers(uniqueTournamentId: Int): Flow<List<PlayerStanding>>
    fun getTopAssists(uniqueTournamentId: Int): Flow<List<PlayerStanding>>
}
```

**파일 경로:** `core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/usecase/GetLeagueStandingsUseCase.kt`

```kotlin
// core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/usecase/GetLeagueStandingsUseCase.kt
package com.chase1st.feetballfootball.core.domain.usecase

import com.chase1st.feetballfootball.core.domain.repository.LeagueRepository
import com.chase1st.feetballfootball.core.model.TeamStanding
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLeagueStandingsUseCase @Inject constructor(
    private val leagueRepository: LeagueRepository,
) {
    operator fun invoke(uniqueTournamentId: Int): Flow<List<TeamStanding>> =
        leagueRepository.getLeagueStandings(uniqueTournamentId)
}
```

**GetTopScorersUseCase.kt:**

```kotlin
// core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/usecase/GetTopScorersUseCase.kt
package com.chase1st.feetballfootball.core.domain.usecase

import com.chase1st.feetballfootball.core.domain.repository.LeagueRepository
import com.chase1st.feetballfootball.core.model.PlayerStanding
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTopScorersUseCase @Inject constructor(
    private val leagueRepository: LeagueRepository,
) {
    operator fun invoke(uniqueTournamentId: Int): Flow<List<PlayerStanding>> =
        leagueRepository.getTopScorers(uniqueTournamentId)
}
```

**GetTopAssistsUseCase.kt:**

```kotlin
// core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/usecase/GetTopAssistsUseCase.kt
package com.chase1st.feetballfootball.core.domain.usecase

import com.chase1st.feetballfootball.core.domain.repository.LeagueRepository
import com.chase1st.feetballfootball.core.model.PlayerStanding
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTopAssistsUseCase @Inject constructor(
    private val leagueRepository: LeagueRepository,
) {
    operator fun invoke(uniqueTournamentId: Int): Flow<List<PlayerStanding>> =
        leagueRepository.getTopAssists(uniqueTournamentId)
}
```

> 💡 **Tip:** UseCase에 `operator fun invoke()`를 사용하면 `getLeagueStandingsUseCase(uniqueTournamentId)` 형태로 함수처럼 호출할 수 있습니다. Kotlin의 관례적 패턴입니다. Repository 내부에서 seasonId를 자동으로 조회하므로 UseCase에는 `uniqueTournamentId`만 전달합니다.

### ✅ 검증
- [ ] `LeagueRepository` 인터페이스에 `getLeagueStandings()`, `getTopScorers()`, `getTopAssists()` 메서드가 있는지 확인
- [ ] `GetLeagueStandingsUseCase`, `GetTopScorersUseCase`, `GetTopAssistsUseCase`가 `@Inject constructor`를 가지고 있는지 확인
- [ ] 모든 UseCase가 `operator fun invoke()`로 선언되어 있는지 확인
- [ ] UseCase의 반환 타입이 `Flow`인지 확인
- [ ] `uniqueTournamentId`만 파라미터로 받는지 확인 (seasonId는 Repository 내부에서 자동 조회)
- [ ] `core-domain` 모듈 빌드 성공 확인

---

## Step 5 — core-data: Mapper + Repository 구현

### 목표
> DTO를 Domain Model로 변환하는 Mapper와, Repository 인터페이스의 구현체를 만듭니다. Hilt `@Binds`로 인터페이스와 구현체를 바인딩합니다.

### 작업 내용

이 Step은 데이터 레이어의 핵심입니다:
1. **StandingMapper** — SofaScore DTO → Domain Model 변환 (`scoresFor`/`scoresAgainst` 직접 사용)
2. **LeagueRepositoryImpl** — 시즌 ID 조회 + API 호출 + Mapper를 통한 변환
3. **DataModule** — Hilt 바인딩

> ⚠️ **주의:** SofaScore는 `scoresFor`와 `scoresAgainst`를 별도의 정수 필드로 제공합니다. 문자열 파싱이 필요 없습니다. 골득실차는 `scoresFor - scoresAgainst`로 계산합니다. 팀 로고 URL은 `https://img.sofascore.com/api/v1/team/{teamId}/image` 패턴으로 생성합니다.

**파일 경로:** `core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/mapper/StandingMapper.kt`

```kotlin
// core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/mapper/StandingMapper.kt
package com.chase1st.feetballfootball.core.data.mapper

import com.chase1st.feetballfootball.core.model.*
import com.chase1st.feetballfootball.core.network.model.*
import javax.inject.Inject

class StandingMapper @Inject constructor() {

    fun mapTeamStandings(dtos: List<StandingRowDto>): List<TeamStanding> =
        dtos.map { dto ->
            TeamStanding(
                rank = dto.position,
                team = Team(
                    id = dto.team.id,
                    name = dto.team.name,
                    logoUrl = "https://img.sofascore.com/api/v1/team/${dto.team.id}/image",
                ),
                points = dto.points,
                played = dto.matches,
                won = dto.wins,
                drawn = dto.draws,
                lost = dto.losses,
                goalsFor = dto.scoresFor,
                goalsAgainst = dto.scoresAgainst,
                goalDifference = dto.scoresFor - dto.scoresAgainst,
                form = null,  // SofaScore standings에는 form 정보 미포함
                description = dto.promotion?.text,  // 진출권/강등권 텍스트
            )
        }

    fun mapTopScorers(dtos: List<TopPlayerItemDto>): List<PlayerStanding> =
        dtos.mapIndexed { index, dto ->
            PlayerStanding(
                rank = index + 1,
                player = Player(
                    id = dto.player.id,
                    name = dto.player.name,
                    photoUrl = "https://img.sofascore.com/api/v1/player/${dto.player.id}/image",
                ),
                team = Team(
                    id = dto.team.id,
                    name = dto.team.name,
                    logoUrl = "https://img.sofascore.com/api/v1/team/${dto.team.id}/image",
                ),
                goals = dto.statistics.goals,
                assists = dto.statistics.assists,
            )
        }

    fun mapTopAssists(dtos: List<TopPlayerItemDto>): List<PlayerStanding> =
        dtos.mapIndexed { index, dto ->
            PlayerStanding(
                rank = index + 1,
                player = Player(
                    id = dto.player.id,
                    name = dto.player.name,
                    photoUrl = "https://img.sofascore.com/api/v1/player/${dto.player.id}/image",
                ),
                team = Team(
                    id = dto.team.id,
                    name = dto.team.name,
                    logoUrl = "https://img.sofascore.com/api/v1/team/${dto.team.id}/image",
                ),
                goals = dto.statistics.goals,
                assists = dto.statistics.assists,
            )
        }
}
```

**파일 경로:** `core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/repository/LeagueRepositoryImpl.kt`

```kotlin
// core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/repository/LeagueRepositoryImpl.kt
package com.chase1st.feetballfootball.core.data.repository

import com.chase1st.feetballfootball.core.common.dispatcher.IoDispatcher
import com.chase1st.feetballfootball.core.data.mapper.StandingMapper
import com.chase1st.feetballfootball.core.domain.repository.LeagueRepository
import com.chase1st.feetballfootball.core.model.PlayerStanding
import com.chase1st.feetballfootball.core.model.TeamStanding
import com.chase1st.feetballfootball.core.network.api.FootballApiService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class LeagueRepositoryImpl @Inject constructor(
    private val apiService: FootballApiService,
    private val mapper: StandingMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LeagueRepository {

    // 현재 시즌 ID를 조회하는 헬퍼
    private suspend fun getCurrentSeasonId(uniqueTournamentId: Int): Int {
        val seasonsResponse = apiService.getSeasons(uniqueTournamentId)
        return seasonsResponse.seasons.firstOrNull()?.id
            ?: throw IllegalStateException("시즌 정보를 찾을 수 없습니다")
    }

    override fun getLeagueStandings(uniqueTournamentId: Int): Flow<List<TeamStanding>> = flow {
        val seasonId = getCurrentSeasonId(uniqueTournamentId)
        val response = apiService.getStandings(uniqueTournamentId, seasonId)
        val rows = response.standings.firstOrNull()?.rows ?: emptyList()
        emit(mapper.mapTeamStandings(rows))
    }.flowOn(ioDispatcher)

    override fun getTopScorers(uniqueTournamentId: Int): Flow<List<PlayerStanding>> = flow {
        val seasonId = getCurrentSeasonId(uniqueTournamentId)
        val response = apiService.getTopPlayers(uniqueTournamentId, seasonId, "goals")
        emit(mapper.mapTopScorers(response.topPlayers.goals))
    }.flowOn(ioDispatcher)

    override fun getTopAssists(uniqueTournamentId: Int): Flow<List<PlayerStanding>> = flow {
        val seasonId = getCurrentSeasonId(uniqueTournamentId)
        val response = apiService.getTopPlayers(uniqueTournamentId, seasonId, "assists")
        emit(mapper.mapTopAssists(response.topPlayers.assists))
    }.flowOn(ioDispatcher)
}
```

> 💡 **Tip:** `flowOn(ioDispatcher)`를 사용하면 API 호출이 IO 스레드에서 실행됩니다. `@IoDispatcher`는 core-common 모듈에서 정의된 Hilt qualifier입니다.

> ⚠️ **주의:** SofaScore는 `uniqueTournamentId`와 `seasonId`를 모두 필요로 합니다. `getCurrentSeasonId()`로 먼저 시즌 목록을 조회하고, 첫 번째 시즌(최신 시즌)의 ID를 사용합니다. `response.standings.firstOrNull()?.rows`에서 `firstOrNull()`은 standings 배열의 첫 번째 그룹을 꺼냅니다.

**파일 경로:** `core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/di/DataModule.kt`

```kotlin
// core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/di/DataModule.kt
package com.chase1st.feetballfootball.core.data.di

import com.chase1st.feetballfootball.core.data.repository.LeagueRepositoryImpl
import com.chase1st.feetballfootball.core.domain.repository.LeagueRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindLeagueRepository(impl: LeagueRepositoryImpl): LeagueRepository

    // 이후 Slice에서 FixtureRepository 등 추가
}
```

### ✅ 검증
- [ ] `StandingMapper`에 `@Inject constructor()`가 있어서 Hilt가 자동 주입할 수 있는지 확인
- [ ] `mapTeamStandings()`가 `scoresFor`/`scoresAgainst`를 직접 사용하고, `scoresFor - scoresAgainst`로 골득실차를 계산하는지 확인
- [ ] `mapTopScorers()`와 `mapTopAssists()`가 `TopPlayerItemDto`를 `PlayerStanding`으로 변환하는지 확인
- [ ] 팀 로고 URL이 `https://img.sofascore.com/api/v1/team/{teamId}/image` 패턴으로 생성되는지 확인
- [ ] 선수 사진 URL이 `https://img.sofascore.com/api/v1/player/{playerId}/image` 패턴으로 생성되는지 확인
- [ ] `LeagueRepositoryImpl`이 `getCurrentSeasonId()`로 시즌 ID를 먼저 조회하는지 확인
- [ ] `LeagueRepositoryImpl`이 `LeagueRepository` 인터페이스를 구현하는지 확인
- [ ] `DataModule`에서 `@Binds`로 인터페이스-구현체 바인딩이 되어 있는지 확인
- [ ] `core-data` 모듈 빌드 성공 확인

---

## Step 6 — feature-league: ViewModel + UiState

### 목표
> 순위 화면의 ViewModel과 UI 상태(Loading/Success/Error)를 정의합니다.

### 작업 내용

`StandingViewModel`은 `GetLeagueStandingsUseCase`를 사용하여 데이터를 로드합니다. `SavedStateHandle`에서 Navigation 인자를 추출합니다 (Navigation 통합 전에는 임시 하드코딩).

**파일 경로:** `feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/standing/StandingUiState.kt`

```kotlin
// feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/standing/StandingUiState.kt
package com.chase1st.feetballfootball.feature.league.standing

import com.chase1st.feetballfootball.core.model.TeamStanding

sealed interface StandingUiState {
    data object Loading : StandingUiState
    data class Success(
        val clubStandings: List<TeamStanding>,
    ) : StandingUiState
    data class Error(val message: String) : StandingUiState
}
```

> 💡 **Tip:** `sealed interface`를 사용하면 `when` 식에서 모든 상태를 처리했는지 컴파일러가 검사해 줍니다. `sealed class`보다 유연하며 Kotlin 권장 패턴입니다.

**파일 경로:** `feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/standing/StandingViewModel.kt`

```kotlin
// feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/standing/StandingViewModel.kt
package com.chase1st.feetballfootball.feature.league.standing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chase1st.feetballfootball.core.domain.usecase.GetLeagueStandingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StandingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getLeagueStandingsUseCase: GetLeagueStandingsUseCase,
) : ViewModel() {

    // Navigation 인자에서 추출 (Navigation 3 통합 전에는 임시 하드코딩)
    // SofaScore uniqueTournamentId 사용 (예: Premier League = 17)
    private val uniqueTournamentId: Int = savedStateHandle["uniqueTournamentId"] ?: 17

    private val _uiState = MutableStateFlow<StandingUiState>(StandingUiState.Loading)
    val uiState: StateFlow<StandingUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val standings = getLeagueStandingsUseCase(uniqueTournamentId).first()

                _uiState.value = StandingUiState.Success(
                    clubStandings = standings,
                )
            } catch (e: Exception) {
                _uiState.value = StandingUiState.Error(
                    e.message ?: "데이터를 불러올 수 없습니다"
                )
            }
        }
    }

    fun retry() = loadData()
}
```

> ⚠️ **주의:** `savedStateHandle["uniqueTournamentId"] ?: 17`은 Navigation 통합 전 임시 기본값입니다. SofaScore uniqueTournamentId를 사용합니다 (Premier League = 17). Navigation 3 통합 시 실제 Navigation 인자로 교체됩니다.

### ✅ 검증
- [ ] `StandingUiState`에 `Loading`, `Success`, `Error` 3개 상태가 있는지 확인
- [ ] `StandingViewModel`이 `@HiltViewModel`로 선언되어 있는지 확인
- [ ] `GetLeagueStandingsUseCase`가 생성자에 주입되는지 확인
- [ ] `uniqueTournamentId` 기본값이 `17` (SofaScore Premier League ID)인지 확인
- [ ] `init` 블록에서 `loadData()`가 호출되는지 확인
- [ ] `retry()` 함수가 `loadData()`를 다시 호출하는지 확인

---

## Step 7 — feature-league: Compose UI

### 목표
> 순위 화면의 Compose UI를 구현합니다. 팀 순위와 선수 순위(득점왕/도움왕) 탭을 구현합니다.

### 작업 내용

팀 순위 화면을 구현합니다. 선수 순위 탭은 SofaScore `/api/v1/unique-tournament/{id}/season/{sid}/top-players/{type}` 엔드포인트를 사용합니다.

1. **StandingScreen.kt** — 전체 화면 (Scaffold + 팀 순위)
2. **ClubStandingTab.kt** — 팀 순위 테이블 (헤더 + 순위 행)

**파일 경로:** `feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/standing/StandingScreen.kt`

```kotlin
// feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/standing/StandingScreen.kt
package com.chase1st.feetballfootball.feature.league.standing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel  // 1.3.0부터 신규 경로
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chase1st.feetballfootball.core.designsystem.component.ErrorContent
import com.chase1st.feetballfootball.core.designsystem.component.FeetballLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingScreen(
    leagueName: String,
    onBack: () -> Unit,
    viewModel: StandingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(leagueName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is StandingUiState.Loading -> FeetballLoadingIndicator()
                is StandingUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = viewModel::retry,
                )
                is StandingUiState.Success -> {
                    ClubStandingTab(standings = state.clubStandings)
                }
            }
        }
    }
}
```

**파일 경로:** `feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/standing/ClubStandingTab.kt`

```kotlin
// feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/standing/ClubStandingTab.kt
package com.chase1st.feetballfootball.feature.league.standing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chase1st.feetballfootball.core.designsystem.component.TeamLogo
import com.chase1st.feetballfootball.core.model.TeamStanding

@Composable
fun ClubStandingTab(standings: List<TeamStanding>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // 테이블 헤더
        item {
            StandingHeader()
        }

        items(
            items = standings,
            key = { it.team.id },
        ) { standing ->
            StandingRow(standing = standing)
            HorizontalDivider()
        }
    }
}

@Composable
private fun StandingHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("#", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.width(40.dp)) // 로고 공간
        Text("팀", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
        Text("경기", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
        Text("승", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
        Text("무", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
        Text("패", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
        Text("득실", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
        Text("승점", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StandingRow(standing: TeamStanding) {
    // 참조: LeagueClubsStandingFragment.kt의 순위 색상 코딩
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${standing.rank}",
            modifier = Modifier.width(24.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        TeamLogo(
            logoUrl = standing.team.logoUrl,
            teamName = standing.team.name,
            size = 28.dp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = standing.team.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
        )
        Text("${standing.played}", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        Text("${standing.won}", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        Text("${standing.drawn}", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        Text("${standing.lost}", modifier = Modifier.width(24.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        Text("${standing.goalDifference}", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        Text("${standing.points}", modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
    }
}
```

**PlayerStandingTab.kt:**

> 선수 순위 탭은 SofaScore `/api/v1/unique-tournament/{id}/season/{sid}/top-players/goals` (득점왕) 및 `/top-players/assists` (도움왕) 엔드포인트를 사용합니다. ViewModel에 `GetTopScorersUseCase`, `GetTopAssistsUseCase`를 주입하여 구현합니다.

### ✅ 검증
- [ ] `StandingScreen`에 `Scaffold` + `TopAppBar` + 뒤로가기 버튼이 있는지 확인
- [ ] `ClubStandingTab`에 테이블 헤더(#, 팀, 경기, 승, 무, 패, 득실, 승점)가 있는지 확인
- [ ] 팀 로고가 SofaScore 이미지 CDN URL (`https://img.sofascore.com/api/v1/team/{teamId}/image`)로 정상 로딩되는지 확인
- [ ] Loading, Error, Success 상태 전환이 정상적인지 확인
- [ ] `feature-league` 모듈 빌드 성공 확인

---

## Step 8 — 빌드 및 최종 검증

### 목표
> 전체 빌드를 수행하고 에뮬레이터/기기에서 API 연동을 검증합니다.

### 작업 내용

```bash
./gradlew assembleDebug
# 앱 실행 → 에뮬레이터/기기
```

### ✅ 검증
- [ ] 리그 선택 → 순위 화면 전환 (임시 하드코딩 or Logcat)
- [ ] 클럽 순위 탭: SofaScore `/api/v1/unique-tournament/{id}/season/{seasonId}/standings/total` API 호출 → 20개 팀 순위표 표시
- [ ] 팀 로고가 SofaScore 이미지 CDN URL (`https://img.sofascore.com/api/v1/team/{teamId}/image`)로 정상 로딩되는지 확인
- [ ] `scoresFor`/`scoresAgainst` 필드가 올바르게 매핑되는지 확인
- [ ] 로딩 상태 → 성공 상태 전환 정상
- [ ] 에러 상태 → 다시 시도 동작
- [ ] 팀 로고 이미지 로딩 정상 (Coil)
- [ ] **이 시점에서 DTO → Domain → Repository → UseCase → ViewModel → UI 패턴 확립**
- [ ] `git commit -m "feat: Slice 2 리그 순위 화면 구현 (SofaScore API 연동)"`

---

## 🎉 Slice 2 완료!

축하합니다! 가장 중요한 Slice를 완료했습니다.

**이 Slice에서 달성한 것:**
- **DTO → Domain → Repository → UseCase → ViewModel → UI** 전체 수직 스택을 구축했습니다
- SofaScore API(`/api/v1/unique-tournament/{id}/season/{seasonId}/standings/total`)에 맞춘 `kotlinx.serialization` DTO와 도메인 모델을 분리했습니다
- Hilt `@Binds`로 Repository 인터페이스-구현체를 바인딩했습니다
- `Flow`와 `StateFlow`를 활용한 반응형 데이터 흐름을 구현했습니다
- SofaScore 이미지 CDN(`https://img.sofascore.com/api/v1/team/{id}/image`)을 활용한 팀 로고 URL 생성 패턴을 확립했습니다

**이 패턴이 중요한 이유:** 이후 모든 Slice(경기 일정, 경기 상세 등)에서 동일한 패턴을 반복합니다. DTO 작성 → Domain Model → Repository → UseCase → ViewModel → Compose UI 순서로 작업하면 됩니다.

**다음 단계:** Slice 3에서는 앱의 메인 화면인 경기 일정 화면을 구현합니다. 날짜 선택, 리그별 그룹핑, 경기 상태별 표시를 다룹니다.


---

# Stage 2 / Slice 3 — 경기 일정 화면

> ⏱ 예상 소요 시간: 3시간 | 난이도: ★★☆ | 선행 조건: Slice 2 완료 (API 연동 패턴 확립: DTO → Domain → Repository → UseCase → ViewModel → UI)

---

## 이 Codelab에서 배우는 것

- Slice 2에서 확립한 **수직 스택 패턴**을 새 feature에 반복 적용하는 방법
- `flatMapLatest`를 사용한 **반응형 날짜 선택** (날짜 변경 시 자동 API 재호출)
- `Flow.map`을 사용한 **리그별 그룹핑** (UseCase 레벨 비즈니스 로직)
- `enum class`를 활용한 **경기 상태 매핑** (SofaScore의 status.code/status.type 필드 기반)
- Compose `LazyColumn`의 `stickyHeader`를 사용한 **리그 헤더 고정**
- `DateTimeFormatter`를 사용한 날짜/시간 포맷팅

---

## 완성 후 결과물

- 날짜 선택 UI (이전/다음 날짜 버튼)
- 리그별로 그룹핑된 경기 목록 (stickyHeader로 리그 헤더 고정)
- 경기 상태별 표시: 예정(시간), 진행중(스코어+경과시간), 종료(스코어), 연기/취소
- 종료/라이브 경기만 클릭 가능
- 경기 없는 날짜에 빈 상태 표시
- Loading → Success / Empty / Error 상태 전환

---

## Step 1 — core-network: DTO 작성

### 목표
> SofaScore `/api/v1/sport/football/{date}/events/{page}` 엔드포인트의 응답 구조에 맞는 DTO를 정의합니다. 응답은 `events[]` 형태의 플랫 배열이며, 각 이벤트에 tournament 정보가 포함됩니다.

### 작업 내용

SofaScore `/api/v1/sport/football/{date}/events/{page}` API 응답의 구조:
```
{
  "events": [
    {
      "id": 12345678,
      "tournament": {
        "uniqueTournament": { "id": 17, "name": "Premier League", "category": { "name": "England" } }
      },
      "season": { "id": 52186 },
      "homeTeam": { "id": 17, "name": "Manchester City", "shortName": "Man City" },
      "awayTeam": { "id": 42, "name": "Arsenal", "shortName": "Arsenal" },
      "homeScore": { "current": 1, "period1": 0, "period2": 1 },
      "awayScore": { "current": 2, "period1": 1, "period2": 1 },
      "status": { "code": 100, "description": "Ended", "type": "finished" },
      "startTimestamp": 1710504000,
      "slug": "manchester-city-arsenal",
      "roundInfo": { "round": 28 }
    }
  ]
}
```

이 구조를 반영하여 DTO 클래스를 정의합니다.

> 💡 **Tip:** `EventListDto.kt` 하나에 모든 DTO를 모아두면 관련 클래스를 한눈에 볼 수 있습니다. API 응답 구조가 복잡할수록 한 파일에 모으는 것이 유지보수에 유리합니다.

> ⚠️ **주의:** `EventScoreDto.current`는 nullable입니다 (아직 시작하지 않은 경기는 null). SofaScore는 팀 로고 URL을 응답에 포함하지 않으며, `https://img.sofascore.com/api/v1/team/{teamId}/image` 형식으로 직접 구성합니다. 리그 로고는 `https://img.sofascore.com/api/v1/unique-tournament/{id}/image` 패턴입니다.

**파일 경로:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/EventListDto.kt`

```kotlin
// core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/EventListDto.kt
package com.chase1st.feetballfootball.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventListResponseDto(
    @SerialName("events") val events: List<EventDto> = emptyList(),
)

@Serializable
data class EventDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("tournament") val tournament: EventTournamentDto? = null,
    @SerialName("season") val season: EventSeasonDto? = null,
    @SerialName("homeTeam") val homeTeam: EventTeamDto,
    @SerialName("awayTeam") val awayTeam: EventTeamDto,
    @SerialName("homeScore") val homeScore: EventScoreDto? = null,
    @SerialName("awayScore") val awayScore: EventScoreDto? = null,
    @SerialName("status") val status: EventStatusDto,
    @SerialName("startTimestamp") val startTimestamp: Long = 0,
    @SerialName("slug") val slug: String = "",
    @SerialName("roundInfo") val roundInfo: EventRoundInfoDto? = null,
)

@Serializable
data class EventTournamentDto(
    @SerialName("uniqueTournament") val uniqueTournament: EventUniqueTournamentDto? = null,
)

@Serializable
data class EventUniqueTournamentDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("category") val category: EventCategoryDto? = null,
)

@Serializable
data class EventCategoryDto(
    @SerialName("name") val name: String = "",
)

@Serializable
data class EventSeasonDto(
    @SerialName("id") val id: Int = 0,
)

@Serializable
data class EventTeamDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("shortName") val shortName: String = "",
)

@Serializable
data class EventScoreDto(
    @SerialName("current") val current: Int? = null,
    @SerialName("period1") val period1: Int? = null,
    @SerialName("period2") val period2: Int? = null,
)

@Serializable
data class EventStatusDto(
    @SerialName("code") val code: Int = 0,
    @SerialName("description") val description: String = "",
    @SerialName("type") val type: String = "",
)

@Serializable
data class EventRoundInfoDto(
    @SerialName("round") val round: Int = 0,
)
```

### ✅ 검증
- [ ] `EventListResponseDto`에 `events` 필드가 있는지 확인
- [ ] `EventDto`에 `tournament`, `homeTeam`, `awayTeam`, `homeScore`, `awayScore`, `status`, `startTimestamp` 필드가 있는지 확인
- [ ] `EventStatusDto`에 `code` (Int), `description` (String), `type` (String) 필드가 있는지 확인
- [ ] `EventScoreDto.current`가 nullable인지 확인 (아직 시작하지 않은 경기는 null)
- [ ] 모든 DTO에 `@Serializable` 어노테이션이 있는지 확인
- [ ] `core-network` 모듈 빌드 성공 확인

---

## Step 2 — core-network: API 엔드포인트 추가

### 목표
> `FootballApiService`에 날짜별 경기 목록 엔드포인트를 추가합니다.

### 작업 내용

SofaScore `/api/v1/sport/football/{date}/events/{page}` 엔드포인트는 URL 경로에 날짜를 포함하여 특정 날짜의 경기를 조회합니다. 타임존 관련 파라미터는 필요 없으며, `startTimestamp` (Unix timestamp, 초 단위)를 클라이언트에서 변환하여 사용합니다.

> 💡 **Tip:** SofaScore는 타임존 파라미터가 필요 없습니다. `startTimestamp`가 UTC 기준 Unix timestamp(초 단위)이므로 클라이언트에서 원하는 타임존으로 변환합니다. 페이지는 기본적으로 `0`을 사용합니다.

> ⚠️ **주의:** SofaScore는 `ApiResponse<T>` 래퍼를 사용하지 않고 직접 `EventListResponseDto`를 반환합니다. 모든 스포츠 이벤트를 반환하므로, `tournament.uniqueTournament.id`로 지원하는 리그만 필터링해야 합니다.

**파일 경로:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/api/FootballApiService.kt` (기존 파일에 추가)

```kotlin
// FootballApiService.kt에 추가
@GET("api/v1/sport/football/{date}/events/{page}")
suspend fun getEventsByDate(
    @Path("date") date: String,         // yyyy-MM-dd 형식
    @Path("page") page: Int = 0,
): EventListResponseDto
```

> ⚠️ **주의:** `date` 파라미터는 `"2026-03-15"` 형식 (yyyy-MM-dd)입니다. `LocalDate.toString()`이 ISO 8601 형식(`2026-03-15`)을 반환하므로 그대로 사용할 수 있습니다. 별도의 `DateTimeFormatter` 변환이 필요 없습니다.

### ✅ 검증
- [ ] `getEventsByDate()`가 `FootballApiService`에 추가되었는지 확인
- [ ] `suspend fun`으로 선언되어 있는지 확인
- [ ] `@Path("date")`와 `@Path("page")`로 URL 경로 파라미터가 올바른지 확인
- [ ] `page` 기본값이 `0`인지 확인
- [ ] 반환 타입이 `EventListResponseDto`인지 확인 (ApiResponse 래퍼 없음)

---

## Step 3 — core-model: Domain Model

### 목표
> 경기 일정의 도메인 모델 `Fixture`와 경기 상태를 나타내는 `MatchStatus` enum을 정의합니다.

### 작업 내용

`MatchStatus`는 SofaScore의 경기 상태 필드(`status.code` int, `status.type` string)를 Kotlin enum으로 매핑합니다. 기존 `FixtureRecyclerViewAdapter.kt`와 `FixtureDetailFragment.kt`에 분산되어 있던 상태 판단 로직을 하나의 enum에 모읍니다.

> 💡 **Tip:** `MatchStatus`의 computed property (`isFinished`, `isLive`, `isClickable`)를 사용하면 UI에서 `when` 분기를 줄일 수 있습니다. 예: `if (fixture.status.isLive)` 로 간결하게 판단할 수 있습니다. SofaScore `status.type`은 `"notstarted"`, `"inprogress"`, `"finished"`, `"postponed"`, `"canceled"` 중 하나입니다.

> ⚠️ **주의:** `Fixture`의 `league` 필드는 Slice 1에서 만든 `LeagueInfo`를 재사용합니다. `groupBy { it.league }`로 리그별 그룹핑에 사용되므로, `LeagueInfo`에 `equals()`/`hashCode()`가 올바르게 동작해야 합니다 (data class이므로 자동 생성됨).

**파일 경로:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/Fixture.kt`

```kotlin
// core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/Fixture.kt
package com.chase1st.feetballfootball.core.model

import java.time.LocalDateTime

data class Fixture(
    val id: Int,
    val date: LocalDateTime,
    val status: MatchStatus,
    val elapsed: Int?,
    val venue: String?,
    val league: LeagueInfo,
    val homeTeam: Team,
    val awayTeam: Team,
    val homeGoals: Int?,
    val awayGoals: Int?,
)

// SofaScore는 status.code (Int)와 status.type (String)으로 상태를 판단
// status.type: "notstarted", "inprogress", "finished", "postponed", "canceled"
// status.code: 0=notstarted, 6=1st half, 7=halftime, 8=2nd half, 31=halftime, 60=postponed, 70=canceled, 100=ended
enum class MatchStatus(val displayText: String) {
    NOT_STARTED(""),
    FIRST_HALF("전반전"),
    HALF_TIME("하프타임"),
    SECOND_HALF("후반전"),
    FINISHED("종료"),
    POSTPONED("연기됨"),
    CANCELLED("취소됨"),
    LIVE("진행중"),
    ;

    companion object {
        fun fromSofaScoreStatus(status: EventStatusDto): MatchStatus = when (status.type) {
            "finished" -> FINISHED
            "postponed" -> POSTPONED
            "canceled" -> CANCELLED
            "inprogress" -> when (status.code) {
                6 -> FIRST_HALF
                7, 31 -> HALF_TIME
                8 -> SECOND_HALF
                else -> LIVE
            }
            "notstarted" -> NOT_STARTED
            else -> NOT_STARTED
        }
    }

    val isFinished: Boolean get() = this == FINISHED
    val isLive: Boolean get() = this in listOf(FIRST_HALF, HALF_TIME, SECOND_HALF, LIVE)
    val isClickable: Boolean get() = this !in listOf(NOT_STARTED, POSTPONED, CANCELLED)
}
```

### ✅ 검증
- [ ] `Fixture` data class에 id, date, status, elapsed, venue, league, homeTeam, awayTeam, homeGoals, awayGoals 10개 필드가 있는지 확인
- [ ] `MatchStatus`에 8개 상태가 정의되어 있는지 확인 (NOT_STARTED, FIRST_HALF, HALF_TIME, SECOND_HALF, FINISHED, POSTPONED, CANCELLED, LIVE)
- [ ] `fromSofaScoreStatus()`가 `type="finished"`일 때 `FINISHED`를 반환하는지 확인
- [ ] `fromSofaScoreStatus()`가 `type="canceled"`일 때 `CANCELLED`를 반환하는지 확인
- [ ] `fromSofaScoreStatus()`가 `type="postponed"`일 때 `POSTPONED`를 반환하는지 확인
- [ ] `fromSofaScoreStatus()`가 `type="inprogress"`, `code=7`일 때 `HALF_TIME`을 반환하는지 확인
- [ ] `fromSofaScoreStatus()`가 `type="inprogress"`, `code=6`일 때 `FIRST_HALF`를 반환하는지 확인
- [ ] `FINISHED.isFinished`가 `true`인지 확인
- [ ] `LIVE.isLive`가 `true`인지 확인
- [ ] `NOT_STARTED.isClickable`이 `false`인지 확인
- [ ] `core-model` 모듈 빌드 성공 확인

---

## Step 4 — core-domain: Repository 인터페이스 + UseCase

### 목표
> `FixtureRepository` 인터페이스와 `GetFixturesByDateUseCase`를 정의합니다. UseCase에서 리그별 그룹핑 로직을 수행합니다.

### 작업 내용

UseCase는 단순 위임이 아니라 **비즈니스 로직**을 포함합니다. 여기서는 경기 목록을 리그별로 그룹핑(`groupBy`)하는 로직이 UseCase에 들어갑니다.

> 💡 **Tip:** `groupBy { it.league }`의 결과는 `Map<LeagueInfo, List<Fixture>>`입니다. `LeagueInfo`가 data class이므로 `equals()`가 자동 구현되어 같은 리그의 경기들이 올바르게 그룹핑됩니다.

**파일 경로:** `core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/repository/FixtureRepository.kt`

```kotlin
// core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/repository/FixtureRepository.kt
package com.chase1st.feetballfootball.core.domain.repository

import com.chase1st.feetballfootball.core.model.Fixture
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface FixtureRepository {
    fun getFixturesByDate(date: LocalDate): Flow<List<Fixture>>
}
```

**파일 경로:** `core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/usecase/GetFixturesByDateUseCase.kt`

```kotlin
// core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/usecase/GetFixturesByDateUseCase.kt
package com.chase1st.feetballfootball.core.domain.usecase

import com.chase1st.feetballfootball.core.domain.repository.FixtureRepository
import com.chase1st.feetballfootball.core.model.Fixture
import com.chase1st.feetballfootball.core.model.LeagueInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class GetFixturesByDateUseCase @Inject constructor(
    private val fixtureRepository: FixtureRepository,
) {
    /**
     * 날짜별 경기 목록을 리그별로 그룹핑하여 반환
     */
    operator fun invoke(date: LocalDate): Flow<Map<LeagueInfo, List<Fixture>>> =
        fixtureRepository.getFixturesByDate(date)
            .map { fixtures ->
                fixtures.groupBy { it.league }
            }
}
```

> ⚠️ **주의:** Repository는 `Flow<List<Fixture>>`를 반환하고, UseCase가 `Flow<Map<LeagueInfo, List<Fixture>>>`로 변환합니다. 이렇게 하면 Repository는 단순히 데이터를 가져오고, 비즈니스 로직(그룹핑)은 UseCase가 담당합니다.

### ✅ 검증
- [ ] `FixtureRepository.getFixturesByDate()`가 `LocalDate`를 파라미터로 받는지 확인
- [ ] `GetFixturesByDateUseCase`가 `operator fun invoke()`로 선언되어 있는지 확인
- [ ] UseCase의 반환 타입이 `Flow<Map<LeagueInfo, List<Fixture>>>`인지 확인
- [ ] `core-domain` 모듈 빌드 성공 확인

---

## Step 5 — core-data: Mapper + Repository 구현

### 목표
> `FixtureMapper`로 DTO를 Domain Model로 변환하고, `FixtureRepositoryImpl`에서 API 호출 + 필터링 + 정렬을 수행합니다. `DataModule`에 Repository 바인딩을 추가합니다.

### 작업 내용

`FixtureMapper`에는 두 가지 핵심 로직이 있습니다:
1. **필터링:** `SupportedLeagues.ALL_LEAGUE_IDS`에 포함된 리그만 남깁니다 (SofaScore API는 모든 스포츠 이벤트를 반환하므로, `tournament.uniqueTournament.id`로 필터링)
2. **정렬:** 경기 시작 시간 순으로 정렬합니다

SofaScore 응답은 `events[]` 플랫 배열이므로, 직접 `filter`와 `map`을 적용합니다.

> ⚠️ **주의:** SofaScore `/api/v1/sport/football/{date}/events/0` API는 해당 날짜의 **모든 축구 리그** 경기를 반환합니다. `SupportedLeagues.ALL_LEAGUE_IDS`로 `tournament.uniqueTournament.id`를 필터링하지 않으면 불필요한 데이터가 UI에 노출됩니다.

> ⚠️ **주의:** SofaScore는 팀 로고와 리그 로고 URL을 응답에 포함하지 않습니다. 대신 SofaScore 이미지 CDN 패턴으로 직접 구성합니다:
> - 팀 로고: `https://img.sofascore.com/api/v1/team/{teamId}/image`
> - 리그 로고: `https://img.sofascore.com/api/v1/unique-tournament/{tournamentId}/image`

**파일 경로:** `core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/mapper/FixtureMapper.kt`

```kotlin
// core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/mapper/FixtureMapper.kt
package com.chase1st.feetballfootball.core.data.mapper

import com.chase1st.feetballfootball.core.model.*
import com.chase1st.feetballfootball.core.network.model.MatchesDayResponseDto
import com.chase1st.feetballfootball.core.network.model.MatchesDayLeagueDto
import com.chase1st.feetballfootball.core.network.model.MatchItemDto
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class FixtureMapper @Inject constructor() {

    fun mapMatchesDayResponse(dto: MatchesDayResponseDto): List<Fixture> {
        return dto.leagues
            .filter { it.id in SupportedLeagues.ALL_LEAGUE_IDS }
            .flatMap { league ->
                league.matches.map { match ->
                    mapMatch(match, league)
                }
            }
            .sortedBy { it.date }
    }

    private fun mapMatch(dto: MatchItemDto, league: MatchesDayLeagueDto): Fixture = Fixture(
        id = dto.id,
        date = parseUtcTime(dto.status.utcTime),
        status = MatchStatus.fromSofaScoreStatus(dto.status),
        elapsed = null,  // SofaScore 날짜별 경기 API에는 elapsed 없음
        venue = null,
        league = LeagueInfo(
            id = league.id,
            name = league.name,
            country = league.ccode,
            logoUrl = "https://img.sofascore.com/api/v1/unique-tournament/${league.id}/image",
        ),
        homeTeam = Team(
            id = dto.home.id,
            name = dto.home.longName.ifEmpty { dto.home.name },
            logoUrl = "https://img.sofascore.com/api/v1/team/${dto.home.id}/image",
        ),
        awayTeam = Team(
            id = dto.away.id,
            name = dto.away.longName.ifEmpty { dto.away.name },
            logoUrl = "https://img.sofascore.com/api/v1/team/${dto.away.id}/image",
        ),
        homeGoals = dto.home.score,
        awayGoals = dto.away.score,
    )

    private fun parseUtcTime(utcTime: String): LocalDateTime = try {
        LocalDateTime.parse(utcTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    } catch (e: Exception) {
        LocalDateTime.now()
    }
}
```

> 💡 **Tip:** SofaScore는 `startTimestamp`를 Unix 초 단위로 반환합니다. `Instant.ofEpochSecond()`로 변환 후 `LocalDateTime`으로 파싱합니다. 파싱 실패 시 `LocalDateTime.now()`를 기본값으로 사용하여 크래시를 방지합니다.

**파일 경로:** `core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/repository/FixtureRepositoryImpl.kt`

```kotlin
// core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/repository/FixtureRepositoryImpl.kt
package com.chase1st.feetballfootball.core.data.repository

import com.chase1st.feetballfootball.core.common.dispatcher.IoDispatcher
import com.chase1st.feetballfootball.core.data.mapper.FixtureMapper
import com.chase1st.feetballfootball.core.domain.repository.FixtureRepository
import com.chase1st.feetballfootball.core.model.Fixture
import com.chase1st.feetballfootball.core.network.api.FootballApiService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class FixtureRepositoryImpl @Inject constructor(
    private val apiService: FootballApiService,
    private val mapper: FixtureMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FixtureRepository {

    override fun getFixturesByDate(date: LocalDate): Flow<List<Fixture>> = flow {
        // SofaScore는 yyyy-MM-dd 형식 사용
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val response = apiService.getMatchesByDate(dateStr)
        emit(mapper.mapMatchesDayResponse(response))
    }.flowOn(ioDispatcher)
}
```

**파일 경로:** `core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/di/DataModule.kt` (기존 파일에 추가)

Slice 2에서 만든 `DataModule`에 `FixtureRepository` 바인딩을 추가합니다.

```kotlin
// core-data/di/DataModule.kt에 추가
@Binds
abstract fun bindFixtureRepository(impl: FixtureRepositoryImpl): FixtureRepository
```

전체 `DataModule.kt`는 다음과 같습니다:

```kotlin
// core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/di/DataModule.kt
package com.chase1st.feetballfootball.core.data.di

import com.chase1st.feetballfootball.core.data.repository.FixtureRepositoryImpl
import com.chase1st.feetballfootball.core.data.repository.LeagueRepositoryImpl
import com.chase1st.feetballfootball.core.domain.repository.FixtureRepository
import com.chase1st.feetballfootball.core.domain.repository.LeagueRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindLeagueRepository(impl: LeagueRepositoryImpl): LeagueRepository

    @Binds
    abstract fun bindFixtureRepository(impl: FixtureRepositoryImpl): FixtureRepository
}
```

### ✅ 검증
- [ ] `FixtureMapper`에 `@Inject constructor()`가 있는지 확인
- [ ] `mapMatchesDayResponse()`가 `leagues`를 `SupportedLeagues.ALL_LEAGUE_IDS`로 필터링하는지 확인
- [ ] `flatMap`으로 모든 리그의 경기를 하나의 리스트로 펼치는지 확인
- [ ] `parseUtcTime()`이 ISO_OFFSET_DATE_TIME 형식을 파싱하는지 확인
- [ ] `parseUtcTime()` 실패 시 `LocalDateTime.now()`를 반환하는지 확인 (크래시 방지)
- [ ] 팀 로고 URL이 `https://img.sofascore.com/api/v1/team/{teamId}/image` 형식인지 확인
- [ ] 리그 로고 URL이 `https://img.sofascore.com/api/v1/unique-tournament/{id}/image` 형식인지 확인
- [ ] `FixtureRepositoryImpl`이 날짜를 `yyyy-MM-dd` 형식으로 변환하는지 확인
- [ ] `sortedBy { it.date }`로 시간순 정렬하는지 확인
- [ ] `DataModule`에 `bindFixtureRepository()`가 추가되었는지 확인
- [ ] `core-data` 모듈 빌드 성공 확인

---

## Step 6 — feature-fixture: ViewModel + UiState

### 목표
> 경기 일정의 ViewModel과 UI 상태를 정의합니다. 날짜 변경 시 `flatMapLatest`로 자동 API 재호출합니다.

### 작업 내용

`FixtureViewModel`의 핵심은 `selectedDate` StateFlow를 `flatMapLatest`로 변환하는 것입니다:
- 사용자가 날짜를 변경하면 `selectedDate`에 새 값이 emit됩니다
- `flatMapLatest`는 이전 API 호출을 취소하고 새 날짜로 재호출합니다
- 결과가 비어있으면 `Empty` 상태, 아니면 `Success` 상태를 emit합니다

> 💡 **Tip:** `flatMapLatest`는 새 값이 emit되면 이전 flow를 **자동 취소**합니다. 사용자가 빠르게 날짜를 여러 번 변경해도 마지막 날짜의 API 호출만 완료됩니다.

**파일 경로:** `feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/FixtureUiState.kt`

```kotlin
// feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/FixtureUiState.kt
package com.chase1st.feetballfootball.feature.fixture

import com.chase1st.feetballfootball.core.model.Fixture
import com.chase1st.feetballfootball.core.model.LeagueInfo
import java.time.LocalDate

sealed interface FixtureUiState {
    data object Loading : FixtureUiState
    data class Success(
        val fixturesByLeague: Map<LeagueInfo, List<Fixture>>,
        val selectedDate: LocalDate,
    ) : FixtureUiState
    data class Empty(val selectedDate: LocalDate) : FixtureUiState
    data class Error(val message: String) : FixtureUiState
}

sealed interface FixtureEvent {
    data class SelectDate(val date: LocalDate) : FixtureEvent
    data class SelectFixture(val fixtureId: Int) : FixtureEvent
}
```

> 💡 **Tip:** `FixtureEvent`를 sealed interface로 정의하면 ViewModel의 `onEvent()` 함수에서 모든 이벤트를 `when`으로 처리할 수 있습니다. MVI 패턴의 단방향 데이터 흐름입니다.

**파일 경로:** `feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/FixtureViewModel.kt`

```kotlin
// feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/FixtureViewModel.kt
package com.chase1st.feetballfootball.feature.fixture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chase1st.feetballfootball.core.domain.usecase.GetFixturesByDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class FixtureViewModel @Inject constructor(
    private val getFixturesByDateUseCase: GetFixturesByDateUseCase,
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<FixtureUiState> = selectedDate
        .flatMapLatest { date ->
            flow {
                emit(FixtureUiState.Loading)
                try {
                    getFixturesByDateUseCase(date).collect { fixtures ->
                        if (fixtures.isEmpty()) {
                            emit(FixtureUiState.Empty(date))
                        } else {
                            emit(FixtureUiState.Success(fixtures, date))
                        }
                    }
                } catch (e: Exception) {
                    emit(FixtureUiState.Error(e.message ?: "오류가 발생했습니다"))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FixtureUiState.Loading)

    fun onEvent(event: FixtureEvent) {
        when (event) {
            is FixtureEvent.SelectDate -> selectedDate.value = event.date
            is FixtureEvent.SelectFixture -> { /* Navigation 3에서 처리 */ }
        }
    }
}
```

> ⚠️ **주의:** `SharingStarted.WhileSubscribed(5_000)`은 마지막 구독자가 사라진 후 5초간 flow를 유지합니다. 화면 회전 등으로 잠시 구독이 끊겨도 API를 다시 호출하지 않습니다.

### ✅ 검증
- [ ] `FixtureUiState`에 `Loading`, `Success`, `Empty`, `Error` 4개 상태가 있는지 확인
- [ ] `FixtureEvent`에 `SelectDate`, `SelectFixture` 2개 이벤트가 있는지 확인
- [ ] `FixtureViewModel`이 `@HiltViewModel`로 선언되어 있는지 확인
- [ ] `selectedDate`가 `MutableStateFlow(LocalDate.now())`로 초기화되는지 확인
- [ ] `flatMapLatest`로 날짜 변경 시 자동 재호출하는지 확인
- [ ] `onEvent()`에서 `SelectDate` 이벤트를 처리하는지 확인

---

## Step 7 — feature-fixture: Compose UI

### 목표
> 경기 일정의 Compose UI를 구현합니다. 날짜 선택기, 경기 아이템, 전체 화면 3개의 Composable을 만듭니다.

### 작업 내용

이 Step에서는 3개의 Composable 파일을 만듭니다:
1. **DateSelector.kt** — 이전/다음 날짜 버튼 + 현재 날짜 표시
2. **FixtureItem.kt** — 개별 경기 아이템 (홈팀 vs 원정팀 + 스코어/시간)
3. **FixtureScreen.kt** — 전체 화면 (DateSelector + 리그별 그룹 LazyColumn)

**파일 경로:** `feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/component/DateSelector.kt`

```kotlin
// feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/component/DateSelector.kt
package com.chase1st.feetballfootball.feature.fixture.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// 참조: FixtureFragment.kt의 날짜 네비게이션 (이전/다음 버튼)

@Composable
fun DateSelector(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "이전 날짜")
        }
        Text(
            text = selectedDate.format(formatter),
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onNextDay) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "다음 날짜")
        }
    }
}
```

> 💡 **Tip:** `DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)")`는 `"2026년 3월 15일 (일)"` 형태로 날짜를 포맷합니다. `(E)`는 요일 약어입니다. 한국어 로케일이 시스템 기본이면 자동으로 한국어로 표시됩니다.

**파일 경로:** `feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/component/FixtureItem.kt`

```kotlin
// feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/component/FixtureItem.kt
package com.chase1st.feetballfootball.feature.fixture.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chase1st.feetballfootball.core.designsystem.component.TeamLogo
import com.chase1st.feetballfootball.core.model.Fixture
import java.time.format.DateTimeFormatter

// 참조: FixtureRecyclerViewAdapter.kt의 상태별 표시 로직

@Composable
fun FixtureItem(
    fixture: Fixture,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = fixture.status.isClickable, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 홈팀
        TeamLogo(logoUrl = fixture.homeTeam.logoUrl, teamName = fixture.homeTeam.name, size = 32.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = fixture.homeTeam.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )

        // 스코어/시간
        FixtureStatus(fixture = fixture, modifier = Modifier.width(64.dp))

        // 원정팀
        Text(
            text = fixture.awayTeam.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            textAlign = TextAlign.End,
        )
        Spacer(modifier = Modifier.width(8.dp))
        TeamLogo(logoUrl = fixture.awayTeam.logoUrl, teamName = fixture.awayTeam.name, size = 32.dp)
    }
}

@Composable
private fun FixtureStatus(fixture: Fixture, modifier: Modifier = Modifier) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            fixture.status.isFinished || fixture.status.isLive -> {
                Text(
                    text = "${fixture.homeGoals ?: 0} - ${fixture.awayGoals ?: 0}",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (fixture.status.isLive) {
                    Text(
                        text = fixture.status.displayText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            fixture.status == com.chase1st.feetballfootball.core.model.MatchStatus.POSTPONED -> {
                Text("연기됨", style = MaterialTheme.typography.labelSmall)
            }
            fixture.status == com.chase1st.feetballfootball.core.model.MatchStatus.CANCELLED -> {
                Text("취소됨", style = MaterialTheme.typography.labelSmall)
            }
            else -> {
                Text(
                    text = fixture.date.format(timeFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
```

> 💡 **Tip:** `FixtureStatus` Composable은 경기 상태에 따라 4가지로 분기합니다:
> - **종료/라이브:** 스코어 표시 (라이브면 상태 텍스트를 빨간색으로 표시. SofaScore 날짜별 경기 API에는 elapsed 분 정보가 없으므로 `displayText`를 사용)
> - **연기:** "연기됨" 텍스트
> - **취소:** "취소됨" 텍스트
> - **예정:** 시작 시간 (HH:mm 형식)

> ⚠️ **주의:** `clickable(enabled = fixture.status.isClickable)`로 클릭 가능 여부를 제어합니다. 아직 시작하지 않은 경기나 연기/취소된 경기는 클릭할 수 없습니다.

**파일 경로:** `feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/FixtureScreen.kt`

```kotlin
// feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/FixtureScreen.kt
package com.chase1st.feetballfootball.feature.fixture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel  // 1.3.0부터 신규 경로
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chase1st.feetballfootball.core.designsystem.component.ErrorContent
import com.chase1st.feetballfootball.core.designsystem.component.FeetballLoadingIndicator
import com.chase1st.feetballfootball.core.designsystem.component.TeamLogo
import com.chase1st.feetballfootball.core.model.Fixture
import com.chase1st.feetballfootball.core.model.LeagueInfo
import com.chase1st.feetballfootball.feature.fixture.component.DateSelector
import com.chase1st.feetballfootball.feature.fixture.component.FixtureItem

@Composable
fun FixtureScreen(
    onFixtureClick: (fixtureId: Int) -> Unit,
    viewModel: FixtureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is FixtureUiState.Loading -> FeetballLoadingIndicator()
        is FixtureUiState.Error -> ErrorContent(
            message = state.message,
            onRetry = { viewModel.onEvent(FixtureEvent.SelectDate(java.time.LocalDate.now())) },
        )
        is FixtureUiState.Empty -> {
            Column {
                DateSelector(
                    selectedDate = state.selectedDate,
                    onPreviousDay = { viewModel.onEvent(FixtureEvent.SelectDate(state.selectedDate.minusDays(1))) },
                    onNextDay = { viewModel.onEvent(FixtureEvent.SelectDate(state.selectedDate.plusDays(1))) },
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text("경기가 없습니다", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        is FixtureUiState.Success -> {
            FixtureList(
                fixturesByLeague = state.fixturesByLeague,
                selectedDate = state.selectedDate,
                onPreviousDay = { viewModel.onEvent(FixtureEvent.SelectDate(state.selectedDate.minusDays(1))) },
                onNextDay = { viewModel.onEvent(FixtureEvent.SelectDate(state.selectedDate.plusDays(1))) },
                onFixtureClick = onFixtureClick,
            )
        }
    }
}

@Composable
private fun FixtureList(
    fixturesByLeague: Map<LeagueInfo, List<Fixture>>,
    selectedDate: java.time.LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onFixtureClick: (Int) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            DateSelector(
                selectedDate = selectedDate,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
            )
        }

        fixturesByLeague.forEach { (league, fixtures) ->
            // 리그 헤더 (참조: FixtureFragment.kt의 LeagueFixtureHolder)
            stickyHeader {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        TeamLogo(logoUrl = league.logoUrl, teamName = league.name, size = 24.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = league.name, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            items(items = fixtures, key = { it.id }) { fixture ->
                FixtureItem(
                    fixture = fixture,
                    onClick = { onFixtureClick(fixture.id) },
                )
                HorizontalDivider()
            }
        }
    }
}
```

> 💡 **Tip:** `stickyHeader`는 스크롤 시 리그 헤더가 화면 상단에 고정됩니다. 기존 `FixtureFragment.kt`의 `LeagueFixtureHolder`와 동일한 UX를 제공합니다.

> ⚠️ **주의:** `LazyColumn` 안에서 `stickyHeader`를 사용하려면 `@OptIn(ExperimentalFoundationApi::class)`가 필요할 수 있습니다 (Compose 버전에 따라 다름). 빌드 시 경고가 나오면 추가하세요.

### ✅ 검증
- [ ] `DateSelector`에 이전/다음 날짜 버튼과 현재 날짜 텍스트가 있는지 확인
- [ ] `FixtureItem`에 홈팀 로고/이름, 스코어/시간, 원정팀 이름/로고가 표시되는지 확인
- [ ] `FixtureStatus`가 경기 상태별로 올바르게 분기하는지 확인:
  - 미시작 → 시작 시간 (HH:mm)
  - 종료(FT) → 스코어
  - 진행중(started) → 스코어 + 상태 텍스트 (빨간색)
  - 연기(PP) → "연기됨"
  - 취소(Canc) → "취소됨"
- [ ] `FixtureScreen`에서 Loading/Error/Empty/Success 4개 상태를 모두 처리하는지 확인
- [ ] `FixtureList`에서 리그별 `stickyHeader`가 있는지 확인
- [ ] `feature-fixture` 모듈 빌드 성공 확인

---

## Step 8 — 빌드 및 최종 검증

### 목표
> 전체 빌드를 수행하고 에뮬레이터/기기에서 경기 일정 화면을 검증합니다.

### 작업 내용

```bash
./gradlew assembleDebug
# 앱 실행 → 에뮬레이터/기기
```

### ✅ 검증
- [ ] 앱 실행 시 오늘 날짜 경기 목록 표시
- [ ] 이전/다음 날짜 버튼 동작 (API 재호출)
- [ ] 리그별 그룹핑 + stickyHeader 표시
- [ ] 경기 상태별 분기: 시간(미시작) / 스코어(FT) / 라이브(started) / 연기(PP) / 취소(Canc)
- [ ] 종료/라이브 경기만 클릭 가능
- [ ] 경기 없는 날짜에 빈 상태 표시
- [ ] `git commit -m "feat: Slice 3 경기 일정 화면 구현"`

---

## 🎉 Slice 3 완료!

축하합니다! 앱의 메인 화면을 완성했습니다.

**이 Slice에서 달성한 것:**
- Slice 2에서 확립한 수직 스택 패턴을 새 feature에 성공적으로 반복 적용했습니다
- `flatMapLatest`를 사용한 반응형 날짜 선택을 구현했습니다
- `MatchStatus` enum으로 경기 상태 로직을 깔끔하게 캡슐화했습니다
- Compose `stickyHeader`로 리그별 그룹핑 UI를 구현했습니다
- MVI 패턴(`FixtureEvent`)으로 단방향 데이터 흐름을 적용했습니다

**Stage 2 / Slice 1~3 완료 후 앱 상태:**
- 리그 선택 화면 (정적 데이터)
- 리그 순위 화면 (팀 순위 + 개인 순위, API 연동)
- 경기 일정 화면 (날짜 선택 + 리그별 그룹핑 + 상태별 표시, API 연동)

**다음 단계:** Navigation 3 통합으로 화면 간 이동을 구현하거나, 경기 상세 화면(Slice 4)을 진행합니다.


---

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


---

# Stage 2 / Slice 5 — 경기 상세 + 3탭 (이벤트/라인업/통계)

> ⏱ 예상 소요 시간: 8시간 | 난이도: ★★★ | 선행 조건: Slice 3 완료 (Fixture Domain Model 존재)

---

## 이 Codelab에서 배우는 것

- **SofaScore 다중 API 호출**: 하나의 화면에 여러 엔드포인트를 병렬 호출하여 데이터 조합
- **API 응답 → DTO → Domain Model** 전체 레이어 매핑 (가장 복잡한 Mapper 구현)
- **`kotlinx.serialization`** 으로 중첩 JSON 구조 역직렬화 (이벤트, 라인업, 통계, 선수 레이팅)
- **Compose `HorizontalPager` + `TabRow`** 로 3개 서브탭 구현
- **`LargeTopAppBar` + `exitUntilCollapsedScrollBehavior`** 로 접히는 헤더 구현
- **Hilt `SavedStateHandle`** 을 통한 Navigation argument 주입
- **포메이션 배치**: formation 문자열 기반 시각적 배치
- **`LinearProgressIndicator`** 로 홈/어웨이 통계 비교 UI
- **조건부 탭 표시**: 데이터 유무에 따라 탭을 동적으로 구성
- **`coroutineScope { async {} }`**: 여러 API를 병렬로 호출하는 패턴

---

## 완성 후 결과물

- SofaScore의 여러 API 엔드포인트(event, incidents, lineups, statistics)를 병렬 호출하여 경기 상세 데이터를 조합합니다.
- 경기 목록에서 경기를 선택하면 상세 화면으로 이동합니다.
- 매치 헤더에 팀 로고, 이름, 스코어, 골 스코어러가 표시됩니다.
- 이벤트 탭: 골/카드/교체/VAR 등 경기 이벤트가 타임라인으로 표시됩니다.
- 라인업 탭: 포메이션 그리드, 선수 카드(번호/이름/레이팅), 감독, 교체 선수가 표시됩니다.
- 통계 탭: 그룹별 통계 항목이 ProgressBar로 홈/어웨이 비교 표시됩니다.
- 데이터가 없는 탭(연기/취소된 경기)은 자동으로 숨겨집니다.
- TopAppBar가 스크롤에 따라 접히고 펼쳐집니다.

---

## 실행 전략

이 Slice는 프로젝트에서 가장 복잡한 화면입니다. 내부적으로 두 Phase로 나눠서 진행합니다:

```
Phase 5-A: 하위 탭 3개 독립 구현 (병렬 가능)
  ├── EventsTab    (Step 11)
  ├── StatisticsTab (Step 12)
  └── LineupTab    (Step 13)

Phase 5-B: FixtureDetailScreen에서 조립 (Step 14)
```

Step 1~4는 SofaScore 다중 엔드포인트별 DTO, Step 5는 API 엔드포인트(4개 suspend 함수), Step 6~10은 하위 레이어(Domain Model, Mapper, UseCase, ViewModel)를 순서대로 구축합니다.

---

## Step 1 — core-network: SofaScore 경기 상세 DTO 작성

### 목표

> SofaScore의 여러 경기 상세 엔드포인트(`/api/v1/event/{eventId}`, `/api/v1/event/{eventId}/incidents`, `/api/v1/event/{eventId}/lineups`, `/api/v1/event/{eventId}/statistics`)의 응답을 역직렬화하기 위한 DTO를 정의합니다. SofaScore는 경기 상세 데이터를 **여러 엔드포인트로 분리**하여 제공하므로, 각 엔드포인트별로 별도의 응답 DTO가 필요합니다.

### 작업 내용

**파일:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/FixtureDetailDto.kt`

이 파일을 새로 생성합니다.

```kotlin
package com.chase1st.feetballfootball.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// SofaScore GET api/v1/event/{eventId} 응답
@Serializable
data class EventResponseDto(
    @SerialName("event") val event: SofaEventDto,
)

@Serializable
data class SofaEventDto(
    @SerialName("id") val id: Int,
    @SerialName("tournament") val tournament: SofaTournamentDto? = null,
    @SerialName("homeTeam") val homeTeam: SofaTeamDto,
    @SerialName("awayTeam") val awayTeam: SofaTeamDto,
    @SerialName("homeScore") val homeScore: SofaScoreDto? = null,
    @SerialName("awayScore") val awayScore: SofaScoreDto? = null,
    @SerialName("status") val status: SofaStatusDto? = null,
    @SerialName("startTimestamp") val startTimestamp: Long? = null,
    @SerialName("venue") val venue: SofaVenueDto? = null,
)

@Serializable
data class SofaTournamentDto(
    @SerialName("uniqueTournament") val uniqueTournament: SofaUniqueTournamentDto? = null,
)

@Serializable
data class SofaUniqueTournamentDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String? = null,
)

@Serializable
data class SofaTeamDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String? = null,
)

@Serializable
data class SofaScoreDto(
    @SerialName("current") val current: Int? = null,
)

@Serializable
data class SofaStatusDto(
    @SerialName("code") val code: Int? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("type") val type: String? = null,
)

@Serializable
data class SofaVenueDto(
    @SerialName("city") val city: SofaCityDto? = null,
    @SerialName("stadium") val stadium: SofaStadiumDto? = null,
)

@Serializable
data class SofaCityDto(
    @SerialName("name") val name: String? = null,
)

@Serializable
data class SofaStadiumDto(
    @SerialName("name") val name: String? = null,
)
```

**WHY:** SofaScore는 하나의 API로 모든 경기 상세 데이터를 반환하지 않습니다. 대신 `event`, `incidents`, `lineups`, `statistics` 등 **여러 엔드포인트로 분리**하여 제공합니다. 따라서 각 엔드포인트별로 별도의 응답 DTO를 정의해야 합니다.

**HOW:** nullable 필드와 기본값 `null`을 적극 사용하여, 경기 상태에 따라 일부 데이터가 없는 경우에도 역직렬화가 실패하지 않도록 합니다. 팀 로고 URL은 DTO에 포함되지 않고, `https://img.sofascore.com/api/v1/team/{id}/image` 패턴으로 별도 구성합니다.

> 💡 **Tip:** SofaScore 이미지 URL 패턴:
> - 팀 로고: `https://img.sofascore.com/api/v1/team/{id}/image`
> - 리그 로고: `https://img.sofascore.com/api/v1/unique-tournament/{id}/image`
> - 선수 사진: `https://img.sofascore.com/api/v1/player/{id}/image`

### ✅ 검증

- [ ] 모든 DTO 클래스에 `@Serializable` 어노테이션이 있다
- [ ] nullable 필드에 기본값 `null`이 설정되어 있다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 2 — core-network: IncidentDto 작성 (SofaScore Incidents)

### 목표

> SofaScore `/api/v1/event/{eventId}/incidents` 엔드포인트의 응답을 역직렬화하기 위한 DTO를 정의합니다. SofaScore는 골, 카드, 교체, 기간(전반/후반) 등을 `incidents` 배열로 통합하여 제공합니다.

### 작업 내용

**파일:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/IncidentDto.kt`

이 파일을 새로 생성합니다.

```kotlin
package com.chase1st.feetballfootball.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// SofaScore GET api/v1/event/{eventId}/incidents 응답
@Serializable
data class IncidentsResponseDto(
    @SerialName("incidents") val incidents: List<IncidentDto> = emptyList(),
)

@Serializable
data class IncidentDto(
    @SerialName("incidentType") val incidentType: String,        // "goal", "card", "substitution", "period", "injuryTime", "var"
    @SerialName("time") val time: Int? = null,
    @SerialName("addedTime") val addedTime: Int? = null,
    @SerialName("player") val player: IncidentPlayerDto? = null,
    @SerialName("assist1") val assist1: IncidentPlayerDto? = null,
    @SerialName("playerIn") val playerIn: IncidentPlayerDto? = null,   // 교체 들어온 선수
    @SerialName("playerOut") val playerOut: IncidentPlayerDto? = null,  // 교체 나간 선수
    @SerialName("isHome") val isHome: Boolean? = null,
    @SerialName("homeScore") val homeScore: Int? = null,
    @SerialName("awayScore") val awayScore: Int? = null,
    @SerialName("incidentClass") val incidentClass: String? = null,    // "regular", "yellow", "red", "yellowRed", "ownGoal", "penalty", "missedPenalty"
    @SerialName("text") val text: String? = null,                      // "HT", "FT" 등 period 이벤트에서 사용
)

@Serializable
data class IncidentPlayerDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("shortName") val shortName: String? = null,
)
```

**WHY:** SofaScore의 incidents 응답은 모든 경기 이벤트를 하나의 배열에 통합합니다. `incidentType`으로 이벤트 종류를 구분하고, `incidentClass`로 세부 유형(일반골/자책골/페널티, 옐로/레드 카드 등)을 구분합니다.

**HOW:**
- `incidentType` + `incidentClass` 조합으로 이벤트 유형을 구분합니다. 예: `incidentType="goal"` + `incidentClass="ownGoal"` → 자책골
- `player`는 골/카드 이벤트의 주체 선수입니다.
- `assist1`은 어시스트 선수입니다. 어시스트가 없는 이벤트에서는 `null`입니다.
- `playerIn`/`playerOut`은 교체 이벤트(`incidentType="substitution"`)에서만 사용됩니다.
- `addedTime`은 추가 시간(예: 90+3분의 3)을 나타냅니다. 정규 시간에는 `null`입니다.
- `isHome`이 `null`인 경우는 `period` 이벤트(전반/후반 구분선) 등입니다.

> ⚠️ **주의:** SofaScore의 `incidentType` + `incidentClass` 매핑은 Step 6의 Domain Model(`EventType.from()`)에서 처리합니다. 주요 매핑:
> - `incidentType="goal"` + `incidentClass="regular"` → 일반 골
> - `incidentType="goal"` + `incidentClass="ownGoal"` → 자책골
> - `incidentType="goal"` + `incidentClass="penalty"` → 페널티 골
> - `incidentType="card"` + `incidentClass="yellow"` → 옐로 카드
> - `incidentType="card"` + `incidentClass="red"` → 레드 카드
> - `incidentType="card"` + `incidentClass="yellowRed"` → 경고 누적 퇴장

### ✅ 검증

- [ ] 모든 DTO 클래스에 `@Serializable` 어노테이션이 있다
- [ ] nullable 필드에 기본값 `null`이 설정되어 있다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 3 — core-network: LineupsDto 작성 (SofaScore Lineups)

### 목표

> SofaScore `/api/v1/event/{eventId}/lineups` 엔드포인트의 응답을 역직렬화하기 위한 DTO를 정의합니다. SofaScore의 라인업은 `home`/`away` 객체로 분리되며, 각 팀의 `players` 배열에서 `substitute: boolean`으로 선발/교체를 구분합니다.

### 작업 내용

**파일:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/LineupDto.kt`

이 파일을 새로 생성합니다.

```kotlin
package com.chase1st.feetballfootball.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// SofaScore GET api/v1/event/{eventId}/lineups 응답
@Serializable
data class LineupsResponseDto(
    @SerialName("home") val home: TeamLineupDto? = null,
    @SerialName("away") val away: TeamLineupDto? = null,
    @SerialName("confirmed") val confirmed: Boolean? = null,
)

@Serializable
data class TeamLineupDto(
    @SerialName("players") val players: List<LineupPlayerEntryDto> = emptyList(),
    @SerialName("formation") val formation: String? = null,
    @SerialName("playerColor") val playerColor: LineupColorDto? = null,
    @SerialName("goalkeeperColor") val goalkeeperColor: LineupColorDto? = null,
)

@Serializable
data class LineupColorDto(
    @SerialName("primary") val primary: String? = null,
    @SerialName("number") val number: String? = null,
)

@Serializable
data class LineupPlayerEntryDto(
    @SerialName("player") val player: LineupPlayerInfoDto,
    @SerialName("shirtNumber") val shirtNumber: Int? = null,
    @SerialName("jerseyNumber") val jerseyNumber: String? = null,
    @SerialName("position") val position: String? = null,    // "G", "D", "M", "F"
    @SerialName("substitute") val substitute: Boolean = false,
    @SerialName("statistics") val statistics: LineupPlayerStatDto? = null,
    @SerialName("captain") val captain: Boolean? = null,
)

@Serializable
data class LineupPlayerInfoDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String? = null,
    @SerialName("shortName") val shortName: String? = null,
    @SerialName("position") val position: String? = null,    // "G", "D", "M", "F"
)

@Serializable
data class LineupPlayerStatDto(
    @SerialName("rating") val rating: Double? = null,
    @SerialName("minutesPlayed") val minutesPlayed: Int? = null,
)
```

**WHY:** SofaScore의 라인업 구조는 `home`/`away`로 팀이 분리되어 있으며, 각 팀의 `players` 배열에 모든 선수가 포함됩니다. `substitute: boolean` 필드로 선발(false)과 교체(true)를 구분합니다. `grid` 필드는 없으며, 포메이션 문자열에서 선수 배치를 유추합니다.

**HOW:**
- `substitute` 필드가 `false`이면 선발, `true`이면 교체 선수입니다.
- `position` 필드는 `"G"` (골키퍼), `"D"` (수비수), `"M"` (미드필더), `"F"` (공격수) 중 하나입니다.
- `playerColor.primary`는 팀 유니폼 색상 hex 값입니다 (예: `"#6CABDD"`). 포메이션 UI의 선수 원형 배경색에 사용합니다.
- `statistics.rating`은 선수의 경기 레이팅(예: 7.2)입니다. 별도 API 호출 없이 라인업 응답에 포함됩니다.
- `confirmed`가 `true`이면 라인업이 확정된 상태입니다.

> 💡 **Tip:** SofaScore는 `grid` 필드를 제공하지 않으므로, 포메이션 문자열(예: `"4-3-3"`)을 파싱하여 각 포지션별 선수 수를 결정하고, `position` 필드와 조합하여 선수 배치를 계산합니다. 이 로직은 Mapper(Step 8)에서 구현합니다.

### ✅ 검증

- [ ] 모든 DTO 클래스에 `@Serializable` 어노테이션이 있다
- [ ] nullable 필드에 기본값 `null` 또는 `emptyList()`가 설정되어 있다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 4 — core-network: StatisticsDto 작성 (SofaScore Statistics)

### 목표

> SofaScore `/api/v1/event/{eventId}/statistics` 엔드포인트의 응답을 역직렬화하기 위한 DTO를 정의합니다. SofaScore의 통계는 `period`(ALL, 1ST, 2ND)별로 분류되고, 각 period 내에서 `groups`로 그룹핑됩니다.

### 작업 내용

**파일:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/StatisticsDto.kt`

이 파일을 새로 생성합니다.

```kotlin
package com.chase1st.feetballfootball.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// SofaScore GET api/v1/event/{eventId}/statistics 응답
@Serializable
data class EventStatisticsResponseDto(
    @SerialName("statistics") val statistics: List<PeriodStatisticsDto> = emptyList(),
)

@Serializable
data class PeriodStatisticsDto(
    @SerialName("period") val period: String,             // "ALL", "1ST", "2ND"
    @SerialName("groups") val groups: List<StatGroupDto> = emptyList(),
)

@Serializable
data class StatGroupDto(
    @SerialName("groupName") val groupName: String,       // "Possession", "Shots", "TVData", etc.
    @SerialName("statisticsItems") val statisticsItems: List<StatItemDto> = emptyList(),
)

@Serializable
data class StatItemDto(
    @SerialName("name") val name: String,                 // "Ball possession", "Total shots", etc.
    @SerialName("home") val home: String? = null,         // "64%", "18"
    @SerialName("away") val away: String? = null,         // "36%", "8"
    @SerialName("compareCode") val compareCode: Int? = null, // 1=home우세, 2=away우세, 3=동일
    @SerialName("statisticsType") val statisticsType: String? = null, // "positive", "negative"
    @SerialName("valueType") val valueType: String? = null,
    @SerialName("homeValue") val homeValue: Int? = null,  // 64, 18
    @SerialName("awayValue") val awayValue: Int? = null,  // 36, 8
)
```

**WHY:** SofaScore의 통계 구조는 3단계 중첩(`statistics` → `groups` → `statisticsItems`)으로 되어 있습니다. `period="ALL"`이 전체 경기 통계이며, `"1ST"`/`"2ND"`는 전반/후반 통계입니다. 각 그룹(`Possession`, `Shots` 등)에 여러 통계 항목이 포함됩니다.

**HOW:**
- `home`/`away`는 화면에 표시할 문자열(예: `"64%"`, `"18"`)입니다.
- `homeValue`/`awayValue`는 비교 계산용 정수값(예: `64`, `18`)입니다. `LinearProgressIndicator`의 비율 계산에 사용합니다.
- `compareCode`는 어느 팀이 우세한지 나타냅니다 (1=홈 우세, 2=어웨이 우세, 3=동일). UI에서 색상 강조에 활용할 수 있습니다.
- 선수 레이팅은 별도 API가 아닌 Step 3의 `LineupsResponseDto` 내 `LineupPlayerStatDto.rating`에 포함되어 있습니다.

> 💡 **Tip:** 보통 UI에서는 `period="ALL"` 통계만 표시합니다. 전반/후반 통계 토글을 추가하려면 `period` 필터를 구현하면 됩니다.

> ⚠️ **주의:** `StatItemDto.home`/`away`가 `null`일 수 있습니다. 경기 시작 전이거나 해당 통계 항목이 수집되지 않은 경우입니다. nullable 처리가 필수입니다.

### ✅ 검증

- [ ] 모든 DTO 클래스에 `@Serializable` 어노테이션이 있다
- [ ] nullable 필드에 기본값 `null` 또는 `emptyList()`가 설정되어 있다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 4.5 — 인증 참고사항 (SofaScore)

### 배경

> SofaScore API는 복잡한 봇 방지 인증을 사용하지 않습니다. SofaScore의 GET 엔드포인트(`event`, `incidents`, `lineups`, `statistics`)는 **인증 없이 기본 데이터를 조회**할 수 있습니다.

### SofaScore 인증 방식

SofaScore는 Bearer Token 기반 인증을 사용합니다:
- **토큰 초기화:** `POST api/v1/token/init`
- **토큰 갱신:** `POST api/v1/token/refresh`
- OkHttp Interceptor에서 `Authorization: Bearer {token}` 헤더 추가 (non-GET/HEAD 요청)
- 서버가 `X-Token-Refresh` 응답 헤더로 토큰 갱신을 트리거할 수 있음

그러나 **이 Slice에서 사용하는 GET 엔드포인트들은 인증 없이 동작합니다.** Bearer Token은 POST 작업(투표 등)이나 프리미엄 기능에 주로 필요합니다.

### 결론

- **invisible WebView, JavaScript Interface 등의 복잡한 인증은 필요 없습니다.**
- 추후 인증이 필요한 기능(예: 투표, 프리미엄 데이터)을 추가할 때 Bearer Token Interceptor를 구현하면 됩니다.
- 현재는 별도의 인증 코드 없이 바로 API 호출이 가능합니다.

> 💡 **Tip:** SofaScore GET 엔드포인트가 인증 없이 동작하므로, 이 Step은 구현할 코드가 없습니다. 바로 Step 5로 넘어가세요.

---

## Step 5 — core-network: SofaScore API 엔드포인트 추가

### 목표

> `FootballApiService`에 SofaScore 경기 상세 조회를 위한 4개의 엔드포인트를 추가합니다. SofaScore는 경기 상세 데이터를 여러 엔드포인트로 분리하여 제공하므로, 각각에 대한 suspend 함수를 정의합니다.

### 작업 내용

**파일:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/FootballApiService.kt`

기존 파일에 아래 함수들을 추가합니다.

```kotlin
// FootballApiService.kt에 추가
// SofaScore 경기 상세 엔드포인트 (4개)

// 1. 기본 경기 정보 (팀, 스코어, 상태, 경기장 등)
@GET("api/v1/event/{eventId}")
suspend fun getEvent(
    @Path("eventId") eventId: Int,
): EventResponseDto

// 2. 경기 이벤트 (골, 카드, 교체, VAR 등)
@GET("api/v1/event/{eventId}/incidents")
suspend fun getEventIncidents(
    @Path("eventId") eventId: Int,
): IncidentsResponseDto

// 3. 라인업 (선발, 교체, 포메이션, 선수 레이팅)
@GET("api/v1/event/{eventId}/lineups")
suspend fun getEventLineups(
    @Path("eventId") eventId: Int,
): LineupsResponseDto

// 4. 통계 (점유율, 슈팅, 패스 등 그룹별 통계)
@GET("api/v1/event/{eventId}/statistics")
suspend fun getEventStatistics(
    @Path("eventId") eventId: Int,
): EventStatisticsResponseDto
```

**WHY:** SofaScore는 하나의 API로 모든 데이터를 반환하지 않고, 관심사별로 엔드포인트를 분리합니다. 이 설계 덕분에 필요한 데이터만 선택적으로 호출할 수 있고, Repository에서 `coroutineScope { async {} }`로 병렬 호출하여 성능을 최적화할 수 있습니다.

**HOW:** `@Path("eventId")`로 URL 경로에 이벤트 ID를 삽입합니다. 모든 GET 엔드포인트는 인증 없이 호출 가능합니다. Base URL은 `https://api.sofascore.com/`으로 설정되어 있어야 합니다.

> 💡 **Tip:** 4개의 API를 순차 호출하면 응답 시간이 누적됩니다. Repository에서 `coroutineScope { async {} }`를 사용하여 4개를 병렬 호출하면 가장 느린 API의 응답 시간만큼만 소요됩니다. 이 패턴은 Step 8의 Repository 구현에서 다룹니다.

### ✅ 검증

- [ ] `EventResponseDto`, `IncidentsResponseDto`, `LineupsResponseDto`, `EventStatisticsResponseDto` 반환 타입이 정상 resolve 된다
- [ ] 해당 DTO import가 모두 추가되었다
- [ ] 기존 엔드포인트들과 충돌 없이 빌드된다
- [ ] 각 엔드포인트가 인증 없이 200 OK 반환 확인

---

## Step 6 — core-model: Domain Model 작성

### 목표

> 경기 상세 화면에서 사용할 Domain Model을 정의합니다. SofaScore의 다중 엔드포인트 DTO를 UI에서 바로 사용하기 쉬운 형태로 통합 설계합니다.

### 작업 내용

총 4개의 파일을 생성합니다.

---

### 6-1. MatchDetail.kt

**파일:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/MatchDetail.kt`

```kotlin
package com.chase1st.feetballfootball.core.model

data class MatchDetail(
    val fixture: Fixture,
    val events: List<MatchEvent>,
    val lineups: MatchLineups?,
    val statistics: MatchStatistics?,
    val scorers: Map<Int, List<GoalScorer>>,  // teamId → scorers
)

data class GoalScorer(
    val playerName: String,
    val minute: Int,
    val extraMinute: Int?,
    val isPenalty: Boolean,
    val isOwnGoal: Boolean,
)
```

**WHY:** `MatchDetail`은 경기 상세 화면의 최상위 Domain Model입니다. 기존 `Fixture`(Slice 3에서 정의)를 재사용하고, 이벤트/라인업/통계를 추가합니다.

**HOW:**
- `lineups`와 `statistics`는 nullable입니다. 경기 시작 전이나 취소된 경기에는 데이터가 없을 수 있습니다.
- `scorers`는 `Map<Int, List<GoalScorer>>`로, 팀 ID를 키로 사용합니다. MatchHeader에서 홈/어웨이별 골 스코어러를 표시할 때 사용합니다.
- `GoalScorer`의 `isPenalty`와 `isOwnGoal`은 골 옆에 "(P)" 또는 "(OG)" 표시를 위해 필요합니다.

---

### 6-2. MatchEvent.kt

**파일:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/MatchEvent.kt`

```kotlin
package com.chase1st.feetballfootball.core.model

data class MatchEvent(
    val minute: Int,
    val extraMinute: Int?,
    val isHome: Boolean,
    val playerName: String,
    val assistName: String?,
    val type: EventType,
    val detail: String,
    // 교체 이벤트 전용
    val playerInName: String? = null,
    val playerOutName: String? = null,
)

// SofaScore incidentType + incidentClass 기반 매핑
enum class EventType {
    GOAL,
    OWN_GOAL,
    PENALTY_GOAL,
    MISSED_PENALTY,
    YELLOW_CARD,
    RED_CARD,
    SECOND_YELLOW,
    SUBSTITUTION,
    VAR,
    PERIOD,          // 전반/후반 구분선 (HT, FT)
    INJURY_TIME,
    ;

    companion object {
        fun from(incidentType: String, incidentClass: String?): EventType = when {
            incidentType == "goal" && incidentClass == "regular" -> GOAL
            incidentType == "goal" && incidentClass == "ownGoal" -> OWN_GOAL
            incidentType == "goal" && incidentClass == "penalty" -> PENALTY_GOAL
            incidentType == "goal" && incidentClass == "missedPenalty" -> MISSED_PENALTY
            incidentType == "card" && incidentClass == "yellow" -> YELLOW_CARD
            incidentType == "card" && incidentClass == "red" -> RED_CARD
            incidentType == "card" && incidentClass == "yellowRed" -> SECOND_YELLOW
            incidentType == "substitution" -> SUBSTITUTION
            incidentType == "var" -> VAR
            incidentType == "period" -> PERIOD
            incidentType == "injuryTime" -> INJURY_TIME
            else -> GOAL
        }
    }
}
```

**WHY:** `EventType` enum은 SofaScore의 `incidentType`+`incidentClass` 문자열 조합을 타입 안전한 enum으로 변환합니다. UI에서 이벤트 타입별 아이콘과 색상을 결정할 때 `when (event.type)` 으로 분기하기 편리합니다.

**HOW:** `EventType.from()` companion object 함수는 SofaScore incidents 응답의 `incidentType`+`incidentClass` 매핑입니다:

| incidentType | incidentClass | EventType |
|-------------|--------------|-----------|
| `"goal"` | `"regular"` | `GOAL` |
| `"goal"` | `"ownGoal"` | `OWN_GOAL` |
| `"goal"` | `"penalty"` | `PENALTY_GOAL` |
| `"goal"` | `"missedPenalty"` | `MISSED_PENALTY` |
| `"card"` | `"yellow"` | `YELLOW_CARD` |
| `"card"` | `"red"` | `RED_CARD` |
| `"card"` | `"yellowRed"` | `SECOND_YELLOW` |
| `"substitution"` | - | `SUBSTITUTION` |
| `"var"` | - | `VAR` |
| `"period"` | - | `PERIOD` |
| `"injuryTime"` | - | `INJURY_TIME` |

> ⚠️ **주의:** `else -> GOAL` fallback은 예상하지 못한 이벤트 타입에 대한 안전장치입니다. `PERIOD` 타입(HT, FT)은 UI에서 구분선으로 표시합니다. SofaScore는 `isHome` 필드로 홈/어웨이를 직접 구분하므로, 기존의 `team` 객체 대신 `isHome: Boolean`을 사용합니다.

---

### 6-3. MatchLineups.kt

**파일:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/MatchLineups.kt`

```kotlin
package com.chase1st.feetballfootball.core.model

data class MatchLineups(
    val home: TeamLineup,
    val away: TeamLineup,
)

data class TeamLineup(
    val team: Team,
    val formation: String,
    val coachName: String,
    val coachPhotoUrl: String?,
    val teamColorHex: String?,
    val startingXI: List<LineupPlayer>,
    val substitutes: List<LineupPlayer>,
)

data class LineupPlayer(
    val id: Int,
    val name: String,
    val number: Int,
    val position: String?,
    val gridRow: Int?,   // grid "row:col" 파싱 결과
    val gridCol: Int?,
    val rating: Float?,
)
```

**WHY:** 라인업은 홈/어웨이 두 팀의 데이터를 항상 쌍으로 표시해야 합니다. `MatchLineups`가 이 쌍을 보장합니다.

**HOW:**
- `gridRow`와 `gridCol`은 API의 `grid` 문자열(예: `"2:3"`)을 파싱한 결과입니다. Row는 포메이션의 행(GK=1, DF=2, ...), Col은 열(왼쪽→오른쪽)을 나타냅니다.
- `rating`은 선수의 경기 중 레이팅(예: 7.2)입니다. API의 별도 `players` 배열에서 가져와 매핑합니다.
- `teamColorHex`는 유니폼 주 색상입니다. 포메이션 그리드에서 선수 원형 배경색으로 사용합니다.

> 💡 **Tip:** `gridRow`/`gridCol`이 `null`이면 해당 선수를 포메이션 그리드에 배치할 수 없습니다. 이 경우 교체 선수 목록에 표시하거나 기본 위치에 배치합니다.

---

### 6-4. MatchStatistics.kt

**파일:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/MatchStatistics.kt`

```kotlin
package com.chase1st.feetballfootball.core.model

data class MatchStatistics(
    val home: TeamMatchStats,
    val away: TeamMatchStats,
)

data class TeamMatchStats(
    val team: Team,
    val teamColorHex: String?,
    val stats: Map<String, String?>,  // "Shots on Goal" → "5"
)
```

**WHY:** 통계는 항상 홈/어웨이 비교로 표시됩니다. `Map<String, String?>`으로 유연하게 어떤 통계 항목이든 처리할 수 있습니다.

**HOW:** API에서 제공하는 주요 통계 항목은 다음과 같습니다:
- `Shots on Goal`, `Shots off Goal`, `Total Shots`
- `Blocked Shots`, `Shots insidebox`, `Shots outsidebox`
- `Fouls`, `Corner Kicks`, `Offsides`
- `Ball Possession` (퍼센트, 예: `"64%"`)
- `Yellow Cards`, `Red Cards`
- `Goalkeeper Saves`
- `Total passes`, `Passes accurate`, `Passes %`
- `expected_goals`

> ⚠️ **주의:** `stats` Map의 value는 `String?`입니다. UI에서 숫자형 값과 퍼센트형 값을 파싱할 때 null 체크가 필요합니다.

### ✅ 검증

- [ ] 4개 파일 모두 올바른 패키지에 생성되었다
- [ ] `Fixture`, `Team` 등 기존 모델 참조가 정상 resolve 된다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 7 — core-domain: UseCase + Repository 인터페이스 업데이트

### 목표

> 경기 상세 데이터를 조회하는 UseCase를 만들고, FixtureRepository 인터페이스에 상세 조회 메서드를 추가합니다.

### 작업 내용

---

### 7-1. FixtureRepository 인터페이스에 메서드 추가

**파일:** `core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/repository/FixtureRepository.kt`

기존 파일에 아래 메서드를 추가합니다.

```kotlin
// core-domain/repository/FixtureRepository.kt에 추가
fun getFixtureDetail(fixtureId: Int): Flow<MatchDetail>
```

**WHY:** Repository 인터페이스는 core-domain 모듈에 있어 core-data와의 의존성을 역전시킵니다. core-domain은 구현 세부사항을 모르고, core-data가 이 인터페이스를 구현합니다.

**HOW:** `Flow<MatchDetail>`을 반환하여 비동기 데이터 스트림을 제공합니다. ViewModel에서 `collect`하여 UI 상태를 업데이트합니다.

> 💡 **Tip:** `MatchDetail`을 import해야 합니다: `import com.chase1st.feetballfootball.core.model.MatchDetail`

---

### 7-2. GetFixtureDetailUseCase 생성

**파일:** `core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/usecase/GetFixtureDetailUseCase.kt`

이 파일을 새로 생성합니다.

```kotlin
package com.chase1st.feetballfootball.core.domain.usecase

import com.chase1st.feetballfootball.core.domain.repository.FixtureRepository
import com.chase1st.feetballfootball.core.model.MatchDetail
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFixtureDetailUseCase @Inject constructor(
    private val fixtureRepository: FixtureRepository,
) {
    operator fun invoke(fixtureId: Int): Flow<MatchDetail> =
        fixtureRepository.getFixtureDetail(fixtureId)
}
```

**WHY:** UseCase는 ViewModel과 Repository 사이의 중간 레이어로, 비즈니스 로직을 캡슐화합니다. 현재는 단순 위임이지만, 캐싱 전략이나 데이터 변환 등의 로직이 추가될 수 있습니다.

**HOW:** `@Inject constructor`로 Hilt가 자동으로 `FixtureRepository`를 주입합니다. `operator fun invoke`로 `useCase(fixtureId)` 형태의 함수 호출 구문을 지원합니다.

### ✅ 검증

- [ ] `FixtureRepository` 인터페이스에 `getFixtureDetail` 메서드가 추가되었다
- [ ] `GetFixtureDetailUseCase`가 올바른 패키지에 생성되었다
- [ ] `MatchDetail` import가 정상 resolve 된다
- [ ] 빌드 오류 없이 컴파일된다 (core-data의 구현은 다음 Step에서)

---

## Step 8 — core-data: FixtureDetailMapper (가장 복잡)

### 목표

> API 응답 DTO를 Domain Model로 변환하는 Mapper를 구현합니다. 이벤트 매핑, 라인업 + 레이팅 결합, 통계 매핑, 골 스코어러 추출 등 가장 복잡한 매핑 로직이 여기에 있습니다.

### 작업 내용

**파일:** `core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/mapper/FixtureDetailMapper.kt`

이 파일을 새로 생성합니다.

```kotlin
package com.chase1st.feetballfootball.core.data.mapper

import com.chase1st.feetballfootball.core.model.*
import com.chase1st.feetballfootball.core.network.model.*
import javax.inject.Inject

class FixtureDetailMapper @Inject constructor(
    private val fixtureMapper: FixtureMapper,
) {

    fun map(dto: FixtureDetailResponseDto): MatchDetail {
        val fixture = fixtureMapper.mapFixture(
            FixtureResponseDto(
                fixture = dto.fixture,
                league = dto.league,
                teams = dto.teams,
                goals = dto.goals,
            )
        )

        val events = dto.events.map { mapEvent(it) }
        val lineups = mapLineups(dto.lineups, dto.players)
        val statistics = mapStatistics(dto.statistics)
        val scorers = extractScorers(dto.events, dto.teams)

        return MatchDetail(
            fixture = fixture,
            events = events,
            lineups = lineups,
            statistics = statistics,
            scorers = scorers,
        )
    }

    private fun mapEvent(dto: EventDto): MatchEvent = MatchEvent(
        minute = dto.time.elapsed,
        extraMinute = dto.time.extra,
        team = Team(dto.team.id, dto.team.name, dto.team.logo),
        playerName = dto.player.name ?: "",
        assistName = dto.assist?.name,
        type = EventType.from(dto.type, dto.detail),
        detail = dto.detail,
    )

    private fun mapLineups(
        lineups: List<LineupDto>,
        playersData: List<TeamPlayersDto>,
    ): MatchLineups? {
        if (lineups.size < 2) return null
        val ratingMap = buildRatingMap(playersData)
        return MatchLineups(
            home = mapTeamLineup(lineups[0], ratingMap),
            away = mapTeamLineup(lineups[1], ratingMap),
        )
    }

    private fun mapTeamLineup(dto: LineupDto, ratingMap: Map<Int, Float>): TeamLineup =
        TeamLineup(
            team = Team(dto.team.id, dto.team.name, dto.team.logo),
            formation = dto.formation ?: "",
            coachName = dto.coach?.name ?: "",
            coachPhotoUrl = dto.coach?.photo,
            teamColorHex = dto.team.colors?.player?.primary,
            startingXI = dto.startXI.map { mapLineupPlayer(it.player, ratingMap) },
            substitutes = dto.substitutes.map { mapLineupPlayer(it.player, ratingMap) },
        )

    // 참조: FixtureDetailLineupFragment.kt의 grid 파싱 로직
    private fun mapLineupPlayer(dto: LineupPlayerDto, ratingMap: Map<Int, Float>): LineupPlayer {
        val (row, col) = parseGrid(dto.grid)
        return LineupPlayer(
            id = dto.id,
            name = dto.name,
            number = dto.number,
            position = dto.pos,
            gridRow = row,
            gridCol = col,
            rating = ratingMap[dto.id],
        )
    }

    private fun parseGrid(grid: String?): Pair<Int?, Int?> {
        if (grid == null) return null to null
        val parts = grid.split(":")
        return if (parts.size == 2) {
            parts[0].toIntOrNull() to parts[1].toIntOrNull()
        } else {
            null to null
        }
    }

    private fun buildRatingMap(playersData: List<TeamPlayersDto>): Map<Int, Float> =
        playersData.flatMap { it.players }
            .mapNotNull { playerRating ->
                val rating = playerRating.statistics.firstOrNull()
                    ?.games?.rating?.toFloatOrNull()
                if (rating != null) playerRating.player.id to rating else null
            }
            .toMap()

    private fun mapStatistics(dtos: List<TeamStatisticsDto>): MatchStatistics? {
        if (dtos.size < 2) return null
        return MatchStatistics(
            home = mapTeamStats(dtos[0]),
            away = mapTeamStats(dtos[1]),
        )
    }

    private fun mapTeamStats(dto: TeamStatisticsDto): TeamMatchStats = TeamMatchStats(
        team = Team(dto.team.id, dto.team.name, dto.team.logo),
        teamColorHex = null, // 통계 DTO에는 색상 정보가 없음, lineups에서 가져와야 함
        stats = dto.statistics.associate { it.type to it.value },
    )

    // 참조: FixtureDetailFragment.kt의 골 스코어러 파싱
    private fun extractScorers(
        events: List<EventDto>,
        teams: FixtureTeamsDto,
    ): Map<Int, List<GoalScorer>> {
        return events
            .filter { it.type == "Goal" }
            .map { event ->
                GoalScorer(
                    playerName = event.player.name ?: "",
                    minute = event.time.elapsed,
                    extraMinute = event.time.extra,
                    isPenalty = event.detail == "Penalty",
                    isOwnGoal = event.detail == "Own Goal",
                )
            }
            .groupBy { scorer ->
                // 자책골은 상대팀에 기록
                val scoringEvent = events.first {
                    it.type == "Goal" && it.player.name == scorer.playerName
                        && it.time.elapsed == scorer.minute
                }
                if (scorer.isOwnGoal) {
                    if (scoringEvent.team.id == teams.home.id) teams.away.id else teams.home.id
                } else {
                    scoringEvent.team.id
                }
            }
    }
}
```

아래에서 각 매핑 로직을 자세히 설명합니다.

---

### 8-1. `map()` — 최상위 매핑

```kotlin
fun map(dto: FixtureDetailResponseDto): MatchDetail {
    val fixture = fixtureMapper.mapFixture(
        FixtureResponseDto(
            fixture = dto.fixture,
            league = dto.league,
            teams = dto.teams,
            goals = dto.goals,
        )
    )
    ...
}
```

**WHY:** 기존 `FixtureMapper.mapFixture()`를 재사용하여 기본 경기 정보(`Fixture`)를 매핑합니다. `FixtureDetailResponseDto`에서 기본 필드만 추출하여 기존 `FixtureResponseDto`를 생성합니다.

> 💡 **Tip:** Mapper 간 의존성(`FixtureDetailMapper` → `FixtureMapper`)은 `@Inject constructor`로 Hilt가 자동 주입합니다.

---

### 8-2. `mapLineups()` + `buildRatingMap()` — 라인업 + 레이팅 결합

```kotlin
private fun mapLineups(
    lineups: List<LineupDto>,
    playersData: List<TeamPlayersDto>,
): MatchLineups? {
    if (lineups.size < 2) return null
    val ratingMap = buildRatingMap(playersData)
    return MatchLineups(
        home = mapTeamLineup(lineups[0], ratingMap),
        away = mapTeamLineup(lineups[1], ratingMap),
    )
}
```

**WHY:** API 응답에서 라인업(`lineups`)과 선수 레이팅(`players`)은 **별도 배열**로 제공됩니다. 선수 ID를 키로 레이팅 맵을 만들어 라인업 매핑 시 레이팅을 결합합니다.

**HOW:** `buildRatingMap`은 `players` 배열의 모든 선수를 순회하며 `playerId → rating` 맵을 구축합니다. `mapLineupPlayer`에서 이 맵으로 각 선수의 레이팅을 조회합니다.

---

### 8-3. `parseGrid()` — 포메이션 위치 파싱

```kotlin
private fun parseGrid(grid: String?): Pair<Int?, Int?> {
    if (grid == null) return null to null
    val parts = grid.split(":")
    return if (parts.size == 2) {
        parts[0].toIntOrNull() to parts[1].toIntOrNull()
    } else {
        null to null
    }
}
```

**WHY:** API의 `grid` 필드는 `"1:1"`, `"2:3"` 형태의 문자열입니다. 첫 번째 숫자는 행(row), 두 번째는 열(column)입니다. 포메이션 그리드 UI에서 선수를 올바른 위치에 배치하려면 숫자로 파싱해야 합니다.

---

### 8-4. `extractScorers()` — 자책골 팀 처리

```kotlin
if (scorer.isOwnGoal) {
    if (scoringEvent.team.id == teams.home.id) teams.away.id else teams.home.id
} else {
    scoringEvent.team.id
}
```

**WHY:** 자책골은 골을 넣은 선수의 팀이 아니라 **상대팀**의 득점으로 기록됩니다. 예를 들어 홈팀 수비수가 자책골을 넣으면 어웨이팀의 득점입니다. 이 로직이 없으면 스코어보드의 골 스코어러가 잘못된 팀에 표시됩니다.

> ⚠️ **주의:** `extractScorers`에서 `events.first { ... }` 호출은 이론상 항상 매칭되는 이벤트가 있어야 합니다. 하지만 데이터 불일치 가능성에 대비하여 `firstOrNull`로 변경하고 예외 처리를 추가하는 것도 고려해 보세요.

### ✅ 검증

- [ ] `FixtureMapper` 참조가 정상 resolve 된다
- [ ] `FixtureResponseDto` 생성자 호출이 기존 DTO 구조와 일치한다
- [ ] `EventType.from()` 호출이 정상 동작한다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 9 — feature-fixture-detail: UiState 정의

### 목표

> 경기 상세 화면의 UI 상태를 sealed interface로 정의합니다.

### 작업 내용

**파일:** `feature/feature-fixture-detail/src/main/kotlin/com/chase1st/feetballfootball/feature/fixturedetail/FixtureDetailUiState.kt`

이 파일을 새로 생성합니다.

```kotlin
package com.chase1st.feetballfootball.feature.fixturedetail

import com.chase1st.feetballfootball.core.model.MatchDetail

sealed interface FixtureDetailUiState {
    data object Loading : FixtureDetailUiState
    data class Success(val detail: MatchDetail) : FixtureDetailUiState
    data class Error(val message: String) : FixtureDetailUiState
}
```

**WHY:** `sealed interface`는 UI 상태를 유한한 타입 집합으로 제한합니다. `when` 분기에서 `else`를 쓸 필요가 없으며, 새 상태가 추가되면 컴파일러가 누락된 처리를 경고합니다.

**HOW:** 3가지 상태:
- `Loading`: API 호출 중. 로딩 인디케이터 표시.
- `Success`: 데이터 로딩 완료. `MatchDetail`을 포함.
- `Error`: 오류 발생. 에러 메시지와 재시도 버튼 표시.

### ✅ 검증

- [ ] `MatchDetail` import가 정상 resolve 된다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 10 — feature-fixture-detail: ViewModel

### 목표

> SavedStateHandle에서 fixtureId를 꺼내고, UseCase를 호출하여 데이터를 StateFlow로 제공하는 ViewModel을 구현합니다.

### 작업 내용

**파일:** `feature/feature-fixture-detail/src/main/kotlin/com/chase1st/feetballfootball/feature/fixturedetail/FixtureDetailViewModel.kt`

이 파일을 새로 생성합니다.

```kotlin
package com.chase1st.feetballfootball.feature.fixturedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asFlow
import com.chase1st.feetballfootball.core.domain.usecase.GetFixtureDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class FixtureDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFixtureDetailUseCase: GetFixtureDetailUseCase,
) : ViewModel() {

    private val fixtureId: Int = savedStateHandle["fixtureId"] ?: 0

    val uiState: StateFlow<FixtureDetailUiState> = flow {
        emit(FixtureDetailUiState.Loading)
        try {
            getFixtureDetailUseCase(fixtureId).collect { detail ->
                emit(FixtureDetailUiState.Success(detail))
            }
        } catch (e: Exception) {
            emit(FixtureDetailUiState.Error(e.message ?: "오류가 발생했습니다"))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FixtureDetailUiState.Loading)
}
```

**WHY:** `@HiltViewModel`은 Hilt가 자동으로 의존성을 주입하는 ViewModel입니다. `SavedStateHandle`은 Navigation argument를 안전하게 전달받는 Hilt의 기본 메커니즘입니다.

**HOW:**
- `savedStateHandle["fixtureId"]`: Navigation에서 전달된 fixtureId를 꺼냅니다. `FixtureDetailRoute(fixtureId: Int)` Route 클래스의 프로퍼티명과 일치해야 합니다.
- `SharingStarted.WhileSubscribed(5_000)`: 마지막 구독자가 사라진 후 5초간 데이터를 유지합니다. 화면 회전 시 데이터를 다시 로딩하지 않습니다.
- `flow { ... }.stateIn(...)`: cold flow를 hot StateFlow로 변환합니다. UI가 구독할 때만 데이터 로딩이 시작됩니다.

> 💡 **Tip:** `fixtureId`가 `0`이면 잘못된 Navigation입니다. 실제 앱에서는 예외를 던지거나 에러 화면을 표시해야 합니다. 추후 Navigation 설정 시 `require(fixtureId > 0)`을 추가하는 것을 고려하세요.

> 💡 **Tip:** SofaScore의 GET 엔드포인트는 인증 없이 기본 데이터를 조회할 수 있으므로, 별도의 인증 대기 로직이 필요하지 않습니다. ViewModel은 곧바로 UseCase를 호출합니다.

### ✅ 검증

- [ ] `@HiltViewModel` 어노테이션이 있다
- [ ] `SavedStateHandle`에서 `"fixtureId"` 키로 값을 가져온다
- [ ] `StateFlow`의 초기값이 `Loading`이다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 11 (Phase 5-A) — EventsTab 컴포넌트

### 목표

> 경기 이벤트(골, 카드, 교체, VAR 등)를 타임라인 형태로 표시하는 탭 컴포넌트를 구현합니다. 홈팀 이벤트는 왼쪽, 어웨이팀 이벤트는 오른쪽에 배치합니다.

### 작업 내용

**파일:** `feature/feature-fixture-detail/src/main/kotlin/com/chase1st/feetballfootball/feature/fixturedetail/component/EventsTab.kt`

이 파일을 새로 생성합니다. 기존 `FixtureDetailEventsFragment.kt`(168줄)의 로직을 Compose로 재구현합니다.

```kotlin
// feature/feature-fixture-detail/src/main/kotlin/.../feature/fixturedetail/component/EventsTab.kt
// 참조: FixtureDetailEventsFragment.kt (168줄)
// - 이벤트 타입 8종 아이콘 분기
// - 시간 표시 (elapsed + extra)
// - 홈/어웨이 배치 (팀 ID로 좌/우 결정)
// 상세 구현은 LazyColumn + MatchEvent 데이터 기반
```

**핵심 구현 로직:**

1. **`EventType`별 아이콘 표시** (Icon 또는 이미지 리소스)
   - `GOAL` → 축구공 아이콘
   - `OWN_GOAL` → 빨간 축구공
   - `PENALTY_GOAL` → 축구공 + "(P)"
   - `MISSED_PENALTY` → 취소선 축구공
   - `YELLOW_CARD` → 노란 사각형
   - `RED_CARD` → 빨간 사각형
   - `SECOND_YELLOW` → 노란+빨간 겹침
   - `SUBSTITUTION` → 화살표 아이콘
   - `VAR` → "VAR" 텍스트 배지

2. **홈/어웨이 정렬**
   - 홈팀 이벤트: 왼쪽 정렬 (Row의 `Arrangement.Start`)
   - 어웨이팀 이벤트: 오른쪽 정렬 (Row의 `Arrangement.End`)
   - 시간은 중앙 고정

3. **골 표시**: 선수명 + 어시스트명 (있는 경우)
4. **교체 표시**: 교체 IN 선수 (초록색) + OUT 선수 (빨간색)
5. **시간 표시**: `elapsed'` 또는 `elapsed+extra'` (예: `45'` 또는 `90+3'`)

**구현 방법 (LazyColumn 기반):**

```kotlin
@Composable
fun EventsTab(
    events: List<MatchEvent>,
    homeTeamId: Int,
) {
    LazyColumn {
        items(events) { event ->
            val isHome = event.team.id == homeTeamId
            EventRow(event = event, isHome = isHome)
        }
    }
}

@Composable
private fun EventRow(event: MatchEvent, isHome: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (isHome) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isHome) {
            // 홈: 선수명 - 아이콘 - 시간
            EventContent(event)
            Spacer(modifier = Modifier.weight(1f))
            TimeText(event)
        } else {
            // 어웨이: 시간 - 아이콘 - 선수명
            TimeText(event)
            Spacer(modifier = Modifier.weight(1f))
            EventContent(event)
        }
    }
}
```

> ⚠️ **주의:** `EventType`별 아이콘 리소스가 프로젝트에 준비되어 있어야 합니다. Material Icons를 활용하거나, `drawable` 리소스를 추가하세요. 기존 앱의 이미지 리소스를 재사용할 수 있습니다.

> 💡 **Tip:** Phase 5-A에서 EventsTab, StatisticsTab, LineupTab은 **독립적으로 개발 가능**합니다. 각 탭은 MatchDetail의 서로 다른 필드만 사용하므로 병렬 작업에 적합합니다.

### ✅ 검증

- [ ] `EventType` 8종 모두에 대해 아이콘이 표시된다
- [ ] 홈팀 이벤트가 왼쪽, 어웨이팀 이벤트가 오른쪽에 배치된다
- [ ] 골 이벤트에 어시스트 정보가 표시된다
- [ ] 교체 이벤트에 IN/OUT 선수가 색상으로 구분된다
- [ ] 시간이 `45'` 또는 `90+3'` 형태로 표시된다
- [ ] Preview에서 정상 렌더링된다

---

## Step 12 (Phase 5-A) — StatisticsTab 컴포넌트

### 목표

> 홈/어웨이 팀의 경기 통계를 비교 막대 그래프(LinearProgressIndicator)로 표시하는 탭 컴포넌트를 구현합니다.

### 작업 내용

**파일:** `feature/feature-fixture-detail/src/main/kotlin/com/chase1st/feetballfootball/feature/fixturedetail/component/StatisticsTab.kt`

이 파일을 새로 생성합니다. 기존 `FixtureDetailStatisticsFragment.kt`(256줄)의 로직을 Compose로 재구현합니다.

```kotlin
// feature/feature-fixture-detail/src/main/kotlin/.../feature/fixturedetail/component/StatisticsTab.kt
// 참조: FixtureDetailStatisticsFragment.kt (256줄)
// 핵심: 15개 통계 항목을 LinearProgressIndicator로 비교 표시
```

**핵심 구현 로직:**

1. **각 통계 항목 레이아웃**: `홈 값 | ProgressBar | 어웨이 값`
2. **`Ball Possession` 퍼센트 파싱**: 예: `"64%"` → `64` (정수로 변환)
3. **`Shots on Goal` / `Total Shots` 슈팅 정확도 계산**
4. **팀 컬러를 ProgressBar 색상에 적용**: 참조 — 흰색(`#FFFFFF` 또는 유사)이면 검정으로 대체
5. **`LinearProgressIndicator`의 `progress`**: `home / (home + away)` 비율로 설정

**구현 방법 (LazyColumn 기반):**

```kotlin
@Composable
fun StatisticsTab(
    statistics: MatchStatistics,
) {
    // 통계 항목 순서 정의
    val statKeys = listOf(
        "Ball Possession",
        "Shots on Goal",
        "Shots off Goal",
        "Total Shots",
        "Blocked Shots",
        "Shots insidebox",
        "Shots outsidebox",
        "Fouls",
        "Corner Kicks",
        "Offsides",
        "Yellow Cards",
        "Red Cards",
        "Goalkeeper Saves",
        "Total passes",
        "Passes accurate",
        "Passes %",
        "expected_goals",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(statKeys) { key ->
            val homeValue = statistics.home.stats[key]
            val awayValue = statistics.away.stats[key]
            // 양쪽 모두 값이 있을 때만 표시
            if (homeValue != null || awayValue != null) {
                StatRow(
                    label = key,
                    homeValue = homeValue ?: "0",
                    awayValue = awayValue ?: "0",
                    homeColor = statistics.home.teamColorHex,
                    awayColor = statistics.away.teamColorHex,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    homeValue: String,
    awayValue: String,
    homeColor: String?,
    awayColor: String?,
) {
    val homeNum = parseStatValue(homeValue)
    val awayNum = parseStatValue(awayValue)
    val total = homeNum + awayNum
    val progress = if (total > 0f) homeNum / total else 0.5f

    Column {
        // 항목 이름
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        // 값 + ProgressBar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = homeValue, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f).height(8.dp),
                // 팀 컬러 적용 (흰색이면 검정으로 대체)
            )
            Text(text = awayValue, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
        }
    }
}

// "64%" → 64f, "5" → 5f
private fun parseStatValue(value: String): Float {
    return value.replace("%", "").trim().toFloatOrNull() ?: 0f
}
```

> 💡 **Tip:** `LinearProgressIndicator`의 `progress`는 0f~1f 범위입니다. `homeNum / (homeNum + awayNum)`으로 계산하면 홈팀의 비율이 바의 채워진 부분이 되고, 나머지가 어웨이팀 부분이 됩니다. 양쪽 모두 0이면 50:50으로 표시합니다.

> ⚠️ **주의:** 팀 컬러 hex 값이 `"ffffff"` (흰색)이거나 매우 밝은 경우, 밝은 배경에서 ProgressBar가 보이지 않을 수 있습니다. 기존 앱(`FixtureDetailStatisticsFragment.kt`)에서 흰색을 검정으로 대체하는 로직이 있으므로 이를 포팅해야 합니다.

### ✅ 검증

- [ ] 모든 통계 항목이 `홈 값 | ProgressBar | 어웨이 값` 형태로 표시된다
- [ ] `Ball Possession`의 퍼센트 값이 올바르게 파싱된다
- [ ] ProgressBar의 비율이 홈/어웨이 값에 비례한다
- [ ] 양쪽 모두 0인 항목은 50:50으로 표시된다
- [ ] Preview에서 정상 렌더링된다

---

## Step 13 (Phase 5-A) — LineupTab 컴포넌트

### 목표

> 포메이션 그리드, 선수 카드, 감독 정보, 교체 선수 목록을 표시하는 가장 복잡한 탭 컴포넌트를 구현합니다.

### 작업 내용

**파일:** `feature/feature-fixture-detail/src/main/kotlin/com/chase1st/feetballfootball/feature/fixturedetail/component/LineupTab.kt`

이 파일을 새로 생성합니다. 기존 `FixtureDetailLineupFragment.kt`(170줄)의 로직을 Compose로 재구현합니다.

```kotlin
// feature/feature-fixture-detail/src/main/kotlin/.../feature/fixturedetail/component/LineupTab.kt
// 참조: FixtureDetailLineupFragment.kt (170줄)
// 가장 복잡한 탭: 포메이션 그리드 표현
```

**핵심 구현 로직:**

1. **`startingXI`를 `gridRow`로 그룹핑** → 각 행에 선수 배치
2. **어웨이팀은 행 순서 역전** (참조: reverse layout) — 홈팀 GK가 상단, 어웨이팀 GK가 하단
3. **`Column` of `Row`** 또는 **`Box` + `Modifier.offset`** 으로 그리드 위치 표현
4. **선수 카드**: 번호 + 이름 + 레이팅
   - 레이팅 >= 7.0 → 초록색
   - 레이팅 < 7.0 → 주황색
   - 레이팅 null → 표시 안함
5. **교체 선수 목록**: 별도 LazyRow 또는 Column
6. **감독 정보**: 사진(AsyncImage) + 이름
7. **포메이션 문자열 표시** (예: "4-3-3")
8. **배경색**: 필드 초록 (StadiumGreen / FormationGreen)

**구현 방법 (Column + Row 기반):**

```kotlin
@Composable
fun LineupTab(
    lineups: MatchLineups,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        // 홈팀 포메이션
        item {
            FormationSection(
                lineup = lineups.home,
                isHome = true,
            )
        }
        // 어웨이팀 포메이션
        item {
            FormationSection(
                lineup = lineups.away,
                isHome = false,
            )
        }
        // 홈팀 교체 선수
        item {
            SubstitutesSection(
                label = "${lineups.home.team.name} 교체 선수",
                substitutes = lineups.home.substitutes,
            )
        }
        // 어웨이팀 교체 선수
        item {
            SubstitutesSection(
                label = "${lineups.away.team.name} 교체 선수",
                substitutes = lineups.away.substitutes,
            )
        }
    }
}

@Composable
private fun FormationSection(
    lineup: TeamLineup,
    isHome: Boolean,
) {
    val rows = lineup.startingXI
        .groupBy { it.gridRow ?: 0 }
        .toSortedMap()
        .let { if (!isHome) it.toSortedMap(reverseOrder()) else it }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B5E20)) // FormationGreen 배경
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 포메이션 + 감독 정보
        Text(
            text = "${lineup.team.name} (${lineup.formation})",
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "감독: ${lineup.coachName}",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 포메이션 그리드
        rows.forEach { (_, playersInRow) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                playersInRow.sortedBy { it.gridCol }.forEach { player ->
                    PlayerCard(player = player, teamColorHex = lineup.teamColorHex)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PlayerCard(
    player: LineupPlayer,
    teamColorHex: String?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp),
    ) {
        // 번호 원형 배지 (팀 컬러 배경)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(parseTeamColor(teamColorHex)),
        ) {
            Text(
                text = "${player.number}",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        // 선수 이름
        Text(
            text = player.name,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // 레이팅 (있는 경우)
        player.rating?.let { rating ->
            Text(
                text = String.format("%.1f", rating),
                color = if (rating >= 7.0f) Color(0xFF4CAF50) else Color(0xFFFF9800),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// 팀 컬러 hex 파싱 (흰색이면 기본 색으로 대체)
private fun parseTeamColor(hex: String?): Color {
    if (hex == null) return Color(0xFF424242)
    return try {
        val colorInt = android.graphics.Color.parseColor("#$hex")
        Color(colorInt)
    } catch (e: Exception) {
        Color(0xFF424242)
    }
}
```

> 💡 **Tip:** 어웨이팀의 포메이션은 `toSortedMap(reverseOrder())`로 행 순서를 뒤집어 GK가 하단에 오도록 합니다. 이렇게 하면 실제 축구 경기 화면처럼 양 팀이 마주보는 형태가 됩니다.

> ⚠️ **주의:** `gridRow`나 `gridCol`이 `null`인 선수가 있을 수 있습니다. 이 경우 포메이션 그리드에서 제외하고, 별도로 "위치 미정" 섹션에 표시하거나 교체 선수 목록에 포함시키세요.

### ✅ 검증

- [ ] 홈팀 GK가 상단, 어웨이팀 GK가 하단에 위치한다
- [ ] 포메이션 행/열이 올바르게 배치된다
- [ ] 선수 카드에 번호, 이름, 레이팅이 표시된다
- [ ] 레이팅 >= 7.0이 초록색, < 7.0이 주황색이다
- [ ] 팀 컬러가 선수 번호 배경에 적용된다
- [ ] 교체 선수 목록이 포메이션 아래에 표시된다
- [ ] 감독 이름과 포메이션 문자열이 표시된다
- [ ] Preview에서 정상 렌더링된다

---

## Step 14 (Phase 5-B) — FixtureDetailScreen 조립

### 목표

> MatchHeader + TabRow + HorizontalPager를 조합하여 경기 상세 화면을 완성합니다. LargeTopAppBar의 접히는 동작과 동적 탭 구성을 포함합니다.

### 작업 내용

**파일:** `feature/feature-fixture-detail/src/main/kotlin/com/chase1st/feetballfootball/feature/fixturedetail/FixtureDetailScreen.kt`

이 파일을 새로 생성합니다. 기존 `FixtureDetailFragment.kt`(268줄)의 로직을 Compose로 재구현합니다.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixtureDetailScreen(
    fixtureId: Int,
    onBack: () -> Unit,
    viewModel: FixtureDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is FixtureDetailUiState.Loading -> FeetballLoadingIndicator()
        is FixtureDetailUiState.Error -> ErrorContent(state.message, onRetry = {})
        is FixtureDetailUiState.Success -> {
            val detail = state.detail
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

            // 사용 가능한 탭 동적 결정 (참조: FixtureDetailFragment.kt)
            val tabs = buildList {
                if (detail.events.isNotEmpty()) add("이벤트" to 0)
                if (detail.lineups != null) add("라인업" to 1)
                if (detail.statistics != null) add("통계" to 2)
            }

            Scaffold(
                topBar = {
                    LargeTopAppBar(
                        title = {
                            // 축소 시: "홈팀 vs 어웨이팀"
                            Text("${detail.fixture.homeTeam.name} vs ${detail.fixture.awayTeam.name}")
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) { /* 뒤로가기 아이콘 */ }
                        },
                        scrollBehavior = scrollBehavior,
                    )
                },
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            ) { padding ->
                Column(modifier = Modifier.padding(padding)) {
                    // 매치 헤더 (팀 로고, 스코어, 골 스코어러)
                    MatchHeader(detail = detail)

                    // 탭 + 페이저
                    if (tabs.isNotEmpty()) {
                        val pagerState = rememberPagerState(pageCount = { tabs.size })
                        TabRow(selectedTabIndex = pagerState.currentPage) {
                            tabs.forEachIndexed { index, (title, _) ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = { /* animateScrollToPage */ },
                                    text = { Text(title) },
                                )
                            }
                        }
                        HorizontalPager(state = pagerState) { page ->
                            when (tabs[page].second) {
                                0 -> EventsTab(events = detail.events, homeTeamId = detail.fixture.homeTeam.id)
                                1 -> LineupTab(lineups = detail.lineups!!)
                                2 -> StatisticsTab(statistics = detail.statistics!!)
                            }
                        }
                    }
                }
            }
        }
    }
}
```

**WHY:** `FixtureDetailScreen`은 Phase 5-A에서 만든 3개 탭을 하나의 화면으로 조립하는 역할입니다. 기존 `FixtureDetailFragment`의 ViewPager2 + TabLayout 패턴을 Compose의 `HorizontalPager` + `TabRow`로 대체합니다.

**HOW:**
- **동적 탭 구성**: `buildList`로 데이터가 있는 탭만 추가합니다. 경기 시작 전에는 이벤트/라인업/통계가 모두 비어있어 탭이 표시되지 않습니다.
- **`LargeTopAppBar` + `exitUntilCollapsedScrollBehavior`**: 스크롤 시 TopAppBar가 축소되어 콘텐츠 영역이 넓어집니다.
- **`nestedScroll(scrollBehavior.nestedScrollConnection)`**: HorizontalPager 내부의 LazyColumn 스크롤과 TopAppBar 축소가 연동됩니다.

---

### 14-1. MatchHeader 컴포넌트 (골 스코어러 포함)

```kotlin
// 참조: FixtureDetailFragment.kt의 골 스코어러 표시 로직
// - 홈팀 골 스코어러: 왼쪽
// - 어웨이팀 골 스코어러: 오른쪽
// - 시간 표시: "45'" 또는 "90+3'"
// - 페널티: "(P)" 표시
// - 자책골: "(OG)" 표시
```

**핵심 구조:**

```kotlin
@Composable
private fun MatchHeader(detail: MatchDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 홈팀: 로고 + 이름 + 골 스코어러
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            // 팀 로고 (AsyncImage 또는 Picasso)
            // 팀 이름
            // 골 스코어러 목록
            detail.scorers[detail.fixture.homeTeam.id]?.forEach { scorer ->
                ScorerText(scorer)
            }
        }

        // 스코어
        Text(
            text = "${detail.fixture.homeGoals ?: "-"} - ${detail.fixture.awayGoals ?: "-"}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )

        // 어웨이팀: 로고 + 이름 + 골 스코어러
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            // 팀 로고
            // 팀 이름
            // 골 스코어러 목록
            detail.scorers[detail.fixture.awayTeam.id]?.forEach { scorer ->
                ScorerText(scorer)
            }
        }
    }
}

@Composable
private fun ScorerText(scorer: GoalScorer) {
    val timeStr = buildString {
        append("${scorer.minute}'")
        scorer.extraMinute?.let { append("+${it}'") }
    }
    val suffix = when {
        scorer.isPenalty -> " (P)"
        scorer.isOwnGoal -> " (OG)"
        else -> ""
    }
    Text(
        text = "${scorer.playerName} $timeStr$suffix",
        style = MaterialTheme.typography.bodySmall,
    )
}
```

> 💡 **Tip:** `Tab`의 `onClick`에서 `pagerState.animateScrollToPage(index)`를 호출할 때는 `rememberCoroutineScope()`로 코루틴 스코프를 만들어 사용하세요.

> ⚠️ **주의:** `detail.lineups!!`와 `detail.statistics!!`는 탭이 추가되는 조건(`detail.lineups != null`)과 동일하므로 NPE가 발생하지 않습니다. 하지만 더 안전하게 `?.let { ... }`을 사용하는 것도 고려해 보세요.

### ✅ 검증

- [ ] Loading 상태에서 로딩 인디케이터가 표시된다
- [ ] Error 상태에서 에러 메시지가 표시된다
- [ ] 매치 헤더에 팀 로고, 이름, 스코어가 표시된다
- [ ] 골 스코어러가 홈(왼쪽)/어웨이(오른쪽)에 올바르게 표시된다
- [ ] 페널티에 "(P)", 자책골에 "(OG)"가 표시된다
- [ ] 데이터가 있는 탭만 동적으로 표시된다
- [ ] TabRow 탭 클릭 시 HorizontalPager가 해당 페이지로 이동한다
- [ ] TopAppBar가 스크롤에 따라 접히고 펼쳐진다
- [ ] 뒤로가기 버튼 클릭 시 이전 화면으로 돌아간다

---

## Step 15 — 통합 빌드 및 최종 검증

### 목표

> 전체 레이어(DTO → Mapper → Domain → UseCase → ViewModel → UI)가 올바르게 연결되어 경기 상세 화면이 정상 동작하는지 최종 확인합니다.

### 작업 내용

1. **빌드 확인:**
   ```bash
   ./gradlew :feature:feature-fixture-detail:assembleDebug
   ./gradlew assembleDebug
   ```

2. **앱 실행 후 경기 상세 테스트:**

| 테스트 항목 | 확인 사항 |
|------------|----------|
| SofaScore API 호출 | `event`, `incidents`, `lineups`, `statistics` 4개 엔드포인트 정상 응답 |
| 경기 선택 | 경기 목록에서 경기 클릭 → 상세 화면 진입 |
| 매치 헤더 | 팀 로고, 이름, 스코어, 골 스코어러 표시 |
| 이벤트 탭 | 골/카드/교체/VAR 타임라인 표시 |
| 라인업 탭 | 포메이션 그리드, 선수 카드, 감독, 교체 선수 |
| 통계 탭 | 15개 항목 비교 ProgressBar |
| 데이터 없음 | 연기/취소된 경기에서 탭이 자동 숨김 |
| TopAppBar | 스크롤 시 접히는 동작 |
| 뒤로가기 | 이전 화면(경기 목록)으로 복귀 |

### ✅ 검증

- [ ] SofaScore API: `event`, `incidents`, `lineups`, `statistics` 4개 엔드포인트 정상 응답
- [ ] 경기 선택 → 상세 화면 진입
- [ ] 매치 헤더: 팀 로고, 이름, 스코어, 골 스코어러 표시
- [ ] 이벤트 탭: 타임라인 표시 (골/카드/교체/VAR)
- [ ] 라인업 탭: 포메이션 표시, 선수 카드, 감독, 교체 선수
- [ ] 통계 탭: 15개 항목 비교 ProgressBar
- [ ] 데이터 없는 탭 자동 숨김 (연기/취소된 경기)
- [ ] 접히는 TopAppBar 동작
- [ ] 뒤로가기 동작

---

## Step 16 — Git 커밋

### 목표

> 경기 상세 화면 구현이 완료되면 변경사항을 커밋합니다.

### 작업 내용

```bash
git add core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/FixtureDetailDto.kt
git add core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/EventDto.kt
git add core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/LineupDto.kt
git add core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/StatisticsDto.kt
git add core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/FootballApiService.kt
git add core/core-model/
git add core/core-domain/
git add core/core-data/
git add feature/feature-fixture-detail/
git commit -m "feat: Slice 5 경기 상세 화면 구현 (SofaScore API 연동 + 이벤트/라인업/통계)"
```

### ✅ 검증

- [ ] 커밋에 필요한 파일만 포함되었다
- [ ] 빌드가 깨지지 않는다

---

## 🎉 Slice 5 완료!

축하합니다! 프로젝트에서 가장 복잡한 화면인 경기 상세를 완성했습니다.

**이 Slice에서 완성한 것:**

| 모듈 | 파일 | 설명 |
|------|------|------|
| `core-network` | `FixtureDetailDto.kt` | 상세 응답 DTO (SofaScore 엔드포인트별 분리) |
| `core-network` | `EventDto.kt` | 이벤트 DTO (시간, 팀, 선수, 유형) |
| `core-network` | `LineupDto.kt` | 라인업 DTO (포메이션, 선수, 감독, 팀컬러) |
| `core-network` | `StatisticsDto.kt` | 통계 + 선수 레이팅 DTO |
| `core-network` | `FootballApiService.kt` | `getEvent()`, `getEventIncidents()`, `getEventLineups()`, `getEventStatistics()` 엔드포인트 추가 (SofaScore) |
| `core-model` | `MatchDetail.kt` | 상세 화면 최상위 Domain Model |
| `core-model` | `MatchEvent.kt` | 이벤트 Domain Model + EventType enum |
| `core-model` | `MatchLineups.kt` | 라인업 Domain Model (포메이션 grid 파싱) |
| `core-model` | `MatchStatistics.kt` | 통계 Domain Model |
| `core-domain` | `FixtureRepository.kt` | `getFixtureDetail()` 인터페이스 추가 |
| `core-domain` | `GetFixtureDetailUseCase.kt` | 상세 조회 UseCase |
| `core-data` | `FixtureDetailMapper.kt` | 가장 복잡한 DTO→Domain 매퍼 |
| `feature-fixture-detail` | `FixtureDetailUiState.kt` | UI 상태 sealed interface |
| `feature-fixture-detail` | `FixtureDetailViewModel.kt` | SavedStateHandle + StateFlow |
| `feature-fixture-detail` | `EventsTab.kt` | 이벤트 타임라인 탭 |
| `feature-fixture-detail` | `StatisticsTab.kt` | 통계 비교 탭 |
| `feature-fixture-detail` | `LineupTab.kt` | 포메이션 + 선수 카드 탭 |
| `feature-fixture-detail` | `FixtureDetailScreen.kt` | 전체 조립 (Header + Tabs + Pager) |

**핵심 학습 포인트:**
- SofaScore의 분리된 엔드포인트(`event`, `incidents`, `lineups`, `statistics`)를 병렬 호출하여 하나의 Domain Model로 조합하는 패턴
- 복잡한 API 응답을 DTO → Domain Model로 매핑할 때 Mapper에 로직을 집중시킵니다
- `HorizontalPager` + `TabRow`는 ViewPager2 + TabLayout의 Compose 대체재입니다
- `LargeTopAppBar` + `nestedScroll`로 기존 CoordinatorLayout 동작을 재현합니다
- 포메이션 그리드는 `gridRow`로 그룹핑 → `Row`로 배치하는 패턴으로 구현합니다
- `SavedStateHandle`은 Hilt ViewModel에서 Navigation argument를 받는 표준 방법입니다


---

# Stage 2 / Navigation — Navigation 3 통합 + Bottom Navigation

> ⏱ 예상 소요 시간: 3시간 | 난이도: ★★☆ | 선행 조건: Slice 1~5 전체 완료 (모든 Compose Screen 구현 완료)

---

## 이 Codelab에서 배우는 것

- **Navigation 3**의 핵심 개념: `BackStack`을 개발자가 직접 소유하는 선언적 네비게이션
- `NavDisplay` + `entryProvider`를 사용한 화면 라우팅
- `NavigationBar` + `NavigationBarItem`을 활용한 Bottom Navigation 구현
- 상세 화면 진입 시 Bottom Bar 숨김 처리
- 탭 전환 시 BackStack 관리 전략
- 기존 Fragment/TabLayout 기반 네비게이션에서 Compose Navigation 3로의 전환

---

## 완성 후 결과물

- 3개 탭(경기/리그/뉴스) Bottom Navigation이 동작하는 단일 `FeetballApp` Composable
- 경기 목록 → 경기 상세, 리그 목록 → 리그 순위 화면 전환이 Navigation 3 BackStack으로 관리됨
- 상세 화면에서는 Bottom Bar가 자동으로 숨겨짐
- 시스템 뒤로가기 버튼이 정상 동작
- `MainActivity`가 `setContent { FeetballApp() }` 한 줄로 정리됨

### 새로 생성되는 파일

| 파일 | 위치 |
|------|------|
| `Routes.kt` | `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/Routes.kt` |
| `FeetballBottomBar.kt` | `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballBottomBar.kt` |
| `FeetballNavGraph.kt` | `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballNavGraph.kt` |

### 수정되는 파일

| 파일 | 위치 |
|------|------|
| `libs.versions.toml` | `gradle/libs.versions.toml` |
| `build.gradle.kts` | `app/build.gradle.kts` |
| `MainActivity.kt` | `app/src/main/kotlin/com/chase1st/feetballfootball/MainActivity.kt` |

---

## Step 1 — Navigation 3 의존성 확인

### 목표

> Navigation 3 라이브러리(1.1.0)가 프로젝트에 올바르게 선언되어 있는지 확인하고, 필요시 추가한다.

### 배경

Navigation 3는 2025년 11월 stable 1.0.0이 출시되었으며, 2026-04-08 **1.1.0이 stable**로 승격되면서 Scene 기반 Shared Elements 기능이 정식 지원됩니다. Navigation 2.x와 근본적으로 다른 점은 **BackStack을 개발자가 직접 소유**한다는 것입니다. `NavController`가 내부적으로 상태를 관리하던 Navigation 2.x와 달리, Navigation 3에서는 `SnapshotStateList`로 BackStack을 직접 조작합니다.

Navigation 3는 두 개의 아티팩트로 분리되어 있습니다:
- `navigation3-runtime` — 핵심 런타임 (BackStack, Route 등)
- `navigation3-ui` — UI 컴포넌트 (NavDisplay, entryProvider 등)

### 작업 내용

**파일: `gradle/libs.versions.toml`**

아래 내용이 이미 존재하는지 확인하고, 없으면 추가합니다:

```toml
[versions]
navigation3 = "1.1.0"            # 2026-04-08 stable 승격. Scene 기반 공유 요소(Shared Elements) 지원 추가

[libraries]
# Navigation 3는 runtime과 ui 두 개의 아티팩트로 분리
androidx-navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "navigation3" }
```

**파일: `app/build.gradle.kts`**

dependencies 블록에 아래를 추가합니다:

```kotlin
dependencies {
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
}
```

> 💡 **Tip:** Navigation 3는 1.1.0에서 stable로 승격되면서 KMP (JVM, Native, Web) 타겟을 포함한 Scene 기반 Shared Elements를 정식 지원합니다. 차기 1.2.0-alpha01도 이미 공개되어 있으나 본 단계에서는 stable 1.1.0을 사용합니다.

### ✅ 검증

- [ ] `./gradlew dependencies --configuration debugRuntimeClasspath | grep navigation3` 실행 시 `navigation3-runtime:1.1.0`과 `navigation3-ui:1.1.0`이 출력됨
- [ ] Sync 에러 없음

---

## Step 2 — Route 정의

### 목표

> 앱의 모든 화면에 대한 타입 안전한 Route를 `@Serializable` data class/object로 정의한다.

### 배경

Navigation 3에서 Route는 **일반 Kotlin 클래스**입니다. `@Serializable` 어노테이션을 붙이면 Navigation 3가 자동으로 직렬화/역직렬화를 처리합니다. 파라미터가 없는 화면은 `data object`, 파라미터가 있는 화면은 `data class`로 정의합니다.

현재 앱의 화면 구조:
- **탑레벨 탭 3개:** 경기 목록(Fixture), 리그 목록(League), 뉴스(News)
- **상세 화면 2개:** 경기 상세(FixtureDetail, fixtureId 필요), 리그 순위(LeagueStanding, leagueId/leagueName/season 필요)

### 작업 내용

**파일: `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/Routes.kt`** (새 파일 생성)

```kotlin
// app/src/main/kotlin/com/chase1st/feetballfootball/navigation/Routes.kt
package com.chase1st.feetballfootball.navigation

import kotlinx.serialization.Serializable

// ── 탑레벨 탭 (Bottom Navigation) ──
// 파라미터가 없으므로 data object 사용

@Serializable data object FixtureRoute      // 경기 일정 목록
@Serializable data object LeagueRoute       // 리그 선택 목록
@Serializable data object NewsRoute         // 뉴스 WebView

// ── 상세 화면 ──
// 파라미터가 있으므로 data class 사용

@Serializable data class FixtureDetailRoute(
    val fixtureId: Int,                     // SofaScore eventId
)

@Serializable data class LeagueStandingRoute(
    val leagueId: Int,                      // SofaScore uniqueTournamentId (예: EPL=17)
    val leagueName: String,                 // 화면 상단 표시용 리그 이름
    val seasonId: Int,                      // SofaScore seasonId (정수)
)
```

> 💡 **Tip:** `@Serializable`을 사용하려면 `kotlinx-serialization` 플러그인이 프로젝트에 적용되어 있어야 합니다. Slice 1(Stage 1)에서 이미 설정되었을 것이지만, `plugins { kotlin("plugin.serialization") }`이 `app/build.gradle.kts`에 있는지 확인하세요.

> ⚠️ **주의:** Route 클래스의 프로퍼티 타입은 직렬화 가능한 기본 타입(Int, String, Boolean 등)만 사용해야 합니다. 복잡한 객체를 Route에 넣지 마세요 — ID만 전달하고 상세 화면의 ViewModel에서 데이터를 로드하는 것이 올바른 패턴입니다.

### ✅ 검증

- [ ] 파일이 `com.chase1st.feetballfootball.navigation` 패키지에 생성됨
- [ ] 빌드 에러 없음 (`./gradlew compileDebugKotlin`)
- [ ] 5개 Route가 모두 `@Serializable`로 선언됨

---

## Step 3 — Bottom Navigation Bar

### 목표

> Material 3 `NavigationBar`를 사용하여 경기/리그/뉴스 3개 탭의 Bottom Navigation을 구현한다.

### 배경

기존 앱은 `TabLayout`으로 탭을 관리했습니다 (참조: `MainActivity.kt` 107줄). Compose에서는 Material 3의 `NavigationBar` + `NavigationBarItem`으로 대체합니다.

`TopLevelDestination` enum으로 탭 목록을 정의하면, 새 탭 추가 시 enum에만 항목을 추가하면 됩니다.

### 작업 내용

**파일: `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballBottomBar.kt`** (새 파일 생성)

```kotlin
// app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballBottomBar.kt
package com.chase1st.feetballfootball.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 탑레벨 탭 정의.
 * 각 탭은 Route, 아이콘, 라벨을 가진다.
 * 새 탭을 추가하려면 이 enum에 항목을 추가하면 된다.
 */
enum class TopLevelDestination(
    val route: Any,              // Navigation 3 Route 객체
    val icon: ImageVector,       // Material Icons
    val label: String,           // 하단 탭에 표시될 텍스트
) {
    FIXTURE(FixtureRoute, Icons.Default.SportsSoccer, "경기"),
    LEAGUE(LeagueRoute, Icons.Default.EmojiEvents, "리그"),
    NEWS(NewsRoute, Icons.Default.Newspaper, "뉴스"),
}

/**
 * 하단 네비게이션 바 Composable.
 *
 * @param currentRoute 현재 BackStack의 최상위 Route (선택 상태 판별용)
 * @param onTabSelected 탭 클릭 시 호출되는 콜백 (해당 탭의 Route 전달)
 */
@Composable
fun FeetballBottomBar(
    currentRoute: Any?,
    onTabSelected: (Any) -> Unit,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                // 현재 Route의 클래스와 탭의 Route 클래스를 비교하여 선택 상태 판별
                selected = currentRoute == destination.route ||
                    currentRoute?.javaClass == destination.route.javaClass,
                onClick = { onTabSelected(destination.route) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}
```

> 💡 **Tip:** `selected` 판별에서 `currentRoute == destination.route`는 data object 비교, `currentRoute?.javaClass == destination.route.javaClass`는 같은 타입의 data class 인스턴스 비교를 처리합니다. data object는 싱글턴이므로 `==`로 충분하지만, 만약 탭 Route가 data class로 변경될 경우를 대비한 방어 코드입니다.

> ⚠️ **주의:** `Icons.Default.SportsSoccer`, `Icons.Default.EmojiEvents`, `Icons.Default.Newspaper`를 사용하려면 `material-icons-extended` 의존성이 필요할 수 있습니다. 만약 아이콘이 resolve되지 않으면 `implementation("androidx.compose.material:material-icons-extended")`를 추가하거나, `Icons.Default.Home`, `Icons.Default.Star`, `Icons.Default.Info` 등 core 아이콘으로 대체하세요.

### ✅ 검증

- [ ] `FeetballBottomBar` Preview에서 3개 탭(경기/리그/뉴스)이 아이콘+라벨과 함께 표시됨
- [ ] `TopLevelDestination.entries`가 3개 항목을 반환함
- [ ] 빌드 에러 없음

---

## Step 4 — NavDisplay + BackStack 구성 (Navigation 3 핵심)

### 목표

> Navigation 3의 `rememberMutableBackStack` + `NavDisplay` + `entryProvider`를 사용하여 전체 앱의 화면 전환을 구성한다. `Scaffold`로 Bottom Bar를 배치하고, 상세 화면에서는 Bottom Bar를 숨긴다.

### 배경

이 Step이 Navigation 3 통합의 **핵심**입니다.

**Navigation 2.x vs Navigation 3 비교:**

| 항목 | Navigation 2.x | Navigation 3 |
|------|---------------|--------------|
| 상태 소유 | `NavController` 내부 관리 | 개발자가 `BackStack` 직접 소유 |
| 화면 등록 | `NavHost { composable<Route> {} }` | `NavDisplay { entryProvider { entry<Route> {} } }` |
| 화면 전환 | `navController.navigate(route)` | `backStack.add(route)` |
| 뒤로가기 | `navController.popBackStack()` | `backStack.removeLastOrNull()` |

Navigation 3에서 BackStack은 `SnapshotStateList`이므로, `add()`, `removeLastOrNull()`, `clear()` 등 일반 리스트 연산을 사용합니다.

기존 `MainActivity.kt`의 Fragment 관리 로직을 참조하면:
- `showFragment(tabPos)` → 탭 전환 시 `backStack.clear()` + `backStack.add(route)`
- `onLeagueSelected(leagueId)` → `backStack.add(LeagueStandingRoute(...))`
- `onFixtureSelected(fixtureId)` → `backStack.add(FixtureDetailRoute(fixtureId))`

### 작업 내용

**파일: `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballNavGraph.kt`** (새 파일 생성)

```kotlin
// app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballNavGraph.kt
package com.chase1st.feetballfootball.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chase1st.feetballfootball.feature.fixture.FixtureScreen
import com.chase1st.feetballfootball.feature.fixturedetail.FixtureDetailScreen
import com.chase1st.feetballfootball.feature.league.LeagueListScreen
import com.chase1st.feetballfootball.feature.league.standing.StandingScreen
import com.chase1st.feetballfootball.feature.news.NewsScreen

// Navigation 3 API 사용
// import androidx.navigation3.*

/**
 * 앱의 루트 Composable.
 * Navigation 3 BackStack을 소유하고, 모든 화면 전환을 관리한다.
 *
 * 구조:
 * - Scaffold: 전체 레이아웃 (bottomBar + content)
 * - NavDisplay: BackStack 기반 화면 전환
 * - entryProvider: Route → Composable 매핑
 */
@Composable
fun FeetballApp() {
    // ── Navigation 3: BackStack을 개발자가 직접 소유 ──
    // 초기 Route로 FixtureRoute(경기 목록)을 지정
    // backStack은 SnapshotStateList이므로 Compose가 변경을 자동 감지
    val backStack = rememberMutableBackStack(FixtureRoute)

    Scaffold(
        bottomBar = {
            // ── 상세 화면에서는 Bottom Bar 숨김 ──
            // BackStack의 최상위(마지막) 항목이 탑레벨 Route인지 확인
            val currentRoute = backStack.lastOrNull()
            val isTopLevel = currentRoute is FixtureRoute ||
                currentRoute is LeagueRoute ||
                currentRoute is NewsRoute

            if (isTopLevel) {
                FeetballBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        // 탭 전환 시 기존 스택 초기화 후 새 탭의 Route만 남김
                        // 이는 기존 MainActivity.showFragment()에서
                        // replace()로 Fragment를 교체하던 것과 동일한 동작
                        backStack.clear()
                        backStack.add(route)
                    },
                )
            }
        },
    ) { padding ->
        // ── NavDisplay: BackStack 기반 화면 렌더링 ──
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(padding),
            entryProvider = entryProvider {

                // ── 경기 목록 화면 ──
                entry<FixtureRoute> {
                    FixtureScreen(
                        onFixtureClick = { fixtureId ->
                            // 경기 상세로 이동 (기존 onFixtureSelected 콜백 대체)
                            backStack.add(FixtureDetailRoute(fixtureId))
                        },
                    )
                }

                // ── 경기 상세 화면 ──
                entry<FixtureDetailRoute> { route ->
                    FixtureDetailScreen(
                        fixtureId = route.fixtureId,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                // ── 리그 목록 화면 ──
                entry<LeagueRoute> {
                    LeagueListScreen(
                        onLeagueClick = { leagueId, leagueName, seasonId ->
                            // 리그 순위로 이동 (기존 onLeagueSelected 콜백 대체)
                            backStack.add(
                                LeagueStandingRoute(leagueId, leagueName, seasonId)
                            )
                        },
                    )
                }

                // ── 리그 순위 화면 ──
                entry<LeagueStandingRoute> { route ->
                    StandingScreen(
                        leagueName = route.leagueName,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                // ── 뉴스 화면 ──
                entry<NewsRoute> {
                    NewsScreen()
                }
            },
        )
    }
}
```

### 화면 전환 흐름 다이어그램

```
[FixtureRoute] ──onFixtureClick──▶ [FixtureDetailRoute(fixtureId)]
      ↑                                        │
      └──────────── onBack (removeLastOrNull) ──┘

[LeagueRoute] ──onLeagueClick──▶ [LeagueStandingRoute(leagueId, name, seasonId)]
      ↑                                        │
      └──────────── onBack (removeLastOrNull) ──┘

[NewsRoute] (단독 화면, 하위 화면 없음)

탭 전환: backStack.clear() → backStack.add(새 탭 Route)
```

> 💡 **Tip:** `backStack.clear()` + `backStack.add(route)` 패턴은 탭 전환 시 이전 탭의 히스토리를 모두 버립니다. 만약 탭별 히스토리를 유지하고 싶다면(예: 리그 탭에서 순위 화면에 있다가 경기 탭 갔다 돌아오면 순위 화면 유지), 탭별로 별도의 BackStack을 관리하는 패턴이 필요합니다. 현 단계에서는 단순 초기화 방식을 사용합니다.

> ⚠️ **주의:** `NavDisplay`의 `entryProvider` 블록 안에서 등록하지 않은 Route가 BackStack에 들어가면 런타임 에러가 발생합니다. 모든 Route에 대한 `entry<>` 등록을 빠뜨리지 마세요.

### ✅ 검증

- [ ] `FeetballApp()` 호출 시 기본 화면으로 경기 목록(FixtureRoute)이 표시됨
- [ ] Bottom Navigation 3개 탭이 하단에 표시됨
- [ ] 경기 탭 → 리그 탭 → 뉴스 탭 전환이 정상 동작
- [ ] 경기 상세/리그 순위 화면에서 Bottom Bar가 숨겨짐
- [ ] 뒤로가기 시 이전 화면으로 정상 복귀

---

## Step 5 — MainActivity 최종 업데이트

### 목표

> 기존 Fragment/TabLayout 기반의 `MainActivity`를 Compose `setContent`로 완전히 교체한다.

### 배경

기존 `MainActivity`(107줄)는 다음 역할을 했습니다:
- `ActivityMainBinding`으로 XML 레이아웃 inflate
- `AndroidThreeTen.init()` 초기화
- `TabLayout` 리스너로 Fragment 교체
- `LeaguesFragment.Callbacks`, `FixtureRecyclerViewAdapter.Callbacks` 인터페이스 구현
- `savedInstanceState`로 선택된 탭 위치 복원

이 모든 것이 Navigation 3의 `FeetballApp()`으로 대체되므로, `MainActivity`는 `setContent` 한 줄로 단순화됩니다.

### 작업 내용

**파일: `app/src/main/kotlin/com/chase1st/feetballfootball/MainActivity.kt`** (기존 파일 교체)

```kotlin
// app/src/main/kotlin/com/chase1st/feetballfootball/MainActivity.kt
package com.chase1st.feetballfootball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chase1st.feetballfootball.navigation.FeetballApp
import com.chase1st.feetballfootball.ui.theme.FeetballTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-Edge: 시스템 바 영역까지 콘텐츠 확장
        enableEdgeToEdge()
        setContent {
            // FeetballTheme: 라이트/다크 모드 + Dynamic Color 지원
            FeetballTheme {
                // 앱의 전체 네비게이션을 FeetballApp()이 관리
                FeetballApp()
            }
        }
    }
}
```

### 기존 코드와 비교

| 기존 (107줄) | 새 코드 (~15줄) |
|-------------|---------------|
| `AppCompatActivity` 상속 | `ComponentActivity` 상속 |
| `ActivityMainBinding.inflate()` | `setContent {}` |
| `AndroidThreeTen.init()` | 불필요 (minSdk 26, `java.time` 직접 사용) |
| `TabLayout.OnTabSelectedListener` | `FeetballBottomBar`의 `onTabSelected` |
| `Callbacks` 인터페이스 2개 구현 | Navigation 3 Route로 화면 전환 |
| `savedInstanceState` 수동 관리 | Compose 상태 자동 관리 |
| `supportFragmentManager.beginTransaction()` | `backStack.add(route)` |

> 💡 **Tip:** `@AndroidEntryPoint`는 Hilt 의존성 주입을 위한 어노테이션입니다. Hilt를 사용하지 않는 경우 제거해도 됩니다.

> ⚠️ **주의:** `enableEdgeToEdge()`를 호출하면 시스템 바(상태 바, 네비게이션 바) 아래까지 콘텐츠가 확장됩니다. `Scaffold`의 `padding`을 반드시 적용해야 콘텐츠가 시스템 바에 가려지지 않습니다. Step 4에서 `Modifier.padding(padding)`을 적용했으므로 정상 동작합니다.

### ✅ 검증

- [ ] `MainActivity`가 `ComponentActivity`를 상속함 (`AppCompatActivity` 아님)
- [ ] `setContentView()` 호출이 없음 (`setContent {}` 사용)
- [ ] `ActivityMainBinding` 참조가 없음
- [ ] Fragment/TabLayout 관련 import가 없음
- [ ] 앱 실행 시 `FeetballApp()` 화면이 정상 표시됨

---

## Step 6 — Navigation 2.9.x 레거시 참고 (선택 사항)

### 목표

> Navigation 2.9.x 기반 코드를 참고용으로 이해한다. 이 Step은 코드를 작성하지 않으며, 기존 Navigation 2.x 프로젝트를 유지보수할 때 참고용입니다.

### 배경

Navigation 3를 사용할 수 없는 환경(라이브러리 호환성 문제, 팀 의사결정 등)에서는 Navigation 2.9.x를 대안으로 사용할 수 있습니다. 아래 코드는 동일한 화면 구조를 Navigation 2.9.x로 구현한 예시입니다.

### 참고 코드 (Navigation 2.9.x 버전)

```kotlin
// Navigation 2.9.x 기반 FeetballApp — 참고용, 실제로 사용하지 않음
@Composable
fun FeetballApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination

            val isTopLevel = TopLevelDestination.entries.any {
                currentRoute?.hasRoute(it.route::class) == true
            }

            if (isTopLevel) {
                FeetballBottomBar(
                    currentRoute = currentRoute,
                    navController = navController,
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = FixtureRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<FixtureRoute> {
                FixtureScreen(
                    onFixtureClick = { id -> navController.navigate(FixtureDetailRoute(id)) },
                )
            }
            composable<FixtureDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<FixtureDetailRoute>()
                FixtureDetailScreen(
                    fixtureId = route.fixtureId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<LeagueRoute> {
                LeagueListScreen(
                    onLeagueClick = { id, name, seasonId ->
                        navController.navigate(LeagueStandingRoute(id, name, seasonId))
                    },
                )
            }
            composable<LeagueStandingRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<LeagueStandingRoute>()
                StandingScreen(
                    leagueName = route.leagueName,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<NewsRoute> { NewsScreen() }
        }
    }
}
```

### Navigation 2.9.x vs Navigation 3 핵심 차이

| 항목 | Navigation 2.9.x | Navigation 3 |
|------|-----------------|--------------|
| BackStack 소유 | `NavController` 내부 | 개발자 직접 (`SnapshotStateList`) |
| 화면 등록 | `NavHost { composable<Route> {} }` | `NavDisplay { entryProvider { entry<Route> {} } }` |
| 화면 전환 | `navController.navigate(route)` | `backStack.add(route)` |
| 뒤로가기 | `navController.popBackStack()` | `backStack.removeLastOrNull()` |
| 시작 화면 | `startDestination = Route` | `rememberMutableBackStack(Route)` |
| Route 접근 | `backStackEntry.toRoute<T>()` | `entry<T> { route -> ... }` 파라미터 |

> 💡 **Tip:** Navigation 3의 가장 큰 장점은 **테스트 용이성**입니다. BackStack이 일반 리스트이므로 단위 테스트에서 `mutableListOf()`로 BackStack을 생성하여 화면 전환 로직을 테스트할 수 있습니다.

### ✅ 검증

- [ ] 이 Step에서는 코드를 작성하지 않음 (참고용)
- [ ] Navigation 3 방식(Step 4)으로 구현되었는지 재확인

---

## Navigation 완료 검증

### 전체 체크리스트

모든 Step이 완료되면 아래 항목을 하나씩 확인합니다:

- [ ] **Bottom Navigation:** 경기/리그/뉴스 3개 탭 전환 정상
- [ ] **경기 상세 진입:** 경기 목록 → 경기 상세 진입 → 뒤로가기 → 목록 복귀
- [ ] **리그 순위 진입:** 리그 선택 → 리그 순위 진입 → 뒤로가기 → 선택 복귀
- [ ] **Bottom Bar 숨김:** 상세 화면(FixtureDetail, LeagueStanding)에서 Bottom Bar 숨김
- [ ] **탭 전환 시 스택 초기화:** 탭 전환 시 이전 탭의 BackStack이 초기화됨
- [ ] **시스템 뒤로가기:** 시스템 뒤로가기 버튼이 BackStack을 따라 정상 동작
- [ ] **의존성:** `navigation3-runtime` + `navigation3-ui` 1.1.0 사용 중
- [ ] **빌드 성공:** `./gradlew assembleDebug` 에러 없음

### 커밋

```bash
git add .
git commit -m "feat: Navigation 3 통합 + Bottom Navigation"
```

### 생성/수정된 파일 요약

| 작업 | 파일 |
|------|------|
| 새 파일 | `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/Routes.kt` |
| 새 파일 | `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballBottomBar.kt` |
| 새 파일 | `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballNavGraph.kt` |
| 수정 | `gradle/libs.versions.toml` (Navigation 3 의존성 추가) |
| 수정 | `app/build.gradle.kts` (Navigation 3 의존성 추가) |
| 교체 | `app/src/main/kotlin/com/chase1st/feetballfootball/MainActivity.kt` (107줄 → ~15줄) |

---

> 다음 단계: [Stage 2 / Cleanup — 레거시 코드 제거](stage-2-cleanup.md)


---

# Stage 2 / Cleanup — 레거시 코드 전면 제거

> ⏱ 예상 소요 시간: 2시간 | 난이도: ★☆☆ | 선행 조건: Stage 2 Navigation 완료, 모든 Compose 화면 정상 동작 확인

---

## 이 Codelab에서 배우는 것

- Fragment/XML 기반 레거시 코드를 안전하게 식별하고 제거하는 절차
- 삭제 후 빌드/Lint 검증을 통한 참조 누락 탐지
- Gradle 의존성 정리 (Groovy DSL → Kotlin DSL 전환 포함)
- Android 리소스(layout, drawable, values) 정리 기준과 판단 방법
- 대규모 코드 삭제 시 단계별 커밋 전략

---

## 완성 후 결과물

- `app` 모듈에 레거시 코드가 전혀 없는 상태 (~50 LOC: `MainActivity` + Application 클래스만 잔존)
- 모든 화면 로직은 `core-*/feature-*` 모듈에 분산
- 불필요한 라이브러리 의존성 제거 (GSON, Picasso, ThreeTenABP, RecyclerView 등)
- XML Layout 20개, Fragment 10개, Adapter 2개, ViewModel 3개, Behavior 3개, API 모델 전부 삭제

### 삭제 대상 총 요약

| 카테고리 | 파일 수 | 위치 |
|----------|---------|------|
| Fragment | 10개 | `app/src/main/java/com/example/feetballfootball/fragment/` |
| XML Layout | 20개 | `app/src/main/res/layout/` |
| Adapter | 2개 | `app/src/main/java/com/example/feetballfootball/adapter/` |
| Behavior | 3개 | `app/src/main/java/com/example/feetballfootball/behavior/` |
| ViewModel | 3개 | `app/src/main/java/com/example/feetballfootball/viewModel/` |
| API/Model | 40+개 | `app/src/main/java/com/example/feetballfootball/api/` |
| Utility | 2개 | `app/src/main/java/com/example/feetballfootball/util/` |
| Drawable | ~14개 | `app/src/main/res/drawable/` |
| Values | ~4개 | `app/src/main/res/values/`, `values-night/` |

---

## ⚠️ 제거 전 필수 확인

> 이 체크리스트를 **모두 통과한 후에만** 삭제 작업을 시작하세요. 하나라도 실패하면 Navigation 또는 Screen 구현을 먼저 수정해야 합니다.

- [ ] **모든 화면 정상 동작** — 경기 일정, 경기 상세 (이벤트/라인업/통계 3탭), 리그 선택, 리그 순위, 뉴스
- [ ] **Navigation 정상** — Bottom Navigation 3탭 전환, 상세 화면 진입/복귀, 시스템 Back 버튼
- [ ] **빌드 성공** — `./gradlew assembleDebug` 에러 없음
- [ ] **별도 브랜치에서 작업** — `feature/renewal-cleanup`에서 제거 후 검증

```bash
# 브랜치 생성 및 전환
git checkout -b feature/renewal-cleanup
```

> 💡 **Tip:** 삭제 작업은 되돌리기 어려우므로, 별도 브랜치에서 작업하고 모든 검증 후 merge하는 것이 안전합니다. 각 Step 완료 후 중간 커밋을 남기면 문제 발생 시 특정 Step으로 롤백할 수 있습니다.

---

## Step 1 — Fragment 파일 제거 (10개)

### 목표

> 기존 Fragment 기반 UI 코드를 모두 삭제한다. 이 파일들은 Compose Screen으로 100% 대체되었다.

### 배경

기존 앱은 Single Activity + Fragment 패턴이었습니다. `MainActivity`가 `supportFragmentManager`로 Fragment를 교체하며 화면을 전환했습니다. Navigation 3 통합이 완료되었으므로 모든 Fragment가 불필요합니다.

### 작업 내용

#### 1.1 Fixture 관련 Fragment (5개)

아래 5개 파일을 삭제합니다:

```
삭제: app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureFragment.kt
삭제: app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailFragment.kt
삭제: app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailEventsFragment.kt
삭제: app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailLineupFragment.kt
삭제: app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailStatisticsFragment.kt
```

| 삭제 파일 | Compose 대체 |
|-----------|-------------|
| `FixtureFragment.kt` (166줄) | `feature.fixture.FixtureScreen` |
| `FixtureDetailFragment.kt` (268줄) | `feature.fixturedetail.FixtureDetailScreen` |
| `FixtureDetailEventsFragment.kt` (168줄) | `feature.fixturedetail` 내부 Events 탭 |
| `FixtureDetailLineupFragment.kt` (170줄) | `feature.fixturedetail` 내부 Lineup 탭 |
| `FixtureDetailStatisticsFragment.kt` (256줄) | `feature.fixturedetail` 내부 Statistics 탭 |

```bash
rm app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureFragment.kt
rm app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailFragment.kt
rm app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailEventsFragment.kt
rm app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailLineupFragment.kt
rm app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailStatisticsFragment.kt
```

#### 1.2 League 관련 Fragment (4개)

아래 4개 파일을 삭제합니다:

```
삭제: app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeaguesFragment.kt
삭제: app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeagueStandingFragment.kt
삭제: app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeagueClubsStandingFragment.kt
삭제: app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeaguePlayerStandingFragment.kt
```

| 삭제 파일 | Compose 대체 |
|-----------|-------------|
| `LeaguesFragment.kt` (79줄) | `feature.league.LeagueListScreen` |
| `LeagueStandingFragment.kt` (136줄) — ViewPager2 호스트 | `feature.league.standing.StandingScreen` |
| `LeagueClubsStandingFragment.kt` (136줄) | `feature.league.standing` 내부 클럽 순위 탭 |
| `LeaguePlayerStandingFragment.kt` (169줄) | `feature.league.standing` 내부 선수 순위 탭 |

```bash
rm app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeaguesFragment.kt
rm app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeagueStandingFragment.kt
rm app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeagueClubsStandingFragment.kt
rm app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeaguePlayerStandingFragment.kt
```

#### 1.3 News Fragment (1개)

```
삭제: app/src/main/java/com/example/feetballfootball/fragment/news/NewsFragment.kt
```

| 삭제 파일 | Compose 대체 |
|-----------|-------------|
| `NewsFragment.kt` (88줄) | `feature.news.NewsScreen` |

```bash
rm app/src/main/java/com/example/feetballfootball/fragment/news/NewsFragment.kt
```

> ⚠️ **주의:** `LeaguesFragment.kt`에는 `Callbacks` 인터페이스가 정의되어 있고, 기존 `MainActivity`에서 구현하고 있었습니다. 새 `MainActivity`에서는 이 인터페이스를 사용하지 않으므로 삭제해도 안전합니다. 만약 새 `MainActivity.kt`에 `LeaguesFragment.Callbacks` import가 남아있다면 함께 제거하세요.

### ✅ 검증

- [ ] `app/src/main/java/com/example/feetballfootball/fragment/` 디렉토리 아래 `.kt` 파일이 0개
- [ ] `./gradlew assembleDebug` — Fragment import 참조 에러가 없는지 확인
- [ ] 중간 커밋: `git commit -m "chore: Fragment 10개 삭제"`

---

## Step 2 — XML Layout 파일 제거 (20개)

### 목표

> Fragment가 사용하던 XML 레이아웃과 RecyclerView 아이템 레이아웃을 모두 삭제한다.

### 배경

Compose에서는 UI를 Kotlin 코드로 선언하므로 XML Layout이 불필요합니다. `LazyColumn`이 RecyclerView를, `Column`/`Row`가 LinearLayout을, `Box`가 FrameLayout을 대체합니다.

### 작업 내용

#### 2.1 Fragment 레이아웃 (10개)

```
삭제: app/src/main/res/layout/fragment_fixture.xml
삭제: app/src/main/res/layout/fragment_fixture_detail.xml
삭제: app/src/main/res/layout/fragment_fixture_detail_events.xml
삭제: app/src/main/res/layout/fragment_fixture_detail_lineup.xml
삭제: app/src/main/res/layout/fragment_fixture_detail_statistics.xml
삭제: app/src/main/res/layout/fragment_leagues.xml
삭제: app/src/main/res/layout/fragment_league_standing.xml
삭제: app/src/main/res/layout/fragment_league_clubs_standing.xml
삭제: app/src/main/res/layout/fragment_league_player_standing.xml
삭제: app/src/main/res/layout/fragment_news.xml
```

```bash
rm app/src/main/res/layout/fragment_fixture.xml
rm app/src/main/res/layout/fragment_fixture_detail.xml
rm app/src/main/res/layout/fragment_fixture_detail_events.xml
rm app/src/main/res/layout/fragment_fixture_detail_lineup.xml
rm app/src/main/res/layout/fragment_fixture_detail_statistics.xml
rm app/src/main/res/layout/fragment_leagues.xml
rm app/src/main/res/layout/fragment_league_standing.xml
rm app/src/main/res/layout/fragment_league_clubs_standing.xml
rm app/src/main/res/layout/fragment_league_player_standing.xml
rm app/src/main/res/layout/fragment_news.xml
```

#### 2.2 RecyclerView 아이템 레이아웃 (9개)

```
삭제: app/src/main/res/layout/fixture.xml                              ← 경기 아이템
삭제: app/src/main/res/layout/league_fixture.xml                       ← 리그 헤더 + 경기 그룹
삭제: app/src/main/res/layout/events_recycler_item.xml                 ← 이벤트 아이템
삭제: app/src/main/res/layout/lineup_player_recycler_item.xml          ← 라인업 선수
삭제: app/src/main/res/layout/lineup_row_recycler_item.xml             ← 라인업 행
삭제: app/src/main/res/layout/lineup_substitute_player_recycler_item.xml ← 교체 선수
삭제: app/src/main/res/layout/standing_item.xml                        ← 순위 아이템
삭제: app/src/main/res/layout/scorer_recycler_item.xml                 ← 득점 아이템
삭제: app/src/main/res/layout/assist_recycler_item.xml                 ← 어시스트 아이템
```

```bash
rm app/src/main/res/layout/fixture.xml
rm app/src/main/res/layout/league_fixture.xml
rm app/src/main/res/layout/events_recycler_item.xml
rm app/src/main/res/layout/lineup_player_recycler_item.xml
rm app/src/main/res/layout/lineup_row_recycler_item.xml
rm app/src/main/res/layout/lineup_substitute_player_recycler_item.xml
rm app/src/main/res/layout/standing_item.xml
rm app/src/main/res/layout/scorer_recycler_item.xml
rm app/src/main/res/layout/assist_recycler_item.xml
```

#### 2.3 Activity 레이아웃 (1개)

새 `MainActivity`가 `setContent { FeetballApp() }`만 호출하고 `setContentView()`를 호출하지 않으므로, `activity_main.xml`은 더 이상 사용되지 않습니다.

```
삭제: app/src/main/res/layout/activity_main.xml
```

```bash
rm app/src/main/res/layout/activity_main.xml
```

> ⚠️ **주의:** `activity_main.xml` 삭제 후, 기존 `MainActivity.kt` (레거시)에서 `R.layout.activity_main`을 참조하는 코드가 남아있지 않은지 확인하세요. 새 `MainActivity.kt`에서는 `setContent {}`를 사용하므로 참조가 없어야 합니다.

> 💡 **Tip:** 삭제 후 `app/src/main/res/layout/` 디렉토리가 비어있다면 디렉토리 자체도 삭제해도 됩니다. 단, 향후 Compose가 아닌 XML이 필요한 경우(예: Widget, Custom View)를 대비해 남겨둬도 무방합니다.

### ✅ 검증

- [ ] `app/src/main/res/layout/` 디렉토리에 XML 파일이 0개 (또는 디렉토리 자체 삭제)
- [ ] `./gradlew assembleDebug` — `R.layout.*` 참조 에러가 없는지 확인
- [ ] 중간 커밋: `git commit -m "chore: XML Layout 20개 삭제"`

---

## Step 3 — Adapter 파일 제거 (2개 + Fragment 내부 어댑터)

### 목표

> RecyclerView Adapter 파일을 삭제한다. Compose의 `LazyColumn`/`LazyRow`가 RecyclerView + Adapter 패턴을 완전히 대체한다.

### 작업 내용

#### 3.1 독립 Adapter 파일 (2개)

```
삭제: app/src/main/java/com/example/feetballfootball/adapter/FixtureRecyclerViewAdapter.kt
삭제: app/src/main/java/com/example/feetballfootball/adapter/PlayerLineupAdapter.kt
```

| 삭제 파일 | Compose 대체 |
|-----------|-------------|
| `FixtureRecyclerViewAdapter.kt` | `FixtureScreen` 내부 `LazyColumn` + `FixtureItem` Composable |
| `PlayerLineupAdapter.kt` | `FixtureDetailScreen` 내부 라인업 `LazyColumn` |

```bash
rm app/src/main/java/com/example/feetballfootball/adapter/FixtureRecyclerViewAdapter.kt
rm app/src/main/java/com/example/feetballfootball/adapter/PlayerLineupAdapter.kt
```

#### 3.2 Fragment 내부 어댑터 (이미 삭제됨)

Fragment 파일 내에 `inner class` 또는 `private class`로 정의된 어댑터들은 Step 1에서 Fragment와 함께 이미 삭제되었습니다:

- `FixtureDetailEventsFragment` 내부 이벤트 어댑터
- `FixtureDetailStatisticsFragment` 내부 통계 어댑터
- `LeagueClubsStandingFragment` 내부 순위 어댑터
- `LeaguePlayerStandingFragment` 내부 선수 어댑터
- `LeaguesFragment` 내부 리그 선택 어댑터

> 💡 **Tip:** `FixtureRecyclerViewAdapter.kt`에는 `Callbacks` 인터페이스가 정의되어 있었고, 기존 `MainActivity`가 이를 구현했습니다. 새 `MainActivity`에서는 Navigation 3 Route를 통해 화면 전환하므로 이 인터페이스가 불필요합니다.

### ✅ 검증

- [ ] `app/src/main/java/com/example/feetballfootball/adapter/` 디렉토리 아래 `.kt` 파일이 0개
- [ ] `./gradlew assembleDebug` 에러 없음

---

## Step 4 — Behavior 파일 제거 (3개)

### 목표

> 경기 상세 화면의 CoordinatorLayout 기반 헤더 애니메이션 파일을 삭제한다. Compose에서는 `LargeTopAppBar` + `TopAppBarScrollBehavior`로 대체되었다.

### 배경

기존 경기 상세 화면(`FixtureDetailFragment`)은 `CoordinatorLayout` + `AppBarLayout`을 사용하여 스크롤 시 헤더(팀 로고, 스코어)가 축소되는 애니메이션을 구현했습니다. 이를 위해 커스텀 `CoordinatorLayout.Behavior` 클래스 3개가 필요했습니다. Compose에서는 `TopAppBarScrollBehavior`가 동일한 효과를 선언적으로 제공합니다.

### 작업 내용

```
삭제: app/src/main/java/com/example/feetballfootball/behavior/BehaviorHomeTeam.kt
삭제: app/src/main/java/com/example/feetballfootball/behavior/BehaviorAwayTeam.kt
삭제: app/src/main/java/com/example/feetballfootball/behavior/BehaviorScoreTextView.kt
```

| 삭제 파일 | 역할 | Compose 대체 |
|-----------|------|-------------|
| `BehaviorHomeTeam.kt` | 홈팀 로고 스크롤 애니메이션 | `LargeTopAppBar` 내 Composable |
| `BehaviorAwayTeam.kt` | 원정팀 로고 스크롤 애니메이션 | `LargeTopAppBar` 내 Composable |
| `BehaviorScoreTextView.kt` | 스코어 텍스트 스크롤 애니메이션 | `TopAppBarScrollBehavior` |

```bash
rm app/src/main/java/com/example/feetballfootball/behavior/BehaviorHomeTeam.kt
rm app/src/main/java/com/example/feetballfootball/behavior/BehaviorAwayTeam.kt
rm app/src/main/java/com/example/feetballfootball/behavior/BehaviorScoreTextView.kt
```

### ✅ 검증

- [ ] `app/src/main/java/com/example/feetballfootball/behavior/` 디렉토리 아래 `.kt` 파일이 0개
- [ ] `./gradlew assembleDebug` 에러 없음

---

## Step 5 — 기존 ViewModel 제거 (3개)

### 목표

> 레거시 ViewModel을 삭제한다. 새 ViewModel이 각 feature 모듈에 이미 존재한다.

### 배경

기존 ViewModel은 `com.example.feetballfootball.viewModel` 패키지에 있었으며, `thread {}` 블록으로 API를 호출하고 `MutableLiveData`로 결과를 전달했습니다. 새 ViewModel은 `com.chase1st.feetballfootball.feature.*` 패키지에 있으며, Kotlin Coroutines + `StateFlow`를 사용합니다.

### 작업 내용

```
삭제: app/src/main/java/com/example/feetballfootball/viewModel/FeetballFootballViewModel.kt
삭제: app/src/main/java/com/example/feetballfootball/viewModel/FixtureDetailViewModel.kt
삭제: app/src/main/java/com/example/feetballfootball/viewModel/StandingViewModel.kt
```

| 삭제 파일 | 새 ViewModel 위치 |
|-----------|------------------|
| `FeetballFootballViewModel.kt` | `feature.fixture` 모듈의 `FixtureViewModel` |
| `FixtureDetailViewModel.kt` | `feature.fixturedetail` 모듈의 `FixtureDetailViewModel` |
| `StandingViewModel.kt` | `feature.league` 모듈의 `StandingViewModel` |

```bash
rm app/src/main/java/com/example/feetballfootball/viewModel/FeetballFootballViewModel.kt
rm app/src/main/java/com/example/feetballfootball/viewModel/FixtureDetailViewModel.kt
rm app/src/main/java/com/example/feetballfootball/viewModel/StandingViewModel.kt
```

> ⚠️ **주의:** 삭제 전, 새 ViewModel이 기존 ViewModel의 모든 기능을 구현하고 있는지 확인하세요. 특히 `FeetballFootballViewModel`에 정의된 리그 ID 상수(`EPL=39`, `LALIGA=140` 등)가 새 코드에도 존재하는지 체크하세요.

### ✅ 검증

- [ ] `app/src/main/java/com/example/feetballfootball/viewModel/` 디렉토리 아래 `.kt` 파일이 0개
- [ ] `./gradlew assembleDebug` 에러 없음

---

## Step 6 — API 모델 및 네트워크 코드 제거

### 목표

> 기존 Retrofit API 인터페이스, 네트워크 유틸리티, API 응답 모델 클래스를 모두 삭제한다. 이 코드들은 `core-network` 모듈로 이동/재작성되었다.

### 배경

기존 네트워크 코드(API-Sports v3 기반)의 문제점:
- `FootballDataFetchr`에 `x-apisports-key` API 키가 하드코딩
- GSON으로 JSON 파싱 (kotlinx.serialization으로 대체)
- `thread {}` 블록으로 비동기 처리 (Coroutines으로 대체)
- 40+개의 모델 클래스가 flat한 패키지 구조

새 `core-network` 모듈에서는 SofaScore API(`https://api.sofascore.com/api/v1/`) + Retrofit 3.0.0 + kotlinx.serialization + Coroutines로 재구성되었습니다. SofaScore API는 Bearer Token 인증을 사용하며, GET 엔드포인트는 인증 없이도 동작합니다.

### 작업 내용

#### 6.1 API 인터페이스 + 유틸리티 (2개)

```
삭제: app/src/main/java/com/example/feetballfootball/api/FootballApi.kt
삭제: app/src/main/java/com/example/feetballfootball/util/FootballDataFetchr.kt
```

```bash
rm app/src/main/java/com/example/feetballfootball/api/FootballApi.kt
rm app/src/main/java/com/example/feetballfootball/util/FootballDataFetchr.kt
```

#### 6.2 최상위 API 모델 클래스 (11개)

```
삭제: app/src/main/java/com/example/feetballfootball/api/Errors.kt
삭제: app/src/main/java/com/example/feetballfootball/api/Fixture.kt
삭제: app/src/main/java/com/example/feetballfootball/api/FixtureResponse.kt
삭제: app/src/main/java/com/example/feetballfootball/api/FixtureStatus.kt
삭제: app/src/main/java/com/example/feetballfootball/api/FixtureVenue.kt
삭제: app/src/main/java/com/example/feetballfootball/api/FootballResponse.kt
삭제: app/src/main/java/com/example/feetballfootball/api/Goals.kt
삭제: app/src/main/java/com/example/feetballfootball/api/League.kt
삭제: app/src/main/java/com/example/feetballfootball/api/TeamAway.kt
삭제: app/src/main/java/com/example/feetballfootball/api/TeamHome.kt
삭제: app/src/main/java/com/example/feetballfootball/api/Teams.kt
```

```bash
rm app/src/main/java/com/example/feetballfootball/api/Errors.kt
rm app/src/main/java/com/example/feetballfootball/api/Fixture.kt
rm app/src/main/java/com/example/feetballfootball/api/FixtureResponse.kt
rm app/src/main/java/com/example/feetballfootball/api/FixtureStatus.kt
rm app/src/main/java/com/example/feetballfootball/api/FixtureVenue.kt
rm app/src/main/java/com/example/feetballfootball/api/FootballResponse.kt
rm app/src/main/java/com/example/feetballfootball/api/Goals.kt
rm app/src/main/java/com/example/feetballfootball/api/League.kt
rm app/src/main/java/com/example/feetballfootball/api/TeamAway.kt
rm app/src/main/java/com/example/feetballfootball/api/TeamHome.kt
rm app/src/main/java/com/example/feetballfootball/api/Teams.kt
```

#### 6.3 경기 상세 API 모델 클래스 (`api/fixturedetail/`, 17개)

```
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Assist.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Coach.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/EventPlayer.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Events.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/FixtureDetailResponse.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Games.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/LineupTeam.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Lineups.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/MatchDetailResponse.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Player.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/PlayerData.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/PlayerRatingData.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/PlayerStatistics.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/PlayersByTeamData.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/ShortPlayerData.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Statistics.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/StatisticsData.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/TeamColorCode.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/TeamColors.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Time.kt
```

```bash
rm -r app/src/main/java/com/example/feetballfootball/api/fixturedetail/
```

#### 6.4 리그 순위 API 모델 클래스 (`api/leaguestanding/`, 7개)

```
삭제: app/src/main/java/com/example/feetballfootball/api/leaguestanding/All.kt
삭제: app/src/main/java/com/example/feetballfootball/api/leaguestanding/Goals.kt
삭제: app/src/main/java/com/example/feetballfootball/api/leaguestanding/League.kt
삭제: app/src/main/java/com/example/feetballfootball/api/leaguestanding/LeagueStandingsResponse.kt
삭제: app/src/main/java/com/example/feetballfootball/api/leaguestanding/StandingResponse.kt
삭제: app/src/main/java/com/example/feetballfootball/api/leaguestanding/Standings.kt
삭제: app/src/main/java/com/example/feetballfootball/api/leaguestanding/Team.kt
```

```bash
rm -r app/src/main/java/com/example/feetballfootball/api/leaguestanding/
```

#### 6.5 선수 순위 API 모델 클래스 (`api/playerstanding/`, 8개)

```
삭제: app/src/main/java/com/example/feetballfootball/api/playerstanding/Goals.kt
삭제: app/src/main/java/com/example/feetballfootball/api/playerstanding/Penalty.kt
삭제: app/src/main/java/com/example/feetballfootball/api/playerstanding/Player.kt
삭제: app/src/main/java/com/example/feetballfootball/api/playerstanding/PlayerStandingResponse.kt
삭제: app/src/main/java/com/example/feetballfootball/api/playerstanding/PlayerStandingStatistics.kt
삭제: app/src/main/java/com/example/feetballfootball/api/playerstanding/PlayerStatistics.kt
삭제: app/src/main/java/com/example/feetballfootball/api/playerstanding/Shots.kt
```

```bash
rm -r app/src/main/java/com/example/feetballfootball/api/playerstanding/
```

### ✅ 검증

- [ ] `app/src/main/java/com/example/feetballfootball/api/` 디렉토리 아래 파일이 0개
- [ ] `app/src/main/java/com/example/feetballfootball/util/FootballDataFetchr.kt` 삭제됨
- [ ] `./gradlew assembleDebug` — 모든 기존 import 참조 에러가 없는지 확인
- [ ] 중간 커밋: `git commit -m "chore: API 모델 및 네트워크 코드 삭제"`

---

## Step 7 — 유틸리티 및 기타 파일 제거

### 목표

> 남은 유틸리티 파일과 XML 전용 drawable 리소스를 삭제한다.

### 작업 내용

#### 7.1 DividerItemDecoration (1개)

Compose에서는 `HorizontalDivider()` / `VerticalDivider()` Composable을 사용합니다.

```
삭제: app/src/main/java/com/example/feetballfootball/util/DividerItemDecoration.kt
```

```bash
rm app/src/main/java/com/example/feetballfootball/util/DividerItemDecoration.kt
```

#### 7.2 Drawable 리소스 정리

**삭제 가능 — RecyclerView/XML 전용 (6개):**

| 파일 | 용도 | Compose 대체 |
|------|------|-------------|
| `fixture_recycler_item_bg.xml` | RecyclerView 아이템 배경 | `Card` 또는 `Surface` |
| `recyclerview_divider.xml` | RecyclerView 구분선 | `HorizontalDivider()` |
| `circle_progressbar.xml` | XML ProgressBar | `CircularProgressIndicator` |
| `horizon_progressbar.xml` | XML ProgressBar | `LinearProgressIndicator` |
| `tab_selector.xml` | TabLayout 셀렉터 | `TabRow` + `Tab` |
| `drawable_line.xml` | XML 구분선 | `HorizontalDivider()` |

```bash
rm app/src/main/res/drawable/fixture_recycler_item_bg.xml
rm app/src/main/res/drawable/recyclerview_divider.xml
rm app/src/main/res/drawable/circle_progressbar.xml
rm app/src/main/res/drawable/horizon_progressbar.xml
rm app/src/main/res/drawable/tab_selector.xml
rm app/src/main/res/drawable/drawable_line.xml
```

**삭제 가능 — Compose로 대체 (8개):**

| 파일 | 용도 | Compose 대체 |
|------|------|-------------|
| `stadium_away_penalty_area_in.xml` | 라인업 경기장 배경 | `Canvas` + `Path` |
| `stadium_away_penalty_area_out.xml` | 라인업 경기장 배경 | `Canvas` + `Path` |
| `stadium_halfline.xml` | 라인업 경기장 배경 | `Canvas` + `Path` |
| `stadium_halfline_circle.xml` | 라인업 경기장 배경 | `Canvas` + `Path` |
| `stadium_home_penalty_area_in.xml` | 라인업 경기장 배경 | `Canvas` + `Path` |
| `stadium_home_penalty_area_out.xml` | 라인업 경기장 배경 | `Canvas` + `Path` |
| `player_face_bg_circle.xml` | 선수 이미지 원형 배경 | `CircleShape` |
| `player_rating_bg_circle.xml` | 선수 평점 원형 배경 | `CircleShape` |

```bash
rm app/src/main/res/drawable/stadium_away_penalty_area_in.xml
rm app/src/main/res/drawable/stadium_away_penalty_area_out.xml
rm app/src/main/res/drawable/stadium_halfline.xml
rm app/src/main/res/drawable/stadium_halfline_circle.xml
rm app/src/main/res/drawable/stadium_home_penalty_area_in.xml
rm app/src/main/res/drawable/stadium_home_penalty_area_out.xml
rm app/src/main/res/drawable/player_face_bg_circle.xml
rm app/src/main/res/drawable/player_rating_bg_circle.xml
```

**삭제 가능 — Material Icons로 대체된 아이콘:**

Compose에서 `Icons.Default.*`를 사용하므로 아래 아이콘 XML은 삭제합니다. 단, **삭제 전 Compose 코드에서 `painterResource(R.drawable.*)`로 참조하는 곳이 없는지 확인**하세요.

| 파일 | 용도 | Compose 대체 |
|------|------|-------------|
| `ic_fixture.xml` | Bottom Nav 아이콘 | `Icons.Default.SportsSoccer` |
| `ic_league.xml` | Bottom Nav 아이콘 | `Icons.Default.EmojiEvents` |
| `ic_news.xml` | Bottom Nav 아이콘 | `Icons.Default.Newspaper` |
| `ic_arrow_back.xml` | TopAppBar 뒤로가기 | `Icons.Default.ArrowBack` |
| `ic_arrow_forward.xml` | 날짜 선택 화살표 | `Icons.Default.ArrowForward` |
| `arrow_circle_up.xml` | 이벤트 아이콘 (교체 IN) | `Icons.Default.ArrowCircleUp` 또는 커스텀 |
| `arrow_circle_down.xml` | 이벤트 아이콘 (교체 OUT) | `Icons.Default.ArrowCircleDown` 또는 커스텀 |
| `soccer_ball.xml` | 골 아이콘 | 커스텀 Vector 또는 Painter |
| `red_card.xml` | 레드카드 아이콘 | 커스텀 Composable |
| `yellow_card.xml` | 옐로카드 아이콘 | 커스텀 Composable |
| `stadium_penalty_area_arc.xml` | 경기장 아크 | `Canvas` + `Path` |

```bash
rm app/src/main/res/drawable/ic_fixture.xml
rm app/src/main/res/drawable/ic_league.xml
rm app/src/main/res/drawable/ic_news.xml
rm app/src/main/res/drawable/ic_arrow_back.xml
rm app/src/main/res/drawable/ic_arrow_forward.xml
rm app/src/main/res/drawable/arrow_circle_up.xml
rm app/src/main/res/drawable/arrow_circle_down.xml
rm app/src/main/res/drawable/soccer_ball.xml
rm app/src/main/res/drawable/red_card.xml
rm app/src/main/res/drawable/yellow_card.xml
rm app/src/main/res/drawable/stadium_penalty_area_arc.xml
```

**유지 — 앱 아이콘 및 PNG 리소스:**

아래 파일들은 삭제하지 않습니다:

| 파일 | 이유 |
|------|------|
| `ic_launcher_background.xml` | 앱 런처 아이콘 — `AndroidManifest.xml`에서 참조 |
| `barricade_128.png` | 특수 이미지 — Compose에서도 사용 가능 |
| `penalty_kick.png` | 특수 이미지 — Compose에서도 사용 가능 |
| `penalty_kick_missed.png` | 특수 이미지 — Compose에서도 사용 가능 |
| `var_cancelled.png` | 특수 이미지 — Compose에서도 사용 가능 |
| `var_sign.png` | 특수 이미지 — Compose에서도 사용 가능 |

> ⚠️ **주의:** PNG 파일(`barricade_128.png`, `penalty_kick.png` 등)이 Compose 코드에서 `painterResource(R.drawable.*)`로 사용되고 있다면 반드시 유지하세요. 이 파일들은 `soccer_ball.xml` 같은 벡터 드로어블과 달리 Compose에서 직접 사용할 수 있는 비트맵 리소스입니다.

> 💡 **Tip:** 어떤 drawable이 사용 중인지 확실하지 않다면, `./gradlew lint`를 실행하여 "Unused resources" 경고를 확인하는 것이 가장 안전합니다. Lint는 코드와 XML에서 참조되지 않는 리소스를 정확하게 탐지합니다.

### ✅ 검증

- [ ] `DividerItemDecoration.kt` 삭제됨
- [ ] RecyclerView/XML 전용 drawable 6개 삭제됨
- [ ] Compose 대체 drawable 8개 (stadium_*, player_*) 삭제됨
- [ ] Material Icons 대체 아이콘 11개 삭제됨
- [ ] 앱 아이콘(`ic_launcher_background.xml`) 유지됨
- [ ] PNG 파일 유지됨
- [ ] `./gradlew assembleDebug` 에러 없음

---

## Step 8 — 레거시 의존성 정리 (build.gradle)

### 목표

> Fragment/XML/RecyclerView 시대의 라이브러리 의존성을 제거하고, app/build.gradle를 Convention Plugin 기반 Kotlin DSL로 교체한다.

### 배경

기존 `app/build.gradle`(Groovy DSL)에는 이제 불필요한 라이브러리가 다수 포함되어 있습니다. 멀티모듈 구조에서는 각 모듈이 필요한 의존성만 선언하고, app 모듈은 feature 모듈에 대한 의존성만 가집니다.

### 작업 내용

#### 8.1 제거할 의존성 목록

| 기존 의존성 | 대체 | 상태 |
|------------|------|------|
| `com.squareup.retrofit2:retrofit:2.9.0` | core-network에 Retrofit 3.0.0 (kotlinx-serialization 컨버터 내장) | 이동 |
| `com.squareup.retrofit2:converter-scalars:2.5.0` | 제거 (kotlinx.serialization 사용) | 삭제 |
| `com.squareup.retrofit2:converter-gson:2.9.0` | 제거 (kotlinx.serialization 사용) | 삭제 |
| `com.google.code.gson:gson:2.9.0` | 제거 (kotlinx.serialization 사용) | 삭제 |
| `com.squareup.picasso:picasso:2.71828` | Coil 3.x (core-designsystem) | 삭제 |
| `com.jakewharton.threetenabp:threetenabp:1.3.0` | java.time (minSdk 26이므로 백포트 불필요) | 삭제 |
| `androidx.legacy:legacy-support-v4:1.0.0` | 제거 (불필요) | 삭제 |
| `androidx.recyclerview:recyclerview:1.2.1` | Compose LazyColumn/LazyRow | 삭제 |
| `androidx.lifecycle:lifecycle-extensions:2.2.0` | lifecycle-viewmodel-compose | 삭제 |
| `androidx.constraintlayout:constraintlayout:2.1.4` | Compose Layout | 삭제 |
| `androidx.appcompat:appcompat:1.4.2` | ComponentActivity (appcompat 불필요) | 삭제 |
| `com.google.android.material:material:1.6.1` | Material 3 Compose | 삭제 |

#### 8.2 app/build.gradle.kts (새 구조)

기존 Groovy DSL 파일(`app/build.gradle`)을 삭제하고, Convention Plugin 기반 Kotlin DSL로 교체합니다:

**파일: `app/build.gradle.kts`**

```kotlin
// app/build.gradle.kts
// Convention Plugin이 compileSdk, minSdk, Compose 설정 등을 일괄 관리
plugins {
    id("feetball.android.application")
    id("feetball.android.compose")
    id("feetball.android.hilt")
}

dependencies {
    // app 모듈은 feature 모듈에만 의존
    // 각 feature 모듈이 필요한 core 모듈을 직접 의존
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(projects.feature.fixture)
    implementation(projects.feature.fixturDetail)
    implementation(projects.feature.league)
    implementation(projects.feature.news)
}
```

> 💡 **Tip:** Convention Plugin(`feetball.android.application` 등)은 Stage 1에서 `build-logic` 모듈에 정의되었습니다. 이 플러그인이 compileSdk, minSdk, Kotlin/JVM 타겟, Compose 컴파일러 등을 일괄 관리하므로, 각 모듈의 build.gradle.kts가 매우 간결해집니다.

> ⚠️ **주의:** `app/build.gradle` (Groovy)과 `app/build.gradle.kts` (Kotlin)가 동시에 존재하면 Gradle 에러가 발생합니다. 반드시 기존 `app/build.gradle`을 삭제한 후 `app/build.gradle.kts`를 사용하세요.

### ✅ 검증

- [ ] 기존 `app/build.gradle` (Groovy) 삭제됨
- [ ] `app/build.gradle.kts`에 레거시 의존성이 없음
- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew dependencies --configuration debugRuntimeClasspath`에서 GSON, Picasso, ThreeTenABP 등이 없음

---

## Step 9 — 빈 패키지 디렉토리 정리

### 목표

> 코드 파일 삭제 후 남은 빈 디렉토리를 제거하여 프로젝트 구조를 깔끔하게 만든다.

### 작업 내용

Step 1~7에서 모든 파일을 삭제한 후, 아래 디렉토리가 비어있을 것입니다:

```
삭제: app/src/main/java/com/example/feetballfootball/fragment/          ← 전체
삭제: app/src/main/java/com/example/feetballfootball/adapter/           ← 전체
삭제: app/src/main/java/com/example/feetballfootball/behavior/          ← 전체
삭제: app/src/main/java/com/example/feetballfootball/viewModel/         ← 전체
삭제: app/src/main/java/com/example/feetballfootball/api/               ← 전체
삭제: app/src/main/java/com/example/feetballfootball/util/              ← 전체
```

```bash
# 빈 디렉토리 일괄 삭제
rm -rf app/src/main/java/com/example/feetballfootball/fragment/
rm -rf app/src/main/java/com/example/feetballfootball/adapter/
rm -rf app/src/main/java/com/example/feetballfootball/behavior/
rm -rf app/src/main/java/com/example/feetballfootball/viewModel/
rm -rf app/src/main/java/com/example/feetballfootball/api/
rm -rf app/src/main/java/com/example/feetballfootball/util/
```

> 💡 **Tip:** 삭제 후 `app/src/main/java/com/example/feetballfootball/` 디렉토리에는 `MainActivity.kt`만 남아야 합니다. 이 `MainActivity.kt`도 새 패키지(`com.chase1st.feetballfootball`)로 이동된 후 최종적으로 삭제될 수 있습니다.

```bash
# 삭제 후 남은 파일 확인
ls app/src/main/java/com/example/feetballfootball/
# 예상 결과: MainActivity.kt (또는 비어있음 — 새 패키지로 이동 완료 시)
```

### ✅ 검증

- [ ] `fragment/`, `adapter/`, `behavior/`, `viewModel/`, `api/`, `util/` 디렉토리가 모두 삭제됨
- [ ] `./gradlew assembleDebug` 에러 없음

---

## Step 10 — Resources 정리

### 목표

> XML 테마, 색상, 크기 등 Compose로 대체된 values 리소스를 정리한다.

### 작업 내용

#### 10.1 values 리소스 검토

**파일: `app/src/main/res/values/`**

| 파일 | 판단 | 이유 |
|------|------|------|
| `colors.xml` | 삭제 | Compose Theme의 `FeetballColors`로 대체 |
| `themes.xml` | 삭제 | Compose `FeetballTheme`으로 대체 |
| `dimens.xml` | 삭제 | Compose에서 `dp` 직접 사용 |
| `strings.xml` | **유지** | `app_name` 등 `AndroidManifest.xml`에서 참조 |

```bash
rm app/src/main/res/values/colors.xml
rm app/src/main/res/values/themes.xml
rm app/src/main/res/values/dimens.xml
# strings.xml은 유지!
```

> ⚠️ **주의:** `strings.xml`의 `app_name` 문자열은 `AndroidManifest.xml`의 `android:label="@string/app_name"`에서 참조합니다. 이 파일을 삭제하면 빌드 에러가 발생합니다. 반드시 유지하세요.

#### 10.2 values-night 검토

**파일: `app/src/main/res/values-night/`**

| 파일 | 판단 | 이유 |
|------|------|------|
| `themes.xml` | 삭제 | Compose `DynamicColorScheme` / `darkColorScheme`으로 대체 |

```bash
rm app/src/main/res/values-night/themes.xml
# values-night 디렉토리가 비어있으면 디렉토리도 삭제
rm -rf app/src/main/res/values-night/
```

> 💡 **Tip:** Compose의 `FeetballTheme`이 `isSystemInDarkTheme()`을 사용하여 다크/라이트 모드를 자동으로 전환하므로, XML 기반 `values-night/themes.xml`은 완전히 불필요합니다.

### ✅ 검증

- [ ] `colors.xml`, `themes.xml`, `dimens.xml` 삭제됨
- [ ] `strings.xml` 유지됨 (`app_name` 등 포함)
- [ ] `values-night/themes.xml` 삭제됨
- [ ] `./gradlew assembleDebug` 에러 없음

---

## Step 11 — 최종 검증

### 목표

> 모든 레거시 코드 제거가 완료된 후 빌드, Lint, 앱 동작을 종합 검증한다.

### 작업 내용

#### 11.1 Clean 빌드 확인

```bash
./gradlew clean assembleDebug
```

Clean 빌드로 캐시 없이 전체 빌드가 성공하는지 확인합니다.

#### 11.2 Lint 검사

```bash
./gradlew lint
```

확인 항목:
- 사용되지 않는 리소스 경고
- 누락된 참조 에러
- 미사용 import

> 💡 **Tip:** Lint에서 "Unused resources" 경고가 나오면, 해당 리소스가 정말 사용되지 않는지 확인 후 추가 삭제합니다. 특히 PNG 파일이나 `strings.xml`의 문자열이 대상이 될 수 있습니다.

#### 11.3 앱 동작 확인

에뮬레이터 또는 실기기에서 아래 항목을 수동 테스트합니다:

- [ ] 앱 실행 정상 (크래시 없음)
- [ ] 경기 일정 화면 진입 가능
- [ ] 경기 상세 화면 진입 가능 (이벤트/라인업/통계 3탭 전환)
- [ ] 리그 선택 화면 진입 가능
- [ ] 리그 순위 화면 진입 가능
- [ ] 뉴스 화면 진입 가능
- [ ] Bottom Navigation 3탭 전환 동작
- [ ] 시스템 뒤로가기 버튼 동작
- [ ] 다크모드 전환 정상 (시스템 설정 변경 후)
- [ ] ProGuard/R8 (release 빌드 시, 해당되는 경우)

#### 11.4 코드 크기 비교

```
제거 전: app 모듈 ~5,300 LOC (Kotlin + XML)
제거 후: app 모듈 ~50 LOC (MainActivity + FeetballApp Application 클래스만 잔존)
         나머지 모든 코드는 core-*/feature-* 모듈에 분산
```

> 💡 **Tip:** 코드 크기를 확인하려면 다음 명령을 사용하세요:
> ```bash
> # Kotlin 파일 라인 수
> find app/src/main -name "*.kt" | xargs wc -l
> # XML 파일 라인 수
> find app/src/main -name "*.xml" | xargs wc -l
> ```

### ✅ 검증

- [ ] `./gradlew clean assembleDebug` 성공
- [ ] `./gradlew lint` 경고 없음 (또는 수용 가능한 수준)
- [ ] 앱 전체 기능 정상 동작 (위 체크리스트 전항목)
- [ ] 다크모드 전환 정상

---

## Cleanup 완료 검증 체크리스트

모든 Step이 완료되면 아래 항목을 최종 확인합니다:

### 코드 삭제 확인

- [ ] Fragment 10개 전부 삭제
- [ ] XML Layout 20개 전부 삭제
- [ ] Adapter 2개 + Fragment 내부 어댑터 전부 삭제
- [ ] Behavior 3개 전부 삭제
- [ ] 기존 ViewModel 3개 전부 삭제
- [ ] API 모델 전부 삭제 (최상위 11개 + fixturedetail 17개 + leaguestanding 7개 + playerstanding 7개 + FootballApi + FootballDataFetchr)
- [ ] DividerItemDecoration 삭제

### 리소스 삭제 확인

- [ ] 사용하지 않는 drawable 리소스 삭제 (25개)
- [ ] `colors.xml`, `themes.xml`, `dimens.xml` 삭제
- [ ] `values-night/themes.xml` 삭제
- [ ] `strings.xml` 유지 (app_name 등)

### 의존성 정리 확인

- [ ] 레거시 의존성 전부 제거 (GSON, Picasso, ThreeTenABP, RecyclerView, ConstraintLayout, AppCompat, Material 1.x)
- [ ] app/build.gradle.kts가 Convention Plugin 기반으로 전환됨

### 디렉토리 정리 확인

- [ ] `fragment/`, `adapter/`, `behavior/`, `viewModel/`, `api/`, `util/` 빈 디렉토리 삭제

### 빌드 및 동작 확인

- [ ] `./gradlew clean assembleDebug` 성공
- [ ] `./gradlew lint` 경고 없음
- [ ] 앱 전체 기능 정상 동작

### 커밋

```bash
git add .
git commit -m "chore: 레거시 코드 전면 제거 (Fragment/XML/Adapter/ViewModel)"
```

### 삭제된 파일 전체 요약

| 카테고리 | 파일 수 | 디렉토리 |
|----------|---------|----------|
| Fragment (Fixture) | 5 | `fragment/fixture/` |
| Fragment (League) | 4 | `fragment/Leagues/` |
| Fragment (News) | 1 | `fragment/news/` |
| XML Layout (Fragment) | 10 | `res/layout/fragment_*.xml` |
| XML Layout (RecyclerView) | 9 | `res/layout/*.xml` |
| XML Layout (Activity) | 1 | `res/layout/activity_main.xml` |
| Adapter | 2 | `adapter/` |
| Behavior | 3 | `behavior/` |
| ViewModel | 3 | `viewModel/` |
| API (최상위) | 11 | `api/` |
| API (fixturedetail) | 17 | `api/fixturedetail/` |
| API (leaguestanding) | 7 | `api/leaguestanding/` |
| API (playerstanding) | 7 | `api/playerstanding/` |
| API Interface | 1 | `api/FootballApi.kt` |
| Utility | 2 | `util/` |
| Drawable | ~25 | `res/drawable/` |
| Values | 3+1 | `res/values/`, `res/values-night/` |
| **합계** | **~110+** | |

---

> 이전 단계: [Stage 2 / Navigation — Navigation 3 통합](stage-2-navigation.md)


---

# Stage 3 / Testing — 테스트 인프라 구축
> ⏱ 예상 소요 시간: 8시간 | 난이도: ★★★ | 선행 조건: Stage 2 완료

---

## 이 Codelab에서 배우는 것

- JUnit 5 Extension을 활용한 **코루틴 테스트 환경 설정** (TestDispatcherExtension)
- **Fake / TestDouble 패턴**으로 외부 의존성 격리 (FakeApiService, FakeRepository)
- **테스트 데이터 팩토리** (TestFixtures)를 통해 반복 코드 제거
- UseCase, Repository, Mapper에 대한 **단위 테스트** 작성
- **Turbine 라이브러리**로 StateFlow/SharedFlow 테스트
- ViewModel의 **UiState 전이** 검증 (Loading → Success / Error / Empty)
- **Compose UI Test**로 화면 렌더링 및 사용자 인터랙션 검증
- **JUnit 5 ParameterizedTest**로 매핑 로직 일괄 검증

---

## 완성 후 결과물

| 항목 | 설명 |
|------|------|
| TestDispatcherExtension | JUnit 5 Extension — 모든 ViewModel 테스트에서 `Dispatchers.Main` 교체 |
| FakeFootballApiService | 네트워크 호출 없이 API 응답을 주입할 수 있는 Fake 구현체 |
| FakeFixtureRepository / FakeLeagueRepository | Repository 인터페이스의 Fake 구현체 |
| TestFixtures | Fixture, TeamStanding, PlayerStanding 샘플 데이터 팩토리 |
| UseCase 테스트 5종 | GetFixturesByDate, GetLeagueStandings, GetTopScorers, GetTopAssists, GetFixtureDetail |
| Repository 통합 테스트 | DTO → Domain 매핑 정상 동작 검증 |
| Mapper 단위 테스트 | SofaScore EventStatus 매핑, 이벤트 타입, 그리드 파싱, 통계 파싱 |
| ViewModel 테스트 | FixtureViewModel, StandingViewModel — Turbine 기반 StateFlow 검증 |
| Compose UI 테스트 | FixtureScreen, StandingScreen — 렌더링 + 인터랙션 검증 |
| 테스트 커버리지 목표 | core-domain 90%+, core-data 80%+, feature ViewModel 80%+, Compose UI 60%+ |

---

## Step 1 — 테스트 의존성 확인

### 목표
> Stage 1에서 추가한 테스트 의존성과 Convention Plugin이 올바르게 설정되어 있는지 확인합니다.

### 작업 내용

**파일:** `gradle/libs.versions.toml`

Stage 1에서 이미 추가한 테스트 라이브러리 버전과 의존성이 아래와 같은지 확인합니다:

```toml
[versions]
junit-jupiter = "6.0.3"          # JUnit 6 GA (2026-02-15). Jupiter API는 5.x 호환, JDK 17+ 필수
mockk = "1.14.7"
turbine = "1.2.1"
coroutines-test = "1.10.2"

[libraries]
# JUnit 6: 좌표는 5.x와 동일 (org.junit.jupiter:*), BOM(`org.junit:junit-bom:6.0.3`) 사용도 가능
junit-jupiter-api = { group = "org.junit.jupiter", name = "junit-jupiter-api", version.ref = "junit-jupiter" }
junit-jupiter-engine = { group = "org.junit.jupiter", name = "junit-jupiter-engine", version.ref = "junit-jupiter" }
junit-jupiter-params = { group = "org.junit.jupiter", name = "junit-jupiter-params", version.ref = "junit-jupiter" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines-test" }
```

**파일:** `build-logic/convention/src/main/kotlin/AndroidTestConventionPlugin.kt`

Stage 1에서 이미 생성된 Convention Plugin입니다. `feetball.android.test` 플러그인을 적용하면 JUnit Jupiter(6.0.3) + MockK + Turbine + Coroutines Test가 자동으로 설정됩니다.

```kotlin
// build-logic/convention/src/main/kotlin/AndroidTestConventionPlugin.kt
// Stage 1에서 이미 생성됨 — JUnit Jupiter(6.0.3) + MockK + Turbine + Coroutines Test 설정
```

> 💡 **Tip:** Convention Plugin 덕분에 각 모듈의 `build.gradle.kts`에서 테스트 의존성을 개별 선언할 필요가 없습니다. `plugins { id("feetball.android.test") }` 한 줄이면 충분합니다.

### ✅ 검증
- [ ] `libs.versions.toml`에 junit-jupiter(6.0.3), mockk, turbine, coroutines-test 버전이 선언되어 있다
- [ ] `AndroidTestConventionPlugin.kt`이 존재하고 올바르게 등록되어 있다
- [ ] 각 모듈의 `build.gradle.kts`에서 `feetball.android.test` 플러그인이 적용되어 있다

---

## Step 2 — 테스트용 Fake/TestDouble 구성

### 목표
> 외부 의존성(네트워크, DB)을 격리하기 위한 Fake 구현체와 재사용 가능한 테스트 데이터 팩토리를 생성합니다.

### 작업 내용

#### 2.1 TestDispatcherExtension 생성

**파일:** `core/core-common/src/test/kotlin/com/chase1st/feetballfootball/core/common/testing/TestDispatcherExtension.kt`

**이유:** ViewModel 테스트에서 `Dispatchers.Main`을 `StandardTestDispatcher`로 교체해야 합니다. JUnit 5에서는 `@Rule` 대신 `Extension`을 사용합니다. 이 Extension을 `@RegisterExtension`으로 등록하면, 각 테스트 전후에 자동으로 Main Dispatcher를 설정/해제합니다.

```kotlin
package com.chase1st.feetballfootball.core.common.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherExtension(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(dispatcher)
    }

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }
}
```

> ⚠️ **주의:** `UnconfinedTestDispatcher`가 아닌 `StandardTestDispatcher`를 기본값으로 사용합니다. `StandardTestDispatcher`는 코루틴을 즉시 실행하지 않으므로, `advanceUntilIdle()` 등으로 실행 타이밍을 명시적으로 제어할 수 있어 더 정확한 테스트가 가능합니다.

#### 2.2 FakeFootballApiService 생성

**파일:** `core/core-network/src/test/kotlin/com/chase1st/feetballfootball/core/network/testing/FakeFootballApiService.kt`

**이유:** 실제 네트워크 호출 없이 Repository 테스트를 수행하기 위한 Fake API 서비스입니다. `shouldThrowError` 플래그로 에러 시나리오도 시뮬레이션할 수 있습니다.

```kotlin
package com.chase1st.feetballfootball.core.network.testing

import com.chase1st.feetballfootball.core.network.api.FootballApiService
import com.chase1st.feetballfootball.core.network.model.*

class FakeFootballApiService : FootballApiService {

    // 테스트에서 응답을 주입할 수 있는 변수들
    // SofaScore API는 ApiResponse<T> 래퍼 없이 직접 응답을 반환
    var matchesByDateResponse: MatchesDayResponseDto = MatchesDayResponseDto()
    var standingsResponse: List<TlTableResponseDto> = emptyList()
    var fixtureDetailResponse: FixtureDetailResponseDto? = null

    // 에러 시뮬레이션
    var shouldThrowError = false
    var errorToThrow: Exception = RuntimeException("Test error")

    override suspend fun getMatchesByDate(date: String, timezone: String, ccode3: String): MatchesDayResponseDto {
        if (shouldThrowError) throw errorToThrow
        return matchesByDateResponse
    }

    override suspend fun getStandings(leagueId: Int): List<TlTableResponseDto> {
        if (shouldThrowError) throw errorToThrow
        return standingsResponse
    }

    override suspend fun getMatchDetail(matchId: Int): FixtureDetailResponseDto {
        if (shouldThrowError) throw errorToThrow
        return fixtureDetailResponse ?: throw RuntimeException("No fixture detail data")
    }
}
```

> 💡 **Tip:** Fake는 MockK의 `mockk<>()`와 달리 실제 인터페이스를 구현하므로, 컴파일 타임에 누락된 메서드를 잡아줍니다. API 인터페이스에 새 메서드가 추가되면 Fake도 강제 업데이트됩니다.

#### 2.3 FakeRepository 구현체 생성

**파일:** `core/core-data/src/test/kotlin/com/chase1st/feetballfootball/core/data/testing/FakeFixtureRepository.kt`

**이유:** UseCase 테스트와 ViewModel 테스트에서 Repository를 대체합니다. Map 자료구조로 입력-출력을 사전 정의하여, 특정 인자에 대한 응답을 제어합니다.

```kotlin
package com.chase1st.feetballfootball.core.data.testing

import com.chase1st.feetballfootball.core.common.Result
import com.chase1st.feetballfootball.core.domain.repository.FixtureRepository
import com.chase1st.feetballfootball.core.model.*
import java.time.LocalDate

class FakeFixtureRepository : FixtureRepository {

    var fixturesByDate: Map<LocalDate, Result<Map<LeagueInfo, List<Fixture>>>> = emptyMap()
    var fixtureDetail: Map<Int, Result<MatchDetail>> = emptyMap()

    override suspend fun getFixturesByDate(date: LocalDate): Result<Map<LeagueInfo, List<Fixture>>> {
        return fixturesByDate[date] ?: Result.Error(Exception("No data for $date"))
    }

    override suspend fun getFixtureDetail(fixtureId: Int): Result<MatchDetail> {
        return fixtureDetail[fixtureId] ?: Result.Error(Exception("No data for fixture $fixtureId"))
    }
}
```

**파일:** `core/core-data/src/test/kotlin/com/chase1st/feetballfootball/core/data/testing/FakeLeagueRepository.kt`

```kotlin
package com.chase1st.feetballfootball.core.data.testing

import com.chase1st.feetballfootball.core.common.Result
import com.chase1st.feetballfootball.core.domain.repository.LeagueRepository
import com.chase1st.feetballfootball.core.model.*

class FakeLeagueRepository : LeagueRepository {

    var standings: Map<Int, Result<List<TeamStanding>>> = emptyMap()
    var topScorers: Map<Int, Result<List<PlayerStanding>>> = emptyMap()
    var topAssists: Map<Int, Result<List<PlayerStanding>>> = emptyMap()

    override suspend fun getStandings(leagueId: Int, season: Int): Result<List<TeamStanding>> {
        return standings[leagueId] ?: Result.Error(Exception("No standings"))
    }

    override suspend fun getTopScorers(leagueId: Int, season: Int): Result<List<PlayerStanding>> {
        return topScorers[leagueId] ?: Result.Error(Exception("No top scorers"))
    }

    override suspend fun getTopAssists(leagueId: Int, season: Int): Result<List<PlayerStanding>> {
        return topAssists[leagueId] ?: Result.Error(Exception("No top assists"))
    }
}
```

#### 2.4 테스트 Fixture 데이터 팩토리 생성

**파일:** `core/core-model/src/test/kotlin/com/chase1st/feetballfootball/core/model/testing/TestFixtures.kt`

**이유:** 테스트마다 Fixture, TeamStanding, PlayerStanding 객체를 반복 생성하는 코드를 제거합니다. 기본값이 있는 팩토리 메서드로, 테스트에서 필요한 필드만 오버라이드하면 됩니다.

```kotlin
package com.chase1st.feetballfootball.core.model.testing

import com.chase1st.feetballfootball.core.model.*
import java.time.LocalDate
import java.time.LocalTime

/**
 * 테스트 전용 샘플 데이터 팩토리
 */
object TestFixtures {

    fun fixture(
        id: Int = 1,
        homeTeam: String = "Arsenal",
        awayTeam: String = "Chelsea",
        homeGoals: Int? = 2,
        awayGoals: Int? = 1,
        status: MatchStatus = MatchStatus.FINISHED,
        date: LocalDate = LocalDate.of(2026, 3, 12),
        time: LocalTime = LocalTime.of(20, 0),
    ) = Fixture(
        id = id,
        homeTeam = Team(id = 42, name = homeTeam, logoUrl = "https://img.sofascore.com/api/v1/team/42/image"),
        awayTeam = Team(id = 49, name = awayTeam, logoUrl = "https://img.sofascore.com/api/v1/team/49/image"),
        homeGoals = homeGoals,
        awayGoals = awayGoals,
        status = status,
        date = date,
        time = time,
    )

    fun teamStanding(
        rank: Int = 1,
        teamName: String = "Arsenal",
        played: Int = 28,
        won: Int = 20,
        drawn: Int = 5,
        lost: Int = 3,
        goalsFor: Int = 60,
        goalsAgainst: Int = 20,
        points: Int = 65,
    ) = TeamStanding(
        rank = rank,
        team = Team(id = rank, name = teamName, logoUrl = "https://img.sofascore.com/api/v1/team/$rank/image"),
        played = played,
        won = won,
        drawn = drawn,
        lost = lost,
        goalsFor = goalsFor,
        goalsAgainst = goalsAgainst,
        goalDifference = goalsFor - goalsAgainst,
        points = points,
    )

    fun playerStanding(
        rank: Int = 1,
        playerName: String = "Haaland",
        teamName: String = "Manchester City",
        goals: Int = 20,
        assists: Int = 5,
    ) = PlayerStanding(
        rank = rank,
        player = Player(id = rank, name = playerName, photoUrl = "https://img.sofascore.com/api/v1/player/$rank/image"),
        team = Team(id = rank, name = teamName, logoUrl = "https://img.sofascore.com/api/v1/team/$rank/image"),
        goals = goals,
        assists = assists,
        appearances = 28,
    )
}
```

> 💡 **Tip:** 팩토리 메서드의 기본값은 "합리적인 기본 상태"로 설정합니다. 테스트에서는 검증 대상 필드만 명시적으로 지정하면 가독성이 크게 향상됩니다. 예: `TestFixtures.fixture(id = 42, homeTeam = "Liverpool")`

### ✅ 검증
- [ ] `TestDispatcherExtension`이 컴파일 성공
- [ ] `FakeFootballApiService`가 `FootballApiService` 인터페이스를 올바르게 구현
- [ ] `FakeFixtureRepository`와 `FakeLeagueRepository`가 각각의 Repository 인터페이스를 올바르게 구현
- [ ] `TestFixtures`의 팩토리 메서드가 유효한 도메인 모델 객체를 생성

---

## Step 3 — UseCase 단위 테스트

### 목표
> 각 UseCase의 비즈니스 로직이 올바르게 동작하는지 검증합니다. FakeRepository를 주입하여 네트워크 의존성 없이 테스트합니다.

### 작업 내용

#### 3.1 GetFixturesByDateUseCase 테스트

**파일:** `core/core-domain/src/test/kotlin/com/chase1st/feetballfootball/core/domain/usecase/GetFixturesByDateUseCaseTest.kt`

**이유:** 가장 핵심적인 UseCase입니다. 날짜별 경기 조회의 성공/실패 시나리오를 검증합니다.

**구조:** Given-When-Then 패턴을 사용합니다. `@BeforeEach`에서 FakeRepository와 UseCase를 초기화하고, 각 테스트에서 Repository의 응답을 사전 설정한 뒤 UseCase를 실행합니다.

```kotlin
package com.chase1st.feetballfootball.core.domain.usecase

import com.chase1st.feetballfootball.core.common.Result
import com.chase1st.feetballfootball.core.data.testing.FakeFixtureRepository
import com.chase1st.feetballfootball.core.model.testing.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetFixturesByDateUseCaseTest {

    private lateinit var repository: FakeFixtureRepository
    private lateinit var useCase: GetFixturesByDateUseCase

    @BeforeEach
    fun setup() {
        repository = FakeFixtureRepository()
        useCase = GetFixturesByDateUseCase(repository)
    }

    @Test
    fun `날짜별 경기 조회 성공`() = runTest {
        // Given
        val date = LocalDate.of(2026, 3, 12)
        val fixtures = mapOf(
            SupportedLeagues.EPL to listOf(TestFixtures.fixture())
        )
        repository.fixturesByDate = mapOf(date to Result.Success(fixtures))

        // When
        val result = useCase(date)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.size)
    }

    @Test
    fun `에러 발생 시 Result_Error 반환`() = runTest {
        // Given
        val date = LocalDate.of(2026, 3, 12)
        // repository에 해당 날짜 데이터 없음 → Error

        // When
        val result = useCase(date)

        // Then
        assertTrue(result is Result.Error)
    }
}
```

#### 3.2 GetLeagueStandingsUseCase 테스트

**파일:** `core/core-domain/src/test/kotlin/com/chase1st/feetballfootball/core/domain/usecase/GetLeagueStandingsUseCaseTest.kt`

**이유:** 리그 순위 조회 시 올바른 정렬 순서가 유지되는지 검증합니다. Repository에서 역순으로 데이터를 반환하더라도 UseCase가 정렬을 보장하는지 확인합니다.

```kotlin
@Test
fun `리그 순위 조회 성공 - 순위 정렬 확인`() = runTest {
    // Given
    val standings = listOf(
        TestFixtures.teamStanding(rank = 2, teamName = "Chelsea", points = 55),
        TestFixtures.teamStanding(rank = 1, teamName = "Arsenal", points = 65),
    )
    repository.standings = mapOf(17 to Result.Success(standings))

    // When
    val result = useCase(leagueId = 17, season = 2025)

    // Then
    assertTrue(result is Result.Success)
    val data = (result as Result.Success).data
    assertEquals("Arsenal", data[0].team.name)  // 순위 1위가 먼저
}
```

> 💡 **Tip:** UseCase 테스트는 "비즈니스 규칙"에 집중합니다. 네트워크 에러 처리나 DTO 매핑은 Repository/Mapper 테스트에서 다룹니다.

### ✅ 검증
- [ ] `GetFixturesByDateUseCaseTest` — 성공/실패 시나리오 통과
- [ ] `GetLeagueStandingsUseCaseTest` — 순위 정렬 검증 통과
- [ ] 같은 패턴으로 `GetTopScorersUseCaseTest`, `GetTopAssistsUseCaseTest`, `GetFixtureDetailUseCaseTest` 추가 작성
- [ ] `./gradlew :core:core-domain:test` 성공

---

## Step 4 — Repository 통합 테스트

### 목표
> FakeApiService를 사용하여 Repository의 DTO → Domain 매핑이 정상적으로 동작하는지, 에러 처리가 올바른지 검증합니다.

### 작업 내용

#### 4.1 FixtureRepositoryImpl 테스트

**파일:** `core/core-data/src/test/kotlin/com/chase1st/feetballfootball/core/data/repository/FixtureRepositoryImplTest.kt`

**이유:** Repository는 API 응답(DTO)을 도메인 모델로 변환하는 핵심 레이어입니다. FakeApiService를 주입하여 매핑 로직과 에러 핸들링을 검증합니다.

**구조:** FakeApiService에 DTO 응답을 설정한 뒤, Repository 메서드를 호출하여 변환 결과를 검증합니다.

```kotlin
package com.chase1st.feetballfootball.core.data.repository

import com.chase1st.feetballfootball.core.common.Result
import com.chase1st.feetballfootball.core.network.testing.FakeFootballApiService
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FixtureRepositoryImplTest {

    private lateinit var fakeApi: FakeFootballApiService
    private lateinit var repository: FixtureRepositoryImpl

    @BeforeEach
    fun setup() {
        fakeApi = FakeFootballApiService()
        repository = FixtureRepositoryImpl(fakeApi)
    }

    @Test
    fun `API 성공 시 도메인 모델로 변환`() = runTest {
        // Given — FakeFootballApiService에 SofaScore DTO 응답 설정
        fakeApi.matchesByDateResponse = createSampleMatchesDayResponse()

        // When
        val result = repository.getFixturesByDate(LocalDate.of(2026, 3, 12))

        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertFalse(data.isEmpty())
    }

    @Test
    fun `API 에러 시 Result_Error 반환`() = runTest {
        // Given
        fakeApi.shouldThrowError = true

        // When
        val result = repository.getFixturesByDate(LocalDate.of(2026, 3, 12))

        // Then
        assertTrue(result is Result.Error)
    }

    @Test
    fun `지원하지 않는 리그 필터링`() = runTest {
        // Given — 지원 리그 + 미지원 리그 혼합 응답
        fakeApi.matchesByDateResponse = createMixedLeagueResponse()

        // When
        val result = repository.getFixturesByDate(LocalDate.of(2026, 3, 12))

        // Then
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        // 모든 key가 지원 리그인지 확인
        data.keys.forEach { league ->
            assertTrue(SupportedLeagues.ALL_LEAGUE_IDS.contains(league.id))
        }
    }
}
```

> ⚠️ **주의:** `createSampleMatchesDayResponse()`와 `createMixedLeagueResponse()`는 테스트 헬퍼 메서드입니다. SofaScore API 응답 구조(`EventsResponseDto`, `EventDto` 등)에 맞는 DTO 객체를 생성하도록 구현해야 합니다. 이 메서드는 같은 테스트 파일 내에 `private fun`으로 작성하거나, 별도의 TestDtoFactory 객체로 분리할 수 있습니다.

#### 4.2 Mapper 단위 테스트 — FixtureMapper

**파일:** `core/core-data/src/test/kotlin/com/chase1st/feetballfootball/core/data/mapper/FixtureMapperTest.kt`

**이유:** SofaScore EventStatus의 `status.type` 필드(`"notstarted"`, `"inprogress"`, `"finished"`, `"postponed"`, `"canceled"`)를 앱 내부 MatchStatus enum으로 변환하는 핵심 로직입니다. 각 상태값에 대해 올바른 상태가 반환되는지 검증합니다.

```kotlin
package com.chase1st.feetballfootball.core.data.mapper

import com.chase1st.feetballfootball.core.model.MatchStatus
import com.chase1st.feetballfootball.core.network.model.MatchStatusDto
import com.chase1st.feetballfootball.core.network.model.MatchStatusReasonDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FixtureMapperTest {

    // SofaScore는 status.type 문자열("notstarted", "inprogress", "finished" 등)으로 상태를 판단
    @Test
    fun `종료된 경기 매핑`() {
        val status = EventStatusDto(type = "finished")
        val result = MatchStatus.fromSofaScoreStatus(status)
        assertEquals(MatchStatus.FINISHED, result)
    }

    @Test
    fun `진행중인 경기 매핑`() {
        val status = EventStatusDto(type = "inprogress")
        val result = MatchStatus.fromSofaScoreStatus(status)
        assertEquals(MatchStatus.LIVE, result)
    }

    @Test
    fun `취소된 경기 매핑`() {
        val status = EventStatusDto(type = "canceled")
        val result = MatchStatus.fromSofaScoreStatus(status)
        assertEquals(MatchStatus.CANCELLED, result)
    }

    @Test
    fun `하프타임 매핑`() {
        val status = EventStatusDto(
            type = "inprogress",
            description = "Halftime",
        )
        val result = MatchStatus.fromSofaScoreStatus(status)
        assertEquals(MatchStatus.HALF_TIME, result)
    }

    @Test
    fun `연기된 경기 매핑`() {
        val status = EventStatusDto(type = "postponed")
        val result = MatchStatus.fromSofaScoreStatus(status)
        assertEquals(MatchStatus.POSTPONED, result)
    }

    @Test
    fun `시작 전 경기는 NOT_STARTED 반환`() {
        val status = EventStatusDto(type = "notstarted")
        val result = MatchStatus.fromSofaScoreStatus(status)
        assertEquals(MatchStatus.NOT_STARTED, result)
    }
}
```

> 💡 **Tip:** SofaScore API는 API-Sports와 달리 `status.type` 문자열(`"notstarted"`, `"inprogress"`, `"finished"`, `"postponed"`, `"canceled"`)로 경기 상태를 표현합니다. 각 상태값에 대한 테스트 케이스를 작성합니다.

#### 4.3 Mapper 단위 테스트 — FixtureDetailMapper

**파일:** `core/core-data/src/test/kotlin/com/chase1st/feetballfootball/core/data/mapper/FixtureDetailMapperTest.kt`

**이유:** 경기 상세 화면의 라인업 그리드 파싱, 이벤트 타입 매핑, 통계 값 파싱은 API 응답의 문자열을 구조화된 데이터로 변환하는 중요한 로직입니다.

```kotlin
class FixtureDetailMapperTest {

    @Test
    fun `라인업 그리드 파싱 - 1_1 형식`() {
        val result = parseGrid("1:1")
        assertEquals(1, result.first)  // row
        assertEquals(1, result.second) // col
    }

    @Test
    fun `라인업 그리드 파싱 - 4_3 형식`() {
        val result = parseGrid("4:3")
        assertEquals(4, result.first)
        assertEquals(3, result.second)
    }

    @Test
    fun `이벤트 타입 매핑 - Goal`() {
        val result = EventType.from(type = "Goal", detail = "Normal Goal")
        assertEquals(EventType.GOAL, result)
    }

    @Test
    fun `이벤트 타입 매핑 - Own Goal`() {
        val result = EventType.from(type = "Goal", detail = "Own Goal")
        assertEquals(EventType.OWN_GOAL, result)
    }

    @Test
    fun `이벤트 타입 매핑 - Missed Penalty`() {
        val result = EventType.from(type = "Goal", detail = "Missed Penalty")
        assertEquals(EventType.MISSED_PENALTY, result)
    }

    @Test
    fun `이벤트 타입 매핑 - Penalty 득점`() {
        val result = EventType.from(type = "Goal", detail = "Penalty")
        assertEquals(EventType.PENALTY, result)
    }

    @Test
    fun `통계 매핑 - 퍼센트 문자열 파싱`() {
        val result = parseStatValue("65%")
        assertEquals(65f, result)
    }

    @Test
    fun `통계 매핑 - 정수 문자열 파싱`() {
        val result = parseStatValue("12")
        assertEquals(12f, result)
    }

    @Test
    fun `통계 매핑 - null 값은 0 반환`() {
        val result = parseStatValue(null)
        assertEquals(0f, result)
    }
}
```

> 💡 **Tip:** 라인업 그리드 파싱(`parseGrid`)과 통계 값 파싱(`parseStatValue`)은 작은 유틸리티 함수이지만, 잘못된 파싱은 UI 렌더링에 직접적인 영향을 미칩니다. 엣지 케이스(null, 빈 문자열, 예상치 못한 형식)를 반드시 테스트하세요.

### ✅ 검증
- [ ] `FixtureRepositoryImplTest` — API 성공/에러/리그 필터링 테스트 통과
- [ ] `FixtureMapperTest` — SofaScore EventStatus 상태값 매핑 테스트 통과
- [ ] `FixtureDetailMapperTest` — 그리드 파싱, 이벤트 타입, 통계 파싱 테스트 통과
- [ ] `./gradlew :core:core-data:test` 성공

---

## Step 5 — ViewModel 테스트

### 목표
> Turbine을 사용하여 ViewModel의 StateFlow 전이(Loading → Success/Error/Empty)를 검증합니다.

### 작업 내용

#### 5.1 FixtureViewModel 테스트

**파일:** `feature/feature-fixture/src/test/kotlin/com/chase1st/feetballfootball/feature/fixture/FixtureViewModelTest.kt`

**이유:** FixtureViewModel은 앱의 메인 화면을 제어합니다. 초기 상태, 데이터 로딩 성공/실패, 날짜 변경에 따른 상태 전이를 모두 검증해야 합니다.

**구조:**
- `TestDispatcherExtension`을 `@RegisterExtension`으로 등록하여 Main Dispatcher를 교체합니다.
- Turbine의 `test {}` 블록으로 StateFlow의 각 emission을 순차적으로 검증합니다.
- `skipItems(1)`로 초기 Loading 상태를 건너뛰고, 다음 상태를 검증합니다.

```kotlin
package com.chase1st.feetballfootball.feature.fixture

import app.cash.turbine.test
import com.chase1st.feetballfootball.core.common.Result
import com.chase1st.feetballfootball.core.common.testing.TestDispatcherExtension
import com.chase1st.feetballfootball.core.data.testing.FakeFixtureRepository
import com.chase1st.feetballfootball.core.model.testing.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate

class FixtureViewModelTest {

    @JvmField
    @RegisterExtension
    val dispatcherExtension = TestDispatcherExtension()

    private lateinit var repository: FakeFixtureRepository
    private lateinit var viewModel: FixtureViewModel

    @BeforeEach
    fun setup() {
        repository = FakeFixtureRepository()
    }

    @Test
    fun `초기 상태는 Loading`() = runTest {
        viewModel = FixtureViewModel(GetFixturesByDateUseCase(repository))

        viewModel.uiState.test {
            assertEquals(FixtureUiState.Loading, awaitItem())
        }
    }

    @Test
    fun `경기 데이터 로딩 성공 시 Success 상태`() = runTest {
        // Given
        val today = LocalDate.now()
        val fixtures = mapOf(
            SupportedLeagues.EPL to listOf(TestFixtures.fixture())
        )
        repository.fixturesByDate = mapOf(today to Result.Success(fixtures))

        // When
        viewModel = FixtureViewModel(GetFixturesByDateUseCase(repository))

        // Then
        viewModel.uiState.test {
            skipItems(1) // Loading
            val state = awaitItem()
            assertTrue(state is FixtureUiState.Success)
        }
    }

    @Test
    fun `경기 데이터 없을 때 Empty 상태`() = runTest {
        // Given
        val today = LocalDate.now()
        repository.fixturesByDate = mapOf(today to Result.Success(emptyMap()))

        // When
        viewModel = FixtureViewModel(GetFixturesByDateUseCase(repository))

        // Then
        viewModel.uiState.test {
            skipItems(1) // Loading
            val state = awaitItem()
            assertTrue(state is FixtureUiState.Empty)
        }
    }

    @Test
    fun `날짜 변경 시 새 데이터 로딩`() = runTest {
        // Given
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        repository.fixturesByDate = mapOf(
            today to Result.Success(mapOf(SupportedLeagues.EPL to listOf(TestFixtures.fixture(id = 1)))),
            tomorrow to Result.Success(mapOf(SupportedLeagues.EPL to listOf(TestFixtures.fixture(id = 2)))),
        )
        viewModel = FixtureViewModel(GetFixturesByDateUseCase(repository))

        // When
        viewModel.onDateSelected(tomorrow)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is FixtureUiState.Success)
        }
    }

    @Test
    fun `에러 발생 시 Error 상태`() = runTest {
        // Given — repository에 데이터 없음 → Error

        // When
        viewModel = FixtureViewModel(GetFixturesByDateUseCase(repository))

        // Then
        viewModel.uiState.test {
            skipItems(1) // Loading
            val state = awaitItem()
            assertTrue(state is FixtureUiState.Error)
        }
    }
}
```

> ⚠️ **주의:** `@RegisterExtension`에 반드시 `@JvmField`를 함께 사용해야 합니다. JUnit 5 Extension은 Java 필드로 노출되어야 동작하기 때문입니다.

#### 5.2 StandingViewModel 테스트

**파일:** `feature/feature-league/src/test/kotlin/com/chase1st/feetballfootball/feature/league/StandingViewModelTest.kt`

**이유:** StandingViewModel은 리그 순위, 득점 순위, 어시스트 순위를 동시에 로딩합니다. 세 API 호출이 모두 성공해야 Success 상태로 전이되는지 검증합니다.

```kotlin
@Test
fun `리그 순위와 선수 순위 동시 로딩`() = runTest {
    // Given
    repository.standings = mapOf(17 to Result.Success(listOf(TestFixtures.teamStanding())))
    repository.topScorers = mapOf(17 to Result.Success(listOf(TestFixtures.playerStanding())))
    repository.topAssists = mapOf(17 to Result.Success(listOf(TestFixtures.playerStanding(assists = 10))))

    // When — SofaScore uniqueTournamentId 사용 (Premier League = 17)
    viewModel = StandingViewModel(
        getStandingsUseCase = GetLeagueStandingsUseCase(repository),
        getTopScorersUseCase = GetTopScorersUseCase(repository),
        getTopAssistsUseCase = GetTopAssistsUseCase(repository),
        savedStateHandle = SavedStateHandle(mapOf("leagueId" to 17)),
    )

    // Then
    viewModel.uiState.test {
        skipItems(1) // Loading
        val state = awaitItem()
        assertTrue(state is StandingUiState.Success)
        val success = state as StandingUiState.Success
        assertFalse(success.clubStandings.isEmpty())
        assertFalse(success.topScorers.isEmpty())
    }
}
```

> 💡 **Tip:** `SavedStateHandle`에 `mapOf("leagueId" to 17)`를 전달하여 Navigation 인자를 시뮬레이션합니다. SofaScore uniqueTournamentId를 사용합니다 (예: Premier League = 17). 이는 Hilt의 `@assisted` 없이도 ViewModel에 Navigation 인자를 주입하는 표준 패턴입니다.

### ✅ 검증
- [ ] `FixtureViewModelTest` — Loading/Success/Empty/Error/날짜 변경 테스트 통과
- [ ] `StandingViewModelTest` — 동시 로딩 테스트 통과
- [ ] 같은 패턴으로 `FixtureDetailViewModelTest` 추가 작성
- [ ] `./gradlew :feature:feature-fixture:test` 성공
- [ ] `./gradlew :feature:feature-league:test` 성공

---

## Step 6 — Compose UI 테스트

### 목표
> Compose UI Test로 각 화면의 렌더링 상태와 사용자 인터랙션을 검증합니다.

### 작업 내용

#### 6.1 의존성 확인

**파일:** `gradle/libs.versions.toml`

Stage 1에서 이미 추가된 Compose UI Test 의존성을 확인합니다:

```toml
[libraries]
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
```

> ⚠️ **주의:** Compose UI 테스트는 `androidTest` 소스셋에 작성합니다 (단위 테스트가 아닌 Instrumented 테스트). 에뮬레이터 또는 실제 디바이스가 필요합니다.

#### 6.2 FixtureScreen UI 테스트

**파일:** `feature/feature-fixture/src/androidTest/kotlin/com/chase1st/feetballfootball/feature/fixture/FixtureScreenTest.kt`

**이유:** FixtureScreen은 Loading/Empty/Success/Error 4가지 상태를 가집니다. 각 상태에서 올바른 UI가 렌더링되는지, 클릭 이벤트가 올바르게 전달되는지 검증합니다.

**구조:** `createComposeRule()`로 Compose 테스트 환경을 생성하고, `setContent`로 특정 UiState를 가진 Composable을 렌더링합니다. ViewModel을 사용하지 않고 "Stateless Composable"을 직접 테스트합니다.

```kotlin
package com.chase1st.feetballfootball.feature.fixture

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.chase1st.feetballfootball.core.model.testing.TestFixtures
import org.junit.Rule
import org.junit.Test

class FixtureScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loading_상태에서_프로그레스바_표시() {
        composeTestRule.setContent {
            FixtureContent(uiState = FixtureUiState.Loading)
        }

        composeTestRule
            .onNodeWithTag("loading_indicator")
            .assertIsDisplayed()
    }

    @Test
    fun empty_상태에서_빈_화면_메시지_표시() {
        composeTestRule.setContent {
            FixtureContent(uiState = FixtureUiState.Empty)
        }

        composeTestRule
            .onNodeWithText("경기가 없습니다")
            .assertIsDisplayed()
    }

    @Test
    fun success_상태에서_경기_목록_표시() {
        val fixtures = mapOf(
            SupportedLeagues.EPL to listOf(
                TestFixtures.fixture(homeTeam = "Arsenal", awayTeam = "Chelsea"),
            )
        )

        composeTestRule.setContent {
            FixtureContent(
                uiState = FixtureUiState.Success(fixtures),
                onFixtureClick = {},
            )
        }

        composeTestRule
            .onNodeWithText("Arsenal")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Chelsea")
            .assertIsDisplayed()
    }

    @Test
    fun 경기_클릭_시_콜백_호출() {
        var clickedId: Int? = null
        val fixtures = mapOf(
            SupportedLeagues.EPL to listOf(
                TestFixtures.fixture(id = 42, homeTeam = "Arsenal", awayTeam = "Chelsea"),
            )
        )

        composeTestRule.setContent {
            FixtureContent(
                uiState = FixtureUiState.Success(fixtures),
                onFixtureClick = { clickedId = it },
            )
        }

        composeTestRule
            .onNodeWithText("Arsenal")
            .performClick()

        assertEquals(42, clickedId)
    }

    @Test
    fun error_상태에서_재시도_버튼_표시() {
        composeTestRule.setContent {
            FixtureContent(
                uiState = FixtureUiState.Error("네트워크 에러"),
                onRetry = {},
            )
        }

        composeTestRule
            .onNodeWithText("다시 시도")
            .assertIsDisplayed()
    }
}
```

> 💡 **Tip:** Compose UI 테스트에서는 ViewModel이 아닌 "Stateless Content Composable"을 직접 테스트합니다. `FixtureScreen`이 아닌 `FixtureContent`를 테스트 대상으로 하여 UiState를 자유롭게 주입할 수 있습니다. 이를 위해 Screen Composable을 `Screen(viewModel)` + `Content(uiState)` 형태로 분리하는 것이 좋습니다.

#### 6.3 StandingScreen UI 테스트

**파일:** `feature/feature-league/src/androidTest/kotlin/com/chase1st/feetballfootball/feature/league/StandingScreenTest.kt`

```kotlin
@Test
fun 순위_테이블_팀명_표시() {
    val standings = listOf(
        TestFixtures.teamStanding(rank = 1, teamName = "Arsenal", points = 65),
        TestFixtures.teamStanding(rank = 2, teamName = "Liverpool", points = 60),
    )

    composeTestRule.setContent {
        ClubStandingTab(standings = standings)
    }

    composeTestRule.onNodeWithText("Arsenal").assertIsDisplayed()
    composeTestRule.onNodeWithText("Liverpool").assertIsDisplayed()
    composeTestRule.onNodeWithText("65").assertIsDisplayed()
}
```

> ⚠️ **주의:** `onNodeWithTag`로 노드를 찾으려면 Composable에 `Modifier.testTag("loading_indicator")`를 추가해야 합니다. 프로덕션 코드에 테스트 태그를 추가하는 것은 표준적인 관행입니다. 릴리즈 빌드에서는 R8이 제거합니다.

### ✅ 검증
- [ ] `FixtureScreenTest` — Loading/Empty/Success/Error/클릭 콜백 테스트 통과
- [ ] `StandingScreenTest` — 순위 테이블 렌더링 테스트 통과
- [ ] `./gradlew :feature:feature-fixture:connectedAndroidTest` 성공
- [ ] `./gradlew :feature:feature-league:connectedAndroidTest` 성공

---

## Step 7 — 테스트 실행 및 CI 설정

### 목표
> 전체 테스트를 실행하고, 커버리지 목표를 확인합니다.

### 작업 내용

#### 7.1 테스트 실행 명령어

```bash
# 전체 단위 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :core:core-domain:test
./gradlew :core:core-data:test
./gradlew :feature:feature-fixture:test
./gradlew :feature:feature-league:test

# Compose UI 테스트 (에뮬레이터/디바이스 필요)
./gradlew connectedAndroidTest

# 특정 모듈 UI 테스트
./gradlew :feature:feature-fixture:connectedAndroidTest

# 테스트 커버리지 리포트 (Jacoco)
./gradlew jacocoTestReport
```

> 💡 **Tip:** CI 파이프라인에서는 단위 테스트(`./gradlew test`)를 먼저 실행하고, 통과하면 UI 테스트(`./gradlew connectedAndroidTest`)를 실행합니다. UI 테스트는 에뮬레이터 부팅 시간이 추가되므로, 별도 Job으로 분리하는 것이 좋습니다.

#### 7.2 테스트 커버리지 목표

| 모듈 | 목표 커버리지 | 우선순위 |
|------|-------------|---------|
| core-domain (UseCase) | 90%+ | 최우선 |
| core-data (Repository, Mapper) | 80%+ | 높음 |
| feature-* (ViewModel) | 80%+ | 높음 |
| feature-* (Compose UI) | 60%+ | 중간 |
| core-network (DTO) | N/A (데이터 클래스) | 낮음 |

> 💡 **Tip:** 커버리지 수치 자체보다 "의미 있는 시나리오 커버리지"가 중요합니다. 90% 라인 커버리지보다 핵심 비즈니스 로직의 성공/실패/엣지 케이스를 모두 다루는 것이 더 가치 있습니다.

### ✅ 검증
- [ ] `./gradlew test` — 전체 단위 테스트 성공
- [ ] `./gradlew connectedAndroidTest` — 전체 UI 테스트 성공 (에뮬레이터 필요)
- [ ] 커버리지 리포트에서 각 모듈의 목표 커버리지 달성 확인

---

## 완료

### 최종 검증 체크리스트

- [ ] TestDispatcherExtension 동작 확인
- [ ] FakeFootballApiService 동작 확인
- [ ] FakeFixtureRepository, FakeLeagueRepository 동작 확인
- [ ] TestFixtures 팩토리 동작 확인
- [ ] UseCase 테스트 전체 통과 (GetFixturesByDate, GetLeagueStandings, GetTopScorers, GetTopAssists, GetFixtureDetail)
- [ ] Repository 통합 테스트 전체 통과
- [ ] Mapper 단위 테스트 전체 통과 (SofaScore EventStatus 매핑, 이벤트 타입, 그리드 파싱, 통계 파싱)
- [ ] ViewModel 테스트 전체 통과 (FixtureViewModel, StandingViewModel, FixtureDetailViewModel)
- [ ] Compose UI 테스트 전체 통과
- [ ] `./gradlew test` 전체 성공

### 커밋

```bash
git commit -m "test: 테스트 인프라 구축 + UseCase/Repository/ViewModel/UI 테스트"
```

### 다음 단계

Stage 3 / Optimization으로 진행하여 R8 최적화, Baseline Profiles, 메모리/네트워크 최적화를 수행합니다.


---

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


---

# Stage 3 / KMP — Kotlin Multiplatform 전환
> ⏱ 예상 소요 시간: 20시간 | 난이도: ★★★★★ | 선행 조건: Stage 3 Optimization 완료

---

## 이 Codelab에서 배우는 것

- **Kotlin Multiplatform** 프로젝트 구조 설계 (shared / androidApp / iosApp)
- `java.time` → **`kotlinx-datetime`** 전환 및 0.7.x **Breaking Change** 대응
- **Retrofit → Ktor** HTTP 클라이언트 전환 (commonMain + 플랫폼별 엔진)
- **Hilt → Koin** DI 프레임워크 전환 (KMP 호환)
- **`expect`/`actual`** 패턴으로 플랫폼별 구현 분리 (Dispatcher, HttpClient)
- **Room KMP** 제한사항 및 iOS 지원 시 주의점
- iOS 앱 기반 설정 (**Xcode 프로젝트 + Swift에서 shared 모듈 사용**)
- **Compose Multiplatform** 도입 판단 기준

---

## 완성 후 결과물

| 항목 | 설명 |
|------|------|
| `shared` 모듈 | KMP 공유 모듈 (commonMain + androidMain + iosMain) |
| core-model | `kotlinx-datetime` 적용, commonMain으로 이동 |
| core-domain | UseCase + Repository 인터페이스, Hilt 어노테이션 제거 |
| core-common | `Result` sealed interface + `expect/actual` Dispatcher |
| core-network | Ktor 기반 `SofaScoreApiService` (Retrofit + API-Sports 대체) |
| core-data | Repository 구현체 + Mapper, `kotlinx-datetime` 적용 |
| DI | Koin 모듈 (sharedModule + androidModule) |
| iosApp | Xcode 프로젝트 기반, SwiftUI + shared framework 연동 |

---

## ⚠️ 전환 전 고려사항

이 단계를 진행하기 전에 아래 질문에 대한 답변을 확인하세요:

| 질문 | 답변 기준 |
|------|----------|
| iOS 앱 개발 계획이 있는가? | Yes → KMP 진행 / No → 보류 |
| 팀에 iOS 개발자가 있는가? | UI 레이어는 SwiftUI 필요 |
| 공유할 비즈니스 로직이 충분한가? | core-model, core-domain, core-data → 충분 |
| 일정 여유가 있는가? | KMP 전환은 2~4주 소요 예상 |

> **결론:** iOS 앱 개발이 확정된 경우에만 진행합니다. 그렇지 않으면 이 단계는 보류합니다.

---

## ⚠️ 주요 Breaking Changes 요약

이 전환에서 가장 주의해야 할 Breaking Change 3가지입니다:

### 1. kotlinx-datetime 0.7.x — Clock/Instant 패키지 변경

```
// 변경 전 (0.6.x 이하)
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// 변경 후 (0.7.0+)
import kotlin.time.Clock          // ← kotlin.time 패키지로 이동됨
import kotlin.time.Instant        // ← kotlin.time 패키지로 이동됨
```

> ⚠️ **주의:** `Clock.System.now()`의 반환 타입이 `kotlinx.datetime.Instant`에서 `kotlin.time.Instant`로 변경되었습니다. `todayIn()` 등의 확장 함수도 영향을 받습니다.

### 2. Hilt → Koin — DI 프레임워크 전환

```
// 변경 전 (Hilt)
class GetFixturesByDateUseCase @Inject constructor(
    private val repository: FixtureRepository,
)

// 변경 후 (Koin)
class GetFixturesByDateUseCase(
    private val repository: FixtureRepository,
)
```

> ⚠️ **주의:** `@Inject`, `@HiltViewModel`, `@Module`, `@InstallIn` 등 모든 Hilt 어노테이션을 제거해야 합니다. ViewModel에서도 `@HiltViewModel` 대신 Koin의 `viewModelOf()`를 사용합니다.

### 3. Retrofit → Ktor — HTTP 클라이언트 전환 (SofaScore API)

```
// 변경 전 (Retrofit + API-Sports)
@GET("fixtures")
suspend fun getFixtures(@Query("date") date: String): ApiResponse<List<FixtureResponseDto>>

// 변경 후 (Ktor + SofaScore)
suspend fun getEventsByDate(date: String): EventsResponseDto {
    return client.get("$baseUrl/sport/football/$date/events/0").body()
}
```

> ⚠️ **주의:** Retrofit의 선언적 인터페이스 방식에서 Ktor의 명시적 함수 호출 방식으로 변경됩니다. 또한 API-Sports에서 SofaScore API로 전환되므로 엔드포인트와 응답 형식이 모두 달라집니다. SofaScore는 Bearer Token 인증을 사용하며, GET 엔드포인트는 인증 없이도 동작합니다.

---

## ⚠️ Room KMP 핵심 제한사항

KMP에서 Room을 사용할 때 다음 제한사항에 유의해야 합니다:

1. **iOS에서 모든 DAO 함수는 반드시 `suspend`여야 함** — `androidMain`에서 구현된 DAO만 예외
2. **`setQueryExecutor()` API는 `commonMain`에서 사용 불가** → `CoroutineContext`로 대체
3. **iOS에서 `NativeSQLiteDriver` 사용** + 링커 옵션 `-lsqlite3` 필요
4. **Database Inspector는 Android 전용** — iOS에서는 사용 불가
5. **`ALTER TABLE` 기본값 처리가 플랫폼별로 다를 수 있음** — 마이그레이션 테스트 주의

---

## Step 1 — KMP 프로젝트 구조 설계

### 목표
> 어떤 모듈을 commonMain으로 이동하고, 어떤 모듈을 플랫폼별로 유지할지 결정합니다.

### 작업 내용

#### 1.1 전환 대상 모듈

```
전환 대상 (commonMain):
├── core-model      ← data class, enum (순수 Kotlin)
├── core-domain     ← UseCase, Repository 인터페이스 (순수 Kotlin)
├── core-common     ← Result, Dispatcher 정의
└── core-data       ← Repository 구현체, Mapper (네트워크/DB 의존)

플랫폼별 유지:
├── core-network    ← Retrofit(Android) / Ktor(공통) 분기
├── core-database   ← Room(Android) / Room KMP(공통) 분기
├── core-designsystem ← Compose Multiplatform 또는 플랫폼별
├── feature-*       ← Compose Multiplatform 또는 플랫폼별
└── app             ← Android 전용 (MainActivity)
```

> 💡 **Tip:** 전환 우선순위는 "의존성이 가장 적은 모듈"부터 시작합니다. `core-model`(순수 data class) → `core-common`(Result) → `core-domain`(인터페이스) → `core-data`(구현체) → `core-network`(Ktor) 순서입니다.

#### 1.2 최종 모듈 구조

```
feetball-football/
├── shared/                           ← KMP 공유 모듈 (신규)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── model/                ← core-model 이동
│       │   ├── domain/               ← core-domain 이동
│       │   ├── data/                 ← core-data 이동
│       │   ├── common/               ← core-common 이동
│       │   └── network/              ← Ktor 기반 API
│       ├── androidMain/kotlin/       ← Android 특화 구현
│       └── iosMain/kotlin/           ← iOS 특화 구현
├── androidApp/                       ← 기존 app 모듈 이름 변경
│   ├── feature-fixture/
│   ├── feature-fixture-detail/
│   ├── feature-league/
│   └── feature-news/
└── iosApp/                           ← Xcode 프로젝트 (신규)
    └── iosApp/
        ├── ContentView.swift
        └── ...
```

> ⚠️ **주의:** 기존 `app` 모듈을 `androidApp`으로 이름을 변경합니다. `settings.gradle.kts`에서 모듈 경로를 업데이트하고, 모든 의존성 참조도 함께 변경해야 합니다.

### ✅ 검증
- [ ] 전환 대상 모듈 목록이 확정됨
- [ ] 최종 모듈 구조가 팀 내 합의됨
- [ ] `shared/` 디렉토리 구조가 생성됨

---

## Step 2 — Gradle 설정 전환

### 목표
> `shared` 모듈의 Gradle 설정을 완성하고, KMP 의존성을 `libs.versions.toml`에 추가합니다.

### 작업 내용

#### 2.1 libs.versions.toml에 KMP 의존성 추가

**파일:** `gradle/libs.versions.toml`

**이유:** KMP 전환에 필요한 새 라이브러리(Ktor, Koin, kotlinx-datetime)를 선언합니다. 기존 Retrofit/Hilt는 Android 앱에서 점진적으로 제거합니다.

```toml
[versions]
kotlin = "2.3.20"          # Stage 1에서 이미 사용 중. KMP targets 그대로 유지
ktor = "3.4.2"             # 3.4.0 (2026-01)의 후속 패치. 3.4.x 시리즈 안정성 개선 포함
koin = "4.1.1"             # Koin BOM — KMP에서 DI 구성할 때 BOM 사용을 권장
kotlinx-datetime = "0.7.1" # 0.7.0에서 Instant/Clock이 kotlinx.datetime → kotlin.time으로 마이그레이션 — Step 8 참조
kotlinx-coroutines = "1.10.2"
room = "2.8.4"             # 2.7.0부터 KMP 지원. Room 3.0-alpha01(2026-03, androidx.room3:room3-*)에서 Kotlin-only 생성/coroutine-first API 예정

[libraries]
# Ktor (Retrofit 대체)
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { group = "io.ktor", name = "ktor-client-darwin", version.ref = "ktor" }

# Koin (Hilt 대체 — KMP 호환)
koin-core = { group = "io.insert-koin", name = "koin-core", version.ref = "koin" }
koin-android = { group = "io.insert-koin", name = "koin-android", version.ref = "koin" }
koin-compose = { group = "io.insert-koin", name = "koin-compose", version.ref = "koin" }

# kotlinx-datetime (java.time 대체)
# ⚠️ 0.7.0 Breaking Change: Instant/Clock이 kotlin.time 패키지로 이동됨
kotlinx-datetime = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version.ref = "kotlinx-datetime" }

# Room KMP
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
```

> ⚠️ **주의:** `kotlinx-datetime = "0.7.1"`을 사용합니다. 0.7.0에서 `Clock`과 `Instant`가 `kotlinx.datetime` 패키지에서 `kotlin.time` 패키지로 이동되었습니다. Step 8에서 이 변경사항을 자세히 다룹니다.

#### 2.2 shared 모듈 build.gradle.kts

**파일:** `shared/build.gradle.kts`

**이유:** KMP shared 모듈의 핵심 설정입니다. `kotlin("multiplatform")` 플러그인으로 Android, iOS 타겟을 동시에 빌드할 수 있습니다.

```kotlin
// shared/build.gradle.kts
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)

            // Koin
            implementation(libs.koin.core)

            // DateTime
            implementation(libs.kotlinx.datetime)

            // Room KMP
            implementation(libs.room.runtime)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.chase1st.feetballfootball.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
}
```

> 💡 **Tip:** `isStatic = true`로 설정하면 iOS framework가 정적 링크됩니다. 동적 프레임워크보다 앱 시작 속도가 빠르고, Swift Package Manager와의 호환성도 좋습니다.

**파일:** `settings.gradle.kts`에 `include(":shared")` 추가를 잊지 마세요.

### ✅ 검증
- [ ] `libs.versions.toml`에 Ktor, Koin, kotlinx-datetime 의존성이 추가됨
- [ ] `shared/build.gradle.kts`가 올바르게 작성됨
- [ ] `settings.gradle.kts`에 `:shared` 모듈이 포함됨
- [ ] `./gradlew :shared:build` 성공 (빈 모듈 상태)

---

## Step 3 — core-model → commonMain 이동

### 목표
> 순수 Kotlin 데이터 클래스와 enum을 commonMain으로 이동합니다. 유일한 변경점은 `java.time` → `kotlinx-datetime` 전환입니다.

### 작업 내용

#### 3.1 java.time → kotlinx-datetime 변환

**이유:** `java.time.LocalDate`와 `java.time.LocalTime`은 JVM 전용입니다. iOS에서 사용할 수 없으므로, 멀티플랫폼 호환 라이브러리인 `kotlinx-datetime`으로 변환합니다. API가 거의 동일하므로 import문만 변경하면 됩니다.

```kotlin
// 변경 전 (Android 전용)
import java.time.LocalDate
import java.time.LocalTime

data class Fixture(
    val id: Int,
    val date: LocalDate,
    val time: LocalTime,
    // ...
)

// 변경 후 (KMP commonMain)
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class Fixture(
    val id: Int,
    val date: LocalDate,
    val time: LocalTime,
    // ...
)
```

> 💡 **Tip:** IDE의 "Find and Replace" 기능으로 `import java.time.LocalDate` → `import kotlinx.datetime.LocalDate`를 일괄 변경할 수 있습니다. 하지만 `java.time.format.DateTimeFormatter`처럼 kotlinx-datetime에 대응물이 없는 클래스는 별도 처리가 필요합니다.

#### 3.2 이동 대상 파일

모든 파일을 `shared/src/commonMain/kotlin/com/chase1st/feetballfootball/model/` 경로로 이동합니다:

```
shared/src/commonMain/kotlin/com/chase1st/feetballfootball/model/
├── Fixture.kt           ← LocalDate, LocalTime → kotlinx.datetime
├── MatchStatus.kt       ← 변경 없음 (순수 enum)
├── MatchDetail.kt       ← 변경 없음
├── MatchEvent.kt        ← 변경 없음
├── EventType.kt         ← 변경 없음
├── MatchLineups.kt      ← 변경 없음
├── MatchStatistics.kt   ← 변경 없음
├── Team.kt              ← 변경 없음
├── TeamStanding.kt      ← 변경 없음
├── PlayerStanding.kt    ← 변경 없음
├── Player.kt            ← 변경 없음
├── LeagueInfo.kt        ← 변경 없음
└── GoalScorer.kt        ← 변경 없음
```

> 💡 **Tip:** 13개 파일 중 `Fixture.kt`만 import문 변경이 필요합니다. 나머지는 순수 Kotlin이므로 경로 이동만으로 충분합니다.

### ✅ 검증
- [ ] 모든 model 클래스가 `shared/src/commonMain/` 아래로 이동됨
- [ ] `Fixture.kt`의 import가 `kotlinx.datetime.LocalDate/LocalTime`으로 변경됨
- [ ] 나머지 model 클래스는 변경 없이 컴파일 성공
- [ ] `./gradlew :shared:build` 성공

---

## Step 4 — core-domain → commonMain 이동

### 목표
> UseCase와 Repository 인터페이스를 commonMain으로 이동합니다. Hilt의 `@Inject` 어노테이션을 제거합니다.

### 작업 내용

#### 이동 대상 파일

```
shared/src/commonMain/kotlin/com/chase1st/feetballfootball/domain/
├── repository/
│   ├── FixtureRepository.kt       ← 인터페이스 (변경 없음)
│   └── LeagueRepository.kt        ← 인터페이스 (변경 없음)
└── usecase/
    ├── GetFixturesByDateUseCase.kt ← LocalDate → kotlinx.datetime
    ├── GetFixtureDetailUseCase.kt  ← 변경 없음
    ├── GetLeagueStandingsUseCase.kt ← 변경 없음
    ├── GetTopScorersUseCase.kt     ← 변경 없음
    └── GetTopAssistsUseCase.kt     ← 변경 없음
```

#### 4.1 DI 어노테이션 제거 (Hilt → Koin)

**이유:** Hilt는 Android 전용 DI 프레임워크로, `@Inject` 어노테이션은 Kotlin/Native(iOS)에서 사용할 수 없습니다. Koin은 순수 Kotlin으로 구현된 DI 프레임워크로, constructor injection에 어노테이션이 필요 없습니다.

```kotlin
// 변경 전 (Hilt)
class GetFixturesByDateUseCase @Inject constructor(
    private val repository: FixtureRepository,
) {
    suspend operator fun invoke(date: LocalDate) = repository.getFixturesByDate(date)
}

// 변경 후 (Koin — constructor injection, 어노테이션 불필요)
class GetFixturesByDateUseCase(
    private val repository: FixtureRepository,
) {
    suspend operator fun invoke(date: LocalDate) = repository.getFixturesByDate(date)
}
```

> ⚠️ **주의:** 모든 UseCase에서 `@Inject constructor` → 일반 `constructor`로 변경합니다. `import javax.inject.Inject` import문도 제거합니다. 이 변경은 5개 UseCase 파일 모두에 적용해야 합니다.

> 💡 **Tip:** Repository 인터페이스(`FixtureRepository`, `LeagueRepository`)는 원래부터 어노테이션이 없으므로 변경 없이 그대로 이동합니다.

### ✅ 검증
- [ ] 모든 UseCase에서 `@Inject` 어노테이션이 제거됨
- [ ] Repository 인터페이스가 변경 없이 이동됨
- [ ] `GetFixturesByDateUseCase`의 LocalDate import가 `kotlinx.datetime`으로 변경됨
- [ ] `./gradlew :shared:build` 성공

---

## Step 5 — core-common → commonMain 이동

### 목표
> `Result` sealed interface를 이동하고, `Dispatcher`를 `expect/actual` 패턴으로 분리합니다.

### 작업 내용

#### 5.1 Result sealed interface

**파일:** `shared/src/commonMain/kotlin/com/chase1st/feetballfootball/common/Result.kt`

**이유:** `Result`는 순수 Kotlin 코드이므로 변경 없이 이동합니다.

```kotlin
// shared/src/commonMain/kotlin/.../common/Result.kt
// 변경 없음 — 순수 Kotlin
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable) : Result<Nothing>
}
```

#### 5.2 Dispatcher → expect/actual 패턴

**이유:** `Dispatchers.IO`는 JVM 전용입니다. iOS(Kotlin/Native)에는 IO Dispatcher가 없습니다. `expect/actual` 패턴으로 플랫폼별 Dispatcher를 제공합니다.

**파일:** `shared/src/commonMain/kotlin/com/chase1st/feetballfootball/common/Dispatcher.kt`

```kotlin
// shared/src/commonMain/kotlin/.../common/Dispatcher.kt
import kotlinx.coroutines.CoroutineDispatcher

expect val ioDispatcher: CoroutineDispatcher
expect val defaultDispatcher: CoroutineDispatcher
```

**파일:** `shared/src/androidMain/kotlin/com/chase1st/feetballfootball/common/Dispatcher.android.kt`

```kotlin
// shared/src/androidMain/kotlin/.../common/Dispatcher.android.kt
import kotlinx.coroutines.Dispatchers

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
actual val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
```

**파일:** `shared/src/iosMain/kotlin/com/chase1st/feetballfootball/common/Dispatcher.ios.kt`

```kotlin
// shared/src/iosMain/kotlin/.../common/Dispatcher.ios.kt
import kotlinx.coroutines.Dispatchers

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default  // iOS에는 IO 없음
actual val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
```

> ⚠️ **주의:** iOS의 `ioDispatcher`는 `Dispatchers.Default`로 매핑합니다. Kotlin/Native의 `Dispatchers.Default`는 백그라운드 스레드 풀을 사용하므로 네트워크 I/O에 사용해도 문제없습니다.

> 💡 **Tip:** `expect/actual`은 KMP의 핵심 패턴입니다. commonMain에서 `expect`로 선언하고, 각 플랫폼 소스셋(androidMain, iosMain)에서 `actual`로 구현합니다. 컴파일 시점에 플랫폼별 구현이 연결됩니다.

### ✅ 검증
- [ ] `Result` sealed interface가 commonMain으로 이동됨
- [ ] `expect val ioDispatcher`가 commonMain에 선언됨
- [ ] `actual val ioDispatcher`가 androidMain과 iosMain에 각각 구현됨
- [ ] `./gradlew :shared:build` 성공

---

## Step 6 — core-network → Ktor + SofaScore API 전환

### 목표
> Retrofit 기반의 `FootballApiService`를 Ktor 기반의 `SofaScoreApiService`로 전환합니다. API-Sports에서 SofaScore API로 변경하고, HttpClient를 `expect/actual` 패턴으로 플랫폼별 설정합니다.

### 작업 내용

#### 6.1 SofaScoreApiService — Retrofit + API-Sports → Ktor + SofaScore

**파일:** `shared/src/commonMain/kotlin/com/chase1st/feetballfootball/network/SofaScoreApiService.kt`

**이유:** Retrofit은 JVM 전용입니다. Ktor는 KMP를 지원하는 HTTP 클라이언트로, Android(OkHttp 엔진)와 iOS(Darwin 엔진)에서 동일한 API를 사용할 수 있습니다. 동시에 API-Sports v3에서 SofaScore API로 전환합니다. SofaScore는 Bearer Token 인증을 사용하며, GET 엔드포인트는 인증 없이도 동작합니다.

**변경 포인트:**
- Retrofit의 `@GET`/`@Query` 어노테이션 → Ktor의 `client.get()` + `parameter()`
- Retrofit 인터페이스 → 일반 Kotlin 클래스
- Base URL: `https://v3.football.api-sports.io` → `https://api.sofascore.com/api/v1`
- 인증: `x-apisports-key` 헤더 제거 (SofaScore GET 엔드포인트는 인증 불필요)
- 엔드포인트: `/fixtures` → `/sport/football/{date}/events/{page}`, `/standings` → `/unique-tournament/{id}/season/{seasonId}/standings/total` 등

```kotlin
// shared/src/commonMain/kotlin/.../network/SofaScoreApiService.kt
package com.chase1st.feetballfootball.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import com.chase1st.feetballfootball.network.dto.*

class SofaScoreApiService(
    private val client: HttpClient,
) {
    private val baseUrl = "https://api.sofascore.com/api/v1"

    suspend fun getEventsByDate(date: String, page: Int = 0): EventsResponseDto {
        return client.get("$baseUrl/sport/football/$date/events/$page").body()
    }

    suspend fun getStandings(tournamentId: Int, seasonId: Int): StandingResponseDto {
        return client.get("$baseUrl/unique-tournament/$tournamentId/season/$seasonId/standings/total").body()
    }

    suspend fun getSeasons(tournamentId: Int): SeasonsResponseDto {
        return client.get("$baseUrl/unique-tournament/$tournamentId/seasons").body()
    }

    suspend fun getTopPlayers(tournamentId: Int, seasonId: Int, type: String): TopPlayersResponseDto {
        return client.get("$baseUrl/unique-tournament/$tournamentId/season/$seasonId/top-players/$type").body()
    }

    suspend fun getEventDetail(eventId: Int): EventDetailResponseDto {
        return client.get("$baseUrl/event/$eventId").body()
    }

    suspend fun getEventIncidents(eventId: Int): EventIncidentsResponseDto {
        return client.get("$baseUrl/event/$eventId/incidents").body()
    }

    suspend fun getEventLineups(eventId: Int): EventLineupsResponseDto {
        return client.get("$baseUrl/event/$eventId/lineups").body()
    }

    suspend fun getEventStatistics(eventId: Int): EventStatisticsResponseDto {
        return client.get("$baseUrl/event/$eventId/statistics").body()
    }
}
```

> 💡 **Tip:** SofaScore API의 GET 엔드포인트는 인증 없이도 동작합니다. Bearer Token이 필요한 경우 `HttpClient`의 `defaultRequest {}` 블록에서 `header("Authorization", "Bearer $token")`으로 설정할 수 있습니다.

#### 6.2 HttpClient 플랫폼별 설정 — expect/actual

**파일:** `shared/src/commonMain/kotlin/com/chase1st/feetballfootball/network/HttpClientFactory.kt`

**이유:** Ktor의 HttpClient는 플랫폼별 "엔진"이 필요합니다. Android는 OkHttp 엔진, iOS는 Darwin(URLSession) 엔진을 사용합니다. `expect/actual`로 엔진 생성을 분리하고, ContentNegotiation(JSON 파싱) 설정은 공통으로 적용합니다.

```kotlin
// shared/src/commonMain/kotlin/.../network/HttpClientFactory.kt
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

expect fun createPlatformHttpClient(): HttpClient

fun createHttpClient(): HttpClient {
    return createPlatformHttpClient().config {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(DefaultRequest) {
            // SofaScore GET 엔드포인트는 인증 불필요
            // Bearer Token이 필요한 경우 아래 주석 해제
            // header("Authorization", "Bearer $apiToken")
        }
    }
}
```

```kotlin
// shared/src/commonMain/kotlin/.../network/AuthInterceptor.kt
// Bearer Token 인증이 필요한 경우 사용 (선택사항)
// SofaScore GET 엔드포인트는 인증 없이 동작
```

**파일:** `shared/src/androidMain/kotlin/com/chase1st/feetballfootball/network/HttpClientFactory.android.kt`

```kotlin
// shared/src/androidMain/kotlin/.../network/HttpClientFactory.android.kt
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*

actual fun createPlatformHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }
}
```

**파일:** `shared/src/iosMain/kotlin/com/chase1st/feetballfootball/network/HttpClientFactory.ios.kt`

```kotlin
// shared/src/iosMain/kotlin/.../network/HttpClientFactory.ios.kt
import io.ktor.client.*
import io.ktor.client.engine.darwin.*

actual fun createPlatformHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        engine {
            configureRequest {
                setTimeoutInterval(15.0)
            }
        }
    }
}
```

> ⚠️ **주의:** Android의 `OkHttp` 엔진은 `ktor-client-okhttp` 의존성이 필요하고, iOS의 `Darwin` 엔진은 `ktor-client-darwin` 의존성이 필요합니다. 각각 `androidMain`과 `iosMain`의 dependencies에만 추가합니다 (Step 2의 `build.gradle.kts` 참조).

### ✅ 검증
- [ ] `SofaScoreApiService`가 Ktor 기반으로 변환됨
- [ ] 8개 API 엔드포인트(getEventsByDate, getStandings, getSeasons, getTopPlayers, getEventDetail, getEventIncidents, getEventLineups, getEventStatistics)가 모두 구현됨
- [ ] `createPlatformHttpClient()`의 `expect`/`actual`이 Android/iOS 모두 구현됨
- [ ] `./gradlew :shared:build` 성공

---

## Step 7 — DI 전환: Hilt → Koin

### 목표
> Hilt DI 모듈을 Koin 모듈로 전환합니다. shared 모듈의 공통 DI와 Android 앱의 ViewModel DI를 각각 설정합니다.

### 작업 내용

#### 7.1 Koin 모듈 정의 (shared)

**파일:** `shared/src/commonMain/kotlin/com/chase1st/feetballfootball/di/SharedModule.kt`

**이유:** Koin의 `module { }` DSL로 의존성 그래프를 선언합니다. `single`은 싱글톤, `factory`는 매번 새 인스턴스를 생성합니다. UseCase는 상태가 없으므로 `factory`로 선언합니다.

```kotlin
// shared/src/commonMain/kotlin/.../di/SharedModule.kt
package com.chase1st.feetballfootball.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val sharedModule = module {
    // Network
    single { createHttpClient() }
    single { SofaScoreApiService(get()) }  // SofaScore GET 엔드포인트는 인증 불필요

    // Repository
    singleOf(::FixtureRepositoryImpl) bind FixtureRepository::class
    singleOf(::LeagueRepositoryImpl) bind LeagueRepository::class

    // UseCase
    factoryOf(::GetFixturesByDateUseCase)
    factoryOf(::GetFixtureDetailUseCase)
    factoryOf(::GetLeagueStandingsUseCase)
    factoryOf(::GetTopScorersUseCase)
    factoryOf(::GetTopAssistsUseCase)
}
```

> 💡 **Tip:** Koin의 `singleOf(::FixtureRepositoryImpl) bind FixtureRepository::class`는 "FixtureRepositoryImpl을 싱글톤으로 생성하되, FixtureRepository 인터페이스로 요청하면 이 인스턴스를 반환하라"는 의미입니다. SofaScore GET 엔드포인트는 인증 불필요하므로 `SofaScoreApiService`에는 `HttpClient`만 주입합니다.

#### 7.2 Android 앱에서 Koin 초기화

**파일:** `androidApp/src/main/kotlin/com/chase1st/feetballfootball/FeetballApp.kt`

**이유:** 기존 `@HiltAndroidApp` 어노테이션을 제거하고, `startKoin {}`으로 Koin을 수동 초기화합니다. API 키를 `properties`로 전달합니다.

```kotlin
// androidApp/src/main/kotlin/.../FeetballApp.kt
class FeetballApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FeetballApp)
            modules(sharedModule, androidModule)
        }
    }
}
```

> ⚠️ **주의:** `@HiltAndroidApp` 어노테이션을 제거하면, 기존 Hilt로 주입받던 모든 곳에서 에러가 발생합니다. `@AndroidEntryPoint`, `@HiltViewModel` 등도 모두 제거하고 Koin 방식으로 교체해야 합니다. 이 작업은 한 번에 진행하는 것을 권장합니다. SofaScore GET 엔드포인트는 인증 불필요하므로 `properties(mapOf("apiKey" to ...))` 설정도 제거합니다.

#### 7.3 ViewModel에서 Koin 사용

**파일:** Android feature 모듈의 ViewModel들

**이유:** `@HiltViewModel`과 `@Inject constructor`를 제거하고, 일반 constructor로 변경합니다. Koin의 `viewModelOf()`로 ViewModel을 등록합니다.

```kotlin
// androidApp/feature-fixture/.../FixtureViewModel.kt
class FixtureViewModel(
    private val getFixturesByDate: GetFixturesByDateUseCase,
) : ViewModel() {
    // 기존 로직 유지, @Inject 어노테이션만 제거
}

// Koin 모듈에 ViewModel 등록
val androidModule = module {
    viewModelOf(::FixtureViewModel)
    viewModelOf(::StandingViewModel)
    viewModelOf(::FixtureDetailViewModel)
    viewModelOf(::LeagueListViewModel)
}
```

> 💡 **Tip:** Compose에서 Koin ViewModel을 사용하려면 `koinViewModel<FixtureViewModel>()`을 호출합니다. Hilt의 `hiltViewModel<>()`과 사용법이 거의 동일합니다.

### ✅ 검증
- [ ] `sharedModule`에 Network, Repository, UseCase가 모두 등록됨
- [ ] `androidModule`에 모든 ViewModel이 등록됨
- [ ] `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@Inject` 어노테이션이 모두 제거됨
- [ ] FeetballApp의 `onCreate()`에서 `startKoin {}` 호출됨
- [ ] `./gradlew assembleDebug` 성공

---

## Step 8 — core-data → commonMain 이동

### 목표
> Repository 구현체와 Mapper를 commonMain으로 이동합니다. 날짜 처리 코드를 `kotlinx-datetime`으로 전환합니다.

### 작업 내용

#### 8.1 날짜 처리 변환

**이유:** `java.time.LocalDate.parse()`는 `DateTimeFormatter`를 인자로 받지만, `kotlinx.datetime.LocalDate.parse()`는 ISO 8601 형식(yyyy-MM-dd)을 기본 지원합니다. SofaScore의 날짜 파라미터가 `yyyy-MM-dd` 형식이므로 `kotlinx.datetime.LocalDate.parse()`로 직접 파싱할 수 있습니다.

```kotlin
// 변경 전
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)

// 변경 후
import kotlinx.datetime.LocalDate

val date = LocalDate.parse(dateString)  // kotlinx-datetime은 ISO 형식 기본 지원
```

#### 8.2 이동 대상

```
shared/src/commonMain/kotlin/.../data/
├── repository/
│   ├── FixtureRepositoryImpl.kt
│   └── LeagueRepositoryImpl.kt
├── mapper/
│   ├── FixtureMapper.kt            ← java.time → kotlinx.datetime
│   ├── StandingMapper.kt           ← 변경 없음
│   └── FixtureDetailMapper.kt      ← 변경 없음
└── util/
    └── SeasonUtil.kt               ← java.time → kotlinx.datetime
```

#### 8.3 SeasonUtil 변환 — kotlinx-datetime 0.7.x Breaking Change 대응

**파일:** `shared/src/commonMain/kotlin/com/chase1st/feetballfootball/data/util/SeasonUtil.kt`

**이유:** `SeasonUtil`은 현재 시즌을 계산하는 유틸리티입니다. `java.time.LocalDate.now()`를 `kotlinx-datetime`으로 변환해야 합니다.

> ⚠️ **핵심 Breaking Change:** kotlinx-datetime 0.7.0부터 `Clock`이 `kotlinx.datetime` 패키지에서 `kotlin.time` 패키지로 이동되었습니다. 0.6.x까지는 `import kotlinx.datetime.Clock`이었지만, 0.7.0+에서는 `import kotlin.time.Clock`을 사용해야 합니다.

```kotlin
// 변경 전
import java.time.LocalDate
import java.time.Month

object SeasonUtil {
    fun currentSeason(): Int {
        val now = LocalDate.now()
        return if (now.month >= Month.JULY) now.year else now.year - 1
    }
}

// 변경 후 (kotlinx-datetime 0.7.x)
// ⚠️ 0.7.0부터 Clock이 kotlin.time 패키지로 이동됨
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

object SeasonUtil {
    fun currentSeason(): Int {
        val now = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return if (now.monthNumber >= 7) now.year else now.year - 1
    }
}
```

> ⚠️ **주의:** 반드시 `import kotlin.time.Clock`을 사용합니다. `import kotlinx.datetime.Clock`은 0.7.0+에서 deprecated되었습니다. IDE의 자동 import가 잘못된 패키지를 제안할 수 있으니 주의하세요.

> 💡 **Tip:** `Month.JULY`(enum 비교) 대신 `monthNumber >= 7`(정수 비교)을 사용합니다. `kotlinx-datetime`의 `LocalDate.month`는 `kotlinx.datetime.Month` enum을 반환하지만, `monthNumber`가 더 간결합니다.

### ✅ 검증
- [ ] FixtureMapper에서 `java.time` import가 모두 `kotlinx.datetime`으로 변경됨
- [ ] SeasonUtil에서 `kotlin.time.Clock` (0.7.x)을 사용함 (`kotlinx.datetime.Clock`이 아님)
- [ ] `DateTimeFormatter` 사용이 제거됨 (kotlinx-datetime은 ISO 형식 기본 지원)
- [ ] Repository 구현체가 정상 컴파일됨
- [ ] `./gradlew :shared:build` 성공

---

## Step 9 — iOS 앱 기반 설정

### 목표
> Xcode 프로젝트를 생성하고, shared framework를 연동하여 iOS에서 API 호출이 동작하는지 확인합니다.

### 작업 내용

#### 9.1 Xcode 프로젝트 생성

```bash
# KMP Wizard 또는 수동 생성
# iosApp/ 디렉토리에 Xcode 프로젝트 위치
```

> 💡 **Tip:** JetBrains의 [KMP Wizard](https://kmp.jetbrains.com/)를 사용하면 Xcode 프로젝트 연동 설정이 자동으로 생성됩니다. 수동으로 설정하려면 Framework 검색 경로에 `shared` 모듈의 빌드 산출물 경로를 추가해야 합니다.

#### 9.2 Swift에서 shared 모듈 사용

**파일:** `iosApp/iosApp/ContentView.swift`

**이유:** iOS 앱의 진입점입니다. SwiftUI의 `TabView`로 Android 앱과 동일한 3탭 구조를 구현합니다. shared 모듈의 UseCase를 Swift에서 호출할 수 있습니다.

```swift
// iosApp/iosApp/ContentView.swift
import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var viewModel = FixtureListViewModel()

    var body: some View {
        TabView {
            FixtureListView(viewModel: viewModel)
                .tabItem {
                    Image(systemName: "sportscourt")
                    Text("경기")
                }

            LeagueListView()
                .tabItem {
                    Image(systemName: "trophy")
                    Text("리그")
                }

            NewsView()
                .tabItem {
                    Image(systemName: "newspaper")
                    Text("뉴스")
                }
        }
    }
}
```

#### 9.3 iOS ViewModel 래퍼

**파일:** `iosApp/iosApp/FixtureListViewModel.swift`

**이유:** Kotlin의 코루틴은 Swift에서 직접 사용할 수 없으므로, Swift의 `async/await`로 래핑합니다. Koin을 통해 UseCase를 가져옵니다.

```swift
// iosApp/iosApp/FixtureListViewModel.swift
import Foundation
import shared

@MainActor
class FixtureListViewModel: ObservableObject {
    @Published var state: FixtureUiState = .loading

    private let useCase: GetFixturesByDateUseCase

    init() {
        let koin = KoinHelper.shared
        self.useCase = koin.getFixturesByDateUseCase()
        loadFixtures()
    }

    func loadFixtures() {
        state = .loading
        Task {
            let today = DateUtil.today()
            let result = try await useCase.invoke(date: today)
            if let success = result as? ResultSuccess {
                state = .success(fixtures: success.data)
            } else {
                state = .error(message: "Failed to load fixtures")
            }
        }
    }
}
```

> ⚠️ **주의:** Kotlin의 `suspend fun`은 Swift에서 `async throws` 함수로 자동 변환됩니다. 하지만 Kotlin의 `sealed interface`(`Result`)는 Swift에서 타입 체크 방식이 다릅니다. `result as? ResultSuccess`와 같은 패턴 매칭을 사용합니다.

> 💡 **Tip:** Koin의 iOS 헬퍼 클래스(`KoinHelper`)를 shared 모듈에 작성하여 Swift에서 쉽게 의존성을 가져올 수 있도록 합니다. `KoinHelper`는 `shared/src/iosMain/kotlin/`에 위치시킵니다.

### ✅ 검증
- [ ] Xcode 프로젝트가 생성되고, shared framework가 링크됨
- [ ] iOS 앱이 시뮬레이터에서 빌드 성공
- [ ] iOS 앱에서 API 호출이 성공 (콘솔 로그 확인)
- [ ] 기본 화면 1개 이상 동작 확인

---

## Step 10 — Compose Multiplatform (선택사항)

### 목표
> UI 레이어까지 공유할지 결정하고, 필요 시 Compose Multiplatform을 도입합니다.

### 작업 내용

#### 10.1 의존성

**파일:** `gradle/libs.versions.toml`

```toml
[versions]
compose-multiplatform = "1.10.3"

[plugins]
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

#### 10.2 판단 기준

| 접근 방식 | 장점 | 단점 |
|----------|------|------|
| **공유 비즈니스 로직 + 네이티브 UI** | iOS는 SwiftUI로 네이티브 경험 제공 | UI 코드 2벌 유지 |
| **Compose Multiplatform (UI도 공유)** | 코드 최대 공유 | iOS UI가 네이티브 느낌 약함 |

> **권장:** 먼저 비즈니스 로직만 공유 (Step 1~9), 이후 Compose Multiplatform 평가

> 💡 **Tip:** Navigation 3 1.1.0-beta01 (2026-03-11)부터 KMP 타겟 (JVM, Native, Web)을 공식 지원합니다. Compose Multiplatform과 함께 사용하면 내비게이션 코드도 공유 가능합니다.

### ✅ 검증
- [ ] Compose Multiplatform 도입 여부가 결정됨
- [ ] (도입 시) shared 모듈에 Compose Multiplatform 플러그인이 적용됨
- [ ] (도입 시) iOS에서 Compose UI가 렌더링됨

---

## 완료

### 최종 검증 체크리스트

#### Phase A: shared 모듈 구축
- [ ] shared 모듈 생성 + Gradle 설정
- [ ] core-model → commonMain 이동 (kotlinx-datetime 적용)
- [ ] core-domain → commonMain 이동
- [ ] core-common → commonMain 이동 (expect/actual Dispatcher)
- [ ] core-network → Ktor + SofaScore API 전환 (commonMain + androidMain/iosMain)
- [ ] core-data → commonMain 이동
- [ ] DI: Hilt → Koin 전환
- [ ] `./gradlew :shared:build` 성공 (Android + iOS)

#### Phase B: Android 앱 연결
- [ ] androidApp이 shared 모듈 의존
- [ ] 기존 core-* 모듈 제거 (shared로 통합)
- [ ] Android 앱 전체 기능 정상 동작
- [ ] `./gradlew assembleDebug` 성공

#### Phase C: iOS 앱 기반
- [ ] Xcode 프로젝트 생성
- [ ] shared framework 링크
- [ ] iOS 앱에서 API 호출 성공
- [ ] 기본 화면 1개 이상 동작 확인

#### Breaking Change 대응 확인
- [ ] kotlinx-datetime 0.7.x — `kotlin.time.Clock` 사용 (`kotlinx.datetime.Clock` 아님)
- [ ] Hilt → Koin — 모든 `@Inject`, `@HiltViewModel`, `@Module`, `@InstallIn` 제거 확인
- [ ] Retrofit + API-Sports → Ktor + SofaScore — 8개 API 엔드포인트 모두 변환 확인
- [ ] `x-apisports-key` 헤더 제거, SofaScore Bearer Token 설정 확인 (GET은 인증 불필요)

#### 선택사항
- [ ] Compose Multiplatform 1.10.3 적용
- [ ] Navigation 3 KMP 지원 평가 (1.1.0-beta01+)

### 커밋

```bash
git commit -m "feat: KMP 전환 — shared 모듈 + Ktor + Koin + kotlinx-datetime"
```

### 다음 단계

Stage 3의 모든 섹션(Testing, Optimization, KMP)이 완료되었습니다. 앱의 현대화 작업이 마무리되었으며, 이후에는 기능 개발과 유지보수에 집중합니다.


---

