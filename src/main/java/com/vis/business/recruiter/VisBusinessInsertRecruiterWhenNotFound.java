package com.vis.business.recruiter;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntityRecruiter;

/**
 * Last step of {@code VisBusinessSaveRecruiterFromLoginAnswers}: a recruiter found in either entity (active or inactive)
 * is placed under {@code _entities}; with none, the recruiter is new and is saved.
 */
class VisBusinessInsertRecruiterWhenNotFound implements CcpBusiness {

	/** The recruiter to save, with the source {@code login}. */
	private final CcpJsonRepresentation recruiter;

	/**
	 * Keeps the recruiter to save.
	 * @param recruiter the recruiter to save, with the source {@code login}
	 */
	VisBusinessInsertRecruiterWhenNotFound(CcpJsonRepresentation recruiter) {
		this.recruiter = recruiter;
	}

	/**
	 * Saves the recruiter when it was found in no entity.
	 * @param jsonAfterTheSteps the json of the procedure
	 * @return the same json
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonAfterTheSteps) {
		boolean found = jsonAfterTheSteps.containsField(CcpEntity.JsonFieldNames._entities);
		if(found) {
			return jsonAfterTheSteps;
		}
		VisEntityRecruiter.ENTITY.save(this.recruiter);
		return jsonAfterTheSteps;
	}
}
