package com.vis.business.recruiter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.query.CcpQueryExecutor;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.vis.entities.VisEntityRecruiter;

/**
 * Writes on each active recruiter of {@link VisEntityRecruiter} the most recent view of a resume they made in the legacy
 * JobsNow ({@code lastLegacyView}), read from the legacy index {@code visualizacao_de_curriculo} (the e-mail of the
 * recruiter in {@code email}, the moment in milliseconds in {@code dataDeInclusao}). The daily e-mail of resumes gives the
 * second places, in turns, to the recruiters who have it.
 *
 * <p>A batch process run by hand where the legacy index exists (the local Elasticsearch); production receives the
 * recruiters by a script of {@code documentation/deploy}. Running it again changes nothing: a date is written only when it
 * is more recent than the stored one.
 *
 * <p>Input: nothing. Output: {@code legacyEmails}, the distinct e-mails of the index, and {@code updated}, how many
 * recruiters got the date.
 */
public class VisBusinessLoadRecruitersLegacyViews implements CcpBusiness {

	/** The legacy index, its fields and the output. */
	enum LegacyFields implements CcpJsonFieldName{
		/** The legacy index of the views of resumes. */
		visualizacao_de_curriculo,
		/** The e-mail of the recruiter who viewed. */
		email,
		/** The moment of the view, in milliseconds. */
		dataDeInclusao,
		/** How long each scroll context of the reading stays open. */
		SCROLL_TIME { public String getValue() { return "1m"; } },
		/** How many distinct e-mails the index has. */
		legacyEmails,
	}

	/** The size of each page of the reading. */
	private static final int PAGE_SIZE = 10000;

	/** The single instance: the business keeps no state. */
	public static final VisBusinessLoadRecruitersLegacyViews INSTANCE = new VisBusinessLoadRecruitersLegacyViews();

	private VisBusinessLoadRecruitersLegacyViews() {}

	/**
	 * Reads the legacy views and writes the most recent one of each recruiter.
	 * @param json nothing
	 * @return the counters
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		CcpQueryOptions query = CcpQueryOptions.INSTANCE.matchAll();
		String legacyIndex = LegacyFields.visualizacao_de_curriculo.getValue();
		String[] legacyIndices = {legacyIndex};
		String scrollTime = LegacyFields.SCROLL_TIME.getValue();
		String emailField = LegacyFields.email.getValue();
		String momentField = LegacyFields.dataDeInclusao.getValue();

		Map<String, Long> lastViewByEmail = new HashMap<>();
		Consumer<CcpJsonRepresentation> collector = new VisLegacyViewDateCollector(lastViewByEmail);
		queryExecutor.consumeQueryResult(query, legacyIndices, scrollTime, PAGE_SIZE, collector, emailField, momentField);

		CcpJsonRepresentation datesByEmail = VisBusinessSaveRecruitersActivity.getDatesByNormalizedEmail(lastViewByEmail, VisEntityRecruiter.Sources.legacy);
		CcpJsonRepresentation saveInput = VisBusinessSaveRecruitersActivity.input(VisEntityRecruiter.Fields.lastLegacyView, datesByEmail);
		CcpJsonRepresentation saved = VisBusinessSaveRecruitersActivity.INSTANCE.execute(saveInput);
		int legacyEmailsCount = lastViewByEmail.size();
		CcpJsonRepresentation withLegacyEmails = json.put(LegacyFields.legacyEmails, legacyEmailsCount);
		Integer updatedCount = saved.getAsIntegerNumber(VisBusinessSaveRecruitersActivity.JsonFieldNames.updated);
		CcpJsonRepresentation result = withLegacyEmails.put(VisBusinessSaveRecruitersActivity.JsonFieldNames.updated, updatedCount);
		return result;
	}
}
