package iscteiul.ista;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import javax.swing.*;

/**
 * Basic JUnit test class for {@link AppUI}, the Swing-based user interface
 * of the ES-Project-TerritoryManagement application.
 *
 * <p>This class demonstrates how to perform simple UI tests without
 * specialized libraries. We check the following:</p>
 * <ul>
 *   <li>Whether the UI can be instantiated without errors.</li>
 *   <li>Whether the title and dimensions are as expected after {@code pack()}. </li>
 *   <li>(Optional) Accessing and verifying components, if getters are available.</li>
 * </ul>
 *
 * <p><strong>Note:</strong> For more thorough UI interaction tests (e.g., simulating clicks
 * and checking output), consider libraries such as AssertJ Swing or FEST.</p>
 */
class AppUITest {

    /**
     * Ensures that constructing a new {@link AppUI} instance completes
     * successfully and that no exceptions are thrown.
     *
     * <p>This test does <em>not</em> attempt to interact with or display the UI.
     * It merely checks for successful instantiation, which also covers
     * event listener attachment.</p>
     */
    @Test
    void testUIInitialization() {
        // Given / When
        AppUI ui = new AppUI();  // Create the UI

        // Then
        assertNotNull(ui, "AppUI instance should be created successfully");
    }

    /**
     * Verifies that the frame (JFrame) properties of {@link AppUI} are set as expected.
     * Specifically, we check the frame title and that the packed dimensions
     * are greater than zero.
     */
    @Test
    void testFrameProperties() {
        // Given
        AppUI ui = new AppUI();

        // Then: check default title
        String expectedTitle = "Territory Management Application";
        assertEquals(expectedTitle, ui.getTitle(),
                "The UI frame should have the expected title.");

        // Because the AppUI constructor calls pack(), the size should be > 0
        // if layout is correct.
        // You may optionally call ui.setVisible(true) if needed, but it’s
        // not strictly required for dimension checks.
        assertTrue(ui.getWidth() > 0 && ui.getHeight() > 0,
                "UI should have positive width and height after pack().");
    }
}
