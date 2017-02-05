package com.dominicc.me;

import java.util.*;

public class Tester {
    public static void main(String... args) {
        PowerCalc c = new PowerCalc("2015gape", true);
        final Map<Integer, Double> map = c.getForKey("total_points");

        List<Integer> teamNums = new ArrayList<>(map.keySet());
        Collections.sort(teamNums, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return (int) Math.round((map.get(o2) - map.get(o1)) * 1000);
            }
        });
        int rank = 1;
        for (Integer t : teamNums) {
            System.out.println(rank + ". Team " + t + ": " + map.get(t));
            rank++;
        }
    }
}
