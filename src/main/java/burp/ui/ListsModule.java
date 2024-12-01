package burp.ui;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;

public class ListsModule {
    private final HttpRequestResponse requestResponse;
    private String URL;
    private String vulType;
    private String vendor;
    public ListsModule(HttpRequestResponse requestResponse,String vulType, String vendor) {
        this.requestResponse = requestResponse;
        this.URL = requestResponse.request().url();
        this.vulType = vulType;
        this.vendor = vendor;
    }

    @Override
    public String toString() {
        return requestResponse.url();
    }

    public HttpRequestResponse getRequestResponse(){
        return requestResponse;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getVulType() {
        return vulType;
    }

    public void setVulType(String vulType) {
        this.vulType = vulType;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
}
