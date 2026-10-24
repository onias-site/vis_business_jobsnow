package com.vis.business.bots;

/**
 * The commands of the support bot that belong to vis: {@code fixSkillHierarchy} (review of the skill hierarchy fix
 * requests) and {@code reviewSkillSuggestion} (review of the skills suggested by the users); {@code allowCommandToUser}
 * and {@code ignoredUsers} went to jn ({@code JnSupportBotCommands}) on 2026-10-23, with the ignoring. vis seeds their
 * configuration in the bot entities of jn (command, steps, names and texts) and the bot runs them by the class names
 * found there, without knowing vis. Until 2026-10-15 they were items of {@code JbSupportBotCommands}, in jb.
 */
public enum VisSupportBotCommands {

	/** Reviews the skill hierarchy fix requests. */
	fixSkillHierarchy,
	/** Reviews the skills suggested by the users. */
	reviewSkillSuggestion,
	;
}
