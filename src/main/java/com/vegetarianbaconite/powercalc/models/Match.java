package com.vegetarianbaconite.powercalc.models;

public class Match {
    String key;
    String comp_level;
    Integer match_number;
    MatchAlliances alliances;

    ScoreBreakdown score_breakdown;

    public Match() {
    }

    public Match(String key, String comp_level, Integer match_number, MatchAlliances alliances, ScoreBreakdown score_breakdown) {
        this.key = key;
        this.comp_level = comp_level;
        this.match_number = match_number;
        this.alliances = alliances;
        this.score_breakdown = score_breakdown;
    }

    public String getKey() {
        return key;
    }

    public String getCompLevel() {
        return comp_level;
    }

    public Integer getMatchNumber() {
        return match_number;
    }

    public MatchAlliances getAlliances() {
        return alliances;
    }

    public ScoreBreakdown getScoreBreakdown() {
        return score_breakdown;
    }
}
