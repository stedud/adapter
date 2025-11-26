package fr.kw.adapter.parser.type.page.pagein.data;

import static org.junit.Assert.*;

import org.junit.Test;

public class TextBlockTest {

	@Test
	public final void testGetTextAtFloatFloatFloatFloat() {
		
		TextBlock textBlock = new TextBlock();
		textBlock.addLine("abcdefghijklmnopqrstuvwxyz");
		textBlock.addLine("bcdefghijklmnopqrstuvwxyza");
		textBlock.addLine("cdefghijklmnopqrstuvwxyzab");
		textBlock.addLine("defghijklmnopqrstuvwxyzabc");
		textBlock.addLine("efghijklmnopqrstuvwxyzabcd");
		
		TextBlock text = textBlock.getTextAt(4, 3, 2, 2);
		
		if (text.getLineAt(1).compareTo("fg") != 0) fail("Found text '" + text.getLineAt(1)  + "' instead of 'fg'");
		if (text.getLineAt(2).compareTo("gh") != 0) fail("Found text '" + text.getLineAt(1)  + "' instead of 'gh'");
		
		
	}

}
