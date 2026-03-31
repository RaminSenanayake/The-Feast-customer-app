package lk.raminsenanayake.thefeast.model;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Cart implements Serializable {
    private List<CartItem> cartItems;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CartItem implements Serializable{
        private String productId, portionSize;
        private int quantity;
    }
}
