package iscteiul.ista;

/**
 * Represents an immutable record of a property, including its geometry, area, ownership,
 * and location details (parish, municipality, island).
 * <p>
 * This class is typically instantiated from parsed CSV data. All fields are declared
 * {@code final}, ensuring that once a {@code PropertyRecord} is created, its values
 * cannot be changed.
 */
public class PropertyRecord {

    /** Unique ID of the property record. */
    private final int objectID;

    /** Identifier for the parcel. */
    private final long parcelID;

    /** A secondary parcel number or code. */
    private final long parcelNumber;

    /** The length of the property's shape. */
    private final double shapeLength;

    /** The area of the property. */
    private final double shapeArea;

    /** A geometry string (e.g., WKT) representing the property's shape. */
    private final String geometry;

    /** An integer representing the owner code or ID. */
    private final int owner;

    /** The parish (freguesia) name, or {@code null} if "NA" in the CSV. */
    private final String parish;

    /** The municipality name, or {@code null} if "NA" in the CSV. */
    private final String municipality;

    /** The island name, or {@code null} if "NA" in the CSV. */
    private final String island;

    /**
     * Constructs an immutable {@code PropertyRecord} with all required fields.
     *
     * @param objectID      the unique object identifier (column 0)
     * @param parcelID      the primary parcel ID, possibly parsed from scientific notation (column 1)
     * @param parcelNumber  a secondary parcel code or number (column 2)
     * @param shapeLength   the length of the property's shape (column 3)
     * @param shapeArea     the area of the property (column 4)
     * @param geometry      a geometry representation (e.g., WKT) for the property's boundaries (column 5)
     * @param owner         an integer representing the owner code or ID (column 6)
     * @param parish        the parish (freguesia) name, or {@code null} if "NA" (column 7)
     * @param municipality  the municipality name, or {@code null} if "NA" (column 8)
     * @param island        the island name, or {@code null} if "NA" (column 9)
     */
    public PropertyRecord(int objectID, long parcelID, long parcelNumber, double shapeLength, double shapeArea,
                          String geometry, int owner, String parish, String municipality, String island) {
        this.objectID = objectID;
        this.parcelID = parcelID;
        this.parcelNumber = parcelNumber;
        this.shapeLength = shapeLength;
        this.shapeArea = shapeArea;
        this.geometry = geometry;
        this.owner = owner;
        this.parish = parish;
        this.municipality = municipality;
        this.island = island;
    }

    /**
     * Returns the unique object ID for this property record.
     *
     * @return the object ID
     */
    public int getObjectID() {
        return objectID;
    }

    /**
     * Returns the primary parcel ID.
     *
     * @return the parcel ID
     */
    public long getParcelID() {
        return parcelID;
    }

    /**
     * Returns the secondary parcel number or code.
     *
     * @return the parcel number
     */
    public long getParcelNumber() {
        return parcelNumber;
    }

    /**
     * Returns the length of the property's shape.
     *
     * @return the shape length
     */
    public double getShapeLength() {
        return shapeLength;
    }

    /**
     * Returns the area of the property.
     *
     * @return the shape area
     */
    public double getShapeArea() {
        return shapeArea;
    }

    /**
     * Returns a geometry string (e.g., WKT) representing the property boundaries.
     *
     * @return the geometry string
     */
    public String getGeometry() {
        return geometry;
    }

    /**
     * Returns the owner code or ID for this property.
     *
     * @return the owner code
     */
    public int getOwner() {
        return owner;
    }

    /**
     * Returns the parish (freguesia) name, or {@code null} if it was "NA" in the CSV.
     *
     * @return the parish name, or null
     */
    public String getParish() {
        return parish;
    }

    /**
     * Returns the municipality name, or {@code null} if it was "NA" in the CSV.
     *
     * @return the municipality name, or null
     */
    public String getMunicipality() {
        return municipality;
    }

    /**
     * Returns the island name, or {@code null} if it was "NA" in the CSV.
     *
     * @return the island name, or null
     */
    public String getIsland() {
        return island;
    }

    @Override
    public String toString() {
        return "PropertyRecord{" + "objectID=" + objectID + ", parcelID=" + parcelID + ", parcelNumber=" + parcelNumber +
                ", shapeLength=" + shapeLength + ", shapeArea=" + shapeArea + ", geometry='" + geometry + '\'' +
                ", owner=" + owner + ", parish='" + parish + '\'' + ", municipality='" + municipality + '\'' +
                ", island='" + island + '\'' + '}';
    }
}
