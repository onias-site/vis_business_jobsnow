package com.vis.utils;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
/**
 * Generates lists of money values from a JSON, behaving differently depending on whether the
 * context is a resume or a position. For a resume, it generates every compensation value from the
 * declared value up to 100,000 (the candidate accepts equal or higher salaries). For a position, it generates
 * every value from the declared maximum down to 1,000 (the position accepts candidates asking for the same or less).
 */
public enum GetMoneyValuesFromJson  {
	/** For a resume: every value from the declared one up to 100,000, in steps of 100. */
	resume {
		/**
		 * Generates the values of the resume.
		 * @param json the resume
		 * @param field the money field ({@code btc}, {@code clt} or {@code pj})
		 * @return one {@code moneyValue}/{@code moneyType} item per value, or none when the field is absent
		 */
		public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json, String field) {
			CcpFieldName moneyFieldName = new CcpFieldName(field);
			boolean fieldIsPresent = json.containsAllFields(moneyFieldName);
			boolean fieldIsNotPresent = false == fieldIsPresent;
			
			if(fieldIsNotPresent) {
				return new ArrayList<>();
			}

			List<CcpJsonRepresentation> response = new ArrayList<>();
			CcpFieldName sameMoneyFieldName = new CcpFieldName(field);
			Double declaredValue = json.getAsDoubleNumber(sameMoneyFieldName);

			int valueGaveByCandidate = declaredValue.intValue();
			
			for(int k = valueGaveByCandidate; k <= 100000; k += 100) {
				CcpJsonRepresentation jsonWithMoneyValue = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.moneyValue, k);
				CcpJsonRepresentation moneyRecord = jsonWithMoneyValue
						.put(JsonFieldNames.moneyType, field);
				response.add(moneyRecord);
			}
			
			return response;
		}
	},
	/** For a position: every value from the declared maximum down to 1,000, in steps of 100. */
	position {
		/**
		 * Generates the values of the position.
		 * @param json the position
		 * @param field the money field ({@code btc}, {@code clt} or {@code pj})
		 * @return one {@code moneyValue}/{@code moneyType} item per value, or none when the field is absent
		 */
		public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json, String field) {
			CcpFieldName moneyFieldName = new CcpFieldName(field);
			boolean fieldIsPresent = json.containsAllFields(moneyFieldName);
			boolean fieldIsNotPresent = false == fieldIsPresent;
			
			if(fieldIsNotPresent) {
				return new ArrayList<>();
			}

			List<CcpJsonRepresentation> response = new ArrayList<>();
			CcpFieldName sameMoneyFieldName = new CcpFieldName(field);
			Double declaredMaxValue = json.getAsDoubleNumber(sameMoneyFieldName);

			int maxValueFromThisPosition = declaredMaxValue.intValue();
			
			for(int k = maxValueFromThisPosition; k >= 1000; k -= 100) {
				CcpJsonRepresentation jsonWithMoneyValue = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.moneyValue, k);
				CcpJsonRepresentation moneyRecord = jsonWithMoneyValue.put(JsonFieldNames.moneyType, field);
				response.add(moneyRecord);
			}
			
			return response;
		}
	};

	/**
	 * Generates the money values of the field.
	 * @param json the resume or the position
	 * @param field the money field
	 * @return the money values
	 */
	public abstract List<CcpJsonRepresentation> apply(CcpJsonRepresentation json, String field);
	/** Fields of a money value. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** The {@code moneyValue} field. */
		moneyValue,
		/** The {@code moneyType} field. */
		moneyType
	}
	
}
