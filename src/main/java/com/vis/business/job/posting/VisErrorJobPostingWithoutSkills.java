package com.vis.business.job.posting;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityJobPosting;

/**
 * The text of a job posting has no skill of the dictionary (since 2026-11-27): thrown by
 * {@link VisBusinessFindTheSkillsOfTheJobPosting} and handled in {@code VisEntityJobPosting} by
 * {@link VisBusinessLogTheRefusedJobPosting}, which only logs it; the handled exception cancels the save of the job posting.
 */
@SuppressWarnings("serial")
public class VisErrorJobPostingWithoutSkills extends RuntimeException {
	/**
	 * Builds the error with the job posting.
	 * @param json the job posting
	 */
	VisErrorJobPostingWithoutSkills(CcpJsonRepresentation json) {
		super("The text of the job posting has no skill: " + json.getJsonPiece(VisEntityJobPosting.Fields.postId));
	}
}
