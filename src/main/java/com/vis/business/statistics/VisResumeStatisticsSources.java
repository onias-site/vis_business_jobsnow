package com.vis.business.statistics;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityViewedResume;
import com.vis.entities.VisEntityVirtualDddGrouper;
import com.vis.entities.VisEntityVirtualViewedResumeDddGrouper;

/**
 * Where the statistics of a region come from: each item is a group of {@code jn_grouper} of the ddd whose children carry
 * the same fields of the resume, so the same charts and averages are calculated over each one. The vacancies will be one
 * more item once their rules are defined.
 */
public enum VisResumeStatisticsSources implements CcpJsonFieldName {
	/** The active resumes of the ddd ({@code vis_virtual_ddd_grouper}). */
	resumes {
		public CcpJsonRepresentation getGroupKey(Integer ddd) {
			CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualDddGrouper.Fields.ddd, ddd);
			CcpJsonRepresentation groupKey = getGroupKey(VisEntityVirtualDddGrouper.ENTITY, VisEntityResume.ENTITY, combination);
			return groupKey;
		}

		public CcpEntity getChildEntity() {
			return VisEntityResume.ENTITY;
		}
	},
	/** The views of the resumes of the ddd by the recruiters, with the resume as it was at the view ({@code vis_virtual_viewed_resume_ddd_grouper}). */
	viewedResumes {
		public CcpJsonRepresentation getGroupKey(Integer ddd) {
			CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualViewedResumeDddGrouper.Fields.ddd, ddd);
			CcpJsonRepresentation groupKey = getGroupKey(VisEntityVirtualViewedResumeDddGrouper.ENTITY, VisEntityViewedResume.ENTITY, combination);
			return groupKey;
		}

		public CcpEntity getChildEntity() {
			return VisEntityViewedResume.ENTITY;
		}
	},
	;

	/**
	 * The key of the group of the ddd in {@code jn_grouper}.
	 * @param ddd the region
	 * @return the parent id, the parent name and the child name of the group
	 */
	public abstract CcpJsonRepresentation getGroupKey(Integer ddd);

	/**
	 * The entity whose records are the children of the groups of this source.
	 * @return the entity
	 */
	public abstract CcpEntity getChildEntity();

	/**
	 * The key of the group of a counter of this source in {@code jn_grouper}.
	 * @param counter the counter: by ddd, by day or by month of insertion
	 * @return the parent id, the parent name and the child name of the group
	 */
	public CcpJsonRepresentation getTotalsGroupKey(VisResumeTotalsCounters counter) {
		CcpEntity parentEntity = counter.getParentEntity();
		CcpEntity childEntity = this.getChildEntity();
		CcpJsonRepresentation combination = counter.getCombination();
		CcpJsonRepresentation groupKey = getGroupKey(parentEntity, childEntity, combination);
		return groupKey;
	}

	private static CcpJsonRepresentation getGroupKey(CcpEntity parentEntity, CcpEntity childEntity, CcpJsonRepresentation combination) {
		String parentId = parentEntity.calculateId(combination);
		CcpEntityMetaData parentDetails = parentEntity.getEntityMetaData();
		CcpEntityMetaData childDetails = childEntity.getEntityMetaData();
		CcpJsonRepresentation groupKey = JnGroupers.getGroupKey(parentDetails.entityName, childDetails.entityName, parentId);
		return groupKey;
	}
}
