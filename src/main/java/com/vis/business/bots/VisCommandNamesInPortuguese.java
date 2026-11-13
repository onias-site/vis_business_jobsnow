package com.vis.business.bots;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * How each command of {@link VisSupportBotCommands} and {@link VisUserBotCommands} is typed in Portuguese (without the {@code /}): the item is the
 * canonical name of the command and {@link #getValue()} its name in Portuguese. Seeded in {@code JnEntityBotCommandName};
 * the canonical name stays valid in every language, and in English it is the name itself.
 */
public enum VisCommandNamesInPortuguese implements CcpJsonFieldName{
	/** {@code /ajustarHierarquiaDeHabilidades}. */
	fixSkillHierarchy { public String getValue() { return "ajustarHierarquiaDeHabilidades"; } },
	/** {@code /avaliarSugestaoDeHabilidade}. */
	reviewSkillSuggestion { public String getValue() { return "avaliarSugestaoDeHabilidade"; } },
	/** {@code /verificarEnderecoDoLinkedin}. */
	verifyLinkedinAddress { public String getValue() { return "verificarEnderecoDoLinkedin"; } },
	/** {@code /receberMensagens}, of the user bot ({@link VisUserBotCommands}). */
	receiveMessages { public String getValue() { return "receberMensagens"; } },
	;
}
