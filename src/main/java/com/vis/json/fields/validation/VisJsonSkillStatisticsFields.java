package com.vis.json.fields.validation;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberInteger;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.vis.json.transformers.VisSeniorityType;

/**
 * Validation rules of the fields of the statistics of skills, whose values differ from the same fields of the resume: the
 * {@code ddd} also accepts {@link #NO_DDD} and {@link #HOME_OFFICE}, and the {@code seniority} is one of
 * {@link VisSeniorityType}, {@code unknown} included. An entity takes these rules through
 * {@code @CcpJsonCopyFieldValidationsFrom(VisJsonSkillStatisticsFields.class)}.
 */
public enum VisJsonSkillStatisticsFields implements CcpJsonFieldName {

	/** The {@code ddd} field: {@link #NO_DDD}, {@link #HOME_OFFICE} or the code of a region. */
	@CcpJsonFieldTypeNumberUnsigned(minValue = 0, maxValue = 99)
	ddd,

	/** The {@code seniority} field: one of {@code VisSeniorityType}. */
	@CcpJsonFieldTypeString(allowedValuesEnum = VisSeniorityType.class)
	seniority,

	/** The {@code skill} field: a single skill. */
	@CcpJsonFieldTypeString(minLength = 1, maxLength = 50)
	skill,

	/** The {@code yearMonth} field: a month, as {@code yyyyMM}. */
	@CcpJsonFieldTypeString(exactLength = 6)
	yearMonth,

	/** The {@code total} field: how many records were counted. */
	@CcpJsonFieldTypeNumberInteger(minValue = 0)
	total,
	;

	/** The {@code ddd} of a position whose region could not be told. */
	public static final int NO_DDD = 0;

	/** The {@code ddd} of a home office position. */
	public static final int HOME_OFFICE = 10;
}
