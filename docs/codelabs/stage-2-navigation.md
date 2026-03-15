# Stage 2 / Navigation — Navigation 3 통합 + Bottom Navigation

> ⏱ 예상 소요 시간: 3시간 | 난이도: ★★☆ | 선행 조건: Slice 1~5 전체 완료 (모든 Compose Screen 구현 완료)

---

## 이 Codelab에서 배우는 것

- **Navigation 3**의 핵심 개념: `BackStack`을 개발자가 직접 소유하는 선언적 네비게이션
- `NavDisplay` + `entryProvider`를 사용한 화면 라우팅
- `NavigationBar` + `NavigationBarItem`을 활용한 Bottom Navigation 구현
- 상세 화면 진입 시 Bottom Bar 숨김 처리
- 탭 전환 시 BackStack 관리 전략
- 기존 Fragment/TabLayout 기반 네비게이션에서 Compose Navigation 3로의 전환

---

## 완성 후 결과물

- 3개 탭(경기/리그/뉴스) Bottom Navigation이 동작하는 단일 `FeetballApp` Composable
- 경기 목록 → 경기 상세, 리그 목록 → 리그 순위 화면 전환이 Navigation 3 BackStack으로 관리됨
- 상세 화면에서는 Bottom Bar가 자동으로 숨겨짐
- 시스템 뒤로가기 버튼이 정상 동작
- `MainActivity`가 `setContent { FeetballApp() }` 한 줄로 정리됨

### 새로 생성되는 파일

| 파일 | 위치 |
|------|------|
| `Routes.kt` | `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/Routes.kt` |
| `FeetballBottomBar.kt` | `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballBottomBar.kt` |
| `FeetballNavGraph.kt` | `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballNavGraph.kt` |

### 수정되는 파일

| 파일 | 위치 |
|------|------|
| `libs.versions.toml` | `gradle/libs.versions.toml` |
| `build.gradle.kts` | `app/build.gradle.kts` |
| `MainActivity.kt` | `app/src/main/kotlin/com/chase1st/feetballfootball/MainActivity.kt` |

---

## Step 1 — Navigation 3 의존성 확인

### 목표

> Navigation 3 라이브러리(1.0.1)가 프로젝트에 올바르게 선언되어 있는지 확인하고, 필요시 추가한다.

### 배경

Navigation 3는 2025년 11월 stable 1.0.0이 출시되었으며, 최신 stable은 **1.0.1 (2026-02-11)** 입니다. Navigation 2.x와 근본적으로 다른 점은 **BackStack을 개발자가 직접 소유**한다는 것입니다. `NavController`가 내부적으로 상태를 관리하던 Navigation 2.x와 달리, Navigation 3에서는 `SnapshotStateList`로 BackStack을 직접 조작합니다.

Navigation 3는 두 개의 아티팩트로 분리되어 있습니다:
- `navigation3-runtime` — 핵심 런타임 (BackStack, Route 등)
- `navigation3-ui` — UI 컴포넌트 (NavDisplay, entryProvider 등)

### 작업 내용

**파일: `gradle/libs.versions.toml`**

아래 내용이 이미 존재하는지 확인하고, 없으면 추가합니다:

```toml
[versions]
navigation3 = "1.0.1"

[libraries]
# Navigation 3는 runtime과 ui 두 개의 아티팩트로 분리
androidx-navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "navigation3" }
```

**파일: `app/build.gradle.kts`**

dependencies 블록에 아래를 추가합니다:

```kotlin
dependencies {
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
}
```

> 💡 **Tip:** Navigation 3 1.1.0-beta01 (2026-03-11)부터 KMP (JVM, Native, Web) 타겟도 지원합니다. Stage 3 KMP 전환 시 활용 가능하지만, 현 단계에서는 stable 1.0.1을 사용합니다.

### ✅ 검증

- [ ] `./gradlew dependencies --configuration debugRuntimeClasspath | grep navigation3` 실행 시 `navigation3-runtime:1.0.1`과 `navigation3-ui:1.0.1`이 출력됨
- [ ] Sync 에러 없음

---

## Step 2 — Route 정의

### 목표

> 앱의 모든 화면에 대한 타입 안전한 Route를 `@Serializable` data class/object로 정의한다.

### 배경

Navigation 3에서 Route는 **일반 Kotlin 클래스**입니다. `@Serializable` 어노테이션을 붙이면 Navigation 3가 자동으로 직렬화/역직렬화를 처리합니다. 파라미터가 없는 화면은 `data object`, 파라미터가 있는 화면은 `data class`로 정의합니다.

현재 앱의 화면 구조:
- **탑레벨 탭 3개:** 경기 목록(Fixture), 리그 목록(League), 뉴스(News)
- **상세 화면 2개:** 경기 상세(FixtureDetail, fixtureId 필요), 리그 순위(LeagueStanding, leagueId/leagueName/season 필요)

### 작업 내용

**파일: `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/Routes.kt`** (새 파일 생성)

```kotlin
// app/src/main/kotlin/com/chase1st/feetballfootball/navigation/Routes.kt
package com.chase1st.feetballfootball.navigation

import kotlinx.serialization.Serializable

// ── 탑레벨 탭 (Bottom Navigation) ──
// 파라미터가 없으므로 data object 사용

@Serializable data object FixtureRoute      // 경기 일정 목록
@Serializable data object LeagueRoute       // 리그 선택 목록
@Serializable data object NewsRoute         // 뉴스 WebView

// ── 상세 화면 ──
// 파라미터가 있으므로 data class 사용

@Serializable data class FixtureDetailRoute(
    val fixtureId: Int,                     // FotMob match ID
)

@Serializable data class LeagueStandingRoute(
    val leagueId: Int,                      // FotMob league ID (예: EPL=47)
    val leagueName: String,                 // 화면 상단 표시용 리그 이름
    val season: String,                     // 시즌 (예: "2025/2026")
)
```

> 💡 **Tip:** `@Serializable`을 사용하려면 `kotlinx-serialization` 플러그인이 프로젝트에 적용되어 있어야 합니다. Slice 1(Stage 1)에서 이미 설정되었을 것이지만, `plugins { kotlin("plugin.serialization") }`이 `app/build.gradle.kts`에 있는지 확인하세요.

> ⚠️ **주의:** Route 클래스의 프로퍼티 타입은 직렬화 가능한 기본 타입(Int, String, Boolean 등)만 사용해야 합니다. 복잡한 객체를 Route에 넣지 마세요 — ID만 전달하고 상세 화면의 ViewModel에서 데이터를 로드하는 것이 올바른 패턴입니다.

### ✅ 검증

- [ ] 파일이 `com.chase1st.feetballfootball.navigation` 패키지에 생성됨
- [ ] 빌드 에러 없음 (`./gradlew compileDebugKotlin`)
- [ ] 5개 Route가 모두 `@Serializable`로 선언됨

---

## Step 3 — Bottom Navigation Bar

### 목표

> Material 3 `NavigationBar`를 사용하여 경기/리그/뉴스 3개 탭의 Bottom Navigation을 구현한다.

### 배경

기존 앱은 `TabLayout`으로 탭을 관리했습니다 (참조: `MainActivity.kt` 107줄). Compose에서는 Material 3의 `NavigationBar` + `NavigationBarItem`으로 대체합니다.

`TopLevelDestination` enum으로 탭 목록을 정의하면, 새 탭 추가 시 enum에만 항목을 추가하면 됩니다.

### 작업 내용

**파일: `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballBottomBar.kt`** (새 파일 생성)

```kotlin
// app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballBottomBar.kt
package com.chase1st.feetballfootball.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 탑레벨 탭 정의.
 * 각 탭은 Route, 아이콘, 라벨을 가진다.
 * 새 탭을 추가하려면 이 enum에 항목을 추가하면 된다.
 */
enum class TopLevelDestination(
    val route: Any,              // Navigation 3 Route 객체
    val icon: ImageVector,       // Material Icons
    val label: String,           // 하단 탭에 표시될 텍스트
) {
    FIXTURE(FixtureRoute, Icons.Default.SportsSoccer, "경기"),
    LEAGUE(LeagueRoute, Icons.Default.EmojiEvents, "리그"),
    NEWS(NewsRoute, Icons.Default.Newspaper, "뉴스"),
}

/**
 * 하단 네비게이션 바 Composable.
 *
 * @param currentRoute 현재 BackStack의 최상위 Route (선택 상태 판별용)
 * @param onTabSelected 탭 클릭 시 호출되는 콜백 (해당 탭의 Route 전달)
 */
@Composable
fun FeetballBottomBar(
    currentRoute: Any?,
    onTabSelected: (Any) -> Unit,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                // 현재 Route의 클래스와 탭의 Route 클래스를 비교하여 선택 상태 판별
                selected = currentRoute == destination.route ||
                    currentRoute?.javaClass == destination.route.javaClass,
                onClick = { onTabSelected(destination.route) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}
```

> 💡 **Tip:** `selected` 판별에서 `currentRoute == destination.route`는 data object 비교, `currentRoute?.javaClass == destination.route.javaClass`는 같은 타입의 data class 인스턴스 비교를 처리합니다. data object는 싱글턴이므로 `==`로 충분하지만, 만약 탭 Route가 data class로 변경될 경우를 대비한 방어 코드입니다.

> ⚠️ **주의:** `Icons.Default.SportsSoccer`, `Icons.Default.EmojiEvents`, `Icons.Default.Newspaper`를 사용하려면 `material-icons-extended` 의존성이 필요할 수 있습니다. 만약 아이콘이 resolve되지 않으면 `implementation("androidx.compose.material:material-icons-extended")`를 추가하거나, `Icons.Default.Home`, `Icons.Default.Star`, `Icons.Default.Info` 등 core 아이콘으로 대체하세요.

### ✅ 검증

- [ ] `FeetballBottomBar` Preview에서 3개 탭(경기/리그/뉴스)이 아이콘+라벨과 함께 표시됨
- [ ] `TopLevelDestination.entries`가 3개 항목을 반환함
- [ ] 빌드 에러 없음

---

## Step 4 — NavDisplay + BackStack 구성 (Navigation 3 핵심)

### 목표

> Navigation 3의 `rememberMutableBackStack` + `NavDisplay` + `entryProvider`를 사용하여 전체 앱의 화면 전환을 구성한다. `Scaffold`로 Bottom Bar를 배치하고, 상세 화면에서는 Bottom Bar를 숨긴다.

### 배경

이 Step이 Navigation 3 통합의 **핵심**입니다.

**Navigation 2.x vs Navigation 3 비교:**

| 항목 | Navigation 2.x | Navigation 3 |
|------|---------------|--------------|
| 상태 소유 | `NavController` 내부 관리 | 개발자가 `BackStack` 직접 소유 |
| 화면 등록 | `NavHost { composable<Route> {} }` | `NavDisplay { entryProvider { entry<Route> {} } }` |
| 화면 전환 | `navController.navigate(route)` | `backStack.add(route)` |
| 뒤로가기 | `navController.popBackStack()` | `backStack.removeLastOrNull()` |

Navigation 3에서 BackStack은 `SnapshotStateList`이므로, `add()`, `removeLastOrNull()`, `clear()` 등 일반 리스트 연산을 사용합니다.

기존 `MainActivity.kt`의 Fragment 관리 로직을 참조하면:
- `showFragment(tabPos)` → 탭 전환 시 `backStack.clear()` + `backStack.add(route)`
- `onLeagueSelected(leagueId)` → `backStack.add(LeagueStandingRoute(...))`
- `onFixtureSelected(fixtureId)` → `backStack.add(FixtureDetailRoute(fixtureId))`

### 작업 내용

**파일: `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballNavGraph.kt`** (새 파일 생성)

```kotlin
// app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballNavGraph.kt
package com.chase1st.feetballfootball.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chase1st.feetballfootball.feature.fixture.FixtureScreen
import com.chase1st.feetballfootball.feature.fixturedetail.FixtureDetailScreen
import com.chase1st.feetballfootball.feature.league.LeagueListScreen
import com.chase1st.feetballfootball.feature.league.standing.StandingScreen
import com.chase1st.feetballfootball.feature.news.NewsScreen

// Navigation 3 API 사용
// import androidx.navigation3.*

/**
 * 앱의 루트 Composable.
 * Navigation 3 BackStack을 소유하고, 모든 화면 전환을 관리한다.
 *
 * 구조:
 * - Scaffold: 전체 레이아웃 (bottomBar + content)
 * - NavDisplay: BackStack 기반 화면 전환
 * - entryProvider: Route → Composable 매핑
 */
@Composable
fun FeetballApp() {
    // ── Navigation 3: BackStack을 개발자가 직접 소유 ──
    // 초기 Route로 FixtureRoute(경기 목록)을 지정
    // backStack은 SnapshotStateList이므로 Compose가 변경을 자동 감지
    val backStack = rememberMutableBackStack(FixtureRoute)

    Scaffold(
        bottomBar = {
            // ── 상세 화면에서는 Bottom Bar 숨김 ──
            // BackStack의 최상위(마지막) 항목이 탑레벨 Route인지 확인
            val currentRoute = backStack.lastOrNull()
            val isTopLevel = currentRoute is FixtureRoute ||
                currentRoute is LeagueRoute ||
                currentRoute is NewsRoute

            if (isTopLevel) {
                FeetballBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        // 탭 전환 시 기존 스택 초기화 후 새 탭의 Route만 남김
                        // 이는 기존 MainActivity.showFragment()에서
                        // replace()로 Fragment를 교체하던 것과 동일한 동작
                        backStack.clear()
                        backStack.add(route)
                    },
                )
            }
        },
    ) { padding ->
        // ── NavDisplay: BackStack 기반 화면 렌더링 ──
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(padding),
            entryProvider = entryProvider {

                // ── 경기 목록 화면 ──
                entry<FixtureRoute> {
                    FixtureScreen(
                        onFixtureClick = { fixtureId ->
                            // 경기 상세로 이동 (기존 onFixtureSelected 콜백 대체)
                            backStack.add(FixtureDetailRoute(fixtureId))
                        },
                    )
                }

                // ── 경기 상세 화면 ──
                entry<FixtureDetailRoute> { route ->
                    FixtureDetailScreen(
                        fixtureId = route.fixtureId,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                // ── 리그 목록 화면 ──
                entry<LeagueRoute> {
                    LeagueListScreen(
                        onLeagueClick = { leagueId, leagueName, season ->
                            // 리그 순위로 이동 (기존 onLeagueSelected 콜백 대체)
                            backStack.add(
                                LeagueStandingRoute(leagueId, leagueName, season)
                            )
                        },
                    )
                }

                // ── 리그 순위 화면 ──
                entry<LeagueStandingRoute> { route ->
                    StandingScreen(
                        leagueName = route.leagueName,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                // ── 뉴스 화면 ──
                entry<NewsRoute> {
                    NewsScreen()
                }
            },
        )
    }
}
```

### 화면 전환 흐름 다이어그램

```
[FixtureRoute] ──onFixtureClick──▶ [FixtureDetailRoute(fixtureId)]
      ↑                                        │
      └──────────── onBack (removeLastOrNull) ──┘

[LeagueRoute] ──onLeagueClick──▶ [LeagueStandingRoute(leagueId, name, season)]
      ↑                                        │
      └──────────── onBack (removeLastOrNull) ──┘

[NewsRoute] (단독 화면, 하위 화면 없음)

탭 전환: backStack.clear() → backStack.add(새 탭 Route)
```

> 💡 **Tip:** `backStack.clear()` + `backStack.add(route)` 패턴은 탭 전환 시 이전 탭의 히스토리를 모두 버립니다. 만약 탭별 히스토리를 유지하고 싶다면(예: 리그 탭에서 순위 화면에 있다가 경기 탭 갔다 돌아오면 순위 화면 유지), 탭별로 별도의 BackStack을 관리하는 패턴이 필요합니다. 현 단계에서는 단순 초기화 방식을 사용합니다.

> ⚠️ **주의:** `NavDisplay`의 `entryProvider` 블록 안에서 등록하지 않은 Route가 BackStack에 들어가면 런타임 에러가 발생합니다. 모든 Route에 대한 `entry<>` 등록을 빠뜨리지 마세요.

### ✅ 검증

- [ ] `FeetballApp()` 호출 시 기본 화면으로 경기 목록(FixtureRoute)이 표시됨
- [ ] Bottom Navigation 3개 탭이 하단에 표시됨
- [ ] 경기 탭 → 리그 탭 → 뉴스 탭 전환이 정상 동작
- [ ] 경기 상세/리그 순위 화면에서 Bottom Bar가 숨겨짐
- [ ] 뒤로가기 시 이전 화면으로 정상 복귀

---

## Step 5 — MainActivity 최종 업데이트

### 목표

> 기존 Fragment/TabLayout 기반의 `MainActivity`를 Compose `setContent`로 완전히 교체한다.

### 배경

기존 `MainActivity`(107줄)는 다음 역할을 했습니다:
- `ActivityMainBinding`으로 XML 레이아웃 inflate
- `AndroidThreeTen.init()` 초기화
- `TabLayout` 리스너로 Fragment 교체
- `LeaguesFragment.Callbacks`, `FixtureRecyclerViewAdapter.Callbacks` 인터페이스 구현
- `savedInstanceState`로 선택된 탭 위치 복원

이 모든 것이 Navigation 3의 `FeetballApp()`으로 대체되므로, `MainActivity`는 `setContent` 한 줄로 단순화됩니다.

### 작업 내용

**파일: `app/src/main/kotlin/com/chase1st/feetballfootball/MainActivity.kt`** (기존 파일 교체)

```kotlin
// app/src/main/kotlin/com/chase1st/feetballfootball/MainActivity.kt
package com.chase1st.feetballfootball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chase1st.feetballfootball.navigation.FeetballApp
import com.chase1st.feetballfootball.ui.theme.FeetballTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-Edge: 시스템 바 영역까지 콘텐츠 확장
        enableEdgeToEdge()
        setContent {
            // FeetballTheme: 라이트/다크 모드 + Dynamic Color 지원
            FeetballTheme {
                // 앱의 전체 네비게이션을 FeetballApp()이 관리
                FeetballApp()
            }
        }
    }
}
```

### 기존 코드와 비교

| 기존 (107줄) | 새 코드 (~15줄) |
|-------------|---------------|
| `AppCompatActivity` 상속 | `ComponentActivity` 상속 |
| `ActivityMainBinding.inflate()` | `setContent {}` |
| `AndroidThreeTen.init()` | 불필요 (minSdk 26, `java.time` 직접 사용) |
| `TabLayout.OnTabSelectedListener` | `FeetballBottomBar`의 `onTabSelected` |
| `Callbacks` 인터페이스 2개 구현 | Navigation 3 Route로 화면 전환 |
| `savedInstanceState` 수동 관리 | Compose 상태 자동 관리 |
| `supportFragmentManager.beginTransaction()` | `backStack.add(route)` |

> 💡 **Tip:** `@AndroidEntryPoint`는 Hilt 의존성 주입을 위한 어노테이션입니다. Hilt를 사용하지 않는 경우 제거해도 됩니다.

> ⚠️ **주의:** `enableEdgeToEdge()`를 호출하면 시스템 바(상태 바, 네비게이션 바) 아래까지 콘텐츠가 확장됩니다. `Scaffold`의 `padding`을 반드시 적용해야 콘텐츠가 시스템 바에 가려지지 않습니다. Step 4에서 `Modifier.padding(padding)`을 적용했으므로 정상 동작합니다.

### ✅ 검증

- [ ] `MainActivity`가 `ComponentActivity`를 상속함 (`AppCompatActivity` 아님)
- [ ] `setContentView()` 호출이 없음 (`setContent {}` 사용)
- [ ] `ActivityMainBinding` 참조가 없음
- [ ] Fragment/TabLayout 관련 import가 없음
- [ ] 앱 실행 시 `FeetballApp()` 화면이 정상 표시됨

---

## Step 6 — Navigation 2.9.x 레거시 참고 (선택 사항)

### 목표

> Navigation 2.9.x 기반 코드를 참고용으로 이해한다. 이 Step은 코드를 작성하지 않으며, 기존 Navigation 2.x 프로젝트를 유지보수할 때 참고용입니다.

### 배경

Navigation 3를 사용할 수 없는 환경(라이브러리 호환성 문제, 팀 의사결정 등)에서는 Navigation 2.9.x를 대안으로 사용할 수 있습니다. 아래 코드는 동일한 화면 구조를 Navigation 2.9.x로 구현한 예시입니다.

### 참고 코드 (Navigation 2.9.x 버전)

```kotlin
// Navigation 2.9.x 기반 FeetballApp — 참고용, 실제로 사용하지 않음
@Composable
fun FeetballApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination

            val isTopLevel = TopLevelDestination.entries.any {
                currentRoute?.hasRoute(it.route::class) == true
            }

            if (isTopLevel) {
                FeetballBottomBar(
                    currentRoute = currentRoute,
                    navController = navController,
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = FixtureRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<FixtureRoute> {
                FixtureScreen(
                    onFixtureClick = { id -> navController.navigate(FixtureDetailRoute(id)) },
                )
            }
            composable<FixtureDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<FixtureDetailRoute>()
                FixtureDetailScreen(
                    fixtureId = route.fixtureId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<LeagueRoute> {
                LeagueListScreen(
                    onLeagueClick = { id, name, season ->
                        navController.navigate(LeagueStandingRoute(id, name, season))
                    },
                )
            }
            composable<LeagueStandingRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<LeagueStandingRoute>()
                StandingScreen(
                    leagueName = route.leagueName,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<NewsRoute> { NewsScreen() }
        }
    }
}
```

### Navigation 2.9.x vs Navigation 3 핵심 차이

| 항목 | Navigation 2.9.x | Navigation 3 |
|------|-----------------|--------------|
| BackStack 소유 | `NavController` 내부 | 개발자 직접 (`SnapshotStateList`) |
| 화면 등록 | `NavHost { composable<Route> {} }` | `NavDisplay { entryProvider { entry<Route> {} } }` |
| 화면 전환 | `navController.navigate(route)` | `backStack.add(route)` |
| 뒤로가기 | `navController.popBackStack()` | `backStack.removeLastOrNull()` |
| 시작 화면 | `startDestination = Route` | `rememberMutableBackStack(Route)` |
| Route 접근 | `backStackEntry.toRoute<T>()` | `entry<T> { route -> ... }` 파라미터 |

> 💡 **Tip:** Navigation 3의 가장 큰 장점은 **테스트 용이성**입니다. BackStack이 일반 리스트이므로 단위 테스트에서 `mutableListOf()`로 BackStack을 생성하여 화면 전환 로직을 테스트할 수 있습니다.

### ✅ 검증

- [ ] 이 Step에서는 코드를 작성하지 않음 (참고용)
- [ ] Navigation 3 방식(Step 4)으로 구현되었는지 재확인

---

## Navigation 완료 검증

### 전체 체크리스트

모든 Step이 완료되면 아래 항목을 하나씩 확인합니다:

- [ ] **Bottom Navigation:** 경기/리그/뉴스 3개 탭 전환 정상
- [ ] **경기 상세 진입:** 경기 목록 → 경기 상세 진입 → 뒤로가기 → 목록 복귀
- [ ] **리그 순위 진입:** 리그 선택 → 리그 순위 진입 → 뒤로가기 → 선택 복귀
- [ ] **Bottom Bar 숨김:** 상세 화면(FixtureDetail, LeagueStanding)에서 Bottom Bar 숨김
- [ ] **탭 전환 시 스택 초기화:** 탭 전환 시 이전 탭의 BackStack이 초기화됨
- [ ] **시스템 뒤로가기:** 시스템 뒤로가기 버튼이 BackStack을 따라 정상 동작
- [ ] **의존성:** `navigation3-runtime` + `navigation3-ui` 1.0.1 사용 중
- [ ] **빌드 성공:** `./gradlew assembleDebug` 에러 없음

### 커밋

```bash
git add .
git commit -m "feat: Navigation 3 통합 + Bottom Navigation"
```

### 생성/수정된 파일 요약

| 작업 | 파일 |
|------|------|
| 새 파일 | `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/Routes.kt` |
| 새 파일 | `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballBottomBar.kt` |
| 새 파일 | `app/src/main/kotlin/com/chase1st/feetballfootball/navigation/FeetballNavGraph.kt` |
| 수정 | `gradle/libs.versions.toml` (Navigation 3 의존성 추가) |
| 수정 | `app/build.gradle.kts` (Navigation 3 의존성 추가) |
| 교체 | `app/src/main/kotlin/com/chase1st/feetballfootball/MainActivity.kt` (107줄 → ~15줄) |

---

> 다음 단계: [Stage 2 / Cleanup — 레거시 코드 제거](stage-2-cleanup.md)
