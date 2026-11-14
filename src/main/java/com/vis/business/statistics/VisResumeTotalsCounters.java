package com.vis.business.statistics;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntityVirtualDddCounter;
import com.vis.entities.VisEntityVirtualInsertionDayCounter;
import com.vis.entities.VisEntityVirtualInsertionMonthCounter;

/**
 * The counters of {@code jn_grouper} read by the screen Supply X Demand when it opens, one group per source of
 * {@link VisResumeStatisticsSources}: each item is a parent whose only child is the JSON text {@code {"<key>": total}}.
 */
public enum VisResumeTotalsCounters implements CcpJsonFieldName {
	/** How many active records each ddd has ({@code vis_virtual_ddd_counter}): the titles of the folders. */
	byRegion {
		public CcpEntity getParentEntity() {
			return VisEntityVirtualDddCounter.ENTITY;
		}

		public CcpJsonRepresentation getCombination() {
			CcpJsonRepresentation combination = VisEntityVirtualDddCounter.getCombination();
			return combination;
		}
	},
	/** How many active records were inserted each day, {@code yyyyMMdd} ({@code vis_virtual_insertion_day_counter}). */
	byDay {
		public CcpEntity getParentEntity() {
			return VisEntityVirtualInsertionDayCounter.ENTITY;
		}

		public CcpJsonRepresentation getCombination() {
			CcpJsonRepresentation combination = VisEntityVirtualInsertionDayCounter.getCombination();
			return combination;
		}
	},
	/** How many active records were inserted each month, {@code yyyyMM} ({@code vis_virtual_insertion_month_counter}). */
	byMonth {
		public CcpEntity getParentEntity() {
			return VisEntityVirtualInsertionMonthCounter.ENTITY;
		}

		public CcpJsonRepresentation getCombination() {
			CcpJsonRepresentation combination = VisEntityVirtualInsertionMonthCounter.getCombination();
			return combination;
		}
	},
	;

	/**
	 * The virtual entity of the parent of the groups of this counter.
	 * @return the entity
	 */
	public abstract CcpEntity getParentEntity();

	/**
	 * The values the parent id comes from.
	 * @return the combination
	 */
	public abstract CcpJsonRepresentation getCombination();
}
