package com.vis.business.statistics;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.utils.JnGroupers;

/**
 * The totals of every region for each of {@link VisResumeStatisticsSources} (the active resumes and the views of the
 * resumes): how many records each ddd has, for the titles of the folders of the screen Supply X Demand. One read by id of
 * the groups of counters by ddd in {@code jn_grouper} ({@code vis_virtual_ddd_counter}, OQL), whose only child is the JSON
 * text {@code {"<ddd>": total}}. A ddd with no record is left out.
 */
public class VisBusinessResumeTotalsByRegion implements CcpBusiness {

	/** The output of this business: one item per source, holding {ddd: total}. No input. */
	public static enum Fields implements CcpJsonFieldName {
		;
	}

	/** The single instance. */
	public static final VisBusinessResumeTotalsByRegion INSTANCE = new VisBusinessResumeTotalsByRegion();

	private VisBusinessResumeTotalsByRegion() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	/**
	 * Reads the groups of counters by ddd of every source.
	 * @param json not used
	 * @return under the name of each source, {ddd: total}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		VisResumeStatisticsSources[] sources = VisResumeStatisticsSources.values();
		List<CcpJsonRepresentation> groupKeys = new ArrayList<>();
		for (VisResumeStatisticsSources source : sources) {
			CcpJsonRepresentation groupKey = source.getTotalsGroupKey();
			groupKeys.add(groupKey);
		}
		List<List<String>> childrenOfEachSource = JnGroupers.getChildrenOfEachGroup(groupKeys);
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON;
		for (VisResumeStatisticsSources source : sources) {
			int sourceIndex = source.ordinal();
			List<String> children = childrenOfEachSource.get(sourceIndex);
			CcpJsonRepresentation totals = getTotals(children);
			response = response.put(source, totals);
		}
		return response;
	}

	/**
	 * The counters of a group.
	 * @param children the only child of the group, the JSON text of the counters; empty when the group does not exist
	 * @return {ddd: total}, empty when there is no group
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
