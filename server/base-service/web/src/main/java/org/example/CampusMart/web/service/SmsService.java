package org.example.CampusMart.web.service;

import java.util.concurrent.ExecutionException;

public interface SmsService {
    void sendCode(String phone, String code) throws ExecutionException, InterruptedException;
}
