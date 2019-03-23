package com.vegetarianbaconite.powercalc.models;

import java.util.Arrays;
import java.util.List;

public class Alliance {
    Integer score;
    String[] team_keys;

    public Alliance() {
    }

    public Alliance(Integer score, String[] team_keys) {
        this.score = score;
        this.team_keys = team_keys;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public List<String> getTeamKeys() {
        return Arrays.asList(team_keys);
    }

    public String[] getTeam_keys() {
        return team_keys;
    }

    public void setTeam_keys(String[] team_keys) {
        this.team_keys = team_keys;
    }
}
