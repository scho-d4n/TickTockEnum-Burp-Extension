package ptp;

public class TickTockEnumModel {
    private String host;
    private int port;
    private String protocol;
    private String request;
    private int attempts;
    private String validInput;
    private String invalidInput;
    private boolean useTLS = true;

    public TickTockEnumModel(String host, Integer port, String protocol, String request, Integer attempts, String validInput, String invalidInput) {
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.request = request;
        this.attempts = attempts;
        this.validInput = validInput;
        this.invalidInput = invalidInput;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getRequest() {
        return request;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getValidInput() {
        return validInput;
    }

    public String getInvalidInput() {
        return invalidInput;
    }

    public boolean getUseTLS() {
        return useTLS;
    }

    public void setUseTLS(boolean useTLS) {
        this.useTLS = useTLS;
    }
}
