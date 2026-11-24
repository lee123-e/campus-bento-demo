package com.neuedu.reggie.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.reggie.dto.order.AdminOrderActionRequest.Action;
import com.neuedu.reggie.dto.order.AdminOrderPageResponse;
import com.neuedu.reggie.dto.order.OrderDetailResponse;
import com.neuedu.reggie.dto.order.OrderSummaryResponse;
import com.neuedu.reggie.entity.CustomerOrder;
import com.neuedu.reggie.entity.OrderStatus;
import com.neuedu.reggie.exception.BusinessException;
import com.neuedu.reggie.mapper.CustomerOrderMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOrderService {

    private final CustomerOrderMapper orders;
    private final EmployeeService employees;
    private final OrderDetailReader details;

    public AdminOrderService(CustomerOrderMapper orders, EmployeeService employees, OrderDetailReader details) {
        this.orders = orders;
        this.employees = employees;
        this.details = details;
    }

    public AdminOrderPageResponse list(String username, int page, int size, String number,
                                       Integer status, LocalDate start, LocalDate end) {
        employees.requireActive(username);
        validateDates(start, end);
        String search = number == null ? "" : number.strip();
        var query = Wrappers.<CustomerOrder>lambdaQuery()
                .select(CustomerOrder::getId, CustomerOrder::getOrderNumber, CustomerOrder::getStatus,
                        CustomerOrder::getTotal, CustomerOrder::getCreatedAt, CustomerOrder::getPaidAt,
                        CustomerOrder::getDispatchedAt, CustomerOrder::getCompletedAt, CustomerOrder::getContactName)
                .like(!search.isEmpty(), CustomerOrder::getOrderNumber, search)
                .eq(status != null, CustomerOrder::getStatus, status)
                .orderByDesc(CustomerOrder::getCreatedAt, CustomerOrder::getId);
        if (start != null) {
            query.ge(CustomerOrder::getCreatedAt, start.atStartOfDay());
        }
        if (end != null) {
            query.le(CustomerOrder::getCreatedAt, end.atTime(23, 59, 59, 999_000_000));
        }
        Page<CustomerOrder> result = orders.selectPage(new Page<>(page, size), query);
        return new AdminOrderPageResponse(result.getTotal(), result.getRecords().stream()
                .map(AdminOrderPageResponse.Row::from).toList());
    }

    public OrderDetailResponse detail(String username, long id) {
        employees.requireActive(username);
        return details.read(requireOrder(id));
    }

    @Transactional
    public OrderSummaryResponse advance(String username, long id, Action action) {
        long employeeId = employees.requireActive(username).id();
        boolean dispatch = action == Action.DISPATCH;
        OrderStatus previous = dispatch ? OrderStatus.AWAITING_DISPATCH : OrderStatus.DISPATCHED;
        OrderStatus next = dispatch ? OrderStatus.DISPATCHED : OrderStatus.COMPLETED;
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        var update = Wrappers.<CustomerOrder>lambdaUpdate()
                .eq(CustomerOrder::getId, id)
                .eq(CustomerOrder::getStatus, previous.code())
                .isNotNull(CustomerOrder::getPaidAt)
                .set(CustomerOrder::getStatus, next.code());
        if (dispatch) {
            update.set(CustomerOrder::getDispatchedAt, now).set(CustomerOrder::getDispatchedBy, employeeId);
        } else {
            update.set(CustomerOrder::getCompletedAt, now).set(CustomerOrder::getCompletedBy, employeeId);
        }
        if (orders.update(null, update) != 1) {
            requireOrder(id);
            throw new BusinessException(BusinessException.Reason.RULE_CONFLICT,
                    dispatch ? "只有待派送的已支付订单可以派送，请查看最新状态。"
                            : "只有已派送的订单可以标记完成，请查看最新状态。");
        }
        return OrderSummaryResponse.from(requireOrder(id));
    }

    private CustomerOrder requireOrder(long id) {
        CustomerOrder order = orders.selectById(id);
        if (order == null) {
            throw new BusinessException(BusinessException.Reason.RECORD_NOT_FOUND, "这笔订单不存在。");
        }
        return order;
    }

    private static void validateDates(LocalDate start, LocalDate end) {
        if ((start != null && (start.getYear() < 1 || start.getYear() > 9999))
                || (end != null && (end.getYear() < 1 || end.getYear() > 9999))) {
            throw new BusinessException(BusinessException.Reason.INVALID_INPUT, "请选择有效的查询日期。");
        }
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(BusinessException.Reason.INVALID_INPUT, "开始日期不能晚于结束日期。");
        }
    }
}
