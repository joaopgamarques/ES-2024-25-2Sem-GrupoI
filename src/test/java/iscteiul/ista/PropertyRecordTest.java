package iscteiul.ista;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test class for PropertyRecord.
 */
class PropertyRecordTest {

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

        assertEquals(1, record.getObjectID());
        assertEquals(7343148L, record.getParcelID());
        assertEquals(2996240000000L, record.getParcelNumber());
        assertEquals(57.2469341921808, record.getShapeLength(), 1e-9);
        assertEquals(202.05981432070362, record.getShapeArea(), 1e-9);
        assertEquals("MULTIPOLYGON(...)", record.getGeometry());
        assertEquals(93, record.getOwner());
        assertEquals("Arco da Calheta", record.getParish());
        assertEquals("Calheta", record.getMunicipality());
        assertEquals("Ilha da Madeira (Madeira)", record.getIsland());
    }

    @Test
    void testToString() {
        PropertyRecord record = new PropertyRecord(
                2, 12345L, 67890L, 10.0, 20.0, "POLYGON((0 0,1 0,1 1,0 1,0 0))",
                999, null, "NA", "NA"
        );
        String toStringResult = record.toString();
        assertTrue(toStringResult.contains("objectID=2"));
        assertTrue(toStringResult.contains("parcelID=12345"));
        assertTrue(toStringResult.contains("owner=999"));
        // etc. Checking that the toString includes some expected parts
    }
}
