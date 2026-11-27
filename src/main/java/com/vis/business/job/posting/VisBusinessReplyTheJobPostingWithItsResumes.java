package com.vis.business.job.posting;

import com.ccp.business.CcpBusiness;
import com.jn.business.messages.JnSendHtmlInstantMessage;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.instant.messenger.CcpInstantMessenger;
import com.jn.business.messages.JnMessageType;
import com.jn.utils.JnSystemProperties;
import com.vis.entities.VisEntityInstantMessagePositionReply;

/**
 * After the save of {@code vis_instant_message_position_reply} (since 2026-11-27): the support bot answers, in the group,
 * the message of the job posting ({@code replyTo}) with the resumes its search found, their count, the averages and the
 * link of the screen {@code curriculos/buscar} ({@link VisJobPostingReplyTexts#resumesFound}, html of Telegram, in the
 * language of the groups). A search that found no resume (no link, or a count of zero) answers nothing. A job posting
 * refused before the save is not saved, and this business does not run.
 */
public class VisBusinessReplyTheJobPostingWithItsResumes implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public static final VisBusinessReplyTheJobPostingWithItsResumes INSTANCE = new VisBusinessReplyTheJobPostingWithItsResumes();

	private VisBusinessReplyTheJobPostingWithItsResumes() {}

	/**
	 * Answers the message of the job posting with its resumes.
	 * @param json the record saved, with the count, the averages and the link of the search
	 * @return the record as it came
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		// the search that writes the link writes the count too
		boolean noLink = false == json.containsAllFields(VisEntityInstantMessagePositionReply.Fields.linkToShowResumes);

		if(noLink) {
			return json;
		}

		Integer resumesCount = json.getAsIntegerNumber(VisEntityInstantMessagePositionReply.Fields.resumesCount);

		if(resumesCount == 0) {
			return json;
		}

		String message = getMessage(json, resumesCount);
		String botType = JnMessageType.JnBotType.support.name();
		CcpStringDecorator botTypeDecorator = new CcpStringDecorator(botType);
		CcpJsonFieldName botName = botTypeDecorator.jsonFieldName();
		String botToken = JnSystemProperties.INSTANCE.getSystemInnerProperty(JnMessageType.InstantMessengerApiFields.bots, botName);
		Long groupChatId = json.getAsLongNumber(VisEntityInstantMessagePositionReply.Fields.groupChatId);
		Long replyTo = json.getAsLongNumber(VisEntityInstantMessagePositionReply.Fields.replyTo);
		CcpInstantMessenger instantMessenger = CcpDependencyInjection.getDependency(CcpInstantMessenger.class);
		JnSendHtmlInstantMessage.send(instantMessenger, botName, botToken, groupChatId, replyTo, message);
		return json;
	}

	/**
	 * The answer: the count, the averages in Brazilian format (an average no resume informed as "não informada") and the
	 * link of the resumes.
	 * @param json the record saved
	 * @param resumesCount how many resumes the search found
	 * @return the html of the answer
	 */
	private static String getMessage(CcpJsonRepresentation json, Integer resumesCount) {
		String link = json.getAsString(VisEntityInstantMessagePositionReply.Fields.linkToShowResumes);
		String avgClt = VisBusinessJobPostingPublishingMessage.getCurrency(json, VisEntityInstantMessagePositionReply.Fields.avgClt);
		String avgPj = VisBusinessJobPostingPublishingMessage.getCurrency(json, VisEntityInstantMessagePositionReply.Fields.avgPj);
		String avgExperience = VisBusinessJobPostingPublishingMessage.getExperience(json);
		CcpJsonRepresentation values = CcpOtherConstants.EMPTY_JSON
				.put(VisBusinessJobPostingPublishingMessage.TemplateFields.resumesCount, resumesCount)
				.put(VisBusinessJobPostingPublishingMessage.TemplateFields.avgClt, avgClt)
				.put(VisBusinessJobPostingPublishingMessage.TemplateFields.avgPj, avgPj)
				.put(VisBusinessJobPostingPublishingMessage.TemplateFields.avgExperience, avgExperience)
				.put(VisBusinessJobPostingPublishingMessage.TemplateFields.linkToShowResumes, link);
		String message = VisJobPostingReplyTexts.resumesFound.getMessage(VisBusinessJobPostingPublishingMessage.LANGUAGE, values);
		return message;
	}
}
