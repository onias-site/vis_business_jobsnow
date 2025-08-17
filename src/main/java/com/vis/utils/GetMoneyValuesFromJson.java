package com.vis.utils;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
public enum GetMoneyValuesFromJson  {
	resume {
		public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json, String field) {
			boolean fieldIsNotPresent = json.getDynamicVersion().containsAllFields(field) == false;
			
			if(fieldIsNotPresent) {
				return new ArrayList<>();
			}

			List<CcpJsonRepresentation> response = new ArrayList<>();
			
			int valueGaveByCandidate = json.getDynamicVersion().getAsDoubleNumber(field).intValue();
			
			for(int k = valueGaveByCandidate; k <= 100000; k += 100) {
				CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.moneyValue, k)
						.put(JsonFieldNames.moneyType, field);
				response.add(put);
			}
			
			return response;
		}
	}, position {
		public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json, String field) {
			boolean fieldIsNotPresent = json.getDynamicVersion().containsAllFields(field) == false;
			
			if(fieldIsNotPresent) {
				return new ArrayList<>();
			}

			List<CcpJsonRepresentation> response = new ArrayList<>();
			
			int maxValueFromThisPosition = json.getDynamicVersion().getAsDoubleNumber(field).intValue();
			
			for(int k = maxValueFromThisPosition; k >= 1000; k -= 100) {
				CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.moneyValue, k).put(JsonFieldNames.moneyType, field);
				response.add(put);
			}
			
			return response;
		}
	};

	public abstract List<CcpJsonRepresentation> apply(CcpJsonRepresentation json, String field);
	enum JsonFieldNames implements CcpJsonFieldName{
		moneyValue, moneyType
	}
	
}
