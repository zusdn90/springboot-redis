package com.demo.redis;

import com.demo.redis.entity.redis.Student;
import com.demo.redis.repo.redis.StudentRedisRepo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisApplicationTests {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private StudentRedisRepo redisRepo;

    /**
     * 문자 데이터 구조 처리
     */
    @Test
    public void opsValue() {
        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        Collection<String> cacheKeys = new ArrayList<>();
        String cacheKey = "value_";
        for (int i = 0; i < 10; i++) {
            cacheKeys.add(cacheKey + i);
            valueOps.set(cacheKey + i, String.valueOf(i), 60, TimeUnit.SECONDS);
        }
        List<String> values = valueOps.multiGet(cacheKeys);
        assertNotNull(values);
        assertEquals(10, values.size());
        log.info("##### opsValue #####");
        log.info("{}", values);
    }

    /**
     * List 데이터 구조 처리 - 순서 있음. value 중복 허용
     */
    @Test
    public void opsList() {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        String cacheKey = "valueList";
        for (int i = 0; i < 10; i++)
            listOps.leftPush(cacheKey, String.valueOf(i));

        assertSame(DataType.LIST, redisTemplate.type(cacheKey));
        assertSame(10L, listOps.size(cacheKey));
        log.info("##### opsList #####");
        log.info("{}", listOps.range(cacheKey, 0, 10));
        assertEquals("0", listOps.rightPop(cacheKey));
        assertEquals("9", listOps.leftPop(cacheKey));
        assertEquals(true, redisTemplate.delete(cacheKey));
    }

    /**
     * Hash 데이터 구조 처리 - 순서 없음. key 중복허용 안함, value 중복 허용
     */
    @Test
    public void opsHash() {
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        String cacheKey = "valueHash";
        for (int i = 0; i < 10; i++)
            hashOps.put(cacheKey, "key_" + i, "value_" + i);

        assertSame(DataType.HASH, redisTemplate.type(cacheKey));
        assertSame(10L, hashOps.size(cacheKey));
        log.info("##### opsHash #####");
        Set<String> hkeys = hashOps.keys(cacheKey);
        for (String hkey : hkeys) {
            log.info("{} / {}", hkey, hashOps.get(cacheKey, hkey));
        }

        assertEquals("value_5", hashOps.get(cacheKey, "key_5"));
        assertSame(1L, hashOps.delete(cacheKey, "key_5"));
        assertSame(null, hashOps.get(cacheKey, "key_5"));
    }

    /**
     * Set 데이터 구조 처리 - 순서 없음, value 중복 허용 안함
     */
    @Test
    public void opsSet() {
        SetOperations<String, String> setOps = redisTemplate.opsForSet();
        String cacheKey = "valueSet";
        for (int i = 0; i < 10; i++)
            setOps.add(cacheKey, String.valueOf(i));

        assertSame(DataType.SET, redisTemplate.type(cacheKey));
        assertSame(10L, setOps.size(cacheKey));

        log.info("##### opsSet #####");
        log.info("{}", setOps.members(cacheKey));

        assertEquals(true, setOps.isMember(cacheKey, "5"));
    }

    /**
     * SortedSet 데이터 구조 처리 - 순서 있음, value 중복 허용 안함
     */
    @Test
    public void opsSortedSet() {
        ZSetOperations<String, String> zsetOps = redisTemplate.opsForZSet();
        String cacheKey = "valueZSet";
        for (int i = 0; i < 10; i++)
            zsetOps.add(cacheKey, String.valueOf(i), i);

        assertSame(DataType.ZSET, redisTemplate.type(cacheKey));
        assertSame(10L, zsetOps.size(cacheKey));
        log.info("##### opsSortedSet #####");
        log.info("{}", zsetOps.range(cacheKey, 0, 10));
        assertSame(0L, zsetOps.reverseRank(cacheKey, "9"));
    }

    /**
     * Geo 데이터 구조 처리 - 좌표 정보 처리, 타입은 zset으로 저장.
     */
//    @Test
//    public void opsGeo() {
//        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
//        String[] cities = {"서울", "부산"};
//        String[][] gu = {{"강남구", "서초구", "관악구", "동작구", "마포구"}, {"사하구", "해운대구", "영도구", "동래구", "수영구"}};
//        Point[][] pointGu = {{new Point(10, -10), new Point(11, -20), new Point(13, 10), new Point(14, 30), new Point(15, 40)}, {new Point(-100, 10), new Point(-110, 20), new Point(-130, 80), new Point(-140, 60), new Point(-150, 30)}};
//        String cacheKey = "valueGeo";
//
//        // previous key delete
//        redisTemplate.delete(cacheKey);
//
//        for (int x = 0; x < cities.length; x++) {
//            for (int y = 0; y < 5; y++) {
//                geoOps.add(cacheKey, pointGu[x][y], gu[x][y]);
//            }
//        }
//
//        log.info("##### opsGeo #####");
//        Distance distance = geoOps.distance(cacheKey, "강남구", "동작구");
//        assertNotNull(distance);
//        assertEquals(4469610.0767, distance.getValue(), 4);
//        log.info("Distance : {}", distance.getValue());
//        List<Point> position = geoOps.position(cacheKey, "동작구");
//        assertNotNull(position);
//        for (Point point : position) {
//            assertEquals(14.000001847743988d, point.getX(), 4);
//            assertEquals(30.000000249977013d, point.getY(), 4);
//            log.info("Position : {} x {}", point.getX(), point.getY());
//        }
//    }

    /**
     * HyperLogLog 데이터 구조 처리 - 집합의 원소의 개수 추정, 타입은 string으로 저장.
     */
    @Test
    public void opsHyperLogLog() {
        HyperLogLogOperations<String, String> hyperLogLogOps = redisTemplate.opsForHyperLogLog();
        String cacheKey = "valueHyperLogLog";
        String[] arr1 = {"1", "2", "2", "3", "4", "5", "5", "5", "5", "6", "7", "7", "7"};
        hyperLogLogOps.add(cacheKey, arr1);
        log.info("##### opsHyperLogLog #####");
        log.info("count : {}", hyperLogLogOps.size(cacheKey));
        redisTemplate.delete(cacheKey);
    }

    @Test
    public void commonCommand() {
        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        valueOps.set("key1", "key1value");
        valueOps.set("key2", "key2value");
        // Key 타입 조회.
        assertEquals(DataType.STRING, redisTemplate.type("key1"));
        // 존재하는 Key의 개수를 반환.
        assertSame(2L, redisTemplate.countExistingKeys(Arrays.asList("key1", "key2", "key3")));
        // Key가 존재하는지 확인
        assertTrue(redisTemplate.hasKey("key1"));
        // Key 만료 날짜 세팅
        assertTrue(redisTemplate.expireAt("key1", Date.from(LocalDateTime.now().plusDays(1L).atZone(ZoneId.systemDefault()).toInstant())));
        // Key 만료 시간 세팅
        assertTrue(redisTemplate.expire("key1", 60, TimeUnit.SECONDS));
        // Key 만료 시간 조회
        assertThat(redisTemplate.getExpire("key1"), greaterThan(0L));
        // Key 만료 시간 해제
        assertTrue(redisTemplate.persist("key1"));
        // Key 만료시간이 세팅 안되어있는경우 -1 반환
        assertSame(-1L, redisTemplate.getExpire("key1"));
        // Key 삭제
        assertTrue(redisTemplate.delete("key1"));
        // Key 일괄 삭제
        assertThat(redisTemplate.delete(Arrays.asList("key1", "key2", "key3")), greaterThan(0L));
    }

    @Test
    public void redisHash_Insert() {
        long studentId = 1L;
        String name = "행복하라";
        Student student = Student.builder().studentId(studentId).name(name).build();
        redisRepo.save(student);

        Student cachedStudent = redisRepo.findById(studentId).orElse(null);
        assertNotNull(cachedStudent);
        assertEquals(1L, cachedStudent.getStudentId());
        assertEquals(name, cachedStudent.getName());
    }

    @Test
    public void redisHash_Update() {
        long studentId = 1L;
        String name = "행복하라";
        Student student = Student.builder().studentId(studentId).name(name).build();
        student.update("정직하라");
        redisRepo.save(student);

        Student cachedStudent = redisRepo.findById(studentId).orElse(null);
        assertNotNull(cachedStudent);
        assertEquals(1L, cachedStudent.getStudentId());
        assertEquals("정직하라", cachedStudent.getName());
    }

}
