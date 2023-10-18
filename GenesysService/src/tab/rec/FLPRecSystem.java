package tab.rec;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.reflect.TypeToken;

import hbm.OffersetTransformers;
import hbm.factory.GHibernateSessionFactory;
import hbm.factory.HibernateSessionFactory;
import hbm.model.Recfiles;
import hbm.model.RecfilesEx;
import hbm.model.RecphoneEx;
import main.Runner;
import tab.EXTJSSortParam;
import tab.GsonUtil;
import tab.RESTDateParam;
import tab.rbac.RbacClient;
import tab.util.Util;
import tab.util.chenUtil;

// (value = "飞利浦录音接口")
@Path("/FLPrec")
public class FLPRecSystem {
	public final static String sInteraction_type_name = "interaction_type";
	public static Log log = LogFactory.getLog(RecSystem.class);
	private static ObjectMapper mapper = new ObjectMapper();
	
	
	public java.util.List<java.util.Map<String, Object>> searchVoice(String phone,String hotline_num,RESTDateParam dtEndtime,RESTDateParam dtStarttime,Integer pageLimit,
			Integer pageStart,EXTJSSortParam Sort ,tab.configServer.ValueInteger  totalCount){
		java.util.List<java.util.Map<String, Object>> list=null;
		String countSelect="select count(*) from philips_record";
		String where ="  where record_start_time>:starttime and record_start_time<:endtime";
	
		if(phone!=null&&phone.length()>0) {
			
			where+=" and user_phone ilike '%"+phone+"%'" ;
		}
		if(hotline_num!=null&&hotline_num.length()>0) {
			where+=" and hotline_num ilike '%"+hotline_num+"%'" ;
		}
		
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				String countQuerystr=countSelect+where;
				log.info("countQuerystr:"+countQuerystr);
				Query countQuery = gdbsession.createSQLQuery(countQuerystr).setTimestamp("starttime", dtStarttime.getDate())
						.setTimestamp("endtime", dtEndtime.getDate());
				Integer nTotalRecords = Util
						.ObjectToNumber(countQuery.uniqueResult(), 0);
				Integer nFilteredRecords = nTotalRecords;
				if(nFilteredRecords>0) {
					totalCount.value=nFilteredRecords;
					String SelectStr="select record_start_time as starttime,record_end_time as endtime,user_phone as phone"
							+ ",duration as length,file_path as filepath,file_name as filename,hotline_num as hotline_num from philips_record A";
					String QueryStr=SelectStr+where;
					Query query = gdbsession.createSQLQuery(QueryStr);
					if(pageLimit>0) {
						query.setMaxResults(pageLimit);
					}
					query.setFirstResult(pageStart);
					query.setTimestamp("starttime", dtStarttime.getDate()).setTimestamp("endtime", dtEndtime.getDate());
					String sProperty = StringUtils.EMPTY;
					if (Sort != null) {
						switch (Sort.getsProperty()) {
						default:
						case "starttime":
							sProperty = "A.record_start_time";
							break;
						case "endtime":
							sProperty = "A.record_end_time";
							break;
						case "phone":
							sProperty = "A.phone";
							break;
						case "length":
							sProperty = "A.duration";
							break;
						}
					}
					if (!sProperty.isEmpty()) {
						QueryStr += " order by " + sProperty + " " + Sort.getsDirection();
					}
					log.info(QueryStr);
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					return CallRecordList;
				}else {
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
			}
			gdbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
		}
		return list;
		
	}
	@POST
	@Path("/UISearchVoicemail")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UISearchVoicemail(@Context HttpServletRequest R,
			@FormParam("starttime") /* "开始时间" */RESTDateParam dtStarttime,
			@FormParam("endtime") /* "结束时间" */RESTDateParam dtEndtime,
			@FormParam("phone") /* "通过主叫号码模糊查询" */String phone, 
			@FormParam("hotline_num") /* "通过被叫号码模糊查询" */String hotline_num, 
			@FormParam("start") /* "开始页" */Integer pageStart, @FormParam("limit") /* "分页大小" */Integer pageLimit,
			@FormParam("sort") /* "排序(ExtJS默认格式, {\"property\":\"字段名\",\"direction\":\"ASC\"})" */EXTJSSortParam Sort) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		result.put("recordItems",null);
		result.put("totalCount", 0);
		HttpSession httpsession = R.getSession();
		if (httpsession == null || dtStarttime == null || dtEndtime == null) {
			return Response.status(200).entity(result).build();
		}
		
		tab.configServer.ValueInteger nTotalCount = new tab.configServer.ValueInteger(0);
		java.util.List<java.util.Map<String, Object>> list=searchVoice(phone,hotline_num,dtEndtime,dtStarttime,pageLimit,pageStart,Sort,nTotalCount);
		result.put("recordItems",list);
		result.put("totalCount", nTotalCount.value);
		return Response.status(200).entity(result).build();
	}
	@POST
	@Path("/UISearchAgentState")//坐席状态明细
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UISearchAgentState(@Context HttpServletRequest R,
			@FormParam("starttime") /* "开始时间" */RESTDateParam dtStarttime,
			@FormParam("endtime") /* "结束时间" */RESTDateParam dtEndtime,
			 @FormParam("agent") String sAgent, @FormParam("agentname") String agentname,
			@FormParam("start") /* "开始页" */Integer pageStart, @FormParam("limit") /* "分页大小" */Integer pageLimit,
			@FormParam("sort") /* "排序(ExtJS默认格式, {\"property\":\"字段名\",\"direction\":\"ASC\"})" */EXTJSSortParam Sort,
			@FormParam("script") String sScript){
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("list", new Recfiles[0]);
		result.put("totalCount", 0);
		HttpSession httpsession = R.getSession();
		if (httpsession == null || dtStarttime == null || dtEndtime == null) {
			return Response.status(200).entity(result).build();
		}
		String countSelect="select count(*) from t_philips_agentstate";
		String where ="  where start_time>:starttime and start_time<:endtime";
		if(sAgent!=null&&sAgent.length()>0) {
			where+=" and agent_name ilike '%"+sAgent+"%'" ;
		}
		if(agentname!=null&&agentname.length()>0) {
			where+=" and user_name ilike '%"+agentname+"%'" ;
		}
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			Session dbsession = HibernateSessionFactory.getThreadSession();
			String sUId=Util.ObjectToString(httpsession.getAttribute("uid"));
			tab.configServer.ValueInteger bRoles = new tab.configServer.ValueInteger(0);
			java.util.Set<String> agentList=chenUtil.Getagentlist(sUId,null,bRoles,dbsession);
			if(bRoles.value==1) {
				where+=" and agent_name in :agentlist ";
			}
			try {
				String countQuerystr=countSelect+where;
				log.info("坐席状态明细countQuerystr: "+countQuerystr);
				Query countQuery = gdbsession.createSQLQuery(countQuerystr).setTimestamp("starttime", dtStarttime.getDate())
						.setTimestamp("endtime", dtEndtime.getDate());
				if(bRoles.value==1) {
					countQuery.setParameterList("agentlist", agentList);
				}
				Integer nTotalRecords = Util
						.ObjectToNumber(countQuery.uniqueResult(), 0);
				Integer nFilteredRecords = nTotalRecords;
				if(nFilteredRecords>0) {
					String SelectStr="select A.* from t_philips_agentstate A";
					String QueryStr=SelectStr+where;
					Query query = gdbsession.createSQLQuery(QueryStr);
					if(bRoles.value==1) {
						query.setParameterList("agentlist", agentList);
					}
					if (sScript == null || sScript.length() < 0) {
						query.setFirstResult(pageStart).setMaxResults(pageLimit);
					}
					query.setTimestamp("starttime", dtStarttime.getDate()).setTimestamp("endtime", dtEndtime.getDate());
					String sProperty = StringUtils.EMPTY;
					if (Sort != null) {
						switch (Sort.getsProperty()) {
						default:
						case "start_time":
							sProperty = "A.start_time";
							break;
						case "stop_time":
							sProperty = "A.stop_time";
							break;
						case "duration":
							sProperty = "A.duration";
							break;
						case "agent":
							sProperty = "A.agent_name";
							break;
						}
					}
					if (!sProperty.isEmpty()) {
						QueryStr += " order by " + sProperty + " " + Sort.getsDirection();
					}
					log.info("坐席状态明细QueryStr:"+QueryStr);
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					
					if (sScript == null || sScript.length() == 0) {
						result.put("list", CallRecordList);
						result.put("totalCount", nFilteredRecords);
						result.put("success", true);
					} else {
						tab.MyExportExcelFile export = null;

						if (sScript == null || sScript.length() == 0) {
						} else {
							export = new tab.MyExportExcelFile();
							if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator")
									+ "WebRoot" + sScript)) {
								if(dbsession.isOpen()) {
									gdbsession.close();
								}
								if(dbsession.isOpen()) {
									dbsession.close();
								}
								return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain")
										.build();
							}
						}
						for (int i = 0; i < CallRecordList.size(); i++) {
							java.util.Map<String, Object> call = CallRecordList.get(i);
							if (sScript == null || sScript.length() == 0) {
							} else {
								export.CommitRow(call);
							}
						}
						String sFileName = export.GetFile();
						if (sFileName != null && sFileName.length() > 0) {
							java.io.File f = new java.io.File(sFileName);
							int pos = sFileName.lastIndexOf(".");
							if(dbsession.isOpen()) {
								gdbsession.close();
							}
							if(dbsession.isOpen()) {
								dbsession.close();
							}
							String filename=export.GetFileName();
							return Response.ok(f).header(HttpHeaders.CONTENT_TYPE, "application/csv;charset=UTF-8")
									.header("Content-Disposition",
											"attachment;filename=\""
													+ URLEncoder.encode("AgentState-", "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				}else {
					if (sScript == null || sScript.length() == 0) {
						result.put("list", null);
						result.put("totalCount", 0);
						result.put("success", true);
					}
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			if(dbsession.isOpen()) {
				gdbsession.close();
			}
			if(dbsession.isOpen()) {
				dbsession.close();
			}
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		if (sScript == null || sScript.length() == 0) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(404).entity("no datas").type("text/plain").build();
	}
	
	
	
	@POST
	@Path("/UISearchAgentLogin")//坐席登录明细
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UISearchAgentLogin(@Context HttpServletRequest R,
			@FormParam("starttime") /* "开始时间" */RESTDateParam dtStarttime,
			@FormParam("endtime") /* "结束时间" */RESTDateParam dtEndtime,
			 @FormParam("agent") String sAgent, @FormParam("agentname") String agentname,
			 @FormParam("tenant_name") String tenant_name,
			@FormParam("start") /* "开始页" */Integer pageStart, @FormParam("limit") /* "分页大小" */Integer pageLimit,
			@FormParam("sort") /* "排序(ExtJS默认格式, {\"property\":\"字段名\",\"direction\":\"ASC\"})" */EXTJSSortParam Sort,
			@FormParam("script") String sScript){
//坐席登录明细里面 resource_name代表人名  employee_id代表工号
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("list", new Recfiles[0]);
		result.put("totalCount", 0);
		HttpSession httpsession = R.getSession();
		if (httpsession == null || dtStarttime == null || dtEndtime == null) {
			return Response.status(200).entity(result).build();
		}
		String countSelect="select count(*) from t_philips_agentlogin";
		String where ="  where start_ts_time>:starttime and start_ts_time<:endtime";
		if(sAgent!=null&&sAgent.length()>0) {
			where+=" and employee_id ilike '%"+sAgent+"%'" ;
		}
		if(agentname!=null&&agentname.length()>0) {
			where+=" and resource_name ilike '%"+agentname+"%'" ;
		}
		if(tenant_name!=null&&tenant_name.length()>0) {
			where+=" and tenant_name ilike '%"+tenant_name+"%'" ;
		}
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			Session dbsession = HibernateSessionFactory.getThreadSession();
			String sUId=Util.ObjectToString(httpsession.getAttribute("uid"));
			tab.configServer.ValueInteger bRoles = new tab.configServer.ValueInteger(0);
			java.util.Set<String> agentList=chenUtil.Getagentlist(sUId,null,bRoles,dbsession);
			if(bRoles.value==1) {
				where+=" and employee_id in :agentlist ";
			}
			try {
				String countQuerystr=countSelect+where;
				log.info("坐席状态明细countQuerystr: "+countQuerystr);
				Query countQuery = gdbsession.createSQLQuery(countQuerystr).setTimestamp("starttime", dtStarttime.getDate())
						.setTimestamp("endtime", dtEndtime.getDate());
				if(bRoles.value==1) {
					countQuery.setParameterList("agentlist", agentList);
				}
				Integer nTotalRecords = Util
						.ObjectToNumber(countQuery.uniqueResult(), 0);
				Integer nFilteredRecords = nTotalRecords;
				if(nFilteredRecords>0) {
					String SelectStr="select A.* from t_philips_agentlogin A";
					String QueryStr=SelectStr+where;
					Query query = gdbsession.createSQLQuery(QueryStr);
					if(bRoles.value==1) {
						query.setParameterList("agentlist", agentList);
					}
					if (sScript == null || sScript.length() < 0) {
						query.setFirstResult(pageStart).setMaxResults(pageLimit);
					}
					query.setTimestamp("starttime", dtStarttime.getDate()).setTimestamp("endtime", dtEndtime.getDate());
					String sProperty = StringUtils.EMPTY;
					if (Sort != null) {
						switch (Sort.getsProperty()) {
						case "start_ts_time":
							sProperty = "A.start_ts_time";
							break;
						case "end_ts_time":
							sProperty = "A.end_ts_time";
							break;
						case "resource_name":
							sProperty = "A.resource_name";
							break;
						case "media_name":
							sProperty = "A.media_name";
							break;
						case "total_duration":
							sProperty = "A.total_duration";
							break;
						case "tenant_name":
							sProperty = "A.tenant_name";
							break;	
						default:
							sProperty = "A.start_time";
						break;
							
						}
					}
					if (!sProperty.isEmpty()) {
						QueryStr += " order by " + sProperty + " " + Sort.getsDirection();
					}
					log.info("坐席状态明细QueryStr:"+QueryStr);
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					
					if (sScript == null || sScript.length() == 0) {
						result.put("list", CallRecordList);
						result.put("totalCount", nFilteredRecords);
						result.put("success", true);
					} else {
						tab.MyExportExcelFile export = null;

						if (sScript == null || sScript.length() == 0) {
						} else {
							export = new tab.MyExportExcelFile();
							if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator")
									+ "WebRoot" + sScript)) {
								if(gdbsession.isOpen()) {
									gdbsession.close();
								}
								if(dbsession.isOpen()) {
									dbsession.close();
								}
								return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain")
										.build();
							}
						}
						for (int i = 0; i < CallRecordList.size(); i++) {
							java.util.Map<String, Object> call = CallRecordList.get(i);
							if (sScript == null || sScript.length() == 0) {
							} else {
								export.CommitRow(call);
							}
						}
						String sFileName = export.GetFile();
						if (sFileName != null && sFileName.length() > 0) {
							java.io.File f = new java.io.File(sFileName);
							int pos = sFileName.lastIndexOf(".");
							if(gdbsession.isOpen()) {
								gdbsession.close();
							}
							if(dbsession.isOpen()) {
								dbsession.close();
							}
							return Response.ok(f).header(HttpHeaders.CONTENT_TYPE, "application/csv;charset=UTF-8")
									.header("Content-Disposition",
											"attachment;filename=\""
													+ URLEncoder.encode("AgentLogin-", "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				}else {
					if (sScript == null || sScript.length() == 0) {
						result.put("list", null);
						result.put("totalCount", 0);
						result.put("success", true);
					}
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			if(gdbsession.isOpen()) {
				gdbsession.close();
			}
			if(dbsession.isOpen()) {
				dbsession.close();
			}
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		if (sScript == null || sScript.length() == 0) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(404).entity("no datas").type("text/plain").build();
	}
	
	@POST
	@Path("/UISearchCdrVoice")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UISearchCdrVoice(@Context HttpServletRequest R,
			@FormParam("starttime") /* "开始时间" */RESTDateParam dtStarttime,
			@FormParam("endtime") /* "结束时间" */RESTDateParam dtEndtime,
			@FormParam("caller") /* "通过主叫号码模糊查询" */String phone, 
			@FormParam("inbound") Integer nInbound, @FormParam("agent") String sAgent,
			@FormParam("workspaceAgent") Boolean workspaceAgent,
			@FormParam("agentname") String agentname,
			@FormParam("groupguid") String sGroupGuid,@FormParam("nType") Integer nType,
			@FormParam("start") /* "开始页" */Integer pageStart, @FormParam("limit") /* "分页大小" */Integer pageLimit,
			@FormParam("sort") /* "排序(ExtJS默认格式, {\"property\":\"字段名\",\"direction\":\"ASC\"})" */EXTJSSortParam Sort,@FormParam("queues") String sQueques,
			@FormParam("script") String sScript){
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		if (nInbound == null)
			nInbound = 0;
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("list", new Recfiles[0]);
		result.put("totalCount", 0);
		HttpSession httpsession = R.getSession();
		if (httpsession == null || dtStarttime == null || dtEndtime == null) {
			return Response.status(200).entity(result).build();
		}
		String countSelect="select count(*) from t_cdr_voice A ";
		String where ="  where start_date_time_string>:starttime and start_date_time_string<:endtime";
		if(phone!=null&&phone.length()>0) {
			where+=" and source_address ilike '%"+phone+"%'" ;
		}
		if(agentname!=null&&agentname.length()>0) {
			where+=" and agent_last_name ilike '%"+agentname+"%'" ;
		}
		if (sQueques != null && sQueques.length() > 0 && !(sQueques.equals("All"))) {
			where +=  " and A.queues in :queues ";
		}
		switch (tab.util.Util.ObjectToNumber(nType, 0)) {
		case 1:// 接听
			where += " and A.talk_duration>0 ";
			break;
		case 2:// 未接
			where += " and  A.talk_duration=0 ";
			break;
		default:
			break;
		}
		switch (nInbound) {
		case 1:// 呼入
			where += " and  upper(A." + RecSystem.sInteraction_type_name + ")='INBOUND' ";
			break;
		case 2:// 呼出
			where += " and  upper(A." + RecSystem.sInteraction_type_name + ")='OUTBOUND'";
			break;
		case 3:// 内线
			where += " and  upper(A." + RecSystem.sInteraction_type_name + ")='INTERNAL'";
			break;
		default:
			break;
		}
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			Session dbsession = HibernateSessionFactory.getThreadSession();
			boolean bRoles = false;//是否非系统组成员
			String sUId=Util.ObjectToString(httpsession.getAttribute("uid"));
			java.util.Set<String> roles = null;
			if (sUId != null && sUId.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
				log.info("通过uid权限查询所属组");
				roles = tab.rbac.RbacClient.getRoleGuidsForRole(sUId, sGroupGuid, dbsession,
						false);
			}
			if(workspaceAgent==true) {//从worspace查询的普通坐席只能看到自己的记录
				java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(RbacClient.getUserAuths(sUId, sUId),new TypeToken<java.util.Map<String, Object>>(){}.getType());
				if(agentinfo!=null) {
					String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
					sAgent=agentNo;
				}
			}
			java.util.Set<String> agentList = null;
			java.util.Set<String> queueList =new java.util.HashSet<>();
			if (!(sAgent != null && sAgent.length() > 0)) {
			if (roles != null && roles.size() > 0) {
				if (roles.contains(tab.rbac.RbacClient.ROOT_ROLEGUID)) {
					// 管理员组的查询操作，无权限限制
					log.info("roles不为空超级管理员权限查询");
				} else {
					log.info("roles不为空按坐席组权限查询");
					// 坐席组
					if (sUId != null) {
						if (agentList == null) {
							agentList = new java.util.HashSet<>();
						}
						for(String role: roles) {
							agentList.addAll(tab.rbac.RbacClient.getUserAgents(sUId, role, false));
							String rolename=tab.rbac.RbacSystem.getUserQueues(role);
							if(rolename!=null) {
								queueList.add(rolename);
							}
						}
					 	java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(RbacClient.getUserAuths(sUId, sUId),new TypeToken<java.util.Map<String, Object>>(){}.getType());
						if(agentinfo!=null) {
							String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
							if(agentNo.length()>0)agentList.add(agentNo);
						}
						//where += " and (A.employee_id in(:agentList) )";
						if(queueList.size()>0) {
							where += " and (A.employee_id in(:agentList) or A.queues in(:queuelist))";
						}else {
							where += " and (A.employee_id in(:agentList) )";
						}
						
						bRoles = true;
						
					} 
					//队列
				}
			} else {
				log.info("roles为空坐席组查询,用户不属于任何组,或者session已丢失");
				// 坐席组
//				if (sUId != null && sUId.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
//					agentList = tab.rbac.RbacClient.getUserAgents(sUId, sGroupGuid, true,dbsession);
//					if (agentList == null) {
//						agentList = new java.util.HashSet<>();
//					}
//					java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(RbacClient.getUserAuths(sUId, sUId),new TypeToken<java.util.Map<String, Object>>(){}.getType());
//					if(agentinfo!=null) {
//						String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
//						if(agentNo.length()>0)agentList.add(agentNo);
//					}
//					where += " and A.employee_id in(:agentList)";
//					bRoles = true;
//				}
			}
			}
			Boolean bAgent = false;
			try {
				if (sAgent != null && sAgent.length() > 0) {
					sAgent = sAgent.trim();
					if (tab.util.Util.NONE_GUID.equals(sAgent)) {
						bAgent = false;
					} else if (sAgent.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}") == false) {
						where += " and A.employee_id=:employee_id";
						bAgent = true;
					} else {
						java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(RbacClient.getUserAuths(sAgent, sAgent),new TypeToken<java.util.Map<String, Object>>(){}.getType());
						if(agentinfo!=null) {
							where += " and A.employee_id='"+tab.util.Util.ObjectToString(agentinfo.get("agent"))+"'";
						}
					}
				}
				String countQuerystr=countSelect+where;
				log.info("通话明细countQuerystr:"+countQuerystr);
				Query countQuery = gdbsession.createSQLQuery(countQuerystr).setTimestamp("starttime", dtStarttime.getDate())
						.setTimestamp("endtime", dtEndtime.getDate());
				if (bRoles) {
					if(queueList.size()>0) {
						countQuery.setParameterList("agentList", agentList).setParameterList("queuelist", queueList);
					}else {
						countQuery.setParameterList("agentList", agentList);
					}
					//
					
				}
				
				if (bAgent==true ) {
					countQuery.setParameter("employee_id", sAgent);
				}
				if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
					java.util.List<String> queueslist = Arrays.asList(sQueques.split(","));
					log.info("输出选择队列:"+queueslist.toString());
					countQuery.setParameterList("queues", queueslist);
				}
				Integer nTotalRecords = Util
						.ObjectToNumber(countQuery.uniqueResult(), 0);
				Integer nFilteredRecords = nTotalRecords;
				if(nFilteredRecords>0) {
					String SelectStr="select A.start_date_time_string as starttime,A.end_date_time_string as endtime,A.source_address as ani,A.target_address as dnis"
							+ " ,A.queues as vdn,(case when upper(party_name)='ROUTINGPOINT' then '' else A.employee_id end) as agent "
							+ " ,A.* from t_cdr_voice A";
					String QueryStr=SelectStr+where;
					log.info("开始时间:"+dtStarttime.toString());
					log.info("结束时间:"+dtEndtime.toString());
					log.info("开始时间:"+dtStarttime);
					log.info("结束时间:"+dtEndtime);
					log.info("通话明细SelectStr:"+SelectStr);
					Query query = gdbsession.createSQLQuery(QueryStr);
					if (sScript == null || sScript.length() < 0) {
						query.setFirstResult(pageStart).setMaxResults(pageLimit);
					}
					query.setTimestamp("starttime", dtStarttime.getDate()).setTimestamp("endtime", dtEndtime.getDate());
					if (bAgent==true ) {
						query.setParameter("employee_id", sAgent);
					}
					if (bRoles) {
						if(queueList.size()>0) {
							query.setParameterList("agentList", agentList).setParameterList("queuelist", queueList);
						}else {
							query.setParameterList("agentList", agentList);
						}
						
					}
					if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
						java.util.List<String> queueslist = Arrays.asList(sQueques.split(","));
						log.info("输出选择队列:"+queueslist.toString());
						query.setParameterList("queues", queueslist);
					}
					String sProperty = StringUtils.EMPTY;
					if (Sort != null) {
						switch (Sort.getsProperty()) {
						case "starttime":
							sProperty = "A.start_date_time_string";
							break;
						case "endtime":
							sProperty = "A.end_date_time_string";
							break;
						case "agent":
							sProperty = "A.employee_id";
							break;
						case "caller":
							sProperty = "A.source_address";
							break;
						case "agent_last_name":
							sProperty = "A.AGENT_LAST_NAME";
							break;
						case "queuetime":
							sProperty = "A.mediation_duration";
							break;
										
							
						default:
							sProperty = "A.start_date_time_string";
							break;
						}
					}
					if (!sProperty.isEmpty()) {
						QueryStr += " order by " + sProperty + " " + Sort.getsDirection();
					}
					log.info(QueryStr);
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					if (sScript == null || sScript.length() == 0) {
						result.put("list", CallRecordList);
						result.put("totalCount", nFilteredRecords);
						result.put("success", true);
					} else {
						tab.MyExportExcelFile export = null;
						if (sScript == null || sScript.length() == 0) {
						} else {
							export = new tab.MyExportExcelFile();
							if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator")
									+ "WebRoot" + sScript)) {
								if(dbsession.isOpen()) {
									dbsession.close();
								}
								if(gdbsession.isOpen()) {
									gdbsession.close();
								}
								return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain")
										.build();
							}
						}
						for (int i = 0; i < CallRecordList.size(); i++) {
							java.util.Map<String, Object> call = CallRecordList.get(i);
							if (sScript == null || sScript.length() == 0) {
							} else {
								export.CommitRow(call);
							}
						}
						String sFileName = export.GetFile();
						if (sFileName != null && sFileName.length() > 0) {
							java.io.File f = new java.io.File(sFileName);
							int pos = sFileName.lastIndexOf(".");
							if(dbsession.isOpen()) {
								dbsession.close();
							}
							if(gdbsession.isOpen()) {
								gdbsession.close();
							}
							return Response.ok(f).header(HttpHeaders.CONTENT_TYPE, "application/csv;charset=UTF-8")
									.header("Content-Disposition",
											"attachment;filename=\""
													+ URLEncoder.encode("CdrVoice-", "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				}else {
					if (sScript == null || sScript.length() == 0) {
						result.put("list", null);
						result.put("totalCount", 0);
						result.put("success", true);
					}else {
						log.warn("cdr_voice导出查询为空");
					}
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			if(dbsession.isOpen()) {
				dbsession.close();
			}
			if(gdbsession.isOpen()) {
				gdbsession.close();
			}
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		if (sScript == null || sScript.length() == 0) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(404).entity("no datas").type("text/plain").build();
	}
	
	public void updaterole(Boolean roles) {
		roles=true;
	}
	
	@POST
	@Path("/UISearchAgentSummary")//坐席汇总
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UISearchAgentSummary(@Context HttpServletRequest R,
			@FormParam("starttime") /* "开始时间" */RESTDateParam dtStarttime,
			@FormParam("endtime") /* "结束时间" */RESTDateParam dtEndtime,
			@FormParam("type") Integer nType,
			@FormParam("inbound") Integer nInbound,
			
			@FormParam("agent") String agent,@FormParam("agentname") String agentname,
			@FormParam("agent_group") String agent_group,
			@FormParam("start") /* "开始页" */Integer pageStart, @FormParam("limit") /* "分页大小" */Integer pageLimit,
			@FormParam("sort") /* "排序(ExtJS默认格式, {\"property\":\"字段名\",\"direction\":\"ASC\"})" */EXTJSSortParam Sort,
			@FormParam("script") String sScript){
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("list", new Recfiles[0]);
		result.put("totalCount", 0);
		HttpSession httpsession = R.getSession();
		if (httpsession == null || dtStarttime == null || dtEndtime == null) {
			return Response.status(200).entity(result).build();
		}
		String tablename="";
		String timefield="";
		String where =" where 1=1 ";
		if(agent!=null&&agent.length()>0) {
			where+="and resource_name ilike '%" +agent+"%' ";
		}
		if(agentname!=null&&agentname.length()>0) {
			where+="and user_name ilike '%" +agentname+"%' ";
		}
		if(agent_group!=null&&agent_group.length()>0) {
			where+="and agent_group ilike '%" +agent_group+"%' ";
		}
		
		switch (nInbound) {
		case 1:// 呼入
			where += " and upper(interaction_type_code) ='INBOUND' ";
			break;
		case 2:// 呼出
			where += " and upper(interaction_type_code) ='OUTBOUND' ";
			break;
		case 3:// 内线
			where += " and upper(interaction_type_code) ='INTERNAL' ";
			break;
		default:
			break;
		}
		switch (nType) {
		default:
		case 0:// hour
			tablename="t_Philips_Agent_Hour";
			timefield="label_yyyy_mm_dd_hh24";
			DateFormat dfh=new  SimpleDateFormat("yyyy-MM-dd HH");
			String	 starttimeh= dfh.format(dtStarttime.getDate());
			String	 endtimeh= dfh.format(dtEndtime.getDate());
			where+=" and label_yyyy_mm_dd_hh24>='"+starttimeh+"' and label_yyyy_mm_dd_hh24<='"+endtimeh+"'";
			break;
		case 1:// day
			tablename="t_Philips_Agent_Day";
			timefield="label_yyyy_mm_dd";
			DateFormat df=new  SimpleDateFormat("yyyy-MM-dd");
			String	 starttime= df.format(dtStarttime.getDate());
			String	 endtime= df.format(dtEndtime.getDate());
			where+=" and label_yyyy_mm_dd>='"+starttime+"' and label_yyyy_mm_dd<='"+endtime+"'";
			break;
		case 2:// month
			tablename="t_Philips_Agent_Month";
			timefield="label_yyyy_mm";
			DateFormat dfm=new  SimpleDateFormat("yyyy-MM");
			String	 starttimem= dfm.format(dtStarttime.getDate());
			String	 endtimem= dfm.format(dtEndtime.getDate());
			where+=" and label_yyyy_mm>='"+starttimem+"' and label_yyyy_mm<='"+endtimem+"'";
			break;
		case 3:// year
			tablename="t_Philips_Agent_Year";
			timefield="label_yyyy";
			DateFormat dfy=new  SimpleDateFormat("yyyy");
			String	 starttimey= dfy.format(dtStarttime.getDate());
			String	 endtimet= dfy.format(dtEndtime.getDate());
			where+=" and label_yyyy>='"+starttimey+"' and label_yyyy<='"+endtimet+"'";
			break;
		}
		String countSelect="select count(*) from "+ tablename;
	//	String where ="  where start_ts_time>:starttime and start_ts_time<:endtime";
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			Session dbsession = HibernateSessionFactory.getThreadSession();
			String sUId=Util.ObjectToString(httpsession.getAttribute("uid"));
			tab.configServer.ValueInteger bRoles = new tab.configServer.ValueInteger(0);
			java.util.Set<String> agentList=chenUtil.Getagentlist(sUId,null,bRoles,dbsession);
			if(bRoles.value==1) {
				where+=" and resource_name in :agentlist ";
			}
			try {
				String countQuerystr=countSelect+where;
				log.info("坐席汇总countQuerystr: "+countQuerystr);
				Query countQuery = gdbsession.createSQLQuery(countQuerystr);
				if(bRoles.value==1) {
					countQuery.setParameterList("agentlist", agentList);
				}
				Integer nTotalRecords = Util
						.ObjectToNumber(countQuery.uniqueResult(), 0);
				Integer nFilteredRecords = nTotalRecords;
				if(nFilteredRecords>0) {
//					String SelectStr="select A.start_ts_time as start_ts_time,A.end_ts_time as end_ts_time,"
//							+ " A.tenant_name as tenant_name,"
//							+ "A.resource_name as resource_name,A.media_name as media_name,A.total_duration as total_duration from t_philips_agentlogin A";
					String SelectStr="select A.*,A."+timefield+" as time"
							+ " from "+tablename+" A";
					String QueryStr=SelectStr+where;
					Query query = gdbsession.createSQLQuery(QueryStr);
					if(bRoles.value==1) {
						query.setParameterList("agentlist", agentList);
					}
					if (sScript == null || sScript.length() < 0) {
						query.setFirstResult(pageStart).setMaxResults(pageLimit);
					}
					String sProperty = StringUtils.EMPTY;
					if (Sort != null) {
						switch (Sort.getsProperty()) {
						case "time":
							sProperty = "A."+timefield;
							break;
						case "resource_name":
							sProperty = "A.resource_name";
							break;
						default:
							sProperty = "A."+timefield;
							break;	
							
						}
					}
					if (!sProperty.isEmpty()) {
						QueryStr += " order by " + sProperty + " " + Sort.getsDirection();
					}
					log.info("坐席汇总QueryStr:"+QueryStr);
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					
					if (sScript == null || sScript.length() == 0) {
						result.put("list", CallRecordList);
						result.put("totalCount", nFilteredRecords);
						result.put("success", true);
					} else {
						tab.MyExportExcelFile export = null;
						if (sScript == null || sScript.length() == 0) {
						} else {
							export = new tab.MyExportExcelFile();
							if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator")
									+ "WebRoot" + sScript)) {
								if(dbsession.isOpen()) {
									dbsession.close();
								}
								if(gdbsession.isOpen()) {
									gdbsession.close();
								}
								return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain")
										.build();
							}
						}
						for (int i = 0; i < CallRecordList.size(); i++) {
							java.util.Map<String, Object> call = CallRecordList.get(i);
							if (sScript == null || sScript.length() == 0) {
							} else {
								export.CommitRow(call);
							}
						}
						String sFileName = export.GetFile();
						if (sFileName != null && sFileName.length() > 0) {
							java.io.File f = new java.io.File(sFileName);
							int pos = sFileName.lastIndexOf(".");
							if(dbsession.isOpen()) {
								dbsession.close();
							}
							if(gdbsession.isOpen()) {
								gdbsession.close();
							}
							return Response.ok(f).header(HttpHeaders.CONTENT_TYPE, "application/csv;charset=UTF-8")
									.header("Content-Disposition",
											"attachment;filename=\""
													+ URLEncoder.encode("AgentSummary-", "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				}else {
					if (sScript == null || sScript.length() == 0) {
						result.put("list", null);
						result.put("totalCount", 0);
						result.put("success", true);
					}
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			if(dbsession.isOpen()) {
				dbsession.close();
			}
			if(gdbsession.isOpen()) {
				gdbsession.close();
			}
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		if (sScript == null || sScript.length() == 0) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(404).entity("no datas").type("text/plain").build();
	}
	
	@POST
	@Path("/UISearchQueuesSummary")//队列汇总
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UISearchQueuesSummary(@Context HttpServletRequest R,
			@FormParam("starttime") /* "开始时间" */RESTDateParam dtStarttime,
			@FormParam("endtime") /* "结束时间" */RESTDateParam dtEndtime,
			@FormParam("type") Integer nType,
			@FormParam("inbound") Integer nInbound,
			@FormParam("queuename") String queuename,
			@FormParam("start") /* "开始页" */Integer pageStart, @FormParam("limit") /* "分页大小" */Integer pageLimit,
			@FormParam("sort") /* "排序(ExtJS默认格式, {\"property\":\"字段名\",\"direction\":\"ASC\"})" */EXTJSSortParam Sort,
			@FormParam("script") String sScript){
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("list", new Recfiles[0]);
		result.put("totalCount", 0);
		HttpSession httpsession = R.getSession();
		if (httpsession == null || dtStarttime == null || dtEndtime == null) {
			return Response.status(200).entity(result).build();
		}
		String tablename="";
		String timefield="";
		String where=" where 1=1 ";
		switch (nInbound) {
		case 1:// 呼入
			where += " and upper(interaction_type_code) ='INBOUND' ";
			break;
		case 2:// 内线
			where += " and upper(interaction_type_code) ='INTERNAL' ";
			break;
		default:
			break;
		}
		switch (nType) {
		default:
		case 0:// hour
			tablename="t_Philips_queue_Hour";
			timefield="label_yyyy_mm_dd_hh24";
			DateFormat dfh=new  SimpleDateFormat("yyyy-MM-dd HH");
			String	 starttimeh= dfh.format(dtStarttime.getDate());
			String	 endtimeh= dfh.format(dtEndtime.getDate());
			where+=" and label_yyyy_mm_dd_hh24>='"+starttimeh+"' and label_yyyy_mm_dd_hh24<='"+endtimeh+"'";
			break;
		case 1:// day
			tablename="t_Philips_queue_Day";
			timefield="label_yyyy_mm_dd";
			DateFormat df=new  SimpleDateFormat("yyyy-MM-dd");
			String	 starttime= df.format(dtStarttime.getDate());
			String	 endtime= df.format(dtEndtime.getDate());
			where+=" and label_yyyy_mm_dd>='"+starttime+"' and label_yyyy_mm_dd<='"+endtime+"'";
			break;
		case 2:// month
			tablename="t_Philips_queue_Month";
			timefield="label_yyyy_mm";
			DateFormat dfm=new  SimpleDateFormat("yyyy-MM");
			String	 starttimem= dfm.format(dtStarttime.getDate());
			String	 endtimem= dfm.format(dtEndtime.getDate());
			where+=" and label_yyyy_mm>='"+starttimem+"' and label_yyyy_mm<='"+endtimem+"'";
			break;
		case 3:// year
			tablename="t_Philips_queue_Year";
			timefield="label_yyyy";
			DateFormat dfy=new  SimpleDateFormat("yyyy");
			String	 starttimey= dfy.format(dtStarttime.getDate());
			String	 endtimet= dfy.format(dtEndtime.getDate());
			where+=" and label_yyyy>='"+starttimey+"' and label_yyyy<='"+endtimet+"'";
			break;
		}
		String countSelect="select count(*) from "+ tablename;
		//String where ="  where "+timefield+">=:starttime and "+timefield+"<=:endtime";
		if(queuename!=null&&queuename.length()>0) {
			where+=" and resource_name ilike '%" +queuename+"%' ";
		}
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				String countQuerystr=countSelect+where;
				log.info("队列汇总countQuerystr: "+countQuerystr);
				Query countQuery = gdbsession.createSQLQuery(countQuerystr);
				Integer nTotalRecords = Util
						.ObjectToNumber(countQuery.uniqueResult(), 0);
				Integer nFilteredRecords = nTotalRecords;
				if(nFilteredRecords>0) {
//					String SelectStr="select A.start_ts_time as start_ts_time,A.end_ts_time as end_ts_time,"
//							+ " A.tenant_name as tenant_name,"
//							+ "A.resource_name as resource_name,A.media_name as media_name,A.total_duration as total_duration from t_philips_agentlogin A";
					String SelectStr="select A.*,A."+timefield+" as time"
							+ " from "+tablename+" A";
					String QueryStr=SelectStr+where;
					Query query = gdbsession.createSQLQuery(QueryStr);
					if (sScript == null || sScript.length() < 0) {
						query.setFirstResult(pageStart).setMaxResults(pageLimit);
					}
					String sProperty = StringUtils.EMPTY;
					if (Sort != null) {
						switch (Sort.getsProperty()) {
						case "time":
							sProperty = "A."+timefield;
							break;
						case "resource_name":
							sProperty = "A.resource_name";
							break;
						default:
							sProperty = "A."+timefield;
							break;	
						}
					}
					if (!sProperty.isEmpty()) {
						QueryStr += " order by " + sProperty + " " + Sort.getsDirection();
					}
					log.info("队列汇总QueryStr:"+QueryStr);
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					
					if (sScript == null || sScript.length() == 0) {
						result.put("list", CallRecordList);
						result.put("totalCount", nFilteredRecords);
						result.put("success", true);
					} else {
						tab.MyExportExcelFile export = null;

						if (sScript == null || sScript.length() == 0) {
						} else {
							export = new tab.MyExportExcelFile();
							if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator")
									+ "WebRoot" + sScript)) {
								gdbsession.close();
								return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain")
										.build();
							}
						}
						for (int i = 0; i < CallRecordList.size(); i++) {
							java.util.Map<String, Object> call = CallRecordList.get(i);
							if (sScript == null || sScript.length() == 0) {
							} else {
								export.CommitRow(call);
							}
						}
						String sFileName = export.GetFile();
						if (sFileName != null && sFileName.length() > 0) {
							java.io.File f = new java.io.File(sFileName);
							int pos = sFileName.lastIndexOf(".");
							gdbsession.close();
							return Response.ok(f).header(HttpHeaders.CONTENT_TYPE, "application/csv;charset=UTF-8")
									.header("Content-Disposition",
											"attachment;filename=\""
													+ URLEncoder.encode("QueueSummary-", "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				}else {
					if (sScript == null || sScript.length() == 0) {
						result.put("list", null);
						result.put("totalCount", 0);
						result.put("success", true);
					}
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			gdbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		if (sScript == null || sScript.length() == 0) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(404).entity("no datas").type("text/plain").build();
	}
	
	@POST
	@Path("/UISearchMissCalls")//未接听来电
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UISearchMissCalls(@Context HttpServletRequest R,
			@FormParam("start") /* "开始页" */Integer pageStart, @FormParam("limit") /* "分页大小" */Integer pageLimit,
			@FormParam("sort") /* "排序(ExtJS默认格式, {\"property\":\"字段名\",\"direction\":\"ASC\"})" */EXTJSSortParam Sort,
			@FormParam("script") String sScript){
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("list", new Recfiles[0]);
		result.put("totalCount", 0);
		HttpSession httpsession = R.getSession();
		if (httpsession == null ) {
			return Response.status(200).entity(result).build();
		}
		String tablename="";
		String timefield="";
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				Calendar now = Calendar.getInstance();
				now.set(Calendar.HOUR_OF_DAY, 0);
				now.set(Calendar.MINUTE, 0);
				now.set(Calendar.SECOND, 0);
				now.set(Calendar.MILLISECOND, 0);
				Date time=now.getTime();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		        String todaystr= sdf.format(time);

				String uid=(String) httpsession.getAttribute("uid");
				String where=" where start_time>=:starttime ";
				java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(RbacClient.getUserAuths(uid, uid),new TypeToken<java.util.Map<String, Object>>(){}.getType());
				if(agentinfo!=null) {
					String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
					 where +=" and agent_id='"+agentNo+"'";
				}else {
					result.put("msg", "agentNo为空");
					result.put("list", null);
					result.put("totalCount", 0);
					result.put("success", false);
					return Response.status(200).entity(result).build();
				}
				String countQuerystr="select count(*) from Philips_Misscalls "+where;
				log.info("Misscalls,countQuerystr: "+countQuerystr);
				Query countQuery = gdbsession.createSQLQuery(countQuerystr).setParameter("starttime", time);
				Integer nTotalRecords = Util
						.ObjectToNumber(countQuery.uniqueResult(), 0);
				Integer nFilteredRecords = nTotalRecords;
				if(nFilteredRecords>0) {
					String SelectStr="select A.* from Philips_Misscalls A "+where;
					String QueryStr=SelectStr;
					Query query = gdbsession.createSQLQuery(QueryStr).setParameter("starttime", time);
					if (sScript == null || sScript.length() < 0) {
						query.setFirstResult(pageStart).setMaxResults(pageLimit);
					}
					log.info("Misscalls,QueryStr:"+QueryStr);
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					
					if (sScript == null || sScript.length() == 0) {
						result.put("list", CallRecordList);
						result.put("totalCount", nFilteredRecords);
						result.put("success", true);
					} else {
						tab.MyExportExcelFile export = null;

						if (sScript == null || sScript.length() == 0) {
						} else {
							export = new tab.MyExportExcelFile();
							if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator")
									+ "WebRoot" + sScript)) {
								gdbsession.close();
								return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain")
										.build();
							}
						}
						for (int i = 0; i < CallRecordList.size(); i++) {
							java.util.Map<String, Object> call = CallRecordList.get(i);
							if (sScript == null || sScript.length() == 0) {
							} else {
								export.CommitRow(call);
							}
						}
						String sFileName = export.GetFile();
						if (sFileName != null && sFileName.length() > 0) {
							java.io.File f = new java.io.File(sFileName);
							int pos = sFileName.lastIndexOf(".");
							gdbsession.close();
							return Response.ok(f).header(HttpHeaders.CONTENT_TYPE, "application/csv;charset=UTF-8")
									.header("Content-Disposition",
											"attachment;filename=\""
													+ URLEncoder.encode("QueueSummary-", "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				}else {
					if (sScript == null || sScript.length() == 0) {
						result.put("list", null);
						result.put("totalCount", 0);
						result.put("success", true);
					}
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			gdbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		if (sScript == null || sScript.length() == 0) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(404).entity("no datas").type("text/plain").build();
	}
	@POST
	@Path("/UISearchRecords")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UISearchRecords(@Context HttpServletRequest R,
			@FormParam("starttime") /* "开始时间" */RESTDateParam dtStarttime,
			@FormParam("endtime") /* "结束时间" */RESTDateParam dtEndtime,
			@FormParam("host") /* "主机编号,大于等于0有效,否则忽略" */Integer nHost,
			@FormParam("caller") /* "通过主叫号码模糊查询" */String caller, @FormParam("called") /* "通过被叫号码模糊查询" */String called,
			@FormParam("extension") /* "通过分机号码查询" */String ext, @FormParam("agent") /* "通过工号查询" */String agent,
			@FormParam("backup") /* "是否备份过" */int nBackup, @FormParam("lock") /* "是否加锁条件, 0全部，1仅加锁，2仅未锁" */int nLock,
			@FormParam("delete") /* "是否删除条件, 0全部，1仅删除，2仅未删除" */int nDelete,
			@FormParam("answer") /* "是否应答条件, 0全部，1仅应答，2仅未应答" */int nAnswer,
			@FormParam("direction") /* "呼叫方向条件, 0全部，1仅呼出，2仅呼入" */int nDirection,
			@FormParam("inside") /* "内外线条件, 0全部，1仅内线，2仅外线" */int nInside,
			@FormParam("minlength") /* "录音最小长度" */int nLength, @FormParam("maxlength") /* "录音最大长度, 0忽略" */long nMaxLength,
			@FormParam("ucid") /* "通过呼叫唯一ID查询录音,忽略时间范围" */String sUcid,
			@FormParam("guidid") /* "通过录音唯一ID查询录音,忽略时间范围" */String sGuidId,
			@FormParam("groupguidid") /* "通过录音所属组查询,该组可以是坐席工号组,也可以是分机组" */String sGroupGuid,
			@FormParam("mark") /* 备注 */String sMark,
			@FormParam("queues") String sQueques,
			@FormParam("morequeues") String sMorequeues,
			
			@FormParam("start") /* "开始页" */Integer pageStart, @FormParam("limit") /* "分页大小" */Integer pageLimit,
			@FormParam("sort") /* "排序(ExtJS默认格式, {\"property\":\"字段名\",\"direction\":\"ASC\"})" */EXTJSSortParam Sort) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("recordItems", new Recfiles[0]);
		result.put("totalCount", 0);
		HttpSession httpsession = R.getSession();
		if (httpsession == null || dtStarttime == null || dtEndtime == null) {
			return Response.status(200).entity(result).build();
		}
		tab.configServer.ValueInteger nTotalCount = new tab.configServer.ValueInteger(0);
			java.util.List<RecfilesEx> recfiles = SearchRecords(Util.ObjectToString(httpsession.getAttribute("uid")),
					null, dtStarttime, dtEndtime, nHost, caller, called, ext, agent, nBackup, nLock, nDelete, nAnswer,
					nDirection, nInside, nLength, nMaxLength, sUcid, sGuidId, sGroupGuid, pageStart, pageLimit, Sort,
					nTotalCount,0,sMark,sMorequeues);
			result.put("recordItems", recfiles);
			result.put("totalCount", nTotalCount.value);
		return Response.status(200).entity(result).build();
	}
	
	@SuppressWarnings("unchecked")
	private static java.util.List<RecfilesEx> SearchRecords(String sUId, String sRoleId, RESTDateParam dtStarttime,
			RESTDateParam dtEndtime, Integer nHost, String caller, String called, String ext, String agent, int nBackup,
			int nLock, int nDelete, int nAnswer, int nDirection, int nInside, int nLength, long nMaxLength,
			String sUcid, String sGuidId, String sGroupGuid, Integer pageStart, Integer pageLimit, EXTJSSortParam sort,
			tab.configServer.ValueInteger nTotalCount,int AuditLogDownExport,String sMark,String sQueques) {
		java.util.List<RecfilesEx> recfiles = null;
		boolean bId = false, bUcid = false, bCaller = false, bCalled = false, bExt = false, bAgent = false;
		String sWhere = new String(" 1=1");
		if (sGuidId != null && sGuidId.length() > 0) {
			sWhere += " and A.media_server_ixn_guid=:id";
			bId = true;
		} else if (sUcid != null && sUcid.length() > 0) {
			sWhere += " and A.media_server_ixn_guid=:ucid";
			bUcid = true;
		} else {
			if (nHost != null && nHost >= 0) {
				sWhere += " and A.host=" + nHost;
			}
			if (sQueques != null && sQueques.length() > 0 && !(sQueques.equals("All"))) {
				sWhere = sWhere + " and A.queues in :queues ";
			}
			if (nLength > 0) {
				sWhere += " and A.talk_duration>=" + nLength;
			}
			if (nMaxLength > 0) {
				sWhere = sWhere + " and A.talk_duration<=" + nMaxLength;
			}
			// 10 = 2 呼入不应答
			// 11 = 3 呼出不应答
			// 01 = 1 呼出应答
			// 00 = 0 呼入应答
			if (nDirection == 2) {// 呼入0,2
				if (nAnswer == 1) {// 应答 0
					sWhere = sWhere + " and(upper(A." + sInteraction_type_name + ")='INBOUND' and  and A.talk_duration>0)";
				} else if (nAnswer == 2) {// 不应答 2
					sWhere = sWhere + " and (upper(A." + sInteraction_type_name + ")='INBOUND' and A.talk_duration=0)";
				} else {
					sWhere = sWhere + " and (upper(A." + sInteraction_type_name + ")='INBOUND')";
				}
			} else if (nDirection == 1) {//呼出1,3
				if (nAnswer == 1) {//应答 1
					sWhere = sWhere + " and (upper(A." + sInteraction_type_name + ")='OUTBOUND' and  and A.talk_duration>0)";
				} else if (nAnswer == 2) {//不应答 3
					sWhere = sWhere + " and (upper(A." + sInteraction_type_name + ")='OUTBOUND' and  and A.talk_duration=0)";
				} else {
					sWhere = sWhere + " and (upper(A." + sInteraction_type_name + ")='OUTBOUND' and (A.talk_duration>0))";
				}
			} else {// 呼入呼出 0,1,2,3
				if (nAnswer == 1) {//应答 0,1
					sWhere = sWhere + " and (A.talk_duration>0)";
				} else if (nAnswer == 2) {// 不应答 2,3
					sWhere = sWhere + " and (A.talk_duration=0)";
				}else {
					sWhere = sWhere + " and (A.talk_duration>0)";
				}
				// nAnswer=0,nDir=0
			}
			if (caller != null)
				caller = caller.trim();
			if (called != null)
				called = called.trim();
			if (caller != null && caller.length() > 0 && caller.equalsIgnoreCase(called)) {
				sWhere = sWhere + " and (A.source_address like :caller or A.target_address like :called)";
				bCaller = true;
				bCalled = true;
			} else {
				if (caller != null && caller.length() > 0) {
					sWhere = sWhere + " and A.source_address like :caller";
					bCaller = true;
				}
				if (called != null && called.length() > 0) {
					sWhere = sWhere + " and A.target_address like :called";
					bCalled = true;
				}
			}
//			if (ext != null && ext.length() > 0) {
//				ext = ext.trim();
//				sWhere = sWhere + " and A.resource_name=:ext";
//				bExt = true;
//			}
			if (agent != null && agent.length() > 0) {
				agent = agent.trim();
				if (tab.util.Util.NONE_GUID.equals(agent)) {
					bAgent = false;
				} else if (agent.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}") == false) {
					sWhere += " and A.employee_id=:agent";
					bAgent = true;
				} else {
					java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(RbacClient.getUserAuths(agent, agent),new TypeToken<java.util.Map<String, Object>>(){}.getType());
					if(agentinfo!=null) {
						sWhere += " and A.employee_id='"+tab.util.Util.ObjectToString(agentinfo.get("agent"))+"'";
					}
				}
			}
			if ((nLock & 2) == 2) {
				if (nBackup == 2) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states=0";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states=4";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (0,4)";
					} else
						return recfiles;
				} else if (nBackup == 1) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states=2";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states=6";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (2,6)";
					} else
						return recfiles;
				} else if (nBackup == 0) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states in (0,2)";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states in (4,6)";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (0,2,4,6)";
					} else
						return recfiles;
				} else
					return recfiles;
			} else if ((nLock & 1) == 1) {
				if (nBackup == 2) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states=1";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states=5";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (1,5)";
					} else
						return recfiles;
				} else if (nBackup == 1) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states=3";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states=7";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (3,7)";
					} else
						return recfiles;
				} else if (nBackup == 0) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states in (1,3)";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states in (5,7)";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (1,3,5,7)";
					} else
						return recfiles;
				} else
					return recfiles;
			} else if ((nLock & 1) == 0) {
				if (nBackup == 2) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states in (0,1)";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states in (4,5)";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (0,1,4,5)";
					} else
						return recfiles;
				} else if (nBackup == 1) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states in (2,3)";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states in (6,7)";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (2,3,6,7)";
					} else
						return recfiles;
				} else if (nBackup == 0) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states in (0,1,2,3)";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states in (4,5,6,7)";
					} else if (nDelete == 0) {
						// 去掉states条件
					} else
						return recfiles;
				} else
					return recfiles;
			} else
				return recfiles;
			sWhere = " A.start_date_time_string>=:start and A.start_date_time_string<=:end and" + sWhere;
		}
		if (tab.util.Util.NONE_GUID.equals(sGroupGuid)) {
			sGroupGuid = "";
		}
		java.util.Map<String, String> usernames = new java.util.HashMap<String, String>();
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			Session dbsession = HibernateSessionFactory.getThreadSession();
			boolean bRoles = false, bAdmin = false;
			java.util.Set<String> roles = null;
			if (sUId != null && sUId.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
				log.info("通过uid权限查询录音");
//				roles = tab.rbac.RbacClient.getRoleGuidsForRole(sUId, sGroupGuid, dbsession,
//						sGroupGuid.length() == 0 ? true : false);
				roles = tab.rbac.RbacClient.getRoleGuidsForRole(sUId, sGroupGuid, dbsession,
						false);
				
			} else {
				if (sRoleId != null
						&& sRoleId.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
					roles = tab.rbac.RbacClient.getRoleGuidsForRole(sRoleId, dbsession, false);
				}
			}
			java.util.Set<String> agentList = null;
			if (roles != null && roles.size() > 0) {
				if (roles.contains(tab.rbac.RbacClient.ROOT_ROLEGUID)) {
					// 管理员组的查询操作，无权限限制
					bAdmin = true;
					log.info("roles不为空超级管理员权限查询");
				} else {
					log.info("roles不为空按坐席组查询");
					// 坐席组
					if (sUId != null && sUId.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
						if (agentList == null) {
							agentList = new java.util.HashSet<>();
						}
						for(String role: roles) {
							agentList.addAll(tab.rbac.RbacClient.getUserAgents(sUId, role, false));
						}
						java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(RbacClient.getUserAuths(sUId, sUId),new TypeToken<java.util.Map<String, Object>>(){}.getType());
						if(agentinfo!=null) {
							String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
							if(agentNo.length()>0)agentList.add(agentNo);
						}
						sWhere += " and A.employee_id in(:roles)";
						bRoles = true;
					} else {
						if (agentList == null) {
							agentList = new java.util.HashSet<>();
						}
						for(String role: roles) {
							agentList.addAll(tab.rbac.RbacClient.getUserAgents(sUId, role, false,dbsession));
						}
						sWhere += " and A.employee_id in(:roles)";
						bRoles = true;
					}
				}
			} else {
				log.info("roles为空坐席组查询");
				// 坐席组
				if (sUId != null && sUId.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
					agentList = tab.rbac.RbacClient.getUserAgents(sUId, sGroupGuid, true,dbsession);
					if (agentList == null) {
						agentList = new java.util.HashSet<>();
					}
					java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(RbacClient.getUserAuths(sUId, sUId),new TypeToken<java.util.Map<String, Object>>(){}.getType());
					if(agentinfo!=null) {
						String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
						if(agentNo.length()>0)agentList.add(agentNo);
					}
					sWhere += " and A.employee_id in(:roles)";
					bRoles = true;
				}
			}
			if (!(bId || bUcid || bAdmin || bRoles)) {
				gdbsession.close();
				dbsession.close();
				return recfiles;
			}
			String sSelect = "select A.media_server_ixn_guid as id, A.start_date_time_string as createdate, A.source_address as caller,A.employee_id as agent,A.AGENT_LAST_NAME as username,A.queues as queues "
					+ ",A.target_address as called,A.talk_duration as seconds,(case when upper(A." + sInteraction_type_name + ")='INBOUND' or upper(A." + sInteraction_type_name 	+ ")='INTERNAL'  then 0 else 1 end) as direction";
			String sSqlStr = StringUtils.EMPTY;
			try {
				if (pageStart != null && pageLimit != null && pageLimit > 0 && pageStart >= 0) {
					sSqlStr = "select count(*) from t_cdr_voice A where" + sWhere;
					log.info("start: " + dtStarttime.toString() + ", end:" + dtEndtime.toString());
					if(bRoles) {
						log.info(GsonUtil.getInstance().toJson(agentList));
					}
					log.info(sSqlStr);
					SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
					//Query query = dbsession.createQuery("select count(*) from Recfiles A where" + sWhere);
					if (bId) {
						query.setString("id", sGuidId);
						if (bRoles)
							query.setParameterList("roles", agentList);
					} else if (bUcid) {
						query.setString("ucid", sUcid);
						if (bRoles)
							query.setParameterList("roles", agentList);
					} else if (bAdmin || bRoles) {
						if (bCaller)
							query.setString("caller", "%" + caller + "%");
						if (bCalled)
							query.setString("called", "%" + called + "%");
						if (bExt)
							query.setString("ext", ext);
						if (bAgent)
							query.setString("agent", agent);
						if (bRoles)
							query.setParameterList("roles", agentList);
						query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
					}
					if(sMark!=null && sMark.length()>0) {
						query.setString("mark", "%" + sMark + "%");
					}
					if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
						java.util.List<String> queueslist = Arrays.asList(sQueques.split(","));
						log.info("输出选择队列:"+queueslist.toString());
						query.setParameterList("queues", queueslist);
					}
				
					nTotalCount.value = Util.ObjectToNumber(query.uniqueResult(), 0);
					if (nTotalCount.value > 0) {
						if (sort == null) {
							sSqlStr = sSelect + " from t_cdr_voice A where" + sWhere + " order by A.start_date_time_string desc";
							log.info(sSqlStr);
							query = gdbsession.createSQLQuery(sSqlStr);
							//query = gdbsession
							//		.createQuery(" from RecfilesEx A where" + sWhere + " order by A.createdate desc");
						} else {
							String sProperty = StringUtils.EMPTY;
							switch(sort.getsProperty()) {
							case "id":
								sProperty = "A.media_server_ixn_guid";
								break;
							default:
							case "bLock":
							case "sSystemTim":
							case "createdate":
								sProperty = "A.start_date_time_string";
								break;
//							case "sExtension":
//							case "extension":
//								sProperty = "A.resource_name";
//								break;
							case "sCaller":
							case "caller":
								sProperty = "A.source_address";
								break;
							case "sCalled":
							case "called":
								sProperty = "A.target_address";
								break;
							case "sGroupName":
							case "email":
							case "sUserName":
							case "agent":
								sProperty = "A.employee_id";
								break;
							case "nSeconds":
							case "seconds":
								sProperty = "A.talk_duration";
								break;
							case "nDirection":
							case "direction":
								sProperty = "A."+sInteraction_type_name;
								break;
							}
							sSqlStr = sSelect + " from t_cdr_voice A where" + sWhere + " order by "	+ sProperty + " " + sort.getsDirection();
							log.info(sSqlStr);
							query = gdbsession.createSQLQuery(sSqlStr);
							//query = gdbsession.createQuery(" from RecfilesEx A where" + sWhere + " order by A."
							//		+ sort.getsProperty() + " " + sort.getsDirection());
						}
						if (bId) {
							query.setString("id", sGuidId);
							if (bRoles)
								query.setParameterList("roles", agentList);
						} else if (bUcid) {
							query.setString("ucid", sUcid);
							if (bRoles)
								query.setParameterList("roles", agentList);
						} else if (bAdmin || bRoles) {
							if (bCaller)
								query.setString("caller", "%" + caller + "%");
							if (bCalled)
								query.setString("called", "%" + called + "%");
							if (bExt)
								query.setString("ext", ext);
							if (bAgent)
								query.setString("agent", agent);
							if (bRoles)
								query.setParameterList("roles", agentList);
							query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
						}
						if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
							java.util.List<String> queueslist = Arrays.asList(sQueques.split(","));
							query.setParameterList("queues", queueslist);
						}
						recfiles = MapToRecfiles(query.setFirstResult(pageStart).setMaxResults(pageLimit).setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP).list());
					}
				} else {
					sSqlStr = sSelect + " from t_cdr_voice A where" + sWhere + " order by A.start_date_time_string desc";
					log.info(sSqlStr);
					SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
					if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
						java.util.List<String> queueslist = Arrays.asList(sQueques.split(","));
						query.setParameterList("queues", queueslist);
					}
					if (bId) {
						query.setString("id", sGuidId);
						if (bRoles)
							query.setParameterList("roles", agentList);
					} else if (bUcid) {
						query.setString("ucid", sUcid);
						if (bRoles)
							query.setParameterList("roles", agentList);
					} else {
						if (bCaller)
							query.setString("caller", "%" + caller + "%");
						if (bCalled)
							query.setString("called", "%" + called + "%");
						if (bExt)
							query.setString("ext", ext);
						if (bAgent)
							query.setString("agent", agent);
						if (bRoles)
							query.setParameterList("roles", agentList);
						query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
					}
					if(sMark!=null && sMark.length()>0) {
						query.setString("mark", "%" + sMark + "%");
					}
					recfiles = MapToRecfiles(query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP).list());
					if(nTotalCount!=null)nTotalCount.value = recfiles.size();
				}


			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
			} catch (Throwable e) {
				log.warn("ERROR:", e);
			}
			if(dbsession.isOpen()) {
				dbsession.close();
			}
			if(gdbsession.isOpen()) {
				gdbsession.close();
			}
			
			
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:", e);
		} catch (Throwable e) {
			log.warn("ERROR: ", e);
		}
		if (recfiles != null) {
			for (int idx = 0; idx < recfiles.size(); idx++) {
				RecfilesEx rec = recfiles.get(idx);
				if ((rec.getStates() & 0x2) == 0x2)
					rec.backup = true;
				else
					rec.backup = false;
				if ((rec.getStates() & 0x4) == 0x4)
					rec.delete = true;
				else
					rec.delete = false;
				if ((rec.getStates() & 0x1) == 0x1)
					rec.lock = true;
				else
					rec.lock = false;
				//rec.username = usernames.get(rec.getAgent());
			}
		}
		if(AuditLogDownExport==1) {
			if(recfiles.size()>1) {
				tab.rbac.RbacSystem.AuditLog("download", "Recfiles", tab.util.Util.NONE_GUID, 1,String.format("{\"recordCount\":%d}",recfiles.size()), null,null,sUId);
			}else {
				tab.rbac.RbacSystem.AuditLog("download", "Recfiles", recfiles.size()>0 ? recfiles.get(0).getId() : tab.util.Util.NONE_GUID,  recfiles.size(),String.format("{\"recordCount\":%d}",recfiles.size()),null,null,sUId);
			}
		}else if(AuditLogDownExport==2){
			tab.rbac.RbacSystem.AuditLog("export", "Recfiles", tab.util.Util.NONE_GUID,  recfiles.size()>0?1:0,String.format("{\"recordCount\":%d}",recfiles.size()),null,null,sUId);
		}
		return recfiles;
	}
	private static java.util.List<RecfilesEx> MapToRecfiles(java.util.List<java.util.Map<String, Object>> items) {
		java.util.List<RecfilesEx> recfiles = new java.util.ArrayList<RecfilesEx>();
		for(int i=0;i<items.size();i++) {
			java.util.Map<String, Object> item = items.get(i);
			RecfilesEx  recfileex = new RecfilesEx();
			recfileex.setAgent(tab.util.Util.ObjectToString(item.get("agent")));
			recfileex.setCalled(tab.util.Util.ObjectToString(item.get("called")));
			recfileex.setCaller(tab.util.Util.ObjectToString(item.get("caller")));
			recfileex.setChannel(tab.util.Util.ObjectToNumber(item.get("channel"),0));
			recfileex.setCreatedate( (Date) item.get("createdate") );
			recfileex.setDirection(tab.util.Util.ObjectToNumber(item.get("direction"),0));
			recfileex.setExtension(tab.util.Util.ObjectToString(item.get("extension")));
			recfileex.setFilename(tab.util.Util.ObjectToString(item.get("id")));
			recfileex.setQueues(tab.util.Util.ObjectToString(item.get("queues")));
			recfileex.setUsername(tab.util.Util.ObjectToString(item.get("username")));
			recfileex.setHost(0);
			recfileex.setId(tab.util.Util.ObjectToString(item.get("id")));
			recfileex.setRoleid(tab.util.Util.ROOT_ROLEGUID);
			recfileex.setSeconds(tab.util.Util.ObjectToNumber(item.get("seconds"),0));
			recfileex.setStates(0);
			recfileex.setUcid(recfileex.getId());
			recfileex.setUserdata("");
			recfiles.add(recfileex);
		}
		return items.size()>0?recfiles:null;
	}
	
	@POST
	@Path("/UISearchRecordsDownload")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/zip" + ";charset=utf-8")
	public Response UISearchRecordsDownload(@Context HttpServletRequest R,
			@FormParam("starttime") /* "开始时间" */RESTDateParam dtStarttime,
			@FormParam("endtime") /* "结束时间" */RESTDateParam dtEndtime,
			@FormParam("host") /* "主机编号,大于等于0有效,否则忽略" */Integer nHost,
			@FormParam("caller") /* "通过主叫号码模糊查询" */String caller, @FormParam("called") /* "通过被叫号码模糊查询" */String called,
			@FormParam("extension") /* "通过分机号码查询" */String ext, @FormParam("agent") /* "通过工号查询" */String agent,
			@FormParam("backup") int nBackup, @FormParam("lock") /* "是否加锁条件, 0全部，1仅加锁，2仅未锁" */int nLock,
			@FormParam("delete") /* "是否删除条件, 0全部，1仅删除，2仅未删除" */int nDelete,
			@FormParam("answer") /* "是否应答条件, 0全部，1仅应答，2仅未应答" */int nAnswer,
			@FormParam("direction") /* "呼叫方向条件, 0全部，1仅呼出，2仅呼入" */int nDirection,
			@FormParam("inside") /* "内外线条件, 0全部，1仅内线，2仅外线" */int nInside,
			@FormParam("length") /* "录音最小长度" */int nLength, @FormParam("maxlength") /* "录音最大长度, 0忽略" */long nMaxLength,
			@FormParam("ucid") /* "通过呼叫唯一ID查询录音,忽略时间范围" */String sUcid,
			@FormParam("queues") String sQueques,
			@FormParam("guidid") /* "通过录音唯一ID查询录音,忽略时间范围" */String sGuidId,
			@FormParam("groupguidid") /* "通过录音所属组查询,该组可以是坐席工号组,也可以是分机组" */String sGroupGuid,
			@FormParam("mark") /* 备注 */String sMark) {
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			return Response.status(404).entity("SESSION NOT FOUND").type("text/plain").build();
		}
		String sPath = System.getProperty("tab.logpath") + "DownloadFiles";
		java.io.File Path = new java.io.File(sPath);
		if (!Path.exists()) {
			Path.mkdirs();
		}
		sPath += File.separator;
		int nSize = 0;
		tab.configServer.ValueInteger vi = new tab.configServer.ValueInteger(200);
			java.util.List<RecfilesEx> recfiles = SearchRecords(Util.ObjectToString(httpsession.getAttribute("uid")),
					null, dtStarttime, dtEndtime, nHost, caller, called, ext, agent, nBackup, nLock, nDelete, nAnswer,
					nDirection, nInside, nLength, nMaxLength, sUcid, sGuidId, sGroupGuid, null, null, null, null,1,sMark,sQueques);
			nSize = recfiles.size();
			log.info("输出一下下载全部录音记录条数:"+nSize);
			try {
				tab.configServer.getInstance().getValue(Runner.ConfigName_, "MaxDownloadFiles", vi, "最大下载文件数量", "",
						true, true);
			} catch (ClassNotFoundException err) {
				vi.value = 50;
			}
			if (vi.value < 50)
				vi.value = 50;
			if (nSize < vi.value) {
				String sZipFileName = sPath + UUID.randomUUID().toString() + ".zip";
				ZipOutputStream zos = null;
				try {
					zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File(sZipFileName))));
				} catch (FileNotFoundException e) {
					log.error("ERR:", e);
					return Response.status(404).entity(e.toString()).type("text/plain").build();
				}
				CloseableHttpClient httpclient = tab.util.Util
						.getIdleHttpClient(Runner.sPlayUrl.indexOf("https://") == 0 ? true : false);
				for (int i = 0; i < nSize; ++i) {
					RecfilesEx Item = recfiles.get(i);
					int npos = Item.getFilename().lastIndexOf(".");
					File f = new File(sPath + Item.getId() + (Runner.nPlayMode == 0 ? ".MP3" : (npos>0 ? Item.getFilename().substring(npos) : "")));
					if (f.exists()) {
						f.delete();
					}
					try {
						tab.configServer.ValueString AudioFormat = new tab.configServer.ValueString("");
						File fileHandle = tab.util.getLocalRecordFileLib.getLocalRecordFile(Item.getUcid(),Item.getExtension(),AudioFormat,Runner.sPlayUrl,Runner.sPlayHost,Runner.sPlayUsername,Runner.sPlayPassword,Item.getAgent());
						if(fileHandle!=null) {
							FileInputStream fin = new FileInputStream(fileHandle);
							java.io.BufferedInputStream buffered = new java.io.BufferedInputStream(fin);
							// 添加文件名到zip
						//	String sNewZipFileName = Item.getFilename().replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "");
							String format = "yyyy-MM-dd HH:mm:ss";  
							 SimpleDateFormat sdf = new SimpleDateFormat(format);  
							 String time= "000";
							 if(Item.getCreatedate()!=null) {
								 time= sdf.format(Item.getCreatedate());
							 }
								 
							String sNewZipFileName = (time + "-" + Item.getCaller()
							+ "-" + Item.getCalled() + "-" + Item.getExtension() + "-" + Item.getAgent()
							+ "-" + i + (Runner.nPlayMode == 0 ? ".MP3" : (npos>0 ? Item.getFilename().substring(npos) : "")))
									.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "");
							sNewZipFileName += "." + AudioFormat.value;
							zos.putNextEntry(new ZipEntry(sNewZipFileName));
							
//							zos.putNextEntry(new ZipEntry((Item.getCreatedate().getTime() + "-" + Item.getCaller()
//							+ "-" + Item.getCalled() + "-" + Item.getExtension() + "-" + Item.getAgent()
//							+ "-" + i + (Runner.nPlayMode == 0 ? ".MP3" : (npos>0 ? Item.getFilename().substring(npos) : "")))
//									.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")));
							
							// 把下载的文件内容添加到zip
							int len = 0;
							while ((len = buffered.read()) != -1) {
								zos.write(len);
							}
							fin.close();
							f.delete();
						}else {
							String format = "yyyy-MM-dd HH:mm:ss";  
							 SimpleDateFormat sdf = new SimpleDateFormat(format);  
							 String time= "000";
							 if(Item.getCreatedate()!=null) {
								 time= sdf.format(Item.getCreatedate());
							 }
							String sNewZipFileName = ("ERROR"+time + "-" + Item.getCaller()
							+ "-" + Item.getCalled() + "-" + Item.getExtension() + "-" + Item.getAgent()
							+ "-" + i + (Runner.nPlayMode == 0 ? ".MP3" : (npos>0 ? Item.getFilename().substring(npos) : "")))
									.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "");
							sNewZipFileName += "." + AudioFormat.value;
							zos.putNextEntry(new ZipEntry(sNewZipFileName));
						}
					} catch (org.apache.http.conn.ConnectTimeoutException e) {
						log.error(e);
					} catch (java.io.FileNotFoundException e) {
						log.error(e);
					} catch (java.io.IOException e) {
						log.error(e);
					} catch (Throwable e) {
						log.error(e);
					}
				}

				try {
					httpclient.close();
				} catch (IOException e) {
					log.error(e);
				}
				try {
					zos.close();
				} catch (IOException e) {
					return Response.status(404).entity(e.toString()).type("text/plain").build();
				}
				java.io.File zipfile = new java.io.File(sZipFileName);
				return Response.ok(zipfile)
						.header("Content-Disposition", "attachment;filename=\"ReordFiles"
								+ dtStarttime.toString().replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "") + ".zip\"")
						.build();
			}
		return Response.status(404).entity("TOO MANY FILES(" + nSize + "), MAX FILES(" + vi.value + ")")
				.type("text/plain").build();
	}

	@GET
	@Path("/recordingfiles/{sUcidPath:.+}")
	public Response recordingfiles(@Context HttpServletRequest R,@PathParam("sUcidPath") String sUcidPath, @DefaultValue("") @QueryParam("exten")String sExten) {
		if(sUcidPath==null) {
			return Response.status(404).entity("paramters error: ucidpath=" + sUcidPath + ",  exten=" + sExten )
					.type("text/plain").build();
		}
		tab.configServer.ValueString AudioFormat = new tab.configServer.ValueString("");
		File fileHandle = tab.util.getLocalRecordFileLib.getLocalRecordFilepath(sUcidPath,AudioFormat);
		//转为文件流返回给客户端
		if(fileHandle!=null) {
			Calendar cNow = Calendar.getInstance();
			return Response
					.ok(fileHandle)
					.header("Content-Type","audio/" + AudioFormat.value)
					.header("Content-Disposition",
							"attachment;filename=\"ReordFiles"
									+ cNow.get(Calendar.YEAR) + "-"
									+ (cNow.get(Calendar.MONTH) + 1) + "-"
									+ cNow.get(Calendar.DAY_OF_MONTH) + "-"
									+ cNow.get(Calendar.HOUR_OF_DAY) + ""
									+ cNow.get(Calendar.MINUTE) + ""
									+ cNow.get(Calendar.SECOND) + ".WAV\"").build();
		}
		return Response.serverError().build();
	}
	private final class RecordFile {
		public String fileName;
		public String guid;
		public String recordName;
		public String extension;
	}

	@POST
	@Path("/UIRecordsDownload")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/zip" + ";charset=utf-8")
	public Response UIRecordsDownload(@Context HttpServletRequest R, @FormParam("files") String JsonFiles) {
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			return Response.status(404).entity("SESSION NOT FOUND").type("text/plain").build();
		}
		java.util.List<RecordFile> recordList = new java.util.ArrayList<RecordFile>();
		try {
			if (JsonFiles == null) {
				return Response.status(404).build();
			}
			java.util.Map<String, Object> pItems;
			pItems = mapper.readValue(JsonFiles, new TypeReference<java.util.Map<String, Object>>() {
			});
			Object attached = pItems.get("recordFiles");
			@SuppressWarnings("unchecked")
			java.util.List<java.util.Map<String, String>> attacheds = (java.util.List<java.util.Map<String, String>>) attached;
			for (int nIdx = 0; nIdx < attacheds.size(); ++nIdx) {
				RecordFile record = new RecordFile();
				record.recordName = attacheds.get(nIdx).get("filepath");
				int npos = record.recordName.lastIndexOf(".");
				//record.fileName = attacheds.get(nIdx).get("fileName") + (Runner.nPlayMode == 0 ? ".MP3" : (npos>0 ? record.recordName.substring(npos) : ""));
				record.fileName = attacheds.get(nIdx).get("fileName") ;
				record.guid = attacheds.get(nIdx).get("filepath");
				recordList.add(record);
			}
		} catch (IOException e) {
			log.error("ERR:", e);
			return Response.status(404).entity(e.toString()).type("text/plain").build();
		}
		String sPath = System.getProperty("tab.logpath") + "DownloadFiles";
		java.io.File Path = new java.io.File(sPath);
		if (!Path.exists()) {
			if (!Path.mkdirs()) {
				return Response.status(404).entity("can't access " + sPath).type("text/plain").build();
			}
		}
		sPath += File.separator;
		String sZipFileName = sPath + UUID.randomUUID().toString() + ".zip";
		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File(sZipFileName))));
		} catch (FileNotFoundException e) {
			log.error("ERR:", e);
			return Response.status(404).entity(e.toString()).type("text/plain").build();
		}
		CloseableHttpClient httpclient = tab.util.Util
				.getIdleHttpClient(Runner.sPlayUrl.indexOf("https://") == 0 ? true : false);
		for (int i = 0; i < recordList.size(); ++i) {
			RecordFile Item = recordList.get(i);
			// 下载文件
			int npos = Item.recordName.lastIndexOf(".");
			File f = new File(sPath + Item.guid + (Runner.nPlayMode == 0 ? ".MP3" : (npos>0 ? Item.recordName.substring(npos) : "")));
			if (f.exists()) {
				f.delete();
			}
			try {
				tab.configServer.ValueString AudioFormat = new tab.configServer.ValueString("");
				File fileHandle = tab.util.getLocalRecordFileLib.getLocalRecordFilepath(Item.guid,AudioFormat);
				if(fileHandle!=null) {
					FileInputStream fin = new FileInputStream(fileHandle);
					java.io.BufferedInputStream buffered = new java.io.BufferedInputStream(fin);
					// 添加文件名到zip
					String sNewZipFileName = Item.fileName.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "");
					sNewZipFileName += "." + AudioFormat.value;
					zos.putNextEntry(new ZipEntry(sNewZipFileName));
					// 把下载的文件内容添加到zip
					int len = 0;
					while ((len = buffered.read()) != -1) {
						zos.write(len);
					}
					fin.close();
					f.delete();
				}else {
					zos.putNextEntry(
							new ZipEntry("ERR-" + Item.fileName.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")));
				}
			} catch (org.apache.http.conn.ConnectTimeoutException e) {
				log.error(e);
			} catch (java.io.FileNotFoundException e) {
				log.error(e);
			} catch (java.io.IOException e) {
				log.error(e);
			} catch (Throwable e) {
				log.error(e);
			}
		}
		try {
			httpclient.close();
		} catch (IOException e) {
			log.error(e);
		}
		try {
			zos.close();
		} catch (IOException e) {
			return Response.status(404).entity(e.toString()).type("text/plain").build();
		}
		Calendar cNow = Calendar.getInstance();
		java.io.File zipfile = new java.io.File(sZipFileName);
		return Response.ok(zipfile).header("Content-Disposition",
				"attachment;filename=\"ReordFiles" + cNow.get(Calendar.YEAR) + "-" + (cNow.get(Calendar.MONTH) + 1)
						+ "-" + cNow.get(Calendar.DAY_OF_MONTH) + "-" + cNow.get(Calendar.HOUR_OF_DAY) + ""
						+ cNow.get(Calendar.MINUTE) + "" + cNow.get(Calendar.SECOND) + ".zip\"")
				.build();
	}
	
	@POST
	@Path("/UISearchVoiceMailsDownload")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/zip" + ";charset=utf-8")
	public Response UISearchVoiceMailsDownload(@Context HttpServletRequest R,
			@FormParam("starttime") /* "开始时间" */RESTDateParam dtStarttime,
			@FormParam("endtime") /* "结束时间" */RESTDateParam dtEndtime,
			@FormParam("hotline_num") /* "通过被叫号码模糊查询" */String hotline_num, 
			@FormParam("phone") /* "主机编号,大于等于0有效,否则忽略" */String phone,
			@FormParam("sort") /* "排序(ExtJS默认格式, {\"property\":\"字段名\",\"direction\":\"ASC\"})" */EXTJSSortParam Sort) {
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			return Response.status(404).entity("SESSION NOT FOUND").type("text/plain").build();
		}
		String sPath = System.getProperty("tab.logpath") + "DownloadFiles";
		java.io.File Path = new java.io.File(sPath);
		if (!Path.exists()) {
			Path.mkdirs();
		}
		sPath += File.separator;
		int nSize = 0;
			tab.configServer.ValueInteger vi = new tab.configServer.ValueInteger(200);
			tab.configServer.ValueInteger nTotalCount = new tab.configServer.ValueInteger(0);
			java.util.List<java.util.Map<String, Object>> recfiles=searchVoice(phone,hotline_num,dtEndtime,dtStarttime,0,0,Sort,nTotalCount);
			
			nSize = recfiles.size();
			log.info("输出一下语音信箱下载全部条数:"+nSize);
			try {
				tab.configServer.getInstance().getValue(Runner.ConfigName_, "MaxDownloadFiles", vi, "最大下载文件数量", "",
						true, true);
			} catch (ClassNotFoundException err) {
				vi.value = 50;
			}
			if (vi.value < 50)
				vi.value = 50;
			if (nSize < vi.value) {
				String sZipFileName = sPath + UUID.randomUUID().toString() + ".zip";
				ZipOutputStream zos = null;
				try {
					zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File(sZipFileName))));
				} catch (FileNotFoundException e) {
					log.error("ERR:", e);
					return Response.status(404).entity(e.toString()).type("text/plain").build();
				}
				CloseableHttpClient httpclient = tab.util.Util
						.getIdleHttpClient(Runner.sPlayUrl.indexOf("https://") == 0 ? true : false);
				for (int i = 0; i < nSize; ++i) {
					java.util.Map<String, Object> Item = recfiles.get(i);
					int npos = ((String) Item.get("filepath")).lastIndexOf(".");
					
					//File f = new File(sPath + filename + (Runner.nPlayMode == 0 ? ".MP3" : (npos>0 ? ((String) Item.get("filepath")).substring(npos) : "")));
					File f = new File(sPath + Item.get("filename") + (Runner.nPlayMode == 0 ? ".MP3" : (npos>0 ? ((String) Item.get("filepath")).substring(npos) : "")));
					if (f.exists()) {
						f.delete();
					}
					try {
						tab.configServer.ValueString AudioFormat = new tab.configServer.ValueString("");
						File fileHandle = tab.util.getLocalRecordFileLib.getLocalRecordFilepath((String)Item.get("filepath"),AudioFormat);
						if(fileHandle!=null) {
							FileInputStream fin = new FileInputStream(fileHandle);
							java.io.BufferedInputStream buffered = new java.io.BufferedInputStream(fin);
							// 添加文件名到zip
							String sNewZipFileName =(String)(Item.get("starttime")+"-"+Item.get("phone")).replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "");
							//String sNewZipFileName = ((String) Item.get("filepath")).replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "");
							sNewZipFileName += "." + AudioFormat.value;
							zos.putNextEntry(new ZipEntry(sNewZipFileName));
							// 把下载的文件内容添加到zip
							int len = 0;
							while ((len = buffered.read()) != -1) {
								zos.write(len);
							}
							fin.close();
							f.delete();
						}else {
							zos.putNextEntry(
									new ZipEntry("ERR-" + ((String) Item.get("filepath")).replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")));
						}
					} catch (org.apache.http.conn.ConnectTimeoutException e) {
						log.error(e);
					} catch (java.io.FileNotFoundException e) {
						log.error(e);
					} catch (java.io.IOException e) {
						log.error(e);
					} catch (Throwable e) {
						log.error(e);
					}
				}

				try {
					httpclient.close();
				} catch (IOException e) {
					log.error(e);
				}
				try {
					zos.close();
				} catch (IOException e) {
					return Response.status(404).entity(e.toString()).type("text/plain").build();
				}
				java.io.File zipfile = new java.io.File(sZipFileName);
				return Response.ok(zipfile)
						.header("Content-Disposition", "attachment;filename=\"ReordFiles"
								+ dtStarttime.toString().replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "") + ".zip\"")
						.build();
			}
		return Response.status(404).entity("TOO MANY FILES(" + nSize + "), MAX FILES(" + vi.value + ")")
				.type("text/plain").build();
	}
	
	
}
