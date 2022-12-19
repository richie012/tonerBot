package com.example.tonerBot.repo;

import com.example.tonerBot.model.WeeklyOrder;
import org.springframework.data.repository.CrudRepository;

public interface WeeklyOrderRepository extends CrudRepository<WeeklyOrder, Long> {
}
