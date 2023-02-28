import {Injectable} from '@angular/core';
import {NewDatasetRequest} from '../../../../projekt/projekt-api';
import {Task} from '../../../../api-bpmn';
import {TaskDependencyFetchers, TaskWithDependenciesService} from '../task-with-dependencies-service';
import {TaskWithDependencies} from '../../../../shared/utils/task-utils';
import {TaskDependencies} from '../task-dependencies.interface';
import {AclService} from '../../../../acl/acl-api';
import {OrganizationService} from '../../../../strukture/api-strukture';
import {NewDatasetRequestTaskMetierService} from './new-dataset-request-task-metier.service';
import {ProjektTaskSearchCriteria} from './projekt-task-search-criteria.interface';
import {OwnerInfo, Project} from '../../../../projekt/projekt-model';
import {DependencyFetcher, OwnerKey} from '../../../../shared/utils/dependencies-utils';
import {ProjektMetierService} from '../../projekt-metier.service';

/**
 * les dépendances attendues pour une tâche
 */
export interface NewDatasetRequestDependencies extends TaskDependencies {
    project?: Project;
    ownerInfo?: OwnerInfo;
}


/**
 * Objet permettant de charger les dépendances d'une tâche
 */
export class NewDatasetRequestTask extends TaskWithDependencies<NewDatasetRequest, NewDatasetRequestDependencies> {
    constructor(task: Task) {
        super(task, {});
    }
}

@Injectable({
    providedIn: 'root'
})
export class NewDatasetRequestTaskDepenciesService
    extends TaskWithDependenciesService<NewDatasetRequestTask, ProjektTaskSearchCriteria, NewDatasetRequest> {

    constructor(readonly newDatasetRequestTaskMetierService: NewDatasetRequestTaskMetierService) {
        super(newDatasetRequestTaskMetierService);
    }

    newTaskWithDependencies(task: Task): NewDatasetRequestTask {
        return new NewDatasetRequestTask(task);
    }

    defaultSearchCriteria(): ProjektTaskSearchCriteria {
        return {};
    }
}

@Injectable({
    providedIn: 'root'
})
export class NewDatasetRequestTaskDependencyFetchers
    extends TaskDependencyFetchers<NewDatasetRequestTask, NewDatasetRequest, NewDatasetRequestDependencies> {

    constructor(
        organizationService: OrganizationService,
        aclService: AclService,
        private readonly projektMetierService:ProjektMetierService
    ) {
        super(organizationService, aclService);
    }
    get project(): DependencyFetcher<NewDatasetRequestTask, Project> {
        return {
            hasPrerequisites: (input: NewDatasetRequestTask) => input != null && input.asset != null && input.asset.uuid != null,
            getKey: taskWithDependencies => taskWithDependencies.asset.uuid,
            getValue: datasetUuid => this.projektMetierService.findProjectByNewDatasetRequest(datasetUuid)
        };
    }
    get ownerInfo(): DependencyFetcher<NewDatasetRequestTask, OwnerInfo> {
        return {
            hasPrerequisites: (input: NewDatasetRequestTask) => input != null && input.dependencies != null
                && input.dependencies.project != null,
            getKey: taskWithDependencies => OwnerKey.serialize(taskWithDependencies.dependencies.project),
            getValue: ownerKey => {
                const {owner_type, owner_uuid} = OwnerKey.deserialize(ownerKey);
                return this.projektMetierService.getOwnerInfo(owner_type, owner_uuid);
            }
        };
    }
}
