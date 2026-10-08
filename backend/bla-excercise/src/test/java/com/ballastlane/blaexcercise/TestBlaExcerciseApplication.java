package com.ballastlane.blaexcercise;

import org.springframework.boot.SpringApplication;

public class TestBlaExcerciseApplication {

    public static void main(String[] args) {
        SpringApplication.from(BlaExcerciseApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
