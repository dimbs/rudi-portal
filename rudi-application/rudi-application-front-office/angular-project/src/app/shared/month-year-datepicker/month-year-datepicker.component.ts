import {Component, Input, OnInit, ViewChild} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE} from '@angular/material/core';
import {MAT_MOMENT_DATE_ADAPTER_OPTIONS, MomentDateAdapter} from '@angular/material-moment-adapter';
import {Moment} from 'moment';
import {MatDatepicker} from '@angular/material/datepicker';
import * as moment from 'moment';

/**
 * Format MM/YYYY appliqué au DatePicker
 */
export const MONTH_YEAR_FORMAT = {
    parse: {
        dateInput: 'MM/YYYY',
    },
    display: {
        dateInput: 'MM/YYYY',
        monthYearLabel: 'MMM YYYY',
        dateA11yLabel: 'LL',
        monthYearA11yLabel: 'MMMM YYYY',
    },
};

@Component({
    selector: 'app-month-year-datepicker',
    templateUrl: './month-year-datepicker.component.html',
    styleUrls: ['./month-year-datepicker.component.scss'],
    providers: [
        // Pour que le forçage en MM/YYYY s'applique on est obligé de configurer avec les providers
        // DateAdapter, car on va surcharger le comportement par défaut
        // MomentDateAdapter, car on va utiliser moment pour forcer le format custom MM/YYYY
        {
            provide: DateAdapter,
            useClass: MomentDateAdapter,
            deps: [MAT_DATE_LOCALE, MAT_MOMENT_DATE_ADAPTER_OPTIONS],
        },
        // C'est ici qu'on donne notre format Custom
        {provide: MAT_DATE_FORMATS, useValue: MONTH_YEAR_FORMAT}
    ]
})
export class MonthYearDatepickerComponent implements OnInit {

    /**
     * Le Formgroup qui va contenir le formcontrol qui contrôle la valeur de date
     */
    @Input()
    public formGroup: FormGroup;

    /**
     * Le nom du contrôle du formgroup
     */
    @Input()
    public controlName: string;

    /**
     * Placeholder qui s'affiche dans le champ de saisie
     */
    @Input()
    public placeholder: string;

    /**
     * Message d'erreur à afficher si composant en état d'erreur
     */
    @Input()
    public errorMessage: string;

    /**
     * Accesseur au datepicker popup pour sélectionner la date au clic
     */
    @ViewChild('datepicker')
    public datepickerPopup: MatDatepicker<Moment>;

    constructor() {
    }

    ngOnInit(): void {
        // on souhaite que la valeur du Control soit toujours au premier jour du mois si on donne une valeur au component
        const value: Moment = this.formGroup.get(this.controlName).value;
        if (value && value.date() !== 1) {
            const normalized: Moment = moment().year(value.year()).month(value.month()).date(1);
            this.formGroup.get(this.controlName).setValue(normalized);
        }
    }

    /**
     * Méthode appelée sur seléction de l'année dans le Datepicker
     * @param normalizedYear la valeur Moment de la date choisie
     */
    chosenYearHandler(normalizedYear: Moment): void {
        const ctrlValue = moment();

        // On se met au 1er jour du 1er mois de l'année choisi
        ctrlValue.year(normalizedYear.year()).date(1);
        this.formGroup.get(this.controlName).setValue(ctrlValue);
        this.formGroup.get(this.controlName).markAsDirty();
    }

    /**
     * Méthode appelée sur selection du mois dans le Datepicker
     * @param normalizedMonth la valeur Moment de la date choisie
     */
    chosenMonthHandler(normalizedMonth: Moment): void {
        const ctrlValue = moment();

        // On se met au 1er jour du mois de l'année choisi
        ctrlValue.year(normalizedMonth.year()).month(normalizedMonth.month()).date(1);
        this.formGroup.get(this.controlName).setValue(ctrlValue);
        this.formGroup.get(this.controlName).markAsDirty();

        // On ferme le picker pour empêcher la saisie au jour
        this.datepickerPopup.close();
    }

    /**
     * Est-ce que le composant est en état d'erreur ?
     */
    hasErrors(): boolean {
        if (this.formGroup != null && this.formGroup.get(this.controlName) != null) {
            const control = this.formGroup.get(this.controlName);

            // Composant en erreur si le contrôle a bougé et sil y'a des erreurs
            if (control.errors != null && control.dirty) {

                // Erreur champ requis
                if (control.errors.required != null && control.value === null) {
                    return true;
                }

                // Erreur date incohérente dans le cas de saisie de période
                if (control.errors.periodInconsistent === true) {
                    return true;
                }
            }
        }

        return false;
    }
}
