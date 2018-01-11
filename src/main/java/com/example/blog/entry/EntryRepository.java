package com.example.blog.entry;

import am.ik.blog.entry.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.*;

@Repository
public class EntryRepository {
	private final NamedParameterJdbcTemplate jdbcTemplate;

	public EntryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public Optional<Entry> findById(EntryId entryId) {
		MapSqlParameterSource source = new MapSqlParameterSource() //
				.addValue("entry_id", entryId.getValue());
		return this.jdbcTemplate.query(
				"SELECT e.entry_id, e.title, e.content, e.created_by, e.created_date, e.last_modified_by, e.last_modified_date, c.category_name"
						+ " FROM entry AS e LEFT OUTER JOIN category AS c ON e.entry_id = c.entry_id"
						+ " WHERE e.entry_id = :entry_id"
						+ " ORDER BY c.category_order ASC",
				source, EntryExtractors.forEntry()) //
				.map(e -> {
					List<Tag> tags = this.jdbcTemplate.query(
							"SELECT tag_name FROM entry_tag WHERE entry_id = :entry_id",
							source, (rs, i) -> new Tag(rs.getString("tag_name")));
					FrontMatter fm = e.getFrontMatter();
					return e.copy()
							.frontMatter(new FrontMatter(fm.title(), fm.categories(),
									new Tags(tags), fm.date(), fm.updated(), fm.point()))
							.build();
				});
	}

	@Transactional(readOnly = true)
	public List<Entry> findAll() {
		List<Long> ids = this.jdbcTemplate.query(
				"SELECT entry_id FROM entry ORDER BY last_modified_date DESC LIMIT 50",
				(rs, i) -> rs.getLong("entry_id"));
		MapSqlParameterSource source = new MapSqlParameterSource() //
				.addValue("entry_ids", ids);
		List<Entry> entries = this.jdbcTemplate.query(
				"SELECT e.entry_id, e.title, e.content, e.created_by, e.created_date, e.last_modified_by, e.last_modified_date, c.category_name"
						+ " FROM entry AS e LEFT OUTER JOIN category AS c ON e.entry_id = c.entry_id"
						+ " WHERE e.entry_id IN (:entry_ids)"
						+ " ORDER BY e.last_modified_date DESC, c.category_order ASC",
				source, EntryExtractors.forEntries());
		Map<EntryId, Tags> tagsMap = this.tagsMap(ids);
		return entries.stream() //
				.map(e -> {
					FrontMatter fm = e.getFrontMatter();
					return e.copy()
							.frontMatter(new FrontMatter(fm.title(), fm.categories(),
									tagsMap.get(e.getEntryId()), fm.date(), fm.updated(),
									fm.point()))
							.build();
				}) //
				.collect(toList());
	}

	@Transactional
	public void create(Entry entry) {
		SqlParameterSource entrySource = entrySource(entry);
		SqlParameterSource[] categorySources = categorySources(entry);
		SqlParameterSource[] tagSources = tagSources(entry);
		this.jdbcTemplate.update(
				"INSERT INTO entry(entry_id, title, content, created_by, created_date, last_modified_by, last_modified_date)"
						+ " VALUES(:entry_id, :title, :content, :created_by, :created_date, :last_modified_by, :last_modified_date)",
				entrySource);
		this.jdbcTemplate.batchUpdate(
				"INSERT INTO category(category_order, entry_id, category_name)"
						+ " VALUES(:category_order, :entry_id, :category_name)",
				categorySources);
		this.jdbcTemplate.batchUpdate("INSERT INTO entry_tag(entry_id, tag_name)"
				+ " VALUES(:entry_id, :tag_name)", tagSources);
	}

	@Transactional
	public void update(Entry entry) {
		SqlParameterSource entrySource = entrySource(entry);
		SqlParameterSource[] categorySources = categorySources(entry);
		SqlParameterSource[] tagSources = tagSources(entry);
		this.jdbcTemplate.update(
				"UPDATE entry SET title=:title, content=:content, created_by=:created_by, created_date=:created_date, last_modified_by=:last_modified_by, last_modified_date=:last_modified_date"
						+ " WHERE  entry_id = :entry_id",
				entrySource);
		this.jdbcTemplate.batchUpdate("DELETE FROM category WHERE entry_id = :entry_id",
				categorySources);
		this.jdbcTemplate.batchUpdate("DELETE FROM entry_tag WHERE entry_id = :entry_id",
				categorySources);
		this.jdbcTemplate.batchUpdate(
				"INSERT INTO category(category_order, entry_id, category_name)"
						+ " VALUES(:category_order, :entry_id, :category_name)",
				categorySources);
		this.jdbcTemplate.batchUpdate("INSERT INTO entry_tag(entry_id, tag_name)"
				+ " VALUES(:entry_id, :tag_name)", tagSources);
	}

	@Transactional
	public void delete(EntryId entryId) {
		SqlParameterSource source = new MapSqlParameterSource() //
				.addValue("entry_id", entryId.getValue());
		this.jdbcTemplate.update("DELETE FROM entry WHERE entry_id = :entry_id", source);
	}

	Map<EntryId, Tags> tagsMap(List<Long> ids) {
		MapSqlParameterSource source = new MapSqlParameterSource() //
				.addValue("entry_ids", ids);
		return this.jdbcTemplate.query(
				"SELECT entry_id, tag_name FROM entry_tag WHERE entry_id IN (:entry_ids)",
				source,
				(rs, i) -> Tuples.of(new EntryId(rs.getLong("entry_id")),
						new Tag(rs.getString("tag_name"))))
				.stream() //
				.collect(groupingBy(Tuple2::getT1)) //
				.entrySet() //
				.stream() //
				.map(e -> Tuples.of(e.getKey(), new Tags(e.getValue() //
						.stream() //
						.map(Tuple2::getT2) //
						.collect(toList()))))
				.collect(toMap(Tuple2::getT1, Tuple2::getT2));
	}

	private SqlParameterSource entrySource(Entry entry) {
		return new MapSqlParameterSource() //
				.addValue("entry_id", entry.getEntryId().getValue()) //
				.addValue("title", entry.getFrontMatter().getTitle().getValue()) //
				.addValue("content", entry.getContent().getValue()) //
				.addValue("created_by", entry.getCreated().getName().getValue()) //
				.addValue("created_date", entry.getCreated().getDate().getValue()) //
				.addValue("last_modified_by", entry.getUpdated().getName().getValue()) //
				.addValue("last_modified_date", entry.getUpdated().getDate().getValue());
	}

	private SqlParameterSource[] categorySources(Entry entry) {
		AtomicInteger categoryOrder = new AtomicInteger(0);
		return entry.getFrontMatter().getCategories().stream()
				.map(c -> new MapSqlParameterSource() //
						.addValue("category_order", categoryOrder.getAndIncrement()) //
						.addValue("entry_id", entry.getEntryId().getValue()) //
						.addValue("category_name", c.getValue())) //
				.toArray(SqlParameterSource[]::new);
	}

	private SqlParameterSource[] tagSources(Entry entry) {
		return entry.getFrontMatter().getTags().stream()
				.map(t -> new MapSqlParameterSource() //
						.addValue("entry_id", entry.getEntryId().getValue()) //
						.addValue("tag_name", t.getValue())) //
				.toArray(SqlParameterSource[]::new);
	}

}
