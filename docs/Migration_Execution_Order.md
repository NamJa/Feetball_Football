# Feetball Football - 전면 리뉴얼 실행 순서 가이드

> **작성일:** 2026-03-12 (재검토: 2026-03-12)
> **기반 문서:** [Migration_Plan.md](./MIGRATION_PLAN.md)
> **접근 방식:** ~~점진적 마이그레이션~~ → **새 앱 구축 (기존 앱은 기능 스펙으로만 참조)**

---

## 목차

1. [왜 "마이그레이션"이 아니라 "새로 만들기"인가](#1-왜-마이그레이션이-아니라-새로-만들기인가)
2. [기존 프로젝트에서 추출할 지식](#2-기존-프로젝트에서-추출할-지식)
3. [실행 순서 원칙](#3-실행-순서-원칙)
4. [3-Stage 실행 계획](#4-3-stage-실행-계획)
5. [상세 실행 순서 (전체 Step)](#5-상세-실행-순서)
6. [마일스톤 체크포인트](#6-마일스톤-체크포인트)
7. [병렬 작업 가이드](#7-병렬-작업-가이드)
8. [Navigation 3 도입 전략](#8-navigation-3-도입-전략)
9. [롤백 및 리스크 관리](#9-롤백-및-리스크-관리)

---

## 1. 왜 "마이그레이션"이 아니라 "새로 만들기"인가

### 1.1 기존 코드 재활용 분석

기존 프로젝트 전체를 스캔한 결과, API 교체 시 코드 재활용률은 사실상 **0%** 입니다.

```
기존 프로젝트 (~5,300 LOC)
┌─────────────────────────────────────────────────────────────┐
│  API 모델 46개 (550 LOC)        → 새 API DTO로 전면 재작성  │ ✘
│  FootballDataFetchr (278 LOC)   → 새 Repository로 대체      │ ✘
│  FootballApi 인터페이스 (60 LOC)→ 새 API Service로 대체      │ ✘
│  ViewModel 3개 (90 LOC)         → 새 ViewModel + UiState    │ ✘
│  Fragment 9개 (1,050 LOC)       → Compose Screen으로 대체   │ ✘
│  XML Layout 21개 (2,800 LOC)    → Compose UI로 대체         │ ✘
│  Adapter 7개 (300 LOC)          → LazyColumn으로 대체       │ ✘
│  Behavior 3개 (110 LOC)         → Compose 애니메이션으로 대체│ ✘
│  DividerItemDecoration (23 LOC) → Compose Divider           │ ✘
│  build.gradle (57 LOC)          → Kotlin DSL로 재작성       │ ✘
├─────────────────────────────────────────────────────────────┤
│  재활용 가능한 코드: 0 LOC (0%)                              │
│  재활용 가능한 지식: 아래 Section 2 참조                      │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 왜 점진적 마이그레이션이 비효율적인가

| 점진적 마이그레이션 방식 | 문제점 |
|------------------------|--------|
| "기존 Fragment 안에 ComposeView 넣기" | 새 API 모델과 기존 ViewModel 호환 안 됨 → 어차피 ViewModel부터 재작성 |
| "기존 코드와 병존시키기" | API 모델이 완전히 다르므로 병존 자체가 불가능. 두 벌 유지 비용만 증가 |
| "Phase별 순차 전환" | Phase 3(Domain) 만들고 Phase 4(Data) 만들어도, 기존 UI에 연결할 수 없음 (API 구조 자체가 다름) |
| "화면 하나씩 전환" | 새 API + 새 ViewModel + 새 UI를 다 만들어야 화면 하나가 동작 → 결국 새로 만드는 것 |

### 1.3 결론: 새 프로젝트로 구축

```
기존 앱 (레퍼런스)                     새 앱 (구축 대상)
┌──────────────────┐                 ┌──────────────────────────────┐
│ 화면 구성 참조    │ ──"지식"──►    │ Compose UI                    │
│ UX 플로우 참조    │                │ Navigation 3                  │
│ 데이터 표시 방식  │                │ Clean Architecture            │
│ 리그/컵 상수      │                │ 새 API + kotlinx.serialization│
│ 상태 코드 매핑    │                │ Hilt DI                       │
│ 통계 항목 종류    │                │ Room + Coroutines + Flow      │
└──────────────────┘                └──────────────────────────────┘
     참조만 함                              처음부터 구축
```

---

## 2. 기존 프로젝트에서 추출할 지식

코드는 버리지만, 다음 **지식**은 새 앱 구축 시 반드시 참조해야 합니다.

### 2.1 기능 스펙 (화면별)

| 화면 | 기존 파일 | 추출할 지식 |
|------|----------|------------|
| **경기 일정** | `FixtureFragment.kt` (166줄) | 날짜 선택 UI, 리그별 그룹핑, "경기 없음" 빈 상태 처리 |
| **경기 상세** | `FixtureDetailFragment.kt` (268줄) | 접히는 AppBar 구조, 골 스코어러 파싱 (elapsed+extra), 탭 구성 (이벤트/라인업/통계), 데이터 없는 탭 동적 제거 |
| **이벤트 탭** | `FixtureDetailEventsFragment.kt` (168줄) | 이벤트 타입 8종 (Goal/OwnGoal/Penalty/MissedPenalty/YellowCard/RedCard/Substitution/VAR), 아이콘 조건 분기 |
| **라인업 탭** | `FixtureDetailLineupFragment.kt` (170줄) | 포메이션 그리드 파싱 (`grid: "1:1"` → row/col), 어웨이팀 역순 배치, 선수 레이팅 색상 (7.0 기준) |
| **통계 탭** | `FixtureDetailStatisticsFragment.kt` (256줄) | 15개 통계 항목명, 팀 컬러 코드 적용, 흰색→검정 대체, 슈팅 정확도 계산 공식 |
| **리그 선택** | `LeaguesFragment.kt` (79줄) | 5대 리그 목록 (EPL/La Liga/Serie A/Bundesliga/Ligue 1) |
| **리그 순위** | `LeagueClubsStandingFragment.kt` (136줄) | 순위 색상 코딩 (유럽대회 초록/챔피언 청록/강등 빨강), 통계 표시 항목 |
| **선수 순위** | `LeaguePlayerStandingFragment.kt` (169줄) | 동률 순위 처리 로직, 득점왕/어시스트왕 분리 표시 |
| **뉴스** | `NewsFragment.kt` (88줄) | WebView 래핑, 다크모드 처리, 뒤로가기 핸들링 |

### 2.2 상수 및 매핑 테이블

**리그/컵 ID (20개) — `FootballDataFetchr.kt`에서 추출:**

```kotlin
// 5대 리그
const val EPL = 39
const val LA_LIGA = 140
const val SERIE_A = 135
const val BUNDESLIGA = 78
const val LIGUE_1 = 61

// 국내 컵 (7개)
const val FA_CUP = 45
const val LEAGUE_CUP = 48
const val COMMUNITY_SHIELD = 528
const val COPA_DEL_REY = 143
const val DFB_POKAL = 81
const val COPPA_ITALIA = 137
const val COUPE_DE_FRANCE = 66

// UEFA (4개)
const val UCL = 2
const val UEL = 3
const val SUPER_CUP = 531
const val UECL = 848

// 월드컵 예선 (4개)
const val WCQ_ASIA = 30
const val WCQ_EUROPE = 32
const val WCQ_SOUTH_AMERICA = 34
const val WCQ_OCEANIA = 33
```

**경기 상태 코드 매핑 — `FixtureRecyclerViewAdapter.kt`, `FixtureDetailFragment.kt`에서 추출:**

```kotlin
enum class MatchStatus(val apiCode: String, val displayText: String) {
    NOT_STARTED("NS", ""),
    FIRST_HALF("1H", "전반전"),
    HALF_TIME("HT", "하프타임"),
    SECOND_HALF("2H", "후반전"),
    EXTRA_TIME("ET", "연장전"),
    PENALTY("P", "승부차기"),
    FINISHED("FT", "종료"),
    FINISHED_AET("AET", "종료(연장)"),
    FINISHED_PEN("PEN", "종료(승부차기)"),
    POSTPONED("PST", "연기됨"),
    CANCELLED("CANC", "취소됨"),
    SUSPENDED("SUSP", "중단됨"),
}
```

**이벤트 타입 매핑 — `FixtureDetailEventsFragment.kt`에서 추출:**

```kotlin
// type + detail 조합으로 8가지 분기
"Goal" + "Normal Goal"     → 골 아이콘
"Goal" + "Own Goal"        → 자책골 아이콘
"Goal" + "Penalty"         → 페널티 골 아이콘
"Goal" + "Missed Penalty"  → 페널티 실축 아이콘
"Card" + "Yellow Card"     → 옐로카드
"Card" + "Red Card"        → 레드카드 (2nd yellow 포함)
"subst"                    → 교체 아이콘
"Var"                      → VAR 아이콘 (Goal cancelled/awarded)
```

**통계 항목명 — `FixtureDetailStatisticsFragment.kt`에서 추출:**

```kotlin
val STAT_TYPES = listOf(
    "Shots on Goal", "Shots off Goal",
    "Shots insidebox", "Shots outsidebox",
    "Total passes", "Passes accurate", "Passes %",
    "Fouls", "Corner Kicks", "Offsides",
    "Ball Possession",
    "Yellow Cards", "Red Cards",
    "Goalkeeper Saves",
    "Total Shots"  // 계산: on + off
)
// 슈팅 정확도 = (Shots on Goal / Total Shots) * 100
```

### 2.3 시즌 계산 로직 — `FeetballFootballViewModel.kt`, `StandingViewModel.kt`에서 추출

```kotlin
// 시즌 연도 결정 로직: 7월 이전이면 전년도 시즌
val currentSeason: Int
    get() {
        val year = Year.now().value
        val month = LocalDate.now().monthValue
        return if (month < 7) year - 1 else year
    }
```

---

## 3. 실행 순서 원칙

### 3.1 새 앱 구축 원칙

| # | 원칙 | 이유 |
|---|------|------|
| 1 | **수직 슬라이스 (Vertical Slice)** | 한 화면의 전체 스택(API→Domain→UI)을 한 번에 완성해야 동작 검증 가능 |
| 2 | **가장 단순한 화면부터** | 인프라(빌드/DI/네트워크) 검증을 간단한 화면에서 먼저 수행 |
| 3 | **기존 앱과 독립** | 기존 코드를 "이동"하거나 "수정"하지 않음. 참조만 함 |
| 4 | **인프라 1회 구축, 반복 활용** | 빌드 시스템/DI/네트워크 모듈은 첫 화면에서 한 번 구축하고 이후 화면에서 재사용 |
| 5 | **매 화면 완성 시 동작 확인** | 화면 단위로 빌드+실행+검증 |

### 3.2 기존 계획과의 차이

```
기존 계획 (Migration_Plan.md)         수정된 계획 (본 문서)
────────────────────────────         ────────────────────────────
Phase 1: 빌드 시스템                  ┐
Phase 2: 모듈 구조                    ├─ Stage 1: 프로젝트 셋업 (한번에)
Phase 5: DI(Hilt)                     ┘
Phase 3: Domain Layer                 ┐
Phase 4: Data Layer                   ├─ Stage 2: 화면별 수직 슬라이스
Phase 6: Compose UI                   │  (인프라 + API + Domain + UI를
Phase 7: Navigation                   ┘   화면 단위로 한꺼번에 구축)
Phase 8: 테스트                       ┐
Phase 9: 최적화                       ├─ Stage 3: 품질 + KMP
Phase 10: KMP                         ┘

"레이어별 수평 구축" → "화면별 수직 구축"으로 전환
```

**왜 수직 슬라이스인가?**

기존 계획은 "Domain 레이어 전체 → Data 레이어 전체 → UI 전체" 순서였습니다. 이 접근은 기존 API를 유지하며 내부 구조만 바꿀 때 유효합니다.

하지만 API가 바뀌면:
- Domain Model → 새 API 응답 구조에 맞춰 처음부터 설계
- DTO → 새 API의 JSON 구조에 맞춰 작성
- Repository → 새 API 엔드포인트 호출
- ViewModel → 새 Domain Model 기반
- UI → 새 ViewModel 기반

**전부 새로 만들어야 하므로**, 레이어별로 나누면 오히려 첫 동작 확인까지 시간이 길어집니다. 한 화면의 전체 스택을 수직으로 관통하여 완성하면, **첫 화면부터 바로 실행해볼 수 있습니다.**

---

## 4. 3-Stage 실행 계획

### 전체 구조 한눈에 보기

```
Stage 1                     Stage 2                              Stage 3
프로젝트 셋업               화면별 수직 슬라이스 구축              품질 + 확장
(1회성 인프라)              (핵심 작업)                           (마무리)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

빌드 시스템 셋업            Slice 1: 리그 선택 (🟢)              테스트 인프라
멀티 모듈 생성              Slice 2: 리그 순위 (🟡)              성능 최적화
Convention Plugins          Slice 3: 경기 일정 (🟡)              구식 코드 정리
Version Catalog             Slice 4: 뉴스 (🟢)                  KMP 전환
DI(Hilt) 기반               Slice 5: 경기 상세 + 3탭 (🔴)
Network 모듈 기반           Navigation 3 통합
Design System 기반          Bottom Navigation
                            기존 코드 제거

★ Milestone 0              ★ Milestone 1          ★ Milestone 2   ★ Milestone 3
  빈 앱 빌드 성공            첫 화면 동작            전체 화면 완성    프로덕션/KMP
```

---

### Stage 1 — 프로젝트 셋업 (1회성)

**목표:** 빈 앱이 빌드되고, DI 그래프가 동작하고, 네트워크 호출이 가능한 상태

이 Stage에서는 **아직 화면이 없습니다.** 순수 인프라만 구축합니다.

```
Step 1.1  빌드 시스템 현대화
          ├── gradle/libs.versions.toml 생성
          ├── settings.gradle.kts 전환 (멀티 모듈 include)
          ├── build.gradle.kts 전환 (프로젝트 루트 + app)
          ├── Kotlin 2.3.0 + AGP 9.0.0 + Gradle 9.3.1
          └── compileSdk 36, targetSdk 35

Step 1.2  build-logic/convention/ 모듈
          ├── AndroidApplicationConventionPlugin
          ├── AndroidLibraryConventionPlugin
          ├── AndroidComposeConventionPlugin
          ├── AndroidHiltConventionPlugin
          └── AndroidTestConventionPlugin

Step 1.3  멀티 모듈 디렉토리 생성 (빈 모듈, build.gradle.kts만)
          ├── core/core-common       ← Result 래퍼, Dispatcher
          ├── core/core-model        ← Domain Model (빈 상태)
          ├── core/core-domain       ← UseCase, Repository IF (빈 상태)
          ├── core/core-network      ← Retrofit + DTO (빈 상태)
          ├── core/core-database     ← Room (빈 상태)
          ├── core/core-data         ← Repository Impl (빈 상태)
          ├── core/core-designsystem ← Theme + 공통 컴포넌트 (빈 상태)
          ├── feature/feature-fixture
          ├── feature/feature-fixture-detail
          ├── feature/feature-league
          └── feature/feature-news
          ★ 빌드 검증: ./gradlew assembleDebug (빈 모듈)

Step 1.4  core-common 기반 코드 작성
          ├── Result<T> sealed interface
          ├── Flow Extensions (asResult)
          └── Dispatcher Qualifiers (@IoDispatcher, @DefaultDispatcher)

Step 1.5  core-network 기반 코드 작성
          ├── 새 API의 Retrofit 인터페이스 (suspend 함수)
          ├── OkHttp 인터셉터 (인증 헤더)
          ├── API 키 → local.properties + BuildConfig
          └── NetworkModule (Hilt @Module)
          ★ 검증: 단위 테스트로 API 호출 1건 성공 확인

Step 1.6  DI 기반 구축
          ├── FeetballApp (@HiltAndroidApp)
          ├── AndroidManifest.xml 업데이트
          ├── DispatcherModule
          └── MainActivity (@AndroidEntryPoint, setContent만 호출)
          ★ 빌드 검증: 빈 Compose 화면 표시 확인

Step 1.7  core-designsystem 기반 구축
          ├── FeetballTheme (Color, Typography, Shape)
          ├── FeetballColors (Primary, Surface, 카드/골/VAR 컬러)
          └── 공통 컴포넌트 (TeamLogo, LoadingIndicator, ErrorContent, TopAppBar)

Step 1.8  core-database 기반 구축
          ├── FeetballDatabase (빈 상태, Entity 0개)
          └── DatabaseModule (Hilt @Module)
```

> **Stage 1 완료 시 상태:**
> - 빌드 성공, 빈 Compose 화면 표시
> - Hilt DI 그래프 동작
> - Retrofit으로 새 API 호출 가능
> - Design System 테마 적용
> - Room DB 연결 (빈 상태)
> - **화면 없음, 비즈니스 로직 없음**

---

### Stage 2 — 화면별 수직 슬라이스 구축 (핵심 작업)

**목표:** 각 화면을 API→Domain→Data→ViewModel→UI 수직으로 관통하여 완성

**실행 순서 근거:**

```
Slice 1: 리그 선택 (🟢 가장 단순)
  └─ 이유: API 호출 없음. DI+Compose+Navigation 파이프라인 검증 용도
     정적 데이터(5대 리그 목록)만 표시하므로 인프라 문제 분리 가능

Slice 2: 리그 순위 (🟡 첫 API 연동)
  └─ 이유: 단순한 리스트 형태. API 1건 호출 → Domain Model → LazyColumn
     "새 API 연동 파이프라인"을 처음으로 end-to-end 검증

Slice 3: 경기 일정 (🟡 핵심 화면)
  └─ 이유: 앱의 메인 화면. 날짜 선택 + 리그별 그룹핑
     Slice 2에서 검증된 패턴을 활용하여 더 복잡한 데이터 처리

Slice 4: 뉴스 (🟢 독립적)
  └─ 이유: WebView 래핑만. 다른 Slice와 완전 독립
     Stage 2 후반에 넣어 중간 쉬어가는 구간

Slice 5: 경기 상세 + 3탭 (🔴 가장 복잡)
  └─ 이유: 이벤트/라인업/통계 3개 탭 + 접히는 AppBar
     가장 마지막에 배치하여 앞선 Slice들의 패턴 숙련 후 진행

Navigation 3 통합 (Slice 5 완료 후)
  └─ 이유: 모든 Compose Screen이 완성된 후 한번에 연결
```

---

#### Slice 1: 리그 선택 화면 (🟢 최우선)

**이 Slice가 첫 번째인 이유:** API 호출이 필요 없는 정적 화면이므로, Compose + Hilt + Navigation 인프라를 격리하여 검증할 수 있습니다.

```
[core-model]
  └── League.kt (id, name, country, logoUrl) — 5대 리그 상수

[core-domain]
  └── (이 Slice에서는 필요 없음 — 정적 데이터)

[feature-league]
  ├── LeagueListScreen.kt (Compose)
  │     5대 리그 선택 Grid/List
  │     참조: LeaguesFragment.kt의 화면 구성
  ├── LeagueListViewModel.kt (@HiltViewModel)
  │     정적 리그 목록 제공
  └── navigation/LeagueNavigation.kt

★ 검증: 앱 실행 → 리그 목록 표시 → 클릭 이벤트 로그 확인
```

#### Slice 2: 리그 순위 화면 (🟡 첫 API 연동)

**이 Slice가 두 번째인 이유:** 첫 번째 full-stack API 연동. 단순한 리스트이므로 DTO→Domain→Repository→ViewModel→UI 파이프라인을 깔끔하게 검증 가능.

```
[core-network]
  ├── dto/StandingDto.kt (새 API 응답 구조에 맞춰 작성)
  └── FootballApiService에 standings 엔드포인트 추가

[core-model]
  ├── TeamStanding.kt (rank, team, points, W/D/L, goalDiff, form)
  └── LeagueStanding.kt (league, standings: List<TeamStanding>)

[core-domain]
  ├── repository/LeagueRepository.kt (interface)
  └── usecase/GetLeagueStandingsUseCase.kt

[core-data]
  ├── mapper/StandingMapper.kt (DTO → Domain)
  ├── repository/LeagueRepositoryImpl.kt
  └── di/DataModule.kt (Repository 바인딩 시작)

[core-database] (선택)
  ├── entity/StandingEntity.kt
  └── dao/StandingDao.kt

[feature-league]
  ├── standing/StandingScreen.kt (HorizontalPager: 클럽/선수 탭)
  ├── standing/ClubStandingTab.kt (LazyColumn)
  │     참조: LeagueClubsStandingFragment.kt의 순위 색상 코딩
  ├── standing/PlayerStandingTab.kt (득점왕/어시스트왕)
  │     참조: LeaguePlayerStandingFragment.kt의 동률 순위 처리
  ├── StandingViewModel.kt (@HiltViewModel)
  │     UiState: Loading / Success(standings) / Error(message)
  └── StandingUiState.kt (sealed interface)

★ 검증: 리그 선택 → API 호출 → 순위표 표시 → 선수 순위 탭 전환
★ 이 시점에서 API 연동 파이프라인 패턴이 확립됨
```

#### Slice 3: 경기 일정 화면 (🟡 핵심 화면)

```
[core-network]
  ├── dto/FixtureDto.kt (새 API 응답)
  └── FootballApiService에 fixtures 엔드포인트 추가

[core-model]
  ├── Fixture.kt (id, date, status, venue, league, homeTeam, awayTeam, goals)
  ├── MatchStatus.kt (enum)
  └── Team.kt (id, name, logoUrl) — Slice 2에서 이미 생성됐을 수 있음

[core-domain]
  ├── repository/FixtureRepository.kt (interface)
  └── usecase/GetFixturesByDateUseCase.kt

[core-data]
  ├── mapper/FixtureMapper.kt (DTO → Domain + 상태 코드 매핑)
  └── repository/FixtureRepositoryImpl.kt

[core-database]
  ├── entity/FixtureEntity.kt
  └── dao/FixtureDao.kt

[feature-fixture]
  ├── FixtureScreen.kt (날짜 선택 + LazyColumn with stickyHeader)
  │     참조: FixtureFragment.kt의 날짜 네비게이션, 리그별 그룹핑
  ├── component/DateSelector.kt
  ├── component/FixtureItem.kt
  │     참조: FixtureRecyclerViewAdapter.kt의 상태별 표시 로직
  ├── component/LeagueHeader.kt
  ├── FixtureViewModel.kt
  ├── FixtureUiState.kt
  └── FixtureEvent.kt (SelectDate, SelectFixture)

★ 검증: 날짜 변경 → API 호출 → 리그별 경기 목록 표시 → 상태별 분기 확인
```

#### Slice 4: 뉴스 화면 (🟢 독립)

```
[feature-news]
  ├── NewsScreen.kt (AndroidView로 WebView 래핑)
  │     참조: NewsFragment.kt의 다크모드 처리, 뒤로가기 핸들링
  └── navigation/NewsNavigation.kt

★ 검증: 뉴스 탭 → WebView 로딩 → 뒤로가기 동작
★ 가장 단순, Slice 3과 병렬 작업 가능
```

#### Slice 5: 경기 상세 + 3탭 (🔴 가장 복잡)

이 Slice는 내부적으로 **순서가 있습니다:**

```
Phase 5-A: 하위 탭 3개 먼저 완성 (독립적, 병렬 가능)
Phase 5-B: 상세 화면 조립 (3개 탭이 모두 준비된 후)
```

```
[core-network]
  ├── dto/FixtureDetailDto.kt (이벤트, 라인업, 통계, 선수 레이팅)
  └── FootballApiService에 fixture detail 엔드포인트 추가

[core-model]
  ├── MatchDetail.kt (fixture + events + lineups + statistics)
  ├── MatchEvent.kt (time, team, player, assist, type, detail)
  ├── EventType.kt (enum: GOAL, CARD, SUBSTITUTION, VAR)
  ├── TeamLineup.kt (team, formation, coach, startingXI, substitutes)
  ├── LineupPlayer.kt (id, name, number, position, grid, rating)
  ├── TeamStatistic.kt (team, statistics: Map<String, String?>)
  └── Coach.kt (name, photoUrl)

[core-domain]
  ├── repository/FixtureRepository.kt에 getFixtureDetail 추가
  └── usecase/GetFixtureDetailUseCase.kt

[core-data]
  └── mapper/FixtureDetailMapper.kt (가장 복잡한 매퍼)

[feature-fixture-detail]

  ── Phase 5-A: 하위 탭 (병렬 작업 가능) ──

  ├── component/EventsTab.kt
  │     참조: FixtureDetailEventsFragment.kt
  │     - 이벤트 타입 8종 아이콘 분기
  │     - 골 어시스트, 교체 선수, VAR 판정 표시
  │
  ├── component/StatisticsTab.kt
  │     참조: FixtureDetailStatisticsFragment.kt
  │     - 15개 통계 항목 LinearProgressIndicator
  │     - 팀 컬러 적용 (흰색→검정 대체)
  │     - 슈팅 정확도 계산
  │
  ├── component/LineupTab.kt (가장 복잡)
  │     참조: FixtureDetailLineupFragment.kt
  │     - 포메이션 그리드 Canvas 또는 Box 배치
  │     - grid "row:col" 파싱 → 좌표 변환
  │     - 어웨이팀 역순 배치
  │     - 선수 레이팅 색상 (≥7.0 초록, <7.0 주황)
  │     - 감독 정보 표시

  ── Phase 5-B: 조립 ──

  ├── FixtureDetailScreen.kt
  │     참조: FixtureDetailFragment.kt
  │     - 접히는 TopAppBar (LargeTopAppBar + nestedScroll)
  │     - 팀 로고/이름/스코어 헤더
  │     - HorizontalPager (이벤트/라인업/통계)
  │     - 골 스코어러 리스트 (elapsed + extra time 파싱)
  │     - 데이터 없는 탭 동적 제거
  │
  ├── FixtureDetailViewModel.kt
  ├── FixtureDetailUiState.kt
  └── navigation/FixtureDetailNavigation.kt

★ 검증: 경기 선택 → 상세 화면 → 3탭 전환 → 접히는 헤더 → 빈 데이터 처리
```

#### Navigation 3 통합 + Bottom Navigation

**모든 Slice 완료 후** 한번에 통합합니다.

```
[app 모듈]
  ├── navigation/AppNavGraph.kt
  │     NavDisplay + BackStack 구성
  │     Route: FixtureRoute, LeagueRoute, NewsRoute,
  │            FixtureDetailRoute(fixtureId), LeagueStandingRoute(leagueId, ...)
  ├── navigation/FeetballBottomBar.kt
  │     Material 3 NavigationBar (경기/리그/뉴스)
  └── MainActivity.kt 업데이트
        setContent { FeetballTheme { FeetballApp() } }

★ 검증: 전체 화면 흐름 (탭 전환, 상세 진입/복귀, 뒤로가기)
```

#### 기존 코드 정리

```
Navigation 통합 완료 후:
  ├── 기존 Fragment 9개 전체 삭제
  ├── 기존 XML Layout 21개 전체 삭제
  ├── 기존 Adapter 7개 전체 삭제
  ├── 기존 Behavior 3개 전체 삭제
  ├── 기존 ViewModel 3개 전체 삭제
  ├── 기존 api/ 패키지 전체 삭제 (46 모델 + FootballApi)
  ├── FootballDataFetchr.kt 삭제
  ├── DividerItemDecoration.kt 삭제
  └── 구식 의존성 제거 (Picasso, ThreeTen-ABP, Gson, etc.)
```

---

### Stage 3 — 품질 + 확장

**목표:** 테스트, 성능 최적화, KMP 전환

```
Step 3.1  테스트 인프라
          ├── 테스트 Convention Plugin (JUnit 5, MockK, Turbine, Truth)
          ├── Fake/Mock 데이터 클래스
          ├── UseCase 단위 테스트
          ├── Repository 단위 테스트 (Mapper 포함)
          ├── ViewModel 단위 테스트
          ├── Room DAO 통합 테스트
          └── Compose UI 테스트 (주요 화면)
          ★ 커버리지 80%+ 확인

Step 3.2  성능 최적화
          ├── R8 + ProGuard 활성화
          ├── Baseline Profiles 생성
          ├── 메모리 누수 검사 (LeakCanary)
          └── 릴리스 빌드 테스트
          ★ Milestone: 프로덕션 릴리스 가능 상태

Step 3.3  KMP 전환 (선택)
          ├── core-model → commonMain (kotlinx-datetime)
          ├── core-common → commonMain
          ├── core-domain → commonMain
          ├── Hilt → Koin (공유 모듈)
          ├── Retrofit → Ktor (공유 모듈)
          ├── Room → Room KMP
          ├── ViewModel → commonMain
          ├── Compose → Compose Multiplatform
          └── iOS 앱 프로젝트 생성
```

---

## 5. 상세 실행 순서

### 전체 Step 순번

#### Stage 1 — 프로젝트 셋업

| # | Step | 작업 내용 | 산출물 |
|---|------|----------|--------|
| 1 | 1.1 | `gradle/libs.versions.toml` 생성 | Version Catalog |
| 2 | 1.2 | `settings.gradle.kts` + 루트 `build.gradle.kts` | Kotlin DSL 빌드 |
| 3 | 1.3 | `app/build.gradle.kts` + Compose 활성화 | Compose 빌드 가능 |
| 4 | 1.4 | Kotlin 2.3.0 / AGP 9.0.0 / Gradle 9.3.1 / SDK 업데이트 | 최신 빌드 환경 |
| 5 | 1.5 | `build-logic/convention/` + 5개 Convention Plugin | 빌드 로직 공유 |
| 6 | 1.6 | 11개 모듈 디렉토리 + `build.gradle.kts` | 멀티 모듈 구조 |
| 7 | **★** | **빌드 검증: `./gradlew assembleDebug`** | Milestone 0-A |
| 8 | 1.7 | `core-common`: Result, Flow Extensions, Dispatchers | 공통 유틸 |
| 9 | 1.8 | `core-network`: 새 API Service + OkHttp + NetworkModule | 네트워크 기반 |
| 10 | 1.9 | API 키 보안 처리 (local.properties → BuildConfig) | 보안 |
| 11 | 1.10 | `FeetballApp` + `MainActivity` + Hilt + DispatcherModule | DI 기반 |
| 12 | **★** | **빈 Compose 화면 실행 확인** | Milestone 0-B |
| 13 | 1.11 | `core-designsystem`: Theme + 공통 컴포넌트 | UI 기반 |
| 14 | 1.12 | `core-database`: FeetballDatabase + DatabaseModule (빈 상태) | DB 기반 |
| 15 | **★** | **Stage 1 완료: 인프라 전체 빌드 검증** | **Milestone 0** |

#### Stage 2 — 수직 슬라이스 구축

| # | Step | Slice | 작업 내용 |
|---|------|-------|----------|
| 16 | 2.1 | S1 | `core-model`: League 상수 + 기본 모델 |
| 17 | 2.2 | S1 | `feature-league`: LeagueListScreen + ViewModel |
| 18 | **★** | S1 | **첫 화면 동작 확인: 리그 목록 표시** | **Milestone 1** |
| 19 | 2.3 | S2 | `core-network`: standings DTO 작성 |
| 20 | 2.4 | S2 | `core-model`: TeamStanding, LeagueStanding, PlayerStanding |
| 21 | 2.5 | S2 | `core-domain`: LeagueRepository IF + GetLeagueStandingsUseCase |
| 22 | 2.6 | S2 | `core-domain`: GetTopScorersUseCase, GetTopAssistsUseCase |
| 23 | 2.7 | S2 | `core-data`: StandingMapper + LeagueRepositoryImpl + DataModule |
| 24 | 2.8 | S2 | `feature-league`: StandingScreen + ClubStandingTab + PlayerStandingTab |
| 25 | **★** | S2 | **API→UI 파이프라인 검증: 순위표 표시** |
| 26 | 2.9 | S3 | `core-network`: fixtures DTO 작성 |
| 27 | 2.10 | S3 | `core-model`: Fixture, MatchStatus, Team |
| 28 | 2.11 | S3 | `core-domain`: FixtureRepository IF + GetFixturesByDateUseCase |
| 29 | 2.12 | S3 | `core-data`: FixtureMapper (상태 코드 매핑 포함) + FixtureRepositoryImpl |
| 30 | 2.13 | S3 | `core-database`: FixtureEntity + FixtureDao (캐싱) |
| 31 | 2.14 | S3 | `feature-fixture`: FixtureScreen + DateSelector + FixtureItem |
| 32 | **★** | S3 | **핵심 화면 검증: 날짜별 경기 목록** |
| 33 | 2.15 | S4 | `feature-news`: NewsScreen (WebView + 다크모드) |
| 34 | **★** | S4 | **뉴스 화면 검증** |
| 35 | 2.16 | S5-A | `core-network`: fixture detail DTO 작성 |
| 36 | 2.17 | S5-A | `core-model`: MatchDetail, MatchEvent, EventType, TeamLineup, etc. |
| 37 | 2.18 | S5-A | `core-domain`: GetFixtureDetailUseCase |
| 38 | 2.19 | S5-A | `core-data`: FixtureDetailMapper (가장 복잡) |
| 39 | 2.20 | S5-A | `feature-fixture-detail`: EventsTab |
| 40 | 2.21 | S5-A | `feature-fixture-detail`: StatisticsTab |
| 41 | 2.22 | S5-A | `feature-fixture-detail`: LineupTab (포메이션 Canvas) |
| 42 | 2.23 | S5-B | `feature-fixture-detail`: FixtureDetailScreen (탭 조립 + 접히는 헤더) |
| 43 | **★** | S5 | **경기 상세 전체 검증** |
| 44 | 2.24 | Nav | Navigation 3: Route 정의 + NavDisplay + BackStack |
| 45 | 2.25 | Nav | Bottom Navigation Bar (Material 3) |
| 46 | 2.26 | Nav | 각 Feature entryProvider 연결 |
| 47 | **★** | Nav | **전체 화면 흐름 검증** | **Milestone 2** |
| 48 | 2.27 | 정리 | 기존 Fragment/XML/Adapter/ViewModel/API 모델 전체 삭제 |
| 49 | 2.28 | 정리 | 구식 의존성 제거 (Picasso, ThreeTen-ABP, Gson, etc.) |
| 50 | **★** | 정리 | **클린 빌드 검증 (기존 코드 0%)** |

#### Stage 3 — 품질 + 확장

| # | Step | 작업 내용 |
|---|------|----------|
| 51 | 3.1 | 테스트 Convention Plugin + Fake 데이터 |
| 52 | 3.2 | Domain 테스트 (UseCase) |
| 53 | 3.3 | Data 테스트 (Mapper, Repository) |
| 54 | 3.4 | ViewModel 테스트 |
| 55 | 3.5 | Compose UI 테스트 (주요 화면) |
| 56 | 3.6 | Room DAO 통합 테스트 |
| 57 | **★** | **커버리지 80%+ 달성** |
| 58 | 3.7 | R8 + ProGuard 최적화 |
| 59 | 3.8 | Baseline Profiles |
| 60 | 3.9 | 메모리 누수 검사 + 릴리스 빌드 |
| 61 | **★** | **프로덕션 릴리스 가능** | **Milestone 3** |
| 62~74 | 3.10~3.22 | KMP 전환 (Phase 10 — 선택) |

---

## 6. 마일스톤 체크포인트

### Milestone 0 — 인프라 완성 (Stage 1 완료)

- [ ] `./gradlew assembleDebug` 성공 (11개 모듈)
- [ ] Kotlin DSL + Version Catalog 기반
- [ ] Hilt DI 그래프 런타임 동작
- [ ] 새 API 호출 1건 성공 (Logcat 확인)
- [ ] 빈 Compose 화면 + FeetballTheme 적용
- [ ] Room DB 연결 (빈 상태)

### Milestone 1 — 첫 화면 동작 (Slice 1 완료)

- [ ] 리그 선택 화면 표시
- [ ] Hilt ViewModel 주입 동작
- [ ] Compose + Material 3 렌더링 정상

### Milestone 2 — 전체 화면 완성 (Stage 2 완료)

- [ ] 5개 Slice 전체 동작 확인
- [ ] Navigation 3 기반 화면 전환 정상
- [ ] Bottom Navigation 정상
- [ ] 기존 코드 100% 제거
- [ ] Dark/Light 테마 정상
- [ ] 화면 회전 시 상태 유지
- [ ] 오프라인 캐싱 동작 (Room)

### Milestone 3 — 프로덕션 (Stage 3 완료)

- [ ] 테스트 커버리지 80%+
- [ ] R8 릴리스 빌드 정상
- [ ] 메모리 누수 없음
- [ ] 전체 기능 수동 테스트 통과

---

## 7. 병렬 작업 가이드

### 7.1 병렬 가능한 작업

| 작업 A | 작업 B | 선행 조건 |
|--------|--------|----------|
| Slice 3 (경기 일정) | Slice 4 (뉴스) | Stage 1 + Slice 2 패턴 확립 |
| S5-A EventsTab | S5-A StatisticsTab | S5-A DTO + Domain Model 완성 |
| S5-A EventsTab | S5-A LineupTab | S5-A DTO + Domain Model 완성 |
| S5-A StatisticsTab | S5-A LineupTab | S5-A DTO + Domain Model 완성 |
| Stage 3 테스트 작성 | Stage 3 ProGuard 설정 | Stage 2 완료 |

### 7.2 반드시 순차 실행

| 선행 | 후행 | 이유 |
|------|------|------|
| Stage 1 전체 | Slice 1 | 인프라가 없으면 화면 구축 불가 |
| Slice 1 | Slice 2 | DI+Compose 파이프라인을 Slice 1에서 검증 |
| Slice 2 | Slice 3 | API 연동 패턴을 Slice 2에서 확립 |
| S5-A (3개 탭) | S5-B (조립) | 하위 탭이 모두 있어야 상세 화면 조립 |
| Slice 전체 | Navigation 3 통합 | 모든 Screen이 있어야 연결 |
| Navigation 통합 | 기존 코드 삭제 | 새 앱이 완전히 동작한 후에만 삭제 |

### 7.3 수직 슬라이스 내 작업 순서

각 Slice 내에서의 고정 순서 (변경 불가):

```
network DTO → core-model → core-domain (IF + UseCase) → core-data (Mapper + Repo) → ViewModel → Compose UI
```

이 순서는 의존 방향을 따른 것이며, 역방향 작업은 컴파일 에러를 유발합니다.

---

## 8. Navigation 3 도입 전략

### 8.1 Navigation 3 핵심 API

Navigation 3는 `NavController` 대신 **개발자가 BackStack을 직접 소유**합니다.

```kotlin
// Route 정의
@Serializable data object FixtureRoute
@Serializable data object LeagueRoute
@Serializable data object NewsRoute
@Serializable data class FixtureDetailRoute(val fixtureId: Int)
@Serializable data class LeagueStandingRoute(
    val leagueId: Int,
    val leagueName: String,
    val season: Int,
)

// 앱 진입점
@Composable
fun FeetballApp() {
    val backStack = rememberMutableBackStack(FixtureRoute)

    Scaffold(
        bottomBar = {
            FeetballBottomBar(
                currentRoute = backStack.lastOrNull(),
                onTabSelected = { route -> backStack.replaceAll(route) }
            )
        }
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(padding),
            entryProvider = entryProvider {
                entry<FixtureRoute> {
                    FixtureScreen(
                        onFixtureClick = { id -> backStack.add(FixtureDetailRoute(id)) }
                    )
                }
                entry<FixtureDetailRoute> { route ->
                    FixtureDetailScreen(
                        fixtureId = route.fixtureId,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<LeagueRoute> {
                    LeagueListScreen(
                        onLeagueClick = { id, name, season ->
                            backStack.add(LeagueStandingRoute(id, name, season))
                        }
                    )
                }
                entry<LeagueStandingRoute> { route ->
                    StandingScreen(
                        leagueId = route.leagueId,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<NewsRoute> { NewsScreen() }
            }
        )
    }
}
```

### 8.2 Navigation 3 안정성 판단

```
Navigation 3가 Stable인가?
├── YES → 바로 사용 (BackStack 직접 제어, KMP 친화적)
└── NO  → Navigation 2.9.x (Type-Safe) 사용
          KMP 전환 시(Stage 3) Navigation 3로 교체
```

### 8.3 ViewModel 스코핑 주의사항

Navigation 3에서 `@HiltViewModel`은 NavEntry 스코프와 직접 호환되지 않을 수 있습니다.

- **방법 1:** `ViewModelStoreNavEntryDecorator` 사용 → Hilt 유지
- **방법 2:** KMP를 계획한다면, 이 시점에서 Koin 전환을 고려
- **방법 3:** `hiltViewModel()` 대신 `viewModel()` + `@AssistedInject` 패턴

---

## 9. 롤백 및 리스크 관리

### 9.1 Git 브랜치 전략

```
master (현재 릴리스)
  └── develop
       └── feature/renewal-stage-1        ← Stage 1 전체
       └── feature/renewal-slice-1-league  ← Slice별 브랜치
       └── feature/renewal-slice-2-standing
       └── feature/renewal-slice-3-fixture
       └── feature/renewal-slice-4-news
       └── feature/renewal-slice-5-detail
       └── feature/renewal-navigation
       └── feature/renewal-cleanup
       └── feature/renewal-testing
       └── feature/renewal-kmp
```

### 9.2 롤백 전략

새 앱 구축 방식의 가장 큰 장점: **기존 코드를 Stage 2 마지막(Step 48~49)까지 삭제하지 않으므로, 언제든 기존 앱으로 복귀 가능.**

| 시점 | 롤백 방법 | 비용 |
|------|----------|------|
| Stage 1 중 | 새 모듈 디렉토리 삭제 | 없음 (기존 앱 그대로) |
| Stage 2 Slice 중간 | 해당 Slice 브랜치 폐기 | 없음 (기존 앱 그대로) |
| Stage 2 완료 전 | develop에서 기존 코드 유지 | 없음 |
| 기존 코드 삭제 후 | `git revert` Step 48~49 | 기존 코드 복원 가능 |

### 9.3 주요 리스크

| 리스크 | 영향 | 완화 |
|--------|------|------|
| 새 API 스펙이 아직 확정되지 않음 | 🔴 높음 | Stage 1에서 API Service를 interface로 추상화, DTO 변경 시 Mapper만 수정 |
| Slice 5(경기 상세)가 예상보다 복잡 | 🟡 중간 | 3개 탭을 독립적으로 구현, 가장 마지막에 배치 |
| Navigation 3 미성숙 | 🟡 중간 | Navigation 2.9.x 폴백 준비 |
| 새 앱과 기존 앱의 UX 차이 | 🟢 낮음 | 기존 앱을 참조 스펙으로 활용, 화면별 스크린샷 비교 |

---

## 부록: 기존 계획(Migration_Plan.md) 대비 변경 사항

| 항목 | 기존 계획 | 수정된 계획 | 이유 |
|------|----------|-----------|------|
| **접근 방식** | 점진적 마이그레이션 (10 Phase) | 새 앱 구축 (3 Stage) | API 교체 시 기존 코드 재활용 0% |
| **실행 단위** | 레이어별 수평 (Domain 전체 → Data 전체 → UI 전체) | 화면별 수직 (한 화면의 전 레이어를 한번에) | 첫 동작 확인까지의 시간 단축 |
| **기존 코드 처리** | 점진적 교체 (ComposeView 브릿지) | 참조만 하고 마지막에 일괄 삭제 | 병존 비용 제거 |
| **Phase 수** | 10개 Phase | 3 Stage + 5 Slice | 단순화 |
| **Phase 순서** | 빌드→모듈→Domain→Data→DI→UI→Nav→테스트→최적화→KMP | 셋업→(리그→순위→일정→뉴스→상세)→품질 | 빠른 동작 확인 |
| **첫 동작 확인** | Phase 6 (Compose UI) 이후 | Slice 1 (리그 선택) 이후 | 인프라 문제 조기 발견 |
| **총 Step 수** | 74 Steps | 50 Steps (+KMP 12) | 중복 제거 |

---

> **문서 끝.** API 교체를 전제로 할 때, 기존 코드를 마이그레이션하려는 시도는 오히려 비용을 증가시킵니다.
> 기존 앱을 **기능 스펙 레퍼런스**로만 참조하고, 새 앱을 **수직 슬라이스 방식**으로 구축하는 것이 가장 효율적입니다.
> 기존 코드는 새 앱의 모든 화면이 완성될 때까지 삭제하지 않으므로, 언제든 비교 및 롤백이 가능합니다.
