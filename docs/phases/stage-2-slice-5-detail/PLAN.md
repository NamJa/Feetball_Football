# Stage 2 / Slice 5 — 경기 상세 + 3탭 (이벤트/라인업/통계)

> **목표:** 가장 복잡한 화면. 접히는 헤더 + 3개 탭 (이벤트/라인업/통계)
> **선행 조건:** Slice 3 완료 (Fixture Domain Model 존재)
> **Git 브랜치:** `feature/renewal-slice-5-detail`
> **참조 파일:** `FixtureDetailFragment.kt` (268줄), `FixtureDetailEventsFragment.kt` (168줄), `FixtureDetailLineupFragment.kt` (170줄), `FixtureDetailStatisticsFragment.kt` (256줄)

---

## 실행 전략

이 Slice는 내부적으로 세 단계로 나뉩니다:

```
Phase 5-0: Turnstile 인증 통합 (matchDetails 접근 전제 조건)
  ├── TurnstileManager 구현
  ├── TurnstileBridge JavaScript 인터페이스
  └── OkHttp Interceptor에 쿠키 주입

Phase 5-A: 하위 탭 3개 독립 구현 (병렬 가능)
  ├── EventsTab
  ├── StatisticsTab
  └── LineupTab

Phase 5-B: FixtureDetailScreen에서 조립
```

---

## Step 1 — core-network: DTO 작성

> **참고:** FotMob matchDetails API는 Cloudflare Turnstile 보호로 인해 직접 API 테스트가 불가합니다.
> 아래 DTO는 FotMob 웹사이트의 실제 matchDetails 데이터를 기반으로 한 **예상 구조**입니다.
> 실제 구현 시 Turnstile 인증 통합 후 응답을 확인하여 DTO를 조정해야 합니다.

### FixtureDetailDto.kt

```kotlin
package com.chase1st.feetballfootball.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 주의: FotMob matchDetails 응답 예상 구조 — Turnstile 인증 후 실제 응답으로 검증 필요
@Serializable
data class FixtureDetailResponseDto(
    @SerialName("fixture") val fixture: FixtureDto,
    @SerialName("league") val league: FixtureLeagueDto,
    @SerialName("teams") val teams: FixtureTeamsDto,
    @SerialName("goals") val goals: FixtureGoalsDto,
    @SerialName("events") val events: List<EventDto> = emptyList(),
    @SerialName("lineups") val lineups: List<LineupDto> = emptyList(),
    @SerialName("statistics") val statistics: List<TeamStatisticsDto> = emptyList(),
    @SerialName("players") val players: List<TeamPlayersDto> = emptyList(),
)
```

### EventDto.kt

```kotlin
@Serializable
data class EventDto(
    @SerialName("time") val time: EventTimeDto,
    @SerialName("team") val team: StandingTeamDto,
    @SerialName("player") val player: EventPlayerDto,
    @SerialName("assist") val assist: EventAssistDto? = null,
    @SerialName("type") val type: String,
    @SerialName("detail") val detail: String,
)

@Serializable
data class EventTimeDto(
    @SerialName("elapsed") val elapsed: Int,
    @SerialName("extra") val extra: Int? = null,
)

@Serializable
data class EventPlayerDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("name") val name: String? = null,
)

@Serializable
data class EventAssistDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("name") val name: String? = null,
)
```

### LineupDto.kt

```kotlin
@Serializable
data class LineupDto(
    @SerialName("team") val team: LineupTeamDto,
    @SerialName("coach") val coach: CoachDto? = null,
    @SerialName("formation") val formation: String? = null,
    @SerialName("startXI") val startXI: List<LineupPlayerWrapperDto> = emptyList(),
    @SerialName("substitutes") val substitutes: List<LineupPlayerWrapperDto> = emptyList(),
)

@Serializable
data class LineupTeamDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("logo") val logo: String,
    @SerialName("colors") val colors: TeamColorsDto? = null,
)

@Serializable
data class TeamColorsDto(
    @SerialName("player") val player: ColorCodeDto? = null,
)

@Serializable
data class ColorCodeDto(
    @SerialName("primary") val primary: String? = null,
)

@Serializable
data class CoachDto(
    @SerialName("name") val name: String? = null,
    @SerialName("photo") val photo: String? = null,
)

@Serializable
data class LineupPlayerWrapperDto(
    @SerialName("player") val player: LineupPlayerDto,
)

@Serializable
data class LineupPlayerDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("number") val number: Int,
    @SerialName("pos") val pos: String? = null,
    @SerialName("grid") val grid: String? = null,
)
```

### StatisticsDto.kt

```kotlin
@Serializable
data class TeamStatisticsDto(
    @SerialName("team") val team: StandingTeamDto,
    @SerialName("statistics") val statistics: List<StatItemDto> = emptyList(),
)

@Serializable
data class StatItemDto(
    @SerialName("type") val type: String,
    @SerialName("value") val value: String? = null,
)

@Serializable
data class TeamPlayersDto(
    @SerialName("team") val team: StandingTeamDto,
    @SerialName("players") val players: List<PlayerRatingDto> = emptyList(),
)

@Serializable
data class PlayerRatingDto(
    @SerialName("player") val player: PlayerRatingInfoDto,
    @SerialName("statistics") val statistics: List<PlayerGameStatDto> = emptyList(),
)

@Serializable
data class PlayerRatingInfoDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
)

@Serializable
data class PlayerGameStatDto(
    @SerialName("games") val games: PlayerGameDto? = null,
)

@Serializable
data class PlayerGameDto(
    @SerialName("rating") val rating: String? = null,
    @SerialName("minutes") val minutes: Int? = null,
)
```

---

## Step 1.5 — Turnstile 인증 통합 (matchDetails 전제 조건)

FotMob의 `/api/matchDetails`는 Cloudflare Turnstile 인증이 필요합니다. 단순 API 호출로는 `403 Forbidden`이 반환되므로, 앱에서 invisible WebView를 통해 Turnstile 챌린지를 통과한 후 쿠키를 확보해야 합니다.

### 인증 플로우

```
1. Turnstile 스크립트 로드 (invisible WebView)
2. turnstile.render() → callback(turnstileToken)
3. POST /api/turnstile/verify { token: turnstileToken }
4. 서버 응답: Set-Cookie: turnstile_verified=...
5. matchDetails API 호출 시 쿠키 포함 → 200 OK
```

### TurnstileManager.kt (app 모듈)

```kotlin
object TurnstileManager {
    var isVerified: Boolean = false
        private set
    var turnstileCookie: String? = null
        private set
    var expiresAt: Long = 0L
        private set
    val verificationLiveData = MutableLiveData<Boolean>(false)
    private var webView: WebView? = null
    private var retryCount = 0
    private val MAX_RETRY = 3

    fun init(context: Context, parentView: ViewGroup) {
        if (isVerified && !isExpired()) return
        retryCount = 0
        webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(0, 0)
            visibility = View.GONE
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
            addJavascriptInterface(TurnstileBridge(), "AndroidBridge")
            webViewClient = WebViewClient()
        }
        parentView.addView(webView)
        webView?.loadDataWithBaseURL(
            "https://www.fotmob.com",
            buildTurnstileHtml(),
            "text/html", "UTF-8", null
        )
    }

    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt

    fun refreshIfNeeded(context: Context, parentView: ViewGroup) {
        if (!isVerified || isExpired()) {
            init(context, parentView)
        }
    }

    private fun buildTurnstileHtml(): String = """
        <html><head>
        <script src="https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit"></script>
        </head><body>
        <div id="turnstile-container"></div>
        <script>
            turnstile.render('#turnstile-container', {
                sitekey: '0x4AAAAAACOZughTsLoeXwvg',
                callback: function(token) {
                    AndroidBridge.onTurnstileToken(token);
                }
            });
        </script>
        </body></html>
    """.trimIndent()

    // TurnstileBridge에서 호출
    internal fun onTokenReceived(token: String) {
        // POST /api/turnstile/verify 호출
        // 성공 시: isVerified = true, turnstileCookie 저장, expiresAt 설정 (24시간)
        // verificationLiveData.postValue(true)
    }

    internal fun onVerificationFailed() {
        if (retryCount < MAX_RETRY) {
            retryCount++
            // 3초 후 WebView reload 재시도
        }
    }
}
```

### TurnstileBridge.kt

```kotlin
class TurnstileBridge {
    @JavascriptInterface
    fun onTurnstileToken(token: String) {
        // POST https://www.fotmob.com/api/turnstile/verify
        // Body: { "token": token }
        // 응답의 Set-Cookie에서 turnstile_verified 쿠키 추출
        TurnstileManager.onTokenReceived(token)
    }

    @JavascriptInterface
    fun onTurnstileError(error: String) {
        TurnstileManager.onVerificationFailed()
    }
}
```

### OkHttp Interceptor에서 쿠키 주입

```kotlin
class TurnstileCookieInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // matchDetails 요청에만 쿠키 추가
        if (request.url.encodedPath.contains("matchDetails")) {
            val cookie = TurnstileManager.turnstileCookie
            if (cookie != null) {
                val newRequest = request.newBuilder()
                    .addHeader("Cookie", cookie)
                    .build()
                return chain.proceed(newRequest)
            }
        }
        return chain.proceed(request)
    }
}
```

### 검증 대기 패턴 (FixtureDetailScreen에서 사용)

```kotlin
// ViewModel에서 Turnstile 검증 완료를 기다린 후 API 호출
suspend fun loadMatchDetail(matchId: Int) {
    if (!TurnstileManager.isVerified || TurnstileManager.isExpired()) {
        // verificationLiveData를 Flow로 변환하여 검증 완료 대기
        TurnstileManager.verificationLiveData.asFlow()
            .first { it == true }
    }
    // 검증 완료 → matchDetails API 호출
    repository.getMatchDetail(matchId)
}
```

### 엣지 케이스 처리

| 시나리오 | 대응 방안 |
|----------|----------|
| Turnstile 검증 실패 | 3초 후 WebView reload 자동 재시도 (최대 3회) |
| 사용자가 matchDetail에 빠르게 진입 | verificationLiveData 관찰 → 검증 완료 후 자동 호출 |
| 24시간 후 토큰 만료 | isExpired() 체크 → refreshIfNeeded() 호출 |

---

## Step 2 — core-network: API 엔드포인트

```kotlin
// FootballApiService.kt에 추가
// 주의: 이 호출은 Turnstile 쿠키가 있어야 200 반환
@GET("api/matchDetails")
suspend fun getMatchDetail(
    @Query("matchId") matchId: Int,
): FixtureDetailResponseDto  // FotMob은 ApiResponse 래퍼 없음
```

---

## Step 3 — core-model: Domain Model

### MatchDetail.kt

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

### MatchEvent.kt

```kotlin
package com.chase1st.feetballfootball.core.model

data class MatchEvent(
    val minute: Int,
    val extraMinute: Int?,
    val team: Team,
    val playerName: String,
    val assistName: String?,
    val type: EventType,
    val detail: String,
)

// 참조: FixtureDetailEventsFragment.kt의 type+detail 분기
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
    ;

    companion object {
        fun from(type: String, detail: String): EventType = when {
            type == "Goal" && detail == "Normal Goal" -> GOAL
            type == "Goal" && detail == "Own Goal" -> OWN_GOAL
            type == "Goal" && detail == "Penalty" -> PENALTY_GOAL
            type == "Goal" && detail == "Missed Penalty" -> MISSED_PENALTY
            type == "Card" && detail == "Yellow Card" -> YELLOW_CARD
            type == "Card" && detail == "Red Card" -> RED_CARD
            type == "Card" && detail.contains("Yellow") -> SECOND_YELLOW
            type == "subst" -> SUBSTITUTION
            type == "Var" -> VAR
            else -> GOAL
        }
    }
}
```

### MatchLineups.kt

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

### MatchStatistics.kt

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

---

## Step 4 — core-domain: UseCase

### GetFixtureDetailUseCase.kt

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

### FixtureRepository 인터페이스 업데이트

```kotlin
// core-domain/repository/FixtureRepository.kt에 추가
fun getFixtureDetail(fixtureId: Int): Flow<MatchDetail>
```

---

## Step 5 — core-data: Mapper (가장 복잡)

### FixtureDetailMapper.kt

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

---

## Step 6 — feature-fixture-detail: ViewModel + UiState

### FixtureDetailUiState.kt

```kotlin
package com.chase1st.feetballfootball.feature.fixturedetail

import com.chase1st.feetballfootball.core.model.MatchDetail

sealed interface FixtureDetailUiState {
    data object Loading : FixtureDetailUiState
    data class Success(val detail: MatchDetail) : FixtureDetailUiState
    data class Error(val message: String) : FixtureDetailUiState
}
```

### FixtureDetailViewModel.kt

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
        // Turnstile 검증 대기
        if (!TurnstileManager.isVerified || TurnstileManager.isExpired()) {
            // 검증 완료 대기 (LiveData → Flow 변환)
            TurnstileManager.verificationLiveData.asFlow()
                .first { it == true }
        }
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

---

## Step 7 (Phase 5-A) — EventsTab

```kotlin
// feature/feature-fixture-detail/src/main/kotlin/.../feature/fixturedetail/component/EventsTab.kt
// 참조: FixtureDetailEventsFragment.kt (168줄)
// - 이벤트 타입 8종 아이콘 분기
// - 시간 표시 (elapsed + extra)
// - 홈/어웨이 배치 (팀 ID로 좌/우 결정)
// 상세 구현은 LazyColumn + MatchEvent 데이터 기반
```

**핵심 로직:**
- `EventType`별 아이콘 표시 (Icon 또는 이미지)
- 홈팀 이벤트: 왼쪽 정렬, 어웨이팀: 오른쪽 정렬
- 골: 선수명 + 어시스트 표시
- 교체: 교체 IN 선수 (초록) + OUT 선수 (빨강)
- 시간: `elapsed'` 또는 `elapsed+extra'`

---

## Step 8 (Phase 5-A) — StatisticsTab

```kotlin
// feature/feature-fixture-detail/src/main/kotlin/.../feature/fixturedetail/component/StatisticsTab.kt
// 참조: FixtureDetailStatisticsFragment.kt (256줄)
// 핵심: 15개 통계 항목을 LinearProgressIndicator로 비교 표시
```

**핵심 로직:**
- 각 통계 항목: 홈 값 | ProgressBar | 어웨이 값
- `Ball Possession`: 퍼센트 파싱 (예: "64%" → 64)
- `Shots on Goal` / `Total Shots`: 슈팅 정확도 계산
- 팀 컬러를 ProgressBar 색상에 적용 (참조: 흰색이면 검정으로 대체)
- `LinearProgressIndicator`의 `progress`를 home/(home+away) 비율로 설정

---

## Step 9 (Phase 5-A) — LineupTab

```kotlin
// feature/feature-fixture-detail/src/main/kotlin/.../feature/fixturedetail/component/LineupTab.kt
// 참조: FixtureDetailLineupFragment.kt (170줄)
// 가장 복잡한 탭: 포메이션 그리드 표현
```

**핵심 로직:**
- `startingXI`를 `gridRow`로 그룹핑 → 각 행에 선수 배치
- 어웨이팀은 행 순서 역전 (참조: reverse layout)
- `Box` + `Modifier.offset`으로 그리드 위치 표현 또는 `Column` of `Row`
- 선수 카드: 번호 + 이름 + 레이팅 (≥7.0 초록, <7.0 주황, null이면 표시 안함)
- 교체 선수 목록: 별도 LazyRow 또는 Column
- 감독 정보: 사진 + 이름
- 포메이션 문자열 표시 (예: "4-3-3")
- 배경색: 필드 초록 (StadiumGreen / FormationGreen)

---

## Step 10 (Phase 5-B) — FixtureDetailScreen 조립

```kotlin
// feature/feature-fixture-detail/src/main/kotlin/.../feature/fixturedetail/FixtureDetailScreen.kt
// 참조: FixtureDetailFragment.kt (268줄)
```

**핵심 구조:**

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

### MatchHeader (골 스코어러 포함)

```kotlin
// 참조: FixtureDetailFragment.kt의 골 스코어러 표시 로직
// - 홈팀 골 스코어러: 왼쪽
- 어웨이팀 골 스코어러: 오른쪽
// - 시간 표시: "45'" 또는 "90+3'"
// - 페널티: "(P)" 표시
// - 자책골: "(OG)" 표시
```

---

## ★ Slice 5 완료 검증

**체크리스트:**

- [ ] Turnstile 인증: 앱 시작 시 invisible WebView로 검증 완료
- [ ] matchDetails API: Turnstile 쿠키 포함 시 200 OK 반환
- [ ] 경기 선택 → 상세 화면 진입
- [ ] 매치 헤더: 팀 로고, 이름, 스코어, 골 스코어러 표시
- [ ] 이벤트 탭: 타임라인 표시 (골/카드/교체/VAR)
- [ ] 라인업 탭: 포메이션 표시, 선수 카드, 감독, 교체 선수
- [ ] 통계 탭: 15개 항목 비교 ProgressBar
- [ ] 데이터 없는 탭 자동 숨김 (연기/취소된 경기)
- [ ] 접히는 TopAppBar 동작
- [ ] 뒤로가기 동작
- [ ] `git commit -m "feat: Slice 5 경기 상세 화면 구현 (이벤트/라인업/통계)"`
