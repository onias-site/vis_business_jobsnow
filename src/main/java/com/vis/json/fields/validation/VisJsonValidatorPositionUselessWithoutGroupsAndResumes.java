package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.validations.global.interfaces.CcpJsonValidator;

/**
 * Class-level rule of the position (since 2026-11-25): a position kept out of the groups of instant messages
 * ({@code mustSendInGroups} false) that receives no resumes ({@code frequencyType} {@code none}) is useless, since it
 * goes neither to the groups nor to the candidates nor sends resumes to the recruiter. The absent fields count as their
 * defaults ({@code mustSendInGroups} true, {@code frequencyType} daily), which are put only after the validation.
 */
public class VisJsonValidatorPositionUselessWithoutGroupsAndResumes implements CcpJsonValidator {
	/** The single instance: the class keeps no state. */
	public final static VisJsonValidatorPositionUselessWithoutGroupsAndResumes INSTANCE = new VisJsonValidatorPositionUselessWithoutGroupsAndResumes();

	private VisJsonValidatorPositionUselessWithoutGroupsAndResumes() {}

	/** The fields of the position read by the rule. */
	enum Fields implements CcpJsonFieldName {
		/** Whether the position is published in the groups. */
		mustSendInGroups,
		/** How the resumes of the position are received. */
		frequencyType
	}

	/**
	 * Tells whether the position goes nowhere.
	 * @param json the position
	 * @param clazz the validation class
	 * @return {@code true} when it is out of the groups and receives no resumes
	 */
	public boolean hasError(CcpJsonRepresentation json, Class<?> clazz) {
		boolean inTheGroups = false == json.containsAllFields(Fields.mustSendInGroups) || json.getAsBoolean(Fields.mustSendInGroups);

		if(inTheGroups) {
			return false;
		}

		// absent, the frequency is daily (its default): resumes are received
		boolean defaultFrequency = false == json.containsAllFields(Fields.frequencyType);

		if(defaultFrequency) {
			return false;
		}

		String frequencyType = json.getAsString(Fields.frequencyType);
		String noResumesFrequency = VisPositionFrequencyTypes.none.name();
		boolean noResumes = noResumesFrequency.equals(frequencyType);
		return noResumes;
	}

	/**
	 * Describes the useless position.
	 * @param json the position
	 * @param clazz the validation class
	 * @return the message
	 */
	public Object getErrorMessage(CcpJsonRepresentation json, Class<?> clazz) {
		return "A position out of the groups of instant messages must receive resumes (frequencyType other than none)";
	}

	/**
	 * The useless position does not stop the other rules of the class.
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
		return "A position must be published in the groups of instant messages or receive resumes: mustSendInGroups false together with frequencyType none is refused.";
	}
}
