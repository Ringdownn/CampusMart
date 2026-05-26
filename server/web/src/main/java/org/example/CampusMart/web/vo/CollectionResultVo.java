package org.example.CampusMart.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionResultVo {
    private Boolean success;
    private Integer collectionCount;
    private Boolean isCollected;
}
