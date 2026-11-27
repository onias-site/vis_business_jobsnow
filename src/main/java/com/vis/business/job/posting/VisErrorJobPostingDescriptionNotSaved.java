package com.vis.business.job.posting;

import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityJobPosting;

/**
 * The text of a job posting did not become a new record of {@code vis_resume_filter_description} (since 2026-11-27): the
 * filters of its resumes already had a text shared under the e-mail of the job postings, or no resume has any of its
 * skills and there are no filters. Thrown by {@link VisBusinessSaveTheDescriptionOfTheJobPosting} and handled in
 * {@code VisEntityJobPosting} by {@link VisBusinessLogTheRefusedJobPosting}, which only logs it; the handled exception
 * cancels the save of the job posting and the businesses after it.
 */
@SuppressWarnings("serial")
public class VisErrorJobPostingDescriptionNotSaved extends RuntimeException {
	/**
	 * Builds the error with the job posting.
	 * @param json the job posting
	 */
	VisErrorJobPostingDescriptionNotSaved(CcpJsonRepresentation json) {
		super("The text of the job posting was not saved as a new description of the filters of its resumes: " + json.getJsonPiece(VisEntityJobPosting.Fields.postId));
	}
}
