import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { HousingLocation } from '../housing-location/housing-location';
import { HousingLocationInfo } from '../housinglocation';
import { HousingService } from '../service/housing-service';

@Component({
  selector: 'app-home',
  imports: [HousingLocation],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home implements OnInit, OnDestroy {

  private subscription = new Subscription();

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

  ngOnInit() {
    this.subscription.add(
      this.housingService.getAllHousingLocations2().subscribe({
        next: (housingLocations) => {
          this.housingLocationList = housingLocations;
          this.filteredLocationList = this.housingLocationList;
        },
        error: (err) => {
          console.error('Error fetching housing locations:', err);
        }
      })
    )
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
  
}

