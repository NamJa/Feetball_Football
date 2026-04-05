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
junit5 = "5.14.3"
mockk = "1.14.7"
turbine = "1.2.1"
coroutines-test = "1.10.2"

[libraries]
junit5-api = { group = "org.junit.jupiter", name = "junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { group = "org.junit.jupiter", name = "junit-jupiter-engine", version.ref = "junit5" }
junit5-params = { group = "org.junit.jupiter", name = "junit-jupiter-params", version.ref = "junit5" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines-test" }
```

**파일:** `build-logic/convention/src/main/kotlin/AndroidTestConventionPlugin.kt`

Stage 1에서 이미 생성된 Convention Plugin입니다. `feetball.android.test` 플러그인을 적용하면 JUnit 5 + MockK + Turbine + Coroutines Test가 자동으로 설정됩니다.

```kotlin
// build-logic/convention/src/main/kotlin/AndroidTestConventionPlugin.kt
// Stage 1에서 이미 생성됨 — JUnit 5 + MockK + Turbine + Coroutines Test 설정
```

> 💡 **Tip:** Convention Plugin 덕분에 각 모듈의 `build.gradle.kts`에서 테스트 의존성을 개별 선언할 필요가 없습니다. `plugins { id("feetball.android.test") }` 한 줄이면 충분합니다.

### ✅ 검증
- [ ] `libs.versions.toml`에 junit5, mockk, turbine, coroutines-test 버전이 선언되어 있다
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
