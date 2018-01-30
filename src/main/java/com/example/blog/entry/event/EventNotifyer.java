package com.example.blog.entry.event;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.UnicastProcessor;

import org.springframework.stereotype.Component;

@Component
public class EventNotifyer {
	final UnicastProcessor<EntryEvent> processor = UnicastProcessor.create();
	final Flux<EntryEvent> flux;

	public EventNotifyer() {
		this.flux = this.processor.publish().autoConnect().log("event").share();
	}

	public void notify(EntryEvent event) {
		this.processor.onNext(event);
	}

	public Publisher<EntryEvent> publisher() {
		return this.flux;
	}

}
