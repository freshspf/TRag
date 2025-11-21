package com.spf.tbackend.controller;

import com.spf.tbackend.service.RedisService;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/redis")
public class RedisTestController {

    private final RedisService redisService;

    public RedisTestController(RedisService redisService) {
        this.redisService = redisService;
    }

    @PostMapping("/set")
    public String set(@RequestParam String key, @RequestParam String value) {
        try {
            redisService.setString(key, value);
            return "Success: Set key '" + key + "' with value '" + value + "'";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/set-with-expiry")
    public String setWithExpiry(@RequestParam String key, 
                               @RequestParam String value,
                               @RequestParam long timeout) {
        try {
            redisService.setString(key, value, timeout, TimeUnit.SECONDS);
            return "Success: Set key '" + key + "' with value '" + value + "' and expiry " + timeout + " seconds";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/get")
    public String get(@RequestParam String key) {
        try {
            Object value = redisService.getString(key);
            if (value == null) {
                return "Key '" + key + "' not found";
            }
            return "Value: " + value.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @DeleteMapping("/delete")
    public String delete(@RequestParam String key) {
        try {
            boolean deleted = redisService.delete(key);
            if (deleted) {
                return "Success: Deleted key '" + key + "'";
            } else {
                return "Key '" + key + "' not found";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/exists")
    public String exists(@RequestParam String key) {
        try {
            boolean exists = redisService.hasKey(key);
            if (exists) {
                return "Key '" + key + "' exists";
            } else {
                return "Key '" + key + "' does not exist";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}