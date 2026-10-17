package com.waymate.service.payload.response;

import com.waymate.service.model.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

	private UUID id;
	private String name;
	private String email;
	private String avatar;
	private Long point;
	private String phoneNumber;
	private String bio;
	private LocalDate dateOfBirth;
	private String role;
	private String status;
	private Instant lastLogin;
	private Instant createdAt;
	private Instant updatedAt;

	public static UserResponse fromUser(User user) {
		if (user == null) {
			return null;
		}
		return UserResponse.builder()
			.id(user.getId())
			.name(user.getName())
			.email(user.getEmail())
			.avatar(user.getAvatar())
			.point(user.getPoint())
			.phoneNumber(user.getPhoneNumber())
			.bio(user.getBio())
			.dateOfBirth(user.getDateOfBirth())
			.role(user.getRole())
			.status(user.getStatus())
			.lastLogin(user.getLastLogin())
			.createdAt(user.getCreatedAt())
			.updatedAt(user.getUpdatedAt())
			.build();
	}
}