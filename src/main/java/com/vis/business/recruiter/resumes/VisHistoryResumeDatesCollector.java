package com.vis.business.recruiter.resumes;

import java.util.Map;
import java.util.function.Consumer;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityResume;

/**
 * Consumer of the history rows of the resumes of {@code VisBusinessSendResumesToRecruiters}: keeps the oldest (added) and
 * the newest (last change) times of each resume, by the hash of its e-mail.
 */
class VisHistoryResumeDatesCollector implements Consumer<CcpJsonRepresentation> {

	/** Receives the oldest and the newest times of each resume. */
	private final Map<String, long[]> dates;

	/**
	 * Keeps where the dates go.
	 * @param dates receives the oldest and the newest times of each resume
	 */
	VisHistoryResumeDatesCollector(Map<String, long[]> dates) {
		this.dates = dates;
	}

	/**
	 * Updates the dates of the resume of the row.
	 * @param historyRow the history row
	 */
	public void accept(CcpJsonRepresentation historyRow) {
		CcpJsonRepresentation record = VisBusinessSendResumesToRecruiters.getRecordOfTheHistoryRow(historyRow);
		String emailHash = record.getAsString(VisEntityResume.Fields.email);
		Double timestampNumber = historyRow.getAsDoubleNumber(JnJsonCommonsFields.timestamp);
		long timestamp = timestampNumber.longValue();
		long[] oldestAndNewest = this.dates.computeIfAbsent(emailHash, x -> new long[] {Long.MAX_VALUE, Long.MIN_VALUE});
		oldestAndNewest[0] = Math.min(oldestAndNewest[0], timestamp);
		oldestAndNewest[1] = Math.max(oldestAndNewest[1], timestamp);
	}
}
