package com.example.blog.entry;

import am.ik.blog.entry.Entry;
import am.ik.blog.entry.EntryId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class EntryController {
	private final EntryRepository entryRepository;

	public EntryController(EntryRepository entryRepository) {
		this.entryRepository = entryRepository;
	}

	@GetMapping("v1/entries")
	public List<Entry> getEntries() {
		return this.entryRepository.findAll();
	}

	@GetMapping("v1/entries/{entryId}")
	public Entry getEntry(@PathVariable EntryId entryId) {
		return this.entryRepository.findById(entryId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"EntryId " + entryId + " is not found."));
	}
}
