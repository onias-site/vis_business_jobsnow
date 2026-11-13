package com.vis.business.recruiter.resumes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpHashDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTextDecorator;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.decorators.CcpUrlDecorator;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.hash.CcpHashAlgorithm;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;
import com.jn.business.messages.JnMessageSenderExceptionHandler;
import com.jn.business.messages.JnMessageType;
import com.jn.entities.JnEntityEmailParametersToSend;
import com.jn.entities.JnEntityVersionable;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.messages.JnSendMessageToUser;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityRecruiterFirstEmail;
import com.vis.entities.VisEntityRecruiterQueueCursor;
import com.vis.entities.VisEntityRecruiterResumes;
import com.vis.entities.VisEntityRecruiterSignUpReturn;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityResumeRestriction;
import com.vis.entities.VisEntityViewedResume;
import com.vis.json.transformers.VisJsonTransformerLinkedinHash;
import com.vis.json.transformers.VisJsonTransformerRecruiterDomain;
import com.vis.json.transformers.VisJsonTransformerRecruiterProfessional;
import com.vis.messages.VisMessages;

/**
 * The daily process (Monday to Friday, as the legacy one) that sends each recruiter of {@code vis_recruiter} the table
 * of the resumes added or changed in the last 7 days (the dates come from the history, {@code jn_versionable}).
 *
 * <ul>
 * <li>Who receives: the recruiters who never viewed a resume of these e-mails and the ones whose last view is less than
 * 35 days old; who stopped viewing for longer is inactivated by this process (moved to {@code vis_inactive_recruiter}) and
 * comes back on viewing a resume again; signing up again counts as a view ({@code vis_recruiter_sign_up_return}). Each day, only
 * {@value #RECRUITERS_PER_DAY} of them: first the ones who viewed a resume in these 35 days, the most recent view first;
 * then the recruiters of the sign-up (source {@code login}) who never received this e-mail
 * ({@code vis_recruiter_first_email}); then the ones who appeared on Telegram in the last six months
 * ({@code lastTelegramAppearance}), the most recent appearance first; then, in turns, the ones who viewed a resume in the
 * legacy JobsNow ({@code lastLegacyView}), the most recent view first, a queue of its own that goes on from where the last
 * run stopped and starts over at its end. The places left go to the queue of the ones who never viewed: the sign-up and then the others,
 * each group in the order of the SHA-1 of the e-mails, which favors no name or domain. The queue goes on from where the last run
 * stopped ({@code vis_recruiter_queue_cursor}) and starts over at its end, so whoever received the e-mail and viewed
 * nothing gives their place to the next one; a run restricted to some recruiters does not move it.</li>
 * <li>Which resumes: every resume added or changed in the last 7 days, except, for a recruiter of a professional
 * domain, the ones whose {@code notAllowedCompany} names the company of the domain, and, since 2026-11-12, the ones whose
 * view by this recruiter is in {@code vis_inactive_viewed_resume}, the twin of {@code vis_viewed_resume}.</li>
 * <li>In which order: first the ones with at least {@value #MINIMUM_SKILLS} skills; then the ones viewed by more
 * recruiters; then the lowest average of the rankings of the skills; then type of resume, CLT, PJ, years of
 * experience.</li>
 * <li>The e-mail shows the first {@value #RESUMES_IN_THE_EMAIL}; its subject and its text count every resume of the list
 * (the ones left after every filter); the whole list goes, before the e-mail, to {@code vis_recruiter_resumes}, read by
 * id by the screen "see all" of the front end (OQL).</li>
 * </ul>
 *
 * <p>Input: optionally {@code recruiters}, the e-mails the run is restricted to (support and tests). Output: the
 * counters of the run.
 */
public class VisBusinessSendResumesToRecruiters implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessSendResumesToRecruiters INSTANCE = new VisBusinessSendResumesToRecruiters();

	private VisBusinessSendResumesToRecruiters() {}

	/** The days of resumes of each e-mail. */
	static final int DAYS_OF_RESUMES = 7;

	/** The days without a view after which the recruiter stops receiving the e-mail. */
	static final int DAYS_WITHOUT_VIEWS = 35;

	/**
	 * How many resumes the e-mail shows; the others are in the screen "see all". 30 since 2026-11-12 (it was 40): with 40
	 * rows the HTML reached about 107 KB, beyond the 102 KB from which Gmail clips the message.
	 */
	public static final int RESUMES_IN_THE_EMAIL = 30;

	/** How many recruiters receive the e-mail each day. */
	public static final int RECRUITERS_PER_DAY = 2000;

	/** The skills from which a resume goes before the ones that have fewer. */
	static final int MINIMUM_SKILLS = 10;

	/** The shortest company name compared with a part of the domain of the recruiter. */
	static final int SHORTEST_COMPANY_NAME = 4;

	/** The language of the e-mails for now: the recruiters have no language yet. */
	static final JnLanguage LANGUAGE = JnLanguage.portuguese;

	/** The texts that are not messages. */
	static enum Constants implements CcpJsonFieldName {
		SCROLL_TIME { public String getValue() { return "1m"; } },
		SEPARATOR { public String getValue() { return "|"; } },
		NOT_LETTERS_OR_DIGITS { public String getValue() { return "[^a-z0-9]"; } },
		DOMAIN_PARTS_SEPARATORS { public String getValue() { return "[.\\-]"; } },
		LINK { public String getValue() { return "<a href=\"{frontendUrl}/meu-recrutamento/curriculos-da-semana?email={email}\" target=\"_blank\">{value}</a>"; } },
		UNSUBSCRIBE_PATH { public String getValue() { return "{frontendUrl}/recrutador/descadastrar?email={email}&token={token}"; } },
		LIST_UNSUBSCRIBE { public String getValue() { return "List-Unsubscribe"; } },
		LIST_UNSUBSCRIBE_POST { public String getValue() { return "List-Unsubscribe-Post"; } },
		LIST_UNSUBSCRIBE_VALUE { public String getValue() { return "<{visApiUrl}/recruiter-email-link/one-click-unsubscribe?email={email}&token={token}>"; } },
		LIST_UNSUBSCRIBE_POST_VALUE { public String getValue() { return "List-Unsubscribe=One-Click"; } },
		QUEUE_GROUP_OF_THE_SIGN_UP { public String getValue() { return "0"; } },
		QUEUE_GROUP_OF_THE_OTHERS { public String getValue() { return "1"; } },
		NO_QUEUE_POSITION { public String getValue() { return ""; } },
		LEGACY_QUEUE_POSITION { public String getValue() { return "%013d%s"; } },
		COMMA_SEPARATOR { public String getValue() { return ", "; } },
	}

	/** How many skills the subject of the e-mail names, besides the fluent English and Spanish, which do not count. */
	static final int SKILLS_IN_THE_SUBJECT = 10;

	/** The group of the fluent English and Spanish in the order of the subject: the first, as in the column. */
	static final int SUBJECT_GROUP_TOP_LANGUAGES = 0;

	/** The group of the skills in the order of the subject. */
	static final int SUBJECT_GROUP_SKILLS = 1;

	/**
	 * The greatest moment, in milliseconds, of 13 digits: the position of a recruiter in the queue of the legacy views is
	 * this number minus their last view, so the most recent view comes first in the order of the text.
	 */
	static final long GREATEST_MOMENT_OF_13_DIGITS = 9_999_999_999_999L;

	/** The size of each page of the readings. */
	static final int PAGE_SIZE = 1000;

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpTimeDecorator now = new CcpTimeDecorator();
		boolean weekend = now.isWeekend();

		if(weekend) {
			CcpJsonRepresentation skipped = json.put(VisRecruiterResumesFields.weekend, true);
			return skipped;
		}

		long resumesSince = now.getDaysAgo(DAYS_OF_RESUMES);
		Set<String> changedResumes = getRecordsChangedSince(VisEntityResume.ENTITY.getEntityMetaData().entityName, resumesSince, VisEntityResume.Fields.email);
		boolean noResumes = changedResumes.isEmpty();

		if(noResumes) {
			CcpJsonRepresentation nothingToSend = json.put(VisRecruiterResumesFields.resumesFound, 0).put(VisRecruiterResumesFields.emailsSent, 0);
			return nothingToSend;
		}

		List<CcpJsonRepresentation> resumes = getResumes(changedResumes);
		CcpJsonRepresentation resumesJson = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.resumes, resumes);
		CcpJsonRepresentation withRankings = VisBusinessLoadSkillRankings.INSTANCE.execute(resumesJson);
		CcpJsonRepresentation rankings = withRankings.getInnerJson(VisRecruiterResumesFields.rankings);
		Map<String, long[]> resumeDates = getResumeDates(changedResumes);

		long viewsSince = now.getDaysAgo(DAYS_WITHOUT_VIEWS);
		Map<String, Long> lastViewByPair = new HashMap<>();
		Map<String, Long> lastViewByRecruiter = new HashMap<>();
		loadViews(lastViewByPair, lastViewByRecruiter);
		Map<String, Long> signUpReturns = getSignUpReturns();
		countTheSignUpReturnsAsViews(lastViewByRecruiter, signUpReturns);
		Map<String, String> aliasByPair = getAliases();
		Set<String> inactivePairs = getInactivePairs();

		Map<String, Long> viewsByResume = getViewsByResume(lastViewByPair);
		List<CcpJsonRepresentation> resumesOfTheEmail = getResumesOfTheEmail(resumes, rankings, resumeDates, viewsByResume);

		CcpJsonRepresentation linkAddresses = getLinkAddresses();
		String frontendUrl = linkAddresses.getAsString(VisRecruiterResumesFields.frontendUrl);
		String visApiUrl = linkAddresses.getAsString(VisRecruiterResumesFields.visApiUrl);
		List<String> restrictedTo = json.getOrDefault(VisRecruiterResumesFields.recruiters, () -> List.of());
		int[] emailsSent = {0};
		VisEntityRecruiterQueueCursor.CursorNames generalQueue = VisEntityRecruiterQueueCursor.CursorNames.resumesToRecruiters;
		VisEntityRecruiterQueueCursor.CursorNames legacyQueue = VisEntityRecruiterQueueCursor.CursorNames.legacyViewersOfResumesToRecruiters;
		String lastPositionOfTheLastRun = getLastQueuePosition(generalQueue);
		String lastLegacyPositionOfTheLastRun = getLastQueuePosition(legacyQueue);
		String[] lastQueuePosition = {lastPositionOfTheLastRun};
		String[] lastLegacyQueuePosition = {lastLegacyPositionOfTheLastRun};
		long telegramSince = now.getDaysAgo(VisEntityRecruiter.DAYS_OF_A_RECENT_TELEGRAM_APPEARANCE);

		Consumer<CcpJsonRepresentation> sendToTheRecruiter = new VisResumesToTheRecruiterSender(restrictedTo, resumesOfTheEmail, lastViewByPair, aliasByPair, inactivePairs, frontendUrl, visApiUrl, emailsSent, lastQueuePosition, lastLegacyQueuePosition);
		List<CcpJsonRepresentation> recruiters = getRecruiters();
		Set<String> recruitersWithFirstEmail = getRecruitersWithFirstEmail();
		List<CcpJsonRepresentation> recruitersInOrder = getRecruitersInOrder(recruiters, lastViewByRecruiter, viewsSince, lastPositionOfTheLastRun, recruitersWithFirstEmail, telegramSince, lastLegacyPositionOfTheLastRun);
		recruitersInOrder.forEach(sendToTheRecruiter);
		saveLastQueuePosition(generalQueue, restrictedTo, lastPositionOfTheLastRun, lastQueuePosition[0]);
		saveLastQueuePosition(legacyQueue, restrictedTo, lastLegacyPositionOfTheLastRun, lastLegacyQueuePosition[0]);
		List<CcpJsonRepresentation> recruitersWhoStoppedViewing = getRecruitersWhoStoppedViewing(recruiters, lastViewByRecruiter, viewsSince, restrictedTo);
		// deleting from the recruiters moves the given record to the twin, the inactive recruiters
		recruitersWhoStoppedViewing.forEach(recruiter -> VisEntityRecruiter.ENTITY.delete(recruiter));

		int resumesFound = resumes.size();
		int recruitersInactivated = recruitersWhoStoppedViewing.size();
		CcpJsonRepresentation counters = json
				.put(VisRecruiterResumesFields.resumesFound, resumesFound)
				.put(VisRecruiterResumesFields.emailsSent, emailsSent[0])
				.put(VisRecruiterResumesFields.recruitersInactivated, recruitersInactivated);
		return counters;
	}

	/**
	 * The recruiters to inactivate: the ones whose last view is older than {@code viewsSince}. A recruiter who never viewed
	 * is not one of them, and a run restricted to some recruiters touches only these.
	 * @param recruiters every recruiter
	 * @param lastViewByRecruiter the last view of each recruiter, by the hash of their e-mail
	 * @param viewsSince the time before which a last view means the recruiter stopped viewing
	 * @param restrictedTo the e-mails the run is restricted to; empty for every recruiter
	 * @return the records of the recruiters to inactivate
	 */
	static List<CcpJsonRepresentation> getRecruitersWhoStoppedViewing(List<CcpJsonRepresentation> recruiters, Map<String, Long> lastViewByRecruiter, long viewsSince, List<String> restrictedTo) {
		List<CcpJsonRepresentation> recruitersWhoStoppedViewing = new ArrayList<>();
		for (CcpJsonRepresentation recruiter : recruiters) {
			String email = recruiter.getAsString(VisEntityRecruiter.Fields.email);
			String emailHash = getSha1OfTheEmail(email);
			Long lastView = lastViewByRecruiter.get(emailHash);
			boolean stillViewing = lastView == null || lastView >= viewsSince;
			if(stillViewing) {
				continue;
			}
			boolean outOfThisRun = false == restrictedTo.isEmpty() && false == restrictedTo.contains(email);
			if(outOfThisRun) {
				continue;
			}
			recruitersWhoStoppedViewing.add(recruiter);
		}
		return recruitersWhoStoppedViewing;
	}

	/** The SHA-1 of an e-mail, which does not refuse an address that is not valid, as {@link #getEmailHash(String)} does. */
	private static String getSha1OfTheEmail(String email) {
		CcpStringDecorator emailDecorator = new CcpStringDecorator(email);
		CcpHashDecorator emailHashDecorator = emailDecorator.hash();
		String emailHash = emailHashDecorator.asString(CcpHashAlgorithm.SHA1);
		return emailHash;
	}

	/**
	 * Every recruiter, read before the first e-mail goes out: sending while reading would hold the scroll open for as long
	 * as the e-mails of a page take.
	 */
	static List<CcpJsonRepresentation> getRecruiters() {
		CcpQueryOptions everyRecruiter = CcpQueryOptions.INSTANCE.matchAll();
		List<CcpJsonRepresentation> recruiters = new ArrayList<>();
		String scrollTime = Constants.SCROLL_TIME.getValue();
		everyRecruiter.selectFrom(VisEntityRecruiter.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, recruiter -> recruiters.add(recruiter));
		return recruiters;
	}

	/** The hashes of the e-mails of the recruiters of the sign-up who already received their first e-mail. */
	private static Set<String> getRecruitersWithFirstEmail() {
		CcpQueryOptions everyRecord = CcpQueryOptions.INSTANCE.matchAll();
		Set<String> recruitersWithFirstEmail = new LinkedHashSet<>();
		String scrollTime = Constants.SCROLL_TIME.getValue();
		everyRecord.selectFrom(VisEntityRecruiterFirstEmail.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, record -> recruitersWithFirstEmail.add(record.getAsString(VisEntityRecruiterFirstEmail.Fields.email)));
		return recruitersWithFirstEmail;
	}

	/** When each inactive recruiter came back by signing up again, by the hash of their e-mail. */
	private static Map<String, Long> getSignUpReturns() {
		CcpQueryOptions everyRecord = CcpQueryOptions.INSTANCE.matchAll();
		Map<String, Long> signUpReturns = new HashMap<>();
		String scrollTime = Constants.SCROLL_TIME.getValue();
		everyRecord.selectFrom(VisEntityRecruiterSignUpReturn.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, record -> signUpReturns.put(record.getAsString(VisEntityRecruiterSignUpReturn.Fields.email), record.getAsLongNumber(VisEntityRecruiterSignUpReturn.Fields.returnedAt)));
		return signUpReturns;
	}

	/**
	 * Takes a return by the sign-up as the last view of a recruiter, when it is more recent, so that signing up again gives
	 * the recruiter another 35 days. Who never viewed stays without a view: they are in the queue, and no one inactivates them.
	 * @param lastViewByRecruiter the last view of each recruiter, by the hash of their e-mail; receives the returns
	 * @param signUpReturns when each recruiter came back, by the hash of their e-mail
	 */
	static void countTheSignUpReturnsAsViews(Map<String, Long> lastViewByRecruiter, Map<String, Long> signUpReturns) {
		for (Map.Entry<String, Long> signUpReturn : signUpReturns.entrySet()) {
			String emailHash = signUpReturn.getKey();
			Long lastView = lastViewByRecruiter.get(emailHash);
			boolean neverViewed = lastView == null;
			if(neverViewed) {
				continue;
			}
			Long returnedAt = signUpReturn.getValue();
			long mostRecent = Math.max(lastView, returnedAt);
			lastViewByRecruiter.put(emailHash, mostRecent);
		}
	}

	/** Where the last run stopped in the queue, or nothing when no run moved it yet. */
	static String getLastQueuePosition(VisEntityRecruiterQueueCursor.CursorNames queue) {
		CcpJsonRepresentation cursorKey = getCursorKey(queue);
		CcpEntityMetaData cursorMetaData = VisEntityRecruiterQueueCursor.ENTITY.getEntityMetaData();
		CcpJsonRepresentation cursor = cursorMetaData.getOneByIdOrHandleItIfThisIdWasNotFound(cursorKey, notFound -> CcpOtherConstants.EMPTY_JSON);
		String lastPosition = cursor.getOrDefault(VisEntityRecruiterQueueCursor.Fields.lastPosition, () -> Constants.NO_QUEUE_POSITION.getValue());
		return lastPosition;
	}

	/** Keeps where this run stopped in the queue, unless it was restricted to some recruiters or the queue did not move. */
	static void saveLastQueuePosition(VisEntityRecruiterQueueCursor.CursorNames queue, List<String> restrictedTo, String lastPositionOfTheLastRun, String lastPosition) {
		boolean restrictedRun = false == restrictedTo.isEmpty();
		boolean queueDidNotMove = lastPosition.equals(lastPositionOfTheLastRun);
		if(restrictedRun || queueDidNotMove) {
			return;
		}
		CcpJsonRepresentation cursorKey = getCursorKey(queue);
		CcpJsonRepresentation cursor = cursorKey.put(VisEntityRecruiterQueueCursor.Fields.lastPosition, lastPosition);
		VisEntityRecruiterQueueCursor.ENTITY.save(cursor);
	}

	private static CcpJsonRepresentation getCursorKey(VisEntityRecruiterQueueCursor.CursorNames queue) {
		String cursorName = queue.getValue();
		CcpJsonRepresentation cursorKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiterQueueCursor.Fields.cursorName, cursorName);
		return cursorKey;
	}

	/**
	 * The order in which the places of the day are given:
	 * <ol>
	 * <li>the recruiters who viewed a resume since {@code viewsSince}, the most recent view first;</li>
	 * <li>the ones of the sign-up waiting for their first e-mail, by the hash;</li>
	 * <li>the ones who appeared on Telegram since {@code telegramSince}, the most recent appearance first;</li>
	 * <li>the ones who viewed a resume in the legacy JobsNow, the most recent view first, right after
	 * {@code lastLegacyQueuePosition} and going back to the beginning at its end;</li>
	 * <li>the queue of the ones who never viewed, right after {@code lastQueuePosition} and going back to the beginning at
	 * its end.</li>
	 * </ol>
	 * Each recruiter goes to the first group they fit. Who viewed before {@code viewsSince} stopped viewing and is left out,
	 * whatever group they would fit: the process inactivates them after the sending. The hash is calculated here and not
	 * read from an entity: {@link #getEmailHash(String)} refuses an address that is not valid, and one such record would stop
	 * the whole run.
	 * @param recruiters every recruiter
	 * @param lastViewByRecruiter the last view of each recruiter, by the hash of their e-mail
	 * @param viewsSince the time before which a last view means the recruiter stopped viewing
	 * @param lastQueuePosition where the last run stopped in the queue of the ones who never viewed
	 * @param recruitersWithFirstEmail the hashes of the e-mails of the recruiters of the sign-up who already received the first e-mail
	 * @param telegramSince the time before which an appearance on Telegram is no longer recent
	 * @param lastLegacyQueuePosition where the last run stopped in the queue of the legacy views
	 * @return the recruiters in order, each one with {@code emailHash}; the ones of the queue with {@code queuePosition}, the
	 * ones of the legacy views with {@code legacyQueuePosition}, the ones waiting for their first e-mail with {@code firstEmail}
	 */
	static List<CcpJsonRepresentation> getRecruitersInOrder(List<CcpJsonRepresentation> recruiters, Map<String, Long> lastViewByRecruiter, long viewsSince, String lastQueuePosition, Set<String> recruitersWithFirstEmail, long telegramSince, String lastLegacyQueuePosition) {
		String loginSource = VisEntityRecruiter.Sources.login.getValue();
		String signUpGroup = Constants.QUEUE_GROUP_OF_THE_SIGN_UP.getValue();
		String othersGroup = Constants.QUEUE_GROUP_OF_THE_OTHERS.getValue();
		String legacyPositionFormat = Constants.LEGACY_QUEUE_POSITION.getValue();
		List<CcpJsonRepresentation> recentInTelegram = new ArrayList<>();
		List<CcpJsonRepresentation> legacyViewers = new ArrayList<>();
		List<CcpJsonRepresentation> viewers = new ArrayList<>();
		List<CcpJsonRepresentation> waitingForTheFirstEmail = new ArrayList<>();
		List<CcpJsonRepresentation> queue = new ArrayList<>();
		for (CcpJsonRepresentation recruiter : recruiters) {
			String email = recruiter.getAsString(VisEntityRecruiter.Fields.email);
			String emailHash = getSha1OfTheEmail(email);
			CcpJsonRepresentation recruiterWithHash = recruiter.put(VisRecruiterResumesFields.emailHash, emailHash);
			Long lastView = lastViewByRecruiter.get(emailHash);
			boolean stoppedViewing = lastView != null && lastView < viewsSince;
			if(stoppedViewing) {
				continue;
			}
			boolean viewedRecently = lastView != null;
			if(viewedRecently) {
				viewers.add(recruiterWithHash);
				continue;
			}
			String source = recruiter.getOrDefault(VisEntityRecruiter.Fields.source, () -> "");
			boolean cameFromTheSignUp = loginSource.equals(source);
			boolean waitsForTheFirstEmail = cameFromTheSignUp && false == recruitersWithFirstEmail.contains(emailHash);
			if(waitsForTheFirstEmail) {
				CcpJsonRepresentation recruiterOfTheFirstEmail = recruiterWithHash.put(VisRecruiterResumesFields.firstEmail, true);
				waitingForTheFirstEmail.add(recruiterOfTheFirstEmail);
				continue;
			}
			boolean appearedOnTelegram = recruiter.containsAllFields(VisEntityRecruiter.Fields.lastTelegramAppearance);
			long telegramAppearance = appearedOnTelegram ? recruiter.getAsLongNumber(VisEntityRecruiter.Fields.lastTelegramAppearance) : 0L;
			boolean appearedRecentlyOnTelegram = appearedOnTelegram && telegramAppearance >= telegramSince;
			if(appearedRecentlyOnTelegram) {
				CcpJsonRepresentation recruiterOfTelegram = recruiterWithHash.put(VisEntityRecruiter.Fields.lastTelegramAppearance, telegramAppearance);
				recentInTelegram.add(recruiterOfTelegram);
				continue;
			}
			boolean viewedOnTheLegacy = recruiter.containsAllFields(VisEntityRecruiter.Fields.lastLegacyView);
			if(viewedOnTheLegacy) {
				Long lastLegacyView = recruiter.getAsLongNumber(VisEntityRecruiter.Fields.lastLegacyView);
				long timeToTheGreatestMoment = GREATEST_MOMENT_OF_13_DIGITS - lastLegacyView;
				String legacyQueuePosition = String.format(legacyPositionFormat, timeToTheGreatestMoment, emailHash);
				CcpJsonRepresentation legacyViewer = recruiterWithHash.put(VisRecruiterResumesFields.legacyQueuePosition, legacyQueuePosition);
				legacyViewers.add(legacyViewer);
				continue;
			}
			String group = cameFromTheSignUp ? signUpGroup : othersGroup;
			String queuePosition = group + emailHash;
			CcpJsonRepresentation recruiterInTheQueue = recruiterWithHash.put(VisRecruiterResumesFields.queuePosition, queuePosition);
			queue.add(recruiterInTheQueue);
		}
		Comparator<CcpJsonRepresentation> byEmailHash = Comparator.comparing(recruiter -> recruiter.getAsString(VisRecruiterResumesFields.emailHash));
		Comparator<CcpJsonRepresentation> byMostRecentTelegramAppearance = Comparator.comparing(recruiter -> -recruiter.getAsLongNumber(VisEntityRecruiter.Fields.lastTelegramAppearance));
		Comparator<CcpJsonRepresentation> byMostRecentView = Comparator.comparing(recruiter -> -lastViewByRecruiter.get(recruiter.getAsString(VisRecruiterResumesFields.emailHash)));
		recentInTelegram.sort(byMostRecentTelegramAppearance.thenComparing(byEmailHash));
		viewers.sort(byMostRecentView.thenComparing(byEmailHash));
		waitingForTheFirstEmail.sort(byEmailHash);
		List<CcpJsonRepresentation> legacyViewersInTurn = getInTurn(legacyViewers, VisRecruiterResumesFields.legacyQueuePosition, lastLegacyQueuePosition);
		List<CcpJsonRepresentation> queueInTurn = getInTurn(queue, VisRecruiterResumesFields.queuePosition, lastQueuePosition);

		List<CcpJsonRepresentation> recruitersInOrder = new ArrayList<>(viewers);
		recruitersInOrder.addAll(waitingForTheFirstEmail);
		recruitersInOrder.addAll(recentInTelegram);
		recruitersInOrder.addAll(legacyViewersInTurn);
		recruitersInOrder.addAll(queueInTurn);
		return recruitersInOrder;
	}

	/**
	 * A queue in turns: by the position, first the ones right after the last position that had its turn, then the ones up
	 * to it, so a queue that reached its end starts over.
	 * @param queue the recruiters of the queue
	 * @param positionField the field with the position of each recruiter
	 * @param lastPosition the last position that had its turn; empty when none had
	 * @return the recruiters in turn
	 */
	static List<CcpJsonRepresentation> getInTurn(List<CcpJsonRepresentation> queue, CcpJsonFieldName positionField, String lastPosition) {
		Comparator<CcpJsonRepresentation> byPosition = Comparator.comparing(recruiter -> recruiter.getAsString(positionField));
		List<CcpJsonRepresentation> sortedQueue = new ArrayList<>(queue);
		sortedQueue.sort(byPosition);
		List<CcpJsonRepresentation> afterTheLastPosition = new ArrayList<>();
		List<CcpJsonRepresentation> upToTheLastPosition = new ArrayList<>();
		for (CcpJsonRepresentation recruiter : sortedQueue) {
			String position = recruiter.getAsString(positionField);
			boolean alreadyHadItsTurn = position.compareTo(lastPosition) <= 0;
			if(alreadyHadItsTurn) {
				upToTheLastPosition.add(recruiter);
				continue;
			}
			afterTheLastPosition.add(recruiter);
		}
		List<CcpJsonRepresentation> inTurn = new ArrayList<>(afterTheLastPosition);
		inTurn.addAll(upToTheLastPosition);
		return inTurn;
	}

	/** The numbers of {@code sortKeys}, the first ones put by {@link #getPriorityKeys(List, CcpJsonRepresentation, long)}. */
	static final Comparator<CcpJsonRepresentation> BY_SORT_KEYS = VisResumesBySortKeys.INSTANCE;

	/**
	 * How many recruiters viewed each resume: a recruiter counts once per resume, however many times they open it.
	 * @param lastViewByPair the last view of each pair recruiter + resume
	 * @return the number of recruiters, by the hash of the LinkedIn of the resume
	 */
	static Map<String, Long> getViewsByResume(Map<String, Long> lastViewByPair) {
		String separator = Constants.SEPARATOR.getValue();
		Map<String, Long> viewsByResume = new HashMap<>();
		for (String pairKey : lastViewByPair.keySet()) {
			int separatorPosition = pairKey.indexOf(separator);
			String linkedinHash = pairKey.substring(separatorPosition + 1);
			viewsByResume.merge(linkedinHash, 1L, Long::sum);
		}
		return viewsByResume;
	}

	/**
	 * The rows of every resume, in the order of the e-mail.
	 * @param resumes the stored resumes
	 * @param rankings each skill and its position in {@code vis_skill}
	 * @param resumeDates the oldest and the newest times of each resume, by the hash of its e-mail
	 * @param viewsByResume how many recruiters viewed each resume, by the hash of its LinkedIn
	 * @return the rows in order
	 */
	static List<CcpJsonRepresentation> getResumesOfTheEmail(List<CcpJsonRepresentation> resumes, CcpJsonRepresentation rankings, Map<String, long[]> resumeDates, Map<String, Long> viewsByResume) {
		List<CcpJsonRepresentation> resumesOfTheEmail = new ArrayList<>();
		for (CcpJsonRepresentation resume : resumes) {
			CcpJsonRepresentation resumeOfTheEmail = getResumeOfTheEmail(resume, rankings, resumeDates, viewsByResume);
			resumesOfTheEmail.add(resumeOfTheEmail);
		}
		resumesOfTheEmail.sort(BY_SORT_KEYS);
		return resumesOfTheEmail;
	}

	/**
	 * The first keys of the order of the resumes, all ascending: 0 for the resume with at least {@value #MINIMUM_SKILLS}
	 * skills and 1 for the others; the number of views, negative because more views go first; the average of the rankings.
	 * @param skills the skills of the resume
	 * @param rankings each skill and its position in {@code vis_skill}
	 * @param views how many recruiters viewed the resume
	 * @return the keys
	 */
	static List<Object> getPriorityKeys(List<String> skills, CcpJsonRepresentation rankings, long views) {
		boolean enoughSkills = skills.size() >= MINIMUM_SKILLS;
		int skillsKey = enoughSkills ? 0 : 1;
		double averageRanking = getAverageRanking(skills, rankings);
		List<Object> priorityKeys = List.of(skillsKey, -views, averageRanking);
		return priorityKeys;
	}

	/**
	 * The sum of the rankings of the skills divided by how many they are. A skill unknown to {@code vis_skill} has no number
	 * to add, so it stays out of both sides: dividing by it too would improve the resume for having an unknown skill.
	 * @param skills the skills of the resume
	 * @param rankings each skill and its position in {@code vis_skill}
	 * @return the average, or {@link VisBusinessBuildResumeRow#NO_RANKING} when no skill has a ranking
	 */
	static double getAverageRanking(List<String> skills, CcpJsonRepresentation rankings) {
		double sumOfRankings = 0;
		int rankedSkills = 0;
		for (String skill : skills) {
			double ranking = VisBusinessBuildResumeRow.getRanking(rankings, skill);
			boolean noRanking = ranking == VisBusinessBuildResumeRow.NO_RANKING;
			if(noRanking) {
				continue;
			}
			sumOfRankings += ranking;
			rankedSkills++;
		}
		boolean nothingRanked = rankedSkills == 0;
		if(nothingRanked) {
			return VisBusinessBuildResumeRow.NO_RANKING;
		}
		double averageRanking = sumOfRankings / rankedSkills;
		return averageRanking;
	}

	/**
	 * The row of a resume, with what the choice of the rows of each recruiter needs and the e-mail does not show: the
	 * hash of its LinkedIn (the key of the views) and the companies it forbids.
	 */
	private static CcpJsonRepresentation getResumeOfTheEmail(CcpJsonRepresentation resume, CcpJsonRepresentation rankings, Map<String, long[]> resumeDates, Map<String, Long> viewsByResume) {
		String emailHash = resume.getAsString(VisEntityResume.Fields.email);
		long now = System.currentTimeMillis();
		long[] oldestAndNewest = resumeDates.getOrDefault(emailHash, new long[] {now, now});
		CcpJsonRepresentation rowInput = resume
				.put(VisRecruiterResumesFields.rankings, rankings)
				.put(VisRecruiterResumesFields.insertion, oldestAndNewest[0])
				.put(VisRecruiterResumesFields.lastChange, oldestAndNewest[1])
				.put(JnJsonCommonsFields.language, LANGUAGE);
		CcpJsonRepresentation row = VisBusinessBuildResumeRow.INSTANCE.execute(rowInput);
		String linkedinAddress = resume.getAsString(VisEntityResume.Fields.linkedinAddress);
		String linkedinHash = getLinkedinHash(linkedinAddress);
		List<String> notAllowedCompanies = resume.getOrDefault(VisEntityResume.Fields.notAllowedCompany, () -> List.of());
		List<String> skills = VisBusinessBuildResumeRow.getSkillsByRanking(rowInput);
		long views = viewsByResume.getOrDefault(linkedinHash, 0L);
		List<Object> priorityKeys = getPriorityKeys(skills, rankings, views);
		List<Object> rowSortKeys = row.getAsObjectList(VisRecruiterResumesFields.sortKeys);
		List<Object> sortKeys = new ArrayList<>(priorityKeys);
		sortKeys.addAll(rowSortKeys);
		List<CcpJsonRepresentation> subjectItems = getSubjectItems(rowInput, skills, rankings);
		CcpJsonRepresentation resumeOfTheEmail = row
				.put(VisRecruiterResumesFields.sortKeys, sortKeys)
				.put(VisRecruiterResumesFields.subjectItems, subjectItems)
				.put(VisEntityViewedResume.Fields.linkedin, linkedinHash)
				.put(VisEntityResume.Fields.notAllowedCompany, notAllowedCompanies);
		return resumeOfTheEmail;
	}

	/**
	 * The items of the subject of a resume: first the fluent English and Spanish (group 0, by the position of the
	 * language, as in the column "languages and skills"), then the skills (group 1, by the ranking in {@code vis_skill}).
	 * The key joins the same item of different resumes: the position for a fluent top language (whatever the spelling of
	 * its name), the skill. Since 2026-11-12 the other languages stay out of the subject (they are not skills), and the
	 * fluent English and Spanish appear but do not count (see {@link #getSubjectValues(List)}).
	 */
	static List<CcpJsonRepresentation> getSubjectItems(CcpJsonRepresentation rowInput, List<String> skills, CcpJsonRepresentation rankings) {
		String separator = Constants.SEPARATOR.getValue();
		List<CcpJsonRepresentation> subjectItems = new ArrayList<>();
		List<String> topLanguages = VisBusinessBuildResumeRow.getTopLanguagesAndOthers(rowInput, LANGUAGE, true);
		List<Integer> topPositions = VisBusinessBuildResumeRow.getTopLanguagePositions(rowInput);
		for (int index = 0; index < topLanguages.size(); index++) {
			String line = topLanguages.get(index);
			Integer position = topPositions.get(index);
			String key = SUBJECT_GROUP_TOP_LANGUAGES + separator + position;
			CcpJsonRepresentation item = getSubjectItem(key, line, SUBJECT_GROUP_TOP_LANGUAGES, position);
			subjectItems.add(item);
		}
		for (String skill : skills) {
			Double ranking = VisBusinessBuildResumeRow.getRanking(rankings, skill);
			String key = SUBJECT_GROUP_SKILLS + separator + skill;
			CcpJsonRepresentation item = getSubjectItem(key, skill, SUBJECT_GROUP_SKILLS, ranking);
			subjectItems.add(item);
		}
		return subjectItems;
	}

	private static CcpJsonRepresentation getSubjectItem(String key, String label, int group, double order) {
		CcpJsonRepresentation item = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.itemKey, key)
				.put(VisRecruiterResumesFields.itemLabel, label)
				.put(VisRecruiterResumesFields.itemGroup, group)
				.put(VisRecruiterResumesFields.itemOrder, order);
		return item;
	}

	/**
	 * The values of the subject, from the items of all the resumes of the recruiter together, without repetitions: the
	 * fluent English and Spanish first, then the first {@value #SKILLS_IN_THE_SUBJECT} skills by ranking, joined by
	 * commas, and how many distinct skills are left. The fluent English and Spanish always appear and count in neither
	 * the {@value #SKILLS_IN_THE_SUBJECT} nor the skills left.
	 * @param rows the rows of the recruiter, each one with {@code subjectItems}
	 * @return {@code topSkills} and {@code otherSkills}
	 */
	static CcpJsonRepresentation getSubjectValues(List<CcpJsonRepresentation> rows) {
		Map<String, CcpJsonRepresentation> itemsByKey = new LinkedHashMap<>();
		for (CcpJsonRepresentation row : rows) {
			List<CcpJsonRepresentation> subjectItems = row.getAsJsonList(VisRecruiterResumesFields.subjectItems);
			for (CcpJsonRepresentation item : subjectItems) {
				String key = item.getAsString(VisRecruiterResumesFields.itemKey);
				itemsByKey.putIfAbsent(key, item);
			}
		}
		List<CcpJsonRepresentation> items = new ArrayList<>(itemsByKey.values());
		items.sort(VisSubjectItemsInOrder.INSTANCE);
		List<String> topSkillsList = new ArrayList<>();
		int namedSkills = 0;
		int otherSkills = 0;
		for (CcpJsonRepresentation item : items) {
			String label = item.getAsString(VisRecruiterResumesFields.itemLabel);
			Integer group = item.getAsIntegerNumber(VisRecruiterResumesFields.itemGroup);
			boolean topLanguage = group == SUBJECT_GROUP_TOP_LANGUAGES;
			if(topLanguage) {
				topSkillsList.add(label);
				continue;
			}
			boolean beyondTheLimit = namedSkills >= SKILLS_IN_THE_SUBJECT;
			if(beyondTheLimit) {
				otherSkills++;
				continue;
			}
			topSkillsList.add(label);
			namedSkills++;
		}
		String commaSeparator = Constants.COMMA_SEPARATOR.getValue();
		String topSkills = String.join(commaSeparator, topSkillsList);
		CcpJsonRepresentation subjectValues = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.topSkills, topSkills)
				.put(VisRecruiterResumesFields.otherSkills, otherSkills);
		return subjectValues;
	}

	/**
	 * Writes the whole list of the recruiter (read by the screen "see all") and sends the e-mail with the first
	 * {@value #RESUMES_IN_THE_EMAIL}. The subject, the text and the paragraph "see all" count the whole list, the same the
	 * screen "see all" shows.
	 */
	static void sendTheEmail(String email, List<CcpJsonRepresentation> rowsWithSubjectItems, String frontendUrl, String visApiUrl) {
		CcpJsonRepresentation subjectValues = getSubjectValues(rowsWithSubjectItems);
		List<CcpJsonRepresentation> rows = new ArrayList<>();
		for (CcpJsonRepresentation rowWithSubjectItems : rowsWithSubjectItems) {
			CcpJsonRepresentation row = rowWithSubjectItems.removeFields(VisRecruiterResumesFields.subjectItems);
			rows.add(row);
		}
		CcpJsonRepresentation recruiterResumes = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityRecruiterResumes.Fields.email, email)
				.put(VisEntityRecruiterResumes.Fields.resumes, rows);
		VisEntityRecruiterResumes.ENTITY.save(recruiterResumes);

		int total = rows.size();
		int shown = Math.min(RESUMES_IN_THE_EMAIL, total);
		List<CcpJsonRepresentation> rowsOfTheEmail = rows.subList(0, shown);
		CcpJsonRepresentation tableInput = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.resumes, rowsOfTheEmail)
				.put(JnJsonCommonsFields.email, email)
				.put(VisRecruiterResumesFields.frontendUrl, frontendUrl)
				.put(JnJsonCommonsFields.language, LANGUAGE);
		CcpJsonRepresentation withTable = VisBusinessRenderResumesTable.INSTANCE.execute(tableInput);
		String resumesTable = withTable.getAsString(VisRecruiterResumesFields.resumesTable);

		String seeAll = getSeeAll(email, shown, total, frontendUrl);
		String unsubscribeLink = getUnsubscribeLink(email, frontendUrl);
		CcpJsonRepresentation emailHeaders = getEmailHeaders(email, visApiUrl);

		CcpJsonRepresentation message = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.email, email)
				.put(JnJsonCommonsFields.language, LANGUAGE)
				.put(VisRecruiterResumesFields.resumesTable, resumesTable)
				.put(VisRecruiterResumesFields.seeAll, seeAll)
				.put(VisRecruiterResumesFields.unsubscribeLink, unsubscribeLink)
				.put(VisRecruiterResumesFields.totalOfResumes, total)
				.put(JnJsonCommonsFields.emailHeaders, emailHeaders)
				.mergeWithAnotherJson(subjectValues);
		String topic =VisMessages.VisNotifyRecruiterAboutResumes.class.getName();
		JnMessageType[] messageTypes = {JnMessageType.email};
		new JnSendMessageToUser().sendAllMessages(message, topic, messageTypes, JnMessageSenderExceptionHandler.LENIENT);
	}

	/**
	 * The paragraph with the link "see all", empty when every resume fits in the e-mail. The link carries the recipient
	 * (since 2026-11-12): the screen reads the list of the account logged in, and when it is another one (a browser
	 * already logged in with another account) the screen asks for the login of the recipient instead of opening empty.
	 */
	private static String getSeeAll(String email, int shown, int total, String frontendUrl) {
		boolean everyResumeFits = shown >= total;
		if(everyResumeFits) {
			return "";
		}
		String seeAllLabel = VisRecruiterResumesTexts.seeAll.getMessage(LANGUAGE);
		CcpStringDecorator emailDecorator = new CcpStringDecorator(email);
		CcpUrlDecorator emailUrl = emailDecorator.url();
		String encodedEmail = emailUrl.asEnconded();
		CcpJsonRepresentation linkValues = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.email, encodedEmail)
				.put(VisRecruiterResumesFields.frontendUrl, frontendUrl)
				.put(CcpJsonCommonsFields.value, seeAllLabel);
		String seeAllLink = resolve(Constants.LINK, linkValues);
		CcpJsonRepresentation paragraphValues = CcpOtherConstants.EMPTY_JSON
				.put(VisRecruiterResumesFields.shown, shown)
				.put(VisRecruiterResumesFields.total, total)
				.put(VisRecruiterResumesFields.seeAllLink, seeAllLink);
		String seeAll = VisRecruiterResumesTexts.seeAllParagraph.getMessage(LANGUAGE, paragraphValues);
		return seeAll;
	}

	private static String getUnsubscribeLink(String email, String frontendUrl) {
		CcpJsonRepresentation linkValues = getUnsubscribeValues(email).put(VisRecruiterResumesFields.frontendUrl, frontendUrl);
		String unsubscribeLink = resolve(Constants.UNSUBSCRIBE_PATH, linkValues);
		return unsubscribeLink;
	}

	/**
	 * The headers of the one-click unsubscribe (RFC 8058), which the mailbox providers demand from who sends in bulk: the
	 * provider calls the API with a POST, without the person leaving the mailbox. The URL goes to the API and not to the
	 * front end because a page of the front end does not answer a POST.
	 */
	static CcpJsonRepresentation getEmailHeaders(String email, String visApiUrl) {
		CcpJsonRepresentation linkValues = getUnsubscribeValues(email).put(VisRecruiterResumesFields.visApiUrl, visApiUrl);
		String listUnsubscribe = resolve(Constants.LIST_UNSUBSCRIBE_VALUE, linkValues);
		String listUnsubscribePost = Constants.LIST_UNSUBSCRIBE_POST_VALUE.getValue();
		CcpJsonRepresentation emailHeaders = CcpOtherConstants.EMPTY_JSON
				.put(Constants.LIST_UNSUBSCRIBE, listUnsubscribe)
				.put(Constants.LIST_UNSUBSCRIBE_POST, listUnsubscribePost);
		return emailHeaders;
	}

	/** The e-mail, ready for a URL, and the signature of the links that stop the e-mails to the recruiter. */
	private static CcpJsonRepresentation getUnsubscribeValues(String email) {
		String token = VisRecruiterLinks.unsubscribe.sign(email);
		CcpStringDecorator emailDecorator = new CcpStringDecorator(email);
		CcpUrlDecorator emailUrl = emailDecorator.url();
		String encodedEmail = emailUrl.asEnconded();
		CcpJsonRepresentation unsubscribeValues = CcpOtherConstants.EMPTY_JSON
				.put(JnJsonCommonsFields.email, encodedEmail)
				.put(VisRecruiterResumesFields.token, token);
		return unsubscribeValues;
	}

	/**
	 * The rows of a recruiter: every resume, but the ones that forbid the company of the recruiter and the ones whose view
	 * by this recruiter is in the twin {@code vis_inactive_viewed_resume}, each one with the last view of this recruiter,
	 * the alias they gave to it and the signed link.
	 */
	static List<CcpJsonRepresentation> getRowsOfTheRecruiter(CcpJsonRepresentation recruiter, String emailHash, List<CcpJsonRepresentation> resumesOfTheEmail,
			Map<String, Long> lastViewByPair, Map<String, String> aliasByPair, Set<String> inactivePairs) {
		String email = recruiter.getAsString(VisEntityRecruiter.Fields.email);
		boolean professional = recruiter.getOrDefault(VisEntityRecruiter.Fields.professional, () -> false);
		String domain = recruiter.getOrDefault(VisEntityRecruiter.Fields.domain, () -> "");
		String separator = Constants.SEPARATOR.getValue();
		String viewResume = VisRecruiterResumesTexts.viewResume.getMessage(LANGUAGE);

		List<CcpJsonRepresentation> rows = new ArrayList<>();
		for (CcpJsonRepresentation resumeOfTheEmail : resumesOfTheEmail) {
			List<String> notAllowedCompanies = resumeOfTheEmail.getAsStringList(VisEntityResume.Fields.notAllowedCompany);
			boolean forbiddenCompany = professional && isForbiddenCompany(domain, notAllowedCompanies);
			if(forbiddenCompany) {
				continue;
			}
			String linkedinHash = resumeOfTheEmail.getAsString(VisEntityViewedResume.Fields.linkedin);
			String pairKey = emailHash + separator + linkedinHash;
			boolean inactiveView = inactivePairs.contains(pairKey);
			if(inactiveView) {
				continue;
			}
			Long lastView = lastViewByPair.get(pairKey);
			boolean viewed = lastView != null;
			String lastViewLine = viewed ? getLastViewLine(lastView) : VisRecruiterResumesTexts.neverViewed.getMessage(LANGUAGE);
			String alias = aliasByPair.get(pairKey);
			String viewLabel = alias == null ? viewResume : alias;
			String resumeId = resumeOfTheEmail.getAsString(VisRecruiterResumesFields.resumeId);
			String token = VisRecruiterLinks.view.sign(email, resumeId);
			CcpJsonRepresentation row = resumeOfTheEmail
					.removeFields(VisRecruiterResumesFields.sortKeys, VisEntityViewedResume.Fields.linkedin, VisEntityResume.Fields.notAllowedCompany)
					// subjectItems stays: sendTheEmail builds the subject with it and then removes it
					.put(VisRecruiterResumesFields.lastView, lastViewLine)
					.put(VisRecruiterResumesFields.viewed, viewed)
					.put(VisRecruiterResumesFields.viewLabel, viewLabel)
					.put(VisRecruiterResumesFields.token, token);
			rows.add(row);
		}
		return rows;
	}

	/** The line with the date of the last view of the resume by the recruiter. */
	private static String getLastViewLine(Long lastView) {
		String lastViewDate = formatDate(lastView);
		CcpJsonRepresentation lastViewValues = CcpOtherConstants.EMPTY_JSON.put(VisRecruiterResumesFields.date, lastViewDate);
		String lastViewLine = VisRecruiterResumesTexts.lastView.getMessage(LANGUAGE, lastViewValues);
		return lastViewLine;
	}

	/**
	 * Tells whether the resume forbids the company of the recruiter: the name of a company of {@code notAllowedCompany},
	 * without accents, spaces and symbols, is a part of the domain (split by dots and hyphens) or, when it has at least
	 * four letters, is inside the domain written without dots and hyphens ({@code Itaú} forbids {@code itau.com.br} and
	 * {@code itau-unibanco.com.br}).
	 */
	/**
	 * Tells whether the resume forbids the company of the recruiter, from their e-mail alone (no read of
	 * {@code vis_recruiter}): the domain and the kind of the e-mail come from the same transformers that write them in the
	 * recruiter. A recruiter of a non-professional domain (gmail and the like) has no company to forbid. Used when the
	 * recruiter opens a resume, so a company forbidden after the e-mail went out no longer sees the resume.
	 * @param recruiterEmail the e-mail of the recruiter
	 * @param notAllowedCompanies the companies the resume forbids, as they are now
	 * @return {@code true} when the recruiter must not see the resume
	 */
	public static boolean isTheCompanyOfTheRecruiterForbidden(String recruiterEmail, List<String> notAllowedCompanies) {
		boolean nothingForbidden = notAllowedCompanies.isEmpty();
		if(nothingForbidden) {
			return false;
		}
		CcpJsonRepresentation recruiter = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiter.Fields.email, recruiterEmail);
		CcpJsonRepresentation recruiterWithProfessional = VisJsonTransformerRecruiterProfessional.INSTANCE.execute(recruiter);
		boolean professional = recruiterWithProfessional.getAsBoolean(VisEntityRecruiter.Fields.professional);
		boolean personalEmail = false == professional;
		if(personalEmail) {
			return false;
		}
		CcpJsonRepresentation recruiterWithDomain = VisJsonTransformerRecruiterDomain.INSTANCE.execute(recruiter);
		String domain = recruiterWithDomain.getAsString(VisEntityRecruiter.Fields.domain);
		boolean forbiddenCompany = isForbiddenCompany(domain, notAllowedCompanies);
		return forbiddenCompany;
	}

	static boolean isForbiddenCompany(String domain, List<String> notAllowedCompanies) {
		String lowerCaseDomain = domain.toLowerCase();
		String partsSeparators = Constants.DOMAIN_PARTS_SEPARATORS.getValue();
		String[] domainParts = lowerCaseDomain.split(partsSeparators);
		List<String> domainPartsList = List.of(domainParts);
		String joinedDomain = String.join("", domainParts);
		for (String company : notAllowedCompanies) {
			String normalizedCompany = normalize(company);
			boolean empty = normalizedCompany.isEmpty();
			if(empty) {
				continue;
			}
			boolean isAPartOfTheDomain = domainPartsList.contains(normalizedCompany);
			if(isAPartOfTheDomain) {
				return true;
			}
			boolean longEnough = normalizedCompany.length() >= SHORTEST_COMPANY_NAME;
			boolean insideTheDomain = longEnough && joinedDomain.contains(normalizedCompany);
			if(insideTheDomain) {
				return true;
			}
		}
		return false;
	}

	private static String normalize(String text) {
		CcpStringDecorator textDecorator = new CcpStringDecorator(text);
		CcpTextDecorator textText = textDecorator.text();
		CcpTextDecorator withoutAccents = textText.stripAccents();
		String lowerCase = withoutAccents.content.toLowerCase();
		String notLettersOrDigits = Constants.NOT_LETTERS_OR_DIGITS.getValue();
		String normalized = lowerCase.replaceAll(notLettersOrDigits, "");
		return normalized;
	}

	/**
	 * The primary keys of the records of an entity written since the given time, read from the history: the
	 * {@code json} of each history row (the {@code id} of the row does not come back from the query).
	 */
	private static Set<String> getRecordsChangedSince(String entityName, long since, CcpJsonFieldName keyField) {
		String timestampField = JnJsonCommonsFields.timestamp.getValue();
		CcpQueryOptions query = CcpQueryOptions.INSTANCE
				.startQuery()
					.startBool()
						.startMust()
							.term(JnJsonCommonsFields.entity, entityName)
							.startRange()
								.startFieldRange(timestampField)
									.greaterThanEquals(since)
								.endFieldRangeAndBackToRange()
							.endRangeAndBackToMust()
						.endMustAndBackToBool()
					.endBoolAndBackToQuery()
				.endQueryAndBackToRequest();
		Set<String> keys = new LinkedHashSet<>();
		Consumer<CcpJsonRepresentation> keepTheKey = new VisHistoryKeyCollector(keys, keyField);
		String scrollTime = Constants.SCROLL_TIME.getValue();
		String jsonField = JnJsonCommonsFields.json.getValue();
		query.selectFrom(JnEntityVersionable.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, keepTheKey, jsonField);
		return keys;
	}

	/**
	 * The stored resumes of the given e-mails (hashes); the inactive ones are not in {@code vis_resume}, and the ones with
	 * {@code invalidLinkedinAddress} or with a restriction to correct ({@code vis_resume_restriction}) are left out.
	 */
	private static List<CcpJsonRepresentation> getResumes(Set<String> emailHashes) {
		CcpQueryOptions query = CcpQueryOptions.INSTANCE
				.startQuery()
					.startBool()
						.startMust()
							.terms(VisEntityResume.Fields.email, emailHashes)
						.endMustAndBackToBool()
					.endBoolAndBackToQuery()
				.endQueryAndBackToRequest();
		List<CcpJsonRepresentation> resumes = new ArrayList<>();
		String scrollTime = Constants.SCROLL_TIME.getValue();
		query.selectFrom(VisEntityResume.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, resume -> resumes.add(resume));
		// the support confirmed that the LinkedIn address does not open (since 2026-11-12): the resume goes to no recruiter
		resumes.removeIf(resume -> resume.getOrDefault(VisEntityResume.Fields.invalidLinkedinAddress, () -> false));
		// the owner must correct the resume first (vis_resume_restriction, since 2026-11-12): it goes to no recruiter
		Set<String> restrictedEmails = getRestrictedEmails(emailHashes);
		resumes.removeIf(resume -> restrictedEmails.contains(resume.getAsString(VisEntityResume.Fields.email)));
		return resumes;
	}

	/**
	 * The e-mails (hashes), among the given ones, whose resumes have a restriction to correct: the restriction keeps the
	 * e-mail hashed as the resume does.
	 */
	private static Set<String> getRestrictedEmails(Set<String> emailHashes) {
		CcpQueryOptions query = CcpQueryOptions.INSTANCE
				.startQuery()
					.startBool()
						.startMust()
							.terms(VisEntityResumeRestriction.Fields.email, emailHashes)
						.endMustAndBackToBool()
					.endBoolAndBackToQuery()
				.endQueryAndBackToRequest();
		Set<String> restrictedEmails = new HashSet<>();
		String scrollTime = Constants.SCROLL_TIME.getValue();
		query.selectFrom(VisEntityResumeRestriction.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, restriction -> restrictedEmails.add(restriction.getAsString(VisEntityResumeRestriction.Fields.email)));
		return restrictedEmails;
	}

	/** The oldest (added) and the newest (last change) times of each resume in the history, by the hash of its e-mail. */
	private static Map<String, long[]> getResumeDates(Set<String> emailHashes) {
		List<String> historyIds = new ArrayList<>();
		for (String emailHash : emailHashes) {
			CcpJsonRepresentation primaryKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.email, emailHash);
			String historyId = primaryKey.asUgglyJson();
			historyIds.add(historyId);
		}
		String resumeEntityName = VisEntityResume.ENTITY.getEntityMetaData().entityName;
		CcpQueryOptions query = CcpQueryOptions.INSTANCE
				.startQuery()
					.startBool()
						.startMust()
							.term(JnJsonCommonsFields.entity, resumeEntityName)
							.terms(JnJsonCommonsFields.id, historyIds)
						.endMustAndBackToBool()
					.endBoolAndBackToQuery()
				.endQueryAndBackToRequest();
		Map<String, long[]> dates = new HashMap<>();
		Consumer<CcpJsonRepresentation> keepTheDates = new VisHistoryResumeDatesCollector(dates);
		String scrollTime = Constants.SCROLL_TIME.getValue();
		String jsonField = JnJsonCommonsFields.json.getValue();
		String timestampField = JnJsonCommonsFields.timestamp.getValue();
		query.selectFrom(JnEntityVersionable.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, keepTheDates, jsonField, timestampField);
		return dates;
	}

	/**
	 * The last view of each pair recruiter + resume and of each recruiter, from the history of {@code vis_viewed_resume}
	 * (the keys are the hashes of the e-mail of the recruiter and of the LinkedIn of the resume).
	 */
	private static void loadViews(Map<String, Long> lastViewByPair, Map<String, Long> lastViewByRecruiter) {
		String viewedResumeEntityName = VisEntityViewedResume.ENTITY.getEntityMetaData().entityName;
		CcpQueryOptions query = CcpQueryOptions.INSTANCE
				.startQuery()
					.startBool()
						.startMust()
							.term(JnJsonCommonsFields.entity, viewedResumeEntityName)
						.endMustAndBackToBool()
					.endBoolAndBackToQuery()
				.endQueryAndBackToRequest();
		String separator = Constants.SEPARATOR.getValue();
		Consumer<CcpJsonRepresentation> keepTheLastView = new VisHistoryLastViewCollector(lastViewByPair, lastViewByRecruiter, separator);
		String scrollTime = Constants.SCROLL_TIME.getValue();
		String jsonField = JnJsonCommonsFields.json.getValue();
		String timestampField = JnJsonCommonsFields.timestamp.getValue();
		query.selectFrom(JnEntityVersionable.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, keepTheLastView, jsonField, timestampField);
	}

	/** The alias of each pair recruiter + resume, by the same key of {@link #loadViews(Map, Map)}. */
	private static Map<String, String> getAliases() {
		Map<String, String> aliasByPair = new HashMap<>();
		String separator = Constants.SEPARATOR.getValue();
		Consumer<CcpJsonRepresentation> keepTheAlias = new VisViewedResumeAliasCollector(aliasByPair, separator);
		CcpQueryOptions everyView = CcpQueryOptions.INSTANCE.matchAll();
		String scrollTime = Constants.SCROLL_TIME.getValue();
		everyView.selectFrom(VisEntityViewedResume.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, keepTheAlias);
		return aliasByPair;
	}

	/**
	 * The pairs recruiter + resume of the twin {@code vis_inactive_viewed_resume}, by the same key of
	 * {@link #loadViews(Map, Map)}: the resumes that no longer go to each recruiter.
	 */
	private static Set<String> getInactivePairs() {
		Set<String> inactivePairs = new LinkedHashSet<>();
		String separator = Constants.SEPARATOR.getValue();
		Consumer<CcpJsonRepresentation> keepThePair = new VisInactiveViewedResumePairCollector(inactivePairs, separator);
		CcpQueryOptions everyInactiveView = CcpQueryOptions.INSTANCE.matchAll();
		CcpEntity inactiveViewedResume = VisEntityViewedResume.ENTITY.getTwinEntity();
		String scrollTime = Constants.SCROLL_TIME.getValue();
		everyInactiveView.selectFrom(inactiveViewedResume).consumeQueryResult(scrollTime, PAGE_SIZE, keepThePair);
		return inactivePairs;
	}

	/** The addresses of the front end and of the API of vis, kept in the sending parameters of the e-mail. */
	private static CcpJsonRepresentation getLinkAddresses() {
		String templateId = VisMessages.VisNotifyRecruiterAboutResumes.class.getName();
		CcpJsonRepresentation parametersId = CcpOtherConstants.EMPTY_JSON.put(JnJsonCommonsFields.templateId, templateId);
		CcpJsonRepresentation parameters = JnEntityEmailParametersToSend.ENTITY.getOneById(parametersId);
		CcpJsonRepresentation moreParameters = parameters.getInnerJson(JnJsonCommonsFields.moreParameters);
		return moreParameters;
	}

	/** The hash of the e-mail, as the entities keep it. */
	static String getEmailHash(String email) {
		CcpJsonRepresentation emailJson = CcpOtherConstants.EMPTY_JSON.put(VisEntityRecruiterResumes.Fields.email, email);
		CcpJsonRepresentation hashedEmail = VisEntityRecruiterResumes.ENTITY.getJsonWithHandledPrimaryKey(emailJson);
		String emailHash = hashedEmail.getAsString(VisEntityRecruiterResumes.Fields.email);
		return emailHash;
	}

	/** The hash of a LinkedIn address, as {@code vis_viewed_resume} keeps it. */
	static String getLinkedinHash(String linkedinAddress) {
		CcpJsonRepresentation linkedinJson = CcpOtherConstants.EMPTY_JSON.put(VisEntityViewedResume.Fields.linkedin, linkedinAddress);
		CcpJsonRepresentation hashedLinkedin = VisJsonTransformerLinkedinHash.INSTANCE.execute(linkedinJson);
		String linkedinHash = hashedLinkedin.getAsString(VisEntityViewedResume.Fields.linkedin);
		return linkedinHash;
	}

	static CcpJsonRepresentation getRecordOfTheHistoryRow(CcpJsonRepresentation historyRow) {
		String recordAsText = historyRow.getAsString(JnJsonCommonsFields.json);
		CcpJsonRepresentation record = new CcpJsonRepresentation(recordAsText);
		return record;
	}

	static String formatDate(long time) {
		CcpTimeDecorator timeDecorator = new CcpTimeDecorator(time);
		String datePattern = VisBusinessBuildResumeRow.Constants.DATE_PATTERN.getValue();
		String formattedDate = timeDecorator.getFormattedDateTime(datePattern);
		return formattedDate;
	}

	static String resolve(CcpJsonFieldName template, CcpJsonRepresentation values) {
		String templateText = template.getValue();
		CcpStringDecorator templateDecorator = new CcpStringDecorator(templateText);
		CcpTextDecorator templateTextDecorator = templateDecorator.text();
		CcpTextDecorator resolved = templateTextDecorator.resolveTemplate(values);
		return resolved.content;
	}
}
