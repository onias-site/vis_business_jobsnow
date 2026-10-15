package com.vis.services;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpErrorJsonFieldNotFound;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.crud.CcpGetEntityId;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.services.JnService;
import com.jn.utils.JnDeleteKeysFromCache;
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
		 * @return the fields of the resume in the JSON, without the session values
		 * @throws CcpErrorFlowDisturb with {@code NOT_FOUND} when the resume is neither active nor inactive
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			// another change of status of the same resume, running at the same time, may move it between the check and the
			// move: the twin answers that it moved (REDIRECT) or the record is gone from the read (field not found). The
			// change already happened, so the answer is the success. Until 2026-10-14 these became 301 and 500.
			try {
				CcpJsonRepresentation result = this.toggle(json);
				return result;
			} catch (CcpErrorJsonFieldNotFound movedByAnotherRequest) {
				CcpJsonRepresentation result = withoutSessionValues(json);
				return result;
			} catch (CcpErrorFlowDisturb e) {
				boolean movedByAnotherRequest = e.status.equals(CcpProcessStatusDefault.REDIRECT);
				if(movedByAnotherRequest) {
					CcpJsonRepresentation result = withoutSessionValues(json);
					return result;
				}
				throw e;
			}
		}

		/**
		 * Deactivates the active resume, or reactivates the inactive one. Both entities are read in one union all (FSED);
		 * the steps decide over that single read, so the resume moved by the first step is not moved back by the second.
		 * Until 2026-10-15 these were two {@code exists}, one trip to the database each.
		 * @param json the resume key
		 * @return the fields of the resume in the JSON, without the session values
		 * @throws CcpErrorFlowDisturb with {@code NOT_FOUND} when the resume is neither active nor inactive
		 */
		private CcpJsonRepresentation toggle(CcpJsonRepresentation json) {
			CcpEntity twinEntity = VisEntityResume.ENTITY.getTwinEntity();
			CcpBusiness deactivate = jsonWithTheStoredResume -> {
				VisEntityResume.ENTITY.delete(json);
				return jsonWithTheStoredResume;
			};
			CcpBusiness reactivate = jsonWithTheStoredResume -> {
				twinEntity.delete(json);
				return jsonWithTheStoredResume;
			};
			// a record found in either entity is placed under _entities; with none, the resume does not exist
			CcpBusiness refuseWhenNotFound = jsonAfterTheSteps -> {
				boolean found = jsonAfterTheSteps.containsField(CcpEntity.JsonFieldNames._entities);
				if(found) {
					return jsonAfterTheSteps;
				}
				CcpErrorFlowDisturb notFound = new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
				throw notFound;
			};

			new CcpGetEntityId(json)
			.toBeginProcedureAnd()
				.ifThisIdIsPresentInEntity(VisEntityResume.ENTITY).executeAction(deactivate).and()
				.ifThisIdIsPresentInEntity(twinEntity).executeAction(reactivate).and()
				.executeAction(refuseWhenNotFound)
				.andFinallyReturningTheseFields(ResumeResponse.inexistentField)
			.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
			;
			return withoutSessionValues(json);
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
		 * @return the fields of the resume in the JSON, without the session values
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation sessionValues) {
			VisEntityResume.ENTITY.deleteAnyWhere(sessionValues);

			return withoutSessionValues(sessionValues);
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
		 * Reads the resume from every associated entity, without the internal fields of the records.
		 * @param json the resume key
		 * @return the records by entity
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation resumeData = VisEntityResume.ENTITY.getOneByIdAnyWhere(json);
			CcpEntity twinEntity = VisEntityResume.ENTITY.getTwinEntity();
			CcpJsonRepresentation withoutInternalFieldsOfTheActive = withoutInternalFields(resumeData, VisEntityResume.ENTITY);
			CcpJsonRepresentation withoutInternalFields = withoutInternalFields(withoutInternalFieldsOfTheActive, twinEntity);
			return withoutInternalFields;
		}

		/**
		 * Removes from the record of the entity what the screen does not use: {@code hash} (the keys that place the resume
		 * in the searches of the recruiters) and {@code email} (stored as a hash; the screen knows the readable one). Until
		 * 2026-10-15 they went to the browser (R05-2).
		 * @param resumeData the records by entity
		 * @param entity the entity whose record is cleaned
		 * @return the records by entity, with the record of the entity cleaned
		 */
		private CcpJsonRepresentation withoutInternalFields(CcpJsonRepresentation resumeData, CcpEntity entity) {
			boolean notFound = false == resumeData.containsField(entity);
			if(notFound) {
				return resumeData;
			}
			CcpJsonRepresentation storedRecord = resumeData.getInnerJson(entity);
			CcpJsonRepresentation fieldsOfTheScreen = storedRecord.removeFields(VisEntityResume.Fields.hash, VisEntityResume.Fields.email);
			CcpJsonRepresentation cleanedResumeData = resumeData.put(entity, fieldsOfTheScreen);
			return cleanedResumeData;
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
		 * @return the fields of the resume in the JSON, without the session values
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation sessionValues) {
			
			VisEntityResume.ENTITY.save(sessionValues);

			return withoutSessionValues(sessionValues);
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

	/** Fields of the search procedures: {@link #ChangeStatus} answers with the json it received, so it returns no field. */
	enum ResumeResponse implements CcpJsonFieldName {
		/** The {@code inexistentField} field. */
		inexistentField
	}

	/**
	 * Keeps only the fields of the resume: the request carries the session values too ({@code sessionToken}, {@code ip},
	 * {@code userAgent}), which were echoed in the response until 2026-10-14.
	 * @param json the request
	 * @return the fields of the resume in the request
	 */
	private static CcpJsonRepresentation withoutSessionValues(CcpJsonRepresentation json) {
		CcpEntityMetaData metaData = VisEntityResume.ENTITY.getEntityMetaData();
		CcpJsonRepresentation resumeFields = metaData.getOnlyExistingFields(json);
		return resumeFields;
	}
}
