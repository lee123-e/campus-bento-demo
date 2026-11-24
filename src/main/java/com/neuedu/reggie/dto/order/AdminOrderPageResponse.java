package com.neuedu.reggie.dto.order;

import com.neuedu.reggie.entity.CustomerOrder;
import java.util.List;

public record AdminOrderPageResponse(long total, List<Row> items) {

    public record Row(OrderSummaryResponse order, String contactName) {

        public static Row from(CustomerOrder order) {
            return new Row(OrderSummaryResponse.from(order), order.getContactName());
        }
    }
}
