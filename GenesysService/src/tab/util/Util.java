package tab.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;

import main.Runner;
import tab.configServer.ValueString;

public class Util {
	public static Log logger = LogFactory.getLog(Util.class);
	public static final String NONE_GUID = "00000000-0000-0000-0000-000000000000";
	public static final String ROOT_ROLEGUID = "9A611B6F-5664-4C43-9D06-C1E2141CCCB1";
	public static final String METRICS_PATH = "/metrics";
	public static boolean isUriValid(String url)
    {
        try {
            new java.net.URL(url).toURI();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
	public static String getStackTrace(Throwable e) {
	    StringBuilder sb = new StringBuilder();
	    for (StackTraceElement element : e.getStackTrace()) {
	        sb.append(element.toString());
	        sb.append("|");
	    }
	    return sb.toString();
	}
	public static Short ObjectToNumber(Object obj,Short nDefault) {
		if(obj==null)return nDefault;
		try {
			return Short.parseShort(String.valueOf(obj));
		}catch(java.lang.NumberFormatException e){
			return nDefault;
		}
	}
	public static Integer ObjectToNumber(Object obj,Integer nDefault) {
		if(obj==null)return nDefault;
		try {
			return Integer.parseInt(String.valueOf(obj));
		}catch(java.lang.NumberFormatException e){
			return nDefault;
		}
	}
	public static Long ObjectToNumber(Object obj,Long nDefault) {
		if(obj==null)return nDefault;
		try {
			return Long.parseLong(String.valueOf(obj));
		}catch(java.lang.NumberFormatException e){
			return nDefault;
		}
	}
	public static Double ObjectToNumber(Object obj,Double nDefault) {
		if(obj==null)return nDefault;
		try {
			return Double.parseDouble(String.valueOf(obj));
		}catch(java.lang.NumberFormatException e){
			return nDefault;
		}
	}
	public static boolean ObjectToBoolean(Object obj) {
		if(obj==null)return false;
		try {
			return Boolean.parseBoolean(String.valueOf(obj));
		}catch(java.lang.NumberFormatException e){
			return false;
		}
	}
	public static String ObjectToString(Object obj) {
		if(obj==null)return "";
		return String.valueOf(obj);
	}
	public static String FirstOfArray(java.util.List<String> objs) {
		return objs != null && objs.size() > 0 ? String.valueOf(objs.get(0)) : "";
	}

    public static String compressUUID(String sUUID) {  
    	UUID uuid = UUID.fromString(sUUID);
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        //java 8
        return (java.util.Base64.getEncoder().encodeToString(bb.array())).replaceAll("\\+", "-").replaceAll("/", "_").replaceAll("=", "");
        //java 6
        //return (javax.xml.bind.DatatypeConverter.printBase64Binary(bb.array())).replaceAll("\\+", "-").replaceAll("/", "_").replaceAll("=", "");
    }  
    public static String uncompressUUID(String sUUID) {
    	sUUID = sUUID.replaceAll("-", "+").replaceAll("_", "/");
		int nMod = sUUID.length()%4;
		if(nMod>0) {
			sUUID += "====".substring(nMod,4);
		}
    	ByteBuffer bb;
    	long high = 0,low = 0;
		try {
			//java 8
			bb = ByteBuffer.wrap(java.util.Base64.getDecoder().decode(sUUID));
			//java 6
			//bb = ByteBuffer.wrap(javax.xml.bind.DatatypeConverter.parseBase64Binary(sUUID));
	        high = bb.getLong();
	        low = bb.getLong();
		}catch(java.nio.BufferUnderflowException e) {
			return "";
		}
        UUID uuid = new UUID(high, low);
        return uuid.toString().toUpperCase();  
    }
    static final byte[] HEX_CHAR_TABLE = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	public static String getHexString(byte[] raw) throws UnsupportedEncodingException {
		byte[] hex = new byte[2 * raw.length];
		int index = 0;
		for (byte b : raw) {
			int v = b & 0xFF;
			hex[index++] = HEX_CHAR_TABLE[v >>> 4];
			hex[index++] = HEX_CHAR_TABLE[v & 0xF];
		}
		return new String(hex, "ASCII");
	}
	public static String getImageBase64(String sImagePath)
	{
		try{
			InputStream inputStream = null;
	        byte[] data = null;
            inputStream = new FileInputStream(sImagePath);
            data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            int nPos = sImagePath.lastIndexOf('.');
			if(nPos>-1){
				return "data:image/"+sImagePath.substring(nPos+1,Math.min(nPos+8,sImagePath.length())).toLowerCase()+";base64,"+java.util.Base64.getEncoder().encodeToString(data);
			}
		}catch(Throwable e){
			logger.error("ERROR:",e);		
		}
		return "";
	}
	
	private static SSLConnectionSocketFactory getSslConnectionSocketFactory(){
        TrustStrategy acceptingTrustStrategy = (x509Certificates, s) -> true;
        SSLContext sslContext = null;
		try {
			sslContext = org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			logger.error("ERROR:",e);
			sslContext = null;
		}
		if(sslContext==null)return null;
        return new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
    }

	public static synchronized CloseableHttpClient getIdleHttpClient(boolean bSsl) {
		if(bSsl) {
			HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
			SSLConnectionSocketFactory factory = getSslConnectionSocketFactory();
			if(factory==null)return HttpClients.createDefault();
	        return httpClientBuilder.setSSLSocketFactory(factory).build();
		}
		return HttpClients.createDefault();
	}
	public static int get(String url, ValueString responseContent)
    {
		boolean bSsl = StringUtils.startsWithIgnoreCase(url, "https://") ? true : false;
    	int nStatusCode = 500;
		CloseableHttpClient client = getIdleHttpClient(bSsl);
		if(client==null)return nStatusCode;
	    try
		{
			HttpGet get = new HttpGet(url);
			HttpResponse res = client.execute(get);
			HttpEntity entity = res.getEntity();
			try {
				responseContent.value = EntityUtils.toString(entity, org.apache.http.Consts.UTF_8);
				nStatusCode = res.getStatusLine().getStatusCode();
			} catch (ParseException e) {
				logger.error("ERROR:",e);
			} catch (IOException e) {
				logger.error("ERROR:",e);
			}
		}catch(IOException e){
			logger.error("ERROR:",e);
		}
    	return nStatusCode;
    }
	
	public static int QRCodeBase64Post(String url, java.util.Map<String, Object> postEntity, tab.configServer.ValueString vs, boolean bSsl,int nMinSize)
    {
    	int nStatusCode = 500;
		CloseableHttpClient client = getIdleHttpClient(bSsl);
		if(client==null)return nStatusCode;
		try {
			logger.info("INFO:"+url);
			HttpPost httpPost = new HttpPost(url);
			logger.info("INFO:"+postEntity);
			if(postEntity!=null){
				StringEntity sEntity = new StringEntity((new ObjectMapper()).writeValueAsString(postEntity), org.apache.http.Consts.UTF_8);
		        httpPost.setEntity(sEntity);
			}
			httpPost.setHeader("Content-type", "application/json");
			HttpResponse res = client.execute(httpPost);
			HttpEntity entity = res.getEntity();
			java.io.BufferedInputStream fif = new java.io.BufferedInputStream( entity.getContent());
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			int data;
			int len = 0;
			while((data = fif.read()) != -1)
			{
				len++;
				buffer.write(data);
			}
            vs.value = "data:image/png;base64,"+java.util.Base64.getEncoder().encodeToString(buffer.toByteArray());
			//图片太小，说明错误了
			nStatusCode = len<nMinSize ? nStatusCode : res.getStatusLine().getStatusCode();
			if(nStatusCode>=300) {
				logger.error("QRCode: " + new String(buffer.toByteArray(), "utf-8"));
			}
		}catch(ClientProtocolException e){
			logger.error("ERROR:",e);
		} catch (IOException e) {
			logger.error("ERROR:",e);
		}
    	return nStatusCode;
    }
	public static int post(String url, String sJson, tab.configServer.ValueString responseContent)
    {
    	int nStatusCode = 500;
    	boolean bSsl = StringUtils.startsWithIgnoreCase(url, "https://") ? true : false;
    	CloseableHttpClient client = getIdleHttpClient(bSsl);
		if(client==null)return nStatusCode;
	    try
		{
			HttpPost httpPost = new HttpPost(url);
			StringEntity sEntity = new StringEntity(sJson, org.apache.http.Consts.UTF_8);
			sEntity.setContentType("application/json");
	        httpPost.setEntity(sEntity);
	        httpPost.setHeader("Content-type", "application/json");
			HttpResponse res = client.execute(httpPost);
			HttpEntity entity = res.getEntity();
			try {
				responseContent.value = EntityUtils.toString(entity, org.apache.http.Consts.UTF_8);
				nStatusCode = res.getStatusLine().getStatusCode();
			} catch (ParseException e) {
				logger.error("ERROR:",e);
			} catch (IOException e) {
				logger.error("ERROR:",e);
			}
		}catch(IOException e){
			logger.error("ERROR:",e);
		}
    	return nStatusCode;
    }
	public static int post(String url, MultipartEntityBuilder multipartEntity, tab.configServer.ValueString responseContent)
    {
    	int nStatusCode = 500;
    	boolean bSsl = StringUtils.startsWithIgnoreCase(url, "https://") ? true : false;
    	CloseableHttpClient client = getIdleHttpClient(bSsl);
		if(client==null)return nStatusCode;
	    try
		{
			HttpPost httpPost = new HttpPost(url);
			httpPost.setEntity(multipartEntity.build());
			httpPost.setHeader("Content-type", "multipart/form-data");
			HttpResponse res = client.execute(httpPost);
			HttpEntity entity = res.getEntity();
			try {
				responseContent.value = EntityUtils.toString(entity, org.apache.http.Consts.UTF_8);
				nStatusCode = res.getStatusLine().getStatusCode();
			} catch (ParseException e) {
				logger.error("ERROR:",e);
			} catch (IOException e) {
				logger.error("ERROR:",e);
			}
		}catch(IOException e){
			logger.error("ERROR:",e);
		}
    	return nStatusCode;
    }
	public static int post(String url, java.util.Map<String, Object> mapEntity, tab.configServer.ValueString responseContent)
    {
    	int nStatusCode = 500;
    	boolean bSsl = StringUtils.startsWithIgnoreCase(url, "https://") ? true : false;
    	CloseableHttpClient client = getIdleHttpClient(bSsl);
		if(client==null)return nStatusCode;
	    try
		{
			HttpPost httpPost = new HttpPost(url);
			java.util.List<NameValuePair> nvps = new java.util.ArrayList<NameValuePair>();
			if(mapEntity!=null){
				for (java.util.Map.Entry<String, Object> entry : mapEntity.entrySet()) {
					if(!(entry==null || entry.getKey()==null || entry.getValue()==null)) {
						if(entry.getValue().getClass().getName().equals("java.util.ArrayList")) {
							@SuppressWarnings("rawtypes")
							java.util.ArrayList al = (java.util.ArrayList) entry.getValue();
							for(int i=0;i<al.size();i++) {
								nvps.add(new BasicNameValuePair(entry.getKey().toString(), al.get(i).toString()));
							}
						}else {
							nvps.add(new BasicNameValuePair(entry.getKey().toString(), entry.getValue().toString()));
						}
					}
				}
			}
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, org.apache.http.Consts.UTF_8));
			httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
			HttpResponse res = client.execute(httpPost);
			HttpEntity entity = res.getEntity();
			try {
				responseContent.value = EntityUtils.toString(entity, org.apache.http.Consts.UTF_8);
				nStatusCode = res.getStatusLine().getStatusCode();
			} catch (ParseException e) {
				logger.error("ERROR:",e);
			} catch (IOException e) {
				logger.error("ERROR:",e);
			}
		}catch(org.apache.http.conn.HttpHostConnectException e) {
			logger.debug("DEBUG:",e);
		}catch(IOException e){
			logger.error("ERROR:",e);
		}
    	return nStatusCode;
    }
	public static class PasswordAlgorithm{
		public int retry = 5;
		public int difok = 5;
		public int minlen = 8;
		public int dcredit = -1;
		public int ucredit = -1;
		public int lcredit = -1;
		public int ocredit = 0;
		public boolean reject_username = false;
		public boolean gecoscheck = false;
		public int nAccountLockTime = 60;
		public int nAccountLockIdleTime = 0;
		public int nAccountChangeTime = 0;
		public boolean bAccountSingleLogin = false;
	}
	public static PasswordAlgorithm passwordAlgorithm = new PasswordAlgorithm();
	public static void LoadPasswordAlgorithm(String sAlgorithm) {
		/*
		authtok_type=XXX
		The default action is for the module to use the following prompts when requesting passwords: "New UNIX password: " and "Retype UNIX password: ". The example word UNIX can be replaced with this option, by default it is empty.
		retry=N
		Prompt user at most N times before returning with error. The default is 1.
		difok=N
		This argument will change the default of 5 for the number of character changes in the new password that differentiate it from the old password.
		minlen=N
		The minimum acceptable size for the new password (plus one if credits are not disabled which is the default). In addition to the number of characters in the new password, credit (of +1 in length) is given for each different kind of character (other, upper, lower and digit). The default for this parameter is 9 which is good for a old style UNIX password all of the same type of character but may be too low to exploit the added security of a md5 system. Note that there is a pair of length limits in Cracklib itself, a "way too short" limit of 4 which is hard coded in and a defined limit (6) that will be checked without reference to minlen. If you want to allow passwords as short as 5 characters you should not use this module.
		dcredit=N
		(N >= 0) This is the maximum credit for having digits in the new password. If you have less than or N digits, each digit will count +1 towards meeting the current minlen value. The default for dcredit is 1 which is the recommended value for minlen less than 10.
		(N < 0) This is the minimum number of digits that must be met for a new password.
		
		ucredit=N
		(N >= 0) This is the maximum credit for having upper case letters in the new password. If you have less than or N upper case letters each letter will count +1 towards meeting the current minlen value. The default for ucredit is 1 which is the recommended value for minlen less than 10.
		(N < 0) This is the minimum number of upper case letters that must be met for a new password.
		
		lcredit=N
		(N >= 0) This is the maximum credit for having lower case letters in the new password. If you have less than or N lower case letters, each letter will count +1 towards meeting the current minlen value. The default for lcredit is 1 which is the recommended value for minlen less than 10.
		(N < 0) This is the minimum number of lower case letters that must be met for a new password.
		
		ocredit=N
		(N >= 0) This is the maximum credit for having other characters in the new password. If you have less than or N other characters, each character will count +1 towards meeting the current minlen value. The default for ocredit is 1 which is the recommended value for minlen less than 10.
		(N < 0) This is the minimum number of other characters that must be met for a new password.
		
		minclass=N
		The minimum number of required classes of characters for the new password. The default number is zero. The four classes are digits, upper and lower letters and other characters. The difference to the credit check is that a specific class if of characters is not required. Instead N out of four of the classes are required.
		maxrepeat=N
		Reject passwords which contain more than N same consecutive characters. The default is 0 which means that this check is disabled.
		maxsequence=N
		Reject passwords which contain monotonic character sequences longer than N. The default is 0 which means that this check is disabled. Examples of such sequence are '12345' or 'fedcb'. Note that most such passwords will not pass the simplicity check unless the sequence is only a minor part of the password.
		maxclassrepeat=N
		Reject passwords which contain more than N consecutive characters of the same class. The default is 0 which means that this check is disabled.
		reject_username
		Check whether the name of the user in straight or reversed form is contained in the new password. If it is found the new password is rejected.
		gecoscheck
		Check whether the words from the GECOS field (usualy full name of the user) longer than 3 characters in straight or reversed form are contained in the new password. If any such word is found the new password is rejected.
		enforce_for_root
		The module will return error on failed check also if the user changing the password is root. This option is off by default which means that just the message about the failed check is printed but root can change the password anyway.
		use_authtok
		This argument is used to force the module to not prompt the user for a new password but use the one provided by the previously stacked password module.
		dictpath=/path/to/dict
		Path to the cracklib dictionaries.
		 */
		String[] sOnes =sAlgorithm.split(" "); 
		for(String algorithm: sOnes) {
			String[] values = algorithm.split("=");
			if("retry".equals(values[0]))passwordAlgorithm.retry = ObjectToNumber(values[1],5);//重试5次锁定
			else if("difok".equals(values[0]))passwordAlgorithm.difok = ObjectToNumber(values[1],5);//不能重复最近5次密码时，密码相同判定最少重复字符个数（策略和cracklib不同）
			else if("minlen".equals(values[0]))passwordAlgorithm.minlen = ObjectToNumber(values[1],8);
			else if("dcredit".equals(values[0]))passwordAlgorithm.dcredit = ObjectToNumber(values[1],-1);//最小1个数字
			else if("ucredit".equals(values[0]))passwordAlgorithm.ucredit = ObjectToNumber(values[1],-1);//最小1个大写字母
			else if("lcredit".equals(values[0]))passwordAlgorithm.lcredit = ObjectToNumber(values[1],-1);//最小1个小写字母
			else if("ocredit".equals(values[0]))passwordAlgorithm.ocredit = ObjectToNumber(values[1],0);//标点
			else if("reject_username".equals(values[0]))passwordAlgorithm.reject_username = true;//拒绝包含用户名
			else if("gecoscheck".equals(values[0]))passwordAlgorithm.gecoscheck = true;//拒绝常用名字
		}
	}
	public static boolean checkPassword(String sUId, String sEncryptPassword, String sDbEncryptPassword,String salt) {
		if(Runner.encryptAlgorithm!=null && Runner.encryptAlgorithm.length()>0) {
			StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();
			stringEncryptor.setPassword(EncryptPassword);
			stringEncryptor.setAlgorithm(Runner.encryptAlgorithm);
			stringEncryptor.setIvGenerator(new RandomIvGenerator()); 
			String sDbPassword = stringEncryptor.decrypt(sDbEncryptPassword);
			sEncryptPassword = decrypt(sEncryptPassword,salt);
			return sEncryptPassword.equals(sDbPassword);
		}
		String sInputPassword = DigestUtils.md5Hex(sUId+sEncryptPassword);
		return sDbEncryptPassword.equals(sInputPassword);
	}
	public static String encryptPassword(String sUserName, String sUId, String sEncryptPassword,String salt,tab.configServer.ValueString cause) {
		if(Runner.encryptAlgorithm!=null && Runner.encryptAlgorithm.length()>0) {
			sEncryptPassword = decrypt(sEncryptPassword,salt);
			if(Util.passwordAlgorithm.minlen>0 && sEncryptPassword.length()<Util.passwordAlgorithm.minlen) {
				if(cause!=null) {
					cause.value = "The password length is not enough!";
				}
				return StringUtils.EMPTY;
			}
			char[] chPassword = sEncryptPassword.toCharArray();
			int nDigits = 0,nUpperChar = 0, nLowerChar = 0, nOtherChar = 0;
			for(char ch : chPassword) {
				if(Character.isDigit(ch))nDigits++;
				else if(ch>='a' && ch<='z') {
					nLowerChar++;
				}else if(ch>='A' && ch<='Z') {
					nUpperChar++;
				}else { 
					nOtherChar++;
				}
			}
			if(Util.passwordAlgorithm.dcredit<0) {
				if(nDigits<Math.abs(Util.passwordAlgorithm.dcredit)) {
					if(cause!=null) {
						cause.value = "The password must contain enough numbers!";
					}
					return StringUtils.EMPTY;
				}
			}else if(Util.passwordAlgorithm.dcredit>0){
				if(nDigits>Util.passwordAlgorithm.dcredit) {
					if(cause!=null) {
						cause.value = "The password contain too more numbers!";
					}
					return StringUtils.EMPTY;
				}
			}
			if(Util.passwordAlgorithm.lcredit<0) {
				if(nLowerChar<Math.abs(Util.passwordAlgorithm.lcredit)) {
					if(cause!=null) {
						cause.value = "The password must contain enough lowercase letters!";
					}
					return StringUtils.EMPTY;
				}
			}else if(Util.passwordAlgorithm.lcredit>0){
				if(nLowerChar>Util.passwordAlgorithm.lcredit) {
					if(cause!=null) {
						cause.value = "The password contain too more lowercase letters!";
					}
					return StringUtils.EMPTY;
				}
			}
			if(Util.passwordAlgorithm.ucredit<0) {
				if(nUpperChar<Math.abs(Util.passwordAlgorithm.ucredit)) {
					if(cause!=null) {
						cause.value = "The password must contain enough uppercase letters!";
					}
					return StringUtils.EMPTY;
				}
			}else if(Util.passwordAlgorithm.ucredit>0){
				if(nUpperChar>Util.passwordAlgorithm.ucredit) {
					if(cause!=null) {
						cause.value = "The password contain too more uppercase letters!";
					}
					return StringUtils.EMPTY;
				}
			}
			if(Util.passwordAlgorithm.ocredit<0) {
				if(nOtherChar<Math.abs(Util.passwordAlgorithm.ocredit)) {
					if(cause!=null) {
						cause.value = "The password must contain other characters!";
					}
					return StringUtils.EMPTY;
				}
			}else if(Util.passwordAlgorithm.ocredit>0){
				if(nOtherChar>Util.passwordAlgorithm.ocredit) {
					if(cause!=null) {
						cause.value = "The password contain too more other characters!";
					}
					return StringUtils.EMPTY;
				}
			}
			if(sUserName.length()>0 && Util.passwordAlgorithm.reject_username && sEncryptPassword.indexOf(sUserName)>=0) {
				if(cause!=null) {
					cause.value = "The password cannot contain username!";
				}
				return StringUtils.EMPTY;
			}
			if(Util.passwordAlgorithm.gecoscheck) {
				String sLowerPassword = sEncryptPassword.toLowerCase();
				if(sLowerPassword.contains("admin")
					|| sLowerPassword.contains("tabadmin")
					|| sLowerPassword.contains("administrator")
					|| sLowerPassword.contains("root")
					) {
					if(cause!=null) {
						cause.value = "The password cannot contain common word!";
					}
					return StringUtils.EMPTY;
				}
			}
			StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();
			stringEncryptor.setPassword(EncryptPassword);
			stringEncryptor.setAlgorithm(Runner.encryptAlgorithm);
			stringEncryptor.setIvGenerator(new RandomIvGenerator()); 
			return stringEncryptor.encrypt(sEncryptPassword);
		}
		sEncryptPassword = DigestUtils.md5Hex(decrypt(sEncryptPassword,salt));
		return DigestUtils.md5Hex(sUId+sEncryptPassword);
	}
	public static String encryptPassword(String sUId, String sPassword) {
		if(Runner.encryptAlgorithm!=null && Runner.encryptAlgorithm.length()>0) {
			StandardPBEStringEncryptor stringEncryptor = new StandardPBEStringEncryptor();
			stringEncryptor.setPassword(EncryptPassword);
			stringEncryptor.setAlgorithm(Runner.encryptAlgorithm);
			stringEncryptor.setIvGenerator(new RandomIvGenerator()); 
			return stringEncryptor.encrypt(sPassword);
		}
		return DigestUtils.md5Hex(sUId+DigestUtils.md5Hex(sPassword));
	}
	public static String EncryptPassword = "MYTAB";
	//private static char[] salt = Arrays.copyOf("3FF2EC019C627B945225DEBAD71A01B6985FE84C95A70EB132882F88C0A59A55".toCharArray(),64);
	private static char[] Iv = Arrays.copyOf("F27D5C9927726BCEFE7510B1BDD3D137".toCharArray(),32);
	private static final String CIPHER_ALGORITHM_CBC = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
    private static int pswdIterations = 10000 ;
    private static int keySize = 128;
    private static SecretKey  GenerateKey(String salt) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, DecoderException {
    	if(salt==null)salt = StringUtils.EMPTY;
    	while(salt.length()<64)salt += '0';
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(EncryptPassword.toCharArray(), org.apache.commons.codec.binary.Hex.decodeHex(Arrays.copyOf(salt.toCharArray(),64)), pswdIterations, 128);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), KEY_ALGORITHM);
    }
	public static String encrypt(String plainText,String salt){   
			try {
		        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM_CBC);
		        cipher.init(Cipher.ENCRYPT_MODE, GenerateKey(salt), new IvParameterSpec(org.apache.commons.codec.binary.Hex.decodeHex(Iv)));
		        byte[] encryptedTextBytes = cipher.doFinal(plainText.getBytes());
		        return org.apache.commons.codec.binary.Base64.encodeBase64String(encryptedTextBytes);
			} catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | DecoderException e) {
				logger.error("ERROR:",e);
			}
	        return StringUtils.EMPTY;
	    }
	
		public static String decrypt(String encryptedText,String salt){
	        try {
	        	Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM_CBC);
		        cipher.init(Cipher.DECRYPT_MODE, GenerateKey(salt), new IvParameterSpec(org.apache.commons.codec.binary.Hex.decodeHex(Iv)));
		        byte[] decryptedTextBytes = cipher.doFinal(org.apache.commons.codec.binary.Base64.decodeBase64(encryptedText.getBytes()));
	            return new String(decryptedTextBytes);
			} catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | DecoderException e) {
				logger.error("ERROR:",e);
			}
	        return StringUtils.EMPTY;
	    }
}
