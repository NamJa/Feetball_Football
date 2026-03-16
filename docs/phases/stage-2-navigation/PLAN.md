# Stage 2 / Navigation 3 통합

> **목표:** 모든 Compose Screen을 Navigation 3로 연결 + Bottom Navigation
> **선행 조건:** Slice 1~5 전체 완료
> **Git 브랜치:** `feature/renewal-navigation`
> **참조 파일:** `MainActivity.kt` (107줄)의 탭/프래그먼트 관리 로직

---

## Step 1 — Navigation 3 의존성 확인

### libs.versions.toml

Navigation 3는 2025년 11월 stable 1.0.0이 출시되었으며, 최신 stable은 1.0.1 (2026-02-11) 입니다.

```toml
[versions]
navigation3 = "1.0.1"

[libraries]
# Navigation 3는 runtime과 ui 두 개의 아티팩트로 분리
androidx-navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "navigation3" }
```

> **참고:** Navigation 3 1.1.0-beta01 (2026-03-11)부터 KMP (JVM, Native, Web) 타겟도 지원합니다. Stage 3 KMP 전환 시 활용 가능합니다.

---

## Step 2 — Route 정의

```kotlin
// app/src/main/kotlin/.../navigation/Routes.kt
package com.chase1st.feetballfootball.navigation

import kotlinx.serialization.Serializable

// 탑레벨 탭 (Bottom Navigation)
@Serializable data object FixtureRoute
@Serializable data object LeagueRoute
@Serializable data object NewsRoute

// 상세 화면
@Serializable data class FixtureDetailRoute(val fixtureId: Int)
@Serializable data class LeagueStandingRoute(
    val leagueId: Int,
    val leagueName: String,
    val season: String,
)
```

---

## Step 3 — Bottom Navigation Bar

```kotlin
// app/src/main/kotlin/.../navigation/FeetballBottomBar.kt
package com.chase1st.feetballfootball.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: Any,
    val icon: ImageVector,
    val label: String,
) {
    FIXTURE(FixtureRoute, Icons.Default.SportsSoccer, "경기"),
    LEAGUE(LeagueRoute, Icons.Default.EmojiEvents, "리그"),
    NEWS(NewsRoute, Icons.Default.Newspaper, "뉴스"),
}

@Composable
fun FeetballBottomBar(
    currentRoute: Any?,
    onTabSelected: (Any) -> Unit,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
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

---

## Step 4 — NavDisplay + BackStack (Navigation 3)

```kotlin
// app/src/main/kotlin/.../navigation/FeetballNavGraph.kt
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

@Composable
fun FeetballApp() {
    // Navigation 3: BackStack을 개발자가 직접 소유
    val backStack = rememberMutableBackStack(FixtureRoute)

    Scaffold(
        bottomBar = {
            // 상세 화면에서는 Bottom Bar 숨김
            val currentRoute = backStack.lastOrNull()
            val isTopLevel = currentRoute is FixtureRoute ||
                currentRoute is LeagueRoute ||
                currentRoute is NewsRoute

            if (isTopLevel) {
                FeetballBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        // 탭 전환: 기존 스택 초기화 후 새 탭
                        // backStack은 SnapshotStateList이므로 clear + add 사용
                        backStack.clear()
                        backStack.add(route)
                    },
                )
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(padding),
            entryProvider = entryProvider {

                entry<FixtureRoute> {
                    FixtureScreen(
                        onFixtureClick = { fixtureId ->
                            backStack.add(FixtureDetailRoute(fixtureId))
                        },
                    )
                }

                entry<FixtureDetailRoute> { route ->
                    FixtureDetailScreen(
                        fixtureId = route.fixtureId,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<LeagueRoute> {
                    LeagueListScreen(
                        onLeagueClick = { leagueId, leagueName, season ->
                            backStack.add(
                                LeagueStandingRoute(leagueId, leagueName, season)
                            )
                        },
                    )
                }

                entry<LeagueStandingRoute> { route ->
                    StandingScreen(
                        leagueName = route.leagueName,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<NewsRoute> {
                    NewsScreen()
                }
            },
        )
    }
}
```

---

## Step 5 — MainActivity 최종 업데이트

```kotlin
// app/src/main/kotlin/.../MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FeetballTheme {
                FeetballApp()
            }
        }
    }
}
```

---

## Step 6 — Navigation 2.9.x 참고 (레거시)

> **참고:** Navigation 3는 2025년 11월 stable 1.0.0이 출시되었으므로, 새 프로젝트에서는 Navigation 3를 사용합니다. 아래는 기존 Navigation 2.9.x 기반 코드를 유지보수할 때 참고용입니다.

<details>
<summary>Navigation 2.9.x 코드 (접기/펼치기)</summary>

```kotlin
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

</details>

---

## ★ Navigation 완료 검증

**체크리스트:**

- [ ] Bottom Navigation: 경기/리그/뉴스 3개 탭 전환 정상
- [ ] 경기 목록 → 경기 상세 진입 → 뒤로가기 → 목록 복귀
- [ ] 리그 선택 → 리그 순위 진입 → 뒤로가기 → 선택 복귀
- [ ] 상세 화면에서 Bottom Bar 숨김
- [ ] 탭 전환 시 이전 탭 상태 유지 (또는 초기화 — 정책 결정)
- [ ] 시스템 뒤로가기 버튼 정상 동작
- [ ] 딥링크 (필요시)
- [ ] Navigation 3 의존성: `navigation3-runtime` + `navigation3-ui` 1.0.1
- [ ] `git commit -m "feat: Navigation 3 통합 + Bottom Navigation"`
