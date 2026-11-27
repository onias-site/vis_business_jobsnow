package com.vis.business.job.posting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.jn.business.messages.JnMessageType;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnGroupers;
import com.jn.utils.JnSystemProperties;
import com.vis.business.position.VisBusinessInstantMessageGroupsOfThePosition;
import com.vis.business.position.VisBusinessResumesOfTheFilters;
import com.vis.entities.VisEntityJobPosting;
import com.vis.entities.VisEntityPositionInstantMessageGroup;
import com.vis.entities.VisEntityVirtualJobPostingHour;
import com.vis.json.transformers.VisJobPostingHourCombinations;
import com.vis.json.transformers.VisJsonTransformerJobPostingId;
import com.vis.json.transformers.VisSeniorityType;

/**
 * The hourly publishing of the job postings of {@code /saveJobPosting} in the groups of technology of the user bot (since
 * 2026-11-26, the Cloud Run Job {@code jobsnow-vagas-do-bot-por-hora}): the children of the group of
 * {@code vis_virtual_job_posting_hour} of the day and of the hour of the run, the records of {@code vis_job_posting}
 * read by id, and the message of each one ({@link VisBusinessJobPostingPublishingMessage}) sent to every group of
 * technology: the chats of {@code vis_position_instant_message_group} of {@code resumeType} 1 with every ddd and every
 * seniority, read by id with the groups of {@code vis_instant_message_group} (MGIE), each chat once
 * ({@link VisBusinessInstantMessageGroupsOfThePosition}). Each group gets its messages in a thread of its own,
 * {@value VisJobPostingsToAGroup#SECONDS_BETWEEN_MESSAGES} seconds apart ({@link VisJobPostingsToAGroup}).
 */
public class VisBusinessPublishJobPostingsOfTheHour implements CcpBusiness {

	/** The output of this business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The day and the hour of the group read. */
		day,
		hour,
		/** How many job postings the group of the hour has, and how many were found. */
		children,
		jobPostings,
		/** How many groups of technology received them. */
		groups,
		messagesSent,
		failures,
	}

	/** The area of the groups: technology. */
	static final int TECHNOLOGY = 1;

	/** The single instance: the class keeps no state. */
	public static final VisBusinessPublishJobPostingsOfTheHour INSTANCE = new VisBusinessPublishJobPostingsOfTheHour();

	private VisBusinessPublishJobPostingsOfTheHour() {}

	/**
	 * Publishes the job postings of the hour.
	 * @param json the input of the scheduled process
	 * @return the input with the counters
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpTimeDecorator now = new CcpTimeDecorator();
		String dayPattern = VisJobPostingHourCombinations.Constants.DAY_PATTERN.getValue();
		String day = now.getFormattedDateTime(dayPattern);
		String hourPattern = VisJobPostingHourCombinations.Constants.HOUR_PATTERN.getValue();
		String hourText = now.getFormattedDateTime(hourPattern);
		int hour = Integer.parseInt(hourText);
		List<CcpJsonRepresentation> jobPostings = getJobPostingsOfTheHour(day, hour);
		CcpJsonRepresentation withTheHour = json
				.put(Fields.day, day)
				.put(Fields.hour, hour)
				.put(Fields.jobPostings, jobPostings.size());
		boolean noJobPosting = jobPostings.isEmpty();

		if(noJobPosting) {
			return withTheHour.put(Fields.groups, 0).put(Fields.messagesSent, 0).put(Fields.failures, 0);
		}

		List<Long> chatIds = getTechnologyChats();
		List<List<String>> messages = new ArrayList<>();
		for (CcpJsonRepresentation jobPosting : jobPostings) {
			CcpJsonRepresentation message = VisBusinessJobPostingPublishingMessage.INSTANCE.execute(jobPosting);
			List<String> pieces = message.getAsStringList(VisBusinessJobPostingPublishingMessage.Fields.pieces);
			messages.add(pieces);
		}
		List<CcpJsonRepresentation> countersOfTheGroups = sendToTheGroups(chatIds, messages);
		int messagesSent = sum(countersOfTheGroups, VisJobPostingsToAGroup.Fields.messagesSent);
		int failures = sum(countersOfTheGroups, VisJobPostingsToAGroup.Fields.failures);
		CcpJsonRepresentation response = withTheHour
				.put(Fields.groups, chatIds.size())
				.put(Fields.messagesSent, messagesSent)
				.put(Fields.failures, failures);
		return response;
	}

	/**
	 * The records of the job postings that are children of the group of the day and hour; a record already discarded
	 * stays out.
	 * @param day the day, {@code yyyyMMdd}
	 * @param hour the hour, from 0 to 23
	 * @return the records
	 */
	private static List<CcpJsonRepresentation> getJobPostingsOfTheHour(String day, int hour) {
		CcpJsonRepresentation groupKey = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityVirtualJobPostingHour.Fields.day, day)
				.put(VisEntityVirtualJobPostingHour.Fields.hour, hour);
		String parentId = VisEntityVirtualJobPostingHour.ENTITY.calculateId(groupKey);
		String parentName = VisEntityVirtualJobPostingHour.ENTITY.getEntityMetaData().entityName;
		String childName = VisEntityJobPosting.ENTITY.getEntityMetaData().entityName;
		List<String> childTexts = JnGroupers.getChildren(false, parentName, childName, parentId);
		boolean noChild = childTexts.isEmpty();

		if(noChild) {
			return List.of();
		}

		// the child keeps the text, which gives the key of the record
		List<CcpJsonRepresentation> keys = new ArrayList<>();
		for (String childText : childTexts) {
			CcpJsonRepresentation child = new CcpJsonRepresentation(childText);
			CcpJsonRepresentation description = child.getJsonPiece(VisEntityJobPosting.Fields.description);
			CcpJsonRepresentation withTheId = VisJsonTransformerJobPostingId.INSTANCE.execute(description);
			CcpJsonRepresentation key = withTheId.getJsonPiece(VisEntityJobPosting.Fields.postId);
			keys.add(key);
		}
		CcpJsonRepresentation[] keysArray = keys.toArray(new CcpJsonRepresentation[0]);
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll found = crud.unionAll(keysArray, JnDeleteKeysFromCache.INSTANCE, VisEntityJobPosting.ENTITY);
		List<CcpJsonRepresentation> records = found.getEntityRows(VisEntityJobPosting.ENTITY);
		return records;
	}

	/**
	 * The chats of the groups of technology: every ddd and every seniority of {@code resumeType} 1, each chat once.
	 * @return the chats
	 */
	private static List<Long> getTechnologyChats() {
		List<Integer> ddds = VisBusinessResumesOfTheFilters.getAllDdds();
		List<String> seniorities = new ArrayList<>();
		VisSeniorityType[] seniorityTypes = VisSeniorityType.values();
		for (VisSeniorityType seniorityType : seniorityTypes) {
			seniorities.add(seniorityType.name());
		}
		CcpJsonRepresentation groupsInput = CcpOtherConstants.EMPTY_JSON
				.put(VisBusinessInstantMessageGroupsOfThePosition.Fields.resumeType, TECHNOLOGY)
				.put(VisBusinessInstantMessageGroupsOfThePosition.Fields.seniority, seniorities)
				.put(VisBusinessInstantMessageGroupsOfThePosition.Fields.ddd, ddds);
		CcpJsonRepresentation groupsOutput = VisBusinessInstantMessageGroupsOfThePosition.INSTANCE.execute(groupsInput);
		boolean noGroup = false == groupsOutput.containsAllFields(VisEntityPositionInstantMessageGroup.Fields.chatId);

		if(noGroup) {
			return List.of();
		}

		List<Object> chats = groupsOutput.getAsObjectList(VisEntityPositionInstantMessageGroup.Fields.chatId);
		List<Long> chatIds = new ArrayList<>();
		for (Object chat : chats) {
			CcpJsonRepresentation chatJson = CcpOtherConstants.EMPTY_JSON.put(VisEntityPositionInstantMessageGroup.Fields.chatId, chat);
			Long chatId = chatJson.getAsLongNumber(VisEntityPositionInstantMessageGroup.Fields.chatId);
			chatIds.add(chatId);
		}
		return chatIds;
	}

	/**
	 * Sends the messages to every group, one thread per group, and waits for all of them.
	 * @param chatIds the groups
	 * @param messages the pieces of each job posting
	 * @return the counters of each group
	 */
	private static List<CcpJsonRepresentation> sendToTheGroups(List<Long> chatIds, List<List<String>> messages) {
		boolean noGroup = chatIds.isEmpty();

		if(noGroup) {
			return List.of();
		}

		String botType = JnMessageType.JnBotType.user.name();
		CcpStringDecorator botTypeDecorator = new CcpStringDecorator(botType);
		CcpJsonFieldName botName = botTypeDecorator.jsonFieldName();
		String botToken = JnSystemProperties.INSTANCE.getSystemInnerProperty(JnMessageType.InstantMessengerApiFields.bots, botName);
		List<VisJobPostingsToAGroup> tasks = new ArrayList<>();
		for (Long chatId : chatIds) {
			VisJobPostingsToAGroup task = new VisJobPostingsToAGroup(botName, botToken, chatId, messages);
			tasks.add(task);
		}
		ExecutorService pool = Executors.newFixedThreadPool(chatIds.size());
		try {
			List<Future<CcpJsonRepresentation>> futures = pool.invokeAll(tasks);
			List<CcpJsonRepresentation> results = new ArrayList<>();
			for (Future<CcpJsonRepresentation> future : futures) {
				CcpJsonRepresentation result = future.get();
				results.add(result);
			}
			return results;
		} catch (InterruptedException | ExecutionException e) {
			throw new PublishingInterrupted(e);
		} finally {
			pool.shutdown();
		}
	}

	/**
	 * The sum of a counter of every group.
	 * @param countersOfTheGroups the counters
	 * @param counter the counter
	 * @return the sum
	 */
	private static int sum(List<CcpJsonRepresentation> countersOfTheGroups, CcpJsonFieldName counter) {
		int total = 0;
		for (CcpJsonRepresentation counters : countersOfTheGroups) {
			Integer value = counters.getAsIntegerNumber(counter);
			total += value;
		}
		return total;
	}

	/** The threads of the groups did not end. */
	@SuppressWarnings("serial")
	static class PublishingInterrupted extends RuntimeException {
		PublishingInterrupted(Exception cause) {
			super(cause);
		}
	}
}
