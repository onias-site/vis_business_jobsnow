package com.vis.business.job.posting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.jn.entities.JnEntityDisposableRecord;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.recruiter.resumes.VisBusinessSendTheResumesToTheRecruiterOfTheJobPosting;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityRecruiterResumes;

/**
 * Sixth and last business before the save of {@code vis_job_posting} (since 2026-11-26; the fifth one until 2026-11-27): each recruiter of the text
 * ({@link VisBusinessRegisterTheRecruitersOfTheJobPosting}) gets the usual e-mail of resumes with the resumes found by the
 * search of the job posting ({@link VisBusinessSaveTheResumeFilterOfTheJobPosting}, the same ids of its link), at most
 * one e-mail of resumes a day: who already has a list written today in {@code vis_recruiter_resumes} (this e-mail or the
 * hourly one to every recruiter) gets nothing, and the list written by this e-mail keeps the hourly one away for the day.
 * "Today" is the day of the calendar (since 2026-11-27, as the hourly process): a list of yesterday is still valid in the
 * disposable entity for 24 hours, but it does not keep away the e-mail of today; the moment of the writing is the
 * {@code trueTimestamp} of its copy in {@code jn_disposable_record}, read in the same round trip.
 * Only the active recruiters get it (an inactive one was not registered again). The recruiters and their lists of today
 * are read once (MGIE). The record goes on to the save without the fields that the businesses before it added for one
 * another (since 2026-11-27; until then it was cut to the fields of {@code vis_job_posting}, which dropped the fields of
 * {@code vis_instant_message_position_reply}, the other entity that runs these businesses before its save).
 */
public class VisBusinessSendTheResumesOfTheJobPosting implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public static final VisBusinessSendTheResumesOfTheJobPosting INSTANCE = new VisBusinessSendTheResumesOfTheJobPosting();

	private VisBusinessSendTheResumesOfTheJobPosting() {}

	/**
	 * Sends the e-mails.
	 * @param json the record, with {@code resumeIds} and {@code recruiterEmails}
	 * @return the record without the fields the businesses before the save added for one another
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpJsonRepresentation record = json.removeFields(VisBusinessRegisterTheRecruitersOfTheJobPosting.Fields.recruiterEmails,
				VisBusinessFindTheSkillsOfTheJobPosting.Fields.requestedSkills, VisBusinessFindTheMostCoveredSkills.Fields.possibleSkills,
				VisBusinessSaveTheResumeFilterOfTheJobPosting.Fields.resumeIds, VisBusinessSaveTheResumeFilterOfTheJobPosting.Fields.filterId);
		List<String> resumeIds = json.getOrDefault(VisBusinessSaveTheResumeFilterOfTheJobPosting.Fields.resumeIds, () -> List.of());
		List<String> recruiterEmails = json.getOrDefault(VisBusinessRegisterTheRecruitersOfTheJobPosting.Fields.recruiterEmails, () -> List.of());
		boolean nothingToSend = resumeIds.isEmpty() || recruiterEmails.isEmpty();

		if(nothingToSend) {
			return record;
		}

		List<CcpJsonRepresentation> keys = new ArrayList<>();
		for (String recruiterEmail : recruiterEmails) {
			CcpJsonRepresentation key = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiter.Fields.email, recruiterEmail);
			keys.add(key);
		}
		CcpJsonRepresentation[] keysArray = keys.toArray(new CcpJsonRepresentation[0]);
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll found = crud.unionAll(keysArray, JnDeleteKeysFromCache.INSTANCE, VisEntityRecruiter.ENTITY, VisEntityRecruiterResumes.ENTITY);
		CcpTimeDecorator now = new CcpTimeDecorator();
		Long midnight = now.getMidnight();
		for (CcpJsonRepresentation key : keys) {
			boolean inactive = false == VisEntityRecruiter.ENTITY.isPresentInThisUnionAll(found, key);

			if(inactive) {
				continue;
			}

			boolean alreadyReceivedToday = receivedToday(found, key, midnight);

			if(alreadyReceivedToday) {
				continue;
			}

			Supplier<CcpJsonRepresentation> keySupplier = key.getJsonSupplier();
			CcpJsonRepresentation recruiter = VisEntityRecruiter.ENTITY.getRecordFromUnionAll(found, keySupplier);
			CcpJsonRepresentation sendingInput = CcpOtherConstants.EMPTY_JSON
					.put(VisBusinessSendTheResumesToTheRecruiterOfTheJobPosting.Fields.recruiter, recruiter)
					.put(VisBusinessSendTheResumesToTheRecruiterOfTheJobPosting.Fields.resumeIds, resumeIds);
			VisBusinessSendTheResumesToTheRecruiterOfTheJobPosting.INSTANCE.execute(sendingInput);
		}
		return record;
	}

	/**
	 * Whether the recruiter has a list written since the midnight of today: a list present in the search whose copy in
	 * {@code jn_disposable_record} was written today. A list present with no copy found counts as of today.
	 * @param found the search of the recruiters and of their lists
	 * @param key the key of the recruiter
	 * @param midnight the midnight of today
	 * @return true when the recruiter already received the e-mail of resumes today
	 */
	private static boolean receivedToday(CcpSelectUnionAll found, CcpJsonRepresentation key, Long midnight) {
		boolean noList = false == VisEntityRecruiterResumes.ENTITY.isPresentInThisUnionAll(found, key);

		if(noList) {
			return false;
		}

		CcpJsonRepresentation handledKey = VisEntityRecruiterResumes.ENTITY.getJsonWithHandledPrimaryKey(key);
		String emailHash = handledKey.getAsString(VisEntityRecruiterResumes.Fields.email);
		CcpJsonRepresentation primaryKeyValues = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiterResumes.Fields.email, emailHash);
		String copyId = primaryKeyValues.asUgglyJson();
		String entityName = VisEntityRecruiterResumes.ENTITY.getEntityMetaData().entityName;
		CcpJsonRepresentation copyKey = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.id, copyId)
				.put(JnJsonCommonsFields.entity, entityName);
		Supplier<CcpJsonRepresentation> copyKeySupplier = copyKey.getJsonSupplier();
		CcpJsonRepresentation copy = JnEntityDisposableRecord.ENTITY.getRecordFromUnionAll(found, copyKeySupplier);
		boolean noCopy = false == copy.containsAllFields(JnEntityDisposableRecord.Fields.trueTimestamp);

		if(noCopy) {
			return true;
		}

		// read as a number: the copy comes from the database, where the long may arrive as a double
		Long written = copy.getAsLongNumber(JnEntityDisposableRecord.Fields.trueTimestamp);
		boolean writtenToday = written >= midnight;
		return writtenToday;
	}
}
