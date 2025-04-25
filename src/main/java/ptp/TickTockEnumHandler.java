package ptp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class TickTockEnumHandler {

    // Montoya API
    private final MontoyaApi montoyaApi;

    // threading
    private ExecutorService executor;
    private final ReentrantLock lock;
    private final Queue<TickTockEnumResultModel> resultQueue;

    // UI elements from Tab
    private DefaultTableModel tableModel;
    private JToggleButton toggleDebug;
    private JPanel graphPlaceholder;
    private JPanel mainPanel;

    // results
    private JFreeChart chart;
    private ArrayList<TickTockEnumResultModel> validResultList;
    private ArrayList<TickTockEnumResultModel> invalidResultList;

    /**
     * Initialise Handler
     *
     * @param montoyaApi MontoyaAPI
     * @param tableModel DefaultTableModel
     * @param graphPlaceholder JPanel
     * @param toggleDebug JToggleButton
     */
    public TickTockEnumHandler(MontoyaApi montoyaApi, DefaultTableModel tableModel, JPanel graphPlaceholder, JToggleButton toggleDebug, JPanel mainPanel) {
        // initialise lock and queue for threading
        this.lock = new ReentrantLock();
        this.resultQueue = new ConcurrentLinkedQueue<>();

        // result lists
        this.validResultList = new ArrayList<TickTockEnumResultModel>();
        this.invalidResultList = new ArrayList<TickTockEnumResultModel>();

        // handed over Montoya API
        this.montoyaApi = montoyaApi;

        // handed over UI elements
        this.tableModel = tableModel;
        this.graphPlaceholder = graphPlaceholder;
        this.toggleDebug = toggleDebug;
        this.mainPanel = mainPanel;
    }

    /**
     * Prepare Http Request with input from UI
     *
     * With MontoyaAPI can create HttpRequest with the HttpService, and either a byte[] or a String format of the request.
     * In this case using the request as string.
     *
     * @param model TickTockEnumModel
     */
    public void prepareRequest(TickTockEnumModel model) {
        // clear old results
        clearResults();

        // Create service based on host, port and protocol
        HttpService httpService = HttpService.httpService(model.getHost(), model.getPort(), model.getUseTLS());

        if (this.toggleDebug.isSelected()) {
            this.montoyaApi.logging().logToOutput(String.format("HTTP Service: %s, %s, %s", httpService.host(), httpService.port(), httpService.secure()));
        }

        // Get the request as string from the UI and replace the placeholder with the valid and invalid data provided.
        String validUserRequestString = model.getRequest().replace("$ticktock$", model.getValidInput());
        String invalidUserRequestString = model.getRequest().replace("$ticktock$", model.getInvalidInput());

        // Create the HttpRequest for each, valid and invalid data
        HttpRequest validUserHttpRequest = HttpRequest.httpRequest(httpService, validUserRequestString);
        HttpRequest invalidUserHttpRequest = HttpRequest.httpRequest(httpService, invalidUserRequestString);

        if (this.toggleDebug.isSelected()) {
            this.montoyaApi.logging().logToOutput(String.format("Valid Request:\n%s\n", validUserRequestString));
            this.montoyaApi.logging().logToOutput(String.format("Invalid Request:\n%s\n", invalidUserRequestString));
        }

        // Start enumeration first, for valid and then invalid.
        startEnum(model.getAttempts(), validUserHttpRequest, "valid");
        startEnum(model.getAttempts(), invalidUserHttpRequest, "invalid");
    }

    /**
     * Start enumeration in thread. Using a single thread executor to get the most accurate results for a
     * time based enumeration. Though this is obviously a bit slower.
     *
     * @param attempts int
     * @param httpRequest HttpRequest
     * @param type String
     */
    private void startEnum(int attempts, HttpRequest httpRequest, String type) {
        if (this.executor == null || this.executor.isShutdown() || this.executor.isTerminated()) {
            // creates a single thread for running the enum - added comment with multi-thread option in case required.
            this.executor = Executors.newSingleThreadExecutor(); //.newFixedThreadPool(2);
        }
        this.executor.submit(() -> sendHttpRequest(httpRequest, attempts, this::onRequestCompletion, type));
    }

    /**
     * Send the requests through Burp (requests will be shown in Logger)
     *
     * @param httpRequest HttpRequest
     * @param attempts int
     * @param onComplete Consumer<String> (allows for onComplete function to have one param for the type)
     * @param type String
     */
    private void sendHttpRequest(HttpRequest httpRequest, int attempts, Consumer<String> onComplete, String type) {
        try {
            // send set number of requests within the thread - made it easier to deal with the output
            for (int i = 0; i < attempts; i++) {
                if (this.toggleDebug.isSelected()) {
                    this.montoyaApi.logging().logToOutput(String.format("Info: sending request number %d of type - %s", (i+1), type));
                }

                // log request start time
                long startTime = System.nanoTime();

                // send request
                HttpRequestResponse requestResponse = this.montoyaApi.http().sendRequest(httpRequest);

                // log and format request completed time
                long endTime = System.nanoTime();
                float responseTime = (endTime - startTime) / 1_000_000.0f;
                responseTime = (float) (Math.round(responseTime * 100.0) / 100.0);

                // add result to queue
                if (requestResponse.response() != null) {
                    this.resultQueue.add(new TickTockEnumResultModel(i+1, responseTime, type, requestResponse.response().statusCode()));
                }
            }
        } catch (Exception e) {
            this.montoyaApi.logging().logToOutput(String.format("Error: sending request\n%s", e.getMessage()));
        } finally {
            // proceed to onComplete function
            onComplete.accept(type);
        }
    }

    /**
     * Process the results
     *
     * @param type String
     */
    private void onRequestCompletion(String type) {
        if (this.toggleDebug.isSelected()) {
            this.montoyaApi.logging().logToOutput(String.format("Info: received responses for type - %s", type));
        }

        // set lock so if next set of results are complete they don't get mixed up in the table and graph
        this.lock.lock();
        try {

            // iterate through responses
            while (!this.resultQueue.isEmpty()) {
                TickTockEnumResultModel result = this.resultQueue.poll();

                if (this.toggleDebug.isSelected()) {
                    this.montoyaApi.logging().logToOutput(String.format("# %s - Type: %s - Status Code: %s - Response Time: %s",
                            result.getAttempt(),
                            result.getType(),
                            result.getStatusCode(),
                            result.getResponseTime()));
                }

                // add to respective result list
                if ("valid".equalsIgnoreCase(result.getType())) {
                    this.validResultList.add(result);
                } else {
                    this.invalidResultList.add(result);
                }

                // update results table
                String formattedValue = String.format("%.2f ms (%d)", result.getResponseTime(), result.getStatusCode());
                if (this.tableModel.getRowCount() < result.getAttempt()) {
                    // table empty, add rows
                    if ("valid".equalsIgnoreCase(result.getType())) {
                        //put in column 1
                        this.tableModel.addRow(new Object[]{result.getAttempt(), formattedValue, ""});
                    } else {
                        //put in column 2
                        this.tableModel.addRow(new Object[]{result.getAttempt(), "", formattedValue});
                    }
                } else {
                    // update existing rows
                    if ("valid".equalsIgnoreCase(result.getType())) {
                        this.tableModel.setValueAt(formattedValue, result.getAttempt()-1, 1);
                    } else {
                        this.tableModel.setValueAt(formattedValue, result.getAttempt()-1, 2);
                    }
                }
            }

            // shut down thread executor
            this.executor.shutdown();
        } catch (Exception e) {
            this.montoyaApi.logging().logToOutput(String.format("Error: collecting/displaying results\n%s", e.getMessage()));
        } finally {
            // remove lock
            this.lock.unlock();
        }

        // update graph if both sets of results came back
        if (!this.validResultList.isEmpty() && this.validResultList.size() == this.invalidResultList.size()) {
            prepareGraph();
        }
    }

    /**
     * Prepare data for the graph and then plot it.
     */
    private void prepareGraph() {
        XYSeries validGraphPoints = parseResultsForGraph(this.validResultList, "valid");
        XYSeries invalidGraphPoints = parseResultsForGraph(this.invalidResultList, "invalid");

        SwingUtilities.invokeLater(() -> {
            ChartPanel chartPanel = plotGraph(validGraphPoints, invalidGraphPoints);
            this.graphPlaceholder.add(chartPanel);
            this.graphPlaceholder.revalidate();
            this.graphPlaceholder.repaint();
        });
    }

    /**
     * Generate graph for extension UI
     *
     * @param validSet XYSeries
     * @param invalidSet XYSeries
     * @return ChartPanel
     */
    private ChartPanel plotGraph(XYSeries validSet, XYSeries invalidSet) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(validSet);
        dataset.addSeries(invalidSet);

        this.chart = ChartFactory.createXYLineChart(
                "",
                "Count",
                "Time (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // Improve graph readability
        XYPlot plot = this.chart.getXYPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setAutoRangeIncludesZero(false);
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        ChartPanel chartPanel = new ChartPanel(this.chart);
        chartPanel.setPreferredSize(new Dimension(400, 190));

        return chartPanel;
    }

    /**
     * Parse ArrayList<TickTockEnumResultModel> to XYSeries
     *
     * @param resultList ArrayList<TickTockEnumResultModel>
     * @param setName String
     * @return XYSeries
     */
    private XYSeries parseResultsForGraph(ArrayList<TickTockEnumResultModel> resultList, String setName) {
        XYSeries parsedResults = new XYSeries(setName);
        for (TickTockEnumResultModel result : resultList) {
            parsedResults.add(result.getAttempt(), result.getResponseTime());
        }
        return parsedResults;
    }

    /**
     * Verify user input from extension UI
     *
     * @param model TickTockEnumModel
     * @return boolean
     */
    public boolean verifyParams(TickTockEnumModel model) {
        // number of attempts
        if (model.getAttempts() == 0) {
            showInputError("Error: No or incorrect value provided in the 'Attempts' field.");
            return false;
        }

        // valid user
        if (model.getValidInput() == null || model.getValidInput().isBlank()) {
            showInputError("Error: No value provided in the 'Valid User' field.");
            return false;
        }

        // invalid user
        if (model.getInvalidInput() == null || model.getInvalidInput().isBlank()) {
            showInputError("Error: No value provided in the 'Invalid User' field.");
            return false;
        }

        // host field
        if (model.getHost() == null || model.getHost().isBlank()) {
            showInputError("Error: No value provided in the 'Host' field.");
            return false;
        }

        // port field
        if (model.getPort() == 0) {
            showInputError("Error: No or incorrect value provided in the 'Port' field.");
            return false;
        }

        // protocol
        if (!"http".equalsIgnoreCase(model.getProtocol()) && !"https".equalsIgnoreCase(model.getProtocol())) {
            showInputError("Error: No or incorrect value provided in the 'Protocol' field.");
            return false;
        } else {
            if ("https".equalsIgnoreCase(model.getProtocol())) {
                model.setUseTLS(true);
            }
        }

        // request
        boolean requestParamsConfirmed;
        if (model.getRequest() == null || model.getRequest().isBlank()) {
            showInputError("Error: No or incorrect value provided in the 'Request' field.");
            return false;
        } else {
            requestParamsConfirmed = verifyRequest(model);
        }

        // check for placeholder
        if (requestParamsConfirmed) {
            if (!model.getRequest().contains("$ticktock$")) {
                showInputError("Error: Placeholder '$ticktock$' not found.");
                return false;
            }
        }

        return true;
    }

    /**
     * Show Swing error message
     *
     * @param errorMessage String
     */
    private void showInputError(String errorMessage) {
        this.montoyaApi.logging().logToError(errorMessage);
        JOptionPane.showMessageDialog(this.mainPanel.getParent(), errorMessage, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * In case user manually copies data into the request fields of the extension UI or adjusts params.
     *
     * @param model TickTockEnumModel
     * @return boolean
     */
    private boolean verifyRequest(TickTockEnumModel model) {
        ArrayList<String> requestHeaders = new ArrayList<String>();

        // split headers/body
        String[] requestParts = model.getRequest().split("\r\n\r\n", 2);

        // headers
        if (requestParts.length > 0) {
            String[] headerLines = requestParts[0].split("\r\n");
            requestHeaders.addAll(Arrays.asList(headerLines));
        }

        // if there are no headers then either parsing error or incorrect data provided
        if (requestHeaders.isEmpty()) {
            showInputError("Error: There was an error extracting the HTTP headers from the 'Request' field.");
            return false;
        }

        // extract method, path, host and port
        // check whether host and port in headers is the same as in the separate input fields
        // in case user made changes in the UI
        String extractedHost = null;
        int extractedPort = 0;

        for (String header : requestHeaders) {
            if (header.toLowerCase().startsWith("host:")) {
                extractedHost = header.substring(5).trim();
                break;
            }
        }

        if (extractedHost != null) {
            String[] parts = extractedHost.split(":");
            if (parts.length == 2) {
                extractedHost = parts[0]; //Hostname

                try {
                    extractedPort = Integer.parseInt(parts[1]); //Port
                } catch (NumberFormatException e) {
                    extractedPort = 0;
                }
            }

            if (!extractedHost.equalsIgnoreCase(model.getHost())) {
                showInputError("Error: Host mismatch between request header and 'Host' input field");
                return false;
            }

            if (extractedPort != 0 && extractedPort != model.getPort()) {
                showInputError("Error: Port mismatch between request header and 'Port' input field");
                return false;
            }
        }

        return true;
    }

    /**
     * Getter method for the chart export
     *
     * @return JFreeChart
     */
    public JFreeChart getExportChart() {
        return this.chart;
    }

    /**
     * Setter method for cleaning up the results
     * @param exportChart JFreeChart
     */
    public void setExportChart(JFreeChart exportChart) {
        this.chart = exportChart;
    }

    /**
     * Getter method for the valid results
     *
     * @return ArrayList<TickTockEnumResultModel>
     */
    public ArrayList<TickTockEnumResultModel> getValidResultList() {
        return validResultList;
    }

    /**
     * Clear results between runs
     */
    private void clearResults() {
        this.validResultList.clear();
        this.invalidResultList.clear();
    }

    /**
     * Getter method for the invalid results
     *
     * @return ArrayList<TickTockEnumResultModel>
     */
    public ArrayList<TickTockEnumResultModel> getInvalidResultList() {
        return invalidResultList;
    }

    /**
     * Getter method for thread executor
     *
     * @return ExecutorService
     */
    public ExecutorService getExecutor() {
        return executor;
    }
}
