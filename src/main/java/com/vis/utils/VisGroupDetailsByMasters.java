package com.vis.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.db.bulk.JnExecuteBulkOperation;

public class VisGroupDetailsByMasters implements Consumer<CcpJsonRepresentation>{
	enum JsonFieldNames implements CcpJsonFieldName{
		entity
	}
	
	private CcpJsonRepresentation groupedRecords = CcpOtherConstants.EMPTY_JSON;

	private final String masterFieldName;

	public VisGroupDetailsByMasters(String masterFieldName, CcpEntity entity , CcpEntity entityGrouper) {
		this.masterFieldName = masterFieldName;
		
		CcpEntity mirrorEntityGrouper = entityGrouper.getTwinEntity();
		CcpEntity mirrorEntity = entity.getTwinEntity();

		String mirrorEntityName = mirrorEntity.getEntityName();
		String entityName = entity.getEntityName();
		
		this.mappers = CcpOtherConstants.EMPTY_JSON
					.getDynamicVersion().put(entityName, entityGrouper)
					.getDynamicVersion().put(mirrorEntityName, mirrorEntityGrouper)
					;
	}

	public void accept(CcpJsonRepresentation record) {
		String master = record.getDynamicVersion().getAsString(this.masterFieldName);
		String entity = record.getAsString(JsonFieldNames.entity);
		CcpJsonRepresentation entityGroup = this.groupedRecords.getDynamicVersion().getInnerJson(entity);
		entityGroup = entityGroup.getDynamicVersion().addToList(master, record);
		this.groupedRecords = this.groupedRecords.getDynamicVersion().put(entity, entityGroup);
	}
	
	private CcpJsonRepresentation mappers;
	
	public VisGroupDetailsByMasters saveAllDetailsGroupedByMasters(){
		
		Set<String> entities = this.groupedRecords.fieldSet();

		List<CcpBulkItem> result = new ArrayList<>();
		
		for (String entity : entities) {
			
			CcpEntity entityGroupToSaveRecords =  this.mappers.getDynamicVersion().getAsObject(entity);
			
			CcpJsonRepresentation mastersInThisGrouping = this.groupedRecords.getDynamicVersion().getInnerJson(entity);
			
			Set<String> masters = mastersInThisGrouping.fieldSet();

			for (String master : masters) {
				List<CcpJsonRepresentation> records = mastersInThisGrouping.getDynamicVersion().getAsJsonList(master);
				CcpJsonRepresentation primaryKeySupplier = CcpOtherConstants.EMPTY_JSON.getDynamicVersion().put(this.masterFieldName, master);
				List<CcpBulkItem> recordsInPages = VisUtils.getRecordsInPages(records, primaryKeySupplier, entityGroupToSaveRecords);
				result.addAll(recordsInPages);
			}
		}
		JnExecuteBulkOperation.INSTANCE.executeBulk(result);
		return this;
	}
}
