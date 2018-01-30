package com.example.blog.entry.event;

public interface EntryEvent {

	default Type getType() {
		String name = this.getClass().getSimpleName();
		String type = name.replace("Entry", "").replace("Event", "").toUpperCase();
		return Type.valueOf(type);
	}

	enum Type {
		CREATE, UPDATE, DELETE
	}
}
