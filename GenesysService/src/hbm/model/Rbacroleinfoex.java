package hbm.model;
// Generated 2018-9-18 13:24:26 by Hibernate Tools 4.0.1.Final

import java.util.Date;

/**
 * Rbacroleinfo generated by hbm2java
 */
public class Rbacroleinfoex implements java.io.Serializable {

	private String roleguid;
	private String secret;
	private Date createdate;
	private Date updatedate;
	private String redirecturi;
	private String appid;
	private String imserver;
	private int channel;
	private String options;
	private String skillname;
	
	public Rbacroleinfoex() {
	}

	public Rbacroleinfoex(String roleguid, String secret, Date createdate, Date updatedate, String redirecturi, String appid, String imserver, int channel,String options,String skillname) {
		this.roleguid = roleguid;
		this.secret = secret;
		this.createdate = createdate;
		this.updatedate = updatedate;
		this.redirecturi = redirecturi;
		this.appid = appid;
		this.imserver = imserver;
		this.channel = channel;
		this.options = options;
		this.skillname = skillname;
	}

	public String getRoleguid() {
		return this.roleguid;
	}

	public void setRoleguid(String roleguid) {
		this.roleguid = roleguid;
	}

	public String getSecret() {
		return this.secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
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

	public String getRedirecturi() {
		return this.redirecturi;
	}

	public void setRedirecturi(String redirecturi) {
		this.redirecturi = redirecturi;
	}

	public String getAppid() {
		return appid;
	}

	public void setAppid(String appid) {
		this.appid = appid;
	}
	
	public String getImserver() {
		return imserver;
	}

	public void setImserver(String imserver) {
		this.imserver = imserver;
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}
	
	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

	public String getSkillname() {
		return skillname;
	}

	public void setSkillname(String skillname) {
		this.skillname = skillname;
	}
	
}
