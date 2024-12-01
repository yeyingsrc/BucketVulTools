package burp.vendor.huawei;

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
import burp.http.RequestHandler;
import burp.ui.ListsModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OBS implements Base {
    List<ListsModule> listsModule = new ArrayList<>();
    private final HttpRequest httpRequest;
    @Override
    public List<ListsModule> checkVul() {
        IAction[] iActions = {
                this::checkPutObject,
                this::bucketsTraversable,
                this::checkObjectAcl,
                this::checkBucketAcl
        };
        for (IAction iAction : iActions) {
            iAction.execute();
        }
        return listsModule;
    }

    private void checkPutObject(){
        String service = getService(httpRequest);
        HttpRequestResponse httpRequestResponse = Base.sendRequest(service + "/testFileByExt.testFileByExt","PUT","test",new ArrayList<>());
        short i = httpRequestResponse.response().statusCode();
        if (i <= successCodeRange){
            listsModule.add(new ListsModule(httpRequestResponse,"PUT文件上传","华为云OBS"));
        }
    }

    private void checkObjectAcl(){
        String url = httpRequest.url();
        String s = Base.removedAllParameters(HttpRequest.httpRequestFromUrl(url));
        String ownerId = null;
        HttpRequestResponse get = Base.sendRequest(s + "?acl", "GET", null, new ArrayList<>());
        Map<String,String> owner = RequestHandler.parse(get.request().url(), "Owner");
        if (owner != null && !owner.isEmpty()) ownerId = owner.get("ID");
        if (get.response().statusCode() <= successCodeRange){
           listsModule.add(new ListsModule(get,"ACL可读","华为云OBS"));
        }
        if (ownerId != null){
            String body = "<AccessControlPolicy><Owner>" +
                    "<ID>%s</ID>" +
                    "</Owner><Delivered>true</Delivered>" +
                    "<AccessControlList><Grant><Grantee>" +
                    "<ID>%s</ID>" +
                    "</Grantee>" +
                    "<Permission>FULL_CONTROL</Permission>" +
                    "</Grant>" +
                    "</AccessControlList></AccessControlPolicy>";
            HttpRequestResponse put = Base.sendRequest(get.request().url(), "PUT", body.formatted(ownerId, ownerId), new ArrayList<>());
            if (put.response().statusCode() <= successCodeRange){
                listsModule.add(new ListsModule(put,"ACL可写","华为云OBS"));
            }
        }
    }
    private void bucketsTraversable(){
        String service = getService(httpRequest);
        HttpRequestResponse get = Base.sendRequest(service, "GET", null, new ArrayList<>());
        if (get.response().statusCode() <= successCodeRange){
            ByteArray body = get.response().body();
            if (body.countMatches("<Name>") != 0 && body.countMatches("<Contents>") != 0){
                listsModule.add(new ListsModule(get,"存储桶可遍历","华为云OBS"));
            }
        }
    }

    private void checkBucketAcl(){
        String service = getService(httpRequest);
        HttpRequestResponse get = Base.sendRequest(service + "/?acl", "GET", null, new ArrayList<>());
        if (get.response().statusCode() <= successCodeRange){
            ByteArray body = get.response().body();
            if (body.countMatches("<Owner>") != 0 && body.countMatches("<AccessControlList>") != 0){
                listsModule.add(new ListsModule(get,"ACL可读","华为云OBS"));
            }
        }
        ArrayList<HttpHeader> objects = new ArrayList<>();
       if (Constant.putAcl){
           objects.add(HttpHeader.httpHeader("x-obs-acl","public-read-write-delivered"));
           HttpRequestResponse put = Base.sendRequest(get.request().url(), "PUT",null,objects);
           if (put.response().statusCode() <= successCodeRange){
               listsModule.add(new ListsModule(get,"ACL可写","华为云OBS"));
           }
       }
    }

    public OBS(HttpRequestResponse baseRequestResponse) {
        this.httpRequest = baseRequestResponse.request();
    }

}
