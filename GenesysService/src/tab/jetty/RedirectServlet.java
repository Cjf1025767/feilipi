package tab.jetty;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import tab.rbac.RbacClient;

public class RedirectServlet  extends HttpServlet {
	private static final long serialVersionUID = -6812158548106263531L;
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		doPost(request,response);
	}
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		String uri =request.getRequestURI();
		if(request.getQueryString() == null){
			response.sendRedirect(RbacClient.getRbacUrl() + uri);
		} else {
			response.sendRedirect(RbacClient.getRbacUrl() + uri + "/?" + request.getQueryString());
		}
	}
}
