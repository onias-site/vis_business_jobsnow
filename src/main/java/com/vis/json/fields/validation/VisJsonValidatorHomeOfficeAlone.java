package com.vis.json.fields.validation;

import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.validations.global.interfaces.CcpJsonValidator;
import com.vis.entities.VisEntityResume;

/**
 * Class-level rule of the regions of the resume ({@code VisEntityResume.Fields.ddd}): the home office (ddd 10) is chosen
 * alone or not at all, the same rule of the screen, where choosing the home office clears the regions and choosing a
 * region clears the home office. Until 2026-10-14 the backend accepted {@code [10, 11]}, a resume the screen cannot
 * build and showed with both marked. A resume without the field is left to the field rules, which already report it.
 */
public class VisJsonValidatorHomeOfficeAlone implements CcpJsonValidator {

	/** The ddd of the home office jobs. */
	private static final double HOME_OFFICE = 10;

	/**
	 * Tells whether the home office comes with other ddds.
	 * @param json the resume
	 * @param clazz the validation class
	 * @return {@code true} when the home office is not alone
	 */
	public boolean hasError(CcpJsonRepresentation json, Class<?> clazz) {
		boolean withoutDdd = false == json.containsAllFields(VisEntityResume.Fields.ddd);

		if(withoutDdd) {
			return false;
		}

		// the json keeps numbers as decimals ("10.0")
		List<String> ddds = json.getAsStringList(VisEntityResume.Fields.ddd);
		boolean alone = ddds.size() <= 1;

		if(alone) {
			return false;
		}

		for (String dddText : ddds) {
			double ddd = Double.parseDouble(dddText);
			boolean isHomeOffice = ddd == HOME_OFFICE;

			if(isHomeOffice) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Describes the error.
	 * @param json the resume
	 * @param clazz the validation class
	 * @return the message
	 */
	public Object getErrorMessage(CcpJsonRepresentation json, Class<?> clazz) {
		List<String> ddds = json.getAsStringList(VisEntityResume.Fields.ddd);
		String message = "The home office (ddd 10) must be chosen alone, but came with other ddds: " + ddds;
		return message;
	}

	/**
	 * The error does not stop the other rules of the class.
	 * @param json the resume
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
		return "The home office (ddd 10) is chosen alone or not at all: it does not go with the regions.";
	}
}
