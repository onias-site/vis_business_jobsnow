package com.vis.utils;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
/**
 * Gera listas de valores monetários a partir de um JSON, com comportamento diferente dependendo se o
 * contexto é de currículo ou de vaga. Para currículo, gera todos os valores de remuneração do valor
 * declarado até 100.000 (o candidato aceita salários iguais ou maiores). Para vaga, gera todos os valores
 * do máximo declarado até 1.000 (a vaga aceita candidatos que pedem igual ou menos).
 */
public enum GetMoneyValuesFromJson  {
	resume {
		public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json, String field) {
			boolean fieldIsNotPresent = false == json.containsAllFields(new CcpFieldName(field));
			
			if(fieldIsNotPresent) {
				return new ArrayList<>();
			}

			List<CcpJsonRepresentation> response = new ArrayList<>();
			
			int valueGaveByCandidate = json.getAsDoubleNumber(new CcpFieldName(field)).intValue();
			
			for(int k = valueGaveByCandidate; k <= 100000; k += 100) {
				CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.moneyValue, k)
						.put(JsonFieldNames.moneyType, field);
				response.add(put);
			}
			
			return response;
		}
	}, position {
		public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json, String field) {
			boolean fieldIsNotPresent = false == json.containsAllFields(new CcpFieldName(field));
			
			if(fieldIsNotPresent) {
				return new ArrayList<>();
			}

			List<CcpJsonRepresentation> response = new ArrayList<>();
			
			int maxValueFromThisPosition = json.getAsDoubleNumber(new CcpFieldName(field)).intValue();
			
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
