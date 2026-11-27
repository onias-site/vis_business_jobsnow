package com.vis.business.bots.job.posting;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityJobPosting;

/**
 * Second and last step of {@code /saveJobPosting} of the support bot (since 2026-11-26): the text typed by the operator,
 * without the spaces around it, from 100 to 5,000 characters, is saved in {@code vis_job_posting} with the millisecond of
 * the saving. The entity is asynchronous: the save only publishes the text to its queue, whose consumer finds the skills
 * of the text and the resumes that best answer it before writing the record; the step ends with the reply {@code saved}
 * at once. A text of another size is asked again ({@code invalidText}).
 */
public class VisSupportSaveJobPostingSaveTheText implements CcpBusiness {
	/** The single instance: the class keeps no state. */
	public final static VisSupportSaveJobPostingSaveTheText INSTANCE = new VisSupportSaveJobPostingSaveTheText();

	private VisSupportSaveJobPostingSaveTheText() {}

	/** The fewest characters of the text, as the entity. */
	private static final int MIN_LENGTH = 100;

	/** The most characters of the text, as the entity. */
	private static final int MAX_LENGTH = 5000;

	/**
	 * Saves the text.
	 * @param json the session, with {@code typedValue}
	 * @return the session with the reply {@code saved}
	 * @throws com.ccp.flow.CcpErrorFlowDisturb with {@code invalidText}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String typedValue = json.getAsString(JnJsonCommonsFields.typedValue);
		String description = typedValue.trim();
		int length = description.length();
		boolean invalidLength = length < MIN_LENGTH || length > MAX_LENGTH;

		if(invalidLength) {
			CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON.put(VisSupportSaveJobPostingConversation.Fields.length, length);
			CcpJsonRepresentation sessionAskingAgain = VisSupportSaveJobPostingConversation.reply(json, VisSupportSaveJobPostingMessages.invalidText, parameters);
			return VisSupportSaveJobPostingStatus.invalidText.throwException(sessionAskingAgain);
		}

		long timestamp = System.currentTimeMillis();
		CcpJsonRepresentation jobPosting = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityJobPosting.Fields.description, description)
				.put(VisEntityJobPosting.Fields.timestamp, timestamp);
		// the asynchronous writer publishes the record by its key: the postId goes calculated, as the search of resumes does
		CcpJsonRepresentation jobPostingWithTheId = VisEntityJobPosting.ENTITY.getJsonWithHandledPrimaryKey(jobPosting);
		VisEntityJobPosting.ENTITY.save(jobPostingWithTheId);
		CcpJsonRepresentation sessionWithReply = VisSupportSaveJobPostingConversation.reply(json, VisSupportSaveJobPostingMessages.saved, CcpOtherConstants.EMPTY_JSON);
		return sessionWithReply;
	}
}
