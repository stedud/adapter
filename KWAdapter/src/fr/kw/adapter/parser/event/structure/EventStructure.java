package fr.kw.adapter.parser.event.structure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;

import fr.kw.adapter.parser.DataDescriptionException;
import fr.kw.adapter.parser.event.Record;
import fr.utils.Utils;

public class EventStructure extends RecordStructure {

	public static void main(String[] args) {

		try {
			EventStructure structure = load(new File(args[0]));
			System.out.print(structure.toXML());
		} catch (DataDescriptionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	

	public static EventStructure load(File eventStructureDef) throws DataDescriptionException {

		if (! eventStructureDef.exists()) return null;
		EventStructure eventStructure = new EventStructure(FilenameUtils.getBaseName(eventStructureDef.getName()));

		RecordStructure currentBlock = null;

		try {
			if (eventStructureDef.exists()) {
				BufferedReader br = new BufferedReader(new FileReader(eventStructureDef));
				String line = null;
				while ((line = br.readLine()) != null) {
					line = StringUtils.removeEnd(line.trim(), ";");
					if (StringUtils.startsWithIgnoreCase(line, "field")) {
						StringTokenizer st = new StringTokenizer(line, ' ', '"');
						List<String> tokens = st.getTokenList();
						String fieldName = Utils.normalizeXmlName(
								StringUtils.removeEnd(StringUtils.removeStart(tokens.get(1), "\""), "\""));
						if (currentBlock != null)
							currentBlock.getFields().put(fieldName, new FieldDefinition(fieldName));

					}
					if (StringUtils.startsWithIgnoreCase(line.trim(), "jdbc(")
							|| (StringUtils.startsWithIgnoreCase(line.trim(), "script")
									&& StringUtils.contains(line.trim(), "("))) {

						// TODO : JDBC definition JDBC(jdbs/myJdbcSettings.jdbc)
						currentBlock.addExternalDataDefinition(line.trim());

					} else if (StringUtils.startsWithIgnoreCase(line, "block")) {
						StringTokenizer st = new StringTokenizer(line, ' ', '"');
						List<String> tokens = st.getTokenList();
						String recordName = Utils.normalizeXmlName(
								StringUtils.removeEnd(StringUtils.removeStart(tokens.get(1), "\""), "\""));
						RecordStructure newRecord = new RecordStructure(recordName, currentBlock);
						// newRecord.setParent(currentBlock);
						currentBlock.getChildren().put(recordName, newRecord);
						currentBlock = newRecord;

					} else if (StringUtils.startsWithIgnoreCase(line, "root name")) {
						currentBlock = eventStructure;

					} else if (StringUtils.startsWithIgnoreCase(line, "end")) {
						if (currentBlock != null)
							currentBlock = currentBlock.parent;

					}
				}

			}
		} catch (IOException e) {
			throw new DataDescriptionException(
					"Could not load event structure " + eventStructureDef.getPath() + ":" + e.getMessage(), e);
		}
		return eventStructure;
	}

	public EventStructure(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

}
