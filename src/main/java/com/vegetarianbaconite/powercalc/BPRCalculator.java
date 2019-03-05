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

/**
 * A tool to calculate BPR, or Breakdown Power Rating, for a team at an event with a given stat.
 *
 * @author Dominic Canora
 * @version 1.0
 */
@SuppressWarnings({"RedundantCollectionOperation", "unused"})
public class BPRCalculator {
    private Boolean qualsOnly;

    public ApiClient apiClient;
    private List<Match> eventMatches;

    private TreeSet<Integer> teams = new TreeSet<>();
    private Map<String, Integer> teamKeyPositionMap = new HashMap<>();
    private double[][] matrix, scores;
    private CholeskyDecomposition cholesky;

    private Set<String> stats;

    /**
     * Default constructor, uses the default settings of only looking at only qualification matches, and not logging.
     *
     * @param apiKey   A Blue Alliance API V3 key.
     * @param eventKey The TBA Event Key of the event to scan, in the form [4 digit year][event code].
     * @throws ApiException ApiException
     */
    public BPRCalculator(String apiKey, String eventKey) throws ApiException {
        this(apiKey, eventKey, true, false);
    }

    /**
     * Get a new <code>BPRCalculator</code>, specifying the API key, event, and whether to look only at qualification matches.
     *
     * @param apiKey    A Blue Alliance API V3 key.
     * @param eventKey  The TBA Event Key of the event to scan, in the form [4 digit year][event code].
     * @param qualsOnly Whether to only look at qualification matches for data.
     * @throws ApiException ApiException
     */
    public BPRCalculator(String apiKey, String eventKey, Boolean qualsOnly) throws ApiException {
        this(apiKey, eventKey, qualsOnly, false);
    }

    /**
     * Get a new <code>BPRCalculator</code>, specifying the API key, event, whether to look only at qualification matches, and the logging mode.
     *
     * @param apiKey    A Blue Alliance API V3 key.
     * @param eventKey  The TBA Event Key of the event to scan, in the form [4 digit year][event code].
     * @param qualsOnly Whether to only look at qualification matches for data.
     * @param log       Whether to log some data to the console.
     * @throws ApiException ApiException
     */
    public BPRCalculator(String apiKey, String eventKey, Boolean qualsOnly, Boolean log) throws ApiException {
        apiClient = new ApiClient();
        apiClient.setApiKey(apiKey);
        EventApi eventApi = new EventApi(apiClient);

        int year = Integer.parseInt(eventKey.substring(0, 3));

        eventMatches = eventApi.getEventMatches(eventKey, null);

        for (Match m : eventMatches) {
            for (String t : m.getAlliances().getRed().getTeamKeys())
                try {
                    if (!teams.contains(Integer.parseInt(t.substring(3))))
                        teams.add(Integer.parseInt(t.substring(3)));
                } catch (NumberFormatException e) {
                    log("Ignoring non-integer team");
                }

            for (String t : m.getAlliances().getBlue().getTeamKeys())
                try {
                    if (!teams.contains(Integer.parseInt(t.substring(3))))
                        teams.add(Integer.parseInt(t.substring(3)));
                } catch (NumberFormatException e) {
                    log("Ignoring non-integer team");
                }
        }

        int i = 0;
        for (Integer t : teams) {
            teamKeyPositionMap.put("frc" + t, i);
            i++;
        }

        matrix = new double[teams.size()][teams.size()];
        scores = new double[teams.size()][1];

        String eventKey1 = eventKey;
        cleanup();
        reInit(qualsOnly);

        //TODO: Set stats to the set of stat keys in the appropriate year

        log("Initialized BPRCalculator");
    }

    /**
     * Get a BPR for the specified stat.
     *
     * @param key The key (from <code>getStats()</code>) to use for the BPR, or 'opr', 'dpr', or 'ccwm'
     * @return A map in the form &lt;Team Number, BPR&gt;
     */
    public Map<Integer, Double> getForKey(String key) {
        synchronized (this) {
            if (key.equalsIgnoreCase("dpr"))
                return getForSupplier(dprProvider);
            if (key.equalsIgnoreCase("ccwm"))
                return getForSupplier(ccwmProvider);
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

    /**
     * Get BPR for the specified stat
     *
     * @param key The key (from <code>getStats()</code>) to use for the BPR
     * @return A map in the form &lt;Team Number, BPR&gt;, sorted by BPR from highest to lowest
     */
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

    /**
     * Get BPR for the given custom metric provider
     *
     * @param sp A <code>MetricProvider</code> that returns a custom metric
     * @return A map in the form &lt;Team Number, BPR&gt;
     */
    public Map<Integer, Double> getForSupplier(MetricProvider sp) {
        synchronized (this) {
            cleanup();

            Map<Integer, Double> returnedMap = new HashMap<>();

            for (Match m : eventMatches) {
                if (qualsOnly && m.getCompLevel() != Match.CompLevelEnum.QM) continue;

                for (String team : m.getAlliances().getBlue().getTeamKeys())
                    scores[teamKeyPositionMap.get(team)][0] += sp.get(m.getScoreBreakdown().getBlue(), m.getScoreBreakdown().getRed());
                for (String team : m.getAlliances().getRed().getTeamKeys())
                    scores[teamKeyPositionMap.get(team)][0] += sp.get(m.getScoreBreakdown().getRed(), m.getScoreBreakdown().getBlue());
            }

            RealMatrix scoreMatrix = MatrixUtils.createRealMatrix(scores);
            double[][] output = cholesky.getSolver().solve(scoreMatrix).getData();

            for (Integer team : teams) {
                returnedMap.put(team, output[teamKeyPositionMap.get("frc" + team)][0]);
            }

            return returnedMap;
        }
    }

    /**
     * Get BPR for the given custom metric provider
     *
     * @param sp A MetricProvider that returns a custom metric
     * @return A map in the form &lt;Team Number, BPR&gt;, sorted by BPR from highest to lowest
     */
    public Map<Integer, Double> getForSupplierSorted(MetricProvider sp) {
        final Map<Integer, Double> resultMap = getForSupplier(sp);

        TreeMap<Integer, Double> m = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer val1, Integer val2) {
                return (int) Math.round(1000 * (resultMap.get(val2) - resultMap.get(val1)));
            }
        });

        m.putAll(resultMap);

        return m;
    }

    /**
     * Resets all scores
     */
    private void cleanup() {
        scores = new double[teams.size()][1];
    }

    private void reInit(boolean qualsOnly) {
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
                RealMatrix finalMatrix = MatrixUtils.createRealMatrix(matrix);
                cholesky = new CholeskyDecomposition(finalMatrix);
            } catch (NonPositiveDefiniteMatrixException e) {
                throw new NoMatchesException();
            }
        }
    }

    private void log(String s) {
        Boolean log = false;
        if (log)
            System.out.println(s);
    }

    /**
     * Get a set of stats present in the current event
     *
     * @return A set of stats given for the current event
     */
    public Set<String> getStats() {
        return stats;
    }

    /**
     * An interface to specify custom metrics for calculating BPRs
     */
    public interface MetricProvider {
        /**
         * Get a custom metric given the current opposing teams' score breakdowns.
         *
         * @param scoreBreakdown    The score breakdown of the current alliance
         * @param opposingBreakdown The score breakdown of the opposing alliance
         * @return A double representing the custom metric
         */
        double get(Map<String, String> scoreBreakdown, Map<String, String> opposingBreakdown);
    }

    private MetricProvider dprProvider = new MetricProvider() {
        @Override
        public double get(Map<String, String> scoreBreakdown, Map<String, String> opposingBreakdown) {
            try {
                return Integer.parseInt(opposingBreakdown.get("totalPoints"));
            } catch (NumberFormatException e) {
                return Integer.parseInt(opposingBreakdown.get("total_points"));
            }
        }
    };

    private MetricProvider ccwmProvider = new MetricProvider() {
        @Override
        public double get(Map<String, String> scoreBreakdown, Map<String, String> opposingBreakdown) {
            try {
                int opposing = Integer.parseInt(opposingBreakdown.get("totalPoints"));
                int current = Integer.parseInt(scoreBreakdown.get("totalPoints"));
                return current - opposing;
            } catch (NumberFormatException e) {
                int opposing = Integer.parseInt(opposingBreakdown.get("total_points"));
                int current = Integer.parseInt(scoreBreakdown.get("total_points"));
                return current - opposing;
            }
        }
    };
}
