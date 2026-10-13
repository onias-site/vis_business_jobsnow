package com.vis.services;

import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.mensageria.JnFunctionMensageriaSender;
import com.jn.services.JnService;
import com.vis.business.recruiter.VisBusinessRecruiterReceivingResumes;
import com.vis.entities.VisEntityResumePerception;


/**
 * Recruiter data access service. Exposes the operations related to the recruiter's interactions
 * with resumes and positions.
 */
public enum VisServiceRecruiter implements JnService {
	/** Saves the opinion of the recruiter about a resume. */
	SaveOpinionAboutThisResume{
		/**
		 * Saves the opinion.
		 * @param json the opinion
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisEntityResumePerception.ENTITY.save(json);

			return json;
		}
	},
	/** Sends the resumes to the e-mail of the recruiter, asynchronously. */
	SendResumesToEmail{
		/**
		 * Publishes the sending.
		 * @param json the request
		 * @return the details of the message
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			JnFunctionMensageriaSender mensageriaSender = new JnFunctionMensageriaSender(VisBusinessRecruiterReceivingResumes.INSTANCE);
			CcpJsonRepresentation result = mensageriaSender.execute(json);
			
			return result;
		}
	},
	/** Changes the opinion: deleting it moves it to the twin. */
	ChangeOpinionAboutThisResume{
		/**
		 * Deletes the opinion.
		 * @param json the opinion key
		 * @return the same JSON
		 */
		public CcpJsonRepresentation apply(CcpJsonRepresentation json) {
			VisEntityResumePerception.ENTITY.delete(json);

			return json;
		}
	},
	;





}
