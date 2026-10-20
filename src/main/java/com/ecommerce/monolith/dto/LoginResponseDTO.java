package com.ecommerce.monolith.dto;

public record LoginResponseDTO(
       String token,
       String type ,
       Long expiresIn,
       UserResponseDTO user
) {
    public LoginResponseDTO(String token, Long expiresIn, UserResponseDTO user) {
        // Calls the main constructor, inserting "Bearer" by default
        this(token, "Bearer", expiresIn, user);
    }
}
