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
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.especifications.db.query.CcpQuery;

/**
 * Tarefa agendada (cron) que busca todos os usuários que fizeram login no último ano e os envia para os
 * processos de agrupamento de visualizações de currículos e percepções. Consome o índice
 * JnEntityDisposableRecord filtrando registros de sessão com timestamp dentro do período anual.
 */
public class VisBusinessGetRecentLoggedUsers implements CcpBusiness{
		

	private VisBusinessGetRecentLoggedUsers() {}
	
	public static final VisBusinessGetRecentLoggedUsers INSTANCE = new VisBusinessGetRecentLoggedUsers();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		CcpEntityMetaData entityMetaData = JnEntityLoginSessionValidation.ENTITY.getEntityMetaData();

		String entityName = entityMetaData.entityName;
		CcpQuery startQuery = CcpQueryOptions.INSTANCE
					.startQuery();
					var startBool = startQuery
						.startBool();
						var startMust = startBool
							.startMust();
							var startRange = startMust
								.startRange();
								String timestampName = JnJsonCommonsFields.timestamp.name();
								var startFieldRange = startRange
									.startFieldRange(timestampName);
									long currentTimeMillis = System.currentTimeMillis();
									double hoursVezes = VisFrequencyOptions.yearly.hours * 3_600_000;
									double currentTimeMillisMenos = currentTimeMillis - hoursVezes;
									var greaterThan = startFieldRange
										.greaterThan(currentTimeMillisMenos);
										var endFieldRangeAndBackToRange = greaterThan
										.endFieldRangeAndBackToRange();
										var endRangeAndBackToMust = endFieldRangeAndBackToRange
										.endRangeAndBackToMust();
										var term = endRangeAndBackToMust	
										.term(JnJsonCommonsFields.entity, entityName);
										var endMustAndBackToBool = term
										.endMustAndBackToBool();
										var endBoolAndBackToQuery = endMustAndBackToBool
										.endBoolAndBackToQuery();
										var endQueryAndBackToRequest = endBoolAndBackToQuery
										.endQueryAndBackToRequest();
										var maxResults = endQueryAndBackToRequest
										.maxResults();
										String timestampName2 = JnJsonCommonsFields.timestamp.name();
		CcpQueryOptions queryToSearchLastUpdated = 
				maxResults
					.addDescSorting(timestampName2)
				;
				CcpEntityMetaData entityMetaData2 = JnEntityDisposableRecord.ENTITY.getEntityMetaData();
				String[] resourcesNames = entityMetaData2.getEntitiesToSelect();
				String idName = JnJsonCommonsFields.id.name();

				queryExecutor.consumeQueryResult(queryToSearchLastUpdated, resourcesNames, "10m", 10000L, VisSendRecentUsersToGroupings.INSTANCE, idName);
		
		return json;
	}

}
