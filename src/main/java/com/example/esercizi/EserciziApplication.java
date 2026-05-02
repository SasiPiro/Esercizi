package com.example.esercizi;

import com.example.esercizi.model.User;
import com.example.esercizi.repository.UserRepository;
import com.example.esercizi.service.PaymentService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EserciziApplication {

	public static void main(String[] args) {
		SpringApplication.run(EserciziApplication.class, args);
	}

}
