package com.vis.schedulling;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.query.CcpDbQueryOptions;
import com.ccp.especifications.db.query.CcpQueryExecutor;
import com.ccp.business.CcpBusiness;
import com.jn.entities.JnEntityDisposableRecord;
import com.jn.entities.JnEntityLoginSessionValidation;
import com.vis.utils.VisFrequencyOptions;
import com.vis.utils.VisSendRecentUsersToGroupings;

public class VisBusinessGetRecentLoggedUsers implements CcpBusiness{
	//TODO JSON VALIDATIONS	

	private VisBusinessGetRecentLoggedUsers() {}
	
	public static final VisBusinessGetRecentLoggedUsers INSTANCE = new VisBusinessGetRecentLoggedUsers();
	
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		
		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		
		String entityName = JnEntityLoginSessionValidation.ENTITY.getEntityName();
		CcpDbQueryOptions queryToSearchLastUpdated = 
				CcpDbQueryOptions.INSTANCE
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
		String[] resourcesNames = JnEntityDisposableRecord.ENTITY.getEntitiesToSelect();

		queryExecutor.consumeQueryResult(queryToSearchLastUpdated, resourcesNames, "10m", 10000L, VisSendRecentUsersToGroupings.INSTANCE, JnEntityDisposableRecord.Fields.id.name());
		
		return json;
	}

}
