package com.vis.json.fields.validation;

import java.lang.reflect.Field;
import java.util.function.Predicate;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.validations.fields.interfaces.CcpJsonFieldType;
import com.ccp.json.validations.fields.interfaces.CcpJsonFieldValidatorInterface;

/**
 * The type of a month asked for by the screens of statistics, pointed to by
 * {@code @CcpJsonFieldTypeCustom(VisYearMonthFieldType.class)}: six digits as {@code yyyyMM}, whose year goes from
 * {@link VisYearMonthCompatibility#MINIMUM_YEAR} up to the current year (America/Sao_Paulo) and whose month goes from 01
 * to 12. Any other value is an incompatible type (422).
 */
public class VisYearMonthFieldType implements CcpJsonFieldType {

	/** The single instance: the class keeps no state. */
	public final static VisYearMonthFieldType INSTANCE = new VisYearMonthFieldType();

	private VisYearMonthFieldType() {}

	public Predicate<CcpJsonRepresentation> evaluateCompatibleType(String fieldName) {
		VisYearMonthCompatibility compatibility = new VisYearMonthCompatibility(fieldName);
		return compatibility;
	}

	public boolean hasRuleExplanation(Field field) {
		return true;
	}

	public CcpJsonFieldValidatorInterface[] getErrorTypes() {
		return new CcpJsonFieldValidatorInterface[0];
	}

	public String name() {
		String name = VisYearMonthFieldType.class.getSimpleName();
		return name;
	}
}
