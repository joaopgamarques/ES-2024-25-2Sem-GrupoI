package iscteiul.ista;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <h2>PropertySwapAdvisor</h2>
 *
 * <p>This utility class provides methods to generate potential swap suggestions
 * among merged properties belonging to different owners. It evaluates:
 * <ul>
 *   <li><strong>Area similarity</strong> – If two properties have similar areas.</li>
 *   <li><strong>Distance to Funchal</strong> – Compares how close each property is to Funchal (Sé).</li>
 *   <li><strong>Distance to Machico</strong> – Compares how close each property is to Machico.</li>
 * </ul>
 *
 * <p>For each pair of adjacent properties (as defined by the <em>merged property graph</em>),
 * if they have different owners and their <em>area ratio</em> is below a given threshold,
 * the advisor computes an overall <em>score</em> that combines these three metrics with
 * different weights:
 *
 * <ul>
 *   <li>Area similarity: 80%</li>
 *   <li>Funchal distance similarity: 15%</li>
 *   <li>Machico distance similarity: 5%</li>
 * </ul>
 *
 * <p>A higher score indicates a more promising swap, balancing the interest in
 * minimal differences in area and in distances to major landmarks.
 *
 * <p>This class is <strong>final</strong> and has a private constructor to prevent instantiation,
 * as it functions purely as a static utility.
 */
public final class PropertySwapAdvisor {

    /** Private constructor to prevent instantiation. */
    private PropertySwapAdvisor() {
        // No instantiation
    }

    /**
     * <p><strong>Suggests swaps</strong> between merged properties in the given graph.
     * Each edge in {@code mergedGraph} represents two properties that are spatially adjacent
     * (touching). We only consider a swap if the two properties belong to <em>different owners</em>
     * and if their areas differ by at most {@code areaThreshold} in relative terms.</p>
     *
     * <p>The method then calculates three similarity measures:
     * <ol>
     *   <li>Area Similarity, in [0..1], defined as {@code 1 - areaRatio}.</li>
     *   <li>Funchal Distance Similarity, comparing how each property differs in
     *       distance to Funchal. If the geometry or reference is missing,
     *       the swap is skipped.</li>
     *   <li>Machico Distance Similarity, analogous to the Funchal approach.</li>
     * </ol>
     *
     * <p>Finally, an overall <em>score</em> is computed as a weighted combination:</p>
     * <blockquote>
     * {@code score = 0.8 * areaSimilarity + 0.15 * funchalSimilarity + 0.05 * machicoSimilarity;}
     * </blockquote>
     *
     * <p>Suggestions are returned in a list, sorted by descending score. If there are
     * more than {@code maxSuggestions} possible swaps, only the top {@code maxSuggestions}
     * are included.</p>
     *
     * @param mergedGraph    the adjacency graph (where each vertex is a merged property)
     * @param areaThreshold  the maximum allowable relative area difference (e.g., 0.1 = 10%)
     * @param maxSuggestions the maximum number of suggestions to return
     * @return a list of {@link SwapSuggestion} objects, sorted descending by score
     *
     * @see SwapSuggestion
     * @see PropertyUtils#distanceToFunchal(int)
     * @see PropertyUtils#distanceToMachico(int)
     */
    public static List<SwapSuggestion> suggestSwaps(
            SimpleGraph<PropertyRecord, DefaultEdge> mergedGraph,
            double areaThreshold,
            int maxSuggestions
    ) {
        List<SwapSuggestion> suggestions = new ArrayList<>();

        for (DefaultEdge edge : mergedGraph.edgeSet()) {
            PropertyRecord p1 = mergedGraph.getEdgeSource(edge);
            PropertyRecord p2 = mergedGraph.getEdgeTarget(edge);

            // 1) Check owners
            if (p1.getOwner() == p2.getOwner()) {
                continue; // same owner => no swap
            }

            // 2) Compute area similarity
            double areaSim = computeAreaSimilarity(p1, p2, areaThreshold);
            if (Double.isNaN(areaSim)) {
                continue; // skip this pair
            }

            // 3) Compute funchal similarity
            double funchalSim = computeDistanceSimilarityFunchal(p1, p2);
            if (Double.isNaN(funchalSim)) {
                continue; // skip if invalid geometry or distance
            }

            // 4) Compute machico similarity
            double machicoSim = computeDistanceSimilarityMachico(p1, p2);
            if (Double.isNaN(machicoSim)) {
                continue; // skip if invalid geometry or distance
            }

            // 5) Combine into a final score
            double score = combineScore(areaSim, funchalSim, machicoSim);

            // We can define cost/benefit in any way; for demonstration:
            double cost = 1.0 - areaSim;  // e.g., using the difference from areaSim
            double benefit = score;

            // Build the suggestion
            SwapSuggestion suggestion = new SwapSuggestion(
                    p1, p2,
                    benefit,
                    cost,
                    score
            );
            suggestions.add(suggestion);
        }

        // Sort by score descending
        suggestions.sort(Comparator.comparingDouble(SwapSuggestion::getScore).reversed());

        // Limit the size to maxSuggestions
        if (suggestions.size() > maxSuggestions) {
            return suggestions.subList(0, maxSuggestions);
        }
        return suggestions;
    }

    /**
     * Computes the area similarity in [0..1], or returns {@code Double.NaN}
     * if the ratio is above the threshold.
     *
     * @param p1             first property
     * @param p2             second property
     * @param areaThreshold  maximum allowable relative difference in area
     * @return a similarity value in [0..1], or NaN if above threshold
     */
    private static double computeAreaSimilarity(PropertyRecord p1, PropertyRecord p2, double areaThreshold) {
        double a1 = Math.max(0.00001, p1.getShapeArea());
        double a2 = Math.max(0.00001, p2.getShapeArea());
        double ratio = Math.abs(a1 - a2) / Math.max(a1, a2);

        if (ratio > areaThreshold) {
            // Return NaN so we know to skip
            return Double.NaN;
        }
        return 1.0 - ratio; // area similarity
    }

    /**
     * Computes the distance similarity for how close p1 and p2 are to Funchal.
     * Returns a value in [0..1], or NaN if geometry is invalid or distances are missing.
     *
     * @param p1 first property
     * @param p2 second property
     * @return distance similarity in [0..1], or NaN if unavailable
     */
    private static double computeDistanceSimilarityFunchal(PropertyRecord p1, PropertyRecord p2) {
        double dist1 = PropertyUtils.distanceToFunchal(p1.getObjectID());
        double dist2 = PropertyUtils.distanceToFunchal(p2.getObjectID());

        if (Double.isNaN(dist1) || Double.isNaN(dist2) || dist1 <= 0.0 || dist2 <= 0.0) {
            return Double.NaN;
        }

        double ratio = Math.abs(dist1 - dist2) / Math.max(dist1, dist2);
        return 1.0 - ratio;
    }

    /**
     * Computes the distance similarity for how close p1 and p2 are to Machico.
     * Returns a value in [0..1], or NaN if geometry is invalid or distances are missing.
     *
     * @param p1 first property
     * @param p2 second property
     * @return distance similarity in [0..1], or NaN if unavailable
     */
    private static double computeDistanceSimilarityMachico(PropertyRecord p1, PropertyRecord p2) {
        double dist1 = PropertyUtils.distanceToMachico(p1.getObjectID());
        double dist2 = PropertyUtils.distanceToMachico(p2.getObjectID());

        if (Double.isNaN(dist1) || Double.isNaN(dist2) || dist1 <= 0.0 || dist2 <= 0.0) {
            return Double.NaN;
        }

        double ratio = Math.abs(dist1 - dist2) / Math.max(dist1, dist2);
        return 1.0 - ratio;
    }

    /**
     * Computes a final weighted score from the three similarities:
     * <blockquote>
     * {@code score = 0.8 * areaSim + 0.15 * funchalSim + 0.05 * machicoSim;}
     * </blockquote>
     *
     * @param areaSim     similarity of area in [0..1]
     * @param funchalSim  similarity of distance to Funchal in [0..1]
     * @param machicoSim  similarity of distance to Machico in [0..1]
     * @return an overall score, higher is better
     */
    private static double combineScore(double areaSim, double funchalSim, double machicoSim) {
        return 0.8 * areaSim + 0.15 * funchalSim + 0.05 * machicoSim;
    }
}
