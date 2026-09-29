package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Virtual entity used to calculate a composite hash that represents a specific combination of
 * resume/position matching attributes (seniority, availability, PCD, compensation type and value,
 * skill synonyms). The matching system uses this hash internally to check compatibility without
 * complex searches. Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(classReferenceWithTheFields = JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(classReferenceWithTheFields = VisEntityVirtualHashGrouper.Fields.class)
public class VisEntityVirtualHashGrouper{

	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualHashGrouper.class).entityInstance;

	public static enum Fields implements CcpJsonFieldName{
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		seniority, 
		@CcpJsonFieldValidatorArray
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		synonym,
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		disponibility, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		pcd, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeNumber(minValue = 1000)
		moneyValue, 
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(allowedValuesEnum = VisMoneyTypes.class)
		moneyType,
		;
	}
}
