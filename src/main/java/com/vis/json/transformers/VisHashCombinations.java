package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityVirtualHashGrouper;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisJsonFieldsSkills;
import com.vis.json.fields.validation.VisLanguages;
import com.vis.json.fields.validation.VisResumeSkillCategories;

/**
 * Produces the hashes of a record, as records of {@code VisEntityVirtualHashGrouper}: one list of values per primary key
 * field of the grouper, then every combination among those lists (each combination is one record, whose id is a hash).
 * The resume ({@link VisResumeHashCombinations}) and the position ({@link VisPositionHashCombinations}) share this
 * logic; only the {@link VisRecordType} given by each subclass changes how the values of the record become the values
 * of the hash (the flags and the seniorities).
 * <p>Only exact values take part; the numeric ranges (claims, availability, temporary job time) stay out of the hash
 * and are compared after the read by id. A spoken language always gets the neutral value {@code none} too, and so does
 * the skill, so a search that requires no language, or no skill, finds the record. The regions (ddd) stay out of the
 * hash, compared after the read by id like the ranges: until 2026-10-14 they took part, and multiplied the combinations
 * by the number of regions (up to 48,720 groups for one resume, saves of 230 s).
 */
public abstract class VisHashCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{

	/** Neutral values of the fields. */
	enum JsonFieldValues implements CcpJsonFieldName {
		/** The language name, or the skill name, of a search that requires no language, or no skill. */
		none
	}

	/** How the values of the record become the values of the hash. */
	private final VisRecordType recordType;

	/**
	 * Keeps the side of the record.
	 * @param recordType the side of the record, given by the subclass
	 */
	protected VisHashCombinations(VisRecordType recordType) {
		this.recordType = recordType;
	}

	/**
	 * Combines the values of every field: each combination becomes one record of the grouper.
	 * @param json the record
	 * @return the records
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		List<List<CcpJsonRepresentation>> valuesByField = this.getValuesByField(json);

		List<CcpJsonRepresentation> result = Arrays.asList(CcpOtherConstants.EMPTY_JSON);

		for (List<CcpJsonRepresentation> fieldValues : valuesByField) {
			List<CcpJsonRepresentation> combinations = new ArrayList<>();
			for (CcpJsonRepresentation partialCombination : result) {
				for (CcpJsonRepresentation fieldValue : fieldValues) {
					CcpJsonRepresentation combination = partialCombination.mergeWithAnotherJson(fieldValue);
					combinations.add(combination);
				}
			}
			result = combinations;
		}
		return result;
	}

	/**
	 * Counts the records {@link #apply} produces, without building them: the product of the sizes of the lists.
	 * @param json the record
	 * @return the number of records
	 */
	public long countCombinations(CcpJsonRepresentation json) {
		List<List<CcpJsonRepresentation>> valuesByField = this.getValuesByField(json);
		long count = 1;
		for (List<CcpJsonRepresentation> fieldValues : valuesByField) {
			int size = fieldValues.size();
			count *= size;
		}
		return count;
	}

	/**
	 * Builds, for each primary key field of the grouper, the list of values the record generates for it.
	 * @param json the record
	 * @return the lists, one per field
	 */
	public List<List<CcpJsonRepresentation>> getValuesByField(CcpJsonRepresentation json) {

		List<CcpJsonRepresentation> resumeTypeValues = new ArrayList<>();
		Integer resumeType = json.getAsIntegerNumber(VisJsonCommonsFields.resumeType);
		CcpJsonRepresentation resumeTypeValue = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualHashGrouper.Fields.resumeType, resumeType);
		resumeTypeValues.add(resumeTypeValue);

		// a fluent speaker (2) also answers a search for the intermediate level (1)
		List<CcpJsonRepresentation> languageValues = new ArrayList<>();
		String noneName = JsonFieldValues.none.name();
		CcpJsonRepresentation jsonWithNoLanguageName = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualHashGrouper.Fields.languageName, noneName);
		CcpJsonRepresentation noLanguageValue = jsonWithNoLanguageName.put(VisEntityVirtualHashGrouper.Fields.languageLevel, 0);
		languageValues.add(noLanguageValue);
		List<CcpJsonRepresentation> spokenLanguages = json.getAsJsonList(VisJsonCommonsFields.spokenLanguage);
		for (CcpJsonRepresentation language : spokenLanguages) {
			String languageName = language.getAsString(VisLanguages.name);
			Integer declaredLevel = language.getAsIntegerNumber(VisLanguages.level);
			for(int languageLevel = declaredLevel; languageLevel >= 1; languageLevel--) {
				CcpJsonRepresentation jsonWithLanguageName = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualHashGrouper.Fields.languageName, languageName);
				CcpJsonRepresentation languageValue = jsonWithLanguageName.put(VisEntityVirtualHashGrouper.Fields.languageLevel, languageLevel);
				languageValues.add(languageValue);
			}
		}

		// only the skills of the main list (by which the resume wants to be found, or which the position requires), plus
		// their parents; each name once. The neutral value comes first: a search that requires no skill (the usual one out
		// of information technology) finds the record too. The category is compared by name: the resume and the position
		// have enums of their own, both with skillsFromText
		Set<String> skillNames = new LinkedHashSet<>();
		skillNames.add(noneName);
		String mainListCategory = VisResumeSkillCategories.skillsFromText.name();
		List<CcpJsonRepresentation> skills = json.getAsJsonList(VisJsonCommonsFields.skill);
		for (CcpJsonRepresentation skill : skills) {
			String category = skill.getAsString(VisJsonFieldsSkills.category);
			boolean isInvalidSkill = false == mainListCategory.equals(category);

			if(isInvalidSkill) {
				continue;
			}

			String skillName = skill.getAsString(VisJsonFieldsSkills.skill);
			skillNames.add(skillName);
			List<String> parents = skill.getAsStringList(VisJsonFieldsSkills.parent);
			for (String parent : parents) {
				skillNames.add(parent);
			}
		}
		List<CcpJsonRepresentation> skillValues = new ArrayList<>();
		for (String skillName : skillNames) {
			CcpJsonRepresentation skillValue = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualHashGrouper.Fields.skillName, skillName);
			skillValues.add(skillValue);
		}

		Integer[] pcds = this.recordType.fromBooleanField(VisJsonCommonsFields.pcd, json);
		List<CcpJsonRepresentation> pcdValues = this.toValues(VisEntityVirtualHashGrouper.Fields.pcd, pcds);

		Integer[] negotiableClaims = this.recordType.fromBooleanField(VisJsonCommonsFields.negotiableClaim, json);
		List<CcpJsonRepresentation> negotiableClaimValues = this.toValues(VisEntityVirtualHashGrouper.Fields.negotiableClaim, negotiableClaims);

		Integer[] travels = this.recordType.fromBooleanField(VisJsonCommonsFields.travel, json);
		List<CcpJsonRepresentation> travelValues = this.toValues(VisEntityVirtualHashGrouper.Fields.travel, travels);

		List<CcpJsonRepresentation> seniorityValues = new ArrayList<>();
		List<VisSeniorityType> seniorities = this.recordType.getSeniorities(json);
		for (VisSeniorityType seniority : seniorities) {
			String seniorityName = seniority.name();
			CcpJsonRepresentation seniorityValue = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualHashGrouper.Fields.seniority, seniorityName);
			seniorityValues.add(seniorityValue);
		}

		List<List<CcpJsonRepresentation>> valuesByField = Arrays.asList(
				resumeTypeValues,
				languageValues,
				skillValues,
				pcdValues,
				negotiableClaimValues,
				travelValues,
				seniorityValues
				);
		return valuesByField;
	}

	/**
	 * Turns each number into a json with the field.
	 * @param field the field
	 * @param values the numbers
	 * @return one json per number
	 */
	private List<CcpJsonRepresentation> toValues(CcpJsonFieldName field, Integer[] values) {
		List<CcpJsonRepresentation> jsonValues = new ArrayList<>();
		for (Integer value : values) {
			CcpJsonRepresentation jsonValue = CcpOtherConstants.EMPTY_JSON.put(field, value);
			jsonValues.add(jsonValue);
		}
		return jsonValues;
	}
}
