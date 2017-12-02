package com.vegetarianbaconite.powercalc;

import io.swagger.client.ApiException;

public class Tester {
    public static void main(String... args) throws ApiException {
        PowerCalc pc = new PowerCalc("dfKdISjuw3Zd0Qt1LlboSHjIasr0u9sbyRCjvlbFrfMXH3TOOjX0R5K3DjWLrEnj",
                "2017lake", false);

        System.out.println(pc.getForKeySorted("opr"));
    }
}
