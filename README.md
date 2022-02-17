[![Build Status](https://img.shields.io/badge/platform-Android-green)](https://www.android.com/) [![API](https://img.shields.io/badge/API-+23-brightgreen)](https://android-arsenal.com/api?level=23) 
# Feetball Football
## USED API
- [API-Sports](https://api-sports.io/documentation/football/v3)

## Short usage shot
<img src="./images/0205pic/shortUsed.gif" width="220" height="489">

## 경기 일정 및 상세 정보 - Light & Dark mode
### - 경기 일정
<img src="./images/0205pic/lightmode/fixture_light_1.jpg" width="220" height="489"> <img src="./images/0205pic/darkmode/fixture_dark_1.jpg" width="220" height="489">

### - 경기 상세 정보 - Light Mode
<img src="./images/0205pic/lightmode/fixturedetail_light_events.jpg" width="220" height="489"> <img src="./images/0205pic/lightmode/fixturedetail_light_lineups_1.jpg" width="220" height="489"> <img src="./images/0205pic/lightmode/fixturedetail_light_lineups_2.jpg" width="220" height="489"> <img src="./images/0205pic/lightmode/fixturedetail_light_lineups_3.jpg" width="220" height="489"> <img src="./images/0205pic/lightmode/fixturedetail_light_stat_1.jpg" width="220" height="489"> <img src="./images/0205pic/lightmode/fixturedetail_light_stat_2.jpg" width="220" height="489">

### - 경기 상세 정보 - Dark Mode
<img src="./images/0205pic/darkmode/fixturedetail_dark_events.jpg" width="220" height="489"> <img src="./images/0205pic/darkmode/fixturedetail_dark_lineups_1.jpg" width="220" height="489"> <img src="./images/0205pic/darkmode/fixturedetail_dark_lineups_2.jpg" width="220" height="489">

<img src="./images/0205pic/darkmode/fixturedetail_dark_stat_1.jpg" width="220" height="489"> <img src="./images/0205pic/darkmode/fixturedetail_dark_stat_2.jpg" width="220" height="489">

### 리그 클럽 및 선수 순위 - Light Mode
<img src="./images/0205pic/lightmode/league_light.jpg" width="220" height="489"> <img src="./images/0205pic/lightmode/league_light_epl_clubstanding.jpg" width="220" height="489"> <img src="./images/0205pic/lightmode/league_light_epl_playerstanding_1.jpg" width="220" height="489"> <img src="./images/0205pic/lightmode/league_light_epl_playerstanding_2.jpg" width="220" height="489">

### 리그 클럽 및 선수 순위 - Dark Mode
<img src="./images/0205pic/darkmode/league_dark.jpg" width="220" height="489"> <img src="./images/0205pic/darkmode/league_dark_epl_clubstanding.jpg" width="220" height="489"> <img src="./images/0205pic/darkmode/league_dark_epl_playerstanding_1.jpg" width="220" height="489"> <img src="./images/0205pic/darkmode/league_dark_epl_playerstanding_2.jpg" width="220" height="489">


### 뉴스 소식
<img src="./images/0205pic/lightmode/news_light.jpg" width="220" height="489"> <img src="./images/0205pic/darkmode/news_dark.jpg" width="220" height="489">

- 네이버 스포츠의 해외 축구 기사를 웹뷰로 출력하거나 Retrofit의 html response로 받아올 예정


## 소개

지원하는 리그는 하단에 표기되어있습니다.

FixtureFragment에서 출력할 구성하는게 제일 골치아팠는데, API 자체가 전세계 리그 및 컵 경기의 전체 데이터를 받아오거나 특정 리그의 정보만 받아올 수 있도록 되어있습니다. 

그렇기 때문에 하단의 주요 인기 리그 및 컵 경기를 가져오도록 구성하는데에 꽤 시간을 많이 소비했습니다. 

하루에 한정된 요청 횟수 제한이 있었기 때문에 꽤 짜릿했습니다. 허허.



## Leagues & Cups
    EPL (England)           FA CUP (England)            UEFA Champions League
    LA LIGA (Spain)         League Cup (England)        UEFA Europa League
    SERIE A (Italy)         Community Shield (England)  UEFA Super Cup
    BUNDESLIGA (Germany)    Copa del Rey (Spain)        UEFA Europa Conference league
    LIGUE 1 (France)        DFB Pokal (Germany)
                            Coppa Italia (Italy)
                            Couppe de France (France)

## Libraries
- [Retrofit2](https://square.github.io/retrofit/)
- [OkHttp](https://square.github.io/okhttp/)
- [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel?hl=ko)
- [GSON](https://github.com/google/gson)
- [LifeCycle-Extensions](https://developer.android.com/jetpack/androidx/releases/lifecycle?hl=ko)
- [ViewPager2](https://developer.android.com/jetpack/androidx/releases/viewpager2?hl=ko)
- [Coroutine](https://developer.android.com/kotlin/coroutines?hl=ko)
- [ThreeTenbp](https://www.threeten.org/threetenbp/)
- [Picasso](https://square.github.io/picasso/)
- [Material UI](https://material.io/) - CoorinatorLayout

## RoadMap
- 경기 일정

    ~~- 이전 및 다음 경기 일정 출력~~
- 경기의 상세 결과 출력

    ~~- 슈팅 통계~~

    ~~- 라인업~~

    ~~- 평점~~
- 리그 순위

    ~~- 득점 및 어시스트, 이를 종합한 공격포인트 순위~~

- 뉴스 수신

    ~~- 언론사 선정 작업중...~~
    - 뉴스 페이지 구현 예정
- 디자인 개선
    
    ~~- 다크모드까지 적용 예정~~

[상세 로드맵](https://github.com/NamJa/Feetball_Football/projects/2)
