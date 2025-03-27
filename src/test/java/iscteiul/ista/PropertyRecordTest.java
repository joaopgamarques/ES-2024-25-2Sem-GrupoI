package iscteiul.ista;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the {@link PropertyRecord} class to ensure correct construction,
 * field retrieval via getters, and behavior of {@link PropertyRecord#toString()}.
 *
 * <p>These tests validate a handful of sample data values, covering
 * integer, long, double, and string fields, as well as verifying
 * that {@code null} string fields don't cause unexpected results.
 *
 * <p>Additional integration testing of {@code PropertyRecord} occurs
 * indirectly in other test classes like {@code CSVFileReaderTest}.
 */
class PropertyRecordTest {

    /**
     * Verifies the {@link PropertyRecord} constructor assigns all fields
     * correctly and that the getter methods return the expected values.
     */
    @Test
    void testConstructorAndGetters() {
        PropertyRecord record = new PropertyRecord(
                1,
                7343148L,
                2996240000000L,
                57.2469341921808,
                202.05981432070362,
                "MULTIPOLYGON(...)",
                93,
                "Arco da Calheta",
                "Calheta",
                "Ilha da Madeira (Madeira)"
        );

        assertEquals(1, record.getObjectID(), "objectID should match constructor input.");
        assertEquals(7343148L, record.getParcelID(), "parcelID should match constructor input.");
        assertEquals(2996240000000L, record.getParcelNumber(), "parcelNumber mismatch.");
        assertEquals(57.2469341921808, record.getShapeLength(), 1e-9, "shapeLength mismatch.");
        assertEquals(202.05981432070362, record.getShapeArea(), 1e-9, "shapeArea mismatch.");
        assertEquals("MULTIPOLYGON(...)", record.getGeometry(), "geometry mismatch.");
        assertEquals(93, record.getOwner(), "owner mismatch.");
        assertEquals("Arco da Calheta", record.getParish(), "parish mismatch.");
        assertEquals("Calheta", record.getMunicipality(), "municipality mismatch.");
        assertEquals("Ilha da Madeira (Madeira)", record.getIsland(), "island mismatch.");
    }

    /**
     * Verifies that {@link PropertyRecord#toString()} includes expected
     * property details in its string representation, such as the {@code objectID}.
     */
    @Test
    void testToString() {
        PropertyRecord record = new PropertyRecord(
                2,
                12345L,
                67890L,
                10.0,
                20.0,
                "POLYGON((0 0,1 0,1 1,0 1,0 0))",
                999,
                null,   // parish
                "NA",   // municipality
                "NA"    // island
        );
        String toStringResult = record.toString();

        assertTrue(toStringResult.contains("objectID=2"), "Should include objectID in toString output.");
        assertTrue(toStringResult.contains("parcelID=12345"), "Should include parcelID in toString output.");
        assertTrue(toStringResult.contains("owner=999"), "Should include owner in toString output.");
        // You can add more assertions as needed to confirm the presence of various fields.
    }
}
