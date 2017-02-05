package com.dominicc.me;

import com.vegetarianbaconite.blueapi.SynchronousBlueAPI;
import com.vegetarianbaconite.blueapi.beans.Alliance;
import com.vegetarianbaconite.blueapi.beans.Match;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class PowerCalc {
    private String eventKey;
    private Boolean qualsOnly;

    private SynchronousBlueAPI api = new SynchronousBlueAPI("DominicCanora", "PowerCalc", "1");
    private List<Match> eventMatches;

    private TreeSet<Integer> teams = new TreeSet<>();
    private Map<String, Integer> teamKeyPositionMap = new HashMap<>();
    private double[][] matrix, scores;
    private RealMatrix finalMatrix;
    private CholeskyDecomposition cholesky;

    public PowerCalc(String eventKey, Boolean qualsOnly) {
        eventMatches = api.getEventMatches(eventKey);

        for (Match m : eventMatches) {
            for(String t : m.getAlliances().getRed().getTeams())
                if(!teams.contains(Integer.parseInt(t.substring(3))))
                    teams.add(Integer.parseInt(t.substring(3)));

            for(String t : m.getAlliances().getBlue().getTeams())
                if(!teams.contains(Integer.parseInt(t.substring(3))))
                    teams.add(Integer.parseInt(t.substring(3)));
        }

        int i = 0;
        for (Integer t : teams) {
            teamKeyPositionMap.put("frc" + t, i);
            i++;
        }

        matrix = new double[teams.size()][teams.size()];
        scores = new double[teams.size()][1];

        this.eventKey = eventKey;
        cleanup();
        reInit(qualsOnly);
        System.out.println("Initialized PowerCalc");
    }

    public Map<Integer, Double> getForKey(String key) {
        synchronized (this) {
            cleanup();

            Map<Integer, Double> returnedMap = new HashMap<>();

            for (Match m : eventMatches) {
                if(!m.getCompLevel().equals("qm") && qualsOnly) continue;

                for (String team : m.getAlliances().getBlue().getTeams())
                    if (key.equalsIgnoreCase("opr"))
                        scores[teamKeyPositionMap.get(team)][0] += m.getAlliances().getBlue().getScore();
                    else
                        scores[teamKeyPositionMap.get(team)][0] += Double.parseDouble(m.getScoreBreakdown().getBlue().get(key));
                for (String team : m.getAlliances().getRed().getTeams())
                    if (key.equalsIgnoreCase("opr"))
                        scores[teamKeyPositionMap.get(team)][0] += m.getAlliances().getBlue().getScore();
                    else
                        scores[teamKeyPositionMap.get(team)][0] += Double.parseDouble(m.getScoreBreakdown().getRed().get(key));
            }

            RealMatrix scoreMatrix = MatrixUtils.createRealMatrix(scores);
            double[][] output = cholesky.getSolver().solve(scoreMatrix).getData();

            for (Integer team : teams) {
                returnedMap.put(team, output[teamKeyPositionMap.get("frc" + team)][0]);
            }

            return returnedMap;
        }
    }

    public Map<Integer, Double> getForSupplier(StatProvider sp) {
        synchronized (this) {
            cleanup();

            Map<Integer, Double> returnedMap = new HashMap<>();

            for (Match m : eventMatches) {
                if(!m.getCompLevel().equals("qm") && qualsOnly) continue;

                for (String team : m.getAlliances().getBlue().getTeams())
                    scores[teamKeyPositionMap.get(team)][0] += sp.get(m.getScoreBreakdown().getBlue());
                for (String team : m.getAlliances().getRed().getTeams())
                    scores[teamKeyPositionMap.get(team)][0] += sp.get(m.getScoreBreakdown().getRed());
            }

            RealMatrix scoreMatrix = MatrixUtils.createRealMatrix(scores);
            double[][] output = cholesky.getSolver().solve(scoreMatrix).getData();

            for (Integer team : teams) {
                returnedMap.put(team, output[teamKeyPositionMap.get("frc" + team)][0]);
            }

            return returnedMap;
        }
    }

    private void cleanup() {
        scores = new double[teams.size()][1];
    }

    public void reInit(boolean qualsOnly) {
        this.qualsOnly = qualsOnly;
        synchronized (this) {
            for (Integer t : teams) {
                for (Match m : api.getTeamEventMatches(t, eventKey)) {
                    if (!m.getCompLevel().equals("qm") && qualsOnly) continue;

                    Alliance a = m.getAlliances().getBlue().contains("frc" + t) ?
                            m.getAlliances().getBlue() : m.getAlliances().getRed();
                    for (String allianceMember : a.getTeams()) {
                        matrix[teamKeyPositionMap.get("frc" + t)][teamKeyPositionMap.get(allianceMember)] += 1;
                    }
                }
            }

            finalMatrix = MatrixUtils.createRealMatrix(matrix);
            cholesky = new CholeskyDecomposition(finalMatrix);
        }
    }

    public interface StatProvider {
        double get(Map<String, String> scoreBreakdown);
    }
}
