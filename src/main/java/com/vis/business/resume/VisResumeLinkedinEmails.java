package com.vis.business.resume;

import java.util.Arrays;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.bulk.CcpBulkScriptedUpdateFields;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityResumeLinkedin;

/**
 * The e-mails (hashes) that use a LinkedIn address in {@link VisEntityResumeLinkedin}. The writes are atomic scripts run
 * by the database ({@code scriptedUpdate}), as in the groupers: two resumes adding or removing their e-mail of the same
 * address at the same time are applied one after the other, and none overwrites the other.
 */
class VisResumeLinkedinEmails {

	/** The scripts run over the record of an address, and their parameters. */
	enum Scripts implements CcpJsonFieldName {
		/** Adds the e-mail when it is not there yet; the record is created by the upsert when the address is new. */
		addEmail {
			public String getValue() {
				return "if (ctx._source.emails.contains(params.email)) { ctx.op = 'none'; } else { ctx._source.emails.add(params.email); }";
			}
		},
		/** Removes the e-mail; deletes the record left without e-mails; creates nothing when the address has no record. */
		removeEmail {
			public String getValue() {
				return "if (ctx._source.emails == null) { ctx.op = 'none'; } "
						+ "else { ctx._source.emails.removeIf(e -> e == params.email); "
						+ "if (ctx._source.emails.isEmpty()) { ctx.op = 'delete'; } }";
			}
		},
		/** Parameter: the hash of the e-mail being added or removed. */
		email,
	}

	private VisResumeLinkedinEmails() {}

	/**
	 * The key of the record of an address.
	 * @param linkedinAddress the address
	 * @return the key
	 */
	static CcpJsonRepresentation getAddressKey(String linkedinAddress) {
		CcpJsonRepresentation addressKey = CcpOtherConstants.EMPTY_JSON.put(VisEntityResumeLinkedin.Fields.linkedinAddress, linkedinAddress);
		return addressKey;
	}

	/**
	 * The e-mails that use the address, read by id.
	 * @param addressKey the key of the address
	 * @return the hashes of the e-mails, empty when the address has no record
	 */
	static List<String> getEmails(CcpJsonRepresentation addressKey) {
		CcpEntityMetaData metaData = VisEntityResumeLinkedin.ENTITY.getEntityMetaData();
		CcpJsonRepresentation addressRecord = metaData.getOneByIdOrHandleItIfThisIdWasNotFound(addressKey, notFound -> CcpOtherConstants.EMPTY_JSON);
		List<String> emails = addressRecord.getAsStringList(VisEntityResumeLinkedin.Fields.emails);
		return emails;
	}

	/**
	 * Adds the e-mail to the address; an address without record gets one with this e-mail only.
	 * @param addressKey the key of the address
	 * @param hashedEmail the hash of the e-mail
	 */
	static void addEmail(CcpJsonRepresentation addressKey, String hashedEmail) {
		List<String> onlyThisEmail = Arrays.asList(hashedEmail);
		CcpJsonRepresentation newRecord = addressKey.put(VisEntityResumeLinkedin.Fields.emails, onlyThisEmail);
		execute(addressKey, Scripts.addEmail, hashedEmail, newRecord, false);
	}

	/**
	 * Takes the e-mail out of the address; the record left without e-mails is deleted.
	 * @param addressKey the key of the address
	 * @param hashedEmail the hash of the e-mail
	 */
	static void removeEmail(CcpJsonRepresentation addressKey, String hashedEmail) {
		// the script also runs when the record does not exist, to decide not to create it
		execute(addressKey, Scripts.removeEmail, hashedEmail, CcpOtherConstants.EMPTY_JSON, true);
	}

	/**
	 * Runs a scripted update over the record of the address.
	 * @param addressKey the key of the address
	 * @param script the script
	 * @param hashedEmail the hash of the e-mail, the parameter of the script
	 * @param upsert the record created as is when there is none (empty for none)
	 * @param scriptedUpsert whether the script also runs when there is no record
	 */
	private static void execute(CcpJsonRepresentation addressKey, Scripts script, String hashedEmail, CcpJsonRepresentation upsert, boolean scriptedUpsert) {
		String source = script.getValue();
		String addressId = VisEntityResumeLinkedin.ENTITY.calculateId(addressKey);
		CcpJsonRepresentation params = CcpOtherConstants.EMPTY_JSON.put(Scripts.email, hashedEmail);
		CcpJsonRepresentation itemWithScript = CcpOtherConstants.EMPTY_JSON.put(CcpBulkScriptedUpdateFields.script, source);
		CcpJsonRepresentation itemWithParams = itemWithScript.put(CcpBulkScriptedUpdateFields.params, params);
		CcpJsonRepresentation itemWithUpsert = itemWithParams.put(CcpBulkScriptedUpdateFields.upsert, upsert);
		CcpJsonRepresentation item = itemWithUpsert.put(CcpBulkScriptedUpdateFields.scriptedUpsert, scriptedUpsert);
		CcpBulkItem bulkItem = new CcpBulkItem(item, CcpBulkEntityOperationType.scriptedUpdate, VisEntityResumeLinkedin.ENTITY, addressId);
		List<CcpBulkItem> bulkItems = Arrays.asList(bulkItem);
		JnExecuteBulkOperation.INSTANCE.executeBulk(bulkItems, JnDeleteKeysFromCache.INSTANCE);
	}
}
