package iscteiul.ista;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>PropertySwapAdvisorTest</h2>
 *
 * <p>This class tests the {@link PropertySwapAdvisor#suggestSwaps} method,
 * verifying area thresholds and sorting, plus ensuring that distances to
 * Funchal and Machico are valid (non-NaN). We create fake references for
 * #11074 (Funchal) and #11517 (Machico) in the {@code App} class so that
 * distance-based logic does not skip all pairs.</p>
 */
public class PropertySwapAdvisorTest {

    /** Holds all property records, including p1..p4 and the references. */
    private static List<PropertyRecord> sampleRecords;

    private static PropertyRecord p1, p2, p3, p4;       // The four test properties
    private static PropertyRecord pFunchal, pMachico;   // Reference props #11074, #11517

    /** Our adjacency graph for each test. */
    private SimpleGraph<PropertyRecord, DefaultEdge> graph;

    /**
     * <p>Builds:
     * <ul>
     *   <li>p1, p2, p3, p4 → main test properties</li>
     *   <li>pFunchal (#11074), pMachico (#11517) → references for distanceToFunchal / distanceToMachico</li>
     * </ul>
     *
     * <p>Then calls {@code App.setPropertyRecords(...)} plus
     * {@code App.setFunchalPropertyRecord(...)} and {@code App.setMachicoPropertyRecord(...)}.</p>
     */
    @BeforeAll
    static void setUpBeforeAll() {
        sampleRecords = new ArrayList<>();

        // 1) Funchal reference (#11074)
        //    geometry => let's place it near (0,0) for convenience
        pFunchal = new PropertyRecord(
                11074,  // must match distanceToFunchal's expected ID
                999999L,
                999999L,
                0.0,
                0.0,
                "POLYGON((0 0,0 1,1 1,1 0,0 0))", // a 1x1 square near origin
                9999,
                "Funchal (Sé)",
                "Funchal",
                "Ilha da Madeira"
        );

        // 2) Machico reference (#11517)
        pMachico = new PropertyRecord(
                11517,  // must match distanceToMachico's expected ID
                888888L,
                888888L,
                0.0,
                0.0,
                "POLYGON((10 10,10 11,11 11,11 10,10 10))", // a 1x1 square near (10,10)
                8888,
                "Machico",
                "Machico",
                "Ilha da Madeira"
        );

        // 3) Our test properties (p1..p4)
        // p1 => area=100 (some geometry near (2,2))
        p1 = new PropertyRecord(
                1, 1111, 11,
                0.0, 100.0,
                "POLYGON((2 2,2 12,12 12,12 2,2 2))", // area=100 (10x10)
                1,
                null,
                null,
                null
        );

        // p2 => area=105 (geometry near (3,2))
        p2 = new PropertyRecord(
                2, 2222, 22,
                0.0, 105.0,
                "POLYGON((2 2,2 13,13 13,13 2,2 2))", // area=121? or adjust it slightly
                2,
                null,
                null,
                null
        );

        // p3 => area=50  (geometry near (25,25) => far from p1/p2 => ~?)
        p3 = new PropertyRecord(
                3, 3333, 33,
                0.0, 50.0,
                "POLYGON((25 25,25 30,30 30,30 25,25 25))",
                3,
                null,
                null,
                null
        );

        // p4 => area=300 (owner=2, same as p2)
        p4 = new PropertyRecord(
                4, 4444, 44,
                0.0, 300.0,
                "POLYGON((2 2,2 20,20 20,20 2,2 2))", // area=324?
                2,
                null,
                null,
                null
        );

        // Add them all to sampleRecords
        sampleRecords.add(pFunchal);
        sampleRecords.add(pMachico);
        sampleRecords.add(p1);
        sampleRecords.add(p2);
        sampleRecords.add(p3);
        sampleRecords.add(p4);

        // Assign them to App, so distance calls won't be NaN
        App.setPropertyRecords(sampleRecords);
        App.setFunchalPropertyRecord(pFunchal);   // #11074
        App.setMachicoPropertyRecord(pMachico);   // #11517
    }

    /**
     * Clears the sampleRecords after all tests. Optionally we can nullify
     * App fields if desired.
     */
    @AfterAll
    static void tearDownAfterAll() {
        sampleRecords.clear();
        // Optionally:
        // App.setPropertyRecords(null);
        // App.setFunchalPropertyRecord(null);
        // App.setMachicoPropertyRecord(null);
    }

    /**
     * Builds a fresh graph for each test, adding p1..p4 as vertices
     * and connecting:
     * <ul>
     *   <li>p1 ↔ p2 => different owners (1 vs 2), area difference ~5%</li>
     *   <li>p1 ↔ p3 => different owners (1 vs 3), bigger difference</li>
     *   <li>p2 ↔ p4 => same owner (2) => no swap expected</li>
     * </ul>
     */
    @BeforeEach
    void setUp() {
        graph = new SimpleGraph<>(DefaultEdge.class);

        // Add p1..p4 as vertices
        graph.addVertex(p1);
        graph.addVertex(p2);
        graph.addVertex(p3);
        graph.addVertex(p4);

        // Edges
        graph.addEdge(p1, p2); // different owners
        graph.addEdge(p1, p3); // different owners
        graph.addEdge(p2, p4); // same owner => won't produce a swap
    }

    /**
     * <p>With a 10% threshold, p1–p2 differ by ~5% area => we expect exactly one suggestion.</p>
     */
    @Test
    void testSuggestSwaps_ProducesExpectedSuggestion() {
        List<SwapSuggestion> suggestions =
                PropertySwapAdvisor.suggestSwaps(graph, 0.10, 10);

        assertEquals(1, suggestions.size(),
                "Exactly one swap (p1–p2) should qualify under 10% threshold");

        SwapSuggestion s = suggestions.get(0);
        boolean isPair =
                (s.getP1().equals(p1) && s.getP2().equals(p2))
                        || (s.getP1().equals(p2) && s.getP2().equals(p1));

        assertTrue(isPair,
                "Should be p1–p2 only");
        assertTrue(s.getScore() > 0.9,
                "Area difference ~5% => score ~0.95+");
    }

    /**
     * <p>Strict threshold (2%) means ~5% difference is too large.
     * So we expect zero suggestions.</p>
     */
    @Test
    void testSuggestSwaps_ThresholdTooLow_NoSuggestions() {
        List<SwapSuggestion> suggestions =
                PropertySwapAdvisor.suggestSwaps(graph, 0.02, 10);

        assertTrue(suggestions.isEmpty(),
                "No suggestions expected below a 2% threshold");
    }

    /**
     * <p>Looser threshold (60%) => p1–p2 and p1–p3 pass. But we set maxSuggestions=1
     * => only 1 result returned.</p>
     */
    @Test
    void testSuggestSwaps_MaxSuggestionsLimit() {
        List<SwapSuggestion> suggestions =
                PropertySwapAdvisor.suggestSwaps(graph, 0.60, 1);

        // We expect p1–p2 and p1–p3, but truncated to 1
        assertEquals(1, suggestions.size(),
                "Must be truncated to 1 suggestion");
    }

    /**
     * <p>Again with threshold=60%, both p1–p2 and p1–p3 appear.
     * p1–p2 is smaller difference => higher score => appears first.</p>
     */
    @Test
    void testSuggestSwaps_SortedDescendingByScore() {
        List<SwapSuggestion> suggestions =
                PropertySwapAdvisor.suggestSwaps(graph, 0.60, 10);

        assertEquals(2, suggestions.size(),
                "Should produce 2 suggestions under 60% threshold");

        SwapSuggestion first  = suggestions.get(0);
        SwapSuggestion second = suggestions.get(1);

        // Ensure descending
        assertTrue(first.getScore() >= second.getScore(),
                "Must be sorted descending by score");

        boolean topIsP1P2 =
                (first.getP1().equals(p1) && first.getP2().equals(p2))
                        || (first.getP1().equals(p2) && first.getP2().equals(p1));

        assertTrue(topIsP1P2,
                "Highest-score suggestion should be p1–p2 (smaller area diff)");
    }
}
