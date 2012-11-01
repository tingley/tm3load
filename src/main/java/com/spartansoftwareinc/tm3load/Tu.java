package com.spartansoftwareinc.tm3load;

import java.util.List;

import com.google.common.collect.Lists;

public class Tu {

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getSourceTmName() {
		return sourceTmName;
	}

	public void setSourceTmName(String sourceTmName) {
		this.sourceTmName = sourceTmName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	private long id;
	private String format;
	private String sourceTmName;
	private String type;
	
	private List<Tuv> tuvs = Lists.newArrayList();
	
	public List<Tuv> getTuvs() {
		return tuvs;
	}
	
	public void addTuv(Tuv tuv) {
		tuvs.add(tuv);
	}

	public boolean isTranslatable() {
		return true;
	}

	public Tuv getSourceTuv() {
		// HACK
		return tuvs.get(0);
	}
	
	public List<Tuv> getTargetTuvs() {
	    // HACK
	    return tuvs.subList(1, tuvs.size());
	}
}
