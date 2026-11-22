package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.validations.global.interfaces.CcpJsonValidator;

/**
 * Class-level rule of a range ({@code VisJsonFieldsSalaryRange}, {@code VisJsonFieldsExpirationRange}): the
 * {@code max} may not be smaller than the {@code min}. A range missing either field is left to the field rules, which
 * already report it.
 */
public class VisJsonValidatorRangeMinNotAboveMax implements CcpJsonValidator {
	/** The single instance: the class keeps no state. */
	public final static VisJsonValidatorRangeMinNotAboveMax INSTANCE = new VisJsonValidatorRangeMinNotAboveMax();

	private VisJsonValidatorRangeMinNotAboveMax() {}

	/** The fields of a range. */
	enum Fields implements CcpJsonFieldName {
		/** The lower end of the range. */
		min,
		/** The upper end of the range. */
		max
	}

	/**
	 * Tells whether the maximum is smaller than the minimum.
	 * @param json the range
	 * @param clazz the validation class
	 * @return {@code true} when {@code max < min}
	 */
	public boolean hasError(CcpJsonRepresentation json, Class<?> clazz) {
		boolean incomplete = false == json.containsAllFields(Fields.min, Fields.max);

		if(incomplete) {
			return false;
		}

		Double min = json.getAsDoubleNumber(Fields.min);
		Double max = json.getAsDoubleNumber(Fields.max);
		boolean maxBelowMin = max < min;
		return maxBelowMin;
	}

	/**
	 * Describes the inverted range.
	 * @param json the range
	 * @param clazz the validation class
	 * @return the message
	 */
	public Object getErrorMessage(CcpJsonRepresentation json, Class<?> clazz) {
		String min = json.getAsString(Fields.min);
		String max = json.getAsString(Fields.max);
		String message = "The max " + max + " is smaller than the min " + min;
		return message;
	}

	/**
	 * The inverted range does not stop the other rules of the class.
	 * @param json the range
	 * @param clazz the validation class
	 * @return {@code false}
	 */
	public boolean isCriticalValidation(CcpJsonRepresentation json, Class<?> clazz) {
		return false;
	}

	/**
	 * Explains the rule.
	 * @param clazz the validation class
	 * @return the explanation
	 */
	public Object getRuleExplanation(Class<?> clazz) {
		return "The max of a range may not be smaller than its min.";
	}
}
