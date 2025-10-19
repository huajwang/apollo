import {Component, inject} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import { HousingService } from '../../service/housing-service';
import { HousingLocationInfo } from '../../housinglocation';

@Component({
  selector: 'product-detail',
  imports: [ReactiveFormsModule],
  templateUrl: './detail.html',
  styleUrl: './detail.scss'
})
export class Detail {
  route: ActivatedRoute = inject(ActivatedRoute);
  housingService = inject(HousingService);
  housingLocation: HousingLocationInfo | undefined;

  applyForm = new FormGroup({
      firstName: new FormControl(''),
      lastName: new FormControl(''),
      email: new FormControl(''),
    });

  submitApplication() {
      this.housingService.submitApplication(
        this.applyForm.value.firstName ?? '',
        this.applyForm.value.lastName ?? '',
        this.applyForm.value.email ?? '',
      );
    }

  constructor() {
    const housingLocationId = Number(this.route.snapshot.params['id']);
     this.housingLocation = this.housingService.getHousingLocationById(housingLocationId);
  }
}
