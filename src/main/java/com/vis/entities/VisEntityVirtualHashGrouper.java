package com.vis.entities;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.annotations.CcpEntitySpecifications;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumber;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.json.fields.validation.VisJsonCommonsFields;

@CcpEntityCache(3600)
@CcpEntitySpecifications(
		entityFieldsTransformers = JnJsonTransformersFieldsEntityDefault.class,
		entityValidation = VisEntityVirtualHashGrouper.Fields.class,
		afterDeleteRecord = {},
		beforeSaveRecord = {},
		afterSaveRecord = {},
		flow = {}
)
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
		@CcpJsonFieldTypeString(allowedValues = {"CLT", "BTC", "PJ"})
		moneyType,
		;
	}
}
