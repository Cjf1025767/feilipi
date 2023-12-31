package hbm.model;
// Generated 2019-5-28 9:28:15 by Hibernate Tools 4.3.5.Final

/**
 * CalloutphoneId generated by hbm2java
 */
public class CalloutphoneId implements java.io.Serializable {

	private static final long serialVersionUID = -1487635770242450300L;
	private String batchid;
	private String phone;

	public CalloutphoneId() {
	}

	public CalloutphoneId(String batchid, String phone) {
		this.batchid = batchid;
		this.phone = phone;
	}

	public String getBatchid() {
		return this.batchid;
	}

	public void setBatchid(String batchid) {
		this.batchid = batchid;
	}

	public String getPhone() {
		return this.phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public boolean equals(Object other) {
		if ((this == other))
			return true;
		if ((other == null))
			return false;
		if (!(other instanceof CalloutphoneId))
			return false;
		CalloutphoneId castOther = (CalloutphoneId) other;

		return ((this.getBatchid() == castOther.getBatchid()) || (this.getBatchid() != null
				&& castOther.getBatchid() != null && this.getBatchid().equals(castOther.getBatchid())))
				&& ((this.getPhone() == castOther.getPhone()) || (this.getPhone() != null
						&& castOther.getPhone() != null && this.getPhone().equals(castOther.getPhone())));
	}

	public int hashCode() {
		int result = 17;

		result = 37 * result + (getBatchid() == null ? 0 : this.getBatchid().hashCode());
		result = 37 * result + (getPhone() == null ? 0 : this.getPhone().hashCode());
		return result;
	}

}
