package com.vis.business.telegram;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFileDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.vis.business.recruiter.VisBusinessLoadRecruitersFromHtmlFolder;

/**
 * The whole load of a folder of files exported from Telegram (HTML) and WhatsApp (text), in this order: the positions; the
 * recruiters (their load reads the folder, so it must come before the files go away); the statistics and the ranking, from
 * the index; and the deletion of the files read, only the ones whose positions are all confirmed in the index (the others
 * stay for the next load, which writes the same ids). With {@code keepFiles}, nothing is deleted.
 *
 * <p>It is a batch process run by hand, not an online request.
 *
 * <p>Input: {@code folderPath}, {@code threads} and {@code keepFiles}. Output: the input plus {@code positions},
 * {@code recruiters}, {@code statistics}, {@code ranking} and {@code filesDeleted}.
 */
public class VisBusinessLoadTelegramFolder implements CcpBusiness {

	/** The single instance: the business keeps no state. */
	public static final VisBusinessLoadTelegramFolder INSTANCE = new VisBusinessLoadTelegramFolder();

	private VisBusinessLoadTelegramFolder() {}

	/**
	 * The fields of the input and of the output.
	 * @return the validation of the input
	 */
	public Class<?> getJsonValidationClass() {
		return VisTelegramLoadFields.class;
	}

	/**
	 * Runs the load.
	 * @param json the folder, the threads and whether the files are kept
	 * @return the input and the counters of each step
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String folderPath = json.getAsString(VisTelegramLoadFields.folderPath);

		CcpJsonRepresentation positions = VisBusinessLoadTelegramPositions.INSTANCE.execute(json);
		System.out.println(positions.getJsonPiece(VisTelegramLoadFields.filesRead, VisTelegramLoadFields.messagesRead, VisTelegramLoadFields.positionsWritten, VisTelegramLoadFields.notConfirmed).asUgglyJson());

		CcpJsonRepresentation recruitersInput = CcpOtherConstants.EMPTY_JSON.put(VisTelegramLoadFields.folderPath, folderPath);
		CcpJsonRepresentation recruiters = VisBusinessLoadRecruitersFromHtmlFolder.INSTANCE.execute(recruitersInput);
		CcpJsonRepresentation recruitersCounters = recruiters.removeFields(VisTelegramLoadFields.folderPath);
		System.out.println(recruitersCounters.asUgglyJson());

		CcpJsonRepresentation statistics = VisBusinessRecalculateSkillStatistics.INSTANCE.execute(CcpOtherConstants.EMPTY_JSON);
		System.out.println(statistics.asUgglyJson());

		CcpJsonRepresentation ranking = VisBusinessRecalculateSkillRanking.INSTANCE.execute(CcpOtherConstants.EMPTY_JSON);
		System.out.println(ranking.asUgglyJson());

		List<String> confirmedFiles = positions.getAsStringList(VisTelegramLoadFields.confirmedFiles);
		boolean keepFiles = json.getAsBoolean(VisTelegramLoadFields.keepFiles);
		int filesDeleted = keepFiles ? 0 : deleteFiles(confirmedFiles);

		CcpJsonRepresentation jsonWithPositions = json.put(VisTelegramLoadFields.positions, positions);
		CcpJsonRepresentation jsonWithRecruiters = jsonWithPositions.put(VisTelegramLoadFields.recruiters, recruitersCounters);
		CcpJsonRepresentation jsonWithStatistics = jsonWithRecruiters.put(VisTelegramLoadFields.statistics, statistics);
		CcpJsonRepresentation jsonWithRanking = jsonWithStatistics.put(VisTelegramLoadFields.ranking, ranking);
		CcpJsonRepresentation jsonWithDeletions = jsonWithRanking.put(VisTelegramLoadFields.filesDeleted, filesDeleted);
		return jsonWithDeletions;
	}

	/**
	 * Deletes the files.
	 * @param filePaths the paths of the files
	 * @return how many were deleted
	 */
	private static int deleteFiles(List<String> filePaths) {
		for (String filePath : filePaths) {
			CcpStringDecorator filePathDecorator = new CcpStringDecorator(filePath);
			CcpFileDecorator file = filePathDecorator.file();
			file.remove();
		}
		return filePaths.size();
	}
}
