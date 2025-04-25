package ptp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TickTockEnumContextMenuProvider implements ContextMenuItemsProvider {

    private final MontoyaApi montoyaApi;
    private final TickTockEnumTab tickTockEnumTab;

    public TickTockEnumContextMenuProvider(MontoyaApi montoyaApi, TickTockEnumTab tickTockEnumTab) {
        this.montoyaApi = montoyaApi;
        this.tickTockEnumTab = tickTockEnumTab;
    }

    /**
     * Burp Suite Context Menu Provider
     *
     * @param event: Burp UI ContextMenuEvent
     * @return a list of components to be shown in the UI
     */
    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        if (event.isFromTool(ToolType.PROXY, ToolType.REPEATER, ToolType.TARGET, ToolType.LOGGER)) {
            List<Component> menuItemList = new ArrayList<>();
            JMenuItem sendRequestToExtension = new JMenuItem("Send Request to Extension");
            menuItemList.add(sendRequestToExtension);

            sendRequestToExtension.addActionListener(e -> {
                // get the first element/request selected or get the request from the request editor
                HttpRequestResponse requestResponse = event.messageEditorRequestResponse().isPresent() ? event.messageEditorRequestResponse().get().requestResponse() : event.selectedRequestResponses().getFirst();
                updateUI(requestResponse.request());
            });

            return menuItemList;
        }

        return null;
    }

    /**
     * Update the Extension UI with the selected request
     *
     * @param request: selected HttpRequest
     */
    private void updateUI(HttpRequest request) {
        String requestLine = String.format("%s %s HTTP/1.1", request.method(), request.path());
        String headersString = request.headers().stream()
                .map(header -> header.name() + ": " + header.value())
                .collect(Collectors.joining("\r\n"));
        String requestBody = request.body().toString();
        String host = request.httpService().host();
        int port = request.httpService().port();
        String protocol = request.httpService().secure() ? "https" : "http";

        // Ensure UI updates run on the Swing thread
        SwingUtilities.invokeLater(() -> {
            tickTockEnumTab.setRequestFields(requestLine + "\r\n" + headersString + "\r\n\r\n" + requestBody, host, String.valueOf(port), protocol);
            if (tickTockEnumTab.isDebug()) {
                this.montoyaApi.logging().logToOutput("UI updated with selected request details.");
            }
        });
    }
}
