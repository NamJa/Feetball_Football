# SofaScore (com.sofascore.results) API 엔드포인트 분석

> 디컴파일 기반 분석
> 네트워크 스택: Retrofit2 + OkHttp3 + Gson / Kotlin Serialization
> Base URL: `https://api.sofascore.com/`
> 총 ~364개 엔드포인트 / 6개 Retrofit 인터페이스

---

## Base URLs

| URL                                | 용도                |
| ---------------------------------- | ----------------- |
| `https://api.sofascore.com/`       | 메인 API (동적 전환 가능) |
| `https://img.sofascore.com/`       | 이미지 CDN           |
| `https://ott.sofascore.com/`       | 스트리밍 (SportRadar) |
| `https://userimage.sofascore.com/` | 사용자 프로필 이미지       |

---

## 인증

Bearer Token 기반 인증.

- OkHttp Interceptor가 GET/HEAD 이외의 모든 요청에 자동 추가:
  - `Authorization: Bearer {token}`
  - `app-version: {versionCode}`
- 토큰 초기화: `POST api/v1/token/init`
- 토큰 갱신: `POST api/v1/token/refresh`
- 서버가 `X-Token-Refresh` 헤더로 갱신 트리거 가능
- AI Insights 프리미엄 기능: `X-Premium-Token` 헤더 사용

---

## 공통 파라미터 / ID 형식

| 파라미터                     | 형식              | 설명                                   |
| ------------------------ | --------------- | ------------------------------------ |
| `eventId`                | int             | 경기(이벤트) 고유 ID                        |
| `id` (team)              | int             | 팀 고유 ID                              |
| `id` (player)            | int             | 선수 고유 ID                             |
| `uniqueTournamentId`     | int             | 리그/대회 고유 ID                          |
| `seasonId`               | int             | 시즌 고유 ID                             |
| `date`                   | `yyyy-MM-dd`    | 날짜                                   |
| `sport` / `sportSlug`    | String          | 스포츠 슬러그                              |
| `span`                   | `next` / `last` | 다음/이전 경기 방향                          |
| `page`                   | int             | 페이지네이션 (0부터)                         |
| `type`                   | String          | 통계 유형 (예: `overall`, `home`, `away`) |
| `alpha2` / `countryCode` | String          | 국가 코드 (예: `KR`, `US`)                |

### 스포츠 슬러그 목록

| 슬러그                 | 스포츠   |
| ------------------- | ----- |
| `football`          | 축구    |
| `baseball`          | 야구    |
| `basketball`        | 농구    |
| `tennis`            | 테니스   |
| `hockey`            | 하키    |
| `cricket`           | 크리켓   |
| `american-football` | 미식축구  |
| `rugby`             | 럭비    |
| `handball`          | 핸드볼   |
| `volleyball`        | 배구    |
| `mma`               | 종합격투기 |
| `motorsport`        | 모터스포츠 |
| `esports`           | e스포츠  |

---

## API 탐색 흐름 (ID 발견 경로)

SofaScore의 모든 엔드포인트는 `uniqueTournamentId`, `seasonId`, `eventId` 등의 숫자 ID를 필요로 한다.
이 ID들은 아래 흐름으로 단계적으로 탐색할 수 있다.

### 흐름 1: 스포츠 → 카테고리(국가) → 리그 → 시즌

```
① GET api/v1/sport/{sportSlug}/categories
   → 해당 스포츠의 국가/카테고리 목록과 categoryId 획득

② GET api/v1/category/{categoryId}/unique-tournaments
   → 해당 국가의 리그/대회 목록과 uniqueTournamentId 획득

③ GET api/v1/unique-tournament/{uniqueTournamentId}/seasons
   → 시즌 목록과 seasonId 획득

④ GET api/v1/unique-tournament/{uniqueTournamentId}/season/{seasonId}/standings/total
   → 순위표, 팀 목록, teamId 획득

⑤ GET api/v1/team/{teamId}/events/last/0
   → 경기 목록과 eventId 획득

⑥ GET api/v1/event/{eventId}
   → 경기 상세
```

```
호출 예시 (KBO 야구):

# ① 야구 카테고리 목록
GET https://api.sofascore.com/api/v1/sport/baseball/categories
→ [{"id": 1385, "name": "South Korea"}, {"id": 1370, "name": "USA"}, ...]

# ② 한국 야구 리그 목록
GET https://api.sofascore.com/api/v1/category/1385/unique-tournaments
→ [{"id": 11204, "name": "KBO"}, {"id": 11531, "name": "KBO Preseason"}, ...]

# ③ KBO 시즌 목록
GET https://api.sofascore.com/api/v1/unique-tournament/11204/seasons
→ [{"id": 88022, "name": "KBO League 2026"}, {"id": 71354, "name": "KBO League 2025"}, ...]

# ④ 2026 시즌 순위표
GET https://api.sofascore.com/api/v1/unique-tournament/11204/season/88022/standings/total
→ 팀 순위, 승/패/무, 승률 등 + 각 팀의 teamId

# ⑤ LG 트윈스 최근 경기
GET https://api.sofascore.com/api/v1/team/188257/events/last/0
→ 최근 경기 목록 + 각 경기의 eventId

# ⑥ 경기 상세
GET https://api.sofascore.com/api/v1/event/12345678
```

### 흐름 2: 검색으로 바로 접근

```
GET api/v1/search/all?q={검색어}&sport={sportSlug}&page=0
→ 검색 결과에서 uniqueTournament, team, player 등의 ID를 직접 획득
```

```
호출 예시:

# 리그 검색
GET https://api.sofascore.com/api/v1/search/all?q=KBO&sport=baseball&page=0
→ uniqueTournament 타입 결과에서 id = 11204

# 팀 검색
GET https://api.sofascore.com/api/v1/search/all?q=LG%20Twins&sport=baseball&page=0
→ team 타입 결과에서 id = 188257

# 선수 검색
GET https://api.sofascore.com/api/v1/search/all?q=Ohtani&sport=baseball&page=0
```

### 흐름 3: 국가별 기본 리그

```
GET api/v1/config/default-unique-tournaments/{countryCode}
→ 해당 국가 사용자에게 기본 노출되는 리그 목록 (uniqueTournamentId 포함)
```

```
호출 예시:
GET https://api.sofascore.com/api/v1/config/default-unique-tournaments/KR
→ 한국 기본 리그: KBO(11204), K리그(292), KBL(596) 등
```

### 흐름 4: 날짜 기반 경기 탐색

```
GET api/v1/sport/{sportSlug}/{date}/events/{page}
→ 특정 날짜의 전체 경기 목록에서 eventId, teamId, uniqueTournamentId 모두 획득 가능
```

```
호출 예시:
GET https://api.sofascore.com/api/v1/sport/baseball/2026-03-29/events/0
→ 해당 날짜의 모든 야구 경기 (KBO, MLB, NPB 등)
```

### ID 탐색 요약

| 목표         | API 경로                                                | 획득하는 ID               |
| ---------- | ----------------------------------------------------- | --------------------- |
| 국가/카테고리 목록 | `sport/{sport}/categories`                            | `categoryId`          |
| 리그/대회 목록   | `category/{categoryId}/unique-tournaments`            | `uniqueTournamentId`  |
| 시즌 목록      | `unique-tournament/{id}/seasons`                      | `seasonId`            |
| 순위표 (팀 목록) | `unique-tournament/{id}/season/{sid}/standings/total` | `teamId`              |
| 팀 경기 목록    | `team/{teamId}/events/last/{page}`                    | `eventId`             |
| 선수 목록      | `team/{teamId}/players`                               | `playerId`            |
| 통합 검색      | `search/all?q={query}`                                | 모든 종류의 ID             |
| 날짜별 경기     | `sport/{sport}/{date}/events/{page}`                  | `eventId`, `teamId` 등 |

### 주요 리그 ID 참고 (KBO 야구)

| 항목                          | ID        |
| --------------------------- | --------- |
| KBO 리그 (uniqueTournamentId) | **11204** |
| KBO 프리시즌                    | 11531     |
| 카테고리 (한국 야구)                | 1385      |
| 2026 시즌                     | 88022     |
| 2025 시즌                     | 71354     |

| KBO 팀    | teamId |
| -------- | ------ |
| LG 트윈스   | 188257 |
| 한화 이글스   | 188243 |
| SSG 랜더스  | 188244 |
| 삼성 라이온즈  | 188245 |
| NC 다이노스  | 188253 |
| KT 위즈    | 188409 |
| 롯데 자이언츠  | 188246 |
| KIA 타이거즈 | 188247 |
| 두산 베어스   | 188248 |
| 키움 히어로즈  | 188258 |

---

## 이미지 URL 패턴

Base: `https://img.sofascore.com/api/v1/`

| 리소스        | URL                                 |
| ---------- | ----------------------------------- |
| 선수 사진      | `player/{id}/image`                 |
| 팀 로고       | `team/{id}/image`                   |
| 리그 로고      | `unique-tournament/{id}/image`      |
| 리그 로고 (다크) | `unique-tournament/{id}/image/dark` |
| 국가 국기      | `country/{alpha2}/flag`             |
| 경기장 사진     | `venue/{id}/image`                  |
| 감독 사진      | `manager/{id}/image`                |
| 심판 사진      | `referee/{id}/image`                |
| 배당 업체 로고   | `odds/provider/{id}/logo`           |

```
호출 예시:
https://img.sofascore.com/api/v1/player/961995/image
https://img.sofascore.com/api/v1/team/17/image
https://img.sofascore.com/api/v1/unique-tournament/17/image
```

---

# 공통 API (전 스포츠)

모든 스포츠에서 공통으로 사용되는 엔드포인트.

---

## 경기 (Event) — 공통

### GET api/v1/event/{eventId}

경기 상세 정보 조회.

- Path: `eventId` — 경기 고유 ID (int)
- Response: `EventResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678
```

### GET api/v1/event/{eventId}/incidents

경기 이벤트 (골, 카드, 교체 등).

- Path: `eventId` (int)
- Response: `EventIncidentsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/incidents
```

### GET api/v1/event/{eventId}/lineups

경기 라인업.

- Path: `eventId` (int)
- Response: `LineupsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/lineups
```

### GET api/v1/event/{id}/statistics

경기 통계.

- Path: `id` (int)
- Response: `EventStatisticsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/statistics
```

### GET api/v1/event/{id}/graph

경기 모멘텀 그래프.

- Path: `id` (int)
- Response: `EventGraphResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/graph
```

### GET api/v1/event/{id}/h2h

상대 전적 (Head to Head).

- Path: `id` (int)
- Response: `Head2HeadResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/h2h
```

### GET api/v1/event/{id}/pregame-form

경기 전 팀 폼 (최근 성적).

- Path: `id` (int)
- Response: `PregameFormResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/pregame-form
```

### GET api/v1/event/{id}/best-players

경기 MVP / 최우수 선수.

- Path: `id` (int)
- Response: `EventBestPlayersResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/best-players
```

### GET api/v1/event/{id}/managers

경기 감독 정보.

- Path: `id` (int)
- Response: `EventManagersResponse`

### GET api/v1/event/{id}/weather

경기 날씨 정보.

- Path: `id` (int)
- Response: `EventWeatherResponse`

### GET api/v1/event/{id}/votes

경기 투표 결과.

- Path: `id` (int)
- Response: `VotesResponse`

### POST api/v1/event/{id}/vote

경기 투표 제출.

- Path: `id` (int)
- Body: `EventVoteBody`
- Response: `Unit`

### GET api/v1/event/{id}/comments/{languageCode}

경기 문자 중계 (Commentary).

- Path: `id` (int), `languageCode` (String, 예: `en`, `ko`)
- Response: `CommentaryResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/comments/en
```

### GET api/v1/event/{eventId}/highlights

경기 하이라이트 영상.

- Path: `eventId` (int)
- Response: `HighlightsResponse`

### GET api/v1/event/{eventId}/ai-insights/{language}

AI 분석 인사이트 (프리미엄).

- Path: `eventId` (int), `language` (String)
- Header: `X-Premium-Token`
- Response: `EventAiInsightsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/ai-insights/en
```

### GET api/v1/event/newly-added-events

새로 추가된 경기 목록.

- Response: `AddedEventsResponse`

---

## 배당률 (Odds) — 공통

### GET api/v1/event/{id}/odds/{providerId}/all

경기별 전체 배당률.

- Path: `id` (int), `providerId` (int)
- Response: `AllOddsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/odds/1/all
```

### GET api/v1/event/{id}/odds/{providerId}/featured

경기별 주요 배당률.

- Path: `id` (int), `providerId` (int)
- Response: `FeaturedOddsResponse`

### GET api/v1/odds/providers/{cc}/{type}

국가별 배당 업체 목록.

- Path: `cc` (국가코드), `type` (String)
- Response: `OddsProvidersResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/odds/providers/KR/prematch
```

### GET api/v1/odds/{id}/dropping/{sportSlug}

하락 배당률.

- Path: `id` (int), `sportSlug` (String)
- Response: `DroppingOddsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/odds/1/dropping/football
```

---

## 선수 (Player) — 공통

### GET api/v1/player/{id}

선수 상세 정보.

- Path: `id` (int)
- Response: `PlayerDetailsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/player/961995
```

### GET api/v1/player/{id}/characteristics

선수 특성 (키, 몸무게, 포지션 등).

- Path: `id` (int)
- Response: `PlayerCharacteristicsResponse`

### GET api/v1/player/{id}/attribute-overviews

선수 능력치 개요.

- Path: `id` (int)
- Response: `AttributeOverviewResponse`

### GET api/v1/player/{id}/statistics/seasons

선수의 시즌별 통계 목록.

- Path: `id` (int)
- Response: `StatisticsSeasonsResponse`

### GET api/v1/player/{id}/unique-tournament/{tid}/season/{sid}/statistics/{type}

시즌별 선수 통계.

- Path: `id`, `tid`, `sid` (int), `type` (String: `overall`, `home`, `away`)
- Response: `PlayerSeasonStatisticsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/player/961995/unique-tournament/17/season/52186/statistics/overall
```

### GET api/v1/player/{id}/transfer-history

선수 이적 이력.

- Path: `id` (int)
- Response: `TransferHistoryResponse`

### GET api/v1/player/{id}/events/last/{page}

선수의 최근 출전 경기 목록.

- Path: `id` (int), `page` (int)
- Response: `PlayerEventsListResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/player/961995/events/last/0
```

### GET api/v1/player/{id}/national-team-statistics

선수 국가대표 통계.

- Path: `id` (int)
- Response: `NationalTeamStatisticsResponse`

---

## 팀 (Team) — 공통

### GET api/v1/team/{id}

팀 상세 정보.

- Path: `id` (int)
- Response: `TeamDetailsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/team/17
```

### GET api/v1/team/{id}/players

팀 소속 선수 목록.

- Path: `id` (int)
- Response: `TeamPlayersResponse`

### GET api/v1/team/{id}/transfers

팀 이적 현황.

- Path: `id` (int)
- Response: `TeamTransfersResponse`

### GET api/v1/team/{id}/events/{span}/{page}

팀 경기 목록.

- Path: `id` (int), `span` (`next`/`last`), `page` (int)
- Response: `EventListResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/team/17/events/next/0
GET https://api.sofascore.com/api/v1/team/17/events/last/0
```

### GET api/v1/team/{id}/near-events

팀 근접 경기 (가장 가까운 이전/다음 경기).

- Path: `id` (int)
- Response: `TeamNearEventsResponse`

### GET api/v1/team/{id}/unique-tournament/{uid}/season/{sid}/statistics/{type}

시즌별 팀 통계.

- Path: `id`, `uid`, `sid` (int), `type` (String)
- Response: `TeamSeasonStatisticsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/team/17/unique-tournament/17/season/52186/statistics/overall
```

### GET api/v1/team/{id}/performance

팀 성과 요약.

- Path: `id` (int)
- Response: `TeamPerformanceResponse`

### GET api/v1/team/{id}/rankings

팀 랭킹 정보.

- Path: `id` (int)
- Response: `TeamRankingsResponse`

---

## 리그/대회 (Unique Tournament) — 공통

### GET api/v1/unique-tournament/{id}

리그/대회 상세 정보.

- Path: `id` (int)
- Response: `UniqueTournamentDetailsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/unique-tournament/17
```

### GET api/v1/unique-tournament/{id}/seasons

리그 시즌 목록.

- Path: `id` (int)
- Response: `TournamentSeasonsResponse`

### GET api/v1/unique-tournament/{id}/season/{seasonId}/standings/{type}

리그 순위표.

- Path: `id`, `seasonId` (int), `type` (String: `total`, `home`, `away`)
- Response: `StandingsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/unique-tournament/17/season/52186/standings/total
```

### GET api/v1/unique-tournament/{id}/season/{seasonId}/events/round/{round}

라운드별 경기 목록.

- Path: `id`, `seasonId`, `round` (int)
- Response: `EventListResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/unique-tournament/17/season/52186/events/round/1
```

### GET api/v1/unique-tournament/{id}/season/{sid}/top-players/{type}

리그 시즌 우수 선수.

- Path: `id`, `sid` (int), `type` (String: `rating`, `goals`, `assists` 등)
- Response: `TopPerformanceResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/unique-tournament/17/season/52186/top-players/rating
```

### GET api/v1/unique-tournament/{id}/season/{seasonId}/teams

리그 시즌 참가 팀 목록.

- Path: `id`, `seasonId` (int)
- Response: `UniqueTournamentTeamsResponse`

### GET api/v1/unique-tournament/{id}/season/{sid}/cuptrees

컵 대회 대진표.

- Path: `id`, `sid` (int)
- Response: `CupTreesResponse`

### GET api/v1/unique-tournament/{id}/season/{seasonId}/team-of-the-week/{roundId}

라운드별 베스트 11 (Team of the Week).

- Path: `id`, `seasonId`, `roundId` (int)
- Response: `TeamOfTheWeekResponse`

---

## 일정 / 스포츠별 경기 조회 — 공통

### GET api/v1/sport/{sport}/{date}/events/{page}

날짜별 전체 경기 목록.

- Path: `sport` (String), `date` (`yyyy-MM-dd`), `page` (int)
- Response: `EventListResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/sport/football/2026-03-29/events/0
GET https://api.sofascore.com/api/v1/sport/baseball/2026-03-29/events/0
GET https://api.sofascore.com/api/v1/sport/basketball/2026-03-29/events/0
```

### GET api/v1/sport/{sport}/events/live

스포츠별 실시간 경기 목록.

- Path: `sport` (String)
- Response: `EventListResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/sport/football/events/live
```

### GET api/v1/sport/{sport}/main-events/{date}

스포츠별 주요 경기.

- Path: `sport` (String), `date` (`yyyy-MM-dd`)
- Response: `EventListResponse`

### GET api/v1/sport/{sport}/categories

스포츠별 카테고리 (국가/리그 그룹).

- Path: `sport` (String)
- Response: `SportCategoriesResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/sport/football/categories
```

### GET api/v1/category/{id}/unique-tournaments

카테고리 내 리그/대회 목록.

- Path: `id` (int, 카테고리 ID)
- Response: `CategoryUniqueTournamentResponse`

### GET api/v1/calendar/{yearAndMonth}/{timezoneOffset}/{sportSlug}/unique-tournaments

월별 대회 캘린더.

- Path: `yearAndMonth` (예: `2026-03`), `timezoneOffset` (예: `9`), `sportSlug`
- Response: `MonthlyUniqueTournamentsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/calendar/2026-03/9/football/unique-tournaments
```

### GET api/v1/trending/events/{countryCodeAlpha2}/all

국가별 트렌딩 경기.

- Path: `countryCodeAlpha2` (String)
- Response: `EventListResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/trending/events/KR/all
```

---

# 축구 (Football)

축구 전용 또는 축구에서 주로 사용되는 엔드포인트.

---

### GET api/v1/event/{id}/shotmap

경기 슛맵.

- Path: `id` (int)
- Response: `FootballShotmapResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/shotmap
```

### GET api/v1/event/{eventId}/shotmap/player/{playerId}

선수별 슛맵.

- Path: `eventId` (int), `playerId` (int)
- Response: `FootballShotmapResponse`

### GET api/v1/event/{eventId}/goalkeeper-shotmap/player/{playerId}

골키퍼 슛맵.

- Path: `eventId` (int), `playerId` (int)
- Response: `FootballShotmapResponse`

### GET api/v1/event/{id}/player/{playerid}/heatmap

경기 내 선수 히트맵.

- Path: `id` (int), `playerid` (int)
- Response: `PlayerHeatmapResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/player/961995/heatmap
```

### GET api/v1/event/{id}/heatmap/{teamId}

팀 히트맵.

- Path: `id` (int), `teamId` (int)
- Response: `EventTeamHeatmapResponse`

### GET api/v1/event/{id}/average-positions

선수 평균 포지션.

- Path: `id` (int)
- Response: `AveragePositionsResponse`

### GET api/v1/event/{eventId}/player/{playerId}/rating-breakdown

선수 평점 상세 분석.

- Path: `eventId` (int), `playerId` (int)
- Response: `FootballEventPlayerRatingBreakdownResponse`

### GET api/v1/player/{pid}/unique-tournament/{tid}/season/{sid}/heatmap

시즌 전체 선수 히트맵.

- Path: `pid`, `tid`, `sid` (int)
- Response: `PlayerSeasonHeatMapResponse`

### GET api/v1/player/{pid}/unique-tournament/{tid}/season/{sid}/shot-actions/{type}

시즌 슛 액션 데이터.

- Path: `pid`, `tid`, `sid` (int), `type` (String)
- Response: `PlayerSeasonShotActionsResponse`

### GET api/v1/player/{pid}/penalty-history

선수 페널티 킥 이력.

- Path: `pid` (int)
- Response: `PlayerPenaltyHistoryResponse`

### GET api/v1/unique-tournament/{id}/season/{seasonId}/team/{teamId}/team-performance-graph-data

팀 퍼포먼스 그래프 데이터.

- Path: `id`, `seasonId`, `teamId` (int)
- Response: `PerformanceGraphDataResponse`

### GET api/v1/unique-tournament/{id}/season/{sid}/power-rankings/round/{roundId}

파워 랭킹 (라운드별).

- Path: `id`, `sid`, `roundId` (int)
- Response: `PowerRankingResponse`

### GET api/v1/unique-tournament/{tid}/season/{sid}/shot-action-areas/{type}

시즌 슛 액션 영역 분석.

- Path: `tid`, `sid` (int), `type` (String)
- Response: `SeasonShotActionAreaResponse`

### GET api/v1/team/{id}/unique-tournament/{tid}/season/{sid}/goal-distributions

팀 시즌 골 분포 (시간대별).

- Path: `id`, `tid`, `sid` (int)
- Response: `GoalDistributionsResponse`

### GET api/v1/unique-tournament/{id}/player-transfer-history/{type}/{page}

리그 이적 이력.

- Path: `id` (int), `type` (String: `in`, `out`), `page` (int)
- Response: `UniqueTournamentPlayerTransactionsResponse`

### GET api/v1/transfer

이적 검색.

- QueryMap: `LinkedHashMap<String, String>`
- Response: `TransfersResponse`

---

# 야구 (Baseball)

야구 전용 엔드포인트.

---

### GET api/v1/event/{id}/innings

이닝별 경기 데이터.

- Path: `id` (int)
- Response: `EventInningsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/innings
```

### GET api/v1/event/{id}/at-bats

타석 데이터.

- Path: `id` (int)
- Response: `BaseballAtBatsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/at-bats
```

### GET api/v1/event/{id}/atbat/{atBatId}/pitches

타석별 투구 데이터.

- Path: `id` (int), `atBatId` (int)
- Response: `BaseballPitchesResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/atbat/456/pitches
```

### GET api/v1/event/{id}/player/{pid}/pitches/{type}

선수별 투구 데이터 (경기 내).

- Path: `id` (int), `pid` (int), `type` (String)
- Response: `BaseballPitchesResponse`

### GET api/v1/player/{id}/season/{sid}/statistical-rankings/{type}

선수 시즌 통계 랭킹.

- Path: `id` (int), `sid` (int), `type` (String)
- Response: `BaseballPlayerSeasonRankedStatisticsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/player/12345/season/52186/statistical-rankings/batting
```

### GET api/v1/player/{id}/unique-tournament/{tid}/season/{sid}/pitches/{type}/{seasonType}

선수 시즌 투구 데이터.

- Path: `id`, `tid`, `sid` (int), `type` (String), `seasonType` (String)
- Response: `BaseballPitchesResponse`

---

# 농구 (Basketball)

농구에서 주로 사용되는 엔드포인트. 공통 API의 경기/선수/팀 엔드포인트를 그대로 사용하며, 추가로 아래 엔드포인트를 활용한다.

---

### GET api/v1/event/{id}/graph/sequence

스코어 시퀀스 그래프 (쿼터별 점수 흐름).

- Path: `id` (int)
- Response: `EventGraphSequenceResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/graph/sequence
```

### GET api/v1/event/{id}/graph/win-probability

실시간 승리 확률 그래프.

- Path: `id` (int)
- Response: `EventGraphResponse`

### GET api/v1/event/{id}/team-streaks

팀 연속 기록 (연승, 연패 등).

- Path: `id` (int)
- Response: `TeamStreaksResponse`

### GET api/v1/event/{id}/shotmap/{teamid}

팀별 슛맵 (농구).

- Path: `id` (int), `teamid` (int)
- Response: `TeamEventShotmapResponse`

### GET api/v1/event/{id}/player/{playerid}/shotmap

선수별 슛맵 (농구).

- Path: `id` (int), `playerid` (int)
- Response: `PlayerShotmapResponse`

### GET api/v1/unique-tournament/{unique_tournament_id}/season/{season_id}/draft

NBA/NFL 드래프트 정보.

- Path: `unique_tournament_id`, `season_id` (int)
- Response: `UniqueTournamentDraftInfoResponse`

### GET api/v1/unique-tournament/{unique_tournament_id}/draft/{year}/pick/{round}

드래프트 픽 목록.

- Path: `unique_tournament_id` (int), `year` (String), `round` (int)
- Response: `UniqueTournamentDraftPicksResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/unique-tournament/132/draft/2026/pick/1
```

---

# 테니스 (Tennis)

테니스 전용 엔드포인트.

---

### GET api/v1/event/{id}/point-by-point

포인트별 데이터.

- Path: `id` (int)
- Response: `PointByPointResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/point-by-point
```

### GET api/v1/event/{id}/tennis-power

테니스 파워 데이터.

- Path: `id` (int)
- Response: `TennisPowerResponse`

### GET api/v1/team/{id}/year-statistics/{year}

선수 연간 통계 (테니스에서 team = 개인 선수).

- Path: `id` (int), `year` (String, 예: `2026`)
- Response: `TennisTeamYearlyStatisticsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/team/12345/year-statistics/2026
```

### GET api/v1/team/{id}/grand-slam/best-results

그랜드슬램 최고 성적.

- Path: `id` (int)
- Response: `TennisGrandSlamPerformanceResponse`

---

# 하키 (Hockey)

하키 전용 엔드포인트.

---

### GET api/v1/event/{id}/shotmap

하키 경기 슛맵.

- Path: `id` (int)
- Response: `HockeyEventShotmapResponse`

### GET api/v1/event/{eventId}/player/{playerId}/shotmap

하키 선수별 슛맵.

- Path: `eventId` (int), `playerId` (int)
- Response: `HockeyPlayerShotmapResponse`

### GET api/v1/event/{id}/comments

하키 Play-by-Play 중계.

- Path: `id` (int)
- Response: `HockeyPlayByPlayResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/comments
```

---

# 크리켓 (Cricket)

크리켓 전용 엔드포인트.

---

### GET api/v1/event/{id}/graph/cricket

오버별 득점 그래프.

- Path: `id` (int)
- Response: `CricketRunsPerOverGraphResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/graph/cricket
```

### GET api/v1/event/{id}/innings

이닝 데이터 (크리켓/야구 공용).

- Path: `id` (int)
- Response: `EventInningsResponse`

### GET api/v1/event/{id}/series

시리즈 정보.

- Path: `id` (int)
- Response: `EventSeriesResponse`

---

# 종합격투기 (MMA)

MMA 전용 엔드포인트.

---

### GET api/v1/unique-tournament/{uid}/tournament/{tid}/mma-events/{type}

MMA 이벤트 목록.

- Path: `uid` (int), `tid` (int), `type` (String)
- Response: `MmaEventListResponse`

### GET api/v1/unique-tournament/{uid}/scheduled-mma-main-events/{date}

MMA 메인 이벤트 일정.

- Path: `uid` (int), `date` (`yyyy-MM-dd`)
- Response: `EventListResponse`

### GET api/v1/rankings/unique-tournament/{id}/summary

MMA 랭킹 요약.

- Path: `id` (int)
- Response: `RankingsSummaryResponse`

### GET api/v1/rankings/unique-tournament/{uniqueTournamentId}/{weightClass}/{gender}

체급/성별 랭킹.

- Path: `uniqueTournamentId` (int), `weightClass` (String), `gender` (String)
- Response: `RankingTypeWithRows`

```
호출 예시:
GET https://api.sofascore.com/api/v1/rankings/unique-tournament/19062/lightweight/male
```

---

# 모터스포츠 (Motorsport)

모터스포츠 전용 엔드포인트.

---

### GET api/v1/stage/{id}

스테이지 상세 정보.

- Path: `id` (int)
- Response: `StageResponse`

### GET api/v1/stage/{id}/v2/substages

서브 스테이지 목록.

- Path: `id` (int)
- Response: `StagesListResponse`

### GET api/v1/stage/{id}/standings/{outrightTeamType}

스테이지 순위표.

- Path: `id` (int), `outrightTeamType` (String: `driver`, `team`)
- Response: `StageStandingsResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/stage/12345/standings/driver
```

### GET api/v1/stage/{stageId}/driver-performance

드라이버 퍼포먼스 그래프.

- Path: `stageId` (int)
- Response: `StageDriverPerformanceGraphResponse`

### GET api/v1/stage/sport/{sport}/featured

주요 모터스포츠 스테이지.

- Path: `sport` (String)
- Response: `StagesListResponse`

### GET api/v1/stage/sport/{sportSlug}/scheduled/{date}

날짜별 스케줄.

- Path: `sportSlug` (String), `date` (`yyyy-MM-dd`)
- Response: `StagesListResponse`

### GET api/v1/team/{id}/driver-career-history

드라이버 커리어 히스토리.

- Path: `id` (int)
- Response: `StageTeamHistoryResponse`

---

# e스포츠 (Esports)

e스포츠 전용 엔드포인트.

---

### GET api/v1/event/{id}/esports-games

e스포츠 게임 목록 (BO3, BO5 등).

- Path: `id` (int)
- Response: `EsportsGamesResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/event/12345678/esports-games
```

### GET api/v1/esports-game/{id}/bans

게임 내 밴 목록 (LoL, Dota 등).

- Path: `id` (int)
- Response: `ESportsBansResponse`

### GET api/v1/esports-game/{id}/lineups

게임 내 라인업.

- Path: `id` (int)
- Response: `ESportsGameLineupsResponse`

### GET api/v1/esports-game/{id}/rounds

게임 내 라운드 (CS 등).

- Path: `id` (int)
- Response: `ESportsGameRoundsResponse`

### GET api/v1/esports-game/{id}/statistics

게임 내 통계.

- Path: `id` (int)
- Response: `EsportsGameStatisticsResponse`

---

# 검색 / 탐색

---

### GET api/v1/search/all

통합 검색.

- Query: `q` (검색어), `sport` (선택), `page` (int)
- Response: `SearchResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/search/all?q=son&page=0
GET https://api.sofascore.com/api/v1/search/all?q=tottenham&sport=football&page=0
```

### GET api/v1/search/{entityType}

엔티티 타입별 검색.

- Path: `entityType` (String: `players`, `teams`, `unique-tournaments`, `managers`, `venues`)
- Query: `q`, `sport` (선택), `page` (int)
- Response: `SearchResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/search/players?q=ohtani&sport=baseball&page=0
```

---

# 감독 / 심판 / 경기장

---

### GET api/v1/manager/{id}

감독 상세 정보.

- Path: `id` (int)
- Response: `ManagerDetailsResponse`

### GET api/v1/manager/{id}/career-history

감독 경력.

- Path: `id` (int)
- Response: `CareerHistoryResponse`

### GET api/v1/referee/{id}

심판 상세 정보.

- Path: `id` (int)
- Response: `RefereeDetailsResponse`

### GET api/v1/referee/{id}/statistics

심판 통계 (카드 발급 횟수 등).

- Path: `id` (int)
- Response: `RefereeStatisticsResponse`

### GET api/v1/venue/{id}

경기장 정보.

- Path: `id` (int)
- Response: `VenueResponse`

### GET api/v1/venue/{id}/events/{sport}/{span}/{page}

경기장별 경기 목록.

- Path: `id` (int), `sport` (String), `span` (`next`/`last`), `page` (int)
- Response: `EventListResponse`

---

# 인증 / 사용자

---

### POST api/v1/token/init

토큰 초기화 (앱 최초 실행).

- Body: `UserInfoRequest`
- Response: `UserInitResponse`

### POST api/v1/token/refresh

토큰 갱신.

- Response: `UserInitResponse`

### POST api/v1/user/login

소셜 로그인.

- Body: `HashMap<String, String>`
- Response: `SyncNetworkResponse`

### POST api/v1/user/logout

로그아웃.

### POST api/v1/user/sync

사용자 데이터 동기화.

- Response: `SyncNetworkResponse`

### POST api/v1/user/teams

팔로우 팀 동기화.

- Body: `HashSet<Integer>` (팀 ID 목록)

### POST api/v1/user/players

팔로우 선수 동기화.

- Body: `HashSet<Integer>` (선수 ID 목록)

### POST api/v1/user/leagues

팔로우 리그 동기화.

- Body: `HashSet<Integer>` (리그 ID 목록)

### POST api/v1/user/pinned-leagues

고정 리그 설정.

- Body: `Set<Integer>`

---

# 판타지

---

### GET api/v1/fantasy/competition/active-competitions

진행 중인 판타지 대회 목록.

- Query: `type` (Integer, 선택)
- Response: `FantasyCompetitionsResponse`

### GET api/v1/fantasy/competition/{id}

판타지 대회 상세.

- Path: `id` (int)
- Response: `FantasyCompetitionResponse`

### GET api/v1/fantasy/round/{id}/players

라운드 선수 목록.

- Path: `id` (int)
- Query: `page`, `q`, `position`, `teamId`, `maxPrice`, `sortParam`, `sortOrder`
- Response: `FantasyCompetitionPlayersResponse`

### POST api/v1/fantasy/competition/{id}/squad/create

판타지 팀 생성.

- Path: `id` (int)
- Body: `FantasyCreateTeamPostBody`
- Response: `FantasyCreateTeamResponse`

### POST api/v1/fantasy/competition/{id}/squad/transfer

판타지 선수 이적.

- Path: `id` (int)
- Body: `FantasyTransfersPostBody`
- Response: `FantasySquadResponse`

---

# 예측 게임 (Toto)

---

### GET api/v1/toto/tournament/all

전체 예측 토너먼트 목록.

- Response: `TotoTournamentsResponse`

### GET api/v1/toto/round/{id}/events

예측 라운드 경기 목록.

- Path: `id` (int)
- Response: `TotoRoundEventsResponse`

### POST api/v1/toto/round/{id}/predict

예측 제출.

- Path: `id` (int)
- Body: `List<TotoEventPredictionPostBody>`
- Response: `TotoUserRoundPredictionsResponse`

### GET api/v1/toto/tournament/{id}/leaderboard

예측 리더보드.

- Path: `id` (int)
- Response: `TotoLeaderboardResponse`

---

# 기타

---

### GET api/v1/country/alpha2

현재 사용자 국가 확인.

- Response: `UserRegionResponse`

### GET api/v1/config/default-unique-tournaments/{countryCode}

국가별 기본 고정 리그.

- Path: `countryCode` (String)
- Response: `DefaultPinnedLeaguesResponse`

```
호출 예시:
GET https://api.sofascore.com/api/v1/config/default-unique-tournaments/KR
```

### POST api/v1/app/feedback

앱 피드백 전송.

- Body: `FeedbackPost`

### POST api/v1/stream/token

스트리밍 토큰 발급.

- Response: `SportRadarTokenResponse`

### GET api/v1/chat/topic/{id}

채팅 메시지 조회.

- Path: `id` (String)
- Response: `ChatMessagesResponse`

### POST api/v1/chat/topic/{id}

채팅 메시지 전송.

- Path: `id` (String)
- Body: `PostChatMessage`

---

## 엔드포인트 수 요약

| 인터페이스                     | GET      | POST    | PUT    | DELETE | HEAD    | PATCH  | 합계       |
| ------------------------- | -------- | ------- | ------ | ------ | ------- | ------ | -------- |
| NetworkCoroutineAPI       | ~195     | ~18     | ~4     | ~3     | —       | —      | ~220     |
| NetworkHeadAPI            | 1        | —       | —      | —      | ~54     | —      | ~55      |
| FantasyAPI                | ~35      | ~10     | 1      | 2      | 2       | 2      | ~52      |
| FantasyHeadAPI            | —        | —       | —      | —      | 6       | —      | 6        |
| RegistrationCoroutinesAPI | —        | 20      | —      | —      | —       | —      | 20       |
| TotoAPI                   | 8        | 1       | 1      | 1      | —       | —      | 11       |
| **합계**                    | **~239** | **~49** | **~6** | **~6** | **~62** | **~2** | **~364** |
