package com.example.blog.entry;

import java.util.List;

import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryId;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EntryController {
	private final EntryRepository entryRepository;

	public EntryController(EntryRepository entryRepository) {
		this.entryRepository = entryRepository;
	}

	@GetMapping("v1/entries")
	public List<Entry> getEntries() {
		return null; // TODO: 演習1
	}

	@GetMapping("v1/entries/{entryId}")
	public Entry getEntry(@PathVariable EntryId entryId) {
		return null; // TODO: 演習1
	}
}
