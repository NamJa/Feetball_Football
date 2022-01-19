package com.example.feetballfootball.api.fixturedetail

class MatchDetailResponse {
    lateinit var response: List<FixtureDetailResponse>
}
/**
 **********************************
    API 구조
    class(json 변수 및 생성되는 변수)
 **********************************
 *      MatchDetailResponse
 *          FixtureDetailResponse(response)
 *
 *              Fixture(fixture)
 *                  id - Int                (경기 id)
 *                  referee - String        (심판)
 *                  date - String           (날짜)
 *                  timestamp - Int         (Unix timestamp, 경기 시작 시간)
 *                  FixtureVenue(venue)
 *                      id - Int            (경기장 id)
 *                      name - String       (경기장 이름)
 *
 *
 *                  FixtureStatus(status)
 *                      short - String      (경기 상태)
 *                      elapsed - Int       (현재 경기 진행 시간)
 *
 *
 *              League(league)
 *                  id - Int                (리그 id)
 *                  name - String           (리그 이름)
 *                  round - String          (리그 라운드)
 *
 *
 *              Teams(teams)
 *                  TeamHome(home) - 홈팀
 *                      id - Int
 *                      name - String       (팀 이름)
 *                      logoUrl - String    (팀 로고)
 *
 *
 *                  TeamAway(away) - 원정팀
 *                      id - Int
 *                      name - String
 *                      logoUrl - String
 *
 *
 *              Goals(goals)
 *                  home - Int  (홈팀 점수)
 *                  away - Int  (원정팀 점수)
 *
 *
 *              List<Events>(events) - 선수 교체, 카드, 득점 기록
 *                  Time(time)
 *                      elapsed - Int (이벤트 발생 시간)
 *                      extra - Int?  (추가시간에 발생한 이벤트를 위함, 정규 시간일 땐 null)
 *
 *
 *                  Team(team)
 *                      id - Int
 *                      name - String
 *                      logo - String
 *
 *
 *                  EventPlayer(player) - events 속성 내에서만 사용되는 Player 정보
 *                      id - Int        (선수 id)
 *                      name - String   (선수 이름)
 *
 *
 *                  Assist(assist) - 교체로 들어오는 선수 및 득점의 어시스트 선수
 *                      id - Int?        (선수 id, 골대 맞고 튀어나온 볼을 골로 만들어내면 어시스트 선수가 없으며, 단독으로 카드를 받을 경우에도 null값)
 *                      name - String?   (교체 명단 선수 및 어시스트 기록 선수 이름, ?인지는 위에서 설명)
 *
 *
 *                  type - String       ("Card", "subst", "Goal" : 카드, 교체, 골 이벤트)
 *                  detail - String     ("Yellow Card", "Red Card", "Normal Goal": 상세한 이벤트 내용)
 *
 *
 *              List<Lineups>(lineups) - index 0: 홈 팀, index 1: 원정 팀 => 총 2개의 인덱스
 *                  LineupTeam(team)
 *                      id - Int                            (팀 id값)
 *                      name - String                       (팀 이름)
 *                      logoUrl - String                    (팀 로고)
 *                      TeamColors(colors) - 팀 컬러코드, ex) 맨유는 빨강, 맨시티는 하늘색
 *                          TeamColorCode(teamColorCode)
 *                              colorCode - String          (6자리의 html 색상코드)
 *
 *
 *                  Coach(coach)
 *                      name - String   (감독 이름)
 *
 *
 *                  formation - String  (진형, ex) "4-3-3", "3-5-2", "4-2-4")
 *                  List<PlayerData>(startXI) - 선발 선수 명단
 *                      Player(player)
 *                          id - Int            (선수 id)
 *                          name - String       (선수 이름)
 *                          number - Int        (선수 등 번호)
 *                          pos - String        (선수 포지션, ex) "D", "M", "F")
 *
 *
 *                  List<PlayerData>(substitutes) - 교체 선수 명단
 *                      Player(player)
*                           id - Int            (위의 설명과 같다)
 *                          name - String
 *                          number - Int
 *                          pos - String
 *
 *
 *              List<Statistics>(statistics) - index 0: 홈 팀, index 1: 원정 팀 => 총 2개의 인덱스
 *                  Team(team)
 *                      id - Int        (팀 id)
 *                      name - String   (팀 이름)
 *                      logo - String   (팀 로고)
 *
 *
 *                  List<StatisticsData>(statistics)
 *                      type - String   (유효 슈팅, 난사, 총 슈팅, 막힌 슛, 박스 내 슛, 박스 밖 슛, 파울, 코너킥, 오프사이드, 점유율, 옐로 카드, 레드 카드, 키퍼 선방, 패스 횟수, 정확한 패스, 패스 성공률)
 *                      value - Any?    (각 항목의 값, Any? 인 이유는 정수값이나 string값이나 null값이 들어오기 때문)
 *
 *
*               List<PlayersByTeamData>(players)
 *                  Team(team)
 *                      id - Int
 *                      name - String
 *                      logo - String
 *
 *
 *                  List<PlayerRatingData>(players) - index 0: 홈 팀, index 1: 원정 팀 => 총 2개의 인덱스
 *                      ShortPlayerData(player)
 *                          id - Int                (선수 id)
 *                          name - String           (선수 이름)
 *
 *
 *                      List<PlayerStatistics>(statistics)
 *                          Games(games)
 *                              minutes - Int?      (출전시간: 경기에 출전하지 않으면 null값이 됨)
 *                              number - Int        (선수 등 번호)
 *                              rating - String?    (경기 평점: 뛰지 않았다면 null값이 됨)
 *
 * */