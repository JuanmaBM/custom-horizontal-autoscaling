package org.jmb;

public class Response {

    public String lag;
    public String status;
    public String message;

    public Response() { }

    public Response(String lag, String status, String message) {
        this.lag = lag;
        this.status = status;
        this.message = message;
    }
}
