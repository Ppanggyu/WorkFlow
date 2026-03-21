package com.workflow.common.context;

import java.util.UUID;

public class UUIDContext {
	
	private static final ThreadLocal<UUID> threadUUID = new ThreadLocal<>();
	
	public static void set(UUID uuid) {
		threadUUID.set(uuid);
	}
	
	public static UUID get() {
		return threadUUID.get();
	}
	
	public static void clear() {
		threadUUID.remove();
	}

}
