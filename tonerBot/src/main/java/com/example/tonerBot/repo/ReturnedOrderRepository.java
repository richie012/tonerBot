package com.example.tonerBot.repo;

import org.springframework.data.repository.CrudRepository;
import com.example.tonerBot.model.ReturnedOrder;

public interface ReturnedOrderRepository extends CrudRepository<ReturnedOrder, Long> {
}
