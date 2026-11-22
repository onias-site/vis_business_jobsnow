package com.vis.json.fields.validation;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.validations.global.interfaces.CcpJsonValidator;

/**
 * Class-level rule of each contact of the position ({@code VisJsonFieldsPositionContact}): the {@code value} must be
 * valid for its {@code type} ({@code VisPositionContactTypes}). A contact missing either field, or with a type out of
 * the enum, is left to the field rules, which already report it.
 */
public class VisJsonValidatorPositionContactValue implements CcpJsonValidator {
	/** The single instance: the class keeps no state. */
	public final static VisJsonValidatorPositionContactValue INSTANCE = new VisJsonValidatorPositionContactValue();

	private VisJsonValidatorPositionContactValue() {}

	/**
	 * Tells whether the value is not valid for the type.
	 * @param json the contact
	 * @param clazz the validation class
	 * @return {@code true} when the type rejects the value
	 */
	public boolean hasError(CcpJsonRepresentation json, Class<?> clazz) {
		boolean incomplete = false == json.containsAllFields(VisJsonFieldsPositionContact.type, VisJsonFieldsPositionContact.value);

		if(incomplete) {
			return false;
		}

		String typeName = json.getAsString(VisJsonFieldsPositionContact.type);
		Optional<VisPositionContactTypes> type = this.find(typeName);
		boolean unknownType = type.isEmpty();

		if(unknownType) {
			return false;
		}

		VisPositionContactTypes foundType = type.get();
		String value = json.getAsString(VisJsonFieldsPositionContact.value);
		boolean invalid = false == foundType.isValid(value);
		return invalid;
	}

	/**
	 * Finds the type with the name.
	 * @param name the name
	 * @return the type, when there is one with the name
	 */
	private Optional<VisPositionContactTypes> find(String name) {
		VisPositionContactTypes[] types = VisPositionContactTypes.values();
		List<VisPositionContactTypes> typeList = Arrays.asList(types);
		Optional<VisPositionContactTypes> found = typeList.stream().filter(type -> type.name().equals(name)).findFirst();
		return found;
	}

	/**
	 * Describes the invalid value.
	 * @param json the contact
	 * @param clazz the validation class
	 * @return the message
	 */
	public Object getErrorMessage(CcpJsonRepresentation json, Class<?> clazz) {
		String typeName = json.getAsString(VisJsonFieldsPositionContact.type);
		String value = json.getAsString(VisJsonFieldsPositionContact.value);
		String message = "The value " + value + " is not a valid contact of the type " + typeName;
		return message;
	}

	/**
	 * The invalid value does not stop the other rules of the class.
	 * @param json the contact
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
		return "The value of a contact must be valid for its type: whatsapp takes 10 to 15 digits, optionally after a +; "
				+ "email takes an e-mail address; link takes an address starting with http:// or https://.";
	}
}
