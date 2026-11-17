package com.vis.business.recruiter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityVirtualRecruiterSourceCounter;

/**
 * The counter of the recruiters by source ({@link VisEntityVirtualRecruiterSourceCounter}) for recruiters written by bulk,
 * which the decorator of {@code @CcpEntityGrouperChildCounter} of {@link VisEntityRecruiter} does not see: the seed
 * creates the group whole, the later loads add their recruiters to the counters already there.
 */
public class VisRecruiterSourceCounts {

	/** How many recruiters of each source, as the child of the group takes them. */
	private final CcpJsonRepresentation counters;

	/**
	 * Counts the recruiters by source.
	 * @param recruiters the recruiters, with their {@code source}
	 */
	public VisRecruiterSourceCounts(Collection<CcpJsonRepresentation> recruiters) {
		// counted in a map of Long: the sums made inside the JSON come back as decimals ({"legacy":4.0})
		Map<String, Object> totalsBySource = new LinkedHashMap<>();
		for (CcpJsonRepresentation recruiter : recruiters) {
			String source = recruiter.getAsString(VisEntityRecruiter.Fields.source);
			Object previous = totalsBySource.getOrDefault(source, 0L);
			long total = (Long) previous + 1;
			totalsBySource.put(source, total);
		}
		this.counters = new CcpJsonRepresentation(totalsBySource);
	}

	/**
	 * The bulk item that creates the group whole, on the main side of the grouper, replacing the group already there: for
	 * the seed, which has every recruiter.
	 * @return one bulk item, none when there is no recruiter
	 */
	public List<CcpBulkItem> getItemsToCreateGroup() {
		boolean noRecruiter = this.counters.isEmpty();

		if(noRecruiter) {
			return List.of();
		}

		String parentName = getParentName();
		String childName = getChildName();
		String parentId = getParentId();
		CcpJsonRepresentation combination = VisEntityVirtualRecruiterSourceCounter.getCombination();
		String countersText = this.counters.asUgglyJson();
		Map<String, CcpJsonRepresentation> parents = Map.of(parentId, combination);
		Map<String, List<String>> children = Map.of(parentId, List.of(countersText));
		List<CcpBulkItem> items = JnGroupers.getItemsToCreateGroups(parentName, childName, parents, children);
		return items;
	}

	/** Adds the recruiters to the counters of their sources on the main side of the grouper. */
	public void addToTheCounter() {
		boolean noRecruiter = this.counters.isEmpty();

		if(noRecruiter) {
			return;
		}

		String parentName = getParentName();
		String childName = getChildName();
		String parentId = getParentId();
		CcpJsonRepresentation combination = VisEntityVirtualRecruiterSourceCounter.getCombination();
		Map<String, CcpJsonRepresentation> parents = Map.of(parentId, combination);
		Map<String, CcpJsonRepresentation> deltas = Map.of(parentId, this.counters);
		CcpEntity grouper = JnGroupers.getGrouper(false);
		JnGroupers.addToCounters(grouper, parentName, childName, deltas, parents);
	}

	private static String getParentId() {
		CcpJsonRepresentation combination = VisEntityVirtualRecruiterSourceCounter.getCombination();
		String parentId = VisEntityVirtualRecruiterSourceCounter.ENTITY.calculateId(combination);
		return parentId;
	}

	private static String getParentName() {
		CcpEntityMetaData metaData = VisEntityVirtualRecruiterSourceCounter.ENTITY.getEntityMetaData();
		String parentName = metaData.entityName;
		return parentName;
	}

	private static String getChildName() {
		CcpEntityMetaData metaData = VisEntityRecruiter.ENTITY.getEntityMetaData();
		String childName = metaData.entityName;
		return childName;
	}
}
