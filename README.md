# ES-Project-TerritoryManagement

A software application to **manage and optimize property data** in Portugal, featuring:

- **Graph-Based Adjacency Checks**
- **Owner-Adjacency Graph** (owners connected if they have adjacent parcels)
- **Property Merging** (planned)
- **Average Area Calculations**
- **Exports to Gephi** for advanced visualization
- **PostGIS Integration** for advanced spatial queries

---

## Overview

This project focuses on analyzing and improving **territorial management** by loading, processing, and visualizing property records. You can **import CSV** data (e.g., `Madeira-Moodle-1.1.csv`) into various graph structures to detect adjacency, explore ownership patterns, and ultimately propose land swaps or merges to optimize property usage.

---

## Key Classes

1. **`CSVFileReader` / `CSVFileWriter`**
    - **`CSVFileReader`**: Imports a semicolon-delimited CSV into a list of `PropertyRecord` objects.
    - **`CSVFileWriter`**: Exports `PropertyRecord` objects back to CSV.

2. **`PropertyRecord`**
    - Immutable model class holding property attributes (objectID, parcelID, geometry, owner, parish, etc.).

3. **`PropertyUtils`**
    - Static methods for filtering, adjacency checks, grouping, and other property-related utilities (e.g. computing average areas).

4. **`GeometryUtils`**
    - Handles low-level geometry operations (parsing WKT, creating envelopes, checking adjacency) via **LocationTech JTS**.

5. **`PropertyGraph`**
    - A **JGraphT**-based graph where each property is a **vertex**, and **edges** represent adjacency.
    - Uses an **R-tree** (`STRtree`) to efficiently find candidate neighbors in large datasets.

6. **`OwnerGraph`**
    - Another **JGraphT**-based graph, but each node represents an **owner**.
    - Creates edges between owners who share **at least one pair** of adjacent properties.

7. **`GraphVisualization`**
    - Builds an **interactive GraphStream** visualization from a `PropertyGraph`.
    - Displays parcels (nodes) and their adjacency (edges), applying simple color rules for connected vs. isolated parcels.

8. **`ExportToGephiDemo`**
    - Demonstrates creating a **GraphStream** graph from a filtered property list (e.g., by parish).
    - Exports the graph to `.gexf` for use in **Gephi**, enabling advanced layout and analytics.

9. **`Graph`**
    - A simpler adjacency-list approach, linking `PropertyRecord` nodes if they share a boundary.
    - Currently uses an O(N²) check; future improvements may include spatial indexing.

10. **`PostGISReader`**
- Provides **PostGIS** functionality to insert, query, and manipulate parcel geometry in a PostgreSQL/PostGIS database:
    - **Bulk Insert** (`insertPropertyRecords`) for reading CSV-derived records into the DB.
    - **Spatial Relationship Queries** (`findTouching`, `findIntersecting`, `findOverlapping`, `findContained`).
    - **Union & Intersection** (`unionByOwner`, `intersection`) to merge or compute intersecting geometries.
    - **Distance & Proximity** (`distance`, `withinDistance`) for measuring how close parcels are.
    - **Area & Centroid** (`area`, `centroid`) to compute parcel surface area and the WKT centroid.

---

## Installation & Dependencies

All dependencies are managed via **Maven**. You’ll find them in the [pom.xml](pom.xml). Major libraries include:

- **OpenCSV** for CSV parsing.
- **LocationTech JTS** for geometry operations (e.g., adjacency, intersection checks).
- **SLF4J + Logback** for logging.
- **JUnit 5** for unit and integration testing.
- **JGraphT** for constructing and storing property graphs.
- **GraphStream** for visualizing and exporting `.gexf`.
- **PostgreSQL JDBC** for connecting to a PostGIS-enabled database.

---

## Planned Features

1. **Property Merging**  
   Combine contiguous parcels owned by the same owner into a larger, single record.

2. **Swap Suggestions**  
   Identify potential property exchanges between owners to maximize the average property area per owner.

---

## Known Issues & Incomplete Features

- **Graph Construction**  
  The `Graph` class uses O(N²) adjacency checks. For large datasets, a more efficient approach (e.g., an R-tree) would be needed. The `PropertyGraph` does support STRtree, but might still require performance tuning for very large data.

- **Property Merging**  
  Not fully integrated yet; geometry-based merging logic remains a placeholder.

- **Swap Suggestions**  
  Algorithmic design is still in progress; not implemented yet. Will consider property areas plus 2+ characteristics.

- **Tests**  
  Some integration tests (involving both geometry checks and adjacency) remain to be expanded.  
  Additional unit tests for edge cases are on the roadmap.

### Usage & Testing

- The `main(...)` method in **`PostGISReader`** offers an interactive console demo for:
    - Loading CSV data once to populate your PostGIS table.
    - Querying neighbors, checking overlaps, distances, areas, centroids, etc.
    - Demonstrates end-to-end DB integration with minimal code changes.

---

## Group Identification

**Group Name:** Group I

**Members:**
- **João Pedro Marques**
    - Student Number: 105377
    - GitHub Username: joaopgamarques
- **José Mesquita**
    - Student Number: 106281
    - GitHub Username: jlgmaIscte
- **Bárbara Albuquerque**
    - Student Number: 106807
    - GitHub Username: bfaae
- **Jéssica Vieira**
    - Student Number: 110812
    - GitHub Username: Je-ssi-ca

---

## Authors

Created and maintained by **ES-Project-TerritoryManagement Group I**.  
Contributions are welcome via pull requests and issues.
