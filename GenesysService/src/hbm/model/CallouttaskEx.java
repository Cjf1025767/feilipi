package hbm.model;
// Generated 2019-5-24 15:31:52 by Hibernate Tools 4.3.5.Final

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/**
 * Callouttask generated by hbm2java
 */
@Entity
@Table(name = "callouttask")
public class CallouttaskEx implements java.io.Serializable {

	private static final long serialVersionUID = -680877220914218645L;
	@Id
	@Column(name = "id", nullable = false)
	private String id;
	private String name;
	private Date startdate;
	private Date expiredate;
	private String trunkid;
	private String workdeptid;
	private int period;
	private String batchid;
	private int status;
	private int executions;
	private int finishes;
	private Date nextdate;
	private double agentratio;
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name = "trunkid",referencedColumnName = "id", insertable = false, updatable = false)
	@NotFound(action=NotFoundAction.IGNORE)
	public Callouttrunk trunk;
	@OneToOne(fetch=FetchType.EAGER)
	@JoinColumn(name = "batchid",referencedColumnName = "id", insertable = false, updatable = false)
	@NotFound(action=NotFoundAction.IGNORE)
	public Calloutbatch batch;
	@Transient
	public java.util.Map<String, Object> summary;
	
	public CallouttaskEx() {
	}

	public CallouttaskEx(String id, String name, Date startdate, Date expiredate, String trunkid, String workdeptid, int period,
			String batchid, int status, int executions, int finishes, Date nextdate, double agentratio) {
		this.id = id;
		this.name = name;
		this.startdate = startdate;
		this.expiredate = expiredate;
		this.trunkid = trunkid;
		this.workdeptid = workdeptid;
		this.period = period;
		this.batchid = batchid;
		this.status = status;
		this.executions = executions;
		this.finishes = finishes;
		this.nextdate = nextdate;
		this.agentratio = agentratio;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getStartdate() {
		return this.startdate;
	}

	public void setStartdate(Date startdate) {
		this.startdate = startdate;
	}

	public Date getExpiredate() {
		return this.expiredate;
	}

	public void setExpiredate(Date expiredate) {
		this.expiredate = expiredate;
	}

	public String getTrunkid() {
		return this.trunkid;
	}

	public void setTrunkid(String trunkid) {
		this.trunkid = trunkid;
	}

	public String getWorkdeptid() {
		return this.workdeptid;
	}

	public void setWorkdeptid(String workdeptid) {
		this.workdeptid = workdeptid;
	}
	
public int getPeriod() {
		return this.period;
	}

	public void setPeriod(int period) {
		this.period = period;
	}

	public String getBatchid() {
		return this.batchid;
	}

	public void setBatchid(String batchid) {
		this.batchid = batchid;
	}

	public int getStatus() {
		return this.status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getExecutions() {
		return this.executions;
	}

	public void setExecutions(int executions) {
		this.executions = executions;
	}

	public int getFinishes() {
		return this.finishes;
	}

	public void setFinishes(int finishes) {
		this.finishes = finishes;
	}

	public Date getNextdate() {
		return this.nextdate;
	}

	public void setNextdate(Date nextdate) {
		this.nextdate = nextdate;
	}

	public double getAgentratio() {
		return this.agentratio;
	}

	public void setAgentratio(double agentratio) {
		this.agentratio = agentratio;
	}

}