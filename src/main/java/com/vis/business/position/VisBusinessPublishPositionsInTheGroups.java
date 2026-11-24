package com.vis.business.position;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.instant.messenger.CcpInstantMessenger;
import com.jn.business.messages.JnMessageType;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnGroupers;
import com.jn.utils.JnSystemProperties;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityPositionInstantMessageGroup;
import com.vis.entities.VisEntityVirtualPositionPublishingDay;
import com.vis.json.transformers.VisPositionPublishingDayCombinations;

/**
 * Publishes in the groups of instant messages the positions of the day (since 2026-11-24), every day at 10:00, by the bot
 * of each position: the group of {@code jn_grouper} of today ({@link VisEntityVirtualPositionPublishingDay}) names the
 * positions with {@code mustSendInGroups} still valid; each one is read by id and goes to every group of its seniorities
 * and ddds ({@link VisBusinessInstantMessageGroupsOfThePosition}), one message per group, waiting
 * {@value #SECONDS_BETWEEN_MESSAGES} seconds between messages so Telegram does not refuse them (429). A failure in a group
 * (bot blocked, removed from the group) is logged and the next group goes on.
 * <p>
 * The message ({@link VisBusinessPositionPublishingMessage}) is html of Telegram; longer than {@value #TELEGRAM_LIMIT}
 * characters, it goes in two messages, the second answering the first.
 */
public class VisBusinessPublishPositionsInTheGroups implements CcpBusiness {

	/** The output of this business. */
	public static enum Fields implements CcpJsonFieldName {
		/** How many positions the group of the day named. */
		positions,
		/** How many messages were sent. */
		messagesSent,
		/** How many sendings failed. */
		failures,
	}

	/** The seconds between two messages. */
	static final int SECONDS_BETWEEN_MESSAGES = 3;

	/** The largest message Telegram accepts. */
	static final int TELEGRAM_LIMIT = 4096;

	/** The single instance: the class keeps no state. */
	public static final VisBusinessPublishPositionsInTheGroups INSTANCE = new VisBusinessPublishPositionsInTheGroups();

	private VisBusinessPublishPositionsInTheGroups() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		List<CcpJsonRepresentation> children = getPositionsOfToday();
		CcpJsonRepresentation withPositions = json.put(Fields.positions, children.size());
		boolean noPosition = children.isEmpty();
		if(noPosition) {
			return withPositions.put(Fields.messagesSent, 0).put(Fields.failures, 0);
		}
		List<CcpJsonRepresentation> positionKeys = new ArrayList<>();
		for (CcpJsonRepresentation child : children) {
			CcpJsonRepresentation positionKey = child.getJsonPiece(VisEntityPosition.Fields.title, VisEntityPosition.Fields.recruiter);
			positionKeys.add(positionKey);
		}
		int keysCount = positionKeys.size();
		CcpJsonRepresentation[] positionKeysArray = positionKeys.toArray(new CcpJsonRepresentation[keysCount]);
		CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
		CcpSelectUnionAll positionsById = crud.unionAll(positionKeysArray, JnDeleteKeysFromCache.INSTANCE, VisEntityPosition.ENTITY);
		long now = System.currentTimeMillis();
		int[] counters = {0, 0};
		for (int index = 0; index < keysCount; index++) {
			CcpJsonRepresentation positionKey = positionKeys.get(index);
			boolean inactive = false == VisEntityPosition.ENTITY.isPresentInThisUnionAll(positionsById, positionKey);
			if(inactive) {
				continue;
			}
			CcpJsonRepresentation position = VisEntityPosition.ENTITY.getRecordFromUnionAll(positionsById, () -> positionKey);
			boolean expired = isExpired(position, now);
			if(expired) {
				continue;
			}
			CcpJsonRepresentation child = children.get(index);
			String botType = child.getOrDefault(VisEntityPositionInstantMessageGroup.Fields.botType, () -> JnMessageType.JnBotType.user.name());
			publish(position, botType, counters);
		}
		CcpJsonRepresentation response = withPositions
				.put(Fields.messagesSent, counters[0])
				.put(Fields.failures, counters[1]);
		return response;
	}

	// the children of the group of today: the title, the recruiter and the bot of each position
	private static List<CcpJsonRepresentation> getPositionsOfToday() {
		CcpTimeDecorator today = new CcpTimeDecorator();
		String pattern = VisPositionPublishingDayCombinations.Constants.DAY_PATTERN.getValue();
		String day = today.getFormattedDateTime(pattern);
		CcpJsonRepresentation dayKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualPositionPublishingDay.Fields.day, day);
		String parentId = VisEntityVirtualPositionPublishingDay.ENTITY.calculateId(dayKey);
		String parentName = VisEntityVirtualPositionPublishingDay.ENTITY.getEntityMetaData().entityName;
		String childName = VisEntityPosition.ENTITY.getEntityMetaData().entityName;
		List<String> childTexts = JnGroupers.getChildren(false, parentName, childName, parentId);
		List<CcpJsonRepresentation> children = new ArrayList<>();
		for (String childText : childTexts) {
			CcpJsonRepresentation child = new CcpJsonRepresentation(childText);
			children.add(child);
		}
		return children;
	}

	private static boolean isExpired(CcpJsonRepresentation position, long now) {
		CcpJsonRepresentation expiration = position.getInnerJson(VisEntityPosition.Fields.expiration);
		Double end = expiration.getAsDoubleNumber(VisPositionPublishingDayCombinations.ExpirationFields.max);
		boolean expired = end < now;
		return expired;
	}

	private static void publish(CcpJsonRepresentation position, String botType, int[] counters) {
		List<Object> chatIds = getChatIds(position);
		boolean noGroup = chatIds.isEmpty();
		if(noGroup) {
			return;
		}
		CcpJsonRepresentation messageOutput = VisBusinessPositionPublishingMessage.INSTANCE.execute(position);
		List<String> pieces = messageOutput.getAsStringList(VisBusinessPositionPublishingMessage.Fields.pieces);
		CcpStringDecorator botTypeDecorator = new CcpStringDecorator(botType);
		CcpJsonFieldName botName = botTypeDecorator.jsonFieldName();
		String botToken = JnSystemProperties.INSTANCE.getSystemInnerProperty(JnMessageType.InstantMessengerApiFields.bots, botName);
		CcpInstantMessenger instantMessenger = CcpDependencyInjection.getDependency(CcpInstantMessenger.class);
		CcpTimeDecorator timer = new CcpTimeDecorator();
		for (Object chatId : chatIds) {
			CcpJsonRepresentation chat = CcpOtherConstants.EMPTY_JSON.put(VisEntityPositionInstantMessageGroup.Fields.chatId, chatId);
			Long chatIdNumber = chat.getAsLongNumber(VisEntityPositionInstantMessageGroup.Fields.chatId);
			try {
				long replyTo = 0L;
				for (String piece : pieces) {
					timer.sleep(SECONDS_BETWEEN_MESSAGES * 1000);
					CcpJsonRepresentation result = instantMessenger.sendHtmlMessage(botName, botToken, chatIdNumber, replyTo, piece);
					// a messenger that does not answer the id of the message: the next piece answers no message
					boolean answeredTheId = result.containsAllFields(CcpJsonCommonsFields.replyTo);
					replyTo = answeredTheId ? result.getAsLongNumber(CcpJsonCommonsFields.replyTo) : 0L;
					counters[0]++;
				}
			} catch (RuntimeException failure) {
				failure.printStackTrace();
				counters[1]++;
			}
		}
	}

	// the groups of the seniorities and ddds of the position; the positions saved before 2026-11-23 keep one seniority as a text
	private static List<Object> getChatIds(CcpJsonRepresentation position) {
		Object seniority = position.getOrDefault(VisEntityPosition.Fields.seniority, () -> List.of());
		boolean singleSeniority = seniority instanceof String;
		List<String> seniorities = singleSeniority ? List.of((String) seniority) : position.getAsStringList(VisEntityPosition.Fields.seniority);
		List<Object> ddds = position.getAsObjectList(VisEntityPosition.Fields.ddd);
		boolean nothingToCombine = seniorities.isEmpty() || ddds.isEmpty();
		if(nothingToCombine) {
			return List.of();
		}
		Object resumeType = position.get(VisEntityPosition.Fields.resumeType);
		CcpJsonRepresentation groupsInput = CcpOtherConstants.EMPTY_JSON
				.put(VisBusinessInstantMessageGroupsOfThePosition.Fields.resumeType, resumeType)
				.put(VisBusinessInstantMessageGroupsOfThePosition.Fields.seniority, seniorities)
				.put(VisBusinessInstantMessageGroupsOfThePosition.Fields.ddd, ddds);
		CcpJsonRepresentation groupsOutput = VisBusinessInstantMessageGroupsOfThePosition.INSTANCE.execute(groupsInput);
		List<Object> chatIds = groupsOutput.getAsObjectList(VisEntityPositionInstantMessageGroup.Fields.chatId);
		return chatIds;
	}
}
