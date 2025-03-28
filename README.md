# ES-Project-TerritoryManagement

A software application to manage and optimize property data in Portugal,  
with features such as graph-based adjacency checks, property merging,  
and average area calculations.

## Dependencies

- **OpenCSV** for CSV parsing (semicolon delimiter).
- **LocationTech JTS** for geometry (adjacency, intersection checks).
- **SLF4J** + **Logback** for logging.
- **JUnit 5** for unit testing.

All are managed via Maven. See the `pom.xml` for exact versions.

## Usage

### Loading Data
A `CSVFileReader` class reads `Madeira-Moodle-1.1.csv` into a list of `PropertyRecord` objects.

### Writing Data
A `CSVFileWriter` class can export these records back to a CSV, using semicolons.

### Graph Representation
*(Planned)* The records may be transformed into a graph for adjacency checks.

### Property Merging
*(Planned)* Merge contiguous properties belonging to the same owner.

### Swap Suggestions
*(Planned)* Suggest property swaps to maximize the average area per owner.

## Group Identification

- **Group Name**: Group I

**Members**:

1. **Name**: João Pedro Marques
   - Student Number: 105377
   - GitHub Username: joaopgamarques

2. **Name**: José Mesquita
   - Student Number: 106281
   - GitHub Username: jlgmaIscte

3. **Name**: Bárbara Albuquerque
   - Student Number: 106807
   - GitHub Username: bfaae

4. **Name**: Jéssica Vieira
   - Student Number: 110812
   - GitHub Username: Je-ssi-ca

## Known Issues & Incomplete Features

- **Graph Construction**  
  The code for building a graph from `PropertyRecord` objects is partially implemented or still pending.

- **Property Merging**  
  The feature to merge contiguous properties belonging to the same owner is not fully integrated.

- **Swap Suggestions**  
  The logic for suggesting property swaps is currently incomplete.

- **Tests**  
  Some integration tests remain to be written.

## Authors

Created and maintained by the ES-Project-TerritoryManagement Group I.
