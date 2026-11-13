package com.vis.services;

import java.util.List;

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
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.services.JnService;
import com.jn.utils.JnLanguage;
import com.vis.business.bots.linkedin.VisSupportVerifyLinkedinConfirmInvalid;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.resume.VisBusinessChangeResumeStatusOncePerDay;
import com.vis.business.resume.VisBusinessDeleteResumeAndSpendTheDay;
import com.vis.business.resume.VisBusinessRefuseResumeNotFound;
import com.vis.business.resume.VisBusinessRefuseResumeUpdateAlreadyRunToday;
import com.vis.business.resume.VisBusinessResumeViewersOfTheMonth;
import com.vis.business.resume.VisBusinessSaveResumeAndSpendTheUpdate;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeDailyAction;
import com.vis.entities.VisEntityResumeRestriction;
import com.vis.json.fields.validation.VisJsonFieldsResumeKey;
import com.vis.json.fields.validation.VisResumeDailyActions;
import com.vis.status.VisProcessStatusResumeDailyAction;


/**
 * Resume data access service. Exposes the CRUD operations on the VisEntityResume entity.
 */
public enum VisServiceResume implements JnService {
	/**
	 * Toggles the resume between active and inactive: an active resume is deactivated (the delete moves it to the twin,
	 * {@code vis_inactive_resume}) and an inactive one is reactivated (the delete on the twin moves it back). Each of the
	 * two runs once a day ({@link VisEntityResumeDailyAction}): the second of the day gets {@code dailyLimitReached} (429).
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
			// once a day each (VisEntityResumeDailyAction): the second of the day gets dailyLimitReached (429)
			CcpBusiness deactivate = new VisBusinessChangeResumeStatusOncePerDay(VisEntityResume.ENTITY, VisResumeDailyActions.inactivation, json);
			CcpBusiness reactivate = new VisBusinessChangeResumeStatusOncePerDay(twinEntity, VisResumeDailyActions.reactivation, json);
			// a record found in either entity is placed under _entities; with none, the resume does not exist
			CcpBusiness refuseWhenNotFound = new VisBusinessRefuseResumeNotFound(json);

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
	/**
	 * Deletes every data of the resume for good, wherever it is (active or inactive), once a day: the candidate who
	 * already deleted a resume in the last 24 hours ({@link VisEntityResumeDailyAction}) gets {@code dailyLimitReached}
	 * (429), so deleting, inserting and deleting again in the same day is refused at the second deletion.
	 */
	Delete{
		/**
		 * Deletes the resume everywhere, or refuses the second deletion of the day.
		 * @param sessionValues the resume key, from the session
		 * @return the fields of the resume in the JSON, without the session values
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation sessionValues) {
			CcpJsonRepresentation jsonWithAction = sessionValues.put(VisEntityResumeDailyAction.Fields.action, VisResumeDailyActions.exclusion.name());
			CcpBusiness deleteAndSpendTheDay = new VisBusinessDeleteResumeAndSpendTheDay(sessionValues);

			new CcpGetEntityId(jsonWithAction)
			.toBeginProcedureAnd()
				.ifThisIdIsPresentInEntity(VisEntityResumeDailyAction.ENTITY).returnStatus(VisProcessStatusResumeDailyAction.dailyLimitReached).and()
				.executeAction(deleteAndSpendTheDay)
				.andFinallyReturningTheseFields(ResumeResponse.inexistentField)
			.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
			;
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
			// the language of the screen: the warning has texts only in English and Portuguese, the default for any other value
			String languageName = json.getOrDefault(JnJsonCommonsFields.language, () -> JnLanguage.portuguese.name());
			String english = JnLanguage.english.name();
			JnLanguage language = english.equals(languageName) ? JnLanguage.english : JnLanguage.portuguese;
			List<String> restrictionMessages = getRestrictionMessages(json);
			CcpJsonRepresentation withoutInternalFieldsOfTheActive = withoutInternalFields(resumeData, VisEntityResume.ENTITY, language, restrictionMessages);
			CcpJsonRepresentation withoutInternalFields = withoutInternalFields(withoutInternalFieldsOfTheActive, twinEntity, language, restrictionMessages);
			return withoutInternalFields;
		}

		/**
		 * What the owner must correct before the resume goes to the recruiters again ({@code vis_resume_restriction},
		 * since 2026-11-12), read by id.
		 * @param json the resume key, with the readable e-mail
		 * @return the messages, none when the resume has no restriction
		 */
		private List<String> getRestrictionMessages(CcpJsonRepresentation json) {
			CcpJsonRepresentation restrictionKey = json.getJsonPiece(VisEntityResumeRestriction.Fields.email);
			try {
				CcpJsonRepresentation restriction = VisEntityResumeRestriction.ENTITY.getOneById(restrictionKey);
				List<String> messages = restriction.getAsStringList(VisEntityResumeRestriction.Fields.message);
				return messages;
			} catch (CcpErrorFlowDisturb noRestriction) {
				return List.of();
			}
		}

		/**
		 * Removes from the record of the entity what the screen does not use: {@code hash} (the keys that place the resume
		 * in the searches of the recruiters) and {@code email} (stored as a hash; the screen knows the readable one). Until
		 * 2026-10-15 they went to the browser (R05-2).
		 * @param resumeData the records by entity
		 * @param entity the entity whose record is cleaned
		 * @param language the language of the screen
		 * @param restrictionMessages what the owner must correct, shown in red at the top of the form
		 * @return the records by entity, with the record of the entity cleaned
		 */
		private CcpJsonRepresentation withoutInternalFields(CcpJsonRepresentation resumeData, CcpEntity entity, JnLanguage language, List<String> restrictionMessages) {
			boolean notFound = false == resumeData.containsField(entity);
			if(notFound) {
				return resumeData;
			}
			CcpJsonRepresentation storedRecord = resumeData.getInnerJson(entity);
			CcpJsonRepresentation fieldsOfTheScreen = storedRecord.removeFields(VisEntityResume.Fields.hash, VisEntityResume.Fields.email);
			CcpJsonRepresentation fieldsWithTheWarning = withTheInvalidLinkedinWarning(fieldsOfTheScreen, language);
			boolean noRestriction = restrictionMessages.isEmpty();
			CcpJsonRepresentation fieldsWithTheRestriction = noRestriction ? fieldsWithTheWarning : fieldsWithTheWarning.put(ResumeResponse.restrictionMessages, restrictionMessages);
			CcpJsonRepresentation cleanedResumeData = resumeData.put(entity, fieldsWithTheRestriction);
			return cleanedResumeData;
		}

		/**
		 * Puts {@code invalidLinkedinAddressMessage}, shown at the top of the form, when the support confirmed that the
		 * LinkedIn address of the resume is invalid (since 2026-11-12): the text of {@code VisInvalidLinkedinAddress}, the
		 * same the owner gets through the user bot, in the language of the screen.
		 * @param resume the record of the screen
		 * @param language the language of the screen
		 * @return the record, with the warning when the address is invalid
		 */
		private CcpJsonRepresentation withTheInvalidLinkedinWarning(CcpJsonRepresentation resume, JnLanguage language) {
			boolean invalidLinkedinAddress = resume.getOrDefault(VisEntityResume.Fields.invalidLinkedinAddress, () -> false);
			boolean validAddress = false == invalidLinkedinAddress;
			if(validAddress) {
				return resume;
			}
			String linkedinAddress = resume.getAsString(VisEntityResume.Fields.linkedinAddress);
			String warning = VisSupportVerifyLinkedinConfirmInvalid.getMessageToTheOwner(linkedinAddress, language);
			CcpJsonRepresentation resumeWithWarning = resume.put(ResumeResponse.invalidLinkedinAddressMessage, warning);
			return resumeWithWarning;
		}

		/**
		 * Validates the input: the key of the resume.
		 * @return the validation class
		 */
		public Class<?> getJsonValidationClass() {
			return VisJsonFieldsResumeKey.class;
		}
	},
	/**
	 * Saves the resume. Inserting (no resume, active or inactive) has no limit; updating an existing one runs once a day:
	 * the candidate who already updated the resume in the last 24 hours ({@link VisEntityResumeDailyAction}) gets
	 * {@code dailyLimitReached} (429) and nothing is saved. The resume (both entities) and the update of the day are read
	 * in one union all.
	 */
	Save{
		/**
		 * Saves the resume, or refuses the second update of the day.
		 * @param sessionValues the resume, with the session values
		 * @return the fields of the resume in the JSON, without the session values
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation sessionValues) {
			CcpJsonRepresentation jsonWithAction = sessionValues.put(VisEntityResumeDailyAction.Fields.action, VisResumeDailyActions.update.name());
			CcpEntity twinEntity = VisEntityResume.ENTITY.getTwinEntity();
			CcpBusiness saveAndSpendTheUpdate = new VisBusinessSaveResumeAndSpendTheUpdate(sessionValues);

			new CcpGetEntityId(jsonWithAction)
			.toBeginProcedureAnd()
				.loadThisIdFromEntity(VisEntityResumeDailyAction.ENTITY).and()
				.ifThisIdIsPresentInEntity(VisEntityResume.ENTITY).executeAction(VisBusinessRefuseResumeUpdateAlreadyRunToday.INSTANCE).and()
				.ifThisIdIsPresentInEntity(twinEntity).executeAction(VisBusinessRefuseResumeUpdateAlreadyRunToday.INSTANCE).and()
				.executeAction(saveAndSpendTheUpdate)
				.andFinallyReturningTheseFields(ResumeResponse.inexistentField)
			.endThisProcedure(this, CcpOtherConstants.DO_NOTHING, CcpOtherConstants.DO_NOTHING, JnDeleteKeysFromCache.INSTANCE)
			;
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
	/** The domains of the recruiters who viewed the resume of the candidate logged in, in a month (the screen "Quem me viu"). */
	GetViewersOfTheMonth{
		/**
		 * Counts the recruiters of each domain who viewed the resume in the month.
		 * @param json the candidate and the month ({@code yyyyMM})
		 * @return the month and the views of each domain
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation response = VisBusinessResumeViewersOfTheMonth.INSTANCE.execute(json);
			return response;
		}

		/**
		 * Validates the input with the fields of the business.
		 * @return the validation class
		 */
		public Class<?> getJsonValidationClass() {
			return VisBusinessResumeViewersOfTheMonth.Fields.class;
		}
	},
	;

	/** Fields of the search procedures: {@link #ChangeStatus} answers with the json it received, so it returns no field. */
	enum ResumeResponse implements CcpJsonFieldName {
		/** The {@code inexistentField} field. */
		inexistentField,
		/** The warning of the form about the LinkedIn address confirmed as invalid. */
		invalidLinkedinAddressMessage,
		/** What the owner must correct ({@code vis_resume_restriction}), shown in red at the top of the form. */
		restrictionMessages,
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
