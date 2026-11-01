package com.vis.business.telegram;

import java.util.ArrayList;
import java.util.List;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.utils.CcpDbRequester;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.especifications.http.CcpHttpMethods;
import com.ccp.especifications.http.CcpHttpResponseType;
import com.vis.entities.VisEntityTelegramPosition;

/**
 * Reads what is in the index of the positions of Telegram: the statistics and the ranking count only the records that
 * really entered it, never the messages read from the files.
 */
public final class VisTelegramPositionsIndex {

	/** The texts of the reading. */
	enum Constants implements CcpJsonFieldName {
		/** How long each page of the reading stays open. */
		SCROLL_TIME { public String getValue() { return "5m"; } },
		/** What makes the index show what was just written. */
		REFRESH { public String getValue() { return "/_refresh"; } },
		/** The start of a path of the database. */
		PATH_START { public String getValue() { return "/"; } },
		/** The name of the refresh in the logs of the database. */
		REFRESH_TRACE { public String getValue() { return "refreshTelegramPositions"; } },
	}

	/** How many positions each page brings. */
	private static final int PAGE_SIZE = 5_000;

	/** Utility class; not instantiable. */
	private VisTelegramPositionsIndex() {}

	/**
	 * Every position of the index, with its {@code id}. The reading of a whole index is a CQD of a batch process.
	 * @return the positions: {@code id}, {@code skill}, {@code ddd} (an integer), {@code seniority} and {@code yearMonth}
	 */
	public static List<CcpJsonRepresentation> readAll() {
		CcpEntityMetaData positionDetails = VisEntityTelegramPosition.ENTITY.getEntityMetaData();
		String refreshPath = Constants.PATH_START.getValue() + positionDetails.entityName + Constants.REFRESH.getValue();
		CcpDbRequester dbRequester = CcpDependencyInjection.getDependency(CcpDbRequester.class);
		String refreshTrace = Constants.REFRESH_TRACE.getValue();
		dbRequester.executeHttpRequest(refreshTrace, refreshPath, CcpHttpMethods.POST, 200, CcpOtherConstants.EMPTY_JSON, CcpHttpResponseType.singleRecord);

		List<CcpJsonRepresentation> storedPositions = new ArrayList<>();
		String scrollTime = Constants.SCROLL_TIME.getValue();
		String skillField = VisEntityTelegramPosition.Fields.skill.getValue();
		String dddField = VisEntityTelegramPosition.Fields.ddd.getValue();
		String seniorityField = VisEntityTelegramPosition.Fields.seniority.getValue();
		String yearMonthField = VisEntityTelegramPosition.Fields.yearMonth.getValue();
		CcpQueryOptions everyPosition = CcpQueryOptions.INSTANCE.matchAll();
		everyPosition.selectFrom(VisEntityTelegramPosition.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, storedPositions::add, skillField, dddField, seniorityField, yearMonthField);

		// a number read from the database may come as a Double, and the ids are calculated from the text of the value
		List<CcpJsonRepresentation> positions = new ArrayList<>();
		for (CcpJsonRepresentation storedPosition : storedPositions) {
			Integer ddd = storedPosition.getAsIntegerNumber(VisEntityTelegramPosition.Fields.ddd);
			CcpJsonRepresentation position = storedPosition.put(VisEntityTelegramPosition.Fields.ddd, ddd);
			positions.add(position);
		}
		return positions;
	}
}
