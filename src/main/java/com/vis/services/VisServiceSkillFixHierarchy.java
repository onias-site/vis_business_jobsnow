package com.vis.services;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.crud.CcpCrud;
import com.ccp.especifications.db.crud.CcpSelectUnionAll;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.flow.CcpErrorFlowDisturb;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorArray;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.process.CcpProcessStatusDefault;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.services.JnService;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.json.fields.validation.VisJsonCommonsFields;

/**
 * Service for skill hierarchy fix suggestions: the candidate asks to associate ({@code add})
 * or dissociate ({@code remove}) skills of their resume with an implicit knowledge ({@code parent}).
 * The suggestion stays pending in {@link VisEntitySkillFixHierarchyPending} until it is approved or rejected.
 */
public enum VisServiceSkillFixHierarchy implements JnService {

	FixSkillHierarchy{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisEntitySkillFixHierarchyPending.ENTITY.save(json);
			return json;
		}
	},

	/**
	 * Returns the candidate's suggestion for the given parent and type, plus the {@code status} field.
	 * Looks in the pending entity first: a suggestion resent after being approved or rejected becomes
	 * pending again, and that is the one the candidate must see. Without a suggestion, returns an empty json.
	 *
	 * The three entities are queried in a single database round trip (union all); the priority order is
	 * applied afterwards, on the result already in memory.
	 */
	GetSkillFixHierarchy{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisSkillFixHierarchyStatus[] statuses = VisSkillFixHierarchyStatus.values();
			Stream<VisSkillFixHierarchyStatus> statusesStream = Arrays.stream(statuses);
			CcpEntity[] entities = statusesStream.map(status -> status.entity).toArray(CcpEntity[]::new);

			CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
			CcpSelectUnionAll unionAll = crud.unionAll(json, JnDeleteKeysFromCache.INSTANCE, entities);
			Supplier<CcpJsonRepresentation> jsonSupplier = json.getJsonSupplier();

			for (VisSkillFixHierarchyStatus status : statuses) {
				CcpJsonRepresentation found = status.entity.getRecordFromUnionAll(unionAll, jsonSupplier);
				boolean notFound = found.isEmpty();
				if(notFound) {
					continue;
				}
				CcpJsonRepresentation suggestionWithStatus = found.put(GetSkillFixHierarchyResponse.status, status.name());
				return suggestionWithStatus;
			}
			return CcpOtherConstants.EMPTY_JSON;
		}
	},

	/**
	 * The candidate withdraws a suggestion that is still pending. Only deletes from the pending entity: what was
	 * already approved or rejected is review history and cannot be undone by the candidate. Responds 404 when
	 * there is no pending suggestion (e.g. it was reviewed between the candidate opening the modal and giving up).
	 */
	DeleteSkillFixHierarchy{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			boolean deleted = VisEntitySkillFixHierarchyPending.ENTITY.delete(json);
			boolean notFound = false == deleted;
			if(notFound) {
				CcpErrorFlowDisturb notFoundError = new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
				throw notFoundError;
			}
			return json;
		}
	}
	;
}

enum GetSkillFixHierarchyResponse implements CcpJsonFieldName{
	status
}

/**
 * Body validation of {@link VisServiceSkillFixHierarchy#FixSkillHierarchy}. Each field copies the
 * rules straight from the class that declares them, because {@code CcpJsonCopyFieldValidationsFrom} is not recursive.
 */
enum FixSkillHierarchy implements CcpJsonFieldName{
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	email,

	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	parent,

	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorArray
	@CcpJsonFieldValidatorRequired
	skill,

	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	description,

	@CcpJsonCopyFieldValidationsFrom(VisEntitySkillFixHierarchyPending.Fields.class)
	@CcpJsonFieldValidatorRequired
	type,
}

/**
 * Body validation of {@link VisServiceSkillFixHierarchy#GetSkillFixHierarchy}: only the primary key
 * of the suggestion entities (email + parent + type).
 */
enum GetSkillFixHierarchy implements CcpJsonFieldName{
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	email,

	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	parent,

	@CcpJsonCopyFieldValidationsFrom(VisEntitySkillFixHierarchyPending.Fields.class)
	@CcpJsonFieldValidatorRequired
	type,
}

/**
 * Body validation of {@link VisServiceSkillFixHierarchy#DeleteSkillFixHierarchy}: the same primary
 * key as the lookup (email + parent + type).
 */
enum DeleteSkillFixHierarchy implements CcpJsonFieldName{
	@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	email,

	@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
	@CcpJsonFieldValidatorRequired
	parent,

	@CcpJsonCopyFieldValidationsFrom(VisEntitySkillFixHierarchyPending.Fields.class)
	@CcpJsonFieldValidatorRequired
	type,
}
