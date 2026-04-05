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
import androidx.hilt.navigation.compose.hiltViewModel
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
