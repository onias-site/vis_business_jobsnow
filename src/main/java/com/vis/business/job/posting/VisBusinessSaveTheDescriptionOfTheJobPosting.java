package com.vis.business.job.posting;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityJobPosting;
import com.vis.entities.VisEntityResumeFilterDescription;

/**
 * Fifth business before the save of {@code vis_job_posting} (since 2026-11-27): the text of the job posting is shared with
 * the filters of its resumes, as the screen {@code curriculos/buscar} does with "Copiar este link para enviar para
 * alguém", in {@code vis_resume_filter_description} under the id of the filters and the e-mail of the job postings. The
 * flow goes on only when the save inserts the record: when the filters already had a text (the save updates it) or
 * there are no filters (no resume has any skill of the text), it throws {@link VisErrorJobPostingDescriptionNotSaved},
 * which cancels the save of the job posting and the e-mail to its recruiters.
 */
public class VisBusinessSaveTheDescriptionOfTheJobPosting implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public static final VisBusinessSaveTheDescriptionOfTheJobPosting INSTANCE = new VisBusinessSaveTheDescriptionOfTheJobPosting();

	private VisBusinessSaveTheDescriptionOfTheJobPosting() {}

	/**
	 * Saves the text under the filters of its resumes.
	 * @param json the job posting, with {@code filterId} when there are filters
	 * @return the job posting as it came
	 * @throws VisErrorJobPostingDescriptionNotSaved when the record was not inserted
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		boolean noFilters = false == json.containsAllFields(VisBusinessSaveTheResumeFilterOfTheJobPosting.Fields.filterId);

		if(noFilters) {
			throw new VisErrorJobPostingDescriptionNotSaved(json);
		}

		String filterId = json.getAsString(VisBusinessSaveTheResumeFilterOfTheJobPosting.Fields.filterId);
		String description = json.getAsString(VisEntityJobPosting.Fields.description);
		String sharingEmail = VisBusinessSaveTheResumeFilterOfTheJobPosting.Constants.SHARING_EMAIL.getValue();
		CcpJsonRepresentation sharedDescription = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityResumeFilterDescription.Fields.filterId, filterId)
				.put(VisEntityResumeFilterDescription.Fields.email, sharingEmail)
				.put(VisEntityResumeFilterDescription.Fields.description, description);
		boolean inserted = VisEntityResumeFilterDescription.ENTITY.save(sharedDescription);

		if(inserted) {
			return json;
		}

		throw new VisErrorJobPostingDescriptionNotSaved(json);
	}
}
