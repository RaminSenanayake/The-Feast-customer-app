package lk.raminsenanayake.thefeast.model;

import java.io.Serializable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {
    private String categoryId, productId, name, description, image;
    private Map<String, Double> portionList;
}
