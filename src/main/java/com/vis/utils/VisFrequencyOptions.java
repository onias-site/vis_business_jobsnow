package com.vis.utils;

/**
 * Defines the possible frequencies for sending resumes to recruiters, with the value in hours
 * corresponding to each frequency. Used to calculate the time windows of the queries that search for
 * recent resumes and positions.
 */
public enum  VisFrequencyOptions {
	minute(1d/60d),
	hourly(1),
	daily(24),
	weekly(168),
	yearly(8766),
	montly(730.5),
	;
	public final double hours;

	private VisFrequencyOptions(double hours) {
		this.hours = hours;
	}
	
}
