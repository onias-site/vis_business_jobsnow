package com.vis.exceptions;

@SuppressWarnings("serial")
public class VisErrorBusinessMissingFeeToFrequency extends RuntimeException {
	public VisErrorBusinessMissingFeeToFrequency(String frequency) {
		super("It is missing the fee of frequency " + frequency);
	}
}
