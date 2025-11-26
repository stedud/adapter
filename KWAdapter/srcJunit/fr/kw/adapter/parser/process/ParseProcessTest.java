package fr.kw.adapter.parser.process;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import fr.kw.adapter.document.Document;
import fr.kw.adapter.parser.type.page.pagein.settings.PageInParserDefinitionLoader;
import fr.kw.adapter.parser.type.page.settings.DocumentParserDefinition;

public class ParseProcessTest {

	@Test
	public final void testParseProcess() {
PageInParserDefinitionLoader loader = new PageInParserDefinitionLoader();
		
		ParseProcessConfiguration processConfig;
		try {
			processConfig = new ParseProcessConfiguration(new File("scanners/pageIn.properties"));
			ParseProcess parseProcess = new ParseProcess(processConfig);
			List<Document> result = parseProcess.parse(new File("EDITBULL.txt"));
			
			if (result == null || result.size() == 0) fail("No document found for PageIn");
			else
			{
				File fileResult = new File("testParseProcess.xml");
				FileOutputStream fos = new FileOutputStream(fileResult);
				for (Document doc : result)
				{
					fos.write((doc.getName() + " - " + doc.getNum() + "\n").getBytes());
					InputStream is = doc.getMainDatasource().getDataInputStreamToClose();
					byte[] buffer = new byte[512];
					int nbBytes = 0;
					while ((nbBytes = is.read(buffer)) >= 0)
					{
						fos.write(buffer, 0, nbBytes);
					}
					fos.flush();
				}
				fos.close();
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
			
		}
	}

}
