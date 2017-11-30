package com.dominicc.me;

import io.swagger.client.ApiException;

import java.util.*;

public class Tester {
    public static void main(String... args) throws ApiException {

        PowerCalc c = new PowerCalc("", "2017flor", true);

        Scanner s = new Scanner(System.in);
        while (true) {
            String stat = s.nextLine();
            final Map<Integer, Double> map = c.getForKey(stat);

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
}
