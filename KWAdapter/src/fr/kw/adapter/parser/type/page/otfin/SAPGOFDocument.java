package fr.kw.adapter.parser.type.page.otfin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import fr.kw.adapter.parser.type.page.FileProcessException;
import fr.kw.adapter.parser.type.page.document.ADocument;
import fr.kw.adapter.parser.type.page.document.Page;
import fr.kw.adapter.parser.type.page.settings.DocumentParserDefinition;
import fr.sap.otf.parser.OTFParser;
import fr.sap.otf.parser.object.Argument;
import fr.sap.otf.parser.object.Command;
import fr.sap.otf.parser.object.HeaderEntry;

public class SAPGOFDocument extends ADocument {

	protected OTFParser otfParser;

	public SAPGOFDocument(DocumentParserDefinition definition) {
		this.definition = definition;

	}

	@Override
	public void createFile(File target) throws IOException, FileProcessException {
		// TODO Auto-generated method stub
		FileWriter fw = new FileWriter(target);
		char eol = (char) Integer.parseInt("0A", 16);
		for (HeaderEntry header : otfParser.getHeader().getHeaders()) {
			fw.write(header.getLine());
			fw.write(eol);
		}
		fw.write(otfParser.getStartOfStream().getLine());
		fw.flush();
		for (Page page : pages) {
			fr.sap.otf.parser.object.Page gofPage = (fr.sap.otf.parser.object.Page) page.getNativePage();
			for (Command command : gofPage.getContents()) {
				if (StringUtils.compare(command.getID(), "OP") == 0) {// Set right number of pages
					Argument arg = command.getParameters().get(6);
					arg.setValue(String.format("%05d", this.pages.size()));
					arg = command.getParameters().get(7);
					arg.setValue(String.format("%05d", this.pages.size()));
					command.rebuildLine();

				}

				fw.write(command.getLine());
				fw.write(eol);
			}
			fw.flush();
		}
		fw.write("//");
		fw.flush();
		fw.close();

		this.fileResult = target;
	}

	@Override
	public void init(Object rootObject) {

		if (rootObject instanceof OTFParser)
			otfParser = (OTFParser) rootObject;

	}

	@Override
	public void close() {
		//Nothing to do for OTF
		
	}

}
