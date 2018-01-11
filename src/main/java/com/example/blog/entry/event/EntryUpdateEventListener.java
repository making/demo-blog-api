package com.example.blog.entry.event;

import am.ik.blog.entry.Entry;
import com.example.blog.entry.EntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EntryUpdateEventListener {
	private final Logger log = LoggerFactory.getLogger(EntryUpdateEventListener.class);
	private final EntryRepository entryRepository;

	public EntryUpdateEventListener(EntryRepository entryRepository) {
		this.entryRepository = entryRepository;
	}

	@EventListener
	public void onUpdate(EntryUpdateEvent event) {
		Entry entry = event.getEntry();
		log.info("Update {}", entry.getEntryId());
		this.entryRepository.update(entry);
	}
}
