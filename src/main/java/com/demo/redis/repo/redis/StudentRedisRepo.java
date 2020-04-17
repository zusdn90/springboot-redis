package com.demo.redis.repo.redis;

import com.demo.redis.entity.redis.Student;
import org.springframework.data.repository.CrudRepository;

public interface StudentRedisRepo extends CrudRepository<Student, Long> {
}
