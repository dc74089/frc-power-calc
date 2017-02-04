package com.dominicc.me;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Tester {
    public static void main(String... args) {
        Map<Integer, Double> map = new PowerCalc("2016flor", true).getForKey("totalPoints");

        List<Integer> teamNums = new ArrayList<>(map.keySet());
        Collections.sort(teamNums, (o1, o2) -> (int) Math.round((map.get(o2) - map.get(o1)) * 1000));
        int rank = 1;
        for (Integer t : teamNums) {
            System.out.println(rank + ". Team " + t + ": " + map.get(t));
            rank++;
        }
    }
}
