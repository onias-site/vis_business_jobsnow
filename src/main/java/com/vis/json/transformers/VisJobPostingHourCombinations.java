package com.vis.json.transformers;

import java.util.List;
import java.util.function.Function;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.vis.entities.VisEntityVirtualJobPostingHour;

/**
 * Produces the group of {@code VisEntityVirtualJobPostingHour} a job posting is a child of: the day ({@code yyyyMMdd})
 * and the hour of its {@code timestamp}, the hour being the millisecond of the saving modulo 24 (since 2026-11-26). The
 * {@code timestamp} is stored, so the stored record gives the same group again; none when the record has no
 * {@code timestamp}.
 */
public class VisJobPostingHourCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisJobPostingHourCombinations INSTANCE = new VisJobPostingHourCombinations();

	private VisJobPostingHourCombinations() {}

	/** The texts of this class. */
	enum Constants implements CcpJsonFieldName {
		/** The format of the day. */
		DAY_PATTERN { public String getValue() { return "yyyyMMdd"; } },
	}

	/** How many groups a day has. */
	private static final long HOURS_OF_A_DAY = 24L;

	/**
	 * The group of the day and of the hour of the saving.
	 * @param json the job posting
	 * @return the combination, {@code {"day": <yyyyMMdd>, "hour": <0 to 23>}}, or none
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		boolean noTimestamp = false == json.containsAllFields(JnJsonCommonsFields.timestamp);

		if(noTimestamp) {
			return List.of();
		}

		Long timestamp = json.getAsLongNumber(JnJsonCommonsFields.timestamp);
		long remainder = timestamp % HOURS_OF_A_DAY;
		int hour = (int) remainder;
		CcpTimeDecorator time = new CcpTimeDecorator(timestamp);
		String dayPattern = Constants.DAY_PATTERN.getValue();
		String day = time.getFormattedDateTime(dayPattern);
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityVirtualJobPostingHour.Fields.day, day)
				.put(VisEntityVirtualJobPostingHour.Fields.hour, hour);
		return List.of(combination);
	}
}
