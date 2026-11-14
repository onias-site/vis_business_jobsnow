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
 * Virtual entity whose id identifies the only group of {@code JnEntityGrouper} that counts, for each area of the resumes
 * ({@code resumeType}: 1 information technology, 2 recruiting, 3 higher education, 4 operational), how many active
 * resumes are of it now ({@code @CcpEntityGrouperChildCounter} with {@code JnGrouperChildCurrentCounterEntityBeforeWrite}
 * and {@code VisResumeTypeCounterKeys}): a chart of the screen Curiosities. Its only field always holds
 * {@code resumeType}. Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualResumeTypeCounter.Fields.class)
public class VisEntityVirtualResumeTypeCounter {

	/** The entity {@code vis_virtual_resume_type_counter}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualResumeTypeCounter.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code counter} field: the primary key, always {@code resumeType}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 50)
		counter,
		;
	}

	/**
	 * The combination of the only group: {@code {"counter": "resumeType"}}.
	 * @return the combination
	 */
	public static CcpJsonRepresentation getCombination() {
		String counterName = VisEntityResume.Fields.resumeType.getValue();
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(Fields.counter, counterName);
		return combination;
	}
}
