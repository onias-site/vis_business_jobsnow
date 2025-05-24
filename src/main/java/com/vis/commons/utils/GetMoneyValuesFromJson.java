package com.vis.commons.utils;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;

public enum GetMoneyValuesFromJson  {
	resume {
		public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json, String field) {
			boolean fieldIsNotPresent = json.containsAllFields(field) == false;
			
			if(fieldIsNotPresent) {
				return new ArrayList<>();
			}

			List<CcpJsonRepresentation> response = new ArrayList<>();
			
			int valueGaveByCandidate = json.getAsDoubleNumber(field).intValue();
			
			for(int k = valueGaveByCandidate; k <= 100000; k += 100) {
				CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON.put("moneyValue", k).put("moneyType", field);
				response.add(put);
			}
			
			return response;
		}
	}, position {
		public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json, String field) {
			boolean fieldIsNotPresent = json.containsAllFields(field) == false;
			
			if(fieldIsNotPresent) {
				return new ArrayList<>();
			}

			List<CcpJsonRepresentation> response = new ArrayList<>();
			
			int maxValueFromThisPosition = json.getAsDoubleNumber(field).intValue();
			
			for(int k = maxValueFromThisPosition; k >= 1000; k -= 100) {
				CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON.put("moneyValue", k).put("moneyType", field);
				response.add(put);
			}
			
			return response;
		}
	};

	public abstract List<CcpJsonRepresentation> apply(CcpJsonRepresentation json, String field);
	
}
