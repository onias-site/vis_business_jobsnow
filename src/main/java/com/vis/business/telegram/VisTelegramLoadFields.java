package com.vis.business.telegram;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNumberInteger;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;

/**
 * The input and the counters of the load of the positions of Telegram.
 */
public enum VisTelegramLoadFields implements CcpJsonFieldName {
	/** The {@code folderPath} field: the folder with the files, text, required. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldTypeString(minLength = 1, maxLength = 1000)
	folderPath,
	/** The {@code threads} field: required, how many files are read at the same time. */
	@CcpJsonFieldValidatorRequired
	@CcpJsonFieldTypeNumberInteger(minValue = 1, maxValue = 64)
	threads,
	/** The {@code keepFiles} field: whether the files are kept at the end of the load. */
	@CcpJsonFieldTypeBoolean
	keepFiles,
	/** The words that are not skills in the texts of Telegram. */
	excludedSkill,
	/** How many files were read to the end. */
	filesRead,
	/** How many messages have a text. */
	messagesRead,
	/** How many messages repeat, in the same file and day, the text of another one. */
	repeatedMessages,
	/** How many messages have no contact. */
	noContact,
	/** How many messages have no known skill. */
	noSkill,
	/** How many positions were sent to the index. */
	positionsWritten,
	/** How many of them did not reach the index. */
	notConfirmed,
	/** The files whose positions are all in the index. */
	confirmedFiles,
	/** The files that failed, with the reason. */
	failedFiles,
	/** How many files were deleted. */
	filesDeleted,
	/** The counters of the positions. */
	positions,
	/** The counters of the recruiters. */
	recruiters,
	/** The counters of the statistics. */
	statistics,
	/** The counters of the ranking. */
	ranking,
	/** How many positions the statistics read. */
	positionsRead,
	/** How many points of the line charts were written. */
	lineCharts,
	/** How many skills by region and seniority were written. */
	statisSkills,
	/** How many pie charts were written. */
	pieCharts,
	/** How many skills were ranked. */
	skillsRanked,
	/** How many skills have some occurrence. */
	skillsWithOccurrences,
	/** How many positions have the skill. */
	occurrences,
	/** Extension of the HTML files. */
	HTML_EXTENSION { public String getValue() { return ".html"; } },
	/** Short extension of the HTML files. */
	HTM_EXTENSION { public String getValue() { return ".htm"; } },
	/** Extension of the text files exported from WhatsApp. */
	TXT_EXTENSION { public String getValue() { return ".txt"; } },
	;
}
