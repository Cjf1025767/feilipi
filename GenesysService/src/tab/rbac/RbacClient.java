package tab.rbac;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.hibernate.Session;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import hbm.factory.HibernateSessionFactory;
import hbm.model.Rbactoken;
import hbm.model.Rbacuserauths;
import main.Runner;
import tab.util.Util;

/*
 * RbacClient模块是服务进程和权限服务对接的模块
 * 通过AppId和Secret授权连接后，可以获取用户的权限列表，可管理的用户标识列表，可管理的组标识
 * 涉及界面显示操作的请求，比如显示用户列表，显示组列表，有两种方式，session共享（建议共用数据库时），通过AppId授权
 * 1）使用session共享，WEB JS客户端直接访问权限服务的/tab/rbac/UI函数
 * 2）通过AppId授权，JAVA 服务端获取数据后，传递给WEB JS客户端，获取的数据，同时受AppId权限约束
 * 
 * RbacClient的使用，调用setRbacUrl，setAppId，setSecret，setSsl初始化
 */
public class RbacClient {
	public static final String ROOT_ROLEGUID = Util.ROOT_ROLEGUID;
	public static Log log = LogFactory.getLog(RbacClient.class);
	private static Object lock = new Object();
	private static ObjectMapper mapper = new ObjectMapper();
	private static Long expiresIn = (long) 0;
	private static String AppId = "";
	private static String Secret = "";
	private static String RbacUrl = "";
	private static int KeepaliveDelay = 360;

	public static int getKeepaliveDelay() {
		return KeepaliveDelay;
	}

	public static void setKeepaliveDelay(int keepaliveDelay) {
		KeepaliveDelay = keepaliveDelay;
	}

	public static Long getExpiresIn() {
		return expiresIn;
	}

	private static String accessToken;

	public static class ValueString {
		public String value;

		public ValueString(String value) {
			this.value = value;
		}
	}

	// 获取用户权限
	public static String getOperations(String sUId) {
		ValueString responseContent = new ValueString("");
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("access_token", getAccessToken());
		if (sUId != null && sUId.length() > 0)
			parameters.put("uid", sUId);
		int nStatusCode = post(RbacUrl + "/tab/rbac/GetOperations", parameters, responseContent, RbacUrl.indexOf("https://")==0?true:false);
		if (nStatusCode < 300) {
			return responseContent.value;
		}
		return "{\"success\":false}";
	}

	// 获取用户信息
	public static String getUserAuths(String sUId, String sUserGuid) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			Rbacuserauths userauth = (Rbacuserauths)dbsession.createQuery(" from Rbacuserauths where userguid=:UserGuid").setString("UserGuid", sUserGuid).uniqueResult();
			if(userauth!=null) {
				result.put("username", userauth.getUsername());
				result.put("userguid", userauth.getUserguid());
				result.put("mobile", userauth.getMobile());
				result.put("identifier", userauth.getIdentifier());
				result.put("email", userauth.getEmail());
				result.put("agent", userauth.getAgent());			
			}else {
				result.put("username", "");
				result.put("userguid", "");
				result.put("mobile", "");
				result.put("identifier", "");
				result.put("email", "");
				result.put("agent", "");
			}
			result.put("success", true);
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", e.toString());
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", e.toString());
		}
		try {
			return mapper.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			log.error("ERROR:",e);
		}
		return "{\"success\":false}";
		/*
		ValueString responseContent = new ValueString("");
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("access_token", getAccessToken());
		parameters.put("uid", sUId);
		if (sUserGuid != null && sUserGuid.length() > 0)
			parameters.put("userguid", sUserGuid);
		int nStatusCode = post(RbacUrl + "/tab/rbac/GetUserAuths", parameters, responseContent, RbacUrl.indexOf("https://")==0?true:false);
		if (nStatusCode < 300) {
			return responseContent.value;
		}
		return "{\"success\":false}";
		*/
	}

	// 修改用户密码
	public static String changeUserPassword(String sUId, String sMD5Password, String sMD5NewPassword) {
		ValueString responseContent = new ValueString("");
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("access_token", getAccessToken());
		parameters.put("uid", sUId);
		parameters.put("password", sMD5Password);
		parameters.put("newpassword", sMD5NewPassword);
		int nStatusCode = post(RbacUrl + "/tab/rbac/ChangeUserPassword", parameters, responseContent, RbacUrl.indexOf("https://")==0?true:false);
		if (nStatusCode < 300) {
			return responseContent.value;
		}
		return "{\"success\":false}";
	}

	// 返回指定用户可管理的用户的GUID列表
	public static Set<String> getUserGuidsForRole(String sUId, String sRoleGuid, Object object, boolean bManageable) {
		ValueString responseContent = new ValueString("");
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("access_token", getAccessToken());
		parameters.put("uid", sUId);
		parameters.put("roleguid", sRoleGuid);
		int nStatusCode = post(RbacUrl + "/tab/rbac/"+(bManageable ? "GetUserGuidsForRoleManageable" : "GetUserGuidsForRole"), parameters, responseContent, RbacUrl.indexOf("https://")==0?true:false);
		if (nStatusCode < 300) {
			java.util.Map<String, Object> data = null;
			try {
				data = mapper.readValue(responseContent.value, new TypeReference<java.util.Map<String, Object>>() {
				});
			} catch (JsonParseException e) {
				log.error("ERROR:", e);
			} catch (JsonMappingException e) {
				log.error("ERROR:", e);
			} catch (IOException e) {
				log.error("ERROR:", e);
			}
			if (data == null || ObjectToBoolean(data.get("success")) == false) {
				log.warn(responseContent);
				return new java.util.HashSet<String>();
			}
			@SuppressWarnings("unchecked")
			java.util.HashSet<String> userGuidList = new java.util.HashSet<String>((java.util.ArrayList<String>)data.get("UserGuids"));
			return userGuidList;
		}
		return new java.util.HashSet<String>();
	}

	// 返回可管理的用户的GUID列表
	public static java.util.List<String> getUserGuids(String sRoleGuid,boolean bManageable) {
		ValueString responseContent = new ValueString("");
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("access_token", getAccessToken());
		parameters.put("roleguid", sRoleGuid);
		int nStatusCode = post(RbacUrl + "/tab/rbac/"+(bManageable ? "GetUserGuidsForRoleManageable" : "GetUserGuidsForRole"), parameters, responseContent, RbacUrl.indexOf("https://")==0?true:false);
		if (nStatusCode < 300) {
			java.util.Map<String, Object> data = null;
			try {
				data = mapper.readValue(responseContent.value, new TypeReference<java.util.Map<String, Object>>() {
				});
			} catch (JsonParseException e) {
				log.error("ERROR:", e);
			} catch (JsonMappingException e) {
				log.error("ERROR:", e);
			} catch (IOException e) {
				log.error("ERROR:", e);
			}
			if (data == null || ObjectToBoolean(data.get("success")) == false) {
				log.warn(responseContent);
				return new java.util.ArrayList<String>();
			}
			@SuppressWarnings("unchecked")
			java.util.List<String> userGuidList = (java.util.List<String>) data.get("UserGuids");
			return userGuidList;
		}
		return new java.util.ArrayList<String>();
	}

	// 返回指定用户可管理的用户的工号列表，bManageable=true返回子组，否则包含本组
	public static java.util.Set<String> getUserAgents(String sUId, String sRoleGuid,boolean bManageable) {
		java.util.HashSet<String> userAgentList = new java.util.HashSet<String>();
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			if(sUId!=null && sUId.length()>0) {
				userAgentList.addAll(RbacSystem.getAuthFieldForRole(sUId,sRoleGuid,dbsession,bManageable,"agent"));
			}else {
				userAgentList.addAll(RbacSystem.getAuthFieldForRole(sRoleGuid,dbsession,bManageable,"agent"));
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
		}
		return userAgentList;
		
	}
	
	// 返回指定用户可管理的用户的工号列表，bManageable=true返回子组，否则包含本组
	public static java.util.Set<String> getUserAgents(String sUId, String sRoleGuid,boolean bManageable,Session dbsession) {
		java.util.HashSet<String> userAgentList = new java.util.HashSet<String>();
		try{
			if(sUId!=null && sUId.length()>0) {
				userAgentList.addAll(RbacSystem.getAuthFieldForRole(sUId,sRoleGuid,dbsession,bManageable,"agent"));
			}else {
				userAgentList.addAll(RbacSystem.getAuthFieldForRole(sRoleGuid,dbsession,bManageable,"agent"));
			}
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
		}
		return userAgentList;
		
	}
	// 返回用户的工号列表，bManageable=true返回子组，否则包含本组
	public static java.util.List<String> getUserAgents(String sRoleGuid,boolean bManageable) {
		ValueString responseContent = new ValueString("");
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("access_token", getAccessToken());
		parameters.put("roleguid", sRoleGuid);
		int nStatusCode = post(RbacUrl + "/tab/rbac/"+(bManageable ? "GetUserAgentsForRoleManageable" : "GetUserAgentsForRole"), parameters, responseContent, RbacUrl.indexOf("https://")==0?true:false);
		if (nStatusCode < 300) {
			java.util.Map<String, Object> data = null;
			try {
				data = mapper.readValue(responseContent.value, new TypeReference<java.util.Map<String, Object>>() {
				});
			} catch (JsonParseException e) {
				log.error("ERROR:", e);
			} catch (JsonMappingException e) {
				log.error("ERROR:", e);
			} catch (IOException e) {
				log.error("ERROR:", e);
			}
			if (data == null || ObjectToBoolean(data.get("success")) == false) {
				log.warn(responseContent);
				return new java.util.ArrayList<String>();
			}
			@SuppressWarnings("unchecked")
			java.util.List<String> userGuidList = (java.util.List<String>) data.get("UserGuids");
			return userGuidList;
		}
		return new java.util.ArrayList<String>();
	}

	// 返回可管理的组的GUID列表
	public static Set<String> getRoleGuidsForRole(String sUId, String sRoleGuid, Object object, boolean bManageable) {
	//public java.util.List<String> getRoleGuids(String sUId, String sRoleGuid) {
		ValueString responseContent = new ValueString("");
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("access_token", getAccessToken());
		parameters.put("uid", sUId);
		parameters.put("roleguid", sRoleGuid);
		int nStatusCode = post(RbacUrl + (bManageable ? "/tab/rbac/GetRoleGuidsForRoleManageable" : "/tab/rbac/GetRoleGuidsForRole"), parameters, responseContent, RbacUrl.indexOf("https://")==0?true:false);
		if (nStatusCode < 300) {
			java.util.Map<String, Object> data = null;
			try {
				data = mapper.readValue(responseContent.value, new TypeReference<java.util.Map<String, Object>>() {
				});
			} catch (JsonParseException e) {
				log.error("ERROR:", e);
			} catch (JsonMappingException e) {
				log.error("ERROR:", e);
			} catch (IOException e) {
				log.error("ERROR:", e);
			}
			if (data == null || ObjectToBoolean(data.get("success")) == false) {
				log.warn("输出返回管理组getRoleGuidsForRole:"+responseContent+"uid为"+sUId+"roleguid为"+sRoleGuid);
				return new java.util.HashSet<String>();
			}
			@SuppressWarnings("unchecked")
			java.util.HashSet<String> userGuidList = new java.util.HashSet<String>((java.util.ArrayList<String>)data.get("RoleGuids"));
			return userGuidList;
		}
		return new java.util.HashSet<String>();
	}

	// 返回可管理的组的GUID列表
	public static Set<String> getRoleGuidsForRole(String sRoleGuid, Object object, boolean bManageable) {
	//public java.util.List<String> getRoleGuids(String sRoleGuid) {
		ValueString responseContent = new ValueString("");
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("access_token", getAccessToken());
		parameters.put("roleguid", sRoleGuid);
		int nStatusCode = post(RbacUrl + (bManageable ? "/tab/rbac/GetRoleGuidsForRoleManageable" : "/tab/rbac/GetRoleGuidsForRole"), parameters, responseContent, RbacUrl.indexOf("https://")==0?true:false);
		if (nStatusCode < 300) {
			java.util.Map<String, Object> data = null;
			try {
				data = mapper.readValue(responseContent.value, new TypeReference<java.util.Map<String, Object>>() {
				});
			} catch (JsonParseException e) {
				log.error("ERROR:", e);
			} catch (JsonMappingException e) {
				log.error("ERROR:", e);
			} catch (IOException e) {
				log.error("ERROR:", e);
			}
			if (data == null || ObjectToBoolean(data.get("success")) == false) {
				log.warn(responseContent);
				return new java.util.HashSet<String>();
			}
			@SuppressWarnings("unchecked")
			java.util.HashSet<String> userGuidList = new java.util.HashSet<String>((java.util.ArrayList<String>)data.get("RoleGuids"));
			return userGuidList;
		}
		return new java.util.HashSet<String>();
	}
	
	// 返回可管理的组的GUID列表
	public static Set<String> getRolesForRole(String sUId, String sRoleGuid, Object object, boolean bManageable) {
	//public java.util.List<String> getRoleGuids(String sUId, String sRoleGuid) {
		ValueString responseContent = new ValueString("");
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("access_token", getAccessToken());
		parameters.put("uid", sUId);
		parameters.put("roleguid", sRoleGuid);
		int nStatusCode = post(RbacUrl + (bManageable ? "/tab/rbac/GetRolesForRoleManageable" : "/tab/rbac/GetRolesForRole"), parameters, responseContent, RbacUrl.indexOf("https://")==0?true:false);
		if (nStatusCode < 300) {
			java.util.Map<String, Object> data = null;
			try {
				data = mapper.readValue(responseContent.value, new TypeReference<java.util.Map<String, Object>>() {
				});
			} catch (JsonParseException e) {
				log.error("ERROR:", e);
			} catch (JsonMappingException e) {
				log.error("ERROR:", e);
			} catch (IOException e) {
				log.error("ERROR:", e);
			}
			if (data == null || ObjectToBoolean(data.get("success")) == false) {
				log.warn(responseContent);
				return new java.util.HashSet<String>();
			}
			@SuppressWarnings("unchecked")
			java.util.HashSet<String> roleList = new java.util.HashSet<String>((java.util.ArrayList<String>)data.get("Roles"));
			return roleList;
		}
		return new java.util.HashSet<String>();
	}

	// 返回可管理的组的GUID列表
	public static Set<String> getRolesForRole(String sRoleGuid, Object object, boolean bManageable) {
	//public java.util.List<String> getRoleGuids(String sRoleGuid) {
		ValueString responseContent = new ValueString("");
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("access_token", getAccessToken());
		parameters.put("roleguid", sRoleGuid);
		int nStatusCode = post(RbacUrl + (bManageable ? "/tab/rbac/GetRolesForRoleManageable" : "/tab/rbac/GetRolesForRole"), parameters, responseContent, RbacUrl.indexOf("https://")==0?true:false);
		if (nStatusCode < 300) {
			java.util.Map<String, Object> data = null;
			try {
				data = mapper.readValue(responseContent.value, new TypeReference<java.util.Map<String, Object>>() {
				});
			} catch (JsonParseException e) {
				log.error("ERROR:", e);
			} catch (JsonMappingException e) {
				log.error("ERROR:", e);
			} catch (IOException e) {
				log.error("ERROR:", e);
			}
			if (data == null || ObjectToBoolean(data.get("success")) == false) {
				log.warn(responseContent);
				return new java.util.HashSet<String>();
			}
			@SuppressWarnings("unchecked")
			java.util.HashSet<String> roleList = new java.util.HashSet<String>((java.util.ArrayList<String>)data.get("Roles"));
			return roleList;
		}
		return new java.util.HashSet<String>();
	}
	
	public static String checkAppIdToken(String sAppId,String sSecret) {
		ValueString responseContent = new ValueString("");
		int nStatusCode = get(RbacUrl + "/tab/oauth2/authorize/?response_type=check&redirect_uri=&appid="+sAppId+"&secret="+sSecret, responseContent, RbacUrl.indexOf("https://")==0?true:false);
		if (nStatusCode < 300) {
			java.util.Map<String, Object> data = null;
			try {
				data = mapper.readValue(responseContent.value,
						new TypeReference<java.util.Map<String, Object>>() {
						});
			} catch (JsonParseException e) {
				log.error("ERROR:", e);
				return "";
			} catch (JsonMappingException e) {
				log.error("ERROR:", e);
				return "";
			} catch (IOException e) {
				log.error("ERROR:", e);
				return "";
			}
			if (data == null || ObjectToNumber(data.get("errcode"), 0) != 0) {
				log.warn(responseContent.value);
				return "";
			}
			return data.get("roleid") == null ? "" : String.valueOf(data.get("roleid"));
		}
		return "";
	}
	public static String getAccessToken() {
		synchronized (lock) {
			if ((System.currentTimeMillis() / 1000) - expiresIn > 0) {
				ValueString responseContent = new ValueString("");
				java.util.Map<String, Object> parameters = new java.util.HashMap<>();
				if (accessToken != null && accessToken.length() > 0) {
					parameters.put("grant_type", "refresh_token");
					parameters.put("refresh_token", accessToken);
				} else {
					parameters.put("grant_type", "client_credentials");
					parameters.put("appid", AppId);
					parameters.put("secret", Secret);
				}
				int nStatusCode = post(RbacUrl + "/tab/oauth2", parameters, responseContent, RbacUrl.indexOf("https://")==0?true:false);
				if (nStatusCode < 300) {
					java.util.Map<String, Object> data = null;
					try {
						data = mapper.readValue(responseContent.value,
								new TypeReference<java.util.Map<String, Object>>() {
								});
					} catch (JsonParseException e) {
						log.error("ERROR:", e);
						return "";
					} catch (JsonMappingException e) {
						log.error("ERROR:", e);
						return "";
					} catch (IOException e) {
						log.error("ERROR:", e);
						return "";
					}
					if (data == null || ObjectToNumber(data.get("errcode"), 0) != 0) {
						log.warn(responseContent.value);
						accessToken = "";
						return "";
					}
					accessToken = data.get("access_token") == null ? "" : String.valueOf(data.get("access_token"));
					expiresIn = (System.currentTimeMillis() / 1000) + ObjectToNumber(data.get("expires_in"), 0)
							- KeepaliveDelay;
				}
			}
			return accessToken;
		}
	}

	public static String getAccessToken(String sCode) {
		synchronized (lock) {
			ValueString responseContent = new ValueString("");
			java.util.Map<String, Object> parameters = new java.util.HashMap<>();
			parameters.put("grant_type", "authorization_code");
			parameters.put("appid", AppId);
			parameters.put("secret", Secret);
			parameters.put("code", sCode);
			int nStatusCode = post(RbacUrl + "/tab/oauth2", parameters, responseContent, RbacUrl.indexOf("https://")==0?true:false);
			if (nStatusCode < 300) {
				java.util.Map<String, Object> data = null;
				try {
					data = mapper.readValue(responseContent.value, new TypeReference<java.util.Map<String, Object>>() {
					});
				} catch (JsonParseException e) {
					log.error("ERROR:", e);
					return "";
				} catch (JsonMappingException e) {
					log.error("ERROR:", e);
					return "";
				} catch (IOException e) {
					log.error("ERROR:", e);
					return "";
				}
				if (data == null || ObjectToNumber(data.get("errcode"), 0) != 0) {
					expiresIn = 0L;
					log.warn(responseContent);
					return "";
				}
				accessToken = data.get("access_token") == null ? "" : String.valueOf(data.get("access_token"));
				expiresIn = (System.currentTimeMillis() / 1000) + ObjectToNumber(data.get("expires_in"), 0)
						- KeepaliveDelay;
			}
			return accessToken;
		}
	}

	/*
	 * 返回APPID
	 */
	public static String checkAccessCode(String sAccessCode) {
		String sAppId = "";
		ValueString responseContent = new ValueString("");
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("access_code", sAccessCode);
		int nStatusCode = post(RbacUrl + "/tab/oauth2/code", parameters, responseContent, RbacUrl.indexOf("https://")==0?true:false);
		if (nStatusCode < 300) {
			java.util.Map<String, Object> data = null;
			try {
				data = mapper.readValue(responseContent.value, new TypeReference<java.util.Map<String, Object>>() {
				});
			} catch (IOException e) {
				log.error("ERROR:", e);
			}
			if (data == null || ObjectToNumber(data.get("errcode"), 0) != 0) {
				log.warn(responseContent.value);
				return sAppId;
			}
			sAppId = data.get("appid") == null ? "" : String.valueOf(data.get("appid"));
			if (AppId.length() == 0 && sAppId.length() > 0) {
				accessToken = data.get("access_token") == null ? "" : String.valueOf(data.get("access_token"));
				expiresIn = (System.currentTimeMillis() / 1000) + ObjectToNumber(data.get("expires_in"), 0)
						- KeepaliveDelay;
			}
			return sAppId;
		}
		return sAppId;
	}
	/*
	 * 返回APPID
	 */
	public static String checkAccessToken(String sAccessToken) {
		String sAppId = "";
		ValueString responseContent = new ValueString("");
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("access_token", sAccessToken);
		int nStatusCode = post(RbacUrl + "/tab/oauth2/token", parameters, responseContent, RbacUrl.indexOf("https://")==0?true:false);
		if (nStatusCode < 300) {
			java.util.Map<String, Object> data = null;
			try {
				data = mapper.readValue(responseContent.value, new TypeReference<java.util.Map<String, Object>>() {
				});
			} catch (IOException e) {
				log.error("ERROR:", e);
			}
			if (data == null || ObjectToNumber(data.get("errcode"), 0) != 0) {
				log.warn(responseContent.value);
				return sAppId;
			}
			sAppId = data.get("appid") == null ? "" : String.valueOf(data.get("appid"));
			if (AppId.length() == 0 && sAppId.length() > 0) {
				accessToken = data.get("access_token") == null ? "" : String.valueOf(data.get("access_token"));
				expiresIn = (System.currentTimeMillis() / 1000) + ObjectToNumber(data.get("expires_in"), 0)
						- KeepaliveDelay;
			}
			return sAppId;
		}
		return sAppId;
	}
	private static SSLContext sslContext = null;
	private static PoolingHttpClientConnectionManager clientConnectionMgr = null;
	private static IdleConnectionMonitorThread idleConnection = null;
	private static HttpClientBuilder sslclientBuilder = null;
	private static HttpClientBuilder clientBuilder = null;

	public static class IdleConnectionMonitorThread extends Thread {

		private final HttpClientConnectionManager connMgr;
		private volatile boolean shutdown;

		public IdleConnectionMonitorThread(HttpClientConnectionManager connMgr) {
			super();
			this.connMgr = connMgr;
		}

		@Override
		public void run() {
			try {
				while (!shutdown) {
					synchronized (this) {
						wait(5000);
						// Close expired connections
						connMgr.closeExpiredConnections();
						// Optionally, close connections
						// that have been idle longer than 30 sec
						connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
					}
				}
			} catch (InterruptedException ex) {
				// terminate
			}
		}

		public void shutdown() {
			shutdown = true;
			synchronized (this) {
				notifyAll();
			}
		}
	}

	private static synchronized CloseableHttpClient getIdleHttpClient(boolean bSsl) {
		if (sslContext == null) {
			try {
				sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
					public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
						return true;
					}
				}).build();
			} catch (KeyManagementException e) {
				e.printStackTrace();
				return null;
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				return null;
			} catch (KeyStoreException e) {
				e.printStackTrace();
				return null;
			}
		}
		if (clientConnectionMgr == null) {
			HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
			SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", sslSocketFactory).build();
			clientConnectionMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			clientConnectionMgr.setMaxTotal(200);
			clientConnectionMgr.setDefaultMaxPerRoute(20);
			idleConnection = new IdleConnectionMonitorThread(clientConnectionMgr);
			idleConnection.start();
		}
		if (bSsl) {
			if (sslclientBuilder == null) {
				sslclientBuilder = HttpClientBuilder.create().setSSLContext(sslContext)
						.setConnectionManager(clientConnectionMgr).disableCookieManagement().disableConnectionState();
			}
			return sslclientBuilder.build();
		}
		if (clientBuilder == null) {
			clientBuilder = HttpClientBuilder.create().disableCookieManagement().disableConnectionState();
		}
		return clientBuilder.build();
	}

	public static int post(String url, java.util.Map<String, Object> postEntity, ValueString responseContent,
			boolean bSsl) {
		int nStatusCode = 500;
		try {
			CloseableHttpClient client = getIdleHttpClient(bSsl);
			try {
				log.info("INFO:" + url);
				HttpPost httpPost = new HttpPost(url);
				log.info("INFO:" + postEntity);
				java.util.List<NameValuePair> nvps = new java.util.ArrayList<NameValuePair>();
				if (postEntity != null) {
					for (java.util.Map.Entry<String, Object> entry : postEntity.entrySet()) {
						if (!(entry == null || entry.getKey() == null || entry.getValue() == null)) {
							nvps.add(new BasicNameValuePair(entry.getKey().toString(), entry.getValue().toString()));
						}
					}
				}
				httpPost.setEntity(new UrlEncodedFormEntity(nvps, org.apache.http.Consts.UTF_8));
				httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
				HttpResponse res = client.execute(httpPost);
				HttpEntity entity = res.getEntity();
				try {
					responseContent.value = EntityUtils.toString(entity, org.apache.http.Consts.UTF_8);
					nStatusCode = res.getStatusLine().getStatusCode();
				} catch (ParseException e) {
					log.error("ERROR:", e);
				} catch (IOException e) {
					log.error("ERROR:", e);
				}
			} catch (ClientProtocolException e) {
				log.error("ERROR:", e);
			}
			client.close();
		} catch (IOException e) {
			log.error("ERROR:", e);
		}
		return nStatusCode;
	}
	public static int get(String url, ValueString responseContent,boolean bSsl)
    {
    	int nStatusCode = 500;
		CloseableHttpClient client = getIdleHttpClient(bSsl);
		if(client==null)return nStatusCode;
	    try
		{
	    	log.info("INFO:"+url);
			HttpGet get = new HttpGet(url);
			HttpResponse res = client.execute(get);
			HttpEntity entity = res.getEntity();
			try {
				responseContent.value = EntityUtils.toString(entity, org.apache.http.Consts.UTF_8);
				nStatusCode = res.getStatusLine().getStatusCode();
			} catch (ParseException e) {
				log.error("ERROR:",e);
			} catch (IOException e) {
				log.error("ERROR:",e);
			}
		}catch(IOException e){
			log.error("ERROR:",e);
		}
    	return nStatusCode;
    }

	public static Integer ObjectToNumber(Object obj, Integer nDefault) {
		if (obj == null)
			return nDefault;
		try {
			return Integer.parseInt(String.valueOf(obj));
		} catch (java.lang.NumberFormatException e) {
			return nDefault;
		}
	}

	public static boolean ObjectToBoolean(Object obj) {
		if (obj == null)
			return false;
		try {
			return Boolean.parseBoolean(String.valueOf(obj));
		} catch (java.lang.NumberFormatException e) {
			return false;
		}
	}

	public static String getAppId() {
		return AppId;
	}

	public static void setAppId(String appId) {
		AppId = appId;
	}

	public static String getSecret() {
		return Secret;
	}

	public static void setSecret(String secret) {
		Secret = secret;
	}

	public static String getRbacUrl() {
		return RbacUrl;
	}

	public static void setRbacUrl(String rbacUrl) {
		RbacUrl = rbacUrl;
	}
}
