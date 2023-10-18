package hbm.model;
// Generated 2021-12-2 0:31:09 by Hibernate Tools 4.3.5.Final

import java.util.Date;

/**
 * Callholidays generated by hbm2java
 */
public class Callholidays implements java.io.Serializable {

	private String callholidayid;
	private int id;
	private String name;
	private int activate;
	private Date startdate;
	private Date starttime;
	private Date endtime;
	private Date createdate;
	private Date updatedate;
	private String dept;
	private String phone;

	public Callholidays() {
	}

	public Callholidays(String callholidayid, int id, String name, int activate, Date startdate, Date createdate,
			Date updatedate, String dept, String phone) {
		this.callholidayid = callholidayid;
		this.id = id;
		this.name = name;
		this.activate = activate;
		this.startdate = startdate;
		this.createdate = createdate;
		this.updatedate = updatedate;
		this.dept = dept;
		this.phone = phone;
	}

	public Callholidays(String callholidayid, int id, String name, int activate, Date startdate, Date starttime,
			Date endtime, Date createdate, Date updatedate, String dept, String phone) {
		this.callholidayid = callholidayid;
		this.id = id;
		this.name = name;
		this.activate = activate;
		this.startdate = startdate;
		this.starttime = starttime;
		this.endtime = endtime;
		this.createdate = createdate;
		this.updatedate = updatedate;
		this.dept = dept;
		this.phone = phone;
	}

	public String getCallholidayid() {
		return this.callholidayid;
	}

	public void setCallholidayid(String callholidayid) {
		this.callholidayid = callholidayid;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getActivate() {
		return this.activate;
	}

	public void setActivate(int activate) {
		this.activate = activate;
	}

	public Date getStartdate() {
		return this.startdate;
	}

	public void setStartdate(Date startdate) {
		this.startdate = startdate;
	}

	public Date getStarttime() {
		return this.starttime;
	}

	public void setStarttime(Date starttime) {
		this.starttime = starttime;
	}

	public Date getEndtime() {
		return this.endtime;
	}

	public void setEndtime(Date endtime) {
		this.endtime = endtime;
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

	public String getDept() {
		return this.dept;
	}

	public void setDept(String dept) {
		this.dept = dept;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

}