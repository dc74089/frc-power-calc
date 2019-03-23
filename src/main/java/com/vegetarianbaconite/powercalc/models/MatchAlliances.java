package com.vegetarianbaconite.powercalc.models;

public class MatchAlliances {
    Alliance red;
    Alliance blue;

    public MatchAlliances() {
    }

    public MatchAlliances(Alliance red, Alliance blue) {
        this.red = red;
        this.blue = blue;
    }

    public Alliance getRed() {
        return red;
    }

    public Alliance getBlue() {
        return blue;
    }
}
