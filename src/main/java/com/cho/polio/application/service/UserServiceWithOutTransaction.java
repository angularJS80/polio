package com.cho.polio.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceWithOutTransaction {

    private final UserCash userCash;
    private final UserService userService;

    @Async
    public void change(String nextName) {
        userService.waitForReadyData(nextName);
        userService.change(nextName);
        // max wait: 5초
        /*long maxWaitMillis = 50000;
        long start = System.currentTimeMillis();

        if(!userCash.getNextNameInProgress().containsKey(nextName)){

            while (!userCash.getNextNameInProgress().containsKey(nextName)) {
                if (System.currentTimeMillis() - start > maxWaitMillis) {
                    throw new RuntimeException("Timeout waiting for someone to begin: " + nextName);
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted", e);
                }
            }
        }

        if(userCash.getNextNameInProgress().containsKey(nextName)){
            System.out.println("in-success");
            while ("IN".equals(userCash.getNextNameInProgress().get(nextName))) {

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted", e);
                }
            }
        }*/



    }

}
