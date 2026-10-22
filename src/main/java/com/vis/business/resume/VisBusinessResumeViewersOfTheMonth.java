package com.vis.business.resume;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.business.recruiter.resumes.VisBusinessViewedResumesOfTheMonth;
import com.vis.entities.VisEntityResumeViewersOfTheMonth;

/**
 * The domains of the recruiters who viewed the resume of the candidate in a month (the screen "Quem me viu"), read by id
 * (OQL): the record of the resume in the month in {@code vis_resume_viewers_of_the_month}, its period given in
 * {@code disposablePeriod}. The record is read straight from the database: the copy that the disposable entities keep in
 * {@code jn_disposable_record} has only the last month written, and would answer a month with no view with another one.
 *
 * <p>Input: {@code email} (the candidate) and {@code month} ({@code yyyyMM}). Output: {@code month} and
 * {@code viewers}, one item per domain ({@code domain} and {@code views}, the recruiters of that domain who viewed the
 * resume for the first time in the month), from the most viewed to the least, ties by the domain; empty when nobody
 * viewed the resume in the month.
 */
public class VisBusinessResumeViewersOfTheMonth implements CcpBusiness {

	/** The input of this business, with its validation rules. */
	public static enum Fields implements CcpJsonFieldName {
		/** The candidate. */
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		email,

		/** The month of the views, as {@code yyyyMM} (the format of the monthly disposable entities). */
		@CcpJsonCopyFieldValidationsFrom(VisBusinessViewedResumesOfTheMonth.Fields.class)
		month,
	}

	/** The fields of the output. */
	public static enum ResponseFields implements CcpJsonFieldName {
		/** The domains that viewed the resume in the month. */
		viewers,
		/** How many recruiters of the domain viewed the resume in the month. */
		views,
	}

	/** The single instance. */
	public static final VisBusinessResumeViewersOfTheMonth INSTANCE = new VisBusinessResumeViewersOfTheMonth();

	private VisBusinessResumeViewersOfTheMonth() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String month = json.getAsString(Fields.month);
		CcpJsonRepresentation candidate = json.getJsonPiece(VisEntityResumeViewersOfTheMonth.Fields.email);
		CcpJsonRepresentation candidateWithHashedEmail = VisEntityResumeViewersOfTheMonth.ENTITY.getJsonWithHandledPrimaryKey(candidate);
		CcpEntityMetaData viewersDetails = VisEntityResumeViewersOfTheMonth.ENTITY.getEntityMetaData();
		// the transformer also keeps the original e-mail: only the hash calculates the id
		CcpJsonRepresentation candidateKey = viewersDetails.getOnlyExistingFields(candidateWithHashedEmail);
		CcpJsonRepresentation candidateKeyInTheMonth = candidateKey.put(JnJsonCommonsFields.disposablePeriod, month);
		CcpJsonRepresentation viewersOfTheMonth = viewersDetails.getOneByIdOrHandleItIfThisIdWasNotFound(candidateKeyInTheMonth, notFound -> CcpOtherConstants.EMPTY_JSON);
		List<String> domains = viewersOfTheMonth.getAsStringList(VisEntityResumeViewersOfTheMonth.Fields.domain);
		List<CcpJsonRepresentation> viewers = countByDomain(domains);
		CcpJsonRepresentation responseWithMonth = CcpOtherConstants.EMPTY_JSON.put(Fields.month, month);
		CcpJsonRepresentation response = responseWithMonth.put(ResponseFields.viewers, viewers);
		return response;
	}

	/**
	 * Counts the items of each domain.
	 * @param domains one item per recruiter who viewed the resume
	 * @return {@code domain} and {@code views} of each domain, the most viewed first, ties by the domain
	 */
	private static List<CcpJsonRepresentation> countByDomain(List<String> domains) {
		Map<String, Integer> viewsByDomain = new TreeMap<>();
		for (String domain : domains) {
			viewsByDomain.merge(domain, 1, Integer::sum);
		}
		List<CcpJsonRepresentation> viewers = new ArrayList<>();
		for (Map.Entry<String, Integer> domainViews : viewsByDomain.entrySet()) {
			String domain = domainViews.getKey();
			Integer views = domainViews.getValue();
			CcpJsonRepresentation viewerWithDomain = CcpOtherConstants.EMPTY_JSON.put(VisEntityResumeViewersOfTheMonth.Fields.domain, domain);
			CcpJsonRepresentation viewer = viewerWithDomain.put(ResponseFields.views, views);
			viewers.add(viewer);
		}
		// the sort is stable: the domains with the same count stay in the alphabetical order of the map
		Comparator<CcpJsonRepresentation> byViews = Comparator.comparing(viewer -> viewer.getAsIntegerNumber(ResponseFields.views));
		Comparator<CcpJsonRepresentation> mostViewedFirst = byViews.reversed();
		viewers.sort(mostViewedFirst);
		return viewers;
	}
}
