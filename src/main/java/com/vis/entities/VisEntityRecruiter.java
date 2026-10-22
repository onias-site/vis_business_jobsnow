package com.vis.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.query.CcpQueryExecutor;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsTransformer;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityFieldsValidator;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityTwin;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionable;
import com.ccp.especifications.db.utils.entity.decorators.annotations.CcpEntityVersionablePurge;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityFactory;
import com.ccp.especifications.db.utils.entity.decorators.enums.CcpEntityDecoratorOperationType;
import com.ccp.especifications.db.utils.entity.decorators.interfaces.CcpEntityConfigurator;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldPrimaryKey;
import com.ccp.especifications.db.utils.entity.fields.annotations.CcpEntityFieldTransformer;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeBoolean;
import com.ccp.json.validations.fields.annotations.type.CcpJsonFieldTypeString;
import com.ccp.json.validations.global.engine.CcpJsonValidationError;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.entities.JnEntityWriteEventSubscribers;
import com.jn.entities.decorators.engine.JnVersionableEntity;
import com.jn.entities.decorators.engine.JnVersionablePurgeEntity;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDefault;
import com.jn.entities.fields.transformers.JnJsonTransformersFieldsEntityDoNothing;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.vis.business.recruiter.VisBusinessLoadRecruitersFromHtmlFolder;
import com.vis.business.recruiter.VisBusinessPrepareRecruiter;
import com.vis.business.recruiter.VisBusinessSaveRecruiterFromLoginAnswers;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.transformers.VisJsonTransformerRecruiterDomain;
import com.vis.json.transformers.VisJsonTransformerRecruiterProfessional;

/**
 * The recruiters: one record per e-mail, kept as typed (no hash), because it is reused to send messages to the
 * recruiter. The {@code domain} and the {@code professional} flag are calculated from the e-mail by the transformers of
 * the fields; the {@code source} tells where the record came from ({@link Sources}).
 *
 * <p>Versioned, with the twin entity {@code vis_inactive_recruiter}, which keeps the inactive recruiters: the delete
 * moves the recruiter there, the delete on the twin moves it back.
 *
 * <p>Loads:
 * <ul>
 * <li>the first one is the seed of this entity ({@link #getFirstRecordsToInsert()}): every e-mail of the legacy indices
 * {@code recruiters} and {@code old_recruiters}, with the source {@code legacy};</li>
 * <li>the next ones read the e-mails of the HTML files of a folder ({@link VisBusinessLoadRecruitersFromHtmlFolder}),
 * with the source {@code telegram}; an e-mail already here or in the twin is not written again.</li>
 * </ul>
 * The sign-up of a user whose goal is recruiting also writes here, with the source {@code login}, or brings back the
 * inactive recruiter: the seed subscribes {@link VisBusinessSaveRecruiterFromLoginAnswers} to the save of
 * {@code js_login_answers} in {@code jn_write_event_subscribers}, so js never imports vis.
 * Both go to the database in bulk, around the decorators of the entity, so the record is transformed and validated
 * beforehand by {@link VisBusinessPrepareRecruiter}.
 */
@CcpEntityVersionable(JnVersionableEntity.class)
@CcpEntityVersionablePurge(JnVersionablePurgeEntity.class)
@CcpEntityFieldsValidator(VisEntityRecruiter.Fields.class)
@CcpEntityFieldsTransformer(JnJsonTransformersFieldsEntityDefault.class)
@CcpEntityTwin(
		twinEntityName = "vis_inactive_recruiter",
		bulkExecutorClass = JnExecuteBulkOperation.class,
		functionToDeleteKeysInTheCacheClass = JnDeleteKeysFromCache.class
		)
public class VisEntityRecruiter implements CcpEntityConfigurator {

	/** The entity {@code vis_recruiter}, with every decorator of this configuration. */
	public static final CcpEntity ENTITY = new CcpEntityFactory(VisEntityRecruiter.class).entityInstance;

	/**
	 * The fields of the entity, with their validation rules (this enum is the class named by
	 * {@code @CcpEntityFieldsValidator}).
	 */
	public static enum Fields implements CcpJsonFieldName{
		/**
		 * The {@code email} field: the primary key, validated as in {@code JnJsonCommonsFields}; transformed by
		 * {@code JnJsonTransformersFieldsEntityDoNothing}, so it is kept as typed (not as a hash) to be used in messages.
		 */
		@CcpEntityFieldPrimaryKey
		@CcpJsonCopyFieldValidationsFrom(JnJsonCommonsFields.class)
		@CcpEntityFieldTransformer(JnJsonTransformersFieldsEntityDoNothing.class)
		email,

		/**
		 * The {@code domain} field: required, text; calculated by {@code VisJsonTransformerRecruiterDomain} as the whole
		 * text of the e-mail after the {@code @}.
		 */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 4, maxLength = 100)
		@CcpEntityFieldTransformer(VisJsonTransformerRecruiterDomain.class)
		domain,

		/**
		 * The {@code professional} field: boolean; calculated by {@code VisJsonTransformerRecruiterProfessional}, it is
		 * {@code false} when the domain is in the non-professional domains of {@code jn_system_message}.
		 */
		@CcpJsonFieldTypeBoolean
		@CcpEntityFieldTransformer(VisJsonTransformerRecruiterProfessional.class)
		professional,

		/** The {@code source} field: required, text of up to 20 characters, where the record came from ({@link Sources}). */
		@CcpJsonFieldValidatorRequired
		@CcpJsonFieldTypeString(minLength = 1, maxLength = 20)
		source,
	}

	/** The values of the {@code source} field. */
	public static enum Sources implements CcpJsonFieldName{
		/** The first load, from the legacy indices {@code recruiters} and {@code old_recruiters}. */
		legacy,
		/** The loads from the HTML files of a folder, with the e-mails gathered on Telegram. */
		telegram,
		/** The sign-up of a user whose goal is recruiting ({@link VisBusinessSaveRecruiterFromLoginAnswers}). */
		login,
	}

	/** The legacy indices of the first load. */
	static enum LegacyIndices implements CcpJsonFieldName{
		/** The {@code recruiters} index. */
		recruiters,
		/** The {@code old_recruiters} index. */
		old_recruiters,
		/** How long each scroll context of the reading stays open. */
		SCROLL_TIME { public String getValue() { return "1m"; } },
	}

	/** The entities of other cost centers whose writes this entity listens to, by name (vis does not import them). */
	static enum ListenedEntities implements CcpJsonFieldName{
		/** The sign-up answers, of js. */
		js_login_answers,
	}

	/**
	 * The first load: the e-mail of every document of the legacy indices {@code recruiters} and {@code old_recruiters}
	 * (the id of the document), normalized by {@link VisBusinessPrepareRecruiter}, once each (an e-mail in both indices
	 * is written once), with the source {@code legacy}. An e-mail that is not a valid address is left out. Plus the
	 * subscription of {@link VisBusinessSaveRecruiterFromLoginAnswers} to the save of {@code js_login_answers}.
	 * @return the seed records
	 */
	public List<CcpBulkItem> getFirstRecordsToInsert() {
		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		CcpQueryOptions query = CcpQueryOptions.INSTANCE.matchAll();
		String recruitersIndex = LegacyIndices.recruiters.getValue();
		String oldRecruitersIndex = LegacyIndices.old_recruiters.getValue();
		String[] legacyIndices = {recruitersIndex, oldRecruitersIndex};
		String scrollTime = LegacyIndices.SCROLL_TIME.getValue();
		String idField = VisJsonCommonsFields.id.getValue();

		Set<String> legacyEmails = new LinkedHashSet<>();
		Consumer<CcpJsonRepresentation> consumer = json -> {
			String legacyEmail = json.getAsString(VisJsonCommonsFields.id);
			legacyEmails.add(legacyEmail);
		};
		queryExecutor.consumeQueryResult(query, legacyIndices, scrollTime, 10000, consumer, idField);

		// by the normalized e-mail: two legacy ids that differ only in accents or capital letters are one recruiter
		Map<String, CcpJsonRepresentation> recruitersByEmail = new LinkedHashMap<>();
		String legacySource = Sources.legacy.getValue();
		for (String legacyEmail : legacyEmails) {
			CcpJsonRepresentation recruiterEmail = CcpOtherConstants.EMPTY_JSON.put(Fields.email, legacyEmail);
			CcpJsonRepresentation recruiterInput = recruiterEmail.put(Fields.source, legacySource);
			try {
				CcpJsonRepresentation recruiter = VisBusinessPrepareRecruiter.INSTANCE.execute(recruiterInput);
				String normalizedEmail = recruiter.getAsString(Fields.email);
				recruitersByEmail.put(normalizedEmail, recruiter);
			} catch (CcpJsonValidationError e) {
				// not a valid address: left out of the load
			}
		}
		Collection<CcpJsonRepresentation> recruiters = recruitersByEmail.values();
		CcpJsonRepresentation[] recruitersArray = recruiters.toArray(CcpJsonRepresentation[]::new);
		List<CcpBulkItem> legacyRecruiters = CcpEntityConfigurator.super.toCreateBulkItems(ENTITY, recruitersArray);
		List<CcpBulkItem> loginAnswersSubscription = JnEntityWriteEventSubscribers.getSeeds(ListenedEntities.js_login_answers, CcpEntityDecoratorOperationType.save, VisBusinessSaveRecruiterFromLoginAnswers.class);
		List<CcpBulkItem> firstRecords = new ArrayList<>(legacyRecruiters);
		firstRecords.addAll(loginAnswersSubscription);
		return firstRecords;
	}
}
