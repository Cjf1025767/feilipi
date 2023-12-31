package hbm.model;
// Generated 2019-5-27 9:42:21 by Hibernate Tools 4.3.5.Final

import java.util.Date;

/**
 * Calloutbatch generated by hbm2java
 */
public class Calloutbatch implements java.io.Serializable {

	private static final long serialVersionUID = 1760670237240728238L;
	private String id;
	private String name;
	private String description;
	private Date createdate;
	private Date updatedate;
	private int status;

	public Calloutbatch() {
	}

	public Calloutbatch(String id, String name, String description, Date createdate, Date updatedate, int status) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.createdate = createdate;
		this.updatedate = updatedate;
		this.status = status;
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

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
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

}
