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
| 도구 버전 | Kotlin 2.3.10 / AGP 9.1.0 / Gradle 9.4.0 |
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
agp = "9.1.0"
kotlin = "2.3.10"
ksp = "2.3.5"                    # KSP 2.3.0부터 버전 체계 변경 (하이픈 형식 폐지)

# AndroidX Core
core-ktx = "1.18.0"
appcompat = "1.7.0"
activity-compose = "1.13.0"
lifecycle = "2.10.0"
navigation = "2.9.7"

# Compose
compose-bom = "2026.02.01"

# DI
hilt = "2.59.2"
hilt-navigation-compose = "1.3.0"

# Networking
retrofit = "3.0.0"
okhttp = "5.3.0"                 # 5.x 메이저 업: Android AAR 아티팩트 분리

# Serialization
kotlinx-serialization = "1.10.0"

# Database
room = "2.8.4"

# Image
coil = "3.4.0"

# Async
coroutines = "1.10.2"

# Testing
junit5 = "5.14.3"
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
junit5 = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit5" }
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
testing = ["junit5", "mockk", "turbine", "truth", "kotlinx-coroutines-test"]

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
    implementation(libs.hilt.navigation.compose)

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
> Gradle 8.13 → 9.4.0, Kotlin 2.1.0 → 2.3.10, AGP 8.13.2 → 9.1.0으로 업그레이드한다.

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
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.0-bin.zip
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

> 💡 **Tip:** Kotlin 2.3.10과 AGP 9.1.0 버전은 Step 1에서 만든 `libs.versions.toml`의 `[versions]` 섹션에 이미 선언되어 있습니다. Gradle wrapper만 별도로 properties 파일에서 관리됩니다.

### ✅ 검증
- [ ] `./gradlew --version` 실행 시 `Gradle 9.4.0` 출력 확인
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
    implementation(libs.hilt.navigation.compose)
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
    implementation(libs.hilt.navigation.compose)
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
    implementation(libs.hilt.navigation.compose)
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
    implementation(libs.hilt.navigation.compose)
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

> ⚠️ **주의:** 첫 빌드는 Gradle 9.4.0 다운로드, 의존성 다운로드 등으로 시간이 오래 걸릴 수 있습니다 (5-10분). 이후 빌드는 Gradle 캐시 덕분에 빨라집니다.

---

## Stage 1 완료!

### 최종 체크리스트

- [ ] 11개 모듈 (core 7 + feature 4) 빌드 성공
- [ ] Kotlin DSL + Version Catalog 기반
- [ ] Convention Plugins 적용
- [ ] Kotlin 2.3.10 / AGP 9.1.0 / Gradle 9.4.0
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
