package com.vis.business.recruiter.resumes;

import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntityViewedResume;

/**
 * The moves of a view of the recruiter between the sides of {@code vis_viewed_resume}, chosen in the screen of the viewed
 * resumes (since 2026-11-15): "Negativar CV" takes the view to the twin ({@code vis_inactive_viewed_resume}), so the
 * resume no longer goes to the recruiter in the e-mail of resumes; "Positivar CV" brings it back.
 */
public enum VisViewedResumeMoves {
	/** "Negativar CV": from the main side to the twin. */
	unwanted {
		CcpEntity getSideLeft() {
			return VisEntityViewedResume.ENTITY;
		}

		CcpEntity getSideReached() {
			return VisEntityViewedResume.ENTITY.getTwinEntity();
		}
	},
	/** "Positivar CV": from the twin back to the main side. */
	wanted {
		CcpEntity getSideLeft() {
			return VisEntityViewedResume.ENTITY.getTwinEntity();
		}

		CcpEntity getSideReached() {
			return VisEntityViewedResume.ENTITY;
		}
	},
	;

	/**
	 * The side the view leaves: the delete on it moves the view to the other side.
	 * @return the side left
	 */
	abstract CcpEntity getSideLeft();

	/**
	 * The side the view goes to.
	 * @return the side reached
	 */
	abstract CcpEntity getSideReached();
}
