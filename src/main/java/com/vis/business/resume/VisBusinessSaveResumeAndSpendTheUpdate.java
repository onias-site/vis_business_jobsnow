package com.vis.business.resume;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeDailyAction;
import com.vis.json.fields.validation.VisResumeDailyActions;

/**
 * Last step of {@code VisServiceResume.Save}: saves the resume and, when it already existed (found, active or inactive,
 * under {@code _entities}), spends the update of the day of the candidate ({@link VisEntityResumeDailyAction}).
 * Inserting a resume spends nothing. Saving an inactive resume also releases its restriction
 * ({@link VisBusinessReleaseResumeRestriction}), which the afterUpdate of the resume releases only for an update.
 */
public class VisBusinessSaveResumeAndSpendTheUpdate implements CcpBusiness {

	/** The resume, as it came (with the session values). */
	private final CcpJsonRepresentation resume;

	/**
	 * Keeps the resume.
	 * @param resume the resume, as it came
	 */
	public VisBusinessSaveResumeAndSpendTheUpdate(CcpJsonRepresentation resume) {
		this.resume = resume;
	}

	/**
	 * Saves the resume and, in an update, the daily record.
	 * @param jsonAfterTheSteps the json of the procedure
	 * @return the json of the procedure
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation jsonAfterTheSteps) {
		CcpJsonRepresentation foundRecords = jsonAfterTheSteps.getInnerJson(CcpEntity.JsonFieldNames._entities);
		CcpEntity twinEntity = VisEntityResume.ENTITY.getTwinEntity();
		boolean active = foundRecords.containsField(VisEntityResume.ENTITY);
		CcpJsonFieldName sideOfTheStoredResume = active ? VisEntityResume.ENTITY : twinEntity;
		CcpJsonRepresentation storedResume = foundRecords.getInnerJson(sideOfTheStoredResume);
		CcpJsonRepresentation resumeWithKeptFields = getResumeWithKeptFields(this.resume, storedResume);
		VisEntityResume.ENTITY.save(resumeWithKeptFields);
		boolean isUpdate = active || foundRecords.containsField(twinEntity);
		boolean isInsertion = false == isUpdate;
		if(isInsertion) {
			return jsonAfterTheSteps;
		}
		// written here, and not by the resume entity, because that one writes through the messaging: the next request
		// of the candidate must already find the day spent
		String email = this.resume.getAsString(VisEntityResume.Fields.email);
		CcpJsonRepresentation dailyActionKey = VisEntityResumeDailyAction.getKey(email, VisResumeDailyActions.update);
		VisEntityResumeDailyAction.ENTITY.save(dailyActionKey);
		if(active) {
			return jsonAfterTheSteps;
		}
		// the save of an inactive resume through the main entity is an insertion for it (the record leaves the twin), so
		// the afterUpdate of the resume does not run: the correction of the owner releases the restriction here
		VisBusinessReleaseResumeRestriction.INSTANCE.execute(resumeWithKeptFields);
		return jsonAfterTheSteps;
	}

	/**
	 * The resume of the owner with the fields the form does not send (since 2026-11-12): the save replaces the whole
	 * record, so without this a save of the owner would erase them. {@code invalidLinkedinAddress} stays {@code true} while the LinkedIn address is the same, and is {@code false} for a
	 * new resume or a new address (the owner fixed the address the support confirmed as invalid).
	 * @param resume the resume, as it came
	 * @param storedResume the stored resume, active or inactive; empty for a new resume
	 * @return the resume to save
	 */
	public static CcpJsonRepresentation getResumeWithKeptFields(CcpJsonRepresentation resume, CcpJsonRepresentation storedResume) {
		String linkedinAddress = resume.getAsString(VisEntityResume.Fields.linkedinAddress);
		String storedLinkedinAddress = storedResume.getOrDefault(VisEntityResume.Fields.linkedinAddress, () -> "");
		boolean sameLinkedinAddress = linkedinAddress.equals(storedLinkedinAddress);
		boolean storedAsInvalid = storedResume.getOrDefault(VisEntityResume.Fields.invalidLinkedinAddress, () -> false);
		boolean invalidLinkedinAddress = sameLinkedinAddress && storedAsInvalid;
		CcpJsonRepresentation resumeWithTheFlag = resume.put(VisEntityResume.Fields.invalidLinkedinAddress, invalidLinkedinAddress);
		return resumeWithTheFlag;
	}
}
