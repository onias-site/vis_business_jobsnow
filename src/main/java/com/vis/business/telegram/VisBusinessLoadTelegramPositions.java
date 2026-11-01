package com.vis.business.telegram;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpFileDecorator;
import com.ccp.decorators.CcpFolderDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpStringDecorator;
import com.vis.entities.VisEntityTelegramPosition;

/**
 * Reads the HTML files exported from Telegram and the text files exported from WhatsApp, each one in a thread, and writes
 * their positions in {@link VisEntityTelegramPosition}, under the day of each message. It only adds: that index is the only source of the
 * statistics, because the files go away after the load.
 *
 * <p>It is a batch process run by hand, not an online request.
 *
 * <p>Input: {@code folderPath} and {@code threads}. Output: the input plus the sum of the counters of the files
 * (see {@link VisTelegramFileLoad}).
 */
public class VisBusinessLoadTelegramPositions implements CcpBusiness {

	/** The single instance: the business keeps no state. */
	public static final VisBusinessLoadTelegramPositions INSTANCE = new VisBusinessLoadTelegramPositions();

	private VisBusinessLoadTelegramPositions() {}

	/**
	 * The fields of the input and of the output.
	 * @return the validation of the input
	 */
	public Class<?> getJsonValidationClass() {
		return VisTelegramLoadFields.class;
	}

	/**
	 * Runs the load.
	 * @param json the folder and the threads
	 * @return the input and the counters of the load
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String folderPath = json.getAsString(VisTelegramLoadFields.folderPath);
		Integer threads = json.getAsIntegerNumber(VisTelegramLoadFields.threads);
		List<CcpFileDecorator> files = getFilesToRead(folderPath);

		VisSkillWordsInMemory skillWords = new VisSkillWordsInMemory();
		VisTelegramDdd regions = new VisTelegramDdd();
		VisBusinessReadTelegramPosition positionReader = new VisBusinessReadTelegramPosition(skillWords, regions);

		List<Callable<CcpJsonRepresentation>> tasks = new ArrayList<>();
		for (CcpFileDecorator file : files) {
			VisTelegramFileLoad task = new VisTelegramFileLoad(file, positionReader);
			tasks.add(task);
		}

		List<CcpJsonRepresentation> countersOfTheFiles = runInThreads(tasks, threads);
		CcpJsonRepresentation result = sumCounters(json, countersOfTheFiles);
		return result;
	}

	/**
	 * The files of the folder that have messages.
	 * @param folderPath the path of the folder
	 * @return the files
	 */
	private static List<CcpFileDecorator> getFilesToRead(String folderPath) {
		CcpStringDecorator folderPathText = new CcpStringDecorator(folderPath);
		CcpFolderDecorator folder = folderPathText.folder();
		List<CcpFileDecorator> entriesOfTheFolder = new ArrayList<>();
		folder.readFiles(entriesOfTheFolder::add);

		String htmlExtension = VisTelegramLoadFields.HTML_EXTENSION.getValue();
		String htmExtension = VisTelegramLoadFields.HTM_EXTENSION.getValue();
		String txtExtension = VisTelegramLoadFields.TXT_EXTENSION.getValue();
		List<CcpFileDecorator> files = new ArrayList<>();
		for (CcpFileDecorator entry : entriesOfTheFolder) {
			String entryName = entry.getName();
			String lowerEntryName = entryName.toLowerCase();
			boolean fileToRead = entry.isFile() && (lowerEntryName.endsWith(htmlExtension) || lowerEntryName.endsWith(htmExtension) || lowerEntryName.endsWith(txtExtension));
			boolean notAFileToRead = false == fileToRead;

			if(notAFileToRead) {
				continue;
			}

			files.add(entry);
		}
		return files;
	}

	/**
	 * Runs the tasks and waits for all of them.
	 * @param tasks the tasks
	 * @param threads how many run at the same time
	 * @return the result of each task
	 */
	private static List<CcpJsonRepresentation> runInThreads(List<Callable<CcpJsonRepresentation>> tasks, int threads) {
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			List<Future<CcpJsonRepresentation>> futures = pool.invokeAll(tasks);
			List<CcpJsonRepresentation> results = new ArrayList<>();
			for (Future<CcpJsonRepresentation> future : futures) {
				CcpJsonRepresentation result = future.get();
				results.add(result);
			}
			return results;
		} catch (InterruptedException | ExecutionException e) {
			throw new VisErrorTelegramLoadInterrupted(e);
		} finally {
			pool.shutdown();
		}
	}

	/**
	 * Puts the sum of the counters of the files in the output.
	 * @param json the input
	 * @param countersOfTheFiles the counters of each file
	 * @return the output
	 */
	private static CcpJsonRepresentation sumCounters(CcpJsonRepresentation json, List<CcpJsonRepresentation> countersOfTheFiles) {
		CcpJsonFieldName[] numbers = {VisTelegramLoadFields.filesRead, VisTelegramLoadFields.messagesRead, VisTelegramLoadFields.repeatedMessages,
				VisTelegramLoadFields.noContact, VisTelegramLoadFields.noSkill, VisTelegramLoadFields.positionsWritten, VisTelegramLoadFields.notConfirmed};
		CcpJsonRepresentation output = json;
		for (CcpJsonFieldName number : numbers) {
			int sum = 0;
			for (CcpJsonRepresentation counters : countersOfTheFiles) {
				boolean noCounter = false == counters.containsAllFields(number);

				if(noCounter) {
					continue;
				}

				sum += counters.getAsIntegerNumber(number);
			}
			output = output.put(number, sum);
		}

		CcpJsonFieldName[] fileLists = {VisTelegramLoadFields.confirmedFiles, VisTelegramLoadFields.failedFiles};
		for (CcpJsonFieldName fileList : fileLists) {
			List<String> allFiles = new ArrayList<>();
			for (CcpJsonRepresentation counters : countersOfTheFiles) {
				List<String> filesOfTheCounters = counters.getAsStringList(fileList);
				allFiles.addAll(filesOfTheCounters);
			}
			output = output.put(fileList, allFiles);
		}
		return output;
	}

	/** Raised when the load is interrupted while the threads run. */
	@SuppressWarnings("serial")
	private static class VisErrorTelegramLoadInterrupted extends RuntimeException {
		/**
		 * Builds the error.
		 * @param cause what interrupted the load
		 */
		private VisErrorTelegramLoadInterrupted(Exception cause) {
			super(cause);
		}
	}
}
