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
import com.jn.json.fields.validation.JnJsonCommonsFields;

/**
 * Virtual entity whose id identifies the group of {@code JnEntityGrouper} that counts the records of an entity by the
 * month of their insertion: its child is a JSON text with, for each {@code yyyyMM}, how many records inserted in that
 * month are active now ({@code @CcpEntityGrouperChildCounter} with {@code JnGrouperChildCurrentCounterEntityBeforeWrite}
 * and {@code JnInsertionYearMonthCounterKeys}): the insertion and the reactivation add 1, the deactivation and the
 * delete subtract 1. Its only field always holds {@code insertionDate}; the group of each counted entity
 * ({@link VisEntityResume}, {@link VisEntityViewedResume}) is told apart by its {@code childName}. Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualInsertionMonthCounter.Fields.class)
public class VisEntityVirtualInsertionMonthCounter {

	/** The entity {@code vis_virtual_insertion_month_counter}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualInsertionMonthCounter.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code counter} field: the primary key, always {@code insertionDate}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 50)
		counter,
		;
	}

	/**
	 * The combination of the group: {@code {"counter": "insertionDate"}}.
	 * @return the combination
	 */
	public static CcpJsonRepresentation getCombination() {
		String counterName = JnJsonCommonsFields.insertionDate.getValue();
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(Fields.counter, counterName);
		return combination;
	}

	/**
	 * The parent id of the group, the {@code parentId} of its group in {@code JnEntityGrouper}.
	 * @return the parent id
	 */
	public static String getParentId() {
		CcpJsonRepresentation combination = getCombination();
		String parentId = ENTITY.calculateId(combination);
		return parentId;
	}
}
