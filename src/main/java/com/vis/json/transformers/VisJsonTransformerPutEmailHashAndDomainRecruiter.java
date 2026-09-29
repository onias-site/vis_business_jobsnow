package com.vis.json.transformers;

import com.ccp.decorators.CcpHashDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.hash.CcpHashAlgorithm;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.business.CcpBusiness;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Field transformer applied to the recruiter's e-mail while the VisEntityPosition entity is persisted.
 * Takes the original e-mail, calculates its SHA1 hash, extracts the domain (the part before the @) and enriches
 * the JSON with the hash (replacing recruiter), the original e-mail (in originalRecruiter) and the domain (in domain).
 * Keeps the recruiter anonymous in the system while preserving traceability by domain.
 */
public class VisJsonTransformerPutEmailHashAndDomainRecruiter implements CcpBusiness {
	enum JsonFieldNames implements CcpJsonFieldName{
		originalRecruiter 
	}

	public final static VisJsonTransformerPutEmailHashAndDomainRecruiter INSTANCE = new VisJsonTransformerPutEmailHashAndDomainRecruiter();

	private VisJsonTransformerPutEmailHashAndDomainRecruiter() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		String recruiter = json.getAsString(VisJsonCommonsFields.recruiter);
		
		String[] emailParts = recruiter.split("@");
		
		String domain =  emailParts[0];
		CcpStringDecorator recruiterDecorator = new CcpStringDecorator(recruiter);

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
