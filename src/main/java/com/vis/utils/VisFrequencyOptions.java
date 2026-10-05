package com.vis.utils;

/**
 * Defines the possible frequencies for sending resumes to recruiters, with the value in hours
 * corresponding to each frequency. Used to calculate the time windows of the queries that search for
 * recent resumes and positions.
 */
public enum  VisFrequencyOptions {
	/** Every minute. */
	minute(1d/60d),
	/** Every hour. */
	hourly(1),
	/** Every day. */
	daily(24),
	/** Every week. */
	weekly(168),
	/** Every year. */
	yearly(8766),
	/** Every month (spelled {@code montly}, unlike {@code monthly} of the position frequency). */
	montly(730.5),
	;
	/** The length of the period, in hours. */
	public final double hours;

	/**
	 * Associates the frequency with the length of its period.
	 * @param hours the length, in hours
	 */
	private VisFrequencyOptions(double hours) {
		this.hours = hours;
	}
	
}
