package com.vis.business.job.posting;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;
import com.ccp.json.validations.global.engine.CcpJsonValidationError;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.vis.entities.VisEntityInstantMessagePositionReply;

/**
 * Takes a message written in a group of Telegram where the support bot is as a job posting (since 2026-11-27): the
 * business that the support bot runs for the messages of its groups ({@code jn_bot_group_message_handler}, seeded by
 * {@link VisEntityInstantMessagePositionReply}). The message is saved in {@code vis_instant_message_position_reply}, whose
 * businesses before the save find the resumes and whose business after the save answers the message in the group.
 * <p>
 * A message that is no job posting is ignored: one the fields of the entity refuse (a text shorter than 100 characters,
 * a conversation of the group, or longer than 5,000) is not saved, and the refusals of the businesses before the save
 * (no skill, filters that already have a text) only log and cancel the save.
 */
public class VisBusinessSaveTheJobPostingOfTheGroup implements CcpBusiness {

	/** The single instance. */
	public static final VisBusinessSaveTheJobPostingOfTheGroup INSTANCE = new VisBusinessSaveTheJobPostingOfTheGroup();

	private VisBusinessSaveTheJobPostingOfTheGroup() {}

	/**
	 * Saves the message of the group as a job posting, when the entity takes it.
	 * @param json the simplified message of the group: {@code chatId}, {@code message_id} and {@code message} (the text)
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		Long groupChatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
		Long replyTo = json.getAsLongNumber(CcpJsonCommonsFields.message_id);
		String description = json.getAsString(JnJsonInstantMessengerFields.message);
		CcpJsonRepresentation jobPosting = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityInstantMessagePositionReply.Fields.groupChatId, groupChatId)
				.put(VisEntityInstantMessagePositionReply.Fields.replyTo, replyTo)
				.put(VisEntityInstantMessagePositionReply.Fields.description, description);
		try {
			VisEntityInstantMessagePositionReply.ENTITY.save(jobPosting);
			return json;
		} catch (CcpJsonValidationError notAJobPosting) {
			// most messages of a group are no job posting: the entity refuses them and the message is ignored
			return json;
		}
	}
}
