package com.vis.json.transformers;

import java.util.function.Function;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.vis.entities.VisEntityViewedResume;

/**
 * The key under which a view of a resume ({@code vis_viewed_resume}) is counted among the views of the month of the
 * resume: the domain of the e-mail of the recruiter, in lower case (the same company may type its domain in capitals).
 */
public class VisViewedResumeRecruiterDomain implements Function<CcpJsonRepresentation, String> {

	/** The single instance. */
	public static final VisViewedResumeRecruiterDomain INSTANCE = new VisViewedResumeRecruiterDomain();

	private VisViewedResumeRecruiterDomain() {}

	/**
	 * The domain of the recruiter of the view.
	 * @param json the view, its e-mail as it came
	 * @return the domain, in lower case
	 */
	public String apply(CcpJsonRepresentation json) {
		String recruiterEmail = json.getAsString(VisEntityViewedResume.Fields.email);
		CcpStringDecorator recruiterEmailText = new CcpStringDecorator(recruiterEmail);
		CcpEmailDecorator recruiterEmailDecorator = recruiterEmailText.email();
		String domain = recruiterEmailDecorator.getDomain();
		String lowerCaseDomain = domain.toLowerCase();
		return lowerCaseDomain;
	}
}
