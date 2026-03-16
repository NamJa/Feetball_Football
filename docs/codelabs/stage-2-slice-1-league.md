# Stage 2 / Slice 1 — 리그 선택 화면

> ⏱ 예상 소요 시간: 1시간 | 난이도: ★☆☆ | 선행 조건: Stage 1 완료 (11개 모듈 + Convention Plugins + Hilt DI 설정)

---

## 이 Codelab에서 배우는 것

- **core-model** 모듈에 도메인 상수(리그 목록)를 정의하는 방법
- **feature-league** 모듈에서 Hilt ViewModel을 생성하고 Compose UI와 연결하는 방법
- StateFlow를 사용한 정적 데이터 노출 패턴
- Compose `LazyColumn` + `Card`로 리스트 UI를 구성하는 방법
- Stage 1에서 만든 `FeetballTheme`과 `TeamLogo` 공통 컴포넌트 활용

---

## 완성 후 결과물

- 5대 리그(EPL, LaLiga, Serie A, Bundesliga, Ligue 1)가 카드 형태로 표시되는 화면
- 각 카드에 리그 로고, 이름, 국가가 표시됨
- 카드 클릭 시 리그 정보가 콜백으로 전달됨 (Slice 2에서 순위 화면으로 연결)
- Hilt ViewModel 주입이 정상 동작하는 것을 확인

---

## Step 1 — core-model: 리그 상수 정의

### 목표
> FotMob API 기준 리그/컵 ID를 **core-model 모듈**의 도메인 객체로 정의합니다.

### 작업 내용

API 호출 없이 정적 데이터만 사용하는 이 Slice의 핵심 데이터입니다. `LeagueInfo` data class와 `SupportedLeagues` object를 FotMob API의 리그 ID와 로고 URL 형식에 맞게 만들어 앱 전체에서 참조할 수 있게 합니다.

> 💡 **Tip:** `SupportedLeagues.ALL_LEAGUE_IDS`는 Slice 3(경기 일정)에서 FotMob API 응답 필터링에 사용됩니다. 지금 미리 정의해 두면 나중에 다시 수정할 필요가 없습니다.

**파일 경로:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/LeagueInfo.kt`

```kotlin
// core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/LeagueInfo.kt
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

### ✅ 검증
- [ ] `core-model` 모듈이 빌드 에러 없이 컴파일되는지 확인
- [ ] `LeagueInfo` data class에 `id`, `name`, `country`, `logoUrl` 4개 필드가 있는지 확인
- [ ] `SupportedLeagues.TOP_5_LEAGUES`에 5개 리그가 정의되어 있는지 확인
- [ ] `SupportedLeagues.ALL_LEAGUE_IDS`에 11개 ID가 정의되어 있는지 확인

---

## Step 2 — core-model: 시즌 유틸리티

### 목표
> FotMob API의 시즌 형식(`"YYYY/YYYY"`)에 맞는 시즌 문자열을 결정하는 **순수 유틸리티 함수**를 만듭니다.

### 작업 내용

축구 시즌은 보통 8월~다음해 5~6월입니다. 따라서 7월 이전이면 전년도에 시작한 시즌으로 판단합니다. FotMob API는 시즌을 `"2025/2026"` 형식의 문자열로 사용하므로, 이에 맞는 형식으로 반환합니다.

> ⚠️ **주의:** `java.time` API를 사용합니다. 이 프로젝트의 `minSdk`는 26이므로 `java.time`을 직접 사용할 수 있습니다 (ThreeTenABP 불필요).

**파일 경로:** `core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/Season.kt`

```kotlin
// core/core-model/src/main/kotlin/com/chase1st/feetballfootball/core/model/Season.kt
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

### ✅ 검증
- [ ] `SeasonUtil.currentSeason()`이 현재 날짜 기준으로 올바른 FotMob 시즌 문자열을 반환하는지 확인
  - 예: 2026년 3월 → `"2025/2026"` 반환 (7월 이전이므로 전년도 시즌)
  - 예: 2026년 8월 → `"2026/2027"` 반환 (7월 이후이므로 올해 시즌)
- [ ] `core-model` 모듈 빌드 성공 확인

---

## Step 3 — feature-league: LeagueListViewModel

### 목표
> Hilt가 주입하는 ViewModel을 만들어 **정적 리그 목록을 StateFlow로 노출**합니다.

### 작업 내용

이 ViewModel은 API 호출 없이 `SupportedLeagues.TOP_5_LEAGUES`를 그대로 노출합니다. Compose UI에서 `collectAsStateWithLifecycle()`로 구독하게 됩니다.

> 💡 **Tip:** `@HiltViewModel` + `@Inject constructor()`가 있으면 Hilt가 자동으로 ViewModel을 생성해 줍니다. `hiltViewModel()` Composable 함수로 주입받을 수 있습니다.

**파일 경로:** `feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/LeagueListViewModel.kt`

```kotlin
// feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/LeagueListViewModel.kt
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

> 💡 **Tip:** 지금은 생성자가 비어있지만, 나중에 "최근 본 리그" 같은 기능을 추가하면 Repository를 주입받을 수 있습니다. `@HiltViewModel`이 있으므로 생성자 파라미터만 추가하면 됩니다.

### ✅ 검증
- [ ] `feature-league` 모듈이 `core-model`에 의존성이 있는지 `build.gradle.kts` 확인
- [ ] `@HiltViewModel` 어노테이션이 있는지 확인
- [ ] `leagues` StateFlow가 5개 리그 목록을 반환하는지 확인

---

## Step 4 — feature-league: LeagueListScreen

### 목표
> Compose UI로 리그 카드 리스트를 구현합니다. core-designsystem의 `TeamLogo` 공통 컴포넌트를 사용합니다.

### 작업 내용

`LazyColumn`으로 5개 리그 카드를 표시합니다. 각 카드에는 로고, 리그명, 국가명이 표시되며, 클릭 시 `onLeagueClick` 콜백이 호출됩니다.

**파일 경로:** `feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/LeagueListScreen.kt`

```kotlin
// feature/feature-league/src/main/kotlin/com/chase1st/feetballfootball/feature/league/LeagueListScreen.kt
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

> ⚠️ **주의:** `TeamLogo`는 core-designsystem에서 제공하는 공통 컴포넌트입니다. Stage 1에서 이미 만들어져 있어야 합니다. 만약 없다면, 먼저 core-designsystem에 `TeamLogo` Composable을 구현하세요.

### ✅ 검증
- [ ] `LeagueListScreen`이 `hiltViewModel()`로 ViewModel을 주입받는지 확인
- [ ] `collectAsStateWithLifecycle()`로 StateFlow를 구독하는지 확인
- [ ] `LazyColumn`의 `items`에 `key = { it.id }`가 설정되어 있는지 확인
- [ ] `LeagueCard`에 로고(48dp), 리그명, 국가명이 표시되는지 확인
- [ ] 카드 클릭 시 `onLeagueClick`에 `leagueId`, `leagueName`, `currentSeason`(String)이 전달되는지 확인

---

## Step 5 — 임시 연결 (Navigation 전까지)

### 목표
> Navigation 통합 전까지 `MainActivity`에서 직접 `LeagueListScreen`을 호출하여 전체 파이프라인을 검증합니다.

### 작업 내용

Stage 2에서 Navigation 3를 통합하기 전까지, `MainActivity`의 `setContent` 블록에서 `LeagueListScreen`을 직접 호출합니다. 카드 클릭 시 Logcat에 리그 정보가 출력되도록 합니다.

> 💡 **Tip:** 이 코드는 임시 코드입니다. Navigation 통합 시 교체됩니다. 하지만 **Hilt DI → ViewModel → Compose UI** 파이프라인이 정상 동작하는지 확인하는 데 필수적입니다.

**파일 경로:** `app/src/main/kotlin/com/chase1st/feetballfootball/MainActivity.kt` (임시 수정)

```kotlin
// app/src/main/kotlin/com/chase1st/feetballfootball/MainActivity.kt (임시)
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

> ⚠️ **주의:** `MainActivity`에서 `LeagueListScreen`을 호출하려면 `app` 모듈이 `feature-league` 모듈에 의존해야 합니다. `app/build.gradle.kts`에 `implementation(projects.feature.featureLeague)` 의존성을 추가하세요.

### ✅ 검증
- [ ] 앱 실행 시 5개 리그 카드가 화면에 표시되는지 확인
- [ ] 각 카드에 리그 로고(Coil) + 이름 + 국가가 표시되는지 확인
- [ ] 카드 클릭 시 Logcat에 `Selected: Premier League (47) season=2025/2026` 같은 로그가 출력되는지 확인
- [ ] Hilt ViewModel 주입이 정상 동작하는지 확인 (크래시 없음)

---

## Step 6 — 빌드 및 최종 검증

### 목표
> 전체 빌드를 수행하고 에뮬레이터/기기에서 동작을 확인합니다.

### 작업 내용

```bash
./gradlew assembleDebug
# 앱 실행 → 에뮬레이터/기기
```

### ✅ 검증
- [ ] 앱 실행 시 5개 리그 카드 표시
- [ ] 각 카드에 리그 로고 (Coil) + 이름 + 국가 표시
- [ ] 카드 클릭 시 Logcat에 리그 정보 출력
- [ ] Hilt ViewModel 주입 정상 (크래시 없음)
- [ ] Dark/Light 테마 전환 정상
- [ ] `git commit -m "feat: Slice 1 리그 선택 화면 구현"`

---

## 🎉 Slice 1 완료!

축하합니다! 첫 번째 Slice를 완료했습니다.

**이 Slice에서 달성한 것:**
- `core-model`에 리그 상수와 시즌 유틸리티를 정의했습니다
- `feature-league`에 Hilt ViewModel과 Compose Screen을 구현했습니다
- **Compose + Hilt + 모듈 의존성** 인프라가 정상 동작함을 확인했습니다

**다음 단계:** Slice 2에서는 FotMob API 첫 연동을 통해 리그 순위 화면을 구현합니다. DTO → Domain → Repository → UseCase → ViewModel → UI 전체 수직 스택을 구축합니다.
