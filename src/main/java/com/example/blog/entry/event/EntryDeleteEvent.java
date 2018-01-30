package com.example.blog.entry.event;

import am.ik.blog.entry.EntryId;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class EntryDeleteEvent implements EntryEvent {
	private final EntryId entryId;

	public EntryDeleteEvent(EntryId entryId) {
		this.entryId = entryId;
	}

	@JsonUnwrapped
	public EntryId getEntryId() {
		return entryId;
	}
}
