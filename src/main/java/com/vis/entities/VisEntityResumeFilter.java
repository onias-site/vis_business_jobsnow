package com.vis.entities;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityAsyncWriter;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityCache;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityDisposable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.operations.CcpAfterInsertFromMainEntity;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityExpurgableOptions;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldTransformer;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeNestedJson;
import com.jn.entities.decorators.engine.JnAsyncWriterEntity;
import com.jn.entities.decorators.engine.JnDisposableEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.vis.business.resume.filter.VisBusinessCacheTheResumesOfTheFilter;
import com.vis.json.fields.validation.VisJsonFieldsResumeFilter;
import com.vis.json.transformers.VisJsonTransformerResumeFilterSortedFields;

/**
 * The filters of a search of resumes (since 2026-11-25): the screen {@code curriculos/buscar} and the position form save
 * here every set of filters the user chooses (added, changed or removed), and the insertion keeps for 1 hour in the cache,
 * under the id of the record, the answer of the search of the resumes of the position form over them (the ids, how many
 * they are and the averages; {@code VisBusinessCacheTheResumesOfTheFilter}). The same filters asked again in the hour
 * have the same id and are answered from the cache, with no second search. The id goes back to the screen, which puts it
 * in its address: opened with it, the screen reads the filters by this id and fills them in.
 * <p>
 * The writing is asynchronous (the REST APIs have no bulk, which every disposable entity needs): the API that answers the
 * screen counts and caches the resumes itself when the cache does not have them yet, and the insertion, in the consumer
 * of the queue, searches only when the cache lost the count meanwhile.
 * <p>
 * The only field is the primary key: the filters, the fields of the position that filter the resumes, with the same
 * validations of the position, all of them optional but the area. The record is discarded with the hour.
 */
@CcpEntityCache(3600)
// the save of a disposable entity is a bulk (the copy in jn_disposable_record is a grouper child), which the REST APIs
// do without: the record is written by the queue of the entity (topicos.tsv)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityDisposable(value = JnDisposableEntity.class, timeOption = CcpEntityExpurgableOptions.hourly)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityFieldsValidator(VisEntityResumeFilter.Fields.class)
// the search of the resumes of the filters, cached under the id of the record
@CcpAfterInsertFromMainEntity(execute = {VisBusinessCacheTheResumesOfTheFilter.class}, operationHandlers = {})
public class VisEntityResumeFilter implements CcpEntityConfigurator {
	/** The single instance: the class keeps no state. */
	public final static VisEntityResumeFilter INSTANCE = new VisEntityResumeFilter();

	private VisEntityResumeFilter() {}

	/** The entity {@code vis_resume_filter}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityResumeFilter.class).entityInstance;

	/** The fields of the entity, with their validation rules. */
	public static enum Fields implements CcpJsonFieldName{
		/**
		 * The {@code json} field: the primary key, the filters ({@code VisJsonFieldsResumeFilter}), kept sorted by name
		 * ({@code VisJsonTransformerResumeFilterSortedFields}) so the same filters always give the same id.
		 */
		@CcpEntityFieldPrimaryKey
		@CcpJsonFieldTypeNestedJson(jsonValidation = VisJsonFieldsResumeFilter.class, allowsEmptyJson = false)
		@CcpEntityFieldTransformer(VisJsonTransformerResumeFilterSortedFields.class)
		json,
		;
	}
}
