package com.vis.json.transformers;

import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.jn.entities.JnEntitySystemMessage;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityViewedResume;

/**
 * The key under which a view of a resume ({@code vis_viewed_resume}) is counted among the views of the month of the
 * resume: the domain of the e-mail of the recruiter, in lower case (the same company may type its domain in capitals).
 * The non-professional domains (public webmail providers, the {@code message} of the {@code jn_system_message} record
 * {@code NONPROFESSIONALDOMAINS} in Portuguese) are all counted as a single one, {@code freelancer}.
 */
public class VisViewedResumeRecruiterDomain implements Function<CcpJsonRepresentation, String> {

	/** The keys of this class. */
	enum Fields implements CcpJsonFieldName{
		/** The key shared by every recruiter whose e-mail has a non-professional domain. */
		freelancer,
	}

	/** The single instance. */
	public static final VisViewedResumeRecruiterDomain INSTANCE = new VisViewedResumeRecruiterDomain();

	private VisViewedResumeRecruiterDomain() {}

	/**
	 * The domain of the recruiter of the view.
	 * @param json the view, its e-mail as it came
	 * @return the domain, in lower case, or {@code freelancer} when it is a non-professional one
	 */
	public String apply(CcpJsonRepresentation json) {
		String recruiterEmail = json.getAsString(VisEntityViewedResume.Fields.email);
		CcpStringDecorator recruiterEmailText = new CcpStringDecorator(recruiterEmail);
		CcpEmailDecorator recruiterEmailDecorator = recruiterEmailText.email();
		String domain = recruiterEmailDecorator.getDomain();
		String lowerCaseDomain = domain.toLowerCase();

		CcpJsonRepresentation systemMessageName = CcpOtherConstants.EMPTY_JSON
				.put(JnEntitySystemMessage.Fields.systemMessageName, JnEntitySystemMessage.NON_PROFESSIONAL_DOMAINS);
		CcpJsonRepresentation systemMessageId = systemMessageName.put(JnJsonCommonsFields.language, JnLanguage.portuguese);
		// cached for an hour by the entity
		CcpJsonRepresentation nonProfessionalDomainsMessage = JnEntitySystemMessage.ENTITY.getOneById(systemMessageId);
		List<String> nonProfessionalDomains = nonProfessionalDomainsMessage.getAsStringList(JnJsonCommonsFields.message);
		boolean nonProfessional = nonProfessionalDomains.contains(lowerCaseDomain);

		if(nonProfessional) {
			return Fields.freelancer.getValue();
		}

		return lowerCaseDomain;
	}
}
