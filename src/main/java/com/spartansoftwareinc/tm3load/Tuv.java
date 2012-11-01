package com.spartansoftwareinc.tm3load;

import java.sql.Timestamp;

public class Tuv {

	public Tu getTu() {
		return tu;
	}
	public void setTu(Tu tu) {
		this.tu = tu;
	}
	public String getSegment() {
		return segment.toString();
	}
	public void appendToSegment(String s) {
		this.segment.append(s);
	}
	public Timestamp getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Timestamp creationDate) {
		this.creationDate = creationDate;
	}
	public Timestamp getModifyDate() {
		return modifyDate;
	}
	public void setModifyDate(Timestamp modifyDate) {
		this.modifyDate = modifyDate;
	}
	private Tu tu;
	private StringBuilder segment = new StringBuilder();
	private Timestamp creationDate, modifyDate;
}
