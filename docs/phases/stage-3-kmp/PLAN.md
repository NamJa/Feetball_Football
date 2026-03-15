# Stage 3 / KMP — Kotlin Multiplatform 전환

> **목표:** core 모듈을 KMP commonMain으로 전환, iOS 앱 기반 마련
> **선행 조건:** Stage 3 Optimization 완료, Android 앱 안정화
> **Git 브랜치:** `feature/renewal-kmp`
> **참조:** Migration_Plan.md Phase 10

---

## ⚠️ 전환 전 고려사항

### KMP 전환이 적합한가?

| 질문 | 답변 기준 |
|------|----------|
| iOS 앱 개발 계획이 있는가? | Yes → KMP 진행 / No → 보류 |
| 팀에 iOS 개발자가 있는가? | UI 레이어는 SwiftUI 필요 |
| 공유할 비즈니스 로직이 충분한가? | core-model, core-domain, core-data → 충분 |
| 일정 여유가 있는가? | KMP 전환은 2~4주 소요 예상 |

> **결론:** iOS 앱 개발이 확정된 경우에만 진행합니다. 그렇지 않으면 이 단계는 보류합니다.

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

### 1.1 전환 대상 모듈

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

### 1.2 최종 모듈 구조

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

---

## Step 2 — Gradle 설정 전환

### 2.1 libs.versions.toml에 KMP 의존성 추가

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

### 2.2 shared 모듈 build.gradle.kts

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

---

## Step 3 — core-model → commonMain 이동

### 3.1 java.time → kotlinx-datetime 변환

core-model은 순수 Kotlin 데이터 클래스이므로 가장 쉽게 전환됩니다. 유일한 변경점은 `java.time` → `kotlinx-datetime`입니다.

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

### 3.2 이동 대상 파일

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

---

## Step 4 — core-domain → commonMain 이동

UseCase와 Repository 인터페이스는 순수 Kotlin이므로 변경 없이 이동됩니다.

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

### 4.1 DI 어노테이션 제거

Hilt의 `@Inject constructor`는 KMP에서 사용할 수 없으므로, Koin으로 전환합니다:

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

---

## Step 5 — core-common → commonMain 이동

### 5.1 Result sealed interface

```kotlin
// shared/src/commonMain/kotlin/.../common/Result.kt
// 변경 없음 — 순수 Kotlin
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable) : Result<Nothing>
}
```

### 5.2 Dispatcher → expect/actual

```kotlin
// shared/src/commonMain/kotlin/.../common/Dispatcher.kt
import kotlinx.coroutines.CoroutineDispatcher

expect val ioDispatcher: CoroutineDispatcher
expect val defaultDispatcher: CoroutineDispatcher
```

```kotlin
// shared/src/androidMain/kotlin/.../common/Dispatcher.android.kt
import kotlinx.coroutines.Dispatchers

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
actual val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
```

```kotlin
// shared/src/iosMain/kotlin/.../common/Dispatcher.ios.kt
import kotlinx.coroutines.Dispatchers

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default  // iOS에는 IO 없음
actual val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
```

---

## Step 6 — core-network → Ktor + FotMob API 전환

### 6.1 Retrofit + API-Sports → Ktor + FotMob 변환

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

### 6.2 HttpClient 플랫폼별 설정

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

---

## Step 7 — DI 전환: Hilt → Koin

### 7.1 Koin 모듈 정의

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

### 7.2 Android 앱에서 Koin 초기화

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

### 7.3 ViewModel에서 Koin 사용

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

---

## Step 8 — core-data → commonMain 이동

Repository 구현체와 Mapper를 이동합니다.

### 8.1 날짜 처리 변환

```kotlin
// 변경 전
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)

// 변경 후
import kotlinx.datetime.LocalDate

val date = LocalDate.parse(dateString)  // kotlinx-datetime은 ISO 형식 기본 지원
```

### 8.2 이동 대상

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

### 8.3 SeasonUtil 변환

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

---

## Step 9 — iOS 앱 기반 설정

### 9.1 Xcode 프로젝트 생성

```bash
# KMP Wizard 또는 수동 생성
# iosApp/ 디렉토리에 Xcode 프로젝트 위치
```

### 9.2 Swift에서 shared 모듈 사용

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

### 9.3 iOS ViewModel 래퍼

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

---

## Step 10 — Compose Multiplatform (선택사항)

UI 레이어까지 공유하려면 Compose Multiplatform을 도입합니다. 이 경우 feature 모듈도 shared로 이동합니다.

### 10.1 의존성

```toml
[versions]
compose-multiplatform = "1.10.2"

[plugins]
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

### 10.2 판단 기준

| 접근 방식 | 장점 | 단점 |
|----------|------|------|
| **공유 비즈니스 로직 + 네이티브 UI** | iOS는 SwiftUI로 네이티브 경험 제공 | UI 코드 2벌 유지 |
| **Compose Multiplatform (UI도 공유)** | 코드 최대 공유 | iOS UI가 네이티브 느낌 약함 |

> **권장:** 먼저 비즈니스 로직만 공유 (Step 1~9), 이후 Compose Multiplatform 평가

> **참고:** Navigation 3 1.1.0-beta01 (2026-03-11)부터 KMP 타겟 (JVM, Native, Web)을 공식 지원합니다. Compose Multiplatform과 함께 사용하면 내비게이션 코드도 공유 가능합니다.

---

## ★ KMP 전환 완료 검증 체크리스트

### Phase A: shared 모듈 구축
- [ ] shared 모듈 생성 + Gradle 설정
- [ ] core-model → commonMain 이동 (kotlinx-datetime 적용)
- [ ] core-domain → commonMain 이동
- [ ] core-common → commonMain 이동 (expect/actual Dispatcher)
- [ ] core-network → Ktor + FotMob API 전환 (commonMain + androidMain/iosMain)
- [ ] core-data → commonMain 이동
- [ ] DI: Hilt → Koin 전환
- [ ] `./gradlew :shared:build` 성공 (Android + iOS)

### Phase B: Android 앱 연결
- [ ] androidApp이 shared 모듈 의존
- [ ] 기존 core-* 모듈 제거 (shared로 통합)
- [ ] Android 앱 전체 기능 정상 동작
- [ ] `./gradlew assembleDebug` 성공

### Phase C: iOS 앱 기반
- [ ] Xcode 프로젝트 생성
- [ ] shared framework 링크
- [ ] iOS 앱에서 API 호출 성공
- [ ] 기본 화면 1개 이상 동작 확인

### 최종
- [ ] kotlinx-datetime 0.7.x Breaking Change 대응 (Clock/Instant 패키지 변경)
- [ ] Compose Multiplatform 1.10.2 적용
- [ ] Navigation 3 KMP 지원 평가 (1.1.0-beta01+)
- [ ] `git commit -m "feat: KMP 전환 — shared 모듈 + Ktor + Koin + kotlinx-datetime"`
