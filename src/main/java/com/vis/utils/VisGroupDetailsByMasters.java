package com.vis.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;

/**
 * Consumer of a stream of records that groups them by a master field (e.g. the recruiter's or the
 * candidate's e-mail), accumulating the records in memory to later save them paginated in bulk. Receives
 * the source and target entities of the grouping in the constructor.
 */
public class VisGroupDetailsByMasters implements Consumer<CcpJsonRepresentation>{
	
	
	/** The records read so far, by entity and master. */
	private CcpJsonRepresentation groupedRecords = CcpOtherConstants.EMPTY_JSON;

	/** The master field. */
	private final String masterFieldName;

	/**
	 * Maps the source entity to the grouping entity, and the twin of the source to the twin of the grouping.
	 * @param masterFieldName the master field
	 * @param entity the source entity
	 * @param entityGrouper the grouping entity
	 */
	public VisGroupDetailsByMasters(String masterFieldName, CcpEntity entity , CcpEntity entityGrouper) {
		this.masterFieldName = masterFieldName;
		
		CcpEntity mirrorEntityGrouper = entityGrouper.getTwinEntity();
		CcpEntity mirrorEntity = entity.getTwinEntity();
		CcpEntityMetaData mirrorEntityMetaData = mirrorEntity.getEntityMetaData();

		String mirrorEntityName = mirrorEntityMetaData.entityName;
		CcpEntityMetaData sourceEntityMetaData = entity.getEntityMetaData();
		String entityName = sourceEntityMetaData.entityName;
		CcpFieldName entityNameField = new CcpFieldName(entityName);
		CcpJsonRepresentation mappersWithEntity = CcpOtherConstants.EMPTY_JSON
					.put(entityNameField, entityGrouper);
					CcpFieldName mirrorEntityNameField = new CcpFieldName(mirrorEntityName);

					this.mappers = mappersWithEntity
					.put(mirrorEntityNameField, mirrorEntityGrouper)
					;
	}

	/**
	 * Adds the record to the group of its entity and master.
	 * @param record the record, with its {@code entity}
	 */
	public void accept(CcpJsonRepresentation record) {
		CcpFieldName masterField = new CcpFieldName(this.masterFieldName);
		String master = record.getAsString(masterField);
		String entity = record.getAsString(JnJsonCommonsFields.entity);
		CcpFieldName entityField = new CcpFieldName(entity);
		CcpJsonRepresentation entityGroup = this.groupedRecords.getInnerJson(entityField);
		CcpFieldName masterKey = new CcpFieldName(master);
		entityGroup = entityGroup.addToList(masterKey, record);
		CcpFieldName sameEntityField = new CcpFieldName(entity);
		this.groupedRecords = this.groupedRecords.put(sameEntityField, entityGroup);
	}
	
	/** The grouping entity of each source entity name. */
	private CcpJsonRepresentation mappers;
	
	/**
	 * Saves, in one bulk, the records of each master in pages of the matching grouping entity.
	 * @return this instance
	 */
	public VisGroupDetailsByMasters saveAllDetailsGroupedByMasters(){
		
		Set<String> entities = this.groupedRecords.fieldSet();

		List<CcpBulkItem> result = new ArrayList<>();
		
		for (String entity : entities) {
			CcpFieldName entityKey = new CcpFieldName(entity);
		
			CcpEntity entityGroupToSaveRecords =  this.mappers.getAsObject(entityKey);
			CcpFieldName sameEntityKey = new CcpFieldName(entity);

			CcpJsonRepresentation mastersInThisGrouping = this.groupedRecords.getInnerJson(sameEntityKey);
			
			Set<String> masters = mastersInThisGrouping.fieldSet();

			for (String master : masters) {
				CcpFieldName masterRecordsKey = new CcpFieldName(master);
				List<CcpJsonRepresentation> records = mastersInThisGrouping.getAsJsonList(masterRecordsKey);
				CcpFieldName masterFieldForKey = new CcpFieldName(this.masterFieldName);
				CcpJsonRepresentation primaryKeySupplier = CcpOtherConstants.EMPTY_JSON.put(masterFieldForKey, master);
				List<CcpBulkItem> recordsInPages = VisUtils.getRecordsInPages(records, primaryKeySupplier, entityGroupToSaveRecords);
				result.addAll(recordsInPages);
			}
		}
		JnExecuteBulkOperation.INSTANCE.executeBulk(result, JnDeleteKeysFromCache.INSTANCE);
		return this;
	}
}
