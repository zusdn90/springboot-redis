package com.demo.redis;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReactiveRedisClusterTest {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private ReactiveRedisTemplate<String,String> reactiveRedisTemplate;

    @Test
    public void opsValue(){
        ReactiveValueOperations<String,String> valueOps = reactiveRedisTemplate.opsForValue();

        Set<String> cacheKeys = new HashSet<>();

        // async process
        log.info("Step-1");

        for(int i=0; i<5000; i++){
            String key = "value_" + i;
            cacheKeys.add(key);
            valueOps.set(key, String.valueOf(i));
        }
        log.info("Step-2");
        Mono<List<String>> values = valueOps.multiGet(cacheKeys);
        log.info("Step-3");
        StepVerifier.create(values)
                .expectNextMatches(x->x.size() == 5000).verifyComplete();

        log.info("Step-4");
    }

    @Test
    public void opsList() {
        ReactiveListOperations<String, String> listOps = reactiveRedisTemplate.opsForList();
        String cacheKey = "valueList";
        // previous key delete - sync process
        redisTemplate.delete(cacheKey);
        // async process
        Mono<Long> results = listOps.leftPushAll(cacheKey, "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
        StepVerifier.create(results).expectNext(10L).verifyComplete();
        StepVerifier.create(reactiveRedisTemplate.type(cacheKey)).expectNext(DataType.LIST).verifyComplete();
        StepVerifier.create(listOps.size(cacheKey)).expectNext(10L).verifyComplete();
        StepVerifier.create(listOps.rightPop(cacheKey)).expectNext("0").verifyComplete();
        StepVerifier.create(listOps.leftPop(cacheKey)).expectNext("9").verifyComplete();
    }

    @Test
    public void opsHash() {
        ReactiveHashOperations<String, String, String> hashOps = reactiveRedisTemplate.opsForHash();
        String cacheKey = "valueHash";

        Map<String,String> setDatas = new HashMap<>();

        for(int i=0; i<10; i++){
            setDatas.put("key_" + i, "value_" + i);
        }

        // previous key delete - sync process
        redisTemplate.delete(cacheKey);
        // async process
        StepVerifier.create(hashOps.putAll(cacheKey, setDatas)).expectNext(true).verifyComplete();
        StepVerifier.create(reactiveRedisTemplate.type(cacheKey)).expectNext(DataType.HASH).verifyComplete();
        StepVerifier.create(hashOps.size(cacheKey)).expectNext(10L).verifyComplete();
        StepVerifier.create(hashOps.get(cacheKey,"key_5")).expectNext("value_5").verifyComplete();
        StepVerifier.create(hashOps.remove(cacheKey, "key_5")).expectNext(1L).verifyComplete();
    }

    @Test
    public void opsSortedSet() {
        ReactiveZSetOperations<String, String> zsetOps = reactiveRedisTemplate.opsForZSet();
        String cacheKey = "valueZSet";
        // previous key delete - sync process
        redisTemplate.delete(cacheKey);
        // async process
        List<ZSetOperations.TypedTuple<String>> tuples = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tuples.add(new DefaultTypedTuple<>(String.valueOf(i), (double) i));
        }
        StepVerifier.create(zsetOps.addAll(cacheKey, tuples)).expectNext(10L).verifyComplete();
        StepVerifier.create(reactiveRedisTemplate.type(cacheKey)).expectNext(DataType.ZSET).verifyComplete();
        StepVerifier.create(zsetOps.size(cacheKey)).expectNext(10L).verifyComplete();
        StepVerifier.create(zsetOps.reverseRank(cacheKey, "9")).expectNext(0L).verifyComplete();
    }

    @Test
    public void opsGeo() {
        ReactiveGeoOperations<String, String> geoOps = reactiveRedisTemplate.opsForGeo();
        String[] cities = {"서울", "부산"};
        String[][] gu = {{"강남구", "서초구", "관악구", "동작구", "마포구"}, {"사하구", "해운대구", "영도구", "동래구", "수영구"}};
        Point[][] pointGu = {{new Point(10, -10), new Point(11, -20), new Point(13, 10), new Point(14, 30), new Point(15, 40)}, {new Point(-100, 10), new Point(-110, 20), new Point(-130, 80), new Point(-140, 60), new Point(-150, 30)}};
        String cacheKey = "valueGeo";
        // previous key delete - sync process
        redisTemplate.delete(cacheKey);
        // async process
        Map<String, Point> memberCoordiateMap = new HashMap<>();
        for (int x = 0; x < cities.length; x++) {
            for (int y = 0; y < 5; y++) {
                memberCoordiateMap.put(gu[x][y], pointGu[x][y]);
            }
        }
        StepVerifier.create(geoOps.add(cacheKey, memberCoordiateMap)).expectNext(10L).verifyComplete();
        StepVerifier.create(geoOps.distance(cacheKey, "강남구", "동작구")).expectNextMatches(x -> x.getValue() == 4469610.0767).verifyComplete();
        StepVerifier.create(geoOps.position(cacheKey, "동작구")).expectNextMatches(x -> x.getX() == 14.000001847743988 && x.getY() == 30.000000249977013).verifyComplete();
    }

    @Test
    public void opsHyperLogLog() {
        ReactiveHyperLogLogOperations<String, String> hyperLogLogOps = reactiveRedisTemplate.opsForHyperLogLog();
        String cacheKey = "valueHyperLogLog";
        // previous key delete - sync process
        redisTemplate.delete(cacheKey);
        // async process
        String[] arr = {"1", "2", "2", "3", "4", "5", "5", "5", "5", "6", "7", "7", "7"};
        StepVerifier.create(hyperLogLogOps.add(cacheKey, arr)).expectNext(1L).verifyComplete();
        StepVerifier.create(hyperLogLogOps.size(cacheKey)).expectNext(7L).verifyComplete();
    }
}
