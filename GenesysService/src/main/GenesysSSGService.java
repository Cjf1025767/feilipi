package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.NotAuthorizedException;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;


import main.TaskCreator.CalloutrecordExtends;

public class GenesysSSGService {
	private static Log log = LogFactory.getLog(GenesysSSGService.class);
	public final static ConcurrentHashMap<String, CompletableFuture<String>> outboundQueue = new ConcurrentHashMap<>();
	public final static String blockMessage="ssg请求阻塞";
	public final static String requestFailMessage="外呼请求发起失败";
	private static int inboudClientTimeoutSeconds = 60;
	public static void Callout() {
		try {
			CalloutrecordExtends recordex = TaskCreator.calloutQueue.poll(1, TimeUnit.SECONDS);
			while(recordex!=null) {
				new Thread(new SyncMakeCallThread(recordex)).start();
				recordex = TaskCreator.calloutQueue.poll(1, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			log.error("ERROR:",e);
		}
	}
	public static String outboundSsg(CalloutrecordExtends recordex) {
		String SSGUrl=Runner.SSGUrl.length()>0?Runner.SSGUrl:StringUtils.EMPTY;
		String NotificationURL=Runner.NotificationURL.length()>0?Runner.NotificationURL:StringUtils.EMPTY;
		int maxretrycount=0;//重试次数
		if(recordex.taskex.trunk!=null) {
			 maxretrycount=recordex.taskex.trunk.getMaxretrycount();
		}
		int timetolive=0;//重试次数
		if(recordex.taskex.trunk!=null) {
			timetolive=recordex.taskex.trunk.getTimetolive();
		}
		HttpPost httpPost = new HttpPost(SSGUrl);
		 String sJsonRpc = String.format("<SSGRequest xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n" + 
		 		"<CreateRequest Token=\"%s\" MaxAttempts=\"%s\" TimeToLive=\"%ss\"\r\n" + 
		 		"IVRProfileName=\"ssg\" Telnum=\"%s\" \r\n" + 
		 		"NotificationURL=\"%s\">\r\n" + 
		 		"<cpd record=\"false\"\r\n" + 
		 		"postconnecttimeout=\"6s\"\r\n" + 
		 		"rnatimeout=\"6s\"\r\n" + 
		 		"preconnect=\"true\"\r\n" + 
		 		"detect=\"all\"/>" +
		 		"</CreateRequest>\r\n" + 
		 		"</SSGRequest>",recordex.getId(),maxretrycount,timetolive,recordex.getPhone(),NotificationURL);
		CloseableHttpClient client = tab.util.Util.getIdleHttpClient(SSGUrl.indexOf("https://")==0?true:false);
		if(client==null) {
			throw new NotAuthorizedException("Invalid CloseableHttpClient");
		}
		try {
			StringEntity sEntity = new StringEntity(sJsonRpc, org.apache.http.Consts.UTF_8);
	        httpPost.setEntity(sEntity);
	        httpPost.setHeader("Content-Type","application/xml");
			HttpResponse res = client.execute(httpPost);
			HttpEntity entity = res.getEntity();
			
			try {
				int nStatusCode = res.getStatusLine().getStatusCode();
				if(nStatusCode<300) {
					log.info( "nStatusCode"+nStatusCode+"==sJsonRpc"+sJsonRpc);
					String sSessionInfo = EntityUtils.toString(entity, org.apache.http.Consts.UTF_8);
					log.info("entitymessage:"+sSessionInfo);
					 try {
						Document doc = DocumentHelper.parseText(sSessionInfo);
						 Element rootsEle = doc.getRootElement();
						 Element roots=rootsEle.element("ResponseElement");
						 if(roots!=null) {
							 System.out.print(roots.getName());
							 if(roots.attributeValue("ResponseType").equals("SUCCESS")){
								 log.info("ssg外呼请求成功"+sSessionInfo);
								 System.out.print(roots.attributeValue("ResponseType"));
								 String requestid=roots.attributeValue("RequestID");
								 return requestid;
							 }
						 }else {
							log.info("请求失败"+sSessionInfo);
						 }
					      
					} catch (DocumentException e) {
						// TODO Auto-generated catch block
						log.error("ERROR:",e);
						e.printStackTrace();
					}
				}else {
					log.error("Invalid StatusCode(" + nStatusCode + ") " + "sJsonRpc"+sJsonRpc);
					throw new NotAuthorizedException("Invalid StatusCode");
				}
			} catch (ParseException e) {
				log.error("ERROR:",e);
				throw new NotAuthorizedException("Invalid ParseException");
			} 
			catch (IOException e) {
				log.error("ERROR:",e);
				throw new NotAuthorizedException("Invalid IOException");
			}
		
		   }catch(IOException e){
				log.error("ERROR:",e);
    			throw new NotAuthorizedException("Invalid IOException");
			
		   }
		return "";
	}
	public static class SyncMakeCallThread implements Runnable {
		private CalloutrecordExtends recordex;
		public SyncMakeCallThread(CalloutrecordExtends recordex){
			this.recordex = recordex;
		}
		@Override
		public void run() {
				String requestid = "";
				//如果成功返回请求id失败返回""
				requestid=outboundSsg(recordex); 
				CallTask.CallStat(recordex.getId(),requestid);
				
//				if(requestid=="") {
//					log.error(recordex.getPhone() + ": 外呼失败, recordid=" + recordex.getId());
//					CallTask.CallStat(recordex.getId(), requestid);
//				}else {//todo 等待外呼通知
//					//CallTask.CallStat(recordex.getId(), null,recordex.taskex.trunk.getTrunk(), "外呼请求发起成功",CallTask.STATUS_COMPLETE,requestid);
//					
//					final CompletableFuture<String> outbound = new CompletableFuture<String>();
//					outboundQueue.put(requestid, outbound);
//					String result="";
//					if (outbound != null) {
//						try {
//							 result=outbound.get(inboudClientTimeoutSeconds, TimeUnit.SECONDS);
//							 log.info(requestid+"的外呼结果为:"+result);
//						} catch (Throwable e) {
//							log.debug(e.toString());
//						} finally {
//							if(result=="") {//说明三分钟之内都没有调用GetNotifition接口  则当做ssg服务端挂了
//								CallTask.CallStat(recordex.getId(),requestid);
//							}
//							outboundQueue.remove(requestid);
//						}
//					}
			//	}
//				switch(nstatus) {
//				case 3://无空闲通道
//					log.error(recordex.getPhone() + ": 无空闲通道, recordid=" + recordex.getId());
//					CallTask.CallStat(recordex.getId(), null,recordex.taskex.trunk.getTrunk(), recordex.taskex.trunk.getName(),CallTask.STATUS_FAILED);
//					break;
//				case 1://ERROR
//					log.error(recordex.getPhone() + ": 外呼失败, recordid=" + recordex.getId());
//					CallTask.CallStat(recordex.getId(),null, recordex.taskex.trunk.getTrunk(), recordex.taskex.trunk.getName(),CallTask.STATUS_FAILED);
//					break;
//				case 2://外呼成功
//		        	CallTask.CallStat(recordex.getId(), null,recordex.taskex.trunk.getTrunk(), recordex.taskex.trunk.getName(),CallTask.STATUS_COMPLETE);
//		        	break;
//				}
		}
	}
}
