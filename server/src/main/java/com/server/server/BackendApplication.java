package com.server.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.shared.shared.model.Packet;

@SpringBootApplication
public class BackendApplication {


    public static void main(String[] args)
    {
        SpringApplication.run(BackendApplication.class, args);
    }

}
