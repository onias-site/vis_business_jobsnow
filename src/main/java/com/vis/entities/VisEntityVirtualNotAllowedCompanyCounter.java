package com.vis.entities;

import com.ccp.constants.CcpOtherConstants;
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
 * Virtual entity whose id identifies the only group of {@code JnEntityGrouper} that counts the consultancies the
 * candidates do not want their resumes sent to: its child is a JSON text with, for each domain of {@code notAllowedCompany}
 * of {@link VisEntityResume}, how many active resumes forbid it now ({@code @CcpEntityGrouperChildCounter} with
 * {@code JnGrouperChildCurrentCounterEntityBeforeWrite}). Its only field always holds the name of that field of the
 * resume, so every resume is counted in the same group. Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualNotAllowedCompanyCounter.Fields.class)
public class VisEntityVirtualNotAllowedCompanyCounter {

	/** The entity {@code vis_virtual_not_allowed_company_counter}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualNotAllowedCompanyCounter.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code counter} field: the primary key, always {@code notAllowedCompany}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 50)
		counter,
		;
	}

	/**
	 * The combination of the only group: {@code {"counter": "notAllowedCompany"}}.
	 * @return the combination
	 */
	public static CcpJsonRepresentation getCombination() {
		String counterName = VisEntityResume.Fields.notAllowedCompany.getValue();
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(Fields.counter, counterName);
		return combination;
	}

	/**
	 * The parent id of the only group, the {@code parentId} of its group in {@code JnEntityGrouper}.
	 * @return the parent id
	 */
	public static String getParentId() {
		CcpJsonRepresentation combination = getCombination();
		String parentId = ENTITY.calculateId(combination);
		return parentId;
	}
}
