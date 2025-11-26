package fr.sap.otf.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import fr.sap.otf.parser.geometry.Box;
import fr.sap.otf.parser.object.Command;
import fr.sap.otf.parser.object.Header;
import fr.sap.otf.parser.object.HeaderEntry;
import fr.sap.otf.parser.object.Page;

public class OTFParser {

	private File otfFile;
	private Header header = new Header();
	private Command endOfStream = null;
	private Command startOfStream = null;
	private BufferedReader reader;
	private String currentLine = null;
	private boolean endOfStreamReached = false;
	private long lineNum = 0;

	public OTFParser(File otf) throws IOException {
		this.otfFile = otf;
		this.reader = new BufferedReader(new FileReader(otfFile));
		initHeaders();

	}

	public static void main(String[] args) {
		File otf = new File(args[0]);

		OTFParser parser;
		try {
			parser = new OTFParser(otf);
			for (HeaderEntry header : parser.header.getHeaders()) {
				System.out.println("Header " + header.getName() + "=" + header.getValue());
			}
			Box textExtract = new Box();
			textExtract.getPosition().setX(4241);
			textExtract.getPosition().setY(421);
			textExtract.getDimension().setW(2101);
			textExtract.getDimension().setH(2000);
			Page page;
			int nbPages = 0;
			do {

				page = parser.getNextPage();
				if (page != null) {
					nbPages++;
					// System.out.println("Page " + nbPages + " found, contents:" +
					// page.getContents().size() + " items");
					String text = page.getText(textExtract, true);
					// System.out.println("Text Extract = " + text);

					for (Command command : page.getContents()) {
						if (StringUtils.compare(command.getID(), "ST") == 0) {
							Box box = page.findBox(command);
							// System.out.println("Text '" + command.getParameters().get(2).getValue()
							// + "', location (twp)=" + box.getPosition().getX() + ", " +
							// box.getPosition().getY()
							// + ", " + box.getDimension().getW() + ", " + box.getDimension().getH());
							// System.out.println("Text '" + command.getParameters().get(2).getValue()
							// + "', location (pt)=" + Page.twipToPoint(box.getPosition().getX()) + ", "
							// + Page.twipToPoint(box.getPosition().getY()) + ", "
							// + Page.twipToPoint(box.getDimension().getW()) + ", "
							// + Page.twipToPoint(box.getDimension().getH()));

						}

					}

				} else {
					parser.endOfStreamReached = true;
				}
			} while (!parser.endOfStreamReached);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void initHeaders() throws IOException {
		lineNum = 0;
		currentLine = reader.readLine();
		lineNum++;
		while (header.isHeader(currentLine)) {
			header.parse(currentLine);
			currentLine = reader.readLine();
			lineNum++;
		}
	}

	public Page getNextPage() {

		if (endOfStreamReached)
			return null;

		Command previousCommand = null;
		Command command = null;
		Page page = new Page();
		boolean startPageFound = false;
		do {

			if (currentLine == null) {
				endOfStreamReached = true;
				command = null;

			} else {
				command = Command.parse(currentLine);
				if (command == null) {
					try {
						currentLine = reader.readLine();
						lineNum++;

					} catch (IOException e) {
						currentLine = null;
					}
					continue;
				}
				command.setPreviousCommand(previousCommand);

				if (StringUtils.compare(command.getID(), "OP") == 0)
					startPageFound = true;
				if (currentLine == null) {
					endOfStreamReached = true;
					endOfStream = command;
				} else if (StringUtils.startsWith(StringUtils.trimToEmpty(currentLine), "//")
						&& StringUtils.compare(StringUtils.trimToEmpty(currentLine), "//") != 0) {
					startOfStream = command;

				} else {
					if (command != null)
						page.getContents().add(command);
				}

				previousCommand = command;
				try {
					currentLine = reader.readLine();
					lineNum++;

				} catch (IOException e) {
					currentLine = null;
				}
				if (currentLine == null) {
					endOfStreamReached = true;
					// endOfStream = Command.parse(currentLine);
				}
			}

		} while (currentLine != null && command != null && StringUtils.compare(command.getID(), "EP") != 0);

		if (currentLine == null) {
			endOfStreamReached = true;

		}
		if (endOfStreamReached)
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		if (!startPageFound) {
			return null;
		} else {
			return page;
		}

	}

	public boolean isEndOfStreamReached() {
		return endOfStreamReached;
	}

	public void setEndOfStreamReached(boolean endOfStreamReached) {
		this.endOfStreamReached = endOfStreamReached;
	}

	public File getOtfFile() {
		return otfFile;
	}

	public Header getHeader() {
		return header;
	}

	public Command getEndOfStream() {
		return endOfStream;
	}

	public Command getStartOfStream() {
		return startOfStream;
	}

}
