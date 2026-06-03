package com.faniot.platform.customer.service;

import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.common.exception.BusinessException;
import com.faniot.platform.customer.domain.Customer;
import com.faniot.platform.customer.dto.CustomerRequest;
import com.faniot.platform.customer.repository.CustomerRepository;
import com.faniot.platform.customer.vo.CustomerVO;
import com.faniot.platform.device.repository.DeviceRepository;
import com.faniot.platform.gateway.repository.GatewayRepository;
import com.faniot.platform.project.repository.ProjectRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final GatewayRepository gatewayRepository;
    private final DeviceRepository deviceRepository;

    public CustomerService(
            CustomerRepository customerRepository,
            ProjectRepository projectRepository,
            GatewayRepository gatewayRepository,
            DeviceRepository deviceRepository
    ) {
        this.customerRepository = customerRepository;
        this.projectRepository = projectRepository;
        this.gatewayRepository = gatewayRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerVO> page(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.from(customerRepository.findAll(buildSpec(keyword), pageable).map(CustomerVO::from));
    }

    @Transactional(readOnly = true)
    public CustomerVO detail(Long id) {
        return CustomerVO.from(getById(id));
    }

    @Transactional
    public CustomerVO create(CustomerRequest request) {
        if (customerRepository.existsByCustomerId(request.customerId())) {
            throw new BusinessException("客户编号已存在");
        }
        Customer customer = new Customer();
        apply(customer, request);
        return CustomerVO.from(customerRepository.save(customer));
    }

    @Transactional
    public CustomerVO update(Long id, CustomerRequest request) {
        Customer customer = getById(id);
        if (customerRepository.existsByCustomerIdAndIdNot(request.customerId(), id)) {
            throw new BusinessException("客户编号已存在");
        }
        apply(customer, request);
        return CustomerVO.from(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = getById(id);
        String customerId = customer.getCustomerId();
        if (projectRepository.existsByCustomerId(customerId)
                || gatewayRepository.existsByCustomerId(customerId)
                || deviceRepository.existsByCustomerId(customerId)) {
            throw new BusinessException("客户已关联项目、盒子或设备，不能删除");
        }
        customerRepository.delete(customer);
    }

    private Customer getById(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new BusinessException("客户不存在"));
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setCustomerId(request.customerId());
        customer.setCustomerName(request.customerName());
        customer.setContactPerson(request.contactPerson());
        customer.setContactPhone(request.contactPhone());
        customer.setRemark(request.remark());
    }

    private Specification<Customer> buildSpec(String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("customerId"), like),
                        cb.like(root.get("customerName"), like),
                        cb.like(root.get("contactPerson"), like)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
