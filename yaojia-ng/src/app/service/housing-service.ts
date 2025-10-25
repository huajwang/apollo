import { inject, Injectable } from '@angular/core';
import { HousingLocationInfo } from '../housinglocation';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class HousingService {

  private http = inject(HttpClient)

  readonly url = 'http://localhost:8080/api';

  readonly baseUrl = 'https://angular.dev/assets/images/tutorials/common';
    housingLocationList: HousingLocationInfo[] = [
      {
        id: 0,
        name: 'Acme Fresh Start Housing',
        city: 'Chicago',
        state: 'IL',
        photo: `${this.baseUrl}/bernard-hermant-CLKGGwIBTaY-unsplash.jpg`,
        availableUnits: 4,
        wifi: true,
        laundry: true,
      },
      {
        id: 1,
        name: 'A113 Transitional Housing',
        city: 'Santa Monica',
        state: 'CA',
        photo: `${this.baseUrl}/brandon-griggs-wR11KBaB86U-unsplash.jpg`,
        availableUnits: 0,
        wifi: false,
        laundry: true,
      },
    ];

  getAllHousingLocations(): HousingLocationInfo[] {
    this.getAllHousingLocations2();
    return this.housingLocationList;
  }

  getHousingLocationById(id: number): HousingLocationInfo | undefined {
      return this.housingLocationList.find((housingLocation) => housingLocation.id === id);
  }

  getAllHousingLocations2(): Observable<HousingLocationInfo[]> {
    return this.http.get<HousingLocationInfo[]>(this.url);
  }

  async getHousingLocationById2(id: number): Promise<HousingLocationInfo | undefined> {
    const data = await fetch(`${this.url}?id=${id}`);
    const locationJson = await data.json();
    return locationJson[0] ?? {};
  }

  submitApplication(firstName: string, lastName: string, email: string) {
      console.log(
        `Homes application received: firstName: ${firstName}, lastName: ${lastName}, email: ${email}.`,
      );
    }
}
