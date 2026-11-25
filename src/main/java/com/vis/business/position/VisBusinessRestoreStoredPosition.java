package com.vis.business.position;

import java.util.Calendar;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.vis.entities.VisEntityPosition;

/**
 * Step of "Restaurar" of the screen "Meu Recrutamento - Vagas": saves again in {@code vis_position} the deleted position,
 * found by the presence check in the twin {@code vis_inactive_position}, valid from the beginning of the current day
 * (America/Sao_Paulo) until the end of the day one week later. The save writes to the main entity and deletes from the
 * twin.
 * <p>
 * When the procedure also loaded a position not deleted with the same title (normally impossible, because saving a title
 * already deletes it from the twin), the restored one gets the title followed by the first free number, from 2 on, and the
 * position not deleted is saved again as it is stored: that save is what takes the deleted one out of the twin.
 */
public class VisBusinessRestoreStoredPosition implements CcpBusiness {

	/** The texts and fields of this step. */
	enum Fields implements CcpJsonFieldName {
		/** The beginning of the position. */
		min,
		/** The expiration of the position. */
		max,
		/** The title of the restored position when the original one is taken: the title, a space and a number. */
		NUMBERED_TITLE {
			public String getValue() {
				return "%s %d";
			}
		},
	}

	/** The maximum length of the title of a position (as {@code VisEntityPosition.Fields.title} validates it). */
	private static final int TITLE_MAX_LENGTH = 100;

	/** The first number tried in the title of a restored position whose title is taken. */
	private static final int FIRST_NUMBER = 2;

	/** The days of validity of the restored position after the current day. */
	private static final int DAYS_OF_VALIDITY = 7;

	/** The e-mail of the logged-in user, the recruiter of the position. */
	private final String email;

	/**
	 * Keeps the e-mail.
	 * @param email the e-mail of the logged-in user
	 */
	public VisBusinessRestoreStoredPosition(String email) {
		this.email = email;
	}

	/**
	 * Restores the position.
	 * @param json the json of the procedure, holding the deleted position (and the one not deleted of the same title, when
	 *        it exists) under {@code _entities}
	 * @return the json with the {@code title} and the {@code expiration} of the restored position
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		CcpEntity twinEntity = VisEntityPosition.ENTITY.getTwinEntity();
		CcpJsonRepresentation deletedPosition = VisPositionOfTheRecruiter.fromStored(json, this.email, twinEntity);
		CcpJsonRepresentation expiration = this.getExpirationOfOneWeek();
		CcpJsonRepresentation restoredPosition = deletedPosition.put(VisEntityPosition.Fields.expiration, expiration);
		CcpJsonRepresentation positionNotDeleted = json.getInnerJsonFromPath(CcpEntity.JsonFieldNames._entities, VisEntityPosition.ENTITY);
		CcpJsonRepresentation jsonWithExpiration = json.put(VisEntityPosition.Fields.expiration, expiration);
		boolean titleIsFree = positionNotDeleted.isEmpty();

		if (titleIsFree) {
			VisEntityPosition.ENTITY.save(restoredPosition);
			return jsonWithExpiration;
		}

		String title = deletedPosition.getAsString(VisEntityPosition.Fields.title);
		String yearMonth = deletedPosition.getAsString(VisEntityPosition.Fields.yearMonth);
		String freeTitle = this.getFreeTitle(title, yearMonth);
		CcpJsonRepresentation renamedPosition = restoredPosition.put(VisEntityPosition.Fields.title, freeTitle);
		VisEntityPosition.ENTITY.save(renamedPosition);
		CcpJsonRepresentation storedPositionNotDeleted = VisPositionOfTheRecruiter.fromStored(json, this.email);
		VisEntityPosition.ENTITY.save(storedPositionNotDeleted);
		CcpJsonRepresentation jsonWithFreeTitle = jsonWithExpiration.put(VisEntityPosition.Fields.title, freeTitle);
		return jsonWithFreeTitle;
	}

	/**
	 * The validity of the restored position: from the midnight of the current day to the last millisecond of the day one
	 * week later, both in the America/Sao_Paulo time zone.
	 * @return {@code {min, max}} in milliseconds
	 */
	private CcpJsonRepresentation getExpirationOfOneWeek() {
		CcpTimeDecorator now = new CcpTimeDecorator();
		Long midnight = now.getMidnight();
		CcpTimeDecorator midnightOfToday = new CcpTimeDecorator(midnight);
		Calendar calendar = midnightOfToday.getBrazilianCalendar();
		calendar.add(Calendar.DAY_OF_MONTH, DAYS_OF_VALIDITY + 1);
		long midnightAfterTheLastDay = calendar.getTimeInMillis();
		long max = midnightAfterTheLastDay - 1;
		CcpJsonRepresentation expiration = CcpOtherConstants.EMPTY_JSON
				.put(Fields.min, midnight)
				.put(Fields.max, max);
		return expiration;
	}

	/**
	 * The title followed by the first number, from 2 on, that no position not deleted of the user has; the title is cut
	 * so the numbered one fits the maximum length.
	 * @param title the title of the deleted position
	 * @param yearMonth the month of the creation of the deleted position, part of the key
	 * @return the free numbered title
	 */
	private String getFreeTitle(String title, String yearMonth) {
		String pattern = Fields.NUMBERED_TITLE.getValue();
		for (int number = FIRST_NUMBER; ; number++) {
			String suffix = String.valueOf(number);
			int baseMaxLength = TITLE_MAX_LENGTH - suffix.length() - 1;
			int baseLength = Math.min(title.length(), baseMaxLength);
			String base = title.substring(0, baseLength);
			String numberedTitle = String.format(pattern, base, number);
			CcpJsonRepresentation key = CcpOtherConstants.EMPTY_JSON
					.put(VisEntityPosition.Fields.recruiter, this.email)
					.put(VisEntityPosition.Fields.title, numberedTitle)
					.put(VisEntityPosition.Fields.yearMonth, yearMonth);
			boolean taken = VisEntityPosition.ENTITY.exists(key);
			if (taken) {
				continue;
			}
			return numberedTitle;
		}
	}
}
