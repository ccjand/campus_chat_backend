package com.ccj.campus.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class CampuschatApplicationTests {

    @Autowired
    private PasswordEncoder passwordEncoder;

    void contextLoads() {
        System.out.println(new BCryptPasswordEncoder(4).encode("123456"));
    }

    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder(4).encode("123456"));
    }

}
