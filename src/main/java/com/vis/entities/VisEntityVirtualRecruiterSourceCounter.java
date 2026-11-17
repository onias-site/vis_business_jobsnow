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
 * Virtual entity whose id identifies the only group of {@code JnEntityGrouper} that counts, for each {@code source} of the
 * recruiters ({@link VisEntityRecruiter.Sources}), how many active recruiters came from it now
 * ({@code @CcpEntityGrouperChildCounter} with {@code JnGrouperChildCurrentCounterEntityBeforeWrite} and
 * {@code VisRecruiterSourceCounterKeys}): the chart "Origem dos recrutadores" of the screen Curiosities (since
 * 2026-11-17). Its only field always holds {@code source}. Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualRecruiterSourceCounter.Fields.class)
public class VisEntityVirtualRecruiterSourceCounter {

	/** The entity {@code vis_virtual_recruiter_source_counter}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualRecruiterSourceCounter.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code counter} field: the primary key, always {@code source}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 50)
		counter,
		;
	}

	/**
	 * The combination of the only group: {@code {"counter": "source"}}.
	 * @return the combination
	 */
	public static CcpJsonRepresentation getCombination() {
		String counterName = VisEntityRecruiter.Fields.source.getValue();
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(Fields.counter, counterName);
		return combination;
	}
}
