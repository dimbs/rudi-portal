package org.rudi.microservice.projekt.service.replacer;

import org.rudi.common.service.exception.AppServiceException;
import org.rudi.microservice.projekt.core.bean.Project;

public interface TransientDtoReplacer {
	void replaceDtoFor(Project project) throws AppServiceException;
}
