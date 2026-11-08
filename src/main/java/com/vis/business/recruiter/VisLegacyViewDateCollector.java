package com.vis.business.recruiter;

import java.util.Map;
import java.util.function.Consumer;

import com.ccp.decorators.CcpJsonRepresentation;

/**
 * Consumer of the documents of the legacy index {@code visualizacao_de_curriculo} of
 * {@link VisBusinessLoadRecruitersLegacyViews}: keeps, for the e-mail of each recruiter, the most recent moment of a view.
 * A document without the moment is left out.
 */
class VisLegacyViewDateCollector implements Consumer<CcpJsonRepresentation> {

	/** Receives the most recent view of each e-mail, as found. */
	private final Map<String, Long> lastViewByEmail;

	/**
	 * Keeps where the views go.
	 * @param lastViewByEmail receives the most recent view of each e-mail, as found
	 */
	VisLegacyViewDateCollector(Map<String, Long> lastViewByEmail) {
		this.lastViewByEmail = lastViewByEmail;
	}

	/**
	 * Keeps the moment of the view when it is the most recent of the e-mail.
	 * @param json the legacy document
	 */
	public void accept(CcpJsonRepresentation json) {
		boolean noMoment = false == json.containsAllFields(VisBusinessLoadRecruitersLegacyViews.LegacyFields.email, VisBusinessLoadRecruitersLegacyViews.LegacyFields.dataDeInclusao);
		if(noMoment) {
			return;
		}
		String email = json.getAsString(VisBusinessLoadRecruitersLegacyViews.LegacyFields.email);
		Long moment = json.getAsLongNumber(VisBusinessLoadRecruitersLegacyViews.LegacyFields.dataDeInclusao);
		this.lastViewByEmail.merge(email, moment, Math::max);
	}
}
