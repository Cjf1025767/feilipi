package tab.oauth2;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import hbm.factory.HibernateSessionFactory;
import hbm.model.Rbacroleinfo;
import hbm.model.Rbactoken;
import hbm.model.Rbacuserauths;
import tab.util.Util;
// (value = "鉴权接口")
@Path("/oauth2")
public class OAuth2System{
	/* OAuth2 四种模式
    授权码模式（authorization code）
    简化模式（implicit）
    密码模式（resource owner password credentials）
    客户端模式（client credentials）
    
    设备授权码（device code）
    刷新授权码  (refresh Token)

	 1）授权码模式：
	 client_id程序跳转到下面的url(get请求)
	 /authorize?response_type=code&client_id=s6BhdRkqt3&state=xyz&redirect_uri=https%3A%2F%2Fclient%2Eexample%2Ecom%2Fcb
	 该url显示授权页面，用户界面点击授权，通过授权后，跳转redirect_uri，并且传入state参数和code参数
	 Location: https://client.example.com/cb?code=SplxlOBeZQQYbYS6WxSbIA&state=xyz
	 该redirect_uri页面使用code参数再次请求授权url(post请求)
	 grant_type=authorization_code&code=SplxlOBeZQQYbYS6WxSbIA&redirect_uri=https%3A%2F%2Fclient%2Eexample%2Ecom%2Fcb
	 该url返回json格式,redirect_uri必须和第一步相同,只是验证不跳转
	 HTTP/1.1 200 OK
     Content-Type: application/json;charset=UTF-8
     Cache-Control: no-store
     Pragma: no-cache
     {
       "access_token":"2YotnFZFEjr1zCsicMWpAA",
       "token_type":"example",
       "expires_in":3600,
       "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA",
       "example_parameter":"example_value"
     }

	 2）简化模式
	 /authorize?response_type=token&client_id=s6BhdRkqt3&state=xyz&redirect_uri=https%3A%2F%2Fclient%2Eexample%2Ecom%2Fcb
	 该url显示授权页面，用户界面点击授权，通过授权后，跳转redirect_uri，并且传入state参数和access_token参数
	 Location: http://example.com/cb#access_token=2YotnFZFEjr1zCsicMWpAA&state=xyz&token_type=example&expires_in=3600state=xyz&token_type=example&expires_in=3600
	 3）密码模式
	 grant_type=password&username=johndoe&password=A3ddj3w
	 该url返回json格式同上（post请求）
	 4）客户端模式
	 grant_type=client_credentials
	 该url返回json格式同上（post请求）
	  
	 刷新授权码
	 grant_type=refresh_token&refresh_token=tGzv3JOkF0XG5Qx2TlKWIA
	 该url返回json格式同上（post请求）
	 */
	public static Log logger = LogFactory.getLog(OAuth2System.class);
	public static final long CodeExpiresTime = 10 * 60 * 1000;
	public static final long TokenExpiresTime = 2 * 60 * 60 * 1000;
	public static final long TokenMaxTimes = Integer.MAX_VALUE;//设定期间可以请求token次数
	public static final long TokenMaxTimesDuration = 24 * 60 * 60 * 1000;
	//API文档: (value = "获取授权",notes = "该请求显示授权页面，用户界面点击授权，通过授权后，跳转redirect_uri，redirect_uri在白名单列表，无授权页面直接跳转</br>授权模式token类型可用于门户网站跳转子模块, 跳转时,将登录的用户标识作为uid参数传入，子模块应该使用access_token验证是否有效")
	@GET
	@Path("/authorize")
	@Produces(MediaType.TEXT_HTML + ";charset=utf-8")
	public Response Authorize(@Context HttpServletRequest request,
			@QueryParam("response_type") String sResponseType,
			@QueryParam("state") String sState,
			@QueryParam("redirect_uri") /*用户授权成功后跳转的网址</br>成功跳转后,code模式会带入code=xxxxxx&state=用户数据.</br>成功跳转后,token模式会带入access_token=xxxxxx&state=用户数据"*/String sRedirectUri,
			@QueryParam("appid")  String sAppId,
			@QueryParam("secret")  String sSecret
			) throws IOException {
		logger.info("输出日志检查参数:sResponseType:"+sResponseType+"state:"+sState+"redirect_uri:"+sRedirectUri+"appid:"+sAppId+"secret:"+sSecret);
		if(sResponseType==null) {
			return Response.status(400).entity("response_type is null!").build();
		}
		if(sRedirectUri==null) {
			return Response.status(400).entity("redirect_uri is null!").build();
		}
		String sAccessToken =  null;
		tab.configServer.ValueString vsRedirectUrl = new tab.configServer.ValueString(sRedirectUri);
		switch(sResponseType) {
		case "check":
			sAccessToken = tab.oauth2.OAuth2System.CheckAppIdToken(sAppId,sSecret,StringUtils.EMPTY);
			if(sAccessToken!=null) {
				if(sState!=null && sState.length()>0) {
					return Response.status(200).entity("{\"roleid\":\""+sAccessToken+"\",\"uid\":\""+ 
							CheckUserGuid(sAccessToken,sState,null) +"\"}").build();
				}
				return Response.status(200).entity("{\"roleid\":\""+sAccessToken+"\"}").build();
			}
			return Response.status(200).entity("{\"errcode\":4002}").build();
			//通过appid和secret验证登录并跳转
		case "redirect":
			if(request.getCookies()!=null) {
				String sCookies = StringUtils.EMPTY;
				for(Cookie cookie :  request.getCookies()) {
					if(sCookies.length()>0)sCookies += ";";
					sCookies += cookie.getName() + "=" + cookie.getValue();
				}
				logger.info(sCookies);
			}else {
				logger.info("############# Miss Cookies!!!");
			}
			sAccessToken = tab.oauth2.OAuth2System.CheckAppIdToken(sAppId,sSecret,StringUtils.EMPTY);
			if(sAccessToken!=null) {
				logger.info("sAccessToken:"+sAccessToken);
				if(sState!=null && sState.length()>0) {
					logger.info("sState:"+sState);
					String sUid = CheckUserGuidByagent(sAccessToken,sState,null);
					if(sUid!=null && sUid.length()>0) {
						javax.servlet.http.HttpSession httpsession = request.getSession();
						if(httpsession!=null) {
							httpsession.setAttribute("uid", sUid);
							httpsession.setAttribute("oid", sAccessToken);
						}
						logger.info("sRedirectUri:"+sRedirectUri);
						return Response.seeOther(URI.create(sRedirectUri)).build();
					}else{
						return Response.status(200).entity("{\"errcode\":4003,\"roleid\":\""+sAccessToken+"\",\"state\":"+sState+"\"}").build();
					}
				}else {
					logger.info("sAccessToken:"+sAccessToken+"sState:null");
				}
				return Response.status(200).entity("{\"errcode\":4002,\"roleid\":\""+sAccessToken+"\"}").build();
			}else {
				logger.info("sAccessToken:null");
			}
			return Response.status(200).entity("{\"errcode\":4002}").build();
		case "code"://显示授权页面, 授权后,跳转到redirect_uri，传入code和state，需要使用code再次做post请求获取access_token
			sAccessToken = CreateAccessCode(sAppId,vsRedirectUrl);
			if(sAccessToken!=null && sAccessToken.length()>0) {
				//TODO: 显示确认授权页面, 需要人工点击后，跳转到redirect_uri，传入code和state
				//return "<html><title>TAB OAuth2.0</title><body><h1>unsupport authorization code mode(Require authorization URL)</h1></body></html> ";
				int pos = sRedirectUri.indexOf("?");
				String sUrl = URLDecoder.decode(pos>0 ? sRedirectUri.substring(0,pos) : sRedirectUri, "UTF-8") + "?code=" + sAccessToken;
				if(sState!=null) {
					sUrl += "&state=" + sState;
				}
				return Response.seeOther(URI.create(sUrl)).build();
			}else if(sAccessToken!=null) {
				//返回错误信息
				return Response.status(500).entity(vsRedirectUrl.value).build();
			}
			return Response.status(500).entity("System error").build();
		case "token"://显示授权页面, 授权后,跳转到redirect_uri，直接传入access_token
			sAccessToken = CreateAccessToken(sAppId,sSecret,null,vsRedirectUrl);
			if(sAccessToken!=null && sAccessToken.length()>0) {
				//TODO: 显示确认授权页面, 需要人工点击后，跳转到redirect_uri，传入state/access_token
				//return Response.status(500).entity("<html><title>TAB OAuth2.0</title><body><h1>unsupport authorization token mode(Require authorization URL)</h1></body></html> ").build();
				int pos = sRedirectUri.indexOf("?");
				String sUrl = URLDecoder.decode(pos>0 ? sRedirectUri.substring(0,pos) : sRedirectUri, "UTF-8") + "?access_token=" + sAccessToken;
				sUrl += "&expires_in="+(TokenExpiresTime/1000);
				if(sState!=null) {
					sUrl += "&state=" + sState;
				}
				javax.servlet.http.HttpSession httpsession = request.getSession();
				if(httpsession!=null) {
					if(httpsession.getAttribute("uid")!=null) {
						sUrl += "&uid=" + Util.compressUUID(Util.ObjectToString(httpsession.getAttribute("uid")));
					}
				}
				return Response.seeOther(URI.create(sUrl)).build();
			}else if(sAccessToken!=null) {
				return Response.status(500).entity(vsRedirectUrl.value).build();
			}
			return Response.status(500).entity("System error").build();
		}
		return Response.status(500).entity("<html><title>TAB OAuth2.0</title><body><h1>unkonw response_type="+sResponseType+"</h1></body></html> ").build();
	}
	/*
	 * 用appid和secret申请access_token
	 */
	//API文档: (value = "获取访问码",notes = "用appid和secret申请access_token访问码(两小时有效).</br>grant_type等于client_credentials可直接获取access_token访问码.</br>grant_type等于authorization_code必须使用授权码加上appid和secret")
	@POST
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
	public Response Index(@Context HttpServletRequest request,@Context HttpServletResponse response,
			@FormParam("grant_type") String sGrantType,
			@FormParam("appid")  String sAppId,
			@FormParam("secret")  String sSecret,
			@FormParam("refresh_token") String sRefreshToken,
			@FormParam("code")  String sCode) throws IOException {
		if(sGrantType==null) {
			return Response.status(Response.Status.OK).entity("{\"errcode\":40000,\"errmsg\":\"grant_type is null\"}").build();
		} 
		if(sAppId==null && sRefreshToken==null) {
			return Response.status(Response.Status.OK).entity("{\"errcode\":40000,\"errmsg\":\"appid is null\"}").build();
		} 
		if(sSecret==null && sRefreshToken==null) {
			return Response.status(Response.Status.OK).entity("{\"errcode\":40000,\"errmsg\":\"secret is null\"}").build();
		} 
		tab.configServer.ValueString vsRedirectUrl = new tab.configServer.ValueString("");
		String sAccessToken = null;
		switch(sGrantType) {
		case "authorization_code":
			if(sCode!=null && sCode.length()>0) {
				sAccessToken = CreateAccessToken(sAppId,sSecret,sCode,vsRedirectUrl);
				if(sAccessToken!=null && sAccessToken.length()>0) {
					return Response.status(Response.Status.OK).entity("{\"expires_in\":"+((TokenExpiresTime-CodeExpiresTime)/1000)+",\"access_token\":\""+sAccessToken+"\"}").build();
				}
				return Response.status(Response.Status.OK).entity("{\"errcode\":40002,\"errmsg\":\"failed, Appid("+sAppId+") and "+vsRedirectUrl.value+", authorization_code mode\"}").build();
			}
			return Response.status(Response.Status.OK).entity("{\"errcode\":40000,\"errmsg\":\"failed, code is null, authorization_code mode\"}").build();
		case "password":
			return Response.status(Response.Status.OK).entity("{\"errcode\":40001,\"errmsg\":\"unsupport password mode\"}").build();
		case "refresh_token":
			sAccessToken = RefreshAccessToken(sRefreshToken);
			if(sAccessToken!=null && sAccessToken.length()>0) {
				return Response.status(Response.Status.OK).entity("{\"expires_in\":"+(TokenExpiresTime/1000)+",\"access_token\":\""+sAccessToken+"\"}").build();
			}
			return Response.status(Response.Status.OK).entity("{\"errcode\":40002,\"errmsg\":\"failed, refresh_token\"}").build();
		case "client_credentials":
			sAccessToken = CreateAccessToken(sAppId,sSecret,null,vsRedirectUrl);
			if(sAccessToken!=null && sAccessToken.length()>0) {
				return Response.status(Response.Status.OK).entity("{\"expires_in\":"+(TokenExpiresTime/1000)+",\"access_token\":\""+sAccessToken+"\"}").build();
			}
			return Response.status(Response.Status.OK).entity("{\"errcode\":40002,\"errmsg\":\"failed, Appid("+sAppId+") and "+vsRedirectUrl.value+", client_credentials mode\"}").build();
		}
		return Response.status(Response.Status.OK).entity("{\"errcode\":40001,\"errmsg\":\"unsupport unkonw mode\"}").build();
	}
	//API文档: (value = "检查访问码有效性",notes = "仅提供给资源服务器调用,获取access_token是否有效")
	@POST
	@Path("/token")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
	public Response Token(@Context HttpServletRequest R,@FormParam("access_token") String sAccessToken) {
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try{
				java.util.Date dtNow = Calendar.getInstance().getTime();
				Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where expiresdate>=:expires and id=:token")
						.setTimestamp("expires", dtNow)
						.setString("token", Util.uncompressUUID(sAccessToken)).uniqueResult();
				if(token!=null) {
					dbsession.close();
					return Response.status(Response.Status.OK).entity("{\"expires_in\":"+((token.getExpiresdate().getTime()-dtNow.getTime())/1000)+",\"access_token\":\""+sAccessToken+"\",\"appid\":\""+tab.util.Util.compressUUID(token.getRoleguid())+"\"}").build();
				}
				dbsession.close();
				return Response.status(Response.Status.OK).entity("{\"errcode\":40002,\"errmsg\":\"access token timeout\"}").build();
			}catch(org.hibernate.HibernateException e){
				dbsession.close();
				logger.warn("ERROR:",e);
				return Response.status(200).entity("{\"errcode\":40003,\"errmsg\":\""+e.toString()+"\"}").build();
			}catch(Throwable e){
				dbsession.close();
				logger.warn("ERROR:",e);
				return Response.status(200).entity("{\"errcode\":40003,\"errmsg\":\""+e.toString()+"\"}").build();
			}
		}catch(org.hibernate.HibernateException e){
			logger.warn("ERROR:",e);
			return Response.status(200).entity("{\"errcode\":40003,\"errmsg\":\""+e.toString()+"\"}").build();
		}catch(Throwable e){
			logger.warn("ERROR:",e);
			return Response.status(200).entity("{\"errcode\":40003,\"errmsg\":\""+e.toString()+"\"}").build();
		}
	}
	//API文档: (value = "检查访问码有效性",notes = "仅提供给资源服务器调用,获取access_code是否有效, 并返回access_token和appid")
	@POST
	@Path("/code")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
	public Response Code(@Context HttpServletRequest R,@FormParam("access_code") String sAccessCode) {
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try{
				java.util.Date dtNow = Calendar.getInstance().getTime();
				Long lTokenExpiresdate = dtNow.getTime() - CodeExpiresTime;
				Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where createdate>=:expires and code=:code")
						.setTimestamp("expires", new java.util.Date(lTokenExpiresdate))
						.setString("code", Util.uncompressUUID(sAccessCode)).uniqueResult();
				if(token!=null) {
					dbsession.close();
					return Response.status(Response.Status.OK).entity("{\"expires_in\":"+((token.getExpiresdate().getTime()-dtNow.getTime())/1000)+",\"access_token\":\""+tab.util.Util.compressUUID(token.getId())+"\",\"appid\":\""+tab.util.Util.compressUUID(token.getRoleguid())+"\"}").build();
				}
				dbsession.close();
				return Response.status(Response.Status.OK).entity("{\"errcode\":40002,\"errmsg\":\"access code timeout\"}").build();
			}catch(org.hibernate.HibernateException e){
				dbsession.close();
				logger.warn("ERROR:",e);
				return Response.status(200).entity("{\"errcode\":40003,\"errmsg\":\""+e.toString()+"\"}").build();
			}catch(Throwable e){
				dbsession.close();
				logger.warn("ERROR:",e);
				return Response.status(200).entity("{\"errcode\":40003,\"errmsg\":\""+e.toString()+"\"}").build();
			}
		}catch(org.hibernate.HibernateException e){
			logger.warn("ERROR:",e);
			return Response.status(200).entity("{\"errcode\":40003,\"errmsg\":\""+e.toString()+"\"}").build();
		}catch(Throwable e){
			logger.warn("ERROR:",e);
			return Response.status(200).entity("{\"errcode\":40003,\"errmsg\":\""+e.toString()+"\"}").build();
		}
	}
	//在许可条件内获取AccessToken，如果已经申请Code，则可以使用Code获取AccessToken
	public static String CreateAccessToken(String sAppId,String sSecret,String sCode, tab.configServer.ValueString sRedirectUri)
	{
		String sAccessToken = null;
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try{
				Rbacroleinfo roleinfo = null;
				sAppId = tab.util.Util.uncompressUUID(sAppId);
				roleinfo = (sSecret==null || sSecret.equals(Util.NONE_GUID)) ? null 
						: (Rbacroleinfo)dbsession.createQuery(" from Rbacroleinfo where roleguid=:RoleGuid and secret=:Secret")
						.setString("RoleGuid", sAppId)
						.setString("Secret", tab.util.Util.uncompressUUID(sSecret)).uniqueResult();
				if(roleinfo!=null) {
					if(sRedirectUri!=null && sRedirectUri.value.length()>=0) { 
							if(roleinfo.getRedirecturi()!=null && roleinfo.getRedirecturi().length()>0) {
								String[] adds = roleinfo.getRedirecturi().split(";");
								boolean bMatch = false;
								for(int idx=0;idx<adds.length;idx++) {
									if(sRedirectUri.value.indexOf(adds[idx])>=0) {
										bMatch = true;
										break;
									}
								}
								if(bMatch) {
									//白名单url无需人工点击授权页面
								}else {
									sRedirectUri.value = "";
								}
							}else {
								sRedirectUri.value = "";
							}
					}
					Transaction ts = dbsession.beginTransaction();
					try{
						Rbactoken token = null;
						java.util.Date dtNow = Calendar.getInstance().getTime();
						if(sCode!=null && sCode.length()>0) {
							Long lCodeExpiresdate = dtNow.getTime() - CodeExpiresTime;
							token = (Rbactoken)dbsession.createQuery(" from Rbactoken where roleguid=:appid and code!=:NoneCode and code=:Code and createdate>=:expires")
									.setString("NoneCode", Util.NONE_GUID).setString("Code", Util.uncompressUUID(sCode)).setString("appid", sAppId)
									.setTimestamp("expires", new java.util.Date(lCodeExpiresdate)).uniqueResult();
							if(token!=null) {
								sAccessToken = tab.util.Util.compressUUID(token.getId());
								token.setCode(UUID.randomUUID().toString().toUpperCase());//重置Code，使申请的Code只能使用一次
								dbsession.update(token);
							}else {
								sAccessToken = "";
								sRedirectUri.value = "Code Expired";
							}
						}else {
							Long lTokenExpiresdate = dtNow.getTime() + TokenExpiresTime;
							Long lTokenMaxTimesDuration = dtNow.getTime() - TokenMaxTimesDuration;
							Long lTokenMaxTimes = (Long)dbsession.createQuery("select count(*) from Rbactoken where roleguid=:appid and code!=:NoneCode and expiresdate>=:today")
									.setString("NoneCode", Util.NONE_GUID).setTimestamp("today", new java.util.Date(lTokenMaxTimesDuration)).setString("appid", sAppId).uniqueResult();
							if(lTokenMaxTimes<TokenMaxTimes) {//设定期间可以请求token次数
								token = new Rbactoken();
								token.setId(UUID.randomUUID().toString().toUpperCase());
								token.setExpiresdate(new java.util.Date(lTokenExpiresdate));
								token.setRoleguid(roleinfo.getRoleguid());
								token.setCode(Util.NONE_GUID);
								token.setCreatedate(dtNow);
								dbsession.save(token);
								ts.commit();
								sAccessToken = tab.util.Util.compressUUID(token.getId());
							}else {
								ts.rollback();
								sAccessToken = "";
								sRedirectUri.value = "Too many authentication times";
							}
						}
					}catch(org.hibernate.HibernateException e){
						if ( ts.getStatus() == TransactionStatus.ACTIVE
								|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
							ts.rollback();
						}else ts.commit();
						logger.warn("ERROR:",e);
					}catch(Throwable e){
						if ( ts.getStatus() == TransactionStatus.ACTIVE
								|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
							ts.rollback();
						}else ts.commit();
						logger.warn("ERROR:",e);
					}
				}else {
					sAccessToken = "";
					sRedirectUri.value = "No such Appid";
				}
			}catch(org.hibernate.HibernateException e){
				logger.warn("ERROR:",e);
			}catch(Throwable e){
				logger.warn("ERROR:",e);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			logger.warn("ERROR:",e);
		}catch(Throwable e){
			logger.warn("ERROR:",e);
		}
		return sAccessToken;
	}
	public static String CheckUserGuidByagent(String sRoleId, String agent,String sMD5Password) {
		String sUserGuid = StringUtils.EMPTY;
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try{
				Rbacuserauths authuser = (Rbacuserauths)dbsession.createQuery(" from Rbacuserauths where roleguid=:roleid and agent=:agent")
					.setString("roleid", sRoleId).setString("agent", agent).setFirstResult(0).setMaxResults(1).uniqueResult();
				if(authuser!=null) {
					if(sMD5Password!=null && sMD5Password.length()>0) {
						if(authuser.getPassword().equals(DigestUtils.md5Hex(authuser.getUserguid()+sMD5Password))) {
							sUserGuid = authuser.getUserguid();
						}
					}else {
						sUserGuid = authuser.getUserguid();
					}
				}
			}catch(org.hibernate.HibernateException e){
				logger.warn("ERROR:",e);
			}catch(Throwable e){
				logger.warn("ERROR:",e);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			logger.warn("ERROR:",e);
		}catch(Throwable e){
			logger.warn("ERROR:",e);
		}
		return sUserGuid;
	}
	
	public static String CheckUserGuid(String sRoleId, String sUsername,String sMD5Password) {
		String sUserGuid = StringUtils.EMPTY;
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try{
				Rbacuserauths authuser = (Rbacuserauths)dbsession.createQuery(" from Rbacuserauths where roleguid=:roleid and username=:username")
					.setString("roleid", sRoleId).setString("username", sUsername).setFirstResult(0).setMaxResults(1).uniqueResult();
				if(authuser!=null) {
					if(sMD5Password!=null && sMD5Password.length()>0) {
						if(authuser.getPassword().equals(DigestUtils.md5Hex(authuser.getUserguid()+sMD5Password))) {
							sUserGuid = authuser.getUserguid();
						}
					}else {
						sUserGuid = authuser.getUserguid();
					}
				}
			}catch(org.hibernate.HibernateException e){
				logger.warn("ERROR:",e);
			}catch(Throwable e){
				logger.warn("ERROR:",e);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			logger.warn("ERROR:",e);
		}catch(Throwable e){
			logger.warn("ERROR:",e);
		}
		return sUserGuid;
	}
	public static String CheckAppIdToken(String sAppId,String sSecret, String sCode)
	{
		String sAccessToken = StringUtils.EMPTY;
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try{
				Rbacroleinfo roleinfo = null;
				if(sAppId!=null && sSecret!=null) {
					sAppId = tab.util.Util.uncompressUUID(sAppId);
					roleinfo = (sSecret==null || sSecret.equals(Util.NONE_GUID)) ? null 
							: (Rbacroleinfo)dbsession.createQuery(" from Rbacroleinfo where roleguid=:RoleGuid and secret=:Secret")
							.setString("RoleGuid", sAppId)
							.setString("Secret", tab.util.Util.uncompressUUID(sSecret)).uniqueResult();
					if(roleinfo!=null) {
						sAccessToken = roleinfo.getRoleguid();
					}
				}else if(sCode!=null && sCode.length()>0) {
					java.util.Date dtNow = Calendar.getInstance().getTime();
					Long lTokenExpiresdate = dtNow.getTime() - CodeExpiresTime;
					Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where createdate>=:expires and code=:code")
							.setTimestamp("expires", new java.util.Date(lTokenExpiresdate))
							.setString("code", Util.uncompressUUID(sCode)).uniqueResult();
					if(token!=null) {
						sAccessToken = token.getRoleguid();
					}
				}
			}catch(org.hibernate.HibernateException e){
				logger.warn("ERROR:",e);
			}catch(Throwable e){
				logger.warn("ERROR:",e);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			logger.warn("ERROR:",e);
		}catch(Throwable e){
			logger.warn("ERROR:",e);
		}
		return sAccessToken;
	}
	public static String CheckAppIdToken(String sAppId,String sSecret, tab.rbac.RbacClient.ValueString vUserName)
	{
		String sAccessToken = StringUtils.EMPTY;
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try{
				Rbacroleinfo roleinfo = null;
				if(sAppId!=null && sSecret!=null) {
					sAppId = tab.util.Util.uncompressUUID(sAppId);
					roleinfo = (sSecret==null || sSecret.equals(Util.NONE_GUID)) ? null 
							: (Rbacroleinfo)dbsession.createQuery(" from Rbacroleinfo where roleguid=:RoleGuid and secret=:Secret")
							.setString("RoleGuid", sAppId)
							.setString("Secret", tab.util.Util.uncompressUUID(sSecret)).uniqueResult();
					if(roleinfo!=null) {
						sAccessToken = roleinfo.getRoleguid();
						vUserName.value = roleinfo.getRedirecturi();
					}
				}
			}catch(org.hibernate.HibernateException e){
				logger.warn("ERROR:",e);
			}catch(Throwable e){
				logger.warn("ERROR:",e);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			logger.warn("ERROR:",e);
		}catch(Throwable e){
			logger.warn("ERROR:",e);
		}
		return sAccessToken;
	}
	public static String RefreshAccessToken(String sRefreshToken)
	{
		boolean bSuccess = false;
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try{
				Transaction ts = dbsession.beginTransaction();
				java.util.Date dtNow = Calendar.getInstance().getTime();
				Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where expiresdate>=:expires and id=:token")
						.setTimestamp("expires", dtNow)
						.setString("token", Util.uncompressUUID(sRefreshToken)).uniqueResult();
				if(token!=null) {
					Long lTokenExpiresdate = dtNow.getTime() + TokenExpiresTime;
					token.setExpiresdate(new java.util.Date(lTokenExpiresdate));
					dbsession.update(token);//TODO: 使用插入新记录更安全
					ts.commit();
					bSuccess = true;
				}
			}catch(org.hibernate.HibernateException e){
				logger.warn("ERROR:",e);
			}catch(Throwable e){
				logger.warn("ERROR:",e);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			logger.warn("ERROR:",e);
		}catch(Throwable e){
			logger.warn("ERROR:",e);
		}
		return bSuccess?sRefreshToken:"";
	}
	public static String CreateAccessCode(String sAppId, tab.configServer.ValueString sRedirectUri)
	{
		sAppId = tab.util.Util.uncompressUUID(sAppId);
		String sAccessCode = null;
		if(sRedirectUri!=null && sRedirectUri.value.length()>0) {
			boolean bMatch = false;
			try {
				Session dbsession = HibernateSessionFactory.getThreadSession();
				if(sRedirectUri.value.substring(0,8).equals("https://") || sRedirectUri.value.substring(0,7).equals("http://")) {
					Rbacroleinfo roleinfo = (Rbacroleinfo)dbsession.createQuery(" from Rbacroleinfo where roleguid=:RoleGuid")
							.setString("RoleGuid", sAppId).uniqueResult();	
					if(roleinfo!=null){
						if(roleinfo.getRedirecturi()!=null && roleinfo.getRedirecturi().length()>0) {
							String[] adds = roleinfo.getRedirecturi().split(";");
							for(int idx=0;idx<adds.length;idx++) {
								if(sRedirectUri.value.indexOf(adds[idx])>=0) {
									bMatch = true;
									break;
								}
							}
						}else {
							sAccessCode = "";
							sRedirectUri.value = "Appid("+sAppId+"), miss URLs!";
						}
					}else {
						sAccessCode = "";
						sRedirectUri.value = "No Appid("+sAppId+"), Please check your configuration!";
					}
				}else {
					//如果使用本地路径或者反向代理则不限制url
					Rbacroleinfo roleinfo = (Rbacroleinfo)dbsession.createQuery(" from Rbacroleinfo where roleguid=:RoleGuid")
							.setString("RoleGuid", sAppId).uniqueResult();	
					if(roleinfo!=null){
						bMatch = true;
					}else {
						sAccessCode = "";
						sRedirectUri.value = "No Appid("+sAppId+"), Please check your configuration!";
					}
				}
				if(bMatch) {
					Transaction ts = dbsession.beginTransaction();
					try{
						java.util.Date dtNow = Calendar.getInstance().getTime();
						Long lToneExpiresdate = dtNow.getTime() + TokenExpiresTime;//返回AccessCode同时创建AccessToken
						Rbactoken token = new Rbactoken();
						token.setId(UUID.randomUUID().toString().toUpperCase());
						token.setExpiresdate(new java.util.Date(lToneExpiresdate));
						token.setRoleguid(sAppId);
						token.setCode(UUID.randomUUID().toString().toUpperCase());
						token.setCreatedate(dtNow);
						dbsession.save(token);
						ts.commit();
						sAccessCode = Util.compressUUID(token.getCode());
					}catch(org.hibernate.HibernateException e){
						if ( ts.getStatus() == TransactionStatus.ACTIVE
								|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
							ts.rollback();
						}else ts.commit();
						logger.warn("ERROR:",e);
						sRedirectUri.value = e.toString();
					}catch(Throwable e){
						if ( ts.getStatus() == TransactionStatus.ACTIVE
								|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
							ts.rollback();
						}else ts.commit();
						logger.warn("ERROR:",e);
						sRedirectUri.value = e.toString();
					}
				}else {
					sRedirectUri.value = "Mismatched URL: "+sRedirectUri.value;
				}
				dbsession.close();
			}catch(org.hibernate.HibernateException e){
				logger.warn("ERROR:",e);
			}catch(Throwable e){
				logger.warn("ERROR:",e);
			}
		}else {
			sAccessCode = "";
			sRedirectUri.value = "Requires authorization of URLs!";
		}
		return sAccessCode;
	}
}
