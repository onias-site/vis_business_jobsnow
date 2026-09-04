package com.vis.entities;

/**
 * Tipos de remuneração aceitos pelo campo {@code moneyType} da entidade virtual de hash de agrupamento,
 * isto é, o regime de contratação ao qual o valor de remuneração ({@code moneyValue}) se refere:
 * carteira assinada (CLT), pessoa jurídica (PJ) ou bitcoin (BTC).
 */
public enum VisMoneyTypes {

	CLT,
	BTC,
	PJ
	;
}
