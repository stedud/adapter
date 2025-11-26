package fr.kw.adapter.parser.type.page.pagein.tools;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Test;

import fr.kw.adapter.parser.type.page.pagein.data.TextBlock;

public class PageInUtilsTest extends PageInUtils {

	@Test
	public final void testGetPages() {
		
		File pageInFile = new File("pageInStrs/test/EDITBULL.txt");
		TextBlock[] pages;
		try {
			pages = PageInUtils.getPages(pageInFile);
			if (pages.length != 15)	fail("Incorrect number of pages found. Expected 15, found " + pages.length);
		} catch (FileNotFoundException e) {
			fail("File " + pageInFile.getPath() + " not found");
		}
	
	}

}
