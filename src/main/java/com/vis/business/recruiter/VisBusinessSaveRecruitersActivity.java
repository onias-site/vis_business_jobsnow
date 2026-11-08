package com.vis.business.recruiter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.CcpDbRequester;
import com.ccp.decorators.CcpFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.global.engine.CcpJsonValidationError;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityRecruiter;

/**
 * Writes a date of activity ({@code lastTelegramAppearance} or {@code lastLegacyView}) on the recruiters of
 * {@link VisEntityRecruiter} who are active, a batch at a time: one union all reads the batch and one bulk updates the
 * records whose stored date is older than the new one (or absent). A recruiter who is not in the entity (an inactive one,
 * in the twin, or an e-mail that is no recruiter) is left as it is.
 *
 * <p>It is a batch process run by hand or by the loads of recruiters, not an online request.
 *
 * <p>Input: {@code activityField}, the name of the field, and {@code datesByEmail}, the date (milliseconds) of each
 * normalized e-mail. Output: the input plus {@code updated}, how many recruiters got the new date.
 */
public class VisBusinessSaveRecruitersActivity implements CcpBusiness {

	/** Input and output fields. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** The {@code activityField} field: the field of {@link VisEntityRecruiter} that receives the dates, required. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 100)
		activityField,
		/** The {@code datesByEmail} field: the date of each normalized e-mail. */
		datesByEmail,
		/** How many recruiters got the new date. */
		updated,
	}

	/** How many recruiters go in each union all and bulk. */
	private static final int BATCH_SIZE = 500;

	/** The single instance: the business keeps no state. */
	public static final VisBusinessSaveRecruitersActivity INSTANCE = new VisBusinessSaveRecruitersActivity();

	private VisBusinessSaveRecruitersActivity() {}

	/**
	 * The input of this business.
	 * @param activityField the field that receives the dates
	 * @param datesByEmail the date of each normalized e-mail
	 * @return the input
	 */
	public static CcpJsonRepresentation input(CcpJsonFieldName activityField, CcpJsonRepresentation datesByEmail) {
		String activityFieldName = activityField.getValue();
		CcpJsonRepresentation withField = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.activityField, activityFieldName);
		CcpJsonRepresentation input = withField.put(JsonFieldNames.datesByEmail, datesByEmail);
		return input;
	}

	/**
	 * The dates by the normalized e-mail, which is the id of the recruiter: e-mails that differ only in accents, capital
	 * letters or spaces keep the most recent date; an e-mail that is not a valid address of the entity is left out.
	 * @param datesByFoundEmail the date of each e-mail as found
	 * @param source the source given to the normalization of {@link VisBusinessPrepareRecruiter}
	 * @return the date of each normalized e-mail
	 */
	public static CcpJsonRepresentation getDatesByNormalizedEmail(Map<String, Long> datesByFoundEmail, CcpJsonFieldName source) {
		String sourceValue = source.getValue();
		Map<String, Long> datesByNormalizedEmail = new HashMap<>();
		for (Map.Entry<String, Long> dateOfTheEmail : datesByFoundEmail.entrySet()) {
			String foundEmail = dateOfTheEmail.getKey();
			CcpJsonRepresentation recruiterEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiter.Fields.email, foundEmail);
			CcpJsonRepresentation recruiterInput = recruiterEmail.put(VisEntityRecruiter.Fields.source, sourceValue);
			try {
				CcpJsonRepresentation recruiter = VisBusinessPrepareRecruiter.INSTANCE.execute(recruiterInput);
				String normalizedEmail = recruiter.getAsString(VisEntityRecruiter.Fields.email);
				Long date = dateOfTheEmail.getValue();
				datesByNormalizedEmail.merge(normalizedEmail, date, Math::max);
			} catch (CcpJsonValidationError e) {
				// not a valid address: no recruiter has it
			}
		}
		CcpJsonRepresentation dates = CcpOtherConstants.EMPTY_JSON;
		for (Map.Entry<String, Long> dateOfTheEmail : datesByNormalizedEmail.entrySet()) {
			String normalizedEmail = dateOfTheEmail.getKey();
			CcpFieldName emailField = new CcpFieldName(normalizedEmail);
			Long date = dateOfTheEmail.getValue();
			dates = dates.put(emailField, date);
		}
		return dates;
	}

	/**
	 * Updates the recruiters whose stored date is older than the new one.
	 * @param json the field and the dates
	 * @return the input and the counter
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String activityFieldName = json.getAsString(JsonFieldNames.activityField);
		CcpFieldName activityField = new CcpFieldName(activityFieldName);
		CcpJsonRepresentation datesByEmail = json.getInnerJson(JsonFieldNames.datesByEmail);
		Set<String> emailsSet = datesByEmail.fieldSet();
		List<String> emails = new ArrayList<>(emailsSet);
		int emailsCount = emails.size();
		int updatedCount = 0;

		for (int start = 0; start < emailsCount; start += BATCH_SIZE) {
			int end = Math.min(start + BATCH_SIZE, emailsCount);
			List<String> batch = emails.subList(start, end);
			int batchUpdatedCount = this.updateBatch(batch, activityField, datesByEmail);
			updatedCount += batchUpdatedCount;
		}

		CcpJsonRepresentation result = json.put(JsonFieldNames.updated, updatedCount);
		return result;
	}

	/**
	 * Reads the recruiters of the batch and updates the ones whose date is older.
	 * @param batch the e-mails of the batch
	 * @param activityField the field that receives the dates
	 * @param datesByEmail the date of each normalized e-mail
	 * @return how many recruiters of the batch were updated
	 */
	private int updateBatch(List<String> batch, CcpFieldName activityField, CcpJsonRepresentation datesByEmail) {
		List<CcpJsonRepresentation> keys = new ArrayList<>();
		for (String email : batch) {
			CcpJsonRepresentation key = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiter.Fields.email, email);
			keys.add(key);
		}
		CcpJsonRepresentation[] keysArray = keys.toArray(CcpJsonRepresentation[]::new);
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll unionAll = crud.unionAll(keysArray, JnDeleteKeysFromCache.INSTANCE, VisEntityRecruiter.ENTITY);
		CcpDbRequester dbRequester = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		String fieldNameToId = dbRequester.getFieldNameToId();
		CcpFieldName idField = new CcpFieldName(fieldNameToId);

		List<CcpBulkItem> items = new ArrayList<>();
		int updatedCount = 0;
		// by the keys of the batch, not by the rows of the union all, which may bring rows of the decorators of the entity
		for (CcpJsonRepresentation key : keys) {
			boolean notAnActiveRecruiter = false == VisEntityRecruiter.ENTITY.isPresentInThisUnionAll(unionAll, key);
			if(notAnActiveRecruiter) {
				continue;
			}
			CcpJsonRepresentation storedRecruiter = VisEntityRecruiter.ENTITY.getRecordFromUnionAll(unionAll, () -> key);
			String email = key.getAsString(VisEntityRecruiter.Fields.email);
			CcpFieldName emailField = new CcpFieldName(email);
			Long newDate = datesByEmail.getAsLongNumber(emailField);
			boolean hasADate = storedRecruiter.containsField(activityField);
			long storedDate = hasADate ? storedRecruiter.getAsLongNumber(activityField) : 0L;
			boolean notMoreRecent = newDate <= storedDate;
			if(notMoreRecent) {
				continue;
			}
			CcpJsonRepresentation recruiterWithoutId = storedRecruiter.removeFields(idField);
			CcpJsonRepresentation updatedRecruiter = recruiterWithoutId.put(activityField, newDate);
			List<CcpBulkItem> recruiterItems = VisEntityRecruiter.ENTITY.toBulkItems(updatedRecruiter, CcpBulkEntityOperationType.update);
			items.addAll(recruiterItems);
			updatedCount++;
		}
		JnExecuteBulkOperation.INSTANCE.executeBulk(items, JnDeleteKeysFromCache.INSTANCE);
		return updatedCount;
	}
}
