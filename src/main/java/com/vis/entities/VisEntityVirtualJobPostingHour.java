package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberUnsigned;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

/**
 * Virtual entity whose id identifies the group of {@code JnEntityGrouper} of one hour of one day of the job postings
 * saved by {@code /saveJobPosting} (since 2026-11-26): the child is a JSON text with the text of the job posting
 * ({@code description}) and its id. The hour is one of the hours still to come in the day of the saving, chosen by its
 * millisecond, and the hour 0 of the next day when saved at 23h ({@code VisJobPostingHourCombinations}). Not a table of its own;
 * cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualJobPostingHour.Fields.class)
public class VisEntityVirtualJobPostingHour {

	/** The entity {@code vis_virtual_job_posting_hour}, used only to calculate the id of the groups and to name them. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualJobPostingHour.class).entityInstance;

	/** The fields of the entity. */
	public static enum Fields implements CcpJsonFieldName{
		/** The day of the saving, {@code yyyyMMdd}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 8, maxLength = 8)
		day,
		/** The millisecond of the saving modulo 24: from 0 to 23. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeNumberUnsigned(maxValue = 23)
		hour,
		;
	}
}
