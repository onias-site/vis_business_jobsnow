package com.vis.business.telegram;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.bulk.CcpBulkEntityOperationType;
import com.ccp.especifications.db.bulk.CcpBulkItem;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.jn.db.bulk.JnExecuteBulkOperation;
import com.jn.entities.decorators.engine.JnEntityGrouperParents;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnDeleteKeysFromCache;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityStatisSkill;
import com.vis.entities.VisEntityStatisSkillLineChart;
import com.vis.entities.VisEntityStatisSkillPieChart;
import com.vis.entities.VisEntityTelegramPosition;

/**
 * Calculates again, from the index of the positions of Telegram, everything the screen of statistics reads by id: the
 * points of {@link VisEntityStatisSkillLineChart} and their group of positions, the pie charts of
 * {@link VisEntityStatisSkillPieChart} (each position counted once, whatever its skills) and the groups of JSON texts of
 * {@link VisEntityStatisSkill} (the months of a skill) and of the pie chart (the skills of a month). The writes go by bulk,
 * which the decorators of the groups do not see, so the groups are written here, each one replaced as a whole; a bulk
 * keeps a single item per document, so each group goes with all its children.
 *
 * <p>It is a batch process run by hand after the load, not an online request.
 *
 * <p>Output: {@code positionsRead}, {@code lineCharts}, {@code statisSkills} and {@code pieCharts}.
 */
public class VisBusinessRecalculateSkillStatistics implements CcpBusiness {

	/** How many items go in each bulk. */
	private static final int BATCH_SIZE = 1_000;

	/** The single instance: the business keeps no state. */
	public static final VisBusinessRecalculateSkillStatistics INSTANCE = new VisBusinessRecalculateSkillStatistics();

	private VisBusinessRecalculateSkillStatistics() {}

	/**
	 * Recalculates the statistics.
	 * @param json not read
	 * @return the input and the counters
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		JnEntityGrouperParents lineChartsOfThePosition = new JnEntityGrouperParents(VisEntityStatisSkillLineChart.class, VisEntityTelegramPosition.class);
		JnEntityGrouperParents statisSkillOfTheLineChart = new JnEntityGrouperParents(VisEntityStatisSkill.class, VisEntityStatisSkillLineChart.class);
		JnEntityGrouperParents pieChartOfTheLineChart = new JnEntityGrouperParents(VisEntityStatisSkillPieChart.class, VisEntityStatisSkillLineChart.class);

		List<CcpJsonRepresentation> positions = VisTelegramPositionsIndex.readAll();
		Map<String, CcpJsonRepresentation> lineCharts = new LinkedHashMap<>();
		Map<String, List<String>> positionsByLineChart = new LinkedHashMap<>();
		Map<String, CcpJsonRepresentation> pieCharts = new LinkedHashMap<>();
		Map<String, Integer> totalByPieChart = new LinkedHashMap<>();
		for (CcpJsonRepresentation position : positions) {
			String positionId = position.getAsString(JnJsonCommonsFields.id);
			Map<String, CcpJsonRepresentation> lineChartsFound = lineChartsOfThePosition.getParents(position);
			lineCharts.putAll(lineChartsFound);
			Set<String> lineChartIds = lineChartsFound.keySet();
			for (String lineChartId : lineChartIds) {
				positionsByLineChart.computeIfAbsent(lineChartId, x -> new ArrayList<>()).add(positionId);
			}
			Map<String, CcpJsonRepresentation> pieChartsFound = pieChartOfTheLineChart.getParents(position);
			pieCharts.putAll(pieChartsFound);
			Set<String> pieChartIds = pieChartsFound.keySet();
			for (String pieChartId : pieChartIds) {
				totalByPieChart.merge(pieChartId, 1, Integer::sum);
			}
		}

		Map<String, CcpJsonRepresentation> statisSkills = new LinkedHashMap<>();
		Map<String, List<String>> monthsByStatisSkill = new LinkedHashMap<>();
		Map<String, List<String>> skillsByPieChart = new LinkedHashMap<>();
		List<CcpBulkItem> items = new ArrayList<>();
		Set<String> lineChartIds = lineCharts.keySet();
		for (String lineChartId : lineChartIds) {
			List<String> positionsOfTheLineChart = positionsByLineChart.get(lineChartId);
			int total = positionsOfTheLineChart.size();
			CcpJsonRepresentation combination = lineCharts.get(lineChartId);
			CcpJsonRepresentation lineChart = combination.put(VisEntityStatisSkillLineChart.Fields.total, total);
			List<CcpBulkItem> itemsOfTheLineChart = VisEntityStatisSkillLineChart.ENTITY.toBulkItems(lineChart, CcpBulkEntityOperationType.create);
			items.addAll(itemsOfTheLineChart);

			Map<String, CcpJsonRepresentation> statisSkillsFound = statisSkillOfTheLineChart.getParents(lineChart);
			statisSkills.putAll(statisSkillsFound);
			CcpJsonRepresentation month = lineChart.getJsonPiece(VisEntityStatisSkillLineChart.Fields.yearMonth, VisEntityStatisSkillLineChart.Fields.total);
			addChild(monthsByStatisSkill, statisSkillsFound, month);

			Map<String, CcpJsonRepresentation> pieChartsFound = pieChartOfTheLineChart.getParents(lineChart);
			CcpJsonRepresentation skill = lineChart.getJsonPiece(VisEntityStatisSkillLineChart.Fields.skill, VisEntityStatisSkillLineChart.Fields.total);
			addChild(skillsByPieChart, pieChartsFound, skill);
		}

		Set<String> pieChartIds = pieCharts.keySet();
		for (String pieChartId : pieChartIds) {
			Integer total = totalByPieChart.get(pieChartId);
			CcpJsonRepresentation combination = pieCharts.get(pieChartId);
			CcpJsonRepresentation pieChart = combination.put(VisEntityStatisSkillPieChart.Fields.total, total);
			List<CcpBulkItem> itemsOfThePieChart = VisEntityStatisSkillPieChart.ENTITY.toBulkItems(pieChart, CcpBulkEntityOperationType.create);
			items.addAll(itemsOfThePieChart);
		}

		CcpEntityMetaData positionDetails = VisEntityTelegramPosition.ENTITY.getEntityMetaData();
		CcpEntityMetaData lineChartDetails = VisEntityStatisSkillLineChart.ENTITY.getEntityMetaData();
		String lineChartGroup = lineChartsOfThePosition.getParentName();
		String statisSkillGroup = statisSkillOfTheLineChart.getParentName();
		String pieChartGroup = pieChartOfTheLineChart.getParentName();
		items.addAll(JnGroupers.getItemsToCreateGroups(lineChartGroup, positionDetails.entityName, lineCharts, positionsByLineChart));
		items.addAll(JnGroupers.getItemsToCreateGroups(statisSkillGroup, lineChartDetails.entityName, statisSkills, monthsByStatisSkill));
		items.addAll(JnGroupers.getItemsToCreateGroups(pieChartGroup, lineChartDetails.entityName, pieCharts, skillsByPieChart));

		writeInBatches(items);

		int positionsCount = positions.size();
		int lineChartsCount = lineCharts.size();
		int statisSkillsCount = statisSkills.size();
		int pieChartsCount = pieCharts.size();
		CcpJsonRepresentation jsonWithPositions = json.put(VisTelegramLoadFields.positionsRead, positionsCount);
		CcpJsonRepresentation jsonWithLineCharts = jsonWithPositions.put(VisTelegramLoadFields.lineCharts, lineChartsCount);
		CcpJsonRepresentation jsonWithStatisSkills = jsonWithLineCharts.put(VisTelegramLoadFields.statisSkills, statisSkillsCount);
		CcpJsonRepresentation jsonWithPieCharts = jsonWithStatisSkills.put(VisTelegramLoadFields.pieCharts, pieChartsCount);
		return jsonWithPieCharts;
	}

	/**
	 * Adds a JSON text to the children of each parent.
	 * @param childrenByParent the children of each parent
	 * @param parents the parents, by id
	 * @param child the child
	 */
	private static void addChild(Map<String, List<String>> childrenByParent, Map<String, CcpJsonRepresentation> parents, CcpJsonRepresentation child) {
		String childText = child.asUgglyJson();
		Set<String> parentIds = parents.keySet();
		for (String parentId : parentIds) {
			childrenByParent.computeIfAbsent(parentId, x -> new ArrayList<>()).add(childText);
		}
	}

	/**
	 * Writes the items, a batch at a time.
	 * @param items the items
	 */
	private static void writeInBatches(List<CcpBulkItem> items) {
		int itemsCount = items.size();
		for (int start = 0; start < itemsCount; start += BATCH_SIZE) {
			int end = Math.min(start + BATCH_SIZE, itemsCount);
			List<CcpBulkItem> batch = items.subList(start, end);
			JnExecuteBulkOperation.INSTANCE.executeBulk(batch, JnDeleteKeysFromCache.INSTANCE);
		}
	}
}
