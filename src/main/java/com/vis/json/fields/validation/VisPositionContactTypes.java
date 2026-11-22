package com.vis.json.fields.validation;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;

/**
 * Types accepted by the {@code type} of each {@code contact} of the position ({@code VisJsonFieldsPositionContact}); each
 * one validates its own {@code value} ({@code VisJsonValidatorPositionContactValue}).
 */
public enum VisPositionContactTypes {
	/** A WhatsApp number: 10 to 15 digits (country and area codes included), optionally after a {@code +}. */
	whatsapp {
		public boolean isValid(String value) {
			CcpStringDecorator valueDecorator = new CcpStringDecorator(value);
			CcpTextDecorator valueText = valueDecorator.text();
			boolean valid = valueText.regexMatches("^\\+?[0-9]{10,15}$");
			return valid;
		}
	},
	/** An e-mail address. */
	email {
		public boolean isValid(String value) {
			CcpStringDecorator valueDecorator = new CcpStringDecorator(value);
			CcpEmailDecorator valueEmail = valueDecorator.email();
			boolean valid = valueEmail.isValid();
			return valid;
		}
	},
	/** A web address, starting with {@code http://} or {@code https://}, with no blanks. */
	link {
		public boolean isValid(String value) {
			CcpStringDecorator valueDecorator = new CcpStringDecorator(value);
			CcpTextDecorator valueText = valueDecorator.text();
			boolean valid = valueText.regexMatches("^https?://[^\\s/$.?#][^\\s]*$");
			return valid;
		}
	}
	;

	/**
	 * Tells whether the value is a valid contact of this type.
	 * @param value the value of the contact
	 * @return {@code true} when valid
	 */
	public abstract boolean isValid(String value);
}
