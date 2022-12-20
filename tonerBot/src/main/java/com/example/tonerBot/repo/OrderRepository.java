package com.example.tonerBot.repo;

import com.example.tonerBot.model.Order;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, Long> {
}
