package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.validations.global.interfaces.CcpJsonValidator;

/**
 * Class-level rule of the position filled in the screen (since 2026-11-22): unless the frequency of sending of the resumes
 * found is {@code none}, the position needs the {@code resumesEmail} that receives them. No frequency informed counts as
 * the default one ({@code daily}), which sends.
 */
public class VisJsonValidatorPositionResumesEmailRequired implements CcpJsonValidator {
	/** The single instance: the class keeps no state. */
	public final static VisJsonValidatorPositionResumesEmailRequired INSTANCE = new VisJsonValidatorPositionResumesEmailRequired();

	/** The fields read by the rule. */
	enum Fields implements CcpJsonFieldName {
		frequencyType, resumesEmail
	}

	private VisJsonValidatorPositionResumesEmailRequired() {}

	/**
	 * Tells whether the position sends the resumes and has no e-mail to receive them.
	 * @param json the position
	 * @param clazz the validation class
	 * @return {@code true} when the e-mail is missing
	 */
	public boolean hasError(CcpJsonRepresentation json, Class<?> clazz) {
		boolean hasTheEmail = json.containsAllFields(Fields.resumesEmail);

		if(hasTheEmail) {
			return false;
		}

		String none = VisPositionFrequencyTypes.none.name();
		String frequencyType = json.getAsString(Fields.frequencyType);
		boolean sendsTheResumes = false == none.equals(frequencyType);
		return sendsTheResumes;
	}

	/**
	 * Describes the missing e-mail.
	 * @param json the position
	 * @param clazz the validation class
	 * @return the message
	 */
	public Object getErrorMessage(CcpJsonRepresentation json, Class<?> clazz) {
		return "The field resumesEmail is required when the frequencyType is not none";
	}

	/**
	 * The missing e-mail does not stop the other rules of the class.
	 * @param json the position
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
		return "The resumesEmail receives the resumes found for the position: it is required unless the frequencyType is none "
				+ "(no frequency informed means daily).";
	}
}
