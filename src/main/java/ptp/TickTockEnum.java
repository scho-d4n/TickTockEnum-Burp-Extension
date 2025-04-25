package ptp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import java.util.concurrent.ExecutorService;

public class TickTockEnum implements BurpExtension {

    private MontoyaApi montoyaApi;
    private TickTockEnumTab tickTockEnumTab;

    /**
     * Burp Extension main function
     *
     * @param montoyaApi: As per documentation, initialise Montoya API
     */
    @Override
    public void initialize(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;

        montoyaApi.extension().setName("TickTock Enum");

        // create extension UI and add Tab in Burp UI
        this.tickTockEnumTab = new TickTockEnumTab(this.montoyaApi);
        this.montoyaApi.userInterface().registerSuiteTab("TickTock Enum", tickTockEnumTab.getMainPanel());

        // add Context Menu
        this.montoyaApi.userInterface().registerContextMenuItemsProvider(new TickTockEnumContextMenuProvider(montoyaApi, tickTockEnumTab));

        // add unloading handler
        this.montoyaApi.extension().registerUnloadingHandler(this::onUnload);
    }

    /**
     * Ensures that all background threads are terminated when extension is unloaded.
     */
    private void onUnload() {
        this.montoyaApi.logging().logToOutput("Unloading extension... Cleaning up resources.");
        ExecutorService executor = this.tickTockEnumTab.getTickTockEnumHandler().getExecutor();

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}