// 仅用于演示目的
package com.sinovoice.aicp10;

import java.util.*;
import java.util.Map.Entry;

import javax.net.ssl.*;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class HciSdk {
	public final static int AUTH_NONE = 0; // 无认证
	public final static int AUTH_BASIC = 1; // WWW BASIC 认证，用于系统服务接口，例如 get-access-token
	public final static int AUTH_TOKEN = 2; // 访问令牌认证，用于能力接口

	private final String sys_url;
	private final String cap_url;
	private final String appkey;
	private final String auth;
	private String refresh_token;
	private long token_expire_time;
	private String token;

	private SSLContext sc;

	public HciSdk(String sys_url, String cap_url, String appkey, String secret) {
		while (sys_url.endsWith("/")) {
			sys_url = sys_url.substring(0, sys_url.length() - 1);
		}
		while (cap_url.endsWith("/")) {
			cap_url = cap_url.substring(0, cap_url.length() - 1);
		}

		this.sys_url = sys_url;
		this.cap_url = cap_url;
		this.appkey = appkey;
		this.auth = "Basic "
				+ new String(Base64.getEncoder().encode(((appkey + ":" + secret).getBytes(StandardCharsets.UTF_8))));

		// 禁用 SSL 证书检查，任何所有证书
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				X509Certificate[] myTrustedAnchors = new X509Certificate[0];
				return myTrustedAnchors;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		try {
			sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new SecureRandom());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String sdkUrl(String url) {
		return this.sdkUrl(url, false);
	}

	public String getAppkey() {
		return appkey;
	}

	public String sdkUrl(String url, boolean isWebsocket) {
		if (url.startsWith("sys:/")) {
			// 替换 sys: 前缀为 sys_url
			url = this.sys_url + url.substring(4);
		} else if (url.startsWith("cap:/")) {
			// 替换 cap: 前缀为 cap_url
			url = this.cap_url + url.substring(4);
		}
		if (isWebsocket) {
			url = url.replaceFirst("^http", "ws");
		}
		return url;
	}

	public JSONObject doHttp(String method, String url, int timeout, int auth_method) {
		return doHttp(method, url, timeout, auth_method, null, null);
	}

	public JSONObject doHttp(String method, String url, int timeout, int auth_method, Map<String, String> headers) {
		return doHttp(method, url, timeout, auth_method, headers, null);
	}

	public JSONObject doHttp(String method, String url, int timeout, int auth_method, Map<String, String> headers,
			Object body) {
		System.out.print("dopost输出url:" + url);
		HttpURLConnection c = null;
		try {
			URL u = new URL(url);
			c = (HttpURLConnection) u.openConnection();
			if (c instanceof HttpsURLConnection) {
				// 如果是 https 连接，需要禁用证书检查
				((HttpsURLConnection) c).setSSLSocketFactory(sc.getSocketFactory());
				((HttpsURLConnection) c).setHostnameVerifier(new HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession session) {
						return true; // 任何服务端证疏都接受
					}
				});
			}
			c.setRequestMethod(method);
			if (auth_method == AUTH_BASIC) {
				// 需要使用 appkey, secret 进行用户认证
				c.setRequestProperty("Authorization", this.auth);
			} else if (auth_method == AUTH_TOKEN) {
				c.setRequestProperty("X-Hci-Access-Token", getAccessToken());
			}
			if (body != null) {
				c.setDoOutput(true);
			}
			if (headers != null) {
				for (Entry<String, String> e : headers.entrySet()) {
					c.setRequestProperty(e.getKey(), e.getValue());
				}
			}
			c.setDoInput(true);
			c.setUseCaches(false);
			c.setAllowUserInteraction(false);
			c.setConnectTimeout(timeout);
			c.setReadTimeout(timeout);
			byte[] bytes = null;
			if (body != null) {
				if (body instanceof byte[]) {
					bytes = (byte[]) (body);
				} else if (body instanceof JSONObject) {
					bytes = ((JSONObject) (body)).toString().getBytes("UTF-8");
				}
				if (bytes != null) {
					App.log.info("输出bytes:" + bytes);
					OutputStream os = c.getOutputStream();
					os.write(bytes);
					os.flush();
					os.close();
				} else {
					App.log.info("bytes为空");
				}
			} else {
				App.log.info("body为空");
			}
			c.connect();
			int status = c.getResponseCode();
			String msg = c.getResponseMessage();
			App.log.info("输出返回message：" + msg);
			App.log.info("输出返回status:" + status);
			switch (status) {
			case 200:
				InputStream inputStream = c.getInputStream();
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int length;
				while ((length = inputStream.read(buffer)) != -1) {
					result.write(buffer, 0, length);
				}
				App.log.info("输出result:" + result.toString("UTF-8"));
				return new JSONObject(result.toString("UTF-8"));
			}
			String res="{\"code\":"+status+"}";
			return new JSONObject(res);

		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (c != null) {
				try {
					c.disconnect();
				} catch (Exception ex) {
				}
			}
		}
		
		return null;
	}

	public String doHttpString(String method, String url, int timeout, int auth_method, Map<String, String> headers,
			Object body) {
		System.out.print("doHttpString输出url:" + url);
		HttpURLConnection c = null;
		try {
			URL u = new URL(url);
			c = (HttpURLConnection) u.openConnection();
			if (c instanceof HttpsURLConnection) {
				// 如果是 https 连接，需要禁用证书检查
				((HttpsURLConnection) c).setSSLSocketFactory(sc.getSocketFactory());
				((HttpsURLConnection) c).setHostnameVerifier(new HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession session) {
						return true; // 任何服务端证疏都接受
					}
				});
			}
			c.setRequestMethod(method);
			if (auth_method == AUTH_BASIC) {
				// 需要使用 appkey, secret 进行用户认证
				c.setRequestProperty("Authorization", this.auth);
			} else if (auth_method == AUTH_TOKEN) {
				c.setRequestProperty("X-Hci-Access-Token", getAccessToken());
			}
			if (body != null) {
				c.setDoOutput(true);
			}
			if (headers != null) {
				for (Entry<String, String> e : headers.entrySet()) {
					c.setRequestProperty(e.getKey(), e.getValue());
				}
			}
			c.setDoInput(true);
			c.setUseCaches(false);
			c.setAllowUserInteraction(false);
			c.setConnectTimeout(timeout);
			c.setReadTimeout(timeout);
			byte[] bytes = null;
			if (body != null) {
				if (body instanceof byte[]) {
					bytes = (byte[]) (body);
				} else if (body instanceof JSONObject) {
					bytes = ((JSONObject) (body)).toString().getBytes("UTF-8");
				}
				if (bytes != null) {
					App.log.info("输出bytes:" + bytes);
					OutputStream os = c.getOutputStream();
					os.write(bytes);
					os.flush();
					os.close();
				} else {
					App.log.info("bytes为空");
				}
			} else {
				App.log.info("body为空");
			}
			c.connect();
			int status = c.getResponseCode();
			String msg = c.getResponseMessage();
			App.log.info("输出返回message：" + msg);
			App.log.info("输出返回status:" + status);
			switch (status) {
			case 200:
				InputStream inputStream = c.getInputStream();
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int length;
				while ((length = inputStream.read(buffer)) != -1) {
					result.write(buffer, 0, length);
				}
				App.log.info("输出result:" + result.toString("UTF-8"));
				return result.toString("UTF-8");
			}
			String res="{\"code\":"+status+"}";
			return  res;

		} catch (MalformedURLException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (c != null) {
				try {
					c.disconnect();
				} catch (Exception ex) {
				}
			}
		}
		return null;
	}

	synchronized public String getAccessToken() {
		String url = this.sdkUrl("sys:/v10/auth/get-access-token?appkey=" + this.appkey);
		if (this.refresh_token != null) {
			url += "&refresh_token=" + this.refresh_token;
		}

		if (this.token == null || this.token_expire_time < new Date().getTime()) {
			JSONObject reply = doHttp("GET", url, 15000, AUTH_BASIC);
			this.token = reply.getString("token");
			this.refresh_token = reply.getString("refresh_token");
			this.token_expire_time = new Date().getTime() + 3600 * 1000; // 一小时后过期
		}
		App.log.info("输出认证令牌:" + this.token);
		return this.token;
	}
}
