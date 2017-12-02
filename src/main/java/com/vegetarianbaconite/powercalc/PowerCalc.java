package com.vegetarianbaconite.powercalc;

import com.vegetarianbaconite.powercalc.exceptions.NoMatchesException;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.EventApi;
import io.swagger.client.model.Match;
import io.swagger.client.model.MatchAlliance;
import io.swagger.client.model.MatchSimpleAlliances;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.NonPositiveDefiniteMatrixException;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.*;

public class PowerCalc {
    private String eventKey;
    private Boolean qualsOnly;

    private ApiClient apiClient;
    private EventApi eventApi;
    private List<Match> eventMatches;

    private TreeSet<Integer> teams = new TreeSet<>();
    private Map<String, Integer> teamKeyPositionMap = new HashMap<>();
    private double[][] matrix, scores;
    private RealMatrix finalMatrix;
    private CholeskyDecomposition cholesky;

    private Set<String> stats;

    public PowerCalc(String apiKey, String eventKey, Boolean qualsOnly) throws ApiException {
        apiClient = new ApiClient();
        apiClient.setApiKey(apiKey);
        eventApi = new EventApi(apiClient);

        eventMatches = eventApi.getEventMatches(eventKey, null);

        for (Match m : eventMatches) {
            for (String t : m.getAlliances().getRed().getTeamKeys())
                if (!teams.contains(Integer.parseInt(t.substring(3))))
                    teams.add(Integer.parseInt(t.substring(3)));

            for (String t : m.getAlliances().getBlue().getTeamKeys())
                if (!teams.contains(Integer.parseInt(t.substring(3))))
                    teams.add(Integer.parseInt(t.substring(3)));
        }

        stats = eventMatches.get(0).getScoreBreakdown().getBlue().keySet();

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
                if (m.getCompLevel() != Match.CompLevelEnum.QM && qualsOnly) continue;

                for (String team : m.getAlliances().getBlue().getTeamKeys())
                    if (key.equalsIgnoreCase("opr"))
                        scores[teamKeyPositionMap.get(team)][0] += m.getAlliances().getBlue().getScore();
                    else
                        scores[teamKeyPositionMap.get(team)][0] += Double.parseDouble(m.getScoreBreakdown().getBlue().get(key));
                for (String team : m.getAlliances().getRed().getTeamKeys())
                    if (key.equalsIgnoreCase("opr"))
                        scores[teamKeyPositionMap.get(team)][0] += m.getAlliances().getRed().getScore();
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

    public Map<Integer, Double> getForKeySorted(String key) {
        final Map<Integer, Double> resultMap = getForKey(key);

        TreeMap<Integer, Double> m = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer val1, Integer val2) {
                return (int) Math.round(1000 * (resultMap.get(val2) - resultMap.get(val1)));
            }
        });

        m.putAll(resultMap);

        return m;
    }

    public Map<Integer, Double> getForSupplier(StatProvider sp) {
        synchronized (this) {
            cleanup();

            Map<Integer, Double> returnedMap = new HashMap<>();

            for (Match m : eventMatches) {
                if (m.getCompLevel() != Match.CompLevelEnum.QM && qualsOnly) continue;

                for (String team : m.getAlliances().getBlue().getTeamKeys())
                    scores[teamKeyPositionMap.get(team)][0] += sp.get(m.getScoreBreakdown().getBlue());
                for (String team : m.getAlliances().getRed().getTeamKeys())
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

    public void reInit(boolean qualsOnly) throws ApiException {
        this.qualsOnly = qualsOnly;
        synchronized (this) {
            for (Integer t : teams) {
                List<Match> thisTeamMatches = new ArrayList<>();

                for (Match m : eventMatches) {
                    MatchSimpleAlliances a = m.getAlliances();

                    if (a.getBlue().getTeamKeys().contains("frc" + t))
                        thisTeamMatches.add(m);
                    else if (a.getRed().getTeamKeys().contains("frc" + t))
                        thisTeamMatches.add(m);
                }


                for (Match m : thisTeamMatches) {
                    if (m.getCompLevel() != Match.CompLevelEnum.QM && qualsOnly) continue;

                    MatchAlliance a = m.getAlliances().getBlue().getTeamKeys().contains("frc" + t) ?
                            m.getAlliances().getBlue() : m.getAlliances().getRed();
                    for (String allianceMember : a.getTeamKeys()) {
                        matrix[teamKeyPositionMap.get("frc" + t)][teamKeyPositionMap.get(allianceMember)] += 1;
                    }
                }
            }

            try {
                finalMatrix = MatrixUtils.createRealMatrix(matrix);
                cholesky = new CholeskyDecomposition(finalMatrix);
            } catch (NonPositiveDefiniteMatrixException e) {
                throw new NoMatchesException();
            }
        }
    }

    public Set<String> getStats() {
        return stats;
    }

    public interface StatProvider {
        double get(Map<String, String> scoreBreakdown);
    }
}
