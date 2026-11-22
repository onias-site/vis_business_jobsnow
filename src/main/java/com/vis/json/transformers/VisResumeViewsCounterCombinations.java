package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.business.resume.VisResumeLinkedinEmails;
import com.vis.entities.VisEntityViewedResume;
import com.vis.entities.VisEntityVirtualResumeViewsCounter;

/**
 * The parents of a view of a resume ({@code vis_viewed_resume}) among the counters of views of each resume
 * ({@code VisEntityVirtualResumeViewsCounter}): one per e-mail that uses the viewed LinkedIn address (read by id in
 * {@code vis_resume_linkedin}; an address of no resume has no parent), whatever the month of the view.
 */
public class VisResumeViewsCounterCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>> {

	/** The single instance: the class keeps no state. */
	public static final VisResumeViewsCounterCombinations INSTANCE = new VisResumeViewsCounterCombinations();

	private VisResumeViewsCounterCombinations() {}

	/**
	 * One combination per e-mail of the viewed address.
	 * @param json the view, its LinkedIn address as it came
	 * @return the combinations, {@code {"email": <SHA-1 of the e-mail>}}
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		String linkedinAddress = json.getAsString(VisEntityViewedResume.Fields.linkedin);
		CcpJsonRepresentation addressKey = VisResumeLinkedinEmails.getAddressKey(linkedinAddress);
		List<String> hashedEmails = VisResumeLinkedinEmails.getEmails(addressKey);
		List<CcpJsonRepresentation> combinations = new ArrayList<>();
		for (String hashedEmail : hashedEmails) {
			CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualResumeViewsCounter.Fields.email, hashedEmail);
			combinations.add(combination);
		}
		return combinations;
	}
}
