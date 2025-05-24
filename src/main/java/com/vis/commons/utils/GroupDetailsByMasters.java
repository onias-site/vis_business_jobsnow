package com.vis.commons.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.CcpEntity;
import com.jn.db.bulk.JnExecuteBulkOperation;

public class GroupDetailsByMasters implements Consumer<CcpJsonRepresentation>{
	
	private CcpJsonRepresentation groupedRecords = CcpOtherConstants.EMPTY_JSON;

	private final String masterFieldName;

	public GroupDetailsByMasters(String masterFieldName, CcpEntity entity , CcpEntity entityGrouper) {
		this.masterFieldName = masterFieldName;
		
		CcpEntity mirrorEntityGrouper = entityGrouper.getTwinEntity();
		CcpEntity mirrorEntity = entity.getTwinEntity();

		String mirrorEntityName = mirrorEntity.getEntityName();
		String entityName = entity.getEntityName();
		
		this.mappers = CcpOtherConstants.EMPTY_JSON
					.put(entityName, entityGrouper)
					.put(mirrorEntityName, mirrorEntityGrouper)
					;
	}

	public void accept(CcpJsonRepresentation record) {
		String master = record.getAsString(this.masterFieldName);
		String entity = record.getAsString("entity");
		CcpJsonRepresentation entityGroup = this.groupedRecords.getInnerJson(entity);
		entityGroup = entityGroup.addToList(master, record);
		this.groupedRecords = this.groupedRecords.put(entity, entityGroup);
	}
	
	private CcpJsonRepresentation mappers;
	
	public GroupDetailsByMasters saveAllDetailsGroupedByMasters(){
		
		Set<String> entities = this.groupedRecords.fieldSet();

		List<CcpBulkItem> result = new ArrayList<>();
		
		for (String entity : entities) {
			
			CcpEntity entityGroupToSaveRecords =  this.mappers.getAsObject(entity);
			
			CcpJsonRepresentation mastersInThisGrouping = this.groupedRecords.getInnerJson(entity);
			
			Set<String> masters = mastersInThisGrouping.fieldSet();

			for (String master : masters) {
				List<CcpJsonRepresentation> records = mastersInThisGrouping.getAsJsonList(master);
				CcpJsonRepresentation primaryKeySupplier = CcpOtherConstants.EMPTY_JSON.put(this.masterFieldName, master);
				List<CcpBulkItem> recordsInPages = VisAsyncUtils.getRecordsInPages(records, primaryKeySupplier, entityGroupToSaveRecords);
				result.addAll(recordsInPages);
			}
		}
		JnExecuteBulkOperation.INSTANCE.executeBulk(result);
		return this;
	}
}
