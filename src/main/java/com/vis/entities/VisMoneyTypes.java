package com.vis.entities;

/**
 * Compensation types accepted by the {@code moneyType} field of the virtual grouping hash entity,
 * that is, the hiring regime the compensation value ({@code moneyValue}) refers to:
 * formal employment (CLT), contractor company (PJ) or bitcoin (BTC).
 */
public enum VisMoneyTypes {

	/** Formal employment. */
	CLT,
	/** Bitcoin. */
	BTC,
	/** Contractor company. */
	PJ
	;
}
