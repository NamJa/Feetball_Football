# Stage 2 / Slice 1 — 리그 선택 화면

> **목표:** Compose + Hilt 파이프라인 첫 검증. 정적 데이터(5대 리그 목록) 표시
> **선행 조건:** Stage 1 완료
> **Git 브랜치:** `feature/renewal-slice-1-league`
> **참조 파일:** `LeaguesFragment.kt` (79줄)

---

## 이 Slice가 첫 번째인 이유

API 호출이 없는 정적 화면이므로, 네트워크 문제를 배제하고 **Compose + Hilt + 모듈 의존성** 인프라만 격리하여 검증할 수 있습니다.

---

## Step 1 — core-model: 리그 상수 정의

### LeagueInfo.kt

```kotlin
// core/core-model/src/main/kotlin/.../core/model/LeagueInfo.kt
package com.chase1st.feetballfootball.core.model

data class LeagueInfo(
    val id: Int,
    val name: String,
    val country: String,
    val logoUrl: String,
)

// FotMob API 기준 리그 상수
object SupportedLeagues {
    private fun leagueLogoUrl(id: Int) = "https://images.fotmob.com/image_resources/logo/leaguelogo/dark/$id.png"

    val TOP_5_LEAGUES = listOf(
        LeagueInfo(id = 47, name = "Premier League", country = "England", logoUrl = leagueLogoUrl(47)),
        LeagueInfo(id = 87, name = "LaLiga", country = "Spain", logoUrl = leagueLogoUrl(87)),
        LeagueInfo(id = 55, name = "Serie A", country = "Italy", logoUrl = leagueLogoUrl(55)),
        LeagueInfo(id = 54, name = "Bundesliga", country = "Germany", logoUrl = leagueLogoUrl(54)),
        LeagueInfo(id = 53, name = "Ligue 1", country = "France", logoUrl = leagueLogoUrl(53)),
    )

    // FotMob 리그/컵 ID (경기 일정 필터링용)
    val ALL_LEAGUE_IDS = listOf(
        47, 87, 55, 54, 53,             // 5대 리그
        132, 133,                        // 잉글랜드 컵 (FA Cup, EFL Cup)
        42, 73,                          // UEFA (Champions League, Europa League)
        77, 50,                          // 국제대회 (World Cup, EURO)
    )
}
```

---

## Step 2 — core-model: 시즌 유틸리티

```kotlin
// core/core-model/src/main/kotlin/.../core/model/Season.kt
package com.chase1st.feetballfootball.core.model

import java.time.LocalDate
import java.time.Year

object SeasonUtil {
    /**
     * FotMob 시즌 문자열 결정
     * 7월 이전이면 전년도 시즌 시작, 현재 연도에서 끝남
     * 예: 2026년 3월 → "2025/2026", 2025년 9월 → "2025/2026"
     */
    fun currentSeason(): String {
        val year = Year.now().value
        val month = LocalDate.now().monthValue
        return if (month < 7) "${year - 1}/$year" else "$year/${year + 1}"
    }
}
```

---

## Step 3 — feature-league: LeagueListViewModel

```kotlin
// feature/feature-league/src/main/kotlin/.../feature/league/LeagueListViewModel.kt
package com.chase1st.feetballfootball.feature.league

import androidx.lifecycle.ViewModel
import com.chase1st.feetballfootball.core.model.LeagueInfo
import com.chase1st.feetballfootball.core.model.SupportedLeagues
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LeagueListViewModel @Inject constructor() : ViewModel() {

    val leagues: StateFlow<List<LeagueInfo>> =
        MutableStateFlow(SupportedLeagues.TOP_5_LEAGUES)
}
```

---

## Step 4 — feature-league: LeagueListScreen

```kotlin
// feature/feature-league/src/main/kotlin/.../feature/league/LeagueListScreen.kt
package com.chase1st.feetballfootball.feature.league

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chase1st.feetballfootball.core.designsystem.component.TeamLogo
import com.chase1st.feetballfootball.core.model.LeagueInfo
import com.chase1st.feetballfootball.core.model.SeasonUtil

@Composable
fun LeagueListScreen(
    onLeagueClick: (leagueId: Int, leagueName: String, season: String) -> Unit,
    viewModel: LeagueListViewModel = hiltViewModel(),
) {
    val leagues by viewModel.leagues.collectAsStateWithLifecycle()
    val currentSeason = SeasonUtil.currentSeason()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = leagues,
            key = { it.id },
        ) { league ->
            LeagueCard(
                league = league,
                onClick = { onLeagueClick(league.id, league.name, currentSeason) },
            )
        }
    }
}

@Composable
private fun LeagueCard(
    league: LeagueInfo,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TeamLogo(
                logoUrl = league.logoUrl,
                teamName = league.name,
                size = 48.dp,
            )
            Column {
                Text(
                    text = league.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = league.country,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
```

---

## Step 5 — 임시 연결 (Navigation 전까지)

Stage 2 Navigation 통합 전까지, `MainActivity`에서 직접 `LeagueListScreen`을 호출하여 검증합니다.

```kotlin
// app/src/main/kotlin/.../MainActivity.kt (임시)
setContent {
    FeetballTheme {
        LeagueListScreen(
            onLeagueClick = { id, name, season ->
                android.util.Log.d("Feetball", "Selected: $name ($id) season=$season")
            }
        )
    }
}
```

---

## ★ Slice 1 완료 검증

```bash
./gradlew assembleDebug
# 앱 실행 → 에뮬레이터/기기
```

**체크리스트:**

- [ ] 앱 실행 시 5개 리그 카드 표시
- [ ] 각 카드에 리그 로고 (Coil) + 이름 + 국가 표시
- [ ] 카드 클릭 시 Logcat에 리그 정보 출력
- [ ] Hilt ViewModel 주입 정상 (크래시 없음)
- [ ] Dark/Light 테마 전환 정상
- [ ] `git commit -m "feat: Slice 1 리그 선택 화면 구현"`
