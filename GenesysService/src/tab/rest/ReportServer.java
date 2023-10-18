package tab.rest;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.reflect.TypeToken;

import hbm.OffersetTransformers;
import hbm.factory.GHibernateSessionFactory;
import hbm.factory.HibernateSessionFactory;
import hbm.model.Rbacrole;
import hbm.model.Recfiles;
import main.Runner;
import tab.EXTJSSortParam;
import tab.GsonUtil;
import tab.RESTDateParam;
import tab.RESTListMapParam;
import tab.configServer;
import tab.jetty.StatWebSocketServlet;
import tab.rbac.RbacClient;
import tab.rbac.RbacSystem;
import tab.rec.RecSystem;
import tab.util.Util;

//call_detual注释 
//mediation_duration 分配时间
//routing_point_duration 路由点中花费时长，跟IVR时长应该一样
@Path("/call")
public class ReportServer {

	public static Log log = LogFactory.getLog(ReportServer.class);
	public static String FILTER_DNIS_SQL = " and A.dnis != '4100' "; // 汇总 明细 统一去掉满意度转接
	public static String FILTER_DETAIL_DNIS_SQL = " and A.called != '4100' "; // 汇总 明细 统一去掉满意度转接
	public static String FILTER_RBAC_GROUP_LIST_SQL = " and (A.agent in (select B.agent from Rbacuserauths B where B.userguid in ( select C.id.userguid from Rbacroleuser C where C.id.roleguid in (:grouplist) ) and length(B.agent)>0 )"
			+ " or A.channel in ( select B.extension from Recextension B where B.roleguid in ( :grouplist ) ) )";
	public static String FILTER_RBAC_GROUP_DETAIL_LIST_SQL = " and (A.agent in (select B.agent from Rbacuserauths B where B.userguid in ( select C.id.userguid from Rbacroleuser C where C.id.roleguid in (:grouplist) ) and length(B.agent)>0 )"
			+ " or A.extension in ( select B.extension from Recextension B where B.roleguid in ( :grouplist ) ) )";
	public static String FILTER_RBAC_GROUP_SIMPLE_LIST_SQL = " and (A.agent in (select B.agent from Rbacuserauths B where B.userguid in ( select C.id.userguid from Rbacroleuser C where C.id.roleguid in (:grouplist) ) and length(B.agent)>0 ))";

	public static boolean bRbacTestFlag = false;

	// API文档: (value = "首页面板查询呼叫数", notes = "")
	@POST
	@Path("/DashboardCallInfo")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response DashboardCallInfo(@Context HttpServletRequest R) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.MONTH, 0);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				java.util.Set<String> agents = null;
				java.util.Set<String> groupList = RbacClient
						.getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")), null, null, false);
				if (groupList.contains(Util.ROOT_ROLEGUID) == bRbacTestFlag) {
					if (groupList.size() > 0) {
						agents = RbacClient.getUserAgents(tab.util.Util.ObjectToString(httpsession.getAttribute("uid")),
								null, false);
					} else {
						agents = new java.util.HashSet<String>();
					}
				}
				String sSqlStr = "select sum(case when (upper(A." + RecSystem.sInteraction_type_name
						+ ")!='INTERNAL' and A.talk_duration>0) then 1 else 0 end) as totalCallCount, sum(case when (upper(A."
						+ RecSystem.sInteraction_type_name
						+ ")='OUTBOUND' and A.talk_duration>0) then 1 else 0 end) as totalCallOutCount from call_detial A where A.start_time>=:start";
				if (agents != null && agents.size() == 0) {
					result.put("totalCallCount", 0);
					result.put("totalCallOutCount", 0);
				} else {
					if (agents != null && agents.size() > 0) {
						sSqlStr += " and A.employee_id in(:agents)";
					}
					log.info(sSqlStr);
					SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
					if (agents != null && agents.size() > 0) {
						query.setTimestamp("start", cal.getTime()).setParameterList("agents", agents)
								.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					} else {
						query.setTimestamp("start", cal.getTime())
								.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					}
					@SuppressWarnings("unchecked")
					java.util.Map<String, Object> mapOne = (Map<String, Object>) query.uniqueResult();
					result.put("totalCallCount", tab.util.Util.ObjectToNumber(mapOne.get("totalcallcount"), 0));
					result.put("totalCallOutCount", tab.util.Util.ObjectToNumber(mapOne.get("totalcalloutcount"), 0));
					result.put("success", true);
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			gdbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}

	// API文档: (value = "首页面板查询录音数", notes = "")
	@POST
	@Path("/DashboardRecInfo")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response DashboardRecInfo(@Context HttpServletRequest R) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.MONTH, 0);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				java.util.Set<String> agents = null;
				java.util.Set<String> groupList = RbacClient
						.getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")), null, null, false);
				if (groupList.contains(Util.ROOT_ROLEGUID) == bRbacTestFlag) {
					if (groupList.size() > 0) {
						agents = RbacClient.getUserAgents(tab.util.Util.ObjectToString(httpsession.getAttribute("uid")),
								null, false);
					} else {
						agents = new java.util.HashSet<String>();
					}
				}
				String sSqlStr = "select sum(case when (A.talk_duration>0) then 1 else 0 end) as totalTagsCount from call_detial A where A.start_time>=:start";
				if (agents != null && agents.size() == 0) {
					result.put("totalTagsCount", 0);
				} else {
					if (agents != null && agents.size() > 0) {
						sSqlStr += " and A.employee_id in(:agents)";
					}
					log.info(sSqlStr);
					SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
					if (agents != null && agents.size() > 0) {
						query.setTimestamp("start", cal.getTime()).setParameterList("agents", agents)
								.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					} else {
						query.setTimestamp("start", cal.getTime())
								.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					}
					@SuppressWarnings("unchecked")
					java.util.Map<String, Object> mapOne = (Map<String, Object>) query.uniqueResult();
					result.put("totalTagsCount", tab.util.Util.ObjectToNumber(mapOne.get("totaltagscount"), 0));
					result.put("success", true);
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			gdbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}

	// API文档: (value = "首页面板查询日汇总", notes = "")
	@POST
	@Path("/DashboardDaySummary")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response DashboardDaySummary(@Context HttpServletRequest R) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.MONTH, -12);
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				java.util.Set<String> agents = null;
				java.util.Set<String> groupList = RbacClient
						.getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")), null, null, false);
				if (groupList.contains(Util.ROOT_ROLEGUID) == bRbacTestFlag) {
					if (groupList.size() > 0) {
						agents = RbacClient.getUserAgents(tab.util.Util.ObjectToString(httpsession.getAttribute("uid")),
								null, false);
					} else {
						agents = new java.util.HashSet<String>();
					}
				}
				String sSqlStr = "select EXTRACT(YEAR FROM A.start_time) as year, EXTRACT(MONTH FROM A.start_time) as month , EXTRACT(DAY FROM A.start_time) as day,"
						+ " sum(case when (upper(A." + RecSystem.sInteraction_type_name
						+ ")!='OUTBOUND' and A.talk_duration>0) then 1 else 0 end) as callins,"
						+ " sum(case when (upper(A." + RecSystem.sInteraction_type_name
						+ ")='OUTBOUND' and A.talk_duration>0) then 1 else 0 end) as callouts from call_detial A where A.start_time>=:start";
				String sGroupby = " group by EXTRACT(YEAR FROM A.start_time),EXTRACT(MONTH FROM A.start_time),EXTRACT(DAY FROM A.start_time)"
						+ " order by EXTRACT(YEAR FROM A.start_time) desc,EXTRACT(MONTH FROM A.start_time) desc,EXTRACT(DAY FROM A.start_time) desc";
				if (agents != null && agents.size() == 0) {
					result.put("items", null);
				} else {
					if (agents != null && agents.size() > 0) {
						sSqlStr += " and A.employee_id in(:agents)";
					}
					sSqlStr += sGroupby;
					log.debug(sSqlStr);
					SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
					if (agents != null && agents.size() > 0) {
						query.setTimestamp("start", cal.getTime()).setParameterList("agents", agents).setFirstResult(0)
								.setMaxResults(30).setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					} else {
						query.setTimestamp("start", cal.getTime()).setFirstResult(0).setMaxResults(30)
								.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					}
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> items = query.list();
					for (java.util.Map<String, Object> item : items) {
						item.put("datetime", item.get("year") + "-" + item.get("month") + "-" + item.get("day"));
					}
					java.util.Collections.reverse(items);
					result.put("items", items);
					result.put("success", true);
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			gdbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}

	// API文档: (value = "首页面板查询月汇总", notes = "")
	@POST
	@Path("/DashboardMonthSummary")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response DashboardMonthSummary(@Context HttpServletRequest R) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.MONTH, -12);
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				java.util.Set<String> agents = null;
				java.util.Set<String> groupList = RbacClient
						.getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")), null, null, false);
				if (groupList.contains(Util.ROOT_ROLEGUID) == bRbacTestFlag) {
					if (groupList.size() > 0) {
						agents = RbacClient.getUserAgents(tab.util.Util.ObjectToString(httpsession.getAttribute("uid")),
								null, false);
					} else {
						agents = new java.util.HashSet<String>();
					}
				}
				String sSqlStr = "select EXTRACT(YEAR FROM A.start_time) as year, EXTRACT(MONTH FROM A.start_time) as month,"
						+ " sum(case when A.talk_duration>0 then 1 else 0 end) as calls from call_detial A where A.start_time>=:start ";
				String sGroupby = " group by EXTRACT(YEAR FROM A.start_time),EXTRACT(MONTH FROM A.start_time) order by EXTRACT(YEAR FROM A.start_time),EXTRACT(MONTH FROM A.start_time)";
				if (agents != null && agents.size() == 0) {
					result.put("items", null);
				} else {
					if (agents != null && agents.size() > 0) {
						sSqlStr += " and A.employee_id in(:agents)";
					}
					sSqlStr += sGroupby;
					log.debug(sSqlStr);
					SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
					if (agents != null && agents.size() > 0) {
						query.setTimestamp("start", cal.getTime()).setParameterList("agents", agents).setFirstResult(0)
								.setMaxResults(30).setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					} else {
						query.setTimestamp("start", cal.getTime()).setFirstResult(0).setMaxResults(30)
								.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					}
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> items = query.list();
					for (java.util.Map<String, Object> item : items) {
						item.put("datetime", item.get("year") + "-" + item.get("month"));
					}
					result.put("items", items);
					result.put("success", true);
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			gdbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}

	// API文档: (value = "首页面板查询今日汇总", notes = "")
	@POST
	@Path("/DashboardHourSummary")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response DashboardHourSummary(@Context HttpServletRequest R) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<java.util.Map<String, Object>>();
		for (int hour = 0; hour < 24; hour++) {
			java.util.Map<String, Object> data = new java.util.HashMap<String, Object>();
			data.put("hour", hour);
			data.put("callouts", 0);
			data.put("callins", 0);
			items.add(hour, data);
		}
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				java.util.Set<String> agents = null;
				java.util.Set<String> groupList = RbacClient
						.getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")), null, null, false);
				if (groupList.contains(Util.ROOT_ROLEGUID) == bRbacTestFlag) {
					if (groupList.size() > 0) {
						agents = RbacClient.getUserAgents(tab.util.Util.ObjectToString(httpsession.getAttribute("uid")),
								null, false);
					} else {
						agents = new java.util.HashSet<String>();
					}
				}
				String sSqlStr = "select TO_CHAR(start_time,'hh24') as hour, sum(case when (upper(A."
						+ RecSystem.sInteraction_type_name
						+ ")!='OUTBOUND' and A.talk_duration>0) then 1 else 0 end) as callins,"
						+ " sum(case when (upper(A." + RecSystem.sInteraction_type_name
						+ ")='OUTBOUND' and A.talk_duration>0) then 1 else 0 end) as callouts from call_detial A where A.start_time>=:start";
				String sGroupby = " group by TO_CHAR(start_time,'hh24')";
				if (agents != null && agents.size() == 0) {
					result.put("items", null);
				} else {
					if (agents != null && agents.size() > 0) {
						sSqlStr += " and A.employee_id in(:agents)";
					}
					sSqlStr += sGroupby;
					log.debug(sSqlStr);
					SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
					if (agents != null && agents.size() > 0) {
						query.setTimestamp("start", cal.getTime()).setParameterList("agents", agents).setFirstResult(0)
								.setMaxResults(30).setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					} else {
						query.setTimestamp("start", cal.getTime()).setFirstResult(0).setMaxResults(30)
								.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					}
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> datas = query.list();
					for (java.util.Map<String, Object> data : datas) {
						int hour = data.get("HOUR") != null ? Util.ObjectToNumber(data.get("HOUR"), 0)
								: Util.ObjectToNumber(data.get("hour"), 0);
						if (hour >= 0 && hour < 24) {
							items.get(hour).put("callins", data.get("callins"));
							items.get(hour).put("callouts", data.get("callouts"));
						}
					}
					result.put("items", items);
					result.put("success", true);
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			gdbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}

	// API文档: (value = "首页面板查询今日汇总", notes = "")
	@POST
	@Path("/DashboardAgentSummary")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response DashboardAgentSummary(@Context HttpServletRequest R) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<java.util.Map<String, Object>>();
		try {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.add(Calendar.HOUR, -24 * 5);
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				java.util.Set<String> agents = null;
				java.util.Set<String> agentAlls = new java.util.HashSet<String>();
				java.util.Set<String> groupList = RbacClient
						.getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")), null, null, false);
				if (groupList.contains(Util.ROOT_ROLEGUID) == bRbacTestFlag) {
					if (groupList.size() > 0) {
						agents = RbacClient.getUserAgents(tab.util.Util.ObjectToString(httpsession.getAttribute("uid")),
								null, false);
					} else {
						agents = new java.util.HashSet<String>();
					}
				}
				if (Runner.MAX_AGENT_RANKING > 0) {
					String sSqlStr = "select A.employee_id as agent, 'username' as username,"
							+ " sum(case when A.talk_duration>0 then 1 else 0 end) as calls,"
							+ " sum(case when (upper(A." + RecSystem.sInteraction_type_name + ")!='OUTBOUND'"
							+ " and A.talk_duration>0) then 1 else 0 end) as callins," + " sum(case when (upper(A."
							+ RecSystem.sInteraction_type_name + ")='OUTBOUND'"
							+ " and A.talk_duration>0) then 1 else 0 end) as callouts" + " from call_detial A"
							+ " where A.start_time>=:start and length(A.employee_id)>0";
					String sGroupby = " group by A.employee_id order by sum(case when A.talk_duration>0 then 1 else 0 end) desc";
					if (agents != null && agents.size() == 0) {
						result.put("items", null);
					} else {
						if (agents != null && agents.size() > 0) {
							sSqlStr += " and A.employee_id in(:agents)";
						}
						sSqlStr += sGroupby;
						log.debug(sSqlStr);
						SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
						if (agents != null && agents.size() > 0) {
							query.setTimestamp("start", cal.getTime()).setParameterList("agents", agents)
									.setFirstResult(0).setMaxResults(Runner.MAX_AGENT_RANKING)
									.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
						} else {
							query.setTimestamp("start", cal.getTime()).setFirstResult(0)
									.setMaxResults(Runner.MAX_AGENT_RANKING)
									.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
						}
						@SuppressWarnings("unchecked")
						java.util.List<java.util.Map<String, Object>> firsdatas = query.list();
						for (java.util.Map<String, Object> data : firsdatas) {
							agentAlls.add(tab.util.Util.ObjectToString(data.get("agent")));
							items.add(data);
						}
					}
				}
				if (Runner.MIN_AGENT_RANKING > 0) {
					String sSqlStr = "select A.employee_id as agent, 'username' as username,"
							+ " sum(case when A.talk_duration>0 then 1 else 0 end) as calls,"
							+ " sum(case when (upper(A." + RecSystem.sInteraction_type_name + ")!='OUTBOUND'"
							+ " and A.talk_duration>0) then 1 else 0 end) as callins," + " sum(case when (upper(A."
							+ RecSystem.sInteraction_type_name + ")='OUTBOUND'"
							+ " and A.talk_duration>0) then 1 else 0 end) as callouts" + " from call_detial A"
							+ " where A.start_time>=:start and length(A.employee_id)>0";
					String sGroupby = " group by A.employee_id order by sum(case when A.talk_duration>0 then 1 else 0 end) desc";
					if (agents != null && agents.size() == 0) {
						result.put("items", null);
					} else {
						if (agents != null && agents.size() > 0) {
							sSqlStr += " and A.employee_id in(:agents)";
						}
						sSqlStr += sGroupby;
						log.debug(sSqlStr);
						SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
						if (agents != null && agents.size() > 0) {
							query.setTimestamp("start", cal.getTime()).setParameterList("agents", agents)
									.setFirstResult(0).setMaxResults(Runner.MAX_AGENT_RANKING)
									.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
						} else {
							query.setTimestamp("start", cal.getTime()).setFirstResult(0)
									.setMaxResults(Runner.MAX_AGENT_RANKING)
									.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
						}
						@SuppressWarnings("unchecked")
						java.util.List<java.util.Map<String, Object>> lastdatas = query.list();
						lastdatas.sort(new java.util.Comparator<java.util.Map<String, Object>>() {
							@Override
							public int compare(java.util.Map<String, Object> o1, java.util.Map<String, Object> o2) {
								return Util.ObjectToNumber(o2.get("calls"), 0)
										- Util.ObjectToNumber(o1.get("calls"), 0);
							}
						});
						for (java.util.Map<String, Object> data : lastdatas) {
							agentAlls.add(tab.util.Util.ObjectToString(data.get("agent")));
							items.add(data);
						}
					}
				}
				if (agentAlls.size() > 0) {
					@SuppressWarnings("unchecked")
					java.util.List<Object[]> maps = (java.util.List<Object[]>) dbsession
							.createQuery("select agent, username from Rbacuserauths where agent in(:agent)")
							.setParameterList("agent", agentAlls).list();
					java.util.Map<String, String> AgentMap = new java.util.HashMap<String, String>();
					for (Object[] map : maps) {
						AgentMap.put(map[0].toString(), tab.util.Util.ObjectToString(map[1]));
					}
					for (java.util.Map<String, Object> item : items) {
						item.put("username", AgentMap.get(item.get("agent")));
					}
				}
				result.put("items", items);
				result.put("success", true);
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			gdbsession.close();
			dbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}

	private static ObjectMapper mapper = new ObjectMapper();

	// API文档: (value = "首页面板查询呼叫数", notes = "")
	@POST
	@Path("/SoftPhoneAgentDetails")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response SoftPhoneAgentDetails(@Context HttpServletRequest R) throws IOException {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		String sJson = org.apache.commons.io.IOUtils.toString(R.getInputStream(), "UTF-8");
		java.util.Map<String, Object> data = mapper.readValue(sJson,
				new TypeReference<java.util.Map<String, Object>>() {
				});
		String sAgent = Util.ObjectToString(data.get("agent"));
		Integer pageStart = Util.ObjectToNumber(data.get("start"), 0);
		Integer pageLimit = Util.ObjectToNumber(data.get("length"), 10);
		@SuppressWarnings("unchecked")
		java.util.List<java.util.Map<String, Object>> order = (java.util.List<java.util.Map<String, Object>>) data
				.get("order");
		@SuppressWarnings("unchecked")
		java.util.List<java.util.Map<String, Object>> columns = (java.util.List<java.util.Map<String, Object>>) data
				.get("columns");
		@SuppressWarnings("unchecked")
		java.util.Map<String, Object> search = (java.util.Map<String, Object>) data.get("search");
		String sOrder = "";
		String sWhere = "";
		String sValue = "";
		for (int i = 0; i < order.size(); i++) {
			int j = Util.ObjectToNumber(order.get(i).get("column"), -1);
			if (j >= 0 && j < columns.size()) {
				if (sOrder.length() == 0)
					sOrder = " order by ";
				else
					sOrder += ",";
				sOrder += columns.get(j).get("data") + " " + order.get(i).get("dir");
				if (Boolean.parseBoolean(Util.ObjectToString(columns.get(j).get("searchable")))) {
					sValue = Util.ObjectToString(search.get("value"));
					if (sValue.length() > 0) {
						sWhere += " and ";
						sWhere += columns.get(j).get("data") + " like :value";
					}
				}
			}
		}
		Session dbsession = HibernateSessionFactory.getThreadSession();
		try {
			Integer nTotalRecords = Util
					.ObjectToNumber(dbsession.createQuery("select count(*) from Recfiles where agent=:agent")
							.setString("agent", sAgent).uniqueResult(), 0);
			Integer nFilteredRecords = nTotalRecords;
			if (nTotalRecords > 0 && sWhere.length() > 0) {
				nFilteredRecords = Util.ObjectToNumber(
						dbsession.createQuery("select count(*) from Recfiles where agent=:agent" + sWhere)
								.setString("value", "%" + sValue + "%").setString("agent", sAgent).uniqueResult(),
						0);
			}
			if (nTotalRecords > 0) {
				Query query = dbsession.createQuery(" from Recfiles where agent=:agent" + sWhere + sOrder)
						.setString("agent", sAgent).setFirstResult(pageStart).setMaxResults(pageLimit);
				if (sWhere.length() > 0) {
					query.setString("value", "%" + sValue + "%");
				}
				@SuppressWarnings("unchecked")
				java.util.List<Recfiles> files = query.list();
				result.put("items", files);
			} else {
				result.put("items", new java.util.ArrayList<Recfiles>());
			}
			result.put("recordsFiltered", nFilteredRecords);
			result.put("recordsTotal", nTotalRecords);
			result.put("success", true);
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		dbsession.close();
		return Response.status(200).entity(result).build();
	}

	// 查询坐席工作明细
	@POST
	@Path("/ReportAgentWork")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ReportAgentWork(@Context HttpServletRequest R, @FormParam("starttime") RESTDateParam dtStarttime,
			@FormParam("endtime") RESTDateParam dtEndtime, @FormParam("agent") String sAgent,
			@FormParam("extension") String sExtension, @FormParam("groupguid") String sGroupguid,
			@FormParam("start") Integer pageStart, @FormParam("limit") Integer pageLimit,
			@FormParam("script") String sScript) {
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(404).entity("no datas").type("text/plain").build();
	}

	// 查询坐席通话明细
	@POST
	@Path("/ReportAgent")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response ReportAgent(@Context HttpServletRequest R, @Context ContainerRequestContext ctx,
			@FormParam("appid") /* "查询者" */String sAppId, @FormParam("token") /* "查询者" */String sToken,
			@FormParam("code") /* "查询者code" */String sCode,
			@FormParam("starttime") /* "开始时间" */RESTDateParam dtStarttime,
			@FormParam("endtime") /* "结束时间" */RESTDateParam dtEndtime, @FormParam("agent") /* "通过工号查询" */String sAgent,
			@FormParam("extension") /* "通过分机号码查询" */String sExtension,
			@FormParam("caller") /* "通过主叫号码模糊查询" */String sCaller,
			@FormParam("called") /* "通过被叫号码模糊查询" */String sCalled,
			@DefaultValue("0") @FormParam("inbound") /* "通过呼叫方向查询，0全部，1呼入，2呼出" */Integer nInbound,
			@DefaultValue("0") @FormParam("inside") /* "通过内外线查询，0全部，1内线，2外线" */Integer nInside,
			@DefaultValue("0") @FormParam("weekend") /* "通过工作日查询，0全部，1无周日，2无周六周日" */Integer nWeekend,
			@DefaultValue("0") @FormParam("length") /* "通话最小长度" */Integer nLength,
			@DefaultValue("0") @FormParam("wait") /* "振铃最小长度" */Integer nWait,
			@FormParam("groupguid") String sGroupguid, @FormParam("start") /* "开始页，从0开始" */Integer pageStart,
			@FormParam("limit") /* "每页数量，从1开始" */Integer pageLimit, @FormParam("script") String sScript) {
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		if (nInbound == null)
			nInbound = 0;
		if (nWeekend == null)
			nWeekend = 0;
		if (nLength == null)
			nLength = 0;
		if (nWait == null)
			nWait = 0;
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(404).entity("no datas").type("text/plain").build();
	}

	@POST
	@Path("/ReportDetail")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response ReportDetail(@Context HttpServletRequest R, @FormParam("starttime") RESTDateParam dtStarttime,
			@FormParam("endtime") RESTDateParam dtEndtime, @FormParam("agent") String sAgent,
			@FormParam("extension") String sExtension, @FormParam("caller") String sCaller,
			@FormParam("inbound") Integer nInbound, @FormParam("inside") Integer nInside,
			@FormParam("nType") Integer nType, @FormParam("weekend") Integer nWeekend,
			@FormParam("length") Integer nLength, @FormParam("groupguid") String sGroupguid,
			@FormParam("start") Integer pageStart, @FormParam("limit") Integer pageLimit,
			@FormParam("script") String sScript) {
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		if (nInbound == null)
			nInbound = 0;
		if (nWeekend == null)
			nWeekend = 0;
		if (nLength == null)
			nLength = 0;
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(404).entity("no datas").type("text/plain").build();
	}

	@POST
	@Path("/ReportInvestigate")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ReportInvestigate(@Context HttpServletRequest R, @FormParam("starttime") RESTDateParam dtStarttime,
			@FormParam("endtime") RESTDateParam dtEndtime, @FormParam("agent") String sAgent,
			@FormParam("caller") String sCaller, @FormParam("groupguid") String sGroupguid,
			@FormParam("start") Integer pageStart, @FormParam("limit") Integer pageLimit,
			@FormParam("script") String sScript) {
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(404).entity("no datas").type("text/plain").build();
	}

	@POST
	@Path("/ReportInvestigateRecfile")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ReportInvestigateRecfile(@Context HttpServletRequest R, @FormParam("callId") String sCalId) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Recfiles recfile = (Recfiles) dbsession.createQuery(" from Recfiles where ucid=:callId")
						.setString("callId", sCalId).setFirstResult(0).setMaxResults(1).uniqueResult();
				if (recfile != null) {
					result.put("recfile", recfile);
					result.put("success", true);
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/ReportInvestigateSummary")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ReportInvestigateSummary(@Context HttpServletRequest R,
			@FormParam("starttime") RESTDateParam dtStarttime, @FormParam("endtime") RESTDateParam dtEndtime,
			@FormParam("type") Integer nType, @FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		String sWhere = " A.investigateDate>=:starttime and A.investigateDate<=:endtime  and length(A.agentNo)>0)";
		java.util.Set<String> groupList = RbacClient
				.getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")), null, null, true);
		if (groupList.size() > 0 && groupList.contains(Util.ROOT_ROLEGUID)) {
			groupList.clear();
		} else if (groupList.size() > 0) {
			// 组列表直接获取工号列表
			sWhere += " and A.agentNo in (select B.agent from Rbacuserauths B where B.userguid in ( select C.id.userguid from Rbacroleuser C where C.id.roleguid in (:grouplist) ) and length(B.agent)>0 )";
		} else {
			sWhere += " and 1=0";
		}
		String sSelect = "select new Map(COUNT(*) as nTotalCallCount,COUNT(case A.result when 0 then 1 end) as n0Count,COUNT(case A.result when 1 then 1 end) as n1Count,COUNT(case A.result when 2 then 1 end) as n2Count,COUNT(case A.result when 3 then 1 end) as n3Count,COUNT(case A.result when 4 then 1 end) as n4Count,COUNT(case A.result when 5 then 1 end) as n5Count,A.businessType as businessType,YEAR(A.investigateDate) as nYear,MONTH(A.investigateDate) as nMonth,DAY(A.investigateDate) as nDay,HOUR(A.investigateDate) as nHour,A.agentNo as agentNo, 0 as nType)";
		String sGroupBy = " group by A.businessType,YEAR(A.investigateDate),MONTH(A.investigateDate),DAY(A.investigateDate),HOUR(A.investigateDate),A.agentNo";
		switch (nType) {
		default:
		case 0:// hour
			break;
		case 1:// day
			sSelect = "select new Map(COUNT(*) as nTotalCallCount,COUNT(case A.result when 0 then 1 end) as n0Count,COUNT(case A.result when 1 then 1 end) as n1Count,COUNT(case A.result when 2 then 1 end) as n2Count,COUNT(case A.result when 3 then 1 end) as n3Count,COUNT(case A.result when 4 then 1 end) as n4Count,COUNT(case A.result when 5 then 1 end) as n5Count,A.businessType as businessType,YEAR(A.investigateDate) as nYear,MONTH(A.investigateDate) as nMonth,DAY(A.investigateDate) as nDay,A.agentNo as agentNo, 1 as nType)";
			sGroupBy = " group by businessType,YEAR(investigateDate),MONTH(investigateDate),DAY(investigateDate),agentNo";
			break;
		case 2:// week
			sSelect = "select new Map(COUNT(*) as nTotalCallCount,COUNT(case A.result when 0 then 1 end) as n0Count,COUNT(case A.result when 1 then 1 end) as n1Count,COUNT(case A.result when 2 then 1 end) as n2Count,COUNT(case A.result when 3 then 1 end) as n3Count,COUNT(case A.result when 4 then 1 end) as n4Count,COUNT(case A.result when 5 then 1 end) as n5Count,A.businessType as businessType,YEAR(A.investigateDate) as nYear,week(A.investigateDate) as nWeek,A.agentNo as agentNo, 2 as nType)";
			sGroupBy = " group by A.businessType,YEAR(A.investigateDate),WEEK(A.investigateDate),A.agentNo";
			break;
		case 3:// month
			sSelect = "select new Map(COUNT(*) as nTotalCallCount,COUNT(case A.result when 0 then 1 end) as n0Count,COUNT(case A.result when 1 then 1 end) as n1Count,COUNT(case A.result when 2 then 1 end) as n2Count,COUNT(case A.result when 3 then 1 end) as n3Count,COUNT(case A.result when 4 then 1 end) as n4Count,COUNT(case A.result when 5 then 1 end) as n5Count,A.businessType as businessType,YEAR(A.investigateDate) as nYear,MONTH(A.investigateDate) as nMonth,A.agentNo as agentNo, 3 as nType)";
			sGroupBy = " group by A.businessType,YEAR(A.investigateDate),MONTH(A.investigateDate),A.agentNo";
			break;
		}
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Query query = dbsession.createQuery(sSelect + " from InvestigateResults A where " + sWhere + sGroupBy)
						.setTimestamp("starttime", dtStarttime.getDate()).setTimestamp("endtime", dtEndtime.getDate());
				if (groupList.size() > 0) {
					query.setParameterList("grouplist", groupList);
				}
				@SuppressWarnings("unchecked")
				java.util.List<java.util.Map<String, Object>> InvestigateResultsList = query.list();
				java.util.Set<String> AgentList = new java.util.HashSet<>();
				for (int i = 0; i < InvestigateResultsList.size(); i++) {
					AgentList.add(Util.ObjectToString(InvestigateResultsList.get(i).get("agentNo")));
				}
				java.util.Map<String, String> AgentMap = new java.util.HashMap<String, String>();
				if (AgentList.size() > 0) {
					@SuppressWarnings("unchecked")
					java.util.List<Object[]> maps = (java.util.List<Object[]>) dbsession.createQuery(
							"select agent as agent, username as username from Rbacuserauths where agent in(:agent)")
							.setParameterList("agent", AgentList).list();
					for (Object[] map : maps) {
						AgentMap.put(map[0].toString(), map[1].toString());
					}
				}
				tab.MyExportExcelFile export = null;

				if (sScript == null || sScript.length() == 0) {

				} else {
					export = new tab.MyExportExcelFile();
					if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator") + "WebRoot"
							+ sScript)) {
						dbsession.close();
						return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain").build();
					}
				}

				if (sScript == null || sScript.length() == 0) {
					for (int i = 0; i < InvestigateResultsList.size(); i++) {
						java.util.Map<String, Object> call = InvestigateResultsList.get(i);
						call.put("username", AgentMap.get(call.get("agentNo")));
					}
				} else {
					for (int i = 0; i < InvestigateResultsList.size(); i++) {
						java.util.HashMap<String, Object> call = (HashMap<String, Object>) InvestigateResultsList
								.get(i);
						call.put("username", AgentMap.get(call.get("agentNo")));
						export.CommitRow(call);
					}
				}
				if (sScript == null || sScript.length() == 0) {
					result.put("list", InvestigateResultsList);
					result.put("success", true);
				} else {
					String sFileName = export.GetFile();
					if (sFileName != null && sFileName.length() > 0) {
						java.io.File f = new java.io.File(sFileName);
						int pos = sFileName.lastIndexOf(".");
						dbsession.close();
						return Response.ok(f)
								.header("Content-Disposition",
										"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
												+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
														.format(Calendar.getInstance().getTime())
														.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
												+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
								.build();
					}
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
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
	@Path("/ReportAgentSummary")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ReportAgentSummary(@Context HttpServletRequest R, @FormParam("starttime") RESTDateParam dtStarttime,
			@FormParam("endtime") RESTDateParam dtEndtime, @DefaultValue("1") @FormParam("type") Integer nType,
			@FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		String sWhere = " A.ringtime>=:starttime and A.ringtime<=:endtime" + FILTER_DNIS_SQL;
		java.util.Set<String> groupList = RbacClient
				.getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")), null, null, true);
		if (groupList.size() > 0 && groupList.contains(Util.ROOT_ROLEGUID)) {
			groupList.clear();
		} else if (groupList.size() > 0) {
			// 组列表直接获取工号列表
			sWhere += FILTER_RBAC_GROUP_LIST_SQL;
		} else {
			sWhere += " and 1=0";
		}
		// 0 呼入接听，坐席挂机
		// 1 呼入接听外线挂机
		// 2 呼出接听，坐席挂机
		// 3 呼入未接
		// 4 呼出未接通
		// 5 呼出接听，外线挂机
		String sSelect = "select new Map(SUM(case when (A.type=2 or A.type=5) then 1 else 0 end) as nOutboundCount,"
				+ "SUM(case when (A.type=2 or A.type=5) then A.length end) as nOutboundLength,"
				+ "SUM(case when (A.type=2 or A.type=5) then A.wait end) as nOutboundWait,"
				+ "SUM(case when A.type=4 then 1 else 0 end) as nNoConnectCount,"
				+ "SUM(case when A.type=4 then A.wait end) as nNoConnectWait,"
				+ "SUM(case when (A.type=0 or A.type=1) then 1 else 0 end) as nInboundCount,"
				+ "SUM(case when (A.type=0 or A.type=1) then A.length end) as nInboundLength,"
				+ "SUM(case when (A.type=0 or A.type=1) then A.wait end) as nInboundWait,"
				+ "SUM(case when A.type=3 then 1 else 0 end) as nNoAnswerCount,"
				+ "SUM(case when A.type=3 then A.wait end) as nNoAnswerWait,"
				+ "MAX(case when (A.type=2 or A.type=5) then A.length end) as nMaxConnectLength,"
				+ "MAX(case when (A.type=0 or A.type=1) then A.length end) as nMaxAnswerLength,"
				+ "SUM(case when ( (A.type=2 or A.type=4 or A.type=5) and A.wait<=5) then 1 else 0 end) as nOutboundWaitLessCount,"
				// 呼入接通数>= -- (通话与振铃总时长 > 最小时长 ) 的呼入电话总数
				+ "SUM(case when ( (A.type=0 or A.type=1 or A.type=3) and A.wait<=5) then 1 else 0 end) as nInboundWaitLessCount,"
				// 小于呼出应答标准的呼出无人应答数
				+ "SUM(case when ( A.type=4 and length(A.dnis)>4 and A.wait<=5) then 1 else 0 end) as nOutboundAbandonWaitLessCount,"
				// 小于应答标准的呼入失败次数
				+ "SUM(case when ( A.type=3 and A.wait<=5) then 1 else 0 end) as nInboundAbandonWaitLessCount";
		String sGroupBy = " group by A.workshifts,A.agent,YEAR(A.ringtime),MONTH(A.ringtime),DAY(A.ringtime),HOUR(A.ringtime)";
		String sOrderBy = " order by A.workshifts,A.agent,nYear,nMonth,nDay,nHour";
		switch (nType) {
		default:
		case 0:// hour
			sSelect += ",A.workshifts as nWorkShifts, YEAR(A.ringtime) as nYear,MONTH(A.ringtime) as nMonth,DAY(A.ringtime) as nDay,HOUR(A.ringtime) as nHour,A.agent as agent, 0 as nType)";
			break;
		case 1:// day
			sSelect += ",A.workshifts as nWorkShifts, YEAR(A.ringtime) as nYear,MONTH(A.ringtime) as nMonth,DAY(A.ringtime) as nDay,A.agent as agent, 1 as nType)";
			sGroupBy = " group by A.workshifts,A.agent,YEAR(A.ringtime),MONTH(A.ringtime),DAY(A.ringtime)";
			sOrderBy = " order by A.workshifts,A.agent,nYear,nMonth,nDay";
			break;
		case 2:// week
			sSelect += ",A.workshifts as nWorkShifts, YEAR(A.ringtime) as nYear,week(A.ringtime) as nWeek,A.agent as agent, 2 as nType)";
			sGroupBy = " group by A.workshifts,A.agent,YEAR(A.ringtime),week(A.ringtime)";
			sOrderBy = " order by A.workshifts,A.agent,nYear,nWeek";
			break;
		case 3:// month
			sSelect += ",A.workshifts as nWorkShifts, YEAR(A.ringtime) as nYear,MONTH(A.ringtime) as nMonth,A.agent as agent, 3 as nType)";
			sGroupBy = " group by A.workshifts,A.agent,YEAR(A.ringtime),MONTH(A.ringtime)";
			sOrderBy = " order by A.workshifts,A.agent,nYear,nMonth";
			break;
		}
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Query query = dbsession
						.createQuery(sSelect + " from Callagentrecord A where " + sWhere + sGroupBy + sOrderBy)
						.setTimestamp("starttime", dtStarttime.getDate()).setTimestamp("endtime", dtEndtime.getDate());
				if (groupList.size() > 0) {
					query.setParameterList("grouplist", groupList);
				}
				@SuppressWarnings("unchecked")
				java.util.List<java.util.Map<String, Object>> callagentrecordList = query.list();
				java.util.Set<String> AgentList = new java.util.HashSet<>();
				for (int i = 0; i < callagentrecordList.size(); i++) {
					AgentList.add(Util.ObjectToString(callagentrecordList.get(i).get("agent")));
				}
				java.util.Map<String, String> AgentMap = new java.util.HashMap<String, String>();
				if (AgentList.size() > 0) {
					@SuppressWarnings("unchecked")
					java.util.List<Object[]> maps = (java.util.List<Object[]>) dbsession.createQuery(
							"select agent as agent, username as username from Rbacuserauths where agent in(:agent)"
									+ "and length(agent)>0")
							.setParameterList("agent", AgentList).list();
					for (Object[] map : maps) {
						AgentMap.put(map[0].toString(), map[1].toString());
					}
				}
				tab.MyExportExcelFile export = null;

				if (sScript == null || sScript.length() == 0) {

				} else {
					export = new tab.MyExportExcelFile();
					if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator") + "WebRoot"
							+ sScript)) {
						dbsession.close();
						return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain").build();
					}
				}

				if (sScript == null || sScript.length() == 0) {
					for (int i = 0; i < callagentrecordList.size(); i++) {
						java.util.Map<String, Object> call = callagentrecordList.get(i);
						call.put("username", AgentMap.get(call.get("agent")));
					}
				} else {
					for (int i = 0; i < callagentrecordList.size(); i++) {
						java.util.HashMap<String, Object> call = (HashMap<String, Object>) callagentrecordList.get(i);
						call.put("username", AgentMap.get(call.get("agent")));
						export.CommitRow(call);
					}
				}
				if (sScript == null || sScript.length() == 0) {
					result.put("list", callagentrecordList);
					result.put("success", true);
				} else {
					String sFileName = export.GetFile();
					if (sFileName != null && sFileName.length() > 0) {
						java.io.File f = new java.io.File(sFileName);
						int pos = sFileName.lastIndexOf(".");
						dbsession.close();
						return Response.ok(f)
								.header("Content-Disposition",
										"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
												+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
														.format(Calendar.getInstance().getTime())
														.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
												+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
								.build();
					}
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		if (sScript == null || sScript.length() == 0) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(404).entity("no datas").type("text/plain").build();
	}

	@SuppressWarnings("unused")
	private static class AgentWorkSummary {
		public Integer nYear, nMonth, nDay, nDayOfWeek, nHour, nType;
		public String agent, username, sWorkTime, sEndTime, sBeginTime;
		public Integer nWorkLength, nReadyCount, nReadyLength, nAfterWorkCount, nAfterWorkLength;
		public Integer nNotReadyCount10, nNotReadyLength10, nNotreadyLength;
	}

	private java.util.List<AgentWorkSummary> MapToAgentWorkSummary(
			java.util.List<java.util.Map<String, Object>> items) {
		java.util.List<AgentWorkSummary> summarys = new java.util.ArrayList<AgentWorkSummary>();
		for (int i = 0; i < items.size(); i++) {
			java.util.Map<String, Object> item = items.get(i);
			AgentWorkSummary summary = new AgentWorkSummary();
			summary.agent = tab.util.Util.ObjectToString(item.get("resource_name"));
			summary.nAfterWorkCount = tab.util.Util.ObjectToNumber(item.get("busycount"), 0);
			summary.nAfterWorkLength = tab.util.Util.ObjectToNumber(item.get("busy_time"), 0);
			summary.nYear = tab.util.Util.ObjectToNumber(item.get("nyear"), 0);
			summary.nDay = tab.util.Util.ObjectToNumber(item.get("nday"), 0);
			summary.nDayOfWeek = tab.util.Util.ObjectToNumber(item.get("ndayofweek"), 0);
			summary.nHour = tab.util.Util.ObjectToNumber(item.get("nhour"), 0);
			summary.nMonth = tab.util.Util.ObjectToNumber(item.get("nmonth"), 0);
			summary.nNotReadyCount10 = tab.util.Util.ObjectToNumber(item.get("notreadycount"), 0);
			summary.nNotReadyLength10 = tab.util.Util.ObjectToNumber(item.get("not_ready_time"), 0);
			summary.nReadyCount = tab.util.Util.ObjectToNumber(item.get("readycount"), 0);
			summary.nReadyLength = tab.util.Util.ObjectToNumber(item.get("ready_time"), 0);
			summary.nType = tab.util.Util.ObjectToNumber(item.get("ntype"), 0);
			summary.nWorkLength = tab.util.Util.ObjectToNumber(item.get("active_time"), 0);
			summarys.add(summary);
		}
		return summarys;
	}

	// 租赁
	@POST
	@Path("/ReportAgentWorkSummary") // 坐席工作汇总
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ReportAgentWorkSummary(@Context HttpServletRequest R,
			@FormParam("starttime") RESTDateParam dtStarttime, @FormParam("endtime") RESTDateParam dtEndtime,
			@FormParam("type") Integer nType, @FormParam("agent") String sAgent, @FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				String sUId = Util.ObjectToString(httpsession.getAttribute("uid"));
				java.util.Set<String> agents = null;
				java.util.Set<String> groupList = RbacClient.getRoleGuidsForRole(sUId, null, null, true);
				if (groupList.contains(Util.ROOT_ROLEGUID) == bRbacTestFlag) {
					if (groupList.size() > 0) {
						agents = RbacClient.getUserAgents(sUId, null, true);
					} else {
						agents = new java.util.HashSet<String>();
					}
					java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(
							RbacClient.getUserAuths(sUId, sUId), new TypeToken<java.util.Map<String, Object>>() {
							}.getType());
					if (agentinfo != null) {
						String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
						if (agentNo.length() > 0)
							agents.add(agentNo);
					}
				}
				if (agents != null && agents.size() == 0) {
					result.put("list", null);
					result.put("totalCount", 0);
					result.put("success", true);
				} else {
					String sSqlStr = "select EXTRACT(YEAR FROM to_date('19700101','YYYYMMDD')+(1/24/60/60)*A.date_time_key) as nYear,"
							+ "EXTRACT(MONTH FROM to_date('19700101','YYYYMMDD')+(1/24/60/60)*A.date_time_key) as nMonth,"
							+ "EXTRACT(DAY FROM to_date('19700101','YYYYMMDD')+(1/24/60/60)*A.date_time_key) as nDay,1 as nType,";
					String sSelect = "MAX(B.RESOURCE_NAME) as RESOURCE_NAME,SUM(ACTIVE_TIME) as ACTIVE_TIME,"
							+ "SUM(READY) as READYCOUNT,SUM(READY_TIME) as READY_TIME,"
							+ "SUM(NOT_READY) as NOTREADYCOUNT,SUM(NOT_READY_TIME) as NOT_READY_TIME,"
							+ "SUM(BUSY) as BUSYCOUNT,SUM(BUSY_TIME) as BUSY_TIME"
							+ " from AGT_I_SESS_STATE_DAY A left JOIN RESOURCE_  B ON A.RESOURCE_KEY = B.RESOURCE_KEY";
					String sGroupby = " group by EXTRACT(YEAR FROM to_date('19700101','YYYYMMDD')+(1/24/60/60)*A.date_time_key),"
							+ "EXTRACT(MONTH FROM to_date('19700101','YYYYMMDD')+(1/24/60/60)*A.date_time_key),"
							+ "EXTRACT(DAY FROM to_date('19700101','YYYYMMDD')+(1/24/60/60)*A.date_time_key),B.RESOURCE_NAME";
					String sWhere = " where A.date_time_key>=:start and A.date_time_key<=:end";
					if (GHibernateSessionFactory.databaseType == "SQLServer") {
						sSqlStr = "select DATEPART(year, DATEADD(S,A.date_time_key,'1970-01-01 00:00:00')) as nYear,"
								+ "DATEPART(month, DATEADD(S,A.date_time_key,'1970-01-01 00:00:00')) as nMonth,"
								+ "DATEPART(day, DATEADD(S,A.date_time_key,'1970-01-01 00:00:00')) as nDay,1 as nType,";

						sGroupby = " group by DATEPART(year, DATEADD(S,A.date_time_key,'1970-01-01 00:00:00')),"
								+ "DATEPART(month, DATEADD(S,A.date_time_key,'1970-01-01 00:00:00')),"
								+ "DATEPART(day, DATEADD(S,A.date_time_key,'1970-01-01 00:00:00')),B.RESOURCE_NAME";

					}

					if (sAgent != null && sAgent.length() > 0) {
						sWhere += " and B.RESOURCE_NAME=:agent";
					}
					if (agents != null && agents.size() > 0) {
						sWhere += " and B.RESOURCE_NAME in(:agents)";
					}
					switch (nType) {
					default:
					case 0:// hour
					case 1:// day
						break;
					case 2:// week
							// DATEPART(week , A.start_time)
						sSqlStr = "select EXTRACT(YEAR FROM to_date('19700101','YYYYMMDD')+(1/24/60/60)*A.date_time_key) as nYear,"
								+ "TO_CHAR(to_date('19700101','YYYYMMDD')+(1/24/60/60)*A.date_time_key,'WW') as nDayOfWeek,2 as nType,";
						sGroupby = " group by EXTRACT(YEAR FROM to_date('19700101','YYYYMMDD')+(1/24/60/60)*A.date_time_key),"
								+ "TO_CHAR(to_date('19700101','YYYYMMDD')+(1/24/60/60)*A.date_time_key,'WW'),B.RESOURCE_NAME";
						sWhere = " where A.date_time_key>=:start and A.date_time_key<=:end";

						if (GHibernateSessionFactory.databaseType == "SQLServer") {

							sSqlStr = "select DATEPART(year, DATEADD(S,A.date_time_key,'1970-01-01 00:00:00')) as nYear,"
									+ "DATEPART(week, DATEADD(S,A.date_time_key,'1970-01-01 00:00:00')) as nDayOfWeek,2 as nType,";
							sGroupby = " group by DATEPART(year, DATEADD(S,A.date_time_key,'1970-01-01 00:00:00')),"
									+ "DATEPART(week, DATEADD(S,A.date_time_key,'1970-01-01 00:00:00')),B.RESOURCE_NAME";
						}
						if (sAgent != null && sAgent.length() > 0) {
							sWhere += " and B.RESOURCE_NAME=:agent";
						}
						if (agents != null && agents.size() > 0) {
							sWhere += " and B.RESOURCE_NAME in(:agents)";
						}
						break;
					case 3:// month
						sSqlStr = "select EXTRACT(YEAR FROM to_date('19700101','YYYYMMDD')+(1/24/60/60)*A.date_time_key) as nYear,"
								+ "EXTRACT(MONTH FROM to_date('19700101','YYYYMMDD')+(1/24/60/60)*A.date_time_key) as nMonth,3 as nType,";
						sGroupby = " group by EXTRACT(YEAR FROM to_date('19700101','YYYYMMDD')+(1/24/60/60)*A.date_time_key),"
								+ "EXTRACT(MONTH FROM to_date('19700101','YYYYMMDD')+(1/24/60/60)*A.date_time_key),B.RESOURCE_NAME";
						if (GHibernateSessionFactory.databaseType == "SQLServer") {
							sSqlStr = "select DATEPART(year, DATEADD(S,A.date_time_key,'1970-01-01 00:00:00')) as nYear,"
									+ "DATEPART(month, DATEADD(S,A.date_time_key,'1970-01-01 00:00:00')) as nMonth,3 as nType,";
							sGroupby = " group by DATEPART(year, DATEADD(S,A.date_time_key,'1970-01-01 00:00:00')),"
									+ "DATEPART(month, DATEADD(S,A.date_time_key,'1970-01-01 00:00:00')),B.RESOURCE_NAME";
						}

						sWhere = " where A.date_time_key>=:start and A.date_time_key<=:end";
						if (sAgent != null && sAgent.length() > 0) {
							sWhere += " and B.RESOURCE_NAME=:agent";
						}
						if (agents != null && agents.size() > 0) {
							sWhere += " and B.RESOURCE_NAME in(:agents)";
						}
						break;
					}
					sSqlStr = sSqlStr + sSelect + sWhere + sGroupby;
					// sSqlStr = sSqlStr + sSelect + sWhere;
					log.info(sSqlStr);
					SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
					query.setParameter("start", dtStarttime.getDate().getTime() / 1000).setParameter("end",
							dtEndtime.getDate().getTime() / 1000);
					if (sAgent != null && sAgent.length() > 0) {
						query.setString("agent", sAgent);
					}
					if (agents != null && agents.size() > 0) {
						query.setParameterList("agents", agents);
					}
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					@SuppressWarnings("unchecked")
					java.util.List<AgentWorkSummary> recordList = MapToAgentWorkSummary(query.list());
					java.util.Map<String, String> usernames = new java.util.HashMap<String, String>();
					for (int idx = 0; recordList != null && idx < recordList.size(); idx++) {
						AgentWorkSummary item = recordList.get(idx);
						String sAgentNo = tab.util.Util.ObjectToString(item.agent);
						if (sAgentNo.length() > 0) {
							usernames.put(sAgentNo, "");
						}
					}
					if (usernames.size() > 0) {
						try {
							Session dbsession = HibernateSessionFactory.getThreadSession();
							try {
								@SuppressWarnings("unchecked")
								java.util.List<Object[]> agentNos = dbsession.createSQLQuery(
										"select agent,name from res_users A,res_partner B where A.partner_id=B.id and A.agent in(:agents)")
										.setParameterList("agents", usernames.keySet()).list();
								for (int idx = 0; idx < agentNos.size(); idx++) {
									Object[] agentUsers = (Object[]) agentNos.get(idx);
									usernames.put(agentUsers[0].toString(),
											tab.util.Util.ObjectToString(agentUsers[1]));
								}
								dbsession.close();
							} catch (Throwable e) {
								log.warn("ERROR:", e);
								result.put("msg", e.toString());
							}
						} catch (Throwable e) {
							log.warn("ERROR:", e);
							result.put("msg", e.toString());
						}
					}
					for (int idx = 0; recordList != null && idx < recordList.size(); idx++) {
						AgentWorkSummary item = recordList.get(idx);
						item.username = usernames.get(item.agent);
					}
					if (sScript == null || sScript.length() == 0) {
						result.put("list", recordList);
						result.put("totalCount", recordList.size());
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
						for (int i = 0; i < recordList.size(); i++) {
							AgentWorkSummary call = recordList.get(i);
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
											"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
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
	@Path("/ReportDetailSummary")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ReportDetailSummary(@Context HttpServletRequest R,
			@FormParam("starttime") RESTDateParam dtStarttime, @FormParam("endtime") RESTDateParam dtEndtime,
			@FormParam("type") Integer nType, @FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		String sWhere = " A.ringtime>=:starttime and A.ringtime<=:endtime" + FILTER_DETAIL_DNIS_SQL;
		java.util.Set<String> groupList = RbacClient
				.getRoleGuidsForRole(Util.ObjectToString(httpsession.getAttribute("uid")), null, null, true);
		if (groupList.size() > 0 && groupList.contains(Util.ROOT_ROLEGUID)) {
			groupList.clear();
		} else if (groupList.size() > 0) {
			// 组列表直接获取工号列表
			sWhere += FILTER_RBAC_GROUP_DETAIL_LIST_SQL;
		} else {
			sWhere += " and 1=0";
		}
		// 0 IVR呼出拒绝 1 IVR呼出失败 2 IVR呼出成功
		// 3 未使用 4 IVR呼入 5 呼入接听，坐席挂机 6 呼入接听外线挂机
		// 8 呼入未接 11 呼入转接应答,外线挂机 12 呼入放弃
		// 13 呼入转接应答,坐席挂机 14 呼入转接没应答,如果没取到工号的13暂时放到14里(2016-11-4继续观察)

		// 7 呼出接听，坐席挂机 9 呼出未接通 10 呼出接听，外线挂机
		String sSelect = "select new Map(SUM(case when (A.type=7 or A.type=10) then 1 else 0 end) as nOutboundCount,"
				+ "SUM(case when (A.type=7 or A.type=10) then A.length end) as nOutboundLength,"
				+ "SUM(case when (A.type=7 or A.type=10) then A.wait end) as nOutboundWait,"
				+ "SUM(case when A.type=9 then 1 else 0 end) as nNoConnectCount,"
				+ "SUM(case when A.type=9 then A.wait end) as nNoConnectWait,"
				+ "SUM(case when (A.type=5 or A.type=6 or A.type=13 or A.type=11) then 1 else 0 end) as nInboundCount,"
				+ "SUM(case when (A.type=5 or A.type=6 or A.type=13 or A.type=11) then A.length end) as nInboundLength,"
				+ "SUM(case when (A.type=5 or A.type=6 or A.type=13 or A.type=11) then A.wait end) as nInboundWait,"
				+ "SUM(case when (A.type=8 or A.type=12 or A.type=14) then 1 else 0 end) as nNoAnswerCount,"
				+ "SUM(case when (A.type=8 or A.type=12 or A.type=14) then A.wait end) as nNoAnswerWait,"
				+ "MAX(case when (A.type=7 or A.type=10) then A.length end) as nMaxConnectLength,"
				+ "MAX(case when (A.type=5 or A.type=6 or A.type=13 or A.type=11) then A.length end) as nMaxAnswerLength,"
				+ "SUM(case when ( (A.type=7 or A.type=9 or A.type=10) and A.wait<=5) then 1 else 0 end) as nOutboundWaitLessCount,"
				// 呼入接通数>= -- (通话与振铃总时长 > 最小时长 ) 的呼入电话总数
				+ "SUM(case when ( (A.type=5 or A.type=6 or A.type=13 or A.type=8 or A.type=12 or A.type=14 or A.type=11) and A.wait<=5) then 1 else 0 end) as nInboundWaitLessCount,"
				// 小于呼出应答标准的呼出无人应答数
				+ "SUM(case when ( A.type=9 and length(A.called)>4 and A.wait<=5) then 1 else 0 end) as nOutboundAbandonWaitLessCount,"
				// 小于应答标准的呼入失败次数
				+ "SUM(case when ( (A.type=8 or A.type=12 or A.type=14) and A.wait<=5) then 1 else 0 end) as nInboundAbandonWaitLessCount";
		String sGroupBy = " group by A.workshifts,A.agent,YEAR(A.ringtime),MONTH(A.ringtime),DAY(A.ringtime),HOUR(A.ringtime)";
		String sOrderBy = " order by A.workshifts,A.agent,nYear,nMonth,nDay,nHour";
		switch (nType) {
		default:
		case 0:// hour
			sSelect += ",A.workshifts as nWorkShifts, YEAR(A.ringtime) as nYear,MONTH(A.ringtime) as nMonth,DAY(A.ringtime) as nDay,HOUR(A.ringtime) as nHour,A.agent as agent, 0 as nType)";
			break;
		case 1:// day
			sSelect += ",A.workshifts as nWorkShifts, YEAR(A.ringtime) as nYear,MONTH(A.ringtime) as nMonth,DAY(A.ringtime) as nDay,A.agent as agent, 1 as nType)";
			sGroupBy = " group by A.workshifts,A.agent,YEAR(A.ringtime),MONTH(A.ringtime),DAY(A.ringtime)";
			sOrderBy = " order by A.workshifts,A.agent,nYear,nMonth,nDay";
			break;
		case 2:// week
			sSelect += ",A.workshifts as nWorkShifts, YEAR(A.ringtime) as nYear,week(A.ringtime) as nWeek,A.agent as agent, 2 as nType)";
			sGroupBy = " group by A.workshifts,A.agent,YEAR(A.ringtime),week(A.ringtime)";
			sOrderBy = " order by A.workshifts,A.agent,nYear,nWeek";
			break;
		case 3:// month
			sSelect += ",A.workshifts as nWorkShifts, YEAR(A.ringtime) as nYear,MONTH(A.ringtime) as nMonth,A.agent as agent, 3 as nType)";
			sGroupBy = " group by A.workshifts,A.agent,YEAR(A.ringtime),MONTH(A.ringtime)";
			sOrderBy = " order by A.workshifts,A.agent,nYear,nMonth";
			break;
		}
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				log.info(sSelect + " from Calldetailrecord A where " + sWhere + sGroupBy + sOrderBy);
				Query query = dbsession
						.createQuery(sSelect + " from Calldetailrecord A where " + sWhere + sGroupBy + sOrderBy)
						.setTimestamp("starttime", dtStarttime.getDate()).setTimestamp("endtime", dtEndtime.getDate());
				if (groupList.size() > 0) {
					query.setParameterList("grouplist", groupList);
				}
				@SuppressWarnings("unchecked")
				java.util.List<java.util.Map<String, Object>> calldetailrecordList = query.list();
				java.util.Set<String> AgentList = new java.util.HashSet<>();
				for (int i = 0; i < calldetailrecordList.size(); i++) {
					AgentList.add(Util.ObjectToString(calldetailrecordList.get(i).get("agent")));
				}
				java.util.Map<String, String> AgentMap = new java.util.HashMap<String, String>();
				if (AgentList.size() > 0) {
					@SuppressWarnings("unchecked")
					java.util.List<Object[]> maps = (java.util.List<Object[]>) dbsession.createQuery(
							"select agent as agent, username as username from Rbacuserauths where agent in(:agent)"
									+ "and length(agent) > 0")
							.setParameterList("agent", AgentList).list();
					for (Object[] map : maps) {
						AgentMap.put(map[0].toString(), map[1].toString());
					}
				}
				tab.MyExportExcelFile export = null;

				if (sScript == null || sScript.length() == 0) {

				} else {
					export = new tab.MyExportExcelFile();
					if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator") + "WebRoot"
							+ sScript)) {
						dbsession.close();
						return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain").build();
					}
				}

				if (sScript == null || sScript.length() == 0) {
					for (int i = 0; i < calldetailrecordList.size(); i++) {
						java.util.Map<String, Object> call = calldetailrecordList.get(i);
						call.put("username", AgentMap.get(call.get("agent")));
					}
				} else {
					for (int i = 0; i < calldetailrecordList.size(); i++) {
						java.util.HashMap<String, Object> call = (HashMap<String, Object>) calldetailrecordList.get(i);
						call.put("username", AgentMap.get(call.get("agent")));
						export.CommitRow(call);
					}
				}
				if (sScript == null || sScript.length() == 0) {
					result.put("list", calldetailrecordList);
					result.put("success", true);
				} else {
					String sFileName = export.GetFile();
					if (sFileName != null && sFileName.length() > 0) {
						java.io.File f = new java.io.File(sFileName);
						int pos = sFileName.lastIndexOf(".");
						dbsession.close();
						return Response.ok(f)
								.header("Content-Disposition",
										"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
												+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
														.format(Calendar.getInstance().getTime())
														.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
												+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
								.build();
					}
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		if (sScript == null || sScript.length() == 0) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(404).entity("no datas").type("text/plain").build();
	}

	// 租赁
	@POST
	@Path("/ReportIvrCall") // 呼入队列明细
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response ReportIvrCall(@Context HttpServletRequest R, @FormParam("starttime") RESTDateParam dtStarttime,
			@FormParam("endtime") RESTDateParam dtEndtime, @FormParam("caller") String sCaller,
			@FormParam("agent") String sAgent, @FormParam("type") Integer nType,
			@FormParam("department") String sDepartment, @FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit, @FormParam("sort") EXTJSSortParam Sort,
			@FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				String sSelect = "select A.queues as vdn, (A.start_time-A.mediation_duration/24/60/60-A.routing_point_duration/24/60/60) as ringtime, A.start_time as begintime, A.end_time as endtime,A.resource_name as channel, "
						+ " A.source_address as ani,A.target_address as dnis,A.employee_id as agent,A.talk_duration as length, A.routing_point_duration as wait,A.mediation_duration as queuetime,"
						+ " (case when (A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0) then 'IVR' else A.technical_result end) as type";
				String sWhere = " from call_detial A where not(A.queues is null) and A.start_time>=:start and A.start_time<=:end";

				if (sCaller != null && sCaller.length() > 0) {
					sWhere = sWhere + " and (A.source_address like :caller or A.target_address like :caller)";
				}
				if (sAgent != null && sAgent.length() > 0) {
					sWhere += " and A.employee_id=:agent";
				}
				switch (nType) {
				default:
				case 0:// All
					break;
				case 1:// Agent
					sWhere += " and (A.technical_result in('Completed'))";
					break;
				case 2:// Queue
					sWhere += " and (A.technical_result in('CustomerAbandoned','Abandoned','Redirected')) and not (A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0)";
					break;
				case 3:// IVR
					sWhere += " and (A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0)";
					break;
				}
				switch (sDepartment) {
				case "consulting":
					sWhere += " and A.queues='consulting'";
					break;
				case "change":
					sWhere += " and A.queues='change'";
					break;
				case "complaints":
					sWhere += " and A.queues='complaints'";
					break;
				case "other":
					sWhere += " and A.queues='other'";
					break;
				case "consulting_jrsyb":
					sWhere += " and A.queues='consulting_jrsyb'";
					break;
				case "change_jrsyb":
					sWhere += " and A.queues='change_jrsyb'";
					break;
				case "complaints_jrsyb":
					sWhere += " and A.queues='complaints_jrsyb'";
					break;
				case "other_jrsyb":
					sWhere += " and A.queues='other_jrsyb'";
					break;
				}
				String sSqlStr = sWhere;
				log.info(sSqlStr);
				// SQLQuery query = gdbsession.createSQLQuery("select count(*) from
				// call_detial");
				SQLQuery query = gdbsession.createSQLQuery("select count(*) " + sSqlStr);
				query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
				if (sCaller != null && sCaller.length() > 0) {
					query.setString("caller", "%" + sCaller + "%");
				}
				if (sAgent != null && sAgent.length() > 0) {
					query.setString("agent", sAgent);
				}
				Integer nTotalCount = tab.util.Util
						.ObjectToNumber(query.setFirstResult(0).setMaxResults(1).uniqueResult(), 0);
				if (nTotalCount > 0) {
					sSqlStr = sSelect + sWhere;
					String sProperty = StringUtils.EMPTY;
					if (Sort != null) {
						switch (Sort.getsProperty()) {
						case "channel":
							sProperty = "A.resource_name";
							break;
						case "begintime":
							sProperty = "A.start_time";
							break;
						case "ani":
							sProperty = "A.source_address";
							break;
						case "dnis":
							sProperty = "A.target_address";
							break;
						case "endtime":
							sProperty = "A.end_time";
							break;
						case "ringtime":
							sProperty = "(A.start_time-A.mediation_duration/24/60/60)";
							break;
						case "type":
							sProperty = "A.technical_result";
							break;
						case "queuetime":
							sProperty = "A.mediation_duration";
							break;
						case "wait":
							sProperty = "A.ring_duration";
							break;
						default:
							sProperty = "A.start_time";
							break;
						}
					}
					if (!sProperty.isEmpty()) {
						sSqlStr += " order by " + sProperty + " " + Sort.getsDirection();
					}
					log.debug(sSqlStr);
					query = gdbsession.createSQLQuery(sSqlStr);
					query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
					if (sCaller != null && sCaller.length() > 0) {
						query.setString("caller", "%" + sCaller + "%");
					}
					if (sAgent != null && sAgent.length() > 0) {
						query.setString("agent", sAgent);
					}
					if (sScript == null || sScript.length() < 0) {
						query.setFirstResult(pageStart).setMaxResults(pageLimit);
					}
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);

					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					if (sScript == null || sScript.length() == 0) {
						result.put("list", CallRecordList);
						result.put("totalCount", nTotalCount);
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
											"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				} else {
					if (sScript == null || sScript.length() == 0) {
						result.put("list", new java.util.ArrayList<java.util.Map<String, Object>>());
						result.put("totalCount", 0);
						result.put("success", true);
					}
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
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
	@Path("/AddInvestigateResult")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response AddInvestigateResult(@Context HttpServletRequest R,
			@FormParam("records") RESTListMapParam recordsParam, @QueryParam("agent") String sAgent,
			@QueryParam("digit") String sDigit) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(500).entity(result).build();
	}

	@POST
	@Path("/AddVoiceMail")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response AddVoiceMail(@Context HttpServletRequest R, @FormParam("records") RESTListMapParam recordsParam) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(500).entity(result).build();
	}

	@POST
	@Path("/GetAgentData")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response GetAgentData(@Context HttpServletRequest R, @FormParam("starttime") RESTDateParam dtStarttime,
			@FormParam("endtime") RESTDateParam dtEndtime, @FormParam("agent") String sAgent,
			@FormParam("extension") String sExtension, @FormParam("inbound") Integer nInbound,
			@DefaultValue("0") @FormParam("inside") Integer nInside, @FormParam("length") Integer nLength,
			@FormParam("groupguid") String sGroupguid, @FormParam("caller") String sCaller,
			@FormParam("start") Integer pageStart, @FormParam("limit") Integer pageLimit) {
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		if (nInbound == null)
			nInbound = 0;
		if (nLength == null)
			nLength = 0;
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	// 租赁
	@POST
	@Path("/ReportAgentDetail") // 坐席通话明细
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response ReportAgentDetail(@Context HttpServletRequest R, @FormParam("starttime") RESTDateParam dtStarttime,
			@FormParam("endtime") RESTDateParam dtEndtime, @FormParam("agent") String sAgent,
			@FormParam("extension") String sExtension, @FormParam("caller") String sCaller,
			@FormParam("inbound") Integer nInbound, @FormParam("nType") Integer nType,
			@FormParam("weekend") Integer nWeekend, @FormParam("length") Integer nLength,
			@FormParam("groupguid") String sGroupguid, @FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit, @FormParam("sort") EXTJSSortParam Sort,
			@FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		if (nInbound == null)
			nInbound = 0;
		if (nWeekend == null)
			nWeekend = 0;
		if (nLength == null)
			nLength = 0;
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				String sUId = tab.util.Util.ObjectToString(httpsession.getAttribute("uid"));
				java.util.Set<String> agents = null;
				java.util.Set<String> groupList = RbacClient.getRoleGuidsForRole(sUId, sGroupguid, null, false);
				if (groupList.contains(Util.ROOT_ROLEGUID) == bRbacTestFlag) {
					if (groupList.size() > 0) {
						agents = RbacClient.getUserAgents(sUId, sGroupguid, true);
					} else {
						agents = new java.util.HashSet<String>();
					}
					java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(
							RbacClient.getUserAuths(sUId, sUId), new TypeToken<java.util.Map<String, Object>>() {
							}.getType());
					if (agentinfo != null) {
						String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
						if (agentNo.length() > 0)
							agents.add(agentNo);
					}
				}
				if (agents != null && agents.size() == 0) {
					result.put("list", null);
					result.put("totalCount", 0);
				} else {
					/*
					 * private Long callagentrecordId; private int callrefid; private int ipaddress;
					 * private Date ringtime; private Date begintime; private Date endtime; private
					 * int channel; private String ani; private String dnis; private String agent;
					 * private int wait; private int length; private int type; private int transfer;
					 * private Integer conference; private Integer hold; private Integer
					 * transferring; private Integer host; private Integer dropStatus; private
					 * String vdn; private String skill; private int split; private String ucid;
					 * private String data; private String trunk; private Integer workshifts;
					 * 
					 * @Formula("(select max(t.username) from rbacuserauths t where t.agent = agent and length(t.agent)>0)"
					 * ) public String username;
					 * 
					 * {name:"userAgent",mapping:"user.userAgent"},
					 * {name:"username",mapping:"user.userName"},
					 */
					String sSelect = "select A.start_time as ringtime, case when A.answer_time is null then A.start_time else A.answer_time end as begintime, A.end_time as endtime,A.resource_name as channel, "
							+ " A.source_address as ani,A.target_address as dnis,A.employee_id as agent,A.talk_duration as length, A.ring_duration as wait,"
							+ " (case when A.talk_duration>0 then (case when upper(A."
							+ RecSystem.sInteraction_type_name + ")='OUTBOUND'"
							+ "  then 2 else 1 end) else (case when upper(A." + RecSystem.sInteraction_type_name
							+ ")='OUTBOUND'" + "  then 4 else 3 end) end) as type," + " A."
							+ RecSystem.sInteraction_type_name;
					String sWhere = " from call_detial A where A.party_name in ('Extension', 'Agent') and A.start_time>=:start and A.start_time<=:end";

					if (sCaller != null && sCaller.length() > 0) {
						sWhere = sWhere + " and (A.source_address like :caller or A.target_address like :caller)";
					}
					if (sExtension != null && sExtension.length() > 0) {
						sWhere = sWhere + " and A.resource_name=:extension";
					}
					if (sAgent != null && sAgent.length() > 0) {
						if (tab.util.Util.NONE_GUID.equals(sAgent)) {

						} else if (sAgent
								.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}") == false) {
							sWhere = sWhere + " and A.employee_id=:agent";
						} else {
							java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(
									RbacClient.getUserAuths(sUId, sAgent),
									new TypeToken<java.util.Map<String, Object>>() {
									}.getType());
							if (agentinfo != null) {
								String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
								if (agentNo.length() > 0) {
									sWhere = sWhere + " and A.employee_id=:agent";
								}
							}
						}
					}
					switch (nInbound) {
					case 1:// 呼入
						sWhere += " and  (upper(A." + RecSystem.sInteraction_type_name + ")='INBOUND' or upper(A."
								+ RecSystem.sInteraction_type_name + ")='INTERNAL')";
						break;
					case 2:// 呼出
						sWhere += " and  upper(A." + RecSystem.sInteraction_type_name + ")='OUTBOUND'";
						break;
					default:
						break;
					}
					switch (tab.util.Util.ObjectToNumber(nType, 0)) {
					case 1:// 呼入接听
						sWhere += " and A.talk_duration>0 and (upper(A." + RecSystem.sInteraction_type_name
								+ ")='INBOUND' or upper(A." + RecSystem.sInteraction_type_name + ")='INTERNAL')";
						break;
					case 2:// 呼出接听
						sWhere += " and  A.talk_duration>0 and upper(A." + RecSystem.sInteraction_type_name
								+ ")='OUTBOUND'";
						break;
					case 3:// 呼入未接
						sWhere += " and  A.talk_duration=0 and (upper(A." + RecSystem.sInteraction_type_name
								+ ")='INBOUND' or upper(A." + RecSystem.sInteraction_type_name + ")='INTERNAL')";
						break;
					case 4:// 呼出未接
						sWhere += " and  A.talk_duration=0 and upper(A." + RecSystem.sInteraction_type_name
								+ ")='OUTBOUND'";
						break;
					default:
						break;
					}
					// switch (nWeekend) {
					// case 1:// 无周日
					// sWhere += " and TO_CHAR(A.start_time,'D')!=1";
					// break;
					// case 2:// 无周六和周日
					// sWhere += " and TO_CHAR(A.start_time,'D') in (2,3,4,5,6)";
					// break;
					// }
					String sSqlStr = sWhere;
					if (agents != null && agents.size() > 0) {
						sSqlStr += " and A.employee_id in(:agents)";
						log.info(GsonUtil.getInstance().toJson(agents));
					}
					log.info("start: " + dtStarttime.toString() + ", end:" + dtEndtime.toString());
					log.info(sSqlStr);
					SQLQuery query = gdbsession.createSQLQuery("select count(*) " + sSqlStr);
					query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
					if (sCaller != null && sCaller.length() > 0) {
						query.setString("caller", "%" + sCaller + "%");
					}
					if (sExtension != null && sExtension.length() > 0) {
						query.setString("extension", sExtension);
					}
					if (sAgent != null && sAgent.length() > 0) {
						if (tab.util.Util.NONE_GUID.equals(sAgent)) {

						} else if (sAgent
								.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}") == false) {
							query.setString("agent", sAgent);
						} else {
							java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(
									RbacClient.getUserAuths(sUId, sAgent),
									new TypeToken<java.util.Map<String, Object>>() {
									}.getType());
							if (agentinfo != null) {
								String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
								if (agentNo.length() > 0) {
									query.setString("agent", agentNo);
								}
							}
						}
					}
					if (agents != null && agents.size() > 0) {
						query.setParameterList("agents", agents);
					}
					Integer nTotalCount = tab.util.Util
							.ObjectToNumber(query.setFirstResult(0).setMaxResults(1).uniqueResult(), 0);
					if (nTotalCount > 0) {
						sSqlStr = sSelect + sWhere;
						if (agents != null && agents.size() > 0) {
							sSqlStr += " and A.employee_id in(:agents)";
						}
						String sProperty = StringUtils.EMPTY;
						if (Sort != null) {
							switch (Sort.getsProperty()) {
							default:
							case "ringtime":
								sProperty = "A.start_time";
								break;
							case "begintime":
								sProperty = "A.answer_time";
								break;
							case "ani":
								sProperty = "A.source_address";
								break;
							case "dnis":
								sProperty = "A.target_address";
								break;
							case "endtime":
								sProperty = "A.end_time";
								break;
							case "type":
								sProperty = "A." + RecSystem.sInteraction_type_name;
								break;
							case "channel":
								sProperty = "A.resource_name";
								break;
							case "username":
							case "agent":
								sProperty = "A.employee_id";
								break;
							case "wait":
								sProperty = "A.ring_duration";
								break;
							case "length":
								sProperty = "A.talk_duration";
								break;
							}
						}
						if (!sProperty.isEmpty()) {
							sSqlStr += " order by " + sProperty + " " + Sort.getsDirection();
						}
						log.info(sSqlStr);
						query = gdbsession.createSQLQuery(sSqlStr);
						query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
						if (sCaller != null && sCaller.length() > 0) {
							query.setString("caller", "%" + sCaller + "%");
						}
						if (sExtension != null && sExtension.length() > 0) {
							query.setString("extension", sExtension);
						}
						if (sAgent != null && sAgent.length() > 0) {
							if (tab.util.Util.NONE_GUID.equals(sAgent)) {

							} else if (sAgent
									.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}") == false) {
								query.setString("agent", sAgent);
							} else {
								java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(
										RbacClient.getUserAuths(sUId, sAgent),
										new TypeToken<java.util.Map<String, Object>>() {
										}.getType());
								if (agentinfo != null) {
									String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
									if (agentNo.length() > 0) {
										query.setString("agent", agentNo);
									}
								}
							}
						}
						if (sScript == null || sScript.length() < 0) {
							query.setFirstResult(pageStart).setMaxResults(pageLimit);
						}
						if (agents != null && agents.size() > 0) {
							query.setParameterList("agents", agents)
									.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
						} else {
							query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
						}

						@SuppressWarnings("unchecked")
						java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
						java.util.Map<String, String> usernames = new java.util.HashMap<String, String>();
						for (int idx = 0; CallRecordList != null && idx < CallRecordList.size(); idx++) {
							java.util.Map<String, Object> item = CallRecordList.get(idx);
							String sAgentNo = tab.util.Util.ObjectToString(item.get("agent"));
							if (sAgentNo.length() > 0) {
								usernames.put(sAgentNo, "");
							}
						}
						if (usernames.size() > 0) {
							try {
								Session dbsession = HibernateSessionFactory.getThreadSession();
								try {
									@SuppressWarnings("unchecked")
									java.util.List<Object[]> agentNos = dbsession.createSQLQuery(
											"select agent,name from res_users A,res_partner B where A.partner_id=B.id and agent in(:agents)")
											.setParameterList("agents", usernames.keySet()).list();
									for (int idx = 0; idx < agentNos.size(); idx++) {
										Object[] agentUsers = (Object[]) agentNos.get(idx);
										usernames.put(agentUsers[0].toString(),
												tab.util.Util.ObjectToString(agentUsers[1]));
									}
									dbsession.close();
								} catch (Throwable e) {
									log.warn("ERROR:", e);
									result.put("msg", e.toString());
								}
							} catch (Throwable e) {
								log.warn("ERROR:", e);
								result.put("msg", e.toString());
							}
						}
						for (int idx = 0; CallRecordList != null && idx < CallRecordList.size(); idx++) {
							java.util.Map<String, Object> item = CallRecordList.get(idx);
							item.put("username", usernames.get(item.get("agent")));
						}

						if (sScript == null || sScript.length() == 0) {
							result.put("list", CallRecordList);
							result.put("totalCount", nTotalCount);
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
														+ URLEncoder.encode(export.GetFileName(), "utf-8")
														+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
																.format(Calendar.getInstance().getTime())
																.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
														+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
										.build();
							}
						}
					} else {
						if (sScript == null || sScript.length() == 0) {
							result.put("list", null);
							result.put("totalCount", 0);
							result.put("success", true);
						}
					}
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
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

	@SuppressWarnings("unused")
	private static class AgentDetailSummaryInfo {
		public Integer nOutboundCount;
		public Integer nOutboundLength;
		public Integer nOutboundWait;
		public Integer nNoConnectCount;
		public Integer nNoConnectWait;
		public Integer nInboundCount;
		public Integer nInboundLength;
		public Integer nInboundWait;
		public Integer nNoAnswerCount;
		public Integer nNoAnswerWait;
		public Integer nMaxConnectLength;
		public Integer nMaxAnswerLength;
		public Integer nYear;
		public Integer nWeek;
		public Integer nMonth;
		public Integer nDay;
		public Integer nHour;
		public Integer nType;
		public String agent;
		public String username;
	}

	private static java.util.List<AgentDetailSummaryInfo> MapToAgentDetailSummaryInfo(
			java.util.List<java.util.Map<String, Object>> items) {
		java.util.List<AgentDetailSummaryInfo> summarys = new java.util.ArrayList<AgentDetailSummaryInfo>();
		for (int i = 0; i < items.size(); i++) {
			java.util.Map<String, Object> item = items.get(i);
			AgentDetailSummaryInfo summary = new AgentDetailSummaryInfo();
			summary.nOutboundCount = tab.util.Util.ObjectToNumber(item.get("noutboundcount"), 0);
			summary.nOutboundLength = tab.util.Util.ObjectToNumber(item.get("noutboundlength"), 0);
			summary.nOutboundWait = tab.util.Util.ObjectToNumber(item.get("noutboundwait"), 0);
			summary.nNoConnectCount = tab.util.Util.ObjectToNumber(item.get("nnoconnectcount"), 0);
			summary.nNoConnectWait = tab.util.Util.ObjectToNumber(item.get("nnoconnectwait"), 0);
			summary.nInboundCount = tab.util.Util.ObjectToNumber(item.get("ninboundcount"), 0);
			summary.nInboundLength = tab.util.Util.ObjectToNumber(item.get("ninboundlength"), 0);
			summary.nInboundWait = tab.util.Util.ObjectToNumber(item.get("ninboundwait"), 0);
			summary.nNoAnswerCount = tab.util.Util.ObjectToNumber(item.get("nnoanswercount"), 0);
			summary.nNoAnswerWait = tab.util.Util.ObjectToNumber(item.get("nnoanswerwait"), 0);
			summary.nMaxConnectLength = tab.util.Util.ObjectToNumber(item.get("nmaxconnectlength"), 0);
			summary.nMaxAnswerLength = tab.util.Util.ObjectToNumber(item.get("nmaxanswerlength"), 0);
			summary.nYear = tab.util.Util.ObjectToNumber(item.get("nyear"), 0);
			summary.nWeek = tab.util.Util.ObjectToNumber(item.get("nweek"), 0);
			summary.nMonth = tab.util.Util.ObjectToNumber(item.get("nmonth"), 0);
			summary.nHour = tab.util.Util.ObjectToNumber(item.get("nhour"), 0);
			summary.nDay = tab.util.Util.ObjectToNumber(item.get("nday"), 0);
			summary.nHour = tab.util.Util.ObjectToNumber(item.get("nhour"), 0);
			summary.agent = tab.util.Util.ObjectToString(item.get("agent"));
			summary.nType = tab.util.Util.ObjectToNumber(item.get("ntype"), 0);
			summarys.add(summary);
		}
		return summarys;
	}

	// 租赁
	@POST
	@Path("/ReportAgentDetailSummary") // 坐席通话汇总
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ReportAgentDetailSummary(@Context HttpServletRequest R,
			@FormParam("starttime") RESTDateParam dtStarttime, @FormParam("endtime") RESTDateParam dtEndtime,
			@FormParam("type") Integer nType, @FormParam("agent") String sAgent, @FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				String sUId = Util.ObjectToString(httpsession.getAttribute("uid"));
				java.util.Set<String> agents = null;
				java.util.Set<String> groupList = RbacClient.getRoleGuidsForRole(sUId, null, null, true);
				if (groupList.contains(Util.ROOT_ROLEGUID) == bRbacTestFlag) {
					if (groupList.size() > 0) {
						agents = RbacClient.getUserAgents(sUId, null, true);
					} else {
						agents = new java.util.HashSet<String>();
					}
					java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(
							RbacClient.getUserAuths(sUId, sUId), new TypeToken<java.util.Map<String, Object>>() {
							}.getType());
					if (agentinfo != null) {
						String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
						if (agentNo.length() > 0)
							agents.add(agentNo);
					}
				}
				if (agents != null && agents.size() == 0) {
					result.put("list", null);
					result.put("totalCount", 0);
				} else {
					String sSelect = "select SUM(case when A.talk_duration>0 and upper(A."
							+ RecSystem.sInteraction_type_name + ")='OUTBOUND' then 1 else 0 end) as nOutboundCount,"
							+ "SUM(case when A.talk_duration>0 and upper(A." + RecSystem.sInteraction_type_name
							+ ")='OUTBOUND' then A.talk_duration end) as nOutboundLength,"
							+ "SUM(case when A.talk_duration>0 and upper(A." + RecSystem.sInteraction_type_name
							+ ")='OUTBOUND' then A.dial_duration end) as nOutboundWait,"
							+ "SUM(case when A.talk_duration=0 and upper(A." + RecSystem.sInteraction_type_name
							+ ")='OUTBOUND' then 1 else 0 end) as nNoConnectCount,"
							+ "SUM(case when A.talk_duration=0 and upper(A." + RecSystem.sInteraction_type_name
							+ ")='OUTBOUND' then A.dial_duration end) as nNoConnectWait,"
							+ "SUM(case when A.talk_duration>0 and upper(A." + RecSystem.sInteraction_type_name
							+ ")='INBOUND' or upper(A." + RecSystem.sInteraction_type_name
							+ ")='INTERNAL' then 1 else 0 end) as nInboundCount,"
							+ "SUM(case when A.talk_duration>0 and upper(A." + RecSystem.sInteraction_type_name
							+ ")='INBOUND' or upper(A." + RecSystem.sInteraction_type_name
							+ ")='INTERNAL' then A.talk_duration else 0 end) as nInboundLength,"
							+ "SUM(case when A.talk_duration>0 and upper(A." + RecSystem.sInteraction_type_name
							+ ")='INBOUND' or upper(A." + RecSystem.sInteraction_type_name
							+ ")='INTERNAL' then A.ring_duration else 0 end) as nInboundWait,"
							+ "SUM(case when A.talk_duration=0 and upper(A." + RecSystem.sInteraction_type_name
							+ ")='INBOUND' or upper(A." + RecSystem.sInteraction_type_name
							+ ")='INTERNAL' then 1 else 0 end) as nNoAnswerCount,"
							+ "SUM(case when A.talk_duration=0 and upper(A." + RecSystem.sInteraction_type_name
							+ ")='INBOUND' or upper(A." + RecSystem.sInteraction_type_name
							+ ")='INTERNAL' then A.ring_duration else 0 end) as nNoAnswerWait,"
							+ "MAX(case when upper(A." + RecSystem.sInteraction_type_name
							+ ")='OUTBOUND' then A.talk_duration end) as nMaxConnectLength," + "MAX(case when upper(A."
							+ RecSystem.sInteraction_type_name + ")='INBOUND' or upper(A."
							+ RecSystem.sInteraction_type_name
							+ ")='INTERNAL' then A.talk_duration end) as nMaxAnswerLength";
					String sGroupBy = " group by A.employee_id,EXTRACT(YEAR FROM A.start_time),EXTRACT(MONTH FROM A.start_time),EXTRACT(DAY FROM A.start_time),TO_CHAR(A.start_time,'hh24')";
					String sOrderBy = " order by A.employee_id,nYear,nMonth,nDay,nHour";
					if (GHibernateSessionFactory.databaseType == "SQLServer") {
						sGroupBy = " group by A.employee_id,DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time),DATEPART(hour,A.start_time)";
						switch (nType) {
						default:
						case 0:// hour
							sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time) as nDay,DATEPART(hour,A.start_time) as nHour,A.employee_id as agent, 0 as nType";
							break;
						case 1:// day
							sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time)  as nDay,A.employee_id as agent, 1 as nType";
							sGroupBy = " group by A.employee_id,DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time)";
							sOrderBy = " order by A.employee_id,nYear,nMonth,nDay";
							break;
						case 2:// week
							sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(week , A.start_time) as nWeek,A.employee_id as agent, 2 as nType";
							sGroupBy = " group by A.employee_id,DATEPART(year , A.start_time),DATEPART(week , A.start_time)";
							sOrderBy = " order by A.employee_id,nYear,nWeek";
							break;
						case 3:// month
							sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,A.employee_id as agent, 3 as nType";
							sGroupBy = " group by A.employee_id,DATEPART(year , A.start_time),DATEPART(month , A.start_time)";
							sOrderBy = " order by A.employee_id,nYear,nMonth";
							break;
						}
					} else {
						switch (nType) {
						default:
						case 0:// hour
							sSelect += ",EXTRACT(YEAR FROM A.start_time) as nYear,EXTRACT(MONTH FROM A.start_time) as nMonth,EXTRACT(DAY FROM A.start_time) as nDay,TO_CHAR(A.start_time,'hh24') as nHour,A.employee_id as agent, 0 as nType";
							break;
						case 1:// day
							sSelect += ",EXTRACT(YEAR FROM A.start_time) as nYear,EXTRACT(MONTH FROM A.start_time) as nMonth,EXTRACT(DAY FROM A.start_time) as nDay,A.employee_id as agent, 1 as nType";
							sGroupBy = " group by A.employee_id,EXTRACT(YEAR FROM A.start_time),EXTRACT(MONTH FROM A.start_time),EXTRACT(DAY FROM A.start_time)";
							sOrderBy = " order by A.employee_id,nYear,nMonth,nDay";
							break;
						case 2:// week
							sSelect += ",EXTRACT(YEAR FROM A.start_time) as nYear,TO_CHAR(A.start_time,'WW') as nWeek,A.employee_id as agent, 2 as nType";
							sGroupBy = " group by A.employee_id,EXTRACT(YEAR FROM A.start_time),TO_CHAR(A.start_time,'WW')";
							sOrderBy = " order by A.employee_id,nYear,nWeek";
							break;
						case 3:// month
							sSelect += ",EXTRACT(YEAR FROM A.start_time) as nYear,EXTRACT(MONTH FROM A.start_time) as nMonth,A.employee_id as agent, 3 as nType";
							sGroupBy = " group by A.employee_id,EXTRACT(YEAR FROM A.start_time),EXTRACT(MONTH FROM A.start_time)";
							sOrderBy = " order by A.employee_id,nYear,nMonth";
							break;
						}
					}

					String sWhere = " from call_detial A where A.party_name in ('Extension', 'Agent') and A.start_time>=:start and A.start_time<=:end";
					if (sAgent != null && sAgent.length() > 0) {
						sWhere += " and A.employee_id=:agent";
					}
					if (agents != null && agents.size() > 0) {
						sWhere += " and A.employee_id in(:agents)";
						log.info(GsonUtil.getInstance().toJson(agents));
					}
					String sSqlStr = "select count(*) " + sWhere;
					sSqlStr += sGroupBy;
					log.info("start: " + dtStarttime.toString() + ", end:" + dtEndtime.toString());
					log.info(sSqlStr);
					SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
					query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
					if (sAgent != null && sAgent.length() > 0) {
						query.setString("agent", sAgent);
					}
					if (agents != null && agents.size() > 0) {
						query.setParameterList("agents", agents);
					}
					Integer nTotalCount = tab.util.Util
							.ObjectToNumber(query.setFirstResult(0).setMaxResults(1).uniqueResult(), 0);
					if (nTotalCount > 0) {
						sSqlStr = sSelect + sWhere;
						sSqlStr += sGroupBy + sOrderBy;
						log.info(sSqlStr);
						query = gdbsession.createSQLQuery(sSqlStr);
						query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
						if (sAgent != null && sAgent.length() > 0) {
							query.setString("agent", sAgent);
						}
						if (agents != null && agents.size() > 0) {
							query.setParameterList("agents", agents)
									.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
						} else {
							query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
						}

						@SuppressWarnings("unchecked")
						java.util.List<AgentDetailSummaryInfo> CallRecordList = MapToAgentDetailSummaryInfo(
								query.list());
						java.util.Map<String, String> usernames = new java.util.HashMap<String, String>();
						for (int idx = 0; CallRecordList != null && idx < CallRecordList.size(); idx++) {
							AgentDetailSummaryInfo item = CallRecordList.get(idx);
							String sAgentNo = tab.util.Util.ObjectToString(item.agent);
							if (sAgentNo.length() > 0) {
								usernames.put(sAgentNo, "");
							}
						}
						if (usernames.size() > 0) {
							try {
								Session dbsession = HibernateSessionFactory.getThreadSession();
								try {
									@SuppressWarnings("unchecked")
									java.util.List<Object[]> agentNos = dbsession.createSQLQuery(
											"select agent,name from res_users A,res_partner B where A.partner_id=B.id and agent in(:agents)")
											.setParameterList("agents", usernames.keySet()).list();
									for (int idx = 0; idx < agentNos.size(); idx++) {
										Object[] agentUsers = (Object[]) agentNos.get(idx);
										usernames.put(agentUsers[0].toString(),
												tab.util.Util.ObjectToString(agentUsers[1]));
									}
									dbsession.close();
								} catch (Throwable e) {
									log.warn("ERROR:", e);
									result.put("msg", e.toString());
								}
							} catch (Throwable e) {
								log.warn("ERROR:", e);
								result.put("msg", e.toString());
							}
						}
						for (int idx = 0; CallRecordList != null && idx < CallRecordList.size(); idx++) {
							AgentDetailSummaryInfo item = CallRecordList.get(idx);
							item.username = usernames.get(item.agent);
						}

						if (sScript == null || sScript.length() == 0) {
							result.put("list", CallRecordList);
							result.put("totalCount", nTotalCount);
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
								AgentDetailSummaryInfo call = CallRecordList.get(i);
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
														+ URLEncoder.encode(export.GetFileName(), "utf-8")
														+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
																.format(Calendar.getInstance().getTime())
																.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
														+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
										.build();
							}
						}
					}
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
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
	@Path("/GetOutboundData")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response GetOutboundData(@Context HttpServletRequest R, @FormParam("customerId") String customerId,
			@FormParam("ucid") String sUcid, @FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit) {
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;

		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/GetOutboundSummaryData")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response GetOutboundSummaryData(@Context HttpServletRequest R, @FormParam("customerId") String customerId,
			@DefaultValue("10") @FormParam("length") String nLength) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}

		String sWhere = "";
		List<String> customerList = null;
		if (customerId != null && customerId.length() > 0) {
			sWhere = "and A.customerId in (:customerId)";
			customerList = java.util.Arrays.asList(customerId.split(","));
		}
		Session dbsession = HibernateSessionFactory.getThreadSession();
		try {
			Query queryQ = (Query) dbsession.createQuery(
					"select new map (A.customerId as customerId,sum(case when (B.callhistorytype='Outgoing received' or B.callhistorytype='呼出') and B.duration>0 then 1 else 0 end) as nTotal, "
							+ "sum(case when (B.callhistorytype='Outgoing received' or B.callhistorytype='呼出') and B.duration >=:length  then 1 else 0 end) as Outbound)"
							+ " from VtigerContactscf A,VtigerCallhistory B where A.contactid=B.destination and B.toNumber != '4100' "
							+ sWhere + "group by A.customerId")
					.setString("length", nLength);

			if (customerId != null && customerId.length() > 0) {
				queryQ.setParameterList("customerId", customerList);
			}

			@SuppressWarnings("unchecked")
			List<String> objs = queryQ.list();

			result.put("list", objs);
			result.put("success", true);
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		dbsession.close();
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/AddGroup")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response AddGroup(@Context HttpServletRequest R, @FormParam("department") String department,
			@FormParam("vdn") String VDN, @FormParam("groupname") String groupname) {
		log.warn("addgroup--department:" + department + "vdn:" + VDN + "groupname:" + groupname);
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		tab.configServer.ValueString vs = new tab.configServer.ValueString("");
		if (department != null && department.length() > 0 && VDN != null && VDN.length() > 0 && groupname != null
				&& groupname.length() > 0) {

			try {
				configServer nconfig = configServer.getInstance();
				@SuppressWarnings("unused")
				String tablename = department.equals("大众") ? "VWSplitList" : "AoDISplitList";
				vs.value = "";
				nconfig.getValue(Runner.ConfigName_, "VWSplitList", vs, "", "", true, true);
				String VWlist = vs.value == null ? "" : vs.value;
				String[] VWarray = VWlist.split("\\,");
				vs.value = "";
				nconfig.getValue(Runner.ConfigName_, "AoDISplitList", vs, "", "", true, true);
				String ADlist = vs.value == null ? "" : vs.value;
				String[] ADarray = ADlist.split("\\,");
				boolean add = true;
				for (int i = 0; i < ADarray.length; i++) {
					if (VDN.equals(ADarray[i])) {
						add = false;
					}
				}
				for (int i = 0; i < VWarray.length; i++) {
					if (VDN.equals(VWarray[i])) {
						add = false;
					}
				}
				if (add) {// 如果待添加的组好是原先不存在的,需要在tabcallcenter表的VDNMaxNumber或者AoDISplitList添加还有在MyCallProxy中添加对应的几列数据
					if (department.equals("大众")) {
						VWlist += "," + VDN;
						nconfig.setValue(Runner.ConfigName_, "VWSplitList", VWlist, "");
					} else {
						ADlist += "," + VDN;
						nconfig.setValue(Runner.ConfigName_, "AoDISplitList", ADlist, "");
					}
					vs.value = "";
					nconfig.getValue("MyCallProxy", "SplitList", vs, "", "", true, true);
					String splitlist = vs.value == null ? "" : vs.value;
					splitlist += "," + VDN;
					nconfig.setValue("MyCallProxy", "SplitList", splitlist, "");
					configServer.ValueInteger vi = new configServer.ValueInteger(1);
					nconfig.getValue(Runner.ConfigName_, "VDNMaxNumber", vi, "", "", true, true);
					nconfig.getValue("MyCallProxy", vi + "", vs, "", "", true, true);
					nconfig.getValue("MyCallProxy", VDN, vi, "", "SPLIT", true, true);
					vs.value = groupname;
					nconfig.getValue("MyCallProxy", vi.value + "", vs, "", "GroupName", true, true);
					int maxn = vi.value + 1;// 每次增加之后都将这个参数永久加1保证增加的组号永不重复;
					nconfig.setValue(Runner.ConfigName_, "VDNMaxNumber", maxn + "", "");
				} else {
					log.warn("vdn" + VDN + "已经存在");
					result.put("des", "已经存在的vdn");
				}
				// nconfig.setValue(Runner.ConfigName_, tablename, tnumber, "");
				result.put("success", true);
			} catch (Throwable e) {
				log.error("ERROR:" + e);
			}
		} else {
			log.warn("参数有空值");
		}
		return Response.status(200).entity(result).build();
	}

	public void deletegroup(List<String> Extensions, String list, String tablename) {// list是数据库中的keyname,tablename是表名,extensions是前台上传的指定删除集合

		tab.configServer.ValueString vs = new tab.configServer.ValueString("");
		vs.value = "";
		try {
			configServer nconfig = configServer.getInstance();
			nconfig.getValue(tablename, list, vs, "", "", true, true);
			vs.value = vs.value == null ? "" : vs.value;
			String[] exArray = vs.value.split("\\,");
			// String[] newArray = vs.value.split("\\,");
			String newvalue = "";
			for (int i = 0; i < exArray.length; i++) {// exArray是数据库中读出来的集合extensions是前台传上来指定删除的集合
				boolean add = true;
				for (int j = 0; j < Extensions.size(); j++) {// 思路是创建一个空白字符串,遍历原集合如果指定删除的集合中不包含原集合元素就将此元素写进新字符串
					if (Extensions.get(j).equals(exArray[i])) {
						add = false;
					}
				}
				if (add) {
					if (i > 0) {
						newvalue += ",";
					}
					newvalue = newvalue + exArray[i];
				}
			}
			nconfig.setValue(tablename, list, newvalue, "");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@POST
	@Path("/DeleteGroup")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response DeleteGroup(@Context HttpServletRequest R, @FormParam("SplitList") java.util.List<String> SplitList,
			@FormParam("VWarray") java.util.List<String> VWLIst, @FormParam("ADarray") java.util.List<String> ADList,
			@FormParam("GroupNum") /* 跟vdn对应的组号 */java.util.List<String> GroupNum) {
		log.warn("deletegroup--splitlist:" + SplitList.toString() + "vwlist:" + VWLIst.toString() + "adlist:"
				+ ADList.toString() + "GroupNum:" + GroupNum);
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		tab.configServer.ValueString vs = new tab.configServer.ValueString("");
		vs.value = "";
		result.put("success", false);
		try {
			deleteextension(SplitList, "SplitList", "MyCallProxy");
			deleteextension(VWLIst, "VWSplitList", Runner.ConfigName_);
			deleteextension(ADList, "AoDISplitList", Runner.ConfigName_);
			configServer nconfig = configServer.getInstance();
			for (int i = 0; i < SplitList.size(); i++) {
				nconfig.remove("MyCallProxy", SplitList.get(i), "SPLIT");
			}
			for (int i = 0; i < GroupNum.size(); i++) {
				nconfig.remove("MyCallProxy", GroupNum.get(i), "GroupName");
			}
			result.put("success", true);
		} catch (Throwable e) {
			log.warn("ERROR:" + e);
		}
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/GetGroupList")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response GetGroupList(@Context HttpServletRequest R, @FormParam("start") /* "开始页，从0开始" */Integer pageStart,
			@FormParam("condition") String condition, @FormParam("department") String department,
			@FormParam("limit") /* "每页数量，从1开始" */Integer pageLimit) {
		log.warn("getgrouplist--condition:" + condition + "department:" + department);
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		tab.configServer.ValueString vs = new tab.configServer.ValueString("");
		configServer.ValueInteger vi = new configServer.ValueInteger(0);
		vs.value = "";
		result.put("success", false);
		try {
			configServer nconfig = configServer.getInstance();
			// nconfig.getValue("MyCallProxy", "SplitList", vs, "", "", true, true);
			vs.value = "";
			nconfig.getValue(Runner.ConfigName_, "VWSplitList", vs, "", "", true, true);
			String VWsplitlist = vs.value == null ? "" : vs.value;
			vs.value = "";
			nconfig.getValue(Runner.ConfigName_, "AoDISplitList", vs, "", "", true, true);
			String AoDIsplistlist = vs.value == null ? "" : vs.value;
			List<Group> grList = new ArrayList<Group>();
			// String[] exArray = vs.value.split("\\,");
			String[] VWArray = VWsplitlist.split("\\,");
			String[] ADArray = AoDIsplistlist.split("\\,");
			if (department != null && department.equals("大众")) {
				ADArray = new String[0];
			} else if (department != null && department.equals("奥迪")) {
				VWArray = new String[0];
			}
			if (condition == null || condition.length() == 0) {
				for (int i = 0; i < VWArray.length; i++) {
					String sSkill = VWArray[i];
					if (sSkill.length() > 0) {
						vi.value = 1;
						nconfig.getValue("MyCallProxy", sSkill, vi, "", "SPLIT", true, true);
						int nGroup = vi.value;
						vs.value = "";
						nconfig.getValue("MyCallProxy", String.valueOf(nGroup), vs, "", "GroupName", true, true);
						String sGroupName = vs.value;
						Group gr = new Group();
						gr.number = nGroup;
						gr.value = sGroupName;
						gr.skillGroup = sSkill;
						gr.department = "大众";
						grList.add(gr);
					}
				}
				for (int i = 0; i < ADArray.length; i++) {
					String sSkill = ADArray[i];
					if (sSkill.length() > 0) {
						vi.value = 1;
						nconfig.getValue("MyCallProxy", sSkill, vi, "", "SPLIT", true, true);
						int nGroup = vi.value;
						vs.value = "";
						nconfig.getValue("MyCallProxy", String.valueOf(nGroup), vs, "", "GroupName", true, true);
						String sGroupName = vs.value;
						Group gr = new Group();
						gr.number = nGroup;
						gr.value = sGroupName;
						gr.skillGroup = sSkill;
						gr.department = "奥迪";
						grList.add(gr);
					}
				}
			} else {
				for (int i = 0; i < VWArray.length; i++) {
					String sSkill = VWArray[i];
					if (sSkill.length() > 0) {
						vi.value = 1;
						nconfig.getValue("MyCallProxy", sSkill, vi, "", "SPLIT", true, true);
						int nGroup = vi.value;
						vs.value = "";
						nconfig.getValue("MyCallProxy", String.valueOf(nGroup), vs, "", "GroupName", true, true);
						String sGroupName = vs.value;
						if (sGroupName.indexOf(condition) != -1) {
							Group gr = new Group();
							gr.number = nGroup;
							gr.value = sGroupName;
							gr.skillGroup = sSkill;
							gr.department = "大众";
							grList.add(gr);
						}

					}
				}
				for (int i = 0; i < ADArray.length; i++) {
					String sSkill = ADArray[i];
					if (sSkill.length() > 0) {
						vi.value = 1;
						nconfig.getValue("MyCallProxy", sSkill, vi, "", "SPLIT", true, true);
						int nGroup = vi.value;
						vs.value = "";
						nconfig.getValue("MyCallProxy", String.valueOf(nGroup), vs, "", "GroupName", true, true);
						String sGroupName = vs.value;
						if (sGroupName.indexOf(condition) != -1) {
							Group gr = new Group();
							gr.number = nGroup;
							gr.value = sGroupName;
							gr.skillGroup = sSkill;
							gr.department = "奥迪";
							grList.add(gr);
						}

					}
				}
			}
			Collections.sort(grList);// 将集合按组号排序
			int start = pageStart;// 分页
			int end = start + pageLimit;
			if (end >= grList.size()) {
				end = grList.size();
			}
			List<Group> newexlist = new ArrayList<Group>();
			for (int i = start; i < end; i++) {
				newexlist.add(grList.get(i));
			}
			result.put("success", true);
			// 根据skill获取每个组对应的组号
			result.put("list", newexlist);
			result.put("totalCount", grList.size());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/DragGroupRow")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response DragGroupRow(@Context HttpServletRequest R, @FormParam("fskillgroup") String fskillgroup,
			@FormParam("fnumber") String fnumber, @FormParam("fvalue") String fvalue,
			@FormParam("tskillgroup") String tskillgroup, @FormParam("tvalue") String tvalue,
			@FormParam("tnumber") String tnumber) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		tab.configServer.ValueString vs = new tab.configServer.ValueString("");
		// configServer.ValueInteger vi = new configServer.ValueInteger(0);
		vs.value = "";
		try {
			configServer nconfig = configServer.getInstance();
			nconfig.setValue("MyCallProxy", fskillgroup, tnumber, "SPLIT");
			nconfig.setValue("MyCallProxy", fnumber, tvalue, "GroupName");
			nconfig.setValue("MyCallProxy", tskillgroup, fnumber, "SPLIT");
			nconfig.setValue("MyCallProxy", tnumber, fvalue, "GroupName");
			result.put("success", true);
			// 根据skill获取每个组对应的组号
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/RestartThread")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response RestartThread(@Context HttpServletRequest R, @FormParam("starttime") String Starttime) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		tab.configServer.ValueString vs = new tab.configServer.ValueString("");
		vs.value = "";
		result.put("success", false);
		log.warn("设置定时重启重启时间为" + Starttime);
		try {
			configServer nconfig = configServer.getInstance();
			nconfig.getValue(Runner.ConfigName_, "TabClassname", vs, "", "", true, true);
			if (Starttime.indexOf("T") > 0) {
				Starttime = Starttime.replace("T", " ");
			}
			String classname = vs.value;
			// String time=RESTDateParam(Starttime).date;
			nconfig.setValue("MyRobServer", "Mode", 1, classname);
			nconfig.setValue("MyRobServer", "StartTime", Starttime, classname);
			result.put("success", true);
			// 根据skill获取每个组对应的组号
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/EditGroupName")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response EditGroupName(@Context HttpServletRequest R, @FormParam("groupname") String groupname,
			@FormParam("skillgroup") String skillgroup) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		tab.configServer.ValueString vs = new tab.configServer.ValueString("");
		configServer.ValueInteger vi = new configServer.ValueInteger(0);
		result.put("success", false);
		try {
			configServer nconfig = configServer.getInstance();
			nconfig.getValue("MyCallProxy", "SplitList", vs, "", "", true, true);
			vi.value = 1;
			nconfig.getValue("MyCallProxy", skillgroup, vi, "", "SPLIT", true, true);
			int nGroup = vi.value;
			vs.value = "";
			nconfig.setValue("MyCallProxy", String.valueOf(nGroup), groupname, "GroupName");
			result.put("success", true);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.status(200).entity(result).build();
	}

	@SuppressWarnings("deprecation")
	@POST
	@Path("/GetExtensionList")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response GetExtensionList(@Context HttpServletRequest R,
			@FormParam("start") /* "开始页，从0开始" */Integer pageStart, @FormParam("condition") String condition,
			@FormParam("department") String department, @FormParam("limit") /* "每页数量，从1开始" */Integer pageLimit) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		tab.configServer.ValueString vs = new tab.configServer.ValueString("");
		vs.value = "";
		result.put("success", false);
		log.warn("getextensionlist--condition:" + condition + "department:" + department);
		try {
			configServer nconfig = configServer.getInstance();
			// nconfig.getValue(Runner.ConfigName_, "SplitList",extensionlist,"","",true,
			// true);
			// tab.configServer.ValueString extensionlist2 = new
			// tab.configServer.ValueString("");
			// nconfig.getValue("MyCallProxy", "DeviceList", vs, "", "", true, true);
			// String DeviceList=vs.value;
			// nconfig.getValue("MyCallProxy", "AdminList", vs, "", "", true, true);
			// String AdminList=vs.value;
			// nconfig.getValue("MyCallProxy", "AgentList", vs, "", "", true, true);
			// String AgentList=vs.value;
			@SuppressWarnings("unused")
			String list = "";
			vs.value = "";
			nconfig.getValue(Runner.ConfigName_, "VWextensionList", vs, "", "", true, true);
			String VWextensionList = vs.value == null ? "" : vs.value;
			vs.value = "";
			nconfig.getValue(Runner.ConfigName_, "AoDIextemsionList", vs, "", "", true, true);
			String AoDIextemsionList = vs.value == null ? "" : vs.value;
			;

			// list=DeviceList.length()>AdminList.length()?DeviceList:AdminList;
			// list=list.length()>AgentList.length()?list:AgentList;

			list = VWextensionList + "," + AoDIextemsionList;
			List<Extension> exlist = new ArrayList<Extension>();
			String[] VWArray = VWextensionList.split("\\,");
			String[] ADArray = AoDIextemsionList.split("\\,");
			if (department != null && department.equals("大众")) {
				ADArray = new String[0];
			} else if (department != null && department.equals("奥迪")) {
				VWArray = new String[0];
			}
			if (condition == null || condition.length() == 0) {
				for (int i = 0; i < VWArray.length; i++) {
					String sSkill = VWArray[i];
					if (sSkill.length() > 0 && NumberUtils.isNumber(sSkill)) {
						Extension ex = new Extension();
						ex.extension = sSkill;
						ex.department = "大众";
						exlist.add(ex);
					}
				}
				for (int i = 0; i < ADArray.length; i++) {
					String sSkill = ADArray[i];
					if (sSkill.length() > 0 && NumberUtils.isNumber(sSkill)) {
						Extension ex = new Extension();
						ex.extension = sSkill;
						ex.department = "奥迪";
						exlist.add(ex);
					}
				}
			} else {
				for (int i = 0; i < VWArray.length; i++) {
					String sSkill = VWArray[i];
					if (sSkill.length() > 0 && NumberUtils.isNumber(sSkill) && sSkill.indexOf(condition) != -1) {
						Extension ex = new Extension();
						ex.extension = sSkill;
						ex.department = "大众";
						exlist.add(ex);
					}
				}
				for (int i = 0; i < ADArray.length; i++) {
					String sSkill = ADArray[i];
					if (sSkill.length() > 0 && NumberUtils.isNumber(sSkill) && sSkill.indexOf(condition) != -1) {
						Extension ex = new Extension();
						ex.extension = sSkill;
						ex.department = "奥迪";
						exlist.add(ex);
					}
				}
			}

			int start = pageStart;// 分页
			int end = start + pageLimit;
			if (end >= exlist.size()) {
				end = exlist.size();
			}
			List<Extension> newexlist = new ArrayList<Extension>();
			Collections.sort(exlist);// 将集合按组号排序
			for (int i = start; i < end; i++) {
				newexlist.add(exlist.get(i));
			}
			// 根据skill获取每个组对应的组号
			result.put("success", true);
			result.put("list", newexlist);
			result.put("totalCount", exlist.size());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.status(200).entity(result).build();
	}

	class Extension implements Comparable<Extension> {
		String extension;
		String department;

		public String getDepartment() {
			return department;
		}

		public void setDepartment(String department) {
			this.department = department;
		}

		public String getExtension() {
			return extension;
		}

		public void setExtension(String extension) {
			this.extension = extension;
		}

		@Override
		public int compareTo(Extension arg0) {
			return Integer.valueOf(this.getExtension()).compareTo(Integer.valueOf(arg0.getExtension())); // 这里定义你排序的规则。
		}

	}

	class Group implements Comparable<Group> {
		public String getDepartment() {
			return department;
		}

		public void setDepartment(String department) {
			this.department = department;
		}

		String skillGroup;
		Integer number;
		String value;
		String department;

		public String getSkillGroup() {
			return skillGroup;
		}

		public void setSkillGroup(String skillGroup) {
			this.skillGroup = skillGroup;
		}

		public Integer getNumber() {
			return number;
		}

		public void setNumber(Integer number) {
			this.number = number;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public int compareTo(Group arg0) {
			return this.getNumber().compareTo(arg0.getNumber()); // 这里定义你排序的规则。
		}
	}

	public void addExtension(List<String> Extensions, String list, String tablename) {
		tab.configServer.ValueString vs = new tab.configServer.ValueString("");
		vs.value = "";
		try {
			configServer nconfig = configServer.getInstance();

			if (list.equals("AoDIextemsionList") || list.equals("VWextensionList")) {
				if (list.equals("AoDIextemsionList")) {// 如果增加的是大众的分机 就要在这里跟奥迪去重,下面用的map跟自己去重了,反之奥迪也是
					nconfig.getValue(Runner.ConfigName_, "VWextensionList", vs, "", "", true, true);
				} else {
					nconfig.getValue(Runner.ConfigName_, "AoDIextemsionList", vs, "", "", true, true);
				}

				String deletelist = vs.value == null ? "" : vs.value;
				String[] deArray = deletelist.split("\\,");
				List<String> newdelist = new ArrayList<String>();
				for (int j = 0; j < Extensions.size(); j++) {
					boolean add = true;
					for (int i = 0; i < deArray.length; i++) {
						if (deArray[i].equals(Extensions.get(j))) {
							add = false;
						}
					}
					if (add) {
						newdelist.add(Extensions.get(j));
					}
				}
				Extensions = newdelist;
			}
			nconfig.getValue(tablename, list, vs, "", "", true, true);
			vs.value = vs.value == null ? "" : vs.value;
			for (int i = 0; i < Extensions.size(); i++) {
				if (vs.value != null && vs.value.length() > 0) {
					vs.value = vs.value + "," + Extensions.get(i);
				} else {
					vs.value = Extensions.get(i);
				}
			}
			Map<String, String> map = new HashMap<String, String>();
			String[] exArray = vs.value.split("\\,");
			String newvalue = "";
			for (int i = 0; i < exArray.length; i++) {
				String sSkill = exArray[i];
				if (sSkill.length() > 0) {
					if (map.get(sSkill) == null) {
						map.put(sSkill, sSkill);
						if (i > 0) {
							newvalue += ",";
						}
						newvalue = newvalue + sSkill;
					} else {
						log.warn("分机:" + sSkill + "已经存在于" + tablename + "." + list);
					}
				}
			}
			nconfig.setValue(tablename, list, newvalue, "");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@POST
	@Path("/AddExtension")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response AddExtension(@Context HttpServletRequest R, @FormParam("department") String department,
			@FormParam("extensionList") java.util.List<String> Extensions) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		tab.configServer.ValueString extensionlist = new tab.configServer.ValueString("");
		extensionlist.value = "";
		result.put("success", false);
		if (Extensions != null && Extensions.size() > 0) {
			try {
				addExtension(Extensions, "DeviceList", "MyCallProxy");
				addExtension(Extensions, "AdminList", "MyCallProxy");
				addExtension(Extensions, "AgentList", "MyCallProxy");
				if (department.equals("奥迪")) {
					addExtension(Extensions, "AoDIextemsionList", Runner.ConfigName_);
				} else if (department.equals("大众")) {
					addExtension(Extensions, "VWextensionList", Runner.ConfigName_);
				}
				result.put("success", true);
			} catch (Throwable e) {

			}
		} else {

		}
		return Response.status(200).entity(result).build();
	}

	public void deleteextension(List<String> Extensions, String list, String tablename) {// list是数据库中的keyname,tablename是表名,extensions是前台上传的指定删除集合

		tab.configServer.ValueString vs = new tab.configServer.ValueString("");
		vs.value = "";
		try {
			configServer nconfig = configServer.getInstance();
			nconfig.getValue(tablename, list, vs, "", "", true, true);
			vs.value = vs.value == null ? "" : vs.value;
			String[] exArray = vs.value.split("\\,");
			// String[] newArray = vs.value.split("\\,");
			String newvalue = "";
			for (int i = 0; i < exArray.length; i++) {// exArray是数据库中读出来的集合extensions是前台传上来指定删除的集合
				boolean add = true;
				for (int j = 0; j < Extensions.size(); j++) {// 思路是创建一个空白字符串,遍历原集合如果指定删除的集合中不包含原集合元素就将此元素写进新字符串
					if (Extensions.get(j).equals(exArray[i])) {
						add = false;
					}
				}
				if (add) {
					if (i > 0) {
						newvalue += ",";
					}
					newvalue = newvalue + exArray[i];
				}
			}
			nconfig.setValue(tablename, list, newvalue, "");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@POST
	@Path("/DeleteExtension")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response DeleteExtension(@Context HttpServletRequest R, @FormParam("ADarray") java.util.List<String> ADarray,
			@FormParam("DeviceList") java.util.List<String> DeviceList,
			@FormParam("VWarray") java.util.List<String> VWarray) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		try {
			deleteextension(DeviceList, "DeviceList", "MyCallProxy");
			deleteextension(DeviceList, "AdminList", "MyCallProxy");
			deleteextension(DeviceList, "AgentList", "MyCallProxy");
			deleteextension(VWarray, "VWextensionList", Runner.ConfigName_);
			deleteextension(ADarray, "AoDIextemsionList", Runner.ConfigName_);
			result.put("success", true);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return Response.status(200).entity(result).build();
	}

	public void editextension(String extension, String OldExtension, String list) {
		tab.configServer.ValueString vs = new tab.configServer.ValueString("");
		vs.value = "";
		try {
			configServer nconfig = configServer.getInstance();
			nconfig.getValue("MyCallProxy", list, vs, "", "", true, true);
			String[] exArray = vs.value.split("\\,");
			String newvalue = "";
			Boolean ifadd = true;
			for (int i = 0; i < exArray.length; i++) {
				@SuppressWarnings("unused")
				String em = exArray[i];
				if (exArray[i].equals(extension)) {// 修改后的分机已经存在了
					ifadd = false;
				}
			}
			if (ifadd) {
				for (int i = 0; i < exArray.length; i++) {
					String sSkill = exArray[i];
					if (exArray[i].equals(OldExtension)) {
						sSkill = extension;
					}
					if (i > 0) {
						newvalue += ",";
					}
					newvalue = newvalue + sSkill;
				}
				nconfig.setValue("MyCallProxy", list, newvalue, "");
			}

			// 根据skill获取每个组对应的组号
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@POST
	@Path("/EditExtension")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response EditExtension(@Context HttpServletRequest R, @FormParam("extension") String extension,
			@FormParam("oldextension") String OldExtension) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		try {
			editextension(extension, OldExtension, "DeviceList");
			editextension(extension, OldExtension, "AdminList");
			editextension(extension, OldExtension, "AgentList");
			result.put("success", true);
		} catch (Throwable e) {
		}
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/GetOutboundAgentSummaryData")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response GetOutboundAgentSummaryData(@Context HttpServletRequest R,
			@FormParam("starttime") RESTDateParam dtStarttime, @FormParam("endtime") RESTDateParam dtEndtime,
			@FormParam("agent") String sAgent, @DefaultValue("10") @FormParam("length") String nLength) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}

		String sWhere = "";
		if (sAgent != null && sAgent.length() > 0) {
			sWhere += " and A.userName=:agent and A.isAdmin='off'";
		}
		Session dbsession = HibernateSessionFactory.getThreadSession();
		try {

			Query sql = (Query) dbsession.createQuery("select new map(A.userName as username,"
					+ "sum(case when B.callhistorytype='Outgoing received' or B.callhistorytype='Outgoing missed' or B.callhistorytype='呼出' then 1 else 0 end) as nTotal,"
					+ "sum(case when (B.callhistorytype='Outgoing received' or B.callhistorytype='呼出') and B.duration>0 then 1 else 0 end) as Outbound,"
					+ "sum(case when (B.callhistorytype='Outgoing received' or B.callhistorytype='呼出') and B.duration>=:length then 1 else 0 end) as Outbound10,"
					+ "sum(case when (B.callhistorytype='Outgoing received' or B.callhistorytype='呼出') then B.duration else 0 end) as OutboundLength)"
					+ " from VtigerUsers A,VtigerCallhistory B where (A.id=B.source or (B.source is null and A.phoneCrmExtensionExtra=B.fromNumberExtra))"
					+ " and B.startTime>=:startTime and B.startTime<=:endTime  and B.toNumber != '4100' " + sWhere
					+ " group by A.userName").setTimestamp("startTime", dtStarttime.getDate())
					.setTimestamp("endTime", dtEndtime.getDate()).setString("length", nLength);

			// Query sql = (Query)dbsession.createQuery("select new map(A.agent as agent,"
			// + "sum(case when (A.type=2 or A.type=4 or A.type=5) then 1 else 0 end) as
			// nTotal,"
			// + "sum(case when (A.type=2 or A.type=5) then 1 else 0 end) as Outbound,"
			// + "sum(case when ((A.type=2 or A.type=5) and A.length>=:length) then 1 else 0
			// end) as Outbound10,"
			// + "sum(case when (A.type=2 or A.type=4 or A.type=5) then A.length else 0 end)
			// as OutboundLength) "
			// + " from CallagentrecordExOut A where callagentrecordId in (select
			// max(callagentrecordId) FROM Callagentrecord group by ucid) "
			// + "and A.ringtime>=:startTime and A.ringtime<:endTime"
			// + sWhere + " group by A.agent")
			// .setTimestamp("startTime", dtStarttime.getDate())
			// .setTimestamp("endTime", dtEndtime.getDate())
			// .setString("length", nLength);
			//
			if (sAgent != null && sAgent.length() > 0) {
				sql.setString("agent", sAgent);
			}

			@SuppressWarnings("unchecked")
			List<String> agentOut = sql.list();

			result.put("list", agentOut);
			result.put("success", true);
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		dbsession.close();
		return Response.status(200).entity(result).build();
	}

	// 租赁
	@POST
	@Path("/ReportIvrCallSummary") // 呼入队列汇总
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ReportIvrCallSummary(@Context HttpServletRequest R, @FormParam("department") String sDepartment,
			@FormParam("starttime") RESTDateParam dtStarttime, @FormParam("endtime") RESTDateParam dtEndtime,
			@FormParam("type") Integer nType, @FormParam("bTransfer") Boolean bTransfer,
			@FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				String sSelect = "select A.queues as vdn, SUM( case when A.technical_result='Completed' and not(A.talk_duration=0 and upper(A.result_reason)='ABANDONEDWHILEQUEUED') then 1 else 0 end) as nInboundCount,"// 接听数
						+ "AVG(case when upper(A." + RecSystem.sInteraction_type_name
						+ ")='INBOUND' then A.sum_routing_point_duration else 0 end) as nInboundLength,"// IVR平均时长
						+ "AVG(case when upper(A." + RecSystem.sInteraction_type_name
						+ ")='INBOUND' then A.sum_mediation_duration else 0 end) as nInboundWait,"// 排队平均时长
						+ "SUM(case when not(A.technical_result='Completed'  or (A.talk_duration=0 and upper(A.result_reason)='ABANDONEDWHILEQUEUED')) and not(A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0 and upper(A.result_reason)!='ABANDONEDWHILEQUEUED')  then 1 else 0 end) as nNoAnswerCount,"// 未接听数
						+ "SUM(case when A.technical_result!='Completed'  and (A.talk_duration=0 and upper(A.result_reason)='ABANDONEDWHILEQUEUED') then 1 else 0 end) as nNoAnswerLength,"// 排队放弃数
						+ "AVG(case when A.talk_duration=0 and upper(A." + RecSystem.sInteraction_type_name
						+ ")='INBOUND' then A.sum_mediation_duration else 0 end) as nNoAnswerWait,"// 无人接听平均时长
						+ "MAX(case when upper(A." + RecSystem.sInteraction_type_name
						+ ")='INBOUND' then A.sum_routing_point_duration end) as nMaxLength,"// --最大IVR时长
						+ "MAX(case when upper(A." + RecSystem.sInteraction_type_name
						+ ")='INBOUND' then A.sum_mediation_duration end) as nMaxWaitLength";// --最大排队时长
				String sGroupBy = " group by A.queues,EXTRACT(YEAR FROM A.start_time),EXTRACT(MONTH FROM A.start_time),EXTRACT(DAY FROM A.start_time),TO_CHAR(A.start_time,'hh24')";
				String sOrderBy = " order by A.queues,nYear,nMonth,nDay,nHour";
				if (GHibernateSessionFactory.databaseType == "SQLServer") {
					// 记得加上queues 还有result_reason
					sGroupBy = " group by A.queues,DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time),DATEPART(hour,A.start_time)";
					switch (nType) {
					default:
						// 记得将employee_id换成queues
					case 0:// hour
						sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time) as nDay,DATEPART(hour,A.start_time) as nHour, A.queues as queue,0 as nType";
						break;
					case 1:// day
						sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time)  as nDay,A.queues as queue, 1 as nType";
						sGroupBy = " group by A.queues,DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time)";
						sOrderBy = " order by A.queues,nYear,nMonth,nDay";
						break;
					case 2:// week
						sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(week , A.start_time) as nWeek,A.queues as queue, 2 as nType";
						sGroupBy = " group by A.queues,DATEPART(year , A.start_time),DATEPART(week , A.start_time)";
						sOrderBy = " order by A.queues,nYear,nWeek";
						break;
					case 3:// month
						sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,A.queues as queue, 3 as nType";
						sGroupBy = " group by A.queues,DATEPART(year , A.start_time),DATEPART(month , A.start_time)";
						sOrderBy = " order by A.queues,nYear,nMonth";
						break;
					}
				} else {
					switch (nType) {
					default:
					case 0:// hour
						sSelect += ",EXTRACT(YEAR FROM A.start_time) as nYear,EXTRACT(MONTH FROM A.start_time) as nMonth,EXTRACT(DAY FROM A.start_time) as nDay,TO_CHAR(A.start_time,'hh24') as nHour,A.queues as queue, 0 as nType";
						break;
					case 1:// day
						sSelect += ",EXTRACT(YEAR FROM A.start_time) as nYear,EXTRACT(MONTH FROM A.start_time) as nMonth,EXTRACT(DAY FROM A.start_time) as nDay,A.queues as queue, 1 as nType";
						sGroupBy = " group by A.queues,EXTRACT(YEAR FROM A.start_time),EXTRACT(MONTH FROM A.start_time),EXTRACT(DAY FROM A.start_time)";
						sOrderBy = " order by A.queues,nYear,nMonth,nDay";
						break;
					case 2:// week
						sSelect += ",EXTRACT(YEAR FROM A.start_time) as nYear,TO_CHAR(A.start_time,'WW') as nWeek,A.queues as queue, 2 as nType";
						sGroupBy = " group by A.queues,EXTRACT(YEAR FROM A.start_time),TO_CHAR(A.start_time,'WW')";
						sOrderBy = " order by A.queues,nYear,nWeek";
						break;
					case 3:// month
						sSelect += ",EXTRACT(YEAR FROM A.start_time) as nYear,EXTRACT(MONTH FROM A.start_time) as nMonth,A.queues as queue, 3 as nType";
						sGroupBy = " group by A.queues,EXTRACT(YEAR FROM A.start_time),EXTRACT(MONTH FROM A.start_time)";
						sOrderBy = " order by A.queues,nYear,nMonth";
						break;
					}
				}

				String sWhere = " from call_detial A where not(A.queues is null) and A.start_time>=:start and A.start_time<=:end";
				if (GHibernateSessionFactory.databaseType == "SQLServer") {
					sWhere = " from call_detial A where  A.start_time>=:start and A.start_time<=:end";
				}

				switch (sDepartment) {
				case "consulting":
					sWhere += " and A.queues='consulting'";
					break;
				case "change":
					sWhere += " and A.queues='change'";
					break;
				case "complaints":
					sWhere += " and A.queues='complaints'";
					break;
				case "other":
					sWhere += " and A.queues='other'";
					break;
				case "consulting_jrsyb":
					sWhere += " and A.queues='consulting_jrsyb'";
					break;
				case "change_jrsyb":
					sWhere += " and A.queues='change_jrsyb'";
					break;
				case "complaints_jrsyb":
					sWhere += " and A.queues='complaints_jrsyb'";
					break;
				case "other_jrsyb":
					sWhere += " and A.queues='other_jrsyb'";
					break;
				}
				String sSqlStr = "select count(*) from ( select row_number() over (partition by A.media_server_ixn_guid order by A.end_time desc) as callid "
						+ sWhere + ") A where A.callid=1";
				log.info(sSqlStr);
				SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
				query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
				Integer nTotalCount = tab.util.Util
						.ObjectToNumber(query.setFirstResult(0).setMaxResults(1).uniqueResult(), 0);
				if (nTotalCount > 0) {
					sSqlStr = sSelect
							+ " from ( select sum(A.routing_point_duration) over (partition by A.media_server_ixn_guid) as sum_routing_point_duration, sum(A.mediation_duration) over (partition by A.media_server_ixn_guid) as sum_mediation_duration, row_number() over (partition by A.media_server_ixn_guid order by A.end_time desc) as callid,A.* "
							+ sWhere + ") A where A.callid=1 " + sGroupBy + sOrderBy;
					log.info(sSqlStr);
					query = gdbsession.createSQLQuery(sSqlStr);
					query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);

					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					if (sScript == null || sScript.length() == 0) {
						result.put("list", CallRecordList);
						result.put("totalCount", nTotalCount);
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
											"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
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

	// 租赁
	@POST
	@Path("/ReportIvrVdn") // 呼入接听明细
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response ReportIvrVdn(@Context HttpServletRequest R, @FormParam("starttime") RESTDateParam dtStarttime,
			@FormParam("endtime") RESTDateParam dtEndtime, @FormParam("caller") String sCaller,
			@FormParam("agent") String sAgent, @FormParam("type") Integer nType,
			@FormParam("department") String sDepartment, @FormParam("start") Integer pageStart,

			@FormParam("limit") Integer pageLimit, @FormParam("sort") EXTJSSortParam Sort,
			@FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				String sSelect = "select A.ringcount as ringcount, A.queues as vdn, (A.start_time-A.mediation_duration/24/60/60-A.routing_point_duration/24/60/60) as ringtime, A.start_time as begintime, A.end_time as endtime,A.resource_name as channel, "
						+ " A.source_address as ani,A.target_address as dnis,(case when upper(party_name)='ROUTINGPOINT' then '' else A.employee_id end) as agent,A.talk_duration as length, A.sum_routing_point_duration as wait,A.sum_mediation_duration as queuetime,"
						+ " (case when (A.technical_result='Completed' or (A.talk_duration=0 and A.result_reason='AbandonedWhileQueued')) and (A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0 and A.result_reason!='AbandonedWhileQueued') then 'IVR' else ("
						+ "case when (A.talk_duration=0 and A.result_reason='AbandonedWhileQueued') then 'AbandonedWhileQueued' else A.technical_result end) end) as type";
				String sWhere = " from call_detial A where not(A.queues is null) and A.start_time>=:start and A.start_time<=:end";
				String sWhereOut = StringUtils.EMPTY;
				if (sCaller != null && sCaller.length() > 0) {
					sWhereOut += " and (A.source_address like :caller or A.target_address like :caller)";
				}
				if (sAgent != null && sAgent.length() > 0) {
					sWhereOut += " and A.employee_id=:agent";
				}
				switch (nType) {
				default:
				case 0:// All
					break;
				case 1:// 坐席接听
					sWhereOut += " and (A.technical_result='Completed') and not(A.talk_duration=0 and A.result_reason='AbandonedWhileQueued') ";
					break;
				case 2:// 坐席未接听
					sWhereOut += " and not(A.technical_result='Completed' or (A.talk_duration=0 and A.result_reason='AbandonedWhileQueued')) and not(A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0 and A.result_reason!='AbandonedWhileQueued') ";
					break;
				case 3:// 客户放弃
					sWhereOut += " and (A.technical_result!='Completed') and (A.talk_duration=0 and A.result_reason='AbandonedWhileQueued') ";
					break;
				case 4:// IVR
					sWhereOut += " and (A.technical_result='Completed' or (A.talk_duration=0 and A.result_reason='AbandonedWhileQueued')) and (A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0 and A.result_reason!='AbandonedWhileQueued') ";
					break;
				}
				switch (sDepartment) {
				case "consulting":
					sWhere += " and A.queues='consulting'";
					break;
				case "change":
					sWhere += " and A.queues='change'";
					break;
				case "complaints":
					sWhere += " and A.queues='complaints'";
					break;
				case "other":
					sWhere += " and A.queues='other'";
					break;
				case "consulting_jrsyb":
					sWhere += " and A.queues='consulting_jrsyb'";
					break;
				case "change_jrsyb":
					sWhere += " and A.queues='change_jrsyb'";
					break;
				case "complaints_jrsyb":
					sWhere += " and A.queues='complaints_jrsyb'";
					break;
				case "other_jrsyb":
					sWhere += " and A.queues='other_jrsyb'";
					break;
				}
				String sSqlStr = "select count(*) from ( select row_number() over (partition by A.media_server_ixn_guid order by A.end_time desc) as callid,A.* "
						+ sWhere + ") A where A.callid=1" + sWhereOut;
				log.info(sSqlStr);
				SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
				query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
				if (sCaller != null && sCaller.length() > 0) {
					query.setString("caller", "%" + sCaller + "%");
				}
				if (sAgent != null && sAgent.length() > 0) {
					query.setString("agent", sAgent);
				}
				Integer nTotalCount = tab.util.Util
						.ObjectToNumber(query.setFirstResult(0).setMaxResults(1).uniqueResult(), 0);
				if (nTotalCount > 0) {
					sSqlStr = sSelect
							+ " from ( select count(*) over (partition by A.media_server_ixn_guid) as ringcount,sum(A.routing_point_duration) over (partition by A.media_server_ixn_guid) as sum_routing_point_duration, sum(A.mediation_duration) over (partition by A.media_server_ixn_guid) as sum_mediation_duration, row_number() over (partition by A.media_server_ixn_guid order by A.end_time desc) as callid,A.* "
							+ sWhere + ") A where A.callid=1 " + sWhereOut;
					String sProperty = StringUtils.EMPTY;
					if (Sort != null) {
						switch (Sort.getsProperty()) {
						case "channel":
							sProperty = "A.resource_name";
							break;
						case "begintime":
							sProperty = "A.start_time";
							break;
						case "ani":
							sProperty = "A.source_address";
							break;
						case "dnis":
							sProperty = "A.target_address";
							break;
						case "endtime":
							sProperty = "A.end_time";
							break;
						case "ringtime":
							sProperty = "(A.start_time-A.mediation_duration/24/60/60)";
							break;
						case "type":
							sProperty = "A.technical_result";
							break;
						case "queuetime":
							sProperty = "A.mediation_duration";
							break;
						case "wait":
							sProperty = "A.ring_duration";
							break;
						default:
							sProperty = "A.start_time";
							break;
						}
					}
					if (!sProperty.isEmpty()) {
						sSqlStr += " order by " + sProperty + " " + Sort.getsDirection();
					}
					log.info(sSqlStr);
					query = gdbsession.createSQLQuery(sSqlStr);
					query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
					if (sCaller != null && sCaller.length() > 0) {
						query.setString("caller", "%" + sCaller + "%");
					}
					if (sAgent != null && sAgent.length() > 0) {
						query.setString("agent", sAgent);
					}
					if (sScript == null || sScript.length() < 0) {
						query.setFirstResult(pageStart).setMaxResults(pageLimit);
					}
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);

					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					java.util.Map<String, String> usernames = new java.util.HashMap<String, String>();
					for (int idx = 0; CallRecordList != null && idx < CallRecordList.size(); idx++) {
						java.util.Map<String, Object> item = CallRecordList.get(idx);
						String sAgentNo = tab.util.Util.ObjectToString(item.get("agent"));
						if (sAgentNo.length() > 0) {
							usernames.put(sAgentNo, "");
						}
					}
					if (usernames.size() > 0) {
						try {
							Session dbsession = HibernateSessionFactory.getThreadSession();
							try {
								@SuppressWarnings("unchecked")
								java.util.List<Object[]> agentNos = dbsession.createSQLQuery(
										"select agent,name from res_users A,res_partner B where A.partner_id=B.id and agent in(:agents)")
										.setParameterList("agents", usernames.keySet()).list();
								for (int idx = 0; idx < agentNos.size(); idx++) {
									Object[] agentUsers = (Object[]) agentNos.get(idx);
									usernames.put(agentUsers[0].toString(),
											tab.util.Util.ObjectToString(agentUsers[1]));
								}
								dbsession.close();
							} catch (Throwable e) {
								log.warn("ERROR:", e);
								result.put("msg", e.toString());
							}
						} catch (Throwable e) {
							log.warn("ERROR:", e);
							result.put("msg", e.toString());
						}
					}
					for (int idx = 0; CallRecordList != null && idx < CallRecordList.size(); idx++) {
						java.util.Map<String, Object> item = CallRecordList.get(idx);
						item.put("username", usernames.get(item.get("agent")));
					}
					if (sScript == null || sScript.length() == 0) {
						result.put("list", CallRecordList);
						result.put("totalCount", nTotalCount);
						result.put("success", true);
					} else {
						tab.MyExportExcelFile export = null;
						export = new tab.MyExportExcelFile();
						if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator")
								+ "WebRoot" + sScript)) {
							gdbsession.close();
							return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain").build();
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
											"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				} else {
					if (sScript == null || sScript.length() == 0) {
						result.put("list", new java.util.ArrayList<java.util.Map<String, Object>>());
						result.put("totalCount", 0);
						result.put("success", true);
					}
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
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

	/**
	 * 
	 * @param 客户:客服系统
	 * @param 报表接口
	 * @param 坐席详单接口
	 * @return
	 */
	@POST
	@Path("/GetAgentDetailData")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response GetAgentDetailData(@Context HttpServletRequest R, @Context ContainerRequestContext ctx,
			@FormParam("appid") String sAppId, @FormParam("token") String sToken,
			@FormParam("starttime") RESTDateParam dtStarttime, @FormParam("endtime") RESTDateParam dtEndtime,
			@FormParam("agent") String sAgent, @FormParam("extension") String sExtension,
			@FormParam("inbound") Integer nInbound, @DefaultValue("0") @FormParam("inside") Integer nInside,
			@FormParam("length") Integer nLength, @FormParam("groupguid") String sGroupguid,
			@FormParam("caller") String sCaller, @FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit) {
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		if (nInbound == null)
			nInbound = 0;
		if (nLength == null)
			nLength = 0;
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	/**
	 * 汇总外部接口
	 */
	@POST
	@Path("/GetAgentDetailSummaryData")
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response GetAgentDetailSummaryData(@Context HttpServletRequest R, @Context ContainerRequestContext ctx,
			@FormParam("appid") String sAppId, @FormParam("token") String sToken,
			@FormParam("groupguid") String sGroupguid, @FormParam("starttime") RESTDateParam dtStarttime,
			@FormParam("endtime") RESTDateParam dtEndtime, @FormParam("agent") String sAgent,
			@DefaultValue("10") @FormParam("length") String nLength) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);

		String sUId = null;

		if (ctx.getProperty("Authorized").equals(true)) {
			sGroupguid = tab.util.Util.ObjectToString(ctx.getProperty("roleid"));
		} else {
			if (sAppId != null && sToken != null) {
				sGroupguid = RbacClient.checkAppIdToken(sAppId, sToken);
			} else {
				HttpSession httpsession = R.getSession();
				if (httpsession == null) {
					log.error("ERROR: getSession() is null");
					return Response.status(200).entity(result).build();
				}
				sUId = Util.ObjectToString(httpsession.getAttribute("uid"));
				if (!sUId.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
					log.error("ERROR: UID is null");
					return Response.status(200).entity(result).build();
				}
			}
		}

		String sWhere = "";
		if (sAgent != null && sAgent.length() > 0) {
			sWhere += " and A.userName=:agent and A.isAdmin='off'";
		}
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {

				Query sql = (Query) dbsession.createQuery("select new map(A.agent as agent,"
						+ "sum(case when (A.type=0 or A.type=1 or A.type=3) then 1 else 0 end) as nInTotal,"
						+ "sum(case when (A.type=0 or A.type=1) then 1 else 0 end) as Inbound,"
						+ "sum(case when ((A.type=0 or A.type=1) and A.length>=:length) then 1 else 0 end) as InboundCount0,"
						+ "sum(case when (A.type=0 or A.type=1 or A.type=3) then A.length else 0 end) as InboundLength,"
						+ "sum(case when (A.type=2 or A.type=4 or A.type=5) then 1 else 0 end) as nOutTotal,"
						+ "sum(case when (A.type=2 or A.type=5) then 1 else 0 end) as Outbound,"
						+ "sum(case when ((A.type=2 or A.type=5) and A.length>=:length) then 1 else 0 end) as OutboundCount0,"
						+ "sum(case when (A.type=2 or A.type=4 or A.type=5) then A.length else 0 end) as OutboundLength) "
						+ " from CallagentrecordExExample A where callagentrecordId in (select max(callagentrecordId) FROM Callagentrecord group by ucid) "
						+ "and A.ringtime>=:startTime and A.ringtime<:endTime" + sWhere + " group by A.agent ")
						.setTimestamp("startTime", dtStarttime.getDate()).setTimestamp("endTime", dtEndtime.getDate())
						.setString("length", nLength);

				if (sAgent != null && sAgent.length() > 0) {
					sql.setString("agent", sAgent);
				}

				@SuppressWarnings("unchecked")
				List<String> agentOut = sql.list();

				result.put("list", agentOut);
				result.put("success", true);
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}

	// 通快
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@POST
	@Path("/NoAnswerCall") // 未接来电明细
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response NoAnswerCall(@Context HttpServletRequest R, @FormParam("starttime") RESTDateParam dtStarttime,
			@FormParam("endtime") RESTDateParam dtEndtime, @FormParam("caller") String sCaller,
			@FormParam("agent") String sAgent, @FormParam("department") String sGroupguid,
			@FormParam("start") Integer pageStart,

			@FormParam("limit") Integer pageLimit, @FormParam("sort") EXTJSSortParam Sort,
			@FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		StatWebSocketServlet sta=new StatWebSocketServlet();
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		result.put("success", false);
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				String sSelect = "select  A.queues as vdn, (A.start_time-A.mediation_duration/24/60/60-A.routing_point_duration/24/60/60) as ringtime, A.start_time as begintime, A.end_time as endtime,A.resource_name as channel, "
						+ " A.source_address as ani,A.target_address as dnis,(case when upper(party_name)='ROUTINGPOINT' then '' else A.employee_id end) as agent,A.talk_duration as length, A.routing_point_duration as wait,A.mediation_duration as queuetime, "
						+ " (case when (A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0) then 'IVR' else A.technical_result end) as type";
				String sWhere = " from call_detial A where not(A.queues is null) and A.start_time>=:start and A.start_time<=:end ";

				String sUId = tab.util.Util.ObjectToString(httpsession.getAttribute("uid"));
				List<Rbacrole> groupList = new ArrayList<Rbacrole>();
				String queuenamestr = "";
				if (sGroupguid == null || sGroupguid.equals("")
						|| sGroupguid.equals("00000000-0000-0000-0000-000000000000")) {// 没有选择组的时候查全部组,这里做的操作是拼接sql语句
					groupList = RbacSystem.getUserDepartment(sUId);
					if (groupList.size() == 0) {
						result.put("list", null);
						result.put("totalCount", 0);
						result.put("success", true);
						return Response.status(200).entity(result).build();
					} else if (groupList.size() == 1) {
						Rbacrole role = groupList.get(0);
						queuenamestr = RbacSystem.getDeptQueues(sGroupguid);
						sSelect += " ,(case when A.queues in " + queuenamestr + " then '" + role.getRolename()
								+ "' else '' end ) as partment";
					} else {
						String endselect = "";
						for (int i = 0; i < groupList.size(); i++) {
							if (i == 0) {
								Rbacrole role = groupList.get(i);
								queuenamestr = RbacSystem.getDeptQueues(role.getRoleguid());
								sSelect += " ,(case when A.queues in " + queuenamestr + " then '" + role.getRolename()
										+ "' else ";
								endselect += "'' end) ";
							} else {
								Rbacrole role = groupList.get(i);
								queuenamestr = RbacSystem.getDeptQueues(role.getRoleguid());
								sSelect += " (case when A.queues in " + queuenamestr + " then '" + role.getRolename()
										+ "' else ";
								endselect += " end)";
							}
						}
						sSelect += endselect += " as department";
					}
				} else { // 当选择了组的时候
					Rbacrole role = RbacSystem.getrole(sGroupguid);
					queuenamestr = RbacSystem.getDeptQueues(sGroupguid);
					sSelect += " ,(case when A.queues in " + queuenamestr + " then '" + role.getRolename()
							+ "' else '' end ) as partment";
					sWhere += " and A.queues in " + queuenamestr + "";
				}
				// 客户放弃+坐席未接听(经过队列到了分机再挂断,)
				sWhere += " and (A.technical_result!='Completed') and (A.talk_duration=0 and A.result_reason='AbandonedWhileQueued') "
						// 下面这个条件为去除未进入队列就挂断的骚扰电话
						+ " and not(A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0 and A.result_reason!='AbandonedWhileQueued')";
				if (sCaller != null && sCaller.length() > 0) {
					sWhere += " and (A.source_address like :caller or A.target_address like :caller)";
				}
				if (sAgent != null && sAgent.length() > 0) {
					sWhere += " and A.employee_id=:agent";
				}

				String sSqlStr = "select count(*) from ( select row_number() over (partition by A.media_server_ixn_guid order by A.end_time desc) as callid,A.* "
						+ sWhere + ") A where A.callid=1";
				log.info(sSqlStr);
				SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
				query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
				if (sCaller != null && sCaller.length() > 0) {
					query.setString("caller", "%" + sCaller + "%");
				}
				if (sAgent != null && sAgent.length() > 0) {
					query.setString("agent", sAgent);
				}
				Integer nTotalCount = tab.util.Util
						.ObjectToNumber(query.setFirstResult(0).setMaxResults(1).uniqueResult(), 0);
				if (nTotalCount > 0) {
					sSqlStr = sSelect
							+ " from ( select count(*) over (partition by A.media_server_ixn_guid) as ringcount, "
							+ " row_number() over (partition by A.media_server_ixn_guid order by A.end_time desc) as callid,A.* "
							+ sWhere + ") A where A.callid=1 ";
					// sSqlStr=sSelect+sWhere;
					String sProperty = StringUtils.EMPTY;
					if (Sort != null) {
						switch (Sort.getsProperty()) {
						case "channel":
							sProperty = "A.resource_name";
							break;
						case "begintime":
							sProperty = "A.start_time";
							break;
						case "ani":
							sProperty = "A.source_address";
							break;
						case "dnis":
							sProperty = "A.target_address";
							break;
						case "endtime":
							sProperty = "A.end_time";
							break;
						case "ringtime":
							sProperty = "(A.start_time-A.mediation_duration/24/60/60)";
							break;
						case "type":
							sProperty = "A.technical_result";
							break;
						case "queuetime":
							sProperty = "A.mediation_duration";
							break;
						case "wait":
							sProperty = "A.ring_duration";
							break;
						default:
							sProperty = "A.start_time";
							break;
						}
					}
					if (!sProperty.isEmpty()) {
						sSqlStr += " order by " + sProperty + " " + Sort.getsDirection();
					}
					log.info(sSqlStr);
					query = gdbsession.createSQLQuery(sSqlStr);
					query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
					if (sCaller != null && sCaller.length() > 0) {
						query.setString("caller", "%" + sCaller + "%");
					}
					if (sAgent != null && sAgent.length() > 0) {
						query.setString("agent", sAgent);
					}
					if (sScript == null || sScript.length() < 0) {
						query.setFirstResult(pageStart).setMaxResults(pageLimit);
					}
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);

					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					java.util.Map<String, String> usernames = new java.util.HashMap<String, String>();
					HashSet<String> callerSet = new HashSet<String>();
					for (int idx = 0; CallRecordList != null && idx < CallRecordList.size(); idx++) {
						java.util.Map<String, Object> item = CallRecordList.get(idx);
						String sAgentNo = tab.util.Util.ObjectToString(item.get("agent"));
						String caller = tab.util.Util.ObjectToString(item.get("ani"));
						callerSet.add(caller);
						if (sAgentNo.length() > 0) {
							usernames.put(sAgentNo, "");
						}
					}
					// 查询这批号码最近一次接通的呼叫发生的时间,在前端对比如果对应的未接听呼叫发生在这个时间之后就需要回拨否则不需要回拨
					//String lwhere = "where 1=1";
					String lwhere = " where (A.technical_result='Completed') and not(A.talk_duration=0 and A.result_reason='AbandonedWhileQueued') ";
					if (callerSet != null && callerSet.size() > 0) {
						lwhere += " and A.source_address in (:callerSet)";
					}
					String lasttimeQuerystr = "select max(start_time) as ringtime,source_address as ani from call_detial A " + lwhere
							+ "  group by A.source_address ";
					log.info(lasttimeQuerystr);
					Query querycaller = gdbsession.createSQLQuery(lasttimeQuerystr);
					if (callerSet != null && callerSet.size() > 0) {
						querycaller.setParameterList("callerSet", callerSet);
					}

					querycaller.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					java.util.List<java.util.Map<String, Object>> callerlist = querycaller.list();
					Map callerMap = new HashMap<String, Object>();
					// 将callerlist中的时间对应带CallRecordList中去
					for (int j = 0; j < callerlist.size(); j++) {
						java.util.Map<String, Object> item = callerlist.get(j);
						callerMap.put(item.get("ani"), item.get("ringtime"));
					}
					for (int idx = 0; CallRecordList != null && idx < CallRecordList.size(); idx++) {
						java.util.Map<String, Object> item = CallRecordList.get(idx);
						String callnumber = tab.util.Util.ObjectToString(item.get("ani"));
						if (callerMap.get(callnumber) != null) {
							item.put("lastcalltime", callerMap.get(callnumber));
						} else {
							item.put("lastcalltime", "");
						}
					}

					if (usernames.size() > 0) {
						try {
							Session dbsession = HibernateSessionFactory.getThreadSession();
							try {
								@SuppressWarnings("unchecked")
								java.util.List<Object[]> agentNos = dbsession
										.createSQLQuery(
												"select agent,username from RbacUserAuths  where  agent in(:agents)")
										.setParameterList("agents", usernames.keySet()).list();
								for (int idx = 0; idx < agentNos.size(); idx++) {
									Object[] agentUsers = (Object[]) agentNos.get(idx);
									usernames.put(agentUsers[0].toString(),
											tab.util.Util.ObjectToString(agentUsers[1]));
								}
								dbsession.close();
							} catch (Throwable e) {
								log.warn("ERROR:", e);
								result.put("msg", e.toString());
							}
						} catch (Throwable e) {
							log.warn("ERROR:", e);
							result.put("msg", e.toString());
						}
					}
					for (int idx = 0; CallRecordList != null && idx < CallRecordList.size(); idx++) {
						java.util.Map<String, Object> item = CallRecordList.get(idx);
						item.put("username", usernames.get(item.get("agent")));
					}
					if (sScript == null || sScript.length() == 0) {
						result.put("list", CallRecordList);
						result.put("totalCount", nTotalCount);
						result.put("success", true);
					} else {
						tab.MyExportExcelFile export = null;
						export = new tab.MyExportExcelFile();
						if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator")
								+ "WebRoot" + sScript)) {
							gdbsession.close();
							return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain").build();
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
											"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				} else {
					if (sScript == null || sScript.length() == 0) {
						result.put("list", new java.util.ArrayList<java.util.Map<String, Object>>());
						result.put("totalCount", 0);
						result.put("success", true);
					}
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
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

	// 通快
	@POST
	@Path("/ReportDeptDetail") // 坐席通话明细
	@Consumes("application/x-www-form-urlencoded")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response ReportDeptDetail(@Context HttpServletRequest R, @FormParam("starttime") RESTDateParam dtStarttime,
			@FormParam("endtime") RESTDateParam dtEndtime, @FormParam("agent") String sAgent,
			@FormParam("extension") String sExtension, @FormParam("caller") String sCaller,
			@FormParam("inbound") Integer nInbound, @FormParam("nType") Integer nType,
			@FormParam("weekend") Integer nWeekend, @FormParam("length") Integer nLength,
			@FormParam("department") String sGroupguid, @FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit, @FormParam("sort") EXTJSSortParam Sort,@FormParam("queues") String sQueques,
			@FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		if (nInbound == null)
			nInbound = 0;
		if (nWeekend == null)
			nWeekend = 0;
		if (nLength == null)
			nLength = 0;

		
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
		
			try {
				//
			
				
				String sSelect = "select A.start_time as ringtime, case when A.answer_time is null then A.start_time else A.answer_time end as begintime, A.end_time as endtime,A.resource_name as channel, "
						+ " A.source_address as ani,A.target_address as dnis,A.employee_id as agent,A.talk_duration as length, A.ring_duration as wait,A.queues as queues,"
						+ " (case when A.talk_duration>0 then (case when upper(A." + RecSystem.sInteraction_type_name
						+ ")='OUTBOUND'" + "  then 2 else 1 end) else (case when upper(A."
						+ RecSystem.sInteraction_type_name + ")='OUTBOUND'" + "  then 4 else 3 end) end) as type,"
						+ " A." + RecSystem.sInteraction_type_name + " ,A.satisfaction as satisfaction";
				String sWhere = " from call_detial A where A.party_name in ('Extension', 'Agent') and A.start_time>=:start and A.start_time<=:end   ";
						// 下面这个条件为去除未进入队列就挂断的骚扰电话
				sWhere +=  "and not(A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0 and A.result_reason!='AbandonedWhileQueued')";
				String sUId = tab.util.Util.ObjectToString(httpsession.getAttribute("uid"));
//				List<Rbacrole> groupList = new ArrayList<Rbacrole>();
//				String queuenamestr = "";
				
				java.util.List<String> agentlist=RbacSystem.GetUsersAgentsFormanageable(sUId,sGroupguid,false);
				  sWhere += " and A.employee_id in  :agentlist ";
				  sSelect+=" ,A.department as department,A.digits_num as digits_num,A.digits_num2 as digits_num2";
//				if (sGroupguid == null || sGroupguid.equals("")
//						|| sGroupguid.equals("00000000-0000-0000-0000-000000000000")) {// 没有选择组的时候查全部组,这里做的操作是拼接sql语句
//					groupList = RbacSystem.getUserDepartment(sUId);//获取所有的非技能组的组
//					if (groupList.size() == 0) {
//						result.put("list", null);
//						result.put("totalCount", 0);
//						result.put("success", true);
//						return Response.status(200).entity(result).build();
//					} else if (groupList.size() == 1) {
//						Rbacrole role = groupList.get(0);
//						queuenamestr = RbacSystem.getDeptAgents(sGroupguid);
//						sSelect += " ,(case when A.employee_id in " + queuenamestr + " then '" + role.getRolename()
//								+ "' else '' end ) as partment";
//					} else {
//						String startselect = "";
//						String endselect = "";
//						for (int i = 0; i < groupList.size(); i++) {
//							if (i == 0) {
//								Rbacrole role = groupList.get(i);
//								queuenamestr = RbacSystem.getDeptAgents(role.getRoleguid());
//								startselect += " ,(case when A.employee_id in " + queuenamestr + " then '"
//										+ role.getRolename() + "' else ";
//								endselect += "'' end ) ";
//							} else {
//								Rbacrole role = groupList.get(i);
//								queuenamestr = RbacSystem.getDeptAgents(role.getRoleguid());
//								startselect += " (case when A.employee_id in " + queuenamestr + " then '"
//										+ role.getRolename() + "' else ";
//								endselect += " end)";
//							}
//						}
//						sSelect = sSelect + startselect + endselect + " as department";
//					}
//				} else { // 当选择了组的时候
//					Rbacrole role = RbacSystem.getrole(sGroupguid);
//					queuenamestr = RbacSystem.getDeptAgents(sGroupguid);
//				    sSelect += " ,(case when A.employee_id in " + queuenamestr + " then '" + role.getRolename()
//							+ "' else '' end ) as department";
//				    sWhere += " and A.employee_id in " + queuenamestr + "";
//				}
				switch (nInbound) {
				case 1:// 呼入
					sWhere += " and  (upper(A." + RecSystem.sInteraction_type_name + ")='INBOUND' or upper(A."
							+ RecSystem.sInteraction_type_name + ")='INTERNAL')";
					break;
				case 2:// 呼出
					sWhere += " and  upper(A." + RecSystem.sInteraction_type_name + ")='OUTBOUND'";
					break;
				default:
					break;
				}
				switch (tab.util.Util.ObjectToNumber(nType, 0)) {
				case 1:// 呼入接听
					sWhere += " and A.talk_duration>0 and (upper(A." + RecSystem.sInteraction_type_name
							+ ")='INBOUND' or upper(A." + RecSystem.sInteraction_type_name + ")='INTERNAL')";
					break;
				case 2:// 呼出接听
					sWhere += " and  A.talk_duration>0 and upper(A." + RecSystem.sInteraction_type_name
							+ ")='OUTBOUND'";
					break;
				case 3:// 呼入未接
					sWhere += " and  A.talk_duration=0 and (upper(A." + RecSystem.sInteraction_type_name
							+ ")='INBOUND' or upper(A." + RecSystem.sInteraction_type_name + ")='INTERNAL')";
					break;
				case 4:// 呼出未接
					sWhere += " and  A.talk_duration=0 and upper(A." + RecSystem.sInteraction_type_name
							+ ")='OUTBOUND'";
					break;
				default:
					break;
				}
				if (sCaller != null && sCaller.length() > 0) {
					sWhere = sWhere + " and (A.source_address like :caller or A.target_address like :caller)";
				}
				if (sExtension != null && sExtension.length() > 0) {
					sWhere = sWhere + " and A.resource_name=:extension";
				}
				if (sQueques != null && sQueques.length() > 0 && !(sQueques.equals("All"))) {
					sWhere = sWhere + " and A.queues=:queues ";
				}
				
				if (sAgent != null && sAgent.length() > 0) {
					log.info("sAgent:"+sAgent);
					if (tab.util.Util.NONE_GUID.equals(sAgent)) {

					} else if (sAgent
							.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}") == false) {
						sWhere = sWhere + " and A.employee_id=:agent";
					} else {
						java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(
								RbacClient.getUserAuths(sUId, sAgent), new TypeToken<java.util.Map<String, Object>>() {
								}.getType());
						if (agentinfo != null) {
							String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
							if (agentNo.length() > 0) {
								sWhere = sWhere + " and A.employee_id=:agent";
							}
						}
					}
				}
				String sSqlStr = "select count(*) from ( select row_number() over (partition by A.media_server_ixn_guid order by A.end_time desc) as callid,A.* "
						+ sWhere + ") A where A.callid=1";
				
				SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
				query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
				query.setParameterList("agentlist", agentlist);
				if (sCaller != null && sCaller.length() > 0) {
					query.setString("caller", "%" + sCaller + "%");
				}
				if (sExtension != null && sExtension.length() > 0) {
					query.setString("extension", sExtension);
				}
				if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
					query.setString("queues", sQueques);
				}
				if (sAgent != null && sAgent.length() > 0) {
					if (tab.util.Util.NONE_GUID.equals(sAgent)) {

					} else if (sAgent
							.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}") == false) {
						query.setString("agent", sAgent);
					} else {
						java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(
								RbacClient.getUserAuths(sUId, sAgent), new TypeToken<java.util.Map<String, Object>>() {
								}.getType());
						if (agentinfo != null) {
							String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
							if (agentNo.length() > 0) {
								query.setString("agent", agentNo);
							}
						}
					}
				}
				Integer nTotalCount = tab.util.Util
						.ObjectToNumber(query.setFirstResult(0).setMaxResults(1).uniqueResult(), 0);
				if (nTotalCount >0) {
					// sSqlStr = sSelect + sWhere;
					sSqlStr = sSelect
							+ " from ( select count(*) over (partition by A.media_server_ixn_guid) as ringcount, "
							+ " row_number() over (partition by A.media_server_ixn_guid order by A.end_time desc) as callid,A.* "
							+ sWhere + ") A where A.callid=1 ";
					String sProperty = StringUtils.EMPTY;
					if (Sort != null) {
						switch (Sort.getsProperty()) {
						default:
						case "ringtime":
							sProperty = "A.start_time";
							break;
						case "begintime":
							sProperty = "A.answer_time";
							break;
						case "ani":
							sProperty = "A.source_address";
							break;
						case "dnis":
							sProperty = "A.target_address";
							break;
						case "endtime":
							sProperty = "A.end_time";
							break;
						case "type":
							sProperty = "A." + RecSystem.sInteraction_type_name;
							break;
						case "channel":
							sProperty = "A.resource_name";
							break;
						case "username":
						case "agent":
							sProperty = "A.employee_id";
							break;
						case "wait":
							sProperty = "A.ring_duration";
							break;
						case "length":
							sProperty = "A.talk_duration";
							break;
						}
					}
					if (!sProperty.isEmpty()) {
						sSqlStr += " order by " + sProperty + " " + Sort.getsDirection();
					}
					log.info(sSqlStr);
					Query queryrec = gdbsession.createSQLQuery(sSqlStr);
					queryrec.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
					queryrec.setParameterList("agentlist", agentlist);
					if (sCaller != null && sCaller.length() > 0) {
						queryrec.setString("caller", "%" + sCaller + "%");
					}
					if (sExtension != null && sExtension.length() > 0) {
						queryrec.setString("extension", sExtension);
					}
					if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
						queryrec.setString("queues", sQueques);
					}
					if (sAgent != null && sAgent.length() > 0) {
						if (tab.util.Util.NONE_GUID.equals(sAgent)) {

						} else if (sAgent
								.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}") == false) {
							queryrec.setString("agent", sAgent);
						} else {
							java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(
									RbacClient.getUserAuths(sUId, sAgent),
									new TypeToken<java.util.Map<String, Object>>() {
									}.getType());
							if (agentinfo != null) {
								String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
								if (agentNo.length() > 0) {
									queryrec.setString("agent", agentNo);
								}
							}
						}
					}
					if (sScript == null || sScript.length() < 0) {
						queryrec.setFirstResult(pageStart).setMaxResults(pageLimit);
					}
					queryrec.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = queryrec.list();
					java.util.Map<String, String> usernames = new java.util.HashMap<String, String>();
					for (int idx = 0; CallRecordList != null && idx < CallRecordList.size(); idx++) {
						java.util.Map<String, Object> item = CallRecordList.get(idx);
						String sAgentNo = tab.util.Util.ObjectToString(item.get("agent"));
						if (sAgentNo.length() > 0) {
							usernames.put(sAgentNo, "");
						}
					}
					if (usernames.size() > 0) {
						try {
							Session dbsession = HibernateSessionFactory.getThreadSession();
							try {
								@SuppressWarnings("unchecked")
								java.util.List<Object[]> agentNos = dbsession
										.createSQLQuery(
												"select agent,username from RbacUserAuths  where  agent in(:agents)")
										.setParameterList("agents", usernames.keySet()).list();
								for (int idx = 0; idx < agentNos.size(); idx++) {
									Object[] agentUsers = (Object[]) agentNos.get(idx);
									usernames.put(agentUsers[0].toString(),
											tab.util.Util.ObjectToString(agentUsers[1]));
								}
								dbsession.close();
							} catch (Throwable e) {
								log.warn("ERROR:", e);
								result.put("msg", e.toString());
							}
						} catch (Throwable e) {
							log.warn("ERROR:", e);
							result.put("msg", e.toString());
						}
					}
					for (int idx = 0; CallRecordList != null && idx < CallRecordList.size(); idx++) {
						java.util.Map<String, Object> item = CallRecordList.get(idx);
						item.put("username", usernames.get(item.get("agent")));
					}

					if (sScript == null || sScript.length() == 0) {
						result.put("list", CallRecordList);
						result.put("totalCount", nTotalCount);
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
											"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				} else {
					if (sScript == null || sScript.length() == 0) {
						result.put("list", null);
						result.put("totalCount", 0);
						result.put("success", true);
					}
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
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
	// 通快
	@POST
	@Path("/ReportQueuesCallSummary") // 呼入队列汇总
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ReportQueuesCallSummary(@Context HttpServletRequest R, @FormParam("department") String sGroupguid,
			@FormParam("starttime") RESTDateParam dtStarttime, @FormParam("endtime") RESTDateParam dtEndtime,
			@FormParam("type") Integer nType, @FormParam("bTransfer") Boolean bTransfer,@FormParam("queues") String sQueques,
			@FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
				result.put("success", false);
				String sSelect = "select  SUM( case when A.technical_result='Completed' and not(A.talk_duration=0 and upper(A.result_reason)='ABANDONEDWHILEQUEUED') then 1 else 0 end) as nInboundCount,"// 接听数
						+ "AVG(case when upper(A." + RecSystem.sInteraction_type_name
						+ ")='INBOUND' then A.sum_routing_point_duration else 0 end) as nInboundLength,"// IVR平均时长
						+ "AVG(case when upper(A." + RecSystem.sInteraction_type_name
						+ ")='INBOUND' then A.sum_mediation_duration else 0 end) as nInboundWait,"// 排队平均时长
						+ "SUM(case when not(A.technical_result='Completed'  or (A.talk_duration=0 and upper(A.result_reason)='ABANDONEDWHILEQUEUED')) and not(A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0 and upper(A.result_reason)!='ABANDONEDWHILEQUEUED')  then 1 else 0 end) as nNoAnswerCount,"// 未接听数
						+ "SUM(case when A.technical_result!='Completed'  and (A.talk_duration=0 and upper(A.result_reason)='ABANDONEDWHILEQUEUED') then 1 else 0 end) as nNoAnswerLength,"// 排队放弃数
						+ "AVG(case when A.talk_duration=0 and upper(A." + RecSystem.sInteraction_type_name
						+ ")='INBOUND' then A.sum_mediation_duration else 0 end) as nNoAnswerWait,"// 无人接听平均时长
						+ "MAX(case when upper(A." + RecSystem.sInteraction_type_name
						+ ")='INBOUND' then A.sum_routing_point_duration end) as nMaxLength,"// --最大IVR时长
						+ "MAX(case when upper(A." + RecSystem.sInteraction_type_name
						+ ")='INBOUND' then A.sum_mediation_duration end) as nMaxWaitLength";// --最大排队时长
				String sOrderBy = " order by nYear,nMonth,nDay,nHour";
				String sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time),DATEPART(hour,A.start_time)";
				String sWhere = " from call_detial A where   A.start_time>=:start and A.start_time<=:end";
				// 下面这个条件为去除未进入队列就挂断的骚扰电话
				sWhere +=  " and not(A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0 and A.result_reason!='AbandonedWhileQueued')";
				switch (nType) {
				default:
				case 0:// hour
					sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time) as nDay,DATEPART(hour,A.start_time) as nHour, 0 as nType";
					break;
				case 1:// day
					sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time)  as nDay,1 as nType";
					sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time)";
					sOrderBy = " order by nYear,nMonth,nDay";
					break;
				case 2:// week
					sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(week , A.start_time) as nWeek, 2 as nType";
					sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(week , A.start_time)";
					sOrderBy = " order by nYear,nWeek";
					break;
				case 3:// month
					sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth, 3 as nType";
					sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time)";
					sOrderBy = " order by nYear,nMonth";
					break;
				}
				if (sQueques != null && sQueques.length() > 0 && !(sQueques.equals("All"))) {
					sWhere = sWhere + " and A.queues=:queues ";
				}
				String sUId = tab.util.Util.ObjectToString(httpsession.getAttribute("uid"));
				java.util.List<String> agentlist=RbacSystem.GetUsersAgentsFormanageable(sUId,sGroupguid,false);
				  sWhere += " and A.employee_id in  :agentlist ";
				  sSelect+=" ,A.department as department";
				  sGroupBy +=" ,A.department";
//				List<Rbacrole> groupList = new ArrayList<Rbacrole>();
//				String queuenamestr = "";
//				if (sGroupguid == null || sGroupguid.equals("")
//						|| sGroupguid.equals("00000000-0000-0000-0000-000000000000")) {// 没有选择组的时候查全部组,这里做的操作是拼接sql语句
//					groupList = RbacSystem.getUserDepartment(sUId);//获取所有的非技能组的组
//					if (groupList.size() == 0) {
//						result.put("list", null);
//						result.put("totalCount", 0);
//						result.put("success", true);
//						return Response.status(200).entity(result).build();
//					} else if (groupList.size() == 1) {
//						Rbacrole role = groupList.get(0);
//						queuenamestr = RbacSystem.getDeptAgents(sGroupguid);
//						sSelect += " ,(case when A.employee_id in " + queuenamestr + " then '" + role.getRolename()
//								+ "' else '' end ) as partment";
//						sGroupBy += " ,(case when A.employee_id " + queuenamestr + " then '" + role.getRolename()
//								+ "' else '' end )";
//					} else {
//						String startselect = "";
//						String endselect = "";
//						for (int i = 0; i < groupList.size(); i++) {
//							if (i == 0) {
//								Rbacrole role = groupList.get(i);
//								queuenamestr = RbacSystem.getDeptAgents(role.getRoleguid());
//								startselect += " ,(case when A.employee_id in " + queuenamestr + " then '"
//										+ role.getRolename() + "' else ";
//								endselect += "'' end ) ";
//							} else {
//								Rbacrole role = groupList.get(i);
//								queuenamestr = RbacSystem.getDeptAgents(role.getRoleguid());
//								startselect += " (case when A.employee_id in " + queuenamestr + " then '"
//										+ role.getRolename() + "' else ";
//								endselect += " end)";
//							}
//						}
//						sGroupBy += startselect + endselect;
//						sSelect = sSelect + startselect + endselect + " as department";
//					}
//				} else { // 当选择了组的时候
//					Rbacrole role = RbacSystem.getrole(sGroupguid);
//					queuenamestr = RbacSystem.getDeptAgents(sGroupguid);
//				    sSelect += " ,(case when A.employee_id in " + queuenamestr + " then '" + role.getRolename()
//							+ "' else '' end ) as department";
//					sGroupBy += " ,(case when A.employee_id in " + queuenamestr + " then '" + role.getRolename()
//							+ "' else '' end )";
//					sWhere += " and A.employee_id in " + queuenamestr + "";
//				}
				String sSqlStr = "select count(*) from ( select row_number() over (partition by A.media_server_ixn_guid order by A.end_time desc) as callid "
						+ sWhere + ") A where A.callid=1";
				log.info(sSqlStr);
				SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
				if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
					query.setString("queues", sQueques);
				}
				query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
				query.setParameterList("agentlist", agentlist);
				Integer nTotalCount = tab.util.Util
						.ObjectToNumber(query.setFirstResult(0).setMaxResults(1).uniqueResult(), 0);
				if (nTotalCount > 0) {
					sSqlStr = sSelect
							+ " from ( select sum(A.routing_point_duration) over (partition by A.media_server_ixn_guid) as sum_routing_point_duration, sum(A.mediation_duration) over (partition by A.media_server_ixn_guid) as sum_mediation_duration, row_number() over (partition by A.media_server_ixn_guid order by A.end_time desc) as callid,A.* "
							+ sWhere + ") A where A.callid=1 " + sGroupBy + sOrderBy;
					log.info(sSqlStr);
					query = gdbsession.createSQLQuery(sSqlStr);
					if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
						query.setString("queues", sQueques);
					}
					query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					query.setParameterList("agentlist", agentlist);
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					if (sScript == null || sScript.length() == 0) {
						result.put("list", CallRecordList);
						result.put("totalCount", nTotalCount);
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
											"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				}else {
					result.put("list", null);
					result.put("totalCount", 0);
					result.put("success", true);
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
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

	// 通快 坐席满意度汇总
	@POST
	@Path("/ReportInvestigateSummaryTK")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ReportInvestigateSummaryTK(@Context HttpServletRequest R,
			@FormParam("starttime") RESTDateParam dtStarttime, @FormParam("endtime") RESTDateParam dtEndtime,
			@FormParam("type") Integer nType, @FormParam("department") String sGroupguid,
			@FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		String sWhere = " A.start_time>=:starttime and A.start_time<=:endtime  and len(A.employee_id)>0 ";

		String sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case A.SATISFACTION when 0 then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time) as nDay,DATEPART(hour , A.start_time) as nHour,A.employee_id as agentNo,  0 as nType";

		String sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time),DATEPART(hour , A.start_time),A.employee_id";
		// sGroupBy="group by A.resource_name";
		switch (nType) {
		default:
		case 0:// hour
			break;
		case 1:// day
			sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case A.SATISFACTION when 0 then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time) as nDay,A.employee_id as agentNo, 1 as nType";
			sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time),A.employee_id";

			break;
		case 2:// week
			sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case A.SATISFACTION when 0 then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(week , A.start_time) as nWeek,A.employee_id as agentNo, 2 as nType";
			sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(week , A.start_time),A.employee_id";
			break;
		case 3:// month
			sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case A.SATISFACTION when 0 then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,A.employee_id as agentNo, 3 as nType";
			sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),A.employee_id";
			break;
		}
		try {
			Session dbsession = GHibernateSessionFactory.getThreadSession();
			try {
				String sUId = tab.util.Util.ObjectToString(httpsession.getAttribute("uid"));
				java.util.Set<String> agents = new java.util.HashSet<String>();
				agents = RbacClient.getUserAgents(sUId, sGroupguid, false);
				java.util.Map<String, Object> agentinfo = GsonUtil.getInstance()
						.fromJson(RbacClient.getUserAuths(sUId, sUId), new TypeToken<java.util.Map<String, Object>>() {
						}.getType());
				if (agentinfo != null) {
					String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
					if (agentNo.length() > 0)
						agents.add(agentNo);
				}
				if (agents != null && agents.size() > 0) {
					sWhere += " and A.employee_id in :agents";
				} else {
					result.put("list", null);
					result.put("totalCount", 0);
					result.put("success", true);
					return Response.status(200).entity(result).build();
				}

				String querystr = sSelect + " from call_detial A where " + sWhere + sGroupBy;
				log.info(querystr);
				Query query = dbsession.createSQLQuery(querystr).setTimestamp("starttime", dtStarttime.getDate())
						.setTimestamp("endtime", dtEndtime.getDate());
				if (agents != null && agents.size() > 0) {
					query.setParameterList("agents", agents);
				}
				query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
				@SuppressWarnings("unchecked")
				java.util.List<java.util.Map<String, Object>> InvestigateResultsList = query.list();
				java.util.Set<String> AgentList = new java.util.HashSet<>();
				for (int i = 0; i < InvestigateResultsList.size(); i++) {
					AgentList.add(Util.ObjectToString(InvestigateResultsList.get(i).get("agentNo")));
				}
				java.util.Map<String, String> AgentMap = new java.util.HashMap<String, String>();
					if (AgentList.size() > 0) {
						Session calldbsession = HibernateSessionFactory.getThreadSession();
						try {
							@SuppressWarnings("unchecked")
							java.util.List<Object[]> maps = (java.util.List<Object[]>) calldbsession.createQuery(
									"select agent as agent, username as username from Rbacuserauths where agent in(:agent)")
									.setParameterList("agent", AgentList).list();
							for (Object[] map : maps) {
								if(map[0].toString().length()>0) {
									AgentMap.put(map[0].toString(), map[1].toString());
								}
							}
							
						}catch(Throwable e){
							log.error("Error:callhibernateerror"+e);
						}
						calldbsession.close();
					}
			
				for (int i = 0; i < InvestigateResultsList.size(); i++) {
					java.util.Map<String, Object> call = InvestigateResultsList.get(i);
					call.put("username", AgentMap.get(call.get("agentno")));
				}

				if (sScript == null || sScript.length() == 0) {
					result.put("list", InvestigateResultsList);
					result.put("success", true);
				} else {
					tab.MyExportExcelFile export = null;
					if (sScript == null || sScript.length() == 0) {
					} else {
						export = new tab.MyExportExcelFile();
						if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator")
								+ "WebRoot" + sScript)) {
							dbsession.close();
							return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain").build();
						}
					}
					for (int i = 0; i < InvestigateResultsList.size(); i++) {
						java.util.Map<String, Object> call = InvestigateResultsList.get(i);
						if (sScript == null || sScript.length() == 0) {
						} else {
							export.CommitRow(call);
						}
					}
					String sFileName = export.GetFile();
					if (sFileName != null && sFileName.length() > 0) {
						java.io.File f = new java.io.File(sFileName);
						int pos = sFileName.lastIndexOf(".");
						dbsession.close();
						return Response.ok(f).header(HttpHeaders.CONTENT_TYPE, "application/csv;charset=UTF-8")
								.header("Content-Disposition",
										"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
												+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
														.format(Calendar.getInstance().getTime())
														.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
												+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
								.build();
					}
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		if (sScript == null || sScript.length() == 0) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(404).entity("no datas").type("text/plain").build();
	}

	// 通快 部门满意度汇总
	@POST
	@Path("/reportDeptInvestigateSummaryTK")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response reportDeptInvestigateSummaryTK(@Context HttpServletRequest R,
			@FormParam("starttime") RESTDateParam dtStarttime, @FormParam("endtime") RESTDateParam dtEndtime,
			@FormParam("type") Integer nType, @FormParam("script") String sScript,
			@FormParam("department") String sGroupguid) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}

		String sWhere = " A.start_time>=:starttime and A.start_time<=:endtime  and len(A.employee_id)>0 ";
		String sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case A.SATISFACTION when 0 then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time) as nDay,DATEPART(hour , A.start_time) as nHour, 0 as nType";

		String sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time),DATEPART(hour , A.start_time)";
		switch (nType) {
		default:
		case 0:// hour
			break;
		case 1:// day
			sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case A.SATISFACTION when 0 then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time) as nDay, 1 as nType";
			sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time)";

			break;
		case 2:// week
			sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case A.SATISFACTION when 0 then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(week , A.start_time) as nWeek, 2 as nType";
			sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(week , A.start_time)";
			break;
		case 3:// month
			sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case A.SATISFACTION when 0 then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth, 3 as nType";
			sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time)";
			break;
		}
		try {
			Session dbsession = GHibernateSessionFactory.getThreadSession();
			try {
				String sUId = tab.util.Util.ObjectToString(httpsession.getAttribute("uid"));
				java.util.List<String> agentlist=RbacSystem.GetUsersAgentsFormanageable(sUId,sGroupguid,false);
				  sWhere += " and A.employee_id in  :agentlist ";
				  sSelect+=" ,A.department as department";
				  sGroupBy +=" ,A.department";
			//	List<Rbacrole> groupList = new ArrayList<Rbacrole>();
//				String queuenamestr = "";
//				if (sGroupguid == null || sGroupguid.equals("")
//						|| sGroupguid.equals("00000000-0000-0000-0000-000000000000")) {// 没有选择组的时候查全部组,这里做的操作是拼接sql语句
//					groupList = RbacSystem.getUserDepartment(sUId);//获取所有的非技能组的组
//					if (groupList.size() == 0) {
//						result.put("list", null);
//						result.put("totalCount", 0);
//						result.put("success", true);
//						return Response.status(200).entity(result).build();
//					} else if (groupList.size() == 1) {
//						Rbacrole role = groupList.get(0);
//						queuenamestr = RbacSystem.getDeptAgents(sGroupguid);
//						sSelect += " ,(case when A.employee_id in " + queuenamestr + " then '" + role.getRolename()
//								+ "' else '' end ) as partment";
//						sGroupBy += " ,(case when A.employee_id " + queuenamestr + " then '" + role.getRolename()
//								+ "' else '' end )";
//					} else {
//						String startselect = "";
//						String endselect = "";
//						for (int i = 0; i < groupList.size(); i++) {
//							if (i == 0) {
//								Rbacrole role = groupList.get(i);
//								queuenamestr = RbacSystem.getDeptAgents(role.getRoleguid());
//								startselect += " ,(case when A.employee_id in " + queuenamestr + " then '"
//										+ role.getRolename() + "' else ";
//								endselect += "'' end ) ";
//							} else {
//								Rbacrole role = groupList.get(i);
//								queuenamestr = RbacSystem.getDeptAgents(role.getRoleguid());
//								startselect += " (case when A.employee_id in " + queuenamestr + " then '"
//										+ role.getRolename() + "' else ";
//								endselect += " end)";
//							}
//						}
//						sGroupBy += startselect + endselect;
//						sSelect = sSelect + startselect + endselect + " as department";
//					}
//				} else { // 当选择了组的时候
//					Rbacrole role = RbacSystem.getrole(sGroupguid);
//					queuenamestr = RbacSystem.getDeptAgents(sGroupguid);
//				    sSelect += " ,(case when A.employee_id in " + queuenamestr + " then '" + role.getRolename()
//							+ "' else '' end ) as department";
//					sGroupBy += " ,(case when A.employee_id in " + queuenamestr + " then '" + role.getRolename()
//							+ "' else '' end )";
//					sWhere += " and A.employee_id in " + queuenamestr + "";
//				}
				String querystr = sSelect + " from call_detial A where " + sWhere + sGroupBy;
				log.info(querystr);
				Query query = dbsession.createSQLQuery(querystr).setTimestamp("starttime", dtStarttime.getDate())
						.setTimestamp("endtime", dtEndtime.getDate());
				query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
				query.setParameterList("agentlist", agentlist);
				@SuppressWarnings("unchecked")
				java.util.List<java.util.Map<String, Object>> InvestigateResultsList = query.list();
				java.util.Set<String> AgentList = new java.util.HashSet<>();
				for (int i = 0; i < InvestigateResultsList.size(); i++) {
					AgentList.add(Util.ObjectToString(InvestigateResultsList.get(i).get("agentNo")));
				}
				java.util.Map<String, String> AgentMap = new java.util.HashMap<String, String>();
				if (AgentList.size() > 0) {
					Session calldbsession = HibernateSessionFactory.getThreadSession();
					try {
						@SuppressWarnings("unchecked")
						java.util.List<Object[]> maps = (java.util.List<Object[]>) calldbsession.createQuery(
								"select agent as agent, username as username from Rbacuserauths where agent in(:agent)")
								.setParameterList("agent", AgentList).list();
						for (Object[] map : maps) {
							AgentMap.put(map[0].toString(), map[1].toString());
						}
						
					}catch(Throwable e){
						log.error("Error:callhibernateerror"+e);
					}
					calldbsession.close();
				}else {
					result.put("list", null);
					result.put("totalCount", 0);
					result.put("success", true);
				}
				tab.MyExportExcelFile export = null;

				if (sScript == null || sScript.length() == 0) {

				} else {
					export = new tab.MyExportExcelFile();
					if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator") + "WebRoot"
							+ sScript)) {
						dbsession.close();
						return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain").build();
					}
				}
				if (sScript == null || sScript.length() == 0) {
					for (int i = 0; i < InvestigateResultsList.size(); i++) {
						java.util.Map<String, Object> call = InvestigateResultsList.get(i);
						call.put("username", AgentMap.get(call.get("agentNo")));
					}
				} else {
					for (int i = 0; i < InvestigateResultsList.size(); i++) {
						java.util.HashMap<String, Object> call = (HashMap<String, Object>) InvestigateResultsList
								.get(i);
						call.put("username", AgentMap.get(call.get("agentNo")));
						export.CommitRow(call);
					}
				}
				if (sScript == null || sScript.length() == 0) {
					result.put("list", InvestigateResultsList);
					result.put("success", true);
				} else {
					String sFileName = export.GetFile();
					if (sFileName != null && sFileName.length() > 0) {
						java.io.File f = new java.io.File(sFileName);
						int pos = sFileName.lastIndexOf(".");
						dbsession.close();
						return Response.ok(f)
								.header("Content-Disposition",
										"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
												+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
														.format(Calendar.getInstance().getTime())
														.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
												+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
								.build();
					}
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		if (sScript == null || sScript.length() == 0) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(404).entity("no datas").type("text/plain").build();
	}

	
	// 通快
		@POST
		@Path("/ReportDeptDetailByqueues") // 坐席通话明细  通过记录所属队列区分部门
		@Consumes("application/x-www-form-urlencoded") 
		@Produces({ MediaType.APPLICATION_JSON })
		public Response ReportDeptDetailByqueues(@Context HttpServletRequest R, @FormParam("starttime") RESTDateParam dtStarttime,
				@FormParam("endtime") RESTDateParam dtEndtime, @FormParam("agent") String sAgent,
				@FormParam("extension") String sExtension, @FormParam("caller") String sCaller,
				@FormParam("inbound") Integer nInbound, @FormParam("nType") Integer nType,
				@FormParam("weekend") Integer nWeekend, @FormParam("length") Integer nLength,
				@FormParam("department") String sGroupguid, @FormParam("start") Integer pageStart,
				@FormParam("limit") Integer pageLimit, @FormParam("sort") EXTJSSortParam Sort,@FormParam("queues") String sQueques,
				@FormParam("firstdigits") String firstdigits,@FormParam("seconddigits") String seconddigits,
				@FormParam("script") String sScript) {
			java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
			result.put("success", false);
			HttpSession httpsession = R.getSession();
			if (httpsession == null) {
				log.error("ERROR: getSession() is null");
				return Response.status(200).entity(result).build();
			}
			if (pageStart == null)
				pageStart = 0;
			if (pageLimit == null)
				pageLimit = 1000;
			if (nInbound == null)
				nInbound = 0;
			if (nWeekend == null)
				nWeekend = 0;
			if (nLength == null)
				nLength = 0;

			try {
				Session gdbsession = GHibernateSessionFactory.getThreadSession();
				try {
					String sSelect = "select A.start_time as ringtime, case when A.answer_time is null then A.start_time else A.answer_time end as begintime, A.end_time as endtime,A.resource_name as channel, "
							+ " A.source_address as ani,A.target_address as dnis,A.employee_id as agent,A.talk_duration as length, A.ring_duration as wait,A.queues as queues,A.TARGET_INFO as targetinfo,"
							+ " (case when A.talk_duration>0 then (case when upper(A." + RecSystem.sInteraction_type_name
							+ ")='OUTBOUND'" + "  then 2 else 1 end) else (case when upper(A."
							+ RecSystem.sInteraction_type_name + ")='OUTBOUND'" + "  then 4 else 3 end) end) as type,"
							+ " A." + RecSystem.sInteraction_type_name + " ,A.satisfaction as satisfaction,A.AGENT_LAST_NAME as username";
					String sWhere = " from call_detial A where A.party_name in ('Extension', 'Agent') and A.start_time>=:start and A.start_time<=:end   ";
							// 下面这个条件为去除未进入队列就挂断的骚扰电话,痛快客户说不需要去除骚扰电话
					//sWhere +=  "and not(A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0 and A.result_reason!='AbandonedWhileQueued')";
					
					String sUId = tab.util.Util.ObjectToString(httpsession.getAttribute("uid"));
					List<String> groupList = new ArrayList<String>();
					groupList=RbacSystem.getUserDepartmentName(sUId,sGroupguid);
					log.info("呼入明细输出用户管理组groupList:"+groupList.toString());
					  sWhere += " and A.department in  :groupList ";
					  sSelect+=" ,A.department as department,A.digits_num as digits_num,A.digits_num2 as digits_num2";
					switch (nInbound) {
					case 1:// 呼入
						sWhere += " and  (upper(A." + RecSystem.sInteraction_type_name + ")='INBOUND' or upper(A."
								+ RecSystem.sInteraction_type_name + ")='INTERNAL')";
						break;
					case 2:// 呼出
						sWhere += " and  upper(A." + RecSystem.sInteraction_type_name + ")='OUTBOUND'";
						break;
					default:
						break;
					}
					
					switch (tab.util.Util.ObjectToNumber(nType, 0)) {
					case 1:// 呼入接听
						sWhere += " and A.talk_duration>0 and (upper(A." + RecSystem.sInteraction_type_name
								+ ")='INBOUND' or upper(A." + RecSystem.sInteraction_type_name + ")='INTERNAL')";
						break;
					case 2:// 呼出接听
						sWhere += " and  A.talk_duration>0 and upper(A." + RecSystem.sInteraction_type_name
								+ ")='OUTBOUND'";
						break;
					case 3:// 呼入未接
						sWhere += " and  A.talk_duration=0 and (upper(A." + RecSystem.sInteraction_type_name
								+ ")='INBOUND' or upper(A." + RecSystem.sInteraction_type_name + ")='INTERNAL')";
						break;
					case 4:// 呼出未接
						sWhere += " and  A.talk_duration=0 and upper(A." + RecSystem.sInteraction_type_name
								+ ")='OUTBOUND'";
						break;
					default:
						break;
					}
					if (sCaller != null && sCaller.length() > 0) {
						sWhere = sWhere + " and (A.source_address like :caller or A.target_address like :caller)";
					}
					if (sExtension != null && sExtension.length() > 0) {
						sWhere = sWhere + " and A.resource_name=:extension";
					}
					if (sQueques != null && sQueques.length() > 0 && !(sQueques.equals("All"))) {
						sWhere = sWhere + " and A.queues in :queues ";
					}
					if(firstdigits!=null&&firstdigits.length()>0) {
						sWhere = sWhere + " and A.digits_num in :firstdigits";
					}
					if(seconddigits!=null&&seconddigits.length()>0) {
						sWhere = sWhere + " and A.digits_num2 in :seconddigits";
					}
					if (sAgent != null && sAgent.length() > 0) {
						log.info("sAgent:"+sAgent);
						if (tab.util.Util.NONE_GUID.equals(sAgent)) {

						} else if (sAgent
								.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}") == false) {
							sWhere = sWhere + " and A.employee_id=:agent";
						} else {
							java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(
									RbacClient.getUserAuths(sUId, sAgent), new TypeToken<java.util.Map<String, Object>>() {
									}.getType());
							if (agentinfo != null) {
								String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
								if (agentNo.length() > 0) {
									sWhere = sWhere + " and A.employee_id=:agent";
								}
							}
						}
					}
					String sSqlStr = "select count(*) from ( select row_number() over (partition by A.media_server_ixn_guid order by A.end_time desc) as callid,A.* "
							+ sWhere + ") A where A.callid=1";
					log.info("输出查询数量sql:"+sSqlStr);
					SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
					query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
					query.setParameterList("groupList", groupList);
					if (sCaller != null && sCaller.length() > 0) {
						query.setString("caller", "%" + sCaller + "%");
					}
					if (sExtension != null && sExtension.length() > 0) {
						query.setString("extension", sExtension);
					}
					if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
						java.util.List<String> queueslist = Arrays.asList(sQueques.split(","));
						log.info("输出选择队列:"+queueslist.toString());
						query.setParameterList("queues", queueslist);
					}
					if (firstdigits != null && firstdigits.length() > 0) {
						java.util.List<String> firstdigitslist = Arrays.asList(firstdigits.split(","));
						query.setParameterList("firstdigits", firstdigitslist);
					}
					if (seconddigits != null && seconddigits.length() > 0) {
						java.util.List<String> seconddigitslist = Arrays.asList(seconddigits.split(","));
						query.setParameterList("seconddigits", seconddigitslist);
					}
					if (sAgent != null && sAgent.length() > 0) {
						if (tab.util.Util.NONE_GUID.equals(sAgent)) {

						} else if (sAgent
								.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}") == false) {
							query.setString("agent", sAgent);
						} else {
							java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(
									RbacClient.getUserAuths(sUId, sAgent), new TypeToken<java.util.Map<String, Object>>() {
									}.getType());
							if (agentinfo != null) {
								String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
								if (agentNo.length() > 0) {
									query.setString("agent", agentNo);
								}
							}
						}
					}
					Integer nTotalCount = tab.util.Util
							.ObjectToNumber(query.setFirstResult(0).setMaxResults(1).uniqueResult(), 0);
					if (nTotalCount >0) {
						// sSqlStr = sSelect + sWhere;
						sSqlStr = sSelect
								+ " from ( select count(*) over (partition by A.media_server_ixn_guid) as ringcount, "
								+ " row_number() over (partition by A.media_server_ixn_guid order by A.end_time desc) as callid,A.* "
								+ sWhere + ") A where A.callid=1 ";
						String sProperty = StringUtils.EMPTY;
						if (Sort != null) {
							switch (Sort.getsProperty()) {
							default:
							case "ringtime":
								sProperty = "A.start_time";
								break;
							case "begintime":
								sProperty = "A.answer_time";
								break;
							case "ani":
								sProperty = "A.source_address";
								break;
							case "dnis":
								sProperty = "A.target_address";
								break;
							case "endtime":
								sProperty = "A.end_time";
								break;
							case "type":
								sProperty = "A." + RecSystem.sInteraction_type_name;
								break;
							case "channel":
								sProperty = "A.resource_name";
								break;
							case "username":
							case "agent":
								sProperty = "A.employee_id";
								break;
							case "wait":
								sProperty = "A.ring_duration";
								break;
							case "length":
								sProperty = "A.talk_duration";
								break;
							}
						}
						if (!sProperty.isEmpty()) {
							sSqlStr += " order by " + sProperty + " " + Sort.getsDirection();
						}
						log.info(sSqlStr);
						Query queryrec = gdbsession.createSQLQuery(sSqlStr);
						queryrec.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
						queryrec.setParameterList("groupList", groupList);
						if (sCaller != null && sCaller.length() > 0) {
							queryrec.setString("caller", "%" + sCaller + "%");
						}
						if (sExtension != null && sExtension.length() > 0) {
							queryrec.setString("extension", sExtension);
						}
						if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
							java.util.List<String> queueslist = Arrays.asList(sQueques.split(","));
							queryrec.setParameterList("queues", queueslist);
						}
						if (firstdigits != null && firstdigits.length() > 0) {
							java.util.List<String> firstdigitslist = Arrays.asList(firstdigits.split(","));
							queryrec.setParameterList("firstdigits", firstdigitslist);
						}
						if (seconddigits != null && seconddigits.length() > 0) {
							java.util.List<String> seconddigitslist = Arrays.asList(seconddigits.split(","));
							queryrec.setParameterList("seconddigits", seconddigitslist);
						}
						if (sAgent != null && sAgent.length() > 0) {
							if (tab.util.Util.NONE_GUID.equals(sAgent)) {

							} else if (sAgent
									.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}") == false) {
								queryrec.setString("agent", sAgent);
							} else {
								java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(
										RbacClient.getUserAuths(sUId, sAgent),
										new TypeToken<java.util.Map<String, Object>>() {
										}.getType());
								if (agentinfo != null) {
									String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
									if (agentNo.length() > 0) {
										queryrec.setString("agent", agentNo);
									}
								}
							}
						}
						if (sScript == null || sScript.length() < 0) {
							queryrec.setFirstResult(pageStart).setMaxResults(pageLimit);
						}
						queryrec.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
						@SuppressWarnings("unchecked")
						java.util.List<java.util.Map<String, Object>> CallRecordList = queryrec.list();
						if (sScript == null || sScript.length() == 0) {
							result.put("list", CallRecordList);
							result.put("totalCount", nTotalCount);
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
												"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
														+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
																.format(Calendar.getInstance().getTime())
																.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
														+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
										.build();
							}
						}
					} else {
						if (sScript == null || sScript.length() == 0) {
							result.put("list", null);
							result.put("totalCount", 0);
							result.put("success", true);
						}
					}
				} catch (org.hibernate.HibernateException e) {
					log.warn("ERROR:", e);
					result.put("msg", e.toString());
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
		
		// 通快
		@POST
		@Path("/ReportQueuesCallSummaryByQueues") // 呼入队列汇总  通过队列所属部门区分记录所属部门
		@Consumes("application/x-www-form-urlencoded")
		@Produces("application/json" + ";charset=utf-8")
		public Response ReportQueuesCallSummaryByQueues(@Context HttpServletRequest R, @FormParam("department") String sGroupguid,
				@FormParam("starttime") RESTDateParam dtStarttime, @FormParam("endtime") RESTDateParam dtEndtime,
				@FormParam("type") Integer nType, @FormParam("bTransfer") Boolean bTransfer,@FormParam("queues") String sQueques,
				@FormParam("firstdigits") String firstdigits,@FormParam("seconddigits") String seconddigits,
				@FormParam("script") String sScript) {
			java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
			HttpSession httpsession = R.getSession();
			if (httpsession == null) {
				log.error("ERROR: getSession() is null");
				return Response.status(200).entity(result).build();
			}
			try {
				Session gdbsession = GHibernateSessionFactory.getThreadSession();
				try {
					result.put("success", false);
				//	String sSelect = "select  SUM( case when A.technical_result='Completed' and not(A.talk_duration=0 and upper(A.result_reason)='ABANDONEDWHILEQUEUED') then 1 else 0 end) as nInboundCount,"// 接听数
							String sSelect = "select  SUM( case when A.talk_duration>0  then 1 else 0 end) as nInboundCount,"// 接听数
							+ "AVG(case when upper(A." + RecSystem.sInteraction_type_name
							+ ")='INBOUND' then A.sum_routing_point_duration else 0 end) as nInboundLength,"// IVR平均时长
							+ "AVG(case when upper(A." + RecSystem.sInteraction_type_name
							+ ")='INBOUND' then A.sum_mediation_duration else 0 end) as nInboundWait,"// 排队平均时长
//							+ "SUM(case when not(A.technical_result='Completed'  or (A.talk_duration=0 and upper(A.result_reason)='ABANDONEDWHILEQUEUED'))  then 1 else 0 end) as nNoAnswerCount,"// 未接听数 and not(A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0 and upper(A.result_reason)!='ABANDONEDWHILEQUEUED') 
//							+ "SUM(case when A.technical_result!='Completed' and upper(A.queues)!='NONE' and (A.talk_duration=0 and upper(A.result_reason)='ABANDONEDWHILEQUEUED') then 1 else 0 end) as nNoAnswerLength,"// 排队放弃数
//							+ "SUM(case when A.technical_result!='Completed' and upper(A.queues)='NONE' and (A.talk_duration=0 and upper(A.result_reason)='ABANDONEDWHILEQUEUED') then 1 else 0 end) as nNoIvrAnswerLength,"// ivr放弃数
							
							+ "SUM(case when (A.talk_duration=0 and A.employee_id is not null)  then 1 else 0 end) as nNoAnswerCount,"// 未接听数 and not(A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0 and upper(A.result_reason)!='ABANDONEDWHILEQUEUED') 
							+ "SUM(case when (A.talk_duration=0 and A.employee_id is  null and upper(A.queues)!='NONE') then 1 else 0 end) as nNoAnswerLength,"// 排队放弃数
							+ "SUM(case when (A.talk_duration=0 and A.employee_id is  null and upper(A.queues)='NONE') then 1 else 0 end) as nNoIvrAnswerLength,"// ivr放弃数
							
							
							+ "AVG(case when A.talk_duration=0 and upper(A." + RecSystem.sInteraction_type_name
							+ ")='INBOUND' then A.sum_mediation_duration else 0 end) as nNoAnswerWait,"// 无人接听平均时长
							+ "MAX(case when upper(A." + RecSystem.sInteraction_type_name
							+ ")='INBOUND' then A.sum_routing_point_duration end) as nMaxLength,"// --最大IVR时长
							+ "MAX(case when upper(A." + RecSystem.sInteraction_type_name
							+ ")='INBOUND' then A.sum_mediation_duration end) as nMaxWaitLength";// --最大排队时长
					String sOrderBy = " order by nYear,nMonth,nDay,nHour";
					String sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time),DATEPART(hour,A.start_time)";
					String sWhere = " from call_detial A where   A.start_time>=:start and A.start_time<=:end";
					// 下面这个条件为去除未进入队列就挂断的骚扰电话,通快不再需要去除骚扰电话
				//	sWhere +=  " and not(A.routing_point_duration>=0 and A.mediation_duration=0 and A.talk_duration=0 and A.result_reason!='AbandonedWhileQueued')";
					switch (nType) {
					default:
					case 0:// hour
						sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time) as nDay,DATEPART(hour,A.start_time) as nHour, 0 as nType";
						break;
					case 1:// day
						sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time)  as nDay,1 as nType";
						sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time)";
						sOrderBy = " order by nYear,nMonth,nDay";
						break;
					case 2:// week
						sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(week , A.start_time) as nWeek, 2 as nType";
						sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(week , A.start_time)";
						sOrderBy = " order by nYear,nWeek";
						break;
					case 3:// month
						sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth, 3 as nType";
						sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time)";
						sOrderBy = " order by nYear,nMonth";
						break;
					}
					if (sQueques != null && sQueques.length() > 0 && !(sQueques.equals("All"))) {
						sWhere = sWhere + " and A.queues in :queues ";
					}
					if(firstdigits!=null&&firstdigits.length()>0) {
						sWhere = sWhere + " and A.digits_num in :firstdigits";
					}
					if(seconddigits!=null&&seconddigits.length()>0) {
						sWhere = sWhere + " and A.digits_num2 in :seconddigits";
					}
					String sUId = tab.util.Util.ObjectToString(httpsession.getAttribute("uid"));
						List<String> groupList = new ArrayList<String>();
						groupList=RbacSystem.getUserDepartmentName(sUId,sGroupguid);
						log.info("呼入汇总输出部门列表:"+groupList.toString());
						  sWhere += " and A.department in  :groupList ";
					  sSelect+=" ,A.department as department";
					  sGroupBy +=" ,A.department";
					String sSqlStr = "select count(*) from ( select row_number() over (partition by A.media_server_ixn_guid order by A.end_time desc) as callid "
							+ sWhere + ") A where A.callid=1";
					log.info(sSqlStr);
					SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
					if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
						java.util.List<String> queueslist = Arrays.asList(sQueques.split(","));
						query.setParameterList("queues", queueslist);
					}
					if (firstdigits != null && firstdigits.length() > 0) {
						java.util.List<String> firstdigitslist = Arrays.asList(firstdigits.split(","));
						query.setParameterList("firstdigits", firstdigitslist);
					}
					if (seconddigits != null && seconddigits.length() > 0) {
						java.util.List<String> seconddigitslist = Arrays.asList(seconddigits.split(","));
						query.setParameterList("seconddigits", seconddigitslist);
					}
					query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
					query.setParameterList("groupList", groupList);
					Integer nTotalCount = tab.util.Util
							.ObjectToNumber(query.setFirstResult(0).setMaxResults(1).uniqueResult(), 0);
					if (nTotalCount > 0) {
						sSqlStr = sSelect
								+ " from ( select sum(A.routing_point_duration) over (partition by A.media_server_ixn_guid) as sum_routing_point_duration, sum(A.mediation_duration) over (partition by A.media_server_ixn_guid) as sum_mediation_duration, row_number() over (partition by A.media_server_ixn_guid order by A.end_time desc) as callid,A.* "
								+ sWhere + ") A where A.callid=1 " + sGroupBy + sOrderBy;
						log.info(sSqlStr);
						query = gdbsession.createSQLQuery(sSqlStr);
						if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
							java.util.List<String> queueslist = Arrays.asList(sQueques.split(","));
							query.setParameterList("queues", queueslist);
						}
						if (firstdigits != null && firstdigits.length() > 0) {
							java.util.List<String> firstdigitslist = Arrays.asList(firstdigits.split(","));
							query.setParameterList("firstdigits", firstdigitslist);
						}
						if (seconddigits != null && seconddigits.length() > 0) {
							java.util.List<String> seconddigitslist = Arrays.asList(seconddigits.split(","));
							query.setParameterList("seconddigits", seconddigitslist);
						}
						query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
						query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
						query.setParameterList("groupList", groupList);
						@SuppressWarnings("unchecked")
						java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
						if (sScript == null || sScript.length() == 0) {
							result.put("list", CallRecordList);
							result.put("totalCount", nTotalCount);
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
												"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
														+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
																.format(Calendar.getInstance().getTime())
																.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
														+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
										.build();
							}
						}
					}else {
						result.put("list", null);
						result.put("totalCount", 0);
						result.put("success", true);
					}
				} catch (org.hibernate.HibernateException e) {
					log.warn("ERROR:", e);
					result.put("msg", e.toString());
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
		
		
		
		// 通快 坐席满意度汇总 通过队列所属部门区分记录所属部门  添加多选队列查询
		@POST
		@Path("/ReportInvestigateSummaryTKByQueues")
		@Consumes("application/x-www-form-urlencoded")
		@Produces("application/json" + ";charset=utf-8")
		public Response ReportInvestigateSummaryTKByQueues(@Context HttpServletRequest R,
				@FormParam("starttime") RESTDateParam dtStarttime, @FormParam("endtime") RESTDateParam dtEndtime,
				@FormParam("type") Integer nType, @FormParam("department") String sGroupguid,
				@FormParam("firstdigits") String firstdigits,@FormParam("seconddigits") String seconddigits,
				@FormParam("script") String sScript,@FormParam("queues") String sQueques) {
			java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
			result.put("success", false);
			HttpSession httpsession = R.getSession();
			if (httpsession == null) {
				log.error("ERROR: getSession() is null");
				return Response.status(200).entity(result).build();
			}
			String sWhere = " A.start_time>=:starttime and A.start_time<=:endtime  and len(A.employee_id)>0 ";

			String sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case  when (A.SATISFACTION is  NULL or A.SATISFACTION=0) then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time) as nDay,DATEPART(hour , A.start_time) as nHour,A.employee_id as agentNo,  0 as nType";

			String sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time),DATEPART(hour , A.start_time),A.employee_id";
			String sUId = tab.util.Util.ObjectToString(httpsession.getAttribute("uid"));
			List<String> grouplist = new ArrayList<String>();
			grouplist=RbacSystem.getUserDepartmentName(sUId,sGroupguid);
			log.info("坐席满意度汇总输出部门列表:"+grouplist.toString());
			
			sWhere+=" and A.department in :grouplist";
			if (sQueques != null && sQueques.length() > 0 && !(sQueques.equals("All"))) {
				sWhere = sWhere + " and A.queues in :queues ";
			}
			if(firstdigits!=null&&firstdigits.length()>0) {
				sWhere = sWhere + " and A.digits_num in :firstdigits";
			}
			if(seconddigits!=null&&seconddigits.length()>0) {
				sWhere = sWhere + " and A.digits_num2 in :seconddigits";
			}
			switch (nType) {
			default:
			case 0:// hour
				break;
			case 1:// day
				sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case when (A.SATISFACTION is  NULL or A.SATISFACTION=0) then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time) as nDay,A.employee_id as agentNo, 1 as nType";
				sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time),A.employee_id";

				break;
			case 2:// week
				sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case when (A.SATISFACTION is  NULL or A.SATISFACTION=0) then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(week , A.start_time) as nWeek,A.employee_id as agentNo, 2 as nType";
				sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(week , A.start_time),A.employee_id";
				break;
			case 3:// month
				sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case when (A.SATISFACTION is  NULL or A.SATISFACTION=0) then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,A.employee_id as agentNo, 3 as nType";
				sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),A.employee_id";
				break;
			}
			try {
				Session dbsession = GHibernateSessionFactory.getThreadSession();
				try {
					String querystr = sSelect + " from call_satisf A where " + sWhere + sGroupBy;
					log.info(querystr);
					Query query = dbsession.createSQLQuery(querystr).setTimestamp("starttime", dtStarttime.getDate())
							.setTimestamp("endtime", dtEndtime.getDate());
					if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
						java.util.List<String> queueslist = Arrays.asList(sQueques.split(","));
						query.setParameterList("queues", queueslist);
					}
					query.setParameterList("grouplist", grouplist);
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					if (firstdigits != null && firstdigits.length() > 0) {
						java.util.List<String> firstdigitslist = Arrays.asList(firstdigits.split(","));
						query.setParameterList("firstdigits", firstdigitslist);
					}
					if (seconddigits != null && seconddigits.length() > 0) {
						java.util.List<String> seconddigitslist = Arrays.asList(seconddigits.split(","));
						query.setParameterList("seconddigits", seconddigitslist);
					}
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> InvestigateResultsList = query.list();
					if (sScript == null || sScript.length() == 0) {
						result.put("list", InvestigateResultsList);
						result.put("totalCount", InvestigateResultsList.size());
						result.put("success", true);
					} else {
						tab.MyExportExcelFile export = null;
						if (sScript == null || sScript.length() == 0) {
						} else {
							export = new tab.MyExportExcelFile();
							if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator")
									+ "WebRoot" + sScript)) {
								dbsession.close();
								return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain").build();
							}
						}
						for (int i = 0; i < InvestigateResultsList.size(); i++) {
							java.util.Map<String, Object> call = InvestigateResultsList.get(i);
							if (sScript == null || sScript.length() == 0) {
							} else {
								export.CommitRow(call);
							}
						}
						String sFileName = export.GetFile();
						if (sFileName != null && sFileName.length() > 0) {
							java.io.File f = new java.io.File(sFileName);
							int pos = sFileName.lastIndexOf(".");
							dbsession.close();
							return Response.ok(f).header(HttpHeaders.CONTENT_TYPE, "application/csv;charset=UTF-8")
									.header("Content-Disposition",
											"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				} catch (Throwable e) {
					log.warn("ERROR:", e);
					result.put("msg", e.toString());
				}
				dbsession.close();
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			if (sScript == null || sScript.length() == 0) {
				return Response.status(200).entity(result).build();
			}
			return Response.status(404).entity("no datas").type("text/plain").build();
		}
		
		// 通快 部门满意度汇总 通过队列区分记录所属部门  添加多选队列查询
		@POST
		@Path("/reportDeptInvestigateSummaryTKByQueues")
		@Consumes("application/x-www-form-urlencoded")
		@Produces("application/json" + ";charset=utf-8")
		public Response reportDeptInvestigateSummaryTKByQueues(@Context HttpServletRequest R,
				@FormParam("starttime") RESTDateParam dtStarttime, @FormParam("endtime") RESTDateParam dtEndtime,
				@FormParam("type") Integer nType, @FormParam("script") String sScript,
				@FormParam("firstdigits") String firstdigits,@FormParam("seconddigits") String seconddigits,
				@FormParam("department") String sGroupguid,@FormParam("queues") String sQueques) {
			java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
			result.put("success", false);
			HttpSession httpsession = R.getSession();
			if (httpsession == null) {
				log.error("ERROR: getSession() is null");
				return Response.status(200).entity(result).build();
			}

			String sWhere = " A.start_time>=:starttime and A.start_time<=:endtime  and len(A.employee_id)>0 ";
			String sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case when (A.SATISFACTION is  NULL or A.SATISFACTION=0) then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time) as nDay,DATEPART(hour , A.start_time) as nHour, 0 as nType";

			String sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time),DATEPART(hour , A.start_time)";
			String sUId = tab.util.Util.ObjectToString(httpsession.getAttribute("uid"));
			java.util.List<String> grouplist=new ArrayList<String>();
			grouplist=RbacSystem.getUserDepartmentName(sUId,sGroupguid);
			log.info("部门满意度汇总输出部门列表:"+grouplist.toString());
			sWhere+=" and A.department in :grouplist";
			if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
				sWhere+=" and A.queues in :queues";
			}
			if(firstdigits!=null&&firstdigits.length()>0) {
				sWhere = sWhere + " and A.digits_num in :firstdigits";
			}
			if(seconddigits!=null&&seconddigits.length()>0) {
				sWhere = sWhere + " and A.digits_num2 in :seconddigits";
			}
			switch (nType) {
			default:
			case 0:// hour
				break;
			case 1:// day
				sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case when (A.SATISFACTION is  NULL or A.SATISFACTION=0) then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time) as nDay, 1 as nType";
				sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time)";

				break;
			case 2:// week
				sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case when (A.SATISFACTION is  NULL or A.SATISFACTION=0) then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(week , A.start_time) as nWeek, 2 as nType";
				sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(week , A.start_time)";
				break;
			case 3:// month
				sSelect = "select COUNT(*) as nTotalCallCount,COUNT(case when (A.SATISFACTION is  NULL or A.SATISFACTION=0) then 1 end) as n0Count,COUNT(case A.SATISFACTION when 1 then 1 end) as n1Count,COUNT(case A.SATISFACTION when 2 then 1 end) as n2Count,COUNT(case A.SATISFACTION when 3 then 1 end) as n3Count,COUNT(case A.SATISFACTION when 4 then 1 end) as n4Count,COUNT(case A.SATISFACTION when 5 then 1 end) as n5Count,DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth, 3 as nType";
				sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time)";
				break;
			}
			try {
				Session dbsession = GHibernateSessionFactory.getThreadSession();
				try {
					
//					java.util.List<String> agentlist=RbacSystem.GetUsersAgentsFormanageable(sUId,sGroupguid,false);
//					  sWhere += " and A.employee_id in  :agentlist ";
					  sSelect+=" ,A.department as department";
					  sGroupBy +=" ,A.department";
					String querystr = sSelect + " from call_satisf A where " + sWhere + sGroupBy;
					log.info(querystr);
					Query query = dbsession.createSQLQuery(querystr).setTimestamp("starttime", dtStarttime.getDate())
							.setTimestamp("endtime", dtEndtime.getDate());
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					//query.setParameterList("agentlist", agentlist);
					query.setParameterList("grouplist", grouplist);
					if (sQueques != null && sQueques.length() > 0&& !(sQueques.equals("All"))) {
						java.util.List<String> queueslist = Arrays.asList(sQueques.split(","));
						query.setParameterList("queues", queueslist);
					}
					if (firstdigits != null && firstdigits.length() > 0) {
						java.util.List<String> firstdigitslist = Arrays.asList(firstdigits.split(","));
						query.setParameterList("firstdigits", firstdigitslist);
					}
					if (seconddigits != null && seconddigits.length() > 0) {
						java.util.List<String> seconddigitslist = Arrays.asList(seconddigits.split(","));
						query.setParameterList("seconddigits", seconddigitslist);
					}
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> InvestigateResultsList = query.list();
					tab.MyExportExcelFile export = null;
					if (sScript == null || sScript.length() == 0) {
						result.put("list", InvestigateResultsList);
						result.put("totalCount", InvestigateResultsList.size());
						result.put("success", true);
					} else {
						export = new tab.MyExportExcelFile();
						if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator") + "WebRoot"
								+ sScript)) {
							dbsession.close();
							return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain").build();
						}
					}
					if (sScript == null || sScript.length() == 0) {
						for (int i = 0; i < InvestigateResultsList.size(); i++) {
							java.util.Map<String, Object> call = InvestigateResultsList.get(i);
						}
					} else {
						for (int i = 0; i < InvestigateResultsList.size(); i++) {
							java.util.HashMap<String, Object> call = (HashMap<String, Object>) InvestigateResultsList
									.get(i);
							export.CommitRow(call);
						}
					}
					if (sScript == null || sScript.length() == 0) {
						result.put("list", InvestigateResultsList);
						result.put("success", true);
					} else {
						String sFileName = export.GetFile();
						if (sFileName != null && sFileName.length() > 0) {
							java.io.File f = new java.io.File(sFileName);
							int pos = sFileName.lastIndexOf(".");
							dbsession.close();
							return Response.ok(f)
									.header("Content-Disposition",
											"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				} catch (Throwable e) {
					log.warn("ERROR:", e);
					result.put("msg", e.toString());
				}
				dbsession.close();
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			if (sScript == null || sScript.length() == 0) {
				return Response.status(200).entity(result).build();
			}
			return Response.status(404).entity("no datas").type("text/plain").build();
		}
		
}
