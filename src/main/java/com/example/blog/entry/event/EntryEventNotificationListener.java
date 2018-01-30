package com.example.blog.entry.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EntryEventNotificationListener {
	private final Logger log = LoggerFactory
			.getLogger(EntryEventNotificationListener.class);
	private final EventNotifyer eventNotifyer;

	public EntryEventNotificationListener(EventNotifyer eventNotifyer) {
		this.eventNotifyer = eventNotifyer;
	}

	@EventListener
	public void onEvent(EntryEvent event) {
		log.info("Notify {}", event);
		this.eventNotifyer.notify(event);
	}
}
