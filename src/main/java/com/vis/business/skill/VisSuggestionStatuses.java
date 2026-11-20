package com.vis.business.skill;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonFieldName;
import com.ccp.decorators.CcpJsonRepresentation;

/**
 * The statuses of a suggestion of the candidate, the keys of the counters of {@code VisEntityVirtualSuggestionStatusCounter}
 * (the slices "Pendente", "Aprovado" and "Rejeitado" of the charts of the screen Curiosities).
 */
public enum VisSuggestionStatuses implements CcpJsonFieldName{
	/** Waiting for the review of the support. */
	pending,
	/** Approved by the support. */
	approved,
	/** Rejected by the support. */
	rejected,
	;

	/**
	 * The change of a suggestion that enters this status.
	 * @return {@code {this: 1}}
	 */
	public CcpJsonRepresentation enter() {
		CcpJsonRepresentation deltas = CcpOtherConstants.EMPTY_JSON.put(this, 1);
		return deltas;
	}

	/**
	 * The change of a suggestion that leaves this status and is gone.
	 * @return {@code {this: -1}}
	 */
	public CcpJsonRepresentation leave() {
		CcpJsonRepresentation deltas = CcpOtherConstants.EMPTY_JSON.put(this, -1);
		return deltas;
	}

	/**
	 * The change of a suggestion that leaves this status for another one.
	 * @param target the status the suggestion enters
	 * @return {@code {this: -1, target: 1}}
	 */
	public CcpJsonRepresentation moveTo(VisSuggestionStatuses target) {
		CcpJsonRepresentation leaving = this.leave();
		CcpJsonRepresentation deltas = leaving.put(target, 1);
		return deltas;
	}
}
