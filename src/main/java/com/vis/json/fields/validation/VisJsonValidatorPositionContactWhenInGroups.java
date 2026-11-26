package com.vis.json.fields.validation;

import java.util.List;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.validations.global.interfaces.CcpJsonValidator;

/**
 * Class-level rule of the position (since 2026-11-25): the contacts are required only when the position is published in
 * the groups of instant messages ({@code mustSendInGroups}, {@code true} when absent, its default), since they are shown
 * only in that publishing; a position kept out of the groups may go without them.
 */
public class VisJsonValidatorPositionContactWhenInGroups implements CcpJsonValidator {
	/** The single instance: the class keeps no state. */
	public final static VisJsonValidatorPositionContactWhenInGroups INSTANCE = new VisJsonValidatorPositionContactWhenInGroups();

	private VisJsonValidatorPositionContactWhenInGroups() {}

	/** The fields of the position read by the rule. */
	enum Fields implements CcpJsonFieldName {
		/** Whether the position is published in the groups. */
		mustSendInGroups,
		/** The contacts of the position. */
		contact
	}

	/**
	 * Tells whether a position published in the groups has no contact.
	 * @param json the position
	 * @param clazz the validation class
	 * @return {@code true} when it goes to the groups with no contact
	 */
	public boolean hasError(CcpJsonRepresentation json, Class<?> clazz) {
		boolean outOfTheGroups = json.containsAllFields(Fields.mustSendInGroups) && false == json.getAsBoolean(Fields.mustSendInGroups);

		if(outOfTheGroups) {
			return false;
		}

		List<Object> contacts = json.getAsObjectList(Fields.contact);
		boolean noContact = contacts.isEmpty();
		return noContact;
	}

	/**
	 * Describes the missing contacts.
	 * @param json the position
	 * @param clazz the validation class
	 * @return the message
	 */
	public Object getErrorMessage(CcpJsonRepresentation json, Class<?> clazz) {
		return "A position published in the groups of instant messages requires at least one contact";
	}

	/**
	 * The missing contacts do not stop the other rules of the class.
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
		return "The contacts are required when the position is published in the groups of instant messages (mustSendInGroups, true by default).";
	}
}
