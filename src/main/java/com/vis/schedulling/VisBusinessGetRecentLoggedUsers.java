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
 * Scheduled (cron) task that fetches every user who logged in during the last year and sends them to the
 * resume views and perceptions grouping processes. Consumes the JnEntityDisposableRecord index,
 * filtering the session records whose timestamp falls within the yearly period.
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
									double oneYearInMillis = VisFrequencyOptions.yearly.hours * 3_600_000;
									double oneYearAgoInMillis = currentTimeMillis - oneYearInMillis;
									var greaterThan = startFieldRange
										.greaterThan(oneYearAgoInMillis);
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
										String sortingFieldName = JnJsonCommonsFields.timestamp.name();
		CcpQueryOptions queryToSearchLastUpdated = 
				maxResults
					.addDescSorting(sortingFieldName)
				;
				CcpEntityMetaData disposableRecordMetaData = JnEntityDisposableRecord.ENTITY.getEntityMetaData();
				String[] resourcesNames = disposableRecordMetaData.getEntitiesToSelect();
				String idName = JnJsonCommonsFields.id.name();

				queryExecutor.consumeQueryResult(queryToSearchLastUpdated, resourcesNames, "10m", 10000L, VisSendRecentUsersToGroupings.INSTANCE, idName);
		
		return json;
	}

}
