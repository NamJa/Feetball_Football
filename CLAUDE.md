# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew clean                  # Clean build artifacts
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)
```

## Project Overview

Feetball Football is an Android app (Kotlin) that displays football (soccer) match schedules, detailed match statistics, league standings, and news. It uses the [API-Sports Football v3](https://api-sports.io/documentation/football/v3) API and supports 20 leagues/cups.

## Architecture

**Single Activity + Fragments (MVVM pattern)**

- `MainActivity` hosts 3 tabs via TabLayout: Fixtures, Leagues, News
- Fragment-to-Activity communication uses callback interfaces
- ViewModels expose data via LiveData; Fragments observe LiveData
- ViewBinding is used throughout (recently migrated from findViewById)

**Key packages** under `com.example.feetballfootball`:

| Package | Purpose |
|---------|---------|
| `api/` | Retrofit API interface (`FootballApi`) and 40+ data model classes |
| `fragment/fixture/` | Match schedule list, match detail with 3 sub-tabs (Events, Lineups, Statistics) |
| `fragment/Leagues/` | League selection, club standings table, player standings |
| `fragment/news/` | WebView-based news display |
| `viewModel/` | `FeetballFootballViewModel`, `FixtureDetailViewModel`, `StandingViewModel` |
| `adapter/` | RecyclerView adapters (some are inner classes within Fragments) |
| `behavior/` | Custom CoordinatorLayout behaviors for match detail header animations |
| `util/` | `FootballDataFetchr` (API calls), `DividerItemDecoration` |

**Data flow**: Fragment → ViewModel (coroutine/thread) → FootballDataFetchr → Retrofit API → LiveData → Fragment observes & updates UI

## API Integration

- Base URL: `https://v3.football.api-sports.io/`
- Auth: `x-apisports-key` header (hardcoded in `FootballDataFetchr`)
- Key endpoints: `/fixtures`, `/standings`, `/players/topscorers`, `/players/topassists`
- Networking: Retrofit2 + GSON + OkHttp3
- Async: Mix of `thread {}` blocks and Kotlin coroutines

## Build Configuration

- **SDK**: compileSdk 34, minSdk 26, targetSdk 34
- **Kotlin**: 2.1.0, JVM target 1.8
- **AGP**: 8.13.2, Gradle 8.13
- **ViewBinding**: enabled (`buildFeatures { viewBinding = true }`)
- **ProGuard**: disabled for release builds

## Conventions

- **Language**: Kotlin (100%), comments in Korean
- **ViewBinding pattern in Fragments**: `_binding` (nullable backing field) / `binding` (non-null accessor), nullified in `onDestroyView`
- **League/cup IDs**: Defined as constants in `FootballDataFetchr` (e.g., EPL=39, LALIGA=140)
- **Image loading**: Picasso for team logos and player images
- **Date handling**: ThreeTenABP (JSR-310 backport)
- **Commit messages**: Korean descriptions, prefixed with `feat:`, `fix:`, etc.

## Key Libraries

Retrofit2, GSON, Picasso, ThreeTenABP, Coroutines, ViewPager2, Material Components (DayNight theme with light/dark mode support), AndroidX Lifecycle/ViewModel
