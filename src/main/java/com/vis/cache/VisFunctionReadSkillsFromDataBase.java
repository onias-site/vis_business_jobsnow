package com.vis.cache;

import java.util.List;
import java.util.function.Function;

import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.dependency.injection.CcpDependencyInjection;
import com.ccp.especifications.db.query.CcpQueryOptions;
import com.ccp.especifications.db.query.CcpQueryExecutor;
import com.vis.entities.VisEntitySkill;

public class VisFunctionReadSkillsFromDataBase implements Function<CcpJsonRepresentation, List<CcpJsonRepresentation>>{
	private VisFunctionReadSkillsFromDataBase() {}
	
	public static final VisFunctionReadSkillsFromDataBase INSTANCE = new VisFunctionReadSkillsFromDataBase();
	
	public List<CcpJsonRepresentation> apply(CcpJsonRepresentation t) {
		CcpQueryExecutor queryExecutor = CcpDependencyInjection.getDependency(CcpQueryExecutor.class);
		CcpQueryOptions query = CcpQueryOptions.INSTANCE.addDescSorting(VisEntitySkill.Fields.ranking.name()).maxResults();
		String[] resourcesNames = VisEntitySkill.ENTITY.getEntitiesToSelect();
		List<CcpJsonRepresentation> list = queryExecutor.getResultAsList(
				query, 
				resourcesNames,  
				VisEntitySkill.Fields.parent.name(), 
				VisEntitySkill.Fields.synonym.name(), 
				VisEntitySkill.Fields.skill.name()
				);
		return list;
	}

}
