package tab.jetty;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import main.Runner;
import tab.FLPgenesysService;
import tab.util.Util;

public class StatWebSocketServlet extends WebSocketServlet  implements WebSocketListener{
	private static final long serialVersionUID = 1L;
    public static Map<String,Session> userSessionMap=new HashMap<String,Session>();
	public static Log log = LogFactory.getLog(StatWebSocketServlet.class);
	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		synchronized(mapMonitorSession) {
			SessionInfo wsSession = mapMonitorSession.remove(oSessionId);
			log.info("onWebSocketClose, session: " + wsSession.wsSession.getRemoteAddress() + " statusCode:" + statusCode + ", reason:" + reason);
		}
	}

	@Override
	public void onWebSocketConnect(Session session) {
		UpgradeRequest req = session.getUpgradeRequest();
		URI url=req.getRequestURI();
		log.info("输出一下websocket请求url:"+url);
		oSessionId = req.getSession();
		java.util.Map<String, java.util.List<String>> params = req.getParameterMap();
		log.info("onWebSocketConnect: " + params.toString());
		wsSession = new SessionInfo(session);
		String sMethod = Util.FirstOfArray(params.get("Library"));
//		String userguid=Util.FirstOfArray(params.get("userguid"));
//		userSessionMap.put(userguid, session);
		if("GetEvents".equals(sMethod)) {
			log.info("GetEvents");
			session.setIdleTimeout(Runner.nSessionIdleTimeout*1000);
			synchronized(mapMonitorSession) {
				mapMonitorSession.put(req.getSession(),wsSession);
			}
			FLPgenesysService.sendAllInfo(null,null,session);
		}
	}

	@Override
	public void onWebSocketError(Throwable e) {
		log.error(e);
	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		log.debug("onWebSocketBinary: offset="+offset+", len="+len);
	}

	@Override
	public void onWebSocketText(String message) {
		log.info("onWebSocketText: " + message);
	}

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.getPolicy().setIdleTimeout(Runner.nSessionIdleTimeout*1000);
		factory.getPolicy().setMaxTextMessageSize(3000000);
		factory.setCreator(new WebSocketCreator() {
            @Override
            public Object createWebSocket(ServletUpgradeRequest req,
                                          ServletUpgradeResponse res) {
            	if(req.getSubProtocols().size()>0)res.setAcceptedSubProtocol("chat");
           		return new StatWebSocketServlet().setHttpSessionId(req.getHttpServletRequest().getSession().getId());
            }
        });
	}
	private SessionInfo wsSession = null;
	private String sHttpSessionId = null;
	private Object oSessionId = null;
	public StatWebSocketServlet setHttpSessionId(String sessionId) {
		this.sHttpSessionId = sessionId;
		log.info("setHttpSessionId: " + this.sHttpSessionId);
		return this;
	}
	public static class SessionInfo{
		public org.eclipse.jetty.websocket.api.Session wsSession;
		SessionInfo(org.eclipse.jetty.websocket.api.Session session){
			this.wsSession = session;
		}
	}
	private static java.util.Map<Object,SessionInfo> mapMonitorSession = new java.util.HashMap<Object, StatWebSocketServlet.SessionInfo>();
	public static void sendMonitorText(String sJson) {
		log.info(sJson);
		synchronized(mapMonitorSession) {
			for (Iterator<Map.Entry<Object, StatWebSocketServlet.SessionInfo>> it = mapMonitorSession.entrySet().iterator(); it.hasNext();){
			    Entry<Object, SessionInfo> item = it.next();
			    item.getValue().wsSession.getRemote().sendStringByFuture(sJson);
			}
		}
	}
	
}
