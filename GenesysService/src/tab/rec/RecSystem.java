package tab.rec;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import hbm.OffersetTransformers;
import hbm.factory.GHibernateSessionFactory;
import hbm.factory.HibernateSessionFactory;
import hbm.model.Rbacroleuser;
import hbm.model.RbacroleuserId;
import hbm.model.Rbacuser;
import hbm.model.Rbacuserauths;
import hbm.model.Reccontent;
import hbm.model.Recextension;
import hbm.model.Recfiles;
import hbm.model.RecfilesEx;
import hbm.model.RecphoneEx;
import main.Runner;
import main.TaskCreator;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import tab.EXTJSSortParam;
import tab.GenesysStatServer;
import tab.GsonUtil;
import tab.RESTDateParam;
import tab.configServer;
import tab.configServer.ValueInfo;
import tab.rbac.AliyunToken;
import tab.rbac.RbacClient;
import tab.util.Util;

// (value = "录音接口")
@Path("/rec")
public class RecSystem {
	public static Log logger = LogFactory.getLog(RecSystem.class);
	private static ObjectMapper mapper = new ObjectMapper();

	// 更新录音
	@SuppressWarnings("unused")
	private static boolean UpdateRecfilesRole(String sNewRoleGuid, String sUId, RESTDateParam dtStarttime,
			RESTDateParam dtEndtime, Integer nHost, String caller, String called, String ext, String agent, int nBackup,
			int nLock, int nDelete, int nAnswer, int nDirection, int nInside, int nLength, long nMaxLength,
			String sUcid, String sGuidId, String sGroupGuid, tab.configServer.ValueInteger nTotalCount) {
		nTotalCount.value = 0;
		boolean bId = false, bUcid = false, bCaller = false, bCalled = false, bExt = false, bAgent = false;
		String sWhere = new String(" 1=1");
		if (sGuidId != null && sGuidId.length() > 0) {
			sWhere += " and A.id=:id";
			bId = true;
		} else if (sUcid != null && sUcid.length() > 0) {
			sWhere += " and A.ucid=:ucid";
			bUcid = true;
		} else {
			if (nHost != null && nHost >= 0) {
				sWhere += " and A.host=" + nHost;
			}
			if (nLength > 0) {
				sWhere += " and A.seconds>=" + nLength;
			}
			if (nMaxLength > 0) {
				sWhere = sWhere + " and A.seconds<=" + nMaxLength;
			}
			// 10 = 2 呼入不应答
			// 11 = 3 呼出不应答
			// 01 = 1 呼出应答
			// 00 = 0 呼入应答
			if (nDirection == 2) {// 呼入0,2
				if (nAnswer == 1) {// 应答 0
					sWhere = sWhere + " and A.direction=0";
				} else if (nAnswer == 2) {// 不应答 2
					sWhere = sWhere + " and A.direction=2";
				} else {
					sWhere = sWhere + " and (A.direction=0 or A.direction=2)";
				}
			} else if (nDirection == 1) {// 呼出1,3
				if (nAnswer == 1) {// 应答 1
					sWhere = sWhere + " and A.direction=1";
				} else if (nAnswer == 2) {// 不应答 3
					sWhere = sWhere + " and A.direction=3";
				} else {
					sWhere = sWhere + " and (A.direction=1 or A.direction=3)";
				}
			} else {// 呼入呼出 0,1,2,3
				if (nAnswer == 1) {// 应答 0,1
					sWhere = sWhere + " and (A.direction=1 or A.direction=0)";
				} else if (nAnswer == 2) {// 不应答 2,3
					sWhere = sWhere + " and (A.direction=2 or A.direction=3)";
				}
				// nAnswer=0,nDir=0
			}
			if (caller != null)
				caller = caller.trim();
			if (called != null)
				called = called.trim();
			if (caller != null && caller.length() > 0 && caller.equalsIgnoreCase(called)) {
				sWhere = sWhere + " and (A.caller like :caller or A.called like :called)";
				bCaller = true;
				bCalled = true;
			} else {
				if (caller != null && caller.length() > 0) {
					sWhere = sWhere + " and A.caller like :caller";
					bCaller = true;
				}
				if (called != null && called.length() > 0) {
					sWhere = sWhere + " and A.called like :called";
					bCalled = true;
				}
			}
			if (ext != null && ext.length() > 0) {
				ext = ext.trim();
				sWhere = sWhere + " and A.extension=:ext";
				bExt = true;
			}
			if (agent != null && agent.length() > 0) {
				agent = agent.trim();
				if (tab.util.Util.NONE_GUID.equals(agent)) {
					bAgent = false;
				} else if (agent.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}") == false) {
					sWhere += " and A.agent=:agent";
					bAgent = true;
				} else {
					sWhere += " and exists ( select 1 from Rbacuserauths B where B.userguid=:agent and B.agent!='' and B.agent is not null and B.agent=A.agent)";
					bAgent = true;
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
						return false;
				} else if (nBackup == 1) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states=2";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states=6";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (2,6)";
					} else
						return false;
				} else if (nBackup == 0) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states in (0,2)";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states in (4,6)";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (0,2,4,6)";
					} else
						return false;
				} else
					return false;
			} else if ((nLock & 1) == 1) {
				if (nBackup == 2) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states=1";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states=5";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (1,5)";
					} else
						return false;
				} else if (nBackup == 1) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states=3";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states=7";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (3,7)";
					} else
						return false;
				} else if (nBackup == 0) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states in (1,3)";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states in (5,7)";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (1,3,5,7)";
					} else
						return false;
				} else
					return false;
			} else if ((nLock & 1) == 0) {
				if (nBackup == 2) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states in (0,1)";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states in (4,5)";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (0,1,4,5)";
					} else
						return false;
				} else if (nBackup == 1) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states in (2,3)";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states in (6,7)";
					} else if (nDelete == 0) {
						sWhere = sWhere + " and A.states in (2,3,6,7)";
					} else
						return false;
				} else if (nBackup == 0) {
					if (nDelete == 2) {
						sWhere = sWhere + " and A.states in (0,1,2,3)";
					} else if (nDelete == 1) {
						sWhere = sWhere + " and A.states in (4,5,6,7)";
					} else if (nDelete == 0) {
						// 去掉states条件
					} else
						return false;
				} else
					return false;
			} else
				return false;
			sWhere = " A.createdate>=:start and A.createdate<=:end and" + sWhere;
		}
		boolean bRoles = false, bAgentList = false;
		java.util.Set<String> roles = tab.rbac.RbacClient.getRoleGuidsForRole(sUId, sGroupGuid, null, false);
		java.util.Set<String> agentList = null;
		if (roles != null) {
			if (roles.contains(tab.rbac.RbacClient.ROOT_ROLEGUID)) {
				// 管理员组的查询操作，无权限限制
			} else {
				// 坐席组
				agentList = sGroupGuid != null && sGroupGuid.length() > 0
						? tab.rbac.RbacClient.getUserGuidsForRole(sUId, sGroupGuid, null, true)
						: null;
				if (agentList != null) {
					// 分机组
					sWhere += " and (A.roleid in(:roles)";
					bRoles = true;
					sWhere += " or exists ( select 1 from Rbacuserauths B where (length(B.agent)>0 and B.agent=A.agent) and B.userguid in (:agentList) )";
					bAgentList = true;
					sWhere += ")";
				} else {
					return false;
				}
			}
		} else {
			return false;
		}
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Transaction ts = dbsession.beginTransaction();
				Query query = null;
				if (Runner.bCompatibleMode) {
					query = dbsession.createQuery("update Recphone A set roleid=:newroleguid where" + sWhere)
							.setString("newroleguid", sNewRoleGuid);
				} else {
					query = dbsession.createQuery("update Recfiles A set roleid=:newroleguid where" + sWhere)
							.setString("newroleguid", sNewRoleGuid);
				}
				if (bId) {
					query.setString("id", sGuidId);
					if (bRoles)
						query.setParameterList("roles", roles);
					if (bAgentList)
						query.setParameterList("agentList", agentList);
				} else if (bUcid) {
					query.setString("ucid", sUcid);
					if (bRoles)
						query.setParameterList("roles", roles);
					if (bAgentList)
						query.setParameterList("agentList", agentList);
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
						query.setParameterList("roles", roles);
					if (bAgentList)
						query.setParameterList("agentList", agentList);
					query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
				}
				nTotalCount.value = query.executeUpdate();	
				tab.rbac.RbacSystem.AuditLog("update", "Recfiles", tab.util.Util.NONE_GUID, 1, String.format("{\"roleguid\":\"%s\",\"recordCount\":%d}",sNewRoleGuid,nTotalCount.value), dbsession, ts, sUId);
				ts.commit();
				dbsession.close();
				return true;
			} catch (org.hibernate.HibernateException e) {
				logger.warn("ERROR:", e);
			} catch (Throwable e) {
				logger.warn("ERROR:", e);
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			logger.warn("ERROR:", e);
		} catch (Throwable e) {
			logger.warn("ERROR: ", e);
		}
		return false;
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
	
	// 查询录音
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
			if (ext != null && ext.length() > 0) {
				ext = ext.trim();
				sWhere = sWhere + " and A.resource_name=:ext";
				bExt = true;
			}
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
			sWhere = " A.start_time>=:start and A.start_time<=:end and" + sWhere;
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
				logger.info("通过uid权限查询录音");
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
					logger.info("roles不为空超级管理员权限查询");
				} else {
					logger.info("roles不为空按坐席组查询");
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
				logger.info("roles为空坐席组查询");
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
			String sSelect = "select A.media_server_ixn_guid as id, A.start_time as createdate, A.resource_name as extension,A.source_address as caller,A.employee_id as agent,A.AGENT_LAST_NAME as username,A.queues as queues "
					+ ",A.target_address as called,A.talk_duration as seconds,(case when upper(A." + sInteraction_type_name + ")='INBOUND' or upper(A." + sInteraction_type_name 	+ ")='INTERNAL'  then 0 else 1 end) as direction";
			String sSqlStr = StringUtils.EMPTY;
			try {
				if (pageStart != null && pageLimit != null && pageLimit > 0 && pageStart >= 0) {
					sSqlStr = "select count(*) from call_detial A where" + sWhere;
					logger.info("start: " + dtStarttime.toString() + ", end:" + dtEndtime.toString());
					if(bRoles) {
						logger.info(GsonUtil.getInstance().toJson(agentList));
					}
					logger.info(sSqlStr);
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
						logger.info("输出选择队列:"+queueslist.toString());
						query.setParameterList("queues", queueslist);
					}
				
					nTotalCount.value = Util.ObjectToNumber(query.uniqueResult(), 0);
					if (nTotalCount.value > 0) {
						if (sort == null) {
							sSqlStr = sSelect + " from call_detial A where" + sWhere + " order by A.start_time desc";
							logger.info(sSqlStr);
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
								sProperty = "A.start_time";
								break;
							case "sExtension":
							case "extension":
								sProperty = "A.resource_name";
								break;
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
							sSqlStr = sSelect + " from call_detial A where" + sWhere + " order by "	+ sProperty + " " + sort.getsDirection();
							logger.info(sSqlStr);
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
					sSqlStr = sSelect + " from call_detial A where" + sWhere + " order by A.start_time desc";
					logger.info(sSqlStr);
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
				//username可以直接从call_detial视图中查询
//				for (int idx = 0; recfiles != null && idx < recfiles.size(); idx++) {
//					RecfilesEx rec = recfiles.get(idx);
//					if (rec.getAgent() != null && rec.getAgent().length() > 0) {
//						usernames.put(rec.getAgent(), "");
//					}
//				}
				// tab.rbac.RbacSystem.setUsername(usernames,dbsession);

			} catch (org.hibernate.HibernateException e) {
				logger.warn("ERROR:", e);
			} catch (Throwable e) {
				logger.warn("ERROR:", e);
			}
			if(dbsession.isOpen()) {
				dbsession.close();
			}
			if(gdbsession.isOpen()) {
				gdbsession.close();
			}
			
			
		} catch (org.hibernate.HibernateException e) {
			logger.warn("ERROR:", e);
		} catch (Throwable e) {
			logger.warn("ERROR: ", e);
		}
		/*
		 * 删除标志位100,备份标志位10,锁标志位1
		 * (备份:010=2,110=6,011=3,111=7)(无备份:000=0,100=4,001=1,101=5)
		 * (删除:100=4,101=5,110=6,111=7)(无删除:000=0,001=1,010=2,011=3)
		 * (加锁:001=1,011=3,101=5,111=7)(无加锁:000=0,010=2,100=4,110=6) 选择未加锁时 不包含备份时：
		 * 不包含删除：states=0 仅删除：states=4 包含删除：states in (0,4) 仅备份时： 不包含删除：states=2
		 * 仅删除：states=6 包含删除：states in (2,6) 包含备份时： 不包含删除：states in (0,2) 仅删除：states in
		 * (4,6) 包含删除：states in (0,2,4,6)
		 * 
		 * 选择加锁时 不包含备份记录: 不包含删除：states=1 仅删除：states=5 包含删除：states in (1,5) 仅备份:
		 * 不包含删除：states=3 仅删除：states=7 包含删除：states in (3,7) 包含备份: 不包含删除：states in (1,3)
		 * 仅删除：states in (5,7) 包含删除：states in (1,3,5,7)
		 * 
		 * 包含加锁时 不包含备份记录: 不包含删除：states in (0,1) 仅删除：states in (4,5) 包含删除：states in
		 * (0,1,4,5) 仅备份记录: 不包含删除：states in (2,3) 仅删除：states in (6,7) 包含删除：states in
		 * (2,3,6,7) 包含备份的: 不包含删除：states in (0,1,2,3) 仅删除：states in (4,5,6,7)
		 * 包含删除：去掉states条件
		 */
		// (备份:010=2,110=6,011=3,111=7)(无备份:000=0,100=4,001=1,101=5)
		// (删除:100=4,101=5,110=6,111=7)(无删除:000=0,001=1,010=2,011=3)
		// (加锁:001=1,011=3,101=5,111=7)(无加锁:000=0,010=2,100=4,110=6)
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

	
	
	private static List<RecphoneEx> SearchRecordsCompatibleMode(String sUId, String sRoleId, RESTDateParam dtStarttime,
			RESTDateParam dtEndtime, Integer nHost, String caller, String called, String ext, String agent, int nBackup,
			int nLock, int nDelete, int nAnswer, int nDirection, int nInside, int nLength, long nMaxLength,
			String sUcid, String sGuidId, String sGroupGuid, Integer pageStart, Integer pageLimit, EXTJSSortParam sort,
			tab.configServer.ValueInteger nTotalCount,int AuditLogDownExport,String sMark) {
		return null;
	}

	// API文档: (value = "查询录音",notes = "支持可选条件分页和排序")
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
			@FormParam("length") /* "录音最小长度" */int nLength, @FormParam("maxlength") /* "录音最大长度, 0忽略" */long nMaxLength,
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
		if (Runner.bCompatibleMode) {
			java.util.List<RecphoneEx> recfiles = SearchRecordsCompatibleMode(
					Util.ObjectToString(httpsession.getAttribute("uid")), null, dtStarttime, dtEndtime, nHost, caller,
					called, ext, agent, nBackup, nLock, nDelete, nAnswer, nDirection, nInside, nLength, nMaxLength,
					sUcid, sGuidId, sGroupGuid, pageStart, pageLimit, Sort, nTotalCount,0,sMark);
			result.put("recordItems", recfiles);
			result.put("totalCount", nTotalCount.value);
		} else {
			java.util.List<RecfilesEx> recfiles = SearchRecords(Util.ObjectToString(httpsession.getAttribute("uid")),
					null, dtStarttime, dtEndtime, nHost, caller, called, ext, agent, nBackup, nLock, nDelete, nAnswer,
					nDirection, nInside, nLength, nMaxLength, sUcid, sGuidId, sGroupGuid, pageStart, pageLimit, Sort,
					nTotalCount,0,sMark,sMorequeues);
			result.put("recordItems", recfiles);
			result.put("totalCount", nTotalCount.value);
		}
		return Response.status(200).entity(result).build();
	}

	// API文档: (value = "使用工号查询该工号有权限的录音",notes = "支持可选条件分页和排序")
	@POST
	@Path("/SearchRecords")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response SearchRecords(@Context HttpServletRequest R, @FormParam("appid") /* "查询者" */String sAppId,
			@FormParam("token") /* "查询者" */String sToken, @FormParam("code") /* "查询者code" */String sCode,
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
			@FormParam("length") /* "录音最小长度" */int nLength, @FormParam("maxlength") /* "录音最大长度, 0忽略" */long nMaxLength,
			@FormParam("ucid") /* "通过呼叫唯一ID查询录音,忽略时间范围" */String sUcid,
			@FormParam("guidid") /* "通过录音唯一ID查询录音,忽略时间范围" */String sGuidId,
			@FormParam("groupguidid") /* "通过录音所属组查询,该组可以是坐席工号组,也可以是分机组" */String sGroupGuid,
			@FormParam("mark") /* 备注 */String sMark,
			@FormParam("start") /* "开始页" */Integer pageStart, @FormParam("limit") /* "分页大小" */Integer pageLimit,
			@FormParam("sort") /* "排序(ExtJS默认格式, {\"property\":\"字段名\",\"direction\":\"ASC\"})" */EXTJSSortParam Sort) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		tab.configServer.ValueInteger nTotalCount = new tab.configServer.ValueInteger(0);
		if (Runner.bCompatibleMode) {
			java.util.List<RecphoneEx> recfiles = SearchRecordsCompatibleMode(null,
					tab.rbac.RbacClient.checkAppIdToken(sAppId, sToken), dtStarttime, dtEndtime, nHost,
					caller, called, ext, agent, nBackup, nLock, nDelete, nAnswer, nDirection, nInside, nLength,
					nMaxLength, sUcid, sGuidId, sGroupGuid, pageStart, pageLimit, Sort, nTotalCount,0,sMark);
			result.put("recordItems", recfiles);
			result.put("totalCount", nTotalCount.value);
		} else {
			java.util.List<RecfilesEx> recfiles = SearchRecords(null,
					tab.rbac.RbacClient.checkAppIdToken(sAppId, sToken), dtStarttime, dtEndtime, nHost,
					caller, called, ext, agent, nBackup, nLock, nDelete, nAnswer, nDirection, nInside, nLength,
					nMaxLength, sUcid, sGuidId, sGroupGuid, pageStart, pageLimit, Sort, nTotalCount,0,sMark,null);
			result.put("recordItems", recfiles);
			result.put("totalCount", nTotalCount.value);
		}
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/UILockRecord")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UILockRecord(@Context HttpServletRequest R, @FormParam("guidid") String sGuid,
			@FormParam("lock") Boolean bLock) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		result.put("msg", "不能修改Genesys系统数据库!");
		return Response.status(200).entity(result).build();
	}

	// API文档: (value = "下载查询到的录音文件",notes = "不超过100条录音文件")
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
		if (Runner.bCompatibleMode) {
			java.util.List<RecphoneEx> recfiles = SearchRecordsCompatibleMode(
					Util.ObjectToString(httpsession.getAttribute("uid")), null, dtStarttime, dtEndtime, nHost, caller,
					called, ext, agent, nBackup, nLock, nDelete, nAnswer, nDirection, nInside, nLength, nMaxLength,
					sUcid, sGuidId, sGroupGuid, null, null, null, null,1,sMark);
			nSize = recfiles.size();
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
					logger.error("ERR:", e);
					return Response.status(404).entity(e.toString()).type("text/plain").build();
				}
				CloseableHttpClient httpclient = tab.util.Util
						.getIdleHttpClient(Runner.sPlayUrl.indexOf("https://") == 0 ? true : false);
				for (int i = 0; i < nSize; ++i) {
					RecphoneEx Item = recfiles.get(i);
					int npos = Item.getFilename().lastIndexOf(".");
					File f = new File(sPath + Item.getGuidid() + (Runner.nPlayMode == 0 ? ".MP3" : (npos>0 ? Item.getFilename().substring(npos) : "")));
					if (f.exists()) {
						f.delete();
					}
					try {
						HttpGet httpget = null;
						if (Runner.nPlayMode == 0) {
							httpget = new HttpGet(Runner.sPlayUrl + "/%7B" + Item.getGuidid() + "%7D.MP3");
						} else {
							httpget = new HttpGet("http://127.0.0.1:" + Runner.nWebPort + "/" + Item.getFilename().replaceAll(" ", "%20"));
						}
						// 设置超时时间为1000s
						RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(3000)
								.setConnectTimeout(3000).setConnectionRequestTimeout(1000).build();
						httpget.setConfig(requestConfig);
						CloseableHttpResponse result = httpclient.execute(httpget);
						if (result != null) {
							HttpEntity entity = result.getEntity();
							try {
								if (ContentType.get(entity).getMimeType().contains("audio/")
										|| ContentType.get(entity).getMimeType().contains("application/octet-stream")) {
									FileOutputStream fos = new FileOutputStream(f);					
									entity.writeTo(fos);
									fos.flush();
									fos.close();					
									FileInputStream fin = new FileInputStream(f);
									java.io.BufferedInputStream buffered = new java.io.BufferedInputStream(fin);
									// 添加文件名到zip
									zos.putNextEntry(new ZipEntry((Item.getSystemtim().getTime() + "-" + Item.getCaller()
											+ "-" + Item.getCalled() + "-" + Item.getExtension() + "-" + Item.getAgent()
											+ "-" + i + (Runner.nPlayMode == 0 ? ".MP3" : (npos>0 ? Item.getFilename().substring(npos) : "")))
													.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")));
									// 把下载的文件内容添加到zip
									int len = 0;
									while ((len = buffered.read()) != -1) {
										zos.write(len);
									}
									fin.close();
									// 删除下载的文件
									f.delete();
								} else {
									FileOutputStream fos = new FileOutputStream(f);
									fos.flush();
									fos.close();
									FileInputStream fin = new FileInputStream(f);
									java.io.BufferedInputStream buffered = new java.io.BufferedInputStream(fin);
									// 添加文件名到zip
									zos.putNextEntry(new ZipEntry(
											"ERR-" + (Item.getSystemtim().getTime() + "-" + Item.getCaller() + "-"
													+ Item.getCalled() + "-" + Item.getExtension() + "-" + Item.getAgent()
													+ "-" + i + (Runner.nPlayMode == 0 ? ".MP3" : (npos>0 ? Item.getFilename().substring(npos) : "")))
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")));
									// 把下载的文件内容添加到zip
									int len = 0;
									while ((len = buffered.read()) != -1) {
										zos.write(len);
									}
									fin.close();
									// 删除下载的文件
									f.delete();
								}
							} finally {
								EntityUtils.consume(entity);
							}
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
				}

				try {
					httpclient.close();
				} catch (IOException e) {
					logger.error(e);
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
		} else {
			java.util.List<RecfilesEx> recfiles = SearchRecords(Util.ObjectToString(httpsession.getAttribute("uid")),
					null, dtStarttime, dtEndtime, nHost, caller, called, ext, agent, nBackup, nLock, nDelete, nAnswer,
					nDirection, nInside, nLength, nMaxLength, sUcid, sGuidId, sGroupGuid, null, null, null, null,1,sMark,sQueques);
			nSize = recfiles.size();
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
					logger.error("ERR:", e);
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
						HttpGet httpget = null;
						if (Runner.nPlayMode == 0) {
							httpget = new HttpGet(Runner.sPlayUrl + "/%7B" + Item.getId() + "%7D.MP3");
						} else {
							httpget = new HttpGet("http://127.0.0.1:" + Runner.nWebPort + "/" + Item.getFilename().replaceAll(" ", "%20"));
						}
						// 设置超时时间为1000s
						RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(3000)
								.setConnectTimeout(3000).setConnectionRequestTimeout(1000).build();
						httpget.setConfig(requestConfig);
						CloseableHttpResponse result = httpclient.execute(httpget);
						if (result != null) {
							HttpEntity entity = result.getEntity();
							try {
								if (ContentType.get(entity).getMimeType().contains("audio/")
										|| ContentType.get(entity).getMimeType().contains("application/octet-stream")) {
									FileOutputStream fos = new FileOutputStream(f);
									entity.writeTo(fos);
									fos.flush();
									fos.close();
									FileInputStream fin = new FileInputStream(f);
									java.io.BufferedInputStream buffered = new java.io.BufferedInputStream(fin);
									// 添加文件名到zip
									zos.putNextEntry(new ZipEntry((Item.getCreatedate().getTime() + "-" + Item.getCaller()
											+ "-" + Item.getCalled() + "-" + Item.getExtension() + "-" + Item.getAgent()
											+ "-" + i + (Runner.nPlayMode == 0 ? ".MP3" : (npos>0 ? Item.getFilename().substring(npos) : "")))
													.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")));
									// 把下载的文件内容添加到zip
									int len = 0;
									while ((len = buffered.read()) != -1) {
										zos.write(len);
									}
									fin.close();
									// 删除下载的文件
									f.delete();
								} else {
									FileOutputStream fos = new FileOutputStream(f);
									fos.flush();
									fos.close();
									FileInputStream fin = new FileInputStream(f);
									java.io.BufferedInputStream buffered = new java.io.BufferedInputStream(fin);
									// 添加文件名到zip
									zos.putNextEntry(new ZipEntry(
											"ERR-" + (Item.getCreatedate().getTime() + "-" + Item.getCaller() + "-"
													+ Item.getCalled() + "-" + Item.getExtension() + "-" + Item.getAgent()
													+ "-" + i + (Runner.nPlayMode == 0 ? ".MP3" : (npos>0 ? Item.getFilename().substring(npos) : "")))
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")));
									// 把下载的文件内容添加到zip
									int len = 0;
									while ((len = buffered.read()) != -1) {
										zos.write(len);
									}
									fin.close();
									// 删除下载的文件
									f.delete();
								}
							} finally {
								EntityUtils.consume(entity);
							}
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
				}

				try {
					httpclient.close();
				} catch (IOException e) {
					logger.error(e);
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
		}
		return Response.status(404).entity("TOO MANY FILES(" + nSize + "), MAX FILES(" + vi.value + ")")
				.type("text/plain").build();
	}

	// API文档: (value = "导出查询查询结果")
	@POST
	@Path("/UISearchRecordsExport")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/zip" + ";charset=utf-8")
	public Response UISearchRecordsExport(@Context HttpServletRequest R,
			@FormParam("script") /* "导出报表的脚本文件相对WebRoot路径" */String sScript,
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
		tab.MyExportExcelFile export = new tab.MyExportExcelFile();
		if (Runner.bCompatibleMode) {
			java.util.List<RecphoneEx> recfiles = SearchRecordsCompatibleMode(
					Util.ObjectToString(httpsession.getAttribute("uid")), null, dtStarttime, dtEndtime, nHost, caller,
					called, ext, agent, nBackup, nLock, nDelete, nAnswer, nDirection, nInside, nLength, nMaxLength,
					sUcid, sGuidId, sGroupGuid, null, null, null, null,2,sMark);
			if (!export.Init(System.getProperty("tab.path") + File.separator + "WebRoot" + sScript)) {
				return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain").build();
			}
			for (int i = 0; recfiles!=null && i < recfiles.size(); ++i) {
				RecphoneEx Item = recfiles.get(i);
				export.CommitRow(Item);
			}
		} else {
			java.util.List<RecfilesEx> recfiles = SearchRecords(Util.ObjectToString(httpsession.getAttribute("uid")),
					null, dtStarttime, dtEndtime, nHost, caller, called, ext, agent, nBackup, nLock, nDelete, nAnswer,
					nDirection, nInside, nLength, nMaxLength, sUcid, sGuidId, sGroupGuid, null, null, null, null,2,sMark,null);
			if (!export.Init(System.getProperty("tab.path") + File.separator + "WebRoot" + sScript)) {
				return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain").build();
			}
			for (int i = 0; recfiles!=null && i < recfiles.size(); ++i) {
				RecfilesEx Item = recfiles.get(i);
				export.CommitRow(Item);
			}
		}
		String sFileName = export.GetFile();
		if (sFileName != null && sFileName.length() > 0) {
			java.io.File f = new java.io.File(sFileName);
			int pos = sFileName.lastIndexOf(".");
			return Response.ok(f)
					.header("Content-Disposition", "attachment;filename=\"" + export.GetFileName()
							+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)")).format(Calendar.getInstance().getTime())
									.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
							+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
					.build();
		}
		return Response.status(404).entity("FILE NOT FOUND" + sScript).type("text/plain").build();
	}

	// 下载批量录音
	private final class RecordFile {
		public String fileName;
		public String guid;
		public String recordName;
		public String extension;
		public String agent;
	}

	// API文档: (value = "下载指定录音文件",notes = "下载由Json提供的录音文件
	// [{fileName:\"下载后的显示文件名\",guid:\"录音唯一标识\"}]")
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
				record.recordName = attacheds.get(nIdx).get("recordName");
				int npos = record.recordName.lastIndexOf(".");
				//record.fileName = attacheds.get(nIdx).get("fileName") + (Runner.nPlayMode == 0 ? ".MP3" : (npos>0 ? record.recordName.substring(npos) : ""));
				record.fileName = attacheds.get(nIdx).get("fileName") ;
				record.guid = attacheds.get(nIdx).get("guid");
				record.agent = attacheds.get(nIdx).get("agent");
				record.extension = attacheds.get(nIdx).get("extension");
				recordList.add(record);
			}
		} catch (IOException e) {
			logger.error("ERR:", e);
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
			logger.error("ERR:", e);
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
				File fileHandle = tab.util.getLocalRecordFileLib.getLocalRecordFile(Item.guid,Item.extension,AudioFormat,Runner.sPlayUrl,Runner.sPlayHost,Runner.sPlayUsername,Runner.sPlayPassword,Item.agent);
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
				logger.error(e);
			} catch (java.io.FileNotFoundException e) {
				logger.error(e);
			} catch (java.io.IOException e) {
				logger.error(e);
			} catch (Throwable e) {
				logger.error(e);
			}
		}
		try {
			httpclient.close();
		} catch (IOException e) {
			logger.error(e);
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

	// API文档: (value = "设置录音所属部门",notes = "最多设置1000条录音记录")
	@POST
	@Path("/UIUpdateRecfiles")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIUpdateRecfiles(@Context HttpServletRequest R, @FormParam("roleguid") String sRoleGuid,
			@FormParam("idList") java.util.List<String> idList) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		result.put("msg", "不能修改Genesys系统数据库!");
		return Response.status(200).entity(result).build();
	}
	@POST
	@Path("/UIAddUsers")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIAddUsers(@Context HttpServletRequest R,
			@FormParam("roleguid") String sRoleGuid,
			@FormParam("extensionList") java.util.List<String> Extensions) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		String existExtensionSet="";
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			Transaction ts = dbsession.beginTransaction();
		
			HashSet<String> extensionListSet=new HashSet<String>(Extensions);
			try {
				for (String sExt : extensionListSet) {
					String sQuery = " from Rbacuserauths where username=:agent or  agent=:agent ";
					Query qyery=dbsession.createQuery(sQuery).setString("agent", sExt);
					List<Rbacuserauths> list =qyery.list();
					if(list.size()>0) {//这个工号已或者用户名经存在
						existExtensionSet+=","+sExt;
					}else {//添加
						Rbacuser user = new Rbacuser();
						user.setHeadimgurl("");
						user.setNickname(sExt+"("+sExt+")");
						user.setUserguid(UUID.randomUUID().toString().toUpperCase());
						dbsession.save(user);
						//dbsession.flush();
						Rbacuserauths userauth =  new Rbacuserauths();
						userauth.setPassword("");
						userauth.setAgent(sExt);
						userauth.setUsername(sExt);
						userauth.setUserguid(user.getUserguid());
						userauth.setRoleguid(Runner.sAuthorizedRoleGuid);
						userauth.setWeixinid("");
						userauth.setAlipayid("");
						userauth.setCreatedate(Calendar.getInstance().getTime());
						userauth.setUpdatedate(Calendar.getInstance().getTime());
						userauth.setLogindate(Calendar.getInstance().getTime());
						userauth.setIdentifier("");
						userauth.setEmail("");
						userauth.setMobile("");
						userauth.setStatus(1);
						dbsession.save(userauth);
						
						Rbacroleuser roleuser = new Rbacroleuser();
						RbacroleuserId roleuserid = new RbacroleuserId();
						roleuserid.setRoleguid(sRoleGuid);
						roleuserid.setUserguid(user.getUserguid());
						roleuser.setId(roleuserid);
						dbsession.save(roleuser);
						
						
					}
					
				}
				ts.commit();
				result.put("success", true);
				
			}catch (org.hibernate.HibernateException e) {
				if (ts.getStatus() == TransactionStatus.ACTIVE || ts.getStatus() == TransactionStatus.MARKED_ROLLBACK) {
					ts.rollback();
				} else
					ts.commit();
				logger.warn("ERROR:", e);
				result.put("msg", e.toString());
			} catch (Throwable e) {
				if (ts.getStatus() == TransactionStatus.ACTIVE || ts.getStatus() == TransactionStatus.MARKED_ROLLBACK) {
					ts.rollback();
				} else
					ts.commit();
				logger.warn("ERROR:", e);
				result.put("msg", e.toString());
			}finally{
				if(dbsession.isOpen()) {
					dbsession.close();
				}
			}
			
		}catch (org.hibernate.HibernateException e) {
			logger.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			logger.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		
		result.put("msg", existExtensionSet);
		return Response.status(200).entity(result).build();
	}
	
	
	// API文档: (value = "设置录音分机所属部门",notes = "")
	@POST
	@Path("/UIUpdateRecExtension")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIUpdateRecExtension(@Context HttpServletRequest R,@FormParam("serverAddress") String serverAddress,
			@FormParam("roleguid") String sRoleGuid, @FormParam("ipaddress") String sIpaddress,
			@FormParam("policy") String sPolicy, @FormParam("extensionList") java.util.List<String> Extensions) {
		HttpSession httpsession = R.getSession();
		tab.configServer.ValueString vs = new tab.configServer.ValueString(tab.util.Util.ObjectToString(serverAddress));
		try {
			tab.configServer.getInstance().getValue(main.Runner.ConfigName_, "URL", vs, "该模块对应的录音系统URL", "", false,
					false);
			if (serverAddress != null && serverAddress.length() > 0 && vs.value.equals(serverAddress) == false) {
				tab.configServer.ValueString responseContent = new tab.configServer.ValueString("");
				java.util.Map<String, Object> parameters = new java.util.HashMap<>();
				parameters.put("serverAddress", serverAddress);
				parameters.put("roleguid", sRoleGuid);
				parameters.put("ipaddress", sIpaddress);
				parameters.put("policy", sPolicy);
				parameters.put("extensionList", Extensions);
				int nStatusCode = tab.util.Util.post(serverAddress + "/tab/rec/UIUpdateRecExtension", parameters,
						responseContent);
				if (nStatusCode < 300) {
					return Response.status(200).entity(responseContent.value).build();
				}
				return Response.status(200).entity("{\"success\":false}").build();
			}
		} catch (ClassNotFoundException err) {
			logger.error("ERROR:", err);
			return Response.status(200).entity("{\"success\":false}").build();
		}
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if (Extensions == null) {
			result.put("msg", "Extensions is null");
			return Response.status(200).entity(result).build();
		}
		Integer nPolicy = tab.util.Util.ObjectToNumber(sPolicy, 0);
		Long lBeginIpaddress = 0L;
		try {
			if (sIpaddress != null && sIpaddress.length() > 0) {
				String regex = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
				java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex); // 编译正则表达式
				java.util.regex.Matcher matcher = pattern.matcher(sIpaddress); // 创建给定输入模式的匹配器
				if (!matcher.matches()) {
					result.put("msg", "IP地址的格式不正确");
					return Response.status(200).entity(result).build();
				}
				java.net.InetAddress addr = java.net.InetAddress.getByName(sIpaddress);
				byte[] byAddr = addr.getAddress();
				lBeginIpaddress = ((byAddr[0] & 0xFF) << (3 * 8)) + ((byAddr[1] & 0xFF) << (2 * 8))
						+ ((byAddr[2] & 0xFF) << (1 * 8)) + (byAddr[3] & 0xFF) & 0xffffffffl;
			}
			java.util.List<String> emptykeys = new java.util.ArrayList<String>();
			java.util.Map<String, ExtensionInfo> extensioninfos = GetExtensionInfo(emptykeys);
			Session dbsession = HibernateSessionFactory.getThreadSession();
			Transaction ts = dbsession.beginTransaction();
			try {
				for (String sExt : Extensions) {
					String sNewIpaddress = "";
					if (lBeginIpaddress > 0) {
						byte[] bytes = new byte[] { (byte) ((lBeginIpaddress >> 24) & 0xff),
								(byte) ((lBeginIpaddress >> 16) & 0xff), (byte) ((lBeginIpaddress >> 8) & 0xff),
								(byte) ((lBeginIpaddress >> 0) & 0xff), };
						sNewIpaddress = java.net.InetAddress.getByAddress(bytes).getHostAddress();
						lBeginIpaddress++;
					}
					if (extensioninfos.containsKey(sExt)) {
						configServer.getInstance().setValue("Phones", sExt, sNewIpaddress, "IP");
						configServer.getInstance().setValue("Phones", sExt, nPolicy, "POLICY");
						tab.rbac.RbacSystem.AuditLog("update", "Phones", tab.util.Util.NONE_GUID, 1, String.format("{\"extension\":\"%s\",\"ip\":\"%s\",\"policy\":%d}", sExt,sNewIpaddress,nPolicy), dbsession, ts, tab.util.Util.ObjectToString(httpsession!=null ? httpsession.getAttribute("uid") : tab.util.Util.NONE_GUID));
					} else {
						// 使用空闲的通道位置添加分机号
						if (emptykeys.size() == 0) {
							result.put("msg", "最大通道数不够");
							return Response.status(200).entity(result).build();
						}
						String skey = emptykeys.remove(0);
						vs.value = sExt;
						configServer.getInstance().getValue("Phones", skey, vs, "",
								tab.util.Util.ObjectToString(configServer.getInstance().getHostId()), false, true);
						if (vs.value == null || vs.value.equals(sExt) == false) {
							configServer.getInstance().setValue("Phones", skey, sExt,
									tab.util.Util.ObjectToString(configServer.getInstance().getHostId()));
							tab.rbac.RbacSystem.AuditLog("update", "Phones", tab.util.Util.NONE_GUID, 1, String.format("{\"channel\":\"%s\",\"extension\":\"%s\"}",skey,sExt), dbsession, ts, tab.util.Util.ObjectToString(httpsession!=null ? httpsession.getAttribute("uid") : tab.util.Util.NONE_GUID));
						}
						vs.value = sNewIpaddress;
						configServer.getInstance().getValue("Phones", sExt, vs, "", "IP", false, true);
						if (!vs.value.equals(sNewIpaddress)) {
							configServer.getInstance().setValue("Phones", sExt, sNewIpaddress, "IP");
							tab.rbac.RbacSystem.AuditLog("update", "Phones", tab.util.Util.NONE_GUID, 1, String.format("{\"extension\":\"%s\",\"ip\":\"%s\"}",sExt,sNewIpaddress), dbsession, ts, tab.util.Util.ObjectToString(httpsession!=null ? httpsession.getAttribute("uid") : tab.util.Util.NONE_GUID));
						}
						configServer.ValueInteger vi = new configServer.ValueInteger(nPolicy);
						configServer.getInstance().getValue("Phones", sExt, vi, "", "POLICY", false, true);
						if (!vi.value.equals(nPolicy)) {
							configServer.getInstance().setValue("Phones", sExt, nPolicy, "POLICY");
							tab.rbac.RbacSystem.AuditLog("update", "Phones", tab.util.Util.NONE_GUID, 1, String.format("{\"extension\":\"%s\",\"policy\":%d}",sExt,nPolicy), dbsession, ts, tab.util.Util.ObjectToString(httpsession!=null ? httpsession.getAttribute("uid") : tab.util.Util.NONE_GUID));
						}
					}
				}
				@SuppressWarnings("unchecked")
				java.util.List<Recextension> extensionList = dbsession
						.createQuery(" from Recextension where extension in(:exten)")
						.setParameterList("exten", Extensions).list();
				for (Recextension exten : extensionList) {
					exten.setRoleguid(sRoleGuid);
					dbsession.update(exten);
					tab.rbac.RbacSystem.AuditLog("update", "Recextension", tab.util.Util.NONE_GUID, 1, String.format("{\"extension\":\"%s\",\"roleguid\":\"%s\"}",exten.getExtension(),sRoleGuid), dbsession, ts, tab.util.Util.ObjectToString(httpsession!=null ? httpsession.getAttribute("uid") : tab.util.Util.NONE_GUID));
					for (int nIdx = Extensions.size() - 1; nIdx >= 0; nIdx--) {
						if (Extensions.get(nIdx).equals(exten.getExtension())) {
							Extensions.remove(nIdx);
						}
					}
				}
				for (String sExt : Extensions) {
					Recextension exten = new Recextension();
					exten.setExtension(sExt);
					exten.setRoleguid(sRoleGuid);
					dbsession.save(exten);
					tab.rbac.RbacSystem.AuditLog("update", "Recextension", tab.util.Util.NONE_GUID, 1, String.format("{\"extension\":\"%s\",\"roleguid\":\"%s\"}",exten.getExtension(),sRoleGuid), dbsession, ts, tab.util.Util.ObjectToString(httpsession!=null ? httpsession.getAttribute("uid") : tab.util.Util.NONE_GUID));
				}
				ts.commit();
				result.put("success", true);
			} catch (org.hibernate.HibernateException e) {
				if (ts.getStatus() == TransactionStatus.ACTIVE || ts.getStatus() == TransactionStatus.MARKED_ROLLBACK) {
					ts.rollback();
				} else
					ts.commit();
				logger.warn("ERROR:", e);
				result.put("msg", e.toString());
			} catch (Throwable e) {
				if (ts.getStatus() == TransactionStatus.ACTIVE || ts.getStatus() == TransactionStatus.MARKED_ROLLBACK) {
					ts.rollback();
				} else
					ts.commit();
				logger.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			logger.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			logger.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}

	// API文档: (value = "修改到新的部门")
	@POST
	@Path("/UIUpdateRecfilesRole")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIUpdateRecfilesRole(@Context HttpServletRequest R,
			@FormParam("newroleguid") /* "新的部门标识" */String sNewRoleguid,
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
			@FormParam("guidid") /* "通过录音唯一ID查询录音,忽略时间范围" */String sGuidId,
			@FormParam("groupguidid") /* "通过录音所属组查询,该组可以是坐席工号组,也可以是分机组" */String sGroupGuid) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			logger.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		tab.configServer.ValueInteger nTotalCount = new tab.configServer.ValueInteger(0);
		result.put("totalCount", nTotalCount.value);
		result.put("msg", "不能修改Genesys系统数据库!");
		return Response.status(200).entity(result).build();
		/*result.put("success",
				UpdateRecfilesRole(sNewRoleguid, Util.ObjectToString(httpsession.getAttribute("uid")), dtStarttime,
						dtEndtime, nHost, caller, called, ext, agent, nBackup, nLock, nDelete, nAnswer, nDirection,
						nInside, nLength, nMaxLength, sUcid, sGuidId, sGroupGuid, nTotalCount));
		result.put("totalCount", nTotalCount.value);
		return Response.status(200).entity(result).build();
		*/
	}

	// API文档: (value = "删除录音分机所属部门记录",notes = "")
	@POST
	@Path("/UIRemoveRecExtension")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIRemoveRecExtension(@FormParam("serverAddress") String serverAddress,
			@FormParam("extensionList") java.util.List<String> Extensions) {
		tab.configServer.ValueString vs = new tab.configServer.ValueString(tab.util.Util.ObjectToString(serverAddress));
		try {
			tab.configServer.getInstance().getValue(main.Runner.ConfigName_, "URL", vs, "该模块对应的录音系统URL", "", false,
					false);
			if (serverAddress != null && serverAddress.length() > 0 && vs.value.equals(serverAddress) == false) {
				tab.configServer.ValueString responseContent = new tab.configServer.ValueString("");
				java.util.Map<String, Object> parameters = new java.util.HashMap<>();
				parameters.put("serverAddress", serverAddress);
				parameters.put("extensionList", Extensions);
				int nStatusCode = tab.util.Util.post(serverAddress + "/tab/rec/UIRemoveRecExtension", parameters,
						responseContent);
				if (nStatusCode < 300) {
					return Response.status(200).entity(responseContent.value).build();
				}
				return Response.status(200).entity("{\"success\":false}").build();
			}
		} catch (ClassNotFoundException err) {
			logger.error("ERROR:", err);
			return Response.status(200).entity("{\"success\":false}").build();
		}
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		if (Extensions == null) {
			result.put("msg", Extensions);
			return Response.status(200).entity(result).build();
		}
		try {
			java.util.Map<String, ExtensionInfo> extensioninfos = GetExtensionInfo(null);
			for (String sExt : Extensions) {
				if (extensioninfos.containsKey(sExt)) {
					configServer.getInstance().setValue("Phones", extensioninfos.get(sExt).key, "",
							tab.util.Util.ObjectToString(configServer.getInstance().getHostId()));
					configServer.getInstance().remove("Phones", sExt, "IP");
					configServer.getInstance().remove("Phones", sExt, "POLICY");
				}
			}
			Session dbsession = HibernateSessionFactory.getThreadSession();
			Transaction ts = dbsession.beginTransaction();
			try {
				dbsession.createQuery("delete Recextension where extension in(:extensions)")
						.setParameterList("extensions", Extensions).executeUpdate();
				ts.commit();
				result.put("success", true);
			} catch (org.hibernate.HibernateException e) {
				if (ts.getStatus() == TransactionStatus.ACTIVE || ts.getStatus() == TransactionStatus.MARKED_ROLLBACK) {
					ts.rollback();
				} else
					ts.commit();
				logger.warn("ERROR:", e);
				result.put("msg", e.toString());
			} catch (Throwable e) {
				if (ts.getStatus() == TransactionStatus.ACTIVE || ts.getStatus() == TransactionStatus.MARKED_ROLLBACK) {
					ts.rollback();
				} else
					ts.commit();
				logger.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			logger.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			logger.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}

	class ExtensionInfo {
		ExtensionInfo(String key, String exten) {
			this.key = key;
			this.extension = exten;
		}

		public String key;
		public String extension;
		public String roleid;
		public String rolename;
		public String ipaddress;
		public Integer policy;
	}

	private java.util.Map<String, ExtensionInfo> GetExtensionInfo(java.util.List<String> EmptyKey)
			throws ClassNotFoundException {
		java.util.Map<String, ExtensionInfo> extensioninfos = new java.util.HashMap<>();
		java.util.Map<String, configServer.ValueInfo> mPhones = new java.util.HashMap<String, configServer.ValueInfo>();
		configServer.getInstance().getValue("Phones", mPhones,
				tab.util.Util.ObjectToString(configServer.getInstance().getHostId()), false, true);
		int nMaxChannel = 0;
		for (java.util.Map.Entry<String, configServer.ValueInfo> v : mPhones.entrySet()) {
			nMaxChannel = Math.max(tab.util.Util.ObjectToNumber(v.getKey(), 0), nMaxChannel);
			if (v.getValue() == null || v.getValue().sValue == null || v.getValue().sValue.length() == 0) {
				if (EmptyKey != null)
					EmptyKey.add(v.getKey());
			} else {
				extensioninfos.put(v.getValue().sValue, new ExtensionInfo(v.getKey(), v.getValue().sValue));
			}
		}
		if (EmptyKey != null) {
			java.util.Collections.sort(EmptyKey);
			configServer.ValueInteger vmax = new configServer.ValueInteger(0);
			configServer.getInstance().getValue("MyRecVoice", "MaxChannel", vmax, "最大语音资源数量", "", true, true);
			while (extensioninfos.size() + EmptyKey.size() < vmax.value) {
				nMaxChannel++;
				EmptyKey.add(tab.util.Util.ObjectToString(nMaxChannel));
			}
		}
		return extensioninfos;
	}
	@POST
	@Path("/UIUpdateRecfileMark")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIUpdateRecfileMark(@Context HttpServletRequest R, @FormParam("id") String sId,@FormParam("mark") String sMark) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		result.put("msg", "不能修改Genesys系统数据库!");
		return Response.status(200).entity(result).build();
	}
	
	@POST
	@Path("/UIGetPlaybackUrl")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetPlaybackUrl() {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("playMode", Runner.nPlayMode);
		result.put("success", true);
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/UIGetServerList")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetServerList() {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		try {
			java.util.Map<String, tab.configServer.ValueInfo> v = new java.util.HashMap<String, configServer.ValueInfo>();
			configServer.getInstance().getValue(main.Runner.ConfigName_, v, "URL", false, false);
			java.util.List<java.util.Map<String, String>> items = new java.util.ArrayList<java.util.Map<String, String>>();
			for (java.util.Map.Entry<String, ValueInfo> e : v.entrySet()) {
				java.util.HashMap<String, String> item = new java.util.HashMap<String, String>();
				item.put("serverId", e.getKey());
				item.put("serverAddress", e.getValue().sValue);
				items.add(item);
			}
			result.put("items", items);
			result.put("success", true);
		} catch (Throwable e) {
			logger.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}

	@GET
	@Path("/GetAsrExtension")
	@Produces("application/json" +";charset=utf-8")
	public Response GetAsrExtension(@Context HttpServletRequest R, @QueryParam("text") String sText,@QueryParam("default") @DefaultValue("1000") String sExtension) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		logger.info("text:"+sText + ", default:"+sExtension);
		result.put("extension", sExtension);
		result.put("success", false);
		if(AliyunToken.getInstance().getAccessToken().length()==0) {
			return Response.status(500).entity(result).build();
		}
		java.util.Map<String, Object> speech;
		try {
			speech = mapper.readValue(sText, new TypeReference<java.util.Map<String, Object>>() {
			});
			sText = tab.util.Util.ObjectToString(speech.get("speech"));
		} catch (IOException e) {
			logger.error("ERROR:",e);
		}
		Matcher matcher = Pattern.compile("\\d+").matcher(sText);
		String sPhoneNumber = "";
		while(matcher.find()) {
			sPhoneNumber = matcher.group(0);
		}
		if(sPhoneNumber.length()==sExtension.length()) {
			result.put("extension", sPhoneNumber);
			result.put("success", true);
			return Response.status(200).entity(result).build();
		}
		HanyuPinyinOutputFormat fmt = new HanyuPinyinOutputFormat();
		fmt.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		String sPinyinText;
		try {
			sPinyinText = PinyinHelper.toHanYuPinyinString(sText, fmt,"",true);
			logger.info("pinyitext:"+sPinyinText);
		} catch (BadHanyuPinyinOutputFormatCombination e) {
			logger.error("ERROR:",e);
			sPinyinText = "";
		}
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				//模糊查找数据库内分机号
				@SuppressWarnings("unchecked")
				java.util.List<Rbacuserauths> users = dbsession.createQuery(" from Rbacuserauths").list();
				for(int i=0;i<users.size();++i) {
					if(sText.contains(users.get(i).getUsername())){
						result.put("extension", users.get(i).getMobile());
						result.put("success", true);
						break;
					}else if(sPinyinText.contains(PinyinHelper.toHanYuPinyinString(users.get(i).getUsername(),fmt, "",true))) {
						result.put("extension", users.get(i).getMobile());
						result.put("success", true);
						break;
					}
				}
			} catch (org.hibernate.HibernateException|BadHanyuPinyinOutputFormatCombination e) {
				result.put("msg", e.toString());
				logger.error("ERROR:", e);
			} catch (Throwable e) {
				result.put("msg", e.toString());
				logger.error("ERROR:", e);
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			result.put("msg", e.toString());
			logger.error("ERROR:",e);
		} catch(Throwable e) {
			result.put("msg", e.toString());
			logger.error("ERROR:",e);
		}
		return Response.status(200).entity(result).build();
	}
	@POST
	@Path("/UIGetRecExtension")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetRecExtension(@FormParam("serverAddress") String serverAddress) {
		tab.configServer.ValueString vs = new tab.configServer.ValueString(tab.util.Util.ObjectToString(serverAddress));
		try {
			tab.configServer.getInstance().getValue(main.Runner.ConfigName_, "URL", vs, "该模块对应的录音系统URL", "", false,
					false);
			if (serverAddress != null && serverAddress.length() > 0 && vs.value.equals(serverAddress) == false) {
				tab.configServer.ValueString responseContent = new tab.configServer.ValueString("");
				java.util.Map<String, Object> parameters = new java.util.HashMap<>();
				parameters.put("serverAddress", serverAddress);
				int nStatusCode = tab.util.Util.post(serverAddress + "/tab/rec/UIGetRecExtensionPrivate", parameters,
						responseContent);
				if (nStatusCode < 300) {
					return Response.status(200).entity(responseContent.value).build();
				}
				return Response.status(200).entity("{\"success\":false}").build();
			}
		} catch (ClassNotFoundException err) {
			logger.error("ERROR:", err);
			return Response.status(200).entity("{\"success\":false}").build();
		}
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		result.put("serverAddress", serverAddress);
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				@SuppressWarnings("unchecked")
				java.util.List<Object[]> extensions = dbsession.createQuery(
						"select A.extension as extension,A.roleguid as roleid,B.rolename as rolename from Recextension A, Rbacrole B where A.roleguid=B.roleguid")
						.list();
				java.util.Map<String, ExtensionInfo> extensioninfos = GetExtensionInfo(null);
				for (int i = 0; i < extensions.size(); i++) {
					String sExten = tab.util.Util.ObjectToString(extensions.get(i)[0]);
					if (extensioninfos.containsKey(sExten)) {
						ExtensionInfo exinfo = extensioninfos.get(sExten);
						exinfo.roleid = tab.util.Util.ObjectToString(extensions.get(i)[1]);
						exinfo.rolename = tab.util.Util.ObjectToString(extensions.get(i)[2]);
					}
				}
				java.util.Map<String, configServer.ValueInfo> mIps = new java.util.HashMap<String, configServer.ValueInfo>();
				configServer.getInstance().getValue("Phones", mIps, "IP", false, true);
				for (java.util.Map.Entry<String, configServer.ValueInfo> v : mIps.entrySet()) {
					if (extensioninfos.containsKey(v.getKey())) {
						ExtensionInfo exinfo = extensioninfos.get(v.getKey());
						exinfo.ipaddress = v.getValue().sValue;
					}
				}
				java.util.Map<String, configServer.ValueInfo> mPolicys = new java.util.HashMap<String, configServer.ValueInfo>();
				configServer.getInstance().getValue("Phones", mPolicys, "POLICY", false, true);
				for (java.util.Map.Entry<String, configServer.ValueInfo> v : mPolicys.entrySet()) {
					if (extensioninfos.containsKey(v.getKey())) {
						ExtensionInfo exinfo = extensioninfos.get(v.getKey());
						exinfo.policy = tab.util.Util.ObjectToNumber(v.getValue().sValue, 0);
					}
				}
				result.put("extensions", extensioninfos.values());
			} catch (Throwable e) {
				logger.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
			result.put("success", true);
		} catch (Throwable e) {
			logger.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/UIGetRecExtensionPrivate")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetRecExtensionPrivate() {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				@SuppressWarnings("unchecked")
				java.util.List<Object[]> extensions = dbsession.createQuery(
						"select A.extension as extension,A.roleguid as roleid,B.rolename as rolename from Recextension A, Rbacrole B where A.roleguid=B.roleguid")
						.list();
				java.util.Map<String, ExtensionInfo> extensioninfos = GetExtensionInfo(null);
				for (int i = 0; i < extensions.size(); i++) {
					String sExten = tab.util.Util.ObjectToString(extensions.get(i)[0]);
					if (extensioninfos.containsKey(sExten)) {
						ExtensionInfo exinfo = extensioninfos.get(sExten);
						exinfo.roleid = tab.util.Util.ObjectToString(extensions.get(i)[1]);
						exinfo.rolename = tab.util.Util.ObjectToString(extensions.get(i)[2]);
					}
				}
				java.util.Map<String, configServer.ValueInfo> mIps = new java.util.HashMap<String, configServer.ValueInfo>();
				configServer.getInstance().getValue("Phones", mIps, "IP", false, true);
				for (java.util.Map.Entry<String, configServer.ValueInfo> v : mIps.entrySet()) {
					if (extensioninfos.containsKey(v.getKey())) {
						ExtensionInfo exinfo = extensioninfos.get(v.getKey());
						exinfo.ipaddress = v.getValue().sValue;
					}
				}
				java.util.Map<String, configServer.ValueInfo> mPolicys = new java.util.HashMap<String, configServer.ValueInfo>();
				configServer.getInstance().getValue("Phones", mPolicys, "POLICY", false, true);
				for (java.util.Map.Entry<String, configServer.ValueInfo> v : mPolicys.entrySet()) {
					if (extensioninfos.containsKey(v.getKey())) {
						ExtensionInfo exinfo = extensioninfos.get(v.getKey());
						exinfo.policy = tab.util.Util.ObjectToNumber(v.getValue().sValue, 0);
					}
				}
				result.put("extensions", extensioninfos.values());
			} catch (Throwable e) {
				logger.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
			result.put("success", true);
		} catch (Throwable e) {
			logger.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}
	// API文档: (value = "录音汇总报表",notes = "")
	@POST
	@Path("/UIReportRecSummary")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIReportRecSummary(@Context HttpServletRequest R, @FormParam("starttime") RESTDateParam dtStarttime,
			@FormParam("endtime") RESTDateParam dtEndtime, @FormParam("type") Integer nType,
			@FormParam("script") String sScript) {
		if (Runner.bCompatibleMode) {
			return ReportRecSummaryCompatibleMode(R, dtStarttime, dtEndtime, nType, sScript);
		}
		return ReportRecSummary(R, dtStarttime, dtEndtime, nType, sScript);
	}

	@SuppressWarnings("unused")
	private static class RecSummaryInfo{
		public Integer  nTotalCount;
		public Integer  nBackupCount;
		public Integer  nInboundCount;
		public Integer  nOutboundCount;
		public Integer  nSeconds;
		public Integer  nYear;
		public Integer  nMonth;
		public Integer  nDay;
		public Integer  nHour;
		public String  roleid;
		public Integer  nHost;
		public Integer  nType;
		public String rolename;
	}
	/*
	public final static String sInteraction_type_name = "interaction_type_code";
	//*/
	///*
	public final static String sInteraction_type_name = "interaction_type";
	//*/
	private static java.util.List<RecSummaryInfo> MapToRecSummaryInfo(java.util.List<java.util.Map<String, Object>> items) {
		java.util.List<RecSummaryInfo> summarys = new java.util.ArrayList<RecSummaryInfo>();
		for(int i=0;i<items.size();i++) {
			java.util.Map<String, Object> item = items.get(i);
			RecSummaryInfo  summary = new RecSummaryInfo();
			summary.nBackupCount = 0;
			summary.nDay = tab.util.Util.ObjectToNumber(item.get("nday"),0);
			summary.nHour = tab.util.Util.ObjectToNumber(item.get("nhour"),0);
			summary.nInboundCount = tab.util.Util.ObjectToNumber(item.get("ninboundcount"),0);
			summary.nOutboundCount =  tab.util.Util.ObjectToNumber(item.get("noutboundcount"),0);
			summary.nMonth = tab.util.Util.ObjectToNumber(item.get("nmonth"),0);
			summary.nSeconds = tab.util.Util.ObjectToNumber(item.get("nseconds"),0);
			summary.nTotalCount = tab.util.Util.ObjectToNumber(item.get("ntotalcount"),0);
			summary.nType = tab.util.Util.ObjectToNumber(item.get("ntype"),0);
			summary.nYear = tab.util.Util.ObjectToNumber(item.get("nyear"),0);
			summary.roleid = RbacClient.ROOT_ROLEGUID;
			summary.nHost = 0;
			summarys.add(summary);
		}
		return summarys;
	}
	private Response ReportRecSummary(HttpServletRequest R, RESTDateParam dtStarttime, RESTDateParam dtEndtime,
			Integer nType, String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null || httpsession.getAttribute("uid") == null) {
			logger.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		if(dtStarttime==null || dtEndtime==null) {
			result.put("msg", "starttime or endtime is null!");
			return Response.status(200).entity(result).build();
		}

		String sWhere = " A.start_time>=:start and A.start_time<=:end and A.talk_duration>0";
		String sSelect = "select count(*) as nTotalCount,"
				+ "0 as nBackupCount,"
				+ "SUM(case when upper(A."+sInteraction_type_name+")!='OUTBOUND' then 1 else 0 end) as nInboundCount,"
				+ "SUM(case when upper(A."+sInteraction_type_name+")='OUTBOUND' then 1 else 0 end) as nOutboundCount,"
				+ "SUM(A.talk_duration) as nSeconds";

		String sGroupBy = " group by EXTRACT(YEAR FROM A.start_time),EXTRACT(MONTH FROM A.start_time),EXTRACT(DAY FROM A.start_time),TO_CHAR(A.start_time,'hh24')";
		String sOrderBy = " order by nYear,nMonth,nDay,nHour";
        if (GHibernateSessionFactory.databaseType == "SQLServer") {
             sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time),DATEPART(day , A.start_time),DATEPART(hour,A.start_time)";
             sOrderBy = " order by nYear,nMonth,nDay,nHour";
         	switch (nType) {
    		default:
    		case 0:// hour
    			sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time) as nDay,DATEPART(hour,A.start_time) as nHour,0 as nType";
    			break;
    		case 1:// day
    			sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,DATEPART(day , A.start_time) as nDay,1 as nType";
    			sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time) ,DATEPART(day , A.start_time)";
    			sOrderBy = " order by nYear,nMonth,nDay";
    			break;
    		case 2:// week
    			sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(week , A.start_time) as nWeek,2 as nType";
    			sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(week , A.start_time)";
    			sOrderBy = " order by nYear,nWeek";
    			break;
    		case 3:// month
    			sSelect += ",DATEPART(year , A.start_time) as nYear,DATEPART(month , A.start_time) as nMonth,3 as nType";
    			sGroupBy = " group by DATEPART(year , A.start_time),DATEPART(month , A.start_time)";
    			sOrderBy = " order by nYear,nMonth";
    			break;
    		}
		}else {
		switch (nType) {
		default:
		case 0:// hour
			sSelect += ",EXTRACT(YEAR FROM A.start_time) as nYear,EXTRACT(MONTH FROM A.start_time) as nMonth,EXTRACT(DAY FROM A.start_time) as nDay,TO_CHAR(A.start_time,'hh24') as nHour,0 as nType";
			break;
		case 1:// day
			sSelect += ",EXTRACT(YEAR FROM A.start_time) as nYear,EXTRACT(MONTH FROM A.start_time) as nMonth,EXTRACT(DAY FROM A.start_time) as nDay,1 as nType";
			sGroupBy = " group by EXTRACT(YEAR FROM A.start_time),EXTRACT(MONTH FROM A.start_time),EXTRACT(DAY FROM A.start_time)";
			sOrderBy = " order by nYear,nMonth,nDay";
			break;
		case 2:// week
			sSelect += ",EXTRACT(YEAR FROM A.start_time) as nYear,week(A.start_time) as nWeek,2 as nType";
			sGroupBy = " group by EXTRACT(YEAR FROM A.start_time),week(A.start_time)";
			sOrderBy = " order by nYear,nWeek";
			break;
		case 3:// month
			sSelect += ",EXTRACT(YEAR FROM A.start_time) as nYear,EXTRACT(MONTH FROM A.start_time) as nMonth,3 as nType";
			sGroupBy = " group by EXTRACT(YEAR FROM A.start_time),EXTRACT(MONTH FROM A.start_time)";
			sOrderBy = " order by nYear,nMonth";
			break;
		}
		}
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				String sUId = Util.ObjectToString(httpsession.getAttribute("uid"));
				boolean bRoles = false, bUserguids = false;
				java.util.Set<String> roles = RbacClient.getRoleGuidsForRole(sUId, null, dbsession, true);
				java.util.Set<String> userguids = null;
				if (roles != null && roles.size() > 0) {
					if (roles.contains(RbacClient.ROOT_ROLEGUID)) {
						// 管理员组的查询操作，无权限限制
					} else {
						// 坐席组
						if (userguids == null) {
							userguids = new java.util.HashSet<>();
						}
						for(String role: roles) {
							userguids.addAll(tab.rbac.RbacClient.getUserAgents(sUId, role, false));
						}
						java.util.Map<String, Object> agentinfo = GsonUtil.getInstance().fromJson(RbacClient.getUserAuths(sUId, sUId),new TypeToken<java.util.Map<String, Object>>(){}.getType());
						if(agentinfo!=null) {
							userguids.add(tab.util.Util.ObjectToString(agentinfo.get("agent")));
						}
						sWhere += " and A.employee_id in(:userguids)";
						bUserguids = true;
					}
				} else {
					// 坐席组
					if (userguids == null) {
						userguids = new java.util.HashSet<>();
					}
					for(String role: roles) {
						userguids.addAll(tab.rbac.RbacClient.getUserAgents(sUId, role, false));
					}
					sWhere += " and A.employee_id in(:userguids)";
					bUserguids = true;
				}
				String sSqlStr = sSelect + " from call_detial A where " + sWhere + sGroupBy + sOrderBy;
				logger.info(sSqlStr);
				SQLQuery query = gdbsession.createSQLQuery(sSqlStr);
				query.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
				if (bRoles)
					query.setParameterList("roles", roles);
				if (bUserguids)
					query.setParameterList("userguids", userguids);
				query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
				@SuppressWarnings("unchecked")
				java.util.List<RecSummaryInfo> recordList = MapToRecSummaryInfo(query.list());
				java.util.Set<String> RoleList = new java.util.HashSet<>();
				for (int i = 0; i < recordList.size(); i++) {
					RoleList.add(Util.ObjectToString(recordList.get(i).roleid));
				}
				java.util.Map<String, String> RoleMap = new java.util.HashMap<String, String>();
				if (RoleList.size() > 0) {
					@SuppressWarnings("unchecked")
					java.util.List<Object[]> maps = (java.util.List<Object[]>) dbsession.createQuery(
							"select roleguid as roleguid, rolename as rolename from Rbacrole where roleguid in(:roleguid)")
							.setParameterList("roleguid", RoleList).list();
					for (Object[] map : maps) {
						RoleMap.put(map[0].toString(), map[1].toString());
					}
				}
				tab.MyExportExcelFile export = null;

				if (sScript == null || sScript.length() == 0) {

				} else {
					export = new tab.MyExportExcelFile();
					if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator") + "WebRoot"
							+ sScript)) {
						gdbsession.close();
						dbsession.close();
						return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain").build();
					}
				}

				if (sScript == null || sScript.length() == 0) {
					for (int i = 0; i < recordList.size(); i++) {
						RecSummaryInfo call = recordList.get(i);
						call.rolename = RoleMap.get(call.roleid);
					}
				} else {
					for (int i = 0; i < recordList.size(); i++) {
						RecSummaryInfo call = recordList.get(i);
						call.rolename = RoleMap.get(call.roleid);
						export.CommitRow(call);
					}
				}
				if (sScript == null || sScript.length() == 0) {
					result.put("list", recordList);
					result.put("success", true);
				} else {
					String sFileName = export.GetFile();
					if (sFileName != null && sFileName.length() > 0) {
						java.io.File f = new java.io.File(sFileName);
						int pos = sFileName.lastIndexOf(".");
						gdbsession.close();
						dbsession.close();
						return Response.ok(f)
								.header("Content-Disposition",
										"attachment;filename=\"" + export.GetFileName()
												+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
														.format(Calendar.getInstance().getTime())
														.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
												+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
								.build();
					}
				}
			} catch (Throwable e) {
				logger.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			gdbsession.close();
			dbsession.close();
		} catch (Throwable e) {
			logger.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		if (sScript == null || sScript.length() == 0) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(404).entity("no datas").type("text/plain").build();
	}

	private Response ReportRecSummaryCompatibleMode(HttpServletRequest R, RESTDateParam dtStarttime,
			RESTDateParam dtEndtime, Integer nType, String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null || httpsession.getAttribute("uid") == null) {
			logger.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		String sWhere = " A.systemtim>=:start and A.systemtim<=:end";
		String sSelect = "select new Map(count(*) as nTotalCount,"
				+ "SUM(case when bitand(A.states,2)=2 then 1 else 0 end) as nBackupCount,"
				+ "SUM(case when bitand(A.direction,1)=1 then 0 else 1 end) as nInboundCount,"
				+ "SUM(case when bitand(A.direction,1)=1 then 1 else 0 end) as nOutboundCount,"
				+ "SUM(A.seconds) as nSeconds";

		String sGroupBy = " group by YEAR(A.systemtim),MONTH(A.systemtim),DAY(A.systemtim),HOUR(A.systemtim),A.roleid,A.host";
		String sOrderBy = " order by A.host,A.roleid,nYear,nMonth,nDay,nHour";
		switch (nType) {
		default:
		case 0:// hour
			sSelect += ",YEAR(A.systemtim) as nYear,MONTH(A.systemtim) as nMonth,DAY(A.systemtim) as nDay,HOUR(A.systemtim) as nHour,A.roleid as roleid,A.host as host,0 as nType)";
			break;
		case 1:// day
			sSelect += ",YEAR(A.systemtim) as nYear,MONTH(A.systemtim) as nMonth,DAY(A.systemtim) as nDay,A.roleid as roleid,A.host as host,1 as nType)";
			sGroupBy = " group by YEAR(A.systemtim),MONTH(A.systemtim),DAY(A.systemtim),A.host,A.roleid";
			sOrderBy = " order by A.host,A.roleid,nYear,nMonth,nDay";
			break;
		case 2:// week
			sSelect += ",YEAR(A.systemtim) as nYear,week(A.systemtim) as nWeek,A.roleid as roleid,A.host as host,2 as nType)";
			sGroupBy = " group by YEAR(A.systemtim),week(A.systemtim),A.host,A.roleid";
			sOrderBy = " order by A.host,A.roleid,nYear,nWeek";
			break;
		case 3:// month
			sSelect += ",YEAR(A.systemtim) as nYear,MONTH(A.systemtim) as nMonth,A.roleid as roleid,A.host as host,3 as nType)";
			sGroupBy = " group by YEAR(A.systemtim),MONTH(A.systemtim),A.host,A.roleid";
			sOrderBy = " order by A.host,A.roleid,nYear,nMonth";
			break;
		}
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				String sUId = Util.ObjectToString(httpsession.getAttribute("uid"));
				boolean bRoles = false, bUserguids = false;
				java.util.Set<String> roles = tab.rbac.RbacClient.getRoleGuidsForRole(sUId, null, dbsession, true);
				java.util.Set<String> userguids = null;
				if (roles != null && roles.size() > 0) {
					if (roles.contains(tab.rbac.RbacClient.ROOT_ROLEGUID)) {
						// 管理员组的查询操作，无权限限制
					} else {
						// 坐席组
						userguids = tab.rbac.RbacClient.getUserGuidsForRole(sUId, null, dbsession, true);
						if (userguids == null) {
							userguids = new java.util.HashSet<>();
						}
						userguids.add(sUId);
						sWhere += " and (A.roleid in(:roles)";
						bRoles = true;
						sWhere += " or exists ( select 1 from Rbacuserauths B where (length(B.agent)>0 and B.agent=A.agent) and B.userguid in (:userguids) )";
						sWhere += ")";
						bUserguids = true;
					}
				} else {
					// 坐席组
					userguids = tab.rbac.RbacClient.getUserGuidsForRole(sUId, null, dbsession, true);
					if (userguids == null) {
						userguids = new java.util.HashSet<>();
					}
					userguids.add(sUId);
					sWhere += " and exists ( select 1 from Rbacuserauths B where (length(B.agent)>0 and B.agent=A.agent) and B.userguid in (:userguids) )";
					bUserguids = true;
				}
				Query query = dbsession.createQuery(sSelect + " from Recphone A where " + sWhere + sGroupBy + sOrderBy)
						.setTimestamp("start", dtStarttime.getDate()).setTimestamp("end", dtEndtime.getDate());
				if (bRoles)
					query.setParameterList("roles", roles);
				if (bUserguids)
					query.setParameterList("userguids", userguids);
				@SuppressWarnings("unchecked")
				java.util.List<java.util.Map<String, Object>> recordList = query.list();
				java.util.Set<String> RoleList = new java.util.HashSet<>();
				for (int i = 0; i < recordList.size(); i++) {
					RoleList.add(Util.ObjectToString(recordList.get(i).get("roleid")));
				}
				java.util.Map<String, String> RoleMap = new java.util.HashMap<String, String>();
				if (RoleList.size() > 0) {
					@SuppressWarnings("unchecked")
					java.util.List<Object[]> maps = (java.util.List<Object[]>) dbsession.createQuery(
							"select roleguid as roleguid, rolename as rolename from Rbacrole where roleguid in(:roleguid)")
							.setParameterList("roleguid", RoleList).list();
					for (Object[] map : maps) {
						RoleMap.put(map[0].toString(), map[1].toString());
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
					for (int i = 0; i < recordList.size(); i++) {
						java.util.Map<String, Object> call = recordList.get(i);
						call.put("rolename", RoleMap.get(call.get("roleid")));
					}
				} else {
					for (int i = 0; i < recordList.size(); i++) {
						java.util.HashMap<String, Object> call = (HashMap<String, Object>) recordList.get(i);
						call.put("rolename", RoleMap.get(call.get("roleid")));
						export.CommitRow(call);
					}
				}
				if (sScript == null || sScript.length() == 0) {
					result.put("list", recordList);
					result.put("success", true);
				} else {
					String sFileName = export.GetFile();
					if (sFileName != null && sFileName.length() > 0) {
						java.io.File f = new java.io.File(sFileName);
						int pos = sFileName.lastIndexOf(".");
						dbsession.close();
						return Response.ok(f)
								.header("Content-Disposition",
										"attachment;filename=\"" + export.GetFileName()
												+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
														.format(Calendar.getInstance().getTime())
														.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
												+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
								.build();
					}
				}
			} catch (Throwable e) {
				logger.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
		} catch (Throwable e) {
			logger.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		if (sScript == null || sScript.length() == 0) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(404).entity("no datas").type("text/plain").build();
	}

	// API文档: (value = "根据录音ID返回识别的文字",notes = "")
	@POST
	@Path("/UIGetRecText")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UIGetRecText(@Context HttpServletRequest R, @FormParam("recordid") String sRecordId) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				@SuppressWarnings("unchecked")
				java.util.List<Reccontent> items = dbsession
						.createQuery(" from Reccontent where id.id=:recordid order by id.filestart,id.fileend asc")
						.setString("recordid", sRecordId).list();
				result.put("items", items);
				result.put("success", true);
			} catch (Throwable e) {
				logger.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
		} catch (Throwable e) {
			logger.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}

	@GET
	@Path("/recordings/{ucid}")
	public Response recordings(@Context HttpServletRequest R,@PathParam("ucid") String sUcidPath, @QueryParam("ucid")String sUcid,@DefaultValue("") @QueryParam("exten")String sExten,
			@DefaultValue("") @QueryParam("agent")String agent) {
		if(sUcid==null && sUcidPath==null) {
			return Response.status(404).entity("paramters error: ucidpath=" + sUcidPath + ", ucid="+sUcid + ", exten=" + sExten )
					.type("text/plain").build();
		}
		if(sUcidPath!=null && sUcidPath.length()>0) {
			sUcid = sUcidPath;
		}
		tab.configServer.ValueString AudioFormat = new tab.configServer.ValueString("");
		File fileHandle = tab.util.getLocalRecordFileLib.getLocalRecordFile(sUcid,sExten,AudioFormat,Runner.sPlayUrl,Runner.sPlayHost,Runner.sPlayUsername,Runner.sPlayPassword,agent);
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
	

	
	@POST
	@Path("/AddNewBatch") //http://172.16.249.63:55511/tab/rec/AddNewBatch
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response AddNewBatch(@Context HttpServletRequest R) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();

		result.put("success", false);
		logger.info("AddNewBatch");
		return Response.status(200).entity(result).build();
	}
	@POST
	@Path("/UpdateSkillBatch") //http://172.16.249.63:55511/tab/rec/AddNewBatch
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response UpdateSkillBatch(@Context HttpServletRequest R) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", GenesysStatServer.UpdateAgentSkills());
		logger.info("UpdateSkillBatch");
		return Response.status(200).entity(result).build();
	}
}
