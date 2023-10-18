package tab.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import main.Runner;
import tab.util.getLocalRecordFileLib.Recording.MediaFiles;

public class getLocalRecordFileLib {
	public static Log logger = LogFactory.getLog(getLocalRecordFileLib.class);

	public class EventHistory {
		public EventHistory(){}
	}
	
	
	public class Recording {
		public Recording() {
		}
	
		private String id;
		private String callerPhoneNumber;
		private String dialedPhoneNumber;
		private String startTime;
		private String stopTime;
	
		@SerializedName("mediaFiles")
		private java.util.List<MediaFiles> mediaFiles = new java.util.ArrayList<>();
	
		@SerializedName("eventHistory")
		private java.util.List<EventHistory> eventHistory = new java.util.ArrayList<>();
		
		@SerializedName("region")
		private String region;
		
		public String getId() {
			return id;
		}
	
		public String getCallerPhoneNumber() {
			return callerPhoneNumber;
		}
	
		public String getDialedPhoneNumber() {
			return dialedPhoneNumber;
		}
	
		public String getStartTime() {
			return startTime;
		}
	
		public String getStopTime() {
			return stopTime;
		}
	
		public java.util.List<MediaFiles> getMediaFiles() {
			return mediaFiles;
		}
		
		public java.util.List<EventHistory> getEventHistory() {
			return eventHistory;
		}
	
		public String getRegion() {
			return region;
		}
	
		public class MediaFiles {
			private String startTime;
			private String stopTime;
			private String callUUID;
			private String mediaId;
			private String type;
			private int duration;
			private String mediaUri;
			@SerializedName("parameters")
			private Parameters parameters =new Parameters();
			public class Parameters {
				private String agentId;
			}
			
			
	
			public MediaFiles() {
			}
	
			private Parameters Parameters() {
				// TODO Auto-generated method stub
				return null;
			}

			public String getStartTime() {
				return startTime;
			}
	
			public tab.util.getLocalRecordFileLib.Recording.MediaFiles.Parameters getParameters() {
				return parameters;
			}
			public String getStopTime() {
				return stopTime;
			}
	
			public String getCallUUID() {
				return callUUID;
			}
	
			public String getMediaId() {
				return mediaId;
			}
	
			public String getType() {
				return type;
			}
	
			public int getDuration() {
				return duration;
			}
	
			public String getMediaUri() {
				return mediaUri;
			}
		}
	}
	
	public interface BaseObject {
	}
	
	public class CallIdJsonObject  implements BaseObject {
		private int statusCode;
		
		@SerializedName("recording")
		private Recording recording = new Recording();
		
		public int getStatusCode() {
			return statusCode;
		}
	
		public Recording getRecording() {
			return recording;
		}
	}

	public static File getLocalRecordFile(String sUcid,String sExten,tab.configServer.ValueString AudioFormat,String sPlayUrl,String sPlayHost,String sPlayUsername,String sPlayPassword,String agent) {
		String sAudioFormat = "WAV";
		java.io.File fileHandle = null;
		try {
			java.net.URL url = new java.net.URL(sPlayHost);
			HttpHost target = new HttpHost(url.getHost(), (url.getPort()<0 ? 80 : url.getPort()), url.getProtocol());
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(
					new AuthScope(target.getHostName(), target.getPort()),
					new UsernamePasswordCredentials(sPlayUsername,sPlayPassword));
			CloseableHttpClient httpclient = HttpClients.custom()
					.setDefaultCredentialsProvider(credsProvider)
					.build();
			AuthCache authCache = new BasicAuthCache();
			BasicScheme basicAuth = new BasicScheme();
			authCache.put(target, basicAuth);
			HttpClientContext localContext = HttpClientContext.create();
			localContext.setAuthCache(authCache);
		//	sUcid="01SEPBGCCOA5N2G3MSFQHG5AES000002";
			// 第一次向远程发出请求，获得mediapath路径
			
			//HttpGet httpget = new HttpGet("/api/v2/ops/contact-centers/625a5cf1-dea8-4e46-b18b-831d4b254488/recordings/" + sUcid);
			HttpGet httpget = new HttpGet(Runner.sPlayPath + sUcid);
			logger.info(Runner.sPlayPath  + sUcid);
			//logger.info("getLocalRecordFile:url=="+"/api/v2/ops/contact-centers/625a5cf1-dea8-4e46-b18b-831d4b254488/recordings/" + sUcid);
			// 设置超时时间为1000s
			RequestConfig requestConfig = RequestConfig.custom()
					.setSocketTimeout(1000000).setConnectTimeout(1000000)
					.setConnectionRequestTimeout(8000).build();
			httpget.setConfig(requestConfig);
			
			// 添加响应处理器
			ResponseHandler<CallIdJsonObject> rh = new ResponseHandler<CallIdJsonObject>() {
				@Override
				public CallIdJsonObject handleResponse(HttpResponse response)
						throws HttpResponseException, ClientProtocolException,
						IOException {
					StatusLine statusLine = response.getStatusLine();
					HttpEntity entity = response.getEntity();

					if (statusLine.getStatusCode() > 300) {
						throw new HttpResponseException(
								statusLine.getStatusCode(),
								statusLine.getReasonPhrase());
					}

					if (entity == null) {
						throw new ClientProtocolException(
								"Response contains no message");
					}

					ContentType contentType = ContentType.getOrDefault(entity);
					Charset charset = contentType.getCharset();
					if (charset == null) {
						charset = Charset.forName("UTF-8");
					}

					Gson gson = new GsonBuilder().create();
					Reader reader = new InputStreamReader(entity.getContent(), charset);

					return gson.fromJson(reader, CallIdJsonObject.class);

				}
			};

			logger.warn("Executing request " + httpget.getRequestLine()
					+ " to target " + target);
			
			BaseObject response =  httpclient.execute(target, httpget, rh, localContext);
			
			if (response == null) {
				logger.error("ERROR:");
				return null;
			}
			//获取Genesys的MediaId
			java.util.List<MediaFiles> mList = ((CallIdJsonObject) response).getRecording().getMediaFiles();
			if(mList.size()<=0){
				logger.error("ERROR:查询录音集合为空");
				return null;
			}
			for(int i=0;i<mList.size();i++){
				logger.info("getMediaId["+i+"]:"+mList.get(i).getMediaId());
			}
			String mediaId0 = mList.get(0).getMediaId();
//			if(mList.size()>1) {
//				for(MediaFiles mf:mList){
//					mediaId = mf.getMediaId();
//					int pos = mediaId.lastIndexOf("_");
//					if(pos>32){
//						int end = mediaId.substring(pos+1).indexOf("-");
//						if(end>0){
//							if(mediaId.substring(pos+1,pos+1+end).equals(sExten)){								
//								break;
//							}	
//						}
//					}
//				}
//			}
// 需要通过UTF-8编码才能得到正确的Id
			
			String mediaIdEncoded0 = URLEncoder.encode(mediaId0, "UTF-8");
			//下载保存为临时文件，用第一个语音文件的名字命名
			sAudioFormat = mediaIdEncoded0.toUpperCase().endsWith(".MP3") ? "MP3" : "WAV";
			fileHandle = new java.io.File(System.getProperty("tab.logpath") +  java.util.UUID.randomUUID().toString() + "." + sAudioFormat);
			fileHandle.createNewFile();
			java.io.FileOutputStream fos = new java.io.FileOutputStream(fileHandle);
			//遍历语音文件集合
			Boolean ifagentequals=true;
			if(mList.size()==1) {
					MediaFiles mf=mList.get(0);
					MediaFiles.Parameters par=mf.getParameters();
					logger.info("查询结果只有一段录音的无需工号匹配:工号为"+agent);
						String	mediaId = mf.getMediaId();
						String mediaIdEncoded = URLEncoder.encode(mediaId, "UTF-8");
						httpget = new HttpGet(sPlayUrl + mediaIdEncoded);
						httpget.setConfig(requestConfig);
						CloseableHttpResponse result = httpclient.execute(httpget);
						if (result != null) {
							logger.warn("Executing request " + httpget.getRequestLine()
							+ " to target " + target);
							HttpEntity entity = result.getEntity();
							BufferedHttpEntity buf = new BufferedHttpEntity(entity);						
							logger.warn(ContentType.get(entity).getMimeType());
							try {
								buf.writeTo(fos);
							} catch (Throwable e) {
								logger.error("ERROR:",e);
							} finally {
								EntityUtils.consume(entity);
							}
						}
			}else {
				ifagentequals=false;
				for(MediaFiles mf:mList){
					MediaFiles.Parameters par=mf.getParameters();
					String agentno=par.agentId;
					logger.info("查询坐席工号为："+agent+"返回录音中工号为："+agentno);
					if(agent.equals(agentno)) {
						ifagentequals=true;
						String	mediaId = mf.getMediaId();
						String mediaIdEncoded = URLEncoder.encode(mediaId, "UTF-8");
						httpget = new HttpGet(sPlayUrl + mediaIdEncoded);
						httpget.setConfig(requestConfig);
						CloseableHttpResponse result = httpclient.execute(httpget);
						if (result != null) {
							logger.warn("Executing request " + httpget.getRequestLine()
							+ " to target " + target);
							HttpEntity entity = result.getEntity();
							BufferedHttpEntity buf = new BufferedHttpEntity(entity);						
							logger.warn(ContentType.get(entity).getMimeType());
							try {
								buf.writeTo(fos);
							} catch (Throwable e) {
								logger.error("ERROR:",e);
							} finally {
								EntityUtils.consume(entity);
							}
						}
					}
				}
			}
			
			httpclient.close();
			fos.flush();
			fos.close();
			if(ifagentequals==false) {
				logger.error("查询的录音集合和数据库记录中的工号没有相等的");
				return null;
			}
		} catch (org.apache.http.conn.ConnectTimeoutException e) {
			logger.error(e);
		} catch (java.io.FileNotFoundException e) {
			logger.error(e);
		} catch (java.io.IOException e) {
			logger.error(e);
		} catch (Throwable e) {
			logger.error(e);
		}
		AudioFormat.value = sAudioFormat;
		return fileHandle;
	}
	
	
	public static File getLocalRecordFilepath(String fielpath,tab.configServer.ValueString AudioFormat) {
		String sAudioFormat = "WAV";
		java.io.File fileHandle = null;
		try {
			
			// 设置超时时间为1000s
			logger.info("getLocalRecordFilepath:url=="+fielpath);
			RequestConfig requestConfig = RequestConfig.custom()
					.setSocketTimeout(1000000).setConnectTimeout(1000000)
					.setConnectionRequestTimeout(8000).build();
			HttpGet httpget = new HttpGet(fielpath);
			httpget.setConfig(requestConfig);
			CloseableHttpClient httpclient = HttpClients.custom()
					.build();
			sAudioFormat = fielpath.toUpperCase().endsWith(".MP3") ? "MP3" : "WAV";
			fileHandle = new java.io.File(System.getProperty("tab.logpath") +  java.util.UUID.randomUUID().toString() + "." + sAudioFormat);
			CloseableHttpResponse result = httpclient.execute(httpget);
			if (result != null) {
				HttpEntity entity = result.getEntity();
				BufferedHttpEntity buf = new BufferedHttpEntity(entity);						
				logger.warn(ContentType.get(entity).getMimeType());
				try {
					fileHandle.createNewFile();
					java.io.FileOutputStream fos = new java.io.FileOutputStream(fileHandle);
					buf.writeTo(fos);
					fos.flush();
					fos.close();
				} catch (Throwable e) {
					logger.error("ERROR:",e);
				} finally {
					EntityUtils.consume(entity);
				}
			}
			httpclient.close();
		} catch (org.apache.http.conn.ConnectTimeoutException e) {
			logger.error(e);
		} catch (java.io.FileNotFoundException e) {
			logger.error(e);
		} catch (java.io.IOException e) {
			logger.error(e);
		} catch (Throwable e) {
			logger.error(e);
		}
		AudioFormat.value = sAudioFormat;
		return fileHandle;
	}
}
