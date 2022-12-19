package com.example.tonerBot.repo;

import com.example.tonerBot.model.DailyOrder;
import org.springframework.data.repository.CrudRepository;

public interface DailyOrderRepository extends CrudRepository<DailyOrder, Long> {
}
