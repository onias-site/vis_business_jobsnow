package com.vis.json.transformers;

import com.ccp.decorators.CcpEmailDecorator;
import com.ccp.decorators.CcpHashDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.hash.CcpHashAlgorithm;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.business.CcpBusiness;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Field transformer applied to the recruiter's e-mail while the VisEntityPosition entity is persisted.
 * Takes the {@code email}, calculates its SHA1 hash, extracts the domain (the part after the @) and enriches
 * the JSON with the hash (in recruiter), the original e-mail (in originalRecruiter) and the domain (in domain):
 * {@code onias@jobsnow.com} has the domain {@code jobsnow.com}. A json with no e-mail is returned as is.
 * Keeps the recruiter anonymous in the system while preserving traceability by domain.
 */
public class VisJsonTransformerPutEmailHashAndDomainRecruiter implements CcpBusiness {
	/** Fields written by the transformer. */
	enum JsonFieldNames implements CcpJsonFieldName{
		/** The {@code originalRecruiter} field. */
		originalRecruiter
	}

	/** The single instance. */
	public final static VisJsonTransformerPutEmailHashAndDomainRecruiter INSTANCE = new VisJsonTransformerPutEmailHashAndDomainRecruiter();

	/** Singleton; use {@link #INSTANCE}. */
	private VisJsonTransformerPutEmailHashAndDomainRecruiter() {}

	/**
	 * Puts in {@code recruiter} the SHA-1 hash of the {@code email}, keeps the e-mail in {@code originalRecruiter}
	 * and puts in {@code domain} the part of the e-mail after the {@code @}.
	 * @param json the position
	 * @return the position with the hash, the original e-mail and the domain
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		boolean noEmail = false == json.containsAllFields(VisJsonCommonsFields.email);

		if(noEmail) {
			return json;
		}

		String recruiter = json.getAsString(VisJsonCommonsFields.email);
		CcpStringDecorator recruiterDecorator = new CcpStringDecorator(recruiter);
		CcpEmailDecorator recruiterEmail = recruiterDecorator.email();
		String domain = recruiterEmail.getDomain();

		CcpHashDecorator recruiterHashDecorator = recruiterDecorator.hash();

		String hash = recruiterHashDecorator.asString(CcpHashAlgorithm.SHA1);
		CcpJsonRepresentation jsonWithOriginalRecruiter = json
				.put(JsonFieldNames.originalRecruiter, recruiter);
		CcpJsonRepresentation jsonWithHashedRecruiter = jsonWithOriginalRecruiter
				.put(VisJsonCommonsFields.recruiter, hash);
		CcpJsonRepresentation jsonWithDomain = jsonWithHashedRecruiter
				.put(VisJsonCommonsFields.domain, domain)
				;
		return jsonWithDomain;
	}
}
