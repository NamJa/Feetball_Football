# Stage 2 / Cleanup — 레거시 코드 제거

> **목표:** 기존 Fragment/XML/Adapter/ViewModel 등 레거시 코드 전면 제거
> **선행 조건:** Stage 2의 모든 Slice (1~5) + Navigation 완료, 모든 화면이 Compose로 동작 확인
> **Git 브랜치:** `feature/renewal-cleanup`
> **원칙:** 새 Compose 앱이 100% 독립적으로 동작하는 것을 확인한 후 제거

---

## ⚠️ 제거 전 필수 확인

1. **모든 화면 정상 동작** — 경기 일정, 경기 상세 (3탭), 리그 선택, 리그 순위, 뉴스
2. **Navigation 정상** — Bottom Navigation 3탭 전환, 상세 화면 진입/복귀, 시스템 Back 버튼
3. **빌드 성공** — `./gradlew assembleDebug` 에러 없음
4. **별도 브랜치에서 작업** — `feature/renewal-cleanup`에서 제거 후 검증

---

## Step 1 — Fragment 파일 제거 (10개)

### 1.1 Fixture 관련 Fragment (5개)

```
삭제 대상:
app/src/main/java/com/example/feetballfootball/fragment/fixture/
├── FixtureFragment.kt                    (166줄)
├── FixtureDetailFragment.kt              (268줄)
├── FixtureDetailEventsFragment.kt        (168줄)
├── FixtureDetailLineupFragment.kt        (170줄)
└── FixtureDetailStatisticsFragment.kt    (256줄)
```

### 1.2 League 관련 Fragment (4개)

```
삭제 대상:
app/src/main/java/com/example/feetballfootball/fragment/Leagues/
├── LeaguesFragment.kt                    (79줄)
├── LeagueStandingFragment.kt             (136줄)  ← ViewPager2 호스트
├── LeagueClubsStandingFragment.kt        (136줄)
└── LeaguePlayerStandingFragment.kt       (169줄)
```

### 1.3 News Fragment (1개)

```
삭제 대상:
app/src/main/java/com/example/feetballfootball/fragment/news/
└── NewsFragment.kt                       (88줄)
```

### 1.4 검증

```bash
./gradlew assembleDebug
# Fragment import 참조 에러가 없는지 확인
```

---

## Step 2 — XML Layout 파일 제거 (20개)

### 2.1 Fragment 레이아웃 (9개)

```
삭제 대상:
app/src/main/res/layout/
├── fragment_fixture.xml
├── fragment_fixture_detail.xml
├── fragment_fixture_detail_events.xml
├── fragment_fixture_detail_lineup.xml
├── fragment_fixture_detail_statistics.xml
├── fragment_leagues.xml
├── fragment_league_standing.xml
├── fragment_league_clubs_standing.xml
├── fragment_league_player_standing.xml
└── fragment_news.xml
```

### 2.2 RecyclerView 아이템 레이아웃 (8개)

```
삭제 대상:
app/src/main/res/layout/
├── fixture.xml                           ← 경기 아이템
├── league_fixture.xml                    ← 리그 헤더 + 경기 그룹
├── events_recycler_item.xml              ← 이벤트 아이템
├── lineup_player_recycler_item.xml       ← 라인업 선수
├── lineup_row_recycler_item.xml          ← 라인업 행
├── lineup_substitute_player_recycler_item.xml ← 교체 선수
├── standing_item.xml                     ← 순위 아이템
├── scorer_recycler_item.xml              ← 득점/어시스트 아이템
└── assist_recycler_item.xml              ← 어시스트 아이템
```

### 2.3 Activity 레이아웃 검토

```
app/src/main/res/layout/activity_main.xml
```

- 새 `MainActivity`가 `setContent { FeetballApp() }` 만 호출한다면, **activity_main.xml은 더 이상 사용되지 않으므로 삭제**
- `setContentView()`를 호출하지 않는지 확인 후 삭제

### 2.4 검증

```bash
./gradlew assembleDebug
# R.layout.* 참조 에러가 없는지 확인
```

---

## Step 3 — Adapter 파일 제거 (2개 + Fragment 내부 어댑터)

### 3.1 독립 Adapter 파일

```
삭제 대상:
app/src/main/java/com/example/feetballfootball/adapter/
├── FixtureRecyclerViewAdapter.kt
└── PlayerLineupAdapter.kt
```

### 3.2 Fragment 내부 어댑터

Fragment 파일 내에 `inner class` 또는 `private class`로 정의된 어댑터들은 Step 1에서 Fragment와 함께 삭제됩니다:

- `FixtureDetailEventsFragment` 내부 이벤트 어댑터
- `FixtureDetailStatisticsFragment` 내부 통계 어댑터
- `LeagueClubsStandingFragment` 내부 순위 어댑터
- `LeaguePlayerStandingFragment` 내부 선수 어댑터
- `LeaguesFragment` 내부 리그 선택 어댑터

---

## Step 4 — Behavior 파일 제거 (3개)

경기 상세 화면의 CoordinatorLayout 기반 헤더 애니메이션 파일들. Compose에서는 `LargeTopAppBar` + `TopAppBarScrollBehavior`로 대체됩니다.

```
삭제 대상:
app/src/main/java/com/example/feetballfootball/behavior/
├── BehaviorHomeTeam.kt
├── BehaviorAwayTeam.kt
└── BehaviorScoreTextView.kt
```

---

## Step 5 — 기존 ViewModel 제거 (3개)

새 ViewModel이 각 feature 모듈에 존재하므로 기존 ViewModel은 삭제합니다.

```
삭제 대상:
app/src/main/java/com/example/feetballfootball/viewModel/
├── FeetballFootballViewModel.kt           ← 새: feature-fixture의 FixtureViewModel
├── FixtureDetailViewModel.kt              ← 새: feature-fixture-detail의 FixtureDetailViewModel
└── StandingViewModel.kt                   ← 새: feature-league의 StandingViewModel
```

---

## Step 6 — API 모델 및 네트워크 코드 제거 (13개)

> 기존 API-Sports v3 기반 네트워크 코드를 삭제합니다. 새 `core-network` 모듈에서 FotMob API(`https://www.fotmob.com/`)로 전환 완료되었으므로 레거시 코드는 안전하게 제거할 수 있습니다.

### 6.1 API 인터페이스 + 유틸리티

```
삭제 대상:
app/src/main/java/com/example/feetballfootball/api/FootballApi.kt
app/src/main/java/com/example/feetballfootball/util/FootballDataFetchr.kt
```

### 6.2 API 모델 클래스 (11개)

```
삭제 대상:
app/src/main/java/com/example/feetballfootball/api/
├── Errors.kt
├── Fixture.kt
├── FixtureResponse.kt
├── FixtureStatus.kt
├── FixtureVenue.kt
├── FootballResponse.kt
├── Goals.kt
├── League.kt
├── TeamAway.kt
├── TeamHome.kt
└── Teams.kt
```

### 6.3 검증

```bash
./gradlew assembleDebug
# 모든 기존 import 참조 에러가 없는지 확인
```

---

## Step 7 — 유틸리티 및 기타 파일 제거

### 7.1 DividerItemDecoration

```
삭제 대상:
app/src/main/java/com/example/feetballfootball/util/DividerItemDecoration.kt
```

Compose에서는 `HorizontalDivider()` / `VerticalDivider()` 사용

### 7.2 Drawable 리소스 검토

**삭제 가능 (RecyclerView/XML 전용):**

```
app/src/main/res/drawable/
├── fixture_recycler_item_bg.xml          ← RecyclerView 아이템 배경
├── recyclerview_divider.xml              ← RecyclerView 구분선
├── circle_progressbar.xml                ← XML 프로그레스바
├── horizon_progressbar.xml               ← XML 프로그레스바
├── tab_selector.xml                      ← TabLayout 셀렉터
├── drawable_line.xml                     ← XML 구분선
```

**삭제 가능 (Compose로 대체):**

```
app/src/main/res/drawable/
├── stadium_*.xml (6개)                   ← 라인업 경기장 배경 → Canvas/Path로 대체
├── player_face_bg_circle.xml             ← CircleShape으로 대체
├── player_rating_bg_circle.xml           ← CircleShape으로 대체
```

**보류 (아이콘, 다른 용도 확인 필요):**

```
app/src/main/res/drawable/
├── ic_fixture.xml                        ← Bottom Nav 아이콘 (Material Icons 사용 시 삭제)
├── ic_league.xml                         ← Bottom Nav 아이콘 (Material Icons 사용 시 삭제)
├── ic_news.xml                           ← Bottom Nav 아이콘 (Material Icons 사용 시 삭제)
├── ic_arrow_back.xml                     ← TopAppBar 뒤로가기 (Material Icons 사용 시 삭제)
├── ic_arrow_forward.xml                  ← 날짜 선택 화살표 (Material Icons 사용 시 삭제)
├── arrow_circle_up.xml                   ← 이벤트 아이콘 (Compose에서 Icon으로 대체 시 삭제)
├── arrow_circle_down.xml                 ← 이벤트 아이콘
├── soccer_ball.xml                       ← 골 아이콘
├── red_card.xml                          ← 레드카드 아이콘
├── yellow_card.xml                       ← 옐로카드 아이콘
├── ic_launcher_background.xml            ← 앱 아이콘 (유지)
```

> **원칙:** Material Icons (`Icons.Default.*`)으로 대체한 것은 삭제, 앱 아이콘(`ic_launcher_*`)은 유지

---

## Step 8 — 레거시 의존성 정리 (build.gradle)

### 8.1 제거할 의존성

기존 `app/build.gradle`에서 새 멀티모듈 구조로 전환하면서 **app 모듈의 build.gradle 자체가 Kotlin DSL로 재작성**됩니다. 아래는 더 이상 필요 없는 라이브러리입니다:

| 기존 의존성 | 대체 | 상태 |
|------------|------|------|
| `com.squareup.retrofit2:retrofit:2.9.0` | core-network에 Retrofit 3.0.0 (kotlinx-serialization 컨버터 내장) | 이동 |
| `com.squareup.retrofit2:converter-scalars:2.5.0` | 제거 (kotlinx.serialization 사용) | 삭제 |
| `com.squareup.retrofit2:converter-gson:2.9.0` | 제거 (kotlinx.serialization 사용) | 삭제 |
| `com.google.code.gson:gson:2.9.0` | 제거 (kotlinx.serialization 사용) | 삭제 |
| `com.squareup.picasso:picasso:2.71828` | Coil 3.x (core-designsystem) | 삭제 |
| `com.jakewharton.threetenabp:threetenabp:1.3.0` | java.time (minSdk 26) | 삭제 |
| `androidx.legacy:legacy-support-v4:1.0.0` | 제거 (불필요) | 삭제 |
| `androidx.recyclerview:recyclerview:1.2.1` | Compose LazyColumn/LazyRow | 삭제 |
| `androidx.lifecycle:lifecycle-extensions:2.2.0` | lifecycle-viewmodel-compose | 삭제 |
| `androidx.constraintlayout:constraintlayout:2.1.4` | Compose Layout | 삭제 |
| `androidx.appcompat:appcompat:1.4.2` | ComponentActivity (appcompat 불필요) | 삭제 |
| `com.google.android.material:material:1.6.1` | Material 3 Compose | 삭제 |

### 8.2 app/build.gradle → app/build.gradle.kts 전환

기존 Groovy DSL 파일을 삭제하고, Convention Plugin 기반 Kotlin DSL로 교체합니다:

```kotlin
// app/build.gradle.kts (Stage 1에서 이미 생성됨)
plugins {
    id("feetball.android.application")
    id("feetball.android.compose")
    id("feetball.android.hilt")
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(projects.feature.fixture)
    implementation(projects.feature.fixturDetail)
    implementation(projects.feature.league)
    implementation(projects.feature.news)
}
```

---

## Step 9 — 빈 패키지 디렉토리 정리

코드 파일 삭제 후 빈 디렉토리가 남으면 제거합니다:

```
삭제 대상 디렉토리:
app/src/main/java/com/example/feetballfootball/fragment/          ← 전체
app/src/main/java/com/example/feetballfootball/adapter/           ← 전체
app/src/main/java/com/example/feetballfootball/behavior/          ← 전체
app/src/main/java/com/example/feetballfootball/viewModel/         ← 전체
app/src/main/java/com/example/feetballfootball/api/               ← 전체
app/src/main/java/com/example/feetballfootball/util/              ← 전체 (모든 유틸리티 제거 시)
```

---

## Step 10 — resources 정리

### 10.1 values 리소스 검토

```
app/src/main/res/values/
├── colors.xml           ← Compose Theme의 FeetballColors로 대체 → 삭제
├── strings.xml          ← 앱 이름 등 필수 문자열은 유지
├── themes.xml           ← Compose FeetballTheme으로 대체 → 삭제
├── dimens.xml           ← Compose dp 직접 사용 → 삭제
└── styles.xml           ← Compose로 대체 → 삭제
```

### 10.2 values-night 검토

```
app/src/main/res/values-night/
└── themes.xml           ← Compose DynamicColorScheme으로 대체 → 삭제
```

> **주의:** `strings.xml`의 `app_name` 등은 `AndroidManifest.xml`에서 참조하므로 유지

---

## Step 11 — 최종 검증

### 11.1 빌드 확인

```bash
./gradlew clean assembleDebug
```

### 11.2 Lint 검사

```bash
./gradlew lint
# 사용되지 않는 리소스, 누락된 참조 등 확인
```

### 11.3 앱 동작 확인

- [ ] 앱 실행 정상
- [ ] 모든 화면 진입 가능
- [ ] Bottom Navigation 동작
- [ ] 경기 상세 3탭 동작
- [ ] 다크모드 전환 정상
- [ ] ProGuard/R8 (release 빌드 시)

### 11.4 코드 크기 비교

```
제거 전: app 모듈 ~5,300 LOC (Kotlin + XML)
제거 후: app 모듈 ~50 LOC (MainActivity + FeetballApp Application 클래스만 잔존)
         나머지 모든 코드는 core-*/feature-* 모듈에 분산
```

---

## ★ Cleanup 완료 검증 체크리스트

- [ ] Fragment 10개 전부 삭제
- [ ] XML Layout 20개 전부 삭제
- [ ] Adapter 2개 + Fragment 내부 어댑터 전부 삭제
- [ ] Behavior 3개 전부 삭제
- [ ] 기존 ViewModel 3개 전부 삭제
- [ ] API 모델 12개 + FootballApi + FootballDataFetchr 전부 삭제
- [ ] 레거시 의존성 전부 제거
- [ ] 사용하지 않는 drawable 리소스 삭제
- [ ] 사용하지 않는 values 리소스 삭제
- [ ] 빈 패키지 디렉토리 삭제
- [ ] `./gradlew clean assembleDebug` 성공
- [ ] `./gradlew lint` 경고 없음
- [ ] 앱 전체 기능 정상 동작
- [ ] `git commit -m "chore: 레거시 코드 전면 제거 (Fragment/XML/Adapter/ViewModel)"`
