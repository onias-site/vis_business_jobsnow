package com.vis.business.telegram;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFileDecorator;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.json.fields.validation.CcpJsonCommonsFields;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDoNotDeleteKeysFromCache;
import com.vis.entities.VisEntityTelegramPosition;

/**
 * The load of one file, a task of a thread: reads its messages (HTML exported from Telegram or text exported from
 * WhatsApp), writes its positions and confirms them in the index.
 * The ids are the same on every load, so a file can be read again with no harm. A failure stays in the counters of the
 * file (the other ones go on) and keeps the file out of the confirmed ones, so it is not deleted.
 */
public class VisTelegramFileLoad implements Callable<CcpJsonRepresentation> {

	/** How many positions go in each bulk. */
	private static final int BATCH_SIZE = 500;

	/** The file. */
	private final CcpFileDecorator file;

	/** Reads each position. */
	private final VisBusinessReadTelegramPosition positionReader;

	/**
	 * Takes the task.
	 * @param file the file
	 * @param positionReader reads each position
	 */
	public VisTelegramFileLoad(CcpFileDecorator file, VisBusinessReadTelegramPosition positionReader) {
		this.file = file;
		this.positionReader = positionReader;
	}

	/**
	 * Runs the load of the file.
	 * @return the counters of the file; {@code confirmedFiles} or {@code failedFiles} has the file
	 */
	public CcpJsonRepresentation call() {
		String filePath = this.file.getPath();
		try {
			CcpJsonRepresentation counters = this.load(filePath);
			System.out.println(counters.asUgglyJson());
			return counters;
		} catch (RuntimeException e) {
			String failure = filePath + ": " + e;
			System.out.println(failure);
			List<String> failedFile = new ArrayList<>();
			failedFile.add(failure);
			return CcpOtherConstants.EMPTY_JSON.put(VisTelegramLoadFields.failedFiles, failedFile);
		}
	}

	/**
	 * Loads the file.
	 * @param filePath the path of the file
	 * @return the counters of the file
	 */
	private CcpJsonRepresentation load(String filePath) {
		List<CcpJsonRepresentation> messages = this.getMessages();

		Map<String, CcpJsonRepresentation> positionsById = new LinkedHashMap<>();
		Set<String> textsRead = new HashSet<>();
		int repeatedMessages = 0;
		int noContact = 0;
		int noSkill = 0;
		for (CcpJsonRepresentation message : messages) {
			String text = message.getAsString(CcpJsonCommonsFields.text);
			String period = message.getAsString(JnJsonCommonsFields.disposablePeriod);
			boolean repeated = false == textsRead.add(period + text);

			if(repeated) {
				repeatedMessages++;
				continue;
			}

			CcpJsonRepresentation position = this.positionReader.execute(message);
			boolean withoutContact = position.containsAllFields(VisTelegramLoadFields.noContact);

			if(withoutContact) {
				noContact++;
				continue;
			}

			boolean withoutSkill = position.containsAllFields(VisTelegramLoadFields.noSkill);

			if(withoutSkill) {
				noSkill++;
				continue;
			}

			String positionId = VisEntityTelegramPosition.ENTITY.calculateId(position);
			positionsById.put(positionId, position);
		}

		int notConfirmed = this.writeAndConfirm(positionsById);
		int messagesCount = messages.size();
		int positionsCount = positionsById.size();
		CcpJsonRepresentation countersWithFiles = CcpOtherConstants.EMPTY_JSON.put(VisTelegramLoadFields.filesRead, 1);
		CcpJsonRepresentation countersWithMessages = countersWithFiles.put(VisTelegramLoadFields.messagesRead, messagesCount);
		CcpJsonRepresentation countersWithRepeated = countersWithMessages.put(VisTelegramLoadFields.repeatedMessages, repeatedMessages);
		CcpJsonRepresentation countersWithNoContact = countersWithRepeated.put(VisTelegramLoadFields.noContact, noContact);
		CcpJsonRepresentation countersWithNoSkill = countersWithNoContact.put(VisTelegramLoadFields.noSkill, noSkill);
		CcpJsonRepresentation countersWithPositions = countersWithNoSkill.put(VisTelegramLoadFields.positionsWritten, positionsCount);
		CcpJsonRepresentation counters = countersWithPositions.put(VisTelegramLoadFields.notConfirmed, notConfirmed);

		List<String> thisFile = new ArrayList<>();
		thisFile.add(filePath);
		boolean someRecordIsMissing = notConfirmed > 0;

		if(someRecordIsMissing) {
			return counters.put(VisTelegramLoadFields.failedFiles, thisFile);
		}

		return counters.put(VisTelegramLoadFields.confirmedFiles, thisFile);
	}

	/**
	 * The messages of the file, by its kind.
	 * @return the messages
	 */
	private List<CcpJsonRepresentation> getMessages() {
		String content = this.file.getStringContent();
		String fileName = this.file.getName();
		String lowerFileName = fileName.toLowerCase();
		String txtExtension = VisTelegramLoadFields.TXT_EXTENSION.getValue();
		boolean exportedFromWhatsApp = lowerFileName.endsWith(txtExtension);

		if(exportedFromWhatsApp) {
			return VisWhatsAppMessages.read(content);
		}

		return VisTelegramMessages.read(content);
	}

	/**
	 * Writes the positions, a batch at a time, and reads their ids back: the bulk does not tell the caller about the
	 * records it could not write.
	 * @param positionsById the positions
	 * @return how many positions are not in the index
	 */
	private int writeAndConfirm(Map<String, CcpJsonRepresentation> positionsById) {
		List<String> ids = new ArrayList<>(positionsById.keySet());
		CcpEntityMetaData positionDetails = VisEntityTelegramPosition.ENTITY.getEntityMetaData();
		int notConfirmed = 0;
		int idsCount = ids.size();
		for (int start = 0; start < idsCount; start += BATCH_SIZE) {
			int end = Math.min(start + BATCH_SIZE, idsCount);
			List<String> idsOfTheBatch = new ArrayList<>(ids.subList(start, end));
			List<CcpBulkItem> items = new ArrayList<>();
			for (String id : idsOfTheBatch) {
				CcpJsonRepresentation position = positionsById.get(id);
				List<CcpBulkItem> itemsOfThePosition = VisEntityTelegramPosition.ENTITY.toBulkItems(position, CcpBulkEntityOperationType.create);
				items.addAll(itemsOfThePosition);
			}
			JnExecuteBulkOperation.INSTANCE.executeBulk(items, JnDoNotDeleteKeysFromCache.INSTANCE);
			CcpJsonRepresentation positionsInTheIndex = positionDetails.getMultipleByRecordIds(idsOfTheBatch);
			Set<String> confirmedIds = positionsInTheIndex.fieldSet();
			int confirmedCount = confirmedIds.size();
			int batchCount = idsOfTheBatch.size();
			notConfirmed += batchCount - confirmedCount;
		}
		return notConfirmed;
	}
}
