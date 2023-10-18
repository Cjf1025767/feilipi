package tab.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sinovoice.aicp10.App;
import com.sinovoice.aicp10.AsrTrans;
import com.sinovoice.aicp10.HciSdk;

import tab.util.Util;


@Path("/genecall")
public class AsrtransServer {
	public static Log log = LogFactory.getLog(AsrtransServer.class);
	@POST
	@Path("/commitTask")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response commitTask(@Context HttpServletRequest R,
			@FormParam("property") String property,@FormParam("file") String filepath,@FormParam("resultType") String resultType,
			@FormParam("notifyUrl") String notifyUrl,@FormParam("sacheckRole") boolean sacheckRole,@FormParam("sachannelRole") String sachannelRole,@FormParam("saoutputVolume") boolean saoutputVolume,@FormParam("saoutputSilence") boolean saoutputSilence) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		HciSdk sdk = new HciSdk( App.sysurl,App.capurl,
				                   "aicp_app", "QWxhZGRpbjpvcGVuIHNlc2FtZQ");
		if(filepath==null||filepath.length()==0) {
			log.info("文件为空:"+filepath);
		}
		 if(resultType==null||resultType.length()==0) {
			   resultType="TXT";
		   } 
	    AsrTrans trans = new AsrTrans(sdk);
	    JSONObject tasks = new JSONObject();
	    // 要转换的音频文件，通过 URL 给出
	    JSONArray files = new JSONArray();
	    files.put(filepath);
	    tasks.put("files", files);
	    tasks.put("audioFormat", "auto");
	    if(notifyUrl!=null&&notifyUrl.length()>0) {
	    	 tasks.put("notifyUrl",
	    			 notifyUrl+"?property="+property+"&resultType="+resultType); // 任务完成后，回调此
	    }else {
	    	 tasks.put("notifyUrl",
		              "http://172.18.9.126:55511/tab/genecall/notification?property="+property+"&resultType="+resultType); // 任务完成后，回调此
	    }
	   
	    tasks.put("resultType", resultType);
	    if(property==null||property.length()==0) {
	    	property="cn_8k_common";
	    }
	    JSONObject saconfig = new JSONObject();
	    saconfig.put("outputSpeed", true);
	    //是否音量检测
	    if(saoutputVolume) {
	    	 saconfig.put("outputVolume", true);
	    }
	    //是否输出静音段数组
	    if(saoutputSilence) {
	    	 saconfig.put("outputSilence", true);
	    }
	    if(sacheckRole) {
	    	 saconfig.put("checkRole", true);
	    }
	    if(sachannelRole!=null&&(sachannelRole.equals("LEFT_AGENT")||sachannelRole.equals("RIGNT_AGENT"))) {
	    	 saconfig.put("channelRole", sachannelRole);
	    }
	    
		 tasks.put("sa", saconfig);
	    JSONObject asresult = trans.submit(property, tasks);
	    String taskid=asresult.getString("taskId");
	    System.out.println(asresult.toString(4));
	    result.put("result", asresult);
	    result.put("taskid", taskid);
	    System.out.println(asresult.toString(4));
	    log.info("提交任务返回结果:"+asresult.toString(4));
		return Response.status(200).entity(asresult.toString(4)).build();
	}
	//查询指定id的任务
	@POST
	@Path("/queryTask")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response queryTask(@Context HttpServletRequest R,
			@FormParam("taskid") String task_id,@FormParam("property") String property) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("taskid",task_id);
		HciSdk sdk = new HciSdk( App.sysurl,App.capurl,
				                   "aicp_app", "QWxhZGRpbjpvcGVuIHNlc2FtZQ");
	    AsrTrans trans = new AsrTrans(sdk);
	    if(property==null||property.length()==0) {
	    	property="cn_8k_common";
	    }
	    JSONObject asresult = trans.query(property, task_id);
	    System.out.println(asresult.toString(4));
	    result.put("result", asresult);
		return Response.status(200).entity(asresult.toString(4)).type("text/plain").build();
	}
	@GET
	@Path("/notification")
	public Response notification(@Context HttpServletRequest R,@QueryParam("task") String task_id,@QueryParam("property") String property,@QueryParam("resultType") String resultType) {
		log.info("进入任务结果通知,任务id为："+task_id);
		log.info("进入任务结果通知,下载格式为："+resultType);
		log.info("进入任务结果通知,转换写模型为："+property);
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("taskid",task_id);
		HciSdk sdk = new HciSdk( App.sysurl,App.capurl,
				                   "aicp_app", "QWxhZGRpbjpvcGVuIHNlc2FtZQ");
	    AsrTrans trans = new AsrTrans(sdk);
	    if(property==null||property.length()==0) {
	    	property="cn_8k_common";
	    }
	    if(resultType!=null&&(resultType.equals("TXT")||resultType.equals("SRT"))) {
	    	String asresult = trans.downloadString(property, task_id,"0",null);
	  	    System.out.println(asresult);
	  	    log.info("notification:返回下载结果text格式："+asresult);
	  	    result.put("result", asresult);
	    }else {
	    	  JSONObject asresult = trans.download(property, task_id,"0",null);
	  	    System.out.println(asresult.toString());
	  	    log.info("notification:返回下载结果json格式："+asresult.toString(4));
	  	    result.put("result", asresult.toString(4));
	    }
	    //todo 将返回报文进行解析和存储处理
	  
		return Response.status(200).entity(result).type("text/plain").build();
	}
	//下载已经转写好的录音文件
	@POST
	@Path("/downloadTask")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response downloadTask(@Context HttpServletRequest R,
			@FormParam("task") String task_id,@FormParam("property") String property,@FormParam("resultType") String resultType) {
		//resultType要和commitask时候的resultType确保一致;
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		HciSdk sdk = new HciSdk( App.sysurl,App.capurl,
				                   "aicp_app", "QWxhZGRpbjpvcGVuIHNlc2FtZQ");
	    AsrTrans trans = new AsrTrans(sdk);
	    if(property==null||property.length()==0) {
	    	property="cn_8k_common";
	    }
	    if(resultType!=null&&(resultType.equals("TXT")||resultType.equals("SRT"))) {
	    	String asresult = trans.downloadString(property, task_id,"0",null);
	  	    System.out.println(asresult);
	  	    log.info("downloadTask:返回下载结果text格式："+asresult);
	  	  return Response.status(200).entity(asresult).type("text/plain").build();
	    }else {
	    	 JSONObject asresult = trans.download(property, task_id,"0",null);
	  	    System.out.println(asresult.toString(4));
	  	    log.info("downloadTask:返回下载结果json格式："+asresult.toString(4));
	  	  return Response.status(200).entity(asresult.toString(4)).type("text/plain").build();
	    }
	}
}
