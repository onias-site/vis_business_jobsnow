package com.vis.json.transformers;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;

public enum VisSeniorityType {
	
	estag(0,1),
	junior(1,3),
	pleno(3,5),
	senior(5,10),
	// the upper bound is exclusive: 71 keeps the 70 years accepted by the validation of the experience field
	especialista(10, 71),
	// a seniority that could not be told, as in the positions read from Telegram: an empty range, so no experience matches it
	unknown(0, 0)
	;
	
	private final int min;
	private final int max;
	
	private VisSeniorityType(int min, int max) {
		this.min = min;
		this.max = max;
	}

	public boolean matches(CcpJsonFieldName field, CcpJsonRepresentation json) {
		Integer number = json.getAsIntegerNumber(field);
		if(number < this.min) {
			return false;
		}
		if(number >= this.max) {
			return false;
		}
		return true;
	}

	/**
	 * The earliest year a career may have started to be of this seniority now: the experience of the resume is the year its
	 * career started, so the years of experience are the current year minus it.
	 * @param currentYear the current year
	 * @return the first year of the range, inclusive
	 */
	public int getFirstExperienceYear(int currentYear) {
		int firstYear = currentYear - this.max + 1;
		return firstYear;
	}

	/**
	 * The latest year a career may have started to be of this seniority now (an empty range, the first year after the last
	 * one, for {@link #unknown}).
	 * @param currentYear the current year
	 * @return the last year of the range, inclusive
	 */
	public int getLastExperienceYear(int currentYear) {
		int lastYear = currentYear - this.min;
		return lastYear;
	}

	
}
