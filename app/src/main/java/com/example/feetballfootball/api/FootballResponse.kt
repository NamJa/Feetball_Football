package com.example.feetballfootball.api

class FootballResponse {
    lateinit var response: List<FixtureResponse>
    // json 최상위 객체 "response"를 받기 위함
}
/**
 **********************************
    API 구조
    class(json 변수 및 생성되는 변수)
 **********************************
    FootballResponse
        FixtureResponse(response)

            Fixture(fixture)
                id - Int
                referee - String
                timestamp - Int
                FixtureVenue(venue)
                    id - Int
                    name - String => 경기장
                FixtureStatus(status)
                    short - String => 경기의 상태 ( ex) PST=Match PostPoned, FT=Match Finished, NS=Not Started

            League(league)
                id - Int      (리그 ID)
                name - String (리그 이름)

            Teams(teams)
                TeamHome(home)
                    id - Int
                    name - String
                    logoUrl - String
                TeamAway(away)
                    id - Int
                    name - String
                    logoUrl - String

            Goals(goals)
                home - Int
                away - Int
* */
/**
 * ***************  League  ******************
 * 39: EPL, 140: LA LIGA,
 * 135: SERIE A, 78: BUNDESLIGA,
 * 61: LIGUE 1,
 * ****************  Cup  ********************
 * 45: FA CUP, 48: League CUP,
 * 528: Community Shield, 143: Copa del Rey,
 * 81: DFB Pokal, 137: Coppa Italia,
 * 66: Couppe de France,
 * **********  UEFA LEAGUE & CUP  ************
 * 2: UEFA Champions League, 3: UEFA Europa League, 531: Super Cup, 848: UEFA Europa Conference League
* */