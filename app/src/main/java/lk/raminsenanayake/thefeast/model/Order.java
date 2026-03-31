package lk.raminsenanayake.thefeast.model;

import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order implements Serializable {

    public static final String PENDING = "Pending";
    public static final String READY = "Ready";
    public static final String PICKED_UP_BY_CUSTOMER = "Picked Up";
    public static final String ON_DELIVERY = "Pending";
    public static final String DELIVERED = "Delivered";
    public static final String CANCELLED = "Cancelled";

    private String orderId, uid, status, orderType, location;
    private List<OrderItem> orderItems;
    private double total;
    @ServerTimestamp
    private Date timestamp;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItem implements Serializable {
        private String productId, portionSize;
        private int quantity;
    }
}



