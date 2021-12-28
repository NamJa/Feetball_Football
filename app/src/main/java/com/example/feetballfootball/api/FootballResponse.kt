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