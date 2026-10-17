package com.waymate.service.payload.request;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequest {
	private String name;
	private String email;
	private String password;
	private String avatar;
	private String phoneNumber;
	private String bio;
	private LocalDate dateOfBirth;
}