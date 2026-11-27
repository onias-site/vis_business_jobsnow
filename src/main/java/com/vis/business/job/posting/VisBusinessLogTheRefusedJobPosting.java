package com.vis.business.job.posting;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.vis.entities.VisEntityInstantMessagePositionReply;
import com.vis.entities.VisEntityJobPosting;

/**
 * Handler of the exceptions that refuse a job posting before its save (since 2026-11-27):
 * {@link VisErrorJobPostingWithoutSkills} and {@link VisErrorJobPostingDescriptionNotSaved}. It only logs, in one line of
 * the output of the consumer, the id of the job posting with what the businesses before the refusal found (no
 * {@code requestedSkills}: no skill; no {@code filterId}: no resume with its skills; with {@code filterId}: the filters
 * already had a text), for a look some day if needed. The handled exception cancels the save.
 */
public class VisBusinessLogTheRefusedJobPosting implements CcpBusiness {

	/** The single instance: the class keeps no state. */
	public static final VisBusinessLogTheRefusedJobPosting INSTANCE = new VisBusinessLogTheRefusedJobPosting();

	private VisBusinessLogTheRefusedJobPosting() {}

	/** The texts of this class. */
	static enum Constants implements CcpJsonFieldName {
		/** The start of the line of the log. */
		PREFIX { public String getValue() { return "JOB POSTING REFUSED: "; } },
	}

	/**
	 * Logs the refused job posting.
	 * @param json the job posting, with what the businesses before the refusal found
	 * @return the job posting as it came
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		// the id of the record: postId in vis_job_posting, chatId (who posted it in the group) in vis_instant_message_position_reply
		CcpJsonRepresentation whatWasFound = json.getJsonPiece(VisEntityJobPosting.Fields.postId, VisEntityInstantMessagePositionReply.Fields.chatId, VisBusinessFindTheSkillsOfTheJobPosting.Fields.requestedSkills,
				VisBusinessFindTheMostCoveredSkills.Fields.possibleSkills, VisBusinessSaveTheResumeFilterOfTheJobPosting.Fields.filterId);
		String prefix = Constants.PREFIX.getValue();
		String line = whatWasFound.asUgglyJson();
		System.out.println(prefix + line);
		return json;
	}
}
