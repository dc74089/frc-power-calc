package com.vegetarianbaconite.powercalc;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.EventApi;
import io.swagger.client.model.Event;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Tester {
    static String api_key = "17lyXJwobWwpFYaVxakR2U2wGNwmuAuxfBDK26lhY30wGS2YbR89xtCpNBH1x3Ef";

    public static void main(String... args) throws ApiException {
        BPRCalculator calc = new BPRCalculator(api_key, "2016flor");
        calc.getForKey("opr");
        calc.getForKey("dpr");
        calc.getForKey("ccwm");
    }

    public static void getFailed(String... args) throws ApiException {
        ApiClient client = new ApiClient();
        client.setApiKey(api_key);
        EventApi api = new EventApi(client);

        Scanner s = new Scanner(System.in);

        List<String> failed;

        for (; true; ) {
            int year = s.nextInt();
            List<String> events = api.getEventsByYearKeys(new BigDecimal(year), null);
            failed = new ArrayList<>();

            for (String eventString : events) {
                System.out.println(eventString);
                try {
                    Event e = api.getEvent(eventString, null);
                    new BPRCalculator(api_key, e.getKey(), true);
                    System.out.println("Succeeded at " + e.getName());
                } catch (Exception ex) {
                    System.out.println("FAILED at " + eventString);
                    //ex.printStackTrace();
                    failed.add(eventString);
                }
            }

            System.out.println(String.format("\n\n\nFailed Events: %d out of %d", failed.size(), events.size()));

            for (String name : failed) {
                System.out.println(name);
            }
        }
    }
}
