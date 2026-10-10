package com.vis.business.skill;

import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.entities.VisEntitySkillPending;
import com.vis.messages.VisMessages;

/**
 * The kinds of pending request a user can have with the support, each one with the entity where it waits and the
 * template of the notice that opened the operator's ticket. Ignoring a user discards every one of them
 * ({@link VisBusinessDiscardPendingRequestsOfIgnoredUser}).
 */
public enum VisIgnoredUserPendingRequests {

	/** Skill suggestions waiting for review. */
	skillSuggestion {
		public CcpEntity getPendingEntity() {
			return VisEntitySkillPending.ENTITY;
		}

		public Class<?> getNoticeTemplate() {
			return VisMessages.VisNotifySupportAndUserAboutPendingSkillRequest.class;
		}
	},
	/** Skill hierarchy fix requests waiting for review. */
	skillFixHierarchy {
		public CcpEntity getPendingEntity() {
			return VisEntitySkillFixHierarchyPending.ENTITY;
		}

		public Class<?> getNoticeTemplate() {
			return VisMessages.VisNotifySupportAndUserAboutPendingSkillHierarchyRequest.class;
		}
	},
	;

	/**
	 * The entity where the requests of this kind wait for review; its {@code email} field holds the user.
	 * @return the entity
	 */
	public abstract CcpEntity getPendingEntity();

	/**
	 * The template of the notice that sent the operator the ticket of each request of this kind.
	 * @return the template class
	 */
	public abstract Class<?> getNoticeTemplate();
}
