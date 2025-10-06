package com.vis.json.fields.validation;

import java.lang.reflect.Field;

import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidator;
import com.ccp.json.validations.fields.enums.CcpJsonFieldType;
import com.ccp.json.validations.fields.enums.CcpJsonFieldTypeError;
import com.ccp.json.validations.fields.interfaces.CcpJsonFieldValidatorCatalog;
import com.ccp.json.validations.fields.interfaces.CcpJsonFieldValidatorInterface;

public class VisJsonValidationsByFieldName implements CcpJsonFieldValidatorCatalog {

	public CcpJsonFieldValidatorInterface[] getValidations(Field field) {
		try {
			String fieldName = field.getName();
			Field declaredField = VisJsonFieldsValidationCatalog.class.getDeclaredField(fieldName);
			CcpJsonFieldValidator annotation = declaredField.getAnnotation(CcpJsonFieldValidator.class);
			CcpJsonFieldType type = annotation.type();
			CcpJsonFieldTypeError[] errorTypes = type.getErrorTypes();
			return errorTypes;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
