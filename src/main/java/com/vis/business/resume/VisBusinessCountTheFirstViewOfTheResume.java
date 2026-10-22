package com.vis.business.resume;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.entities.VisEntityResumeViewersOfTheMonth;
import com.vis.json.transformers.VisJsonTransformerLinkedinHash;

/**
 * Runs after each insert of {@code vis_viewed_resume} (the first view of a resume by a recruiter): adds the domain of
 * the e-mail of the recruiter to the viewers of the month of each resume that uses the LinkedIn address
 * ({@link VisEntityResumeViewersOfTheMonth}, for the screen "Quem me viu" of the candidate). The e-mails of the address
 * are read by id in {@code vis_resume_linkedin}; an address of no resume counts for nobody. The later views of the same
 * resume by the same recruiter are updates, and do not count.
 */
public class VisBusinessCountTheFirstViewOfTheResume implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessCountTheFirstViewOfTheResume INSTANCE = new VisBusinessCountTheFirstViewOfTheResume();

	private VisBusinessCountTheFirstViewOfTheResume() {}

	/**
	 * Runs the business described in the class documentation.
	 * @param json the inserted view (its LinkedIn and e-mail already hashed, the originals kept in
	 * {@code originalLinkedin} and {@code originalEmail})
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String recruiterEmail = json.getAsString(JnJsonTransformersFieldsEntityDefault.JsonFieldNames.originalEmail);
		CcpStringDecorator recruiterEmailText = new CcpStringDecorator(recruiterEmail);
		CcpEmailDecorator recruiterEmailDecorator = recruiterEmailText.email();
		String domain = recruiterEmailDecorator.getDomain();
		// the same company may type its domain in capitals
		String lowerCaseDomain = domain.toLowerCase();
		List<String> domains = List.of(lowerCaseDomain);

		String linkedinAddress = json.getAsString(VisJsonTransformerLinkedinHash.JsonFieldNames.originalLinkedin);
		CcpJsonRepresentation addressKey = VisResumeLinkedinEmails.getAddressKey(linkedinAddress);
		List<String> hashedEmails = VisResumeLinkedinEmails.getEmails(addressKey);
		for (String hashedEmail : hashedEmails) {
			CcpJsonRepresentation resume = CcpOtherConstants.EMPTY_JSON.put(VisEntityResumeViewersOfTheMonth.Fields.email, hashedEmail);
			CcpJsonRepresentation viewers = resume.put(VisEntityResumeViewersOfTheMonth.Fields.domain, domains);
			VisEntityResumeViewersOfTheMonth.ENTITY.save(viewers);
		}
		return json;
	}
}
