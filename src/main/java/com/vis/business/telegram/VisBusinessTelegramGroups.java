package com.vis.business.telegram;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.entities.JnEntitySystemParameter;

/**
 * The links of the Telegram groups whose positions are read by the load: one read by id of {@link JnEntitySystemParameter}
 * (OQL), kept in the cache of the entity. Takes no input.
 */
public class VisBusinessTelegramGroups implements CcpBusiness {

	/** The output of this business. */
	public static enum Fields implements CcpJsonFieldName {
		/** The links of the groups. */
		groups,
	}

	/** The single instance. */
	public static final VisBusinessTelegramGroups INSTANCE = new VisBusinessTelegramGroups();

	private VisBusinessTelegramGroups() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	/**
	 * Reads the links.
	 * @param json not used
	 * @return {@code groups}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		String systemMessageName = VisTelegramSystemParameters.groups.getSystemMessageName();
		List<String> links = JnEntitySystemParameter.getValues(systemMessageName);
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON.put(Fields.groups, links);
		return response;
	}
}
