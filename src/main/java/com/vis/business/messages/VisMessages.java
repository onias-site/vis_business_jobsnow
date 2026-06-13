package com.vis.business.messages;

import com.jn.business.messages.JnBusinessSendMessage;
import com.vis.entities.VisEntitySkill;
import com.vis.entities.VisEntitySkillFixHierarchyApproved;
import com.vis.entities.VisEntitySkillFixHierarchyPending;
import com.vis.entities.VisEntitySkillFixHierarchyRejected;
import com.vis.entities.VisEntitySkillPending;
import com.vis.entities.VisEntitySkillRejected;

/**
 * Agrupa as classes de envio de mensagens relacionadas ao ciclo de vida de habilidades (skills) no sistema —
 * aprovação, rejeição e estado pendente, tanto para a habilidade em si quanto para sua hierarquia.
 * Cada inner class herda de JnBusinessSendMessage e associa a entidade-destino correspondente.
 */
public class VisMessages {
	//FIXME FALTANDO TEMPLATE
	public static class RejectedSkillHierarchy extends JnBusinessSendMessage{
		protected RejectedSkillHierarchy() {
			super(VisEntitySkillFixHierarchyRejected.ENTITY);
		}
	}
	//FIXME FALTANDO TEMPLATE
	public static class AprovedSkillHierarchy extends JnBusinessSendMessage{
		protected AprovedSkillHierarchy() {
			super(VisEntitySkillFixHierarchyApproved.ENTITY);
		}
	}
	//FIXME FALTANDO TEMPLATE
	public static class PendingSkillHierarchy extends JnBusinessSendMessage{
		protected PendingSkillHierarchy() {
			super(VisEntitySkillFixHierarchyPending.ENTITY);
		}
	}
	//FIXME FALTANDO TEMPLATE
	public static class RejectedSkill extends JnBusinessSendMessage{
		protected RejectedSkill() {
			super(VisEntitySkillRejected.ENTITY);
		}
	}
	//FIXME FALTANDO TEMPLATE
	public static class PendingSkill extends JnBusinessSendMessage{
		protected PendingSkill() {
			super(VisEntitySkillPending.ENTITY);
		}
	}
	//FIXME FALTANDO TEMPLATE
	public static class AprovedSkill extends JnBusinessSendMessage{
		protected AprovedSkill() {
			super(VisEntitySkill.ENTITY);
		}
	}
}
