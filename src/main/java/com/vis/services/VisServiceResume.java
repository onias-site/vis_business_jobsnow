package com.vis.services;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.services.JnService;
import com.vis.entities.VisEntityResume;


/**
 * Resume data access service. Exposes the CRUD operations on the VisEntityResume entity.
 */
public enum VisServiceResume implements JnService {
	/** Toggles the resume between active and inactive: deleting it moves it to the twin (inactive). */
	ChangeStatus{
		/**
		 * Deletes (deactivates) the resume.
		 * @param json the resume key
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisEntityResume.ENTITY.delete(json);

			return  json;
		}
	},
	/** Deletes the resume (moves it to the twin, like {@link #ChangeStatus}). */
	Delete{
		/**
		 * Deletes the resume.
		 * @param sessionValues the resume key, from the session
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation sessionValues) {
			VisEntityResume.ENTITY.delete(sessionValues);

			return sessionValues;
		}
	},
	/** Returns the resume, wherever it is. */
	GetData{
		/**
		 * Reads the resume from every associated entity.
		 * @param json the resume key
		 * @return the records by entity
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			CcpJsonRepresentation resumeData = VisEntityResume.ENTITY.getOneByIdAnyWhere(json);
			
			return resumeData;
		}
	},
	/** Saves the resume. */
	Save{
		/**
		 * Saves the resume.
		 * @param sessionValues the resume, with the session values
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation sessionValues) {
			
			VisEntityResume.ENTITY.save(sessionValues);

			return sessionValues;
		}
		
		/**
		 * Validates the input with the fields of the resume entity.
		 * @return the validation class
		 */
		public Class<?> getJsonValidationClass() {
			return VisEntityResume.Fields.class;
		}
	}, 
	;
}
