package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * The side of a record in the hashes of {@code VisEntityVirtualHashGrouper}: the record that is saved and found (the
 * resume) or the record that searches (the position). Each side turns the values of the record into the values of the
 * hash in its own way ({@link VisHashCombinations}).
 */
public enum VisRecordType {
	/** The record that is found by the searches: the resume. */
	recordToSave(new Integer[] {1}, new Integer[] {1, 2}){

		public Integer[] fromIntegerField(CcpJsonFieldName field, CcpJsonRepresentation json, int threshold) {
			List<Integer> list = new ArrayList<>();

			Integer number = json.getAsIntegerNumber(field);

			for(int k = number; k <= threshold; k++) {
				list.add(k);
			}

			Integer[] array = list.toArray(new Integer[list.size()]);
			return array;
		}

		public Integer[] fromDoubleField(CcpJsonFieldName field, CcpJsonRepresentation json, double threshold, double increment) {

			List<Integer> list = new ArrayList<>();

			Double number = json.getAsDoubleNumber(field) / increment;

			double round = Math.ceil(number) * increment;

			for(double k = round; k <= threshold; k += increment) {
				list.add((int) k);
			}

			Integer[] array = list.toArray(new Integer[list.size()]);
			return array;
		}

		/**
		 * The area of the resume, as declared in it.
		 * @param json the resume
		 * @return the area of the resume
		 */
		public Integer[] getResumeTypes(CcpJsonRepresentation json) {
			Integer resumeType = json.getAsIntegerNumber(VisJsonCommonsFields.resumeType);
			Integer[] resumeTypes = new Integer[] {resumeType};
			return resumeTypes;
		}

		/**
		 * Every seniority whose range holds the years of experience of the resume, also when the json already carries
		 * the year the experience started.
		 * @param json the resume
		 * @return the seniorities of the resume
		 */
		public List<VisSeniorityType> getSeniorities(CcpJsonRepresentation json) {
			int yearsOfExperience = this.getYearsOfExperience(json);
			CcpJsonRepresentation jsonWithYearsOfExperience = json.put(VisJsonCommonsFields.experience, yearsOfExperience);
			List<VisSeniorityType> seniorities = new ArrayList<>();
			List<VisSeniorityType> allSeniorities = Arrays.asList(VisSeniorityType.values());
			for (VisSeniorityType seniority : allSeniorities) {
				boolean incompatibleExperience = false == seniority.matches(VisJsonCommonsFields.experience, jsonWithYearsOfExperience);
				if(incompatibleExperience) {
					continue;
				}
				seniorities.add(seniority);
			}
			return seniorities;
		}

		/**
		 * Reads the years of experience: the field itself on the input of the resume, or the current year minus the field
		 * once {@code VisJsonTransformerExperienceStartYear} turned it into the year the experience started.
		 * @param json the resume
		 * @return the years of experience
		 */
		private int getYearsOfExperience(CcpJsonRepresentation json) {
			Integer experience = json.getAsIntegerNumber(VisJsonCommonsFields.experience);
			boolean isYearsOfExperience = experience <= MAX_YEARS_OF_EXPERIENCE;

			if(isYearsOfExperience) {
				return experience;
			}

			CcpTimeDecorator now = new CcpTimeDecorator();
			int currentYear = now.getYear();
			int yearsOfExperience = currentYear - experience;
			return yearsOfExperience;
		}
	},
	/** The record that searches: the position. */
	recordToSearch(new Integer[] {1, 2}, new Integer[] {1}){

		public Integer[] fromIntegerField(CcpJsonFieldName field, CcpJsonRepresentation json, int threshold) {
			List<Integer> list = new ArrayList<>();

			Integer number = json.getAsIntegerNumber(field);

			for(int k = number; k >= threshold; k--) {
				list.add(k);
			}

			Integer[] array = list.toArray(new Integer[list.size()]);
			return array;
		}

		public Integer[] fromDoubleField(CcpJsonFieldName field, CcpJsonRepresentation json, double threshold, double increment) {

			List<Integer> list = new ArrayList<>();

			Double number = json.getAsDoubleNumber(field) / increment;

			double round = Math.floor(number) * increment;

			for(double k = round; k >= threshold; k -= increment) {
				list.add((int) k);
			}

			Integer[] array = list.toArray(new Integer[list.size()]);
			return array;
		}

		/**
		 * Every area of resume: the position does not declare one, so it searches the resumes of all of them.
		 * @param json the position
		 * @return the areas accepted by {@code VisJsonCommonsFields.resumeType}
		 */
		public Integer[] getResumeTypes(CcpJsonRepresentation json) {
			return ALL_RESUME_TYPES;
		}

		/**
		 * The seniority the position asks for.
		 * @param json the position
		 * @return the seniority of the position
		 */
		public List<VisSeniorityType> getSeniorities(CcpJsonRepresentation json) {
			VisSeniorityType seniority = json.getAsEnum(VisJsonCommonsFields.seniority, VisSeniorityType.class);
			List<VisSeniorityType> seniorities = Arrays.asList(seniority);
			return seniorities;
		}
	}
	;

	/** The largest number of years of experience: a larger {@code experience} is the year the experience started. */
	private static final int MAX_YEARS_OF_EXPERIENCE = 70;

	/** The areas accepted by {@code VisJsonCommonsFields.resumeType}. */
	private static final Integer[] ALL_RESUME_TYPES = new Integer[] {1, 2, 3, 4};

	private final Integer[] whenFalse;
	private final Integer[] whenTrue;

	private VisRecordType(Integer[] whenFalse, Integer[] whenTrue) {
		this.whenFalse = whenFalse;
		this.whenTrue = whenTrue;
	}



	public Integer[] fromBooleanField(CcpJsonFieldName field, CcpJsonRepresentation json) {
		boolean asBoolean = json.getAsBoolean(field);
		if(asBoolean) {
			return this.whenTrue;
		}



		return this.whenFalse;
	}

	public abstract Integer[] fromIntegerField(CcpJsonFieldName field, CcpJsonRepresentation json, int threshold);

	public abstract Integer[] fromDoubleField(CcpJsonFieldName field, CcpJsonRepresentation json, double threshold, double increment);

	/**
	 * The values of {@code resumeType} in the hashes of the record.
	 * @param json the record
	 * @return the areas
	 */
	public abstract Integer[] getResumeTypes(CcpJsonRepresentation json);

	/**
	 * The values of {@code seniority} in the hashes of the record.
	 * @param json the record
	 * @return the seniorities
	 */
	public abstract List<VisSeniorityType> getSeniorities(CcpJsonRepresentation json);

}
