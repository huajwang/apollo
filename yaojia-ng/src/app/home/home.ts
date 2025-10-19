import { Component, inject } from '@angular/core';
import { HousingLocation } from '../housing-location/housing-location';
import { HousingLocationInfo } from '../housinglocation';
import { HousingService } from '../service/housing-service';

@Component({
  selector: 'app-home',
  imports: [HousingLocation],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home {
  housingLocationList: HousingLocationInfo[] = [];
  filteredLocationList: HousingLocationInfo[] = [];

  housingService = inject(HousingService);

  filterResults(searchText: string) {
    this.filteredLocationList = this.housingLocationList.filter((location: HousingLocationInfo) => {
      const searchTextLower = searchText.toLowerCase();
      return location.name.toLowerCase().includes(searchTextLower) ||
             location.city.toLowerCase().includes(searchTextLower) ||
             location.state.toLowerCase().includes(searchTextLower);
    });
  }

  constructor() {
    this.housingLocationList = this.housingService.getAllHousingLocations();
    this.filteredLocationList = this.housingLocationList;
  }
}
