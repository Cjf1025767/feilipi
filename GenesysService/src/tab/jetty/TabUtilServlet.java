package tab.jetty;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import main.Runner;

public class TabUtilServlet  extends HttpServlet {
	private static final long serialVersionUID = -6812158548106263531L;
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		doPost(request,response);
	}
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		String uri =request.getRequestURI();
		String basPath = StringUtils.EMPTY;
		if(uri.endsWith("GPhoner.js")) {
			basPath = TabUtilServlet.class.getClassLoader().getResource("tab/util/GPhoner.js").getPath().toString();
		}else if(uri.endsWith("FSPhoner.js")) {
			basPath = TabUtilServlet.class.getClassLoader().getResource("tab/util/FSPhoner.js").getPath().toString();
		}else if(uri.endsWith("TabPhoner.js")) {
			basPath = TabUtilServlet.class.getClassLoader().getResource("tab/util/TabPhoner.js").getPath().toString();
		}
		// 读到流中
        InputStream inStream = new FileInputStream(basPath);
        int length = inStream.available();
        byte bytes[] = new byte[length];
        inStream.read(bytes);
        inStream.close();
        // 设置输出的格式
        response.reset();
        response.setContentType("application/javascript");
        String strBuffer =new String(bytes, StandardCharsets.UTF_8);
        strBuffer = strBuffer.replaceFirst("\\{BASEURI\\}", Runner.PhonerBaseUri);
        strBuffer = strBuffer.replaceFirst("\\{WSURI\\}", Runner.PhonerWsUri);
        strBuffer = strBuffer.replaceFirst("\\{ACWTIME\\}", tab.util.Util.ObjectToString(Runner.PhonerAcwTime));
        strBuffer = strBuffer.replaceFirst("\\{AUTOREADYTIME\\}", tab.util.Util.ObjectToString(Runner.PhonerAutoReadyTime));
        
	    response.addHeader("Content-Disposition", "attachment; filename=MyPhoner.js");
        try {
                response.getOutputStream().write(strBuffer.getBytes(), 0, strBuffer.getBytes().length);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
