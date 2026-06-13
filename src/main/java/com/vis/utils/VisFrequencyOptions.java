package com.vis.utils;

/**
 * Define as frequências possíveis de envio de currículos para recrutadores, com o valor em horas
 * correspondente a cada frequência. Usado para calcular janelas de tempo nas queries de busca por
 * currículos e vagas recentes.
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
