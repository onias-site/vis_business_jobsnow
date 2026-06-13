package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.query.CcpQueryExecutor;
import com.ccp.business.CcpBusiness;
import com.jn.entities.JnEntityDisposableRecord;
import com.jn.entities.JnEntityLoginSessionValidation;
import com.vis.utils.VisFrequencyOptions;
import com.vis.utils.VisSendRecentUsersToGroupings;

/**
 * Tarefa agendada (cron) que busca todos os usuários que fizeram login no último ano e os envia para os
 * processos de agrupamento de visualizações de currículos e percepções. Consome o índice
 * JnEntityDisposableRecord filtrando registros de sessão com timestamp dentro do período anual.
 */
public class VisBusinessGetRecentLoggedUsers implements CcpBusiness{
	//TODO JSON VALIDATIONS	

	private VisBusinessGetRecentLoggedUsers() {}
	
	public static final VisBusinessGetRecentLoggedUsers INSTANCE = new VisBusinessGetRecentLoggedUsers();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		
		String entityName = JnEntityLoginSessionValidation.ENTITY.getEntityMetaData().entityName;
		CcpQueryOptions queryToSearchLastUpdated = 
				CcpQueryOptions.INSTANCE
					.startQuery()
						.startBool()
							.startMust()
								.startRange()
									.startFieldRange(JnEntityDisposableRecord.Fields.timestamp.name())
										.greaterThan(System.currentTimeMillis() - VisFrequencyOptions.yearly.hours * 3_600_000)
									.endFieldRangeAndBackToRange()
								.endRangeAndBackToMust()	
								.term(JnEntityDisposableRecord.Fields.entity, entityName)
							.endMustAndBackToBool()
						.endBoolAndBackToQuery()
					.endQueryAndBackToRequest()
					.maxResults()
					.addDescSorting(JnEntityDisposableRecord.Fields.timestamp.name())
				;
		String[] resourcesNames = JnEntityDisposableRecord.ENTITY.getEntityMetaData().getEntitiesToSelect();

		queryExecutor.consumeQueryResult(queryToSearchLastUpdated, resourcesNames, "10m", 10000L, VisSendRecentUsersToGroupings.INSTANCE, JnEntityDisposableRecord.Fields.id.name());
		
		return json;
	}

}
