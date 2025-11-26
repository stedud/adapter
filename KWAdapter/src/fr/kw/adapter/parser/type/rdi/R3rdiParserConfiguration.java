package fr.kw.adapter.parser.type.rdi;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import fr.kw.adapter.parser.IParserConfiguration;
import fr.kw.adapter.parser.process.ParseProcessConfiguration;
import fr.kw.adapter.parser.type.fieldin.FieldParserConfiguration;

public class R3rdiParserConfiguration implements IParserConfiguration {

	protected Map<String, String> metadata = new HashMap<String, String>();
	protected String id;
	protected boolean simple = false;
	protected String charset = null;
	protected ParseProcessConfiguration rootConfiguration;

	public R3rdiParserConfiguration() {

	}

	@Override
	public void load(File descriptionFile, ParseProcessConfiguration mainConf) throws IOException {

		this.rootConfiguration = mainConf;
		List<String> lines = FileUtils.readLines(descriptionFile, StandardCharsets.UTF_8.name());

		for (String line : lines) {
			line = StringUtils.removeEnd(line.trim(), ";").trim();

			if (StringUtils.startsWithIgnoreCase(line, "R3RDI")) {
				String[] tokens = StringUtils.split(line.trim(), '"');
				if (tokens.length <= 1)
					throw new IOException("ID not set for r3rdi descriptor " + descriptionFile.getPath());
				String tmpId = tokens[1];
				this.setId(tmpId);
			} else if (StringUtils.startsWithIgnoreCase(line, "MODE")) {
				String[] tokens = StringUtils.split(line.trim(), ' ');
				if (tokens.length >= 2) {
					String mode = tokens[1];
					if (mode.toLowerCase().contains("simple"))
						this.simple = true;
				}
			} else if (StringUtils.startsWithIgnoreCase(line, "ENCODING")) {
				String[] tokens = StringUtils.split(line.trim(), ' ');
				if (tokens.length >= 2) {
					String charset = tokens[1];
					this.charset = charset;

				}
			} else if (StringUtils.startsWithIgnoreCase(line, "METADATA")) {
				Map<String, String> metadataMap = FieldParserConfiguration.parseMetadata(line);
				this.metadata.putAll(metadataMap);
			}
		}
		if (StringUtils.isBlank(this.charset)) {
			this.charset = mainConf.get("ENCODING", StandardCharsets.UTF_8.name());
		}
	}

	@Override
	public String getId() {

		return this.id;
	}

	@Override
	public void setId(String id) {
		this.id = id;

	}

	public boolean isSimple() {
		return simple;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	@Override
	public ParseProcessConfiguration getRootConfiguration() {

		return this.rootConfiguration;
	}

}
