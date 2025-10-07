package com.vis.json.transformers;

import java.util.function.Function;

import com.ccp.decorators.CcpHashDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.utils.CcpHashAlgorithm;
import com.vis.entities.VisEntityDeniedViewToCompany;
import com.vis.entities.VisEntityResumePerception;
public class VisJsonTransformerPutEmailHashAndDomainRecruiter implements Function<CcpJsonRepresentation, CcpJsonRepresentation> {
	enum JsonFieldNames implements CcpJsonFieldName{
		originalRecruiter
	}

	public final static VisJsonTransformerPutEmailHashAndDomainRecruiter INSTANCE = new VisJsonTransformerPutEmailHashAndDomainRecruiter();

	private VisJsonTransformerPutEmailHashAndDomainRecruiter() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		String recruiter = json.getAsString(VisEntityResumePerception.Fields.recruiter);
		
		String[] split = recruiter.split("@");
		
		String domain =  split[0];
		
		CcpHashDecorator hash2 = new CcpStringDecorator(recruiter).hash();
		
		String hash = hash2.asString(CcpHashAlgorithm.SHA1);
		//LATER NONPROFESSIONAL DOMAINS
		CcpJsonRepresentation put = json
				.put(JsonFieldNames.originalRecruiter, recruiter)
				.put(VisEntityResumePerception.Fields.recruiter, hash)
				.put(VisEntityDeniedViewToCompany.Fields.domain, domain)
				;
		return put;
	}
}
