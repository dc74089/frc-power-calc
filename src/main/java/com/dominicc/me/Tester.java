package com.dominicc.me;

import com.vegetarianbaconite.blueapi.beans.Team;

import java.util.Map;

public class Tester {
    public static void main(String... args) {
        Map<Integer, Double> map = new PowerCalc("2016flor", true).getForKey("autoPoints");
        for (Integer t : map.keySet()) {
            System.out.println("Team " + t + ": " + map.get(t));
        }
    }
}
