package iscteiul.ista;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A Swing-based user interface for the ES-Project-TerritoryManagement application.
 *
 * <p>This UI displays:
 * <ul>
 *   <li>Two dropdowns: one for distinct parishes and one for distinct municipalities.</li>
 *   <li>Two radio buttons to select whether to focus on "Parish" or "Municipality" (default = parish).</li>
 *   <li>Six buttons:
 *       <ol>
 *         <li><strong>Show Properties</strong> (Req #1)</li>
 *         <li><strong>Properties Adjacency</strong> (Req #2)</li>
 *         <li><strong>Owner Adjacency</strong> (Req #3)</li>
 *         <li><strong>Compute Average Area</strong> (Req #4)</li>
 *         <li><strong>Compute Average Area by Owner</strong> (Req #5)</li>
 *         <li><strong>Suggest Property Swaps</strong> (Req #6) &mdash; now implemented</li>
 *       </ol>
 *       <em>
 *         "Show Properties", "Properties Adjacency", "Owner Adjacency",
 *         "Compute Average Area", "Compute Average Area by Owner",
 *         and "Suggest Property Swaps"
 *         have logic implemented.
 *       </em>
 *   </li>
 *   <li>A text area ({@code JTextArea}) for output logs/results.</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 *   // In App.main(...) after CSV data is loaded:
 *   AppUI.showUI();
 * }</pre>
 */
public class AppUI extends JFrame {

    // ============== Combo boxes ==============
    /** Combo box for parish selection. */
    private JComboBox<String> parishComboBox;

    /** Combo box for municipality selection. */
    private JComboBox<String> municipalityComboBox;

    // ============== Radio buttons ==============
    /** Radio button for filtering by parish. */
    private JRadioButton parishRadioButton;

    /** Radio button for filtering by municipality. */
    private JRadioButton municipalityRadioButton;

    // ============== Buttons ==============
    /** Button to show properties (Req #1). */
    private JButton showPropertiesButton;

    /** Button to compute adjacency among properties (Req #2). */
    private JButton propertiesAdjacencyButton;

    /** Button to compute adjacency among owners (Req #3). */
    private JButton ownerAdjacencyButton;

    /** Button to compute average area of properties (Req #4). */
    private JButton computeAverageAreaButton;

    /** Button to compute average area by merging same-owner adjacent props (Req #5). */
    private JButton computeAverageAreaByOwnerButton;

    /** Button to suggest property swaps (Req #6). */
    private JButton suggestPropertySwapsButton;

    // ============== Output area ==============
    /** A text area to display results or logs to the user. */
    private JTextArea outputTextArea;

    /**
     * Constructs the UI, populating the combo boxes for parishes and municipalities
     * from the data loaded in {@link App#getPropertyRecords()}, and setting up
     * two radio buttons for choosing either parish or municipality scope.
     * <p>
     * Also creates six buttons: Show Properties, Properties Adjacency,
     * Owner Adjacency, Compute Average Area, Compute Average Area by Owner,
     * and Suggest Property Swaps. Now all six have logic implemented.
     */
    public AppUI() {
        super("Territory Management Application");

        // Retrieve property records from the App class
        List<PropertyRecord> propertyRecords = App.getPropertyRecords();

        // Distinct parishes & municipalities
        Set<String> distinctParishes = PropertyUtils.getDistinctParishes(propertyRecords);
        Set<String> distinctMunicipalities = PropertyUtils.getDistinctMunicipalities(propertyRecords);

        // Convert sets to lists for combo box models
        List<String> parishList = new ArrayList<>(distinctParishes);
        List<String> muniList = new ArrayList<>(distinctMunicipalities);

        // =================== TOP PANEL ===================
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // ============= LINE 1 (combo + radio) =============
        JPanel line1Panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        line1Panel.add(new JLabel("Select Parish:"));
        parishComboBox = new JComboBox<>(parishList.toArray(new String[0]));
        line1Panel.add(parishComboBox);

        line1Panel.add(new JLabel("Select Municipality:"));
        municipalityComboBox = new JComboBox<>(muniList.toArray(new String[0]));
        line1Panel.add(municipalityComboBox);

        parishRadioButton = new JRadioButton("Parish", true);
        municipalityRadioButton = new JRadioButton("Municipality");

        ButtonGroup bg = new ButtonGroup();
        bg.add(parishRadioButton);
        bg.add(municipalityRadioButton);

        line1Panel.add(parishRadioButton);
        line1Panel.add(municipalityRadioButton);

        // ============= LINE 2 (buttons) =============
        JPanel line2Panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        showPropertiesButton = new JButton("Show Properties");
        propertiesAdjacencyButton = new JButton("Properties Adjacency");
        ownerAdjacencyButton = new JButton("Owner Adjacency");
        computeAverageAreaButton = new JButton("Compute Average Area");
        computeAverageAreaByOwnerButton = new JButton("Compute Average Area by Owner");
        suggestPropertySwapsButton = new JButton("Suggest Property Swaps");

        line2Panel.add(showPropertiesButton);
        line2Panel.add(propertiesAdjacencyButton);
        line2Panel.add(ownerAdjacencyButton);
        line2Panel.add(computeAverageAreaButton);
        line2Panel.add(computeAverageAreaByOwnerButton);
        line2Panel.add(suggestPropertySwapsButton);

        topPanel.add(line1Panel);
        topPanel.add(line2Panel);

        // =================== CENTER PANEL (Output) ===================
        outputTextArea = new JTextArea(40, 60);
        outputTextArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(
                outputTextArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        );

        // Layout: top panel at NORTH, scroll pane at CENTER
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Window settings
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ============= Attach Action Listeners =============
        showPropertiesButton.addActionListener(e -> handleShowProperties());
        propertiesAdjacencyButton.addActionListener(e -> handlePropertiesAdjacency());
        ownerAdjacencyButton.addActionListener(e -> handleOwnerAdjacency());
        computeAverageAreaButton.addActionListener(e -> handleComputeAverageArea());
        computeAverageAreaByOwnerButton.addActionListener(e -> handleComputeAverageAreaByOwner());

        // NEW: suggest property swaps
        suggestPropertySwapsButton.addActionListener(e -> handleSuggestPropertySwaps());
    }

    /**
     * Handles the "Show Properties" button click (Req #1).
     * <p>Based on the selected radio button (Parish or Municipality),
     * retrieves the chosen item from the respective combo box,
     * filters the property list, and displays the results in
     * the {@code outputTextArea}.
     */
    private void handleShowProperties() {
        outputTextArea.setText("");

        boolean isParishSelected = parishRadioButton.isSelected();
        String selectedValue = isParishSelected
                ? (String) parishComboBox.getSelectedItem()
                : (String) municipalityComboBox.getSelectedItem();

        if (selectedValue == null || selectedValue.isBlank()) {
            outputTextArea.append("No valid selection.\n");
            return;
        }

        List<PropertyRecord> allRecords = App.getPropertyRecords();
        if (allRecords == null || allRecords.isEmpty()) {
            outputTextArea.append("No property records loaded.\n");
            return;
        }

        List<PropertyRecord> filtered = isParishSelected
                ? PropertyUtils.findByParish(allRecords, selectedValue)
                : PropertyUtils.findByMunicipality(allRecords, selectedValue);

        if (filtered.isEmpty()) {
            outputTextArea.append("No properties found for " + selectedValue + ".\n");
        } else {
            outputTextArea.append("Properties in " + selectedValue + ":\n");
            filtered.forEach(pr -> outputTextArea.append(
                    "ObjectID=" + pr.getObjectID()
                            + ", ParcelID=" + pr.getParcelID()
                            + ", ParcelNumber=" + pr.getParcelNumber()
                            + ", Perimeter[km]=" + String.format("%.2f", pr.getShapeLength())
                            + ", Area[ha]=" + String.format("%.2f", pr.getShapeArea())
                            + ", Owner=" + pr.getOwner()
                            + ", Parish=" + pr.getParish()
                            + ", Municipality=" + pr.getMunicipality()
                            + ", Island=" + pr.getIsland()
                            + "\n"
            ));
        }
    }

    /**
     * Handles the "Properties Adjacency" button click (Req #2).
     * <p>Determines the selected region, filters the property list,
     * builds a {@link Graph} of properties, and lists each property's
     * neighbors in the {@code outputTextArea}.
     */
    private void handlePropertiesAdjacency() {
        outputTextArea.setText("");

        boolean isParishSelected = parishRadioButton.isSelected();
        String selectedValue = isParishSelected
                ? (String) parishComboBox.getSelectedItem()
                : (String) municipalityComboBox.getSelectedItem();

        if (selectedValue == null || selectedValue.isBlank()) {
            outputTextArea.append("No valid selection for adjacency.\n");
            return;
        }

        List<PropertyRecord> allRecords = App.getPropertyRecords();
        if (allRecords == null || allRecords.isEmpty()) {
            outputTextArea.append("No property records loaded.\n");
            return;
        }

        List<PropertyRecord> subset = isParishSelected
                ? PropertyUtils.findByParish(allRecords, selectedValue)
                : PropertyUtils.findByMunicipality(allRecords, selectedValue);

        if (subset.isEmpty()) {
            outputTextArea.append("No properties found for " + selectedValue + ".\n");
            return;
        }

        // Build the adjacency graph
        Graph propertyGraph = new Graph(subset);

        // For each property, list its neighbors
        for (PropertyRecord record : subset) {
            int objectID = record.getObjectID();
            List<Graph.GraphNode> neighbors = propertyGraph.getNeighbors(objectID);

            if (neighbors.isEmpty()) {
                outputTextArea.append("Property ID=" + objectID
                        + " (Area[ha]=" + String.format("%.2f", record.getShapeArea())
                        + ", Owner=" + record.getOwner()
                        + ") has no neighbors.\n");
            } else {
                outputTextArea.append("Neighbors of Property ID=" + objectID
                        + " (Area[ha]=" + String.format("%.2f", record.getShapeArea())
                        + ", Owner=" + record.getOwner() + "):\n");
                neighbors.forEach(neighbor -> outputTextArea.append(
                        "  -> objectID=" + neighbor.getObjectID()
                                + ", area[ha]=" + String.format("%.2f", neighbor.getShapeArea())
                                + ", owner=" + neighbor.getOwner()
                                + "\n"
                ));
            }
        }
    }

    /**
     * Handles the "Owner Adjacency" button click (Req #3).
     * <p>Determines which region is selected, filters the list,
     * builds an {@link OwnerGraph}, and displays each owner's neighbors.
     */
    private void handleOwnerAdjacency() {
        outputTextArea.setText("");

        boolean isParishSelected = parishRadioButton.isSelected();
        String selectedValue = isParishSelected
                ? (String) parishComboBox.getSelectedItem()
                : (String) municipalityComboBox.getSelectedItem();

        if (selectedValue == null || selectedValue.isBlank()) {
            outputTextArea.append("No valid selection for owner adjacency.\n");
            return;
        }

        List<PropertyRecord> allRecords = App.getPropertyRecords();
        if (allRecords == null || allRecords.isEmpty()) {
            outputTextArea.append("No property records loaded.\n");
            return;
        }

        // Filter
        List<PropertyRecord> subset = isParishSelected
                ? PropertyUtils.findByParish(allRecords, selectedValue)
                : PropertyUtils.findByMunicipality(allRecords, selectedValue);

        if (subset.isEmpty()) {
            outputTextArea.append("No properties found for " + selectedValue + ".\n");
            return;
        }

        // Build the owner graph
        OwnerGraph ownerGraph = new OwnerGraph();
        ownerGraph.buildGraph(subset);

        Set<Integer> owners = ownerGraph.getOwners();
        if (owners.isEmpty()) {
            outputTextArea.append("No owners found in " + selectedValue + ".\n");
            return;
        }

        outputTextArea.append("Owner adjacency for " + selectedValue + ":\n");
        for (Integer owner : owners) {
            Set<Integer> neighbors = ownerGraph.getNeighbors(owner);
            if (neighbors.isEmpty()) {
                outputTextArea.append("Owner " + owner + " has no adjacent owners.\n");
            } else {
                outputTextArea.append("Owner " + owner + " is adjacent to owners: " + neighbors + "\n");
            }
        }
    }

    /**
     * Handles the "Compute Average Area" button click (Req #4).
     * <p>Determines which region is selected, filters the list,
     * then calls {@code PropertyUtils.calculateAverageArea(...)} and
     * displays the result in the {@code outputTextArea}.
     */
    private void handleComputeAverageArea() {
        outputTextArea.setText("");

        boolean isParishSelected = parishRadioButton.isSelected();
        String selectedValue = isParishSelected
                ? (String) parishComboBox.getSelectedItem()
                : (String) municipalityComboBox.getSelectedItem();

        if (selectedValue == null || selectedValue.isBlank()) {
            outputTextArea.append("No valid selection for average area.\n");
            return;
        }

        List<PropertyRecord> allRecords = App.getPropertyRecords();
        if (allRecords == null || allRecords.isEmpty()) {
            outputTextArea.append("No property records loaded.\n");
            return;
        }

        // Filter
        List<PropertyRecord> subset = isParishSelected
                ? PropertyUtils.findByParish(allRecords, selectedValue)
                : PropertyUtils.findByMunicipality(allRecords, selectedValue);

        if (subset.isEmpty()) {
            outputTextArea.append("No properties found for " + selectedValue + ".\n");
            return;
        }

        // Compute average
        double averageArea = PropertyUtils.calculateAverageArea(subset);

        // Display
        outputTextArea.append("Average area in " + selectedValue
                + " (no adjacency grouping) = " + String.format("%.2f", averageArea) + " [ha]\n");
    }

    /**
     * Handles the "Compute Average Area by Owner" button click (Req #5).
     * <p>Determines which region is selected, filters the list, merges
     * adjacent properties (same owner), then calculates the average
     * grouped area via
     * {@code PropertyUtils.calculateAverageGroupedArea(...)} or similar.
     * <p>We only show the grouped average area result, no comparison
     * with ungrouped area is printed.
     */
    private void handleComputeAverageAreaByOwner() {
        outputTextArea.setText("");

        boolean isParishSelected = parishRadioButton.isSelected();
        String selectedValue = isParishSelected
                ? (String) parishComboBox.getSelectedItem()
                : (String) municipalityComboBox.getSelectedItem();

        if (selectedValue == null || selectedValue.isBlank()) {
            outputTextArea.append("No valid selection for 'average area by owner'.\n");
            return;
        }

        // Retrieve all records
        List<PropertyRecord> allRecords = App.getPropertyRecords();
        if (allRecords == null || allRecords.isEmpty()) {
            outputTextArea.append("No property records loaded.\n");
            return;
        }

        // Filter by parish or municipality
        List<PropertyRecord> subset = isParishSelected
                ? PropertyUtils.findByParish(allRecords, selectedValue)
                : PropertyUtils.findByMunicipality(allRecords, selectedValue);

        if (subset.isEmpty()) {
            outputTextArea.append("No properties found for " + selectedValue + ".\n");
            return;
        }

        // Build a JGraphT-based property graph
        PropertyGraph propertyGraph = new PropertyGraph();
        propertyGraph.buildGraph(subset);

        // JGraphT graph from the propertyGraph
        org.jgrapht.Graph<PropertyRecord, DefaultEdge> jgtGraph = propertyGraph.getGraph();

        // Now compute grouped area (owner-based adjacency)
        double averageGroupedArea = PropertyUtils.calculateAverageGroupedArea(subset, jgtGraph);

        // Print only the grouped average
        outputTextArea.append("Average area in " + selectedValue + " (grouped by owner adjacency) = "
                + String.format("%.2f", averageGroupedArea) + " [ha]\n");
    }

    /**
     * Handles the "Suggest Property Swaps" button click (Req #6).
     * <p>Determines which region is selected, filters the list, merges
     * same-owner parcels, builds an adjacency graph of the merged
     * properties, and uses {@link PropertySwapAdvisor#suggestSwaps}
     * (with areaThreshold=0.1, maxSuggestions=10) to list potential swaps.
     * <p>Prints each suggestion to the {@code outputTextArea}.
     */
    private void handleSuggestPropertySwaps() {
        outputTextArea.setText("");

        boolean isParishSelected = parishRadioButton.isSelected();
        String selectedValue = isParishSelected
                ? (String) parishComboBox.getSelectedItem()
                : (String) municipalityComboBox.getSelectedItem();

        if (selectedValue == null || selectedValue.isBlank()) {
            outputTextArea.append("No valid selection for property swaps.\n");
            return;
        }

        // Retrieve all records
        List<PropertyRecord> allRecords = App.getPropertyRecords();
        if (allRecords == null || allRecords.isEmpty()) {
            outputTextArea.append("No property records loaded.\n");
            return;
        }

        // Filter
        List<PropertyRecord> subset = isParishSelected
                ? PropertyUtils.findByParish(allRecords, selectedValue)
                : PropertyUtils.findByMunicipality(allRecords, selectedValue);

        if (subset.isEmpty()) {
            outputTextArea.append("No properties found for " + selectedValue + ".\n");
            return;
        }

        // 1) Merge same-owner adjacency
        List<PropertyRecord> merged = PropertyMerger.mergeSameOwner(subset);

        // 2) Build adjacency among these merged properties
        SimpleGraph<PropertyRecord, DefaultEdge> mergedGraph = MergedPropertyGraph.buildGraph(merged);

        // 3) Suggest swaps with areaThreshold=0.1 => up to 10 suggestions
        List<SwapSuggestion> suggestions = PropertySwapAdvisor.suggestSwaps(mergedGraph, 0.1, 10);

        // 4) Print the suggestions
        if (suggestions.isEmpty()) {
            outputTextArea.append("No swap suggestions found in " + selectedValue + ".\n");
        } else {
            outputTextArea.append("Swap suggestions for " + selectedValue + ":\n");
            for (SwapSuggestion s : suggestions) {
                outputTextArea.append(
                        String.format("- Swap: Property %d (owner=%d) <--> Property %d (owner=%d)%n"
                                        + "  benefit=%.3f, cost=%.3f, score=%.3f%n",
                                s.getP1().getObjectID(), s.getP1().getOwner(),
                                s.getP2().getObjectID(), s.getP2().getOwner(),
                                s.getBenefit(), s.getCost(), s.getScore())
                );
            }
        }
    }

    /**
     * Displays this UI on the Event Dispatch Thread.
     * <p>
     * Invoke this after loading data in the {@link App} class, for example:
     * <pre>{@code
     *   AppUI.showUI();
     * }</pre>
     */
    public static void showUI() {
        SwingUtilities.invokeLater(() -> {
            AppUI ui = new AppUI();
            ui.setVisible(true);
        });
    }
}
