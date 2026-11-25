package com.vis.business.recruiter.resumes;

import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.jn.entities.JnEntitySystemParameter;
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * The records of {@link JnEntitySystemParameter} about the daily e-mail of resumes to the recruiters
 * ({@link VisBusinessSendResumesToRecruiters}): each item is a parameter whose only line is its value, so that it can be
 * changed in the database, with no deploy (since 2026-11-24).
 */
public enum VisRecruiterResumesSystemParameters {
	/** How many recruiters receive the e-mail each day (2000 when the parameter was created). */
	recruitersPerDay {
		String getDefaultValue() {
			String defaultValue = String.valueOf(DEFAULT_RECRUITERS_PER_DAY);
			return defaultValue;
		}
	};

	/** The value of {@link #recruitersPerDay} seeded in the database. */
	public static final int DEFAULT_RECRUITERS_PER_DAY = 2000;

	/** The value seeded for the parameter. */
	abstract String getDefaultValue();

	/**
	 * The name of the record of this item: the name of the enum followed by the name of the item.
	 * @return the {@code systemMessageName}
	 */
	public String getSystemMessageName() {
		Class<?> declaringClass = this.getDeclaringClass();
		String className = declaringClass.getName();
		String systemMessageName = className + "." + this.name();
		return systemMessageName;
	}

	/**
	 * The value of the parameter as an integer, read by id (cached for an hour by the entity).
	 * @return the first line of {@code message}, as a number
	 */
	public int getAsInteger() {
		String systemMessageName = this.getSystemMessageName();
		List<String> values = JnEntitySystemParameter.getValues(systemMessageName);
		String firstValue = values.get(0);
		String trimmedValue = firstValue.trim();
		int value = Integer.parseInt(trimmedValue);
		return value;
	}

	/**
	 * The record of the parameter, with its seeded value.
	 * @return the record of {@code jn_system_parameter}
	 */
	public CcpJsonRepresentation getRecord() {
		String systemMessageName = this.getSystemMessageName();
		String defaultValue = this.getDefaultValue();
		CcpJsonRepresentation parameterId = JnEntitySystemParameter.getKey(systemMessageName);
		List<String> values = List.of(defaultValue);
		CcpJsonRepresentation parameterRecord = parameterId.put(JnJsonCommonsFields.message, values);
		return parameterRecord;
	}

	/**
	 * The seeds of every parameter of this enum.
	 * @return the records of {@code jn_system_parameter}
	 */
	public static List<CcpBulkItem> getSeeds() {
		CcpJsonRepresentation recruitersPerDayRecord = recruitersPerDay.getRecord();
		List<CcpBulkItem> seeds = JnEntitySystemParameter.ENTITY.toBulkItems(recruitersPerDayRecord, CcpBulkEntityOperationType.create);
		return seeds;
	}
}
