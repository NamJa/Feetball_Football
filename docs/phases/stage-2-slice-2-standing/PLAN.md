# Stage 2 / Slice 2 — 리그 순위 화면

> **목표:** 첫 API 연동. DTO→Domain→Repository→UseCase→ViewModel→UI 전체 수직 스택 구축
> **선행 조건:** Slice 1 완료
> **Git 브랜치:** `feature/renewal-slice-2-standing`
> **참조 파일:** `LeagueClubsStandingFragment.kt` (136줄), `LeaguePlayerStandingFragment.kt` (169줄), `LeagueStandingFragment.kt` (114줄)

---

## 이 Slice가 두 번째인 이유

단순 리스트 형태의 API 응답이므로, **새 API → Domain Model → UI** 파이프라인을 처음으로 end-to-end 검증하기에 적합합니다. 이 Slice에서 확립한 패턴을 이후 모든 Slice에서 반복 사용합니다.

---

## Step 1 — core-network: DTO 작성

> FotMob API(`/api/tltable`)의 응답 구조에 맞는 DTO를 정의합니다.

### StandingDto.kt

```kotlin
// core/core-network/src/main/kotlin/.../core/network/model/StandingDto.kt
package com.chase1st.feetballfootball.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TlTableResponseDto(
    @SerialName("data") val data: TlTableDataDto,
)

@Serializable
data class TlTableDataDto(
    @SerialName("ccode") val ccode: String = "",
    @SerialName("leagueId") val leagueId: Int = 0,
    @SerialName("leagueName") val leagueName: String = "",
    @SerialName("isCurrentSeason") val isCurrentSeason: Boolean = true,
    @SerialName("tableFilterTypes") val tableFilterTypes: List<String> = emptyList(),
    @SerialName("legend") val legend: List<LegendDto> = emptyList(),
    @SerialName("table") val table: TableDto,
)

@Serializable
data class LegendDto(
    @SerialName("title") val title: String = "",
    @SerialName("color") val color: String = "",
    @SerialName("indices") val indices: List<Int> = emptyList(),
)

@Serializable
data class TableDto(
    @SerialName("all") val all: List<TableItemDto> = emptyList(),
    @SerialName("home") val home: List<TableItemDto> = emptyList(),
    @SerialName("away") val away: List<TableItemDto> = emptyList(),
)

@Serializable
data class TableItemDto(
    @SerialName("name") val name: String = "",
    @SerialName("shortName") val shortName: String = "",
    @SerialName("id") val id: Int = 0,
    @SerialName("pageUrl") val pageUrl: String = "",
    @SerialName("played") val played: Int = 0,
    @SerialName("wins") val wins: Int = 0,
    @SerialName("draws") val draws: Int = 0,
    @SerialName("losses") val losses: Int = 0,
    @SerialName("scoresStr") val scoresStr: String = "0-0",
    @SerialName("goalConDiff") val goalConDiff: Int = 0,
    @SerialName("pts") val pts: Int = 0,
    @SerialName("idx") val idx: Int = 0,
    @SerialName("qualColor") val qualColor: String? = null,
    @SerialName("deduction") val deduction: Int? = null,
)
```

### PlayerStandingDto.kt

> FotMob에서는 선수 통계를 `/api/leagues?id={leagueId}&tab=stats&season={season}` 엔드포인트로 조회합니다. 상세 응답 구조는 추후 확인이 필요하므로, 현재는 팀 순위 기능을 우선 구현하고 선수 순위는 이후 업데이트합니다.

### 공통 응답 래퍼

> FotMob API는 `ApiResponse<T>` 형태의 공통 래퍼를 사용하지 않습니다. `/api/tltable`은 JSON 배열(`List<TlTableResponseDto>`)을 직접 반환합니다. 따라서 별도의 래퍼 클래스가 필요 없습니다.

---

## Step 2 — core-network: API 엔드포인트 추가

```kotlin
// FootballApiService.kt에 추가
import com.chase1st.feetballfootball.core.network.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface FootballApiService {

    @GET("api/tltable")
    suspend fun getStandings(
        @Query("leagueId") leagueId: Int,
    ): List<TlTableResponseDto>

    // 선수 통계는 /api/leagues 엔드포인트의 stats 탭에서 조회
    // FotMob API에서는 별도의 topscorers/topassists 엔드포인트를 제공하지 않음
    // → /api/leagues?id={leagueId}&tab=stats&season={season} 으로 접근
}
```

---

## Step 3 — core-model: Domain Model

### TeamStanding.kt

```kotlin
// core/core-model/src/main/kotlin/.../core/model/TeamStanding.kt
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

### PlayerStanding.kt

```kotlin
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

---

## Step 4 — core-domain: Repository 인터페이스 + UseCase

### LeagueRepository.kt

```kotlin
// core/core-domain/src/main/kotlin/.../core/domain/repository/LeagueRepository.kt
package com.chase1st.feetballfootball.core.domain.repository

import com.chase1st.feetballfootball.core.model.TeamStanding
import kotlinx.coroutines.flow.Flow

interface LeagueRepository {
    fun getLeagueStandings(leagueId: Int): Flow<List<TeamStanding>>
    // 선수 통계 메서드는 FotMob /api/leagues stats 탭 응답 구조 확인 후 추가
}
```

### GetLeagueStandingsUseCase.kt

```kotlin
package com.chase1st.feetballfootball.core.domain.usecase

import com.chase1st.feetballfootball.core.domain.repository.LeagueRepository
import com.chase1st.feetballfootball.core.model.TeamStanding
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLeagueStandingsUseCase @Inject constructor(
    private val leagueRepository: LeagueRepository,
) {
    operator fun invoke(leagueId: Int): Flow<List<TeamStanding>> =
        leagueRepository.getLeagueStandings(leagueId)
}
```

### GetTopScorersUseCase.kt, GetTopAssistsUseCase.kt

> FotMob API에서 선수 통계는 `/api/leagues?id={leagueId}&tab=stats&season={season}` 으로 접근합니다. 상세 응답 구조 확인 후 UseCase를 추가할 예정입니다.

---

## Step 5 — core-data: Mapper + Repository 구현

### StandingMapper.kt

```kotlin
// core/core-data/src/main/kotlin/.../core/data/mapper/StandingMapper.kt
package com.chase1st.feetballfootball.core.data.mapper

import com.chase1st.feetballfootball.core.model.*
import com.chase1st.feetballfootball.core.network.model.*
import javax.inject.Inject

class StandingMapper @Inject constructor() {

    fun mapTeamStandings(dtos: List<TableItemDto>): List<TeamStanding> =
        dtos.map { dto ->
            val (goalsFor, goalsAgainst) = parseScoresStr(dto.scoresStr)
            TeamStanding(
                rank = dto.idx,
                team = Team(
                    id = dto.id,
                    name = dto.name,
                    logoUrl = "https://images.fotmob.com/image_resources/logo/teamlogo/${dto.id}.png",
                ),
                points = dto.pts,
                played = dto.played,
                won = dto.wins,
                drawn = dto.draws,
                lost = dto.losses,
                goalsFor = goalsFor,
                goalsAgainst = goalsAgainst,
                goalDifference = dto.goalConDiff,
                form = null,  // form 탭에서 별도 조회 가능
                description = dto.qualColor,  // 진출권 색상으로 대체
            )
        }

    private fun parseScoresStr(scoresStr: String): Pair<Int, Int> {
        val parts = scoresStr.split("-")
        return if (parts.size == 2) {
            (parts[0].trim().toIntOrNull() ?: 0) to (parts[1].trim().toIntOrNull() ?: 0)
        } else {
            0 to 0
        }
    }
}
```

### LeagueRepositoryImpl.kt

```kotlin
package com.chase1st.feetballfootball.core.data.repository

import com.chase1st.feetballfootball.core.common.dispatcher.IoDispatcher
import com.chase1st.feetballfootball.core.data.mapper.StandingMapper
import com.chase1st.feetballfootball.core.domain.repository.LeagueRepository
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

    override fun getLeagueStandings(leagueId: Int): Flow<List<TeamStanding>> = flow {
        val response = apiService.getStandings(leagueId)
        val standings = response.firstOrNull()?.data?.table?.all ?: emptyList()
        emit(mapper.mapTeamStandings(standings))
    }.flowOn(ioDispatcher)
}
```

### DataModule.kt (Repository 바인딩)

```kotlin
// core/core-data/src/main/kotlin/.../core/data/di/DataModule.kt
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

---

## Step 6 — feature-league: ViewModel + UiState

### StandingUiState.kt

```kotlin
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

### StandingViewModel.kt

```kotlin
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
    // FotMob 리그 ID 사용 (예: Premier League = 47)
    private val leagueId: Int = savedStateHandle["leagueId"] ?: 47

    private val _uiState = MutableStateFlow<StandingUiState>(StandingUiState.Loading)
    val uiState: StateFlow<StandingUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val standings = getLeagueStandingsUseCase(leagueId).first()

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

---

## Step 7 — feature-league: Compose UI

### StandingScreen.kt

> 현재는 팀 순위 탭만 구현합니다. 선수 순위 탭은 FotMob `/api/leagues` stats 응답 구조 확인 후 추가합니다.

```kotlin
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

### ClubStandingTab.kt

```kotlin
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

### PlayerStandingTab.kt

> 선수 순위 탭은 FotMob `/api/leagues?id={leagueId}&tab=stats&season={season}` 응답 구조 확인 후 추가합니다. 현재 Slice에서는 팀 순위 기능을 우선 구현합니다.

---

## ★ Slice 2 완료 검증

**체크리스트:**

- [ ] 리그 선택 → 순위 화면 전환 (임시 하드코딩 or Logcat)
- [ ] 클럽 순위 탭: FotMob `/api/tltable` API 호출 → 20개 팀 순위표 표시
- [ ] 팀 로고가 FotMob CDN URL (`https://images.fotmob.com/image_resources/logo/teamlogo/{teamId}.png`)로 정상 로딩되는지 확인
- [ ] `scoresStr` ("61-22" 형태) 파싱이 정상적인지 확인
- [ ] 로딩 상태 → 성공 상태 전환 정상
- [ ] 에러 상태 → 다시 시도 동작
- [ ] 팀 로고 이미지 로딩 정상 (Coil)
- [ ] **이 시점에서 DTO→Domain→Repository→UseCase→ViewModel→UI 패턴 확립**
- [ ] `git commit -m "feat: Slice 2 리그 순위 화면 구현 (FotMob API 연동)"`
