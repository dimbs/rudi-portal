import {Injectable} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {TranslateService} from '@ngx-translate/core';
import {Observable} from 'rxjs';
import {defaultIfEmpty, filter, map, switchMap, take, tap} from 'rxjs/operators';

const DEFAULT_PAGE_TITLE = 'RUDI';

@Injectable({
    providedIn: 'root'
})
export class PageTitleService {

    constructor(
        private readonly angularTitleService: Title,
        private readonly translateService: TranslateService,
    ) {
    }

    /**
     * @param titles Tous les titres possibles, le premier non "falsy" est utilisé
     */
    public setPageTitle(...titles: string[]): void {
        for (const title of titles) {
            if (title) {
                this.angularTitleService.setTitle(title + ' - ' + DEFAULT_PAGE_TITLE);
                return;
            }
        }
        this.setDefaultPageTitle();
    }

    private setDefaultPageTitle(): void {
        this.angularTitleService.setTitle(DEFAULT_PAGE_TITLE);
    }

    /**
     * @param fullUrl URL provenant du router Angular (commence par un "/" mais ne finit pas par un "/")
     */
    public setPageTitleFromUrl(fullUrl: string): void {

        /** Toutes les URL possibles en retirant à chaque fois le dernier dossier */
        const url$: Observable<string> = new Observable(subscriber => {
            let url = fullUrl;
            while (url && !subscriber.closed) {
                subscriber.next(url);

                const lastIndexOfSlash = url.lastIndexOf('/');
                if (lastIndexOfSlash === -1) {
                    break;
                }
                url = url.substring(0, lastIndexOfSlash);
            }
            subscriber.complete();
        });

        url$
            .pipe(
                switchMap(url => this.getPageTitleFromI18nOrNull(url)),
                filter(pageTitle => !!pageTitle),
                take(1),
                defaultIfEmpty(null)
            )
            .subscribe(pageTitle => {
                this.setPageTitle(pageTitle);
            });
    }

    private getPageTitleFromI18nOrNull(url: string): Observable<string|null> {
        const i18nkey = 'pageTitle.' + url;
        return this.translateService.get(i18nkey).pipe(
            map(translatedValue => translatedValue !== i18nkey ? translatedValue : null)
        );
    }
}
