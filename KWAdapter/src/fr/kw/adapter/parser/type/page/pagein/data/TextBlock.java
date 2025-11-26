package fr.kw.adapter.parser.type.page.pagein.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class TextBlock {

	protected List<String> lines = null;

	protected int width = 0;
	
	public TextBlock() {
		lines = new ArrayList<String>();
	}

	public void addLine(String line){
		if (line.length() > width) width = line.length();
		this.lines.add(line);
	}

	
	
	/**
	 * Returns the text block corresponding to the positions.
	 * Fills with blanks if needed.
	 * x starts from 0
	 * y starts from 0
	 * w starts from 1
	 * h starts from 1
	 * 
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @return
	 */
	public TextBlock getTextAt(int x, int y, int w, int h)	{
		
		TextBlock textBlock = new TextBlock();
		
		for (int i = y; i < y+h;i++)
		{
			String line = getLineAt(i);
			if (w < 0)
			{
				line = StringUtils.substring(line, x-1);
			}
			else
			{
				line = StringUtils.substring(line, x-1, x+w-1);
				line = StringUtils.rightPad(line, w);
			}
			textBlock.addLine(line);
		}
		
		
		return textBlock;
	}
	
	public TextBlock getTextAt(Float x, Float y, Float w, Float h)
	{
		return getTextAt(x.intValue(), y.intValue(), w.intValue(), h.intValue());
	}
	
	
	
	protected String getLineAt(int y)
	{
		
		try {
			return lines.get(y-1);
		} catch (Exception e) {
			return "";
		}
	}

	@Override
	public String toString() {
		StringBuffer value = new StringBuffer();
		for (String line : lines)
		{
			if (value.length() > 0)
			{
				value.append(System.lineSeparator());
			}
			value.append(line);
			
		}
		return value.toString();
	}

	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return lines.size();
	}


}
