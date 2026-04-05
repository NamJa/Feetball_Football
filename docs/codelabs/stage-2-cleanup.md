# Stage 2 / Cleanup — 레거시 코드 전면 제거

> ⏱ 예상 소요 시간: 2시간 | 난이도: ★☆☆ | 선행 조건: Stage 2 Navigation 완료, 모든 Compose 화면 정상 동작 확인

---

## 이 Codelab에서 배우는 것

- Fragment/XML 기반 레거시 코드를 안전하게 식별하고 제거하는 절차
- 삭제 후 빌드/Lint 검증을 통한 참조 누락 탐지
- Gradle 의존성 정리 (Groovy DSL → Kotlin DSL 전환 포함)
- Android 리소스(layout, drawable, values) 정리 기준과 판단 방법
- 대규모 코드 삭제 시 단계별 커밋 전략

---

## 완성 후 결과물

- `app` 모듈에 레거시 코드가 전혀 없는 상태 (~50 LOC: `MainActivity` + Application 클래스만 잔존)
- 모든 화면 로직은 `core-*/feature-*` 모듈에 분산
- 불필요한 라이브러리 의존성 제거 (GSON, Picasso, ThreeTenABP, RecyclerView 등)
- XML Layout 20개, Fragment 10개, Adapter 2개, ViewModel 3개, Behavior 3개, API 모델 전부 삭제

### 삭제 대상 총 요약

| 카테고리 | 파일 수 | 위치 |
|----------|---------|------|
| Fragment | 10개 | `app/src/main/java/com/example/feetballfootball/fragment/` |
| XML Layout | 20개 | `app/src/main/res/layout/` |
| Adapter | 2개 | `app/src/main/java/com/example/feetballfootball/adapter/` |
| Behavior | 3개 | `app/src/main/java/com/example/feetballfootball/behavior/` |
| ViewModel | 3개 | `app/src/main/java/com/example/feetballfootball/viewModel/` |
| API/Model | 40+개 | `app/src/main/java/com/example/feetballfootball/api/` |
| Utility | 2개 | `app/src/main/java/com/example/feetballfootball/util/` |
| Drawable | ~14개 | `app/src/main/res/drawable/` |
| Values | ~4개 | `app/src/main/res/values/`, `values-night/` |

---

## ⚠️ 제거 전 필수 확인

> 이 체크리스트를 **모두 통과한 후에만** 삭제 작업을 시작하세요. 하나라도 실패하면 Navigation 또는 Screen 구현을 먼저 수정해야 합니다.

- [ ] **모든 화면 정상 동작** — 경기 일정, 경기 상세 (이벤트/라인업/통계 3탭), 리그 선택, 리그 순위, 뉴스
- [ ] **Navigation 정상** — Bottom Navigation 3탭 전환, 상세 화면 진입/복귀, 시스템 Back 버튼
- [ ] **빌드 성공** — `./gradlew assembleDebug` 에러 없음
- [ ] **별도 브랜치에서 작업** — `feature/renewal-cleanup`에서 제거 후 검증

```bash
# 브랜치 생성 및 전환
git checkout -b feature/renewal-cleanup
```

> 💡 **Tip:** 삭제 작업은 되돌리기 어려우므로, 별도 브랜치에서 작업하고 모든 검증 후 merge하는 것이 안전합니다. 각 Step 완료 후 중간 커밋을 남기면 문제 발생 시 특정 Step으로 롤백할 수 있습니다.

---

## Step 1 — Fragment 파일 제거 (10개)

### 목표

> 기존 Fragment 기반 UI 코드를 모두 삭제한다. 이 파일들은 Compose Screen으로 100% 대체되었다.

### 배경

기존 앱은 Single Activity + Fragment 패턴이었습니다. `MainActivity`가 `supportFragmentManager`로 Fragment를 교체하며 화면을 전환했습니다. Navigation 3 통합이 완료되었으므로 모든 Fragment가 불필요합니다.

### 작업 내용

#### 1.1 Fixture 관련 Fragment (5개)

아래 5개 파일을 삭제합니다:

```
삭제: app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureFragment.kt
삭제: app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailFragment.kt
삭제: app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailEventsFragment.kt
삭제: app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailLineupFragment.kt
삭제: app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailStatisticsFragment.kt
```

| 삭제 파일 | Compose 대체 |
|-----------|-------------|
| `FixtureFragment.kt` (166줄) | `feature.fixture.FixtureScreen` |
| `FixtureDetailFragment.kt` (268줄) | `feature.fixturedetail.FixtureDetailScreen` |
| `FixtureDetailEventsFragment.kt` (168줄) | `feature.fixturedetail` 내부 Events 탭 |
| `FixtureDetailLineupFragment.kt` (170줄) | `feature.fixturedetail` 내부 Lineup 탭 |
| `FixtureDetailStatisticsFragment.kt` (256줄) | `feature.fixturedetail` 내부 Statistics 탭 |

```bash
rm app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureFragment.kt
rm app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailFragment.kt
rm app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailEventsFragment.kt
rm app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailLineupFragment.kt
rm app/src/main/java/com/example/feetballfootball/fragment/fixture/FixtureDetailStatisticsFragment.kt
```

#### 1.2 League 관련 Fragment (4개)

아래 4개 파일을 삭제합니다:

```
삭제: app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeaguesFragment.kt
삭제: app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeagueStandingFragment.kt
삭제: app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeagueClubsStandingFragment.kt
삭제: app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeaguePlayerStandingFragment.kt
```

| 삭제 파일 | Compose 대체 |
|-----------|-------------|
| `LeaguesFragment.kt` (79줄) | `feature.league.LeagueListScreen` |
| `LeagueStandingFragment.kt` (136줄) — ViewPager2 호스트 | `feature.league.standing.StandingScreen` |
| `LeagueClubsStandingFragment.kt` (136줄) | `feature.league.standing` 내부 클럽 순위 탭 |
| `LeaguePlayerStandingFragment.kt` (169줄) | `feature.league.standing` 내부 선수 순위 탭 |

```bash
rm app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeaguesFragment.kt
rm app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeagueStandingFragment.kt
rm app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeagueClubsStandingFragment.kt
rm app/src/main/java/com/example/feetballfootball/fragment/Leagues/LeaguePlayerStandingFragment.kt
```

#### 1.3 News Fragment (1개)

```
삭제: app/src/main/java/com/example/feetballfootball/fragment/news/NewsFragment.kt
```

| 삭제 파일 | Compose 대체 |
|-----------|-------------|
| `NewsFragment.kt` (88줄) | `feature.news.NewsScreen` |

```bash
rm app/src/main/java/com/example/feetballfootball/fragment/news/NewsFragment.kt
```

> ⚠️ **주의:** `LeaguesFragment.kt`에는 `Callbacks` 인터페이스가 정의되어 있고, 기존 `MainActivity`에서 구현하고 있었습니다. 새 `MainActivity`에서는 이 인터페이스를 사용하지 않으므로 삭제해도 안전합니다. 만약 새 `MainActivity.kt`에 `LeaguesFragment.Callbacks` import가 남아있다면 함께 제거하세요.

### ✅ 검증

- [ ] `app/src/main/java/com/example/feetballfootball/fragment/` 디렉토리 아래 `.kt` 파일이 0개
- [ ] `./gradlew assembleDebug` — Fragment import 참조 에러가 없는지 확인
- [ ] 중간 커밋: `git commit -m "chore: Fragment 10개 삭제"`

---

## Step 2 — XML Layout 파일 제거 (20개)

### 목표

> Fragment가 사용하던 XML 레이아웃과 RecyclerView 아이템 레이아웃을 모두 삭제한다.

### 배경

Compose에서는 UI를 Kotlin 코드로 선언하므로 XML Layout이 불필요합니다. `LazyColumn`이 RecyclerView를, `Column`/`Row`가 LinearLayout을, `Box`가 FrameLayout을 대체합니다.

### 작업 내용

#### 2.1 Fragment 레이아웃 (10개)

```
삭제: app/src/main/res/layout/fragment_fixture.xml
삭제: app/src/main/res/layout/fragment_fixture_detail.xml
삭제: app/src/main/res/layout/fragment_fixture_detail_events.xml
삭제: app/src/main/res/layout/fragment_fixture_detail_lineup.xml
삭제: app/src/main/res/layout/fragment_fixture_detail_statistics.xml
삭제: app/src/main/res/layout/fragment_leagues.xml
삭제: app/src/main/res/layout/fragment_league_standing.xml
삭제: app/src/main/res/layout/fragment_league_clubs_standing.xml
삭제: app/src/main/res/layout/fragment_league_player_standing.xml
삭제: app/src/main/res/layout/fragment_news.xml
```

```bash
rm app/src/main/res/layout/fragment_fixture.xml
rm app/src/main/res/layout/fragment_fixture_detail.xml
rm app/src/main/res/layout/fragment_fixture_detail_events.xml
rm app/src/main/res/layout/fragment_fixture_detail_lineup.xml
rm app/src/main/res/layout/fragment_fixture_detail_statistics.xml
rm app/src/main/res/layout/fragment_leagues.xml
rm app/src/main/res/layout/fragment_league_standing.xml
rm app/src/main/res/layout/fragment_league_clubs_standing.xml
rm app/src/main/res/layout/fragment_league_player_standing.xml
rm app/src/main/res/layout/fragment_news.xml
```

#### 2.2 RecyclerView 아이템 레이아웃 (9개)

```
삭제: app/src/main/res/layout/fixture.xml                              ← 경기 아이템
삭제: app/src/main/res/layout/league_fixture.xml                       ← 리그 헤더 + 경기 그룹
삭제: app/src/main/res/layout/events_recycler_item.xml                 ← 이벤트 아이템
삭제: app/src/main/res/layout/lineup_player_recycler_item.xml          ← 라인업 선수
삭제: app/src/main/res/layout/lineup_row_recycler_item.xml             ← 라인업 행
삭제: app/src/main/res/layout/lineup_substitute_player_recycler_item.xml ← 교체 선수
삭제: app/src/main/res/layout/standing_item.xml                        ← 순위 아이템
삭제: app/src/main/res/layout/scorer_recycler_item.xml                 ← 득점 아이템
삭제: app/src/main/res/layout/assist_recycler_item.xml                 ← 어시스트 아이템
```

```bash
rm app/src/main/res/layout/fixture.xml
rm app/src/main/res/layout/league_fixture.xml
rm app/src/main/res/layout/events_recycler_item.xml
rm app/src/main/res/layout/lineup_player_recycler_item.xml
rm app/src/main/res/layout/lineup_row_recycler_item.xml
rm app/src/main/res/layout/lineup_substitute_player_recycler_item.xml
rm app/src/main/res/layout/standing_item.xml
rm app/src/main/res/layout/scorer_recycler_item.xml
rm app/src/main/res/layout/assist_recycler_item.xml
```

#### 2.3 Activity 레이아웃 (1개)

새 `MainActivity`가 `setContent { FeetballApp() }`만 호출하고 `setContentView()`를 호출하지 않으므로, `activity_main.xml`은 더 이상 사용되지 않습니다.

```
삭제: app/src/main/res/layout/activity_main.xml
```

```bash
rm app/src/main/res/layout/activity_main.xml
```

> ⚠️ **주의:** `activity_main.xml` 삭제 후, 기존 `MainActivity.kt` (레거시)에서 `R.layout.activity_main`을 참조하는 코드가 남아있지 않은지 확인하세요. 새 `MainActivity.kt`에서는 `setContent {}`를 사용하므로 참조가 없어야 합니다.

> 💡 **Tip:** 삭제 후 `app/src/main/res/layout/` 디렉토리가 비어있다면 디렉토리 자체도 삭제해도 됩니다. 단, 향후 Compose가 아닌 XML이 필요한 경우(예: Widget, Custom View)를 대비해 남겨둬도 무방합니다.

### ✅ 검증

- [ ] `app/src/main/res/layout/` 디렉토리에 XML 파일이 0개 (또는 디렉토리 자체 삭제)
- [ ] `./gradlew assembleDebug` — `R.layout.*` 참조 에러가 없는지 확인
- [ ] 중간 커밋: `git commit -m "chore: XML Layout 20개 삭제"`

---

## Step 3 — Adapter 파일 제거 (2개 + Fragment 내부 어댑터)

### 목표

> RecyclerView Adapter 파일을 삭제한다. Compose의 `LazyColumn`/`LazyRow`가 RecyclerView + Adapter 패턴을 완전히 대체한다.

### 작업 내용

#### 3.1 독립 Adapter 파일 (2개)

```
삭제: app/src/main/java/com/example/feetballfootball/adapter/FixtureRecyclerViewAdapter.kt
삭제: app/src/main/java/com/example/feetballfootball/adapter/PlayerLineupAdapter.kt
```

| 삭제 파일 | Compose 대체 |
|-----------|-------------|
| `FixtureRecyclerViewAdapter.kt` | `FixtureScreen` 내부 `LazyColumn` + `FixtureItem` Composable |
| `PlayerLineupAdapter.kt` | `FixtureDetailScreen` 내부 라인업 `LazyColumn` |

```bash
rm app/src/main/java/com/example/feetballfootball/adapter/FixtureRecyclerViewAdapter.kt
rm app/src/main/java/com/example/feetballfootball/adapter/PlayerLineupAdapter.kt
```

#### 3.2 Fragment 내부 어댑터 (이미 삭제됨)

Fragment 파일 내에 `inner class` 또는 `private class`로 정의된 어댑터들은 Step 1에서 Fragment와 함께 이미 삭제되었습니다:

- `FixtureDetailEventsFragment` 내부 이벤트 어댑터
- `FixtureDetailStatisticsFragment` 내부 통계 어댑터
- `LeagueClubsStandingFragment` 내부 순위 어댑터
- `LeaguePlayerStandingFragment` 내부 선수 어댑터
- `LeaguesFragment` 내부 리그 선택 어댑터

> 💡 **Tip:** `FixtureRecyclerViewAdapter.kt`에는 `Callbacks` 인터페이스가 정의되어 있었고, 기존 `MainActivity`가 이를 구현했습니다. 새 `MainActivity`에서는 Navigation 3 Route를 통해 화면 전환하므로 이 인터페이스가 불필요합니다.

### ✅ 검증

- [ ] `app/src/main/java/com/example/feetballfootball/adapter/` 디렉토리 아래 `.kt` 파일이 0개
- [ ] `./gradlew assembleDebug` 에러 없음

---

## Step 4 — Behavior 파일 제거 (3개)

### 목표

> 경기 상세 화면의 CoordinatorLayout 기반 헤더 애니메이션 파일을 삭제한다. Compose에서는 `LargeTopAppBar` + `TopAppBarScrollBehavior`로 대체되었다.

### 배경

기존 경기 상세 화면(`FixtureDetailFragment`)은 `CoordinatorLayout` + `AppBarLayout`을 사용하여 스크롤 시 헤더(팀 로고, 스코어)가 축소되는 애니메이션을 구현했습니다. 이를 위해 커스텀 `CoordinatorLayout.Behavior` 클래스 3개가 필요했습니다. Compose에서는 `TopAppBarScrollBehavior`가 동일한 효과를 선언적으로 제공합니다.

### 작업 내용

```
삭제: app/src/main/java/com/example/feetballfootball/behavior/BehaviorHomeTeam.kt
삭제: app/src/main/java/com/example/feetballfootball/behavior/BehaviorAwayTeam.kt
삭제: app/src/main/java/com/example/feetballfootball/behavior/BehaviorScoreTextView.kt
```

| 삭제 파일 | 역할 | Compose 대체 |
|-----------|------|-------------|
| `BehaviorHomeTeam.kt` | 홈팀 로고 스크롤 애니메이션 | `LargeTopAppBar` 내 Composable |
| `BehaviorAwayTeam.kt` | 원정팀 로고 스크롤 애니메이션 | `LargeTopAppBar` 내 Composable |
| `BehaviorScoreTextView.kt` | 스코어 텍스트 스크롤 애니메이션 | `TopAppBarScrollBehavior` |

```bash
rm app/src/main/java/com/example/feetballfootball/behavior/BehaviorHomeTeam.kt
rm app/src/main/java/com/example/feetballfootball/behavior/BehaviorAwayTeam.kt
rm app/src/main/java/com/example/feetballfootball/behavior/BehaviorScoreTextView.kt
```

### ✅ 검증

- [ ] `app/src/main/java/com/example/feetballfootball/behavior/` 디렉토리 아래 `.kt` 파일이 0개
- [ ] `./gradlew assembleDebug` 에러 없음

---

## Step 5 — 기존 ViewModel 제거 (3개)

### 목표

> 레거시 ViewModel을 삭제한다. 새 ViewModel이 각 feature 모듈에 이미 존재한다.

### 배경

기존 ViewModel은 `com.example.feetballfootball.viewModel` 패키지에 있었으며, `thread {}` 블록으로 API를 호출하고 `MutableLiveData`로 결과를 전달했습니다. 새 ViewModel은 `com.chase1st.feetballfootball.feature.*` 패키지에 있으며, Kotlin Coroutines + `StateFlow`를 사용합니다.

### 작업 내용

```
삭제: app/src/main/java/com/example/feetballfootball/viewModel/FeetballFootballViewModel.kt
삭제: app/src/main/java/com/example/feetballfootball/viewModel/FixtureDetailViewModel.kt
삭제: app/src/main/java/com/example/feetballfootball/viewModel/StandingViewModel.kt
```

| 삭제 파일 | 새 ViewModel 위치 |
|-----------|------------------|
| `FeetballFootballViewModel.kt` | `feature.fixture` 모듈의 `FixtureViewModel` |
| `FixtureDetailViewModel.kt` | `feature.fixturedetail` 모듈의 `FixtureDetailViewModel` |
| `StandingViewModel.kt` | `feature.league` 모듈의 `StandingViewModel` |

```bash
rm app/src/main/java/com/example/feetballfootball/viewModel/FeetballFootballViewModel.kt
rm app/src/main/java/com/example/feetballfootball/viewModel/FixtureDetailViewModel.kt
rm app/src/main/java/com/example/feetballfootball/viewModel/StandingViewModel.kt
```

> ⚠️ **주의:** 삭제 전, 새 ViewModel이 기존 ViewModel의 모든 기능을 구현하고 있는지 확인하세요. 특히 `FeetballFootballViewModel`에 정의된 리그 ID 상수(`EPL=39`, `LALIGA=140` 등)가 새 코드에도 존재하는지 체크하세요.

### ✅ 검증

- [ ] `app/src/main/java/com/example/feetballfootball/viewModel/` 디렉토리 아래 `.kt` 파일이 0개
- [ ] `./gradlew assembleDebug` 에러 없음

---

## Step 6 — API 모델 및 네트워크 코드 제거

### 목표

> 기존 Retrofit API 인터페이스, 네트워크 유틸리티, API 응답 모델 클래스를 모두 삭제한다. 이 코드들은 `core-network` 모듈로 이동/재작성되었다.

### 배경

기존 네트워크 코드(API-Sports v3 기반)의 문제점:
- `FootballDataFetchr`에 `x-apisports-key` API 키가 하드코딩
- GSON으로 JSON 파싱 (kotlinx.serialization으로 대체)
- `thread {}` 블록으로 비동기 처리 (Coroutines으로 대체)
- 40+개의 모델 클래스가 flat한 패키지 구조

새 `core-network` 모듈에서는 SofaScore API(`https://api.sofascore.com/api/v1/`) + Retrofit 3.0.0 + kotlinx.serialization + Coroutines로 재구성되었습니다. SofaScore API는 Bearer Token 인증을 사용하며, GET 엔드포인트는 인증 없이도 동작합니다.

### 작업 내용

#### 6.1 API 인터페이스 + 유틸리티 (2개)

```
삭제: app/src/main/java/com/example/feetballfootball/api/FootballApi.kt
삭제: app/src/main/java/com/example/feetballfootball/util/FootballDataFetchr.kt
```

```bash
rm app/src/main/java/com/example/feetballfootball/api/FootballApi.kt
rm app/src/main/java/com/example/feetballfootball/util/FootballDataFetchr.kt
```

#### 6.2 최상위 API 모델 클래스 (11개)

```
삭제: app/src/main/java/com/example/feetballfootball/api/Errors.kt
삭제: app/src/main/java/com/example/feetballfootball/api/Fixture.kt
삭제: app/src/main/java/com/example/feetballfootball/api/FixtureResponse.kt
삭제: app/src/main/java/com/example/feetballfootball/api/FixtureStatus.kt
삭제: app/src/main/java/com/example/feetballfootball/api/FixtureVenue.kt
삭제: app/src/main/java/com/example/feetballfootball/api/FootballResponse.kt
삭제: app/src/main/java/com/example/feetballfootball/api/Goals.kt
삭제: app/src/main/java/com/example/feetballfootball/api/League.kt
삭제: app/src/main/java/com/example/feetballfootball/api/TeamAway.kt
삭제: app/src/main/java/com/example/feetballfootball/api/TeamHome.kt
삭제: app/src/main/java/com/example/feetballfootball/api/Teams.kt
```

```bash
rm app/src/main/java/com/example/feetballfootball/api/Errors.kt
rm app/src/main/java/com/example/feetballfootball/api/Fixture.kt
rm app/src/main/java/com/example/feetballfootball/api/FixtureResponse.kt
rm app/src/main/java/com/example/feetballfootball/api/FixtureStatus.kt
rm app/src/main/java/com/example/feetballfootball/api/FixtureVenue.kt
rm app/src/main/java/com/example/feetballfootball/api/FootballResponse.kt
rm app/src/main/java/com/example/feetballfootball/api/Goals.kt
rm app/src/main/java/com/example/feetballfootball/api/League.kt
rm app/src/main/java/com/example/feetballfootball/api/TeamAway.kt
rm app/src/main/java/com/example/feetballfootball/api/TeamHome.kt
rm app/src/main/java/com/example/feetballfootball/api/Teams.kt
```

#### 6.3 경기 상세 API 모델 클래스 (`api/fixturedetail/`, 17개)

```
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Assist.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Coach.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/EventPlayer.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Events.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/FixtureDetailResponse.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Games.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/LineupTeam.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Lineups.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/MatchDetailResponse.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Player.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/PlayerData.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/PlayerRatingData.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/PlayerStatistics.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/PlayersByTeamData.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/ShortPlayerData.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Statistics.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/StatisticsData.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/TeamColorCode.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/TeamColors.kt
삭제: app/src/main/java/com/example/feetballfootball/api/fixturedetail/Time.kt
```

```bash
rm -r app/src/main/java/com/example/feetballfootball/api/fixturedetail/
```

#### 6.4 리그 순위 API 모델 클래스 (`api/leaguestanding/`, 7개)

```
삭제: app/src/main/java/com/example/feetballfootball/api/leaguestanding/All.kt
삭제: app/src/main/java/com/example/feetballfootball/api/leaguestanding/Goals.kt
삭제: app/src/main/java/com/example/feetballfootball/api/leaguestanding/League.kt
삭제: app/src/main/java/com/example/feetballfootball/api/leaguestanding/LeagueStandingsResponse.kt
삭제: app/src/main/java/com/example/feetballfootball/api/leaguestanding/StandingResponse.kt
삭제: app/src/main/java/com/example/feetballfootball/api/leaguestanding/Standings.kt
삭제: app/src/main/java/com/example/feetballfootball/api/leaguestanding/Team.kt
```

```bash
rm -r app/src/main/java/com/example/feetballfootball/api/leaguestanding/
```

#### 6.5 선수 순위 API 모델 클래스 (`api/playerstanding/`, 8개)

```
삭제: app/src/main/java/com/example/feetballfootball/api/playerstanding/Goals.kt
삭제: app/src/main/java/com/example/feetballfootball/api/playerstanding/Penalty.kt
삭제: app/src/main/java/com/example/feetballfootball/api/playerstanding/Player.kt
삭제: app/src/main/java/com/example/feetballfootball/api/playerstanding/PlayerStandingResponse.kt
삭제: app/src/main/java/com/example/feetballfootball/api/playerstanding/PlayerStandingStatistics.kt
삭제: app/src/main/java/com/example/feetballfootball/api/playerstanding/PlayerStatistics.kt
삭제: app/src/main/java/com/example/feetballfootball/api/playerstanding/Shots.kt
```

```bash
rm -r app/src/main/java/com/example/feetballfootball/api/playerstanding/
```

### ✅ 검증

- [ ] `app/src/main/java/com/example/feetballfootball/api/` 디렉토리 아래 파일이 0개
- [ ] `app/src/main/java/com/example/feetballfootball/util/FootballDataFetchr.kt` 삭제됨
- [ ] `./gradlew assembleDebug` — 모든 기존 import 참조 에러가 없는지 확인
- [ ] 중간 커밋: `git commit -m "chore: API 모델 및 네트워크 코드 삭제"`

---

## Step 7 — 유틸리티 및 기타 파일 제거

### 목표

> 남은 유틸리티 파일과 XML 전용 drawable 리소스를 삭제한다.

### 작업 내용

#### 7.1 DividerItemDecoration (1개)

Compose에서는 `HorizontalDivider()` / `VerticalDivider()` Composable을 사용합니다.

```
삭제: app/src/main/java/com/example/feetballfootball/util/DividerItemDecoration.kt
```

```bash
rm app/src/main/java/com/example/feetballfootball/util/DividerItemDecoration.kt
```

#### 7.2 Drawable 리소스 정리

**삭제 가능 — RecyclerView/XML 전용 (6개):**

| 파일 | 용도 | Compose 대체 |
|------|------|-------------|
| `fixture_recycler_item_bg.xml` | RecyclerView 아이템 배경 | `Card` 또는 `Surface` |
| `recyclerview_divider.xml` | RecyclerView 구분선 | `HorizontalDivider()` |
| `circle_progressbar.xml` | XML ProgressBar | `CircularProgressIndicator` |
| `horizon_progressbar.xml` | XML ProgressBar | `LinearProgressIndicator` |
| `tab_selector.xml` | TabLayout 셀렉터 | `TabRow` + `Tab` |
| `drawable_line.xml` | XML 구분선 | `HorizontalDivider()` |

```bash
rm app/src/main/res/drawable/fixture_recycler_item_bg.xml
rm app/src/main/res/drawable/recyclerview_divider.xml
rm app/src/main/res/drawable/circle_progressbar.xml
rm app/src/main/res/drawable/horizon_progressbar.xml
rm app/src/main/res/drawable/tab_selector.xml
rm app/src/main/res/drawable/drawable_line.xml
```

**삭제 가능 — Compose로 대체 (8개):**

| 파일 | 용도 | Compose 대체 |
|------|------|-------------|
| `stadium_away_penalty_area_in.xml` | 라인업 경기장 배경 | `Canvas` + `Path` |
| `stadium_away_penalty_area_out.xml` | 라인업 경기장 배경 | `Canvas` + `Path` |
| `stadium_halfline.xml` | 라인업 경기장 배경 | `Canvas` + `Path` |
| `stadium_halfline_circle.xml` | 라인업 경기장 배경 | `Canvas` + `Path` |
| `stadium_home_penalty_area_in.xml` | 라인업 경기장 배경 | `Canvas` + `Path` |
| `stadium_home_penalty_area_out.xml` | 라인업 경기장 배경 | `Canvas` + `Path` |
| `player_face_bg_circle.xml` | 선수 이미지 원형 배경 | `CircleShape` |
| `player_rating_bg_circle.xml` | 선수 평점 원형 배경 | `CircleShape` |

```bash
rm app/src/main/res/drawable/stadium_away_penalty_area_in.xml
rm app/src/main/res/drawable/stadium_away_penalty_area_out.xml
rm app/src/main/res/drawable/stadium_halfline.xml
rm app/src/main/res/drawable/stadium_halfline_circle.xml
rm app/src/main/res/drawable/stadium_home_penalty_area_in.xml
rm app/src/main/res/drawable/stadium_home_penalty_area_out.xml
rm app/src/main/res/drawable/player_face_bg_circle.xml
rm app/src/main/res/drawable/player_rating_bg_circle.xml
```

**삭제 가능 — Material Icons로 대체된 아이콘:**

Compose에서 `Icons.Default.*`를 사용하므로 아래 아이콘 XML은 삭제합니다. 단, **삭제 전 Compose 코드에서 `painterResource(R.drawable.*)`로 참조하는 곳이 없는지 확인**하세요.

| 파일 | 용도 | Compose 대체 |
|------|------|-------------|
| `ic_fixture.xml` | Bottom Nav 아이콘 | `Icons.Default.SportsSoccer` |
| `ic_league.xml` | Bottom Nav 아이콘 | `Icons.Default.EmojiEvents` |
| `ic_news.xml` | Bottom Nav 아이콘 | `Icons.Default.Newspaper` |
| `ic_arrow_back.xml` | TopAppBar 뒤로가기 | `Icons.Default.ArrowBack` |
| `ic_arrow_forward.xml` | 날짜 선택 화살표 | `Icons.Default.ArrowForward` |
| `arrow_circle_up.xml` | 이벤트 아이콘 (교체 IN) | `Icons.Default.ArrowCircleUp` 또는 커스텀 |
| `arrow_circle_down.xml` | 이벤트 아이콘 (교체 OUT) | `Icons.Default.ArrowCircleDown` 또는 커스텀 |
| `soccer_ball.xml` | 골 아이콘 | 커스텀 Vector 또는 Painter |
| `red_card.xml` | 레드카드 아이콘 | 커스텀 Composable |
| `yellow_card.xml` | 옐로카드 아이콘 | 커스텀 Composable |
| `stadium_penalty_area_arc.xml` | 경기장 아크 | `Canvas` + `Path` |

```bash
rm app/src/main/res/drawable/ic_fixture.xml
rm app/src/main/res/drawable/ic_league.xml
rm app/src/main/res/drawable/ic_news.xml
rm app/src/main/res/drawable/ic_arrow_back.xml
rm app/src/main/res/drawable/ic_arrow_forward.xml
rm app/src/main/res/drawable/arrow_circle_up.xml
rm app/src/main/res/drawable/arrow_circle_down.xml
rm app/src/main/res/drawable/soccer_ball.xml
rm app/src/main/res/drawable/red_card.xml
rm app/src/main/res/drawable/yellow_card.xml
rm app/src/main/res/drawable/stadium_penalty_area_arc.xml
```

**유지 — 앱 아이콘 및 PNG 리소스:**

아래 파일들은 삭제하지 않습니다:

| 파일 | 이유 |
|------|------|
| `ic_launcher_background.xml` | 앱 런처 아이콘 — `AndroidManifest.xml`에서 참조 |
| `barricade_128.png` | 특수 이미지 — Compose에서도 사용 가능 |
| `penalty_kick.png` | 특수 이미지 — Compose에서도 사용 가능 |
| `penalty_kick_missed.png` | 특수 이미지 — Compose에서도 사용 가능 |
| `var_cancelled.png` | 특수 이미지 — Compose에서도 사용 가능 |
| `var_sign.png` | 특수 이미지 — Compose에서도 사용 가능 |

> ⚠️ **주의:** PNG 파일(`barricade_128.png`, `penalty_kick.png` 등)이 Compose 코드에서 `painterResource(R.drawable.*)`로 사용되고 있다면 반드시 유지하세요. 이 파일들은 `soccer_ball.xml` 같은 벡터 드로어블과 달리 Compose에서 직접 사용할 수 있는 비트맵 리소스입니다.

> 💡 **Tip:** 어떤 drawable이 사용 중인지 확실하지 않다면, `./gradlew lint`를 실행하여 "Unused resources" 경고를 확인하는 것이 가장 안전합니다. Lint는 코드와 XML에서 참조되지 않는 리소스를 정확하게 탐지합니다.

### ✅ 검증

- [ ] `DividerItemDecoration.kt` 삭제됨
- [ ] RecyclerView/XML 전용 drawable 6개 삭제됨
- [ ] Compose 대체 drawable 8개 (stadium_*, player_*) 삭제됨
- [ ] Material Icons 대체 아이콘 11개 삭제됨
- [ ] 앱 아이콘(`ic_launcher_background.xml`) 유지됨
- [ ] PNG 파일 유지됨
- [ ] `./gradlew assembleDebug` 에러 없음

---

## Step 8 — 레거시 의존성 정리 (build.gradle)

### 목표

> Fragment/XML/RecyclerView 시대의 라이브러리 의존성을 제거하고, app/build.gradle를 Convention Plugin 기반 Kotlin DSL로 교체한다.

### 배경

기존 `app/build.gradle`(Groovy DSL)에는 이제 불필요한 라이브러리가 다수 포함되어 있습니다. 멀티모듈 구조에서는 각 모듈이 필요한 의존성만 선언하고, app 모듈은 feature 모듈에 대한 의존성만 가집니다.

### 작업 내용

#### 8.1 제거할 의존성 목록

| 기존 의존성 | 대체 | 상태 |
|------------|------|------|
| `com.squareup.retrofit2:retrofit:2.9.0` | core-network에 Retrofit 3.0.0 (kotlinx-serialization 컨버터 내장) | 이동 |
| `com.squareup.retrofit2:converter-scalars:2.5.0` | 제거 (kotlinx.serialization 사용) | 삭제 |
| `com.squareup.retrofit2:converter-gson:2.9.0` | 제거 (kotlinx.serialization 사용) | 삭제 |
| `com.google.code.gson:gson:2.9.0` | 제거 (kotlinx.serialization 사용) | 삭제 |
| `com.squareup.picasso:picasso:2.71828` | Coil 3.x (core-designsystem) | 삭제 |
| `com.jakewharton.threetenabp:threetenabp:1.3.0` | java.time (minSdk 26이므로 백포트 불필요) | 삭제 |
| `androidx.legacy:legacy-support-v4:1.0.0` | 제거 (불필요) | 삭제 |
| `androidx.recyclerview:recyclerview:1.2.1` | Compose LazyColumn/LazyRow | 삭제 |
| `androidx.lifecycle:lifecycle-extensions:2.2.0` | lifecycle-viewmodel-compose | 삭제 |
| `androidx.constraintlayout:constraintlayout:2.1.4` | Compose Layout | 삭제 |
| `androidx.appcompat:appcompat:1.4.2` | ComponentActivity (appcompat 불필요) | 삭제 |
| `com.google.android.material:material:1.6.1` | Material 3 Compose | 삭제 |

#### 8.2 app/build.gradle.kts (새 구조)

기존 Groovy DSL 파일(`app/build.gradle`)을 삭제하고, Convention Plugin 기반 Kotlin DSL로 교체합니다:

**파일: `app/build.gradle.kts`**

```kotlin
// app/build.gradle.kts
// Convention Plugin이 compileSdk, minSdk, Compose 설정 등을 일괄 관리
plugins {
    id("feetball.android.application")
    id("feetball.android.compose")
    id("feetball.android.hilt")
}

dependencies {
    // app 모듈은 feature 모듈에만 의존
    // 각 feature 모듈이 필요한 core 모듈을 직접 의존
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(projects.feature.fixture)
    implementation(projects.feature.fixturDetail)
    implementation(projects.feature.league)
    implementation(projects.feature.news)
}
```

> 💡 **Tip:** Convention Plugin(`feetball.android.application` 등)은 Stage 1에서 `build-logic` 모듈에 정의되었습니다. 이 플러그인이 compileSdk, minSdk, Kotlin/JVM 타겟, Compose 컴파일러 등을 일괄 관리하므로, 각 모듈의 build.gradle.kts가 매우 간결해집니다.

> ⚠️ **주의:** `app/build.gradle` (Groovy)과 `app/build.gradle.kts` (Kotlin)가 동시에 존재하면 Gradle 에러가 발생합니다. 반드시 기존 `app/build.gradle`을 삭제한 후 `app/build.gradle.kts`를 사용하세요.

### ✅ 검증

- [ ] 기존 `app/build.gradle` (Groovy) 삭제됨
- [ ] `app/build.gradle.kts`에 레거시 의존성이 없음
- [ ] `./gradlew assembleDebug` 성공
- [ ] `./gradlew dependencies --configuration debugRuntimeClasspath`에서 GSON, Picasso, ThreeTenABP 등이 없음

---

## Step 9 — 빈 패키지 디렉토리 정리

### 목표

> 코드 파일 삭제 후 남은 빈 디렉토리를 제거하여 프로젝트 구조를 깔끔하게 만든다.

### 작업 내용

Step 1~7에서 모든 파일을 삭제한 후, 아래 디렉토리가 비어있을 것입니다:

```
삭제: app/src/main/java/com/example/feetballfootball/fragment/          ← 전체
삭제: app/src/main/java/com/example/feetballfootball/adapter/           ← 전체
삭제: app/src/main/java/com/example/feetballfootball/behavior/          ← 전체
삭제: app/src/main/java/com/example/feetballfootball/viewModel/         ← 전체
삭제: app/src/main/java/com/example/feetballfootball/api/               ← 전체
삭제: app/src/main/java/com/example/feetballfootball/util/              ← 전체
```

```bash
# 빈 디렉토리 일괄 삭제
rm -rf app/src/main/java/com/example/feetballfootball/fragment/
rm -rf app/src/main/java/com/example/feetballfootball/adapter/
rm -rf app/src/main/java/com/example/feetballfootball/behavior/
rm -rf app/src/main/java/com/example/feetballfootball/viewModel/
rm -rf app/src/main/java/com/example/feetballfootball/api/
rm -rf app/src/main/java/com/example/feetballfootball/util/
```

> 💡 **Tip:** 삭제 후 `app/src/main/java/com/example/feetballfootball/` 디렉토리에는 `MainActivity.kt`만 남아야 합니다. 이 `MainActivity.kt`도 새 패키지(`com.chase1st.feetballfootball`)로 이동된 후 최종적으로 삭제될 수 있습니다.

```bash
# 삭제 후 남은 파일 확인
ls app/src/main/java/com/example/feetballfootball/
# 예상 결과: MainActivity.kt (또는 비어있음 — 새 패키지로 이동 완료 시)
```

### ✅ 검증

- [ ] `fragment/`, `adapter/`, `behavior/`, `viewModel/`, `api/`, `util/` 디렉토리가 모두 삭제됨
- [ ] `./gradlew assembleDebug` 에러 없음

---

## Step 10 — Resources 정리

### 목표

> XML 테마, 색상, 크기 등 Compose로 대체된 values 리소스를 정리한다.

### 작업 내용

#### 10.1 values 리소스 검토

**파일: `app/src/main/res/values/`**

| 파일 | 판단 | 이유 |
|------|------|------|
| `colors.xml` | 삭제 | Compose Theme의 `FeetballColors`로 대체 |
| `themes.xml` | 삭제 | Compose `FeetballTheme`으로 대체 |
| `dimens.xml` | 삭제 | Compose에서 `dp` 직접 사용 |
| `strings.xml` | **유지** | `app_name` 등 `AndroidManifest.xml`에서 참조 |

```bash
rm app/src/main/res/values/colors.xml
rm app/src/main/res/values/themes.xml
rm app/src/main/res/values/dimens.xml
# strings.xml은 유지!
```

> ⚠️ **주의:** `strings.xml`의 `app_name` 문자열은 `AndroidManifest.xml`의 `android:label="@string/app_name"`에서 참조합니다. 이 파일을 삭제하면 빌드 에러가 발생합니다. 반드시 유지하세요.

#### 10.2 values-night 검토

**파일: `app/src/main/res/values-night/`**

| 파일 | 판단 | 이유 |
|------|------|------|
| `themes.xml` | 삭제 | Compose `DynamicColorScheme` / `darkColorScheme`으로 대체 |

```bash
rm app/src/main/res/values-night/themes.xml
# values-night 디렉토리가 비어있으면 디렉토리도 삭제
rm -rf app/src/main/res/values-night/
```

> 💡 **Tip:** Compose의 `FeetballTheme`이 `isSystemInDarkTheme()`을 사용하여 다크/라이트 모드를 자동으로 전환하므로, XML 기반 `values-night/themes.xml`은 완전히 불필요합니다.

### ✅ 검증

- [ ] `colors.xml`, `themes.xml`, `dimens.xml` 삭제됨
- [ ] `strings.xml` 유지됨 (`app_name` 등 포함)
- [ ] `values-night/themes.xml` 삭제됨
- [ ] `./gradlew assembleDebug` 에러 없음

---

## Step 11 — 최종 검증

### 목표

> 모든 레거시 코드 제거가 완료된 후 빌드, Lint, 앱 동작을 종합 검증한다.

### 작업 내용

#### 11.1 Clean 빌드 확인

```bash
./gradlew clean assembleDebug
```

Clean 빌드로 캐시 없이 전체 빌드가 성공하는지 확인합니다.

#### 11.2 Lint 검사

```bash
./gradlew lint
```

확인 항목:
- 사용되지 않는 리소스 경고
- 누락된 참조 에러
- 미사용 import

> 💡 **Tip:** Lint에서 "Unused resources" 경고가 나오면, 해당 리소스가 정말 사용되지 않는지 확인 후 추가 삭제합니다. 특히 PNG 파일이나 `strings.xml`의 문자열이 대상이 될 수 있습니다.

#### 11.3 앱 동작 확인

에뮬레이터 또는 실기기에서 아래 항목을 수동 테스트합니다:

- [ ] 앱 실행 정상 (크래시 없음)
- [ ] 경기 일정 화면 진입 가능
- [ ] 경기 상세 화면 진입 가능 (이벤트/라인업/통계 3탭 전환)
- [ ] 리그 선택 화면 진입 가능
- [ ] 리그 순위 화면 진입 가능
- [ ] 뉴스 화면 진입 가능
- [ ] Bottom Navigation 3탭 전환 동작
- [ ] 시스템 뒤로가기 버튼 동작
- [ ] 다크모드 전환 정상 (시스템 설정 변경 후)
- [ ] ProGuard/R8 (release 빌드 시, 해당되는 경우)

#### 11.4 코드 크기 비교

```
제거 전: app 모듈 ~5,300 LOC (Kotlin + XML)
제거 후: app 모듈 ~50 LOC (MainActivity + FeetballApp Application 클래스만 잔존)
         나머지 모든 코드는 core-*/feature-* 모듈에 분산
```

> 💡 **Tip:** 코드 크기를 확인하려면 다음 명령을 사용하세요:
> ```bash
> # Kotlin 파일 라인 수
> find app/src/main -name "*.kt" | xargs wc -l
> # XML 파일 라인 수
> find app/src/main -name "*.xml" | xargs wc -l
> ```

### ✅ 검증

- [ ] `./gradlew clean assembleDebug` 성공
- [ ] `./gradlew lint` 경고 없음 (또는 수용 가능한 수준)
- [ ] 앱 전체 기능 정상 동작 (위 체크리스트 전항목)
- [ ] 다크모드 전환 정상

---

## Cleanup 완료 검증 체크리스트

모든 Step이 완료되면 아래 항목을 최종 확인합니다:

### 코드 삭제 확인

- [ ] Fragment 10개 전부 삭제
- [ ] XML Layout 20개 전부 삭제
- [ ] Adapter 2개 + Fragment 내부 어댑터 전부 삭제
- [ ] Behavior 3개 전부 삭제
- [ ] 기존 ViewModel 3개 전부 삭제
- [ ] API 모델 전부 삭제 (최상위 11개 + fixturedetail 17개 + leaguestanding 7개 + playerstanding 7개 + FootballApi + FootballDataFetchr)
- [ ] DividerItemDecoration 삭제

### 리소스 삭제 확인

- [ ] 사용하지 않는 drawable 리소스 삭제 (25개)
- [ ] `colors.xml`, `themes.xml`, `dimens.xml` 삭제
- [ ] `values-night/themes.xml` 삭제
- [ ] `strings.xml` 유지 (app_name 등)

### 의존성 정리 확인

- [ ] 레거시 의존성 전부 제거 (GSON, Picasso, ThreeTenABP, RecyclerView, ConstraintLayout, AppCompat, Material 1.x)
- [ ] app/build.gradle.kts가 Convention Plugin 기반으로 전환됨

### 디렉토리 정리 확인

- [ ] `fragment/`, `adapter/`, `behavior/`, `viewModel/`, `api/`, `util/` 빈 디렉토리 삭제

### 빌드 및 동작 확인

- [ ] `./gradlew clean assembleDebug` 성공
- [ ] `./gradlew lint` 경고 없음
- [ ] 앱 전체 기능 정상 동작

### 커밋

```bash
git add .
git commit -m "chore: 레거시 코드 전면 제거 (Fragment/XML/Adapter/ViewModel)"
```

### 삭제된 파일 전체 요약

| 카테고리 | 파일 수 | 디렉토리 |
|----------|---------|----------|
| Fragment (Fixture) | 5 | `fragment/fixture/` |
| Fragment (League) | 4 | `fragment/Leagues/` |
| Fragment (News) | 1 | `fragment/news/` |
| XML Layout (Fragment) | 10 | `res/layout/fragment_*.xml` |
| XML Layout (RecyclerView) | 9 | `res/layout/*.xml` |
| XML Layout (Activity) | 1 | `res/layout/activity_main.xml` |
| Adapter | 2 | `adapter/` |
| Behavior | 3 | `behavior/` |
| ViewModel | 3 | `viewModel/` |
| API (최상위) | 11 | `api/` |
| API (fixturedetail) | 17 | `api/fixturedetail/` |
| API (leaguestanding) | 7 | `api/leaguestanding/` |
| API (playerstanding) | 7 | `api/playerstanding/` |
| API Interface | 1 | `api/FootballApi.kt` |
| Utility | 2 | `util/` |
| Drawable | ~25 | `res/drawable/` |
| Values | 3+1 | `res/values/`, `res/values-night/` |
| **합계** | **~110+** | |

---

> 이전 단계: [Stage 2 / Navigation — Navigation 3 통합](stage-2-navigation.md)
