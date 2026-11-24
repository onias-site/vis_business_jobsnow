package com.vis.business.position;

import java.util.ArrayList;
import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.vis.entities.VisEntityPosition;

/**
 * The active positions (in {@code vis_position}, not expired) that receive their resumes in the given frequencies (since
 * 2026-11-24): the processes of the resumes of the positions and the daily e-mail to every recruiter, which leaves out the
 * recruiters who already receive the resumes of a position. A complex query, run only by the scheduled processes and by
 * the queue listener of the resumes (OQL).
 */
public class VisBusinessActivePositionsOfTheFrequency implements CcpBusiness {

	/** The input and the output of this business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The frequencies ({@code VisPositionFrequencyTypes} names). */
		frequencyType,
		/** The output: the positions, as stored. */
		positions,
	}

	/** The texts of this class. */
	static enum Constants implements CcpJsonFieldName {
		SCROLL_TIME { public String getValue() { return "1m"; } },
		EXPIRATION_MAX { public String getValue() { return "expiration.max"; } },
	}

	/** The size of each page of the reading. */
	static final int PAGE_SIZE = 1000;

	/** The single instance: the class keeps no state. */
	public static final VisBusinessActivePositionsOfTheFrequency INSTANCE = new VisBusinessActivePositionsOfTheFrequency();

	private VisBusinessActivePositionsOfTheFrequency() {}

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		List<String> frequencies = json.getAsStringList(Fields.frequencyType);
		long now = System.currentTimeMillis();
		String expirationMax = Constants.EXPIRATION_MAX.getValue();
		CcpQueryOptions query = CcpQueryOptions.INSTANCE
				.startQuery()
					.startBool()
						.startMust()
							.terms(VisEntityPosition.Fields.frequencyType, frequencies)
							.startRange()
								.startFieldRange(expirationMax)
									.greaterThanEquals(now)
								.endFieldRangeAndBackToRange()
							.endRangeAndBackToMust()
						.endMustAndBackToBool()
					.endBoolAndBackToQuery()
				.endQueryAndBackToRequest();
		List<CcpJsonRepresentation> positions = new ArrayList<>();
		String scrollTime = Constants.SCROLL_TIME.getValue();
		query.selectFrom(VisEntityPosition.ENTITY).consumeQueryResult(scrollTime, PAGE_SIZE, position -> positions.add(position));
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(Fields.positions, positions);
		return response;
	}
}
