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
 * Consumidor de stream de registros que os agrupa por um campo-master (ex: e-mail do recrutador ou do
 * candidato), acumulando os registros em memória para depois salvá-los paginados em bulk. Recebe no
 * construtor as entidades de origem e de destino do agrupamento.
 */
public class VisGroupDetailsByMasters implements Consumer<CcpJsonRepresentation>{
	
	
	private CcpJsonRepresentation groupedRecords = CcpOtherConstants.EMPTY_JSON;

	private final String masterFieldName;

	public VisGroupDetailsByMasters(String masterFieldName, CcpEntity entity , CcpEntity entityGrouper) {
		this.masterFieldName = masterFieldName;
		
		CcpEntity mirrorEntityGrouper = entityGrouper.getTwinEntity();
		CcpEntity mirrorEntity = entity.getTwinEntity();
		CcpEntityMetaData entityMetaData = mirrorEntity.getEntityMetaData();

		String mirrorEntityName = entityMetaData.entityName;
		CcpEntityMetaData entityMetaData2 = entity.getEntityMetaData();
		String entityName = entityMetaData2.entityName;
		CcpFieldName ccpFieldName = new CcpFieldName(entityName);
		CcpJsonRepresentation put = CcpOtherConstants.EMPTY_JSON
					.put(ccpFieldName, entityGrouper);
					CcpFieldName ccpFieldName2 = new CcpFieldName(mirrorEntityName);

					this.mappers = put
					.put(ccpFieldName2, mirrorEntityGrouper)
					;
	}

	public void accept(CcpJsonRepresentation record) {
		CcpFieldName ccpFieldName3 = new CcpFieldName(this.masterFieldName);
		String master = record.getAsString(ccpFieldName3);
		String entity = record.getAsString(JnJsonCommonsFields.entity);
		CcpFieldName ccpFieldName4 = new CcpFieldName(entity);
		CcpJsonRepresentation entityGroup = this.groupedRecords.getInnerJson(ccpFieldName4);
		CcpFieldName ccpFieldName5 = new CcpFieldName(master);
		entityGroup = entityGroup.addToList(ccpFieldName5, record);
		CcpFieldName ccpFieldName6 = new CcpFieldName(entity);
		this.groupedRecords = this.groupedRecords.put(ccpFieldName6, entityGroup);
	}
	
	private CcpJsonRepresentation mappers;
	
	public VisGroupDetailsByMasters saveAllDetailsGroupedByMasters(){
		
		Set<String> entities = this.groupedRecords.fieldSet();

		List<CcpBulkItem> result = new ArrayList<>();
		
		for (String entity : entities) {
			CcpFieldName ccpFieldName7 = new CcpFieldName(entity);
		
			CcpEntity entityGroupToSaveRecords =  this.mappers.getAsObject(ccpFieldName7);
			CcpFieldName ccpFieldName8 = new CcpFieldName(entity);

			CcpJsonRepresentation mastersInThisGrouping = this.groupedRecords.getInnerJson(ccpFieldName8);
			
			Set<String> masters = mastersInThisGrouping.fieldSet();

			for (String master : masters) {
				CcpFieldName ccpFieldName9 = new CcpFieldName(master);
				List<CcpJsonRepresentation> records = mastersInThisGrouping.getAsJsonList(ccpFieldName9);
				CcpFieldName ccpFieldName10 = new CcpFieldName(this.masterFieldName);
				CcpJsonRepresentation primaryKeySupplier = CcpOtherConstants.EMPTY_JSON.put(ccpFieldName10, master);
				List<CcpBulkItem> recordsInPages = VisUtils.getRecordsInPages(records, primaryKeySupplier, entityGroupToSaveRecords);
				result.addAll(recordsInPages);
			}
		}
		JnExecuteBulkOperation.INSTANCE.executeBulk(result, JnDeleteKeysFromCache.INSTANCE);
		return this;
	}
}
