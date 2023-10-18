package hbm.model;
// Generated 2018-7-15 15:11:51 by Hibernate Tools 4.0.1.Final

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Recfiles generated by hbm2java
 */
@Entity
@Table(name = "recfiles")
public class RecfilesEx implements java.io.Serializable {
	@Id
	@Column(name = "id", nullable = false)
	private String id;
	private String agent;
	private String caller;
	private String called;
	private int channel;
	private int direction;
	private String extension;
	private String filename;
	private int host;
	private int seconds;
	@JsonIgnore private int states;
	private Date createdate;
	private String ucid;
	private String queues;
	private String userdata;
	private String roleid;
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name = "roleid",referencedColumnName = "roleguid", insertable = false, updatable = false)
	@NotFound(action=NotFoundAction.IGNORE)
	public Rbacrole role;
	@Transient
	public String username;
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Transient
	public boolean lock;
	@Transient
	public boolean backup;
	@Transient
	public boolean delete;
	public RecfilesEx() {
	}

	public RecfilesEx(String id, String agent, String caller, String called, int channel, int direction,
			String extension, String filename, int host, int seconds, int states, Date createdate, String ucid,
			String userdata, String roleid,String queues) {
		this.id = id;
		this.agent = agent;
		this.caller = caller;
		this.called = called;
		this.channel = channel;
		this.direction = direction;
		this.extension = extension;
		this.filename = filename;
		this.host = host;
		this.seconds = seconds;
		this.states = states;
		this.createdate = createdate;
		this.ucid = ucid;
		this.userdata = userdata;
		this.roleid = roleid;
		this.queues = queues;
	}

	public String getQueues() {
		return queues;
	}

	public void setQueues(String queues) {
		this.queues = queues;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAgent() {
		return this.agent;
	}

	public void setAgent(String agent) {
		this.agent = agent;
	}

	public String getCaller() {
		return this.caller;
	}

	public void setCaller(String caller) {
		this.caller = caller;
	}

	public String getCalled() {
		return this.called;
	}

	public void setCalled(String called) {
		this.called = called;
	}

	public int getChannel() {
		return this.channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}

	public int getDirection() {
		return this.direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public String getExtension() {
		return this.extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public String getFilename() {
		return this.filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public int getHost() {
		return this.host;
	}

	public void setHost(int host) {
		this.host = host;
	}

	public int getSeconds() {
		return this.seconds;
	}

	public void setSeconds(int seconds) {
		this.seconds = seconds;
	}

	public int getStates() {
		return this.states;
	}

	public void setStates(int states) {
		this.states = states;
	}

	public Date getCreatedate() {
		return this.createdate;
	}

	public void setCreatedate(Date createdate) {
		this.createdate = createdate;
	}

	public String getUcid() {
		return this.ucid;
	}

	public void setUcid(String ucid) {
		this.ucid = ucid;
	}

	public String getUserdata() {
		return this.userdata;
	}

	public void setUserdata(String userdata) {
		this.userdata = userdata;
	}

	public String getRoleid() {
		return this.roleid;
	}

	public void setRoleid(String roleid) {
		this.roleid = roleid;
	}

}
