package com.vis.json.transformers;

import com.ccp.decorators.CcpHashDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.utils.entity.fields.CcpJsonTransformersDefaultEntityField;
import com.ccp.hash.CcpHashAlgorithm;
import com.vis.entities.VisEntityJobPosting;

/**
 * Field transformer of the {@code postId} of {@code vis_job_posting} (since 2026-11-26), the primary key of the entity:
 * the SHA-1 of the text of the job posting ({@code description}) without the spaces around it and in upper case, so the
 * same text typed again (with other spaces around or other capitals) is the same record. A record with no text (a read
 * by the id) is returned as is.
 */
public class VisJsonTransformerJobPostingId implements CcpJsonTransformersDefaultEntityField {
	/** The single instance: the class keeps no state. */
	public final static VisJsonTransformerJobPostingId INSTANCE = new VisJsonTransformerJobPostingId();

	private VisJsonTransformerJobPostingId() {}

	/**
	 * Puts in the record the id of its text.
	 * @param record the record of {@code vis_job_posting}
	 * @return the record with the id of the text, or as is when it has no text
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation record) {
		boolean noText = false == record.containsAllFields(VisEntityJobPosting.Fields.description);

		if(noText) {
			return record;
		}

		String description = record.getAsString(VisEntityJobPosting.Fields.description);
		String trimmedDescription = description.trim();
		String upperCaseDescription = trimmedDescription.toUpperCase();
		CcpStringDecorator descriptionDecorator = new CcpStringDecorator(upperCaseDescription);
		CcpHashDecorator hashDecorator = descriptionDecorator.hash();
		String postId = hashDecorator.asString(CcpHashAlgorithm.SHA1);
		CcpJsonRepresentation recordWithTheId = record.put(VisEntityJobPosting.Fields.postId, postId);
		return recordWithTheId;
	}

	/**
	 * Tells that the result can be part of a primary key: the same text always gives the same hash.
	 * @return {@code true}
	 */
	public boolean canBePrimaryKey() {
		return true;
	}

	/**
	 * Returns the name of the transformed field.
	 * @return {@code postId}
	 */
	public String name() {
		String fieldName = VisEntityJobPosting.Fields.postId.name();
		return fieldName;
	}
}
