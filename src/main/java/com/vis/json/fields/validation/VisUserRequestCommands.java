package com.vis.json.fields.validation;

/**
 * Support bot commands that review a request made by the user, and so the commands a user can be ignored
 * for ({@code commandName} of {@code VisEntityCommandNotAllowedToUser}). Each name is the same as the one
 * of the support bot command.
 */
public enum VisUserRequestCommands {
	/** The command that reviews a skill hierarchy fix request. */
	fixSkillHierarchy
}
