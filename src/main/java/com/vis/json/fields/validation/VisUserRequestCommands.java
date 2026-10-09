package com.vis.json.fields.validation;

/**
 * Support bot commands that review a request made by the user. The {@code commandName} of
 * {@code VisEntityCommandNotAllowedToUser} records in which of them the user was ignored (the ignoring itself is
 * global). Each name is the same as the one
 * of the support bot command.
 */
public enum VisUserRequestCommands {
	/** The command that reviews a skill hierarchy fix request. */
	fixSkillHierarchy,
	/** The command that reviews a skill suggestion. */
	reviewSkillSuggestion
}
