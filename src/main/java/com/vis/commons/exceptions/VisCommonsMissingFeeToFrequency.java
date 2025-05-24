package com.vis.commons.exceptions;

@SuppressWarnings("serial")
public class VisCommonsMissingFeeToFrequency extends RuntimeException {
	public VisCommonsMissingFeeToFrequency(String frequency) {
		super("It is missing the fee of frequency " + frequency);
	}
}
