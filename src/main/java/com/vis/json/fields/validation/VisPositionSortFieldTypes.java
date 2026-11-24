package com.vis.json.fields.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;

/**
 * Fields accepted by the {@code field} of each item of the {@code sort} of the position ({@code VisJsonFieldsPositionSort}):
 * the criteria by which the resumes found for the position are sorted. Since 2026-11-23 each item names one criterion,
 * and the criteria of one language or of one skill name it in the {@code value} of the item. Since 2026-11-24 each one
 * reads its value from the {@code sortValues} of a resume ({@code VisBusinessResumeSortValues}), the same rules of the
 * screen ({@code resumesSort.ts}), for the e-mails of the resumes of the positions.
 */
public enum VisPositionSortFieldTypes {
	/** By the value as a contractor (PJ). */
	pj {
		public double getValue(CcpJsonRepresentation sortValues, String criterionValue, Set<String> desiredSkills) {
			double value = getNumberOrMissing(sortValues, VisJsonCommonsFields.pj);
			return value;
		}
	},
	/** By the salary as an employee (CLT). */
	clt {
		public double getValue(CcpJsonRepresentation sortValues, String criterionValue, Set<String> desiredSkills) {
			double value = getNumberOrMissing(sortValues, VisJsonCommonsFields.clt);
			return value;
		}
	},
	/** By the level of the language named in the {@code value} of the item. */
	spokenLanguage {
		public double getValue(CcpJsonRepresentation sortValues, String criterionValue, Set<String> desiredSkills) {
			CcpJsonRepresentation levels = sortValues.getInnerJson(VisJsonCommonsFields.spokenLanguage);
			Set<String> languages = levels.fieldSet();
			for (String language : languages) {
				boolean sameLanguage = language.equalsIgnoreCase(criterionValue);
				if(false == sameLanguage) {
					continue;
				}
				Double level = levels.getAsDoubleNumber(() -> language);
				return level;
			}
			return 0;
		}
	},
	/** By the years of experience. */
	experience {
		public double getValue(CcpJsonRepresentation sortValues, String criterionValue, Set<String> desiredSkills) {
			double value = getNumberOrZero(sortValues, VisJsonCommonsFields.experience);
			return value;
		}
	},
	/** By how many skills the resume has. */
	skillsTotal {
		public double getValue(CcpJsonRepresentation sortValues, String criterionValue, Set<String> desiredSkills) {
			List<String> skills = sortValues.getAsStringList(VisJsonCommonsFields.skill);
			int total = skills.size();
			return total;
		}
	},
	/** By how many of the skills the position would like ({@code shouldSkills}) the resume has. */
	shouldSkillsTotal {
		public double getValue(CcpJsonRepresentation sortValues, String criterionValue, Set<String> desiredSkills) {
			List<String> skills = getAllSkills(sortValues);
			long total = desiredSkills.stream().filter(skills::contains).count();
			return total;
		}
	},
	/** By the days the candidate needs to start. */
	disponibility {
		public double getValue(CcpJsonRepresentation sortValues, String criterionValue, Set<String> desiredSkills) {
			double value = getNumberOrZero(sortValues, VisJsonCommonsFields.disponibility);
			return value;
		}
	},
	/** By the months of temporary job the candidate accepts. */
	temporallyJobTime {
		public double getValue(CcpJsonRepresentation sortValues, String criterionValue, Set<String> desiredSkills) {
			double value = getNumberOrZero(sortValues, VisJsonCommonsFields.temporallyJobTime);
			return value;
		}
	},
	/** By having the skill named in the {@code value} of the item. */
	skill {
		public double getValue(CcpJsonRepresentation sortValues, String criterionValue, Set<String> desiredSkills) {
			List<String> skills = getAllSkills(sortValues);
			boolean hasTheSkill = skills.contains(criterionValue);
			return hasTheSkill ? 1 : 0;
		}
	}
	;

	/** The value of a resume without the value of a criterion (without the claim): the end, in both directions. */
	public static final double MISSING = Double.MAX_VALUE;

	/**
	 * The value of a resume in this criterion.
	 * @param sortValues the {@code sortValues} of the resume
	 * @param criterionValue the {@code value} of the item (the language or the skill); empty for the others
	 * @param desiredSkills the skills the position would like
	 * @return the value, {@link #MISSING} when the resume does not have it
	 */
	public abstract double getValue(CcpJsonRepresentation sortValues, String criterionValue, Set<String> desiredSkills);

	private static double getNumberOrMissing(CcpJsonRepresentation sortValues, VisJsonCommonsFields field) {
		boolean absent = false == sortValues.containsAllFields(field);
		if(absent) {
			return MISSING;
		}
		Double value = sortValues.getAsDoubleNumber(field);
		return value;
	}

	private static double getNumberOrZero(CcpJsonRepresentation sortValues, VisJsonCommonsFields field) {
		boolean absent = false == sortValues.containsAllFields(field);
		if(absent) {
			return 0;
		}
		Double value = sortValues.getAsDoubleNumber(field);
		return value;
	}

	// the skills that count for the desired ones: the written ones and the ones above them in the hierarchy
	private static List<String> getAllSkills(CcpJsonRepresentation sortValues) {
		List<String> writtenSkills = sortValues.getAsStringList(VisJsonCommonsFields.skill);
		List<String> parentSkills = sortValues.getAsStringList(SortValuesFields.parentSkill);
		List<String> skills = new ArrayList<>(writtenSkills);
		skills.addAll(parentSkills);
		return skills;
	}

	/** The fields of the {@code sortValues} that {@code VisJsonCommonsFields} does not have. */
	static enum SortValuesFields implements CcpJsonFieldName {
		parentSkill,
	}
}