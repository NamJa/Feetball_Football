# Stage 2 / Slice 3 — 경기 일정 화면

> ⏱ 예상 소요 시간: 3시간 | 난이도: ★★☆ | 선행 조건: Slice 2 완료 (API 연동 패턴 확립: DTO → Domain → Repository → UseCase → ViewModel → UI)

---

## 이 Codelab에서 배우는 것

- Slice 2에서 확립한 **수직 스택 패턴**을 새 feature에 반복 적용하는 방법
- `flatMapLatest`를 사용한 **반응형 날짜 선택** (날짜 변경 시 자동 API 재호출)
- `Flow.map`을 사용한 **리그별 그룹핑** (UseCase 레벨 비즈니스 로직)
- `enum class`를 활용한 **경기 상태 매핑** (FotMob의 finished, started, cancelled boolean 필드 기반)
- Compose `LazyColumn`의 `stickyHeader`를 사용한 **리그 헤더 고정**
- `DateTimeFormatter`를 사용한 날짜/시간 포맷팅

---

## 완성 후 결과물

- 날짜 선택 UI (이전/다음 날짜 버튼)
- 리그별로 그룹핑된 경기 목록 (stickyHeader로 리그 헤더 고정)
- 경기 상태별 표시: 예정(시간), 진행중(스코어+경과시간), 종료(스코어), 연기/취소
- 종료/라이브 경기만 클릭 가능
- 경기 없는 날짜에 빈 상태 표시
- Loading → Success / Empty / Error 상태 전환

---

## Step 1 — core-network: DTO 작성

### 목표
> FotMob `/api/data/matches` 엔드포인트의 응답 구조에 맞는 DTO를 정의합니다. 응답은 `leagues[]` > `matches[]` 형태의 중첩 구조입니다.

### 작업 내용

FotMob `/api/data/matches` API 응답의 구조:
```
{
  "leagues": [
    {
      "ccode": "ENG", "id": 47, "primaryId": 47, "name": "Premier League",
      "matches": [
        {
          "id": 4506355, "leagueId": 47, "time": "15.03.2026 21:00",
          "home": { "id": 8456, "score": 1, "name": "Man City", "longName": "Manchester City" },
          "away": { "id": 9825, "score": 2, "name": "Arsenal", "longName": "Arsenal" },
          "status": { "utcTime": "...", "finished": true, "started": true, "cancelled": false, "scoreStr": "1 - 2", "reason": { "short": "FT", "long": "Full-Time" } },
          "timeTS": 1710504000000
        }
      ],
      "internalRank": 23
    }
  ],
  "date": "20260315"
}
```

이 구조를 반영하여 6개의 DTO 클래스를 정의합니다.

> 💡 **Tip:** `MatchesDayDto.kt` 하나에 모든 DTO를 모아두면 관련 클래스를 한눈에 볼 수 있습니다. API 응답 구조가 복잡할수록 한 파일에 모으는 것이 유지보수에 유리합니다.

> ⚠️ **주의:** `MatchTeamDto.score`는 nullable입니다 (아직 시작하지 않은 경기는 null). `MatchStatusDto.reason`과 `scoreStr`도 nullable이므로 기본값을 설정합니다. FotMob은 팀 로고 URL을 응답에 포함하지 않으며, `https://images.fotmob.com/image_resources/logo/teamlogo/{teamId}.png` 형식으로 직접 구성합니다.

**파일 경로:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/MatchesDayDto.kt`

```kotlin
// core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/MatchesDayDto.kt
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

### ✅ 검증
- [ ] `MatchesDayResponseDto`에 `leagues`, `date` 필드가 있는지 확인
- [ ] `MatchesDayLeagueDto`에 `ccode`, `id`, `name`, `matches` 필드가 있는지 확인
- [ ] `MatchStatusDto`에 `finished`, `started`, `cancelled` boolean 필드가 있는지 확인
- [ ] `MatchTeamDto.score`가 nullable인지 확인 (아직 시작하지 않은 경기는 null)
- [ ] 모든 DTO에 `@Serializable` 어노테이션이 있는지 확인
- [ ] `core-network` 모듈 빌드 성공 확인

---

## Step 2 — core-network: API 엔드포인트 추가

### 목표
> `FootballApiService`에 날짜별 경기 목록 엔드포인트를 추가합니다.

### 작업 내용

FotMob `/api/data/matches` 엔드포인트는 `date` 파라미터로 특정 날짜의 경기를 조회합니다. `timezone` 파라미터를 `Asia/Seoul`로 설정하여 한국 시간 기준으로 데이터를 받습니다. `ccode3` 파라미터로 국가 코드를 지정할 수 있습니다.

> 💡 **Tip:** `timezone`과 `ccode3` 파라미터에 기본값을 설정하면 호출 시 생략할 수 있습니다. 다른 타임존이나 국가가 필요하면 오버라이드하면 됩니다.

> ⚠️ **주의:** FotMob은 `ApiResponse<T>` 래퍼를 사용하지 않고 직접 `MatchesDayResponseDto`를 반환합니다. API-Sports v3와 달리 응답 래핑이 없습니다.

**파일 경로:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/api/FootballApiService.kt` (기존 파일에 추가)

```kotlin
// FootballApiService.kt에 추가
@GET("api/data/matches")
suspend fun getMatchesByDate(
    @Query("date") date: String,           // YYYYMMDD 형식
    @Query("timezone") timezone: String = "Asia/Seoul",
    @Query("ccode3") ccode3: String = "KOR",
): MatchesDayResponseDto
```

> ⚠️ **주의:** `date` 파라미터는 `"20260315"` 형식 (YYYYMMDD)입니다. `LocalDate.toString()`은 ISO 8601 형식(`2026-03-15`)을 반환하므로, `DateTimeFormatter.ofPattern("yyyyMMdd")`로 변환하여 전달해야 합니다.

### ✅ 검증
- [ ] `getMatchesByDate()`가 `FootballApiService`에 추가되었는지 확인
- [ ] `suspend fun`으로 선언되어 있는지 확인
- [ ] `timezone` 기본값이 `"Asia/Seoul"`인지 확인
- [ ] `ccode3` 기본값이 `"KOR"`인지 확인
- [ ] 반환 타입이 `MatchesDayResponseDto`인지 확인 (ApiResponse 래퍼 없음)

---

## Step 3 — core-model: Domain Model

### 목표
> 경기 일정의 도메인 모델 `Fixture`와 경기 상태를 나타내는 `MatchStatus` enum을 정의합니다.

### 작업 내용

`MatchStatus`는 FotMob의 경기 상태 boolean 필드(`finished`, `started`, `cancelled`)와 `reason.short` 값을 Kotlin enum으로 매핑합니다. 기존 `FixtureRecyclerViewAdapter.kt`와 `FixtureDetailFragment.kt`에 분산되어 있던 상태 판단 로직을 하나의 enum에 모읍니다.

> 💡 **Tip:** `MatchStatus`의 computed property (`isFinished`, `isLive`, `isClickable`)를 사용하면 UI에서 `when` 분기를 줄일 수 있습니다. 예: `if (fixture.status.isLive)` 로 간결하게 판단할 수 있습니다.

> ⚠️ **주의:** `Fixture`의 `league` 필드는 Slice 1에서 만든 `LeagueInfo`를 재사용합니다. `groupBy { it.league }`로 리그별 그룹핑에 사용되므로, `LeagueInfo`에 `equals()`/`hashCode()`가 올바르게 동작해야 합니다 (data class이므로 자동 생성됨).

**파일 경로:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/Fixture.kt`

```kotlin
// core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/Fixture.kt
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

### ✅ 검증
- [ ] `Fixture` data class에 id, date, status, elapsed, venue, league, homeTeam, awayTeam, homeGoals, awayGoals 10개 필드가 있는지 확인
- [ ] `MatchStatus`에 8개 상태가 정의되어 있는지 확인 (NOT_STARTED, FIRST_HALF, HALF_TIME, SECOND_HALF, FINISHED, POSTPONED, CANCELLED, LIVE)
- [ ] `fromFotMobStatus()`가 `finished=true`일 때 `FINISHED`를 반환하는지 확인
- [ ] `fromFotMobStatus()`가 `cancelled=true`일 때 `CANCELLED`를 반환하는지 확인
- [ ] `fromFotMobStatus()`가 `reason.short="PP"`일 때 `POSTPONED`를 반환하는지 확인
- [ ] `fromFotMobStatus()`가 `reason.short="HT"`일 때 `HALF_TIME`을 반환하는지 확인
- [ ] `FINISHED.isFinished`가 `true`인지 확인
- [ ] `LIVE.isLive`가 `true`인지 확인
- [ ] `NOT_STARTED.isClickable`이 `false`인지 확인
- [ ] `core-model` 모듈 빌드 성공 확인

---

## Step 4 — core-domain: Repository 인터페이스 + UseCase

### 목표
> `FixtureRepository` 인터페이스와 `GetFixturesByDateUseCase`를 정의합니다. UseCase에서 리그별 그룹핑 로직을 수행합니다.

### 작업 내용

UseCase는 단순 위임이 아니라 **비즈니스 로직**을 포함합니다. 여기서는 경기 목록을 리그별로 그룹핑(`groupBy`)하는 로직이 UseCase에 들어갑니다.

> 💡 **Tip:** `groupBy { it.league }`의 결과는 `Map<LeagueInfo, List<Fixture>>`입니다. `LeagueInfo`가 data class이므로 `equals()`가 자동 구현되어 같은 리그의 경기들이 올바르게 그룹핑됩니다.

**파일 경로:** `core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/repository/FixtureRepository.kt`

```kotlin
// core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/repository/FixtureRepository.kt
package com.chase1st.feetballfootball.core.domain.repository

import com.chase1st.feetballfootball.core.model.Fixture
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface FixtureRepository {
    fun getFixturesByDate(date: LocalDate): Flow<List<Fixture>>
}
```

**파일 경로:** `core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/usecase/GetFixturesByDateUseCase.kt`

```kotlin
// core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/usecase/GetFixturesByDateUseCase.kt
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

> ⚠️ **주의:** Repository는 `Flow<List<Fixture>>`를 반환하고, UseCase가 `Flow<Map<LeagueInfo, List<Fixture>>>`로 변환합니다. 이렇게 하면 Repository는 단순히 데이터를 가져오고, 비즈니스 로직(그룹핑)은 UseCase가 담당합니다.

### ✅ 검증
- [ ] `FixtureRepository.getFixturesByDate()`가 `LocalDate`를 파라미터로 받는지 확인
- [ ] `GetFixturesByDateUseCase`가 `operator fun invoke()`로 선언되어 있는지 확인
- [ ] UseCase의 반환 타입이 `Flow<Map<LeagueInfo, List<Fixture>>>`인지 확인
- [ ] `core-domain` 모듈 빌드 성공 확인

---

## Step 5 — core-data: Mapper + Repository 구현

### 목표
> `FixtureMapper`로 DTO를 Domain Model로 변환하고, `FixtureRepositoryImpl`에서 API 호출 + 필터링 + 정렬을 수행합니다. `DataModule`에 Repository 바인딩을 추가합니다.

### 작업 내용

`FixtureMapper`에는 두 가지 핵심 로직이 있습니다:
1. **필터링:** `SupportedLeagues.ALL_LEAGUE_IDS`에 포함된 리그만 남깁니다 (FotMob API는 전체 리그의 경기를 반환하므로)
2. **정렬:** 경기 시작 시간 순으로 정렬합니다

FotMob 응답은 `leagues[]` > `matches[]` 구조이므로, `flatMap`으로 모든 리그의 경기를 하나의 리스트로 펼칩니다.

> ⚠️ **주의:** FotMob `/api/data/matches?date=20260315` API는 해당 날짜의 **모든 리그** 경기를 반환합니다. `SupportedLeagues.ALL_LEAGUE_IDS`로 리그를 필터링하지 않으면 불필요한 데이터가 UI에 노출됩니다.

> ⚠️ **주의:** FotMob은 팀 로고와 리그 로고 URL을 응답에 포함하지 않습니다. 대신 고정된 URL 패턴으로 직접 구성합니다:
> - 팀 로고: `https://images.fotmob.com/image_resources/logo/teamlogo/{teamId}.png`
> - 리그 로고: `https://images.fotmob.com/image_resources/logo/leaguelogo/dark/{leagueId}.png`

**파일 경로:** `core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/mapper/FixtureMapper.kt`

```kotlin
// core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/mapper/FixtureMapper.kt
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

> 💡 **Tip:** `DateTimeFormatter.ISO_OFFSET_DATE_TIME`은 `"2026-03-15T12:00:00.000Z"` 형식을 파싱합니다. FotMob이 `status.utcTime`에서 이 형식으로 시간을 반환합니다. 파싱 실패 시 `LocalDateTime.now()`를 기본값으로 사용하여 크래시를 방지합니다. 팀 이름은 `longName`을 우선 사용하고, 비어있으면 `name`을 폴백으로 사용합니다.

**파일 경로:** `core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/repository/FixtureRepositoryImpl.kt`

```kotlin
// core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/repository/FixtureRepositoryImpl.kt
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

**파일 경로:** `core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/di/DataModule.kt` (기존 파일에 추가)

Slice 2에서 만든 `DataModule`에 `FixtureRepository` 바인딩을 추가합니다.

```kotlin
// core-data/di/DataModule.kt에 추가
@Binds
abstract fun bindFixtureRepository(impl: FixtureRepositoryImpl): FixtureRepository
```

전체 `DataModule.kt`는 다음과 같습니다:

```kotlin
// core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/di/DataModule.kt
package com.chase1st.feetballfootball.core.data.di

import com.chase1st.feetballfootball.core.data.repository.FixtureRepositoryImpl
import com.chase1st.feetballfootball.core.data.repository.LeagueRepositoryImpl
import com.chase1st.feetballfootball.core.domain.repository.FixtureRepository
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

    @Binds
    abstract fun bindFixtureRepository(impl: FixtureRepositoryImpl): FixtureRepository
}
```

### ✅ 검증
- [ ] `FixtureMapper`에 `@Inject constructor()`가 있는지 확인
- [ ] `mapMatchesDayResponse()`가 `leagues`를 `SupportedLeagues.ALL_LEAGUE_IDS`로 필터링하는지 확인
- [ ] `flatMap`으로 모든 리그의 경기를 하나의 리스트로 펼치는지 확인
- [ ] `parseUtcTime()`이 ISO_OFFSET_DATE_TIME 형식을 파싱하는지 확인
- [ ] `parseUtcTime()` 실패 시 `LocalDateTime.now()`를 반환하는지 확인 (크래시 방지)
- [ ] 팀 로고 URL이 `https://images.fotmob.com/image_resources/logo/teamlogo/{teamId}.png` 형식인지 확인
- [ ] 리그 로고 URL이 `https://images.fotmob.com/image_resources/logo/leaguelogo/dark/{leagueId}.png` 형식인지 확인
- [ ] `FixtureRepositoryImpl`이 날짜를 YYYYMMDD 형식으로 변환하는지 확인
- [ ] `sortedBy { it.date }`로 시간순 정렬하는지 확인
- [ ] `DataModule`에 `bindFixtureRepository()`가 추가되었는지 확인
- [ ] `core-data` 모듈 빌드 성공 확인

---

## Step 6 — feature-fixture: ViewModel + UiState

### 목표
> 경기 일정의 ViewModel과 UI 상태를 정의합니다. 날짜 변경 시 `flatMapLatest`로 자동 API 재호출합니다.

### 작업 내용

`FixtureViewModel`의 핵심은 `selectedDate` StateFlow를 `flatMapLatest`로 변환하는 것입니다:
- 사용자가 날짜를 변경하면 `selectedDate`에 새 값이 emit됩니다
- `flatMapLatest`는 이전 API 호출을 취소하고 새 날짜로 재호출합니다
- 결과가 비어있으면 `Empty` 상태, 아니면 `Success` 상태를 emit합니다

> 💡 **Tip:** `flatMapLatest`는 새 값이 emit되면 이전 flow를 **자동 취소**합니다. 사용자가 빠르게 날짜를 여러 번 변경해도 마지막 날짜의 API 호출만 완료됩니다.

**파일 경로:** `feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/FixtureUiState.kt`

```kotlin
// feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/FixtureUiState.kt
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

> 💡 **Tip:** `FixtureEvent`를 sealed interface로 정의하면 ViewModel의 `onEvent()` 함수에서 모든 이벤트를 `when`으로 처리할 수 있습니다. MVI 패턴의 단방향 데이터 흐름입니다.

**파일 경로:** `feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/FixtureViewModel.kt`

```kotlin
// feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/FixtureViewModel.kt
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

> ⚠️ **주의:** `SharingStarted.WhileSubscribed(5_000)`은 마지막 구독자가 사라진 후 5초간 flow를 유지합니다. 화면 회전 등으로 잠시 구독이 끊겨도 API를 다시 호출하지 않습니다.

### ✅ 검증
- [ ] `FixtureUiState`에 `Loading`, `Success`, `Empty`, `Error` 4개 상태가 있는지 확인
- [ ] `FixtureEvent`에 `SelectDate`, `SelectFixture` 2개 이벤트가 있는지 확인
- [ ] `FixtureViewModel`이 `@HiltViewModel`로 선언되어 있는지 확인
- [ ] `selectedDate`가 `MutableStateFlow(LocalDate.now())`로 초기화되는지 확인
- [ ] `flatMapLatest`로 날짜 변경 시 자동 재호출하는지 확인
- [ ] `onEvent()`에서 `SelectDate` 이벤트를 처리하는지 확인

---

## Step 7 — feature-fixture: Compose UI

### 목표
> 경기 일정의 Compose UI를 구현합니다. 날짜 선택기, 경기 아이템, 전체 화면 3개의 Composable을 만듭니다.

### 작업 내용

이 Step에서는 3개의 Composable 파일을 만듭니다:
1. **DateSelector.kt** — 이전/다음 날짜 버튼 + 현재 날짜 표시
2. **FixtureItem.kt** — 개별 경기 아이템 (홈팀 vs 원정팀 + 스코어/시간)
3. **FixtureScreen.kt** — 전체 화면 (DateSelector + 리그별 그룹 LazyColumn)

**파일 경로:** `feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/component/DateSelector.kt`

```kotlin
// feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/component/DateSelector.kt
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

> 💡 **Tip:** `DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)")`는 `"2026년 3월 15일 (일)"` 형태로 날짜를 포맷합니다. `(E)`는 요일 약어입니다. 한국어 로케일이 시스템 기본이면 자동으로 한국어로 표시됩니다.

**파일 경로:** `feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/component/FixtureItem.kt`

```kotlin
// feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/component/FixtureItem.kt
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

> 💡 **Tip:** `FixtureStatus` Composable은 경기 상태에 따라 4가지로 분기합니다:
> - **종료/라이브:** 스코어 표시 (라이브면 상태 텍스트를 빨간색으로 표시. FotMob 일별 매치 API에는 elapsed 분 정보가 없으므로 `displayText`를 사용)
> - **연기:** "연기됨" 텍스트
> - **취소:** "취소됨" 텍스트
> - **예정:** 시작 시간 (HH:mm 형식)

> ⚠️ **주의:** `clickable(enabled = fixture.status.isClickable)`로 클릭 가능 여부를 제어합니다. 아직 시작하지 않은 경기나 연기/취소된 경기는 클릭할 수 없습니다.

**파일 경로:** `feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/FixtureScreen.kt`

```kotlin
// feature/feature-fixture/src/main/kotlin/com/chase1st/feetballfootball/feature/fixture/FixtureScreen.kt
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

> 💡 **Tip:** `stickyHeader`는 스크롤 시 리그 헤더가 화면 상단에 고정됩니다. 기존 `FixtureFragment.kt`의 `LeagueFixtureHolder`와 동일한 UX를 제공합니다.

> ⚠️ **주의:** `LazyColumn` 안에서 `stickyHeader`를 사용하려면 `@OptIn(ExperimentalFoundationApi::class)`가 필요할 수 있습니다 (Compose 버전에 따라 다름). 빌드 시 경고가 나오면 추가하세요.

### ✅ 검증
- [ ] `DateSelector`에 이전/다음 날짜 버튼과 현재 날짜 텍스트가 있는지 확인
- [ ] `FixtureItem`에 홈팀 로고/이름, 스코어/시간, 원정팀 이름/로고가 표시되는지 확인
- [ ] `FixtureStatus`가 경기 상태별로 올바르게 분기하는지 확인:
  - 미시작 → 시작 시간 (HH:mm)
  - 종료(FT) → 스코어
  - 진행중(started) → 스코어 + 상태 텍스트 (빨간색)
  - 연기(PP) → "연기됨"
  - 취소(Canc) → "취소됨"
- [ ] `FixtureScreen`에서 Loading/Error/Empty/Success 4개 상태를 모두 처리하는지 확인
- [ ] `FixtureList`에서 리그별 `stickyHeader`가 있는지 확인
- [ ] `feature-fixture` 모듈 빌드 성공 확인

---

## Step 8 — 빌드 및 최종 검증

### 목표
> 전체 빌드를 수행하고 에뮬레이터/기기에서 경기 일정 화면을 검증합니다.

### 작업 내용

```bash
./gradlew assembleDebug
# 앱 실행 → 에뮬레이터/기기
```

### ✅ 검증
- [ ] 앱 실행 시 오늘 날짜 경기 목록 표시
- [ ] 이전/다음 날짜 버튼 동작 (API 재호출)
- [ ] 리그별 그룹핑 + stickyHeader 표시
- [ ] 경기 상태별 분기: 시간(미시작) / 스코어(FT) / 라이브(started) / 연기(PP) / 취소(Canc)
- [ ] 종료/라이브 경기만 클릭 가능
- [ ] 경기 없는 날짜에 빈 상태 표시
- [ ] `git commit -m "feat: Slice 3 경기 일정 화면 구현"`

---

## 🎉 Slice 3 완료!

축하합니다! 앱의 메인 화면을 완성했습니다.

**이 Slice에서 달성한 것:**
- Slice 2에서 확립한 수직 스택 패턴을 새 feature에 성공적으로 반복 적용했습니다
- `flatMapLatest`를 사용한 반응형 날짜 선택을 구현했습니다
- `MatchStatus` enum으로 경기 상태 로직을 깔끔하게 캡슐화했습니다
- Compose `stickyHeader`로 리그별 그룹핑 UI를 구현했습니다
- MVI 패턴(`FixtureEvent`)으로 단방향 데이터 흐름을 적용했습니다

**Stage 2 / Slice 1~3 완료 후 앱 상태:**
- 리그 선택 화면 (정적 데이터)
- 리그 순위 화면 (팀 순위 + 개인 순위, API 연동)
- 경기 일정 화면 (날짜 선택 + 리그별 그룹핑 + 상태별 표시, API 연동)

**다음 단계:** Navigation 3 통합으로 화면 간 이동을 구현하거나, 경기 상세 화면(Slice 4)을 진행합니다.
