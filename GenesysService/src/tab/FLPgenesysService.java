package tab;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.util.ajax.JSON;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.hibernate.Query;
import org.hibernate.Session;

import com.genesyslab.platform.applicationblocks.com.IConfService;
import com.genesyslab.platform.applicationblocks.warmstandby.WarmStandbyConfiguration;
import com.genesyslab.platform.applicationblocks.warmstandby.WarmStandbyService;
import com.genesyslab.platform.commons.connection.configuration.PropertyConfiguration;
import com.genesyslab.platform.commons.connection.configuration.ClientADDPOptions.AddpTraceMode;
import com.genesyslab.platform.commons.protocol.ChannelClosedEvent;
import com.genesyslab.platform.commons.protocol.ChannelErrorEvent;
import com.genesyslab.platform.commons.protocol.ChannelListener;
import com.genesyslab.platform.commons.protocol.Endpoint;
import com.genesyslab.platform.commons.protocol.Message;
import com.genesyslab.platform.commons.protocol.MessageHandler;
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
import com.genesyslab.platform.reporting.protocol.statserver.events.EventInfo;
import com.genesyslab.platform.reporting.protocol.statserver.requests.RequestOpenStatistic;
import com.genesyslab.platform.reporting.protocol.statserver.requests.RequestPeekStatistic;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import hbm.OffersetTransformers;
import hbm.factory.GHibernateSessionFactory;
import hbm.model.Recextension;
import main.Runner;
import tab.GenesysStatServer.ExtensionItem;
import tab.GenesysStatServer.QueueItem;
import tab.GenesysStatServer.StatModel;
import tab.jetty.StatWebSocketServlet;
import tab.rbac.RbacSystem;
import tab.util.Util;
import tab.util.chenUtil;

public class FLPgenesysService {
	public static Log log = LogFactory.getLog(FLPgenesysService.class);
	public static FLPgenesysService Instance = new FLPgenesysService();
	private StatServerProtocol stat_protocol;
	private IConfService service;
	private WarmStandbyService warmStandbyService;
	private static String TENANT_NAME = "Environment";
	private static GsonBuilder gsonBuilder = new GsonBuilder().serializeNulls();
	static Integer RequestID = 1;
	static StatModel statAllInfo = new StatModel();
	public static class MapObj{
		public String typename;//
		public String subname;
		public String name;
		public int requestid;
		public MapObj(String name,String typename,String subname,int requestid) {
			this.typename=typename;//是队列还是坐席组
			this.subname=subname;//订阅数据
			this.name=name;//坐席组或者队列名称
			this.requestid=requestid;//请求id
			
		}
	}
	public  void ReplaceAgentGroup (AgentGroup agentgroup) {
		for(int i=0;i<statAllInfo.agentgroup.size();i++) {
			AgentGroup obj=statAllInfo.agentgroup.get(i);
			if(agentgroup.subname.equals(obj.subname)&&agentgroup.name.equals(obj.name)) {
			//	obj=agentgroup;
				obj.value=agentgroup.value;
			}
		}
	}
	public  void ReplaceAgentQueue (QueueGroup queuegroup) {
		for(int i=0;i<statAllInfo.queuegroup.size();i++) {
			
			QueueGroup obj=statAllInfo.queuegroup.get(i);
			if(queuegroup.name.equals(obj.name)&&queuegroup.subname.equals(obj.subname)) {
				obj.value=queuegroup.value;
			}
		}
	}
	public static Map<Integer,MapObj> mapobj=new HashMap<Integer,MapObj>();
	public static class StatModel {
		public String sender;
		public String wsessionId;
		public java.util.List<java.util.Map<String, Object>> weeklydata;
		public java.util.List<java.util.Map<String, Object>> daylydata;
		public java.util.List<AgentGroup> agentgroup;
		public java.util.List<QueueGroup> queuegroup;
		public Boolean init;
		public StatModel() {
			this.agentgroup = new java.util.ArrayList<FLPgenesysService.AgentGroup>();
			this.queuegroup = new java.util.ArrayList<FLPgenesysService.QueueGroup>();
		}
	}
	public static class AgentGroup{
		public String value ;
		public String subname ;
		public String name ;
		public AgentGroup(String value,String subname,String name) {
			this.value=value;
			this.subname=subname;
			this.name=name;
		}
	}
	public static class QueueGroup{
		public String value ;
		public String subname ;
		public String name ;
		public QueueGroup(String value,String subname,String name) {
			this.value=value;
			this.subname=subname;
			this.name=name;
		}
	}
	
	

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
				OpenStatisticStateFLP();
				//PeekStatisticAgentState();
			}
		});

		stat_protocol.setMessageHandler(new MessageHandler() {
			public void onMessage(Message msg) {
				Integer nRefId = tab.util.Util.ObjectToNumber(msg.getMessageAttribute("ReferenceId"),0);
				if(nRefId>0) {
				if(msg instanceof EventInfo) {
					try {
				    log.info("输出一下分eventinfo返回消息日志:"+msg);
				    EventInfo info = (EventInfo) msg;
					MapObj obj= mapobj.get(nRefId);
					String value ="0";
					if(obj!=null) {
						StatModel setmodel=new StatModel();
						setmodel.init = false;
						setmodel.sender = "Event";
						switch(obj.subname) {
						case "CurrentReadyAgents":
						case "CurrNumberInboundStatuses":
						case "CurrNumberInternalStatuses":
						case "CurrNumberOutboundStatuses":
						case "CurrNumberACWStatuses":
						case "CurrNumberNotReadyStatuses":
						case "Total_Calls_Offered":
						case "Total_Calls_Outbound":
						case "Total_Calls_Answered_In_Threshold":
						case "Total_Calls_Answered":
						case "Total_Calls_Entered":
						case "CurrNumberWaitingCalls":
						case "CurrNumberNotReadyReasons":	
							
							//取int类型的值
						 value=info.getIntValue().toString();
							break;
						case "Average_Handle_Time":	
						case "Average_Hold_Time":
							value=info.getStringValue();
							String stringvalue=info.getStringValue();
							int value0=info.getIntValue();
							Double dd=Double.parseDouble(stringvalue);
							value=dd.toString();
							log.info("=========================================="+value);
								break;
							default:
						}
						if(obj.typename.equals("AGENTGROUP")) {
							AgentGroup agentgroup=new AgentGroup(value,obj.subname,obj.name);
							setmodel.agentgroup.add(agentgroup);
							ReplaceAgentGroup(agentgroup);
							
						}else if(obj.typename.equals("QUEUEGROUP")){
							QueueGroup queuegroup=new QueueGroup(value,obj.subname,obj.name);
							setmodel.queuegroup.add(queuegroup);
							ReplaceAgentQueue(queuegroup);
						}
						Gson gson = gsonBuilder.create();
						String json = gson.toJson(setmodel);
						log.info("json:"+json);
						StatWebSocketServlet.sendMonitorText(json);
					}else {
						log.warn("nRefId:"+nRefId+"没有被放入请求map");
					}
					}catch(Throwable e){
						log.warn("ERROR:"+e);
					}
				}else {
					log.info("unexceptmessage-非eventinfo消息:"+msg);
					MapObj obj= mapobj.get(nRefId);
					Gson gson = gsonBuilder.create();
					String json = gson.toJson(obj);
					log.info("nRefId:"+nRefId+"obj:"+json);
				}
				}else{
					log.warn("nRefId为0的消息:"+nRefId);
				}
			}
		});
	}
	
	private void OpenStatisticStateFLP() {
		for (int i=0;i<Runner.AgentGroups.size();i++) {
			String queuename=Runner.AgentGroups.get(i);
			sendrequeservice(queuename,"CurrentReadyAgents",StatisticObjectType.GroupAgents,TENANT_NAME,"AGENTGROUP");
			sendrequeservice(queuename,"CurrNumberInboundStatuses",StatisticObjectType.GroupAgents,TENANT_NAME,"AGENTGROUP");
			sendrequeservice(queuename,"CurrNumberInternalStatuses",StatisticObjectType.GroupAgents,TENANT_NAME,"AGENTGROUP");
			sendrequeservice(queuename,"CurrNumberOutboundStatuses",StatisticObjectType.GroupAgents,TENANT_NAME,"AGENTGROUP");
			sendrequeservice(queuename,"CurrNumberNotReadyReasons",StatisticObjectType.GroupAgents,TENANT_NAME,"AGENTGROUP");
			sendrequeservice(queuename,"CurrNumberNotReadyStatuses",StatisticObjectType.GroupAgents,TENANT_NAME,"AGENTGROUP");
			sendrequeservice(queuename,"Total_Calls_Outbound",StatisticObjectType.GroupAgents,TENANT_NAME,"AGENTGROUP");
			sendrequeservice(queuename,"Total_Calls_Offered",StatisticObjectType.GroupAgents,TENANT_NAME,"AGENTGROUP");
			sendrequeservice(queuename,"Average_Handle_Time",StatisticObjectType.GroupAgents,TENANT_NAME,"AGENTGROUP");
			sendrequeservice(queuename,"Average_Hold_Time",StatisticObjectType.GroupAgents,TENANT_NAME,"AGENTGROUP");
		}
		for (int i=0;i<Runner.Queues.size();i++) {
			String queuename=Runner.Queues.get(i);
			sendrequeservice(queuename,"Total_Calls_Answered",StatisticObjectType.GroupQueues,TENANT_NAME,"QUEUEGROUP");
			sendrequeservice(queuename,"Total_Calls_Entered",StatisticObjectType.GroupQueues,TENANT_NAME,"QUEUEGROUP");
			sendrequeservice(queuename,"Total_Calls_Answered_In_Threshold",StatisticObjectType.GroupQueues,TENANT_NAME,"QUEUEGROUP");
			sendrequeservice(queuename,"CurrNumberWaitingCalls",StatisticObjectType.GroupQueues,TENANT_NAME,"QUEUEGROUP");
		}
	}
	
	public void PeekStatisticAgentState() {
		try {
			for(Integer id = 1; id<RequestID;id++) {
				RequestPeekStatistic request = RequestPeekStatistic.create();
				request.setStatisticId(id);
				stat_protocol.send(request);
			}
		} catch (ProtocolException e) {
			log.error(e);
		}
	}
	
	//参数 1 队列或者坐席组名称 2 订阅数据，3订阅类型，4 env，5组还是队列，是3的string形式
	public void sendrequeservice(String targetname,String target,StatisticObjectType sub_type,String environment,String typename) {
		try {
			MapObj obj=new MapObj(targetname,typename,target,RequestID);//四个参数分别是  订阅的对象名称,订阅类别，订阅数据类型，请求id
			mapobj.put(RequestID, obj);
			
			RequestOpenStatistic request = RequestOpenStatistic.create();
			StatisticObject statisticObject = StatisticObject.create(targetname, sub_type,environment);
			StatisticMetric statisticMetric = StatisticMetric.create();
			if(target.equals("Total_Calls_Answered_In_Threshold")) {
				statisticMetric.setTimeRange("Range0-10");
			}
			if(target.equals("CurrNumberNotReadyReasons")) {
				statisticMetric.setFilter("ACW");
			}
			statisticMetric.setStatisticType(target);
			request.setStatisticObject(statisticObject);
			request.setStatisticMetric(statisticMetric);
			Notification notification = Notification.create();
			notification.setMode(NotificationMode.Immediate);
			request.setNotification(notification);
			request.setReferenceId(RequestID++);
			stat_protocol.send(request);
			//将请求内容放入map中  以用于消息事件中从请求id获取请求的别的信息
			if(typename.equals("AGENTGROUP")) {
				AgentGroup agentgroup=new AgentGroup("0",target,targetname);
				statAllInfo.agentgroup.add(agentgroup);
			}else if(typename.equals("QUEUEGROUP")) {
				QueueGroup queuegroup=new QueueGroup("0",target,targetname);
				statAllInfo.queuegroup.add(queuegroup);
			}
			
		} catch (ProtocolException e) {
			log.error(e);
		}
	}
	public static java.util.List<java.util.Map<String, Object>> GetWeeklydata(){
		java.util.List<java.util.Map<String, Object>> list=new ArrayList<java.util.Map<String, Object>>();
	//	String startdate=chenUtil.GetBeforeSevenData();//获取七天前日期
		String QueryStr =" SELECT t.cal_day_name," + 
				"    t.label_yyyy_mm_dd," + 
				"        CASE" + 
				"            WHEN e.group_name IS NULL THEN t.resource_name" + 
				"            ELSE e.group_name" + 
				"        END AS resource_name," + 
				"    sum(t.accepted_agent) as accepted_agent," + 
				"    sum(t.accepted_10s) as accepted_10s," + 
				"    sum(f.abandoned) as abandoned," + 
				"    sum(f.abandoned_10s) as abandoned_10s" + 
				"   FROM ( SELECT " + 
				"	  d.cal_day_name," + 
				"    d.label_yyyy_mm_dd," + 
				"	 d.cal_hour_24_num_in_day," + 
				"            d.label_yyyy_mm_dd_hh24," + 
				"            r.resource_name," + 
				"            agt_queue_acc_agent_hour.date_time_key," + 
				"            agt_queue_acc_agent_hour.resource_key," + 
				"            agt_queue_acc_agent_hour.accepted_agent," + 
				"            agt_queue_acc_agent_hour.accepted_agent_sti_1 + agt_queue_acc_agent_hour.accepted_agent_sti_2 AS accepted_10s" + 
				"           FROM agt_queue_acc_agent_hour" + 
				"             LEFT JOIN date_time d ON agt_queue_acc_agent_hour.date_time_key = d.date_time_key" + 
				"             LEFT JOIN resource_ r ON agt_queue_acc_agent_hour.resource_key = r.resource_key" + 
				"						 where cal_hour_24_num_in_day between '8' and '18') t" + 
				"     FULL JOIN ( SELECT  d.cal_day_name," + 
				"    d.label_yyyy_mm_dd," + 
				"		 d.cal_hour_24_num_in_day," + 
				"            d.label_yyyy_mm_dd_hh24," + 
				"            r.resource_name," + 
				"            agt_queue_abn_hour.date_time_key," + 
				"            agt_queue_abn_hour.resource_key," + 
				"            agt_queue_abn_hour.abandoned," + 
				"            agt_queue_abn_hour.abandoned_sti_1 + agt_queue_abn_hour.abandoned_sti_2 AS abandoned_10s" + 
				"           FROM agt_queue_abn_hour" + 
				"             LEFT JOIN date_time d ON agt_queue_abn_hour.date_time_key = d.date_time_key" + 
				"             LEFT JOIN resource_ r ON agt_queue_abn_hour.resource_key = r.resource_key" + 
				"						 where cal_hour_24_num_in_day between '8' and '18'" + 
				"						 ) f ON t.date_time_key = f.date_time_key AND t.resource_key = f.resource_key" + 
				"     LEFT JOIN ( SELECT r.resource_name," + 
				"            g.group_name" + 
				"           FROM resource_group_fact t_1" + 
				"             LEFT JOIN group_ g ON t_1.group_key = g.group_key" + 
				"             LEFT JOIN resource_ r ON t_1.resource_key = r.resource_key" + 
				"          WHERE g.group_type = 'Queue') e ON t.resource_name = e.resource_name" + 
				"  WHERE to_date(t.label_yyyy_mm_dd_hh24, 'yyyy-mm-dd') >= (CURRENT_DATE - interval '6 days')" + 
				"	and t.cal_hour_24_num_in_day between '8' and '18'" + 
				"	AND e.group_name = 'C1'" + 
				"	group by  t.cal_day_name," + 
				"    t.label_yyyy_mm_dd," + 
				"        CASE" + 
				"            WHEN e.group_name IS NULL THEN t.resource_name" + 
				"            ELSE e.group_name" + 
				"        END" + 
				"				";
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
					Query query = gdbsession.createSQLQuery(QueryStr);
					String sProperty = StringUtils.EMPTY;
					log.info(QueryStr);
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					java.util.List<java.util.Map<String, Object>> alldate=chenUtil.datelist();
					for(int i=0;i<alldate.size();i++) {
						for(int j=0;j<CallRecordList.size();j++) {
							Map<String,Object> map1=alldate.get(i);
							Map<String,Object> map2=CallRecordList.get(j);
							if(map1.get("label_yyyy_mm_dd")!=null&&map2.get("label_yyyy_mm_dd")!=null&&map1.get("label_yyyy_mm_dd").equals(map2.get("label_yyyy_mm_dd"))
									&&map1.get("resource_name")!=null&&map2.get("resource_name")!=null&&map1.get("resource_name").equals(map2.get("resource_name")) ){
								map1.put("cal_day_name",map2.get("cal_day_name"));
								map1.put("accepted_agent",map2.get("accepted_agent"));
								map1.put("accepted_10s",map2.get("accepted_10s"));
								map1.put("abandoned",map2.get("abandoned"));
								map1.put("abandoned_10s",map2.get("abandoned_10s"));
							}
						}
					}
					list=alldate;
			} catch (Throwable e) {
				log.warn("ERROR:", e);
			}
			if(gdbsession.isOpen()) {
				gdbsession.close();
			}
		}catch (Throwable e) {
			log.warn("ERROR:", e);
		}
		
		return list;
	}
	
	public static java.util.List<java.util.Map<String, Object>> GetDaylyDate(){
		java.util.List<java.util.Map<String, Object>> list=new ArrayList<java.util.Map<String, Object>>();
		//String startdate=chenUtil.GetBeforeSevenData();//获取七天前日期
		String QueryStr ="      SELECT t.cal_hour_24_num_in_day," + 
				"    t.label_yyyy_mm_dd_hh24," + 
				"        CASE" + 
				"            WHEN e.group_name IS NULL THEN t.resource_name" + 
				"            ELSE e.group_name" + 
				"        END AS resource_name," + 
				"    sum(t.accepted_agent) as accepted_agent," + 
				"    sum(t.accepted_10s) as accepted_10s," + 
				"    sum(f.abandoned) as abandoned," + 
				"    sum(f.abandoned_10s) as abandoned_10s" + 
				"   FROM ( SELECT d.cal_hour_24_num_in_day," + 
				"            d.label_yyyy_mm_dd_hh24," + 
				"            r.resource_name," + 
				"            agt_queue_acc_agent_hour.date_time_key," + 
				"            agt_queue_acc_agent_hour.resource_key," + 
				"            agt_queue_acc_agent_hour.accepted_agent," + 
				"            agt_queue_acc_agent_hour.accepted_agent_sti_1 + agt_queue_acc_agent_hour.accepted_agent_sti_2 AS accepted_10s" + 
				"           FROM agt_queue_acc_agent_hour" + 
				"             LEFT JOIN date_time d ON agt_queue_acc_agent_hour.date_time_key = d.date_time_key" + 
				"             LEFT JOIN resource_ r ON agt_queue_acc_agent_hour.resource_key = r.resource_key) t" + 
				"     FULL JOIN ( SELECT d.cal_hour_24_num_in_day," + 
				"            d.label_yyyy_mm_dd_hh24," + 
				"            r.resource_name," + 
				"            agt_queue_abn_hour.date_time_key," + 
				"            agt_queue_abn_hour.resource_key," + 
				"            agt_queue_abn_hour.abandoned," + 
				"            agt_queue_abn_hour.abandoned_sti_1 + agt_queue_abn_hour.abandoned_sti_2 AS abandoned_10s" + 
				"           FROM agt_queue_abn_hour" + 
				"             LEFT JOIN date_time d ON agt_queue_abn_hour.date_time_key = d.date_time_key" + 
				"             LEFT JOIN resource_ r ON agt_queue_abn_hour.resource_key = r.resource_key) f ON t.date_time_key = f.date_time_key AND t.resource_key = f.resource_key" + 
				"     LEFT JOIN ( SELECT r.resource_name," + 
				"            g.group_name" + 
				"           FROM resource_group_fact t_1" + 
				"             LEFT JOIN group_ g ON t_1.group_key = g.group_key" + 
				"             LEFT JOIN resource_ r ON t_1.resource_key = r.resource_key" + 
				"          WHERE g.group_type = 'Queue') e ON t.resource_name = e.resource_name" + 
				"  WHERE to_date(t.label_yyyy_mm_dd_hh24, 'yyyy-mm-dd') = CURRENT_DATE AND e.group_name = 'C1'" + 
				"	group by  t.cal_hour_24_num_in_day," + 
				"    t.label_yyyy_mm_dd_hh24," + 
				"        CASE" + 
				"            WHEN e.group_name IS NULL THEN t.resource_name" + 
				"            ELSE e.group_name" + 
				"        END";
		
		try {
			Session gdbsession = GHibernateSessionFactory.getThreadSession();
			try {
					Query query = gdbsession.createSQLQuery(QueryStr);
					String sProperty = StringUtils.EMPTY;
					log.info(QueryStr);
					query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP);
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					list=CallRecordList;
			} catch (Throwable e) {
				log.warn("ERROR:", e);
			}
			if(gdbsession.isOpen()) {
				gdbsession.close();
			}
		}catch (Throwable e) {
			log.warn("ERROR:", e);
		}
		return list;
	}
	
	public static void  senddaylyinfo() {
		Gson gson = gsonBuilder.create();
		statAllInfo.init = false;
		statAllInfo.sender = "Event";
		statAllInfo.daylydata=GetDaylyDate();
		String json = gson.toJson(statAllInfo);
		log.info("输出sendweeklyinfo:"+json);
		StatWebSocketServlet.sendMonitorText(json);
	}
	
	public static void  sendweeklyinfo() {
		Gson gson = gsonBuilder.create();
		statAllInfo.init = false;
		statAllInfo.sender = "Event";
		statAllInfo.weeklydata=GetWeeklydata();
		String json = gson.toJson(statAllInfo);
		log.info("输出sendweeklyinfo:"+json);
		StatWebSocketServlet.sendMonitorText(json);
	}
	public static void sendAllInfo(String roleguid,String queues, org.eclipse.jetty.websocket.api.Session wsSession) {
		Gson gson = gsonBuilder.create();
		statAllInfo.init = true;
		statAllInfo.sender = "Event";
		statAllInfo.weeklydata=GetWeeklydata();
		statAllInfo.daylydata=GetDaylyDate();
		String json = gson.toJson(statAllInfo);
			log.info("输出statAllInfo:"+json);
			if(wsSession!=null) {
				//仅仅将消息返回给请求连接者
				wsSession.getRemote().sendStringByFuture(json);
			}else {
				//将消息发送给所有wssession
				StatWebSocketServlet.sendMonitorText(json);
			}
	//	GenesysStatServer.Instance.PeekStatisticAgentState();
	}
	
}
