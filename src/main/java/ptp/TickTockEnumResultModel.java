package ptp;

public class TickTockEnumResultModel {

    private int attempt;
    private float responseTime;
    private String type;
    private int statusCode;

    public TickTockEnumResultModel(int attempt, float resonseTime, String type, int statusCode) {
        this.attempt = attempt;
        this.responseTime = resonseTime;
        this.type = type;
        this.statusCode = statusCode;
    }

    public int getAttempt() {
        return attempt;
    }

    public float getResponseTime() {
        return responseTime;
    }

    public String getType() {
        return type;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
