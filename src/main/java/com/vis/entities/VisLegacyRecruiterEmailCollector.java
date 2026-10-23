package com.vis.entities;

import java.util.Set;
import java.util.function.Consumer;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Consumer of the documents of the legacy recruiter indices of {@link VisEntityRecruiter}: keeps the e-mail of each one
 * (the id of the document).
 */
class VisLegacyRecruiterEmailCollector implements Consumer<CcpJsonRepresentation> {

	/** Receives the e-mails. */
	private final Set<String> legacyEmails;

	/**
	 * Keeps where the e-mails go.
	 * @param legacyEmails receives the e-mails
	 */
	VisLegacyRecruiterEmailCollector(Set<String> legacyEmails) {
		this.legacyEmails = legacyEmails;
	}

	/**
	 * Keeps the e-mail of the document.
	 * @param json the legacy document
	 */
	public void accept(CcpJsonRepresentation json) {
		String legacyEmail = json.getAsString(VisJsonCommonsFields.id);
		this.legacyEmails.add(legacyEmail);
	}
}
