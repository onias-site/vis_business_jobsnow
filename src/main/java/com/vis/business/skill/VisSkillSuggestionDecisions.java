package com.vis.business.skill;

import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntitySkillApproved;
import com.vis.entities.VisEntitySkillRejected;

/**
 * Decision taken by the support bot operator on a skill suggestion, each one with the entity the suggestion goes to.
 */
public enum VisSkillSuggestionDecisions {

	/** The skill was approved. */
	approved {
		public CcpEntity getTargetEntity() {
			return VisEntitySkillApproved.ENTITY;
		}
	},
	/** The skill was rejected. */
	rejected {
		public CcpEntity getTargetEntity() {
			return VisEntitySkillRejected.ENTITY;
		}
	}
	;

	/**
	 * The entity the suggestion goes to with this decision.
	 * @return the entity
	 */
	public abstract CcpEntity getTargetEntity();
}
