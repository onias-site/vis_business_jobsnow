package com.vis.json.transformers;

import java.util.ArrayList;
import java.util.List;

import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;

public enum VisRecordType {
	recordToSave(new Integer[] {1}, new Integer[] {1, 2}){

		public Integer[] fromIntegerField(CcpJsonFieldName field, CcpJsonRepresentation json, int threshold) {
			List<Integer> list = new ArrayList<>();
			
			Integer number = json.getAsIntegerNumber(field);
			
			for(int k = number; k <= threshold; k++) {
				list.add(k);
			}
			
			Integer[] array = list.toArray(new Integer[list.size()]);
			return array;
		}

		public Integer[] fromDoubleField(CcpJsonFieldName field, CcpJsonRepresentation json, double threshold, double increment) {
			
			List<Integer> list = new ArrayList<>();

			Double number = json.getAsDoubleNumber(field) / increment;
			
			double round = Math.ceil(number) * increment;
			
			for(double k = round; k <= threshold; k += increment) {
				list.add((int) k);
			}
			
			Integer[] array = list.toArray(new Integer[list.size()]);
			return array;
		}
		
	},
	recordToSearch(new Integer[] {1, 2}, new Integer[] {1}){

		public Integer[] fromIntegerField(CcpJsonFieldName field, CcpJsonRepresentation json, int threshold) {
			List<Integer> list = new ArrayList<>();
			
			Integer number = json.getAsIntegerNumber(field);
			
			for(int k = number; k >= threshold; k--) {
				list.add(k);
			}
			
			Integer[] array = list.toArray(new Integer[list.size()]);
			return array;
		}

		public Integer[] fromDoubleField(CcpJsonFieldName field, CcpJsonRepresentation json, double threshold, double increment) {
			
			List<Integer> list = new ArrayList<>();

			Double number = json.getAsDoubleNumber(field) / increment;
			
			double round = Math.floor(number) * increment;
			
			for(double k = round; k >= threshold; k -= increment) {
				list.add((int) k);
			}
			
			Integer[] array = list.toArray(new Integer[list.size()]);
			return array;
		}
		
	}
	;
	
	private final Integer[] whenFalse;
	private final Integer[] whenTrue;
	
	private VisRecordType(Integer[] whenFalse, Integer[] whenTrue) {
		this.whenFalse = whenFalse;
		this.whenTrue = whenTrue;
	}



	public Integer[] fromBooleanField(CcpJsonFieldName field, CcpJsonRepresentation json) {
		boolean asBoolean = json.getAsBoolean(field);
		if(asBoolean) {
			return this.whenTrue;
		}
		
		

		return this.whenFalse;
	}
	
	public abstract Integer[] fromIntegerField(CcpJsonFieldName field, CcpJsonRepresentation json, int threshold);

	public abstract Integer[] fromDoubleField(CcpJsonFieldName field, CcpJsonRepresentation json, double threshold, double increment);
	
}
