package com.smartfarm.ledger.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaMigrator implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SchemaMigrator.class);

	private final JdbcTemplate jdbc;

	public SchemaMigrator(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!tableExists("batches")) {
			return;
		}
		if (columnExists("batches", "plant_id") && !tableExists("houses")) {
			return;
		}
		if (!tableExists("houses")) {
			throw new IllegalStateException("기존 하우스 테이블이 없어 배치를 온실과 식물로 옮길 수 없습니다.");
		}
		copyMasters();
		ensureBatchColumns();
		backfillBatches();
		assertFilled();
		jdbc.execute("alter table batches modify greenhouse_id int not null");
		jdbc.execute("alter table batches modify plant_id int not null");
		dropHouseLink();
		addForeignKey("fk_batches_greenhouse", "alter table batches add constraint fk_batches_greenhouse foreign key (greenhouse_id) references greenhouses (id)");
		addForeignKey("fk_batches_plant", "alter table batches add constraint fk_batches_plant foreign key (plant_id) references plants (id)");
		addIndex("idx_batches_greenhouse", "greenhouse_id");
		addIndex("idx_batches_plant", "plant_id");
		jdbc.execute("drop table houses");
		log.info("moved fixed houses into greenhouses and plants");
	}

	private void copyMasters() {
		jdbc.update("""
				insert into plants (name, day_temp, night_temp, light_note)
				select h.crop_name, h.day_temp, h.night_temp, h.light_note
				from houses h
				where not exists (select 1 from plants p where p.name = h.crop_name)
				""");
		jdbc.update("""
				insert into greenhouses (id, name)
				select h.id, h.name
				from houses h
				where not exists (select 1 from greenhouses g where g.id = h.id or g.name = h.name)
				""");
		raiseAutoIncrement("plants");
		raiseAutoIncrement("greenhouses");
	}

	private void ensureBatchColumns() {
		if (!columnExists("batches", "greenhouse_id")) {
			jdbc.execute("alter table batches add column greenhouse_id int null");
		}
		if (!columnExists("batches", "plant_id")) {
			jdbc.execute("alter table batches add column plant_id int null");
		}
	}

	private void backfillBatches() {
		if (!columnExists("batches", "house_id")) {
			return;
		}
		jdbc.update("update batches set greenhouse_id = house_id where greenhouse_id is null");
		jdbc.update("""
				update batches b
				join houses h on h.id = b.house_id
				join plants p on p.name = h.crop_name
				set b.plant_id = p.id
				where b.plant_id is null
				""");
	}

	private void assertFilled() {
		Integer missing = jdbc.queryForObject(
				"select count(*) from batches where greenhouse_id is null or plant_id is null",
				Integer.class);
		if (missing != null && missing > 0) {
			throw new IllegalStateException("배치 " + missing + "건을 온실과 식물에 연결하지 못했습니다.");
		}
	}

	private void dropHouseLink() {
		if (!columnExists("batches", "house_id")) {
			return;
		}
		List<String> foreignKeys = jdbc.query(
				"""
				select constraint_name
				from information_schema.key_column_usage
				where table_schema = database()
				  and table_name = 'batches'
				  and column_name = 'house_id'
				  and referenced_table_name is not null
				""",
				(rs, rowNum) -> rs.getString(1));
		for (String foreignKey : foreignKeys) {
			jdbc.execute("alter table batches drop foreign key " + identifier(foreignKey));
		}
		jdbc.execute("alter table batches drop column house_id");
	}

	private void addForeignKey(String name, String sql) {
		Integer count = jdbc.queryForObject(
				"""
				select count(*) from information_schema.table_constraints
				where table_schema = database() and table_name = 'batches' and constraint_name = ?
				""",
				Integer.class,
				name);
		if (count == null || count == 0) {
			jdbc.execute(sql);
		}
	}

	private void addIndex(String name, String column) {
		Integer count = jdbc.queryForObject(
				"""
				select count(*) from information_schema.statistics
				where table_schema = database() and table_name = 'batches' and index_name = ?
				""",
				Integer.class,
				name);
		if (count == null || count == 0) {
			jdbc.execute("create index " + name + " on batches (" + column + ")");
		}
	}

	private void raiseAutoIncrement(String table) {
		Long next = jdbc.queryForObject("select coalesce(max(id), 0) + 1 from " + identifier(table), Long.class);
		jdbc.execute("alter table " + identifier(table) + " auto_increment = " + next);
	}

	private boolean tableExists(String table) {
		Integer count = jdbc.queryForObject(
				"select count(*) from information_schema.tables where table_schema = database() and table_name = ?",
				Integer.class,
				table);
		return count != null && count > 0;
	}

	private boolean columnExists(String table, String column) {
		Integer count = jdbc.queryForObject(
				"""
				select count(*) from information_schema.columns
				where table_schema = database() and table_name = ? and column_name = ?
				""",
				Integer.class,
				table,
				column);
		return count != null && count > 0;
	}

	private static String identifier(String value) {
		if (!value.matches("[A-Za-z0-9_]+")) {
			throw new IllegalStateException("식별자를 확인하지 못했습니다.");
		}
		return value;
	}
}
