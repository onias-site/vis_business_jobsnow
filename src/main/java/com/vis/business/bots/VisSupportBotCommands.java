package com.vis.business.bots;

/**
 * The commands of the support bot that belong to vis: {@code fixSkillHierarchy} (review of the skill hierarchy fix
 * requests), {@code reviewSkillSuggestion} (review of the skills suggested by the users), {@code allowCommandToUser}
 * (stops ignoring a user) and {@code ignoredUsers} (goes through the users ignored by the support). vis seeds their
 * configuration in the bot entities of jn (command, steps, names and texts) and the bot runs them by the class names
 * found there, without knowing vis. Until 2026-10-15 they were items of {@code JbSupportBotCommands}, in jb.
 */
public enum VisSupportBotCommands {

	/** Reviews the skill hierarchy fix requests. */
	fixSkillHierarchy,
	/** Stops ignoring a user for a command. */
	allowCommandToUser,
	/** Reviews the skills suggested by the users. */
	reviewSkillSuggestion,
	/** Goes through the users ignored by the support, to stop ignoring them. */
	ignoredUsers
	;
}
