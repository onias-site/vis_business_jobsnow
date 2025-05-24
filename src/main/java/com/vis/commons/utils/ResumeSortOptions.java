package com.vis.commons.utils;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.commons.entities.VisEntityPosition;
import com.vis.commons.entities.VisEntityResume;

public enum ResumeSortOptions {

	disponibility(VisEntityResume.Fields.disponibility.name()),
	desiredSkill(VisEntityPosition.Fields.desiredSkill.name()),
	money(VisEntityResume.Fields.clt.name(), VisEntityResume.Fields.pj.name(), VisEntityResume.Fields.btc.name()),
	experience(VisEntityResume.Fields.experience.name()),
	;
	final String[] fieldsToSort;
	
	
	private ResumeSortOptions(String... fieldsToSort) {
		this.fieldsToSort = fieldsToSort;
	}

	public int compare(CcpJsonRepresentation o1, CcpJsonRepresentation o2) {
		int compareTo = this.compareTo(o1, o2, this.fieldsToSort);
		return compareTo;
	}
	
	private int compareTo(CcpJsonRepresentation o1, CcpJsonRepresentation o2, String... keys) {
		
		for (String key : keys) {
			
			if(o1.containsAllFields(key) == false) {
				continue;
			}
			
			if(o2.containsAllFields(key) == false) {
				continue;
			}
			
			Double value1 = o1.getAsDoubleNumber(key);
			Double value2 = o2.getAsDoubleNumber(key);
			
			int compareTo = value1.compareTo(value2);
			
			boolean areEquals = compareTo == 0;
			
			if(areEquals) {
				continue;
			}
			
			return compareTo;
		}
		return 0;
	}
	
}
