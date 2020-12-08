package fr.smile.poc;

import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

import org.springframework.integration.handler.GenericHandler;
import org.springframework.messaging.MessageHeaders;

/**
 * A very raw logger =D
 */
public class LogHandler implements GenericHandler<byte[]> {
	private String message;
	private boolean displayPayload;

	public LogHandler(String message, boolean displayPayload) {
		super();
		this.message = message;
		this.displayPayload = displayPayload;
	}

	@Override
	public Object handle(byte[] payload, MessageHeaders headers) {
		System.out.println("------------> got a message " + message);
		System.out.println("---> headers");
		for (Entry<String, Object> headerEntry : headers.entrySet()) {
			System.out.println("     " + headerEntry.getKey() + "=" + headerEntry.getValue());
		}
		if (displayPayload) {
			System.out.println("---> payload " + new String(payload, StandardCharsets.UTF_8));
		}
		return payload;
	}
}
