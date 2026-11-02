package com.vis.business.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpBusinessDeleteStoredRecordFromEntity;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntityRecruiterSignUpReturn;

/**
 * Step of {@code VisBusinessSaveRecruiterFromLoginAnswers}: moves the inactive recruiter back, with the record it had, and
 * keeps when it came back, so that the next e-mail of resumes does not inactivate them again.
 */
class VisBusinessReactivateRecruiterFromSignUp implements CcpBusiness {

	/** Moves the stored record back to the recruiters. */
	private final CcpBusiness reactivate;

	/** The e-mail of the recruiter, as typed. */
	private final String email;

	/**
	 * Keeps what the step needs.
	 * @param twinEntity the inactive recruiters
	 * @param email the e-mail of the recruiter, as typed
	 */
	VisBusinessReactivateRecruiterFromSignUp(CcpEntity twinEntity, String email) {
		this.reactivate = new CcpBusinessDeleteStoredRecordFromEntity(twinEntity);
		this.email = email;
	}

	/**
	 * Moves the recruiter back and keeps the moment.
	 * @param json the json of the procedure, holding the record under {@code _entities}
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation afterReactivating = this.reactivate.execute(json);
		long now = System.currentTimeMillis();
		CcpJsonRepresentation signUpReturn = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityRecruiterSignUpReturn.Fields.email, this.email)
				.put(VisEntityRecruiterSignUpReturn.Fields.returnedAt, now);
		VisEntityRecruiterSignUpReturn.ENTITY.save(signUpReturn);
		return afterReactivating;
	}
}
