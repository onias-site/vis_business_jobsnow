package com.vis.utils;

import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityPosition;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Define os critérios de ordenação de currículos em relação a uma vaga. Cada constante representa um
 * critério de comparação numérica entre dois currículos, usando os campos relevantes do JSON do currículo.
 */
enum ResumeSortOptions {

	disponibility(VisJsonCommonsFields.disponibility.name()),
	desiredSkill(VisEntityPosition.Fields.desiredSkill.name()),
	money(VisJsonCommonsFields.clt.name(), VisJsonCommonsFields.pj.name(), VisJsonCommonsFields.btc.name()),
	experience(VisJsonCommonsFields.experience.name()),
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
			CcpFieldName ccpFieldName = new CcpFieldName(key);
			boolean containsAllFields = o1.containsAllFields(ccpFieldName);
			boolean valorIgual = false == containsAllFields;
		
			if(valorIgual) {
				continue;
			}
			CcpFieldName ccpFieldName2 = new CcpFieldName(key);
			boolean containsAllFields2 = o2.containsAllFields(ccpFieldName2);
			boolean valorIgual2 = false == containsAllFields2;

			if(valorIgual2) {
				continue;
			}
			CcpFieldName ccpFieldName3 = new CcpFieldName(key);

			Double value1 = o1.getAsDoubleNumber(ccpFieldName3);
			CcpFieldName ccpFieldName4 = new CcpFieldName(key);
			Double value2 = o2.getAsDoubleNumber(ccpFieldName4);
			
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
