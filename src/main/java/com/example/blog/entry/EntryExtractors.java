package com.example.blog.entry;

import am.ik.blog.entry.*;
import am.ik.blog.entry.Entry.EntryBuilder;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class EntryExtractors {
	static ResultSetExtractor<Optional<Entry>> forEntry() {
		return rs -> {
			if (!rs.next()) {
				return Optional.empty();
			}
			List<Category> categories = new ArrayList<>();
			EntryBuilder builder = builder(rs, categories);
			do {
				String categoryName = rs.getString("category_name");
				if (!StringUtils.isEmpty(categoryName)) {
					categories.add(new Category(categoryName));
				}
			}
			while (rs.next());
			return Optional.of(builder.build());
		};
	}

	static ResultSetExtractor<List<Entry>> forEntries() {
		return rs -> {
			List<Entry> entries = new ArrayList<>();
			if (rs.next()) {
				long prevId = -1;
				EntryBuilder builder = null;
				List<Category> categories = new ArrayList<>();
				do {
					long entryId = rs.getLong("entry_id");
					if (entryId != prevId) {
						if (builder != null) {
							entries.add(builder.build());
						}
						categories = new ArrayList<>();
						builder = builder(rs, categories);
					}
					categories.add(new Category(rs.getString("category_name")));
					prevId = entryId;
				}
				while (rs.next());
				if (builder != null) {
				    // for last loop
					entries.add(builder.build());
				}
			}
			return entries;
		};
	}

	private static EntryBuilder builder(ResultSet rs, List<Category> categories)
			throws SQLException {
		EventTime createdDate = new EventTime(OffsetDateTime
				.of(rs.getTimestamp("created_date").toLocalDateTime(), ZoneOffset.UTC));
		EventTime lastModifiedDate = new EventTime(OffsetDateTime.of(
				rs.getTimestamp("last_modified_date").toLocalDateTime(), ZoneOffset.UTC));
		return Entry.builder() //
				.entryId(new EntryId(rs.getLong("entry_id"))) //
				.content(new Content(rs.getString("content"))) //
				.frontMatter(new FrontMatter(new Title(rs.getString("title")),
						new Categories(categories), new Tags(Collections.emptyList()),
						createdDate, lastModifiedDate, null)) //
				.created(new Author(new Name(rs.getString("created_by")), createdDate)) //
				.updated(new Author(new Name(rs.getString("last_modified_by")),
						lastModifiedDate));
	}
}
