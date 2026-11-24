package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.vis.entities.VisEntityPosition;
import com.vis.entities.VisEntityVirtualPositionPublishingDay;

/**
 * The days on which a position is published in the groups of instant messages (since 2026-11-24): with
 * {@code mustSendInGroups} checked, every day from today (or from the start of the validity, when later) up to the end of
 * the validity, at most {@value #MAXIMUM_DAYS} days; unchecked or expired, none. Each day is the parent of a group of
 * {@code jn_grouper} ({@link VisEntityVirtualPositionPublishingDay}).
 */
public class VisPositionPublishingDayCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisPositionPublishingDayCombinations INSTANCE = new VisPositionPublishingDayCombinations();

	private VisPositionPublishingDayCombinations() {}

	/** The most days a position is published. */
	public static final int MAXIMUM_DAYS = 366;

	/** One day, in milliseconds. */
	static final long ONE_DAY = 86_400_000L;

	/** The texts of this class. */
	public static enum Constants implements CcpJsonFieldName {
		DAY_PATTERN { public String getValue() { return "yyyyMMdd"; } },
	}

	/** The fields of the validity of the position. */
	public static enum ExpirationFields implements CcpJsonFieldName {
		min, max
	}

	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation position) {
		List<CcpJsonRepresentation> combinations = new ArrayList<>();
		// the default value of mustSendInGroups (true) may come as a text: getAsBoolean reads both
		boolean publish = false == position.containsAllFields(VisEntityPosition.Fields.mustSendInGroups) || position.getAsBoolean(VisEntityPosition.Fields.mustSendInGroups);
		boolean hasExpiration = position.containsAllFields(VisEntityPosition.Fields.expiration);
		if(false == publish || false == hasExpiration) {
			return combinations;
		}
		CcpJsonRepresentation expiration = position.getInnerJson(VisEntityPosition.Fields.expiration);
		Double start = expiration.getAsDoubleNumber(ExpirationFields.min);
		Double end = expiration.getAsDoubleNumber(ExpirationFields.max);
		CcpTimeDecorator today = new CcpTimeDecorator();
		Long midnight = today.getMidnight();
		long firstDay = Math.max(midnight, start.longValue());
		String pattern = Constants.DAY_PATTERN.getValue();
		long day = firstDay;
		for (int count = 0; count < MAXIMUM_DAYS && day <= end; count++) {
			CcpTimeDecorator dayDecorator = new CcpTimeDecorator(day);
			String formattedDay = dayDecorator.getFormattedDateTime(pattern);
			CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualPositionPublishingDay.Fields.day, formattedDay);
			combinations.add(combination);
			day += ONE_DAY;
		}
		return combinations;
	}
}