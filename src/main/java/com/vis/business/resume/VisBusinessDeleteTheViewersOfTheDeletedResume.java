package com.vis.business.resume;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityResumeViewsOfTheMonth;
import com.vis.entities.VisEntityViewedResume;

/**
 * Runs after a resume is deleted everywhere (the owner deleted all the data of the resume): deletes, from
 * {@code jn_grouper}, every group of the screen "Quem me viu" of the e-mail of the resume (parent
 * {@code vis_resume_views_of_the_month}, child {@code vis_viewed_resume}), one per month. Without this, the counters of
 * the domains that viewed the resume stayed, and a new resume of the same e-mail showed them again. The deactivation
 * (the resume goes to the twin) keeps them.
 * <p>
 * No query (OQL): the id of each group is calculated from the e-mail and the month, from the first month of the
 * counting ({@link Constants#FIRST_MONTH}) to the current one, and the groups are deleted by id in one bulk (deleting a
 * group that does not exist is silent), which also clears their cache keys. The groups of counters are always on the
 * main side of the grouper.
 */
public class VisBusinessDeleteTheViewersOfTheDeletedResume implements CcpBusiness {

	/** The fixed values of this business. */
	enum Constants implements CcpJsonFieldName {
		/** The first month of the counting by domain (yyyyMM): the screen "Quem me viu" was rebuilt on 2026-10-22. */
		FIRST_MONTH {
			public String getValue() {
				return "202610";
			}
		}
	}

	/** The single instance: the class keeps no state. */
	public static final VisBusinessDeleteTheViewersOfTheDeletedResume INSTANCE = new VisBusinessDeleteTheViewersOfTheDeletedResume();

	private VisBusinessDeleteTheViewersOfTheDeletedResume() {}

	/**
	 * Runs the business described in the class documentation.
	 * @param json the key of the deleted resume (its {@code email}, readable or already hashed)
	 * @return the same JSON
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpEntityMetaData viewsDetails = VisEntityResumeViewsOfTheMonth.ENTITY.getEntityMetaData();
		CcpEntityMetaData viewDetails = VisEntityViewedResume.ENTITY.getEntityMetaData();
		CcpEntity grouper = JnGroupers.getGrouper(false);
		CcpEntityMetaData grouperDetails = grouper.getEntityMetaData();
		CcpJsonRepresentation owner = json.getJsonPiece(VisEntityResumeViewsOfTheMonth.Fields.email);
		List<String> months = getMonths();
		List<CcpBulkItem> items = new ArrayList<>();
		for (String month : months) {
			CcpJsonRepresentation ownerInTheMonth = owner.put(VisEntityResumeViewsOfTheMonth.Fields.monthYear, month);
			CcpJsonRepresentation ownerWithHashedEmail = VisEntityResumeViewsOfTheMonth.ENTITY.getJsonWithHandledPrimaryKey(ownerInTheMonth);
			// the transformer also keeps the original e-mail: only the hash calculates the id
			CcpJsonRepresentation parentKey = viewsDetails.getOnlyExistingFields(ownerWithHashedEmail);
			String parentId = VisEntityResumeViewsOfTheMonth.ENTITY.calculateId(parentKey);
			CcpJsonRepresentation groupKey = JnGroupers.getGroupKey(viewsDetails.entityName, viewDetails.entityName, parentId);
			CcpBulkItem deleteItem = grouperDetails.toDeleteBulkItem(groupKey);
			items.add(deleteItem);
		}
		JnExecuteBulkOperation.INSTANCE.executeBulk(items, JnDeleteKeysFromCache.INSTANCE);
		return json;
	}

	/**
	 * The months of the counting, as {@code yyyyMM}: from the first one to the current one.
	 * @return the months, the oldest first
	 */
	static List<String> getMonths() {
		String currentMonth = CcpEntityExpurgableOptions.monthly.getFormattedDate();
		int lastMonth = Integer.parseInt(currentMonth);
		String firstMonthText = Constants.FIRST_MONTH.getValue();
		int month = Integer.parseInt(firstMonthText);
		List<String> months = new ArrayList<>();
		while (month <= lastMonth) {
			String monthText = String.valueOf(month);
			months.add(monthText);
			boolean december = month % 100 == 12;
			// december goes to january of the next year
			month = december ? month + 89 : month + 1;
		}
		return months;
	}
}
