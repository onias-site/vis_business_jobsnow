package com.vis.json.transformers;

import com.ccp.decorators.CcpHashDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.utils.entity.fields.CcpJsonTransformersDefaultEntityField;
import com.ccp.hash.CcpHashAlgorithm;
import com.vis.entities.VisEntityViewedResume;

/**
 * Field transformer applied to the {@code linkedin} of the VisEntityViewedResume entity: the LinkedIn address is
 * replaced by its SHA-1 hash, so the record does not keep it in a readable form (LGPD). The original goes on, for the
 * decorators inside the transformers (the {@code after} side effects), in {@code originalLinkedin}, which is not a field
 * of the entity and so is never written. A
 * json with no LinkedIn is returned as is.
 */
public class VisJsonTransformerLinkedinHash implements CcpJsonTransformersDefaultEntityField {
	/** The single instance: the class keeps no state. */
	public final static VisJsonTransformerLinkedinHash INSTANCE = new VisJsonTransformerLinkedinHash();

	private VisJsonTransformerLinkedinHash() {}

	/** The field that carries the original address past the transformer. */
	public static enum JsonFieldNames implements CcpJsonFieldName {
		/** The LinkedIn address as it came, before the hash. */
		originalLinkedin,
	}

	/**
	 * Replaces the LinkedIn address with its SHA-1 hash, keeping the original in {@code originalLinkedin}.
	 * @param json the record
	 * @return the record with the hash in {@code linkedin}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		boolean noLinkedin = false == json.containsAllFields(VisEntityViewedResume.Fields.linkedin);

		if(noLinkedin) {
			return json;
		}

		String linkedin = json.getAsString(VisEntityViewedResume.Fields.linkedin);
		CcpStringDecorator linkedinText = new CcpStringDecorator(linkedin);
		CcpHashDecorator linkedinHash = linkedinText.hash();
		String hash = linkedinHash.asString(CcpHashAlgorithm.SHA1);
		CcpJsonRepresentation jsonWithOriginalLinkedin = json.put(JsonFieldNames.originalLinkedin, linkedin);
		CcpJsonRepresentation jsonWithHashedLinkedin = jsonWithOriginalLinkedin.put(VisEntityViewedResume.Fields.linkedin, hash);
		return jsonWithHashedLinkedin;
	}

	/**
	 * Tells that the result can be part of a primary key: the same address always gives the same hash.
	 * @return {@code true}
	 */
	public boolean canBePrimaryKey() {
		return true;
	}

	/**
	 * Returns the name of the transformed field.
	 * @return {@code linkedin}
	 */
	public String name() {
		String fieldName = VisEntityViewedResume.Fields.linkedin.name();
		return fieldName;
	}
}
