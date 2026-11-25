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
 * The virtual entity that gives the id of the group of every candidate who sent the resume through a position (since
 * 2026-11-24): one group of {@code jn_grouper} per position, whose children are the ids of the resumes ({@code vis_resume})
 * of {@link VisEntityPositionInterest}, whatever the hour of the sending. Read by id by the edition of the position: a
 * position with candidates cannot change its title, since the interests keep only the hash of the e-mails and cannot be
 * written again under the id of the new title. Not a table of its own.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualPositionCandidates.Fields.class)
public class VisEntityVirtualPositionCandidates {

	/** The entity, used only to calculate the id of the groups and to name them. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualPositionCandidates.class).entityInstance;

	/** The fields of the entity. */
	public static enum Fields implements CcpJsonFieldName{
		/** The id of the position ({@code vis_position}). */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
		position,
		;
	}

	/**
	 * The parent id of the group of a position, the {@code parentId} of its group in {@code jn_grouper}.
	 * @param positionId the id of the position
	 * @return the id of the group
	 */
	public static String getParentId(String positionId) {
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(Fields.position, positionId);
		String parentId = ENTITY.calculateId(combination);
		return parentId;
	}
}
