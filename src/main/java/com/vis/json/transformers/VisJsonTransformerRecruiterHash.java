package com.vis.json.transformers;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpHashDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.utils.entity.fields.CcpJsonTransformersDefaultEntityField;
import com.ccp.hash.CcpHashAlgorithm;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Field transformer applied to the {@code recruiter} of the VisEntityPosition entity: the e-mail of the recruiter who
 * owns the position is replaced by its SHA-1 hash, so the record does not keep it in a readable form (LGPD). The
 * original goes on, for the decorators inside the transformers, in {@code originalRecruiter}, which is not a field of
 * the entity and so is never written. A json with no recruiter, or whose recruiter is no longer an e-mail (the hash
 * already calculated), is returned as is.
 */
public class VisJsonTransformerRecruiterHash implements CcpJsonTransformersDefaultEntityField {
	/** The single instance: the class keeps no state. */
	public final static VisJsonTransformerRecruiterHash INSTANCE = new VisJsonTransformerRecruiterHash();

	private VisJsonTransformerRecruiterHash() {}

	/** The field that carries the original e-mail past the transformer. */
	public static enum JsonFieldNames implements CcpJsonFieldName {
		/** The e-mail of the recruiter as it came, before the hash. */
		originalRecruiter,
	}

	/**
	 * Replaces the e-mail of the recruiter with its SHA-1 hash, keeping the original in {@code originalRecruiter}.
	 * @param json the position
	 * @return the position with the hash in {@code recruiter}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		boolean noRecruiter = false == json.containsAllFields(VisJsonCommonsFields.recruiter);

		if(noRecruiter) {
			return json;
		}

		String recruiter = json.getAsString(VisJsonCommonsFields.recruiter);
		CcpStringDecorator recruiterText = new CcpStringDecorator(recruiter);
		CcpEmailDecorator recruiterEmail = recruiterText.email();
		boolean alreadyHashed = false == recruiterEmail.isValid();

		if(alreadyHashed) {
			return json;
		}

		CcpHashDecorator recruiterHash = recruiterEmail.hash();
		String hash = recruiterHash.asString(CcpHashAlgorithm.SHA1);
		CcpJsonRepresentation jsonWithOriginalRecruiter = json.put(JsonFieldNames.originalRecruiter, recruiter);
		CcpJsonRepresentation jsonWithHashedRecruiter = jsonWithOriginalRecruiter.put(VisJsonCommonsFields.recruiter, hash);
		return jsonWithHashedRecruiter;
	}

	/**
	 * Tells that the result can be part of a primary key: the same e-mail always gives the same hash.
	 * @return {@code true}
	 */
	public boolean canBePrimaryKey() {
		return true;
	}

	/**
	 * Returns the name of the transformed field.
	 * @return {@code recruiter}
	 */
	public String name() {
		String fieldName = VisJsonCommonsFields.recruiter.name();
		return fieldName;
	}
}
