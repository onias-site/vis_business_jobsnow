package com.vis.business.recruiter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.jn.entities.decorators.engine.JnEntityGrouperParents;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityRecruiter;
import com.vis.entities.VisEntityVirtualRecruiterDomainInitialsGrouper;

/**
 * The groups of the initials of the domain ({@link VisEntityVirtualRecruiterDomainInitialsGrouper}) of recruiters written
 * by bulk, which the decorator of {@code @CcpEntityGrouperChild} of {@link VisEntityRecruiter} does not see: the seed
 * creates the groups whole, the later loads add their recruiters to the groups already there.
 */
public class VisRecruiterDomainGroups {

	/** The ids of the recruiters of each parent, in the order the recruiters came. */
	private final Map<String, List<String>> childrenByParentId;

	/** The values each parent id came from. */
	private final Map<String, CcpJsonRepresentation> parentJsonByParentId;

	/**
	 * Calculates the groups of the recruiters.
	 * @param recruiters the recruiters, their fields already transformed ({@code domain} and {@code professional})
	 */
	public VisRecruiterDomainGroups(Collection<CcpJsonRepresentation> recruiters) {
		JnEntityGrouperParents parents = new JnEntityGrouperParents(VisEntityVirtualRecruiterDomainInitialsGrouper.class, VisEntityRecruiter.class);
		Map<String, List<String>> children = new LinkedHashMap<>();
		Map<String, CcpJsonRepresentation> parentJsons = new LinkedHashMap<>();
		for (CcpJsonRepresentation recruiter : recruiters) {
			String recruiterId = VisEntityRecruiter.ENTITY.calculateId(recruiter);
			Map<String, CcpJsonRepresentation> recruiterParents = parents.getParents(recruiter);
			parentJsons.putAll(recruiterParents);
			Set<String> parentIds = recruiterParents.keySet();
			for (String parentId : parentIds) {
				List<String> childrenOfTheParent = children.computeIfAbsent(parentId, key -> new ArrayList<>());
				boolean alreadyAChild = childrenOfTheParent.contains(recruiterId);

				if(alreadyAChild) {
					continue;
				}

				childrenOfTheParent.add(recruiterId);
			}
		}
		this.childrenByParentId = Collections.unmodifiableMap(children);
		this.parentJsonByParentId = Collections.unmodifiableMap(parentJsons);
	}

	/**
	 * The bulk items that create the groups whole, on the main side of the grouper, replacing any group already there:
	 * for the seed, which has every recruiter.
	 * @return one bulk item per group
	 */
	public List<CcpBulkItem> getItemsToCreateGroups() {
		String parentName = getParentName();
		String childName = getChildName();
		List<CcpBulkItem> items = JnGroupers.getItemsToCreateGroups(parentName, childName, this.parentJsonByParentId, this.childrenByParentId);
		return items;
	}

	/** Adds the recruiters to their groups on the main side of the grouper, keeping the children already there. */
	public void addToTheGroups() {
		String parentName = getParentName();
		String childName = getChildName();
		CcpEntity grouper = JnGroupers.getGrouper(false);
		JnGroupers.addChildren(grouper, parentName, childName, this.childrenByParentId, this.parentJsonByParentId);
	}

	/**
	 * The kind of group.
	 * @return the name of the virtual entity of the initials of the domain
	 */
	private static String getParentName() {
		CcpEntityMetaData metaData = VisEntityVirtualRecruiterDomainInitialsGrouper.ENTITY.getEntityMetaData();
		String parentName = metaData.entityName;
		return parentName;
	}

	/**
	 * The entity of the children.
	 * @return the name of the recruiters entity
	 */
	private static String getChildName() {
		CcpEntityMetaData metaData = VisEntityRecruiter.ENTITY.getEntityMetaData();
		String childName = metaData.entityName;
		return childName;
	}
}
