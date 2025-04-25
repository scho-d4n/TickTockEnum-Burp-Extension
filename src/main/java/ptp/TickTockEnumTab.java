package ptp;

import burp.api.montoya.MontoyaApi;
import org.jfree.chart.ChartUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class TickTockEnumTab {
    private final MontoyaApi montoyaApi;
    private final TickTockEnumHandler tickTockEnumHandler;

    private TickTockEnumModel model;

    private JPanel mainPanel;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JPanel graphPlaceholder;
    private JTextArea requestArea;
    private JTextField hostField;
    private JTextField portField;
    private JTextField protocolField;
    private JTextField attemptsField;
    private JTextField validInputField;
    private JTextField invalidInputField;
    private JToggleButton toggleDebug;

    /**
     * Initialise UI and Handler
     *
     * @param montoyaApi: hand over Montoya API from main class
     */
    public TickTockEnumTab(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
        this.mainPanel = constructTickTockEnumTab();
        this.tickTockEnumHandler = new TickTockEnumHandler(montoyaApi, this.tableModel, this.graphPlaceholder, this.toggleDebug, this.mainPanel);
    }

    /**
     * Create extension Tab UI
     *
     * @return JPanel
     */
    public JPanel constructTickTockEnumTab() {
        // Main panel with BorderLayout (keeps everything aligned to the top)
        JPanel panel = new JPanel(new BorderLayout());

        // Wrapper panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // First row - Inputs & Start Button
        gbc.gridy = 0;
        gbc.gridx = 0;
        contentPanel.add(new JLabel("Attempts"), gbc);
        gbc.gridx++;
        this.attemptsField = new JTextField(5);
        this.attemptsField.setText("10");
        contentPanel.add(this.attemptsField, gbc);

        gbc.gridx++;
        contentPanel.add(new JLabel("Valid Input"), gbc);
        gbc.gridx++;
        this.validInputField = new JTextField(10);
        contentPanel.add(this.validInputField, gbc);

        gbc.gridx++;
        contentPanel.add(new JLabel("Invalid Input"), gbc);
        gbc.gridx++;
        this.invalidInputField = new JTextField(10);
        contentPanel.add(this.invalidInputField, gbc);

        gbc.gridx++;
        JButton startButton = new JButton("Start");
        contentPanel.add(startButton, gbc);
        startButton.addActionListener(e -> handleStartButton());

        // Second row - Login Request label & text area
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        contentPanel.add(new JLabel("Request (set $ticktock$ as the placeholder for the input)"), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        this.requestArea = new JTextArea(15, 50);
        this.requestArea.setLineWrap(true);
        this.requestArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(this.requestArea);
        contentPanel.add(scrollPane, gbc);

        // Third row - Host, Port, Protocol, Buttons
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        contentPanel.add(new JLabel("Host"), gbc);
        gbc.gridx++;
        this.hostField = new JTextField(15);
        contentPanel.add(this.hostField, gbc);

        gbc.gridx++;
        contentPanel.add(new JLabel("Port"), gbc);
        gbc.gridx++;
        this.portField = new JTextField(5);
        contentPanel.add(this.portField, gbc);

        gbc.gridx++;
        contentPanel.add(new JLabel("Protocol"), gbc);
        gbc.gridx++;
        this.protocolField = new JTextField(5);
        contentPanel.add(this.protocolField, gbc);

        gbc.gridx++;
        JButton clearButton = new JButton("Clear Request");
        contentPanel.add(clearButton, gbc);
        clearButton.addActionListener(e -> handleClearRequest());

        // Fourth row - Results table and Graph Panel (properly aligned)
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        this.tableModel = new DefaultTableModel(new String[]{"#", "Valid (Status Code)", "Invalid (Status Code)"}, 0);
        this.resultsTable = new JTable(this.tableModel);
        JScrollPane tableScrollPane = new JScrollPane(this.resultsTable);
        tableScrollPane.setPreferredSize(new Dimension(400, 200));

        gbc.gridx = 3;
        gbc.gridwidth = 3;
        this.graphPlaceholder = new JPanel();
        this.graphPlaceholder.setPreferredSize(new Dimension(400, 200));
        this.graphPlaceholder.setBorder(BorderFactory.createLineBorder(Color.BLACK)); // Placeholder for graph

        // Create separate panels to ensure correct alignment
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.add(new JLabel("Results"), BorderLayout.NORTH);
        resultsPanel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel graphWrapperPanel = new JPanel(new BorderLayout());
        graphWrapperPanel.add(new JLabel("Graph"), BorderLayout.NORTH);
        graphWrapperPanel.add(this.graphPlaceholder, BorderLayout.CENTER);

        // Use JSplitPane for proper alignment
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, resultsPanel, graphWrapperPanel);
        splitPane.setResizeWeight(0.5); // Split equally

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        contentPanel.add(splitPane, gbc);

        // Fifth row - Buttons for Results & Exporting
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        JButton clearResultsButton = new JButton("Clear Results");
        contentPanel.add(clearResultsButton, gbc);
        clearResultsButton.addActionListener(e -> handleClearResults());

        gbc.gridx++;
        JButton exportCsvButton = new JButton("Export CSV");
        contentPanel.add(exportCsvButton, gbc);
        exportCsvButton.addActionListener(e -> handleExportCSV());

        gbc.gridx++;
        JButton exportGraphButton = new JButton("Export Graph");
        contentPanel.add(exportGraphButton, gbc);
        exportGraphButton.addActionListener(e -> handleExportGraph());

        gbc.gridx++;
        this.toggleDebug = new JToggleButton("Turn Debug On");
        this.toggleDebug.setSelected(false);
        contentPanel.add(this.toggleDebug, gbc);
        this.toggleDebug.addActionListener(e -> handleDebugToggle());

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        contentPanel.add(new JLabel("Credit to Ben Marr (https://github.com/fullstackpotato) for the initial python script!"), gbc);

        // Add inner contentPanel to the main panel (aligning to top-left)
        panel.add(contentPanel, BorderLayout.NORTH);
        return panel;
    }

    /**
     * Handles "Start" button action
     */
    private void handleStartButton() {
        if (isDebug()) {
            this.montoyaApi.logging().logToOutput("Action: start enum");
        }

        // clear previous results
        handleClearResults();

        // create object
        this.model = new TickTockEnumModel(
                this.hostField.getText(),
                parseInt(this.portField.getText()),
                this.protocolField.getText(),
                this.requestArea.getText(),
                parseInt(this.attemptsField.getText()),
                this.validInputField.getText(),
                this.invalidInputField.getText()
        );

        // verify params and placeholder
        boolean paramsSet = this.tickTockEnumHandler.verifyParams(this.model);

        // start enum
        if (paramsSet) {
            this.tickTockEnumHandler.prepareRequest(this.model);
        }
    }

    /**
     * Handles "Clear Request" button action
     */
    private void handleClearRequest() {
        if (isDebug()) {
            this.montoyaApi.logging().logToOutput("Action: clear request details");
        }
        this.requestArea.setText("");
        this.hostField.setText("");
        this.portField.setText("");
        this.protocolField.setText("");

        // remove chart
        this.graphPlaceholder.removeAll();
        this.graphPlaceholder.revalidate();
        this.graphPlaceholder.repaint();
    }

    /**
     * Handles "Clear Results" button action
     */
    private void handleClearResults() {
        if (isDebug()) {
            this.montoyaApi.logging().logToOutput("Action: clear results");
        }

        SwingUtilities.invokeLater(() -> {
            // clear table
            ((DefaultTableModel) this.resultsTable.getModel()).setRowCount(0);

            // clear chart
            this.graphPlaceholder.removeAll();
            this.graphPlaceholder.revalidate();
            this.graphPlaceholder.repaint();

            // clear export
            tickTockEnumHandler.setExportChart(null);
        });

        // clear object
        this.model = null;
    }

    /**
     * Handles "Export CSV" button action
     */
    private void handleExportCSV() {
        if (isDebug()) {
            this.montoyaApi.logging().logToOutput("Action: export CSV");
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Results as CSV");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV File", "csv"));

        int userSelection = fileChooser.showSaveDialog(this.mainPanel.getParent());
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }

            try (FileWriter writer = new FileWriter(fileToSave)) {

                writer.append("Request, Input, Response Time, Status Code\n");
                appendData(writer, tickTockEnumHandler.getValidResultList());
                appendData(writer, tickTockEnumHandler.getInvalidResultList());

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this.mainPanel.getParent(), "Error saving results: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Handles "Export Graph" button action
     */
    private void handleExportGraph() {
        if (isDebug()) {
            this.montoyaApi.logging().logToOutput("Action: export graph");
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Graph as JPG");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JPG Image", "jpg"));

        int userSelection = fileChooser.showSaveDialog(this.mainPanel.getParent());
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().endsWith(".jpg")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".jpg");
            }

            try {
                ChartUtils.saveChartAsJPEG(fileToSave, tickTockEnumHandler.getExportChart(), this.graphPlaceholder.getWidth(), graphPlaceholder.getHeight());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this.mainPanel.getParent(), "Error saving graph: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Handles "Debug" button action
     */
    private void handleDebugToggle() {
        if (this.toggleDebug.isSelected()) {
            this.toggleDebug.setText("Turn Debug Off");
        } else {
            this.toggleDebug.setText("Turn Debug On");
        }
    }

    /**
     * Safely parse String to Int
     *
     * @param input String
     * @return int
     */
    private int parseInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Append data to the CSV output
     * @param writer FileWriter
     * @param resultList ArrayList<TickTockEnumResultModel>
     * @throws IOException exception handled in handleExportCSV()
     */
    private void appendData(FileWriter writer, ArrayList<TickTockEnumResultModel> resultList) throws IOException {
        for (TickTockEnumResultModel result : resultList) {
            writer.append(result.getAttempt() + ", " +
                    result.getType() + ", " +
                    result.getResponseTime() + ", " +
                    result.getStatusCode() + "\n");
        }
    }

    /**
     * Set all request fields in the extension UI
     *
     * @param request String
     * @param host String
     * @param port String
     * @param protocol String
     */
    public void setRequestFields(String request, String host, String port, String protocol) {
        this.requestArea.setText(request);
        this.hostField.setText(host);
        this.portField.setText(port);
        this.protocolField.setText(protocol);
    }

    /**
     * Public function to return main panel for the Burp Extension
     *
     * @return JPanel
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Public function to return the debug toggle value
     *
     * @return boolean
     */
    public boolean isDebug() {
        return this.toggleDebug.isSelected();
    }

    /**
     * Getter method for handler
     *
     * @return TickTockEnumHandler
     */
    public TickTockEnumHandler getTickTockEnumHandler() {
        return tickTockEnumHandler;
    }
}
