package com.faniot.platform.customer.controller;

import com.faniot.platform.common.api.ApiResponse;
import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.customer.dto.CustomerRequest;
import com.faniot.platform.customer.service.CustomerService;
import com.faniot.platform.customer.vo.CustomerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "客户管理", description = "客户新增、编辑、删除和分页查询")
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "分页查询客户")
    @GetMapping
    public ApiResponse<PageResponse<CustomerVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(customerService.page(keyword, page, size));
    }

    @Operation(summary = "客户详情")
    @GetMapping("/{id}")
    public ApiResponse<CustomerVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(customerService.detail(id));
    }

    @Operation(summary = "新增客户")
    @PostMapping
    public ApiResponse<CustomerVO> create(@Valid @RequestBody CustomerRequest request) {
        return ApiResponse.ok(customerService.create(request));
    }

    @Operation(summary = "编辑客户")
    @PutMapping("/{id}")
    public ApiResponse<CustomerVO> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return ApiResponse.ok(customerService.update(id, request));
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.ok();
    }
}
