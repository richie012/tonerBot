package com.example.tonerBot.repo;

import com.example.tonerBot.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
