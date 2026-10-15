package com.vis.business.resume;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeLinkedin;

/**
 * Runs before a resume is saved and before it is deleted everywhere: takes the e-mail of the resume out of the LinkedIn
 * address of the stored resume (active or inactive) in {@link VisEntityResumeLinkedin}, when the resume no longer uses
 * it, that is, when the save brings another address or the resume is going away. The other e-mails of the address stay,
 * and the record goes away with the last one. Without this, the old address would keep this e-mail and the next resume
 * to use it would wrongly reach the support operator.
 */
public class VisBusinessResumeLinkedinRelease implements CcpBusiness {

	/**
	 * Runs the business described in the class documentation.
	 * @param json the resume being saved, or the key of the resume being deleted (no {@code linkedinAddress})
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		// the id of the stored records is calculated from the hash of the e-mail
		CcpJsonRepresentation resumeWithHashedEmail = VisResumeEmails.withHashedEmail(json);
		CcpJsonRepresentation storedResume = this.getStoredResume(resumeWithHashedEmail);
		String storedAddress = storedResume.getAsString(VisEntityResume.Fields.linkedinAddress);
		boolean noStoredAddress = storedAddress.isEmpty();

		if (noStoredAddress) {
			return json;
		}

		String newAddress = json.getAsString(VisEntityResume.Fields.linkedinAddress);
		boolean sameAddress = storedAddress.equals(newAddress);

		if (sameAddress) {
			return json;
		}

		// only this e-mail leaves the address: the other resumes that use it keep it
		String hashedEmail = resumeWithHashedEmail.getAsString(JnJsonCommonsFields.email);
		CcpJsonRepresentation addressKey = VisResumeLinkedinEmails.getAddressKey(storedAddress);
		VisResumeLinkedinEmails.removeEmail(addressKey, hashedEmail);
		return json;
	}

	/**
	 * Reads the stored resume, active or inactive.
	 * @param json the resume or its key, with the hash of the e-mail
	 * @return the stored resume, or an empty JSON when the e-mail has no resume yet
	 */
	private CcpJsonRepresentation getStoredResume(CcpJsonRepresentation json) {
		CcpEntityMetaData activeMetaData = VisEntityResume.ENTITY.getEntityMetaData();
		CcpJsonRepresentation activeResume = activeMetaData.getOneByIdOrHandleItIfThisIdWasNotFound(json, notFound -> CcpOtherConstants.EMPTY_JSON);
		boolean isActive = false == activeResume.isEmpty();

		if (isActive) {
			return activeResume;
		}

		CcpEntity inactiveEntity = VisEntityResume.ENTITY.getTwinEntity();
		CcpEntityMetaData inactiveMetaData = inactiveEntity.getEntityMetaData();
		CcpJsonRepresentation inactiveResume = inactiveMetaData.getOneByIdOrHandleItIfThisIdWasNotFound(json, notFound -> CcpOtherConstants.EMPTY_JSON);
		return inactiveResume;
	}
}
