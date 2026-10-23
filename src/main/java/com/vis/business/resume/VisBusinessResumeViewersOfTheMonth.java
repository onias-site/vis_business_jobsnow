package com.vis.business.resume;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnGroupers;
import com.vis.business.recruiter.resumes.VisBusinessViewedResumesOfTheMonth;
import com.vis.entities.VisEntityResumeViewsOfTheMonth;
import com.vis.entities.VisEntityViewedResume;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * The domains of the recruiters who viewed the resume of the candidate in a month (the screen "Quem me viu"), read by id
 * (OQL): the group of counters of the resume in the month in {@code jn_grouper} (parent
 * {@code vis_resume_views_of_the_month}, from the e-mail of the candidate and the month; child {@code vis_viewed_resume}),
 * whose only child holds one counter per domain, filled by {@code @CcpEntityGrouperChildCounter} of
 * {@code vis_viewed_resume}.
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
		CcpJsonRepresentation candidate = json.getJsonPiece(VisEntityResumeViewsOfTheMonth.Fields.email);
		CcpJsonRepresentation candidateInTheMonth = candidate.put(VisEntityResumeViewsOfTheMonth.Fields.monthYear, month);
		CcpJsonRepresentation candidateWithHashedEmail = VisEntityResumeViewsOfTheMonth.ENTITY.getJsonWithHandledPrimaryKey(candidateInTheMonth);
		CcpEntityMetaData viewsDetails = VisEntityResumeViewsOfTheMonth.ENTITY.getEntityMetaData();
		// the transformer also keeps the original e-mail: only the hash calculates the id
		CcpJsonRepresentation candidateKey = viewsDetails.getOnlyExistingFields(candidateWithHashedEmail);
		String parentId = VisEntityResumeViewsOfTheMonth.ENTITY.calculateId(candidateKey);
		CcpEntityMetaData viewDetails = VisEntityViewedResume.ENTITY.getEntityMetaData();
		CcpJsonRepresentation viewsByDomain = JnGroupers.getCounters(viewsDetails.entityName, viewDetails.entityName, parentId);
		List<CcpJsonRepresentation> viewers = sortByViews(viewsByDomain);
		CcpJsonRepresentation responseWithMonth = CcpOtherConstants.EMPTY_JSON.put(Fields.month, month);
		CcpJsonRepresentation response = responseWithMonth.put(ResponseFields.viewers, viewers);
		return response;
	}

	/**
	 * The domains and their counters, the most viewed first.
	 * @param viewsByDomain the counter of each domain
	 * @return {@code domain} and {@code views} of each domain, the most viewed first, ties by the domain
	 */
	private static List<CcpJsonRepresentation> sortByViews(CcpJsonRepresentation viewsByDomain) {
		Set<String> unsortedDomains = viewsByDomain.fieldSet();
		Set<String> domains = new TreeSet<>(unsortedDomains);
		List<CcpJsonRepresentation> viewers = new ArrayList<>();
		for (String domain : domains) {
			CcpFieldName domainField = new CcpFieldName(domain);
			Integer views = viewsByDomain.getAsIntegerNumber(domainField);
			CcpJsonRepresentation viewerWithDomain = CcpOtherConstants.EMPTY_JSON.put(VisJsonCommonsFields.domain, domain);
			CcpJsonRepresentation viewer = viewerWithDomain.put(ResponseFields.views, views);
			viewers.add(viewer);
		}
		// the sort is stable: the domains with the same count stay in the alphabetical order of the set
		Comparator<CcpJsonRepresentation> byViews = Comparator.comparing(viewer -> viewer.getAsIntegerNumber(ResponseFields.views));
		Comparator<CcpJsonRepresentation> mostViewedFirst = byViews.reversed();
		viewers.sort(mostViewedFirst);
		return viewers;
	}
}
