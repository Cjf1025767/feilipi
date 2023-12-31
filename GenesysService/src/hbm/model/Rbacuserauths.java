package hbm.model;
// Generated 2019-7-7 18:58:24 by Hibernate Tools 4.3.5.Final

import java.util.Date;

/**
 * Rbacuserauths generated by hbm2java
 */
public class Rbacuserauths implements java.io.Serializable {

	private String userguid;
	private String roleguid;
	private String password;
	private String username;
	private String agent;
	private String email;
	private String mobile;
	private String identifier;
	private Date logindate;
	private Date createdate;
	private Date updatedate;
	private int status;
	private String weixinid;
	private String alipayid;

	public Rbacuserauths() {
	}

	public Rbacuserauths(String userguid, String roleguid, String password, String username, String agent, String email,
			String mobile, String identifier, Date logindate, Date createdate, Date updatedate, int status,
			String weixinid, String alipayid) {
		this.userguid = userguid;
		this.roleguid = roleguid;
		this.password = password;
		this.username = username;
		this.agent = agent;
		this.email = email;
		this.mobile = mobile;
		this.identifier = identifier;
		this.logindate = logindate;
		this.createdate = createdate;
		this.updatedate = updatedate;
		this.status = status;
		this.weixinid = weixinid;
		this.alipayid = alipayid;
	}

	public String getUserguid() {
		return this.userguid;
	}

	public void setUserguid(String userguid) {
		this.userguid = userguid;
	}

	public String getRoleguid() {
		return this.roleguid;
	}

	public void setRoleguid(String roleguid) {
		this.roleguid = roleguid;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getAgent() {
		return this.agent;
	}

	public void setAgent(String agent) {
		this.agent = agent;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMobile() {
		return this.mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public Date getLogindate() {
		return this.logindate;
	}

	public void setLogindate(Date logindate) {
		this.logindate = logindate;
	}

	public Date getCreatedate() {
		return this.createdate;
	}

	public void setCreatedate(Date createdate) {
		this.createdate = createdate;
	}

	public Date getUpdatedate() {
		return this.updatedate;
	}

	public void setUpdatedate(Date updatedate) {
		this.updatedate = updatedate;
	}

	public int getStatus() {
		return this.status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getWeixinid() {
		return this.weixinid;
	}

	public void setWeixinid(String weixinid) {
		this.weixinid = weixinid;
	}

	public String getAlipayid() {
		return this.alipayid;
	}

	public void setAlipayid(String alipayid) {
		this.alipayid = alipayid;
	}

}
