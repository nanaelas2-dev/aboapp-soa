package ma.telecomatlas.abo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import ma.telecomatlas.abo.outils.DateUtil;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class AbstractDao {

	protected Logger log = Logger.getLogger(getClass());

	protected JdbcTemplate jdbcTemplate;

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	protected Date date(ResultSet rs, String col) throws SQLException {
		return DateUtil.depuisSql(rs.getString(col));
	}

	protected String sql(Date d) {
		return DateUtil.versSql(d);
	}

	protected int dernierId() {
		return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
	}
}
