package com.vis.entities;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpTimeDecorator;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;

/**
 * The virtual entity that gives the id of the groups of the positions that interested candidates in each hour (since
 * 2026-11-24): one group of {@code jn_grouper} per hour of sending ({@code yyyyMMddHH}), whose children are the ids of the
 * positions that some candidate asked to receive their resume ({@link VisEntityPositionInterest}). The process of every
 * full hour reads the group of the hour by id and, for each position, the group of its interested resumes
 * ({@link VisEntityVirtualPositionInterestedResumes}). Not a table of its own.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualPositionInterestHour.Fields.class)
public class VisEntityVirtualPositionInterestHour {

	/** The entity, used only to calculate the id of the groups and to name them. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualPositionInterestHour.class).entityInstance;

	/** One hour, in milliseconds. */
	public static final long ONE_HOUR = 3_600_000L;

	/** The texts of this class. */
	static enum Constants implements CcpJsonFieldName {
		HOUR_PATTERN { public String getValue() { return "yyyyMMddHH"; } },
	}

	/** The fields of the entity. */
	public static enum Fields implements CcpJsonFieldName{
		/** The hour of the sending, {@code yyyyMMddHH}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 10, maxLength = 10)
		sendingHour,
		;
	}

	/**
	 * The parent id of the group of an hour, the {@code parentId} of its group in {@code jn_grouper}.
	 * @param sendingHour the hour, {@code yyyyMMddHH}
	 * @return the id of the group
	 */
	public static String getParentId(String sendingHour) {
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(Fields.sendingHour, sendingHour);
		String parentId = ENTITY.calculateId(combination);
		return parentId;
	}

	/**
	 * The hour of a moment, {@code yyyyMMddHH}: the process of a full hour sends the group of that hour.
	 * @param time the moment
	 * @return the hour
	 */
	public static String getSendingHour(long time) {
		CcpTimeDecorator timeDecorator = new CcpTimeDecorator(time);
		String pattern = Constants.HOUR_PATTERN.getValue();
		String sendingHour = timeDecorator.getFormattedDateTime(pattern);
		return sendingHour;
	}

	/**
	 * The next full hour after a moment, {@code yyyyMMddHH}: an interest registered before 22:00 goes at 22:00, one
	 * registered at 22:00 or 22:01 goes at 23:00.
	 * @param now the moment of the interest
	 * @return the hour of its sending
	 */
	public static String getNextSendingHour(long now) {
		long oneHourLater = now + ONE_HOUR;
		String sendingHour = getSendingHour(oneHourLater);
		return sendingHour;
	}
}
