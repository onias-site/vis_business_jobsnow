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
 * Serviço de sugestões de correção de hierarquia de skill: o candidato pede para associar ({@code add})
 * ou desassociar ({@code remove}) skills do seu currículo a um conhecimento implícito ({@code parent}).
 * A sugestão fica pendente em {@link VisEntitySkillFixHierarchyPending} até ser aprovada ou rejeitada.
 */
public enum VisServiceSkillFixHierarchy implements JnService {

	FixSkillHierarchy{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisEntitySkillFixHierarchyPending.ENTITY.save(json);
			return json;
		}
	},

	/**
	 * Devolve a sugestão do candidato para o parent e o type informados, acrescida do campo {@code status}.
	 * Procura primeiro na pendente: uma sugestão reenviada depois de aprovada ou rejeitada volta a ficar
	 * pendente, e é ela que o candidato deve ver. Sem sugestão, devolve json vazio.
	 *
	 * As três entidades são consultadas numa única ida ao banco (union all); a ordem de prioridade é
	 * aplicada depois, sobre o resultado já em memória.
	 */
	GetSkillFixHierarchy{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisSkillFixHierarchyStatus[] statuses = VisSkillFixHierarchyStatus.values();
			Stream<VisSkillFixHierarchyStatus> stream = Arrays.stream(statuses);
			CcpEntity[] entities = stream.map(status -> status.entity).toArray(CcpEntity[]::new);

			CcpCrud crud = CcpDependencyInjection.getDependency(CcpCrud.class);
			CcpSelectUnionAll unionAll = crud.unionAll(json, JnDeleteKeysFromCache.INSTANCE, entities);
			Supplier<CcpJsonRepresentation> jsonSupplier = json.getJsonSupplier();

			for (VisSkillFixHierarchyStatus status : statuses) {
				CcpJsonRepresentation found = status.entity.getRecordFromUnionAll(unionAll, jsonSupplier);
				boolean notFound = found.isEmpty();
				if(notFound) {
					continue;
				}
				CcpJsonRepresentation put = found.put(GetSkillFixHierarchyResponse.status, status.name());
				return put;
			}
			return CcpOtherConstants.EMPTY_JSON;
		}
	},

	/**
	 * O candidato desiste da sugestão ainda pendente. Só apaga da pendente: o que já foi aprovado ou
	 * rejeitado é histórico da análise e não pode ser desfeito por ele. Responde 404 quando não há
	 * sugestão pendente (ex.: foi analisada entre o candidato abrir o modal e desistir).
	 */
	DeleteSkillFixHierarchy{
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			boolean deleted = VisEntitySkillFixHierarchyPending.ENTITY.delete(json);
			boolean notFound = false == deleted;
			if(notFound) {
				CcpErrorFlowDisturb ccpErrorFlowDisturb = new CcpErrorFlowDisturb(json, CcpProcessStatusDefault.NOT_FOUND);
				throw ccpErrorFlowDisturb;
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
 * Validação do corpo de {@link VisServiceSkillFixHierarchy#FixSkillHierarchy}. Cada campo copia as
 * regras direto da classe que as declara, pois {@code CcpJsonCopyFieldValidationsFrom} não é recursiva.
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
 * Validação do corpo de {@link VisServiceSkillFixHierarchy#GetSkillFixHierarchy}: só a chave primária
 * das entidades de sugestão (email + parent + type).
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
 * Validação do corpo de {@link VisServiceSkillFixHierarchy#DeleteSkillFixHierarchy}: a mesma chave
 * primária da busca (email + parent + type).
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
