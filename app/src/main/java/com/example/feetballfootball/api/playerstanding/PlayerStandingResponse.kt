package com.example.feetballfootball.api.playerstanding


class PlayerStandingResponse {
    lateinit var response: List<PlayerStandingStatistics>
}
/**
 **********************************
    API 구조
    class(json 변수 및 생성되는 변수)
 **********************************
 *  PlayerStandingResponse
 *      List<PlayerStandingStatistics>(response)
 *
 *          Player(player)
 *              id - Int            - player id
 *              name - String       - player 이름
 *
 *          List<PlayerStatistics>(statistics)
 *              Team(team)
 *                  id - Int        - team id
 *                  name - String   - team 이름
 *                  logo - String   - team logo url
 *
 *              Shots(shots)
 *                  total - Int     - 총 슈팅 수
 *                  on - Int        - 유효 슈팅
 *
 *              Goals(goals)
 *                  total - Int     - 골
 *                  assists - Int   - 어시스트
 *
 *              Penalty(penalty)
 *                  scored - Int    - 패널티킥 골
 */