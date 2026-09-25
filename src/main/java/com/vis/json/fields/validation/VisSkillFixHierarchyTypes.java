package com.vis.json.fields.validation;

/**
 * Tipos aceitos pelo campo {@code type} das sugestões de correção de hierarquia de skill, isto é,
 * se o usuário pede para associar ({@code add}) ou desassociar ({@code remove}) uma skill do seu
 * currículo a um conhecimento implícito ({@code parent}).
 */
public enum VisSkillFixHierarchyTypes {

	add,
	remove
	;
}
