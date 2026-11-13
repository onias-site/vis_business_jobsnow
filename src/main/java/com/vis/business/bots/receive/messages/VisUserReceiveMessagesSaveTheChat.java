package com.vis.business.bots.receive.messages;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityResume;

/**
 * Saves the chat of the session in the resume of the e-mail, active ({@code vis_resume}) or inactive
 * ({@code vis_inactive_resume}), read on both sides at once. The chat goes by a partial update of the bulk, on the side
 * where the resume is: the save of the resume replaces the whole record, would e-mail the owner and would reactivate an
 * inactive resume. An e-mail without resume is told to register one ({@code resumeNotFound}).
 */
public class VisUserReceiveMessagesSaveTheChat implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public final static VisUserReceiveMessagesSaveTheChat INSTANCE = new VisUserReceiveMessagesSaveTheChat();

	private VisUserReceiveMessagesSaveTheChat() {}

	/**
	 * Saves the chat in the resume.
	 * @param json the session, with {@code email} and {@code chatId}
	 * @return the session, as it came
	 * @throws CcpErrorFlowDisturb with {@code resumeNotFound}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String email = json.getAsString(JnJsonCommonsFields.email);
		CcpJsonRepresentation resumeKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.email, email);
		CcpJsonRepresentation bothSides = VisEntityResume.ENTITY.getOneByIdAnyWhere(resumeKey);
		CcpEntity twinEntity = VisEntityResume.ENTITY.getTwinEntity();
		CcpJsonRepresentation activeResume = bothSides.getInnerJson(VisEntityResume.ENTITY);
		CcpJsonRepresentation inactiveResume = bothSides.getInnerJson(twinEntity);
		boolean active = false == activeResume.isEmpty();
		boolean resumeNotFound = false == active && inactiveResume.isEmpty();
		if(resumeNotFound) {
			CcpJsonRepresentation jsonWithReply = VisUserReceiveMessagesConversation.reply(json, VisUserReceiveMessagesMessages.resumeNotFound);
			return VisUserReceiveMessagesStatus.resumeNotFound.throwException(jsonWithReply);
		}
		CcpEntity side = active ? VisEntityResume.ENTITY : twinEntity;
		CcpJsonRepresentation resume = active ? activeResume : inactiveResume;
		// the stored record holds the e-mail already hashed, which is what the id is calculated from
		String resumeId = side.calculateId(resume);
		Long chatId = json.getAsLongNumber(JnJsonInstantMessengerFields.chatId);
		CcpJsonRepresentation chat = CcpOtherConstants.EMPTY_JSON.put(VisEntityResume.Fields.chatId, chatId);
		CcpBulkItem item = new CcpBulkItem(chat, CcpBulkEntityOperationType.update, side, resumeId);
		List<CcpBulkItem> items = List.of(item);
		JnExecuteBulkOperation.INSTANCE.executeBulk(items, JnDeleteKeysFromCache.INSTANCE);
		return json;
	}
}
