package com.vis.json.transformers;

import com.ccp.decorators.CcpHashDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.hash.CcpHashAlgorithm;
import com.ccp.decorators.CcpStringDecorator;
import com.ccp.business.CcpBusiness;
import com.vis.entities.VisEntityDeniedViewToCompany;
import com.vis.entities.VisEntityResumePerception;

/**
 * Transformador de campo aplicado ao e-mail do recrutador durante a persistência da entidade VisEntityPosition.
 * Extrai o e-mail original, calcula seu hash SHA1, extrai o domínio (parte antes do @) e enriquece o JSON
 * com o hash (substituindo recruiter), o e-mail original (em originalRecruiter) e o domínio (em domain).
 * Garante o anonimato do recrutador no sistema enquanto mantém a rastreabilidade por domínio.
 */
public class VisJsonTransformerPutEmailHashAndDomainRecruiter implements CcpBusiness {
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
