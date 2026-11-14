package com.vis.business.statistics;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.utils.JnGroupers;

/**
 * The counters read by the screen Supply X Demand when it opens, for each of {@link VisResumeTotalsCounters} (by ddd, by
 * day and by month of insertion) and each of {@link VisResumeStatisticsSources} (the active resumes and the views of the
 * resumes): the titles of the folders and the line chart of the insertions. One read by id of all the groups in
 * {@code jn_grouper} (OQL), whose only child is the JSON text {@code {"<key>": total}}. A key with no record is left out.
 */
public class VisBusinessResumeTotals implements CcpBusiness {

	/** The output of this business: one item per counter, holding one item per source with {key: total}. No input. */
	public static enum Fields implements CcpJsonFieldName {
		;
	}

	/** The single instance. */
	public static final VisBusinessResumeTotals INSTANCE = new VisBusinessResumeTotals();

	private VisBusinessResumeTotals() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	/**
	 * Reads the groups of every counter of every source.
	 * @param json not used
	 * @return {counter: {source: {key: total}}}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		VisResumeTotalsCounters[] counters = VisResumeTotalsCounters.values();
		VisResumeStatisticsSources[] sources = VisResumeStatisticsSources.values();
		List<CcpJsonRepresentation> groupKeys = new ArrayList<>();
		for (VisResumeTotalsCounters counter : counters) {
			for (VisResumeStatisticsSources source : sources) {
				CcpJsonRepresentation groupKey = source.getTotalsGroupKey(counter);
				groupKeys.add(groupKey);
			}
		}
		List<List<String>> childrenOfEachGroup = JnGroupers.getChildrenOfEachGroup(groupKeys);
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON;
		int groupIndex = 0;
		for (VisResumeTotalsCounters counter : counters) {
			CcpJsonRepresentation totalsOfTheCounter = CcpOtherConstants.EMPTY_JSON;
			for (VisResumeStatisticsSources source : sources) {
				List<String> children = childrenOfEachGroup.get(groupIndex);
				groupIndex++;
				CcpJsonRepresentation totals = getTotals(children);
				totalsOfTheCounter = totalsOfTheCounter.put(source, totals);
			}
			response = response.put(counter, totalsOfTheCounter);
		}
		return response;
	}

	/**
	 * The counters of a group.
	 * @param children the only child of the group, the JSON text of the counters; empty when the group does not exist
	 * @return {key: total}, empty when there is no group
	 */
	private static CcpJsonRepresentation getTotals(List<String> children) {
		boolean noGroup = children.isEmpty();

		if(noGroup) {
			return CcpOtherConstants.EMPTY_JSON;
		}

		String counters = children.get(0);
		CcpJsonRepresentation totals = new CcpJsonRepresentation(counters);
		return totals;
	}
}
