package com.vis.business.statistics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpFieldName;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;
import com.ccp.especifications.db.utils.entity.decorators.engine.CcpEntityMetaData;
import com.ccp.business.CcpBusiness;
import com.ccp.json.validations.fields.annotations.CcpJsonCopyFieldValidationsFrom;
import com.ccp.json.validations.fields.annotations.CcpJsonFieldValidatorRequired;
import com.jn.utils.JnGroupers;
import com.vis.entities.VisEntityResume;
import com.vis.entities.VisEntityVirtualDddGrouper;
import com.vis.json.fields.validation.VisJsonCommonsFields;
import com.vis.json.transformers.VisSeniorityType;

/**
 * The statistics of the active resumes of a region (a ddd, or home office): how many resumes, the averages of the CLT and
 * PJ salary expectations by seniority and of all of them, and the slices of each pie chart of
 * {@link VisResumeStatisticsCharts}. One read by id (OQL): the group of the ddd in {@code jn_grouper}
 * ({@code VisEntityVirtualDddGrouper}), whose children carry the fields of each resume. An average counts only the
 * resumes that informed the salary, and is 0 when none did.
 */
public class VisBusinessResumeStatisticsOfTheRegion implements CcpBusiness {

	/** The input and the output of this business, with the validation rules of the input. */
	public static enum Fields implements CcpJsonFieldName {
		/** The region: one of the ddds of the resume, home office (10) included. */
		@CcpJsonCopyFieldValidationsFrom(VisJsonCommonsFields.class)
		@CcpJsonFieldValidatorRequired
		ddd,

		/** How many active resumes the region has. */
		total,

		/** The averages, by seniority and of {@code all}: {@code resumes}, {@code clt} and {@code pj}. */
		averages,

		/** The slices of each pie chart: {chart: {key: resumes}}. */
		charts,

		/** The averages of every seniority together. */
		all,

		/** How many resumes an average refers to. */
		resumes,

		/** The average of the CLT salary expectation. */
		clt,

		/** The average of the PJ salary expectation. */
		pj,
	}

	/** The single instance. */
	public static final VisBusinessResumeStatisticsOfTheRegion INSTANCE = new VisBusinessResumeStatisticsOfTheRegion();

	private VisBusinessResumeStatisticsOfTheRegion() {}

	public Class<?> getJsonValidationClass() {
		return Fields.class;
	}

	/**
	 * Reads the resumes of the region and calculates the statistics.
	 * @param json the region
	 * @return {@code ddd}, {@code total}, {@code averages} and {@code charts}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
		Integer ddd = json.getAsIntegerNumber(Fields.ddd);
		List<String> children = getResumesOfTheRegion(ddd);
		Map<String, Map<String, Long>> slicesByChart = new LinkedHashMap<>();
		Map<String, Map<String, Double>> sumsBySeniority = new LinkedHashMap<>();
		Map<String, Map<String, Long>> countsBySeniority = new LinkedHashMap<>();
		for (String child : children) {
			CcpJsonRepresentation resume = new CcpJsonRepresentation(child);
			addToTheCharts(resume, slicesByChart);
			addToTheAverages(resume, sumsBySeniority, countsBySeniority);
		}
		CcpJsonRepresentation averages = getAverages(sumsBySeniority, countsBySeniority);
		CcpJsonRepresentation charts = toJson(slicesByChart);
		int total = children.size();
		CcpJsonRepresentation response = CcpOtherConstants.EMPTY_JSON
				.put(Fields.ddd, ddd)
				.put(Fields.total, total)
				.put(Fields.averages, averages)
				.put(Fields.charts, charts);
		return response;
	}

	/**
	 * The children of the group of the ddd: one JSON text per active resume.
	 * @param ddd the region
	 * @return the children, empty when the region has no resume
	 */
	private static List<String> getResumesOfTheRegion(Integer ddd) {
		CcpJsonRepresentation combination = CcpOtherConstants.EMPTY_JSON.put(VisEntityVirtualDddGrouper.Fields.ddd, ddd);
		String parentId = VisEntityVirtualDddGrouper.ENTITY.calculateId(combination);
		CcpEntityMetaData parentDetails = VisEntityVirtualDddGrouper.ENTITY.getEntityMetaData();
		CcpEntityMetaData resumeDetails = VisEntityResume.ENTITY.getEntityMetaData();
		List<String> children = JnGroupers.getChildren(false, parentDetails.entityName, resumeDetails.entityName, parentId);
		return children;
	}

	/**
	 * Counts the resume in a slice of each chart.
	 * @param resume the child, parsed
	 * @param slicesByChart receives the count of each slice, by chart
	 */
	private static void addToTheCharts(CcpJsonRepresentation resume, Map<String, Map<String, Long>> slicesByChart) {
		VisResumeStatisticsCharts[] charts = VisResumeStatisticsCharts.values();
		for (VisResumeStatisticsCharts chart : charts) {
			String chartName = chart.name();
			Map<String, Long> slices = slicesByChart.computeIfAbsent(chartName, name -> new LinkedHashMap<>());
			List<String> keys = chart.getKeys(resume);
			for (String key : keys) {
				slices.merge(key, 1L, Long::sum);
			}
		}
	}

	/**
	 * Adds the resume to the averages of its seniority and of all of them.
	 * @param resume the child, parsed
	 * @param sumsBySeniority receives the sums of the salaries, by seniority
	 * @param countsBySeniority receives the counts of resumes and of informed salaries, by seniority
	 */
	private static void addToTheAverages(CcpJsonRepresentation resume, Map<String, Map<String, Double>> sumsBySeniority, Map<String, Map<String, Long>> countsBySeniority) {
		VisSeniorityType seniority = VisResumeStatisticsCharts.getSeniority(resume);
		String seniorityName = seniority.name();
		String allName = Fields.all.name();
		List<String> groups = List.of(seniorityName, allName);
		double clt = VisResumeStatisticsCharts.getSalary(resume, VisEntityResume.Fields.clt);
		double pj = VisResumeStatisticsCharts.getSalary(resume, VisEntityResume.Fields.pj);
		String resumesName = Fields.resumes.name();
		for (String group : groups) {
			Map<String, Double> sums = sumsBySeniority.computeIfAbsent(group, name -> new LinkedHashMap<>());
			Map<String, Long> counts = countsBySeniority.computeIfAbsent(group, name -> new LinkedHashMap<>());
			counts.merge(resumesName, 1L, Long::sum);
			addSalary(Fields.clt, clt, sums, counts);
			addSalary(Fields.pj, pj, sums, counts);
		}
	}

	/**
	 * Adds an informed salary to its sum and count.
	 * @param field {@code clt} or {@code pj}
	 * @param salary the salary, 0 when not informed
	 * @param sums the sums of the group
	 * @param counts the counts of the group
	 */
	private static void addSalary(Fields field, double salary, Map<String, Double> sums, Map<String, Long> counts) {
		boolean notInformed = salary <= 0d;

		if(notInformed) {
			return;
		}

		String fieldName = field.name();
		sums.merge(fieldName, salary, Double::sum);
		counts.merge(fieldName, 1L, Long::sum);
	}

	/**
	 * The averages of every seniority (each one, even with no resume) and of all of them.
	 * @param sumsBySeniority the sums of the salaries, by seniority
	 * @param countsBySeniority the counts of resumes and of informed salaries, by seniority
	 * @return {seniority: {resumes, clt, pj}}, with {@code all} last
	 */
	private static CcpJsonRepresentation getAverages(Map<String, Map<String, Double>> sumsBySeniority, Map<String, Map<String, Long>> countsBySeniority) {
		CcpJsonRepresentation averages = CcpOtherConstants.EMPTY_JSON;
		VisSeniorityType[] seniorities = VisSeniorityType.values();
		for (VisSeniorityType seniority : seniorities) {
			boolean neverCounted = VisSeniorityType.unknown == seniority;

			if(neverCounted) {
				continue;
			}

			String seniorityName = seniority.name();
			averages = putTheAverage(averages, seniorityName, sumsBySeniority, countsBySeniority);
		}
		String allName = Fields.all.name();
		CcpJsonRepresentation averagesWithAll = putTheAverage(averages, allName, sumsBySeniority, countsBySeniority);
		return averagesWithAll;
	}

	private static CcpJsonRepresentation putTheAverage(CcpJsonRepresentation averages, String group, Map<String, Map<String, Double>> sumsBySeniority, Map<String, Map<String, Long>> countsBySeniority) {
		Map<String, Double> sums = sumsBySeniority.getOrDefault(group, Map.of());
		Map<String, Long> counts = countsBySeniority.getOrDefault(group, Map.of());
		String resumesName = Fields.resumes.name();
		long resumes = counts.getOrDefault(resumesName, 0L);
		long cltAverage = getAverage(Fields.clt, sums, counts);
		long pjAverage = getAverage(Fields.pj, sums, counts);
		CcpJsonRepresentation average = CcpOtherConstants.EMPTY_JSON
				.put(Fields.resumes, resumes)
				.put(Fields.clt, cltAverage)
				.put(Fields.pj, pjAverage);
		CcpFieldName groupField = new CcpFieldName(group);
		CcpJsonRepresentation averagesWithGroup = averages.put(groupField, average);
		return averagesWithGroup;
	}

	private static long getAverage(Fields field, Map<String, Double> sums, Map<String, Long> counts) {
		String fieldName = field.name();
		long informed = counts.getOrDefault(fieldName, 0L);
		boolean noneInformed = informed == 0L;

		if(noneInformed) {
			return 0L;
		}

		double sum = sums.get(fieldName);
		long average = Math.round(sum / informed);
		return average;
	}

	/**
	 * The slices of each chart as JSON.
	 * @param slicesByChart the count of each slice, by chart
	 * @return {chart: {key: resumes}}, every chart present
	 */
	private static CcpJsonRepresentation toJson(Map<String, Map<String, Long>> slicesByChart) {
		CcpJsonRepresentation charts = CcpOtherConstants.EMPTY_JSON;
		VisResumeStatisticsCharts[] chartTypes = VisResumeStatisticsCharts.values();
		for (VisResumeStatisticsCharts chartType : chartTypes) {
			String chartName = chartType.name();
			Map<String, Long> slices = slicesByChart.getOrDefault(chartName, Map.of());
			CcpJsonRepresentation chart = CcpOtherConstants.EMPTY_JSON;
			Set<String> keys = slices.keySet();
			for (String key : keys) {
				Long count = slices.get(key);
				CcpFieldName keyField = new CcpFieldName(key);
				chart = chart.put(keyField, count);
			}
			charts = charts.put(chartType, chart);
		}
		return charts;
	}
}
