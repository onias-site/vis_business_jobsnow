package com.vis.business.telegram;

import java.util.Arrays;
import java.util.List;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.jn.entities.JnEntitySystemMessage;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.messages.JnSystemMessage;
import com.jn.utils.JnLanguage;

/**
 * The records of {@link JnEntitySystemMessage} about the Telegram: each line of {@link #groups} is the link of a group
 * whose positions are read by the load.
 */
public enum VisTelegramSystemMessages implements JnSystemMessage {
	/** The links of the groups, one per line of the message. */
	groups;

	/**
	 * The record of the links of the groups, in Portuguese.
	 * @return the record
	 */
	public static CcpJsonRepresentation getGroupsRecord() {
		List<String> links = Arrays.asList(
				  "https://t.me/empregossp"
				, "https://t.me/vagas_saopaulo"
				, "https://t.me/VAGASELENIZEDEPOLI"
				, "https://t.me/vagadeempregogoiania"
				, "https://t.me/empregajampa"
				, "https://t.me/itnetworkingbrazil"
				, "https://t.me/ITJOBS_EMPREGOSTI"
				, "https://t.me/vagasRH"
				, "https://t.me/multiplataformaTI"
				, "https://t.me/joinchat/q_PRgF_18n00NjEx"
				, "https://t.me/vagastibr"
				, "https://t.me/vagasparacursosuperioroutecnico"
				, "https://t.me/vagastisapbrasil"
				, "https://t.me/+f9d1rNi7-15iZTRh"
				, "https://t.me/Jobs4IT"
				, "https://t.me/joinchat/VCso3zuEeF-GkDnr"
				, "https://t.me/phpbrasilvagas"
				);
		CcpJsonRepresentation groupsId = groups.getSystemMessageId(JnLanguage.portuguese);
		CcpJsonRepresentation groupsRecord = groupsId.put(JnJsonCommonsFields.message, links);
		return groupsRecord;
	}

	/**
	 * The seed of the links of the groups.
	 * @return the records of {@code jn_system_message}
	 */
	public static List<CcpBulkItem> getSeeds() {
		CcpJsonRepresentation groupsRecord = getGroupsRecord();
		List<CcpBulkItem> seeds = JnEntitySystemMessage.ENTITY.toBulkItems(groupsRecord, CcpBulkEntityOperationType.create);
		return seeds;
	}
}
