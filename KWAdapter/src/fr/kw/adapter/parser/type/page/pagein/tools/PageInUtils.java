package fr.kw.adapter.parser.type.page.pagein.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

import fr.kw.adapter.parser.type.page.pagein.data.TextBlock;
import fr.utils.Utils;

public class PageInUtils {

	public PageInUtils() {
		// TODO Auto-generated constructor stub
	}
	
	
	public static TextBlock[] getPages(File file) throws FileNotFoundException
	{
		
		List<TextBlock> pages = new ArrayList<TextBlock>();
		Scanner pageIterator = Utils.getPageScanner(file, StandardCharsets.UTF_8.name());
		while (pageIterator.hasNext())
		{
			String page = pageIterator.next();
			String[] pageLines = page.split(Utils.LFRegHex);
			TextBlock pageTextBlock = new TextBlock();
			for (String line : pageLines)pageTextBlock.addLine(StringUtils.removeStart(line, "\uFEFF"));
			pages.add(pageTextBlock);
		}
		
		return pages.toArray(new TextBlock[] {});
	}

}
