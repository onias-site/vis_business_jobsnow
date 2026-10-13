package com.vis.json.transformers;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;

public enum VisSeniorityType {
	
	estag(0,1),
	junior(1,3),
	pleno(3,5),
	senior(5,10),
	especialista(10, 70)
	;
	
	private final int min;
	private final int max;
	
	private VisSeniorityType(int min, int max) {
		this.min = min;
		this.max = max;
	}

	public boolean matches(CcpJsonFieldName field, CcpJsonRepresentation json) {
		Integer number = json.getAsIntegerNumber(field);
		if(number < this.min) {
			return false;
		}
		if(number >= this.max) {
			return false;
		}
		return true;
	}
	
	
}
