package com.vis.json.transformers;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.especifications.db.utils.entity.fields.CcpJsonTransformersDefaultEntityField;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Field transformer applied to the {@code experience} of the VisEntityResume entity while it is persisted. The front end
 * sends the years of experience; the database keeps the year the experience started, which is the current year of the
 * server minus those years (17 years sent in 2026 are stored as 2009). A json with no experience is returned as is.
 */
public class VisJsonTransformerExperienceStartYear implements CcpJsonTransformersDefaultEntityField {
	/** The single instance: the class keeps no state. */
	public final static VisJsonTransformerExperienceStartYear INSTANCE = new VisJsonTransformerExperienceStartYear();

	private VisJsonTransformerExperienceStartYear() {}

	/**
	 * Replaces the years of experience with the year the experience started.
	 * @param json the resume
	 * @return the resume with the year the experience started
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		boolean noExperience = false == json.containsAllFields(VisJsonCommonsFields.experience);

		if(noExperience) {
			return json;
		}

		Integer yearsOfExperience = json.getAsIntegerNumber(VisJsonCommonsFields.experience);
		CcpTimeDecorator now = new CcpTimeDecorator();
		int currentYear = now.getYear();
		int experienceStartYear = currentYear - yearsOfExperience;
		CcpJsonRepresentation jsonWithExperienceStartYear = json.put(VisJsonCommonsFields.experience, experienceStartYear);
		return jsonWithExperienceStartYear;
	}

	/**
	 * Tells that the result can not be part of a primary key: it depends on the current date.
	 * @return {@code false}
	 */
	public boolean canBePrimaryKey() {
		return false;
	}

	/**
	 * Returns the name of the transformed field.
	 * @return {@code experience}
	 */
	public String name() {
		String fieldName = VisJsonCommonsFields.experience.name();
		return fieldName;
	}
}
