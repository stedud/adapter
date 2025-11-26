package fr.sap.otf.parser.object;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class Header {

	private List<HeaderEntry> headers = new ArrayList<HeaderEntry>();

	public Header() {
		// TODO Auto-generated constructor stub
	}

	public boolean isHeader(String line) {
		return StringUtils.startsWith(line, "*");
	}

	public void parse(String line) {// *HEADERNAME=HEADERVALUE
		if (isHeader(line)) {
			String name = StringUtils.substringBefore(StringUtils.substring(line, 1), "=");
			String value = StringUtils.substringAfter(StringUtils.substring(line, 1), "=");
			HeaderEntry header = new HeaderEntry();
			header.setName(name);
			header.setValue(value);
			header.setLine(line);
			getHeaders().add(header);
		}
	}

	public List<HeaderEntry> getHeaders() {
		return headers;
	}
}
