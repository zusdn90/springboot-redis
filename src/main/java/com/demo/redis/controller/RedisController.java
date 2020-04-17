package com.demo.redis.controller;

import com.demo.redis.cluster.common.CacheKey;
import com.demo.redis.entity.User;
import com.demo.redis.repo.UserJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/redis")
@RestController
public class RedisController {

    private final RedisTemplate<String, String> redisTemplate;
    private final UserJpaRepo userJpaRepo;

    // Redis에 캐싱된 데이터가 있으면 반환하고, 없으면 DB에서 조회한 다은 Redis에 캐시함.
    @Cacheable(value = CacheKey.USER, key = "#msrl", unless="#result ==null")
    @GetMapping("/user/{msrl}")
    public User fineOne(@PathVariable long msrl) {
        return userJpaRepo.findById(msrl).orElse(null);
    }

    //캐시 정보 갱신
    @CachePut(value = CacheKey.USER, key = "#user.msrl")
    @PutMapping("/user")
    @ResponseBody
    public User putUser(@RequestBody User user) {
        return userJpaRepo.save(user);
    }

    @PostMapping("/user")
    @ResponseBody
    public User saveUser(@RequestBody User user) {
        return userJpaRepo.save(user);
    }


    //캐시 삭제
    @CacheEvict(value = CacheKey.USER, key = "#msrl")
    @DeleteMapping("/user/{msrl}")
    @ResponseBody
    public boolean deleteUser(@PathVariable long msrl) {
        userJpaRepo.deleteById(msrl);
        return true;
    }
}
