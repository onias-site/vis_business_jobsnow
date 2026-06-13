package com.vis.utils;

import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityResume;

/**
 * Define os critérios de ordenação de currículos em relação a uma vaga. Cada constante representa um
 * critério de comparação numérica entre dois currículos, usando os campos relevantes do JSON do currículo.
 */
enum ResumeSortOptions {

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
			
			if(false == o1.containsAllFields(new CcpFieldName(key))) {
				continue;
			}
			
			if(false == o2.containsAllFields(new CcpFieldName(key))) {
				continue;
			}
			
			Double value1 = o1.getAsDoubleNumber(new CcpFieldName(key));
			Double value2 = o2.getAsDoubleNumber(new CcpFieldName(key));
			
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
