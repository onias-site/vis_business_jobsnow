package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.vis.business.resume.VisResumeLinkedinEmails;
import com.vis.entities.VisEntityResumeViewsOfTheMonth;
import com.vis.entities.VisEntityViewedResume;

/**
 * The parents of a view of a resume ({@code vis_viewed_resume}) among the views of the month of the resumes
 * ({@code VisEntityResumeViewsOfTheMonth}): one per e-mail that uses the viewed LinkedIn address (read by id in
 * {@code vis_resume_linkedin}; an address of no resume has no parent), in the month of the view. The month is the one of
 * the {@code date} of the view ({@code dd/MM/yyyy HH:mm:ss}, filled at every save by the decorator of
 * {@code @CcpEntityGrouperChild}), so the stored view gives the month of its previous save; with no date, the current
 * month.
 */
public class VisResumeViewsOfTheMonthCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> {

	/** The single instance. */
	public static final VisResumeViewsOfTheMonthCombinations INSTANCE = new VisResumeViewsOfTheMonthCombinations();

	private VisResumeViewsOfTheMonthCombinations() {}

	/**
	 * One combination per e-mail of the viewed address: the e-mail (its SHA-1) and the month of the view.
	 * @param json the view, its LinkedIn address as it came
	 * @return the combinations
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		String monthYear = getMonthYear(json);
		String linkedinAddress = json.getAsString(VisEntityViewedResume.Fields.linkedin);
		CcpJsonRepresentation addressKey = VisResumeLinkedinEmails.getAddressKey(linkedinAddress);
		List<String> hashedEmails = VisResumeLinkedinEmails.getEmails(addressKey);
		List<CcpJsonRepresentation> combinations = new ArrayList<>();
		for (String hashedEmail : hashedEmails) {
			CcpJsonRepresentation combinationWithEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityResumeViewsOfTheMonth.Fields.email, hashedEmail);
			CcpJsonRepresentation combination = combinationWithEmail.put(VisEntityResumeViewsOfTheMonth.Fields.monthYear, monthYear);
			combinations.add(combination);
		}
		return combinations;
	}

	/**
	 * The month of the view, as {@code yyyyMM}.
	 * @param json the view
	 * @return the month of its {@code date}, or the current month when it has none
	 */
	private static String getMonthYear(CcpJsonRepresentation json) {
		boolean noDate = false == json.containsAllFields(VisEntityViewedResume.Fields.date);

		if(noDate) {
			return CcpEntityExpurgableOptions.monthly.getFormattedDate();
		}

		// dd/MM/yyyy HH:mm:ss
		String date = json.getAsString(VisEntityViewedResume.Fields.date);
		String year = date.substring(6, 10);
		String month = date.substring(3, 5);
		String monthYear = year + month;
		return monthYear;
	}
}
