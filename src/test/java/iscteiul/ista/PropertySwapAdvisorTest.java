package iscteiul.ista;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>PropertySwapAdvisorTest</h2>
 *
 * <p>Unit-tests for {@link PropertySwapAdvisor#suggestSwaps(SimpleGraph, double, int)}.</p>
 *
 * <p>The synthetic test-graph contains four <em>merged</em> properties:</p>
 * <table border="1">
 *   <tr><th>Label</th><th>objectID</th><th>Owner</th><th>Area&nbsp;(m²)</th><th>Edges</th></tr>
 *   <tr><td>P1</td><td>1</td><td>1</td><td>100</td><td>P2, P3</td></tr>
 *   <tr><td>P2</td><td>2</td><td>2</td><td>105</td><td>P1, P4</td></tr>
 *   <tr><td>P3</td><td>3</td><td>3</td><td>50</td><td>P1</td></tr>
 *   <tr><td>P4</td><td>4</td><td>2</td><td>300</td><td>P2</td></tr>
 * </table>
 *
 * <p>Hence:</p>
 * <ul>
 *   <li>P1–P2 are <strong>different owners</strong> with a small relative&nbsp;area-difference (~4.7 %)</li>
 *   <li>P1–P3 differ by 50 % area</li>
 *   <li>P2–P4 differ by ≈65 % area (same owner for P2 &amp; P4 is deliberately avoided by giving both owner 2; the code should therefore ignore this edge)</li>
 * </ul>
 *
 * <p>The tests validate:</p>
 * <ol>
 *   <li>Suggestions are produced only for <em>different-owner</em> pairs whose area-ratio falls below the chosen threshold.</li>
 *   <li>No suggestions when the threshold is set below the ratio.</li>
 *   <li>The {@code maxSuggestions} parameter truncates output as expected.</li>
 *   <li>Suggestions are sorted in descending order of score (i.e.&nbsp;highest similarity first).</li>
 * </ol>
 */
public class PropertySwapAdvisorTest {

    private SimpleGraph<PropertyRecord, DefaultEdge> graph;
    private PropertyRecord p1, p2, p3, p4;

    // --------------------------------------------------------------------- //
    //  Test-fixture construction                                             //
    // --------------------------------------------------------------------- //
    @BeforeEach
    void setUp() {
        graph = new SimpleGraph<>(DefaultEdge.class);

        p1 = new PropertyRecord(1, 1111, 11, 0, 100,
                "POLYGON EMPTY", 1, null, null, null);
        p2 = new PropertyRecord(2, 2222, 22, 0, 105,
                "POLYGON EMPTY", 2, null, null, null);
        p3 = new PropertyRecord(3, 3333, 33, 0, 50,
                "POLYGON EMPTY", 3, null, null, null);
        p4 = new PropertyRecord(4, 4444, 44, 0, 300,
                "POLYGON EMPTY", 2, null, null, null);

        // add vertices
        graph.addVertex(p1);
        graph.addVertex(p2);
        graph.addVertex(p3);
        graph.addVertex(p4);

        // add edges representing adjacency of merged polygons
        graph.addEdge(p1, p2);   // candidate (owners 1 vs 2)
        graph.addEdge(p1, p3);   // different owners but large area gap
        graph.addEdge(p2, p4);   // SAME owner (2) → must be ignored
    }

    // --------------------------------------------------------------------- //
    //  1. Normal case – one good swap (P1↔P2)                               //
    // --------------------------------------------------------------------- //
    /**
     * <p>With a threshold of <code>0.10</code> (10 %) only the P1–P2 edge
     * should be suggested (relative diff ≈4.7 %).</p>
     */
    @Test
    void testSuggestSwaps_ProducesExpectedSuggestion() {
        List<SwapSuggestion> suggestions =
                PropertySwapAdvisor.suggestSwaps(graph, 0.10, 10);

        assertEquals(1, suggestions.size(),
                "Exactly one swap (P1–P2) should qualify under 10 % threshold");

        SwapSuggestion s = suggestions.get(0);
        assertTrue(   (s.getP1().equals(p1) && s.getP2().equals(p2))
                        || (s.getP1().equals(p2) && s.getP2().equals(p1)),
                "Suggested pair must be P1 and P2");
        assertTrue(s.getScore() > 0.9,
                "Similarity score for ~4.7 % diff should exceed 0.9");
    }

    // --------------------------------------------------------------------- //
    //  2. Threshold too strict → zero suggestions                            //
    // --------------------------------------------------------------------- //
    /**
     * <p>With a very strict threshold (2 %) <em>no</em> swap should pass,
     * because P1–P2 diff ≈4.7 %.</p>
     */
    @Test
    void testSuggestSwaps_ThresholdTooLow_NoSuggestions() {
        List<SwapSuggestion> suggestions =
                PropertySwapAdvisor.suggestSwaps(graph, 0.02, 10);

        assertTrue(suggestions.isEmpty(),
                "No suggestions expected below a 2 % area-difference threshold");
    }

    // --------------------------------------------------------------------- //
    //  3. maxSuggestions parameter honoured                                  //
    // --------------------------------------------------------------------- //
    /**
     * <p>Here we loosen the threshold to 60 % so <em>both</em> P1–P2 and P1–P3
     * qualify.  Setting <code>maxSuggestions = 1</code> must truncate the list
     * to exactly one element.</p>
     */
    @Test
    void testSuggestSwaps_MaxSuggestionsLimit() {
        List<SwapSuggestion> suggestions =
                PropertySwapAdvisor.suggestSwaps(graph, 0.60, 1);

        assertEquals(1, suggestions.size(),
                "Output must be truncated to one suggestion");
    }

    // --------------------------------------------------------------------- //
    //  4. Sorting by score (highest first)                                   //
    // --------------------------------------------------------------------- //
    /**
     * <p>With a 60 % threshold we expect two suggestions where P1–P2
     * (4.7 % diff → score≈0.953) outranks P1–P3 (50 % diff → score 0.5).</p>
     */
    @Test
    void testSuggestSwaps_SortedDescendingByScore() {
        List<SwapSuggestion> suggestions =
                PropertySwapAdvisor.suggestSwaps(graph, 0.60, 10);

        assertEquals(2, suggestions.size(), "Should return two suggestions");

        SwapSuggestion first  = suggestions.get(0);
        SwapSuggestion second = suggestions.get(1);

        assertTrue(first.getScore() >= second.getScore(),
                "Suggestions must be sorted descending by score");
        // Explicitly check that first suggestion is the P1–P2 pair (highest similarity)
        assertTrue(   (first.getP1().equals(p1) && first.getP2().equals(p2))
                        || (first.getP1().equals(p2) && first.getP2().equals(p1)),
                "Highest-score suggestion should correspond to P1–P2");
    }
}
