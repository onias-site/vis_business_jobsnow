package com.vis.business.resume;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.bulk.CcpBulkScriptedUpdateFields;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.entities.JnEntityGrouper;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntityResume;

/**
 * Writes the groups of {@code JnEntityGrouper} that hold the resumes ({@code parentName = resumesGrouping},
 * {@code childName = vis_resume}). Each group is split in {@link #CHILD_PAGES} pages and a resume always falls in the
 * same page, chosen by its e-mail, so adding or removing it touches a single record of each group. The writes are
 * atomic scripts run by the database ({@code scriptedUpdate}): two resumes changing the same page at the same time are
 * applied one after the other, and none overwrites the other. Nothing is read before writing.
 */
public final class VisResumeGroupers {

	/** The scripts run over a page of a group, and their parameters. */
	enum Scripts implements CcpJsonFieldName {
		/** Removes the child with the same key, if any, adds the new version and recounts; keeps the parent values. */
		addChild {
			public String getValue() {
				return "ctx._source.childJson.removeIf(c -> c[params.childKeyField] == params.childKey); "
						+ "ctx._source.childJson.add(params.child); "
						+ "ctx._source.childCount = ctx._source.childJson.size(); "
						+ "ctx._source.parentJson = params.parentJson;";
			}
		},
		/** Removes the child with the key; deletes the page left empty; creates nothing when the page does not exist. */
		removeChild {
			public String getValue() {
				return "if (ctx._source.childJson == null) { ctx.op = 'none'; } "
						+ "else { ctx._source.childJson.removeIf(c -> c[params.childKeyField] == params.childKey); "
						+ "if (ctx._source.childJson.isEmpty()) { ctx.op = 'delete'; } "
						+ "else { ctx._source.childCount = ctx._source.childJson.size(); } }";
			}
		},
		/** Parameter: the name of the field that identifies a child. */
		childKeyField,
		/** Parameter: the value of that field in the child being added or removed. */
		childKey,
		/** Parameter: the child being added. */
		child,
		/** Parameter: the values the parent id came from. */
		parentJson,
	}

	/** How many pages each group has. */
	public static final int CHILD_PAGES = 32;

	/** How many groups are written at a time. */
	static final int BULK_SIZE = 1_000;

	private VisResumeGroupers() {}

	/**
	 * The page of every group a resume falls in: always the same for the same resume.
	 * @param resumeEmail the e-mail of the stored resume (already transformed)
	 * @return the page, from 0 to {@link #CHILD_PAGES} - 1
	 */
	public static int getChildPage(String resumeEmail) {
		int emailHash = resumeEmail.hashCode();
		int page = Math.floorMod(emailHash, CHILD_PAGES);
		return page;
	}

	/**
	 * The key of a page of the group of a hash.
	 * @param hash the hash (the parent id)
	 * @param childPage the page
	 * @return the key: parent id, parent name, child name and page
	 */
	public static CcpJsonRepresentation getGroupKey(String hash, int childPage) {
		String parentName = VisResumeGrouperNames.resumesGrouping.name();
		CcpEntityMetaData resumeMetaData = VisEntityResume.ENTITY.getEntityMetaData();
		String childName = resumeMetaData.entityName;
		CcpJsonRepresentation keyWithParentId = CcpOtherConstants.EMPTY_JSON.put(JnEntityGrouper.Fields.parentId, hash);
		CcpJsonRepresentation keyWithParentName = keyWithParentId.put(JnEntityGrouper.Fields.parentName, parentName);
		CcpJsonRepresentation keyWithChildName = keyWithParentName.put(JnEntityGrouper.Fields.childName, childName);
		CcpJsonRepresentation groupKey = keyWithChildName.put(JnEntityGrouper.Fields.childPage, childPage);
		return groupKey;
	}

	/**
	 * Reads the stored resume wherever it is: in the main entity or in the twin (an inactive resume).
	 * @param json the resume (at least its key)
	 * @return the stored resume, or an empty json when it is in neither
	 */
	static CcpJsonRepresentation getStoredResume(CcpJsonRepresentation json) {
		boolean inTheMainEntity = VisEntityResume.ENTITY.exists(json);

		if(inTheMainEntity) {
			CcpJsonRepresentation storedResume = VisEntityResume.ENTITY.getOneById(json);
			return storedResume;
		}

		CcpEntity twinEntity = VisEntityResume.ENTITY.getTwinEntity();
		boolean inTheTwin = twinEntity.exists(json);

		if(inTheTwin) {
			CcpJsonRepresentation storedResume = twinEntity.getOneById(json);
			return storedResume;
		}

		return CcpOtherConstants.EMPTY_JSON;
	}

	/**
	 * The parameters every script gets to find the resume among the children: the e-mail field and its value.
	 * @param resumeEmail the e-mail of the stored resume (already transformed)
	 * @return the parameters
	 */
	static CcpJsonRepresentation getChildKeyParams(String resumeEmail) {
		String emailFieldName = VisEntityResume.Fields.email.name();
		CcpJsonRepresentation paramsWithKeyField = CcpOtherConstants.EMPTY_JSON.put(Scripts.childKeyField, emailFieldName);
		CcpJsonRepresentation params = paramsWithKeyField.put(Scripts.childKey, resumeEmail);
		return params;
	}

	/**
	 * A scripted update of a page of a group.
	 * @param groupKey the key of the page
	 * @param script the script
	 * @param params the parameters of the script
	 * @param upsert the page created as is when it does not exist (empty for none)
	 * @param scriptedUpsert whether the script also runs when the page does not exist
	 * @return the bulk item
	 */
	static CcpBulkItem getScriptedItem(CcpJsonRepresentation groupKey, Scripts script, CcpJsonRepresentation params, CcpJsonRepresentation upsert, boolean scriptedUpsert) {
		String source = script.getValue();
		String groupId = JnEntityGrouper.ENTITY.calculateId(groupKey);
		CcpJsonRepresentation itemWithScript = CcpOtherConstants.EMPTY_JSON.put(CcpBulkScriptedUpdateFields.script, source);
		CcpJsonRepresentation itemWithParams = itemWithScript.put(CcpBulkScriptedUpdateFields.params, params);
		CcpJsonRepresentation itemWithUpsert = itemWithParams.put(CcpBulkScriptedUpdateFields.upsert, upsert);
		CcpJsonRepresentation item = itemWithUpsert.put(CcpBulkScriptedUpdateFields.scriptedUpsert, scriptedUpsert);
		CcpBulkItem bulkItem = new CcpBulkItem(item, CcpBulkEntityOperationType.scriptedUpdate, JnEntityGrouper.ENTITY, groupId);
		return bulkItem;
	}

	/**
	 * Writes, {@link #BULK_SIZE} at a time, the scripted update of each hash.
	 * @param hashes the hashes
	 * @param itemOfTheHash builds the scripted update of the hash
	 */
	static void writeGroups(List<String> hashes, Function<String, CcpBulkItem> itemOfTheHash) {
		int hashesCount = hashes.size();
		for (int start = 0; start < hashesCount; start += BULK_SIZE) {
			int end = Math.min(start + BULK_SIZE, hashesCount);
			List<String> page = hashes.subList(start, end);
			List<CcpBulkItem> bulkItems = new ArrayList<>();
			for (String hash : page) {
				CcpBulkItem item = itemOfTheHash.apply(hash);
				bulkItems.add(item);
			}
			JnExecuteBulkOperation.INSTANCE.executeBulk(bulkItems, JnDeleteKeysFromCache.INSTANCE);
		}
	}
}
