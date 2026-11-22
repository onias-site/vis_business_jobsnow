package com.vis.json.fields.validation;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.validations.global.interfaces.CcpJsonValidator;

/**
 * Class-level rule of each skill of the position ({@code VisJsonFieldsPositionSkills}): the {@code type} must be one of the types
 * its {@code category} accepts ({@code VisPositionSkillCategories}). A skill missing either field, or carrying a value out
 * of the enums, is left to the field rules, which already report it.
 */
public class VisJsonValidatorPositionSkillTypeMatchesCategory implements CcpJsonValidator {
	/** The single instance: the class keeps no state. */
	public final static VisJsonValidatorPositionSkillTypeMatchesCategory INSTANCE = new VisJsonValidatorPositionSkillTypeMatchesCategory();

	private VisJsonValidatorPositionSkillTypeMatchesCategory() {}

	/**
	 * Tells whether the type does not match the category.
	 * @param json the skill
	 * @param clazz the validation class
	 * @return {@code true} when the category does not accept the type
	 */
	public boolean hasError(CcpJsonRepresentation json, Class<?> clazz) {
		boolean incomplete = false == json.containsAllFields(VisJsonFieldsPositionSkills.category, VisJsonFieldsPositionSkills.type);

		if(incomplete) {
			return false;
		}

		String categoryName = json.getAsString(VisJsonFieldsPositionSkills.category);
		String typeName = json.getAsString(VisJsonFieldsPositionSkills.type);
		Optional<VisPositionSkillCategories> category = this.find(VisPositionSkillCategories.values(), categoryName);
		Optional<VisPositionSkillTypes> type = this.find(VisPositionSkillTypes.values(), typeName);
		boolean unknownValue = category.isEmpty() || type.isEmpty();

		if(unknownValue) {
			return false;
		}

		VisPositionSkillCategories foundCategory = category.get();
		VisPositionSkillTypes foundType = type.get();
		boolean mismatch = false == foundCategory.accepts(foundType);
		return mismatch;
	}

	/**
	 * Finds the constant with the name.
	 * @param <T> the enum
	 * @param constants the constants of the enum
	 * @param name the name
	 * @return the constant, when there is one with the name
	 */
	private <T extends Enum<T>> Optional<T> find(T[] constants, String name) {
		List<T> constantList = Arrays.asList(constants);
		Optional<T> found = constantList.stream().filter(constant -> constant.name().equals(name)).findFirst();
		return found;
	}

	/**
	 * Describes the mismatch.
	 * @param json the skill
	 * @param clazz the validation class
	 * @return the message
	 */
	public Object getErrorMessage(CcpJsonRepresentation json, Class<?> clazz) {
		String categoryName = json.getAsString(VisJsonFieldsPositionSkills.category);
		String typeName = json.getAsString(VisJsonFieldsPositionSkills.type);
		VisPositionSkillCategories category = VisPositionSkillCategories.valueOf(categoryName);
		List<VisPositionSkillTypes> acceptedTypes = category.getAcceptedTypes();
		String message = "The type " + typeName + " does not go with the category " + categoryName + ", which accepts " + acceptedTypes;
		return message;
	}

	/**
	 * The mismatch does not stop the other rules of the class.
	 * @param json the skill
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
		return "The type of a skill must be one of the types its category accepts: skillsFromText takes skillsFromResume, "
				+ "shouldSkills takes shouldSkills, parent takes parent, and discardedSkills takes isPieceOfOtherWord, "
				+ "isPieceOfOtherSkill or skillAlreadyAdded.";
	}
}
