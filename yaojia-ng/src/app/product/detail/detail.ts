import {Component, inject} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import { HousingService } from '../../service/housing-service';
import { HousingLocationInfo } from '../../housinglocation';

@Component({
  selector: 'product-detail',
  imports: [],
  templateUrl: './detail.html',
  styleUrl: './detail.scss'
})
export class Detail {
  route: ActivatedRoute = inject(ActivatedRoute);
  housingService = inject(HousingService);
  housingLocation: HousingLocationInfo | undefined;
  constructor() {
    const housingLocationId = Number(this.route.snapshot.params['id']);
     this.housingLocation = this.housingService.getHousingLocationById(housingLocationId);
  }
}
