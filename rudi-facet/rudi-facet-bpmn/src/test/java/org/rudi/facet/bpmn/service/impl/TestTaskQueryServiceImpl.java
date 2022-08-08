/**
 * RUDI Portail
 */
package org.rudi.facet.bpmn.service.impl;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.StringUtils;
import org.rudi.common.service.helper.UtilContextHelper;
import org.rudi.facet.bpmn.bean.workflow.TestTaskSearchCriteria;
import org.rudi.facet.bpmn.helper.form.FormHelper;
import org.rudi.facet.bpmn.helper.workflow.BpmnHelper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * @author FNI18300
 *
 */
@Service
public class TestTaskQueryServiceImpl extends AbstractTaskQueryServiceImpl<TestTaskSearchCriteria> {

	private static final String FIELD_A = "A";

	public TestTaskQueryServiceImpl(ProcessEngine processEngine, FormHelper formHelper, BpmnHelper bpmnHelper,
			UtilContextHelper utilContextHelper, ApplicationContext applicationContext) {
		super(processEngine, formHelper, bpmnHelper, utilContextHelper, applicationContext);
	}

	@Override
	protected void applyExtentedCriteria(TaskQuery taskQuery, TestTaskSearchCriteria taskSearchCriteria) {
		if (StringUtils.isNotEmpty(taskSearchCriteria.getA())) {
			taskQuery.processVariableValueEquals(FIELD_A, taskSearchCriteria.getA());
		}
	}

}
