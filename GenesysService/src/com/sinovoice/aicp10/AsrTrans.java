package com.sinovoice.aicp10;

import java.util.HashMap;
import org.json.JSONObject;

public class AsrTrans {
  private final HciSdk sdk;

  public AsrTrans(HciSdk sdk) { this.sdk = sdk; }
  public JSONObject submit(String property, JSONObject tasks) {
    String url =
        "cap:/v10/asr/trans/" + property + "/submit?appkey=" + sdk.getAppkey();
    url = this.sdk.sdkUrl(url);
    HashMap<String, String> headers = new HashMap<String, String>();
    headers.put("Content-Type", "application/json");
    App.log.info("输出调用参数:url:"+url+",tasks:"+tasks.toString());
    return this.sdk.doHttp("POST", url, 30000, HciSdk.AUTH_TOKEN, headers,
                           tasks);
  }
  public JSONObject query(String property, String task_id) {
    String url = "cap:/v10/asr/trans/" + property +
                 "/query?appkey=" + sdk.getAppkey() + "&task=" + task_id;
    url = this.sdk.sdkUrl(url);
    HashMap<String, String> headers = new HashMap<String, String>();
    headers.put("Content-Type", "application/json");
    return this.sdk.doHttp("GET", url, 30000, HciSdk.AUTH_TOKEN, headers, null);
  }
  public JSONObject cancel(String property, String task_id) {
    String url = "cap:/v10/asr/trans/" + property +
                 "/cancel?appkey=" + sdk.getAppkey() + "&task=" + task_id;
    url = this.sdk.sdkUrl(url);
    HashMap<String, String> headers = new HashMap<String, String>();
    headers.put("Content-Type", "application/json");
    return this.sdk.doHttp("GET", url, 30000, HciSdk.AUTH_TOKEN, headers, null);
  }
  public JSONObject download(String property, String task_id,String files,String name_style) {
	  //下载json格式文本
	    String url = "cap:/v10/asr/trans/" + property +
	                 "/download?appkey=" + sdk.getAppkey() + "&task=" + task_id+ "&files=" + files;
	    url = this.sdk.sdkUrl(url);
	    HashMap<String, String> headers = new HashMap<String, String>();
	    headers.put("Content-Type", "application/json");
	    return this.sdk.doHttp("GET", url, 30000, HciSdk.AUTH_TOKEN, headers, null);
	  }
  
  public String downloadString(String property, String task_id,String files,String name_style) {
	  //下载text格式文本
	    String url = "cap:/v10/asr/trans/" + property +
	                 "/download?appkey=" + sdk.getAppkey() + "&task=" + task_id+ "&files=" + files;
	    url = this.sdk.sdkUrl(url);
	    HashMap<String, String> headers = new HashMap<String, String>();
	    headers.put("Content-Type", "application/json");
	    return this.sdk.doHttpString("GET", url, 30000, HciSdk.AUTH_TOKEN, headers, null);
	  } 
  
}
