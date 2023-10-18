package tab.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.NotAuthorizedException;

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
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import com.google.gson.reflect.TypeToken;

import hbm.factory.GHibernateSessionFactory;
import hbm.factory.HibernateSessionFactory;
import main.Runner;
import main.TaskCreator.CalloutrecordExtends;
import tab.GsonUtil;
import tab.rbac.RbacClient;
import tab.rbac.RbacSystem;
import tab.rec.RecSystem;

public class chenUtil {

	public static Log log = LogFactory.getLog(RecSystem.class);

	public static void main(String[] args) {
		chenUtil tt = new chenUtil();
		tt.outboundSsg();
	}

	public Date dataadd(Calendar current) {
		current.add(current.DATE, 1);
		current.set(Calendar.HOUR_OF_DAY, 0);
		current.set(Calendar.MINUTE, 0);
		current.set(Calendar.SECOND, 0);
		current.set(Calendar.SECOND, 0);
		System.out.print(current.getTime());
		return current.getTime();
	}
	// 通过登录用户角色和前端选择的组 用户获取本组和子组坐席

	public static java.util.Set<String> Getagentlist(String sUId, String sGroupGuid,
			tab.configServer.ValueInteger bRoles, Session dbsession) {
		java.util.Set<String> roles = null;
		if (sUId != null && sUId.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
			roles = tab.rbac.RbacClient.getRoleGuidsForRole(sUId, sGroupGuid, dbsession, false);
		}
		java.util.Set<String> agentList = null;
		if (roles != null && roles.size() > 0) {
			if (roles.contains(tab.rbac.RbacClient.ROOT_ROLEGUID)) {
				// 管理员组的查询操作，无权限限制
				log.info("roles不为空超级管理员权限查询");
				bRoles.value = 0;
			} else {
				bRoles.value = 1;
				log.info("roles不为空按坐席组查询");
				// 坐席组
				if (sUId != null && sUId.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
					if (agentList == null) {
						agentList = new java.util.HashSet<>();
					}
					for (String role : roles) {
						agentList.addAll(tab.rbac.RbacClient.getUserAgents(sUId, role, false));
					}
					java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(
							RbacClient.getUserAuths(sUId, sUId), new TypeToken<java.util.Map<String, Object>>() {
							}.getType());
					if (agentinfo != null) {
						String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
						if (agentNo.length() > 0)
							agentList.add(agentNo);
					}
				} else {
					if (agentList == null) {
						agentList = new java.util.HashSet<>();
					}
					for (String role : roles) {
						agentList.addAll(tab.rbac.RbacClient.getUserAgents(sUId, role, false, dbsession));
					}
				}
			}
		} else {
			log.info("roles为空坐席组查询");
			// 坐席组
			if (sUId != null && sUId.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
				agentList = tab.rbac.RbacClient.getUserAgents(sUId, sGroupGuid, true, dbsession);
				if (agentList == null) {
					agentList = new java.util.HashSet<>();
				}
				java.util.Map<String, Object> agentinfo = GsonUtil.getInstance()
						.fromJson(RbacClient.getUserAuths(sUId, sUId), new TypeToken<java.util.Map<String, Object>>() {
						}.getType());
				if (agentinfo != null) {
					String agentNo = tab.util.Util.ObjectToString(agentinfo.get("agent"));
					if (agentNo.length() > 0)
						agentList.add(agentNo);
				}
				bRoles.value = 1;
			}
		}
		return agentList;
	}

	public static int outboundSsg() {
		String SSGUrl = "http://192.168.8.120:9800/SSG?TenantName=Environment";
		HttpPost httpPost = new HttpPost(SSGUrl);

		String sJsonRpc = String.format("<SSGRequest xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ "<CreateRequest Token=\"T7034\" MaxAttempts=\"2\" TimeToLive=\"60s\"\r\n"
				+ "IVRProfileName=\"ssg\" Telnum=\"600000\" \r\n"
				+ "NotificationURL=\"http://127.0.0.1:55511/tab/call/GetNotifition\">\r\n" + "</CreateRequest>\r\n "
				+ "<cpd record=\"false\"\r\n" + "postconnecttimeout=\"6s\"\r\n" + "rnatimeout=\"6s\"\r\n"
				+ "preconnect=\"true\"\r\n" + "detect=\"all\"/>" + "</SSGRequest>");
		CloseableHttpClient client = tab.util.Util.getIdleHttpClient(SSGUrl.indexOf("https://") == 0 ? true : false);
		if (client == null) {
			throw new NotAuthorizedException("Invalid CloseableHttpClient");
		}
		try {
			StringEntity sEntity = new StringEntity(sJsonRpc, org.apache.http.Consts.UTF_8);
			httpPost.setEntity(sEntity);
			httpPost.setHeader("Content-Type", "application/xml");
			HttpResponse res = client.execute(httpPost);
			HttpEntity entity = res.getEntity();
			try {
				int nStatusCode = res.getStatusLine().getStatusCode();
				if (nStatusCode < 300) {
					String sSessionInfo = EntityUtils.toString(entity, org.apache.http.Consts.UTF_8);
					try {
						Document doc = DocumentHelper.parseText(sSessionInfo);
						Element roots = doc.getRootElement();
						Element res1 = roots.element("ResponseElement");

						if (res1 != null) {
							System.out.print(res1.getName());
							if (res1.attributeValue("ResponseType").equals("SUCCESS")) {
								System.out.print(res1.attributeValue("ResponseType"));
							}

							System.out.print(res1.attributeValue("RequestID"));
							System.out.print(res1.attributeValue("Token"));
							System.out.print(res1.getData());
						} else {
							System.out.print("请求失败" + sSessionInfo);
						}

					} catch (DocumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					throw new NotAuthorizedException("Invalid StatusCode");
				}
			} catch (ParseException e) {
				throw new NotAuthorizedException("Invalid ParseException");
			} catch (IOException e) {
				throw new NotAuthorizedException("Invalid IOException");
			}

		} catch (IOException e) {
			throw new NotAuthorizedException("Invalid IOException");

		}
		return 0;
	}

	// 参数分别为 视图名 时间字段 session 明细表或者汇总表
	public static void copytable(String viewnamestr, String timelabel, Session gdbsession, Boolean detialtable) {
		try {
			Transaction ts = gdbsession.beginTransaction();
			try {
				// todo 先查出上次插入表的最大时间点 根据这个值可以找出视图中新增的数据,然后将表中最大时间点的数据删除,将视图中新查出来的数据插入表
				// 之所以用删除表中记录再插入而不是用更新记录的方式 是因为indert into select * from 比update 方便
				String tablenamestr = "t_" + viewnamestr;
				String querymaxtimestr = "select max(" + timelabel + ") from " + tablenamestr;
				log.info("打印日志sql语句lasttimeSql:" + querymaxtimestr);
				Query querymaxtime = gdbsession.createSQLQuery(querymaxtimestr);
				java.util.List<Object> objlist = querymaxtime.list();
				if (objlist.size() > 0 && objlist.get(0) != null) {
					if (viewnamestr.equals("cdr_voice")) {
						log.info("cdr_voice明细表更新");
						BigDecimal maxtime=(BigDecimal) objlist.get(0);
						String queryviewaddstr = "insert into " + tablenamestr + " select * from " + viewnamestr
								+ " where " + timelabel + ">'" + maxtime + "'";
						log.info("打印日志sql语句querycdr_voicrview:" + queryviewaddstr);
						Query queryaddview = gdbsession.createSQLQuery(queryviewaddstr);
						int insertRec = queryaddview.executeUpdate();
						if (insertRec >= 0) {
							ts.commit();
						} else {
							ts.rollback();
						}
					} else if (detialtable) {
						String maxtime = "";
						log.info("明细表更新");
						if (objlist.get(0) instanceof String) {
							maxtime = (String) objlist.get(0);
						} else {
							Date send_date = (Date) objlist.get(0);
							DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							maxtime = df.format(send_date);
						}
						String queryviewaddstr = "insert into " + tablenamestr + " select * from " + viewnamestr
								+ " where " + timelabel + ">'" + maxtime + "'";
						log.info("打印日志sql语句queryaddview:" + queryviewaddstr);
						Query queryaddview = gdbsession.createSQLQuery(queryviewaddstr);
						int insertRec = queryaddview.executeUpdate();
						if (insertRec >= 0) {
							ts.commit();
						} else {
							ts.rollback();
						}
					} else {
						log.info("汇总表更新");
						String maxtime = (String) objlist.get(0);
						String deletetablestr = "delete from " + tablenamestr + " where " + timelabel + "='" + maxtime
								+ "'";
						log.info("打印日志sql语句queryupdateview:" + deletetablestr);
						Query quertdelete = gdbsession.createSQLQuery(deletetablestr);
						int deleteRes = quertdelete.executeUpdate();
						String queryviewaddstr = "insert into " + tablenamestr + " select * from " + viewnamestr
								+ " where " + timelabel + ">='" + maxtime + "'";
						log.info("打印日志sql语句queryaddview:" + queryviewaddstr);
						Query queryaddview = gdbsession.createSQLQuery(queryviewaddstr);
						int insertRec = queryaddview.executeUpdate();
						if (deleteRes >= 0 && insertRec >= 0) {
							ts.commit();
						} else {
							ts.rollback();
						}
					}
				} else {// 全部复制
					String queryviewaddstr = "insert into " + tablenamestr + " select * from " + viewnamestr;
					log.info("数据全部复制打印日志sql语句queryaddview:" + queryviewaddstr);
					Query queryaddview = gdbsession.createSQLQuery(queryviewaddstr);
					int insertRec = queryaddview.executeUpdate();
					if (insertRec >= 0) {
						ts.commit();
					} else {
						ts.rollback();
					}
				}

			} catch (org.hibernate.HibernateException e) {
				if (ts.getStatus() == TransactionStatus.ACTIVE || ts.getStatus() == TransactionStatus.MARKED_ROLLBACK) {
					ts.rollback();
				} else
					ts.commit();
				log.warn("ERROR:", e);
			} catch (Throwable e) {
				if (ts.getStatus() == TransactionStatus.ACTIVE || ts.getStatus() == TransactionStatus.MARKED_ROLLBACK) {
					ts.rollback();
				} else
					ts.commit();
				log.warn("ERROR:", e);
			}
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:", e);
		} catch (Throwable e) {
			log.warn("ERROR:", e);
		}
	}

	// 获取当前时间的前七天日期格式
	public static String GetBeforeSevenData(int daynumber) {
		Calendar caledar = Calendar.getInstance();
		caledar.setTime(new java.util.Date());
		caledar.add(Calendar.DATE, daynumber);
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		java.util.Date date = caledar.getTime();
		String strdate = format.format(date);
		return strdate;
	}

	public static java.util.List<Map<String, Object>> datelist() {

		java.util.List<Map<String, Object>> list = new java.util.ArrayList<Map<String, Object>>();
		java.util.List<String> datelist = new ArrayList<String>();
		datelist.add(GetBeforeSevenData(0));
		datelist.add(GetBeforeSevenData(-1));
		datelist.add(GetBeforeSevenData(-2));
		datelist.add(GetBeforeSevenData(-3));
		datelist.add(GetBeforeSevenData(-4));
		datelist.add(GetBeforeSevenData(-5));
		datelist.add(GetBeforeSevenData(-6));
		// for(int i=-6;i<=0;i++) {
		// Map<String,Object> map=new HashMap<String,Object>();
		// map.put("label_yyyy_mm_dd",GetBeforeSevenData(i));
		// map.put("cal_day_name","");
		// map.put("resource_name","710000");
		// map.put("accepted_agent","");
		// map.put("abandoned","");
		// map.put("abandoned_10s","");
		// list.add(map);
		// }
		for (int i = -6; i <= 0; i++) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("label_yyyy_mm_dd", GetBeforeSevenData(i));
			map.put("cal_day_name", "");
			map.put("resource_name", "C2");
			map.put("accepted_agent", "");
			map.put("abandoned", "");
			map.put("abandoned_10s", "");
			list.add(map);
		}
		for (int i = -6; i <= 0; i++) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("label_yyyy_mm_dd", GetBeforeSevenData(i));
			map.put("cal_day_name", "");
			map.put("resource_name", "C1");
			map.put("accepted_agent", "");
			map.put("abandoned", "");
			map.put("abandoned_10s", "");
			list.add(map);
		}
		return list;

	}

	public static java.util.Set<String> GetUserInfo(String sUId) {
		java.util.HashSet<String> userAgentList = new java.util.HashSet<String>();
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			java.util.List<String> list = dbsession.createQuery("select * from Rbacuserauths where userguid =:userguid")
					.setParameter("userguid", sUId).list();

			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:", e);
		} catch (Throwable e) {
			log.warn("ERROR:", e);
		}
		return userAgentList;

	}

}
