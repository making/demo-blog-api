package com.example.blog.entry;

import com.example.blog.entry.event.EntryEvent;
import com.example.blog.entry.event.EventNotifyer;
import org.reactivestreams.Publisher;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventNotificationController {
	private final EventNotifyer eventNotifyer;

	public EventNotificationController(EventNotifyer eventNotifyer) {
		this.eventNotifyer = eventNotifyer;
	}

	@GetMapping(path = "v1/notification", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Publisher<EntryEvent> notification() {
		return this.eventNotifyer.publisher();
	}
}
