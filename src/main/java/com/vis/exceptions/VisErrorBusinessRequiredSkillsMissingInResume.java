package com.vis.exceptions;

import java.util.List;

/**
 * Exceção lançada quando um currículo não possui todas as skills obrigatórias exigidas por uma vaga
 * (nem diretamente, nem por sinônimo, nem por relação de parentesco na hierarquia). Carrega a lista
 * das skills faltantes para diagnóstico.
 */
@SuppressWarnings("serial")
public class VisErrorBusinessRequiredSkillsMissingInResume extends RuntimeException{

	public final List<String> requiredSkillsNotFoundInResume;

	public VisErrorBusinessRequiredSkillsMissingInResume(List<String> requiredSkillsNotFoundInResume) {
		this.requiredSkillsNotFoundInResume = requiredSkillsNotFoundInResume;
	}
	
	
	
}
