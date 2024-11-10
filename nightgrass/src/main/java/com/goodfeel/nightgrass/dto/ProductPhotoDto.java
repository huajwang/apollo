package com.goodfeel.nightgrass.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class ProductPhotoDto {
    private Long id;
    private Long productId;
    private String photoUrl;
}
