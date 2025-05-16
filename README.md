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

- **Swap Suggestions**
    - Current logic considers area plus distances to Funchal and Machico.
    - Future expansions could incorporate additional metrics (e.g., **price**, **population density**) to enrich the swap scoring mechanism.
    - Threshold-based approaches and scoring heuristics are partially integrated but not yet extensively tested.

---

## Known Issues & Incomplete Features

- **Tests**
    - We have robust unit tests for adjacency checks, property merges, and distance calculations.
    - However, integration tests involving both **merging** and **swap suggestions** in a single pipeline remain to be expanded.
    - Large-scale performance testing and stress tests on bigger datasets are also areas for future improvement.

### Usage & Testing

- In general, the `main(...)` method in **`App`** demonstrates CSV loading, building graphs, listing neighbors, merging, and a basic call to `PropertySwapAdvisor.suggestSwaps()`.
- For **database** operations, the `main(...)` in **`PostGISUtils`** (or a dedicated demo class) shows how to import data into PostGIS, run spatial queries, measure distances, union, and so forth.

---

## Build & Run Instructions

> **Repository:** <https://github.com/joaopgamarques/ES-2024-25-2Sem-GrupoI>

### 1. Clone & Build
```bash
git clone https://github.com/joaopgamarques/ES-2024-25-2Sem-GrupoI.git
cd ES-2024-25-2Sem-GrupoI
mvn clean install
```

### 2. Run All Tests + Coverage & Static Analysis
```bash
mvn clean install site
```
After the build finishes you can open the following reports locally:

| Report                | Purpose                  | Path                            |
| :-------------------- | :----------------------- | :------------------------------ |
| **JaCoCo (Coverage)** | Unit-test coverage       | `target/site/jacoco/index.html` |
| **PMD**               | Code-quality rules       | `target/reports/pmd.html`       |
| **SpotBugs**          | Bug-pattern detection    | `target/site/spotbugs.html`     |
| **CPD**               | Copy-and-paste detection | `target/reports/cpd.html`       |

#### Test Coverage
- According to our latest JaCoCo results:
  - Missed Instructions: 38% (which implies 62% coverage of instructions)
  - Missed Branches: 46% (which implies 54% branch coverage)
  - We plan to improve these metrics by adding more integration and UI tests in future sprint

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
