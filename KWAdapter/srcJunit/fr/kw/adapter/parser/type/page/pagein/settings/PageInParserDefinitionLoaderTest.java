package fr.kw.adapter.parser.type.page.pagein.settings;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import fr.kw.adapter.parser.process.ParseProcessConfiguration;
import fr.kw.adapter.parser.type.page.settings.DocumentParserDefinition;

public class PageInParserDefinitionLoaderTest {

	@Test
	public final void testLoad() {
		
		PageInParserDefinitionLoader loader = new PageInParserDefinitionLoader();
		
		ParseProcessConfiguration rootConfig;
		try {
			rootConfig =  ParseProcessConfiguration.get(new File("main.properties"));
			DocumentParserDefinition pageInDefinition = loader.load(new File("dsc/BULLETIN_PageIn.pagein"), rootConfig);
			
			if (pageInDefinition.getPageDefinitions().size() != 2) throw new Exception("Expecting 2 page definitions, found " + pageInDefinition.getPageDefinitions().size());
		} catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
		

	}

}
