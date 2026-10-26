package com.waymate.service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service bridging chat messages with the WayMate AI Agent (FastAPI / Ollama backend).
 * Detects @WayMate mentions, prompts the agent, and generates responses.
 */
@Service
@Slf4j
public class AiAgentBridgeService {

	@Value("${waymate.ai-agent.base-url:http://localhost:8000}")
	private String aiAgentBaseUrl;

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	private static final Pattern AI_TAG_PATTERN = Pattern.compile("(?i)@(WayMate(?:\\s+(?:AI|Agent))?|AI|Agent)");

	public AiAgentBridgeService() {
		this.objectMapper = new ObjectMapper();
		this.httpClient = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(5))
			.build();
	}

	/**
	 * Check if message text mentions @WayMate, @WayMate AI, or @WayMate Agent.
	 */
	public boolean containsAiMention(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		return AI_TAG_PATTERN.matcher(text).find();
	}

	/**
	 * Extract question prompt by removing the @WayMate tag.
	 */
	public String extractAiPrompt(String text) {
		if (text == null) {
			return "Xin chào WayMate AI! Bạn có thể giúp gì cho tôi?";
		}
		Matcher matcher = AI_TAG_PATTERN.matcher(text);
		String cleaned = matcher.replaceAll("").trim();
		if (cleaned.isEmpty() || cleaned.equalsIgnoreCase("agent") || cleaned.equalsIgnoreCase("ai")) {
			return "Xin chào WayMate AI! Bạn có thể giới thiệu về các tính năng và hỗ trợ phượt của mình không?";
		}
		return cleaned;
	}

	/**
	 * Asynchronously call waymate-agent to get an AI response.
	 */
	public String queryAiAgent(String prompt, String sessionId, String userId) {
		try {
			String endpoint = aiAgentBaseUrl.replaceAll("/+$", "") + "/api/chat";
			String safePrompt = (prompt != null && !prompt.isBlank()) ? prompt : "Xin chào WayMate AI!";
			Map<String, Object> payload = new java.util.HashMap<>();
			payload.put("message", safePrompt);
			payload.put("prompt", safePrompt);
			payload.put("session_id", (sessionId != null && !sessionId.isBlank()) ? sessionId : "room_session_" + System.currentTimeMillis());
			payload.put("user_id", (userId != null && !userId.isBlank()) ? userId : "waymate_rider");

			String requestBody = objectMapper.writeValueAsString(payload);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(endpoint))
				.version(HttpClient.Version.HTTP_1_1)
				.timeout(Duration.ofSeconds(45))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();

			log.info("Sending prompt to WayMate AI Agent: {}", prompt);
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				JsonNode root = objectMapper.readTree(response.body());
				if (root.has("response")) {
					return root.get("response").asText();
				}
			}
			log.warn("WayMate AI Agent returned status {}: {}", response.statusCode(), response.body());
		} catch (Exception e) {
			log.warn("Failed to reach WayMate AI Agent at {}: {}", aiAgentBaseUrl, e.getMessage());
		}

		// Fallback intelligent motorcycle & ride assistant reply if agent is starting
		return generateOfflineAssistantResponse(prompt);
	}

	private String generateOfflineAssistantResponse(String prompt) {
		String lower = prompt.toLowerCase();
		if (lower.contains("nhớt") || lower.contains("oil") || lower.contains("bảo dưỡng") || lower.contains("maintenance")) {
			return "🤖 [WayMate AI]: Đối với xe máy & phân khối lớn, nên thay nhớt định kỳ sau mỗi 2,000 - 3,000 km (nhớt bán tổng hợp) hoặc 4,000 - 5,000 km (nhớt tổng hợp toàn phần 100% Synthetic). Nhớ kiểm tra thêm lọc nhớt và áp suất lốp trước các chuyến đi xa nhé!";
		}
		if (lower.contains("đèo") || lower.contains("pass") || lower.contains("tour") || lower.contains("trip") || lower.contains("đà lạt") || lower.contains("hà giang")) {
			return "🤖 [WayMate AI]: Tuyệt vời! Đi cung này hãy chú ý kiểm tra hệ thống phanh trước/sau, lốp xe và đổ đầy bình xăng trước khi vào cung đường đèo. Nên giữ khoảng cách an toàn tối thiểu 15-20m giữa các xe trong đoàn!";
		}
		if (lower.contains("thời tiết") || lower.contains("weather") || lower.contains("mưa") || lower.contains("rain")) {
			return "🤖 [WayMate AI]: Trước khi xuất phát, bạn hãy chuẩn bị sẵn bộ quần áo mưa bộ phản quang, bọc chống nước cho balo và kiểm tra rãnh lốp để tránh hiện tượng trượt nước (aquaplaning).";
		}
		return "🤖 [WayMate AI]: Chào bạn! Mình là Trợ lý AI WayMate sẵn sàng hỗ trợ các tay lái về kỹ thuật xe, gợi ý cung đường phượt, mẹo an toàn và bảo dưỡng garage. Bạn cần hỗ trợ gì cho chuyến đi sắp tới?";
	}
}
