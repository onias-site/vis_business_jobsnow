package com.vis.json.transformers;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.especifications.db.utils.entity.fields.CcpJsonTransformersDefaultEntityField;
import com.vis.entities.VisEntityRecruiter;

/**
 * Field transformer applied to the {@code domain} of the VisEntityRecruiter entity while it is persisted: the domain is
 * the whole text of the {@code email} after the {@code @} ({@code maria@empresa.com.br} has the domain
 * {@code empresa.com.br}). A json with no e-mail is returned as is.
 */
public class VisJsonTransformerRecruiterDomain implements CcpJsonTransformersDefaultEntityField {
	/** The single instance: the class keeps no state. */
	public final static VisJsonTransformerRecruiterDomain INSTANCE = new VisJsonTransformerRecruiterDomain();

	private VisJsonTransformerRecruiterDomain() {}

	/**
	 * Puts in {@code domain} the part of the e-mail after the {@code @}.
	 * @param json the recruiter
	 * @return the recruiter with the domain
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		boolean noEmail = false == json.containsAllFields(VisEntityRecruiter.Fields.email);

		if(noEmail) {
			return json;
		}

		String email = json.getAsString(VisEntityRecruiter.Fields.email);
		CcpStringDecorator emailText = new CcpStringDecorator(email);
		CcpEmailDecorator emailDecorator = emailText.email();
		String domain = emailDecorator.getDomain();
		CcpJsonRepresentation jsonWithDomain = json.put(VisEntityRecruiter.Fields.domain, domain);
		return jsonWithDomain;
	}

	/**
	 * Tells that the result can be part of a primary key: it depends only on the e-mail.
	 * @return {@code true}
	 */
	public boolean canBePrimaryKey() {
		return true;
	}

	/**
	 * Returns the name of the transformed field.
	 * @return {@code domain}
	 */
	public String name() {
		String fieldName = VisEntityRecruiter.Fields.domain.name();
		return fieldName;
	}
}
