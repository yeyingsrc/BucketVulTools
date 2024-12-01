package burp.vendor.aliyun;

import burp.common.Base;
import burp.common.Constant;
import burp.common.IAction;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import burp.ui.ListsModule;

import java.util.ArrayList;
import java.util.List;

public class OSS implements Base {

    List<ListsModule> listsModule = new ArrayList<>();
    private final HttpRequest httpRequest;
    @Override
    public List<ListsModule> checkVul() {
        IAction[] iActions = {
                this::bucketsTraversable,
                this::checkUploadFile,
                this::checkObjectAcl,
                this::checkBucketPolicy
        };
        for (IAction iAction : iActions) {
            iAction.execute();
        }
        return listsModule;
    }


    private void bucketsTraversable(){
        String service = getService(httpRequest);
        HttpRequestResponse get = Base.sendRequest(service, "GET", null, new ArrayList<>());
        if (get.response().statusCode() <= successCodeRange){
            ByteArray body = get.response().body();
            if (body.countMatches("<ListBucketResult>") != 0 && body.countMatches("<Name>") != 0){
                listsModule.add(new ListsModule(get,"存储桶可遍历","阿里云OSS"));
            }
        }
    }

    private void checkUploadFile(){
        String fileName = "testFileByExt.testFileByExt";
        String service = getService(httpRequest) + "/" + fileName;
        HttpRequestResponse httpRequestResponse = Base.sendRequest(service, "PUT", "test fileUpload", new ArrayList<>());
        if (httpRequestResponse.response().statusCode() <= successCodeRange){
            listsModule.add(new ListsModule(httpRequestResponse,"put文件上传","阿里云OSS"));
        }
    }

    public OSS(HttpRequestResponse baseRequestResponse){
        this.httpRequest = baseRequestResponse.request();
    }


    private void checkObjectAcl(){
        String currentUrl = Base.removedAllParameters(httpRequest);
        String currentBucketAcl = "default";
        HttpRequestResponse get = Base.sendRequest(currentUrl +  "?acl", "GET", null, new ArrayList<>());
        short i = get.response().statusCode();
        if (get.response().statusCode() <= successCodeRange){
            listsModule.add(new ListsModule(get,"ACL可读","阿里OSS"));
        }
        if (Constant.putAcl){
            ArrayList<HttpHeader> headers = new ArrayList<>();
            headers.add(HttpHeader.httpHeader("x-oss-object-acl",currentBucketAcl));
            HttpRequestResponse put = Base.sendRequest(currentUrl + "?acl", "PUT", null, headers);
            if (put.response().statusCode() <= successCodeRange) {
                listsModule.add(new ListsModule(put,"ACL可写","阿里云OSS"));
            }
        }
    }



    private void checkBucketPolicy(){
        if (Constant.policyFlag){
            String service = getService(httpRequest);
            ArrayList<HttpHeader> objects = new ArrayList<>();
            String body = "{\n" +
                    "   \"Version\":\"1\",\n" +
                    "   \"Statement\":[\n" +
                    "   {\n" +
                    "     \"Action\":[\n" +
                    "       \"oss:PutObject\",\n" +
                    "       \"oss:GetObject\"\n" +
                    "    ],\n" +
                    "    \"Effect\":\"Allow\",\n" +
                    "    \"Principal\":[\"1234567890\"],\n" +
                    "    \"Resource\":[\"acs:oss:*:*/*\"]\n" +
                    "   }\n" +
                    "  ]\n" +
                    " }";
            HttpRequestResponse put = Base.sendRequest(service + "/?policy", "PUT", body, objects);
            if (put.response().statusCode() <= successCodeRange){
                listsModule.add(new ListsModule(put,"policy可写","阿里云OSS"));
            }
        }
    }

}
