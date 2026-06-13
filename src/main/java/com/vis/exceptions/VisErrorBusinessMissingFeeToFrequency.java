package com.vis.exceptions;

/**
 * Exceção lançada quando não existe uma tarifa cadastrada para a frequência de envio configurada em
 * uma vaga. Impede o processamento do envio de currículos sem definição de custo associado.
 */
@SuppressWarnings("serial")
public class VisErrorBusinessMissingFeeToFrequency extends RuntimeException {
	public VisErrorBusinessMissingFeeToFrequency(String frequency) {
		super("It is missing the fee of frequency " + frequency);
	}
}
