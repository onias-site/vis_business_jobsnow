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
 * Virtual entity whose id identifies the group of {@code JnEntityGrouper} that counts the records of an entity by ddd:
 * its child is a JSON text with, for each ddd, how many records of the main side list it now
 * ({@code @CcpEntityGrouperChildCounter} with {@code JnGrouperChildCurrentCounterEntityBeforeWrite} and
 * {@code VisDddCounterKeys}): the insertion and the reactivation add 1, the deactivation and the delete subtract 1, and a
 * save that changes the ddds moves the count. A record with several ddds counts once in each. Its only field always holds
 * {@code ddd}; the group of each counted entity ({@link VisEntityResume}, {@link VisEntityViewedResume}) is told apart by
 * its {@code childName}. The totals of the folders of the screen Supply X Demand. Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualDddCounter.Fields.class)
public class VisEntityVirtualDddCounter {

	/** The entity {@code vis_virtual_ddd_counter}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualDddCounter.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code counter} field: the primary key, always {@code ddd}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 50)
		counter,
		;
	}

	/**
	 * The combination of the group: {@code {"counter": "ddd"}}.
	 * @return the combination
	 */
	public static CcpJsonRepresentation getCombination() {
		String counterName = VisEntityResume.Fields.ddd.getValue();
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
