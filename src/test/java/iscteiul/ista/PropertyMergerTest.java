package iscteiul.ista;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h2>PropertyMergerTest</h2>
 *
 * <p>Unit-tests for {@link PropertyMerger#mergeSameOwner(List)}.</p>
 *
 * <p>The scenarios covered are:</p>
 * <ol>
 *   <li><strong>No merge needed</strong> – a single property for one owner yields an identical list.</li>
 *   <li><strong>Two touching polygons</strong> owned by the same owner are merged into one polygon whose
 *       area is the algebraic sum of the originals.</li>
 *   <li><strong>Mixed owners</strong> – adjacent polygons with <em>different</em> owners must <em>not</em> be merged.</li>
 *   <li><strong>Null / empty input</strong> – defensive behaviour for edge cases.</li>
 * </ol>
 *
 * <p>All tests use very small, hard-coded geometries so that areas and adjacency
 * relationships are obvious “on sight”.  JTS handles the geometry maths for us; we merely
 * verify that {@code PropertyMerger} wires the right polygons together.</p>
 */
public class PropertyMergerTest {

    private PropertyRecord squareA;   // (0,0) – (1,1)
    private PropertyRecord squareB;   // (1,0) – (2,1)  – touches squareA at x=1
    private PropertyRecord squareC;   // (3,0) – (4,1)  – disjoint from A & B

    @BeforeEach
    void setUp() {
        squareA = new PropertyRecord(
                1, 1111, 11, 4.0, 1.0,
                "POLYGON((0 0,0 1,1 1,1 0,0 0))",
                100, "ParishX", "MunX", "IslandX");

        squareB = new PropertyRecord(
                2, 2222, 22, 4.0, 1.0,
                "POLYGON((1 0,1 1,2 1,2 0,1 0))",
                100, "ParishX", "MunX", "IslandX");

        squareC = new PropertyRecord(
                3, 3333, 33, 4.0, 1.0,
                "POLYGON((3 0,3 1,4 1,4 0,3 0))",
                200, "ParishX", "MunX", "IslandX");
    }

    /**
     * <p>Verifies that calling {@code mergeSameOwner} with a list containing
     * <strong>one</strong> property returns a list identical in size and content.</p>
     */
    @Test
    void testSingleProperty_NoMerge() {
        List<PropertyRecord> merged = PropertyMerger.mergeSameOwner(List.of(squareA));

        assertEquals(1, merged.size(), "Exactly one record expected");
        assertSame(squareA, merged.get(0),
                "The single record should be returned unchanged");
    }

    /**
     * <p>Ensures that <em>two adjacent polygons</em> belonging to the <em>same owner</em>
     * are unioned into a single polygon whose area equals the sum of the originals
     * (1 + 1 = 2 m² in this toy example).</p>
     */
    @Test
    void testAdjacentSameOwner_AreMerged() {
        List<PropertyRecord> merged =
                PropertyMerger.mergeSameOwner(List.of(squareA, squareB));

        assertEquals(1, merged.size(),
                "Adjacent polygons from the same owner should collapse into one record");

        PropertyRecord result = merged.get(0);
        assertEquals(2.0, result.getShapeArea(), 1e-9,
                "Merged area must equal sum of component areas (1+1)");
        assertEquals(100, result.getOwner(),
                "Owner id should remain unchanged");
    }

    /**
     * <p>Checks that <em>different owners</em> are <strong>never</strong> merged even
     * if their polygons touch.  Here we supply one pair that can merge (A+B) and
     * another polygon with a different owner; the output should therefore contain
     * two records: one merged (owner 100) and one untouched (owner 200).</p>
     */
    @Test
    void testMixedOwners_MergeOnlyPerOwner() {
        List<PropertyRecord> merged =
                PropertyMerger.mergeSameOwner(List.of(squareA, squareB, squareC));

        // Expect:  (A+B) merged => one record, plus C untouched => total 2
        assertEquals(2, merged.size(), "Should return two records total");

        long countOwner100 = merged.stream()
                .filter(r -> r.getOwner() == 100)
                .count();
        long countOwner200 = merged.stream()
                .filter(r -> r.getOwner() == 200)
                .count();

        assertEquals(1, countOwner100, "Owner 100 should have one merged parcel");
        assertEquals(1, countOwner200, "Owner 200 should keep its original parcel");
    }

    /**
     * <p>Confirms that null or empty collections are handled gracefully –
     * no exception and an empty list result.</p>
     */
    @Test
    void testNullAndEmptyInput_EdgeCases() {
        assertTrue(PropertyMerger.mergeSameOwner(null).isEmpty(),
                "Null input should yield an empty list");

        assertTrue(PropertyMerger.mergeSameOwner(List.of()).isEmpty(),
                "Empty input list should yield an empty list");
    }
}
