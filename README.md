# ES-Project-TerritoryManagement

A software application to **manage and optimize property data** in Portugal, featuring:

- **Graph-Based Adjacency Checks**
- **Owner-Adjacency Graph** (owners connected if they have adjacent parcels)
- **Property Merging** (partially implemented)
- **Swap Suggestions** (partially implemented — uses area + distance to Funchal + distance to Machico)
- **Average Area Calculations**
- **Exports to Gephi** for advanced visualization
- **PostGIS Integration** for advanced spatial queries

---

## Overview

This project focuses on analyzing and improving **territorial management** by loading, processing, and visualizing property records. You can **import CSV** data (e.g., `Madeira-Moodle-1.2.csv`) into various graph structures to detect adjacency, explore ownership patterns, and ultimately propose land swaps or merges to optimize property usage.

---

## Key Classes

1. **`CSVFileReader` / `CSVFileWriter`**
    - **`CSVFileReader`**: Imports a semicolon-delimited CSV into a list of `PropertyRecord` objects.
    - **`CSVFileWriter`**: Exports `PropertyRecord` objects back to CSV.

2. **`PropertyRecord`**
    - Immutable model class holding property attributes (objectID, parcelID, geometry, owner, parish, etc.).

3. **`PropertyUtils`**
    - Static methods for filtering, adjacency checks, grouping, merges, and other property-related utilities (e.g., computing average areas, distance to Funchal/Machico).

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

8. **`ExportToGephiUtils`**
    - Demonstrates creating a **GraphStream** graph from a filtered property list (e.g., by parish).
    - Exports the graph to `.gexf` for use in **Gephi**, enabling advanced layout and analytics.

9. **`Graph`**
    - A simpler adjacency-list approach, linking `PropertyRecord` nodes if they share a boundary.
    - Currently uses an O(N²) check; `PropertyGraph` is recommended for larger datasets.

10. **`PostGISUtils`**
    - Provides **PostGIS** functionality to insert, query, and manipulate parcel geometry in a PostgreSQL/PostGIS database:
        - **Bulk Insert** (`insertPropertyRecords`) for reading CSV-derived records into the DB.
        - **Spatial Relationship Queries** (`findTouching`, `findIntersecting`, `findOverlapping`, `findContained`).
        - **Union & Intersection** (`unionByOwner`, `intersection`) to merge or compute intersecting geometries.
        - **Distance & Proximity** (`distance`, `withinDistance`) for measuring how close parcels are.
        - **Area & Centroid** (`area`, `centroid`) to compute parcel surface area and the WKT centroid.

11. **`PropertyMerger`**
    - Merges properties **owned by the same owner** if they are spatially adjacent.
    - Returns a new list of “merged” records, each representing one connected component.

12. **`PropertySwapAdvisor`**
    - Suggests **potential swaps** among merged properties from **different** owners, taking into account:
        - **Area similarity** (80% weight)
        - **Distance to Funchal** (15% weight)
        - **Distance to Machico** (5% weight)
    - Produces a score for each swap, sorted descending.

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

## Planned / Partially Implemented Features

1. **Property Merging**
    - We now have a `PropertyMerger` class that unifies contiguous parcels (same owner + adjacent).
    - Not yet fully integrated into all workflows or the UI.
    - Future work: hooking it into the main application flows.

2. **Swap Suggestions**
    - A new `PropertySwapAdvisor` class identifies property pairs (with different owners) that have similar areas and comparable distances to major landmarks (Funchal / Machico).
    - Currently tested with an example adjacency graph.
    - Future improvements:
        - Possibly integrate more metrics (e.g., price, infra quality).
        - Provide a user interface for selecting thresholds, printing detailed results, etc.

---

## Known Issues & Incomplete Features

- **Graph Construction**
    - The basic `Graph` class uses O(N²) adjacency checks. For large datasets, a more efficient approach (like the STRtree in `PropertyGraph`) is recommended.

- **Integration with Main**
    - While `PropertyMerger` and `PropertySwapAdvisor` exist, they’re not yet deeply integrated into the main demonstration code or UI flows.
    - The merging/swap steps can be called manually or from inside `main`, but a polished interface is still pending.

- **Tests**
    - Some integration tests remain to be expanded, especially around combining property merges and swap suggestions in a single pipeline.
    - The “distance to Funchal / Machico” logic requires you to set up reference properties (#11074 / #11517) in `App`. This can lead to `NaN` in tests if not done.

### Usage & Testing

- In general, the `main(...)` method in **`App`** demonstrates CSV loading, building graphs, listing neighbors, merging, and a basic call to `PropertySwapAdvisor.suggestSwaps()`.
- For **database** operations, the `main(...)` in **`PostGISUtils`** (or a dedicated demo class) shows how to import data into PostGIS, run spatial queries, measure distances, union, and so forth.

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
