package com.vis.services;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.services.JnService;
import com.vis.entities.VisEntityResume;
import com.vis.json.fields.validation.VisJsonFieldsResumeKey;


/**
 * Resume data access service. Exposes the CRUD operations on the VisEntityResume entity.
 */
public enum VisServiceResume implements JnService {
	/**
	 * Toggles the resume between active and inactive: an active resume is deactivated (the delete moves it to the twin,
	 * {@code vis_inactive_resume}) and an inactive one is reactivated (the delete on the twin moves it back).
	 */
	ChangeStatus{
		/**
		 * Deactivates the active resume, or reactivates the inactive one.
		 * @param json the resume key
		 * @return the same JSON
		 * @throws CcpErrorFlowDisturb with {@code NOT_FOUND} when the resume is neither active nor inactive
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			boolean active = VisEntityResume.ENTITY.exists(json);

			if(active) {
				VisEntityResume.ENTITY.delete(json);
				return json;
			}

			CcpEntity twinEntity = VisEntityResume.ENTITY.getTwinEntity();
			boolean inactive = twinEntity.exists(json);

			if(inactive) {
				twinEntity.delete(json);
				return json;
			}

			CcpErrorFlowDisturb notFound = new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
			throw notFound;
		}

		/**
		 * Validates the input: the key of the resume.
		 * @return the validation class
		 */
		public Class<?> getJsonValidationClass() {
			return VisJsonFieldsResumeKey.class;
		}
	},
	/** Deletes every data of the resume for good, wherever it is (active or inactive). */
	Delete{
		/**
		 * Deletes the resume everywhere.
		 * @param sessionValues the resume key, from the session
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation sessionValues) {
			VisEntityResume.ENTITY.deleteAnyWhere(sessionValues);

			return sessionValues;
		}

		/**
		 * Validates the input: the key of the resume.
		 * @return the validation class
		 */
		public Class<?> getJsonValidationClass() {
			return VisJsonFieldsResumeKey.class;
		}
	},
	/** Returns the resume, wherever it is. */
	GetData{
		/**
		 * Reads the resume from every associated entity.
		 * @param json the resume key
		 * @return the records by entity
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation resumeData = VisEntityResume.ENTITY.getOneByIdAnyWhere(json);
			
			return resumeData;
		}

		/**
		 * Validates the input: the key of the resume.
		 * @return the validation class
		 */
		public Class<?> getJsonValidationClass() {
			return VisJsonFieldsResumeKey.class;
		}
	},
	/** Saves the resume. */
	Save{
		/**
		 * Saves the resume.
		 * @param sessionValues the resume, with the session values
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation sessionValues) {
			
			VisEntityResume.ENTITY.save(sessionValues);

			return sessionValues;
		}
		
		/**
		 * Validates the input with the fields of the resume entity.
		 * @return the validation class
		 */
		public Class<?> getJsonValidationClass() {
			return VisEntityResume.Fields.class;
		}
	}, 
	;
}
