# Stage 2 / Slice 5 — 경기 상세 + 3탭 (이벤트/라인업/통계)

> ⏱ 예상 소요 시간: 8시간 | 난이도: ★★★ | 선행 조건: Slice 3 완료 (Fixture Domain Model 존재)

---

## 이 Codelab에서 배우는 것

- **SofaScore 다중 API 호출**: 하나의 화면에 여러 엔드포인트를 병렬 호출하여 데이터 조합
- **API 응답 → DTO → Domain Model** 전체 레이어 매핑 (가장 복잡한 Mapper 구현)
- **`kotlinx.serialization`** 으로 중첩 JSON 구조 역직렬화 (이벤트, 라인업, 통계, 선수 레이팅)
- **Compose `HorizontalPager` + `TabRow`** 로 3개 서브탭 구현
- **`LargeTopAppBar` + `exitUntilCollapsedScrollBehavior`** 로 접히는 헤더 구현
- **Hilt `SavedStateHandle`** 을 통한 Navigation argument 주입
- **포메이션 배치**: formation 문자열 기반 시각적 배치
- **`LinearProgressIndicator`** 로 홈/어웨이 통계 비교 UI
- **조건부 탭 표시**: 데이터 유무에 따라 탭을 동적으로 구성
- **`coroutineScope { async {} }`**: 여러 API를 병렬로 호출하는 패턴

---

## 완성 후 결과물

- SofaScore의 여러 API 엔드포인트(event, incidents, lineups, statistics)를 병렬 호출하여 경기 상세 데이터를 조합합니다.
- 경기 목록에서 경기를 선택하면 상세 화면으로 이동합니다.
- 매치 헤더에 팀 로고, 이름, 스코어, 골 스코어러가 표시됩니다.
- 이벤트 탭: 골/카드/교체/VAR 등 경기 이벤트가 타임라인으로 표시됩니다.
- 라인업 탭: 포메이션 그리드, 선수 카드(번호/이름/레이팅), 감독, 교체 선수가 표시됩니다.
- 통계 탭: 그룹별 통계 항목이 ProgressBar로 홈/어웨이 비교 표시됩니다.
- 데이터가 없는 탭(연기/취소된 경기)은 자동으로 숨겨집니다.
- TopAppBar가 스크롤에 따라 접히고 펼쳐집니다.

---

## 실행 전략

이 Slice는 프로젝트에서 가장 복잡한 화면입니다. 내부적으로 두 Phase로 나눠서 진행합니다:

```
Phase 5-A: 하위 탭 3개 독립 구현 (병렬 가능)
  ├── EventsTab    (Step 11)
  ├── StatisticsTab (Step 12)
  └── LineupTab    (Step 13)

Phase 5-B: FixtureDetailScreen에서 조립 (Step 14)
```

Step 1~4는 SofaScore 다중 엔드포인트별 DTO, Step 5는 API 엔드포인트(4개 suspend 함수), Step 6~10은 하위 레이어(Domain Model, Mapper, UseCase, ViewModel)를 순서대로 구축합니다.

---

## Step 1 — core-network: SofaScore 경기 상세 DTO 작성

### 목표

> SofaScore의 여러 경기 상세 엔드포인트(`/api/v1/event/{eventId}`, `/api/v1/event/{eventId}/incidents`, `/api/v1/event/{eventId}/lineups`, `/api/v1/event/{eventId}/statistics`)의 응답을 역직렬화하기 위한 DTO를 정의합니다. SofaScore는 경기 상세 데이터를 **여러 엔드포인트로 분리**하여 제공하므로, 각 엔드포인트별로 별도의 응답 DTO가 필요합니다.

### 작업 내용

**파일:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/FixtureDetailDto.kt`

이 파일을 새로 생성합니다.

```kotlin
package com.chase1st.feetballfootball.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// SofaScore GET api/v1/event/{eventId} 응답
@Serializable
data class EventResponseDto(
    @SerialName("event") val event: SofaEventDto,
)

@Serializable
data class SofaEventDto(
    @SerialName("id") val id: Int,
    @SerialName("tournament") val tournament: SofaTournamentDto? = null,
    @SerialName("homeTeam") val homeTeam: SofaTeamDto,
    @SerialName("awayTeam") val awayTeam: SofaTeamDto,
    @SerialName("homeScore") val homeScore: SofaScoreDto? = null,
    @SerialName("awayScore") val awayScore: SofaScoreDto? = null,
    @SerialName("status") val status: SofaStatusDto? = null,
    @SerialName("startTimestamp") val startTimestamp: Long? = null,
    @SerialName("venue") val venue: SofaVenueDto? = null,
)

@Serializable
data class SofaTournamentDto(
    @SerialName("uniqueTournament") val uniqueTournament: SofaUniqueTournamentDto? = null,
)

@Serializable
data class SofaUniqueTournamentDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String? = null,
)

@Serializable
data class SofaTeamDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String? = null,
)

@Serializable
data class SofaScoreDto(
    @SerialName("current") val current: Int? = null,
)

@Serializable
data class SofaStatusDto(
    @SerialName("code") val code: Int? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("type") val type: String? = null,
)

@Serializable
data class SofaVenueDto(
    @SerialName("city") val city: SofaCityDto? = null,
    @SerialName("stadium") val stadium: SofaStadiumDto? = null,
)

@Serializable
data class SofaCityDto(
    @SerialName("name") val name: String? = null,
)

@Serializable
data class SofaStadiumDto(
    @SerialName("name") val name: String? = null,
)
```

**WHY:** SofaScore는 하나의 API로 모든 경기 상세 데이터를 반환하지 않습니다. 대신 `event`, `incidents`, `lineups`, `statistics` 등 **여러 엔드포인트로 분리**하여 제공합니다. 따라서 각 엔드포인트별로 별도의 응답 DTO를 정의해야 합니다.

**HOW:** nullable 필드와 기본값 `null`을 적극 사용하여, 경기 상태에 따라 일부 데이터가 없는 경우에도 역직렬화가 실패하지 않도록 합니다. 팀 로고 URL은 DTO에 포함되지 않고, `https://img.sofascore.com/api/v1/team/{id}/image` 패턴으로 별도 구성합니다.

> 💡 **Tip:** SofaScore 이미지 URL 패턴:
> - 팀 로고: `https://img.sofascore.com/api/v1/team/{id}/image`
> - 리그 로고: `https://img.sofascore.com/api/v1/unique-tournament/{id}/image`
> - 선수 사진: `https://img.sofascore.com/api/v1/player/{id}/image`

### ✅ 검증

- [ ] 모든 DTO 클래스에 `@Serializable` 어노테이션이 있다
- [ ] nullable 필드에 기본값 `null`이 설정되어 있다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 2 — core-network: IncidentDto 작성 (SofaScore Incidents)

### 목표

> SofaScore `/api/v1/event/{eventId}/incidents` 엔드포인트의 응답을 역직렬화하기 위한 DTO를 정의합니다. SofaScore는 골, 카드, 교체, 기간(전반/후반) 등을 `incidents` 배열로 통합하여 제공합니다.

### 작업 내용

**파일:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/IncidentDto.kt`

이 파일을 새로 생성합니다.

```kotlin
package com.chase1st.feetballfootball.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// SofaScore GET api/v1/event/{eventId}/incidents 응답
@Serializable
data class IncidentsResponseDto(
    @SerialName("incidents") val incidents: List<IncidentDto> = emptyList(),
)

@Serializable
data class IncidentDto(
    @SerialName("incidentType") val incidentType: String,        // "goal", "card", "substitution", "period", "injuryTime", "var"
    @SerialName("time") val time: Int? = null,
    @SerialName("addedTime") val addedTime: Int? = null,
    @SerialName("player") val player: IncidentPlayerDto? = null,
    @SerialName("assist1") val assist1: IncidentPlayerDto? = null,
    @SerialName("playerIn") val playerIn: IncidentPlayerDto? = null,   // 교체 들어온 선수
    @SerialName("playerOut") val playerOut: IncidentPlayerDto? = null,  // 교체 나간 선수
    @SerialName("isHome") val isHome: Boolean? = null,
    @SerialName("homeScore") val homeScore: Int? = null,
    @SerialName("awayScore") val awayScore: Int? = null,
    @SerialName("incidentClass") val incidentClass: String? = null,    // "regular", "yellow", "red", "yellowRed", "ownGoal", "penalty", "missedPenalty"
    @SerialName("text") val text: String? = null,                      // "HT", "FT" 등 period 이벤트에서 사용
)

@Serializable
data class IncidentPlayerDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("shortName") val shortName: String? = null,
)
```

**WHY:** SofaScore의 incidents 응답은 모든 경기 이벤트를 하나의 배열에 통합합니다. `incidentType`으로 이벤트 종류를 구분하고, `incidentClass`로 세부 유형(일반골/자책골/페널티, 옐로/레드 카드 등)을 구분합니다.

**HOW:**
- `incidentType` + `incidentClass` 조합으로 이벤트 유형을 구분합니다. 예: `incidentType="goal"` + `incidentClass="ownGoal"` → 자책골
- `player`는 골/카드 이벤트의 주체 선수입니다.
- `assist1`은 어시스트 선수입니다. 어시스트가 없는 이벤트에서는 `null`입니다.
- `playerIn`/`playerOut`은 교체 이벤트(`incidentType="substitution"`)에서만 사용됩니다.
- `addedTime`은 추가 시간(예: 90+3분의 3)을 나타냅니다. 정규 시간에는 `null`입니다.
- `isHome`이 `null`인 경우는 `period` 이벤트(전반/후반 구분선) 등입니다.

> ⚠️ **주의:** SofaScore의 `incidentType` + `incidentClass` 매핑은 Step 6의 Domain Model(`EventType.from()`)에서 처리합니다. 주요 매핑:
> - `incidentType="goal"` + `incidentClass="regular"` → 일반 골
> - `incidentType="goal"` + `incidentClass="ownGoal"` → 자책골
> - `incidentType="goal"` + `incidentClass="penalty"` → 페널티 골
> - `incidentType="card"` + `incidentClass="yellow"` → 옐로 카드
> - `incidentType="card"` + `incidentClass="red"` → 레드 카드
> - `incidentType="card"` + `incidentClass="yellowRed"` → 경고 누적 퇴장

### ✅ 검증

- [ ] 모든 DTO 클래스에 `@Serializable` 어노테이션이 있다
- [ ] nullable 필드에 기본값 `null`이 설정되어 있다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 3 — core-network: LineupsDto 작성 (SofaScore Lineups)

### 목표

> SofaScore `/api/v1/event/{eventId}/lineups` 엔드포인트의 응답을 역직렬화하기 위한 DTO를 정의합니다. SofaScore의 라인업은 `home`/`away` 객체로 분리되며, 각 팀의 `players` 배열에서 `substitute: boolean`으로 선발/교체를 구분합니다.

### 작업 내용

**파일:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/LineupDto.kt`

이 파일을 새로 생성합니다.

```kotlin
package com.chase1st.feetballfootball.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// SofaScore GET api/v1/event/{eventId}/lineups 응답
@Serializable
data class LineupsResponseDto(
    @SerialName("home") val home: TeamLineupDto? = null,
    @SerialName("away") val away: TeamLineupDto? = null,
    @SerialName("confirmed") val confirmed: Boolean? = null,
)

@Serializable
data class TeamLineupDto(
    @SerialName("players") val players: List<LineupPlayerEntryDto> = emptyList(),
    @SerialName("formation") val formation: String? = null,
    @SerialName("playerColor") val playerColor: LineupColorDto? = null,
    @SerialName("goalkeeperColor") val goalkeeperColor: LineupColorDto? = null,
)

@Serializable
data class LineupColorDto(
    @SerialName("primary") val primary: String? = null,
    @SerialName("number") val number: String? = null,
)

@Serializable
data class LineupPlayerEntryDto(
    @SerialName("player") val player: LineupPlayerInfoDto,
    @SerialName("shirtNumber") val shirtNumber: Int? = null,
    @SerialName("jerseyNumber") val jerseyNumber: String? = null,
    @SerialName("position") val position: String? = null,    // "G", "D", "M", "F"
    @SerialName("substitute") val substitute: Boolean = false,
    @SerialName("statistics") val statistics: LineupPlayerStatDto? = null,
    @SerialName("captain") val captain: Boolean? = null,
)

@Serializable
data class LineupPlayerInfoDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String? = null,
    @SerialName("shortName") val shortName: String? = null,
    @SerialName("position") val position: String? = null,    // "G", "D", "M", "F"
)

@Serializable
data class LineupPlayerStatDto(
    @SerialName("rating") val rating: Double? = null,
    @SerialName("minutesPlayed") val minutesPlayed: Int? = null,
)
```

**WHY:** SofaScore의 라인업 구조는 `home`/`away`로 팀이 분리되어 있으며, 각 팀의 `players` 배열에 모든 선수가 포함됩니다. `substitute: boolean` 필드로 선발(false)과 교체(true)를 구분합니다. `grid` 필드는 없으며, 포메이션 문자열에서 선수 배치를 유추합니다.

**HOW:**
- `substitute` 필드가 `false`이면 선발, `true`이면 교체 선수입니다.
- `position` 필드는 `"G"` (골키퍼), `"D"` (수비수), `"M"` (미드필더), `"F"` (공격수) 중 하나입니다.
- `playerColor.primary`는 팀 유니폼 색상 hex 값입니다 (예: `"#6CABDD"`). 포메이션 UI의 선수 원형 배경색에 사용합니다.
- `statistics.rating`은 선수의 경기 레이팅(예: 7.2)입니다. 별도 API 호출 없이 라인업 응답에 포함됩니다.
- `confirmed`가 `true`이면 라인업이 확정된 상태입니다.

> 💡 **Tip:** SofaScore는 `grid` 필드를 제공하지 않으므로, 포메이션 문자열(예: `"4-3-3"`)을 파싱하여 각 포지션별 선수 수를 결정하고, `position` 필드와 조합하여 선수 배치를 계산합니다. 이 로직은 Mapper(Step 8)에서 구현합니다.

### ✅ 검증

- [ ] 모든 DTO 클래스에 `@Serializable` 어노테이션이 있다
- [ ] nullable 필드에 기본값 `null` 또는 `emptyList()`가 설정되어 있다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 4 — core-network: StatisticsDto 작성 (SofaScore Statistics)

### 목표

> SofaScore `/api/v1/event/{eventId}/statistics` 엔드포인트의 응답을 역직렬화하기 위한 DTO를 정의합니다. SofaScore의 통계는 `period`(ALL, 1ST, 2ND)별로 분류되고, 각 period 내에서 `groups`로 그룹핑됩니다.

### 작업 내용

**파일:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/StatisticsDto.kt`

이 파일을 새로 생성합니다.

```kotlin
package com.chase1st.feetballfootball.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// SofaScore GET api/v1/event/{eventId}/statistics 응답
@Serializable
data class EventStatisticsResponseDto(
    @SerialName("statistics") val statistics: List<PeriodStatisticsDto> = emptyList(),
)

@Serializable
data class PeriodStatisticsDto(
    @SerialName("period") val period: String,             // "ALL", "1ST", "2ND"
    @SerialName("groups") val groups: List<StatGroupDto> = emptyList(),
)

@Serializable
data class StatGroupDto(
    @SerialName("groupName") val groupName: String,       // "Possession", "Shots", "TVData", etc.
    @SerialName("statisticsItems") val statisticsItems: List<StatItemDto> = emptyList(),
)

@Serializable
data class StatItemDto(
    @SerialName("name") val name: String,                 // "Ball possession", "Total shots", etc.
    @SerialName("home") val home: String? = null,         // "64%", "18"
    @SerialName("away") val away: String? = null,         // "36%", "8"
    @SerialName("compareCode") val compareCode: Int? = null, // 1=home우세, 2=away우세, 3=동일
    @SerialName("statisticsType") val statisticsType: String? = null, // "positive", "negative"
    @SerialName("valueType") val valueType: String? = null,
    @SerialName("homeValue") val homeValue: Int? = null,  // 64, 18
    @SerialName("awayValue") val awayValue: Int? = null,  // 36, 8
)
```

**WHY:** SofaScore의 통계 구조는 3단계 중첩(`statistics` → `groups` → `statisticsItems`)으로 되어 있습니다. `period="ALL"`이 전체 경기 통계이며, `"1ST"`/`"2ND"`는 전반/후반 통계입니다. 각 그룹(`Possession`, `Shots` 등)에 여러 통계 항목이 포함됩니다.

**HOW:**
- `home`/`away`는 화면에 표시할 문자열(예: `"64%"`, `"18"`)입니다.
- `homeValue`/`awayValue`는 비교 계산용 정수값(예: `64`, `18`)입니다. `LinearProgressIndicator`의 비율 계산에 사용합니다.
- `compareCode`는 어느 팀이 우세한지 나타냅니다 (1=홈 우세, 2=어웨이 우세, 3=동일). UI에서 색상 강조에 활용할 수 있습니다.
- 선수 레이팅은 별도 API가 아닌 Step 3의 `LineupsResponseDto` 내 `LineupPlayerStatDto.rating`에 포함되어 있습니다.

> 💡 **Tip:** 보통 UI에서는 `period="ALL"` 통계만 표시합니다. 전반/후반 통계 토글을 추가하려면 `period` 필터를 구현하면 됩니다.

> ⚠️ **주의:** `StatItemDto.home`/`away`가 `null`일 수 있습니다. 경기 시작 전이거나 해당 통계 항목이 수집되지 않은 경우입니다. nullable 처리가 필수입니다.

### ✅ 검증

- [ ] 모든 DTO 클래스에 `@Serializable` 어노테이션이 있다
- [ ] nullable 필드에 기본값 `null` 또는 `emptyList()`가 설정되어 있다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 4.5 — 인증 참고사항 (SofaScore)

### 배경

> SofaScore API는 복잡한 봇 방지 인증을 사용하지 않습니다. SofaScore의 GET 엔드포인트(`event`, `incidents`, `lineups`, `statistics`)는 **인증 없이 기본 데이터를 조회**할 수 있습니다.

### SofaScore 인증 방식

SofaScore는 Bearer Token 기반 인증을 사용합니다:
- **토큰 초기화:** `POST api/v1/token/init`
- **토큰 갱신:** `POST api/v1/token/refresh`
- OkHttp Interceptor에서 `Authorization: Bearer {token}` 헤더 추가 (non-GET/HEAD 요청)
- 서버가 `X-Token-Refresh` 응답 헤더로 토큰 갱신을 트리거할 수 있음

그러나 **이 Slice에서 사용하는 GET 엔드포인트들은 인증 없이 동작합니다.** Bearer Token은 POST 작업(투표 등)이나 프리미엄 기능에 주로 필요합니다.

### 결론

- **invisible WebView, JavaScript Interface 등의 복잡한 인증은 필요 없습니다.**
- 추후 인증이 필요한 기능(예: 투표, 프리미엄 데이터)을 추가할 때 Bearer Token Interceptor를 구현하면 됩니다.
- 현재는 별도의 인증 코드 없이 바로 API 호출이 가능합니다.

> 💡 **Tip:** SofaScore GET 엔드포인트가 인증 없이 동작하므로, 이 Step은 구현할 코드가 없습니다. 바로 Step 5로 넘어가세요.

---

## Step 5 — core-network: SofaScore API 엔드포인트 추가

### 목표

> `FootballApiService`에 SofaScore 경기 상세 조회를 위한 4개의 엔드포인트를 추가합니다. SofaScore는 경기 상세 데이터를 여러 엔드포인트로 분리하여 제공하므로, 각각에 대한 suspend 함수를 정의합니다.

### 작업 내용

**파일:** `core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/FootballApiService.kt`

기존 파일에 아래 함수들을 추가합니다.

```kotlin
// FootballApiService.kt에 추가
// SofaScore 경기 상세 엔드포인트 (4개)

// 1. 기본 경기 정보 (팀, 스코어, 상태, 경기장 등)
@GET("api/v1/event/{eventId}")
suspend fun getEvent(
    @Path("eventId") eventId: Int,
): EventResponseDto

// 2. 경기 이벤트 (골, 카드, 교체, VAR 등)
@GET("api/v1/event/{eventId}/incidents")
suspend fun getEventIncidents(
    @Path("eventId") eventId: Int,
): IncidentsResponseDto

// 3. 라인업 (선발, 교체, 포메이션, 선수 레이팅)
@GET("api/v1/event/{eventId}/lineups")
suspend fun getEventLineups(
    @Path("eventId") eventId: Int,
): LineupsResponseDto

// 4. 통계 (점유율, 슈팅, 패스 등 그룹별 통계)
@GET("api/v1/event/{eventId}/statistics")
suspend fun getEventStatistics(
    @Path("eventId") eventId: Int,
): EventStatisticsResponseDto
```

**WHY:** SofaScore는 하나의 API로 모든 데이터를 반환하지 않고, 관심사별로 엔드포인트를 분리합니다. 이 설계 덕분에 필요한 데이터만 선택적으로 호출할 수 있고, Repository에서 `coroutineScope { async {} }`로 병렬 호출하여 성능을 최적화할 수 있습니다.

**HOW:** `@Path("eventId")`로 URL 경로에 이벤트 ID를 삽입합니다. 모든 GET 엔드포인트는 인증 없이 호출 가능합니다. Base URL은 `https://api.sofascore.com/`으로 설정되어 있어야 합니다.

> 💡 **Tip:** 4개의 API를 순차 호출하면 응답 시간이 누적됩니다. Repository에서 `coroutineScope { async {} }`를 사용하여 4개를 병렬 호출하면 가장 느린 API의 응답 시간만큼만 소요됩니다. 이 패턴은 Step 8의 Repository 구현에서 다룹니다.

### ✅ 검증

- [ ] `EventResponseDto`, `IncidentsResponseDto`, `LineupsResponseDto`, `EventStatisticsResponseDto` 반환 타입이 정상 resolve 된다
- [ ] 해당 DTO import가 모두 추가되었다
- [ ] 기존 엔드포인트들과 충돌 없이 빌드된다
- [ ] 각 엔드포인트가 인증 없이 200 OK 반환 확인

---

## Step 6 — core-model: Domain Model 작성

### 목표

> 경기 상세 화면에서 사용할 Domain Model을 정의합니다. SofaScore의 다중 엔드포인트 DTO를 UI에서 바로 사용하기 쉬운 형태로 통합 설계합니다.

### 작업 내용

총 4개의 파일을 생성합니다.

---

### 6-1. MatchDetail.kt

**파일:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/MatchDetail.kt`

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

**WHY:** `MatchDetail`은 경기 상세 화면의 최상위 Domain Model입니다. 기존 `Fixture`(Slice 3에서 정의)를 재사용하고, 이벤트/라인업/통계를 추가합니다.

**HOW:**
- `lineups`와 `statistics`는 nullable입니다. 경기 시작 전이나 취소된 경기에는 데이터가 없을 수 있습니다.
- `scorers`는 `Map<Int, List<GoalScorer>>`로, 팀 ID를 키로 사용합니다. MatchHeader에서 홈/어웨이별 골 스코어러를 표시할 때 사용합니다.
- `GoalScorer`의 `isPenalty`와 `isOwnGoal`은 골 옆에 "(P)" 또는 "(OG)" 표시를 위해 필요합니다.

---

### 6-2. MatchEvent.kt

**파일:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/MatchEvent.kt`

```kotlin
package com.chase1st.feetballfootball.core.model

data class MatchEvent(
    val minute: Int,
    val extraMinute: Int?,
    val isHome: Boolean,
    val playerName: String,
    val assistName: String?,
    val type: EventType,
    val detail: String,
    // 교체 이벤트 전용
    val playerInName: String? = null,
    val playerOutName: String? = null,
)

// SofaScore incidentType + incidentClass 기반 매핑
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
    PERIOD,          // 전반/후반 구분선 (HT, FT)
    INJURY_TIME,
    ;

    companion object {
        fun from(incidentType: String, incidentClass: String?): EventType = when {
            incidentType == "goal" && incidentClass == "regular" -> GOAL
            incidentType == "goal" && incidentClass == "ownGoal" -> OWN_GOAL
            incidentType == "goal" && incidentClass == "penalty" -> PENALTY_GOAL
            incidentType == "goal" && incidentClass == "missedPenalty" -> MISSED_PENALTY
            incidentType == "card" && incidentClass == "yellow" -> YELLOW_CARD
            incidentType == "card" && incidentClass == "red" -> RED_CARD
            incidentType == "card" && incidentClass == "yellowRed" -> SECOND_YELLOW
            incidentType == "substitution" -> SUBSTITUTION
            incidentType == "var" -> VAR
            incidentType == "period" -> PERIOD
            incidentType == "injuryTime" -> INJURY_TIME
            else -> GOAL
        }
    }
}
```

**WHY:** `EventType` enum은 SofaScore의 `incidentType`+`incidentClass` 문자열 조합을 타입 안전한 enum으로 변환합니다. UI에서 이벤트 타입별 아이콘과 색상을 결정할 때 `when (event.type)` 으로 분기하기 편리합니다.

**HOW:** `EventType.from()` companion object 함수는 SofaScore incidents 응답의 `incidentType`+`incidentClass` 매핑입니다:

| incidentType | incidentClass | EventType |
|-------------|--------------|-----------|
| `"goal"` | `"regular"` | `GOAL` |
| `"goal"` | `"ownGoal"` | `OWN_GOAL` |
| `"goal"` | `"penalty"` | `PENALTY_GOAL` |
| `"goal"` | `"missedPenalty"` | `MISSED_PENALTY` |
| `"card"` | `"yellow"` | `YELLOW_CARD` |
| `"card"` | `"red"` | `RED_CARD` |
| `"card"` | `"yellowRed"` | `SECOND_YELLOW` |
| `"substitution"` | - | `SUBSTITUTION` |
| `"var"` | - | `VAR` |
| `"period"` | - | `PERIOD` |
| `"injuryTime"` | - | `INJURY_TIME` |

> ⚠️ **주의:** `else -> GOAL` fallback은 예상하지 못한 이벤트 타입에 대한 안전장치입니다. `PERIOD` 타입(HT, FT)은 UI에서 구분선으로 표시합니다. SofaScore는 `isHome` 필드로 홈/어웨이를 직접 구분하므로, 기존의 `team` 객체 대신 `isHome: Boolean`을 사용합니다.

---

### 6-3. MatchLineups.kt

**파일:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/MatchLineups.kt`

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

**WHY:** 라인업은 홈/어웨이 두 팀의 데이터를 항상 쌍으로 표시해야 합니다. `MatchLineups`가 이 쌍을 보장합니다.

**HOW:**
- `gridRow`와 `gridCol`은 API의 `grid` 문자열(예: `"2:3"`)을 파싱한 결과입니다. Row는 포메이션의 행(GK=1, DF=2, ...), Col은 열(왼쪽→오른쪽)을 나타냅니다.
- `rating`은 선수의 경기 중 레이팅(예: 7.2)입니다. API의 별도 `players` 배열에서 가져와 매핑합니다.
- `teamColorHex`는 유니폼 주 색상입니다. 포메이션 그리드에서 선수 원형 배경색으로 사용합니다.

> 💡 **Tip:** `gridRow`/`gridCol`이 `null`이면 해당 선수를 포메이션 그리드에 배치할 수 없습니다. 이 경우 교체 선수 목록에 표시하거나 기본 위치에 배치합니다.

---

### 6-4. MatchStatistics.kt

**파일:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/MatchStatistics.kt`

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

**WHY:** 통계는 항상 홈/어웨이 비교로 표시됩니다. `Map<String, String?>`으로 유연하게 어떤 통계 항목이든 처리할 수 있습니다.

**HOW:** API에서 제공하는 주요 통계 항목은 다음과 같습니다:
- `Shots on Goal`, `Shots off Goal`, `Total Shots`
- `Blocked Shots`, `Shots insidebox`, `Shots outsidebox`
- `Fouls`, `Corner Kicks`, `Offsides`
- `Ball Possession` (퍼센트, 예: `"64%"`)
- `Yellow Cards`, `Red Cards`
- `Goalkeeper Saves`
- `Total passes`, `Passes accurate`, `Passes %`
- `expected_goals`

> ⚠️ **주의:** `stats` Map의 value는 `String?`입니다. UI에서 숫자형 값과 퍼센트형 값을 파싱할 때 null 체크가 필요합니다.

### ✅ 검증

- [ ] 4개 파일 모두 올바른 패키지에 생성되었다
- [ ] `Fixture`, `Team` 등 기존 모델 참조가 정상 resolve 된다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 7 — core-domain: UseCase + Repository 인터페이스 업데이트

### 목표

> 경기 상세 데이터를 조회하는 UseCase를 만들고, FixtureRepository 인터페이스에 상세 조회 메서드를 추가합니다.

### 작업 내용

---

### 7-1. FixtureRepository 인터페이스에 메서드 추가

**파일:** `core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/repository/FixtureRepository.kt`

기존 파일에 아래 메서드를 추가합니다.

```kotlin
// core-domain/repository/FixtureRepository.kt에 추가
fun getFixtureDetail(fixtureId: Int): Flow<MatchDetail>
```

**WHY:** Repository 인터페이스는 core-domain 모듈에 있어 core-data와의 의존성을 역전시킵니다. core-domain은 구현 세부사항을 모르고, core-data가 이 인터페이스를 구현합니다.

**HOW:** `Flow<MatchDetail>`을 반환하여 비동기 데이터 스트림을 제공합니다. ViewModel에서 `collect`하여 UI 상태를 업데이트합니다.

> 💡 **Tip:** `MatchDetail`을 import해야 합니다: `import com.chase1st.feetballfootball.core.model.MatchDetail`

---

### 7-2. GetFixtureDetailUseCase 생성

**파일:** `core/core-domain/src/main/kotlin/com/chase1st/feetballfootball/core/domain/usecase/GetFixtureDetailUseCase.kt`

이 파일을 새로 생성합니다.

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

**WHY:** UseCase는 ViewModel과 Repository 사이의 중간 레이어로, 비즈니스 로직을 캡슐화합니다. 현재는 단순 위임이지만, 캐싱 전략이나 데이터 변환 등의 로직이 추가될 수 있습니다.

**HOW:** `@Inject constructor`로 Hilt가 자동으로 `FixtureRepository`를 주입합니다. `operator fun invoke`로 `useCase(fixtureId)` 형태의 함수 호출 구문을 지원합니다.

### ✅ 검증

- [ ] `FixtureRepository` 인터페이스에 `getFixtureDetail` 메서드가 추가되었다
- [ ] `GetFixtureDetailUseCase`가 올바른 패키지에 생성되었다
- [ ] `MatchDetail` import가 정상 resolve 된다
- [ ] 빌드 오류 없이 컴파일된다 (core-data의 구현은 다음 Step에서)

---

## Step 8 — core-data: FixtureDetailMapper (가장 복잡)

### 목표

> API 응답 DTO를 Domain Model로 변환하는 Mapper를 구현합니다. 이벤트 매핑, 라인업 + 레이팅 결합, 통계 매핑, 골 스코어러 추출 등 가장 복잡한 매핑 로직이 여기에 있습니다.

### 작업 내용

**파일:** `core/core-data/src/main/kotlin/com/chase1st/feetballfootball/core/data/mapper/FixtureDetailMapper.kt`

이 파일을 새로 생성합니다.

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

아래에서 각 매핑 로직을 자세히 설명합니다.

---

### 8-1. `map()` — 최상위 매핑

```kotlin
fun map(dto: FixtureDetailResponseDto): MatchDetail {
    val fixture = fixtureMapper.mapFixture(
        FixtureResponseDto(
            fixture = dto.fixture,
            league = dto.league,
            teams = dto.teams,
            goals = dto.goals,
        )
    )
    ...
}
```

**WHY:** 기존 `FixtureMapper.mapFixture()`를 재사용하여 기본 경기 정보(`Fixture`)를 매핑합니다. `FixtureDetailResponseDto`에서 기본 필드만 추출하여 기존 `FixtureResponseDto`를 생성합니다.

> 💡 **Tip:** Mapper 간 의존성(`FixtureDetailMapper` → `FixtureMapper`)은 `@Inject constructor`로 Hilt가 자동 주입합니다.

---

### 8-2. `mapLineups()` + `buildRatingMap()` — 라인업 + 레이팅 결합

```kotlin
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
```

**WHY:** API 응답에서 라인업(`lineups`)과 선수 레이팅(`players`)은 **별도 배열**로 제공됩니다. 선수 ID를 키로 레이팅 맵을 만들어 라인업 매핑 시 레이팅을 결합합니다.

**HOW:** `buildRatingMap`은 `players` 배열의 모든 선수를 순회하며 `playerId → rating` 맵을 구축합니다. `mapLineupPlayer`에서 이 맵으로 각 선수의 레이팅을 조회합니다.

---

### 8-3. `parseGrid()` — 포메이션 위치 파싱

```kotlin
private fun parseGrid(grid: String?): Pair<Int?, Int?> {
    if (grid == null) return null to null
    val parts = grid.split(":")
    return if (parts.size == 2) {
        parts[0].toIntOrNull() to parts[1].toIntOrNull()
    } else {
        null to null
    }
}
```

**WHY:** API의 `grid` 필드는 `"1:1"`, `"2:3"` 형태의 문자열입니다. 첫 번째 숫자는 행(row), 두 번째는 열(column)입니다. 포메이션 그리드 UI에서 선수를 올바른 위치에 배치하려면 숫자로 파싱해야 합니다.

---

### 8-4. `extractScorers()` — 자책골 팀 처리

```kotlin
if (scorer.isOwnGoal) {
    if (scoringEvent.team.id == teams.home.id) teams.away.id else teams.home.id
} else {
    scoringEvent.team.id
}
```

**WHY:** 자책골은 골을 넣은 선수의 팀이 아니라 **상대팀**의 득점으로 기록됩니다. 예를 들어 홈팀 수비수가 자책골을 넣으면 어웨이팀의 득점입니다. 이 로직이 없으면 스코어보드의 골 스코어러가 잘못된 팀에 표시됩니다.

> ⚠️ **주의:** `extractScorers`에서 `events.first { ... }` 호출은 이론상 항상 매칭되는 이벤트가 있어야 합니다. 하지만 데이터 불일치 가능성에 대비하여 `firstOrNull`로 변경하고 예외 처리를 추가하는 것도 고려해 보세요.

### ✅ 검증

- [ ] `FixtureMapper` 참조가 정상 resolve 된다
- [ ] `FixtureResponseDto` 생성자 호출이 기존 DTO 구조와 일치한다
- [ ] `EventType.from()` 호출이 정상 동작한다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 9 — feature-fixture-detail: UiState 정의

### 목표

> 경기 상세 화면의 UI 상태를 sealed interface로 정의합니다.

### 작업 내용

**파일:** `feature/feature-fixture-detail/src/main/kotlin/com/chase1st/feetballfootball/feature/fixturedetail/FixtureDetailUiState.kt`

이 파일을 새로 생성합니다.

```kotlin
package com.chase1st.feetballfootball.feature.fixturedetail

import com.chase1st.feetballfootball.core.model.MatchDetail

sealed interface FixtureDetailUiState {
    data object Loading : FixtureDetailUiState
    data class Success(val detail: MatchDetail) : FixtureDetailUiState
    data class Error(val message: String) : FixtureDetailUiState
}
```

**WHY:** `sealed interface`는 UI 상태를 유한한 타입 집합으로 제한합니다. `when` 분기에서 `else`를 쓸 필요가 없으며, 새 상태가 추가되면 컴파일러가 누락된 처리를 경고합니다.

**HOW:** 3가지 상태:
- `Loading`: API 호출 중. 로딩 인디케이터 표시.
- `Success`: 데이터 로딩 완료. `MatchDetail`을 포함.
- `Error`: 오류 발생. 에러 메시지와 재시도 버튼 표시.

### ✅ 검증

- [ ] `MatchDetail` import가 정상 resolve 된다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 10 — feature-fixture-detail: ViewModel

### 목표

> SavedStateHandle에서 fixtureId를 꺼내고, UseCase를 호출하여 데이터를 StateFlow로 제공하는 ViewModel을 구현합니다.

### 작업 내용

**파일:** `feature/feature-fixture-detail/src/main/kotlin/com/chase1st/feetballfootball/feature/fixturedetail/FixtureDetailViewModel.kt`

이 파일을 새로 생성합니다.

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

**WHY:** `@HiltViewModel`은 Hilt가 자동으로 의존성을 주입하는 ViewModel입니다. `SavedStateHandle`은 Navigation argument를 안전하게 전달받는 Hilt의 기본 메커니즘입니다.

**HOW:**
- `savedStateHandle["fixtureId"]`: Navigation에서 전달된 fixtureId를 꺼냅니다. `FixtureDetailRoute(fixtureId: Int)` Route 클래스의 프로퍼티명과 일치해야 합니다.
- `SharingStarted.WhileSubscribed(5_000)`: 마지막 구독자가 사라진 후 5초간 데이터를 유지합니다. 화면 회전 시 데이터를 다시 로딩하지 않습니다.
- `flow { ... }.stateIn(...)`: cold flow를 hot StateFlow로 변환합니다. UI가 구독할 때만 데이터 로딩이 시작됩니다.

> 💡 **Tip:** `fixtureId`가 `0`이면 잘못된 Navigation입니다. 실제 앱에서는 예외를 던지거나 에러 화면을 표시해야 합니다. 추후 Navigation 설정 시 `require(fixtureId > 0)`을 추가하는 것을 고려하세요.

> 💡 **Tip:** SofaScore의 GET 엔드포인트는 인증 없이 기본 데이터를 조회할 수 있으므로, 별도의 인증 대기 로직이 필요하지 않습니다. ViewModel은 곧바로 UseCase를 호출합니다.

### ✅ 검증

- [ ] `@HiltViewModel` 어노테이션이 있다
- [ ] `SavedStateHandle`에서 `"fixtureId"` 키로 값을 가져온다
- [ ] `StateFlow`의 초기값이 `Loading`이다
- [ ] 빌드 오류 없이 컴파일된다

---

## Step 11 (Phase 5-A) — EventsTab 컴포넌트

### 목표

> 경기 이벤트(골, 카드, 교체, VAR 등)를 타임라인 형태로 표시하는 탭 컴포넌트를 구현합니다. 홈팀 이벤트는 왼쪽, 어웨이팀 이벤트는 오른쪽에 배치합니다.

### 작업 내용

**파일:** `feature/feature-fixture-detail/src/main/kotlin/com/chase1st/feetballfootball/feature/fixturedetail/component/EventsTab.kt`

이 파일을 새로 생성합니다. 기존 `FixtureDetailEventsFragment.kt`(168줄)의 로직을 Compose로 재구현합니다.

```kotlin
// feature/feature-fixture-detail/src/main/kotlin/.../feature/fixturedetail/component/EventsTab.kt
// 참조: FixtureDetailEventsFragment.kt (168줄)
// - 이벤트 타입 8종 아이콘 분기
// - 시간 표시 (elapsed + extra)
// - 홈/어웨이 배치 (팀 ID로 좌/우 결정)
// 상세 구현은 LazyColumn + MatchEvent 데이터 기반
```

**핵심 구현 로직:**

1. **`EventType`별 아이콘 표시** (Icon 또는 이미지 리소스)
   - `GOAL` → 축구공 아이콘
   - `OWN_GOAL` → 빨간 축구공
   - `PENALTY_GOAL` → 축구공 + "(P)"
   - `MISSED_PENALTY` → 취소선 축구공
   - `YELLOW_CARD` → 노란 사각형
   - `RED_CARD` → 빨간 사각형
   - `SECOND_YELLOW` → 노란+빨간 겹침
   - `SUBSTITUTION` → 화살표 아이콘
   - `VAR` → "VAR" 텍스트 배지

2. **홈/어웨이 정렬**
   - 홈팀 이벤트: 왼쪽 정렬 (Row의 `Arrangement.Start`)
   - 어웨이팀 이벤트: 오른쪽 정렬 (Row의 `Arrangement.End`)
   - 시간은 중앙 고정

3. **골 표시**: 선수명 + 어시스트명 (있는 경우)
4. **교체 표시**: 교체 IN 선수 (초록색) + OUT 선수 (빨간색)
5. **시간 표시**: `elapsed'` 또는 `elapsed+extra'` (예: `45'` 또는 `90+3'`)

**구현 방법 (LazyColumn 기반):**

```kotlin
@Composable
fun EventsTab(
    events: List<MatchEvent>,
    homeTeamId: Int,
) {
    LazyColumn {
        items(events) { event ->
            val isHome = event.team.id == homeTeamId
            EventRow(event = event, isHome = isHome)
        }
    }
}

@Composable
private fun EventRow(event: MatchEvent, isHome: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (isHome) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isHome) {
            // 홈: 선수명 - 아이콘 - 시간
            EventContent(event)
            Spacer(modifier = Modifier.weight(1f))
            TimeText(event)
        } else {
            // 어웨이: 시간 - 아이콘 - 선수명
            TimeText(event)
            Spacer(modifier = Modifier.weight(1f))
            EventContent(event)
        }
    }
}
```

> ⚠️ **주의:** `EventType`별 아이콘 리소스가 프로젝트에 준비되어 있어야 합니다. Material Icons를 활용하거나, `drawable` 리소스를 추가하세요. 기존 앱의 이미지 리소스를 재사용할 수 있습니다.

> 💡 **Tip:** Phase 5-A에서 EventsTab, StatisticsTab, LineupTab은 **독립적으로 개발 가능**합니다. 각 탭은 MatchDetail의 서로 다른 필드만 사용하므로 병렬 작업에 적합합니다.

### ✅ 검증

- [ ] `EventType` 8종 모두에 대해 아이콘이 표시된다
- [ ] 홈팀 이벤트가 왼쪽, 어웨이팀 이벤트가 오른쪽에 배치된다
- [ ] 골 이벤트에 어시스트 정보가 표시된다
- [ ] 교체 이벤트에 IN/OUT 선수가 색상으로 구분된다
- [ ] 시간이 `45'` 또는 `90+3'` 형태로 표시된다
- [ ] Preview에서 정상 렌더링된다

---

## Step 12 (Phase 5-A) — StatisticsTab 컴포넌트

### 목표

> 홈/어웨이 팀의 경기 통계를 비교 막대 그래프(LinearProgressIndicator)로 표시하는 탭 컴포넌트를 구현합니다.

### 작업 내용

**파일:** `feature/feature-fixture-detail/src/main/kotlin/com/chase1st/feetballfootball/feature/fixturedetail/component/StatisticsTab.kt`

이 파일을 새로 생성합니다. 기존 `FixtureDetailStatisticsFragment.kt`(256줄)의 로직을 Compose로 재구현합니다.

```kotlin
// feature/feature-fixture-detail/src/main/kotlin/.../feature/fixturedetail/component/StatisticsTab.kt
// 참조: FixtureDetailStatisticsFragment.kt (256줄)
// 핵심: 15개 통계 항목을 LinearProgressIndicator로 비교 표시
```

**핵심 구현 로직:**

1. **각 통계 항목 레이아웃**: `홈 값 | ProgressBar | 어웨이 값`
2. **`Ball Possession` 퍼센트 파싱**: 예: `"64%"` → `64` (정수로 변환)
3. **`Shots on Goal` / `Total Shots` 슈팅 정확도 계산**
4. **팀 컬러를 ProgressBar 색상에 적용**: 참조 — 흰색(`#FFFFFF` 또는 유사)이면 검정으로 대체
5. **`LinearProgressIndicator`의 `progress`**: `home / (home + away)` 비율로 설정

**구현 방법 (LazyColumn 기반):**

```kotlin
@Composable
fun StatisticsTab(
    statistics: MatchStatistics,
) {
    // 통계 항목 순서 정의
    val statKeys = listOf(
        "Ball Possession",
        "Shots on Goal",
        "Shots off Goal",
        "Total Shots",
        "Blocked Shots",
        "Shots insidebox",
        "Shots outsidebox",
        "Fouls",
        "Corner Kicks",
        "Offsides",
        "Yellow Cards",
        "Red Cards",
        "Goalkeeper Saves",
        "Total passes",
        "Passes accurate",
        "Passes %",
        "expected_goals",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(statKeys) { key ->
            val homeValue = statistics.home.stats[key]
            val awayValue = statistics.away.stats[key]
            // 양쪽 모두 값이 있을 때만 표시
            if (homeValue != null || awayValue != null) {
                StatRow(
                    label = key,
                    homeValue = homeValue ?: "0",
                    awayValue = awayValue ?: "0",
                    homeColor = statistics.home.teamColorHex,
                    awayColor = statistics.away.teamColorHex,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    homeValue: String,
    awayValue: String,
    homeColor: String?,
    awayColor: String?,
) {
    val homeNum = parseStatValue(homeValue)
    val awayNum = parseStatValue(awayValue)
    val total = homeNum + awayNum
    val progress = if (total > 0f) homeNum / total else 0.5f

    Column {
        // 항목 이름
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        // 값 + ProgressBar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = homeValue, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f).height(8.dp),
                // 팀 컬러 적용 (흰색이면 검정으로 대체)
            )
            Text(text = awayValue, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
        }
    }
}

// "64%" → 64f, "5" → 5f
private fun parseStatValue(value: String): Float {
    return value.replace("%", "").trim().toFloatOrNull() ?: 0f
}
```

> 💡 **Tip:** `LinearProgressIndicator`의 `progress`는 0f~1f 범위입니다. `homeNum / (homeNum + awayNum)`으로 계산하면 홈팀의 비율이 바의 채워진 부분이 되고, 나머지가 어웨이팀 부분이 됩니다. 양쪽 모두 0이면 50:50으로 표시합니다.

> ⚠️ **주의:** 팀 컬러 hex 값이 `"ffffff"` (흰색)이거나 매우 밝은 경우, 밝은 배경에서 ProgressBar가 보이지 않을 수 있습니다. 기존 앱(`FixtureDetailStatisticsFragment.kt`)에서 흰색을 검정으로 대체하는 로직이 있으므로 이를 포팅해야 합니다.

### ✅ 검증

- [ ] 모든 통계 항목이 `홈 값 | ProgressBar | 어웨이 값` 형태로 표시된다
- [ ] `Ball Possession`의 퍼센트 값이 올바르게 파싱된다
- [ ] ProgressBar의 비율이 홈/어웨이 값에 비례한다
- [ ] 양쪽 모두 0인 항목은 50:50으로 표시된다
- [ ] Preview에서 정상 렌더링된다

---

## Step 13 (Phase 5-A) — LineupTab 컴포넌트

### 목표

> 포메이션 그리드, 선수 카드, 감독 정보, 교체 선수 목록을 표시하는 가장 복잡한 탭 컴포넌트를 구현합니다.

### 작업 내용

**파일:** `feature/feature-fixture-detail/src/main/kotlin/com/chase1st/feetballfootball/feature/fixturedetail/component/LineupTab.kt`

이 파일을 새로 생성합니다. 기존 `FixtureDetailLineupFragment.kt`(170줄)의 로직을 Compose로 재구현합니다.

```kotlin
// feature/feature-fixture-detail/src/main/kotlin/.../feature/fixturedetail/component/LineupTab.kt
// 참조: FixtureDetailLineupFragment.kt (170줄)
// 가장 복잡한 탭: 포메이션 그리드 표현
```

**핵심 구현 로직:**

1. **`startingXI`를 `gridRow`로 그룹핑** → 각 행에 선수 배치
2. **어웨이팀은 행 순서 역전** (참조: reverse layout) — 홈팀 GK가 상단, 어웨이팀 GK가 하단
3. **`Column` of `Row`** 또는 **`Box` + `Modifier.offset`** 으로 그리드 위치 표현
4. **선수 카드**: 번호 + 이름 + 레이팅
   - 레이팅 >= 7.0 → 초록색
   - 레이팅 < 7.0 → 주황색
   - 레이팅 null → 표시 안함
5. **교체 선수 목록**: 별도 LazyRow 또는 Column
6. **감독 정보**: 사진(AsyncImage) + 이름
7. **포메이션 문자열 표시** (예: "4-3-3")
8. **배경색**: 필드 초록 (StadiumGreen / FormationGreen)

**구현 방법 (Column + Row 기반):**

```kotlin
@Composable
fun LineupTab(
    lineups: MatchLineups,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        // 홈팀 포메이션
        item {
            FormationSection(
                lineup = lineups.home,
                isHome = true,
            )
        }
        // 어웨이팀 포메이션
        item {
            FormationSection(
                lineup = lineups.away,
                isHome = false,
            )
        }
        // 홈팀 교체 선수
        item {
            SubstitutesSection(
                label = "${lineups.home.team.name} 교체 선수",
                substitutes = lineups.home.substitutes,
            )
        }
        // 어웨이팀 교체 선수
        item {
            SubstitutesSection(
                label = "${lineups.away.team.name} 교체 선수",
                substitutes = lineups.away.substitutes,
            )
        }
    }
}

@Composable
private fun FormationSection(
    lineup: TeamLineup,
    isHome: Boolean,
) {
    val rows = lineup.startingXI
        .groupBy { it.gridRow ?: 0 }
        .toSortedMap()
        .let { if (!isHome) it.toSortedMap(reverseOrder()) else it }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B5E20)) // FormationGreen 배경
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 포메이션 + 감독 정보
        Text(
            text = "${lineup.team.name} (${lineup.formation})",
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "감독: ${lineup.coachName}",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 포메이션 그리드
        rows.forEach { (_, playersInRow) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                playersInRow.sortedBy { it.gridCol }.forEach { player ->
                    PlayerCard(player = player, teamColorHex = lineup.teamColorHex)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PlayerCard(
    player: LineupPlayer,
    teamColorHex: String?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp),
    ) {
        // 번호 원형 배지 (팀 컬러 배경)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(parseTeamColor(teamColorHex)),
        ) {
            Text(
                text = "${player.number}",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        // 선수 이름
        Text(
            text = player.name,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // 레이팅 (있는 경우)
        player.rating?.let { rating ->
            Text(
                text = String.format("%.1f", rating),
                color = if (rating >= 7.0f) Color(0xFF4CAF50) else Color(0xFFFF9800),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// 팀 컬러 hex 파싱 (흰색이면 기본 색으로 대체)
private fun parseTeamColor(hex: String?): Color {
    if (hex == null) return Color(0xFF424242)
    return try {
        val colorInt = android.graphics.Color.parseColor("#$hex")
        Color(colorInt)
    } catch (e: Exception) {
        Color(0xFF424242)
    }
}
```

> 💡 **Tip:** 어웨이팀의 포메이션은 `toSortedMap(reverseOrder())`로 행 순서를 뒤집어 GK가 하단에 오도록 합니다. 이렇게 하면 실제 축구 경기 화면처럼 양 팀이 마주보는 형태가 됩니다.

> ⚠️ **주의:** `gridRow`나 `gridCol`이 `null`인 선수가 있을 수 있습니다. 이 경우 포메이션 그리드에서 제외하고, 별도로 "위치 미정" 섹션에 표시하거나 교체 선수 목록에 포함시키세요.

### ✅ 검증

- [ ] 홈팀 GK가 상단, 어웨이팀 GK가 하단에 위치한다
- [ ] 포메이션 행/열이 올바르게 배치된다
- [ ] 선수 카드에 번호, 이름, 레이팅이 표시된다
- [ ] 레이팅 >= 7.0이 초록색, < 7.0이 주황색이다
- [ ] 팀 컬러가 선수 번호 배경에 적용된다
- [ ] 교체 선수 목록이 포메이션 아래에 표시된다
- [ ] 감독 이름과 포메이션 문자열이 표시된다
- [ ] Preview에서 정상 렌더링된다

---

## Step 14 (Phase 5-B) — FixtureDetailScreen 조립

### 목표

> MatchHeader + TabRow + HorizontalPager를 조합하여 경기 상세 화면을 완성합니다. LargeTopAppBar의 접히는 동작과 동적 탭 구성을 포함합니다.

### 작업 내용

**파일:** `feature/feature-fixture-detail/src/main/kotlin/com/chase1st/feetballfootball/feature/fixturedetail/FixtureDetailScreen.kt`

이 파일을 새로 생성합니다. 기존 `FixtureDetailFragment.kt`(268줄)의 로직을 Compose로 재구현합니다.

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

**WHY:** `FixtureDetailScreen`은 Phase 5-A에서 만든 3개 탭을 하나의 화면으로 조립하는 역할입니다. 기존 `FixtureDetailFragment`의 ViewPager2 + TabLayout 패턴을 Compose의 `HorizontalPager` + `TabRow`로 대체합니다.

**HOW:**
- **동적 탭 구성**: `buildList`로 데이터가 있는 탭만 추가합니다. 경기 시작 전에는 이벤트/라인업/통계가 모두 비어있어 탭이 표시되지 않습니다.
- **`LargeTopAppBar` + `exitUntilCollapsedScrollBehavior`**: 스크롤 시 TopAppBar가 축소되어 콘텐츠 영역이 넓어집니다.
- **`nestedScroll(scrollBehavior.nestedScrollConnection)`**: HorizontalPager 내부의 LazyColumn 스크롤과 TopAppBar 축소가 연동됩니다.

---

### 14-1. MatchHeader 컴포넌트 (골 스코어러 포함)

```kotlin
// 참조: FixtureDetailFragment.kt의 골 스코어러 표시 로직
// - 홈팀 골 스코어러: 왼쪽
// - 어웨이팀 골 스코어러: 오른쪽
// - 시간 표시: "45'" 또는 "90+3'"
// - 페널티: "(P)" 표시
// - 자책골: "(OG)" 표시
```

**핵심 구조:**

```kotlin
@Composable
private fun MatchHeader(detail: MatchDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 홈팀: 로고 + 이름 + 골 스코어러
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            // 팀 로고 (AsyncImage 또는 Picasso)
            // 팀 이름
            // 골 스코어러 목록
            detail.scorers[detail.fixture.homeTeam.id]?.forEach { scorer ->
                ScorerText(scorer)
            }
        }

        // 스코어
        Text(
            text = "${detail.fixture.homeGoals ?: "-"} - ${detail.fixture.awayGoals ?: "-"}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )

        // 어웨이팀: 로고 + 이름 + 골 스코어러
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            // 팀 로고
            // 팀 이름
            // 골 스코어러 목록
            detail.scorers[detail.fixture.awayTeam.id]?.forEach { scorer ->
                ScorerText(scorer)
            }
        }
    }
}

@Composable
private fun ScorerText(scorer: GoalScorer) {
    val timeStr = buildString {
        append("${scorer.minute}'")
        scorer.extraMinute?.let { append("+${it}'") }
    }
    val suffix = when {
        scorer.isPenalty -> " (P)"
        scorer.isOwnGoal -> " (OG)"
        else -> ""
    }
    Text(
        text = "${scorer.playerName} $timeStr$suffix",
        style = MaterialTheme.typography.bodySmall,
    )
}
```

> 💡 **Tip:** `Tab`의 `onClick`에서 `pagerState.animateScrollToPage(index)`를 호출할 때는 `rememberCoroutineScope()`로 코루틴 스코프를 만들어 사용하세요.

> ⚠️ **주의:** `detail.lineups!!`와 `detail.statistics!!`는 탭이 추가되는 조건(`detail.lineups != null`)과 동일하므로 NPE가 발생하지 않습니다. 하지만 더 안전하게 `?.let { ... }`을 사용하는 것도 고려해 보세요.

### ✅ 검증

- [ ] Loading 상태에서 로딩 인디케이터가 표시된다
- [ ] Error 상태에서 에러 메시지가 표시된다
- [ ] 매치 헤더에 팀 로고, 이름, 스코어가 표시된다
- [ ] 골 스코어러가 홈(왼쪽)/어웨이(오른쪽)에 올바르게 표시된다
- [ ] 페널티에 "(P)", 자책골에 "(OG)"가 표시된다
- [ ] 데이터가 있는 탭만 동적으로 표시된다
- [ ] TabRow 탭 클릭 시 HorizontalPager가 해당 페이지로 이동한다
- [ ] TopAppBar가 스크롤에 따라 접히고 펼쳐진다
- [ ] 뒤로가기 버튼 클릭 시 이전 화면으로 돌아간다

---

## Step 15 — 통합 빌드 및 최종 검증

### 목표

> 전체 레이어(DTO → Mapper → Domain → UseCase → ViewModel → UI)가 올바르게 연결되어 경기 상세 화면이 정상 동작하는지 최종 확인합니다.

### 작업 내용

1. **빌드 확인:**
   ```bash
   ./gradlew :feature:feature-fixture-detail:assembleDebug
   ./gradlew assembleDebug
   ```

2. **앱 실행 후 경기 상세 테스트:**

| 테스트 항목 | 확인 사항 |
|------------|----------|
| SofaScore API 호출 | `event`, `incidents`, `lineups`, `statistics` 4개 엔드포인트 정상 응답 |
| 경기 선택 | 경기 목록에서 경기 클릭 → 상세 화면 진입 |
| 매치 헤더 | 팀 로고, 이름, 스코어, 골 스코어러 표시 |
| 이벤트 탭 | 골/카드/교체/VAR 타임라인 표시 |
| 라인업 탭 | 포메이션 그리드, 선수 카드, 감독, 교체 선수 |
| 통계 탭 | 15개 항목 비교 ProgressBar |
| 데이터 없음 | 연기/취소된 경기에서 탭이 자동 숨김 |
| TopAppBar | 스크롤 시 접히는 동작 |
| 뒤로가기 | 이전 화면(경기 목록)으로 복귀 |

### ✅ 검증

- [ ] SofaScore API: `event`, `incidents`, `lineups`, `statistics` 4개 엔드포인트 정상 응답
- [ ] 경기 선택 → 상세 화면 진입
- [ ] 매치 헤더: 팀 로고, 이름, 스코어, 골 스코어러 표시
- [ ] 이벤트 탭: 타임라인 표시 (골/카드/교체/VAR)
- [ ] 라인업 탭: 포메이션 표시, 선수 카드, 감독, 교체 선수
- [ ] 통계 탭: 15개 항목 비교 ProgressBar
- [ ] 데이터 없는 탭 자동 숨김 (연기/취소된 경기)
- [ ] 접히는 TopAppBar 동작
- [ ] 뒤로가기 동작

---

## Step 16 — Git 커밋

### 목표

> 경기 상세 화면 구현이 완료되면 변경사항을 커밋합니다.

### 작업 내용

```bash
git add core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/FixtureDetailDto.kt
git add core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/EventDto.kt
git add core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/LineupDto.kt
git add core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/model/StatisticsDto.kt
git add core/core-network/src/main/kotlin/com/chase1st/feetballfootball/core/network/FootballApiService.kt
git add core/core-model/
git add core/core-domain/
git add core/core-data/
git add feature/feature-fixture-detail/
git commit -m "feat: Slice 5 경기 상세 화면 구현 (SofaScore API 연동 + 이벤트/라인업/통계)"
```

### ✅ 검증

- [ ] 커밋에 필요한 파일만 포함되었다
- [ ] 빌드가 깨지지 않는다

---

## 🎉 Slice 5 완료!

축하합니다! 프로젝트에서 가장 복잡한 화면인 경기 상세를 완성했습니다.

**이 Slice에서 완성한 것:**

| 모듈 | 파일 | 설명 |
|------|------|------|
| `core-network` | `FixtureDetailDto.kt` | 상세 응답 DTO (SofaScore 엔드포인트별 분리) |
| `core-network` | `EventDto.kt` | 이벤트 DTO (시간, 팀, 선수, 유형) |
| `core-network` | `LineupDto.kt` | 라인업 DTO (포메이션, 선수, 감독, 팀컬러) |
| `core-network` | `StatisticsDto.kt` | 통계 + 선수 레이팅 DTO |
| `core-network` | `FootballApiService.kt` | `getEvent()`, `getEventIncidents()`, `getEventLineups()`, `getEventStatistics()` 엔드포인트 추가 (SofaScore) |
| `core-model` | `MatchDetail.kt` | 상세 화면 최상위 Domain Model |
| `core-model` | `MatchEvent.kt` | 이벤트 Domain Model + EventType enum |
| `core-model` | `MatchLineups.kt` | 라인업 Domain Model (포메이션 grid 파싱) |
| `core-model` | `MatchStatistics.kt` | 통계 Domain Model |
| `core-domain` | `FixtureRepository.kt` | `getFixtureDetail()` 인터페이스 추가 |
| `core-domain` | `GetFixtureDetailUseCase.kt` | 상세 조회 UseCase |
| `core-data` | `FixtureDetailMapper.kt` | 가장 복잡한 DTO→Domain 매퍼 |
| `feature-fixture-detail` | `FixtureDetailUiState.kt` | UI 상태 sealed interface |
| `feature-fixture-detail` | `FixtureDetailViewModel.kt` | SavedStateHandle + StateFlow |
| `feature-fixture-detail` | `EventsTab.kt` | 이벤트 타임라인 탭 |
| `feature-fixture-detail` | `StatisticsTab.kt` | 통계 비교 탭 |
| `feature-fixture-detail` | `LineupTab.kt` | 포메이션 + 선수 카드 탭 |
| `feature-fixture-detail` | `FixtureDetailScreen.kt` | 전체 조립 (Header + Tabs + Pager) |

**핵심 학습 포인트:**
- SofaScore의 분리된 엔드포인트(`event`, `incidents`, `lineups`, `statistics`)를 병렬 호출하여 하나의 Domain Model로 조합하는 패턴
- 복잡한 API 응답을 DTO → Domain Model로 매핑할 때 Mapper에 로직을 집중시킵니다
- `HorizontalPager` + `TabRow`는 ViewPager2 + TabLayout의 Compose 대체재입니다
- `LargeTopAppBar` + `nestedScroll`로 기존 CoordinatorLayout 동작을 재현합니다
- 포메이션 그리드는 `gridRow`로 그룹핑 → `Row`로 배치하는 패턴으로 구현합니다
- `SavedStateHandle`은 Hilt ViewModel에서 Navigation argument를 받는 표준 방법입니다
