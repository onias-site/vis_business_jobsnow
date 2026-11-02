package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpHashDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.query.CcpQueryExecutor;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.hash.CcpHashAlgorithm;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.business.messages.JnMessageType;
import com.jn.entities.JnEntityEmailSuppressed;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.messages.JnSendMessageToUser;
import com.vis.entities.VisEntityResume;
import com.vis.messages.VisMessages;

/**
 * Runs once, at the deploy: invites each candidate of the legacy index {@code profissionais2} to register the resume in
 * the new form, with a preview of how the recruiters will see it; the status of the legacy index does not matter. Left
 * out: the address that is not valid, the one who already has a resume (active or inactive), the one already invited and
 * the suppressed address.
 *
 * <p>The sending throws on a failure of the provider, so a key refused does not burn the list: only an e-mail accepted is
 * handed to {@code onInvited}, and a new run goes on from where this one stopped.
 *
 * <p>Input: {@code frontendUrl}. Output: the counters of the run.
 */
public class VisBusinessInviteLegacyCandidates implements CcpBusiness {

	/** The legacy index. */
	static enum Constants implements CcpJsonFieldName {
		LEGACY_INDEX { public String getValue() { return "profissionais2"; } },
		SCROLL_TIME { public String getValue() { return "5m"; } },
		RESUME_FORM_PATH { public String getValue() { return "{frontendUrl}/meus-dados/sobre-mim"; } },
	}

	/** The size of each page of the reading. */
	static final int PAGE_SIZE = 200;

	/** The hashes of the e-mails already invited by the runs before this one. */
	private final Set<String> alreadyInvited;

	/** Receives the hash of each e-mail accepted by the provider. */
	private final Consumer<String> onInvited;

	/** The most e-mails this run sends. */
	private final int limit;

	/**
	 * Keeps how this run goes.
	 * @param alreadyInvited the hashes of the e-mails already invited by the runs before this one
	 * @param onInvited receives the hash of each e-mail accepted by the provider
	 * @param limit the most e-mails this run sends
	 */
	public VisBusinessInviteLegacyCandidates(Set<String> alreadyInvited, Consumer<String> onInvited, int limit) {
		this.alreadyInvited = alreadyInvited;
		this.onInvited = onInvited;
		this.limit = limit;
	}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String frontendUrl = json.getAsString(VisRecruiterResumesFields.frontendUrl);
		String resumeFormLink = VisBusinessSendResumesToRecruiters.resolve(Constants.RESUME_FORM_PATH, json);
		List<CcpJsonRepresentation> candidates = getLegacyCandidates();
		Set<String> withResume = getEmailsWithResume();

		List<CcpJsonRepresentation> toInvite = new ArrayList<>();
		int invalid = 0;
		int hasResume = 0;
		int invitedBefore = 0;
		for (CcpJsonRepresentation candidate : candidates) {
			String legacyEmail = candidate.getAsString(VisBusinessBuildLegacyCandidateTable.Legacy.id);
			String email = legacyEmail.trim().toLowerCase();
			CcpStringDecorator emailText = new CcpStringDecorator(email);
			CcpEmailDecorator emailDecorator = emailText.email();
			boolean validAddress = emailDecorator.isValid();
			if(false == validAddress) {
				invalid++;
				continue;
			}
			CcpHashDecorator emailHashDecorator = emailDecorator.hash();
			String emailHash = emailHashDecorator.asString(CcpHashAlgorithm.SHA1);
			if(withResume.contains(emailHash)) {
				hasResume++;
				continue;
			}
			if(this.alreadyInvited.contains(emailHash)) {
				invitedBefore++;
				continue;
			}
			CcpJsonRepresentation candidateToInvite = candidate
					.put(VisBusinessBuildLegacyCandidateTable.Legacy.id, email)
					.put(VisRecruiterResumesFields.emailHash, emailHash);
			toInvite.add(candidateToInvite);
		}

		int sent = 0;
		int suppressed = 0;
		for (CcpJsonRepresentation candidate : toInvite) {
			boolean limitReached = sent >= this.limit;
			if(limitReached) {
				break;
			}
			String email = candidate.getAsString(VisBusinessBuildLegacyCandidateTable.Legacy.id);
			CcpJsonRepresentation suppressionKey = CcpOtherConstants.EMPTY_JSON.put(JnJsonCommonsFields.email, email);
			boolean suppressedAddress = JnEntityEmailSuppressed.ENTITY.exists(suppressionKey);
			if(suppressedAddress) {
				suppressed++;
				continue;
			}
			CcpJsonRepresentation tableInput = CcpOtherConstants.EMPTY_JSON
					.put(VisRecruiterResumesFields.legacyCandidate, candidate)
					.put(JnJsonCommonsFields.email, email)
					.put(VisRecruiterResumesFields.frontendUrl, frontendUrl);
			CcpJsonRepresentation withTable = VisBusinessBuildLegacyCandidateTable.INSTANCE.execute(tableInput);
			String resumesTable = withTable.getAsString(VisRecruiterResumesFields.resumesTable);
			sendTheInvitation(email, resumesTable, resumeFormLink);
			String emailHash = candidate.getAsString(VisRecruiterResumesFields.emailHash);
			this.onInvited.accept(emailHash);
			sent++;
		}

		int legacyCandidates = candidates.size();
		CcpJsonRepresentation counters = json
				.put(LegacyCounters.legacyCandidates, legacyCandidates)
				.put(LegacyCounters.invalidAddress, invalid)
				.put(LegacyCounters.hasResume, hasResume)
				.put(LegacyCounters.invitedBefore, invitedBefore)
				.put(LegacyCounters.suppressed, suppressed)
				.put(VisRecruiterResumesFields.emailsSent, sent);
		return counters;
	}

	/** The counters of the run. */
	public static enum LegacyCounters implements CcpJsonFieldName {
		legacyCandidates,
		invalidAddress,
		hasResume,
		invitedBefore,
		suppressed,
	}

	/**
	 * Sends the invitation. {@code THROWS}: a failure stops the run, so that the next one goes on from this candidate.
	 * @param email the candidate
	 * @param resumesTable the table with the row of the candidate
	 * @param resumeFormLink the address of the form of the resume
	 */
	static void sendTheInvitation(String email, String resumesTable, String resumeFormLink) {
		CcpJsonRepresentation message = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.email, email)
				.put(JnJsonCommonsFields.language, VisBusinessBuildLegacyCandidateTable.LANGUAGE)
				.put(VisRecruiterResumesFields.resumesTable, resumesTable)
				.put(VisRecruiterResumesFields.resumeFormLink, resumeFormLink);
		String topic = VisMessages.VisInviteLegacyCandidate.class.getName();
		JnMessageType[] messageTypes = {JnMessageType.email};
		new JnSendMessageToUser().sendAllMessages(message, topic, messageTypes, JnMessageSenderExceptionHandler.THROWS);
	}

	/** Every document of the legacy index, only with the fields used: the file of the resume weighs hundreds of megabytes. */
	private static List<CcpJsonRepresentation> getLegacyCandidates() {
		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		CcpQueryOptions everyCandidate = CcpQueryOptions.INSTANCE.matchAll();
		String[] legacyIndices = {Constants.LEGACY_INDEX.getValue()};
		String scrollTime = Constants.SCROLL_TIME.getValue();
		VisBusinessBuildLegacyCandidateTable.Legacy[] legacyFields = VisBusinessBuildLegacyCandidateTable.Legacy.values();
		List<String> fields = new ArrayList<>();
		for (VisBusinessBuildLegacyCandidateTable.Legacy legacyField : legacyFields) {
			String field = legacyField.getValue();
			fields.add(field);
		}
		String[] fieldsArray = fields.toArray(new String[fields.size()]);
		List<CcpJsonRepresentation> candidates = new ArrayList<>();
		queryExecutor.consumeQueryResult(everyCandidate, legacyIndices, scrollTime, PAGE_SIZE, candidate -> candidates.add(candidate), fieldsArray);
		return candidates;
	}

	/** The hashes of the e-mails of every resume, active or inactive. */
	private static Set<String> getEmailsWithResume() {
		CcpEntity inactiveResume = VisEntityResume.ENTITY.getTwinEntity();
		CcpEntity[] resumeEntities = {VisEntityResume.ENTITY, inactiveResume};
		Set<String> emails = new HashSet<>();
		String scrollTime = Constants.SCROLL_TIME.getValue();
		String emailField = VisEntityResume.Fields.email.getValue();
		CcpQueryOptions everyResume = CcpQueryOptions.INSTANCE.matchAll();
		everyResume.selectFrom(resumeEntities).consumeQueryResult(scrollTime, PAGE_SIZE, resume -> emails.add(resume.getAsString(VisEntityResume.Fields.email)), emailField);
		return emails;
	}
}
