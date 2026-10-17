package com.waymate.service.payload.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleLoginRequest {
	private String code;
	private String redirectUri;
	private String idToken;
	private String accessToken;
	private String email;
	private String name;
	private String avatar;
	private String googleId;
}