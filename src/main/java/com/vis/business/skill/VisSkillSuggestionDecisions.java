package com.vis.business.skill;

import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntitySkillApproved;
import com.vis.entities.VisEntitySkillRejected;
import com.vis.entities.VisEntitySkillReviewed;

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

	/**
	 * Records this decision in {@link VisEntitySkillReviewed}, so that every candidate who suggests the same skill
	 * afterwards sees it, instead of the suggestion reaching the operator again.
	 * @param reviewedSuggestion the suggestion with the operator's {@code explanation}
	 */
	public void shareWithEveryone(CcpJsonRepresentation reviewedSuggestion) {
		String skill = reviewedSuggestion.getAsString(VisEntitySkillReviewed.Fields.skill);
		List<String> synonyms = reviewedSuggestion.getAsStringList(VisEntitySkillReviewed.Fields.synonym);
		String explanation = reviewedSuggestion.getAsString(VisEntitySkillReviewed.Fields.explanation);

		CcpJsonRepresentation reviewWithSkill = CcpOtherConstants.EMPTY_JSON.put(VisEntitySkillReviewed.Fields.skill, skill);
		CcpJsonRepresentation reviewWithSynonyms = reviewWithSkill.put(VisEntitySkillReviewed.Fields.synonym, synonyms);
		CcpJsonRepresentation reviewWithExplanation = reviewWithSynonyms.put(VisEntitySkillReviewed.Fields.explanation, explanation);
		String status = this.name();
		CcpJsonRepresentation review = reviewWithExplanation.put(VisEntitySkillReviewed.Fields.status, status);
		VisEntitySkillReviewed.ENTITY.save(review);
	}
}
