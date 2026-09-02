package com.vis.utils;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonFieldName;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.mensageria.JnFunctionMensageriaSender;
import com.vis.schedulling.VisBusinessGroupResumeViewsByRecruiter;
import com.vis.schedulling.VisBusinessGroupResumeViewsByResume;
import com.vis.schedulling.VisBusinessGroupResumesOpinionsByRecruiter;
import com.vis.schedulling.VisBusinessGroupResumesOpinionsByResume;
import java.util.stream.Stream;

/**
 * Consumidor de lista de registros de sessão recentes que extrai os e-mails dos usuários e os envia para os
 * quatro processos de agrupamento assíncronos (opiniões por recrutador, opiniões por currículo, visualizações
 * por recrutador, visualizações por currículo), disparando as mensageiras correspondentes.
 */
public class VisSendRecentUsersToGroupings implements Consumer<List<CcpJsonRepresentation>> {
	enum JsonFieldNames implements CcpJsonFieldName{
		masters
	}
	
	private VisSendRecentUsersToGroupings() {}
	
	public final static VisSendRecentUsersToGroupings INSTANCE = new VisSendRecentUsersToGroupings();

	public void accept(List<CcpJsonRepresentation> records) {
		Stream<CcpJsonRepresentation> stream = records.stream();
		var streamMap = stream
		.map(rec ->	rec.getAsString(JnJsonCommonsFields.id));
		var streamMapMap = streamMap
		.map(id -> new CcpJsonRepresentation(id));
		var streamMapMapMap = streamMapMap
		.map(json -> json.getAsString(JnJsonCommonsFields.email));
		List<String> emails = streamMapMapMap
		.collect(Collectors.toList());
		
		CcpJsonRepresentation message = CcpOtherConstants.EMPTY_JSON.put(JsonFieldNames.masters, emails);
		JnFunctionMensageriaSender jnFunctionMensageriaSender = new JnFunctionMensageriaSender(VisBusinessGroupResumesOpinionsByRecruiter.INSTANCE);

		jnFunctionMensageriaSender.sendToMensageria(message);
		VisBusinessGroupResumesOpinionsByResume.INSTANCE.sendToMensageria(message);
		VisBusinessGroupResumeViewsByRecruiter.INSTANCE.sendToMensageria(message);
		VisBusinessGroupResumeViewsByResume.INSTANCE.sendToMensageria(message);
	}

}
