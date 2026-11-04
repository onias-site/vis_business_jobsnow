package com.vis.json.fields.validation;

import java.util.function.Predicate;

import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;
import com.ccp.decorators.CcpTimeDecorator;

/**
 * Tells whether a value is a month as {@code yyyyMM}: six digits, the first four a year from {@link #MINIMUM_YEAR} up to
 * the current year (America/Sao_Paulo), the last two a month from 1 to 12.
 */
class VisYearMonthCompatibility implements Predicate<CcpJsonRepresentation> {

	enum Fields implements CcpJsonFieldName {
		SIX_DIGITS {
			public String getValue() {
				return "^[0-9]{6}$";
			}
		},
	}

	/** The first year of the statistics. */
	static final int MINIMUM_YEAR = 2015;

	private final String fieldName;

	VisYearMonthCompatibility(String fieldName) {
		this.fieldName = fieldName;
	}

	public boolean test(CcpJsonRepresentation json) {
		CcpFieldName ccpFieldName = new CcpFieldName(this.fieldName);
		String value = json.getAsString(ccpFieldName);
		CcpStringDecorator decorator = new CcpStringDecorator(value);
		String sixDigits = Fields.SIX_DIGITS.getValue();
		CcpTextDecorator text = decorator.text();
		boolean notSixDigits = false == text.regexMatches(sixDigits);

		if(notSixDigits) {
			return false;
		}

		String yearText = value.substring(0, 4);
		String monthText = value.substring(4);
		int year = Integer.parseInt(yearText);
		int month = Integer.parseInt(monthText);
		CcpTimeDecorator now = new CcpTimeDecorator();
		int currentYear = now.getYear();
		boolean validYear = year >= MINIMUM_YEAR && year <= currentYear;
		boolean validMonth = month >= 1 && month <= 12;
		boolean compatible = validYear && validMonth;
		return compatible;
	}
}
