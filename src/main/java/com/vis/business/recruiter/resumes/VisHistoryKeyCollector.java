package com.vis.business.recruiter.resumes;

import java.util.Set;
import java.util.function.Consumer;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;

/**
 * Consumer of the history rows of {@code VisBusinessSendResumesToRecruiters}: keeps the primary key of the record of each
 * row.
 */
class VisHistoryKeyCollector implements Consumer<CcpJsonRepresentation> {

	/** Receives the keys. */
	private final Set<String> keys;

	/** The primary key field of the records. */
	private final CcpJsonFieldName keyField;

	/**
	 * Keeps where the keys go.
	 * @param keys receives the keys
	 * @param keyField the primary key field of the records
	 */
	VisHistoryKeyCollector(Set<String> keys, CcpJsonFieldName keyField) {
		this.keys = keys;
		this.keyField = keyField;
	}

	/**
	 * Keeps the key of the record of the row.
	 * @param historyRow the history row
	 */
	public void accept(CcpJsonRepresentation historyRow) {
		CcpJsonRepresentation record = VisBusinessSendResumesToRecruiters.getRecordOfTheHistoryRow(historyRow);
		String key = record.getAsString(this.keyField);
		this.keys.add(key);
	}
}
