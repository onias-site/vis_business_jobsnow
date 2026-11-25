package com.vis.entities;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

/**
 * The virtual entity that gives the id of the groups of the candidates interested in a position in an hour (since
 * 2026-11-24): one group of {@code jn_grouper} per position and hour of sending ({@code yyyyMMddHH}), whose children are
 * the ids of the resumes ({@code vis_resume}) of the candidates who, logged in, asked to send their resume to the position
 * ({@link VisEntityPositionInterest}) before that hour. Not a table of its own.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualPositionInterestedResumes.Fields.class)
public class VisEntityVirtualPositionInterestedResumes {

	/** The entity, used only to calculate the id of the groups and to name them. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualPositionInterestedResumes.class).entityInstance;

	/** The fields of the entity. */
	public static enum Fields implements CcpJsonFieldName{
		/** The id of the position ({@code vis_position}). */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
		position,

		/** The hour of the sending, {@code yyyyMMddHH}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 10, maxLength = 10)
		sendingHour,
		;
	}

	/**
	 * The parent id of the group of a position in an hour, the {@code parentId} of its group in {@code jn_grouper}.
	 * @param positionId the id of the position
	 * @param sendingHour the hour, {@code yyyyMMddHH}
	 * @return the id of the group
	 */
	public static String getParentId(String positionId, String sendingHour) {
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON
				.put(Fields.position, positionId)
				.put(Fields.sendingHour, sendingHour);
		String parentId = ENTITY.calculateId(combination);
		return parentId;
	}
}
