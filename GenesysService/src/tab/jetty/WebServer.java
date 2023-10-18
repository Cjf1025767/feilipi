package tab.jetty;

import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.DatabaseAdaptor;
import org.eclipse.jetty.server.session.DefaultSessionCacheFactory;
import org.eclipse.jetty.server.session.HouseKeeper;
import org.eclipse.jetty.server.session.JDBCSessionDataStore;
import org.eclipse.jetty.server.session.JDBCSessionDataStore.SessionTableSchema;
import org.eclipse.jetty.server.session.JDBCSessionDataStoreFactory;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.server.ResourceConfig;

public class WebServer {
public static Log logger = LogFactory.getLog(WebServer.class);
private Server server_;
private long nPort_ = 0;
private long nSecurePort_ = nPort_;
private long AcceptQueueSize_ = 50;
private int SessionIdleTimeout_ = 0;
private String webRootContextPath_ = "";
private String webRoot_ = "./WebRoot";
private long RequestHeaderSize_ = 0;
private JDBCSessionDataStoreFactory storeFactory_ = null;
private DefaultSessionCacheFactory cacheFactory_ = null;
private ContextHandlerCollection contexts_ = new ContextHandlerCollection();
private java.util.List<Handler> handlers_ = new java.util.ArrayList<Handler>();

public WebServer() {
	server_ = new Server(createThreadPool());
}
public void setPort(long nPort){
	nPort_ = nPort;
}
public long getPort(){
	return nPort_;
}
public void setSslPort(long nPort){
	nSecurePort_ = nPort;
}
public long getSslPort() {
	return nSecurePort_;
}
public void setWebRoot(String sWebRoot,String sContextPath){
	webRootContextPath_ = sContextPath;
	webRoot_ = sWebRoot;
}
public String getWebRoot()
{
	return webRoot_;
}

public void setDriverInfo(String sSessionNodeName, String sDriverClassName, String sConnectionUrl)
{
	org.eclipse.jetty.server.session.DefaultSessionIdManager idManager = 
			new org.eclipse.jetty.server.session.DefaultSessionIdManager(server_);
	idManager.setWorkerName(sSessionNodeName);//必须唯一
	HouseKeeper houseKeeper =  new HouseKeeper();
	houseKeeper.setSessionIdManager(idManager);
	try {
		logger.warn("default IntervalSec:"+houseKeeper.getIntervalSec());
		houseKeeper.setIntervalSec(1);
	} catch (Exception e) {
		logger.error("ERROR: ",e);
		return;
	}
	idManager.setSessionHouseKeeper(houseKeeper);
	server_.setSessionIdManager(idManager);
	idManager.setServer(server_);

	DatabaseAdaptor da = new DatabaseAdaptor();
    da.setDriverInfo(sDriverClassName, sConnectionUrl);
    storeFactory_ = new JDBCSessionDataStoreFactory();
    storeFactory_.setDatabaseAdaptor(da);
    
    SessionTableSchema sessionTableSchema = new JDBCSessionDataStore.SessionTableSchema();
	storeFactory_.setSessionTableSchema(sessionTableSchema);
	logger.debug("default SchemaName:"+sessionTableSchema.getSchemaName());
	logger.debug("default TableName:"+sessionTableSchema.getTableName());
	logger.debug("	default IdColumn:"+sessionTableSchema.getIdColumn());
	logger.debug("	default AccessTime:"+sessionTableSchema.getAccessTimeColumn());
	logger.debug("	default ContextPath:"+sessionTableSchema.getContextPathColumn());
	logger.debug("	default CookieTime:"+sessionTableSchema.getCookieTimeColumn());
	logger.debug("	default CreateTime:"+sessionTableSchema.getCreateTimeColumn());
	logger.debug("	default ExpiryTime:"+sessionTableSchema.getExpiryTimeColumn());
	logger.debug("	default LastAccessTime:"+sessionTableSchema.getLastAccessTimeColumn());
	logger.debug("	default LastNode:"+sessionTableSchema.getLastNodeColumn());
	logger.debug("	default LastSavedTime:"+sessionTableSchema.getLastSavedTimeColumn());
	logger.debug("	default Map:"+sessionTableSchema.getMapColumn());
	logger.debug("	default MaxInterval:"+sessionTableSchema.getMaxIntervalColumn());
	cacheFactory_ = new DefaultSessionCacheFactory();
}
public void setIdleTimeout(int IdleTimeout){
	SessionIdleTimeout_ = IdleTimeout;
}
public void setAcceptQueueSize(long AcceptQueueSize){
	AcceptQueueSize_ = AcceptQueueSize;
}
public void setRequestHeaderSize(long RequestHeaderSize){
	RequestHeaderSize_ = RequestHeaderSize;
}

public void addStaticResource(String sResourcePath,String sContextPath)
{
	ContextHandler docContext = new ContextHandler(sContextPath);
	ResourceHandler resourceHandler = new ResourceHandler();
	resourceHandler.setDirectoriesListed(false);
	resourceHandler.setResourceBase(sResourcePath);
	docContext.setHandler(resourceHandler);
	handlers_.add(docContext);
}

public void addHttpServlet(HttpServlet servlet, String sContextPath,boolean bUsingCookies)
{
	ServletHolder wxchatHolder = new ServletHolder(servlet);
	ServletContextHandler context = new ServletContextHandler(contexts_,sContextPath,ServletContextHandler.SESSIONS);
	context.addServlet(wxchatHolder, "/");
	context.addServlet(wxchatHolder, "/*");
	SessionHandler sessionHandler = context.getSessionHandler();
	if(SessionIdleTimeout_>0){
		sessionHandler.setMaxInactiveInterval(SessionIdleTimeout_);
	}
	sessionHandler.setUsingCookies(bUsingCookies);
	if(bUsingCookies) {
		sessionHandler.setHttpOnly(true);
		sessionHandler.setSecureRequestOnly(true);
	}
	if(cacheFactory_!=null && storeFactory_!=null) {
		SessionCache cache = cacheFactory_.getSessionCache(sessionHandler);
	    SessionDataStore store = storeFactory_.getSessionDataStore(sessionHandler);
	    cache.setSessionDataStore(store);
	    sessionHandler.setSessionCache(cache);
	}
	handlers_.add(context);
}

public void addWebSocketServlet(HttpServlet servlet, String sContextPath)
{
	/*jetty 11
	Servlet websocketServlet = new JettyWebSocketServlet() {
        @Override protected void configure(JettyWebSocketServletFactory factory) {
            factory.addMapping(sContextPath, (req, res) -> servlet);
        }
    };
    ServletHolder wxchatHolder = new ServletHolder(websocketServlet);
	ServletContextHandler context = new ServletContextHandler(contexts_,sContextPath,ServletContextHandler.SESSIONS);
	context.addServlet(wxchatHolder, "/");
	context.addServlet(wxchatHolder, "/*");
    JettyWebSocketServletContainerInitializer.configure(context, null);
    };*/
	ServletHolder wxchatHolder = new ServletHolder(servlet);
	ServletContextHandler context = new ServletContextHandler(contexts_,sContextPath,ServletContextHandler.SESSIONS);
	context.addServlet(wxchatHolder, "/");
	context.addServlet(wxchatHolder, "/*");
	WebSocketServerContainerInitializer.configure(context, null);
	SessionHandler sessionHandler = context.getSessionHandler();
	if(SessionIdleTimeout_>0){
		sessionHandler.setMaxInactiveInterval(SessionIdleTimeout_);
	}
	if(cacheFactory_!=null && storeFactory_!=null) {
		SessionCache cache = cacheFactory_.getSessionCache(sessionHandler);
	    SessionDataStore store = storeFactory_.getSessionDataStore(sessionHandler);
	    cache.setSessionDataStore(store);
	    sessionHandler.setSessionCache(cache);
	}
	handlers_.add(context);
}
	
public void addRestConfig(ResourceConfig config, String sContextPath,boolean bUsingCookies)
{
	//方法1
	//final ResourceConfig application = new ResourceConfig()
    //.packages("tab.api.rest")
    //.register(com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider.class);
	//ServletHolder restHolder = new ServletHolder(new org.glassfish.jersey.servlet.ServletContainer(application));
	
	//方法2
	//ServletHolder restHolder = new ServletHolder(new org.glassfish.jersey.servlet.ServletContainer());
	//restHolder.setInitParameter("jersey.config.server.provider.packages","tab.api.rest");//,com.fasterxml.jackson.jaxrs.json");
	//restHolder.setInitParameter("jersey.config.server.provider.scanning.recursive","false");
	
	//方法3
	//ServletHolder restHolder = new ServletHolder(new org.glassfish.jersey.servlet.ServletContainer());
	//restHolder.setInitParameter("javax.ws.rs.Application",JerseyResourceConfigClassName_);
	
	ServletHolder restHolder = new ServletHolder(new org.glassfish.jersey.servlet.ServletContainer(config));
	restHolder.setInitOrder(1);
	restHolder.setClassName(org.glassfish.jersey.servlet.ServletContainer.class.getName());
	restHolder.setName("RESTful WebService for "+sContextPath);

	ServletContextHandler restContext = new ServletContextHandler(contexts_, sContextPath, ServletContextHandler.SESSIONS );
	restContext.addServlet(DefaultServlet.class, "/");
	restContext.addServlet(restHolder, "/*");
	SessionHandler sessionHandler = restContext.getSessionHandler();
	if(SessionIdleTimeout_>0){
		sessionHandler.setMaxInactiveInterval(SessionIdleTimeout_);
	}
	sessionHandler.setUsingCookies(bUsingCookies);
	if(bUsingCookies) {
		sessionHandler.setHttpOnly(true);
		sessionHandler.setSecureRequestOnly(true);
	}
	if(cacheFactory_!=null && storeFactory_!=null) {
		SessionCache cache = cacheFactory_.getSessionCache(sessionHandler);
	    SessionDataStore store = storeFactory_.getSessionDataStore(sessionHandler);
	    cache.setSessionDataStore(store);
	    sessionHandler.setSessionCache(cache);
	}
	handlers_.add(restContext);
}

public void doStart() throws Exception
{
	//根据设置的端口，自动开放对应服务
	if(nPort_==0 && nSecurePort_==0)throw new Exception("port error!");
	if(nPort_==nSecurePort_){
		server_.setConnectors(new Connector[]{createSslConnector()});
	}else if(nPort_==0){
		server_.setConnectors(new Connector[]{createSslConnector()});
	}else if(nSecurePort_==0){
		server_.setConnectors(new Connector[]{createConnector()});
	}else if(nPort_!=nSecurePort_){
		server_.setConnectors(new Connector[]{createConnector(),createSslConnector()});
	}
	
	//static resource context
	ContextHandler docContext = new ContextHandler(webRootContextPath_);
	ResourceHandler resourceHandler = new ResourceHandler();
	resourceHandler.setDirectoriesListed(false);
	resourceHandler.setWelcomeFiles(new String[]{"login.html"});
	resourceHandler.setResourceBase(webRoot_);
	MimeTypes mimetype = new MimeTypes();
	mimetype.addMimeMapping("ttf", "application/x-font-truetype");
	resourceHandler.setMimeTypes(mimetype);
	docContext.setHandler(resourceHandler);
	handlers_.add(docContext);

	Handler[] handlers = new Handler[handlers_.size()];
	contexts_.setHandlers(handlers_.toArray(handlers));
	
	org.eclipse.jetty.server.handler.gzip.GzipHandler gzipHandler = new org.eclipse.jetty.server.handler.gzip.GzipHandler();
	gzipHandler.addIncludedMimeTypes("text/html,text/plain,text/xml,application/xhtml+xml,text/css,application/javascript,application/json,image/svg+xml");
	gzipHandler.setMinGzipSize(1024);
	gzipHandler.setHandler(contexts_);
	server_.setHandler(gzipHandler);
	    
	server_.start();
	server_.join();
}
private ThreadPool createThreadPool()
{
	QueuedThreadPool threadPool = new QueuedThreadPool();
	threadPool.setMinThreads(100);
	return threadPool;
}
private NetworkConnector createConnector()
{
	HttpConfiguration configuration = new HttpConfiguration();
	if(RequestHeaderSize_>0){
		configuration.setRequestHeaderSize((int)RequestHeaderSize_);
	}
	ServerConnector connector = new ServerConnector(server_,new HttpConnectionFactory(configuration));
	connector.setPort((int)nPort_);
	connector.setAcceptQueueSize((int)AcceptQueueSize_);
	return connector;
}

private NetworkConnector createSslConnector()
{
	HttpConfiguration configuration = new HttpConfiguration();
	if(RequestHeaderSize_>0){
		configuration.setRequestHeaderSize((int)RequestHeaderSize_);
	}
	configuration.setSecureScheme("https");
	configuration.setSecurePort((int)nSecurePort_);
	configuration.addCustomizer(new SecureRequestCustomizer());
	
	@SuppressWarnings("deprecation")
	SslContextFactory sslContextFactory = new SslContextFactory();
    sslContextFactory.setKeyStorePath(new java.io.File(webRoot_+"/keystore/keystore.jks").getAbsolutePath());
    sslContextFactory.setKeyStorePassword("OBF:1vgj1xtb1v9i1v941xu71vgb");
    sslContextFactory.setKeyManagerPassword("OBF:1vgj1xtb1v9i1v941xu71vgb");
    
	ServerConnector connector = new ServerConnector(server_, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(configuration));
	connector.setPort((int)nSecurePort_);
	connector.setAcceptQueueSize((int)AcceptQueueSize_);
	return connector;
}
}
