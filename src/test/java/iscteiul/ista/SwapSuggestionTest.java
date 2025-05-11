package iscteiul.ista;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link SwapSuggestion}.
 */
public class SwapSuggestionTest {

    /**
     * A small tolerance for floating-point comparisons.
     */
    private static final double EPSILON = 1e-9;

    /**
     * Tests the creation of a SwapSuggestion object and the getters.
     */
    @Test
    public void testSwapSuggestionCreation() {
        // Arrange: create two sample PropertyRecord objects
        PropertyRecord p1 = new PropertyRecord(
                101,            // objectID
                1001L,          // parcelID
                2001L,          // parcelNumber
                15.5,           // shapeLength
                300.0,          // shapeArea
                "MULTIPOLYGON EMPTY", // geometry (placeholder)
                999,            // owner
                "SampleParishA",
                "SampleMunicipalityA",
                "SampleIslandA"
        );

        PropertyRecord p2 = new PropertyRecord(
                102,
                1002L,
                2002L,
                17.5,
                310.0,
                "MULTIPOLYGON EMPTY",
                888,
                "SampleParishB",
                "SampleMunicipalityB",
                "SampleIslandB"
        );

        // Example metrics for benefit, cost, and score
        double benefit = 0.8;
        double cost = 0.2;
        double score = 0.9;

        // Act: create a new SwapSuggestion
        SwapSuggestion suggestion = new SwapSuggestion(p1, p2, benefit, cost, score);

        // Assert: verify the fields via the getters
        assertSame(p1, suggestion.getP1(), "SwapSuggestion should hold the correct first property");
        assertSame(p2, suggestion.getP2(), "SwapSuggestion should hold the correct second property");
        assertEquals(benefit, suggestion.getBenefit(), EPSILON, "Benefit value mismatch");
        assertEquals(cost, suggestion.getCost(), EPSILON, "Cost value mismatch");
        assertEquals(score, suggestion.getScore(), EPSILON, "Score value mismatch");
    }

    /**
     * Tests the toString() method to ensure it produces a formatted output
     * containing the relevant property IDs, owner IDs, benefit, cost, and score.
     */
    @Test
    public void testToString() {
        // Arrange
        PropertyRecord p1 = new PropertyRecord(
                201,
                1101L,
                2101L,
                20.0,
                500.0,
                "MULTIPOLYGON EMPTY",
                111,
                "ParishX",
                "MunicipalityX",
                "IslandX"
        );
        PropertyRecord p2 = new PropertyRecord(
                202,
                1102L,
                2102L,
                25.0,
                550.0,
                "MULTIPOLYGON EMPTY",
                222,
                "ParishY",
                "MunicipalityY",
                "IslandY"
        );
        SwapSuggestion suggestion = new SwapSuggestion(p1, p2, 0.7, 0.3, 0.8);

        // Act
        String result = suggestion.toString();

        // Assert
        // We expect the string to contain certain key identifiers:
        assertTrue(result.contains("Prop 201 (owner=111)"), "toString() should include the first property info");
        assertTrue(result.contains("Prop 202 (owner=222)"), "toString() should include the second property info");
        assertTrue(result.contains("benefit=0.700"),       "toString() should include benefit");
        assertTrue(result.contains("cost=0.300"),          "toString() should include cost");
        assertTrue(result.contains("score=0.800"),         "toString() should include score");
    }
}
