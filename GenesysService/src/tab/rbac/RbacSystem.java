package tab.rbac;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.owasp.esapi.ESAPI;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import hbm.factory.HibernateSessionFactory;
import hbm.model.Rbacauditlog;
import hbm.model.Rbacoperation;
import hbm.model.Rbacrole;
import hbm.model.RbacroleEx;
import hbm.model.Rbacroleinfoex;
import hbm.model.Rbacroleoperation;
import hbm.model.RbacroleoperationId;
import hbm.model.Rbacroleuser;
import hbm.model.RbacroleuserId;
import hbm.model.Rbactoken;
import hbm.model.Rbacuser;
import hbm.model.Rbacuserauths;
import hbm.model.Recextension;
import main.Runner;
import tab.util.Util;

//(value = "权限接口")
@Path("/rbac")
public class RbacSystem {
	public static Log log = LogFactory.getLog(RbacSystem.class);
	private static ObjectMapper mapper = new ObjectMapper();
	//权限系统根GUID
	public static final String ROOT_ROLEGUID = Util.ROOT_ROLEGUID;
	public static final String OTHER_ROLEGUID = "9A611B6F-5664-4C43-9D06-C1E2141CCCB2";
	public static final String POWER_OPERATIONGUID = "6B46C170-833F-413C-9D90-D887817B3E34";
	public class AuthTypes
	{
	    public static final int AUTH_BASIC = 0; //用户名
	    public static final int AUTH_IDNUMBER = 1;//身份证
	    public static final int AUTH_MOBILE = 2;//手机
	    public static final int AUTH_EMAIL = 3;//邮箱
	    public static final int AUTH_AGENT =4;//工号
	    public static final int AUTH_UID = 5;//系统唯一用户标识
	};
	static final byte[] VERIFYCODE_CHAR_TABLE = { '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
			'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };
	static final byte[] VERIFYCODE_NUMBER_TABLE = { '0','1','2', '3', '4', '5', '6', '7', '8', '9' };
	@SuppressWarnings("unused")
	private static String generateVerifyCode(boolean bDigit) {
		@SuppressWarnings("rawtypes")
		java.util.List list = java.util.Arrays.asList(bDigit?VERIFYCODE_NUMBER_TABLE:VERIFYCODE_CHAR_TABLE);
		java.util.Collections.shuffle(list);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			sb.append(list.get(i));
		}
		return sb.toString().substring(5, 9);
	}
	
	/*
	 * 权限系统RbacUserAuths表，支持从第三方导入账号信息，导入信息必须通过APPID鉴权机制导入，APPID为roleguid字段值
	 * 用户信息与roleguid字段必须具备唯一性，例如roleguid + usernage||agent||email||mobile||identifier不能重复
	 * 权限界面添加账号时，roleguid使用ROOT_ROLEGUID = "9A611B6F-5664-4C43-9D06-C1E2141CCCB1";
	 */
	private class OperationInfo  implements Comparable<OperationInfo>{
		OperationInfo(String name,String guid,boolean bchecked){
			this.operationguid = guid;
			this.operationname = name;
			this.checked = bchecked;
		}
		public String operationname;
		public String operationguid;
		public boolean checked;
		@Override
		public int compareTo(OperationInfo anotherInfo) {
			return this.operationname.compareTo(anotherInfo.operationname);
		}
	}
	/*
	 * 获取该用户管理的组包括子组，顶级组根据bManageable条件确定是否包含
	 */
	public static java.util.Set<String> getRoleGuidsForRole(String sUId,String sRoleGuid,Session session,boolean bManageable)
	{
		java.util.Set<String> RoleSet = new java.util.HashSet<String>();
		boolean bIsRoleGuid = false;
		if (sRoleGuid != null && tab.util.Util.NONE_GUID.equals(sRoleGuid)==false && sRoleGuid.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
			bIsRoleGuid = true;
		}
		boolean bCanAdd = false;
		try {
			Session dbsession =  null;
			if(session==null) dbsession = HibernateSessionFactory.getThreadSession();
			else dbsession =  session;
			try {
				Query query = dbsession.createQuery(
						"select B from Rbacroleuser A, Rbacrole B where A.id.userguid=:UserGuid and A.id.roleguid=B.roleguid");
				query.setString("UserGuid", sUId);
				@SuppressWarnings("unchecked")
				java.util.List<Rbacrole> firstroles = query.list();
				if(bManageable==false){
					if(bCanAdd==false && bIsRoleGuid) {
						for(int idx=0;idx<firstroles.size();++idx)
						{
							if(firstroles.get(idx).getRoleguid().equals(sRoleGuid)){
								Rbacrole firstrole = firstroles.get(idx);
								firstroles.clear();
								firstroles.add(firstrole);
								RoleSet.add(sRoleGuid);
								bCanAdd = true;
								break;
							}
						}
					}else {
						for(int idx=0;idx<firstroles.size();++idx)
						{
							RoleSet.add(firstroles.get(idx).getRoleguid());
						}
					}
				}else if(bManageable) {
					if(bIsRoleGuid) {
						for(int idx=0;idx<firstroles.size();++idx)
						{
							if(firstroles.get(idx).getRoleguid().equals(sRoleGuid)) {
								bCanAdd = true;
								break;
							}
						}
					}else {
						for(int idx=0;idx<firstroles.size();++idx)
						{
							if(firstroles.get(idx).getRoleguid().equals(ROOT_ROLEGUID)) {
								RoleSet.add(ROOT_ROLEGUID);
								break;
							}
						}
					}
				}
				Stack<Rbacrole> firststack = new Stack<Rbacrole>();
				firststack.addAll(firstroles);
				
				while (!firststack.isEmpty()) {
					Rbacrole role = (Rbacrole) firststack.firstElement();
					@SuppressWarnings("unchecked")
					java.util.List<Rbacrole> roles = dbsession.createCriteria(Rbacrole.class)
							.add(Restrictions.eq("fatherroleguid", role.getRoleguid())).list();
					firststack.remove(0);
					firststack.addAll(roles);
					for (int i = 0; i < roles.size(); ++i) {
						if (bCanAdd==false && bIsRoleGuid) {
							if (roles.get(i).getRoleguid().equals(sRoleGuid)) {//遍历子组查找需要的组
								bCanAdd = true;
								firststack.clear();
								firststack.addElement(roles.get(i));
								if(bManageable==false)RoleSet.add(roles.get(i).getRoleguid());
								break;
							}
						} else {
							RoleSet.add(roles.get(i).getRoleguid());
						}
					}
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR: sessionid=" + sUId, e);
			} catch (Throwable e) {
				log.warn("ERROR: sessionid=" + sUId, e);
			}
			if(session==null) dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR: sessionid=" + sUId, e);
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return RoleSet;
	}
	/*
	 * 根据组获取下属所有组
	 */
	public static java.util.Set<String> getRoleGuidsForRole(String sRoleGuid,Session session,boolean bManageable)
	{
		java.util.Set<String> RoleSet = new java.util.HashSet<String>();
		try {
			Session dbsession =  null;
			if(session==null) dbsession = HibernateSessionFactory.getThreadSession();
			else dbsession =  session;
			try {
				Query query = dbsession.createQuery(
						" from Rbacrole where roleguid=:roleguid");
				query.setString("roleguid", sRoleGuid);
				@SuppressWarnings("unchecked")
				java.util.List<Rbacrole> firstroles = query.list();
				if(bManageable==false){
					for(int idx=0;idx<firstroles.size();++idx)
					{
						if(firstroles.get(idx).getRoleguid().equals(sRoleGuid)){
							Rbacrole firstrole = firstroles.get(idx);
							firstroles.clear();
							firstroles.add(firstrole);
							RoleSet.add(sRoleGuid);
							break;
						}
					}
				}else{
					for(int idx=0;idx<firstroles.size();++idx)
					{
						if(firstroles.get(idx).getRoleguid().equals(ROOT_ROLEGUID)) {
							RoleSet.add(ROOT_ROLEGUID);
							break;
						}
					}
				}
				Stack<Rbacrole> firststack = new Stack<Rbacrole>();
				firststack.addAll(firstroles);
				while (!firststack.isEmpty()) {
					Rbacrole role = (Rbacrole) firststack.firstElement();
					@SuppressWarnings("unchecked")
					java.util.List<Rbacrole> roles = dbsession.createCriteria(Rbacrole.class)
							.add(Restrictions.eq("fatherroleguid", role.getRoleguid())).list();
					firststack.remove(0);
					firststack.addAll(roles);
					for (int i = 0; i < roles.size(); ++i) {
						RoleSet.add(roles.get(i).getRoleguid());
					}
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR: ", e);
			} catch (Throwable e) {
				log.warn("ERROR: ", e);
			}
			if(session==null) dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR: ", e);
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return RoleSet;
	}
	/*
	 * 获取该用户管理的本组用户和子组用户的认证信息，本组用户根据bManageable条件确定是否包含
	 */
	public static java.util.Set<String> getAuthFieldForRole(String sUId, String sRoleGuid, Session session,boolean bManageable, String sFieldName)
	{
		java.util.Set<String> authsList = new java.util.HashSet<String>();
		try {
			Session dbsession =  null;
			if(session==null) dbsession = HibernateSessionFactory.getThreadSession();
			else dbsession =  session;
			try {
				java.util.Set<String> sRoleGuids = getRoleGuidsForRole(sUId,sRoleGuid,dbsession,bManageable);
				if(sRoleGuids.size()>0) {
					@SuppressWarnings("unchecked")
					java.util.List<String> list = dbsession.createQuery(
							"select "+sFieldName+" from Rbacuserauths where userguid in (select id.userguid from Rbacroleuser where id.roleguid in (:RoleGuids)) and ( not ("+sFieldName+" is null or "+sFieldName+"=''))")
							.setParameterList("RoleGuids", sRoleGuids).list();
					authsList.addAll(list);
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR: sessionid=" + sUId, e);
			} catch (Throwable e) {
				log.warn("ERROR: sessionid=" + sUId, e);
			}
			if(session==null)dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR: sessionid=" + sUId, e);
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return authsList;
	}
	public static java.util.Set<String> getAuthFieldForRole(String sRoleGuid, Session session,boolean bManageable, String sFieldName)
	{
		java.util.Set<String> authsList = new java.util.HashSet<String>();
		try {
			Session dbsession =  null;
			if(session==null) dbsession = HibernateSessionFactory.getThreadSession();
			else dbsession =  session;
			try {
				java.util.Set<String> sRoleGuids = getRoleGuidsForRole(sRoleGuid,dbsession,bManageable);
				if(sRoleGuids.size()>0) {
					@SuppressWarnings("unchecked")
					java.util.List<String> list = dbsession.createQuery(
							"select "+sFieldName+" from Rbacuserauths where userguid in (select id.userguid from Rbacroleuser where id.roleguid in (:RoleGuids))")
							.setParameterList("RoleGuids", sRoleGuids).list();
					authsList.addAll(list);
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR: " , e);
			} catch (Throwable e) {
				log.warn("ERROR: ", e);
			}
			if(session==null)dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR: ", e);
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return authsList;
	}
	/*
	 * 获取该用户管理的本组用户和子组用户，本组用户根据bManageable条件确定是否包含
	 */
	public static java.util.Set<String> getUserGuidsForRole(String sUId, String sRoleGuid, Session session,boolean bManageable)
	{
		java.util.Set<String> userGuidsList = new java.util.HashSet<String>();
		try {
			Session dbsession =  null;
			if(session==null) dbsession = HibernateSessionFactory.getThreadSession();
			else dbsession =  session;
			try {
				java.util.Set<String> sRoleGuids = getRoleGuidsForRole(sUId,sRoleGuid,dbsession,bManageable);
				if(sRoleGuids.size()>0) {
					@SuppressWarnings("unchecked")
					java.util.List<Rbacuser> users = dbsession.createQuery(
							" from Rbacuser where userguid in (select id.userguid from Rbacroleuser where id.roleguid in (:RoleGuids))")
							.setParameterList("RoleGuids", sRoleGuids).list();
					for (int i = 0; i < users.size(); ++i) {
						userGuidsList.add(users.get(i).getUserguid());
					}
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR: sessionid=" + sUId, e);
			} catch (Throwable e) {
				log.warn("ERROR: sessionid=" + sUId, e);
			}
			if(session==null)dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR: sessionid=" + sUId, e);
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return userGuidsList;
	}
	/*
	 * 根据组获取下属所有用户
	 */
	public static java.util.Set<String> getUserGuidsForRole(String sRoleGuid, Session session,boolean bManageable)
	{
		java.util.Set<String> userGuidsList = new java.util.HashSet<String>();
		try {
			Session dbsession =  null;
			if(session==null) dbsession = HibernateSessionFactory.getThreadSession();
			else dbsession =  session;
			try {
				java.util.Set<String> sRoleGuids = getRoleGuidsForRole(sRoleGuid,dbsession,bManageable);
				if(sRoleGuids.size()>0) {
					@SuppressWarnings("unchecked")
					java.util.List<Rbacuser> users = dbsession.createQuery(
							" from Rbacuser where userguid in (select id.userguid from Rbacroleuser where id.roleguid in (:RoleGuids))")
							.setParameterList("RoleGuids", sRoleGuids).list();
					for (int i = 0; i < users.size(); ++i) {
						userGuidsList.add(users.get(i).getUserguid());
					}
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR: ", e);
			} catch (Throwable e) {
				log.warn("ERROR: ", e);
			}
			if(session==null)dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR: ", e);
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return userGuidsList;
	}
	public static java.util.Set<String> getUidOperations(String sUId,Session session) {
		java.util.Set<String> Operations = new java.util.HashSet<String>();
		try {
			Session dbsession = session==null ? HibernateSessionFactory.getThreadSession() : session;
			try {
				@SuppressWarnings("unchecked")
				java.util.List<String> oplist = dbsession.createQuery("select id.operationguid from Rbacroleoperation where id.roleguid in( select id.roleguid from Rbacroleuser where id.userguid=:UserGuid)").setString("UserGuid", sUId).list();
				if(oplist.size()>0)Operations.addAll(oplist);
			} catch (Throwable e) {
				log.warn("ERROR:",e);
			}
			if(session==null)dbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return Operations;
	}
	/*
	 * 获取该用户的权限，包含本组和子组权限
	 */
	public static java.util.Set<String> getRoleOperations(String sUId,Session session,boolean bIgnoreInheritance) {
		java.util.Set<String> Operations = new java.util.HashSet<String>();
		try {
			Session dbsession = session==null ? HibernateSessionFactory.getThreadSession() : session;
			try {
				Query query = dbsession.createQuery("select B from Rbacroleuser A, Rbacrole B where A.id.userguid=:UserGuid and A.id.roleguid=B.roleguid");
				query.setString("UserGuid", sUId);
				@SuppressWarnings("unchecked")
				java.util.List<Rbacrole> firstroles = query.list();
				Stack<Rbacrole> firststack = new Stack<Rbacrole>();
				firststack.addAll(firstroles);
	
				while (!firststack.isEmpty()) {
					Rbacrole role = (Rbacrole) firststack.firstElement();
					if(role.getRoleguid().equals(ROOT_ROLEGUID)) {
						Operations.add(ROOT_ROLEGUID);
					}
					@SuppressWarnings("unchecked")
					java.util.List<Rbacroleoperation> roleoperations = dbsession
							.createCriteria(Rbacroleoperation.class)
							.add(Restrictions.eq("id.roleguid", role.getRoleguid())).list();
					for (int i = 0; i < roleoperations.size(); ++i) {
						Operations.add(roleoperations.get(i).getId().getOperationguid());
					}
					@SuppressWarnings("unchecked")
					java.util.List<Rbacrole> roles = bIgnoreInheritance
						? dbsession.createCriteria(Rbacrole.class)
							.add(Restrictions.eq("fatherroleguid", role.getRoleguid()))
							.list()
						: dbsession.createCriteria(Rbacrole.class)
						.add(Restrictions.and(Restrictions.eq("fatherroleguid", role.getRoleguid()),
								Restrictions.in("inheritance", Arrays.asList(1,3))))
						.list();
					firststack.remove(0);
					firststack.addAll(roles);
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:",e);
			} catch (Throwable e) {
				log.warn("ERROR:",e);
			}
			if(session==null)dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:",e);
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return Operations;
	}
	private static Map<String, Integer> getRoleOperationReference(String sUId,Session session) {
		Map<String, Integer> mapOperation = new HashMap<>();
		try {
			Session dbsession = session==null ? HibernateSessionFactory.getThreadSession() : session;
			try {
				Query query = dbsession.createQuery("select B from Rbacroleuser A, Rbacrole B where A.id.userguid=:UserGuid and A.id.roleguid=B.roleguid");
				query.setString("UserGuid", sUId);
				@SuppressWarnings("unchecked")
				java.util.List<Rbacrole> firstroles = query.list();
				Stack<Rbacrole> firststack = new Stack<Rbacrole>();
				firststack.addAll(firstroles);
	
				while (!firststack.isEmpty()) {
					Rbacrole role = (Rbacrole) firststack.firstElement();
					if(role.getRoleguid().equals(ROOT_ROLEGUID)) {
						mapOperation.put(ROOT_ROLEGUID, 0);
					}
					@SuppressWarnings("unchecked")
					java.util.List<Rbacroleoperation> roleoperations = dbsession
							.createCriteria(Rbacroleoperation.class)
							.add(Restrictions.eq("id.roleguid", role.getRoleguid())).list();
					for (int i = 0; i < roleoperations.size(); ++i) {
						if(mapOperation.containsKey(roleoperations.get(i).getId().getOperationguid())){
							mapOperation.put(roleoperations.get(i).getId().getOperationguid(), mapOperation.get(roleoperations.get(i).getId().getOperationguid())+1);
						}else {
							mapOperation.put(roleoperations.get(i).getId().getOperationguid(), 1);
						}
					}
					@SuppressWarnings("unchecked")
					java.util.List<Rbacrole> roles = dbsession.createCriteria(Rbacrole.class)
							.add(Restrictions.eq("fatherroleguid", role.getRoleguid()))
							.list();
					firststack.remove(0);
					firststack.addAll(roles);
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:",e);
			} catch (Throwable e) {
				log.warn("ERROR:",e);
			}
			if(session==null)dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:",e);
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return mapOperation;
	}
	//API文档: (value = "获取Base64二维码",notes = "返回success:true|false, 可选返回值:msg(错误信息), data,scan")
	@POST
	@Path("/QRCode")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response QRCode(@Context HttpServletRequest request,
			@FormParam("type") /*"创建二维码的类型值"*/Integer nType,
			@FormParam("typevalue") /*"创建二维码的值"*/String sTypeValue,
			@FormParam("uid") /*"用户ID"*/String sUId,
			@FormParam("data") /*"用户数据"*/String sData,
			@FormParam("width") /*"创建二维码的宽, 缺省43"*/Integer nWidth, 
			@FormParam("height") /*"创建二维码的高, 缺省43"*/Integer nHeight) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if (nType == null) {
			log.error("ERROR: type is null");
			return Response.status(200).entity(result).build();
		}
		if (sTypeValue==null) {
			sTypeValue = Runner.sLoginTypeValue;
		}
		if(sUId==null || sUId.length()==0) {
			sUId = Util.NONE_GUID;
		}
		if(!sUId.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
			result.put("msg", Runner.sErrorMessage);
			log.error("ERROR: "+"invalid uid: " + sUId);
			return Response.status(200).entity(result).build();
		}
		if (nWidth == null || nWidth < 43) {  
			nWidth = 43;  
        }
        if (nHeight == null || nHeight < 43) {  
        	nHeight = nWidth;  
        }	
		if((Runner.nLoginType==nType && Runner.sLoginTypeValue.length()>0) || Runner.nLoginType!=nType) {
			java.util.Map<String, Object> parameters = new java.util.HashMap<>();
			parameters.put("type", nType);
			parameters.put("typevalue", sTypeValue);
			parameters.put("uid", sUId);
			parameters.put("data", sData);
			parameters.put("width", nWidth);
			parameters.put("height", nHeight);
			tab.configServer.ValueString responseContent = new tab.configServer.ValueString("");
			if(StringUtils.isBlank(Runner.sQRCodeUrl )==false && Util.post(Runner.sQRCodeUrl + "/MiniProgram/QRCode", parameters, responseContent)<300) {
				try {
					java.util.Map<String, Object> data = mapper.readValue(responseContent.value, new TypeReference<java.util.Map<String, Object>>(){});
					String sEventCode = Util.ObjectToString(data.get("eventcode"));
					HttpSession httpsession = request.getSession();
					if (httpsession != null) {
						httpsession.setAttribute("eventcode",sEventCode);
						result.put("data", data.get("data"));
						result.put("success", data.get("success"));
						if(sEventCode.length()>0)result.put("eventcode", sEventCode);
						log.info("QRCode: wait="+result.get("wait")+", success="+result.get("success")+", eventcode="+sEventCode);
						return Response.status(200).entity(result).build();
					}
				} catch (IOException e) {
					log.error("ERROR:",e);
				}
			}
		}
		if(Runner.nLoginType==nType) {
			String sBase64 = Util.getImageBase64(Runner.server.getWebRoot()+"/images/qrcodeImage.png");
			if(sBase64.length()>0) {
				result.put("data", sBase64);
				result.put("success", true);
			}
		}
		return Response.status(200).entity(result).build();
	}
	//API文档: (value = "Web登录,等待扫码二维码事件",notes = "登录流程: QRCode->QRCodeWaitScan(Ajax45秒超时)|QRCodeCheckLogin(WeChat)->QRCodeWaitLogin(Ajax45秒超时)|ConfirmLogin(WeChat)->UIRedirect(登录完成), ")
	@POST
	@Path("/QRCodeWaitScan")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public String QRCodeWaitScan(@Context HttpServletRequest request,@Context HttpServletResponse response)
	{
		HttpSession httpsession = request.getSession();
		if(httpsession==null) {
			return "{\"success\":false,\"msg\":\"授权码会话丢失\"}";
		}	
		String sEventCode = Util.ObjectToString(httpsession.getAttribute("eventcode"));
		if(sEventCode.length()==0) {
			return "{\"success\":false,\"msg\":\"授权码丢失\"}";
		}
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("eventcode", sEventCode);
		tab.configServer.ValueString responseContent = new tab.configServer.ValueString("");
		if(Util.post(Runner.sQRCodeUrl + "/MiniProgram/QRCodeWaitScan", parameters, responseContent)<300) {
			return responseContent.value;
		}
		return "{\"success\":false,\"msg\":\"\"}";
	}
	//API文档: (value = "等待扫码后确认登录事件",notes = "收到微信端确认，第二步Ajax调用该接口, 等待用户小程序确认登录, 45秒超时")
	@POST
	@Path("/QRCodeWaitLogin")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public String QRCodeWaitLogin(@Context HttpServletRequest request,@Context HttpServletResponse response)
	{
		HttpSession httpsession = request.getSession();
		if(httpsession==null) {
			return "{\"success\":false,\"msg\":\"授权码会话丢失\"}";
		}
		String sEventCode = Util.ObjectToString(httpsession.getAttribute("eventcode"));
		if(sEventCode.length()==0) {
			return "{\"success\":false,\"msg\":\"授权码丢失\"}";
		}
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("eventcode", sEventCode);
		parameters.put("oid", Runner.sAuthorizedRoleGuid);//网关判断是权限系统根GUID则使用该本地权限系统，返回的roleguid也为根权限GUID
		tab.configServer.ValueString responseContent = new tab.configServer.ValueString("");
		if(Util.post(Runner.sQRCodeUrl + "/MiniProgram/QRCodeWaitLogin", parameters, responseContent)<300) {
			try {
				java.util.Map<String, Object> data = mapper.readValue(responseContent.value, new TypeReference<java.util.Map<String, Object>>(){});
				int nType = Util.ObjectToNumber(data.get("type"), 0);
				if(nType==Runner.nLoginType) {
					Session dbsession = HibernateSessionFactory.getThreadSession();
					try {
						String sUId = (String)dbsession.createQuery("select userguid from Rbacuserauths where weixinid=:weixinid and roleguid=:roleguid")
							.setString("weixinid", Util.ObjectToString(data.get("weixinid"))).setString("roleguid",Util.ObjectToString(data.get("roleguid"))).uniqueResult();
						if(sUId!=null && Util.ObjectToString(data.get("typevalue")).equals(Runner.sLoginTypeValue)) {
							httpsession.setAttribute("oid",Util.ObjectToString(data.get("roleguid")));
							httpsession.setAttribute("uid",sUId);
							dbsession.close();
							return "{\"success\":true,\"msg\":\"微信已确认登录\"}";
						}
						dbsession.close();
						return "{\"success\":false,\"msg\":\"weixinid="+data.get("weixinid")+"\"}";
					} catch (org.hibernate.HibernateException e) {
						log.error("ERROR:",e);
					}
					dbsession.close();
				}else if(nType==Runner.nBindType) {
					Session dbsession = HibernateSessionFactory.getThreadSession();
					try {
						Transaction ts = dbsession.beginTransaction();
						int nRow = dbsession.createQuery("update Rbacuserauths set weixinid=:weixinid where  userguid=:userguid and roleguid=:roleguid")
							.setString("weixinid", Util.ObjectToString(data.get("weixinid"))).setString("roleguid",Util.ObjectToString(data.get("roleguid"))).setString("userguid", Util.ObjectToString(data.get("typevalue"))).executeUpdate();
						ts.commit();
						if(nRow>0) {
							dbsession.close();
							return "{\"success\":true,\"msg\":\"微信已确认绑定\"}";
						}
						dbsession.close();
						return "{\"success\":false,\"msg\":\"weixinid="+data.get("weixinid")+"\"}";
					} catch (org.hibernate.HibernateException e) {
						log.error("ERROR:",e);
					} catch (Throwable e) {
						log.warn("ERROR: ", e);
					}
					dbsession.close();
				}
			} catch (IOException e) {
				log.error("ERROR:",e);
			}
		}
		return "{\"success\":false,\"msg\":\"\"}";
	}
	//API文档: (value = "小程序扫描二维码后首页调用",notes = "控制Web页面更换确认提示,流程：QRCodeCheckLogin(WeChat)->ConfirmLogin(WeChat)")
	@POST
	@Path("/QRCodeCheckLogin")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public String QRCodeCheckLogin(@Context HttpServletRequest request,
			@Context HttpServletResponse response,
			@FormParam("progid")String sProgid,
			@FormParam("authcode")String sScene) throws IOException
	{
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("progid", sProgid);
		parameters.put("authcode", sScene);
		tab.configServer.ValueString responseContent = new tab.configServer.ValueString("");
		if(Util.post(Runner.sQRCodeUrl + "/MiniProgram/QRCodeCheckLogin", parameters, responseContent)<300) {
			return responseContent.value;
		}
		return "{\"success\":false}";
	}
	//API文档: (value = "扫码后确认登录事件",notes = "微信端点击确认调用接口")
	@POST
	@Path("/ConfirmLogin")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public String ConfirmLogin(@Context HttpServletRequest request,
			@Context HttpServletResponse response,
			@FormParam("progid")String sProgid,
			@FormParam("jscode")String sMpCode,
			@FormParam("authcode")String sScene,
			@DefaultValue("") @FormParam("headimgurl")String sHeadImgUrl,
			@DefaultValue("") @FormParam("nickname")String sNickName
			) throws IOException
	{
		java.util.Map<String, Object> parameters = new java.util.HashMap<>();
		parameters.put("progid", sProgid);
		parameters.put("jscode", sMpCode);
		parameters.put("authcode", sScene);
		tab.configServer.ValueString responseContent = new tab.configServer.ValueString("");
		if(Util.post(Runner.sQRCodeUrl + "/MiniProgram/ConfirmLogin", parameters, responseContent)<300) {
			return responseContent.value;
		}
		return "{\"success\":false}";
	}
	/*
	 * 当用户属于多个Role时，判断是否为顶级Role，参数sFatherroleguid为用户所属组的父组, 参数firstroles为用户所有所属组
	 */
	private static boolean noTopRole(String sFatherroleguid,java.util.List<Rbacrole> firstroles,Session dbsession) 
	{
		do {	
			for(Rbacrole l : firstroles) {
				if(sFatherroleguid.equals(l.getRoleguid())) {
					return true;
				}
			}
			//用户所属组可能越级,所以需要确保所有的父亲节点都不在firstroles里
			sFatherroleguid = (String)dbsession.createQuery("select fatherroleguid from Rbacrole where roleguid=:roleguid").setString("roleguid", sFatherroleguid).uniqueResult();
		}while(sFatherroleguid!=null);
		return false;
	}
	/*
	 * 判断用户是否和当前用户有平级组，超级管理员始终不为平级用户忽略该限制
	 */
	public static boolean IsTopUser(String sUId,String sUserGuid,Session dbsession, java.util.Set<String> UIdRoles)
	{
		if(UIdRoles.contains(ROOT_ROLEGUID))return false;
		@SuppressWarnings("unchecked")
		java.util.List<Rbacrole> roles = dbsession.createQuery(
				"select B from Rbacroleuser A, Rbacrole B where A.id.userguid=:uid and A.id.roleguid=B.roleguid and B.roleguid!=:rootguid")
				.setString("uid", sUId).setString("rootguid", ROOT_ROLEGUID).list();
		for(int i=0;i<roles.size();++i) {
			Rbacrole role = roles.get(i);
			if(!noTopRole(role.getFatherroleguid(),roles,dbsession)) {
				if(Util.ObjectToNumber(dbsession.createQuery("select count(*) from Rbacroleuser where id.roleguid=:roleguid and id.userguid=:UserGuid")
					.setString("roleguid", role.getRoleguid()).setString("UserGuid", sUserGuid).uniqueResult(),0)>0){
						return true;
					}
			}
		}
		return false;
	}

	//API文档: (value="获取用户信息", notes = "获取access_token所属组的子组用户和本组用户的权限信息")
	@POST
	@Path("/GetUserAuths")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetUserAuths(@Context HttpServletRequest R,@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") /*"当前用户标识"*/ String sUId,@FormParam("userguid") /*"需要查询的用户标识,忽略则查询当前用户"*/ String sUserGuid){
		log.info("GetUserAuths RemoteAddr() = " + R.getRemoteAddr());
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if(sAccessToken==null || sAccessToken.length()==0 || sUId==null || sUId.length()==0) {
			return Response.status(200).entity(result).build();
		}
		if(sUserGuid==null || sUserGuid.length()==0)sUserGuid = sUId;
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			java.util.Date dtNow = Calendar.getInstance().getTime();
			Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where expiresdate>=:expires and id=:token")
					.setTimestamp("expires", dtNow)
					.setString("token", Util.uncompressUUID(sAccessToken)).uniqueResult();
			if(token!=null) {
				if(token.getRoleguid().equals(Runner.sAuthorizedRoleGuid) || getUserGuidsForRole(token.getRoleguid(),dbsession,false).contains(sUserGuid)){
					Rbacuserauths userauth = (Rbacuserauths)dbsession.createQuery(" from Rbacuserauths where userguid=:UserGuid").setString("UserGuid", sUserGuid).uniqueResult();
					if(userauth!=null) {
						result.put("registerPbxAgent", Runner.bRegisterPbxAgent);
						result.put("username", userauth.getUsername());
						result.put("userguid", userauth.getUserguid());
						result.put("mobile", userauth.getMobile());
						result.put("identifier", userauth.getIdentifier());
						result.put("email", userauth.getEmail());
						result.put("agent", userauth.getAgent());	
						result.put("logintime", userauth.getLogindate().getTime());
					}else {
						result.put("registerPbxAgent", Runner.bRegisterPbxAgent);
						result.put("username", "");
						result.put("userguid", "");
						result.put("mobile", "");
						result.put("identifier", "");
						result.put("email", "");
						result.put("agent", "");
						result.put("logintime", new Date().getTime());
					}
					result.put("success", true);
				}
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", "发现错误请记录时间等信息, 联系管理员查看后台日志");
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", "发现错误请记录时间等信息, 联系管理员查看后台日志");
		}
		return Response.status(200).entity(result).build();
	}
	//API文档: (value="修改用户密码", notes = "可修改access_token所属组的子组用户密码")
	@POST
	@Path("/ChangeUserPassword")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ChangeUserPassword(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") String sUId,
			@FormParam("password") String sPassword,@FormParam("newpassword") String sNewPassword,@FormParam("verifycode") @DefaultValue("") String sVerifycode){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if(sAccessToken==null || sAccessToken.length()==0 || sUId==null || sUId.length()==0) {
			log.error("Parameters cannot be empty!");
			result.put("msg", Runner.sErrorMessage);
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			java.util.Date dtNow = Calendar.getInstance().getTime();
			Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where expiresdate>=:expires and id=:token")
					.setTimestamp("expires", dtNow)
					.setString("token", Util.uncompressUUID(sAccessToken)).uniqueResult();
			if(token!=null) {
				if(token.getRoleguid().equals(Runner.sAuthorizedRoleGuid) || getUserGuidsForRole(token.getRoleguid(),dbsession,false).contains(sUId)){
					Transaction ts = dbsession.beginTransaction();
					Rbacuserauths userauth = (Rbacuserauths)dbsession.createQuery("from Rbacuserauths where userguid=:UserGuid")
							.setString("UserGuid", sUId).setFirstResult(0).setMaxResults(1).uniqueResult();
					tab.configServer.ValueString cause = new tab.configServer.ValueString(StringUtils.EMPTY);
					String sEncryptPassword = Util.encryptPassword(userauth.getUsername(),sUId,sNewPassword,sVerifycode,cause);
					if(sEncryptPassword!=null && sEncryptPassword.length()>0) {
						if(CheckChangePasswordAuditLog(userauth.getUsername(),userauth.getUserguid(),sEncryptPassword,dbsession,cause)) {
							if(userauth!=null && Util.checkPassword(sUId,sPassword, userauth.getPassword(),sVerifycode) &&  dbsession.createQuery("update Rbacuserauths set password=:newpassword where userguid=:UserGuid")
									.setString("UserGuid", sUId).setString("newpassword", sEncryptPassword).executeUpdate()>0) {
								result.put("success", true);
								AuditLog("changepassword", "Rbacuser", userauth.getUserguid(), 1,sEncryptPassword, dbsession,ts,userauth.getUserguid());
								ts.commit();
							}
						}else {
							log.warn(cause.value);
							result.put("msg", Runner.sErrorMessage);
						}
					}else {
						log.warn(cause.value);
						result.put("msg", Runner.sErrorMessage);
					}
				}
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
	}
	//API文档: (value="获取用户可管理的用户标识列表", notes = "用户为access_token所属组的子组用户")
	@POST
	@Path("/GetUserGuidsForRole")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetUserGuidsForRole(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") /*"当前用户标识"*/String sUId,@FormParam("roleguid") /*"需要查询的组标识,忽略则查询当前用户可管理的所有用户标识"*/ String sRoleGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if(sAccessToken==null || sAccessToken.length()==0) {
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			java.util.Date dtNow = Calendar.getInstance().getTime();
			Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where expiresdate>=:expires and id=:token")
					.setTimestamp("expires", dtNow)
					.setString("token", Util.uncompressUUID(sAccessToken)).uniqueResult();
			if(token!=null) {
				if(sUId!=null && sUId.length()>0) {
					result.put("UserGuids", getUserGuidsForRole(sUId,sRoleGuid,dbsession,false));
				}else {
					result.put("UserGuids", getUserGuidsForRole(sRoleGuid,dbsession,false));
				}
				result.put("success", true);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
	}
	//TODO: 暂时无调用
	@POST
	@Path("/GetUserGuidsForRoleManageable")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetUserGuidsForRoleManageable(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") /*"当前用户标识"*/ String sUId,@FormParam("roleguid") /*"需要查询的组标识,忽略则查询当前用户可管理的所有用户标识"*/ String sRoleGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if(sAccessToken==null || sAccessToken.length()==0) {
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			java.util.Date dtNow = Calendar.getInstance().getTime();
			Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where expiresdate>=:expires and id=:token")
					.setTimestamp("expires", dtNow)
					.setString("token", Util.uncompressUUID(sAccessToken)).uniqueResult();
			if(token!=null) {
				if(sUId!=null && sUId.length()>0) {
					result.put("UserGuids", getUserGuidsForRole(sUId,sRoleGuid,dbsession,true));
				}else {
					result.put("UserGuids", getUserGuidsForRole(sRoleGuid,dbsession,true));
				}
				result.put("success", true);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/GetUserAgentsForRole")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetUserAgentsForRole(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") /*"当前用户标识"*/String sUId,@FormParam("roleguid") /*"需要查询的组标识,忽略则查询当前用户可管理的所有用户标识"*/ String sRoleGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if(sAccessToken==null || sAccessToken.length()==0) {
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			java.util.Date dtNow = Calendar.getInstance().getTime();
			Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where expiresdate>=:expires and id=:token")
					.setTimestamp("expires", dtNow)
					.setString("token", Util.uncompressUUID(sAccessToken)).uniqueResult();
			if(token!=null) {
				if(sUId!=null && sUId.length()>0) {
					result.put("Agents", getAuthFieldForRole(sUId,sRoleGuid,dbsession,false,"agent"));
					result.put("Extensions", getAuthFieldForRole(sUId,sRoleGuid,dbsession,true,"mobile"));
				}else {
					result.put("Agents", getAuthFieldForRole(sRoleGuid,dbsession,false,"agent"));
					result.put("Extensions", getAuthFieldForRole(sRoleGuid,dbsession,true,"mobile"));
				}
				result.put("success", true);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
	}
	//API文档: (value="获取用户可管理的用户标识列表", notes = "用户为access_token所属组的子组用户")
	@POST
	@Path("/GetUserAgentsForRoleManageable")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetUserAgentsForRoleManageable(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") /*"当前用户标识"*/ String sUId,
			@FormParam("roleguid") /*"需要查询的组标识,忽略则查询当前用户可管理的所有用户标识"*/ String sRoleGuid,@FormParam("extension") @DefaultValue("false") Boolean bExten){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if(sAccessToken==null || sAccessToken.length()==0) {
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			java.util.Date dtNow = Calendar.getInstance().getTime();
			Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where expiresdate>=:expires and id=:token")
					.setTimestamp("expires", dtNow)
					.setString("token", Util.uncompressUUID(sAccessToken)).uniqueResult();
			if(token!=null) {
				if(sUId!=null && sUId.length()>0) {
					result.put("Agents", getAuthFieldForRole(sUId,sRoleGuid,dbsession,false,"agent"));
					if(bExten)result.put("Extensions", getAuthFieldForRole(sUId,sRoleGuid,dbsession,true,"extension"));
				}else {
					result.put("Agents", getAuthFieldForRole(sRoleGuid,dbsession,false,"agent"));
					if(bExten)result.put("Extensions", getAuthFieldForRole(sRoleGuid,dbsession,true,"extension"));
				}
				result.put("success", true);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
	}
	//API文档: (value="获取用户可管理的组标识列表", notes = "用户为access_token所属组的子组用户")
	@POST
	@Path("/GetRoleGuidsForRoleManageable")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetRoleGuidsForRoleManageable(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") /*"当前用户标识"*/ String sUId,@FormParam("roleguid") /*"需要查询的组标识,忽略则查询当前用户可管理的所有组"*/ String sRoleGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if(sAccessToken==null || sAccessToken.length()==0) {
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			java.util.Date dtNow = Calendar.getInstance().getTime();
			Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where expiresdate>=:expires and id=:token")
					.setTimestamp("expires", dtNow)
					.setString("token", Util.uncompressUUID(sAccessToken)).uniqueResult();
			if(token!=null) {
				if(sUId==null || sUId.length()==0) {
					result.put("RoleGuids", getRoleGuidsForRole(sRoleGuid,dbsession,true));
				}else {
					result.put("RoleGuids", getRoleGuidsForRole(sUId,sRoleGuid,dbsession,true));
				}
				result.put("success", true);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
	}

	//API文档: (value="获取用户可管理的组标识列表", notes = "用户为access_token所属组的子组用户")
	@POST
	@Path("/GetRoleGuidsForRole")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetRoleGuidsForRole(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("roleguid") /*"需要查询的组标识,忽略则查询当前用户可管理的所有组"*/ String sRoleGuid,@FormParam("uid") /*"当前用户标识"*/ String sUId){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if(sAccessToken==null || sAccessToken.length()==0) {
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			java.util.Date dtNow = Calendar.getInstance().getTime();
			Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where expiresdate>=:expires and id=:token")
					.setTimestamp("expires", dtNow)
					.setString("token", Util.uncompressUUID(sAccessToken)).uniqueResult();
			if(token!=null) {
				if(sUId==null || sUId.length()==0) {
					result.put("RoleGuids", getRoleGuidsForRole(sRoleGuid,dbsession,false));
				}else {
					result.put("RoleGuids", getRoleGuidsForRole(sUId,sRoleGuid,dbsession,false));
				}
				result.put("success", true);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
	}
	
	@POST
	@Path("/GetRoleNamesManageable")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetRoleNamesManageable(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") /*"当前用户标识"*/ String sUId,@FormParam("roleguid") /*"需要查询的组标识,忽略则查询当前用户可管理的所有组"*/ String sRoleGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if(sAccessToken==null || sAccessToken.length()==0) {
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			java.util.Date dtNow = Calendar.getInstance().getTime();
			Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where expiresdate>=:expires and id=:token")
					.setTimestamp("expires", dtNow)
					.setString("token", Util.uncompressUUID(sAccessToken)).uniqueResult();
			if(token!=null) {
				if(sUId==null || sUId.length()==0) {
					result.put("Roles", dbsession.createQuery("select rolename from Rbacrole where roleguid in (:roleguid)").setParameterList("roleguid", getRoleGuidsForRole(sRoleGuid,dbsession,true)).list());
				}else {
					result.put("Roles", dbsession.createQuery("select rolename from Rbacrole where roleguid in (:roleguid)").setParameterList("roleguid", getRoleGuidsForRole(sUId,sRoleGuid,dbsession,true)).list());
				}
				result.put("success", true);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/GetRoleNames")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetRoleNames(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") /*"当前用户标识"*/ String sUId,@FormParam("roleguid") /*"需要查询的组标识,忽略则查询当前用户可管理的所有组"*/ String sRoleGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if(sAccessToken==null || sAccessToken.length()==0) {
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			java.util.Date dtNow = Calendar.getInstance().getTime();
			Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where expiresdate>=:expires and id=:token")
					.setTimestamp("expires", dtNow)
					.setString("token", Util.uncompressUUID(sAccessToken)).uniqueResult();
			if(token!=null) {
				if(sUId==null || sUId.length()==0) {
					result.put("Roles", dbsession.createQuery("select rolename from Rbacrole where roleguid in (:roleguid)").setParameterList("roleguid", getRoleGuidsForRole(sRoleGuid,dbsession,false)).list());
				}else {
					result.put("Roles", dbsession.createQuery("select rolename from Rbacrole where roleguid in (:roleguid)").setParameterList("roleguid", getRoleGuidsForRole(sUId,sRoleGuid,dbsession,false)).list());
				}
				result.put("success", true);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
	}
	
	static public boolean CheckLoginErrorAuditLog(String sUsername, String sUId,String sSessoinId,Session session, Transaction t,tab.configServer.ValueString cause) {
		boolean bPass = true;
		try{
			Session dbsession = (session==null ? HibernateSessionFactory.getThreadSession() : session);
			try{
				//长期不用的账号自动锁定，需要管理员重置密码
				if(bPass && Util.passwordAlgorithm.nAccountLockIdleTime>0 && !("admin".equalsIgnoreCase(sUsername) || "tabadmin".equalsIgnoreCase(sUsername))) {
					LocalDateTime ldtNow = LocalDateTime.now();
					ldtNow = ldtNow.plusDays(-Util.passwordAlgorithm.nAccountLockIdleTime);
					java.util.Date dtNow = Date.from(ldtNow.atZone(java.time.ZoneId.systemDefault()).toInstant());
					Integer timeResult =  Util.ObjectToNumber(dbsession.createQuery("select count(*)"
							+ " from Rbacauditlog where resourcename='Rbacuser' and resourceid=:resourceid"
							+ " and updatetime>:lasttime and operationstatus=1")
							.setTimestamp("lasttime", dtNow) .setString("resourceid", sUId).uniqueResult(),0);
					if(timeResult<=0) {
						bPass = false;
						if(cause!=null) {
							cause.value = "The account is locked!";
						}
						log.error("The account is locked! result=" + timeResult);
					}					
				}
				//超时范围内，登录失败次数。operationstatus=0是失败，1是成功
				if(Util.passwordAlgorithm.nAccountLockTime>0) {
					LocalDateTime ldtNow = LocalDateTime.now();
					ldtNow = ldtNow.plusMinutes(-Util.passwordAlgorithm.nAccountLockTime);
					java.util.Date dtNow = Date.from(ldtNow.atZone(java.time.ZoneId.systemDefault()).toInstant());
					Integer timeResult =  Util.ObjectToNumber(dbsession.createQuery("select count(*)"
							+ " from Rbacauditlog where resourcename='Rbacuser' and resourceid=:resourceid"
							+ " and operationname='login' and operationstatus=0 and updatetime>:lasttime and "
							+ " updatetime>(select max(updatetime) from Rbacauditlog where operationname='resetpassword' and operationstatus=1 and resourcename='Rbacuser' and resourceid=:resourceid)")
							.setTimestamp("lasttime", dtNow).setString("resourceid", sUId).uniqueResult(),0);
					if(Util.passwordAlgorithm.retry>0 && timeResult>=Util.passwordAlgorithm.retry) {
						bPass = false;
						if(cause!=null) {
							cause.value = "The account password is incorrect!";
						}
						log.error("The account password is incorrect! result=" + timeResult);
					}
				}
				//长期不改密码，5次提示锁定，超时解锁同retry参数
				if(bPass && Util.passwordAlgorithm.nAccountChangeTime>0 && !("admin".equalsIgnoreCase(sUsername) || "tabadmin".equalsIgnoreCase(sUsername))) {
					LocalDateTime ldtNow = LocalDateTime.now();
					ldtNow = ldtNow.plusDays(-Util.passwordAlgorithm.nAccountChangeTime);
					java.util.Date dtNow = Date.from(ldtNow.atZone(java.time.ZoneId.systemDefault()).toInstant());
					Integer timeResult =  Util.ObjectToNumber(dbsession.createQuery("select count(*)"
							+ " from Rbacauditlog where resourcename='Rbacuser' and resourceid=:resourceid"
							+ " and updatetime>:lasttime and operationname='changepassword' and operationstatus=1")
							.setTimestamp("lasttime", dtNow) .setString("resourceid", sUId).uniqueResult(),0);
					if(timeResult<=0) {
						tab.rbac.RbacSystem.AuditLog("login", "Rbacuser", sUId, 0,String.format("{\"username\":\"%s\",\"roleguid\":\"%s\",\"login\":false}",sUsername,Runner.sAuthorizedRoleGuid), dbsession,t,sUId);
						if(cause!=null) {
							cause.value = "Please change the password, it will be locked!";
						}
						log.warn("Please change the password, it will be locked!");
					}					
				}
				//强制修改密码
				if(bPass && Util.ObjectToString(dbsession.createQuery("select operationname"
						+ " from Rbacauditlog where resourcename='Rbacuser' and resourceid=:resourceid"
						+ " and (operationname='changepassword'  or operationname='resetpassword') and operationstatus=1 order by updatetime desc").setString("resourceid", sUId)
					.setFirstResult(0).setMaxResults(1).uniqueResult()).equals("resetpassword")) {
					tab.rbac.RbacSystem.AuditLog("login", "Rbacuser", sUId, 0,String.format("{\"username\":\"%s\",\"roleguid\":\"%s\",\"login\":false}",sUsername,Runner.sAuthorizedRoleGuid), dbsession,t,sUId);
					if(cause!=null) {
						cause.value = "Please change the password(administrator), it will be locked!";
					}
					log.warn("Please change the password(administrator), it will be locked!");
				}
				if(bPass && Util.passwordAlgorithm.bAccountSingleLogin && sSessoinId!=null) {
					//删除其他用户的session记录，插入自己的session记录，IsLogged判断是否有自己的session记录，无则清空session返回失败
					AuditLog("session", "httpsession", Runner.sAuthorizedRoleGuid, 1,sSessoinId, dbsession,t,sUId);
				}
			}catch(Throwable e){
				log.warn("ERROR:",e);
			}
			if(session==null)dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
		}catch(Throwable e){
			log.warn("ERROR:",e);
		}
		return bPass;
	}
	static public boolean CheckSingleLoginErrorAuditLog(String sUId,String sSessionId,Session session) {
		boolean bPass = true;
		if(Util.passwordAlgorithm.bAccountSingleLogin) {
			try{
				Session dbsession = (session==null ? HibernateSessionFactory.getThreadSession() : session);
				try{
					LocalDateTime ldtNow = LocalDateTime.now();
					ldtNow = ldtNow.plusHours(-16);
					java.util.Date dtNow = Date.from(ldtNow.atZone(java.time.ZoneId.systemDefault()).toInstant());
					//判断是否有自己的session记录,无则返回false
					if(Util.ObjectToNumber(dbsession.createQuery("select count(*)"
							+ " from Rbacauditlog where resourcename='httpsession' and operationvalue=:sessionid"
							+ " and updatetime>:lasttime and operationname='session' and operationstatus=1 and userguid=:userguid")
							.setTimestamp("lasttime", dtNow).setString("sessionid", sSessionId).setString("userguid", sUId).uniqueResult(),0)<=0) {
						bPass = false;
					}
				}catch(Throwable e){
					log.warn("ERROR:",e);
				}
				if(session==null)dbsession.close();
			}catch(org.hibernate.HibernateException e){
				log.warn("ERROR:",e);
			}catch(Throwable e){
				log.warn("ERROR:",e);
			}
		}
		return bPass;
	}
	static public boolean CheckChangePasswordAuditLog(String sUsername, String sUId,String sNewEncryptPassword,Session session,tab.configServer.ValueString cause) {
		boolean bPass = true;
		try{
			Session dbsession = (session==null ? HibernateSessionFactory.getThreadSession() : session);
			try{
				if(Util.passwordAlgorithm.difok>0) {
					@SuppressWarnings("unchecked")
					java.util.List<String> lastPassword =  dbsession.createQuery("select operationvalue"
							+ " from Rbacauditlog where resourcename='Rbacuser' and resourceid=:resourceid"
							+ " and operationname='changepassword' and operationstatus=1 order by updatetime desc")
							.setString("resourceid", sUId).setFirstResult(0).setMaxResults(Util.passwordAlgorithm.difok).list();					
					StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();
					stringEncryptor.setPassword(Util.EncryptPassword);
					stringEncryptor.setAlgorithm(Runner.encryptAlgorithm);
					stringEncryptor.setIvGenerator(new RandomIvGenerator()); 
					String sNewPassword = stringEncryptor.decrypt(sNewEncryptPassword);
					for(String sPassword : lastPassword) {
						String sDbPassword = stringEncryptor.decrypt(sPassword);
						if(sDbPassword.equals(sNewPassword)) {
							if(cause!=null) {
								cause.value = "Recently passwords cannot be used!";
							}
							bPass = false;
						}
						if(!bPass)break;
					}
				}
			}catch(Throwable e){
				log.warn("ERROR:",e);
			}
			if(session==null)dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
		}catch(Throwable e){
			log.warn("ERROR:",e);
		}
		return bPass;
	}
	public static String romveSpecialChar(String str) {
		String regEx = "\\pP|\\pS|\\s+";
		str = java.util.regex.Pattern.compile(regEx).matcher(str).replaceAll("").trim();
		return str;
	}
	// 1）加密策略：verifycode = null, (支持用户名区分大小写)
	// 2）防攻击加密策略：verifycode = md5(md5(password)+verifycode)
	// 3）手机号码+验证码登陆, 则sToken送入手机号码明文token = phone, verifycode = md5(phone+verifycode)
	//      如果是手机无密码的不返回验证码，提示从手机查看验证码
	// 4)  为支持多租户登录模式，用户名必须和roleguid对应
	//		优先使用http header: server-name 获取登录roleguid(可能多个)，无server-name通过配置里授权roleguid
	//API文档: (value = "WEB界面登录函数",notes = "返回success:true|false, 可选返回值:msg(错误信息), verifycode(检验码), uid(用户唯一标识)</br>支持多账号登录登录,账号对应用户信息表各字段")
	@POST
	@Path("/UILogin")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UILogin(@Context HttpServletRequest request, 
			@FormParam("token") /*"用户名/身份证/电话/邮箱/工号"*/String sToken,
			@FormParam("type") /*"用户名:0/身份证:1/电话:2/邮箱:3/工号:4"*/Integer nType,
			@FormParam("password")  /*"加密的密码"*/String sPassword,
			@FormParam("verifycode") @DefaultValue("") String sVerifycode) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if (sToken == null || sToken.length() == 0 || nType==null) {
			log.warn("UIIsLogin: Token is null, (" + nType + ")" );
			return Response.status(200).entity(result).build();
		}
		if(!romveSpecialChar(sToken).equals(sToken)) {
			log.warn("UIIsLogin: Invalid Token!" );
			return Response.status(200).entity(result).build();
		}
		HttpSession httpsession = request.getSession();
		if (httpsession == null) {
			result.put("msg", Runner.sErrorMessage);
			log.warn("UIIsLogin: " + sToken + "(" + nType + ")" + " newSession() is null" );
			return Response.status(200).entity(result).build();
		}
		
		String httpsessionId = httpsession.getId();
		String sServerName = request.getHeader("server-name");
		java.util.Map<String, Object> Attributes = loginUser(sToken,sPassword,sVerifycode,nType,sServerName,httpsessionId,result);
		for (Map.Entry<String, Object> entry : Attributes.entrySet()) {
			httpsession.setAttribute(entry.getKey(),entry.getValue());
		}
		if(tab.util.Util.ObjectToBoolean(result.get("success")))result.put("url", tab.util.Util.encrypt("Rbac/index.html",sVerifycode));//Rbac/build/index.html
		return Response.status(200).entity(result).build();
	}
	
	@POST
	@Path("/Login")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response Login(@FormParam("access_token") /*"访问码"*/String sAccessToken,
			@FormParam("token") /*"用户名/身份证/电话/邮箱/工号"*/String sToken,
			@FormParam("type") /*"用户名:0/身份证:1/电话:2/邮箱:3/工号:4"*/Integer nType,
			@FormParam("password")  /*"加密的密码"*/String sPassword,
			@FormParam("verifycode") @DefaultValue("") String sVerifycode,
			@FormParam("servername") @DefaultValue("") String sServerName,
			@FormParam("httpsessionId") @DefaultValue("") String sHttpsessionId
			) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("Attributes", loginUser(sToken,sPassword,sVerifycode,nType,sServerName,sHttpsessionId,result));
		return Response.status(200).entity(result).build();
	}
	
	public static java.util.Map<String, Object> loginUser(String sToken,String sPassword, String sVerifycode,Integer nType,String sServerName,String httpsessionId, java.util.Map<String, Object> result)
	{
		java.util.Map<String, Object> Attributes = new java.util.HashMap<String, Object>();
		result.put("success", false);
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				String sQuery;
				if(nType==null)nType = AuthTypes.AUTH_BASIC;
				if(sServerName==null || sServerName.length()==0) {
					switch(nType) {
					default:
					case AuthTypes.AUTH_BASIC:
						sQuery = " from Rbacuserauths A where A.roleguid=:roleguid and A.username=:token";
						break;
					case AuthTypes.AUTH_MOBILE:
						sQuery = " from Rbacuserauths A where A.roleguid=:roleguid and A.mobile=:token";
						break;
					case AuthTypes.AUTH_EMAIL:
						sQuery = " from Rbacuserauths A where A.email=:token";
						break;
					case AuthTypes.AUTH_IDNUMBER:
						sQuery = " from Rbacuserauths A where A.roleguid=:roleguid and A.identifier=:token";
						break;
					case AuthTypes.AUTH_AGENT:
						sQuery = " from Rbacuserauths A where A.roleguid=:roleguid and A.agent=:token";
						break;
					}
				}else {
					switch(nType) {
					default:
					case AuthTypes.AUTH_BASIC:
						sQuery = " from Rbacuserauths A where A.roleguid in (select roleguid from Rbacroleinfo where redirecturi=:redirecturi) and A.username=:token";
						break;
					case AuthTypes.AUTH_MOBILE:
						sQuery = " from Rbacuserauths A where A.roleguid in (select roleguid from Rbacroleinfo where redirecturi=:redirecturi) and A.mobile=:token";
						break;
					case AuthTypes.AUTH_EMAIL:
						sQuery = " from Rbacuserauths A where A.email=:token";
						break;
					case AuthTypes.AUTH_IDNUMBER:
						sQuery = " from Rbacuserauths A where A.roleguid in (select roleguid from Rbacroleinfo where redirecturi=:redirecturi) and A.identifier=:token";
						break;
					case AuthTypes.AUTH_AGENT:
						sQuery = " from Rbacuserauths A where A.roleguid in (select roleguid from Rbacroleinfo where redirecturi=:redirecturi) and A.agent=:token";
						break;
					}
				}
				Query query = dbsession.createQuery(sQuery);
				if(nType==AuthTypes.AUTH_EMAIL) {
					query.setString("token", sToken);
				}else {
					if(sServerName==null || sServerName.length()==0) {
						query.setString("token", sToken).setString("roleguid", Runner.sAuthorizedRoleGuid);
					}else {
						query.setString("token", sToken).setString("redirecturi", sServerName);
					}
				}
				@SuppressWarnings("unchecked")
				java.util.List<Rbacuserauths> auths = query.list();
				if (auths.size()==1) {
					Rbacuserauths authuser = auths.get(0);
					boolean bSuccess = false;
					boolean bPass = true;
					tab.configServer.ValueString cause = new tab.configServer.ValueString("");
					if((bPass = CheckLoginErrorAuditLog(authuser.getUsername(), authuser.getUserguid(),httpsessionId,dbsession,null,cause)) && Util.checkPassword(authuser.getUserguid(), sPassword, authuser.getPassword(),sVerifycode)) {
						bSuccess = true;
					}
					if(bSuccess) {
						Transaction ts = dbsession.beginTransaction();
						try{
							authuser.setLogindate(Calendar.getInstance().getTime());
							AuditLog("login", "Rbacuser", authuser.getUserguid(), 1,String.format("{\"username\":\"%s\",\"roleguid\":\"%s\",\"login\":true}",sToken,(sServerName==null || sServerName.length()==0 ? Runner.sAuthorizedRoleGuid:sServerName)), dbsession,ts,authuser.getUserguid());
							ts.commit();
						}catch(Throwable e){
							if ( ts.getStatus() == TransactionStatus.ACTIVE
									|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
								ts.rollback();
							}else ts.commit();
							log.warn("ERROR:",e);
							result.put("msg", Runner.sErrorMessage);
						}
						Attributes.put("username",authuser.getUsername());
						Attributes.put("oid",authuser.getRoleguid());
						Attributes.put("uid",authuser.getUserguid());
						if("Please change the password(administrator), it will be locked!".equals(cause.value)) {
							Attributes.put("password","reset");
						}
						result.put("msg", cause.value);
						result.put("success", true);
					}else {
						if(bPass)result.put("msg", "no user or Wrong password!");
						else result.put("msg", "The account is locked!");
						log.error(sQuery);
						AuditLog("login", "Rbacuser", authuser.getUserguid(), 0,String.format("{\"username\":\"%s\",\"roleguid\":\"%s\",\"login\":false}",sToken,(sServerName==null || sServerName.length()==0 ? Runner.sAuthorizedRoleGuid:sServerName)), dbsession,null,tab.util.Util.NONE_GUID);
					}
				} else {// 没找到用户
					result.put("msg", "no user or Wrong password!");
					if(auths.size()>1) {
						log.error("######### 找到了多个用户，需要确定原因 #########");
					}else {
						log.error(sQuery);
					}
					AuditLog("login", "Rbacuser", tab.util.Util.NONE_GUID, 0,	String.format("{\"username\":\"%s\",\"roleguid\":\"%s\",\"login\":false}",sToken,(sServerName==null || sServerName.length()==0 ? Runner.sAuthorizedRoleGuid:sServerName)), dbsession,null,tab.util.Util.NONE_GUID);
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:",e);
			} catch (Throwable e) {
				log.warn("ERROR: ", e);
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR: ", e);
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		log.warn("loginUser: " + sToken + "(" + nType + ") success is " + result.get("success") + ", httpsessionId=" + httpsessionId);
		return Attributes;		
	}
	
	//API文档: (value = "检查是否已经登录",notes = "返回success:true|false, verifycode, keepaliveDelay(检验间隔时间), datetime(服务器时间)")
	@POST
	@Path("/UIIsLogin")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIIsLogin(@Context HttpServletRequest R) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		HttpSession httpsession = R.getSession();
		if (httpsession != null && httpsession.getAttribute("uid")!=null && httpsession.getAttribute("uid").toString().length()==36) {
			if(CheckSingleLoginErrorAuditLog(Util.ObjectToString(httpsession.getAttribute("uid")),httpsession.getId(),null)) {
				result.put("success", true);
				result.put("keepaliveDelay", Runner.nKeepaliveDelay);
				result.put("datetime", Calendar.getInstance().getTime().getTime());
				if("reset".equals(httpsession.getAttribute("password"))) {
					result.put("password", "reset");
					httpsession.setAttribute("password",null);
				}
			}else {
				log.info("INFO: Session("+httpsession.getId()+") set null, 仅允许单用户登录模式!");
				result.put("success", false);
				httpsession.invalidate();
			}
		} else if (httpsession != null){
			log.error("ERROR: getSession("+httpsession.getId()+") is null");
			result.put("success", false);
		}
		return Response.status(200).entity(result).build();
	}
	
	@POST
	@Path("/IsLogin")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response IsLogin(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") String sUid,@FormParam("httpsessionId") String sHttpsessionId) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		if(CheckSingleLoginErrorAuditLog(sUid,sHttpsessionId,null)) {
			result.put("success", true);
			result.put("keepaliveDelay", Runner.nKeepaliveDelay);
			result.put("datetime", Calendar.getInstance().getTime().getTime());
		}else {
			log.info("INFO: Session() set null, 仅允许单用户登录模式!");
			result.put("success", false);
		}
		result.put("success", true);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "登出")
	@POST
	@Path("/UILogout")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UILogout(@Context HttpServletRequest R) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		HttpSession httpsession = R.getSession();
		if (httpsession != null) {
			String sUname = tab.util.Util.ObjectToString(httpsession.getAttribute("username"));
			String sUid = tab.util.Util.ObjectToString(httpsession.getAttribute("uid"));
			String sOid = tab.util.Util.ObjectToString(httpsession.getAttribute("oid"));
			httpsession.setAttribute("username",StringUtils.EMPTY);
			httpsession.setAttribute("oid",StringUtils.EMPTY);
			httpsession.setAttribute("uid",StringUtils.EMPTY);
			AuditLog("logout", "Rbacuser", sUid, 1,String.format("{\"username\":\"%s\",\"roleguid\":\"%s\",\"logout\":true)",sUname,sOid), null,null,sUid);
			result.put("success", true);
			log.warn(String.format("UILogout(%s) = %s, %s",sUname,sUid,sOid));
		} else {
			log.error("ERROR: UILogout(), Session is null");
			result.put("success", false);
		}
		return Response.status(200).entity(result).build();
	}

	//API文档: (value="获取当前用户权限标识列表", notes = "包含所属组的子组权限和本组权限")
	@POST
	@Path("/UIGetOperations")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetOperations(@Context HttpServletRequest R) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		java.util.Set<String> operations = getRoleOperations(Util.ObjectToString(httpsession.getAttribute("uid")),null,false);
		result.put("Operations", operations);
		result.put("totalCount", operations.size());
		result.put("success", true);
		return Response.status(200).entity(result).build();
	}
	
	//API文档: (value="获取权限列表", notes = "可获得access_token所属组的子组和本组操作权限,或access_token所属组的子组用户和本组用户的操作权限")
	@POST
	@Path("/GetOperations")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetOperations(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") /*"用户标识(可选)"*/String sUId) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if(sAccessToken==null || sAccessToken.length()==0) {
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			java.util.Date dtNow = Calendar.getInstance().getTime();
			Rbactoken token = (Rbactoken)dbsession.createQuery(" from Rbactoken where expiresdate>=:expires and id=:token")
					.setTimestamp("expires", dtNow)
					.setString("token", Util.uncompressUUID(sAccessToken)).uniqueResult();
			if(token!=null) {
				java.util.Set<String> operations = getRoleOperations(sUId,dbsession,false);
				result.put("Operations", operations);
				result.put("totalCount", operations.size());
				result.put("success", true);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
	}

	//API文档: (value="获取当前用户权限列表", notes = "包含所属组的子组权限和本组权限")
	@POST
	@Path("/UIGetOperationInfos")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetOperationInfos(@Context HttpServletRequest R, @FormParam("roleguid") String sRoleGuid) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession != null) {
			try {
				Session dbsession = HibernateSessionFactory.getThreadSession();
				try {
					Query query = dbsession.createQuery(
							//根据登录用户查用户对应的角色，admin对应的是权限系统由 where A.id.userguid=:UserGuid确定用户,由A.id.roleguid=B.roleguid确定角色
							"select B from Rbacroleuser A, Rbacrole B where A.id.userguid=:UserGuid and A.id.roleguid=B.roleguid");
					query.setString("UserGuid", Util.ObjectToString(httpsession.getAttribute("uid")));
					@SuppressWarnings("unchecked")
					java.util.List<Rbacrole> firstroles = query.list();
					Stack<Rbacrole> firststack = new Stack<Rbacrole>();
					firststack.addAll(firstroles);
					java.util.List<OperationInfo> operationList = new java.util.ArrayList<>();
					while (!firststack.isEmpty()) {
						Rbacrole role = (Rbacrole) firststack.firstElement();
						@SuppressWarnings("unchecked")
						//有角色roleguid查出这个角色拥有的权限
						java.util.List<Rbacoperation> operations = dbsession
								.createQuery("select B from Rbacroleoperation A, Rbacoperation B where B.operationguid=A.id.operationguid and A.id.roleguid=:roleguid")
								.setString("roleguid", role.getRoleguid()).list();
						for (int i = 0; i < operations.size(); ++i) {
							Rbacoperation operation = operations.get(i);
							boolean bExists = false;
							for(int j=0;j<operationList.size();++j) {
								OperationInfo info = operationList.get(j);
								if(info.operationguid.equals(operation.getOperationguid())) {
									if(!info.checked) {
										info.checked = role.getRoleguid().equals(sRoleGuid)?true:false;
									}
									bExists = true;
									break;
								}
							}
							if(!bExists) {
								operationList.add(new OperationInfo(operations.get(i).getOperationname(),operations.get(i).getOperationguid(),role.getRoleguid().equals(sRoleGuid)?true:false));
							}
						}
						@SuppressWarnings("unchecked")
						java.util.List<Rbacrole> roles = dbsession.createCriteria(Rbacrole.class)
								.add(Restrictions.eq("fatherroleguid", role.getRoleguid()))
								.list();
						//遍历所有子角色的权限
						firststack.remove(0);
						firststack.addAll(roles);
					}
					java.util.Collections.sort(operationList);
					result.put("OperationInfos", operationList);
					result.put("totalCount", operationList.size());
					result.put("success", true);
				} catch (org.hibernate.HibernateException e) {
					log.warn("ERROR: sessionid=" + httpsession.getAttribute("UserGuid"), e);
				} catch (Throwable e) {
					log.warn("ERROR: sessionid=" + httpsession.getAttribute("UserGuid"), e);
				}
				dbsession.close();
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR: sessionid=" + httpsession.getAttribute("UserGuid"), e);
			} catch (Throwable e) {
				log.warn("ERROR: ", e);
			}
		} else {
			log.error("ERROR: getSession() is null");
		}
		return Response.status(200).entity(result).build();
	}
	//API文档: (value="获取指定组下用户列表", notes = "如果RoleGuid为空则返回所有的子用户列表，不包含顶层组用户")
	@POST
	@Path("/UIGetUsersForRoleManageable")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetUsersForRoleManageable(@Context HttpServletRequest R,@FormParam("roleguid") String sRoleGuid) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		HttpSession httpsession = R.getSession();
		result.put("success", false);
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			result.put("msg", Runner.sErrorMessage);
			return Response.status(200).entity(result).build();
		}
		java.util.List<Rbacuser> users = getUsersForRole(Util.ObjectToString(httpsession.getAttribute("uid")),sRoleGuid,true);
		result.put("Users", users);
		result.put("totalCount", users.size());
		result.put("success",true);
		return Response.status(200).entity(result).build();
	}
	
	@POST
	@Path("/GetUsersForRoleManageable")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetUsersForRoleManageable(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") String sUid,@FormParam("roleguid") String sRoleGuid) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		java.util.List<Rbacuser> users = getUsersForRole(Util.ObjectToString(sUid),sRoleGuid,true);
		result.put("Users", users);
		result.put("totalCount", users.size());
		result.put("success",true);
		return Response.status(200).entity(result).build();
	}
	
	@POST
	@Path("/GetUsersForRole")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetUsersForRole(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") String sUid,@FormParam("roleguid") String sRoleGuid) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		java.util.List<Rbacuser> users = getUsersForRole(Util.ObjectToString(sUid),sRoleGuid,false);
		result.put("Users", users);
		result.put("totalCount", users.size());
		result.put("success",true);
		return Response.status(200).entity(result).build();
	}
	
	public static java.util.List<Rbacuser> getUsersForRole(String sUid,String sRoleGuid,boolean bManageable)
	{
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				java.util.Set<String> sRoleGuids = getRoleGuidsForRole(Util.ObjectToString(sUid),null,dbsession,bManageable);
				if(sRoleGuid!=null && sRoleGuids.contains(sRoleGuid)) {
					@SuppressWarnings("unchecked")
					java.util.List<Rbacuser> users = dbsession.createQuery(
							" from Rbacuser where userguid in (select id.userguid from Rbacroleuser where id.roleguid=:sRoleGuid)")
							.setString("sRoleGuid", sRoleGuid).list();
					dbsession.close();
					return users;
				}else {
					@SuppressWarnings("unchecked")
					java.util.List<Rbacuser> users = dbsession.createQuery(
							" from Rbacuser where userguid in (select id.userguid from Rbacroleuser where id.roleguid in (:RoleGuids))")
							.setParameterList("RoleGuids", sRoleGuids).list();
					dbsession.close();
					return users;
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR: sessionid=" + sUid, e);
			} catch (Throwable e) {
				log.warn("ERROR: sessionid=" + sUid, e);
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR: sessionid=" + sUid, e);
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return new java.util.ArrayList<Rbacuser>();
	}

	//API文档: (value="获取指定组下的用户列表", notes = "如果RoleGuid为空则返回所属组的所有子组用户标识列表")
	@POST
	@Path("/UIGetUserGuidsForRoleManageable")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetUserGuidsForRoleManageable(@Context HttpServletRequest R, @FormParam("roleguid") String sRoleGuid) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		HttpSession httpsession = R.getSession();
		result.put("success", false);
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		@SuppressWarnings("unchecked")
		java.util.Set<String> users  = (java.util.Set<String>)httpsession.getAttribute(sRoleGuid+":users");
		if(users==null) {
			users = getUserGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")),sRoleGuid,null, true);
			httpsession.setAttribute(sRoleGuid+":users",users);
		}
		result.put("UserGuids", users);
		result.put("totalCount", users.size());
		result.put("success", true);
		return Response.status(200).entity(result).build();
	}
	
	//API文档: (value="获取指定组下的工号列表", notes = "如果RoleGuid为空则返回所属组的所有子组工号列表")
	@POST
	@Path("/UIGetAgentsForRoleManageable")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetAgentsForRoleManageable(@Context HttpServletRequest R, @FormParam("roleguid") String sRoleGuid) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		HttpSession httpsession = R.getSession();
		result.put("success", false);
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		@SuppressWarnings("unchecked")
		java.util.Set<String> agents  = (java.util.Set<String>)httpsession.getAttribute(sRoleGuid+":agents");
		if(agents==null) {
			agents = getAuthFieldForRole(Util.ObjectToString(httpsession.getAttribute("uid")),sRoleGuid,null, true,"agent");
			httpsession.setAttribute(sRoleGuid+":agents",agents);
		}
		result.put("Agents", agents);
		result.put("totalCount", agents.size());
		result.put("success", true);
		return Response.status(200).entity(result).build();
	}
	
	@POST
	@Path("/UIGetAgentsForRole")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetAgentsForRole(@Context HttpServletRequest R, @FormParam("roleguid") String sRoleGuid) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		HttpSession httpsession = R.getSession();
		result.put("success", false);
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		@SuppressWarnings("unchecked")
		java.util.Set<String> agents  = (java.util.Set<String>)httpsession.getAttribute(sRoleGuid+":agents");
		if(agents==null) {
			agents = getAuthFieldForRole(Util.ObjectToString(httpsession.getAttribute("uid")),sRoleGuid,null, false,"agent");
			httpsession.setAttribute(sRoleGuid+":agents",agents);
		}
		result.put("Agents", agents);
		result.put("totalCount", agents.size());
		result.put("success", true);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value="获取指定组下的分机列表", notes = "如果RoleGuid为空则返回所属组的所有子组分机列表")
	@POST
	@Path("/UIGetExtensionsForRoleManageable")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetExtensionsForRoleManageable(@Context HttpServletRequest R, @FormParam("roleguid") String sRoleGuid) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		HttpSession httpsession = R.getSession();
		result.put("success", false);
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		@SuppressWarnings("unchecked")
		java.util.Set<String> extensions  = (java.util.Set<String>)httpsession.getAttribute(sRoleGuid+":extensions");
		if(extensions==null) {
			extensions = getAuthFieldForRole(Util.ObjectToString(httpsession.getAttribute("uid")),sRoleGuid,null, true,"extension");
			httpsession.setAttribute(sRoleGuid+":extensions",extensions);
		}
		result.put("Extensions", extensions);
		result.put("totalCount", extensions.size());
		result.put("success", true);
		return Response.status(200).entity(result).build();
	}
	
	//API文档: (value="获取当前用户所属组列表", notes = "如果多个组,显示时建议有一个表示全部的组，这个显示的组RoleGuid为空")
	@POST
	@Path("/UIGetRoles")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetRoles(@Context HttpServletRequest R) {
		HttpSession httpsession = R.getSession();
		if (httpsession != null) {
			return Response.status(200).entity(getRoles(Util.ObjectToString(httpsession.getAttribute("uid")),false)).build();
		} else {
			log.error("ERROR: getSession() is null");
		}
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}
	
	@POST
	@Path("/GetRoles")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetRoles(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") String sUid) {
		return Response.status(200).entity(getRoles(Util.ObjectToString(sUid),false)).build();
	}
	//API文档: (value="获取当前用户可管理的组列表", notes = "不包含用户所属的本组或称顶级组，显示时建议有一个表示全部的组，这个显示的组RoleGuid为空")
	@POST
	@Path("/UIGetRolesManageable")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetRolesManageable(@Context HttpServletRequest R) {
		
		HttpSession httpsession = R.getSession();
		if (httpsession != null) {
			return Response.status(200).entity(getRoles(Util.ObjectToString(httpsession.getAttribute("uid")),true)).build();
		} else {
			log.error("ERROR: getSession() is null");
		}
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}
	
	@POST
	@Path("/GetRolesManageable")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetRolesManageable(@FormParam("access_token") /*"访问码"*/String sAccessToken,@FormParam("uid") String sUid) {
		return Response.status(200).entity(getRoles(Util.ObjectToString(sUid),true)).build();
	}
	
	private java.util.Map<String, Object> getRoles(String sUid,boolean bManageable)
	{
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if(bManageable) {
			try {
				Session dbsession = HibernateSessionFactory.getThreadSession();
				try {
					Query query = dbsession.createQuery(
							"select B from Rbacroleuser A, Rbacrole B where A.id.userguid=:UserGuid and A.id.roleguid=B.roleguid");
					query.setString("UserGuid", Util.ObjectToString(sUid));
					@SuppressWarnings("unchecked")
					java.util.List<Rbacrole> firstroles = query.list();
					Stack<Rbacrole> firststack = new Stack<Rbacrole>();
					firststack.addAll(firstroles);
					
					boolean isManageable = false;
					java.util.HashMap<String,Rbacrole> mapRoles = new java.util.HashMap<String, Rbacrole>();
					while (!firststack.isEmpty()) {
						Rbacrole role = (Rbacrole) firststack.firstElement();
						@SuppressWarnings("unchecked")
						java.util.List<Rbacrole> roles = dbsession.createCriteria(Rbacrole.class)
								.add(Restrictions.eq("fatherroleguid", role.getRoleguid())).list();
						firststack.remove(0);
						firststack.addAll(roles);
						for(Rbacrole r: roles) {
							isManageable = true;
							mapRoles.put(r.getRoleguid(),r);
						}
					}
					if(isManageable) {
						for(Rbacrole r: firstroles) {
							mapRoles.put(r.getRoleguid(),r);
						}
					}
					result.put("Roles", mapRoles.values());
					result.put("totalCount", mapRoles.size());
					result.put("success", true);
				} catch (org.hibernate.HibernateException e) {
					log.error("ERROR: " , e);
				} catch (Throwable e) {
					log.error("ERROR: " , e);
				}
				dbsession.close();
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR: ", e);
			} catch (Throwable e) {
				log.warn("ERROR: ", e);
			}
			return result;
		}
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Query query = dbsession.createQuery(
						"select B from Rbacroleuser A, Rbacrole B where A.id.userguid=:UserGuid and A.id.roleguid=B.roleguid");
				query.setString("UserGuid", Util.ObjectToString(sUid));
				@SuppressWarnings("unchecked")
				java.util.List<Rbacrole> roles = query.list();
				result.put("Roles", roles);
				result.put("totalCount", roles.size());
				result.put("success", true);
			} catch (org.hibernate.HibernateException e) {
				log.error("ERROR: ", e);
			} catch (Throwable e) {
				log.error("ERROR: ", e);
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR: ", e);
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return result;
	}

	//API文档: (value="获取指定组下的组列表", notes = "如果RoleGuid为空则返回所有的子组列表。(包含用户所属本组或称顶级组)")
	@POST
	@Path("/UIGetRolesForRole")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetRolesForRole(@Context HttpServletRequest R, @FormParam("roleguid") String sRoleGuid) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			result.put("Roles", null);
			result.put("totalCount", 0);
			result.put("success", false);
			return Response.status(200).entity(result).build();
		}
		java.util.List<Rbacrole> ListRoles = getRolesForRole(Util.ObjectToString(httpsession.getAttribute("uid")),sRoleGuid);
		result.put("Roles", ListRoles);
		result.put("totalCount", ListRoles.size());
		result.put("success", true);
		return Response.status(200).entity(result).build();
	}
	
	@POST
	@Path("/GetRolesForRole")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetRolesForRole(@FormParam("access_token") /*"访问码"*/String sAccessToken, @FormParam("uid") String sUid, @FormParam("roleguid") String sRoleGuid) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		java.util.List<Rbacrole> ListRoles = getRolesForRole(Util.ObjectToString(sUid),sRoleGuid);
		result.put("Roles", ListRoles);
		result.put("totalCount", ListRoles.size());
		result.put("success", true);
		return Response.status(200).entity(result).build();
	}

	public static java.util.List<Rbacrole> getRolesForRole(String sUid, String sRoleGuid)
	{
		java.util.HashMap<String,Rbacrole> RoleGuids = new java.util.HashMap<String, Rbacrole>();
		boolean bRoleGuid = false;
		if (sRoleGuid != null && tab.util.Util.NONE_GUID.equals(sRoleGuid)==false && sRoleGuid.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
			bRoleGuid = true;
		}
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Query query = dbsession.createQuery(
						"select B from Rbacroleuser A, Rbacrole B where A.id.userguid=:UserGuid and A.id.roleguid=B.roleguid");
				query.setString("UserGuid", Util.ObjectToString(sUid));
				@SuppressWarnings("unchecked")
				java.util.List<Rbacrole> firstroles = query.list();
				if(bRoleGuid) {
					for(int idx=0;idx<firstroles.size();++idx)
					{
						if(firstroles.get(idx).getRoleguid().equals(sRoleGuid)){
							Rbacrole firstrole = firstroles.get(idx);
							firstroles.clear();
							firstroles.add(firstrole);
							RoleGuids.put(firstrole.getRoleguid(),firstrole);
							bRoleGuid = false;
							break;
						}
					}
				}
				Stack<Rbacrole> firststack = new Stack<Rbacrole>();
				firststack.addAll(firstroles);
				
				while (!firststack.isEmpty()) {
					Rbacrole role = (Rbacrole) firststack.firstElement();
					if(bRoleGuid) {
						if (role.getRoleguid().equals(sRoleGuid)) {
							RoleGuids.put(role.getRoleguid(),role);
							bRoleGuid = false;
						}
					}else {
						RoleGuids.put(role.getRoleguid(),role);
					}
					@SuppressWarnings("unchecked")
					java.util.List<Rbacrole> roles = dbsession.createCriteria(Rbacrole.class)
							.add(Restrictions.eq("fatherroleguid", role.getRoleguid())).list();
					firststack.remove(0);
					firststack.addAll(roles);
					for (int i = 0; i < roles.size(); ++i) {
						if (bRoleGuid) {
							if (roles.get(i).getRoleguid().equals(sRoleGuid)) {//遍历子组查找需要的组
								bRoleGuid = false;
								firststack.clear();
								firststack.addElement(roles.get(i));
								RoleGuids.put(roles.get(i).getRoleguid(),roles.get(i));
								break;
							}
						} else {
							RoleGuids.put(roles.get(i).getRoleguid(),roles.get(i));
						}
					}
				}
				java.util.List<Rbacrole> ListRoles = new java.util.ArrayList<Rbacrole>(RoleGuids.values());
			    java.util.Collections.sort(ListRoles, new java.util.Comparator<Rbacrole>() {
                    @Override
                    public int compare(Rbacrole l, Rbacrole r) {
                        return l.getRolename().compareTo(r.getRolename());
                    }
                });
			    dbsession.close();
				return ListRoles;
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR: ", e);
			} catch (Throwable e) {
				log.warn("ERROR: ", e);
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR: ", e);
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return new java.util.ArrayList<Rbacrole>();
	}

	//API文档: (value="获取指定组下的子组", notes = "该接口用于树形分组管理")
	@POST
    @Path("/UIGetRolesTreeNodes")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIGetRolesTreeNodes(@Context HttpServletRequest R,@FormParam("node")String sRoleGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null || httpsession.getAttribute("uid")==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try{
				if(ROOT_ROLEGUID.equals(sRoleGuid)) {
					//查询组为Root组时, 根据获取用户所属组, 用户可能同时属于子组，所以只取顶级组
					Query query = dbsession.createQuery(
							"select B from Rbacroleuser A, Rbacrole B where A.id.userguid=:UserGuid and A.id.roleguid=B.roleguid");
					query.setString("UserGuid", Util.ObjectToString(httpsession.getAttribute("uid")));
					@SuppressWarnings("unchecked")
					java.util.List<Rbacrole> firstroles = query.list();
					java.util.List<Rbacrole> topRoles = new java.util.ArrayList<Rbacrole>();
					for(Rbacrole role : firstroles) {
						if(role.getRoleguid().equals(ROOT_ROLEGUID)) {
							//Root最高权限组是唯一的，如果用户属于最高权限组立即返回结果
							topRoles.clear();
							@SuppressWarnings("unchecked")
							java.util.List<RbacroleEx> Roles = dbsession.createQuery(" from RbacroleEx where fatherroleguid=:roleguid")
									.setString("roleguid", sRoleGuid).list();
							dbsession.close();
							result.put("Roles", Roles);
							result.put("success", Roles.size()>0?true:false);
							return Response.status(200).entity(result).build();
						}else {
							if(!noTopRole(role.getFatherroleguid(), firstroles,dbsession)) {
								if(!role.getRoleguid().equals(ROOT_ROLEGUID)) {
									//返回的组，父组标识必须为Root组才能显示
									role.setFatherroleguid(ROOT_ROLEGUID);
									topRoles.add(role);
								}
							}
						}
					}
					result.put("Roles", topRoles);
					result.put("success", topRoles.size()>0?true:false);
				}else {
					@SuppressWarnings("unchecked")
					java.util.List<RbacroleEx> Roles = dbsession.createQuery(" from RbacroleEx where fatherroleguid=:roleguid")
							.setString("roleguid", sRoleGuid).list();
					result.put("Roles", Roles);
					result.put("success", Roles.size()>0?true:false);
				}
			}catch(org.hibernate.HibernateException e){
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}catch(Throwable e){
				log.warn("ERROR:",e);		
				result.put("msg", Runner.sErrorMessage);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	//API文档: (value="添加组到指定父组", notes = "该接口用于树形分组管理界面")
	@POST
    @Path("/UIAddRoleToTreeNodes")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIAddRoleToTreeNodes(@Context HttpServletRequest R,@FormParam("fatherroleguid")String sFatherRoleGuid
    		,@FormParam("rolename")String sRoleName,@DefaultValue("0") @FormParam("inheritance")Boolean bInheritance){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			if(!tab.rbac.RbacSystem.getUidOperations(Util.ObjectToString(httpsession.getAttribute("uid")), dbsession).contains("6B46C170-833F-413C-9D90-D887817B3E34")) {
				dbsession.close();
				return Response.status(200).entity(result).build();
			}
			try{
				@SuppressWarnings("unchecked")
				java.util.List<String> roles = dbsession.createQuery(
						"select id.roleguid from Rbacroleuser where id.userguid=:UserGuid").setString("UserGuid", Util.ObjectToString(httpsession.getAttribute("uid"))).list();
				Object sRoleGuid = sFatherRoleGuid;
				do {
					sRoleGuid = dbsession.createQuery(
							"select fatherroleguid from Rbacrole where roleguid=:RoleGuid").setString("RoleGuid", sRoleGuid.toString())
							.uniqueResult();
					if(sRoleGuid!=null && (sRoleGuid.equals(Util.NONE_GUID) || roles.contains(sRoleGuid)) ) {
						Transaction ts = dbsession.beginTransaction();
						try{
							Rbacrole role = new Rbacrole();
							role.setFatherroleguid(sFatherRoleGuid);
							role.setInheritance(bInheritance?1:0);
							role.setRoleguid(UUID.randomUUID().toString().toUpperCase());
							role.setRolename(sRoleName);
							dbsession.save(role);
							ts.commit();
							result.put("role", role);
							result.put("success", true);
						}catch(org.hibernate.HibernateException e){
							if ( ts.getStatus() == TransactionStatus.ACTIVE
									|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
								ts.rollback();
							}else ts.commit();
							log.warn("ERROR:",e);
							result.put("msg", Runner.sErrorMessage);
						}catch(Throwable e){
							if ( ts.getStatus() == TransactionStatus.ACTIVE
									|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
								ts.rollback();
							}else ts.commit();
							log.warn("ERROR:",e);
							result.put("msg", Runner.sErrorMessage);
						}
						dbsession.close();
						return Response.status(200).entity(result).build();
					}
				}while(sRoleGuid!=null);
			}catch(org.hibernate.HibernateException e){
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}catch(Throwable e){
				log.warn("ERROR:",e);		
				result.put("msg", Runner.sErrorMessage);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	//API文档: (value="设置指定组的指定权限", notes = "该接口用于树形分组管理")
	@POST
    @Path("/UIUpdateRoleOperation")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIUpdateRoleOperation(@Context HttpServletRequest R,@FormParam("roleguid")String sRoleGuid
    		,@FormParam("operationguid")String sOperationGuid,@FormParam("checked")Boolean bChecked,@FormParam("all")Boolean bAll){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		if(sRoleGuid==null || sRoleGuid.length()==0) {
			result.put("msg", "请选择分配权限的组");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			if(!tab.rbac.RbacSystem.getUidOperations(Util.ObjectToString(httpsession.getAttribute("uid")), dbsession).contains("6B46C170-833F-413C-9D90-D887817B3E34")) {
				dbsession.close();
				return Response.status(200).entity(result).build();
			}
			Transaction ts = dbsession.beginTransaction();
			try{
				if(bChecked) {
					//添加该权限到组
					java.util.Set<String> operations;
					if(getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")),sRoleGuid,dbsession,false).contains(sRoleGuid)){
						operations = getRoleOperations(Util.ObjectToString(httpsession.getAttribute("uid")),dbsession,true);
					}else {
						operations = new java.util.HashSet<>();
					}
					if(bAll!=null && bAll) {
						for(String sOperationGuidMore: operations) {
							if(!sOperationGuidMore.equals(ROOT_ROLEGUID)) {
								Rbacroleoperation roleoperation = new Rbacroleoperation();
								RbacroleoperationId id = new RbacroleoperationId();
								id.setOperationguid(sOperationGuidMore);
								id.setRoleguid(sRoleGuid);
								roleoperation.setId(id);
								try {
									dbsession.save(roleoperation);
								}catch(org.hibernate.NonUniqueObjectException e) {
									//忽略重复的记录
								}
							}
						}
						ts.commit();
						result.put("success", true);
					}else {
						if(operations.contains(sOperationGuid) || operations.contains(ROOT_ROLEGUID)) {
							Rbacroleoperation roleoperation = new Rbacroleoperation();
							RbacroleoperationId id = new RbacroleoperationId();
							id.setOperationguid(sOperationGuid);
							id.setRoleguid(sRoleGuid);
							roleoperation.setId(id);
							dbsession.save(roleoperation);
							ts.commit();
							result.put("success", true);
						}else {
							result.put("msg", "不能分配该权限,必须拥有该权限才能分配");
						}
					}
				}else {
					if(bAll!=null && bAll) {
						Map<String, Integer> mp = getRoleOperationReference(Util.ObjectToString(httpsession.getAttribute("uid")),dbsession);
						for(Map.Entry<String, Integer> entry: mp.entrySet()) {
							if(entry.getValue()>1) {
								dbsession.createQuery("delete Rbacroleoperation where id.operationguid=:operationguid and id.roleguid=:roleguid")
									.setString("operationguid", entry.getKey()).setString("roleguid",sRoleGuid).executeUpdate();
							}
						}
						ts.commit();
						result.put("success", true);
					}else {
						//删除该权限,如果仅有一个引用，不能删除。因为删除后该用户将不能管理该权限，需要管理员重新分配给该用户所属组
						Map<String, Integer> mp = getRoleOperationReference(Util.ObjectToString(httpsession.getAttribute("uid")),dbsession);
						if(StringUtils.equalsIgnoreCase(POWER_OPERATIONGUID,sOperationGuid) && StringUtils.equalsIgnoreCase(ROOT_ROLEGUID,sRoleGuid)){
							result.put("msg", "有该权限不能从[权限系统]删除");
						}else {
							if(mp.containsKey(sOperationGuid) && mp.get(sOperationGuid)>1) {
								dbsession.createQuery("delete Rbacroleoperation where id.operationguid=:operationguid and id.roleguid=:roleguid")
								.setString("operationguid", sOperationGuid).setString("roleguid",sRoleGuid).executeUpdate();
								ts.commit();
								result.put("success", true);
							}else {
								result.put("msg", "这是唯一拥有该权限的组，请将该权限分配给其他组再删除");
							}
						}
					}
				}
			}catch(org.hibernate.HibernateException e){
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}catch(Throwable e){
				log.warn("ERROR:",e);		
				result.put("msg", Runner.sErrorMessage);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	//API文档: (value="修改密码", notes = "仅修改当前用户的密码")
	@POST
    @Path("/UIChangePassword")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIChangePassword(@Context HttpServletRequest R,@FormParam("password")String sPassword,@FormParam("newpassword")String sNewPassword,@FormParam("verifycode") @DefaultValue("") String sVerifycode)
    {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		String sUId = Util.ObjectToString(httpsession.getAttribute("uid"));
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			Transaction ts = dbsession.beginTransaction();
			try{			
				Rbacuserauths userauth = (Rbacuserauths)dbsession.createQuery("from Rbacuserauths where userguid=:UserGuid")
						.setString("UserGuid", sUId).setFirstResult(0).setMaxResults(1).uniqueResult();
				tab.configServer.ValueString cause = new tab.configServer.ValueString(StringUtils.EMPTY);
				String sEncryptPassword = Util.encryptPassword(userauth.getUsername(),sUId,sNewPassword,sVerifycode,cause);
				if(sEncryptPassword!=null && sEncryptPassword.length()>0) {
					if(CheckChangePasswordAuditLog(userauth.getUsername(),userauth.getUserguid(),sEncryptPassword,dbsession,cause)) {
						if(userauth!=null && Util.checkPassword(sUId, sPassword, userauth.getPassword(),sVerifycode) && dbsession.createQuery("update Rbacuserauths set password=:newpassword where userguid=:UserGuid")
							.setString("newpassword", sEncryptPassword).setString("UserGuid", sUId).executeUpdate()>0){
							AuditLog("changepassword", "Rbacuser", userauth.getUserguid(), 1,sEncryptPassword, dbsession,ts,userauth.getUserguid());
							ts.commit();
							result.put("success", true);
						}
					}else {
						log.warn(cause.value);
						result.put("msg", Runner.sErrorMessage);
					}
				}else {
					if (dbsession.createQuery(
							"update Rbacuserauths set password=:newpassword where userguid=:UserGuid and password=:password")
							.setString("password", DigestUtils.md5Hex(sUId + sPassword))
							.setString("newpassword", DigestUtils.md5Hex(sUId + sNewPassword))
							.setString("UserGuid", sUId).executeUpdate() > 0) {
						ts.commit();
						result.put("success", true);
					}else {
						log.warn(cause.value);
						result.put("msg", Runner.sErrorMessage);
					}
				}
			}catch(org.hibernate.HibernateException e){
				if ( ts.getStatus() == TransactionStatus.ACTIVE
						|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
					ts.rollback();
				}else ts.commit();
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}catch(Throwable e){
				if ( ts.getStatus() == TransactionStatus.ACTIVE
						|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
					ts.rollback();
				}else ts.commit();
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		log.warn("UIChangePassword: " + sUId + " success is " + result.get("success") );
		return Response.status(200).entity(result).build();
    }
	//API文档: (value="获取用户信息", notes = "获取当前用户的信息或所属组的本组用户和子组用户信息")
	@POST
    @Path("/UIGetUserAuths")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIGetUserAuths(@Context HttpServletRequest R,@FormParam("userguid") /*"如果参数空则返回当前用户信息"*/ String sUserGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		if(httpsession.getAttribute("uid")==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			if(sUserGuid==null || sUserGuid.length()==0) {
				sUserGuid = Util.ObjectToString(httpsession.getAttribute("uid"));
			}
			if(Util.ObjectToString(httpsession.getAttribute("uid")).equals(sUserGuid) 
					|| getUserGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")),null,dbsession, false).contains(sUserGuid)) {
				Rbacuserauths userauth = (Rbacuserauths)dbsession.createQuery(" from Rbacuserauths where userguid=:UserGuid").setString("UserGuid", sUserGuid).uniqueResult();
				if(userauth!=null) {
					result.put("registerPbxAgent", Runner.bRegisterPbxAgent);
					result.put("username", userauth.getUsername());
					result.put("userguid", userauth.getUserguid());
					result.put("mobile", userauth.getMobile());
					result.put("identifier", userauth.getIdentifier());
					result.put("email", userauth.getEmail());
					result.put("agent", userauth.getAgent());
					result.put("logintime", userauth.getLogindate().getTime());
				}else {
					result.put("registerPbxAgent", Runner.bRegisterPbxAgent);
					result.put("username", "");
					result.put("userguid", "");
					result.put("mobile", "");
					result.put("identifier", "");
					result.put("email", "");
					result.put("agent", "");
					result.put("logintime", new Date().getTime());
				}
			}else {
				result.put("registerPbxAgent", Runner.bRegisterPbxAgent);
				result.put("username", "");
				result.put("userguid", "");
				result.put("mobile", "");
				result.put("identifier", "");
				result.put("email", "");
				result.put("agent", "");
			}
			dbsession.close();
			result.put("success", true);
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	
	static public void AuditLog(String sOperationName, String sResourceName,String sResourceId,int nOperationStatus,String sOperationValue, Session session, Transaction t,String sUid) {
		try{
			Session dbsession = (session==null ? HibernateSessionFactory.getThreadSession() : session);
			Transaction ts = (t==null ? dbsession.beginTransaction() : t);
			try{
				if("httpsession".equals(sResourceName) && sUid!=null) {
					dbsession.createQuery("delete from Rbacauditlog where resourcename='httpsession' and userguid=:userguid").setString("userguid", sUid).executeUpdate();
				}
				Rbacauditlog audialog = new Rbacauditlog(UUID.randomUUID().toString().toUpperCase(), sResourceName, sResourceId, 
						sUid, Calendar.getInstance().getTime(), sOperationName,nOperationStatus,sOperationValue);
				dbsession.save(audialog);
				dbsession.flush();
				if(t==null)ts.commit();
			}catch(org.hibernate.HibernateException e){
				if(t==null) {
					if ( ts.getStatus() == TransactionStatus.ACTIVE
							|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
						ts.rollback();
					}else ts.commit();
				}
				log.warn("ERROR:",e);
			}catch(Throwable e){
				if(t==null) {
					if ( ts.getStatus() == TransactionStatus.ACTIVE
							|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
						ts.rollback();
					}else ts.commit();
				}
				log.warn("ERROR:",e);
			}
			if(session==null)dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
		}
	}
	
	static public void UpdateResourceIdAuditLog(String sOperationName, String sResourceName,String sResourceId,int nOperationStatus,String sOperationValue, Session session, Transaction t,String sUid) {
		try{
			Session dbsession = (session==null ? HibernateSessionFactory.getThreadSession() : session);
			Transaction ts = (t==null ? dbsession.beginTransaction() : t);
			try{
				Rbacauditlog audialog =  (Rbacauditlog)dbsession.createQuery(" from Rbacauditlog where resourcename=:resourcename and resourceid=:resourceid and operationname=:operationname")
						.setString("operationname", sOperationName).setString("resourcename", sResourceName).setString("resourceid", sResourceId).setFirstResult(0).setMaxResults(1).uniqueResult();
				if(audialog!=null) {
					audialog.setOperationstatus(nOperationStatus);
					audialog.setOperationvalue(sOperationValue);
					audialog.setUpdatetime(Calendar.getInstance().getTime());
					dbsession.update(audialog);
				}else {
					audialog = new Rbacauditlog(UUID.randomUUID().toString().toUpperCase(), sResourceName, sResourceId, 
							sUid, Calendar.getInstance().getTime(), sOperationName,nOperationStatus,sOperationValue);
					dbsession.save(audialog);
				}
				if(t==null)ts.commit();
			}catch(org.hibernate.HibernateException e){
				if(t==null) {
					if ( ts.getStatus() == TransactionStatus.ACTIVE
							|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
						ts.rollback();
					}else ts.commit();
				}
				log.warn("ERROR:",e);
			}catch(Throwable e){
				if(t==null) {
					if ( ts.getStatus() == TransactionStatus.ACTIVE
							|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
						ts.rollback();
					}else ts.commit();
				}
				log.warn("ERROR:",e);
			}
			if(session==null)dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
		}
	}
	@POST
    @Path("/UIGetAuditLogs")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIGetAuditLogs(@Context HttpServletRequest R,@DefaultValue("0") @FormParam("start") /* "开始页" */Integer pageStart, 
    		@DefaultValue("25") @FormParam("limit") /* "分页大小" */Integer pageLimit)
    {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try{
				String sQuery = " from Rbacauditlog ";
				String sUId = Util.ObjectToString(httpsession.getAttribute("uid"));
				java.util.Set<String> roles = tab.rbac.RbacSystem.getRoleGuidsForRole(sUId, null, dbsession, true);
				if (roles != null && roles.size() > 0 && roles.contains(tab.rbac.RbacSystem.ROOT_ROLEGUID)) {
					
				}else {
					sQuery += " where userguid='"+sUId+"'";
				}
				long nTotalCount = Util.ObjectToNumber(dbsession.createQuery("select count(*) "+sQuery).uniqueResult(),0L);
				sQuery += " order by updatetime desc";
				@SuppressWarnings("unchecked")
				java.util.List<Rbacauditlog> logs = dbsession.createQuery(sQuery).setFirstResult(pageStart).setMaxResults(pageLimit).list();
				result.put("logs", logs);
				result.put("totalCount", nTotalCount);
				result.put("success", true);
			}catch(org.hibernate.HibernateException e){
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}catch(Throwable e){
				log.warn("ERROR:",e);		
				result.put("msg", Runner.sErrorMessage);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();		
    }
	@POST
    @Path("/UIAuditLog")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIAuditLog(@Context HttpServletRequest R,
    		@FormParam("resourcename") /*"审计的资源名，例如录音表名"*/String sResourceName,
    		@FormParam("resourceid") /*资源ID，例如录音ID*/String sResourceId,
    		@FormParam("operationname") /*操作*/String sOperationName,
    		@FormParam("operationstatus") /*操作状态*/Integer nOperationStatus,
    		@FormParam("operationvalue") /*操作值*/String sOperationValue){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			Transaction ts = dbsession.beginTransaction();
			try{
				if(StringUtils.isBlank(sOperationValue)) {
					sOperationValue = "{\"username\":\""+Util.ObjectToString(httpsession.getAttribute("username"))+"\",\"id\":\""+sResourceId+"\"}";
				}
				Rbacauditlog audialog = new Rbacauditlog(UUID.randomUUID().toString().toUpperCase(), sResourceName, sResourceId, 
						tab.util.Util.ObjectToString(httpsession.getAttribute("uid")), Calendar.getInstance().getTime(), sOperationName,nOperationStatus,sOperationValue);
				dbsession.save(audialog);
				ts.commit();
				result.put("success", true);
			}catch(org.hibernate.HibernateException e){
				if ( ts.getStatus() == TransactionStatus.ACTIVE
						|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
					ts.rollback();
				}else ts.commit();
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}catch(Throwable e){
				if ( ts.getStatus() == TransactionStatus.ACTIVE
						|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
					ts.rollback();
				}else ts.commit();
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	
	@POST
    @Path("/AuditLog")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response AuditLog(@FormParam("access_token") /*"访问码"*/String sAccessToken,
    		@FormParam("uid") String sUId,
    		@FormParam("resourcename") /*"审计的资源名，例如录音表名"*/String sResourceName,
    		@FormParam("resourceid") /*资源ID，例如录音ID*/String sResourceId,
    		@FormParam("operationname") /*操作*/String sOperationName,
    		@FormParam("operationstatus") /*操作状态*/Integer nOperationStatus,
    		@FormParam("operationvalue") /*操作值*/String sOperationValue){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			Transaction ts = dbsession.beginTransaction();
			try{
				Rbacauditlog audialog = new Rbacauditlog(UUID.randomUUID().toString().toUpperCase(), sResourceName, sResourceId, 
						tab.util.Util.ObjectToString(sUId), Calendar.getInstance().getTime(), sOperationName,nOperationStatus,sOperationValue);
				dbsession.save(audialog);
				ts.commit();
				result.put("success", true);
			}catch(org.hibernate.HibernateException e){
				if ( ts.getStatus() == TransactionStatus.ACTIVE
						|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
					ts.rollback();
				}else ts.commit();
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}catch(Throwable e){
				if ( ts.getStatus() == TransactionStatus.ACTIVE
						|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
					ts.rollback();
				}else ts.commit();
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	@POST
    @Path("/UIFindUsers")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIFindUsers(@Context HttpServletRequest R,@FormParam("username") String sUsername,
    		@FormParam("agent") String sAgent,@FormParam("mobile") String sMobile,@FormParam("email") String sMail) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		if(StringUtils.isBlank(sUsername) && StringUtils.isBlank(sAgent) && StringUtils.isBlank(sMobile) && StringUtils.isBlank(sMail)) {
			log.error("ERROR: params is null");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			if(!tab.rbac.RbacSystem.getUidOperations(Util.ObjectToString(httpsession.getAttribute("uid")), dbsession).contains("6B46C170-833F-413C-9D90-D887817B3E34")) {
				dbsession.close();
				return Response.status(200).entity(result).build();
			}
			java.util.Set<String> sRoles = getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")),null,dbsession,false);
			String sQuery = "select new Map(A.username as username,A.agent as agent,A.mobile as mobile,A.email as email,C.rolename as rolename)" +
					" from Rbacuserauths A,Rbacroleuser B,Rbacrole C where" + 
					" A.userguid=B.id.userguid and B.id.roleguid=C.roleguid and B.id.roleguid in (:roleguids) and (";
			boolean bOr = false;
			if(!StringUtils.isBlank(sUsername)) {
				sQuery += " A.username like :username";
				bOr = true;
			}
			if(!StringUtils.isBlank(sAgent)) {
				if(bOr)sQuery += " or";
				sQuery += " A.agent like :agent";
				bOr = true;
			}
			if(!StringUtils.isBlank(sMobile)) {
				if(bOr)sQuery += " or";
				sQuery += " A.mobile like :mobile";
				bOr = true;
			}
			if(!StringUtils.isBlank(sMail)) {
				if(bOr)sQuery += " or";
				sQuery += " A.email like :email";
				bOr = true;
			}
			sQuery += ") group by A.username,A.agent,A.mobile,A.email,C.rolename" + 
					" order by A.username,A.agent,A.mobile,A.email,C.rolename";
			Query query = dbsession.createQuery(sQuery).setParameterList("roleguids", sRoles);
			if(!StringUtils.isBlank(sUsername))query.setString("username", "%"+sUsername+"%");
			if(!StringUtils.isBlank(sAgent))query.setString("agent", "%"+sAgent+"%");
			if(!StringUtils.isBlank(sMobile))query.setString("mobile", "%"+sMobile+"%");
			if(!StringUtils.isBlank(sMail))query.setString("email", "%"+sMail+"%");
			
			@SuppressWarnings("unchecked")
			java.util.List<java.util.Map<String, String>> users = query.list();
			result.put("success", true);
			result.put("users", users);
			result.put("totalCount", users.size());
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
	}
	//API文档: (value="增加用户", notes = "用户有权限增加子组和本组用户，可修改子组用户，无权修改本组用户")
	@POST
    @Path("/UIAddNewUser")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIAddNewUser(@Context HttpServletRequest R,
    		@FormParam("roleguid") /*"权限组"*/String sRoleGuid,
    		@FormParam("nickname") /*"昵称"*/String sNickname,
    		@FormParam("headimgurl") /*"头像URL"*/String sHeadimgurl,
    		@FormParam("agent") /*"工号"*/String sAgent,
    		@FormParam("identifier") /*"证件"*/String sIdentifier,
    		@FormParam("mobile") /*"电话"*/String sMobile,
    		@FormParam("email") /*"邮箱地址"*/String sEmail,
    		@FormParam("username") /*"用户名"*/String sUserName,
    		@FormParam("password") String sPassword,
    		@FormParam("verifycode") @DefaultValue("") String sVerifycode,
    		@FormParam("activate") /*"状态激活"*/Boolean bActivate){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			if(getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")),null,dbsession,false).contains(sRoleGuid)) {
				Transaction ts = dbsession.beginTransaction();
				try{
					if(sAgent!=null || sIdentifier!=null || sMobile!=null  || sEmail!=null || sUserName!=null || (sPassword!=null && sPassword.length()>0) || bActivate!=null)
					{
						String sQuery = " from Rbacuserauths where roleguid=:roleguid and ";
						Rbacuserauths userauth =  new Rbacuserauths();
						userauth.setCreatedate(Calendar.getInstance().getTime());
						userauth.setUpdatedate(userauth.getCreatedate());
						if(bActivate!=null)userauth.setStatus(bActivate?1:0);
						else userauth.setStatus(1);
						boolean bOr = false;
						userauth.setAgent(sAgent!=null?sAgent:"");
						if(userauth.getAgent().length()>0) {
							sQuery += (bOr?" or ":" ( ") + "agent=:agent";
							bOr = true;
						}

						userauth.setIdentifier(sIdentifier!=null?sIdentifier:"");
						if(userauth.getIdentifier().length()>0) {
							sQuery += (bOr?" or ":" ( ") + "identifier=:identifier";
							bOr = true;
						}
						userauth.setMobile(sMobile!=null?sMobile:"");
						userauth.setEmail(sEmail!=null?sEmail:"");
						if(userauth.getEmail().length()>0) {
							sQuery += (bOr?" or ":" ( ") + "email=:email";
							bOr = true;
						}
						userauth.setUsername(sUserName!=null?sUserName:"");
						if(userauth.getUsername().length()>0) {
							sQuery += (bOr?" or ":" ( ") + "username=:username";
							bOr = true;
						}
						if(bOr) {
							sQuery += " )";
						}
						userauth.setLogindate(Calendar.getInstance().getTime());
						Query query = dbsession.createQuery(sQuery);
						query.setString("roleguid", Runner.sAuthorizedRoleGuid);
						if(userauth.getAgent().length()>0) {
							query.setString("agent", userauth.getAgent());
						}
						if(userauth.getIdentifier().length()>0) {
							query.setString("identifier", userauth.getIdentifier());
						}
						if(userauth.getEmail().length()>0) {
							query.setString("email", userauth.getEmail());
						}
						if(userauth.getUsername().length()>0) {
							query.setString("username", userauth.getUsername());
						}
						Rbacuserauths auth = (Rbacuserauths)query.uniqueResult();
						if(auth!=null) {
							if(userauth.getUsername().length()>0 && auth.getUsername().equals(sUserName)) {
								result.put("msg", "用户名重复, [" + sUserName + "]属于组:" +dbsession.createQuery("select rolename from Rbacrole where roleguid in ( select id.roleguid from Rbacroleuser where id.userguid=:uid)").setParameter("uid", auth.getUserguid()).list().toString());
							}else if(userauth.getAgent().length()>0 && auth.getAgent().equals(sAgent)) {
								result.put("msg", "工号重复, [" + sAgent + "]属于组:" +dbsession.createQuery("select rolename from Rbacrole where roleguid in ( select id.roleguid from Rbacroleuser where id.userguid=:uid)").setParameter("uid", auth.getUserguid()).list().toString());
							}else if(userauth.getIdentifier().length()>0 && auth.getIdentifier().equals(sIdentifier)) {
								result.put("msg", "证件重复, [" + sIdentifier + "]属于组:" +dbsession.createQuery("select rolename from Rbacrole where roleguid in ( select id.roleguid from Rbacroleuser where id.userguid=:uid)").setParameter("uid", auth.getUserguid()).list().toString());
							}else if(userauth.getEmail().length()>0 && auth.getEmail().equals(sEmail)) {
								result.put("msg", "邮箱地址重复, [" + sEmail + "]属于组:" +dbsession.createQuery("select rolename from Rbacrole where roleguid in ( select id.roleguid from Rbacroleuser where id.userguid=:uid)").setParameter("uid", auth.getUserguid()).list().toString());
							}
							ts.rollback();
						}else {
							Rbacuser user = new Rbacuser();
							user.setHeadimgurl((sHeadimgurl!=null && sHeadimgurl.length()>0)?sHeadimgurl:"");
							user.setNickname((sNickname!=null && sNickname.length()>0) ? sNickname : sUserName);
							user.setUserguid(UUID.randomUUID().toString().toUpperCase());
							if(sPassword!=null && sPassword.length()>0) {
								tab.configServer.ValueString cause = new tab.configServer.ValueString(StringUtils.EMPTY);
								String sEncryptPassword = Util.encryptPassword(userauth.getUsername(),userauth.getUserguid(),sPassword,sVerifycode,cause);
								if(sEncryptPassword!=null && sEncryptPassword.length()>0) {
									userauth.setPassword(sEncryptPassword);
									userauth.setStatus(userauth.getStatus()|0x2);
								}else {
									ts.rollback();
									dbsession.close();
									log.warn(cause.value);
									result.put("msg", Runner.sErrorMessage);
									return Response.status(200).entity(result).build();
								}
							}else {
								userauth.setPassword("");
							}
							dbsession.save(user);
							dbsession.flush();
							Rbacroleuser roleuser = new Rbacroleuser();
							RbacroleuserId roleuserid = new RbacroleuserId();
							roleuserid.setRoleguid(sRoleGuid);
							roleuserid.setUserguid(user.getUserguid());
							roleuser.setId(roleuserid);
							dbsession.save(roleuser);
							userauth.setUserguid(user.getUserguid());
							userauth.setRoleguid(Runner.sAuthorizedRoleGuid);
							userauth.setWeixinid("");
							userauth.setAlipayid("");
							dbsession.save(userauth);
							AuditLog("create", "Rbacuser", userauth.getUserguid(), 1,String.format("{\"username\":\"%s\",\"roleguid\":\"%s\",\"create\":true}",userauth.getUsername(),Runner.sAuthorizedRoleGuid ), dbsession,ts,userauth.getUserguid());
							ts.commit();
							result.put("userguid", user.getUserguid());
							result.put("success", true);
						}
					}else {
						Rbacuser user = new Rbacuser();
						user.setHeadimgurl((sHeadimgurl!=null && sHeadimgurl.length()>0)?sHeadimgurl:"");
						user.setNickname((sNickname!=null && sNickname.length()>0) ? sNickname : sUserName);
						user.setUserguid(UUID.randomUUID().toString().toUpperCase());
						dbsession.save(user);
						dbsession.flush();
						Rbacroleuser roleuser = new Rbacroleuser();
						RbacroleuserId roleuserid = new RbacroleuserId();
						roleuserid.setRoleguid(sRoleGuid);
						roleuserid.setUserguid(user.getUserguid());
						roleuser.setId(roleuserid);
						dbsession.save(roleuser);
						ts.commit();
						result.put("success", true);
					}
				}catch(org.hibernate.HibernateException e){
					if ( ts.getStatus() == TransactionStatus.ACTIVE
							|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
						ts.rollback();
					}else ts.commit();
					log.warn("ERROR:",e);
					result.put("msg", Runner.sErrorMessage);
				}catch(Throwable e){
					if ( ts.getStatus() == TransactionStatus.ACTIVE
							|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
						ts.rollback();
					}else ts.commit();
					log.warn("ERROR:",e);
					result.put("msg", Runner.sErrorMessage);
				}
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	@POST
    @Path("/UIUpdateNickname")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIUpdateNickname(@Context HttpServletRequest R,@FormParam("userguid")String sUserGuid,
    		@FormParam("nickname")String sNickname){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		if(!sUserGuid.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
			return Response.status(200).entity(result).build();
		}
		if(sNickname==null || sNickname.length()==0) {
			return Response.status(200).entity(result).build();
		}
		//String sUId = Util.ObjectToString(httpsession.getAttribute("uid"));
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			if(!tab.rbac.RbacSystem.getUidOperations(Util.ObjectToString(httpsession.getAttribute("uid")), dbsession).contains("6B46C170-833F-413C-9D90-D887817B3E34")) {
				dbsession.close();
				return Response.status(200).entity(result).build();
			}
			try {
				Transaction ts = dbsession.beginTransaction();
				Rbacuser user = (Rbacuser)dbsession.createQuery(" from Rbacuser where userguid=:userguid").setString("userguid",sUserGuid).uniqueResult();
				if(user!=null) {
					user.setNickname(sNickname);
					dbsession.update(user);
					ts.commit();
					result.put("success", true);
				}else {
					ts.rollback();
				}
			}catch(Throwable e){
				log.warn("ERROR:",e);		
				result.put("msg", Runner.sErrorMessage);
			}
			dbsession.close();
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
	}
	//API文档: (value="修改用户信息", notes = "仅修改所属组的子组用户信息")
	@POST
    @Path("/UIUpdateUser")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIUpdateUser(@Context HttpServletRequest R,@FormParam("userguid")String sUserGuid,
    		@FormParam("agent")String sAgent,@FormParam("identifier")String sIdentifier,
    		@FormParam("mobile")String sMobile,@FormParam("email")String sEmail,
    		@FormParam("username")String sUserName,@FormParam("password")String sNewPassword,
    		@FormParam("verifycode") @DefaultValue("") String sVerifycode,
    		@FormParam("activate") /*"状态激活"*/Boolean bActivate){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		if(!sUserGuid.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
			return Response.status(200).entity(result).build();
		}
		String sUId = Util.ObjectToString(httpsession.getAttribute("uid"));
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			if(!tab.rbac.RbacSystem.getUidOperations(Util.ObjectToString(httpsession.getAttribute("uid")), dbsession).contains("6B46C170-833F-413C-9D90-D887817B3E34")) {
				dbsession.close();
				return Response.status(200).entity(result).build();
			}
			java.util.Set<String> sUIdRoles = getRoleGuidsForRole(sUId,null,dbsession,true);
			if(!IsTopUser(sUId,sUserGuid,dbsession,sUIdRoles)) {
				if(getUserGuidsForRole(sUId,null,dbsession,true).contains(sUserGuid)) {
					boolean  bChangeUser = false;
					boolean bChangePassword = false;
					if(sAgent!=null || sIdentifier!=null || sMobile!=null  || sEmail!=null || sUserName!=null || (sNewPassword!=null && sNewPassword.length()>0) || bActivate!=null)
					{
						Transaction ts = dbsession.beginTransaction();
						try {
							Rbacuserauths userauth = (Rbacuserauths)dbsession.createQuery(" from Rbacuserauths where userguid=:UserGuid").setString("UserGuid", sUserGuid).uniqueResult();
							if(userauth==null) {
								userauth = new Rbacuserauths();
								userauth.setCreatedate(Calendar.getInstance().getTime());
								userauth.setUpdatedate(userauth.getCreatedate());
								if(bActivate!=null)userauth.setStatus(bActivate?1:0);
								else userauth.setStatus(1);
								userauth.setUserguid(sUserGuid);
								if(sNewPassword!=null && sNewPassword.length()>0) {
									tab.configServer.ValueString cause = new tab.configServer.ValueString(StringUtils.EMPTY);
									String sEncryptPassword = Util.encryptPassword(userauth.getUsername(),userauth.getUserguid(),sNewPassword,sVerifycode,cause);
									if(sEncryptPassword!=null && sEncryptPassword.length()>0) {
										userauth.setPassword(sEncryptPassword);
										userauth.setStatus(userauth.getStatus()|0x2);
										bChangePassword = true;
									}else {
										ts.rollback();
										dbsession.close();
										log.warn(cause.value);
										result.put("msg", Runner.sErrorMessage);
										return Response.status(200).entity(result).build();
									}
								}	
								userauth.setAgent(sAgent!=null && StringUtils.isAlphanumeric(sAgent)?sAgent:"");
								if(sEmail!=null) {
									if(StringUtils.isAlphanumeric(sEmail)) {
										userauth.setEmail(sEmail);
									}else {
										userauth.setEmail(ESAPI.validator().getValidInput("User Email", sEmail, "Email", 128, true));
									}
								}else {
									userauth.setEmail(StringUtils.EMPTY);
								}
								userauth.setIdentifier(sIdentifier!=null && StringUtils.isAlphanumeric(sIdentifier)?sIdentifier:"");
								userauth.setMobile(sMobile!=null && StringUtils.isAlphanumeric(sMobile)?sMobile:"");
								userauth.setUsername(sUserName!=null && StringUtils.isAlphanumeric(sUserName)?sUserName:"");
								userauth.setLogindate(Calendar.getInstance().getTime());
								userauth.setRoleguid(Runner.sAuthorizedRoleGuid);
								userauth.setWeixinid("");
								userauth.setAlipayid("");
								bChangeUser = true;
							}else {
								if(sAgent!=null && sAgent.equals(userauth.getAgent())==false) {
									userauth.setAgent(sAgent);
									bChangeUser = true;
								}
								if(sIdentifier!=null && sIdentifier.equals(userauth.getIdentifier())==false) {
									userauth.setIdentifier(sIdentifier);
									bChangeUser = true;
								}
								if(sMobile!=null && sMobile.equals(userauth.getMobile())==false) {
									userauth.setMobile(sMobile);
									bChangeUser = true;
								}
								if(sEmail!=null && sEmail.equals(userauth.getEmail())==false) {
									if(StringUtils.isAlphanumeric(sEmail)) {
										userauth.setEmail(sEmail);
									}else {
										userauth.setEmail(ESAPI.validator().getValidInput("User Email", sEmail, "Email", 128, true));
									}
									bChangeUser = true;
								}
								if(sNewPassword!=null && sNewPassword.length()>0) {
									tab.configServer.ValueString cause = new tab.configServer.ValueString(StringUtils.EMPTY);
									String sEncryptPassword = Util.encryptPassword(userauth.getUsername(),userauth.getUserguid(),sNewPassword,sVerifycode,cause);
									if(sEncryptPassword!=null && sEncryptPassword.length()>0) {
										userauth.setPassword(sEncryptPassword);
										userauth.setStatus(userauth.getStatus()|0x2);
										bChangeUser = true;
										bChangePassword = true;
									}else {
										ts.rollback();
										dbsession.close();
										log.warn(cause.value);
										result.put("msg", Runner.sErrorMessage);
										return Response.status(200).entity(result).build();
									}
								}
								if(sUserName!=null && sUserName.equals(userauth.getUsername())==false) {
									userauth.setUsername(sUserName);
									bChangeUser = true;
								}
								if(bActivate!=null) {
									userauth.setStatus(bActivate?(userauth.getStatus()|0x1):(userauth.getStatus()&0xFFFE));
									bChangeUser = true;
								}
							}
							String sQuery = " from Rbacuserauths where roleguid=:roleguid and userguid!=:userguid and ";
							boolean bOr = false;
							if(userauth.getAgent().length()>0) {
								sQuery += (bOr?" or ":" ( ") + "agent=:agent";
								bOr = true;
							}
							if(userauth.getIdentifier().length()>0) {
								sQuery += (bOr?" or ":" ( ") + "identifier=:identifier";
								bOr = true;
							}
							if(userauth.getEmail().length()>0) {
								sQuery += (bOr?" or ":" ( ") + "email=:email";
								bOr = true;
							}
							if(userauth.getUsername().length()>0) {
								sQuery += (bOr?" or ":" ( ") + "username=:username";
								bOr = true;
							}
							if(bOr) {
								sQuery += " )";
							}
							Query query = dbsession.createQuery(sQuery);
							query.setString("roleguid", ROOT_ROLEGUID);
							query.setString("userguid", userauth.getUserguid());
							if(userauth.getAgent().length()>0) {
								query.setString("agent", userauth.getAgent());
							}
							if(userauth.getIdentifier().length()>0) {
								query.setString("identifier", userauth.getIdentifier());
							}
							if(userauth.getEmail().length()>0) {
								query.setString("email", userauth.getEmail());
							}
							if(userauth.getUsername().length()>0) {
								query.setString("username", userauth.getUsername());
							}
							Rbacuserauths auth = (Rbacuserauths)query.uniqueResult();		
							if(auth!=null) {
								if(userauth.getUsername().length()>0 && auth.getUsername().equals(sUserName)) {
									result.put("msg", "用户名重复, [" + sUserName + "]属于组:" +dbsession.createQuery("select rolename from Rbacrole where roleguid in ( select id.roleguid from Rbacroleuser where id.userguid=:uid)").setParameter("uid", auth.getUserguid()).list().toString());
								}else if(userauth.getAgent().length()>0 && auth.getAgent().equals(sAgent)) {
									result.put("msg", "工号重复, [" + sAgent + "]属于组:" +dbsession.createQuery("select rolename from Rbacrole where roleguid in ( select id.roleguid from Rbacroleuser where id.userguid=:uid)").setParameter("uid", auth.getUserguid()).list().toString());
								}else if(userauth.getIdentifier().length()>0 && auth.getIdentifier().equals(sIdentifier)) {
									result.put("msg", "证件重复, [" + sIdentifier + "]属于组:" +dbsession.createQuery("select rolename from Rbacrole where roleguid in ( select id.roleguid from Rbacroleuser where id.userguid=:uid)").setParameter("uid", auth.getUserguid()).list().toString());
								}else if(userauth.getEmail().length()>0 && auth.getEmail().equals(sEmail)) {
									result.put("msg", "邮箱地址重复, [" + sEmail + "]属于组:" +dbsession.createQuery("select rolename from Rbacrole where roleguid in ( select id.roleguid from Rbacroleuser where id.userguid=:uid)").setParameter("uid", auth.getUserguid()).list().toString());
								}
								ts.rollback();
							}else {
								if(bChangeUser) {
									dbsession.saveOrUpdate(userauth);
									AuditLog("update", "Rbacuser", userauth.getUserguid(), 1,String.format("{\"username\":\"%s\",\"roleguid\":\"%s\",\"update\":true}",userauth.getUsername(),Runner.sAuthorizedRoleGuid ), dbsession,ts,userauth.getUserguid());
									if(bChangePassword) {
										AuditLog("resetpassword", "Rbacuser", userauth.getUserguid(), 1,String.format("{\"username\":\"%s\",\"roleguid\":\"%s\",\"update\":true}",userauth.getUsername(),Runner.sAuthorizedRoleGuid ), dbsession,ts,userauth.getUserguid());
									}
									ts.commit();
									result.put("userguid", userauth.getUserguid());
									result.put("success", true);
								}else {
									ts.rollback();
									result.put("msg", "没有修改信息");
								}
							}
						}catch(org.hibernate.HibernateException e){
							if ( ts.getStatus() == TransactionStatus.ACTIVE
									|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
								ts.rollback();
							}else ts.commit();
							log.warn("ERROR:",e);
							result.put("msg", Runner.sErrorMessage);
						}catch(Throwable e){
							if ( ts.getStatus() == TransactionStatus.ACTIVE
									|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
								ts.rollback();
							}else ts.commit();
							log.warn("ERROR:",e);
							result.put("msg", Runner.sErrorMessage);
						}
					}
				}else {
					result.put("success", false);
					result.put("msg", "没有权限修改信息");
				}
			}else {
				result.put("success", false);
				result.put("msg", "没有权限修改信息(本组成员需要更高级管理者,可管理子组成员)");
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	//API文档: (value="改变用户所属组", notes = "可移动所属组的子组用户,可复制所属组的子组用户和本组用户")
	@POST
    @Path("/UIChangeUserRole")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIChangeUserRole(@Context HttpServletRequest R,@FormParam("userguid")String sUserGuid,
    		@FormParam("newroleguid")String sNewRoleGuid,
    		@FormParam("roleguid") /*"源角色(组) 唯一标识, 如果不指定, 则复制到新的组"*/String sRoleGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			if(!tab.rbac.RbacSystem.getUidOperations(Util.ObjectToString(httpsession.getAttribute("uid")), dbsession).contains("6B46C170-833F-413C-9D90-D887817B3E34")) {
				dbsession.close();
				return Response.status(200).entity(result).build();
			}
			if(getUserGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")),null,dbsession, true).contains(sUserGuid) 
					&& getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")),sNewRoleGuid,dbsession,false).contains(sNewRoleGuid)) {
				Transaction ts = dbsession.beginTransaction();
				try{
					if(sRoleGuid!=null && sRoleGuid.length()>0) {
						if(dbsession.createQuery("delete Rbacroleuser where  id.userguid=:UserGuid and id.roleguid=:RoleGuid").setString("UserGuid",sUserGuid).setString("RoleGuid", sRoleGuid).executeUpdate()>0) {
							Rbacroleuser roleuser = new Rbacroleuser();
							RbacroleuserId roleuserid = new RbacroleuserId();
							roleuserid.setRoleguid(sNewRoleGuid);
							roleuserid.setUserguid(sUserGuid);
							roleuser.setId(roleuserid);
							dbsession.save(roleuser);
							ts.commit();
							result.put("success", true);
						}
					}else {
						Rbacroleuser roleuser = new Rbacroleuser();
						RbacroleuserId roleuserid = new RbacroleuserId();
						roleuserid.setRoleguid(sNewRoleGuid);
						roleuserid.setUserguid(sUserGuid);
						roleuser.setId(roleuserid);
						dbsession.save(roleuser);
						ts.commit();
						result.put("success", true);
					}
				}catch(org.hibernate.HibernateException e){
					if ( ts.getStatus() == TransactionStatus.ACTIVE
							|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
						ts.rollback();
					}else ts.commit();
					log.warn("ERROR:",e);
					result.put("msg", Runner.sErrorMessage);
				}catch(Throwable e){
					if ( ts.getStatus() == TransactionStatus.ACTIVE
							|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
						ts.rollback();
					}else ts.commit();
					log.warn("ERROR:",e);
					result.put("msg", Runner.sErrorMessage);
				}
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	//API文档: (value="删除用户", notes = "从所属组的子组中删除指定用户,如果由第三方界面管理用户,则仅删除用户权限和分组记录")
	@POST
    @Path("/UIRemoveUser")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIRemoveUser(@Context HttpServletRequest R,@FormParam("userguid")String sUserGuid,
    		@FormParam("roleguid")String sRoleGuid,@FormParam("all")Boolean bAll){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		result.put("msg", "无权限删除");
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		if(Util.ObjectToString(httpsession.getAttribute("uid")).equals(sUserGuid)){
			result.put("msg", "不能删除自己");
			return Response.status(200).entity(result).build();
		}
		try{
			String sUId = Util.ObjectToString(httpsession.getAttribute("uid"));
			Session dbsession = HibernateSessionFactory.getThreadSession();
			if(!tab.rbac.RbacSystem.getUidOperations(Util.ObjectToString(httpsession.getAttribute("uid")), dbsession).contains("6B46C170-833F-413C-9D90-D887817B3E34")) {
				dbsession.close();
				return Response.status(200).entity(result).build();
			}
			java.util.Set<String> sUIdRoles = getRoleGuidsForRole(sUId,null,dbsession,true);
			if(!IsTopUser(sUId,sUserGuid,dbsession,sUIdRoles)) {
				if(sUIdRoles.contains(sRoleGuid)) {
					Transaction ts = dbsession.beginTransaction();
					try{
						if(bAll!=null && bAll==false) {
							dbsession.createQuery("delete Rbacroleuser where id.userguid=:UserGuid and id.roleguid=:RoleGuid")
								.setString("UserGuid",sUserGuid).setString("RoleGuid", sRoleGuid).executeUpdate();
							if(Util.ObjectToNumber(dbsession.createQuery("select count(*) from Rbacroleuser where id.userguid=:UserGuid").setString("UserGuid",sUserGuid).uniqueResult(),0)==0) {
								dbsession.createQuery("delete Rbacuser where userguid=:UserGuid").setString("UserGuid",sUserGuid).executeUpdate();
								dbsession.createQuery("delete Rbacuserauths where userguid=:UserGuid").setString("UserGuid",sUserGuid).executeUpdate();
							}
						}else {
							dbsession.createQuery("delete Rbacuser where userguid=:UserGuid").setString("UserGuid",sUserGuid).executeUpdate();
							dbsession.createQuery("delete Rbacroleuser where id.userguid=:UserGuid").setString("UserGuid",sUserGuid).executeUpdate();
							dbsession.createQuery("delete Rbacuserauths where userguid=:UserGuid").setString("UserGuid",sUserGuid).executeUpdate();
						}
						ts.commit();
						result.put("success", true);
					}catch(org.hibernate.HibernateException e){
						if ( ts.getStatus() == TransactionStatus.ACTIVE
								|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
							ts.rollback();
						}else ts.commit();
						log.warn("ERROR:",e);
						result.put("msg", Runner.sErrorMessage);
					}catch(Throwable e){
						if ( ts.getStatus() == TransactionStatus.ACTIVE
								|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
							ts.rollback();
						}else ts.commit();
						log.warn("ERROR:",e);
						result.put("msg", Runner.sErrorMessage);
					}
				}else {
					result.put("msg", "没有权限删除用户(本组成员需要更高级管理者,可管理子组成员)");
				}
			}else {
				result.put("msg", "没有权限删除用户(本组成员需要更高级管理者,可管理子组成员)");
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	//API文档: (value="添加用户到指定组", notes = "该接口用于树形分组管理界面")
	@POST
    @Path("/UIAddUserToTreeNodes")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIAddUserToTreeNodes(@Context HttpServletRequest R,@FormParam("roleguid")String sRoleGuid
    		,@FormParam("userguid")String sUserGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			if(!tab.rbac.RbacSystem.getUidOperations(Util.ObjectToString(httpsession.getAttribute("uid")), dbsession).contains("6B46C170-833F-413C-9D90-D887817B3E34")) {
				dbsession.close();
				return Response.status(200).entity(result).build();
			}
			try{
				if(getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")),null,dbsession,true).contains(sRoleGuid)) {
					Transaction ts = dbsession.beginTransaction();
					try{
						Rbacroleuser roleuser = new Rbacroleuser();
						RbacroleuserId roleuserid = new RbacroleuserId();
						roleuserid.setRoleguid(sRoleGuid);
						if(sUserGuid!=null && sUserGuid.length()>0) {
							roleuserid.setUserguid(sUserGuid);
							roleuser.setId(roleuserid);
							dbsession.save(roleuser);
							ts.commit();
							result.put("success", true);
						}else {
							ts.rollback();
						}
					}catch(org.hibernate.HibernateException e){
						if ( ts.getStatus() == TransactionStatus.ACTIVE
								|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
							ts.rollback();
						}else ts.commit();
						log.warn("ERROR:",e);
						result.put("msg", Runner.sErrorMessage);
					}catch(Throwable e){
						if ( ts.getStatus() == TransactionStatus.ACTIVE
								|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
							ts.rollback();
						}else ts.commit();
						log.warn("ERROR:",e);
						result.put("msg", Runner.sErrorMessage);
					}
					dbsession.close();
					return Response.status(200).entity(result).build();
				}else {
					result.put("msg", "没有权限添加用户(用户可管理子组成员,本组成员需要更高管理者)");
				}
			}catch(org.hibernate.HibernateException e){
				log.warn("ERROR:",e);
				result.put("msg", "用户已经存在");
			}catch(Throwable e){
				log.warn("ERROR:",e);		
				result.put("msg", Runner.sErrorMessage);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	//API文档: (value="获取指定组下的用户列表", notes = "该接口用于树形分组管理")
	@POST
    @Path("/UIGetUsersTreeNodes")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIGetUsersTreeNodes(@Context HttpServletRequest R,@FormParam("roleguid")String sRoleGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			if(!tab.rbac.RbacSystem.getUidOperations(Util.ObjectToString(httpsession.getAttribute("uid")), dbsession).contains("6B46C170-833F-413C-9D90-D887817B3E34")) {
				dbsession.close();
				return Response.status(200).entity(result).build();
			}
			try{
				@SuppressWarnings("unchecked")
				java.util.List<Rbacuser> Users = dbsession.createQuery("select A from Rbacuser A, Rbacroleuser B where B.id.roleguid=:roleguid and B.id.userguid=A.userguid")
						.setString("roleguid", sRoleGuid).list();
				result.put("Users", Users);
				result.put("success", true);
			}catch(org.hibernate.HibernateException e){
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}catch(Throwable e){
				log.warn("ERROR:",e);		
				result.put("msg", Runner.sErrorMessage);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	//API文档: (value="获取未分配到组的用户列表", notes = "该接口用于树形分组管理")
	@POST
    @Path("/UIGetUsersNotAssigned")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIGetUsersNotAssigned(@Context HttpServletRequest R){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try{
				@SuppressWarnings("unchecked")
				java.util.List<Rbacuser> Users = dbsession.createQuery("select A from Rbacuser A where not exists(select B.id.userguid from Rbacroleuser B where B.id.userguid=A.userguid) and A.nickname!='' and A.nickname is not null").list();
				result.put("Users", Users);
				result.put("success", true);
			}catch(org.hibernate.HibernateException e){
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}catch(Throwable e){
				log.warn("ERROR:",e);		
				result.put("msg", Runner.sErrorMessage);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	//API文档: (value="修改指定组", notes = "该接口用于树形分组管理，修改组名称和继承标识")
	@POST
    @Path("/UIUpdateRoleInfo")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIUpdateRoleInfo(@Context HttpServletRequest R,@FormParam("roleguid")String sRoleGuid,
    		@FormParam("rolename")String sRoleName,@DefaultValue("0") @FormParam("inheritance")Boolean bInheritance){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		HttpSession httpsession = R.getSession();
		result.put("success", false);
		if (httpsession != null) {
			try {
				Session dbsession = HibernateSessionFactory.getThreadSession();
				if(!tab.rbac.RbacSystem.getUidOperations(Util.ObjectToString(httpsession.getAttribute("uid")), dbsession).contains("6B46C170-833F-413C-9D90-D887817B3E34")) {
					dbsession.close();
					return Response.status(200).entity(result).build();
				}
				try {
					Transaction ts = dbsession.beginTransaction();
					try{
						if(getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")),sRoleGuid,dbsession,false).contains(sRoleGuid)) {
							Query query = dbsession.createQuery(
									"select B from Rbacrole B where B.roleguid=:RoleGuid").setString("RoleGuid", sRoleGuid);
							Rbacrole role = (Rbacrole)query.uniqueResult();
							if(role!=null) {
								if(sRoleName!=null && sRoleName.length()>0) {
									role.setRolename(sRoleName);
								}
								if(bInheritance!=null) {
									role.setInheritance(bInheritance?1:0);
								}
								dbsession.update(role);
								ts.commit();
								result.put("success", true);
							}else {
								ts.rollback();
							}
						}
					}catch(org.hibernate.HibernateException e){
						if ( ts.getStatus() == TransactionStatus.ACTIVE
								|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
							ts.rollback();
						}else ts.commit();
						log.warn("ERROR:",e);
						result.put("msg", Runner.sErrorMessage);
					}catch(Throwable e){
						if ( ts.getStatus() == TransactionStatus.ACTIVE
								|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
							ts.rollback();
						}else ts.commit();
						log.warn("ERROR:",e);
						result.put("msg", Runner.sErrorMessage);
					}
				} catch (org.hibernate.HibernateException e) {
					log.warn("ERROR: sessionid=" + httpsession.getAttribute("uid"), e);
				} catch (Throwable e) {
					log.warn("ERROR: sessionid=" + httpsession.getAttribute("uid"), e);
				}
				dbsession.close();
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR: sessionid=" + httpsession.getAttribute("uid"), e);
			} catch (Throwable e) {
				log.warn("ERROR: ", e);
			}
		} else {
			log.error("ERROR: getSession() is null");
		}
		return Response.status(200).entity(result).build();
    }
	//API文档: (value="改变指定组的父组", notes = "该接口用于树形分组管理界面")
	@POST
    @Path("/UIChangeFatherRole")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIChangeFatherRole(@Context HttpServletRequest R,@FormParam("roleguid")String sRoleGuid,@FormParam("fatherroleguid")String sFatherRoleGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		HttpSession httpsession = R.getSession();
		result.put("success", false);
		if (httpsession != null) {
			try {
				Session dbsession = HibernateSessionFactory.getThreadSession();
				if(!tab.rbac.RbacSystem.getUidOperations(Util.ObjectToString(httpsession.getAttribute("uid")), dbsession).contains("6B46C170-833F-413C-9D90-D887817B3E34")) {
					dbsession.close();
					return Response.status(200).entity(result).build();
				}
				try {
					Transaction ts = dbsession.beginTransaction();
					try{
						if(getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")),sRoleGuid,dbsession,false).contains(sRoleGuid)) {
							Rbacrole role = (Rbacrole)dbsession.createQuery(
									"select B from Rbacrole B where B.roleguid=:RoleGuid")
										.setString("RoleGuid", sRoleGuid).uniqueResult();
							if(role!=null) {
								if(dbsession.createQuery(" from Rbacrole where roleguid=:RoleGuid").setString("RoleGuid", sFatherRoleGuid).uniqueResult()!=null) {
									role.setFatherroleguid(sFatherRoleGuid);
									dbsession.update(role);
									ts.commit();
									result.put("success", true);
								}else {
									ts.rollback();
								}
							}else {
								ts.rollback();
							}
						}else {
							ts.rollback();
						}
					}catch(org.hibernate.HibernateException e){
						if ( ts.getStatus() == TransactionStatus.ACTIVE
								|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
							ts.rollback();
						}else ts.commit();
						log.warn("ERROR:",e);
						result.put("msg", Runner.sErrorMessage);
					}catch(Throwable e){
						if ( ts.getStatus() == TransactionStatus.ACTIVE
								|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
							ts.rollback();
						}else ts.commit();
						log.warn("ERROR:",e);
						result.put("msg", Runner.sErrorMessage);
					}
				} catch (org.hibernate.HibernateException e) {
					log.warn("ERROR: sessionid=" + httpsession.getAttribute("uid"), e);
				} catch (Throwable e) {
					log.warn("ERROR: sessionid=" + httpsession.getAttribute("uid"), e);
				}
				dbsession.close();
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR: sessionid=" + httpsession.getAttribute("uid"), e);
			} catch (Throwable e) {
				log.warn("ERROR: ", e);
			}
		} else {
			log.error("ERROR: getSession() is null");
		}
		return Response.status(200).entity(result).build();
    }
	//API文档: (value="删除指定组", notes = "该接口用于树形分组管理")
	@POST
    @Path("/UIRemoveRole")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIRemoveRole(@Context HttpServletRequest R,@FormParam("roleguid")String sRoleGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		HttpSession httpsession = R.getSession();
		result.put("success", false);
		//TODO: 因为有权限获取组GUID，因此暂时不限制删除操作
		if (httpsession != null) {
			try {
				Session dbsession = HibernateSessionFactory.getThreadSession();
				if(!tab.rbac.RbacSystem.getUidOperations(Util.ObjectToString(httpsession.getAttribute("uid")), dbsession).contains("6B46C170-833F-413C-9D90-D887817B3E34")) {
					dbsession.close();
					return Response.status(200).entity(result).build();
				}
				try {
					boolean bRoleGuid = true;
					java.util.Set<String> RoleGuids = new java.util.HashSet<String>();
					Query query = dbsession.createQuery(
							"select B from Rbacroleuser A, Rbacrole B where A.id.userguid=:UserGuid and A.id.roleguid=B.roleguid");
					query.setString("UserGuid", Util.ObjectToString(httpsession.getAttribute("uid")));
					@SuppressWarnings("unchecked")
					java.util.List<Rbacrole> firstroles = query.list();
					for(int idx=0;idx<firstroles.size();++idx)
					{
						if(firstroles.get(idx).getRoleguid().equals(sRoleGuid)){
							bRoleGuid = false;
							Rbacrole firstrole = firstroles.get(idx);
							firstroles.clear();
							firstroles.add(firstrole);
							RoleGuids.add(sRoleGuid);
							break;
						}
					}
					Stack<Rbacrole> firststack = new Stack<Rbacrole>();
					firststack.addAll(firstroles);

					while (!firststack.isEmpty()) {
						Rbacrole role = (Rbacrole) firststack.firstElement();
						@SuppressWarnings("unchecked")
						java.util.List<Rbacrole> roles = dbsession.createCriteria(Rbacrole.class)
								.add(Restrictions.eq("fatherroleguid", role.getRoleguid())).list();
						firststack.remove(0);
						firststack.addAll(roles);
						for (int i = 0; i < roles.size(); ++i) {
							if (bRoleGuid) {
								if (roles.get(i).getRoleguid().equals(sRoleGuid)) {//遍历子组查找需要的组
									bRoleGuid = false;
									firststack.clear();
									firststack.addElement(roles.get(i));
									RoleGuids.add(roles.get(i).getRoleguid());
									break;
								}
							} else {
								RoleGuids.add(roles.get(i).getRoleguid());
							}
						}
					}
					Transaction ts = dbsession.beginTransaction();
					try{
						if(Util.ObjectToNumber(dbsession.createQuery("select count(*) from Rbacroleuser where id.roleguid in (:roles)").setParameterList("roles", RoleGuids).uniqueResult(),0)==0) {
							int nTotalCount = dbsession.createQuery("delete Rbacrole where roleguid in (:roles)").setParameterList("roles", RoleGuids).executeUpdate();
							if(nTotalCount>0) {
								dbsession.createQuery("delete Rbacroleinfo where roleguid in (:roles)").setParameterList("roles", RoleGuids).executeUpdate();
								dbsession.createQuery("delete Rbacroleoperation where id.roleguid in (:roles)").setParameterList("roles", RoleGuids).executeUpdate();
							}
							ts.commit();
							result.put("totalCount", nTotalCount);
							result.put("success", true);
						}else {
							result.put("msg", "先删除该组内的所有用户，才能删除该组");
							ts.rollback();
						}
					}catch(org.hibernate.HibernateException e){
						if ( ts.getStatus() == TransactionStatus.ACTIVE
								|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
							ts.rollback();
						}else ts.commit();
						log.warn("ERROR:",e);
						result.put("msg", Runner.sErrorMessage);
					}catch(Throwable e){
						if ( ts.getStatus() == TransactionStatus.ACTIVE
								|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
							ts.rollback();
						}else ts.commit();
						log.warn("ERROR:",e);
						result.put("msg", Runner.sErrorMessage);
					}
				} catch (org.hibernate.HibernateException e) {
					log.warn("ERROR: sessionid=" + httpsession.getAttribute("uid"), e);
				} catch (Throwable e) {
					log.warn("ERROR: sessionid=" + httpsession.getAttribute("uid"), e);
				}
				dbsession.close();
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR: sessionid=" + httpsession.getAttribute("uid"), e);
			} catch (Throwable e) {
				log.warn("ERROR: ", e);
			}
		} else {
			log.error("ERROR: getSession() is null");
		}
		return Response.status(200).entity(result).build();
    }
	//API文档: (value="获取组信息对应的APP信息", notes = "返回值: appid, 安全redirect_uri, 创建时间createdate, 鉴权的时间updatedate")
	@POST
    @Path("/UIGetAppInfo")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIGetRoleInfo(@Context HttpServletRequest R,@FormParam("roleguid") /*"角色(组) 唯一标识"*/String sRoleGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try{
				@SuppressWarnings("unchecked")
				java.util.List<String> roles = dbsession.createQuery(
						"select id.roleguid from Rbacroleuser where id.userguid=:UserGuid").setString("UserGuid", Util.ObjectToString(httpsession.getAttribute("uid"))).list();
				Object sFatherRoleGuid = sRoleGuid==null ? "" : sRoleGuid;
				do {
					sFatherRoleGuid = dbsession.createQuery(
							"select fatherroleguid from Rbacrole where roleguid=:RoleGuid").setString("RoleGuid", sFatherRoleGuid.toString())
							.uniqueResult();
					if(sFatherRoleGuid!=null && (sFatherRoleGuid.equals(Util.NONE_GUID) || roles.contains(sFatherRoleGuid))) {
						Rbacroleinfoex roleinfo = (Rbacroleinfoex)dbsession.createQuery(" from Rbacroleinfoex where roleguid=:RoleGuid").setString("RoleGuid", sRoleGuid).uniqueResult();
						if(roleinfo!=null) {					
							result.put("appid", Util.compressUUID(roleinfo.getRoleguid()));
							result.put("createdate", roleinfo.getCreatedate().getTime());
							result.put("updatedate", roleinfo.getUpdatedate().getTime());
							result.put("redirect_uri", roleinfo.getRedirecturi());
							result.put("third_appid", roleinfo.getAppid());
							result.put("channel", roleinfo.getChannel());
							result.put("imserver", roleinfo.getImserver());
							result.put("options", roleinfo.getOptions());
							result.put("skillname", roleinfo.getSkillname());
							if(StringUtils.isBlank(roleinfo.getSecret()) || Util.NONE_GUID.equals(roleinfo.getSecret())) {
								result.put("secret", StringUtils.EMPTY);
							}else {
								result.put("secret", Util.compressUUID(roleinfo.getSecret()));
								log.warn("appid:"+ result.get("appid") +", secret:"+Util.compressUUID(roleinfo.getSecret()));
							}
							result.put("success", true);
						}else {
							Transaction ts = dbsession.beginTransaction();
							try{
								roleinfo = new Rbacroleinfoex();
								roleinfo.setCreatedate(Calendar.getInstance().getTime());
								roleinfo.setRedirecturi(StringUtils.EMPTY);
								roleinfo.setRoleguid(sRoleGuid);
								roleinfo.setSecret(Util.NONE_GUID);
								roleinfo.setUpdatedate(roleinfo.getCreatedate());
								roleinfo.setAppid(StringUtils.EMPTY);
								roleinfo.setChannel(0);
								roleinfo.setImserver(StringUtils.EMPTY);
								roleinfo.setOptions(StringUtils.EMPTY);
								roleinfo.setRedirecturi(StringUtils.EMPTY);
								roleinfo.setSkillname(StringUtils.EMPTY);
								dbsession.save(roleinfo);
								ts.commit();
								result.put("appid", Util.compressUUID(roleinfo.getRoleguid()));
								result.put("createdate", roleinfo.getCreatedate().getTime());
								result.put("updatedate", roleinfo.getUpdatedate().getTime());
								result.put("redirect_uri", roleinfo.getRedirecturi());
								result.put("third_appid", roleinfo.getAppid());
								result.put("channel", roleinfo.getChannel());
								result.put("imserver", roleinfo.getImserver());
								result.put("options", roleinfo.getOptions());
								result.put("skillname", roleinfo.getSkillname());
								if(StringUtils.isBlank(roleinfo.getSecret()) || Util.NONE_GUID.equals(roleinfo.getSecret())) {
									result.put("secret", StringUtils.EMPTY);
								}else {
									result.put("secret", Util.compressUUID(roleinfo.getSecret()));
									log.warn("appid:"+ result.get("appid") +", secret:"+Util.compressUUID(roleinfo.getSecret()));
								}
								result.put("success", true);
							}catch(org.hibernate.HibernateException e){
								if ( ts.getStatus() == TransactionStatus.ACTIVE
										|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
									ts.rollback();
								}else ts.commit();
								log.warn("ERROR:",e);
								result.put("msg", Runner.sErrorMessage);
							}catch(Throwable e){
								if ( ts.getStatus() == TransactionStatus.ACTIVE
										|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
									ts.rollback();
								}else ts.commit();
								log.warn("ERROR:",e);
								result.put("msg", Runner.sErrorMessage);
							}
						}
						dbsession.close();
						return Response.status(200).entity(result).build();
					}
				}while(sFatherRoleGuid!=null);
			}catch(org.hibernate.HibernateException e){
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}catch(Throwable e){
				log.warn("ERROR:",e);		
				result.put("msg", Runner.sErrorMessage);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
	}
	//API文档: (value="更新组的APPID(应用唯一标识)和SECRET(安全码)", notes = "该接口用于树形分组管理，更新后原有的应用不能再继续获取访问权限，因此提醒用户慎重更新")
	@POST
    @Path("/UIUpdateAppSecretInfo")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIUpdateAppSecretInfo(@Context HttpServletRequest R,@FormParam("roleguid") /*"角色/组 唯一标识"*/String sRoleGuid){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			if(!tab.rbac.RbacSystem.getUidOperations(Util.ObjectToString(httpsession.getAttribute("uid")), dbsession).contains("6B46C170-833F-413C-9D90-D887817B3E34")) {
				dbsession.close();
				return Response.status(200).entity(result).build();
			}
			try{
				@SuppressWarnings("unchecked")
				java.util.List<String> roles = dbsession.createQuery(
						"select id.roleguid from Rbacroleuser where id.userguid=:UserGuid").setString("UserGuid", Util.ObjectToString(httpsession.getAttribute("uid"))).list();
				Object sFatherRoleGuid = sRoleGuid==null ? "" : sRoleGuid;
				do {
					sFatherRoleGuid = dbsession.createQuery(
							"select fatherroleguid from Rbacrole where roleguid=:RoleGuid").setString("RoleGuid", sFatherRoleGuid.toString())
							.uniqueResult();
					if(sFatherRoleGuid!=null && (sFatherRoleGuid.equals(Util.NONE_GUID) || roles.contains(sFatherRoleGuid))) {
						Transaction ts = dbsession.beginTransaction();
						try{
							Rbacroleinfoex roleinfo = (Rbacroleinfoex)dbsession.createQuery(" from Rbacroleinfoex where roleguid=:RoleGuid").setString("RoleGuid", sRoleGuid).uniqueResult();
							if(roleinfo==null) {
								roleinfo = new Rbacroleinfoex();
								roleinfo.setCreatedate(Calendar.getInstance().getTime());
								roleinfo.setRedirecturi(StringUtils.EMPTY);
								roleinfo.setRoleguid(sRoleGuid);
								roleinfo.setSecret(Util.NONE_GUID);
								roleinfo.setUpdatedate(roleinfo.getCreatedate());
								roleinfo.setAppid(StringUtils.EMPTY);
								roleinfo.setChannel(0);
								roleinfo.setImserver(StringUtils.EMPTY);
								roleinfo.setOptions(StringUtils.EMPTY);
								roleinfo.setRedirecturi(StringUtils.EMPTY);
								roleinfo.setSkillname(StringUtils.EMPTY);
							}
							roleinfo.setSecret(UUID.randomUUID().toString().toUpperCase());
							dbsession.saveOrUpdate(roleinfo);
							ts.commit();
							result.put("secret", Util.compressUUID(roleinfo.getSecret()));
							result.put("success", true);
							log.warn("UPDATE APPID:"+Util.compressUUID(sRoleGuid)+" SECRET:" + result.get("secret"));
						}catch(org.hibernate.HibernateException e){
							if ( ts.getStatus() == TransactionStatus.ACTIVE
									|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
								ts.rollback();
							}else ts.commit();
							log.warn("ERROR:",e);
							result.put("msg", Runner.sErrorMessage);
						}catch(Throwable e){
							if ( ts.getStatus() == TransactionStatus.ACTIVE
									|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
								ts.rollback();
							}else ts.commit();
							log.warn("ERROR:",e);
							result.put("msg", Runner.sErrorMessage);
						}
						dbsession.close();
						return Response.status(200).entity(result).build();
					}
				}while(sFatherRoleGuid!=null);
			}catch(org.hibernate.HibernateException e){
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}catch(Throwable e){
				log.warn("ERROR:",e);		
				result.put("msg", Runner.sErrorMessage);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	@POST
    @Path("/UIUpdateAppInfo")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIUpdateAppInfo(@Context HttpServletRequest R,@FormParam("roleguid") String sRoleGuid,
    		@FormParam("appid") String appid,@FormParam("channel") Integer channel,@FormParam("imserver") String imserver
    		,@FormParam("options") String options,@FormParam("skillname") String skillname,@FormParam("redirect_uri") String redirect_uri){
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if(httpsession==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			if(!tab.rbac.RbacSystem.getUidOperations(Util.ObjectToString(httpsession.getAttribute("uid")), dbsession).contains("6B46C170-833F-413C-9D90-D887817B3E34")) {
				dbsession.close();
				return Response.status(200).entity(result).build();
			}
			try{
				@SuppressWarnings("unchecked")
				java.util.List<String> roles = dbsession.createQuery(
						"select id.roleguid from Rbacroleuser where id.userguid=:UserGuid").setString("UserGuid", Util.ObjectToString(httpsession.getAttribute("uid"))).list();
				Object sFatherRoleGuid = sRoleGuid==null ? "" : sRoleGuid;
				do {
					sFatherRoleGuid = dbsession.createQuery(
							"select fatherroleguid from Rbacrole where roleguid=:RoleGuid").setString("RoleGuid", sFatherRoleGuid.toString())
							.uniqueResult();
					if(sFatherRoleGuid!=null && (sFatherRoleGuid.equals(Util.NONE_GUID) || roles.contains(sFatherRoleGuid))) {
						Transaction ts = dbsession.beginTransaction();
						try{
							Rbacroleinfoex roleinfo = (Rbacroleinfoex)dbsession.createQuery(" from Rbacroleinfoex where roleguid=:RoleGuid").setString("RoleGuid", sRoleGuid).uniqueResult();
							if(roleinfo==null) {
								roleinfo = new Rbacroleinfoex();
								roleinfo.setCreatedate(Calendar.getInstance().getTime());
								roleinfo.setRoleguid(sRoleGuid);
								roleinfo.setUpdatedate(roleinfo.getCreatedate());
							}
							if(StringUtils.isBlank(redirect_uri)) {
								roleinfo.setRedirecturi(StringUtils.EMPTY);
							}else {
								roleinfo.setRedirecturi(redirect_uri);
							}
							if(StringUtils.isBlank(appid)) {
								roleinfo.setAppid(StringUtils.EMPTY);
							}else {
								roleinfo.setAppid(appid);
							}
							if(channel==null) {
								roleinfo.setChannel(0);
							}else {
								roleinfo.setChannel(channel);
							}
							if(StringUtils.isBlank(imserver)) {
								roleinfo.setImserver(StringUtils.EMPTY);
							}else {
								roleinfo.setImserver(imserver);
							}
							if(StringUtils.isBlank(options)) {
								roleinfo.setOptions(StringUtils.EMPTY);
							}else {
								roleinfo.setOptions(options);
							}
							if(StringUtils.isBlank(skillname)) {
								roleinfo.setSkillname(StringUtils.EMPTY);
							}else {
								roleinfo.setSkillname(skillname);
							}
							dbsession.saveOrUpdate(roleinfo);
							ts.commit();
							if(StringUtils.isBlank(roleinfo.getSecret()) || Util.NONE_GUID.equals(roleinfo.getSecret())) {
								result.put("secret", StringUtils.EMPTY);
							}else {
								result.put("secret", Util.compressUUID(roleinfo.getSecret()));
								log.warn("appid:"+ result.get("appid") +", secret:"+Util.compressUUID(roleinfo.getSecret()));
							}
							result.put("success", true);
						}catch(org.hibernate.HibernateException e){
							if ( ts.getStatus() == TransactionStatus.ACTIVE
									|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
								ts.rollback();
							}else ts.commit();
							log.warn("ERROR:",e);
							result.put("msg", Runner.sErrorMessage);
						}catch(Throwable e){
							if ( ts.getStatus() == TransactionStatus.ACTIVE
									|| ts.getStatus() == TransactionStatus.MARKED_ROLLBACK ) {
								ts.rollback();
							}else ts.commit();
							log.warn("ERROR:",e);
							result.put("msg", Runner.sErrorMessage);
						}
						dbsession.close();
						return Response.status(200).entity(result).build();
					}
				}while(sFatherRoleGuid!=null);
			}catch(org.hibernate.HibernateException e){
				log.warn("ERROR:",e);
				result.put("msg", Runner.sErrorMessage);
			}catch(Throwable e){
				log.warn("ERROR:",e);		
				result.put("msg", Runner.sErrorMessage);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.warn("ERROR:",e);
			result.put("msg", Runner.sErrorMessage);
		}catch(Throwable e){
			log.warn("ERROR:",e);		
			result.put("msg", Runner.sErrorMessage);
		}
		return Response.status(200).entity(result).build();
    }
	@POST
    @Path("/AliyunAccessToken")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response AliyunAccessToken(@FormParam("app")String sApp){
		return Response.status(200).entity(AliyunToken.getInstance().getAccessToken()).build();
	}
	
	// 获取用户管理的所有非技能组 用于痛快部门 如果是权限系统就返回其下所有部门，否则返回本部门和其下所有部门
	public static java.util.ArrayList<Rbacrole> getUserDepartment(String uid) {
		java.util.ArrayList<Rbacrole> groupset = new java.util.ArrayList<Rbacrole>();
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Query query = dbsession.createQuery(
						"select B from Rbacroleuser A, Rbacrole B where A.id.userguid=:UserGuid and A.id.roleguid=B.roleguid and bitand(B.inheritance,2)!=2");
				query.setString("UserGuid", Util.ObjectToString(uid));
				@SuppressWarnings("unchecked")
				java.util.List<Rbacrole> firstroles = query.list();
				Stack<Rbacrole> firststack = new Stack<Rbacrole>();
				firststack.addAll(firstroles);

				java.util.HashMap<String, Rbacrole> mapRoles = new java.util.HashMap<String, Rbacrole>();
				while (!firststack.isEmpty()) {
					Rbacrole role = (Rbacrole) firststack.firstElement();
					@SuppressWarnings("unchecked")
					java.util.List<Rbacrole> roles = dbsession
							.createQuery(
									" from Rbacrole where fatherroleguid=:fatherroleguid and bitand(inheritance,2)!=2")
							.setString("fatherroleguid", role.getRoleguid()).list();
					firststack.remove(0);
					firststack.addAll(roles);
					for (Rbacrole r : roles) {
						groupset.add(r);
					}
				}
				for (Rbacrole r : firstroles) {
					if (!r.getRoleguid().equals(Util.ROOT_ROLEGUID)) {
						mapRoles.put(r.getRoleguid(), r);
					}
				}
			} catch (org.hibernate.HibernateException e) {
			} catch (Throwable e) {
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return groupset;

	}
	
	// 查询角色
	public static Rbacrole getrole(String roleguid) {
		Rbacrole roles = null;
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Rbacrole role = (Rbacrole) dbsession.createQuery(" from Rbacrole where roleguid=:roleguid )")
						.setString("roleguid", roleguid).uniqueResult();
				roles = role;
			} catch (org.hibernate.HibernateException e) {
			} catch (Throwable e) {
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return roles;

	}
	// 获取组下面的技能组
	public static String getDeptQueues(String groupid) {
		String returnstr = "";
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				@SuppressWarnings("unchecked")
				java.util.List<Rbacrole> roles = dbsession
						.createQuery(" from Rbacrole where fatherroleguid=:fatherroleguid and bitand(inheritance,2)=2")
						.setString("fatherroleguid", groupid).list();
				if (roles.size() == 0) {
					return "('填充队列')";

				} else if (roles.size() == 1) {
					returnstr = "('" + roles.get(0).getRolename() + "')";
				} else {
					returnstr += "('" + roles.get(0).getRolename() + "'";
					for (int i = 1; i < roles.size(); i++) {
						returnstr += ",'" + roles.get(i).getRolename() + "'";
					}
					returnstr += ")";
				}

			} catch (org.hibernate.HibernateException e) {
			} catch (Throwable e) {
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return returnstr;
	}
	// 获取用户管理的坐席
	public static java.util.List<String> GetUsersAgentsFormanageable(String uid, String sRoleGuid, boolean management) {
		java.util.List<String> useragent = null;
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				java.util.Set<String> sRoleGuids = getRoleGuidsForRole(uid, sRoleGuid, dbsession, management);
				log.info("输出用户可管理组:" + sRoleGuids.toString());
				if (sRoleGuids.size() > 0) {
					@SuppressWarnings("unchecked")
					java.util.List<Rbacuserauths> authusers = dbsession.createQuery(
							" from Rbacuserauths where userguid in (select id.userguid from Rbacroleuser where id.roleguid in (:RoleGuids))")
							.setParameterList("RoleGuids", sRoleGuids).list();
					useragent = new java.util.ArrayList<String>();
					for (Rbacuserauths authuser : authusers) {
						useragent.add(authuser.getAgent());
					}

				} else {
					log.warn("用户可管理组数量为0");
				}

			} catch (org.hibernate.HibernateException e) {
			} catch (Throwable e) {
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return useragent;
	}
	// 获取用户管理的所有非技能组 用于痛快部门 如果是权限系统就返回其下所有部门的名字，否则返回本部门的名字和其下所有部门的名字
	// 前端已经确保了不会传权限系统组名过来
	public static java.util.ArrayList<String> getUserDepartmentName(String uid, String groupguid) {
		java.util.ArrayList<String> groupset = new java.util.ArrayList<String>();
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				if (groupguid != null && groupguid.length() > 0
						&& !groupguid.equals("00000000-0000-0000-0000-000000000000")) {
					Query query = dbsession.createQuery("from Rbacrole where roleguid=:roliguid");
					query.setParameter("roliguid", groupguid);
					java.util.List<Rbacrole> firstroles = query.list();
					for (Rbacrole r : firstroles) {
						groupset.add(r.getRolename());
					}
				} else {
					// 痛快要求报表组长也可以看到所有人的数据
					Query query = dbsession.createQuery("from Rbacrole B where  bitand(B.inheritance,2)!=2");
					java.util.List<Rbacrole> firstroles = query.list();
					for (Rbacrole r : firstroles) {
						groupset.add(r.getRolename());
					}

					// Query query = dbsession.createQuery(
					// "select B from Rbacroleuser A, Rbacrole B where A.id.userguid=:UserGuid and
					// A.id.roleguid=B.roleguid and bitand(B.inheritance,2)!=2");
					// query.setString("UserGuid", Util.ObjectToString(uid));
					// @SuppressWarnings("unchecked")
					// java.util.List<Rbacrole> firstroles = query.list();
					// Stack<Rbacrole> firststack = new Stack<Rbacrole>();
					// //添加本部门
					// firststack.addAll(firstroles);
					// for(Rbacrole r: firstroles) {
					// if(!(r.getRoleguid().equals("9A611B6F-5664-4C43-9D06-C1E2141CCCB1"))) {
					// groupset.add(r.getRolename());
					// }
					//
					// }
					// while (!firststack.isEmpty()) {
					// Rbacrole role = (Rbacrole) firststack.firstElement();
					// @SuppressWarnings("unchecked")
					// java.util.List<Rbacrole> roles = dbsession.createQuery(" from Rbacrole where
					// fatherroleguid=:fatherroleguid and
					// bitand(inheritance,2)!=2").setString("fatherroleguid",
					// role.getRoleguid()).list();
					// firststack.remove(0);
					// firststack.addAll(roles);
					// for(Rbacrole r: roles) {
					// groupset.add(r.getRolename());
					// }
					// }
				}

			} catch (org.hibernate.HibernateException e) {
			} catch (Throwable e) {
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return groupset;
	}
	// 根据组id获取队列名称
	public static String getUserQueues(String roleguid) {
		java.util.HashSet<String> mapRoles = new java.util.HashSet<String>();
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Query query = dbsession.createQuery(
						"from Rbacrole  where roleguid=:roleguid");
				query.setString("roleguid", Util.ObjectToString(roleguid));
				java.util.List<Rbacrole> firstroles =query.list();
				if(firstroles.size()>0) {
					Rbacrole role=firstroles.get(0);
					if((role.getInheritance()&2)==2) {
						return role.getRolename();
					}else {
						return null;
					}
				}
			} catch (org.hibernate.HibernateException e) {
			} catch (Throwable e) {
			}
			if(dbsession.isOpen()) {
				dbsession.close();
			}
			
		} catch (org.hibernate.HibernateException e) {
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return null;

	}
	// 通过部门和队列筛分机
	@SuppressWarnings("unchecked")
	public static java.util.List<Recextension> getQueuesExtension(String roleguid, String queues, boolean bManageable) {
		// 默认roleguid和queues不会同时为空
		java.util.List<Recextension> extensions = new java.util.ArrayList<Recextension>();
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			java.util.Set<String> sRoleGuids = null;
			try {
				if (queues != null && queues.length() > 0) {
					sRoleGuids = new HashSet<String>(Arrays.asList(queues.split(",")));
				} else {
					if (roleguid != null && roleguid.length() > 0) {
						sRoleGuids = getRoleGuidsForRole(roleguid, dbsession, bManageable);
					}
				}
				log.info("输出一下用户管理组:" + sRoleGuids.toString());
				extensions = dbsession
						.createQuery(" from Recextension where length(extension)>0 and roleguid in :roleguids")
						.setParameterList("roleguids", sRoleGuids).list();
				log.info("输出一下通过部门和队列筛选后的分机列表:" + extensions.toString());

			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR: ", e);
			} catch (Throwable e) {
				log.warn("ERROR: ", e);
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		return extensions;

	}
}
