package org.example.CampusMart.web.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResultVo {
    private Boolean success;
    private Integer likeCount;
    private Boolean isLiked;
}
