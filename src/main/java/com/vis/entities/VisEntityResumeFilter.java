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
 * here every set of filters the user chooses (added, changed or removed), and the id of the record goes back to the
 * screen, which puts it in its address: opened with it, even days later, the screen reads the filters by this id and
 * fills them in. The records are discarded day by day (a record per set of filters would make the database huge): a link
 * opens its filters on the day it was made; on another day, the screen does nothing. The same filters in any order are
 * one record of the day (VisJsonTransformerResumeFilterSortedFields).
 * <p>
 * The answer of the search of the resumes over the filters (the ids, how many they are and the averages) is kept for 1
 * hour in the cache, under the id of the record, so the same filters asked again in the hour do not search
 * {@code jn_grouper} again: the API that answers the screen counts and caches them when the cache does not have them, and
 * the insertion ({@code VisBusinessCacheTheResumesOfTheFilter}) searches only when the cache lost the count meanwhile.
 * <p>
 * The writing is asynchronous: the save of a disposable entity is a bulk (the copy in jn_disposable_record is a grouper
 * child), which the REST APIs do without, so the record is written by the queue of the entity (topicos.tsv).
 * <p>
 * The only field is the primary key: the filters, the fields of the position that filter the resumes, with the same
 * validations of the position, all of them optional but the area.
 */
@CcpEntityCache(3600)
@CcpEntityAsyncWriter(JnAsyncWriterEntity.class)
@CcpEntityDisposable(value = JnDisposableEntity.class, timeOption = CcpEntityExpurgableOptions.daily)
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
