package tab.rbac;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.http.ProtocolType;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AliyunToken {
public static Log logger = LogFactory.getLog(AliyunToken.class);
private static ObjectMapper mapper = new ObjectMapper();
private static AliyunToken  uniqueInstance = null;
public static AliyunToken getInstance() {
	synchronized(mapper) {
		if (uniqueInstance == null) {
			uniqueInstance = new AliyunToken();
		}
		return uniqueInstance;
	}
}
//地域ID
private static final String REGIONID = "cn-shanghai";
// 获取Token服务域名
private static final String DOMAIN = "nls-meta.cn-shanghai.aliyuncs.com";
// API版本
private static final String API_VERSION = "2019-02-28";
// API名称
private static final String REQUEST_ACTION = "CreateToken";
private Long expiresIn = (long)0;
private String accessToken = "";
public void setExpiresIn(Long expiresIn) {
	this.expiresIn = expiresIn;
    logger.info("获取到的Token： " + accessToken + "，有效期时间戳(单位：秒): " + expiresIn);
    String expireDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(getInstance().expiresIn * 1000));
    logger.info("Token有效期的北京时间：" + expireDate);
}
public void setAccessToken(String accessToken) {
	this.accessToken = accessToken;
}
public String getAccessToken() {
	synchronized(mapper) {
		if((System.currentTimeMillis() / 1000)-expiresIn>0) {
			// 创建DefaultAcsClient实例并初始化
	        DefaultProfile profile = DefaultProfile.getProfile(
	                "cn-shanghai",
	                main.Runner.AliyunAccessKeyId, main.Runner.AliyunAccessKeySecret);

	        IAcsClient client = new DefaultAcsClient(profile);
	        CommonRequest request = new CommonRequest();

	        CommonResponse response;
			try {
				request.setDomain(DOMAIN);
		        request.setVersion(API_VERSION);
		        request.setAction(REQUEST_ACTION);
		        request.setMethod(MethodType.POST);
		        request.setProtocol(ProtocolType.HTTPS);
		        
				response = client.getCommonResponse(request);
		        System.out.println(response.getData());
		        if (response.getHttpStatus() == 200) {
		        	java.util.Map<String, Object> data = mapper.readValue(response.getData(), new TypeReference<java.util.Map<String, Object>>(){});
		        	@SuppressWarnings("unchecked")
					java.util.Map<String, Object> token = (java.util.Map<String, Object>)data.get("Token");
		            accessToken = tab.util.Util.ObjectToString(token.get("Id"));
		            tab.configServer.getInstance().setValue(main.Runner.ConfigName_, "AliyunAccessToken",accessToken,"");
		            expiresIn = tab.util.Util.ObjectToNumber(token.get("ExpireTime"),0L)  - 3*60*60;
		            tab.configServer.getInstance().setValue(main.Runner.ConfigName_, "AliyunExpiresIn",expiresIn.toString(),"");
		            logger.info("获取到的Token： " + accessToken + "，有效期时间戳(单位：秒): " + expiresIn);
		            // 将10位数的时间戳转换为北京时间
		            String expireDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(getInstance().expiresIn * 1000));
		            logger.info("Token有效期的北京时间：" + expireDate);
		        }
	        else {
	        	logger.error("获取Token失败！");
	        	accessToken = "";
	        }
			} catch (ClientException | IOException | ClassNotFoundException e) {
				logger.error("ERROR:",e);
				accessToken = "";
			}
		}
	}
	return accessToken;
}
}
