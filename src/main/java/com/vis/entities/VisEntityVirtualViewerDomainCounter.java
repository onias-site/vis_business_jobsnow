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
 * Virtual entity whose id identifies the only group of {@code JnEntityGrouper} that counts, for each domain of the
 * e-mails of the recruiters, how many views of resumes it ever made ({@code @CcpEntityGrouperChildCounter} with
 * {@code JnGrouperChildCounterEntityBeforeWrite} and {@code VisViewedResumeRecruiterDomain}, a history: the deactivation
 * and the delete of a view do not subtract). The non-professional domains are counted together as {@code freelancer}.
 * The chart of the companies that view the most resumes, in the screen Curiosities. Its only field always holds
 * {@code domain}. Cached for 1 hour.
 */
@CcpEntityCache(3600)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityVirtualViewerDomainCounter.Fields.class)
public class VisEntityVirtualViewerDomainCounter {

	/** The entity {@code vis_virtual_viewer_domain_counter}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityVirtualViewerDomainCounter.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/** The {@code counter} field: the primary key, always {@code domain}. */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 50)
		counter,
		;
	}

	/**
	 * The combination of the only group: {@code {"counter": "domain"}}.
	 * @return the combination
	 */
	public static CcpJsonRepresentation getCombination() {
		String counterName = VisEntityRecruiter.Fields.domain.getValue();
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(Fields.counter, counterName);
		return combination;
	}
}
