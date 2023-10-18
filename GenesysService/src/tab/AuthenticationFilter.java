package tab;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;

import hbm.factory.HibernateSessionFactory;
import hbm.model.Rbacuserauths;
import tab.util.Util;

public class AuthenticationFilter implements ContainerRequestFilter, ContainerResponseFilter
{
	private static Log log = LogFactory.getLog(AuthenticationFilter.class);
	@Context HttpServletRequest request; 
  	@Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException
    {
  		responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
  		responseContext.getHeaders().add("Access-Control-Allow-Headers","CSRF-Token, X-Requested-By, Authorization, Content-Type");
  		responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
  		responseContext.getHeaders().add("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, OPTIONS, HEAD");
 		if(request.getCookies()!=null) {
  			String sCookies = StringUtils.EMPTY;
			for(Cookie cookie :  request.getCookies()) {
				if(sCookies.length()>0)sCookies += ";";
				sCookies += cookie.getName() + "=" + cookie.getValue();
			}
		}else {
			//log.info("############# Miss request.getCookies() Cookies!!!");
		}
  		String sUsername = requestContext.getUriInfo().getQueryParameters().getFirst("username");
  		String sUsernameString=requestContext.getHeaderString("Referer");
  		if(sUsernameString!=null&&sUsernameString.length()>0) {
  			int firstindex=sUsernameString.indexOf("username=");
  	  		int lastindex=sUsernameString.indexOf("%23");
  	  		if(firstindex>0&&lastindex>9) {
  	  			sUsername = sUsernameString.substring(firstindex+9, lastindex);
  	  		}
  		}else {
  			//log.info("sUsernameString为null");
  		}
  		if(!StringUtils.isBlank(sUsername)) {
  			log.info("用户名为:"+sUsername);
	  		String sUserGuid = StringUtils.EMPTY;
			try{
				Session dbsession = HibernateSessionFactory.getThreadSession();
				try{
					Rbacuserauths authuser = (Rbacuserauths)dbsession.createQuery(" from Rbacuserauths where roleguid=:roleid and username=:username")
						.setString("roleid", Util.ROOT_ROLEGUID).setString("username", sUsername).setFirstResult(0).setMaxResults(1).uniqueResult();
					if(authuser!=null) {
						sUserGuid = authuser.getUserguid();
						request.setAttribute("uid", sUserGuid);
						request.setAttribute("oid", Util.ROOT_ROLEGUID);
						HttpSession httpsession = request.getSession();
						if(httpsession!=null) {
							httpsession.setAttribute("uid", sUserGuid);
							httpsession.setAttribute("oid", Util.ROOT_ROLEGUID);
						}else {
							log.info("httpsession是null");
						}
					}else {
						log.info("没有根据用户名查到用户，用户名为:"+sUsername);
					}
				}catch(Throwable e){
					log.warn("ERROR:",e);
				}
				dbsession.close();
			}catch(Throwable e){
				log.warn("ERROR:",e);
			}
  		}else {
  			log.info("sUsername:"+sUsername);
  		}
    }
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		// TODO Auto-generated method stub
  		String sUsername = requestContext.getUriInfo().getQueryParameters().getFirst("username");
  		String sUsernameString=requestContext.getHeaderString("Referer");
  		if(sUsernameString!=null&&sUsernameString.length()>0) {
  			int firstindex=sUsernameString.indexOf("username=");
  	  		int lastindex=sUsernameString.indexOf("%23");
  	  		if(firstindex>0&&lastindex>9) {
  	  			sUsername = sUsernameString.substring(firstindex+9, lastindex);
  	  		}
  		}else {
  			//log.info("sUsernameString为null");
  		}
  		if(!StringUtils.isBlank(sUsername)) {
  			log.info("用户名为:"+sUsername);
	  		String sUserGuid = StringUtils.EMPTY;
			try{
				Session dbsession = HibernateSessionFactory.getThreadSession();
				try{
					Rbacuserauths authuser = (Rbacuserauths)dbsession.createQuery(" from Rbacuserauths where roleguid=:roleid and agent=:username")
						.setString("roleid", Util.ROOT_ROLEGUID).setString("username", sUsername).setFirstResult(0).setMaxResults(1).uniqueResult();
					if(authuser!=null) {
						sUserGuid = authuser.getUserguid();
						request.setAttribute("uid", sUserGuid);
						request.setAttribute("oid", Util.ROOT_ROLEGUID);
					}else {
						log.info("没有根据用户名查到用户，用户名为:"+sUsername);
					}
					HttpSession httpsession = request.getSession();
					if(httpsession!=null) {
						httpsession.setAttribute("uid", sUserGuid);
						httpsession.setAttribute("oid", Util.ROOT_ROLEGUID);
					}else {
						//log.info("httpsession是null");
					}
				}catch(Throwable e){
					log.warn("ERROR:",e);
				}
				dbsession.close();
			}catch(Throwable e){
				log.warn("ERROR:",e);
			}
  		}else {
  			log.info("sUsername:"+sUsername);
  		}
	}

}
