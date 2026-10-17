package com.waymate.service.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * User Neo4j Node entity.
 * Represents registered riders and members in the Waymate ecosystem.
 */
@Node("User")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends BaseModel {

	@Id
	@GeneratedValue(GeneratedValue.UUIDGenerator.class)
	@Property("id")
	private UUID id;

	@Property("name")
	private String name;

	@Property("email")
	private String email;

	@Property("password")
	private String password;

	@Property("avatar")
	private String avatar;

	@Property("point")
	@Builder.Default
	private Long point = 0L;

	@Property("phone_number")
	private String phoneNumber;

	@Property("bio")
	private String bio;

	@Property("date_of_birth")
	private LocalDate dateOfBirth;

	@Property("role")
	@Builder.Default
	private String role = "USER";

	@Property("status")
	@Builder.Default
	private String status = "ACTIVE";

	@Property("last_login")
	private Instant lastLogin;
}