package com.ecommerce.monolith;

import com.ecommerce.monolith.model.User;
import com.ecommerce.monolith.repository.UserRepository;
import com.ecommerce.monolith.service.PaymentService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EcommerceMonolithApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcommerceMonolithApplication.class, args);
	}

}
