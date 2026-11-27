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
 * Produces the group of {@code VisEntityVirtualJobPostingHour} a job posting is a child of (since 2026-11-26): the day
 * ({@code yyyyMMdd}) of the saving and one of the hours still to come in that day, from the next hour to 23, chosen by
 * the millisecond of the saving: {@code hour of the saving + 1 + millisecond % (23 - hour of the saving)} (saved at 19h,
 * 20 to 23; at 16h, 17 to 23). Saved at 23h no hour is left in the day and the group is 23. The hour of the saving is in
 * the time zone of {@code CcpTimeDecorator}. The {@code timestamp} is stored, so the stored record gives the same group
 * again; none when the record has no {@code timestamp}.
 */
public class VisJobPostingHourCombinations implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	/** The single instance: the class keeps no state. */
	public final static VisJobPostingHourCombinations INSTANCE = new VisJobPostingHourCombinations();

	private VisJobPostingHourCombinations() {}

	/** The texts of this class. */
	public enum Constants implements CcpJsonFieldName {
		/** The format of the day. */
		DAY_PATTERN { public String getValue() { return "yyyyMMdd"; } },
		/** The format of the hour of the saving, from 00 to 23. */
		HOUR_PATTERN { public String getValue() { return "HH"; } },
	}

	/** The last hour of a day. */
	private static final int LAST_HOUR = 23;

	/**
	 * The group of the day of the saving and of one of the hours still to come in it.
	 * @param json the job posting
	 * @return the combination, {@code {"day": <yyyyMMdd>, "hour": <next hour to 23>}}, or none
	 */
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation json) {
		boolean noTimestamp = false == json.containsAllFields(JnJsonCommonsFields.timestamp);

		if(noTimestamp) {
			return List.of();
		}

		Long timestamp = json.getAsLongNumber(JnJsonCommonsFields.timestamp);
		CcpTimeDecorator time = new CcpTimeDecorator(timestamp);
		String dayPattern = Constants.DAY_PATTERN.getValue();
		String day = time.getFormattedDateTime(dayPattern);
		String hourPattern = Constants.HOUR_PATTERN.getValue();
		String hourOfTheSavingText = time.getFormattedDateTime(hourPattern);
		int hourOfTheSaving = Integer.parseInt(hourOfTheSavingText);
		int hour = getHour(hourOfTheSaving, timestamp);
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON
				.put(VisEntityVirtualJobPostingHour.Fields.day, day)
				.put(VisEntityVirtualJobPostingHour.Fields.hour, hour);
		return List.of(combination);
	}

	/**
	 * One of the hours after the hour of the saving, up to 23, by the millisecond.
	 * @param hourOfTheSaving the hour of the saving, from 0 to 23
	 * @param timestamp the millisecond of the saving
	 * @return the hour of the group; 23 when the saving is at 23h
	 */
	private static int getHour(int hourOfTheSaving, long timestamp) {
		int hoursStillToCome = LAST_HOUR - hourOfTheSaving;

		if(hoursStillToCome == 0) {
			return LAST_HOUR;
		}

		long remainder = timestamp % hoursStillToCome;
		int hour = hourOfTheSaving + 1 + (int) remainder;
		return hour;
	}
}
