package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDataTransfer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDataTransfers;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityTwin;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityDataTransferType;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.skill.VisBusinessSkillFixHierarchyItem;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.fields.validation.VisSkillFixHierarchyTypes;

/**
 * Represents each skill of a pending skill hierarchy fix request, one record per skill: the same
 * fields as {@link VisEntitySkillFixHierarchyPending}, but without {@code email} and {@code description}, and {@code skill} holds
 * a single value and is part of the primary key (parent + type + skill).
 * Has the twin entity vis_skill_fix_hierarchy_item_rejected for the rejected items. The approved items are
 * transferred to {@link VisEntitySkillFixHierarchyItemApproved}, and once the transfer happens
 * {@link VisBusinessSkillFixHierarchyItem} runs.
 * 1-hour cache.
 */
@CcpEntityTwin(
		twinEntityName = "vis_skill_fix_hierarchy_item_rejected",
		bulkExecutorClass = JnExecuteBulkOperation.class,
		functionToDeleteKeysInTheCacheClass = JnDeleteKeysFromCache.class
		)
@CcpEntityDataTransfers({
		@CcpEntityDataTransfer(operationType = CcpEntityDataTransferType.afterTransferDataFromMainEntity, targetEntity = VisEntitySkillFixHierarchyItemApproved.class, execute = {VisBusinessSkillFixHierarchyItem.class}, transferHandlers = {}),
})
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntitySkillFixHierarchyItemPending.Fields.class)
public class VisEntitySkillFixHierarchyItemPending implements CcpEntityConfigurator {

	/** The entity {@code vis_skill_fix_hierarchy_item_pending}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntitySkillFixHierarchyItemPending.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code parent} field: validated as in {@code VisJsonCommonsFields}, part of the primary key. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldPrimaryKey
		parent,

		/** The {@code skill} field: validated as in {@code VisJsonCommonsFields}, part of the primary key. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpEntityFieldPrimaryKey
		skill,

		/** The {@code type} field: text, part of the primary key. */
		@CcpJsonFieldTypeString(allowedValuesEnum = VisSkillFixHierarchyTypes.class)
		@CcpEntityFieldPrimaryKey
		type,

	}
}
