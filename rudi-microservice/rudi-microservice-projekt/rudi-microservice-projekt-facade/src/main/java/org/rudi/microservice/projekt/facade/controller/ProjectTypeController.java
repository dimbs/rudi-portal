package org.rudi.microservice.projekt.facade.controller;

import java.util.UUID;

import org.rudi.common.facade.util.UtilPageable;
import org.rudi.common.service.exception.AppServiceException;
import org.rudi.microservice.projekt.core.bean.PagedProjectTypeList;
import org.rudi.microservice.projekt.core.bean.ProjectType;
import org.rudi.microservice.projekt.core.bean.criteria.ProjectTypeSearchCriteria;
import org.rudi.microservice.projekt.facade.controller.api.TypesApi;
import org.rudi.microservice.projekt.service.type.ProjectTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import lombok.val;
import static org.rudi.common.core.security.QuotedRoleCodes.ADMINISTRATOR;
import static org.rudi.common.core.security.QuotedRoleCodes.MODULE_PROJEKT;
import static org.rudi.common.core.security.QuotedRoleCodes.MODULE_PROJEKT_ADMINISTRATOR;

@RestController
@RequiredArgsConstructor
public class ProjectTypeController implements TypesApi {

	private final ProjectTypeService projectTypeService;
	private final UtilPageable utilPageable;

	@Override
	@PreAuthorize("hasAnyRole(" + ADMINISTRATOR + ", " + MODULE_PROJEKT_ADMINISTRATOR + ", " + MODULE_PROJEKT + ")")
	public ResponseEntity<ProjectType> createProjectType(ProjectType projectType) throws AppServiceException {
		val createdProjectType = projectTypeService.createProjectType(projectType);
		val location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{uuid}")
				.buildAndExpand(createdProjectType.getUuid())
				.toUri();
		return ResponseEntity.created(location).body(createdProjectType);
	}

	@Override
	public ResponseEntity<ProjectType> getProjectType(UUID uuid) {
		return ResponseEntity.ok(projectTypeService.getProjectType(uuid));
	}

	/**
	 * GET /types : Recherche de type de projet
	 * Recherche de type de projet
	 *
	 * @param limit  Le nombre de résultats à retourner par page (optional)
	 * @param offset Index de début (positionne le curseur pour parcourir les résultats de la recherche) (optional)
	 * @param order  (optional)
	 * @param active (optional)
	 * @return OK (status code 200)
	 * or Internal server error (status code 500)
	 */
	@Override
	public ResponseEntity<PagedProjectTypeList> searchProjectTypes(Boolean active, Integer limit, Integer offset, String order) throws Exception {
		val searchCriteria = ProjectTypeSearchCriteria.builder().active(active).build();
		val pageable = utilPageable.getPageable(offset, limit, order);
		val page = projectTypeService.searchProjectTypes(searchCriteria, pageable);
		return ResponseEntity.ok(new PagedProjectTypeList()
				.total(page.getTotalElements())
				.elements(page.getContent()));
	}

	@Override
	@PreAuthorize("hasAnyRole(" + ADMINISTRATOR + ", " + MODULE_PROJEKT_ADMINISTRATOR + ", " + MODULE_PROJEKT + ")")
	public ResponseEntity<Void> updateProjectType(UUID uuid, ProjectType projectType) throws AppServiceException {
		projectType.setUuid(uuid);
		projectTypeService.updateProjectType(projectType);
		return ResponseEntity.noContent().build();
	}

	@Override
	@PreAuthorize("hasAnyRole(" + ADMINISTRATOR + ", " + MODULE_PROJEKT_ADMINISTRATOR + ", " + MODULE_PROJEKT + ")")
	public ResponseEntity<Void> deleteProjectType(UUID uuid) {
		projectTypeService.deleteProjectType(uuid);
		return ResponseEntity.noContent().build();
	}
}
