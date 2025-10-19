import { Component, input } from '@angular/core';
import { HousingLocationInfo } from '../housinglocation';

@Component({
  selector: 'app-housing-location',
  imports: [],
  templateUrl: './housing-location.html',
  styleUrl: './housing-location.scss'
})
export class HousingLocation {
  housingLocation = input.required<HousingLocationInfo>();
}

