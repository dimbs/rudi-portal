import {Injectable} from '@angular/core';
import {UserService} from '@core/services/user.service';
import {TranslateService} from '@ngx-translate/core';
import {AclService} from 'micro_service_modules/acl/acl-api';
import {KonsultService} from 'micro_service_modules/konsult/konsult-api';
import {ProjektService} from 'micro_service_modules/projekt/projekt-api';
import {OrganizationService} from 'micro_service_modules/strukture/api-strukture';
import {AbstractApiAccessService} from '../abstract-api-access.service';

@Injectable({
    providedIn: 'root'
})
export class SelfdataApiAccessService extends AbstractApiAccessService {

    constructor(
        userService: UserService,
        aclService: AclService,
        konsultService: KonsultService,
        projektService: ProjektService,
        organizationService: OrganizationService,
        translateService: TranslateService) {
        super(aclService, konsultService, projektService, organizationService, translateService);
    }
}
