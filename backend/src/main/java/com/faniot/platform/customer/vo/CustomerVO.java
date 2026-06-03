package com.faniot.platform.customer.vo;

import com.faniot.platform.customer.domain.Customer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "客户信息")
public record CustomerVO(
        Long id,
        String customerId,
        String customerName,
        String contactPerson,
        String contactPhone,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CustomerVO from(Customer customer) {
        return new CustomerVO(
                customer.getId(),
                customer.getCustomerId(),
                customer.getCustomerName(),
                customer.getContactPerson(),
                customer.getContactPhone(),
                customer.getRemark(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
