# Stage 2 / Slice 3 — 경기 일정 화면

> **목표:** 앱의 메인 화면. 날짜 선택 + 리그별 그룹핑 + 경기 상태별 표시
> **선행 조건:** Slice 2 완료 (API 연동 패턴 확립)
> **Git 브랜치:** `feature/renewal-slice-3-fixture`
> **참조 파일:** `FixtureFragment.kt` (166줄), `FixtureRecyclerViewAdapter.kt` (98줄)

---

## Step 1 — core-network: DTO 작성

### MatchesDayDto.kt

```kotlin
package com.chase1st.feetballfootball.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchesDayResponseDto(
    @SerialName("leagues") val leagues: List<MatchesDayLeagueDto> = emptyList(),
    @SerialName("date") val date: String = "",
)

@Serializable
data class MatchesDayLeagueDto(
    @SerialName("ccode") val ccode: String = "",
    @SerialName("id") val id: Int = 0,
    @SerialName("primaryId") val primaryId: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("matches") val matches: List<MatchItemDto> = emptyList(),
    @SerialName("internalRank") val internalRank: Int = 0,
)

@Serializable
data class MatchItemDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("leagueId") val leagueId: Int = 0,
    @SerialName("time") val time: String = "",
    @SerialName("home") val home: MatchTeamDto,
    @SerialName("away") val away: MatchTeamDto,
    @SerialName("statusId") val statusId: Int = 0,
    @SerialName("tournamentStage") val tournamentStage: String = "",
    @SerialName("status") val status: MatchStatusDto,
    @SerialName("timeTS") val timeTS: Long = 0,
)

@Serializable
data class MatchTeamDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("score") val score: Int? = null,
    @SerialName("name") val name: String = "",
    @SerialName("longName") val longName: String = "",
)

@Serializable
data class MatchStatusDto(
    @SerialName("utcTime") val utcTime: String = "",
    @SerialName("finished") val finished: Boolean = false,
    @SerialName("started") val started: Boolean = false,
    @SerialName("cancelled") val cancelled: Boolean = false,
    @SerialName("scoreStr") val scoreStr: String? = null,
    @SerialName("reason") val reason: MatchReasonDto? = null,
)

@Serializable
data class MatchReasonDto(
    @SerialName("short") val short: String = "",
    @SerialName("long") val long: String = "",
)
```

## Step 2 — core-network: API 엔드포인트 추가

```kotlin
// FootballApiService.kt에 추가
@GET("api/data/matches")
suspend fun getMatchesByDate(
    @Query("date") date: String,           // YYYYMMDD 형식
    @Query("timezone") timezone: String = "Asia/Seoul",
    @Query("ccode3") ccode3: String = "KOR",
): MatchesDayResponseDto
```

주의: FotMob은 `ApiResponse<T>` 래퍼를 사용하지 않고 직접 응답을 반환합니다.

---

## Step 3 — core-model: Domain Model

### Fixture.kt

```kotlin
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

// FotMob은 status.finished, status.started, status.cancelled boolean으로 상태를 판단
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
        fun fromFotMobStatus(status: MatchStatusDto): MatchStatus = when {
            status.cancelled -> CANCELLED
            status.reason?.short == "PP" -> POSTPONED
            status.finished -> FINISHED
            status.reason?.short == "HT" -> HALF_TIME
            status.started -> LIVE  // 1H, 2H 등은 세부 구분 어려움
            else -> NOT_STARTED
        }
    }

    val isFinished: Boolean get() = this == FINISHED
    val isLive: Boolean get() = this in listOf(FIRST_HALF, HALF_TIME, SECOND_HALF, LIVE)
    val isClickable: Boolean get() = this !in listOf(NOT_STARTED, POSTPONED, CANCELLED)
}
```

---

## Step 4 — core-domain: Repository 인터페이스 + UseCase

### FixtureRepository.kt

```kotlin
package com.chase1st.feetballfootball.core.domain.repository

import com.chase1st.feetballfootball.core.model.Fixture
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface FixtureRepository {
    fun getFixturesByDate(date: LocalDate): Flow<List<Fixture>>
}
```

### GetFixturesByDateUseCase.kt

```kotlin
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

---

## Step 5 — core-data: Mapper + Repository 구현

### FixtureMapper.kt

```kotlin
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
        status = MatchStatus.fromFotMobStatus(dto.status),
        elapsed = null,  // FotMob 일별 매치 API에는 elapsed 없음
        venue = null,
        league = LeagueInfo(
            id = league.id,
            name = league.name,
            country = league.ccode,
            logoUrl = "https://images.fotmob.com/image_resources/logo/leaguelogo/dark/${league.id}.png",
        ),
        homeTeam = Team(
            id = dto.home.id,
            name = dto.home.longName.ifEmpty { dto.home.name },
            logoUrl = "https://images.fotmob.com/image_resources/logo/teamlogo/${dto.home.id}.png",
        ),
        awayTeam = Team(
            id = dto.away.id,
            name = dto.away.longName.ifEmpty { dto.away.name },
            logoUrl = "https://images.fotmob.com/image_resources/logo/teamlogo/${dto.away.id}.png",
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

### FixtureRepositoryImpl.kt

```kotlin
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
        // FotMob은 YYYYMMDD 형식 사용
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val response = apiService.getMatchesByDate(dateStr)
        emit(mapper.mapMatchesDayResponse(response))
    }.flowOn(ioDispatcher)
}
```

### DataModule.kt 업데이트

```kotlin
// core-data/di/DataModule.kt에 추가
@Binds
abstract fun bindFixtureRepository(impl: FixtureRepositoryImpl): FixtureRepository
```

---

## Step 6 — feature-fixture: ViewModel + UiState

### FixtureUiState.kt

```kotlin
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

### FixtureViewModel.kt

```kotlin
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

---

## Step 7 — feature-fixture: Compose UI

### DateSelector.kt

```kotlin
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

### FixtureItem.kt

```kotlin
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

### FixtureScreen.kt

```kotlin
package com.chase1st.feetballfootball.feature.fixture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

---

## ★ Slice 3 완료 검증

**체크리스트:**

- [ ] 앱 실행 시 오늘 날짜 경기 목록 표시
- [ ] 이전/다음 날짜 버튼 동작 (API 재호출)
- [ ] 리그별 그룹핑 + stickyHeader 표시
- [ ] 경기 상태별 분기: 시간(미시작) / 스코어(FT) / 라이브(started) / 연기(PP) / 취소(Canc)
- [ ] 종료/라이브 경기만 클릭 가능
- [ ] 경기 없는 날짜에 빈 상태 표시
- [ ] `git commit -m "feat: Slice 3 경기 일정 화면 구현"`
