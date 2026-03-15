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
| core-network | Ktor 기반 `FotMobApiService` (Retrofit + API-Sports 대체) |
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

### 3. Retrofit → Ktor — HTTP 클라이언트 전환 (FotMob API)

```
// 변경 전 (Retrofit + API-Sports)
@GET("fixtures")
suspend fun getFixtures(@Query("date") date: String): ApiResponse<List<FixtureResponseDto>>

// 변경 후 (Ktor + FotMob)
suspend fun getMatches(date: String, timezone: String = "Asia/Seoul"): MatchesResponseDto {
    return client.get("$baseUrl/api/data/matches") {
        parameter("date", date)
        parameter("timezone", timezone)
    }.body()
}
```

> ⚠️ **주의:** Retrofit의 선언적 인터페이스 방식에서 Ktor의 명시적 함수 호출 방식으로 변경됩니다. 또한 API-Sports에서 FotMob API로 전환되므로 엔드포인트와 응답 형식이 모두 달라집니다. FotMob은 API 키가 불필요하며, X-Mas 헤더를 MD5 기반으로 동적 생성하는 것이 권장됩니다.

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
kotlin = "2.3.10"          # 기존 유지 (KMP 지원)
ktor = "3.4.1"
koin = "4.1.1"
kotlinx-datetime = "0.7.1"    # 0.7.0에서 Instant/Clock이 kotlin.time으로 마이그레이션 — 코드 변경 필요
kotlinx-coroutines = "1.10.2"
room = "2.8.4"             # Room KMP 지원 버전 (2.7.0부터 KMP 지원)

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

## Step 6 — core-network → Ktor + FotMob API 전환

### 목표
> Retrofit 기반의 `FootballApiService`를 Ktor 기반의 `FotMobApiService`로 전환합니다. API-Sports에서 FotMob API로 변경하고, HttpClient를 `expect/actual` 패턴으로 플랫폼별 설정합니다.

### 작업 내용

#### 6.1 FotMobApiService — Retrofit + API-Sports → Ktor + FotMob

**파일:** `shared/src/commonMain/kotlin/com/chase1st/feetballfootball/network/FotMobApiService.kt`

**이유:** Retrofit은 JVM 전용입니다. Ktor는 KMP를 지원하는 HTTP 클라이언트로, Android(OkHttp 엔진)와 iOS(Darwin 엔진)에서 동일한 API를 사용할 수 있습니다. 동시에 API-Sports v3에서 FotMob API로 전환합니다. FotMob은 API 키가 불필요하고, X-Mas 헤더(MD5 기반)를 선택적으로 추가할 수 있습니다.

**변경 포인트:**
- Retrofit의 `@GET`/`@Query` 어노테이션 → Ktor의 `client.get()` + `parameter()`
- Retrofit 인터페이스 → 일반 Kotlin 클래스
- Base URL: `https://v3.football.api-sports.io` → `https://www.fotmob.com`
- 인증: `x-apisports-key` 헤더 제거 (FotMob은 API 키 불필요)
- 엔드포인트: `/fixtures` → `/api/data/matches`, `/standings` → `/api/tltable` 등

```kotlin
// shared/src/commonMain/kotlin/.../network/FotMobApiService.kt
package com.chase1st.feetballfootball.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import com.chase1st.feetballfootball.network.dto.*

class FotMobApiService(
    private val client: HttpClient,
) {
    private val baseUrl = "https://www.fotmob.com"

    suspend fun getMatches(date: String, timezone: String = "Asia/Seoul"): MatchesResponseDto {
        return client.get("$baseUrl/api/data/matches") {
            parameter("date", date)
            parameter("timezone", timezone)
        }.body()
    }

    suspend fun getStandings(leagueId: Int): StandingResponseDto {
        return client.get("$baseUrl/api/tltable") {
            parameter("leagueId", leagueId)
        }.body()
    }

    suspend fun getLeagueFixtures(leagueId: Int, season: String): LeagueResponseDto {
        return client.get("$baseUrl/api/leagues") {
            parameter("id", leagueId)
            parameter("tab", "fixtures")
            parameter("season", season)
        }.body()
    }

    suspend fun getAllLeagues(): AllLeaguesResponseDto {
        return client.get("$baseUrl/api/allLeagues").body()
    }

    suspend fun getMatchDetails(matchId: Int): MatchDetailsResponseDto {
        // ⚠️ matchDetails 엔드포인트는 Turnstile 보호 — 별도 처리 필요
        return client.get("$baseUrl/api/matchDetails") {
            parameter("matchId", matchId)
        }.body()
    }

    suspend fun getTeamFixtures(teamId: Int): TeamResponseDto {
        return client.get("$baseUrl/api/teams") {
            parameter("id", teamId)
            parameter("tab", "fixtures")
        }.body()
    }
}
```

> 💡 **Tip:** FotMob API는 API 키가 불필요합니다. X-Mas 헤더(MD5 기반 동적 생성)를 추가하면 요청 안정성이 높아집니다. `HttpClient`의 `defaultRequest {}` 또는 별도 Interceptor에서 X-Mas 헤더를 자동 생성하도록 구성하는 것을 권장합니다.

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
            // FotMob은 API 키 불필요
            // X-Mas 헤더는 XMasInterceptor에서 MD5 기반으로 동적 생성
        }
    }
}
```

```kotlin
// shared/src/commonMain/kotlin/.../network/XMasInterceptor.kt
// X-Mas 헤더 생성 로직 (MD5 기반, 선택사항이나 권장)
// 요청 시점에 동적으로 생성하여 헤더에 추가
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
- [ ] `FotMobApiService`가 Ktor 기반으로 변환됨
- [ ] 6개 API 엔드포인트(getMatches, getStandings, getLeagueFixtures, getAllLeagues, getMatchDetails, getTeamFixtures)가 모두 구현됨
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
    single { FotMobApiService(get()) }  // FotMob은 API 키 불필요

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

> 💡 **Tip:** Koin의 `singleOf(::FixtureRepositoryImpl) bind FixtureRepository::class`는 "FixtureRepositoryImpl을 싱글톤으로 생성하되, FixtureRepository 인터페이스로 요청하면 이 인스턴스를 반환하라"는 의미입니다. FotMob은 API 키가 불필요하므로 `FotMobApiService`에는 `HttpClient`만 주입합니다.

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

> ⚠️ **주의:** `@HiltAndroidApp` 어노테이션을 제거하면, 기존 Hilt로 주입받던 모든 곳에서 에러가 발생합니다. `@AndroidEntryPoint`, `@HiltViewModel` 등도 모두 제거하고 Koin 방식으로 교체해야 합니다. 이 작업은 한 번에 진행하는 것을 권장합니다. FotMob은 API 키가 불필요하므로 `properties(mapOf("apiKey" to ...))` 설정도 제거합니다.

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

**이유:** `java.time.LocalDate.parse()`는 `DateTimeFormatter`를 인자로 받지만, `kotlinx.datetime.LocalDate.parse()`는 ISO 8601 형식(yyyy-MM-dd)을 기본 지원합니다. FotMob의 날짜 파라미터가 `YYYYMMDD` 형식이므로 Mapper에서 적절히 변환해야 합니다.

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
compose-multiplatform = "1.10.2"

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
- [ ] core-network → Ktor + FotMob API 전환 (commonMain + androidMain/iosMain)
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
- [ ] Retrofit + API-Sports → Ktor + FotMob — 6개 API 엔드포인트 모두 변환 확인
- [ ] `x-apisports-key` 헤더 제거, X-Mas 헤더 Interceptor 구성 확인

#### 선택사항
- [ ] Compose Multiplatform 1.10.2 적용
- [ ] Navigation 3 KMP 지원 평가 (1.1.0-beta01+)

### 커밋

```bash
git commit -m "feat: KMP 전환 — shared 모듈 + Ktor + Koin + kotlinx-datetime"
```

### 다음 단계

Stage 3의 모든 섹션(Testing, Optimization, KMP)이 완료되었습니다. 앱의 현대화 작업이 마무리되었으며, 이후에는 기능 개발과 유지보수에 집중합니다.
