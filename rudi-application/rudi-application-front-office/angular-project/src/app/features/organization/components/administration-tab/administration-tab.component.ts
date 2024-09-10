import {Component, Input, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Organization} from 'micro_service_modules/strukture/strukture-model';
import {IconRegistryService} from '@core/services/icon-registry.service';
import {ALL_TYPES} from '@shared/models/title-icon-type';

@Component({
    selector: 'app-administration-tab',
    templateUrl: './administration-tab.component.html',
})
export class AdministrationTabComponent implements OnInit {

    @Input() isLoading: boolean;
    @Input() organization: Organization;

    /**
     * Indique si le captcha doit s'activer sur cette page
     */
    enableCaptchaOnPage = true;

    constructor(private readonly iconRegistryService: IconRegistryService,
                private readonly route: ActivatedRoute) {
        iconRegistryService.addAllSvgIcons(ALL_TYPES);
    }

    ngOnInit(): void {
        if (this.route.snapshot.data?.aclAppInfo) {
            // Si on a pu recuperer l'info d'activation du captcha sinon il reste à false par défaut
            this.enableCaptchaOnPage = this.route.snapshot.data.aclAppInfo.captchaEnabled;
        } else {
            this.enableCaptchaOnPage = false;
        }
    }
}
