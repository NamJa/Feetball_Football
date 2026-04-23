# Stage 1 — 프로젝트 셋업

> **목표:** 빈 앱이 빌드되고, DI 그래프가 동작하고, 네트워크 호출이 가능한 상태
> **산출물:** 11개 모듈 빌드 성공 + 빈 Compose 화면 + Hilt DI + Retrofit API 호출 가능
> **선행 조건:** 없음 (첫 Stage)
> **Git 브랜치:** `feature/renewal-stage-1`

---

## Step 1.1 — Version Catalog 생성

**파일:** `gradle/libs.versions.toml`

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

**검증:** 파일 문법 오류 없이 저장 완료

---

## Step 1.2 — Kotlin DSL 빌드 파일 전환

### settings.gradle.kts

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
// core 모듈 (Step 1.6에서 추가)
// feature 모듈 (Step 1.6에서 추가)
```

### build.gradle.kts (프로젝트 루트)

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

**작업:** 기존 `settings.gradle`, `build.gradle` 파일 삭제

---

## Step 1.3 — app/build.gradle.kts 전환

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
    // 모듈 의존성 (Step 1.6 이후 추가)

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

**작업:** 기존 `app/build.gradle` 삭제

---

## Step 1.4 — Gradle/Kotlin/AGP 업그레이드

### gradle/wrapper/gradle-wrapper.properties

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip
```

### gradle.properties 업데이트

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

**검증:** `./gradlew --version` → Gradle 9.4.1 확인

---

## Step 1.5 — Convention Plugins

### 디렉토리 구조

```
build-logic/
├── convention/
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       ├── AndroidApplicationConventionPlugin.kt
│       ├── AndroidLibraryConventionPlugin.kt
│       ├── AndroidComposeConventionPlugin.kt
│       ├── AndroidHiltConventionPlugin.kt
│       └── AndroidTestConventionPlugin.kt
└── settings.gradle.kts
```

### build-logic/settings.gradle.kts

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

### build-logic/convention/build.gradle.kts

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

### AndroidLibraryConventionPlugin.kt

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

### AndroidComposeConventionPlugin.kt

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

### AndroidHiltConventionPlugin.kt

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

### AndroidTestConventionPlugin.kt

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

> **유틸리티:** Convention Plugin 내에서 `libs`를 사용하려면 확장 프로퍼티가 필요합니다:

```kotlin
// build-logic/convention/src/main/kotlin/Extensions.kt
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
```

---

## Step 1.6 — 멀티 모듈 생성

### 모듈 목록 및 build.gradle.kts

#### core/core-common/build.gradle.kts

```kotlin
plugins {
    id("feetball.android.library")
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
```

#### core/core-model/build.gradle.kts

```kotlin
plugins {
    id("feetball.android.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
```

#### core/core-domain/build.gradle.kts

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

#### core/core-network/build.gradle.kts

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

#### core/core-database/build.gradle.kts

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

#### core/core-data/build.gradle.kts

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

#### core/core-designsystem/build.gradle.kts

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

#### feature/feature-fixture/build.gradle.kts (모든 feature 동일 패턴)

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

> feature-fixture-detail, feature-league, feature-news 모두 위와 동일한 패턴.

### settings.gradle.kts 업데이트

```kotlin
// core 모듈 추가
include(":core:core-common")
include(":core:core-model")
include(":core:core-domain")
include(":core:core-network")
include(":core:core-database")
include(":core:core-data")
include(":core:core-designsystem")

// feature 모듈 추가
include(":feature:feature-fixture")
include(":feature:feature-fixture-detail")
include(":feature:feature-league")
include(":feature:feature-news")
```

### app/build.gradle.kts 모듈 의존성 추가

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

    // ... 기존 의존성
}
```

### 각 모듈에 빈 소스 디렉토리 생성

```
core/core-common/src/main/kotlin/com/chase1st/feetballfootball/core/common/
core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/
core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/
core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/
core/core-database/src/main/kotlin/com/chase1st/feetballfootball/core/database/
core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/
core/core-designsystem/src/main/kotlin/com/chase1st/feetballfootball/core/designsystem/
feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/
feature/feature-fixture-detail/src/main/kotlin/com/chase1st/feetballfootball/feature/fixturedetail/
feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/
feature/feature-news/src/main/kotlin/com/chase1st/feetballfootball/feature/news/
```

각 모듈에 빈 `AndroidManifest.xml` 생성 (library 모듈용):

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

**★ 빌드 검증:** `./gradlew assembleDebug` — 11개 빈 모듈 포함 성공

---

## Step 1.7 — core-common 기반 코드

### Result.kt

```kotlin
// core/core-common/src/main/kotlin/.../core/common/result/Result.kt
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

### Dispatcher Qualifiers

```kotlin
// core/core-common/src/main/kotlin/.../core/common/dispatcher/Dispatchers.kt
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

### DispatcherModule

```kotlin
// core/core-common/src/main/kotlin/.../core/common/dispatcher/DispatcherModule.kt
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

---

## Step 1.8 — core-network 기반 코드

### FootballApiService.kt (비어있는 인터페이스, Slice에서 엔드포인트 추가)

```kotlin
// core/core-network/src/main/kotlin/.../core/network/api/FootballApiService.kt
package com.chase1st.feetballfootball.core.network.api

interface FootballApiService {
    // 엔드포인트는 각 Slice에서 추가
}
```

### NetworkModule.kt

```kotlin
// core/core-network/src/main/kotlin/.../core/network/di/NetworkModule.kt
package com.chase1st.feetballfootball.core.network.di

import com.chase1st.feetballfootball.core.network.api.FootballApiService
import com.chase1st.feetballfootball.core.network.interceptor.XMasInterceptor
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
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(XMasInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://www.fotmob.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideFootballApiService(retrofit: Retrofit): FootballApiService =
        retrofit.create(FootballApiService::class.java)
}
```

### XMasInterceptor.kt

```kotlin
// core/core-network/src/main/kotlin/.../core/network/interceptor/XMasInterceptor.kt
package com.chase1st.feetballfootball.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest
import android.util.Base64
import org.json.JSONObject

class XMasInterceptor : Interceptor {

    companion object {
        private const val PRODUCTION_HASH = "production:c3326dbe307f25cb698f764c46cb5a473dcd6773"
        // Three Lions 가사 (X-Mas 시그니처 생성용 시크릿)
        private const val SECRET = "..." // FotMob _app.js에서 추출한 시크릿 문자열
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url.encodedPath +
            if (original.url.encodedQuery != null) "?${original.url.encodedQuery}" else ""

        val xMas = generateXMas(url)

        val request = original.newBuilder()
            .header("x-mas", xMas)
            .build()

        return chain.proceed(request)
    }

    private fun generateXMas(url: String): String {
        val body = JSONObject().apply {
            put("url", url)
            put("code", System.currentTimeMillis())
            put("foo", PRODUCTION_HASH)
        }
        val signature = md5(body.toString() + SECRET).uppercase()
        val payload = JSONObject().apply {
            put("body", body)
            put("signature", signature)
        }
        return Base64.encodeToString(payload.toString().toByteArray(), Base64.NO_WRAP)
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
```

---

## Step 1.9 — Turnstile 인증 설계 (matchDetails용)

FotMob API는 API Key가 불필요하지만, `matchDetails` 엔드포인트만 Cloudflare Turnstile 인증이 필요합니다. 이 엔드포인트에 인증 없이 접근하면 403 응답이 반환됩니다. 나머지 엔드포인트(`/api/leagues`, `/api/matches` 등)는 X-Mas 헤더만으로 접근 가능합니다.

### TurnstileManager (Singleton)

Turnstile 토큰 획득과 쿠키 관리를 담당합니다. WebView를 사용하여 Cloudflare Turnstile 챌린지를 수행하고, 획득한 `cf_clearance` 쿠키를 저장합니다.

```kotlin
// core/core-network/src/main/kotlin/.../core/network/turnstile/TurnstileManager.kt
package com.chase1st.feetballfootball.core.network.turnstile

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TurnstileManager @Inject constructor() {

    private val _cfClearance = MutableStateFlow<String?>(null)
    val cfClearance: StateFlow<String?> = _cfClearance.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    fun updateClearance(cookie: String) {
        _cfClearance.value = cookie
    }

    fun startVerification() {
        _isVerifying.value = true
    }

    fun endVerification() {
        _isVerifying.value = false
    }

    fun isAuthenticated(): Boolean = _cfClearance.value != null
}
```

### TurnstileBridge (JavaScript Interface)

WebView에서 Turnstile 챌린지 완료 시 네이티브 코드로 콜백합니다.

```kotlin
// core/core-network/src/main/kotlin/.../core/network/turnstile/TurnstileBridge.kt
package com.chase1st.feetballfootball.core.network.turnstile

import android.webkit.JavascriptInterface

class TurnstileBridge(
    private val onTokenReceived: (String) -> Unit,
) {
    @JavascriptInterface
    fun onTurnstileSuccess(token: String) {
        onTokenReceived(token)
    }
}
```

### OkHttp Interceptor에서 Turnstile 쿠키 주입

`matchDetails` 요청 시 `cf_clearance` 쿠키를 자동으로 주입하는 Interceptor입니다.

```kotlin
// core/core-network/src/main/kotlin/.../core/network/interceptor/TurnstileCookieInterceptor.kt
package com.chase1st.feetballfootball.core.network.interceptor

import com.chase1st.feetballfootball.core.network.turnstile.TurnstileManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class TurnstileCookieInterceptor @Inject constructor(
    private val turnstileManager: TurnstileManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        // matchDetails 엔드포인트만 Turnstile 쿠키 필요
        if (path.contains("/api/matchDetails")) {
            val cookie = turnstileManager.cfClearance.value
            if (cookie != null) {
                val request = original.newBuilder()
                    .header("Cookie", "cf_clearance=$cookie")
                    .build()
                return chain.proceed(request)
            }
        }

        return chain.proceed(original)
    }
}
```

### NetworkModule에 TurnstileCookieInterceptor 등록

Step 1.8의 `NetworkModule.kt`에서 `provideOkHttpClient`를 다음과 같이 수정합니다:

```kotlin
@Provides
@Singleton
fun provideOkHttpClient(
    turnstileCookieInterceptor: TurnstileCookieInterceptor,
): OkHttpClient =
    OkHttpClient.Builder()
        .addInterceptor(XMasInterceptor())
        .addInterceptor(turnstileCookieInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
```

### 검증 대기 패턴 (matchDetail 화면 진입 시)

matchDetail 화면 진입 시, `TurnstileManager.isAuthenticated()`가 `false`이면 WebView 기반 Turnstile 챌린지를 먼저 수행합니다. 인증 완료 후 matchDetails API를 호출합니다. 이 흐름은 feature-fixture-detail 모듈에서 구현합니다.

---

## Step 1.10 — Hilt + MainActivity 기반

### FeetballApp.kt

```kotlin
// app/src/main/kotlin/.../FeetballApp.kt
package com.chase1st.feetballfootball

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FeetballApp : Application()
```

### AndroidManifest.xml 업데이트

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

### MainActivity.kt (새 버전)

```kotlin
// app/src/main/kotlin/.../MainActivity.kt
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

**★ 빌드 검증:** 빈 Compose 화면 + "Feetball Football v2.0" 텍스트 표시

---

## Step 1.11 — core-designsystem

### Color.kt

```kotlin
// core/core-designsystem/src/main/kotlin/.../core/designsystem/theme/Color.kt
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

### Type.kt

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

### Theme.kt

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

### 공통 컴포넌트 — TeamLogo.kt

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

### 공통 컴포넌트 — LoadingIndicator.kt

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

### 공통 컴포넌트 — ErrorContent.kt

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

---

## Step 1.12 — core-database 기반

### FeetballDatabase.kt (빈 상태)

```kotlin
// core/core-database/src/main/kotlin/.../core/database/FeetballDatabase.kt
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

### DatabaseModule.kt

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

---

## ★ Stage 1 완료 검증

```bash
./gradlew assembleDebug
```

**체크리스트:**

- [ ] 11개 모듈 (core 7 + feature 4) 빌드 성공
- [ ] Kotlin DSL + Version Catalog 기반
- [ ] Convention Plugins 적용
- [ ] Kotlin 2.3.20 / AGP 9.1.1 / Gradle 9.4.1
- [ ] compileSdk 36, targetSdk 35
- [ ] Hilt DI 그래프 런타임 동작 (FeetballApp + MainActivity)
- [ ] Retrofit + OkHttp 설정 완료 (X-Mas 헤더 + Turnstile 인증)
- [ ] FeetballTheme + 공통 컴포넌트 존재
- [ ] Room Database 연결 (빈 상태)
- [ ] 빈 Compose 화면 표시
- [ ] `git commit -m "feat: Stage 1 프로젝트 셋업 완료"`
