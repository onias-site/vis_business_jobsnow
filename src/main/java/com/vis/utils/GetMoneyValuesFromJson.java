package com.vis.utils;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
/**
 * Gera listas de valores monetários a partir de um JSON, com comportamento diferente dependendo se o
 * contexto é de currículo ou de vaga. Para currículo, gera todos os valores de remuneração do valor
 * declarado até 100.000 (o candidato aceita salários iguais ou maiores). Para vaga, gera todos os valores
 * do máximo declarado até 1.000 (a vaga aceita candidatos que pedem igual ou menos).
 */
public enum GetMoneyValuesFromJson  {
	resume {
		public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json, String field) {
			CcpFieldName ccpFieldName = new CcpFieldName(field);
			boolean containsAllFields = json.containsAllFields(ccpFieldName);
			boolean fieldIsNotPresent = false == containsAllFields;
			
			if(fieldIsNotPresent) {
				return new ArrayList<>();
			}

			List<CcpJsonRepresentation> response = new ArrayList<>();
			CcpFieldName ccpFieldName2 = new CcpFieldName(field);
			Double asDoubleNumber = json.getAsDoubleNumber(ccpFieldName2);

			int valueGaveByCandidate = asDoubleNumber.intValue();
			
			for(int k = valueGaveByCandidate; k <= 100000; k += 100) {
				CcpJsonRepresentation put2 = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.moneyValue, k);
				CcpJsonRepresentation put = put2
						.put(JsonFieldNames.moneyType, field);
				response.add(put);
			}
			
			return response;
		}
	}, position {
		public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json, String field) {
			CcpFieldName ccpFieldName3 = new CcpFieldName(field);
			boolean containsAllFields2 = json.containsAllFields(ccpFieldName3);
			boolean fieldIsNotPresent = false == containsAllFields2;
			
			if(fieldIsNotPresent) {
				return new ArrayList<>();
			}

			List<CcpJsonRepresentation> response = new ArrayList<>();
			CcpFieldName ccpFieldName4 = new CcpFieldName(field);
			Double asDoubleNumber2 = json.getAsDoubleNumber(ccpFieldName4);

			int maxValueFromThisPosition = asDoubleNumber2.intValue();
			
			for(int k = maxValueFromThisPosition; k >= 1000; k -= 100) {
				CcpJsonRepresentation put3 = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.moneyValue, k);
				CcpJsonRepresentation put = put3.put(JsonFieldNames.moneyType, field);
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
