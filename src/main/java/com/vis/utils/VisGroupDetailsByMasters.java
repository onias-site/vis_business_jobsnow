package com.vis.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.ccp.constantes.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.CcpEntity;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.utils.JnDeleteKeysFromCache;

/**
 * Consumidor de stream de registros que os agrupa por um campo-master (ex: e-mail do recrutador ou do
 * candidato), acumulando os registros em memória para depois salvá-los paginados em bulk. Recebe no
 * construtor as entidades de origem e de destino do agrupamento.
 */
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

		String mirrorEntityName = mirrorEntity.getEntityMetaData().entityName;
		String entityName = entity.getEntityMetaData().entityName;
		
		this.mappers = CcpOtherConstants.EMPTY_JSON
					.put(new CcpFieldName(entityName), entityGrouper)
					.put(new CcpFieldName(mirrorEntityName), mirrorEntityGrouper)
					;
	}

	public void accept(CcpJsonRepresentation record) {
		String master = record.getAsString(new CcpFieldName(this.masterFieldName));
		String entity = record.getAsString(JsonFieldNames.entity);
		CcpJsonRepresentation entityGroup = this.groupedRecords.getInnerJson(new CcpFieldName(entity));
		entityGroup = entityGroup.addToList(new CcpFieldName(master), record);
		this.groupedRecords = this.groupedRecords.put(new CcpFieldName(entity), entityGroup);
	}
	
	private CcpJsonRepresentation mappers;
	
	public VisGroupDetailsByMasters saveAllDetailsGroupedByMasters(){
		
		Set<String> entities = this.groupedRecords.fieldSet();

		List<CcpBulkItem> result = new ArrayList<>();
		
		for (String entity : entities) {
			
			CcpEntity entityGroupToSaveRecords =  this.mappers.getAsObject(new CcpFieldName(entity));
			
			CcpJsonRepresentation mastersInThisGrouping = this.groupedRecords.getInnerJson(new CcpFieldName(entity));
			
			Set<String> masters = mastersInThisGrouping.fieldSet();

			for (String master : masters) {
				List<CcpJsonRepresentation> records = mastersInThisGrouping.getAsJsonList(new CcpFieldName(master));
				CcpJsonRepresentation primaryKeySupplier = CcpOtherConstants.EMPTY_JSON.put(new CcpFieldName(this.masterFieldName), master);
				List<CcpBulkItem> recordsInPages = VisUtils.getRecordsInPages(records, primaryKeySupplier, entityGroupToSaveRecords);
				result.addAll(recordsInPages);
			}
		}
		JnExecuteBulkOperation.INSTANCE.executeBulk(result, JnDeleteKeysFromCache.INSTANCE);
		return this;
	}
}
