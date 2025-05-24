# ES-Project-TerritoryManagement

A software application to **manage and optimize property data** in Portugal, featuring:

- **Graph-Based Adjacency Checks**
- **Owner-Adjacency Graph** (owners connected if they have adjacent parcels)
- **Property Merging** (same-owner parcels)
- **Swap Suggestions** (area + distance-based scoring)
- **Average Area Calculations** (ungrouped vs. merged by owner)
- **Exports to Gephi** for advanced visualization
- **PostGIS Integration** for spatial queries (optional)

---

## Table of Contents
- [Overview](#overview)
- [Project Objectives (Mapping Requirements)](#project-objectives-mapping-requirements)
- [Features & Classes](#features--classes)
- [Usage Flow](#usage-flow)
- [Running the UI](#running-the-ui)
- [Build & Test](#build--test)
- [Reports & Coverage](#reports--coverage)
- [Known Issues & Future Enhancements](#known-issues--future-enhancements)
- [Project Repository & Credits](#project-repository--credits)
- [Authors / Group Members](#authors--group-members)

---

## Overview
This project focuses on analyzing and improving **territorial management** by loading, processing, and visualizing property records from (potentially) large data sets. Once loaded, the data can be represented in **multiple graph structures** to:
1. Detect adjacency among properties (parcels).
2. Explore adjacency relationships among **owners**.
3. **Merge** contiguous parcels that belong to the same owner (increasing average area).
4. Propose **land swaps** between different owners to improve property contiguity, balancing similarity in **area** and other characteristics (like distance to major locations).

Some highlights:

- **Data Import/Export**: Reads from CSV, optionally writes updated data back to CSV or inserts it into a PostGIS database.
- **Visualization**: We provide both a Swing-based UI (`AppUI`) and integration with **GraphStream** for interactive or advanced `.gexf` exports (usable in Gephi).
- **Tests**: Includes a comprehensive set of JUnit 5 tests for each core component, as well as integration tests for database operations (if PostGIS is configured).

---

## Project Objectives (Mapping Requirements)

The official requirements for the project (from the statement) are:

1. **Load property data from a CSV**
    - *Our Solution*: `CSVFileReader` + `PropertyRecord`.
2. **Represent property adjacency** (a graph of parcels)
    - *Our Solution*: `PropertyGraph` (JGraphT + R-tree) or the simpler `Graph` (adjacency list, O(N²) checks).
3. **Represent owners in a graph** (owners who have adjacent parcels become neighbors)
    - *Our Solution*: `OwnerGraph`.
4. **Compute the average area of properties** in a specified region (parish/municipality)
    - *Our Solution*: `PropertyUtils.calculateAverageArea(...)`.
5. **Compute average area considering merged (contiguous) parcels per owner**
    - *Our Solution*: `PropertyMerger` (merges same-owner adjacent parcels) + `PropertyUtils.calculateAverageGroupedArea(...)`.
6. **Suggest swaps among owners** to maximize the (merged) average area
    - *Our Solution*: `PropertySwapAdvisor` ranks potential swaps by a composite metric that considers **area similarity**, **distance to Funchal**, and **distance to Machico**.
7. **Use at least area + two additional characteristics** for swap scoring
    - *Our Solution*: The two extra characteristics are the property’s centroid distance to **Funchal** and **Machico** references.

All features are functionally present and demonstrated in the code. Below is a fuller breakdown of how the code base maps to these objectives.

---

## Features & Classes

1. **`CSVFileReader` / `CSVFileWriter`**
    - **`CSVFileReader`** imports semicolon-delimited CSV files into a list of `PropertyRecord`.
    - **`CSVFileWriter`** exports a list of `PropertyRecord` back to CSV.
    - **Test**: `CSVFileReaderTest`, `CSVFileWriterTest`.

2. **`PropertyRecord`**
    - Immutable model object that holds all main attributes of each property (objectID, parcelID, geometry in WKT, owner, parish, municipality, etc.).
    - **Test**: `PropertyRecordTest`.

3. **`GeometryUtils`**
    - Wrappers around the LocationTech JTS library for checking adjacency (`touches`), intersection, and building bounding envelopes.
    - **Test**: `GeometryUtilsTest`.

4. **`PropertyGraph`**
    - Builds a JGraphT graph where each `PropertyRecord` is a vertex, and edges exist if two parcels are adjacent.
    - Uses a spatial index (`STRtree`) for efficient adjacency queries on large datasets.
    - **Test**: `PropertyGraphTest`.

5. **`OwnerGraph`**
    - Another JGraphT graph, but each **owner** is a vertex. Two owners have an edge if they each own at least one pair of adjacent parcels.
    - **Test**: `OwnerGraphTest`.

6. **`PropertyUtils`**
    - Contains static helper methods:
        - **Filtering**: by owner, municipality, island, etc.
        - **Adjacency**: `arePropertiesAdjacent(...)`
        - **Average Area**: `calculateAverageArea(...)`
        - **Distance**: `distanceToFunchal(...)` / `distanceToMachico(...)` if #11074/#11517 references are loaded in `App`.
        - **Grouping**: `calculateAverageGroupedArea(...)`
        - **Merge**: `mergeAdjacentPropertiesSameOwner(...)` (though the actual merging logic is mostly in `PropertyMerger`).
    - **Test**: `PropertyUtilsTest`.

7. **`PropertyMerger`**
    - Merges contiguous parcels for the same owner into one “big” polygon using JTS geometry union.
    - **Test**: `PropertyMergerTest`.

8. **`PropertySwapAdvisor`**
    - Identifies potential swaps between **different** owners (where parcels are adjacent) and calculates a **score** based on:
        - **Area similarity** (80% weight)
        - **Distance to Funchal** similarity (15%)
        - **Distance to Machico** similarity (5%)
    - Returns a sorted list of suggestions (`SwapSuggestion`) up to a `maxSuggestions` limit.
    - **Test**: `PropertySwapAdvisorTest`.

9. **`GraphVisualization`** and **`ExportToGephiUtils`**
    - For **interactive** or **advanced** visualization of property adjacency using **GraphStream** or Gephi (`.gexf`).
    - **Test**: `ExportToGephiUtilsTest`, `GraphVisualizationTest`.

10. **`PostGISUtils`**
- Optional. For those who want to store/query the parcels in **PostgreSQL** + **PostGIS**.
- Provides methods to **insert** CSV data into a `properties` table, run `ST_Touches`, `ST_Intersects`, etc., and measure distances, areas, or union them in the DB.
- **Test**: `PostGISUtilsTest` (integration tests requiring a running database).

11. **`App` and `AppUI`**
- `App` is a **main** entry point that demonstrates loading data, building graphs, computing adjacency, merging, etc.
- `AppUI` is a **Swing-based** GUI with a menu for showing properties, adjacency, computing average areas, generating swap suggestions, etc.
- **Tests**: `AppTest`, `AppUITest`.

---

## Usage Flow
A typical usage scenario might be:

1. **Load CSV** into memory:
   ```java
   List<PropertyRecord> records = new CSVFileReader().importData("/Madeira-Moodle-1.2.csv");
   
2. **Build a property graph** to identify adjacency:
    ```java
    PropertyGraph propGraph = new PropertyGraph();
    propGraph.buildGraph(records);

3. **Merge same-owner adjacent parcels** to reduce fragmentation:
    ```java
    List<PropertyRecord> mergedRecords = PropertyMerger.mergeSameOwner(records);

4. **Compute average area** (ungrouped or grouped):
    ```java
    double averageArea = PropertyUtils.calculateAverageArea(records);
    double averageGroupedArea = PropertyUtils.calculateAverageGroupedArea(records, propGraph.getGraph());

5. **Suggest property swaps** to improve contiguity:
    ```java
    SimpleGraph<PropertyRecord, DefaultEdge> mergedGraph = MergedPropertyGraph.buildGraph(mergedRecords);
    List<SwapSuggestion> suggestions = PropertySwapAdvisor.suggestSwaps(mergedGraph, 0.10, 10);

6. **Visualize** the results:
   - Use GraphVisualization.visualizeGraph(propGraph) (GraphStream).
   - Or ExportToGephiUtils.buildGraph(filteredList) + export .gexf.

7. **(Optional) PostGIS:** Insert the same records into a PostGIS DB via:
   - `PostGISUtils.insertPropertyRecords(records)`, then run SQL-based queries or use methods like `findTouching(int objectId)`.

---

## Running the UI

If you want to see the Swing interface (`AppUI`):

1. **Build the project:**

```bash
  mvn clean install
  mvn exec:java -Dexec.mainClass="iscteiul.ista.App"
```

2. **UI Features:**

    * Dropdowns for selecting a Parish or Municipality.
    * Use the menu options to explore adjacency, compute average areas, and generate swap suggestions:

        * **Show Properties** (Requirement #2)
        * **Properties Adjacency** (Requirement #2)
        * **Owners Adjacency** (Requirement #3)
        * **Average Area** (Requirement #4)
        * **Average Grouped Area** (Requirement #5)
        * **Suggest Property Swaps** (Requirement #6/7)

---

## Build & Test

**Clone the repository and run all tests & package:**

```bash
  git clone https://github.com/joaopgamarques/ES-2024-25-2Sem-GrupoI.git
  cd ES-2024-25-2Sem-GrupoI
  mvn clean install
```

**Generate site reports** (test coverage, PMD, SpotBugs, CPD):

```bash
  mvn clean install site
```

After completion, open the generated HTML reports in `target/site/` or `target/reports/`.

---

## Reports & Coverage

By default, the **JaCoCo** plugin is configured. After running `mvn site`, the following reports are generated:

* **Coverage Report:** `target/site/jacoco/index.html`
* **PMD:** `target/reports/pmd.html`
* **SpotBugs:** `target/site/spotbugs.html`
* **Copy/Paste Detection (CPD):** `target/reports/cpd.html`

**Current approximate coverage** (based on local runs):

* **Instruction Coverage:** \~60–70 %
* **Branch Coverage:** \~50–60 %

---

## Known Issues & Future Enhancements

* **UI:** The current Swing UI is fairly basic. Future iterations could include a map‑based visualization or richer user interactions.
* **Swap Logic:** Scoring currently weights 80 % area, 15 % distance to Funchal, and 5 % distance to Machico. These weights can be adjusted or extended with additional metrics such as property value, slope, or road distance.
* **Performance:** Building an O(N²) adjacency list can be slow for very large CSV files. Consider integrating `PropertyGraph` with an R‑tree index for better performance.
* **PostGIS Integration:** Database integration is optional and not yet fully automated. Providing scripts or Docker images would streamline setup.

---

## Project Repository & Credits

* **GitHub:** [joaopgamarques/ES-2024-25-2Sem-GrupoI](https://github.com/joaopgamarques/ES-2024-25-2Sem-GrupoI)

---

## Authors / Group Members

| Name                | Student # | GitHub                                               |
| ------------------- | --------- | ---------------------------------------------------- |
| João Pedro Marques  | 105377    | [@joaopgamarques](https://github.com/joaopgamarques) |
| José Mesquita       | 106281    | [@jlgmaIscte](https://github.com/jlgmaIscte)         |
| Bárbara Albuquerque | 106807    | [@bfaae](https://github.com/bfaae)                   |
| Jéssica Vieira      | 110812    | [@Je-ssi-ca](https://github.com/Je-ssi-ca)           |

Created and maintained by **ES-Project-TerritoryManagement Group I**. Contributions are welcome via pull requests and issues.

---
