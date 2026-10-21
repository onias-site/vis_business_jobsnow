package com.vis.business.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.vis.entities.VisEntityRecruiter;

/**
 * Turns an e-mail and its source into the record of {@link VisEntityRecruiter} ready for a bulk write, which goes
 * around the decorators of the entity: the e-mail loses its accents, goes to lower case and loses the spaces around it;
 * then the transformers of the entity fill {@code domain} and {@code professional} and the validation of the entity
 * runs over the result. The validation runs only here, after the transformers, because {@code domain} is required and is
 * calculated by them.
 *
 * <p>Input: {@code email} (as found, possibly with accents and capital letters) and {@code source}.
 * @throws com.ccp.json.validations.global.engine.CcpJsonValidationError when the e-mail is not a valid address of the
 * entity; the loads count it as invalid and go on with the next one
 */
public class VisBusinessPrepareRecruiter implements CcpBusiness {

	/** The single instance: the business keeps no state. */
	public static final VisBusinessPrepareRecruiter INSTANCE = new VisBusinessPrepareRecruiter();

	private VisBusinessPrepareRecruiter() {}

	/**
	 * Normalizes the e-mail, transforms and validates the record.
	 * @param json the e-mail and the source
	 * @return the record of the recruiter
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String email = json.getAsString(VisEntityRecruiter.Fields.email);
		String lowerEmail = email.toLowerCase();
		String trimmedEmail = lowerEmail.trim();
		CcpStringDecorator emailText = new CcpStringDecorator(trimmedEmail);
		CcpEmailDecorator emailDecorator = emailText.email();
		CcpEmailDecorator emailWithoutAccents = emailDecorator.stripAccents();
		String normalizedEmail = emailWithoutAccents.content;

		CcpJsonRepresentation jsonWithNormalizedEmail = json.put(VisEntityRecruiter.Fields.email, normalizedEmail);
		CcpJsonRepresentation recruiter = VisEntityRecruiter.ENTITY.getHandledJson(jsonWithNormalizedEmail);
		VisEntityRecruiter.ENTITY.validateJson(recruiter);
		return recruiter;
	}
}
