package tab;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.hibernate.Session;

import com.genesyslab.platform.applicationblocks.com.ConfServiceFactory;
import com.genesyslab.platform.applicationblocks.com.IConfService;
import com.genesyslab.platform.applicationblocks.warmstandby.WarmStandbyConfiguration;
import com.genesyslab.platform.applicationblocks.warmstandby.WarmStandbyService;
import com.genesyslab.platform.commons.connection.configuration.ClientADDPOptions.AddpTraceMode;
import com.genesyslab.platform.commons.connection.configuration.PropertyConfiguration;
import com.genesyslab.platform.commons.protocol.ChannelClosedEvent;
import com.genesyslab.platform.commons.protocol.ChannelErrorEvent;
import com.genesyslab.platform.commons.protocol.ChannelListener;
import com.genesyslab.platform.commons.protocol.ChannelState;
import com.genesyslab.platform.commons.protocol.Endpoint;
import com.genesyslab.platform.commons.protocol.Message;
import com.genesyslab.platform.commons.protocol.MessageHandler;
import com.genesyslab.platform.commons.protocol.Protocol;
import com.genesyslab.platform.commons.protocol.ProtocolException;
import com.genesyslab.platform.reporting.protocol.StatServerProtocol;
import com.genesyslab.platform.reporting.protocol.statserver.AgentStatus;
import com.genesyslab.platform.reporting.protocol.statserver.DnStatus;
import com.genesyslab.platform.reporting.protocol.statserver.Notification;
import com.genesyslab.platform.reporting.protocol.statserver.NotificationMode;
import com.genesyslab.platform.reporting.protocol.statserver.PlaceStatus;
import com.genesyslab.platform.reporting.protocol.statserver.StatisticMetric;
import com.genesyslab.platform.reporting.protocol.statserver.StatisticObject;
import com.genesyslab.platform.reporting.protocol.statserver.StatisticObjectType;
import com.genesyslab.platform.reporting.protocol.statserver.StatisticProfile;
import com.genesyslab.platform.reporting.protocol.statserver.events.EventInfo;
import com.genesyslab.platform.reporting.protocol.statserver.requests.RequestCloseStatistic;
import com.genesyslab.platform.reporting.protocol.statserver.requests.RequestGetStatistic;
import com.genesyslab.platform.reporting.protocol.statserver.requests.RequestGetStatisticProfile;
import com.genesyslab.platform.reporting.protocol.statserver.requests.RequestOpenStatistic;
import com.genesyslab.platform.reporting.protocol.statserver.requests.RequestPeekStatistic;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import hbm.OffersetTransformers;
import hbm.factory.HibernateSessionFactory;
import hbm.model.Rbacuserauths;
import hbm.model.Recextension;
import main.Runner;
import tab.jetty.StatWebSocketServlet;
import tab.rbac.RbacSystem;
import tab.util.Util;

public class GenesysStatServer {
	public static Log log = LogFactory.getLog(GenesysStatServer.class);
	private static GsonBuilder gsonBuilder = new GsonBuilder().serializeNulls();
	private static Integer STATISTIC_REFID_AGENT = 10000;
	private static Integer STATISTIC_REFID_DN = 20000;
	private static Integer STATISTIC_REFID_QUEUE = 30000;
	private static String TENANT_NAME = "Environment";
	private StatServerProtocol stat_protocol;
	public final int WaitForNextCall  = 4;
	public final int OffHook  = 5;
	public final int CallDialing  = 6;
	public final int CallRinging  = 7;
	public final int NotReadyForNextCall  = 8;
	public final int AfterCallWork  = 9;
	public final int CallOnHold  = 13;
	public final int ASM_Engaged  = 16;
	public final int ASM_Outbound  = 17;
	public final int CallUnknown  = 18;
	public final int CallConsult  = 19;
	public final int CallInternal  = 20;
	public final int CallOutbound  = 21;
	public final int CallInbound  = 22;
	private GenesysStatServer() {}
	public static GenesysStatServer Instance = new GenesysStatServer();
	public static class MapObj{
		public String typename;
		public String subname;
		public int requestid;
	}
	public static HashMap<Integer,MapObj> mapobj=null;
	public static class StatModel {
		public String sender;
		public String wsessionId;
		public java.util.List<QueueItem> queues;
		public java.util.List<ExtensionItem> extensions;
		public java.util.List<AgentGroup> agentgroup;
		public java.util.List<QueueGroup> queuegroup;
		public Boolean init;
		public StatModel() {
			this.extensions = new java.util.ArrayList<GenesysStatServer.ExtensionItem>();
			this.queues = new java.util.ArrayList<GenesysStatServer.QueueItem>();
			this.agentgroup = new java.util.ArrayList<GenesysStatServer.AgentGroup>();
			this.queuegroup = new java.util.ArrayList<GenesysStatServer.QueueGroup>();
		}
	}
	public static class AgentGroup{
		public int CurrentReadyAgents ;
		public int CurrNumberInboundStatuses ;
		public int CurrNumberInternalStatuses  ;
		public int CurrNumberOutboundStatuses ;
		public int CurrNumberACWStatuses ;
		public int CurrNumberNotReadyStatuses ;
		public AgentGroup() {
		}
	}
	public static class QueueGroup{
		public int Total_Calls_Entered ;
		public QueueGroup() {
		}
	}
	public static class QueueItem{
		public String group;
		public java.util.List<String> callers;
		public java.util.List<AgentItem> agents;
		public QueueItem(String sGroup) {
			this.group = sGroup;
			this.callers = new java.util.ArrayList<String>();
			this.agents = new java.util.ArrayList<GenesysStatServer.AgentItem>();
		}
		public QueueItem addCaller(String sCaller) {
			this.callers.add(sCaller);
			return this;
		}
	}
	public static class AgentItem{
		public Long time;
		public String agent;
		public String extension;
	}
	public static class ExtensionItem{
		public java.util.List<Integer> group;
		public String username;
		public String agent;
		public String extension;
		public String caller;
		public String called;
		public Boolean ready;
		public Integer state;
		public Integer host;
		public String recordid;
		public String callstate;
		public String roleguid;
		public ExtensionItem(String agent,String extension,String username){
			this.agent = agent;
			this.extension = extension;
			if(username!=null)this.username = username;
			this.group = new java.util.ArrayList<Integer>();
			this.caller = StringUtils.EMPTY;
			this.called = StringUtils.EMPTY;
			this.ready = false;
			this.state = 0;
			this.host = 0;
			this.recordid = StringUtils.EMPTY;
			this.callstate = StringUtils.EMPTY;
		}
		public ExtensionItem(String agent,String extension,String username,String roleguid){
			this.agent = agent;
			this.extension = extension;
			this.roleguid = roleguid;
			if(username!=null)this.username = username;
			this.group = new java.util.ArrayList<Integer>();
			this.caller = StringUtils.EMPTY;
			this.called = StringUtils.EMPTY;
			this.ready = false;
			this.state = 0;
			this.host = 0;
			this.recordid = StringUtils.EMPTY;
			this.callstate = StringUtils.EMPTY;
		}
		public ExtensionItem setAgent(String sAgent,String sUsername) {
			this.agent = sAgent;
			if(sUsername!=null)this.username = sUsername;
			return this;
		}
		public ExtensionItem setReady(boolean bReady,int nState) {
			this.ready = bReady;
			this.state = nState;
			return this;
		}
		public ExtensionItem setCallState(String sCallState) {
			this.callstate = sCallState;
			return this;
		}
		public ExtensionItem setCallInfo(String sCaller,String sCalled) {
			this.caller = sCaller;
			this.called = sCalled;
			return this;
		}
	}
	static StatModel statAllInfo = new StatModel();
	@SuppressWarnings("unused")
	public static void sendAllInfo(String roleguid,String queues, org.eclipse.jetty.websocket.api.Session wsSession) {
		/*
		Gson gson = gsonBuilder.create();
		String json = gson.toJson(stat);
		StatWebSocketServlet.sendMonitorText(json);
		*/
		Gson gson = gsonBuilder.create();
		statAllInfo.init = true;
		statAllInfo.sender = "Event";
		UpgradeRequest req = wsSession.getUpgradeRequest();
		Object oSessionId = req.getSession();
		
		StatModel mmessage = new StatModel();
		mmessage.init = true;
		mmessage.sender = "Event";
		mmessage.queues = statAllInfo.queues;
		mmessage.wsessionId=oSessionId.toString();
		//根据权限过滤分机,返回给监控界面用户根据部门还有队列筛选后的分机列表
		queues=Util.ObjectToString(queues);
		roleguid=Util.ObjectToString(roleguid);
		if(roleguid.equals("00000000-0000-0000-0000-000000000000")) {
			roleguid="";
		}
		if((roleguid.length()>0) || (queues.length()>0)) {
			java.util.List<Recextension> extensionlist=RbacSystem.getQueuesExtension(roleguid,queues,false);
			log.info("输出一下用户管理分机:"+extensionlist.toString());
			for(int i=0;i<statAllInfo.extensions.size();i++) {
				ExtensionItem extensionItem=statAllInfo.extensions.get(i);
				for(int j=0;j<extensionlist.size();j++) {
					Recextension recExtension=extensionlist.get(j);
					if(recExtension.getExtension().equals(extensionItem.extension)) {
						mmessage.extensions.add(extensionItem);
						break;
					}
				}
			}
			String json = gson.toJson(mmessage);
			log.info("普通坐席监控:"+json);
			if(wsSession!=null) {
				//仅仅将消息返回给请求连接者
				wsSession.getRemote().sendStringByFuture(json);
			}else {
				//将消息发送给所有wssession
				StatWebSocketServlet.sendMonitorText(json);
			}
		}else {
			String json = gson.toJson(statAllInfo);
			log.info("坐席监控没有选择部门和队列:"+json);
			if(wsSession!=null) {
				//仅仅将消息返回给请求连接者
				wsSession.getRemote().sendStringByFuture(json);
			}else {
				//将消息发送给所有wssession
				StatWebSocketServlet.sendMonitorText(json);
			}
			
		}
		GenesysStatServer.Instance.PeekStatisticAgentState();
	}
	private IConfService service;
	private WarmStandbyService warmStandbyService;
	public boolean initializePSDKProtocol() {
		if(Runner.sConfHost.length()>0) {

		}
		if(Runner.sStatHost.length()>0) {	
			PropertyConfiguration config = new PropertyConfiguration();
			config.setUseAddp(true);
			config.setAddpServerTimeout(20);
			config.setAddpClientTimeout(10);
			config.setAddpTraceMode(AddpTraceMode.Both);
			Endpoint endpoint = new Endpoint(Runner.sStatHost, Runner.nStatPort, config);
			Endpoint backupEndpoint = new Endpoint(Runner.sStatHostBackup, Runner.nStatPortBackup, config);
//			Endpoint endpoint = new Endpoint("101.133.132.212", 3060, config);
//			Endpoint backupEndpoint = new Endpoint("101.133.132.212", 3060, config);
			WarmStandbyConfiguration warmStandbyConfig = new WarmStandbyConfiguration(
					endpoint, backupEndpoint);
			warmStandbyConfig.setTimeout(5000);
			warmStandbyConfig.setAttempts((short) 2);
			stat_protocol = new StatServerProtocol();
    		stat_protocol.setClientName("TabStatServer");
			warmStandbyService = new WarmStandbyService(stat_protocol);
			warmStandbyService.applyConfiguration(warmStandbyConfig);
			warmStandbyService.start();
			setMessageHandler();

			try {
				stat_protocol.beginOpen();
			} catch (ProtocolException | IllegalStateException e) {
				log.error(e);
				return false;
			}
		}
		return true;
	}
	public void finalizePSDKProtocol() {
		try {
			if (service != null) {
				CloseStatisticState();
				Protocol protocol = service.getProtocol();
				if (protocol.getState() != ChannelState.Closed)
					protocol.close();
				ConfServiceFactory.releaseConfService(service);
			}
		} catch (Exception e) {
			System.out.println("Exception occured while releasing Config Service");
			e.printStackTrace();
		}
	}
	
	private void setMessageHandler(){
		stat_protocol.addChannelListener(new ChannelListener() {
			public void onChannelClosed(ChannelClosedEvent arg0) {
				log.warn("Channel Closed: " + arg0.toString());
			}

			public void onChannelError(ChannelErrorEvent arg0) {
				log.warn("Channel Error: " + arg0.toString());
			}

			public void onChannelOpened(EventObject arg0) {
				log.warn("Channel Opened: " + arg0.toString());
				//获取可以监视的信息
				//getStatisticProfile();
			//	OpenStatisticState();
				OpenStatisticStateFLP();
				PeekStatisticAgentState();

				
			}
		});

		// Define an anonymous message handler class
		// Alternatively you can implement the MessageHandler interface
		// separately
		// and pass an instance of that class here
		stat_protocol.setMessageHandler(new MessageHandler() {
			public void onMessage(Message msg) {
				log.info("输出一下分Genesys返回消息日志:"+msg);
				Integer nRefId = tab.util.Util.ObjectToNumber(msg.getMessageAttribute("ReferenceId"),0);
				log.info("输出一下Message RefId: " + nRefId);
				if(nRefId>=60000) {
					if(msg instanceof EventInfo) {
						EventInfo evt = (EventInfo)msg;
						log.info("Message Queue: " + evt);
						log.info("Message Queue(" + Runner.Queues.get(nRefId-STATISTIC_REFID_QUEUE) + ") CallerCount = " +  evt.getIntValue());
					}
				}
				if(nRefId>=50000) {
					if(msg instanceof EventInfo) {
						EventInfo evt = (EventInfo)msg;
						log.info("测试输出坐席消息: " + evt);
						AgentStatus agentStatus = (AgentStatus) evt.getStateValue();
						PlaceStatus placeStatus = agentStatus.getPlace();
						 int status=placeStatus.getStatus();
							log.info("测试输出坐席状态: " + status);
					}
				}
				if(nRefId>=STATISTIC_REFID_QUEUE) {					if(msg instanceof EventInfo) {
						EventInfo evt = (EventInfo)msg;
						log.info("Message Queue: " + evt);
						log.info("Message Queue(" + Runner.Queues.get(nRefId-STATISTIC_REFID_QUEUE) + ") CallerCount = " +  evt.getIntValue());
						StatModel stat = new StatModel();
						stat.init = false;
						stat.sender = "Event";
						QueueItem item = new QueueItem(Runner.Queues.get(nRefId-STATISTIC_REFID_QUEUE));
						for(int i=0;i<evt.getIntValue();i++) {item.addCaller("Unknow "+i);}
						stat.queues.add(item);
						Gson gson = gsonBuilder.create();
						String json = gson.toJson(stat);
						StatWebSocketServlet.sendMonitorText(json);
					}
				}else if(nRefId>=STATISTIC_REFID_DN) {
					if(msg instanceof EventInfo) {
						StatModel stat = new StatModel();
						stat.init = false;
						stat.sender = "Event";
						EventInfo evt = (EventInfo)msg;
						log.info("Message DN: " + evt);
						DnStatus dnStatus = (DnStatus) evt.getStateValue();
						if(IsLogout(dnStatus.getDnId())){
							ExtensionItem exten = new ExtensionItem("",dnStatus.getDnId(),"");
							switch(dnStatus.getStatus()) {
							case WaitForNextCall:
								exten.setReady(false, 0).setCallState("onhook");
								UpdateExtensionItem(exten);
								break;
							case NotReadyForNextCall:
								exten.setReady(false, 0).setCallState("onhook");
								UpdateExtensionItem(exten);
								break;
							case AfterCallWork:
								exten.setReady(false, 1).setCallState("onhook");
								UpdateExtensionItem(exten);
								break;
								
							case OffHook:
								exten.setReady(GetExtenStatus(exten.extension), 0).setCallState("outbound");
								break;
							case ASM_Outbound:
							case CallConsult:
							case CallOutbound:
							case CallDialing:
								exten.setReady(GetExtenStatus(exten.extension), 0).setCallState("outbound");
								break;
							case CallRinging:
							case CallOnHold:
							case ASM_Engaged:
							case CallInbound:
							case CallInternal:
								exten.setReady(GetExtenStatus(exten.extension), 0).setCallState("inbound");
								break;
							case CallUnknown:
							default:
								exten.setReady(GetExtenStatus(exten.extension), 0).setCallState("onhook");
								break;
							}
							stat.extensions.add(exten);
							Gson gson = gsonBuilder.create();
							String json = gson.toJson(stat);
							StatWebSocketServlet.sendMonitorText(json);
						}
					}
				}else if(nRefId>=STATISTIC_REFID_AGENT) {
					//痛快监控用的是这里的消息
					if(msg instanceof EventInfo) {
						StatModel stat = new StatModel();
						stat.init = false;
						stat.sender = "Event";
						EventInfo evt = (EventInfo)msg;
						log.info("Message Agent: " + evt);
						AgentStatus agentStatus = (AgentStatus) evt.getStateValue();
						PlaceStatus placeStatus = agentStatus.getPlace();
						if(placeStatus.getPlaceId()!=null && placeStatus.getPlaceId().length()>0) {//判断是否带有分机号
							ExtensionItem exten = new ExtensionItem(agentStatus.getAgentId(),placeStatus.getPlaceId(),Usernames.get(agentStatus.getAgentId()));
							switch(placeStatus.getStatus()) {
							case WaitForNextCall:
								exten.setReady(true, 0).setCallState("onhook");
								UpdateExtensionItem(exten);								
								break;
							case NotReadyForNextCall:
								exten.setReady(false, 0).setCallState("onhook");
								UpdateExtensionItem(exten);								
								break;
							case AfterCallWork:
								exten.setReady(false, 1).setCallState("onhook");
								UpdateExtensionItem(exten);								
								break;
								
							case OffHook:
								exten.setReady(GetExtenStatus(exten.extension), 0).setCallState("outbound");
								break;
							case ASM_Outbound:
							case CallConsult:
							case CallOutbound:
							case CallDialing:
								exten.setReady(GetExtenStatus(exten.extension), 0).setCallState("outbound");
								break;
							case CallRinging:
							case CallOnHold:
							case ASM_Engaged:
							case CallInbound:
							case CallInternal:
								exten.setReady(GetExtenStatus(exten.extension), 0).setCallState("inbound");
								break;
							case CallUnknown:
							default:
								exten.setReady(GetExtenStatus(exten.extension), 0).setCallState("onhook");
								break;
							}
							UpdateExtensionItem(exten);	
							stat.extensions.add(exten);
							Gson gson = gsonBuilder.create();
							String json = gson.toJson(stat);
							StatWebSocketServlet.sendMonitorText(json);
							LogIn(agentStatus.getAgentId(),placeStatus.getPlaceId());
						}else {
							ExtensionItem exten = Logout(agentStatus.getAgentId());
							if(exten!=null) {
								stat.extensions.add(exten);
								Gson gson = gsonBuilder.create();
								String json = gson.toJson(stat);
								StatWebSocketServlet.sendMonitorText(json);
							}
						}
					}
				}
			}
		});
	}
	boolean GetExtenStatus(String sExten) {
		for(ExtensionItem exten: statAllInfo.extensions) {
			if(exten.extension!=null && exten.extension.length()>0 && sExten.equals(exten.extension)) {
				return exten.ready;
			}
		}
		return false;
	}
	ExtensionItem Logout(String sAgent) {
		for(ExtensionItem exten: statAllInfo.extensions) {
			if(exten.agent!=null && exten.agent.length()>0 && sAgent.equals(exten.agent)) {
				exten.agent = StringUtils.EMPTY;
				return exten;
			}
		}
		return null;
	}
	void LogIn(String sAgent,String sExtension) {
		for(ExtensionItem exten: statAllInfo.extensions) {
			if(sExtension.equals(exten.extension) && (exten.agent!=null && (exten.agent.length()==0 || sAgent.equals(exten.agent)==false))) {
				exten.agent = sAgent;
				return;
			}
		}
	}
	boolean IsLogout(String sExtension) {
		for(ExtensionItem exten: statAllInfo.extensions) {
			if(exten.agent!=null && exten.agent.length()>0) {
				return false;
			}
		}
		return true;
	}
	void UpdateExtensionItem(ExtensionItem NewExten) {
		for(ExtensionItem exten: statAllInfo.extensions) {
			if(exten.extension!=null && exten.extension.length()>0 && exten.extension.equals(NewExten.extension)) {
				exten.ready = NewExten.ready;
				exten.callstate = NewExten.callstate;
				if(NewExten.agent!=null && NewExten.agent.length()>0)exten.agent = NewExten.agent;
				if(NewExten.username!=null && NewExten.username.length()>0)exten.username = NewExten.username;
				if(NewExten.called!=null && NewExten.called.length()>0)exten.called = NewExten.called;
				if(NewExten.caller!=null && NewExten.caller.length()>0)exten.caller = NewExten.caller;
				if(NewExten.group.size()>0) {
					exten.group.clear();
					exten.group.addAll(NewExten.group);
				}
				if(NewExten.recordid!=null && NewExten.recordid.length()>0)exten.recordid = NewExten.recordid;
				if(NewExten.state!=null && NewExten.state>0)exten.state = NewExten.state;
			}
		}
	}
	// RequestGetStatisticProfile
	// Requests information on the currently available statistical types,
	// time profiles, time ranges, or filters.
	@SuppressWarnings("unused")
	private void getStatisticProfile() {
		try {
			RequestGetStatisticProfile request = RequestGetStatisticProfile.create();
			request.setStatisticProfile(StatisticProfile.StatTypes);
			log.debug(request.toString());
			stat_protocol.send(request);
		} catch (ProtocolException e) {
			log.error(e);
		}
	}
	private static java.util.Map<String, String> Usernames = new java.util.HashMap<String, String>();
	@SuppressWarnings("unchecked")
	public static void UpdateUsernames(Session newDbsession,java.util.List<String> newAgents) {
		log.info("进入UpdateUsernames");
		Session dbsession = null;
		java.util.List<String> agents = new java.util.ArrayList<String>();
		try {
			if(newDbsession!=null && agents!=null) {
				log.info("newDbsession!=null && agents!=null");
				dbsession = newDbsession;
				agents.addAll(newAgents);
			}else {
				log.info("newDbsession!=null && agents!=null---else");
				dbsession = HibernateSessionFactory.getThreadSession();
				agents = dbsession.createQuery("select agent from Rbacuserauths where length(agent)>0").list();
			}
			try {
				String querysql="";
				if(Runner.CustomerName.equals("江租")) {
					 querysql="select agent,name from res_users A,res_partner B where A.partner_id=B.id and agent in(:agents)";
					 log.info("Runner.CustomerName.equals(\"江租\")");
				}else {
					 log.info("Runner.CustomerName.equals(\"江租\")--else");
					querysql="select agent as agent, username as username from Rbacuserauths where agent in(:agents)";
				}
				java.util.List<Object[]> agentNos = dbsession
						.createSQLQuery(querysql)
						.setParameterList("agents", agents).list();
				 log.info("输出agents"+agents.toString()+agents.size());
				for (int idx = 0; idx < agentNos.size(); idx++) {
					Object[] agentUsers = (Object[]) agentNos.get(idx);
					 log.info("输出agentUsers"+"key:"+agentUsers[0].toString()+"---value:"+tab.util.Util.ObjectToString(agentUsers[1]));
					Usernames.put(agentUsers[0].toString(), tab.util.Util.ObjectToString(agentUsers[1]));
				}
				
			} catch (Throwable e) {
				log.warn(e);
			}
			if(!(newDbsession!=null && agents!=null)) {
				dbsession.close();
			}
		} catch (Throwable e) {
			log.warn(e);
		}		
	}
	private static class GenesysResponeSkill{
		public GenesysResponeSkill(String skillname,String sLevel, Integer nOperation) {
			this.name = skillname;
			this.level = sLevel;
			this.nOperation = nOperation;
		}
		public String id;
		public String name;
		public String uri;
		public String path;
		public String level;
		public transient Integer nOperation;//null删除，0不变，1添加
	}
	private static class GenesysUser{
		public GenesysUser(String id,String userName,String firstName,java.util.List<GenesysResponeSkill> skills) {
			this.id = id;
			this.userName = userName;
			this.firstName = firstName;
			if(this.skills==null)this.skills = new java.util.ArrayList<GenesysResponeSkill>();
			this.skills.addAll(skills);
			this.nOperation = null;
		}
		public GenesysUser(GenesysResponeSkill skill) {
			if(this.skills==null)this.skills = new java.util.ArrayList<GenesysResponeSkill>();
			skills.add(skill);
		}
		public String id;
		public String userName;
		public String firstName;
		public Integer nOperation;//null删除，0不变，1添加
		public java.util.List<GenesysResponeSkill> skills;
		public GenesysAgentInfo agentLogin;
	}
	private static class GenesysUserSetting{
		public String displayName;
		public String name;
		public final String key = "name";
	}
	private static class GenesysResponeUserSettingList{
		public Integer statusCode;
		public java.util.List<GenesysUserSetting> settings;
	}
	private static class GenesysResponeUser{
		public Integer statusCode;
		public String statusMessage;
		public String id;
		public GenesysUser user;
	}
	private static class GenesysResponeUserList{
		public Integer statusCode;
		public String statusMessage;
		public java.util.List<String> paths;
	}
	@SuppressWarnings("unused")
	private static class GenesysCreateUserInfo{
		public String type = "User";
		public String firstName;
		public String lastName;
		public java.util.List<String> roles = new java.util.ArrayList<String>();
		public String userName;
		public String employeeId;
		public String password;
		public String loginCode;
	}
	private static class GenesysUpdateUserInfo{
		public String loginCode;
	}
	private static class GenesysTServer{
		@SerializedName("wrap-up-time")
		String wrapUpTime = "60";
	}
	private static class AgentUserProperties{
		GenesysTServer TServer = new GenesysTServer();
	}
	private static class GenesysAgentInfo{
		public String password;
		public String switchSpecificType;
		public String DBID;
		public String useOverride;
		public AgentUserProperties userProperties;
		public String loginCode;
		public String switchDBID;
		public String state;
		public String tenantDBID;
	}
	private static class GenesysCreateAgent{
		@SerializedName("agent-login")
		public GenesysAgentInfo agentLogin = new GenesysAgentInfo();
	}
	private static class GenesysResponeAgentList{
		public Integer statusCode;
		public String statusMessage;
		@SerializedName("agent-logins")
		public java.util.List<GenesysAgentInfo> agentLogin;
	}
	private static class GenesysResponeAgent{
		public Integer statusCode;
		public String statusMessage;
		public String id;
		public String path;
		public String uri;
	}
	public static boolean UpdateAgentSkills()
	{
		try {
			java.util.Map<String, GenesysUser> agentList = new java.util.HashMap<String, GenesysStatServer.GenesysUser>();
			//获取Genesys的用户和技能组信息，插入users。获取本地数据库用户和技能组，双向同步
			HttpAuthenticationFeature auth = HttpAuthenticationFeature.basic(Runner.sConfUsername, Runner.sConfPassword);
			Client rsclient = ClientBuilder.newClient(new ClientConfig().register(GsonJerseyProvider.class)).register(auth);
			//连接genesys网址
			WebTarget webtarget = rsclient.target(Runner.sConfHost).path("/api/v2/me");
			Invocation.Builder invocationBuilder =  webtarget.request(MediaType.APPLICATION_JSON);
			Response response = invocationBuilder.get();
			GenesysResponeUser me = response.readEntity(GenesysResponeUser.class);
			if(me.statusCode!=0) {
				log.error(me.statusMessage);
				rsclient.close();
				return false;
			}
			javax.ws.rs.core.MultivaluedMap<String,Object> headers = response.getHeaders();
			String csrfHeaderName = tab.util.Util.ObjectToString(headers.getFirst("X-CSRF-HEADER"));
			String csrfToken = tab.util.Util.ObjectToString(headers.getFirst("X-CSRF-TOKEN"));
			if(csrfHeaderName.length()==0 || csrfToken.length()==0) {
				log.error("X-CSRF-HEADER && X-CSRF-TOKEN is NULL!");
				rsclient.close();
				return false;
			}
			java.util.Map<String, NewCookie> cookies = response.getCookies();
			rsclient.close();
			rsclient = ClientBuilder.newClient(new ClientConfig().register(GsonJerseyProvider.class)).register(GZipEncoder.class);
			webtarget = rsclient.target(Runner.sConfHost).path("/api/v2/users");
			invocationBuilder =  webtarget.request(MediaType.APPLICATION_JSON).header(csrfHeaderName, csrfToken).acceptEncoding("gzip");
			for(NewCookie cookie : cookies.values()) {
				invocationBuilder = invocationBuilder.cookie(cookie);
			}
			response = invocationBuilder.get();
			GenesysResponeUserList users = response.readEntity(GenesysResponeUserList.class);
			if(users.statusCode!=0) {
				log.error(users.statusMessage);
				rsclient.close();
				return false;
			}
			Pattern pattern = Pattern.compile(Runner.sHideAgentRegEx);			
			java.util.Map<String, GenesysResponeSkill> skillsMap = new java.util.HashMap<String, GenesysStatServer.GenesysResponeSkill>();
			//获取Genesys服务端坐席和技能组,并且默认都是删除标记
			for(String id: users.paths) {
				String sUserId = id.replace("/users/", "");
				webtarget = rsclient.target(Runner.sConfHost).path("/api/v2/users/"+sUserId).queryParam("subresources", "skills");
				invocationBuilder =  webtarget.request(MediaType.APPLICATION_JSON).header(csrfHeaderName, csrfToken).acceptEncoding("gzip");
				for(NewCookie cookie : cookies.values()) {
					invocationBuilder = invocationBuilder.cookie(cookie);
				}
				response = invocationBuilder.get();
				GenesysResponeUser user = response.readEntity(GenesysResponeUser.class);
				if(user==null) {
				}else if(user.statusCode!=0) {
					log.error(user.statusMessage + ", " + sUserId);
				}else {
					if(user.user!=null && user.user.skills!=null) {
						Matcher matcher = pattern.matcher((CharSequence) user.user.userName);
						if(!matcher.matches()) {
							agentList.put(user.user.userName,new GenesysUser(user.user.id,user.user.userName,user.user.firstName,user.user.skills));
							if(user.user.skills.size()>0) {
								for(GenesysResponeSkill skill : user.user.skills) {
									skillsMap.put(skill.name, skill);
									log.info("[GET] agent=" + user.user.userName + ", skill=" + skill.name + ", level=" + skill.level + ", Operatoin=null, SkillOperatoin=" + skill.nOperation);
								}
							}else {
								log.info("[GET] agent=" + user.user.userName + ", skill=null" + ", Operatoin=null, SkillOperatoin=null");
							}
						}
					}
				}
			}
			if(agentList.size()==0) {
				rsclient.close();
				log.info("can not support init Genesys Skill Group.");
				return false;
			}
			try {
				Session dbsession = HibernateSessionFactory.getThreadSession();
				try {
					String sSqlStr = "select A.login as login,A.agent as agent,C.value as skillname,D.mpriority as level from res_users A " +
							" left join res_users_tab_phone_ctiskill_rel B on B.res_users_id=A.id" + 
							" left join tab_phone_ctiskill C on B.tab_phone_ctiskill_id=C.id" +
							" left join user_priority D on D.ctiskill_id=C.id and D.res_user=A.id" +
							" where not(A.agent is null) and length(A.agent)>0 and A.active";
					log.info(sSqlStr);
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, String>> agents = dbsession.createSQLQuery(sSqlStr).setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP).list();
					for(java.util.Map<String, String> item : agents) {
						GenesysUser agentskills = agentList.get(item.get("agent"));
						Matcher matcher = pattern.matcher((CharSequence) item.get("agent"));
						if(!matcher.matches()) {
							String sLevel = tab.util.Util.ObjectToString(item.get("level"));
							if(agentskills!=null) {
								if(agentskills.nOperation==null)agentskills.nOperation = 0;//找到配置标记为不变
								log.info("[DAT] agent=" + item.get("agent") + ", skill=" + item.get("skillname") + ", level=" + sLevel + ", Operatoin=" + agentskills.nOperation);
								boolean bFound = false;
								for(GenesysResponeSkill skill : agentskills.skills) {
									if(item.get("skillname")!=null) {
										if(item.get("skillname").equals(skill.name)) {
											if(sLevel.length()==0 || sLevel.equals(skill.level)) {
												skill.nOperation = 0;//找到配置标记为不变
											}else {
												skill.nOperation = 1;
												skill.level = sLevel;
											}
											log.info("[REV] agent=" + agentskills.userName + ", skill=" + skill.name + ", level=" + skill.level + ", Operatoin=" + agentskills.nOperation + ", SkillOperatoin=" + skill.nOperation);
											bFound = true;
											break;
										}
									}
								}
								if(!bFound) {//没找到配置则添加
									if(item.get("skillname")!=null) {
										agentskills.skills.add(new GenesysResponeSkill(item.get("skillname"),sLevel,1));
										log.info("[ADD] agent=" + agentskills.userName + ", skill=" + item.get("skillname") + ", level=" + sLevel + ", Operatoin=" + agentskills.nOperation + ", SkillOperatoin=1");
									}
								}
							}else {
								agentskills = new GenesysUser(new GenesysResponeSkill(item.get("skillname"),sLevel,1));//没找到配置则添加
								agentskills.nOperation = 1;//没找到配置则添加
								agentskills.userName = item.get("agent");
								agentList.put(item.get("agent"), agentskills);
								log.info("[ADD] agent=" + agentskills.userName + ", skill=" + item.get("skillname") + ", level=" + sLevel + ", Operatoin=" + agentskills.nOperation + ", SkillOperatoin=1");
							}
						}
					}
					for(Iterator<String> it = agentList.keySet().iterator(); it.hasNext();) {
						String userName = it.next();
						Matcher matcher = pattern.matcher((CharSequence) userName);
						if(matcher.matches()) {
							it.remove();
						}
					}
					//更新技能组列表
					webtarget = rsclient.target(Runner.sConfHost).path("api/v2/skills").queryParam("fields", "*");
					invocationBuilder =  webtarget.request(MediaType.APPLICATION_JSON).header(csrfHeaderName, csrfToken).acceptEncoding("gzip");
					for(NewCookie cookie : cookies.values()) {
						invocationBuilder = invocationBuilder.cookie(cookie);
					}
					response = invocationBuilder.get();
					GenesysUser userSkillList = response.readEntity(GenesysUser.class);
					if(userSkillList!=null && userSkillList.skills!=null) {
						for(GenesysResponeSkill skill : userSkillList.skills) {
							skill.level = "1";
							skillsMap.put(skill.name, skill);
						}
					}
					webtarget = rsclient.target(Runner.sConfHost).path("api/v2/platform/configuration/agent-logins");
					invocationBuilder =  webtarget.request(MediaType.APPLICATION_JSON).header(csrfHeaderName, csrfToken).acceptEncoding("gzip");
					for(NewCookie cookie : cookies.values()) {
						invocationBuilder = invocationBuilder.cookie(cookie);
					}
					response = invocationBuilder.get();
					GenesysResponeAgentList agentLogins = response.readEntity(GenesysResponeAgentList.class);
					GenesysAgentInfo defaultAgentInfo = null;
					if(agentLogins.statusCode==0) {
						for(GenesysAgentInfo agentlogin : agentLogins.agentLogin) {
							if(defaultAgentInfo==null)defaultAgentInfo = agentlogin;
							Matcher matcher = pattern.matcher((CharSequence) agentlogin.loginCode);
							if(!matcher.matches()) {
								GenesysUser agentskills = agentList.get(agentlogin.loginCode);
								if(agentskills!=null && (agentskills.nOperation==null || agentskills.nOperation==0)) {
									agentskills.agentLogin = agentlogin;
									log.info("[LINK] agent=" + agentskills.userName + ", loginCode=" + agentlogin.loginCode + ", password=" + agentlogin.password + ", Operatoin=" + agentskills.nOperation);
								}
							}
						}
					}
					//缺少工号的，添加工号, 并且建立关联
					if(defaultAgentInfo!=null) {
						for(GenesysUser user : agentList.values()) {
							if(user.agentLogin==null && (user.nOperation==null || user.nOperation==0)) {
								webtarget = rsclient.target(Runner.sConfHost).path("api/v2/platform/configuration/agent-logins");
								invocationBuilder =  webtarget.request(MediaType.APPLICATION_JSON).header(csrfHeaderName, csrfToken).acceptEncoding("gzip");
								for(NewCookie cookie : cookies.values()) {
									invocationBuilder = invocationBuilder.cookie(cookie);
								}
								GenesysCreateAgent agentlogin = new GenesysCreateAgent();
								agentlogin.agentLogin.DBID = null;
								agentlogin.agentLogin.loginCode = user.userName;
								agentlogin.agentLogin.password = user.userName;
								agentlogin.agentLogin.state = defaultAgentInfo.state;
								agentlogin.agentLogin.switchDBID = defaultAgentInfo.switchDBID;
								agentlogin.agentLogin.switchSpecificType = defaultAgentInfo.switchSpecificType;
								agentlogin.agentLogin.tenantDBID = defaultAgentInfo.tenantDBID;
								agentlogin.agentLogin.useOverride = defaultAgentInfo.useOverride;
								agentlogin.agentLogin.userProperties = defaultAgentInfo.userProperties;
								response = invocationBuilder.post(javax.ws.rs.client.Entity.entity(agentlogin,MediaType.APPLICATION_JSON));
								String sRet = response.readEntity(String.class);
								GenesysResponeAgent res = GsonUtil.getInstance().fromJson(sRet, GenesysResponeAgent.class);
								if(res.statusCode==0) {
									user.agentLogin = agentlogin.agentLogin;
									user.agentLogin.DBID = res.id;
									log.info("[ADD] agent=" + user.userName + ", loginCode=" + user.agentLogin.loginCode + ", password=" + user.agentLogin.password + ", Operatoin=" + user.nOperation);
								}else {
									log.info("[ADD] agent=" + user.userName + ", loginCode=" + user.agentLogin.loginCode + ", password=" + user.agentLogin.password + ", Operatoin=" + user.nOperation + ", request=" + sRet);
								}
								webtarget = rsclient.target(Runner.sConfHost).path("/api/v2/users/" + user.id);
								invocationBuilder =  webtarget.request(MediaType.APPLICATION_JSON).header(csrfHeaderName, csrfToken);
								for(NewCookie cookie : cookies.values()) {
									invocationBuilder = invocationBuilder.cookie(cookie);
								}
								GenesysUpdateUserInfo updater = new GenesysUpdateUserInfo();
								updater.loginCode = user.userName;
								response = invocationBuilder.put(javax.ws.rs.client.Entity.entity(updater,MediaType.APPLICATION_JSON));
								sRet = response.readEntity(String.class);
								log.info("User: " + user.userName + ", update=" + sRet);
							}
						}
					}
					@SuppressWarnings("unused")
					java.util.List<GenesysUserSetting> defaultUserSettings = null;
					for(GenesysUser user : agentList.values()) {
						if(user.nOperation==null || user.nOperation==0) {
							webtarget = rsclient.target(Runner.sConfHost).path("/api/v2/users/" + user.id + "/settings");
							invocationBuilder =  webtarget.request(MediaType.APPLICATION_JSON).header(csrfHeaderName, csrfToken).acceptEncoding("gzip");
							for(NewCookie cookie : cookies.values()) {
								invocationBuilder = invocationBuilder.cookie(cookie);
							}
							response = invocationBuilder.get();
							String sRet = response.readEntity(String.class);
							try {			
								GenesysResponeUserSettingList usersettings = GsonUtil.getInstance().fromJson(sRet, GenesysResponeUserSettingList.class);
								if(usersettings.statusCode==0) {
									defaultUserSettings = usersettings.settings;
									break;
								}
							}catch(Throwable e) {
								log.error("/api/v2/users/" + user.id + "/settings" + ", " +sRet);
								log.error(e);
								break;
							}
						}
					}
					for(Entry<String, GenesysUser> user : agentList.entrySet()) {
						if(user.getValue().nOperation==null) {
						
						}else if(user.getValue().nOperation==1) {
							
						}else {
							//Update Skill
							for(GenesysResponeSkill skill : user.getValue().skills) {
								GenesysResponeSkill rawSkill = skillsMap.get(skill.name);
								if(rawSkill!=null) {
									if(skill.nOperation==null) {
										webtarget = rsclient.target(Runner.sConfHost).path("/api/v2/users/"+ user.getValue().id+"/skills/" + rawSkill.id);
										invocationBuilder =  webtarget.request(MediaType.APPLICATION_JSON).header(csrfHeaderName, csrfToken).acceptEncoding("gzip");
										for(NewCookie cookie : cookies.values()) {
											invocationBuilder = invocationBuilder.cookie(cookie);
										}
										response = invocationBuilder.delete();
										String sRet = response.readEntity(String.class);
										log.info("[DEL] " + user.getValue().userName + ", skill: " + skill.name + " level=" + skill.level + ", Result=" + sRet);
									}else if(skill.nOperation==1) {
										webtarget = rsclient.target(Runner.sConfHost).path("/api/v2/users/"+ user.getValue().id+"/skills");
										invocationBuilder =  webtarget.request(MediaType.APPLICATION_JSON).header(csrfHeaderName, csrfToken).acceptEncoding("gzip");
										for(NewCookie cookie : cookies.values()) {
											invocationBuilder = invocationBuilder.cookie(cookie);
										}
										skill.id = rawSkill.id;
										skill.name = rawSkill.name;
										skill.path = rawSkill.path;
										skill.uri = rawSkill.uri;
										if(skill.level==null || skill.level.length()==0 || skill.level.equals("0"))skill.level = rawSkill.level;
										response = invocationBuilder.post(javax.ws.rs.client.Entity.entity(skill,MediaType.APPLICATION_JSON));
										String sRet = response.readEntity(String.class);
										log.info("[NEW] " + user.getValue().userName + ", skill: " + skill.name + ", level:" + skill.level + ", Result=" + sRet);
									}
								}
							}
						}
					}
					dbsession.close();
					return true;
				} catch (Throwable e) {
					log.warn("ERROR:", e);
				}
				dbsession.close();
			} catch (Throwable e) {
				log.warn("ERROR:", e);
			}
		} catch (Throwable e) {
			log.warn("ERROR:", e);
		}
		return false;
	}
	
	@SuppressWarnings("unused")
	private void getStatisticState() {
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				@SuppressWarnings("unchecked")
				java.util.List<String> agents = dbsession.createQuery("select agent from Rbacuserauths where length(agent)>0").list();
				UpdateUsernames(dbsession,agents);
				for(String agent : agents) {
					try {
						RequestGetStatistic request = RequestGetStatistic.create();
						StatisticObject statisticObject = StatisticObject.create(	agent, StatisticObjectType.Agent,TENANT_NAME);
						StatisticMetric statisticMetric = StatisticMetric.create();
						statisticMetric.setStatisticType("CurrentAgentState");
						request.setStatisticObject(statisticObject);
						request.setStatisticMetric(statisticMetric);	
						log.info("Agent(get):" + agent + ", " + request.toString());
						stat_protocol.send(request);
					} catch (ProtocolException e) {
						log.error(e);
					}
				}
				@SuppressWarnings("unchecked")
				java.util.List<String> extensions = dbsession.createQuery("select extension from Recextension where length(extension)>0").list();
				for(String extension : extensions) {
					try {
						RequestGetStatistic request = RequestGetStatistic.create();
						StatisticObject statisticObject = StatisticObject.create(extension+Runner.sSuffix, StatisticObjectType.RegularDN,TENANT_NAME);
						StatisticMetric statisticMetric = StatisticMetric.create();
						statisticMetric.setStatisticType("CurrentDNState");
						request.setStatisticObject(statisticObject);
						request.setStatisticMetric(statisticMetric);
						log.info("DN(get):" + extension + ", " + request.toString());
						stat_protocol.send(request);
					} catch (ProtocolException e) {
						log.error(e);
					}
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
			}
			dbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
		}	
		for(Integer id=0;id<Runner.Queues.size();id++) {
			try {
				RequestGetStatistic request = RequestGetStatistic.create();
				StatisticObject statisticObject = StatisticObject.create(Runner.Queues.get(id)+Runner.sQuffix, StatisticObjectType.Queue,TENANT_NAME);
				StatisticMetric statisticMetric = StatisticMetric.create();
				statisticMetric.setStatisticType("CurrNumberWaitingCalls");
				request.setStatisticObject(statisticObject);
				request.setStatisticMetric(statisticMetric);
				log.info("Queue(get):" + Runner.Queues.get(id) + ", " + request.toString());
				stat_protocol.send(request);
			} catch (ProtocolException e) {
				log.error(e);
			}
		}
	}
	
	public void PeekStatisticAgentState() {
		try {
			for(Integer id = 10000; id<CurrentReadyAgents;id++) {
				RequestPeekStatistic request = RequestPeekStatistic.create();
				request.setStatisticId(id);
				stat_protocol.send(request);
			}
			for(Integer id = 70000; id<CallsAnswered;id++) {
				RequestPeekStatistic request = RequestPeekStatistic.create();
				request.setStatisticId(id);
				stat_protocol.send(request);
			}
		} catch (ProtocolException e) {
			log.error(e);
		}
	}
	static Integer nAgentRefId = STATISTIC_REFID_AGENT;
	static Integer nDnRefId = STATISTIC_REFID_DN;	
	static Integer nQueueRefId = STATISTIC_REFID_QUEUE;
	
	static Integer CurrentReadyAgents = 10000;
	static Integer CurrNumberInboundStatuses = 20000;
	static Integer CurrNumberInternalStatuses  = 30000;
	static Integer CurrNumberOutboundStatuses = 40000;
	static Integer CurrNumberACWStatuses = 50000;
	static Integer CurrNumberNotReadyStatuses = 60000;
	static Integer CallsAnswered  = 70000;
	public static class subclass{
		String subname;
		Integer substartid;
		StatisticObjectType sub_type;
	}
	public static java.util.List<subclass> sublist=new java.util.ArrayList<subclass>();
	
 //	static String sSuffix = "@Switch";
//	static String sSuffix = "@SIP_Switch";
//	static String sQuffix = "_SIP_Switch";
	public void sendrequeservice(Integer id,String targetname,String target,StatisticObjectType sub_type,String environment) {
		try {
			RequestOpenStatistic request = RequestOpenStatistic.create();
			StatisticObject statisticObject = StatisticObject.create(targetname, sub_type,environment);
			StatisticMetric statisticMetric = StatisticMetric.create();
			statisticMetric.setStatisticType(target);
			request.setStatisticObject(statisticObject);
			request.setStatisticMetric(statisticMetric);
			Notification notification = Notification.create();
			notification.setMode(NotificationMode.Immediate);
			request.setNotification(notification);
			request.setReferenceId(id++);
			stat_protocol.send(request);
		} catch (ProtocolException e) {
			log.error(e);
		}
	}
	private void OpenStatisticStateFLP() {
		sendrequeservice(CallsAnswered,"CCC","Total_Calls_Entered",StatisticObjectType.GroupQueues,TENANT_NAME);
		for (int i=0;i<Runner.AgentGroups.size();i++) {
			String queuename=Runner.AgentGroups.get(i);
			sendrequeservice(CurrentReadyAgents,queuename,"CurrentReadyAgents",StatisticObjectType.GroupAgents,TENANT_NAME);
//			sendrequeservice(CurrNumberInboundStatuses,queuename,"CurrNumberInboundStatuses",StatisticObjectType.GroupAgents,TENANT_NAME);
//			sendrequeservice(CurrNumberInternalStatuses,queuename,"CurrNumberInternalStatuses",StatisticObjectType.GroupAgents,TENANT_NAME);
//			sendrequeservice(CurrNumberOutboundStatuses,queuename,"CurrNumberOutboundStatuses",StatisticObjectType.GroupAgents,TENANT_NAME);
//			sendrequeservice(CurrNumberACWStatuses,queuename,"CurrNumberACWStatuses",StatisticObjectType.GroupAgents,TENANT_NAME);
//			sendrequeservice(CallsAnswered,queuename,"CallsAnswered",StatisticObjectType.GroupAgents,TENANT_NAME);
//			sendrequeservice(CallsAnswered,queuename,"CallsAnswered",StatisticObjectType.GroupAgents,TENANT_NAME);
//			try {
//				RequestOpenStatistic request = RequestOpenStatistic.create();
//				StatisticObject statisticObject = StatisticObject.create(queuename, StatisticObjectType.GroupAgents,TENANT_NAME);
//				StatisticMetric statisticMetric = StatisticMetric.create();
//				statisticMetric.setStatisticType("CurrentReadyAgents");
//				request.setStatisticObject(statisticObject);
//				request.setStatisticMetric(statisticMetric);
//				Notification notification = Notification.create();
//				notification.setMode(NotificationMode.Immediate);
//				request.setNotification(notification);
//				request.setReferenceId(CurrentReadyAgents++);
//				stat_protocol.send(request);
//			} catch (ProtocolException e) {
//				log.error(e);
//			}
		}
	}
	private void OpenStatisticState() {
 
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				@SuppressWarnings("unchecked")
				java.util.List<String> agents = dbsession.createQuery("select agent from Rbacuserauths where length(agent)>0").list();
				UpdateUsernames(dbsession,agents);
				nAgentRefId = STATISTIC_REFID_AGENT;
				for(String agent : agents) {
					try {
						RequestOpenStatistic request = RequestOpenStatistic.create();
						StatisticObject statisticObject = StatisticObject.create(agent, StatisticObjectType.Agent,TENANT_NAME);
						StatisticMetric statisticMetric = StatisticMetric.create();
						statisticMetric.setStatisticType("CurrentAgentState");
						request.setStatisticObject(statisticObject);
						request.setStatisticMetric(statisticMetric);
						Notification notification = Notification.create();
						notification.setMode(NotificationMode.Immediate);
						request.setNotification(notification);
						request.setReferenceId(nAgentRefId++);
						log.debug("Agent:" + agent + ", " + request.toString());
						stat_protocol.send(request);
					} catch (ProtocolException e) {
						log.error(e);
					}
				}
				statAllInfo.extensions.clear();
				@SuppressWarnings("unchecked")
				java.util.List<Recextension> extensions = dbsession.createQuery(" from Recextension where length(extension)>0").list();
				nDnRefId = STATISTIC_REFID_DN;	
				for(Recextension extension : extensions) {
					try {
						RequestOpenStatistic request = RequestOpenStatistic.create();
						StatisticObject statisticObject = StatisticObject.create(extension+Runner.sSuffix, StatisticObjectType.RegularDN,TENANT_NAME);
						log.info("设置分级后缀是:"+Runner.sSuffix);
						StatisticMetric statisticMetric = StatisticMetric.create();
						statisticMetric.setStatisticType("CurrentDNState");
						request.setStatisticObject(statisticObject);
						request.setStatisticMetric(statisticMetric);
						Notification notification = Notification.create();
						notification.setMode(NotificationMode.Immediate);
						request.setNotification(notification);
						request.setReferenceId(nDnRefId++);
						log.debug("DN:" + extension.getExtension() + ", " + request.toString());
						stat_protocol.send(request); 
						statAllInfo.extensions.add(new ExtensionItem("",extension.getExtension(),""));
					} catch (ProtocolException e) {
						log.error(e);
					}
				}
			} catch (Throwable e) {
				log.warn("ERROR:", e);
			}
			dbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
		}	
		statAllInfo.queues.clear();
		//首次建立连接返回队列,现在会根据权限返回客户所属组下所有队列
		nQueueRefId = STATISTIC_REFID_QUEUE;
		for(Integer id=0;id<Runner.Queues.size();id++) {
			try {
				RequestOpenStatistic request = RequestOpenStatistic.create();
				StatisticObject statisticObject = StatisticObject.create(	Runner.Queues.get(id)+Runner.sQuffix, StatisticObjectType.Queue,TENANT_NAME);
				log.info("设置队列后缀是:"+Runner.sQuffix);
				StatisticMetric statisticMetric = StatisticMetric.create();
				statisticMetric.setStatisticType("CurrNumberWaitingCalls");
				request.setStatisticObject(statisticObject);
				request.setStatisticMetric(statisticMetric);
				Notification notification = Notification.create();
				notification.setMode(NotificationMode.Immediate);
				request.setNotification(notification);
				request.setReferenceId(nQueueRefId++);
				log.debug("Queue:" + Runner.Queues.get(id) + ", " + request.toString());
				stat_protocol.send(request);
				statAllInfo.queues.add(new QueueItem(Runner.Queues.get(id)));
			} catch (ProtocolException e) {
				log.error(e);
			}
		}
		sendAllInfo(null,null,null);
	}

	private void CloseStatisticState() {
		try {
			for(Integer id = STATISTIC_REFID_AGENT; id<nAgentRefId;id++) {
				RequestCloseStatistic request = RequestCloseStatistic.create();
				request.setStatisticId(id);
				stat_protocol.send(request);
			}
			for(Integer id = STATISTIC_REFID_DN; id<nDnRefId;id++) {
				RequestCloseStatistic request = RequestCloseStatistic.create();
				request.setStatisticId(id);
				stat_protocol.send(request);
			}
			for(Integer id = STATISTIC_REFID_QUEUE; id<nQueueRefId;id++) {
				RequestCloseStatistic request = RequestCloseStatistic.create();
				request.setStatisticId(id);
				stat_protocol.send(request);
			}
		} catch (ProtocolException e) {
			log.error(e);
		}
	}
}
