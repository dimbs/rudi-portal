/**
 * RUDI Portail
 */
package org.rudi.microservice.projekt.service.workflow;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.StringUtils;
import org.rudi.bpmn.core.bean.Task;
import org.rudi.common.service.helper.UtilContextHelper;
import org.rudi.facet.bpmn.entity.workflow.AssetDescriptionEntity;
import org.rudi.facet.bpmn.exception.FormDefinitionException;
import org.rudi.facet.bpmn.exception.InvalidDataException;
import org.rudi.facet.bpmn.helper.form.FormHelper;
import org.rudi.facet.bpmn.helper.workflow.BpmnHelper;
import org.rudi.facet.bpmn.service.impl.AbstractTaskQueryServiceImpl;
import org.rudi.microservice.projekt.core.bean.workflow.ProjektTaskSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * @author FNI18300
 *
 */
@Service
public class ProjektTaskQueryServiceImpl extends AbstractTaskQueryServiceImpl<ProjektTaskSearchCriteria> {

	@Autowired
	private ProjektFormEnhancerHelper projektFormEnhancerHelper;

	public ProjektTaskQueryServiceImpl(ProcessEngine processEngine, FormHelper formHelper, BpmnHelper bpmnHelper,
			UtilContextHelper utilContextHelper, ApplicationContext applicationContext) {
		super(processEngine, formHelper, bpmnHelper, utilContextHelper, applicationContext);
	}

	@Override
	protected void applyExtentedCriteria(TaskQuery taskQuery, ProjektTaskSearchCriteria taskSearchCriteria) {
		if (taskSearchCriteria.getProjectStatus() != null) {
			taskQuery.processVariableValueEquals(ProjektWorkflowConstants.PROJECT_STATUS,
					taskSearchCriteria.getProjectStatus().name());
		}
		if (StringUtils.isNotEmpty(taskSearchCriteria.getTitle())) {
			taskQuery.processVariableValueLikeIgnoreCase(ProjektWorkflowConstants.TITLE, taskSearchCriteria.getTitle());
		}

		final var datasetProducerUuid = taskSearchCriteria.getDatasetProducerUuid();
		if (datasetProducerUuid != null) {
			taskQuery.processVariableValueEquals(ProjektWorkflowConstants.DATASET_PRODUCER_UUID, datasetProducerUuid);
		}

		final var projectUuid = taskSearchCriteria.getProjectUuid();
		if (projectUuid != null) {
			taskQuery.processVariableValueEquals(ProjektWorkflowConstants.OWNER_PROJECT_UUID, projectUuid);
		}
	}

	@Override
	protected void updateDtoAssetData(Task task, org.activiti.engine.task.Task originalTask,
			AssetDescriptionEntity assetDescriptionEntity) throws InvalidDataException, FormDefinitionException {
		super.updateDtoAssetData(task, originalTask, assetDescriptionEntity);

		projektFormEnhancerHelper.enhance(task.getAsset().getForm());
	}

}
