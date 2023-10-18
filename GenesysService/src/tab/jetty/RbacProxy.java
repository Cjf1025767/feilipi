package tab.jetty;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.proxy.ProxyServlet;

import tab.rbac.RbacClient;

public class RbacProxy extends ProxyServlet {
	
	/**
	 * 内置反向代理模块，代理给权限系统
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected String rewriteTarget(HttpServletRequest request) {
		String uri =request.getRequestURI();
		if(request.getQueryString() == null){
		    return RbacClient.getRbacUrl() + uri;
		} else {
			return RbacClient.getRbacUrl() + uri + "/?" + request.getQueryString();
		}
    }
}
