# Stage 3 / Testing — 테스트 인프라 구축

> **목표:** UseCase, Repository, ViewModel, Compose UI에 대한 테스트 커버리지 확보
> **선행 조건:** Stage 2 전체 완료 (모든 Slice + Navigation + Cleanup)
> **Git 브랜치:** `feature/renewal-testing`
> **테스트 프레임워크:** JUnit Jupiter 6.0.3 + MockK + Turbine + Compose UI Test

---

## Step 1 — 테스트 의존성 확인

### libs.versions.toml (Stage 1에서 이미 추가됨)

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

### Convention Plugin: feetball.android.test

```kotlin
// build-logic/convention/src/main/kotlin/AndroidTestConventionPlugin.kt
// Stage 1에서 이미 생성됨 — JUnit Jupiter (6.0.3) + MockK + Turbine + Coroutines Test 설정
```

---

## Step 2 — 테스트용 Fake/TestDouble 구성

### 2.1 core-common: TestDispatcherRule

```kotlin
// core/core-common/src/test/kotlin/.../testing/TestDispatcherRule.kt
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

### 2.2 core-network: FakeFootballApiService

```kotlin
// core/core-network/src/test/kotlin/.../testing/FakeFootballApiService.kt
package com.chase1st.feetballfootball.core.network.testing

import com.chase1st.feetballfootball.core.network.api.FootballApiService
import com.chase1st.feetballfootball.core.network.model.*

class FakeFootballApiService : FootballApiService {

    // 테스트에서 응답을 주입할 수 있는 변수들
    // FotMob API는 ApiResponse<T> 래퍼 없이 직접 응답을 반환
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

### 2.3 core-data: FakeRepository 구현체

```kotlin
// core/core-data/src/test/kotlin/.../testing/FakeFixtureRepository.kt
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

```kotlin
// core/core-data/src/test/kotlin/.../testing/FakeLeagueRepository.kt
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

### 2.4 테스트 Fixture 데이터 팩토리

```kotlin
// core/core-model/src/test/kotlin/.../testing/TestFixtures.kt
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
        homeTeam = Team(id = 42, name = homeTeam, logoUrl = "https://images.fotmob.com/image_resources/logo/teamlogo/42.png"),
        awayTeam = Team(id = 49, name = awayTeam, logoUrl = "https://images.fotmob.com/image_resources/logo/teamlogo/49.png"),
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
        team = Team(id = rank, name = teamName, logoUrl = "https://images.fotmob.com/image_resources/logo/teamlogo/$rank.png"),
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
        player = Player(id = rank, name = playerName, photoUrl = "https://images.fotmob.com/image_resources/playerimages/$rank.png"),
        team = Team(id = rank, name = teamName, logoUrl = "https://images.fotmob.com/image_resources/logo/teamlogo/$rank.png"),
        goals = goals,
        assists = assists,
        appearances = 28,
    )
}
```

---

## Step 3 — UseCase 단위 테스트

### 3.1 GetFixturesByDateUseCase 테스트

```kotlin
// core/core-domain/src/test/kotlin/.../usecase/GetFixturesByDateUseCaseTest.kt
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

### 3.2 GetLeagueStandingsUseCase 테스트

```kotlin
// core/core-domain/src/test/kotlin/.../usecase/GetLeagueStandingsUseCaseTest.kt

@Test
fun `리그 순위 조회 성공 - 순위 정렬 확인`() = runTest {
    // Given
    val standings = listOf(
        TestFixtures.teamStanding(rank = 2, teamName = "Chelsea", points = 55),
        TestFixtures.teamStanding(rank = 1, teamName = "Arsenal", points = 65),
    )
    repository.standings = mapOf(47 to Result.Success(standings))

    // When
    val result = useCase(leagueId = 47, season = 2025)

    // Then
    assertTrue(result is Result.Success)
    val data = (result as Result.Success).data
    assertEquals("Arsenal", data[0].team.name)  // 순위 1위가 먼저
}
```

---

## Step 4 — Repository 통합 테스트

Fake API 서비스를 사용하여 Repository의 DTO → Domain 매핑이 정상인지 검증합니다.

### 4.1 FixtureRepositoryImpl 테스트

```kotlin
// core/core-data/src/test/kotlin/.../repository/FixtureRepositoryImplTest.kt
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
        // Given — FakeFootballApiService에 FotMob DTO 응답 설정
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

### 4.2 Mapper 단위 테스트

```kotlin
// core/core-data/src/test/kotlin/.../mapper/FixtureMapperTest.kt
package com.chase1st.feetballfootball.core.data.mapper

import com.chase1st.feetballfootball.core.model.MatchStatus
import com.chase1st.feetballfootball.core.network.model.MatchStatusDto
import com.chase1st.feetballfootball.core.network.model.MatchStatusReasonDto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FixtureMapperTest {

    // FotMob은 MatchStatusDto의 boolean 필드(finished, started, cancelled)로 상태를 판단
    @Test
    fun `종료된 경기 매핑`() {
        val status = MatchStatusDto(finished = true, started = true, cancelled = false)
        val result = MatchStatus.fromFotMobStatus(status)
        assertEquals(MatchStatus.FINISHED, result)
    }

    @Test
    fun `진행중인 경기 매핑`() {
        val status = MatchStatusDto(finished = false, started = true, cancelled = false)
        val result = MatchStatus.fromFotMobStatus(status)
        assertEquals(MatchStatus.LIVE, result)
    }

    @Test
    fun `취소된 경기 매핑`() {
        val status = MatchStatusDto(finished = false, started = false, cancelled = true)
        val result = MatchStatus.fromFotMobStatus(status)
        assertEquals(MatchStatus.CANCELLED, result)
    }

    @Test
    fun `하프타임 매핑`() {
        val status = MatchStatusDto(
            finished = false, started = true, cancelled = false,
            reason = MatchStatusReasonDto(short = "HT"),
        )
        val result = MatchStatus.fromFotMobStatus(status)
        assertEquals(MatchStatus.HALF_TIME, result)
    }

    @Test
    fun `연기된 경기 매핑`() {
        val status = MatchStatusDto(
            finished = false, started = false, cancelled = false,
            reason = MatchStatusReasonDto(short = "PP"),
        )
        val result = MatchStatus.fromFotMobStatus(status)
        assertEquals(MatchStatus.POSTPONED, result)
    }

    @Test
    fun `시작 전 경기는 NOT_STARTED 반환`() {
        val status = MatchStatusDto(finished = false, started = false, cancelled = false)
        val result = MatchStatus.fromFotMobStatus(status)
        assertEquals(MatchStatus.NOT_STARTED, result)
    }
}
```

```kotlin
// core/core-data/src/test/kotlin/.../mapper/FixtureDetailMapperTest.kt

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

---

## Step 5 — ViewModel 테스트

### 5.1 FixtureViewModel 테스트

```kotlin
// feature/feature-fixture/src/test/kotlin/.../FixtureViewModelTest.kt
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

### 5.2 StandingViewModel 테스트

```kotlin
// feature/feature-league/src/test/kotlin/.../StandingViewModelTest.kt

@Test
fun `리그 순위와 선수 순위 동시 로딩`() = runTest {
    // Given
    repository.standings = mapOf(47 to Result.Success(listOf(TestFixtures.teamStanding())))
    repository.topScorers = mapOf(47 to Result.Success(listOf(TestFixtures.playerStanding())))
    repository.topAssists = mapOf(47 to Result.Success(listOf(TestFixtures.playerStanding(assists = 10))))

    // When — FotMob 리그 ID 사용 (Premier League = 47)
    viewModel = StandingViewModel(
        getStandingsUseCase = GetLeagueStandingsUseCase(repository),
        getTopScorersUseCase = GetTopScorersUseCase(repository),
        getTopAssistsUseCase = GetTopAssistsUseCase(repository),
        savedStateHandle = SavedStateHandle(mapOf("leagueId" to 47)),
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

---

## Step 6 — Compose UI 테스트

### 6.1 의존성 확인

```toml
# libs.versions.toml (Stage 1에서 추가됨)
[libraries]
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
```

### 6.2 FixtureScreen UI 테스트

```kotlin
// feature/feature-fixture/src/androidTest/kotlin/.../FixtureScreenTest.kt
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

### 6.3 StandingScreen UI 테스트

```kotlin
// feature/feature-league/src/androidTest/kotlin/.../StandingScreenTest.kt

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

---

## Step 7 — 테스트 실행 및 CI 설정

### 7.1 테스트 실행 명령어

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

### 7.2 테스트 커버리지 목표

| 모듈 | 목표 커버리지 | 우선순위 |
|------|-------------|---------|
| core-domain (UseCase) | 90%+ | 최우선 |
| core-data (Repository, Mapper) | 80%+ | 높음 |
| feature-* (ViewModel) | 80%+ | 높음 |
| feature-* (Compose UI) | 60%+ | 중간 |
| core-network (DTO) | N/A (데이터 클래스) | 낮음 |

---

## ★ Testing 완료 검증 체크리스트

- [ ] TestDispatcherExtension 동작 확인
- [ ] FakeFootballApiService 동작 확인
- [ ] FakeFixtureRepository, FakeLeagueRepository 동작 확인
- [ ] TestFixtures 팩토리 동작 확인
- [ ] UseCase 테스트 전체 통과 (GetFixturesByDate, GetLeagueStandings, GetTopScorers, GetTopAssists, GetFixtureDetail)
- [ ] Repository 통합 테스트 전체 통과
- [ ] Mapper 단위 테스트 전체 통과 (FotMob MatchStatusDto 매핑, 이벤트 타입, 그리드 파싱, 통계 파싱)
- [ ] ViewModel 테스트 전체 통과 (FixtureViewModel, StandingViewModel, FixtureDetailViewModel)
- [ ] Compose UI 테스트 전체 통과
- [ ] `./gradlew test` 전체 성공
- [ ] `git commit -m "test: 테스트 인프라 구축 + UseCase/Repository/ViewModel/UI 테스트"`
