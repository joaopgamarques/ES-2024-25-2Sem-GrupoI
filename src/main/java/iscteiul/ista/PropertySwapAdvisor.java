package iscteiul.ista;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finds potential swaps among merged properties from different owners,
 * based solely on area similarity.
 */
public final class PropertySwapAdvisor {

    private PropertySwapAdvisor() {
        // No instantiation
    }

    /**
     * Scans each edge in the "merged property" graph, checking if they belong
     * to different owners. If so, we measure how similar their areas are.
     *
     * If the similarity is high (or difference is below some threshold),
     * we create a {@link SwapSuggestion}.
     *
     * @param mergedGraph The adjacency graph of merged properties
     * @param areaThreshold The max relative area difference we allow (e.g. 0.10 => 10%)
     * @param maxSuggestions The maximum number of suggestions to return
     * @return A sorted list (descending) of potential swaps (by "similarity" score).
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

            // Only consider swapping if different owners
            if (p1.getOwner() == p2.getOwner()) {
                continue;
            }

            // measure area difference ratio
            double a1 = Math.max(0.00001, p1.getShapeArea());
            double a2 = Math.max(0.00001, p2.getShapeArea());
            double ratio = Math.abs(a1 - a2) / Math.max(a1, a2);

            // if ratio <= areaThreshold => they are "similar enough"
            if (ratio <= areaThreshold) {
                // benefit could be the "sum of owners' new average area"
                // but for simplicity, let's define a simple "similarity" measure:
                double similarity = 1.0 - ratio; // in [0..1]

                // We can define cost = ratio, so score = similarity
                double cost = ratio;
                double benefit = similarity;
                double score = similarity; // or something more advanced

                SwapSuggestion suggestion = new SwapSuggestion(p1, p2, benefit, cost, score);
                suggestions.add(suggestion);
            }
        }

        // Sort descending by score
        suggestions.sort(Comparator.comparingDouble(SwapSuggestion::getScore).reversed());

        // Return up to maxSuggestions
        if (suggestions.size() > maxSuggestions) {
            return suggestions.subList(0, maxSuggestions);
        }
        return suggestions;
    }
}
