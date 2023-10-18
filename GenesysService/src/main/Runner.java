package main;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.slf4j.bridge.SLF4JBridgeHandler;

import hbm.factory.GHibernateSessionFactory;
import hbm.factory.HibernateSessionFactory;
import hbm.model.Rbacoperation;
import hbm.model.Rbacrole;
import hbm.model.Rbacroleoperation;
import hbm.model.RbacroleoperationId;
import hbm.model.Rbacroleuser;
import hbm.model.RbacroleuserId;
import hbm.model.Rbacuser;
import hbm.model.Rbacuserauths;
import tab.FLPgenesysService;
import tab.GenesysStatServer;
import tab.jetty.StatWebSocketServlet;
import tab.rbac.AliyunToken;
import tab.rbac.RbacClient;
import tab.rbac.RbacSystem;
import tab.util.Util;
import tab.util.chenUtil;

public class Runner {
	public static String ConfigName_;
	static {
		try {
			java.io.File f = new java.io.File(Runner.class.getProtectionDomain().getCodeSource().getLocation().getFile());
			if(f.exists()){
				 String sRealPath = java.net.URLDecoder.decode(f.getAbsolutePath(), "utf-8");
				if(sRealPath.substring(sRealPath.length()-4).equalsIgnoreCase(File.separator + "bin")){
					//Eclipse调试状态
					sRealPath = sRealPath.substring(0,sRealPath.length()-4);
					int pos = sRealPath.lastIndexOf(File.separator);
					if(pos>=0){
						ConfigName_ = sRealPath.substring(pos+1);
					}
				}else if(sRealPath.substring(sRealPath.length()-4).equalsIgnoreCase(".jar")){
					//Jar包运行状态
					int pos = sRealPath.lastIndexOf(File.separator);
					if(pos>=0){
						ConfigName_ = sRealPath.substring(pos+1,sRealPath.length()-4);
						sRealPath = sRealPath.substring(0,pos);						
					}
				}else {
					sRealPath = java.net.URLDecoder.decode(f.getParentFile().getAbsolutePath(), "utf-8");
					int pos = sRealPath.lastIndexOf(File.separator);
					if(pos>=0){
						ConfigName_ = sRealPath.substring(pos+1);
					}
				}
				//真实路径设置到系统变量
				System.setProperty("tab.path", sRealPath);
				System.setProperty("org.sqlite.tmpdir", sRealPath+File.separator+"tmpdir");
				java.io.File path = new java.io.File(System.getProperty("org.sqlite.tmpdir"));
				path.mkdir();
				java.io.File[] tempFile = path.listFiles(); 
				for(int i = 0; i < tempFile.length; i++){ 
					java.io.File file = new java.io.File(tempFile[i].getAbsolutePath());
					if(file.isFile()){
						file.delete();
					}
				}
				int pos = sRealPath.lastIndexOf(File.separator);
				if(pos>=0){
					String sFileName = sRealPath.substring(0, pos+1) + "Logs" + File.separator + ConfigName_ + File.separator;
					System.setProperty("tab.logpath", sFileName);
				}
				PropertyConfigurator.configure(sRealPath + File.separator + "log4j.properties");
			}
		} catch (UnsupportedEncodingException e) {
		}
	}
	public static String PhonerWsUri = StringUtils.EMPTY;
	public static String SSGUrl = StringUtils.EMPTY;
	public static String NotificationURL = StringUtils.EMPTY;
	public static String IVRProfileName = "ssg";
	public static int nCurrentNumber = 100;
	public static String PhonerBaseUri = StringUtils.EMPTY;
	public static boolean bRegisterPbxAgent = false;
	public static final String sErrorMessage = "发现错误请记录时间等信息, 联系管理员查看后台日志";
	public static int PhonerAcwTime = 60;
	public static int PhonerAutoReadyTime = 0;
	public static String encryptAlgorithm = null;
	public static int nJerseyLogLevel = 0;
	public static long nSessionIdleTimeout = 43200;//12小时
	public static long nVerifycodeTimeout = 60;//秒
	public static int nKeepaliveDelay = 60;//秒
	public static int nWebPort = 55511;
	public static boolean bCompatibleMode = false;
	public static int nPlayMode = 1;
	public static boolean bUsingCallAgentRecord = false;
	public static int MAX_AGENT_RANKING = 5;// 坐席排名取的最大
	public static int MIN_AGENT_RANKING = 0;// 坐席排名取的最小值
	public static String AliyunAccessKeyId = StringUtils.EMPTY;
	public static String AliyunAccessKeySecret = StringUtils.EMPTY;
	public static String sConfHost = StringUtils.EMPTY;
	public static String sConfUsername = "default";
	public static String sConfPassword = StringUtils.EMPTY;
	public static int nConfPort = 2020;
	public static String sStatHost = StringUtils.EMPTY;
	public static int nStatPort = 3060;
	public static String sStatHostBackup = StringUtils.EMPTY;
	public static int nStatPortBackup = 3060;
	public static GenesysStatServer statServer = null;
	public static FLPgenesysService FLPstatServer = null;
	public static String sPlayHost = "http://gene.jsleasing.cn";
	public static String sPlayPath = "/api/v2/ops/contact-centers/625a5cf1-dea8-4e46-b18b-831d4b254488/recordings/";
	public static String sPlayUrl = "http://gene.jsleasing.cn";
	public static String sPlayUsername = "gir";
	public static String sPlayPassword = "gir";
	public static String sJFL_JRSYB = "19";
	public static String sJFL_CRZX = "15";	
	public static String sJFL_IVRNAME = "admin";
	public static Integer nStartTime = 28800;
	public static Integer nEndTime = 82800;
	public static java.util.List<String> Queues = new java.util.ArrayList<String>();
	public static java.util.List<String> AgentGroups = new java.util.ArrayList<String>();
	
	public static tab.jetty.WebServer server;
	public static String sHideAgentRegEx = ".*[a-zA-Z]+.*|80000[0-2]";
	public static String sDebugPhone = "";
	public static String sQRCodeUrl = "https://dev.tab.sh.cn";
	public static int nLoginType = 1;
	public static String sLoginTypeValue = StringUtils.EMPTY;
	public static int nBindType = 90;
	public static String sAuthorizedRoleGuid;
	public static String sAuthorizedAppId;
	public static String CustomerName="";
	public static String sSuffix="@SIP_Switch";
	public static String sQuffix="_SIP_Switch@SIP_Switch";
	public static String firstDigit=StringUtils.EMPTY;
	public static String secondDigit=StringUtils.EMPTY;
	public static Integer startCopy = 0;//数据库实体表转换试图
	public static long copttableEveryTime = 300;//秒
	public static long getdaylyeverytime = 900;//秒
	public static long getweeklylyeverytime = 3600;//秒
	//CommonsLog日志输出
	private static Log logger = LogFactory.getLog(Runner.class);
	//Log4j日志输出
	private static final org.apache.log4j.Logger log4jLogger = org.apache.log4j.Logger.getLogger(Runner.class);
	//JDK14日志输出
	private static final java.util.logging.Logger jdkLogger = java.util.logging.Logger.getLogger(Runner.class.getName());
	//Slft4j日志输出
	private static final org.slf4j.Logger slf4jLogger = org.slf4j.LoggerFactory.getLogger(Runner.class);
	
	public static void main(String[] args) throws ClassNotFoundException {
		//整合(slf4j+commonslog+log4j+jdklogger)
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();	
		server = new tab.jetty.WebServer();
		tab.configServer.ValueString vs = new tab.configServer.ValueString(StringUtils.EMPTY);
		tab.configServer.ValueInteger vi = new tab.configServer.ValueInteger(nWebPort);
		tab.configServer.getInstance().getValue(ConfigName_, "WebPort",vi,"配置服务提供的RESTful Service网络端口，0关闭,关闭后必须开启SSL端口",StringUtils.EMPTY,true,true);
		if(vi.value>0)server.setPort(vi.value);
		vi.value = 0;
		tab.configServer.getInstance().getValue(ConfigName_, "WebSslPort",vi,"配置服务提供的RESTful Service SSL网络端口，默认0关闭",StringUtils.EMPTY,true,true);
		if(vi.value>0)server.setSslPort(vi.value);
		vi.value = 50;
		tab.configServer.getInstance().getValue(ConfigName_, "AcceptQueueSize",vi,"接受网络连接请求的队列大小",StringUtils.EMPTY,true,true);
		server.setAcceptQueueSize(vi.value);
		vi.value = 0;
		tab.configServer.getInstance().getValue(ConfigName_, "RequestHeaderSize",vi,"默认0不限制大小，资源不够时需要限制大小",StringUtils.EMPTY,true,true);
		server.setRequestHeaderSize(vi.value);
		
		vi.value = 0;
		tab.configServer.getInstance().getValue(ConfigName_, "JettyLogLevel",vi,"Jetty日志等级，0默认，1是警告，2是INFO，其他全部",StringUtils.EMPTY,true,true);
		switch(vi.value){
		case 0:
			break;
		case 1:
			LogManager.getLogger("org.eclipse.jetty").setLevel(org.apache.log4j.Level.WARN);
			java.util.logging.Logger.getLogger(StringUtils.EMPTY).setLevel(java.util.logging.Level.WARNING);
			break;
		case 2:
			LogManager.getLogger("org.eclipse.jetty").setLevel(org.apache.log4j.Level.INFO);
			java.util.logging.Logger.getLogger(StringUtils.EMPTY).setLevel(java.util.logging.Level.INFO);
			break;
		default:
			LogManager.getLogger("org.eclipse.jetty").setLevel(org.apache.log4j.Level.ALL);
			java.util.logging.Logger.getLogger(StringUtils.EMPTY).setLevel(java.util.logging.Level.SEVERE);
			break;
		}
		
		vi.value = 0;
		tab.configServer.getInstance().getValue(ConfigName_, "JerseyLogLevel",vi,"jersey日志等级，0默认，1是定制,  2关闭和INFO，3demand和警告，其他全部",StringUtils.EMPTY,false,true);
		nJerseyLogLevel = vi.value;
		
		vi.value = nKeepaliveDelay;
		tab.configServer.getInstance().getValue(ConfigName_, "KeepaliveDelay",vi,"Web界面无操作退出时间, 默认60,单位分钟",StringUtils.EMPTY,false,true);
		nKeepaliveDelay = vi.value;		
		RbacClient.setKeepaliveDelay(nKeepaliveDelay);
		vi.value = (int)nSessionIdleTimeout;
		tab.configServer.getInstance().getValue(ConfigName_, "KeepaliveTimeout",vi,"Web客户端空闲自动退出时长,默认43200为12小时,单位秒",StringUtils.EMPTY,false,true);
		nSessionIdleTimeout = vi.value;
		
		vi.value = (int)nVerifycodeTimeout;
		tab.configServer.getInstance().getValue(ConfigName_, "VerifycodeTimeout",vi,"验证码超时时长,单位秒",StringUtils.EMPTY,false,true);
		nVerifycodeTimeout = vi.value;		
		vi.value = (int)copttableEveryTime;
		tab.configServer.getInstance().getValue(ConfigName_, "CopttableEveryTime",vi,"复制表任务间隔",StringUtils.EMPTY,false,true);
		copttableEveryTime = vi.value;	
		
		vi.value = (int)getdaylyeverytime;
		tab.configServer.getInstance().getValue(ConfigName_, "getdaylyeverytime",vi,"复制表任务间隔",StringUtils.EMPTY,false,true);
		getdaylyeverytime = vi.value;	
		
		
		vi.value = (int)getweeklylyeverytime;
		tab.configServer.getInstance().getValue(ConfigName_, "getweeklylyeverytime",vi,"复制表任务间隔",StringUtils.EMPTY,false,true);
		getweeklylyeverytime = vi.value;	
		
		vi.value = 0;
		tab.configServer.getInstance().getValue(ConfigName_, "InitSystem",vi,"初始化系统, 1清除无关联用户记录, 2重置录音权限, 3重置admin密码, 4重置admin权限和录音权限",StringUtils.EMPTY,true,true);
		Integer nInitSystem = vi.value;
		
		vs.value = StringUtils.EMPTY;
		tab.configServer.getInstance().getValue(ConfigName_, "NewOperation",vs,"新增权限，默认空不增加权限",StringUtils.EMPTY,true,true);
		String sNewOperation = vs.value;
		
		vs.value = "新增权限";
		tab.configServer.getInstance().getValue(ConfigName_, "NewOperationName",vs,"新增权限名字",StringUtils.EMPTY,true,true);
		String sNewOperationName = vs.value.length()==0?"新增权限":vs.value;
		
		vs.value = StringUtils.EMPTY;
		tab.configServer.getInstance().getValue(ConfigName_, "AuthorizedAppId",vs,"授权管理的APPID，默认空无授权APPID, 第三方应用必须使用反向代理调用/tab/rabc/UILogin,共享session方法登录",StringUtils.EMPTY, false,true);
		if(vs.value.length()>0) {
			sAuthorizedRoleGuid = Util.uncompressUUID(vs.value);
			if(!sAuthorizedRoleGuid.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
				tab.configServer.getInstance().setValue(ConfigName_, "AuthorizedAppId",StringUtils.EMPTY,StringUtils.EMPTY);
				sAuthorizedRoleGuid = Util.ROOT_ROLEGUID;
			}
		}else {
			sAuthorizedRoleGuid = Util.ROOT_ROLEGUID;
		}
		sAuthorizedAppId = Util.compressUUID(sAuthorizedRoleGuid);
		
		vs.value = "ws://127.0.0.1:55511";
		tab.configServer.getInstance().getValue(ConfigName_, "BaseUri",vs,"软电话二次开发接口地址","Phoner",false,true);
		PhonerBaseUri = vs.value;	
		vs.value = "";
		tab.configServer.getInstance().getValue(ConfigName_, "WsUri",vs,"软电话二次开发接口地址","Phoner",false,true);
		PhonerWsUri = vs.value;
		vi.value = PhonerAutoReadyTime;
		tab.configServer.getInstance().getValue(ConfigName_, "AutoReadyTime",vi,"软电话二次开发接口, 自动就绪理时长, 小于后处理时长(PhonerAcwTime)则不开启自动就绪功能, 例如300","Phoner",false,true);
		PhonerAutoReadyTime = vi.value;
		vi.value = PhonerAcwTime;
		tab.configServer.getInstance().getValue(ConfigName_, "AcwTime",vi,"软电话二次开发接口, 后处理时长, 例如60","Phoner",false,true);
		PhonerAcwTime = vi.value;
		
		vs.value = sDebugPhone;
		tab.configServer.getInstance().getValue(ConfigName_, "DebugPhone",vs,"使用模拟电话外呼","", false,true);
		sDebugPhone = vs.value;
		
		vs.value = sHideAgentRegEx;
		tab.configServer.getInstance().getValue(ConfigName_, "HideAgentRegEx",vs,"隐藏不处理的坐席工号正则表达式","", false,true);
		sHideAgentRegEx = vs.value;

		vs.value = sConfUsername;
		tab.configServer.getInstance().getValue(ConfigName_, "ConfUsername",vs,"Genesys配置账号",StringUtils.EMPTY,false,true);
		sConfUsername = vs.value;
		vs.value = sConfPassword;
		tab.configServer.getInstance().getValue(ConfigName_, "ConfPassword",vs,"Genesys配置密码",StringUtils.EMPTY,false,true);
		sConfPassword = vs.value;
		
		vs.value = sConfHost;
		tab.configServer.getInstance().getValue(ConfigName_, "ConfHost",vs,"Genesys配置服务器地址URL",StringUtils.EMPTY,false,true);
		sConfHost = vs.value;
		
		vi.value = nConfPort;
		tab.configServer.getInstance().getValue(ConfigName_, "ConfPort",vi,"Genesys配置服务器网络端口",StringUtils.EMPTY,false,true);
		nConfPort = vi.value;
		
		
		vs.value = sStatHost;
		tab.configServer.getInstance().getValue(ConfigName_, "StatHost",vs,"Genesys监控服务器地址",StringUtils.EMPTY,false,true);
		sStatHost = vs.value;
		
		vi.value = nStatPort;
		tab.configServer.getInstance().getValue(ConfigName_, "StatPort",vi,"Genesys监控服务器网络端口",StringUtils.EMPTY,false,true);
		nStatPort = vi.value;
		
		vs.value = sStatHostBackup;
		tab.configServer.getInstance().getValue(ConfigName_, "StatHostBackup",vs,"备份Genesys监控服务器地址",StringUtils.EMPTY,false,true);
		sStatHostBackup = vs.value;
		
		vi.value = nStatPortBackup;
		tab.configServer.getInstance().getValue(ConfigName_, "StatPortBackup",vi,"备份Genesys监控服务器网络端口",StringUtils.EMPTY,false,true);
		nStatPortBackup = vi.value;
		
		vs.value = sPlayHost;
		tab.configServer.getInstance().getValue(ConfigName_, "PlayHost",vs,"录音查询对接的录音查询接口服务器URL",StringUtils.EMPTY,false,true);
		sPlayHost = vs.value;
		vs.value = sPlayPath;
		tab.configServer.getInstance().getValue(ConfigName_, "PlayPath",vs,"录音查询对接的录音查询路径",StringUtils.EMPTY,false,true);
		sPlayPath = vs.value;
		
		
		vs.value = sPlayUrl;
		tab.configServer.getInstance().getValue(ConfigName_, "PlayUrl",vs,"录音查询对接的录音后台系统URL",StringUtils.EMPTY,false,true);
		sPlayUrl = vs.value;
		
		vs.value = sPlayUsername;
		tab.configServer.getInstance().getValue(ConfigName_, "PlayUsername",vs,"录音查询对接的录音后台系统Basic验证用户名",StringUtils.EMPTY,false,true);
		sPlayUsername = vs.value;
		
		vs.value = sPlayPassword;
		tab.configServer.getInstance().getValue(ConfigName_, "PlayPassword",vs,"录音查询对接的录音后台系统Basic验证密码",StringUtils.EMPTY,false,true);
		sPlayPassword = vs.value;
		
		vs.value = StringUtils.EMPTY;
		tab.configServer.getInstance().getValue(ConfigName_, "Queues",vs,"使用的队列, 多个队列逗号分隔","",false,true);
		logger.warn("Queues: " + vs.value);
		String[] vGroups = vs.value.split(",");
		for(String sGroup:vGroups) {
			Queues.add(sGroup);
		}
		vs.value = StringUtils.EMPTY;
		tab.configServer.getInstance().getValue(ConfigName_, "AgentGroups",vs,"使用的坐席组, 多个队列逗号分隔","",false,true);
		logger.warn("AgentGroups: " + vs.value);
		String[] vAgentGroups = vs.value.split(",");
		for(String sGroup:vAgentGroups) {
			AgentGroups.add(sGroup);
		}
		
		
		vs.value = "WebRoot";
		tab.configServer.getInstance().getValue(ConfigName_, "WebRoot",vs,"网页根目录",StringUtils.EMPTY,false,true);
		server.setWebRoot(System.getProperty("tab.path") + File.separator + vs.value,"/");
		
		vs.value = StringUtils.EMPTY;
		tab.configServer.getInstance().getValue(ConfigName_, "NodeName",vs,"多个Jetty负载均衡群集运行时,用于共享Session的唯一节点名,例如:node0,默认空则关闭该功能",StringUtils.EMPTY,false,true);
		if(vs.value.length()>0) {
			server.setDriverInfo(vs.value,HibernateSessionFactory.getDriverClassName(), HibernateSessionFactory.getConnectionUrl());
		}
		
		vs.value = "http://localhost:55511";
		tab.configServer.getInstance().getValue(ConfigName_, "RbacURL",vs,"权限系统的URL地址前缀","",false,true);
		RbacClient.setRbacUrl(vs.value);
		
		vs.value = "";
		tab.configServer.getInstance().getValue(ConfigName_, "RbacAppId",vs,"权限系统的APPID, 如果不使用该配置, 则表示创建反向代理, 共享Session的应用","",false,true);
		RbacClient.setAppId(vs.value);
		
		vs.value = "";
		tab.configServer.getInstance().getValue(ConfigName_, "RbacSecret",vs,"权限系统的安全码(SECRET)","",false,true);
		RbacClient.setSecret(vs.value);
		
		vs.value = AliyunAccessKeyId;
		tab.configServer.getInstance().getValue(ConfigName_, "AliyunAccessKeyId",vs,"阿里云访问账号",StringUtils.EMPTY,false,true);
		AliyunAccessKeyId = vs.value;
		vs.value = AliyunAccessKeySecret;
		tab.configServer.getInstance().getValue(ConfigName_, "AliyunAccessKeySecret",vs,"阿里云访问安全码",StringUtils.EMPTY,false,true);
		AliyunAccessKeySecret = vs.value;
		
		vs.value = StringUtils.EMPTY;
		tab.configServer.getInstance().getValue(ConfigName_, "AliyunAccessToken",vs,"阿里云访问令牌",StringUtils.EMPTY,false,true);
		AliyunToken.getInstance().setAccessToken(vs.value);
		vs.value = "0";
		tab.configServer.getInstance().getValue(ConfigName_, "AliyunExpiresIn",vs,"阿里云访问令牌过期时间",StringUtils.EMPTY,false,true);
		if(!"0".equals(vs.value)) {
			AliyunToken.getInstance().setExpiresIn(Long.parseLong(vs.value));
		}
		
		vi.value = 15;
		tab.configServer.getInstance().getValue(ConfigName_, "DbSearchPeriod",vi,"数据库轮询间隔(分钟)","",false,true);
		int nDbSearchPeriod = vi.value<15?15:vi.value;
		
		vs.value = sJFL_JRSYB;
		tab.configServer.getInstance().getValue(ConfigName_, "JFL_JRSYB",vs,"对应的部门编号",StringUtils.EMPTY,false,true);
		sJFL_JRSYB = vs.value;
		
		vs.value = sJFL_CRZX;
		tab.configServer.getInstance().getValue(ConfigName_, "JFL_CRZX",vs,"对应的部门编号",StringUtils.EMPTY,false,true);
		sJFL_CRZX = vs.value;		
		
		vs.value = sJFL_IVRNAME;
		tab.configServer.getInstance().getValue(ConfigName_, "JFL_IVRNAME",vs,"对应的自动语音用户名",StringUtils.EMPTY,false,true);
		sJFL_IVRNAME = vs.value;	
		
		vi.value = nStartTime;
		tab.configServer.getInstance().getValue(ConfigName_, "StartTime",vi,"对应的自动语音每天开始时间",StringUtils.EMPTY,false,true);
		nStartTime = vi.value;		
		
		vi.value = nEndTime;
		tab.configServer.getInstance().getValue(ConfigName_, "EndTime",vi,"对应的自动语音每天结束时间",StringUtils.EMPTY,false,true);
		nEndTime = vi.value;
		
		vs.value = SSGUrl;
		tab.configServer.getInstance().getValue(ConfigName_, "SSGUrl",vs,"ssg接口url",StringUtils.EMPTY,false,true);
		SSGUrl = vs.value;
		vs.value = NotificationURL;
		tab.configServer.getInstance().getValue(ConfigName_, "NotificationURL",vs,"ssg结果返回通知接口",StringUtils.EMPTY,false,true);
		NotificationURL = vs.value;
		vs.value = IVRProfileName;
		tab.configServer.getInstance().getValue(ConfigName_, "IVRProfileName",vs,"ivr流程名称",StringUtils.EMPTY,false,true);
		IVRProfileName = vs.value;
		
		vs.value = CustomerName;
		tab.configServer.getInstance().getValue(ConfigName_, "CustomerName",vs,"客户的名称",StringUtils.EMPTY,false,true);
		CustomerName = vs.value;
		
		vs.value = sQuffix;
		tab.configServer.getInstance().getValue(ConfigName_, "sQuffix",vs,"Genesys队列后缀",StringUtils.EMPTY,false,true);
		sQuffix = vs.value;
		
		vs.value = sSuffix;
		tab.configServer.getInstance().getValue(ConfigName_, "sSuffix",vs,"Genesys分机后缀",StringUtils.EMPTY,false,true);
		sSuffix = vs.value;
		
		vs.value = firstDigit;
		tab.configServer.getInstance().getValue(ConfigName_, "firstDIgit",vs,"一层按键",StringUtils.EMPTY,false,true);
		firstDigit = vs.value;
		
		vs.value = secondDigit;
		tab.configServer.getInstance().getValue(ConfigName_, "secondDigit",vs,"二层按键",StringUtils.EMPTY,false,true);
		secondDigit = vs.value;
		vi.value = nCurrentNumber;
		tab.configServer.getInstance().getValue(ConfigName_, "nCurrentNumber",vi,"没有网关配置的时候选择的并发数",StringUtils.EMPTY,false,true);
		nCurrentNumber = vi.value;
		vi.value=startCopy;
		tab.configServer.getInstance().getValue(ConfigName_, "nstartCopy",vi,"启动数据库实体表视图转换",StringUtils.EMPTY,false,true);
		startCopy = vi.value;
		//server.addHttpServlet(new tab.jetty.RbacProxy(), "/tab/oauth2", true);
		//server.addHttpServlet(new tab.jetty.RbacProxy(), "/tab/rbac", true);
		server.addHttpServlet(new tab.jetty.TabUtilServlet(), "/tab/util", false);
		server.addWebSocketServlet(new StatWebSocketServlet(), "/tab/monitor");
		server.addRestConfig(new tab.TabResourceConfig(),"/tab",true);
		
		jdkLogger.warning("ConfigTableName(Jdk): " + ConfigName_);
		logger.warn("LogFile(CommonsLog): " + System.getProperty("tab.logpath"));
		log4jLogger.warn("WebRoot(Log4j): " + server.getWebRoot());
		slf4jLogger.warn("WebPort(Slf4j): ["+server.getPort()+", "+server.getSslPort()+"]");
		
		if(args.length>0 && args[0].equals("init")) {
			nInitSystem = 4;
		}else if(args.length>0 && args[0].equals("initcall")) {
			sNewOperation = "6E48F8F3-93EC-11E9-9604-54E1AD6C1F93,328F332E-AEAE-11E9-80B6-000C294BD5A6,6C8605F6-B994-11E9-9EA9-54E1AD6C1F93,"
					+ "BBFED234-B994-11E9-9EA9-54E1AD6C1F93,C454655B-B994-11E9-9EA9-54E1AD6C1F93,B0ED5C1A-BCD7-11E9-BF43-54E1AD6C1F93,"
					+ "48E6621D-D3A2-11E9-80B6-000C294BD5A6,952C8D60-4F8A-11EB-B5EB-00155D782604,C554655B-B994-11E9-9EA9-54E1AD6C1F93";
		}
		if(nInitSystem>0 || sNewOperation.length()>0) {
			try{
				Session session = HibernateSessionFactory.getThreadSession();
				try{
					Transaction ts = session.beginTransaction();
					try{
						if(nInitSystem==1) {
							int nRow = session.createQuery("delete Rbacroleuser A where not exists(select B.userguid from Rbacuser B where B.userguid=A.id.userguid)").executeUpdate();
							logger.info("清理"+nRow+"条垃圾用户成功");
						}
						if(nInitSystem==2 || nInitSystem==4){
							if(session.createQuery(" from Rbacoperation where operationguid=:operationguid")
									.setString("operationguid","45BBBF87-25B8-492A-B206-CC55F3E4903C").list().size()<=0) {
								//录音查询权限
								Rbacoperation recsearch = new Rbacoperation();
								recsearch.setOperationguid("45BBBF87-25B8-492A-B206-CC55F3E4903C");
								recsearch.setOperationname("录音查询");
								session.save(recsearch);
							}
							if(session.createQuery(" from Rbacroleoperation where id.operationguid=:operationguid and id.roleguid=:roleguid")
									.setString("operationguid","45BBBF87-25B8-492A-B206-CC55F3E4903C").setString("roleguid",RbacSystem.ROOT_ROLEGUID).list().size()<=0) {
								Rbacroleoperation recsearch = new Rbacroleoperation();
								RbacroleoperationId recsearchId = new RbacroleoperationId();
								recsearchId.setOperationguid("45BBBF87-25B8-492A-B206-CC55F3E4903C");
								recsearchId.setRoleguid(RbacSystem.ROOT_ROLEGUID);
								recsearch.setId(recsearchId);
								session.save(recsearch);
							}
							if(session.createQuery(" from Rbacoperation where operationguid=:operationguid")
									.setString("operationguid","4C0ECEB3-312C-4C42-B919-AFA33AFF8CA6").list().size()<=0) {
								//录音播放权限
								Rbacoperation recplay = new Rbacoperation();
								recplay.setOperationguid("4C0ECEB3-312C-4C42-B919-AFA33AFF8CA6");
								recplay.setOperationname("录音播放");
								session.save(recplay);
							}
							if(session.createQuery(" from Rbacroleoperation where id.operationguid=:operationguid and id.roleguid=:roleguid")
									.setString("operationguid","4C0ECEB3-312C-4C42-B919-AFA33AFF8CA6").setString("roleguid",RbacSystem.ROOT_ROLEGUID).list().size()<=0) {
								Rbacroleoperation recplay = new Rbacroleoperation();
								RbacroleoperationId recplayId = new RbacroleoperationId();
								recplayId.setOperationguid("4C0ECEB3-312C-4C42-B919-AFA33AFF8CA6");
								recplayId.setRoleguid(RbacSystem.ROOT_ROLEGUID);
								recplay.setId(recplayId);
								session.save(recplay);
							}
							if(session.createQuery(" from Rbacoperation where operationguid=:operationguid")
									.setString("operationguid","DEAE3629-2649-4EAA-A1E3-32C758FF2FF8").list().size()<=0) {
								//录音下载权限
								Rbacoperation recdownload = new Rbacoperation();
								recdownload.setOperationguid("DEAE3629-2649-4EAA-A1E3-32C758FF2FF8");
								recdownload.setOperationname("录音下载");
								session.save(recdownload);
							}
							if(session.createQuery(" from Rbacroleoperation where id.operationguid=:operationguid and id.roleguid=:roleguid")
									.setString("operationguid","DEAE3629-2649-4EAA-A1E3-32C758FF2FF8").setString("roleguid",RbacSystem.ROOT_ROLEGUID).list().size()<=0) {
								Rbacroleoperation recdownload = new Rbacroleoperation();
								RbacroleoperationId recdownloadId = new RbacroleoperationId();
								recdownloadId.setOperationguid("DEAE3629-2649-4EAA-A1E3-32C758FF2FF8");
								recdownloadId.setRoleguid(RbacSystem.ROOT_ROLEGUID);
								recdownload.setId(recdownloadId);
								session.save(recdownload);
							}
							if(session.createQuery(" from Rbacoperation where operationguid=:operationguid")
									.setString("operationguid","3CA268C0-E098-4A8B-ABF7-86E215AE7F42").list().size()<=0) {
								//录音标记权限
								Rbacoperation recdownload = new Rbacoperation();
								recdownload.setOperationguid("3CA268C0-E098-4A8B-ABF7-86E215AE7F42");
								recdownload.setOperationname("录音标记");
								session.save(recdownload);
							}
							if(session.createQuery(" from Rbacroleoperation where id.operationguid=:operationguid and id.roleguid=:roleguid")
									.setString("operationguid","3CA268C0-E098-4A8B-ABF7-86E215AE7F42").setString("roleguid",RbacSystem.ROOT_ROLEGUID).list().size()<=0) {
								Rbacroleoperation recdownload = new Rbacroleoperation();
								RbacroleoperationId recdownloadId = new RbacroleoperationId();
								recdownloadId.setOperationguid("3CA268C0-E098-4A8B-ABF7-86E215AE7F42");
								recdownloadId.setRoleguid(RbacSystem.ROOT_ROLEGUID);
								recdownload.setId(recdownloadId);
								session.save(recdownload);
							}
							if(session.createQuery(" from Rbacoperation where operationguid=:operationguid")
									.setString("operationguid","1A16A347-A2CF-11E9-9604-54E1AD6C1F93").list().size()<=0) {
								//录音监控权限
								Rbacoperation recdownload = new Rbacoperation();
								recdownload.setOperationguid("1A16A347-A2CF-11E9-9604-54E1AD6C1F93");
								recdownload.setOperationname("录音监控");
								session.save(recdownload);
							}
							if(session.createQuery(" from Rbacroleoperation where id.operationguid=:operationguid and id.roleguid=:roleguid")
									.setString("operationguid","1A16A347-A2CF-11E9-9604-54E1AD6C1F93").setString("roleguid",RbacSystem.ROOT_ROLEGUID).list().size()<=0) {
								Rbacroleoperation recdownload = new Rbacroleoperation();
								RbacroleoperationId recdownloadId = new RbacroleoperationId();
								recdownloadId.setOperationguid("1A16A347-A2CF-11E9-9604-54E1AD6C1F93");
								recdownloadId.setRoleguid(RbacSystem.ROOT_ROLEGUID);
								recdownload.setId(recdownloadId);
								session.save(recdownload);
							}
							
							if(session.createQuery(" from Rbacoperation where operationguid=:operationguid")
									.setString("operationguid","4A16A347-A2CF-11E9-9604-54E1AD6C1F93").list().size()<=0) {
								//分机监控
								Rbacoperation recdownload = new Rbacoperation();
								recdownload.setOperationguid("4A16A347-A2CF-11E9-9604-54E1AD6C1F93");
								recdownload.setOperationname("分机监控");
								session.save(recdownload);
							}
							if(session.createQuery(" from Rbacroleoperation where id.operationguid=:operationguid and id.roleguid=:roleguid")
									.setString("operationguid","4A16A347-A2CF-11E9-9604-54E1AD6C1F93").setString("roleguid",RbacSystem.ROOT_ROLEGUID).list().size()<=0) {
								Rbacroleoperation recdownload = new Rbacroleoperation();
								RbacroleoperationId recdownloadId = new RbacroleoperationId();
								recdownloadId.setOperationguid("4A16A347-A2CF-11E9-9604-54E1AD6C1F93");
								recdownloadId.setRoleguid(RbacSystem.ROOT_ROLEGUID);
								recdownload.setId(recdownloadId);
								session.save(recdownload);
							}
							if(session.createQuery(" from Rbacoperation where operationguid=:operationguid")
									.setString("operationguid","282CB471-EBC0-11E8-B69B-54E1AD6C1F93").list().size()<=0) {
								//录音分机设置权限
								Rbacoperation recdownload = new Rbacoperation();
								recdownload.setOperationguid("282CB471-EBC0-11E8-B69B-54E1AD6C1F93");
								recdownload.setOperationname("录音分机设置");
								session.save(recdownload);
							}
							if(session.createQuery(" from Rbacroleoperation where id.operationguid=:operationguid and id.roleguid=:roleguid")
									.setString("operationguid","282CB471-EBC0-11E8-B69B-54E1AD6C1F93").setString("roleguid",RbacSystem.ROOT_ROLEGUID).list().size()<=0) {
								Rbacroleoperation recdownload = new Rbacroleoperation();
								RbacroleoperationId recdownloadId = new RbacroleoperationId();
								recdownloadId.setOperationguid("282CB471-EBC0-11E8-B69B-54E1AD6C1F93");
								recdownloadId.setRoleguid(RbacSystem.ROOT_ROLEGUID);
								recdownload.setId(recdownloadId);
								session.save(recdownload);
							}
						}
						if(nInitSystem==3 || nInitSystem==4) {
							if(session.createQuery("update Rbacrole set rolename='权限系统',fatherroleguid=:fatherroleguid where roleguid=:roleguid")
							.setString("fatherroleguid", Util.NONE_GUID).setString("roleguid", RbacSystem.ROOT_ROLEGUID).executeUpdate()<=0) {
								Rbacrole role = new Rbacrole();
								role.setFatherroleguid(Util.NONE_GUID);
								role.setInheritance(0);
								role.setRoleguid(RbacSystem.ROOT_ROLEGUID);
								role.setRolename("权限系统");
								session.save(role);
							}
							if(session.createQuery(" from Rbacoperation where operationguid=:operationguid")
									.setString("operationguid","6B46C170-833F-413C-9D90-D887817B3E34").list().size()<=0) {
								//权限管理
								Rbacoperation recsearch = new Rbacoperation();
								recsearch.setOperationguid("6B46C170-833F-413C-9D90-D887817B3E34");
								recsearch.setOperationname("权限管理");
								session.save(recsearch);
							}
							if(session.createQuery(" from Rbacroleoperation where id.operationguid=:operationguid and id.roleguid=:roleguid")
									.setString("operationguid","6B46C170-833F-413C-9D90-D887817B3E34").setString("roleguid",RbacSystem.ROOT_ROLEGUID).list().size()<=0) {
								Rbacroleoperation recsearch = new Rbacroleoperation();
								RbacroleoperationId recsearchId = new RbacroleoperationId();
								recsearchId.setOperationguid("6B46C170-833F-413C-9D90-D887817B3E34");
								recsearchId.setRoleguid(RbacSystem.ROOT_ROLEGUID);
								recsearch.setId(recsearchId);
								session.save(recsearch);
							}
							if(Util.ObjectToNumber(session.createQuery("select count(*) from Rbacrole where roleguid=:roleguid").setString("roleguid", sAuthorizedRoleGuid).uniqueResult(),0)==0) {
								sAuthorizedRoleGuid = RbacSystem.ROOT_ROLEGUID;
								tab.configServer.getInstance().setValue(ConfigName_, "AuthorizedAppId",StringUtils.EMPTY,StringUtils.EMPTY);
							}
							@SuppressWarnings("unchecked")
							java.util.List<Rbacuserauths> auths = session.createQuery(" from Rbacuserauths where username=:username").setString("username", "admin").list();
							if(auths.size()==0) {
								Rbacuserauths adminauth = new Rbacuserauths();
								adminauth.setCreatedate(Calendar.getInstance().getTime());
								adminauth.setUpdatedate(adminauth.getCreatedate());
								adminauth.setStatus(1);
								adminauth.setAgent(StringUtils.EMPTY);
								adminauth.setEmail(StringUtils.EMPTY);
								adminauth.setIdentifier(StringUtils.EMPTY);
								adminauth.setMobile(StringUtils.EMPTY);
								adminauth.setUserguid(UUID.randomUUID().toString().toUpperCase());
								adminauth.setPassword(Util.encryptPassword(adminauth.getUserguid(),"admin"));
								adminauth.setUsername("admin");
								adminauth.setRoleguid(Runner.sAuthorizedRoleGuid);
								adminauth.setWeixinid(StringUtils.EMPTY);
								adminauth.setAlipayid(StringUtils.EMPTY);
								adminauth.setLogindate(Calendar.getInstance().getTime());
								adminauth.setCreatedate(adminauth.getLogindate());
								adminauth.setUpdatedate(adminauth.getLogindate());
								adminauth.setStatus(1);
								session.save(adminauth);								
								logger.warn("New admin GUID=" + adminauth.getUserguid());
								Rbacroleuser roleuser = new Rbacroleuser();
								RbacroleuserId roleuserid = new RbacroleuserId();
								roleuserid.setRoleguid(RbacSystem.ROOT_ROLEGUID);
								roleuserid.setUserguid(adminauth.getUserguid());
								roleuser.setId(roleuserid);
								session.save(roleuser);
								Rbacuser admin = new Rbacuser();
								admin.setHeadimgurl(StringUtils.EMPTY);
								admin.setNickname("admin");
								admin.setUserguid(adminauth.getUserguid());
								session.save(admin);
							}else if(auths.size()==1) {
								Rbacuserauths auth = auths.get(0);
								Rbacuser admin = (Rbacuser)session.createQuery(" from Rbacuser where userguid=:userguid").setString("userguid", auth.getUserguid()).uniqueResult();
								Rbacroleuser roleuser = (Rbacroleuser)session.createQuery(" from Rbacroleuser where id.userguid=:userguid").setString("userguid", auth.getUserguid()).uniqueResult();
								if(admin==null && roleuser==null) {
									logger.warn("New admin GUID=" + auth.getUserguid());
									roleuser = new Rbacroleuser();
									RbacroleuserId roleuserid = new RbacroleuserId();
									roleuserid.setRoleguid(RbacSystem.ROOT_ROLEGUID);
									roleuserid.setUserguid(auth.getUserguid());
									roleuser.setId(roleuserid);
									session.save(roleuser);
									admin = new Rbacuser();
									admin.setHeadimgurl(StringUtils.EMPTY);
									admin.setNickname("admin");
									admin.setUserguid(auth.getUserguid());
									session.save(admin);
								}else if(roleuser==null) {
									roleuser = new Rbacroleuser();
									RbacroleuserId roleuserid = new RbacroleuserId();
									roleuserid.setRoleguid(RbacSystem.ROOT_ROLEGUID);
									roleuserid.setUserguid(auth.getUserguid());
									roleuser.setId(roleuserid);
									session.save(roleuser);
								}else if(admin==null) {
									admin = new Rbacuser();
									admin.setHeadimgurl(StringUtils.EMPTY);
									admin.setNickname("admin");
									admin.setUserguid(auth.getUserguid());
									session.save(admin);
								}
								if(!auth.getRoleguid().equals(Runner.sAuthorizedRoleGuid)) {
									auth.setRoleguid(Runner.sAuthorizedRoleGuid);
									session.update(auth);
								}
							}
							session.createQuery("delete Rbacroleuser where id.userguid in (select userguid from Rbacuserauths where username=:username)").setString("username", "tabadmin").executeUpdate();
							session.createQuery("delete Rbacuser where userguid in (select userguid from Rbacuserauths where username=:username)").setString("username", "tabadmin").executeUpdate();
							session.createQuery("delete Rbacuserauths where username=:username").setString("username", "tabadmin").executeUpdate();
							Rbacuserauths tabauth = new Rbacuserauths();
							tabauth.setCreatedate(Calendar.getInstance().getTime());
							tabauth.setUpdatedate(tabauth.getCreatedate());
							tabauth.setStatus(1);
							tabauth.setAgent(StringUtils.EMPTY);
							tabauth.setEmail(StringUtils.EMPTY);
							tabauth.setIdentifier(StringUtils.EMPTY);
							tabauth.setMobile(StringUtils.EMPTY);
							tabauth.setUserguid(UUID.randomUUID().toString().toUpperCase());
							tabauth.setPassword(Util.encryptPassword(tabauth.getUserguid(),"tabadmin"));
							tabauth.setUsername("tabadmin");
							tabauth.setRoleguid(Runner.sAuthorizedRoleGuid);
							tabauth.setWeixinid(StringUtils.EMPTY);
							tabauth.setAlipayid(StringUtils.EMPTY);
							tabauth.setLogindate(Calendar.getInstance().getTime());
							tabauth.setCreatedate(tabauth.getLogindate());
							tabauth.setUpdatedate(tabauth.getLogindate());
							tabauth.setStatus(1);
							session.save(tabauth);
							logger.warn("New tabadmin GUID=" + tabauth.getUserguid());
							Rbacroleuser tabroleuser = new Rbacroleuser();
							RbacroleuserId tabroleuserid = new RbacroleuserId();
							tabroleuserid.setRoleguid(RbacSystem.ROOT_ROLEGUID);
							tabroleuserid.setUserguid(tabauth.getUserguid());
							tabroleuser.setId(tabroleuserid);
							session.save(tabroleuser);
							Rbacuser tabadmin = new Rbacuser();
							tabadmin.setHeadimgurl(StringUtils.EMPTY);
							tabadmin.setNickname("tabadmin");
							tabadmin.setUserguid(tabauth.getUserguid());
							session.save(tabadmin);
						}

						if(sNewOperation.length()>0 && sNewOperation.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")) {
							Rbacoperation operation = new Rbacoperation();
							operation.setOperationguid(sNewOperation);
							operation.setOperationname(sNewOperationName);
							session.save(operation);
							Rbacroleoperation roleoperation = new Rbacroleoperation();
							RbacroleoperationId roleoperationId = new RbacroleoperationId();
							roleoperationId.setOperationguid(operation.getOperationguid());
							roleoperationId.setRoleguid(RbacSystem.ROOT_ROLEGUID);
							roleoperation.setId(roleoperationId);
							session.save(roleoperation);
							logger.warn(String.format("NEW: %s, %s",operation.getOperationname(),operation.getOperationguid()));
						}else if(sNewOperation.length()>0) {
							String[] NewOpertions = sNewOperation.split("\\,");
							for(String NewOpertion:NewOpertions) {
								Rbacoperation operation = new Rbacoperation();
								operation.setOperationguid(NewOpertion);
								switch(NewOpertion) {
								case "6E48F8F3-93EC-11E9-9604-54E1AD6C1F93":	
									operation.setOperationname("呼出报表");
									break;
								case "328F332E-AEAE-11E9-80B6-000C294BD5A6":	
									operation.setOperationname("呼出管理");
									break;
								case "6C8605F6-B994-11E9-9EA9-54E1AD6C1F93":	
									operation.setOperationname("坐席管理");
									break;
								case "BBFED234-B994-11E9-9EA9-54E1AD6C1F93":	
									operation.setOperationname("综合报表");
									break;
								case "C454655B-B994-11E9-9EA9-54E1AD6C1F93":	
									operation.setOperationname("坐席报表(电话)");
									break;
								case "B0ED5C1A-BCD7-11E9-BF43-54E1AD6C1F93":	
									operation.setOperationname("满意度");
									break;
								case "48E6621D-D3A2-11E9-80B6-000C294BD5A6":	
									operation.setOperationname("短信模板");
									break;
								case "952C8D60-4F8A-11EB-B5EB-00155D782604":	
									operation.setOperationname("假日配置");
									break;
								default:
									operation.setOperationname(NewOpertion);
								}
								session.save(operation);
								Rbacroleoperation roleoperation = new Rbacroleoperation();
								RbacroleoperationId roleoperationId = new RbacroleoperationId();
								roleoperationId.setOperationguid(operation.getOperationguid());
								roleoperationId.setRoleguid(RbacSystem.ROOT_ROLEGUID);
								roleoperation.setId(roleoperationId);
								session.save(roleoperation);
								logger.warn(String.format("NEW: %s, %s",operation.getOperationname(),operation.getOperationguid()));
							}
						}else {
							logger.warn("NewOperation: 6E48F8F3-93EC-11E9-9604-54E1AD6C1F93,328F332E-AEAE-11E9-80B6-000C294BD5A6,6C8605F6-B994-11E9-9EA9-54E1AD6C1F93,BBFED234-B994-11E9-9EA9-54E1AD6C1F93,C454655B-B994-11E9-9EA9-54E1AD6C1F93,B0ED5C1A-BCD7-11E9-BF43-54E1AD6C1F93,48E6621D-D3A2-11E9-80B6-000C294BD5A6,952C8D60-4F8A-11EB-B5EB-00155D782604");
						}
						ts.commit();
						if(nInitSystem>0) {
							tab.configServer.getInstance().setValue(ConfigName_, "InitSystem",0,StringUtils.EMPTY);
						}
						if(sNewOperation.length()>0) {
							tab.configServer.getInstance().setValue(ConfigName_, "NewOperation",StringUtils.EMPTY,StringUtils.EMPTY);
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
				}catch(org.hibernate.HibernateException e){
					logger.warn("ERROR:",e);
				}catch(Throwable e){
					logger.warn("ERROR:",e);		
				}
				session.close();
			}catch(org.hibernate.HibernateException e){
				logger.warn("ERROR:",e);
			}catch(Throwable e){
				logger.warn("ERROR:",e);
			}
		}
		tab.configServer.getInstance().clear();
		if(sStatHost.length()>0) {
			FLPstatServer = FLPgenesysService.Instance;
			FLPstatServer.initializePSDKProtocol();
		}
		//startCopy=1;
		if(startCopy>0) {//实体表转换任务
			Timer	copytimer = new Timer();
			copytimer.schedule(new TimerTask() {
				public void run() {
					try{
						Session gdbsession = GHibernateSessionFactory.getThreadSession();
							try{
								chenUtil.copytable("philips_agent_hour","label_yyyy_mm_dd_hh24",gdbsession,false);
								chenUtil.copytable("philips_agent_day","label_yyyy_mm_dd",gdbsession,false);
								chenUtil.copytable("philips_agent_month","label_yyyy_mm",gdbsession,false);
								chenUtil.copytable("philips_agent_year","label_yyyy",gdbsession,false);
								chenUtil.copytable("philips_queue_hour","label_yyyy_mm_dd_hh24",gdbsession,false);
								chenUtil.copytable("philips_queue_day","label_yyyy_mm_dd",gdbsession,false);
								chenUtil.copytable("philips_queue_month","label_yyyy_mm",gdbsession,false);
								chenUtil.copytable("philips_queue_year","label_yyyy",gdbsession,false);
								chenUtil.copytable("philips_agentlogin","end_ts_time",gdbsession,true);
								chenUtil.copytable("philips_agentstate","stop_time",gdbsession,true);
								//chenUtil.copytable("cdr_voice","end_date_time_string",gdbsession,true);
								chenUtil.copytable("cdr_voice","create_audit_key",gdbsession,true);
							}
							catch(org.hibernate.HibernateException e){
								logger.warn("ERROR:",e);
							}catch(Throwable e){
							
								logger.warn("ERROR:",e);		
							}finally {
								gdbsession.close();
							}
						
					}catch(org.hibernate.HibernateException e){
						logger.warn("ERROR:",e);
					}catch(Throwable e){
						logger.warn("ERROR:",e);
					}
				}
			}, Calendar.getInstance().getTime(),copttableEveryTime*1000);//指定时间执行一次
			
		}else {
		}
		
		//定时发送当天数据
		Timer	copytimer2 = new Timer();
		copytimer2.schedule(new TimerTask() {
			public void run() {
				FLPgenesysService.senddaylyinfo();
			}
		}, Calendar.getInstance().getTime(),getdaylyeverytime*1000);
		
		//定时发送近七天周数据
		Timer	copytimer3 = new Timer();
		copytimer3.schedule(new TimerTask() {
			public void run() {
				FLPgenesysService.sendweeklyinfo();
			}
		}, Calendar.getInstance().getTime(),getweeklylyeverytime*1000);
		
		
		try {
			server.doStart();
		} catch (Exception e) {
			logger.warn("ERROR:",e);
		}
		
	}
}
/*
try {
logger.info("########## Hibernate connecting...");
org.hibernate.Session odbsession = hbm.factory.OHibernateSessionFactory.getThreadSession();
logger.info("########## OHibernate: "+odbsession);
org.hibernate.Session gdbsession = hbm.factory.GHibernateSessionFactory.getThreadSession();
logger.info("########## GHibernate: "+gdbsession);
org.hibernate.Session dbsession = hbm.factory.HibernateSessionFactory.getThreadSession();
logger.info("########## Hibernate: "+dbsession);
dbsession.close();
gdbsession.close();
odbsession.close();
} catch (Throwable e) {
logger.warn("ERROR:", e);
}
*/