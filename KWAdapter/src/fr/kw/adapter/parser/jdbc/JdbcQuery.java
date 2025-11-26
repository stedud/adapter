package fr.kw.adapter.parser.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.kw.adapter.parser.event.Event;

public class JdbcQuery {

	protected String name;
	protected String select;
	private Map<String, List<JdbcUpdate>> update = new HashMap();

	protected JdbcQuery parent;
	protected List<JdbcQuery> children = new ArrayList<>();
	protected String dataSourceId;

	public void execute(Event event) {// TODO

		for (UpdateWhen when : UpdateWhen.values()) {
			update.put(when.toString(), new ArrayList<JdbcUpdate>());
		}

	}

	public void addUpdate(JdbcUpdate update) {
		this.update.get(update.when.name()).add(update);
	}

	@Override
	public String toString() {
		return "JdbcQuery [name=" + name + ", dataSourceId=" + dataSourceId + "]";
	}

	public List<JdbcUpdate> getUpdates(UpdateWhen when) {
		return this.update.get(when.name());
	}
}
