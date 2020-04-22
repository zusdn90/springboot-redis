package com.demo.redis.controller;

import com.demo.redis.pubsub.RedisPublisher;
import com.demo.redis.pubsub.RedisSubscriber;
import com.demo.redis.pubsub.RoomMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@RequestMapping("/pubsub")
@RestController
public class PubSubController {
    private final RedisMessageListenerContainer redisMessageListener;

    private final RedisPublisher redisPublisher;
    private final RedisSubscriber redisSubscriber;
    private Map<String, ChannelTopic> channels;

    @PostConstruct
    public void init() {
        channels = new HashMap<>();
    }

    @GetMapping("/room")
    public Set<String> findAllRoom() {
        return channels.keySet();
    }

    // create Topic
    @PutMapping("/room/{roomId}")
    public void createRoom(@PathVariable String roomId) {
        ChannelTopic channel = new ChannelTopic(roomId);
        redisMessageListener.addMessageListener(redisSubscriber, channel);
        channels.put(roomId, channel);
    }

    // send message to Topic
    @PostMapping("/room/{roomId}")
    public void pushMessage(@PathVariable String roomId
                          , @RequestParam String name
                          , @RequestParam String message) {

        ChannelTopic channel = channels.get(roomId);
        redisPublisher.publish(channel, RoomMessage.builder().name(name).roomId(roomId).message(message).build());
    }

    @DeleteMapping("/room/{roomId}")
    public void deleteRoom(@PathVariable String roomId) {
        ChannelTopic channel = channels.get(roomId);
        redisMessageListener.removeMessageListener(redisSubscriber, channel);
        channels.remove(roomId);
    }

}
