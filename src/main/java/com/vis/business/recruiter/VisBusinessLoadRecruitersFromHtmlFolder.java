package com.vis.business.recruiter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpFileDecorator;
import com.ccp.decorators.CcpFolderDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.global.engine.CcpJsonValidationError;
import com.jn.db.bulk.JnBulkCreateResult;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.telegram.VisTelegramMessages;
import com.vis.business.telegram.VisWhatsAppMessages;
import com.vis.entities.VisEntityRecruiter;

/**
 * The loads of recruiters after the first one: every e-mail found in the HTML files ({@code .html} or {@code .htm}) and
 * in the text files exported from WhatsApp ({@code .txt}) of a folder becomes a recruiter of {@link VisEntityRecruiter} with the source {@code telegram}. The e-mails lose their
 * accents and go to lower case; an e-mail already in the entity or in its twin ({@code vis_inactive_recruiter}) is not
 * written again. The writes go in bulk, a batch of e-mails at a time: one union all checks the batch in both entities
 * and one bulk creates what is new. The recruiters created enter the groups of the initials of their domains
 * ({@link VisRecruiterDomainGroups}), which the bulk does not do.
 *
 * <p>The day of the most recent message with each e-mail goes to {@code lastTelegramAppearance} when the message is at
 * most {@value VisEntityRecruiter#DAYS_OF_A_RECENT_TELEGRAM_APPEARANCE} days old: on the recruiters created and, by
 * {@link VisBusinessSaveRecruitersActivity}, on the active ones whose stored day is older. An older message writes no date.
 *
 * <p>It is a batch process run by hand, not an online request.
 *
 * <p>Input: {@code folderPath}. Output: the input plus {@code filesRead}, {@code emailsFound}, {@code invalidEmails},
 * {@code created}, {@code alreadyExisting} and {@code appearancesUpdated}.
 */
public class VisBusinessLoadRecruitersFromHtmlFolder implements CcpBusiness {

	/** Input and output fields. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** The {@code folderPath} field: the folder with the HTML files, text, required. */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 1000)
		folderPath,
		/** How many HTML files were read. */
		filesRead,
		/** How many distinct e-mails were found in the files. */
		emailsFound,
		/** The e-mails found that are not valid addresses of the entity. */
		invalidEmails,
		/** How many recruiters were created. */
		created,
		/** How many e-mails were already in the entity or in its twin. */
		alreadyExisting,
		/** How many recruiters already in the entity got a more recent appearance. */
		appearancesUpdated,
		/** The format of the day of a message. */
		DAY_OF_THE_MESSAGE { public String getValue() { return "ddMMyyyy"; } },
		/** Extension of the HTML files. */
		HTML_EXTENSION { public String getValue() { return ".html"; } },
		/** Short extension of the HTML files. */
		HTM_EXTENSION { public String getValue() { return ".htm"; } },
		/** Extension of the text files exported from WhatsApp. */
		TXT_EXTENSION { public String getValue() { return ".txt"; } },
	}

	/** How many e-mails go in each union all and bulk. */
	private static final int BATCH_SIZE = 500;

	/** The single instance: the business keeps no state. */
	public static final VisBusinessLoadRecruitersFromHtmlFolder INSTANCE = new VisBusinessLoadRecruitersFromHtmlFolder();

	private VisBusinessLoadRecruitersFromHtmlFolder() {}

	/**
	 * Reads the e-mails of the HTML files of the folder and creates the new recruiters.
	 * @param json the folder
	 * @return the folder and the counters of the load
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String folderPath = json.getAsString(JsonFieldNames.folderPath);
		CcpStringDecorator folderPathText = new CcpStringDecorator(folderPath);
		CcpFolderDecorator folder = folderPathText.folder();

		Set<String> foundEmails = new LinkedHashSet<>();
		List<String> filesRead = new ArrayList<>();
		Map<String, Long> appearancesByFoundEmail = new HashMap<>();
		folder.readFiles(file -> this.readEmails(file, foundEmails, filesRead, appearancesByFoundEmail));

		CcpTimeDecorator now = new CcpTimeDecorator();
		long recentSince = now.getDaysAgo(VisEntityRecruiter.DAYS_OF_A_RECENT_TELEGRAM_APPEARANCE);
		Map<String, Long> recentAppearancesByFoundEmail = new HashMap<>(appearancesByFoundEmail);
		recentAppearancesByFoundEmail.values().removeIf(appearance -> appearance < recentSince);
		CcpJsonRepresentation recentAppearances = VisBusinessSaveRecruitersActivity.getDatesByNormalizedEmail(recentAppearancesByFoundEmail, VisEntityRecruiter.Sources.telegram);

		// by the normalized e-mail, which is the id of the recruiter
		Map<String, CcpJsonRepresentation> recruitersByEmail = new LinkedHashMap<>();
		List<String> invalidEmails = new ArrayList<>();
		String telegramSource = VisEntityRecruiter.Sources.telegram.getValue();
		for (String foundEmail : foundEmails) {
			CcpJsonRepresentation recruiterEmail = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiter.Fields.email, foundEmail);
			CcpJsonRepresentation recruiterInput = recruiterEmail.put(VisEntityRecruiter.Fields.source, telegramSource);
			try {
				CcpJsonRepresentation preparedRecruiter = VisBusinessPrepareRecruiter.INSTANCE.execute(recruiterInput);
				String normalizedEmail = preparedRecruiter.getAsString(VisEntityRecruiter.Fields.email);
				CcpFieldName emailField = new CcpFieldName(normalizedEmail);
				boolean appearedRecently = recentAppearances.containsAllFields(emailField);
				CcpJsonRepresentation recruiter = appearedRecently ? preparedRecruiter.put(VisEntityRecruiter.Fields.lastTelegramAppearance, recentAppearances.getAsLongNumber(emailField)) : preparedRecruiter;
				recruitersByEmail.put(normalizedEmail, recruiter);
			} catch (CcpJsonValidationError e) {
				invalidEmails.add(foundEmail);
			}
		}

		List<CcpJsonRepresentation> recruiters = new ArrayList<>(recruitersByEmail.values());
		int createdCount = this.createNewRecruiters(recruiters);
		CcpJsonRepresentation appearancesInput = VisBusinessSaveRecruitersActivity.input(VisEntityRecruiter.Fields.lastTelegramAppearance, recentAppearances);
		CcpJsonRepresentation appearancesSaved = VisBusinessSaveRecruitersActivity.INSTANCE.execute(appearancesInput);
		Integer appearancesUpdatedCount = appearancesSaved.getAsIntegerNumber(VisBusinessSaveRecruitersActivity.JsonFieldNames.updated);
		int recruitersCount = recruiters.size();
		int alreadyExistingCount = recruitersCount - createdCount;
		int filesReadCount = filesRead.size();
		int emailsFoundCount = foundEmails.size();

		CcpJsonRepresentation jsonWithFilesRead = json.put(JsonFieldNames.filesRead, filesReadCount);
		CcpJsonRepresentation jsonWithEmailsFound = jsonWithFilesRead.put(JsonFieldNames.emailsFound, emailsFoundCount);
		CcpJsonRepresentation jsonWithInvalidEmails = jsonWithEmailsFound.put(JsonFieldNames.invalidEmails, invalidEmails);
		CcpJsonRepresentation jsonWithCreated = jsonWithInvalidEmails.put(JsonFieldNames.created, createdCount);
		CcpJsonRepresentation jsonWithAlreadyExisting = jsonWithCreated.put(JsonFieldNames.alreadyExisting, alreadyExistingCount);
		CcpJsonRepresentation jsonWithAppearances = jsonWithAlreadyExisting.put(JsonFieldNames.appearancesUpdated, appearancesUpdatedCount);
		return jsonWithAppearances;
	}

	/**
	 * Adds the e-mails of the file to the found ones, when it is an HTML or text file, and keeps the day of the most recent
	 * message of each e-mail.
	 * @param file an entry of the folder
	 * @param foundEmails the e-mails found so far
	 * @param filesRead the files read so far
	 * @param appearancesByFoundEmail the midnight of the day of the most recent message of each e-mail, as found
	 */
	private void readEmails(CcpFileDecorator file, Set<String> foundEmails, List<String> filesRead, Map<String, Long> appearancesByFoundEmail) {
		String fileName = file.getName();
		String lowerFileName = fileName.toLowerCase();
		String htmlExtension = JsonFieldNames.HTML_EXTENSION.getValue();
		String htmExtension = JsonFieldNames.HTM_EXTENSION.getValue();
		String txtExtension = JsonFieldNames.TXT_EXTENSION.getValue();
		boolean fileToRead = file.isFile() && (lowerFileName.endsWith(htmlExtension) || lowerFileName.endsWith(htmExtension) || lowerFileName.endsWith(txtExtension));
		boolean notAFileToRead = false == fileToRead;

		if(notAFileToRead) {
			return;
		}

		String content = file.getStringContent();
		CcpStringDecorator contentText = new CcpStringDecorator(content);
		CcpEmailDecorator contentEmails = contentText.email();
		Set<String> fileEmails = contentEmails.findAllInText();
		foundEmails.addAll(fileEmails);
		filesRead.add(fileName);

		boolean exportedFromWhatsApp = lowerFileName.endsWith(txtExtension);
		List<CcpJsonRepresentation> messages = exportedFromWhatsApp ? VisWhatsAppMessages.read(content) : VisTelegramMessages.read(content);
		String dayPattern = JsonFieldNames.DAY_OF_THE_MESSAGE.getValue();
		for (CcpJsonRepresentation message : messages) {
			String text = message.getAsString(CcpJsonCommonsFields.text);
			CcpStringDecorator messageText = new CcpStringDecorator(text);
			CcpEmailDecorator messageEmails = messageText.email();
			Set<String> emailsOfTheMessage = messageEmails.findAllInText();
			boolean noEmail = emailsOfTheMessage.isEmpty();
			if(noEmail) {
				continue;
			}
			String dayOfTheMessage = message.getAsString(JnJsonCommonsFields.disposablePeriod);
			CcpTimeDecorator day = new CcpTimeDecorator(dayOfTheMessage, dayPattern);
			Long appearance = day.getContent();
			for (String email : emailsOfTheMessage) {
				appearancesByFoundEmail.merge(email, appearance, Math::max);
			}
		}
	}

	/**
	 * Creates, a batch at a time, the recruiters that are neither in the entity nor in its twin.
	 * @param recruiters the recruiters, already transformed and validated
	 * @return how many were created
	 */
	private int createNewRecruiters(List<CcpJsonRepresentation> recruiters) {
		CcpEntity inactiveRecruiterEntity = VisEntityRecruiter.ENTITY.getTwinEntity();
		CcpEntity[] entitiesThatPreventCreation = {VisEntityRecruiter.ENTITY, inactiveRecruiterEntity};
		int createdCount = 0;
		int recruitersCount = recruiters.size();

		for (int start = 0; start < recruitersCount; start += BATCH_SIZE) {
			int end = Math.min(start + BATCH_SIZE, recruitersCount);
			List<CcpJsonRepresentation> batch = recruiters.subList(start, end);
			CcpJsonRepresentation[] batchArray = batch.toArray(CcpJsonRepresentation[]::new);
			List<JnBulkCreateResult> results = JnExecuteBulkOperation.INSTANCE.executeCreateBulk(VisEntityRecruiter.ENTITY, entitiesThatPreventCreation, JnDeleteKeysFromCache.INSTANCE, batchArray);
			Stream<JnBulkCreateResult> resultsStream = results.stream();
			Stream<JnBulkCreateResult> createdResultsStream = resultsStream.filter(result -> result.created);
			Stream<CcpJsonRepresentation> createdRecruitersStream = createdResultsStream.map(result -> result.json);
			List<CcpJsonRepresentation> createdRecruiters = createdRecruitersStream.collect(Collectors.toList());
			// the bulk goes around the decorator of @CcpEntityGrouperChild: the new recruiters enter their groups here
			VisRecruiterDomainGroups domainGroups = new VisRecruiterDomainGroups(createdRecruiters);
			domainGroups.addToTheGroups();
			int batchCreatedCount = createdRecruiters.size();
			createdCount += batchCreatedCount;
		}
		return createdCount;
	}
}
