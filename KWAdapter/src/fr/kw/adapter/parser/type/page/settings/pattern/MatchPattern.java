package fr.kw.adapter.parser.type.page.settings.pattern;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDPage;

import fr.kw.adapter.parser.type.page.AFileProcess;
import fr.kw.adapter.parser.type.page.FileProcessException;
import fr.kw.adapter.parser.type.page.document.Page;
import fr.kw.adapter.parser.type.page.pagein.data.TextBlock;
import fr.kw.adapter.parser.type.page.pagein.settings.FrameBlockDefinition;
import fr.kw.adapter.parser.type.page.pagein.settings.FrameDefinition;
import fr.kw.adapter.parser.type.page.pdfin.PDFProcess;
import fr.kw.adapter.parser.type.page.settings.PointMm;
import fr.kw.adapter.parser.type.page.settings.Unit;
import fr.sap.otf.parser.geometry.Box;
import fr.utils.LogHelper;

public class MatchPattern extends APatternDefinition {

	protected String name;
	protected PointMm position = new PointMm();
	protected PointMm dimension = new PointMm();
	protected String pattern;
	protected String script;
	protected boolean ignoreLine = false;
	protected boolean ignoreColumn = false;

	protected boolean wildcards = true;

	public MatchPattern(String name) {
		this.name = name;
	}

	
	
	public boolean match(TextBlock textBlock, boolean ignoreLigne, boolean ignoreColumn, int yoffset) throws FileProcessException, IOException {

		int nbLines = textBlock.getHeight();
		int nbColumns = textBlock.getWidth();
		
		int loopLine = ignoreLigne ? nbLines : 1;
		int loopColumns = ignoreColumn ? nbColumns : 1;
		boolean matched = false;
		for (int j=1; j<= loopLine;j++)
		{
			for (int i=1; i<= loopColumns;i++)
			{
				String text = textBlock.getTextAt(ignoreColumn? i: position.getX(), (ignoreLigne ? j: position.getY()) + yoffset, dimension.getX(), dimension.getY()).toString();
				
				if (wildcards) {
					matched = Pattern.matches(pattern+ ".*", text);
					//LogHelper.debug("Pattern " + this.name + ", wildcard('" + pattern + "','" + text + "')=" + matched);
					if (matched) {
						LogHelper.debug("Pattern " + this.name + ", wildcard('" + pattern + "','" + text + "')=" + matched);
						return matched;
					}
				} else {
					matched = StringUtils.equals(text, pattern);
					if (matched)
					{
						LogHelper.debug("Pattern " + this.name + ", match('" + pattern + "','" + text + "')=" + matched);
						return matched;
					}
				}		
			}
			
		}
		return matched;
		
	}
	
	public boolean match(TextBlock textBlock,boolean ignoreLigne, boolean ignoreColumn) throws FileProcessException, IOException {

		return match(textBlock, ignoreLigne,  ignoreColumn, 0);
	}


	public boolean match(Page docPage) throws FileProcessException, IOException {

		String text = "";
		if (docPage.getNativePage() instanceof PDPage) {
			text = PDFProcess.extractText((PDPage) docPage.getNativePage(), position.getX(), position.getY(),
					dimension.getX(), dimension.getY(), Unit.MM);

		} else if (docPage.getNativePage() instanceof fr.sap.otf.parser.object.Page) {
			fr.sap.otf.parser.object.Page gofPage = (fr.sap.otf.parser.object.Page) docPage.getNativePage();

			long xTwip = fr.sap.otf.parser.object.Page
					.pointToTwip(Math.round(AFileProcess.getPoints(position.getX(), Unit.MM))
							- docPage.getPageDocumentDefinition().getxOffset());
			long yTwip = fr.sap.otf.parser.object.Page
					.pointToTwip(Math.round(AFileProcess.getPoints(position.getY(), Unit.MM))
							- docPage.getPageDocumentDefinition().getyOffset());
			long wTwip = fr.sap.otf.parser.object.Page
					.pointToTwip(Math.round(AFileProcess.getPoints(dimension.getX(), Unit.MM)));
			long hTwip = fr.sap.otf.parser.object.Page
					.pointToTwip(Math.round(AFileProcess.getPoints(dimension.getY(), Unit.MM)));

			Box box = new Box();
			box.getPosition().setX(xTwip);
			box.getPosition().setY(yTwip);
			box.getDimension().setW(wTwip);
			box.getDimension().setH(hTwip);
			text = gofPage.getText(box, false);
			// System.out.println("Text=" + text);
		}
		if (wildcards) {
			boolean matched = Pattern.matches(pattern + ".*", text);
			LogHelper.debug("Pattern " + this.name + ", wildcard('" + pattern + "','" + text + "')=" + matched);
			return matched;
		} else {
			boolean matched = StringUtils.equals(text, pattern);
			LogHelper.debug("Pattern " + this.name + ", match('" + pattern + "','" + text + "')=" + matched);
			return StringUtils.equals(text, pattern);
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PointMm getPosition() {
		return position;
	}

	public void setPosition(PointMm position) {
		this.position = position;
	}

	public PointMm getDimension() {
		return dimension;
	}

	public void setDimension(PointMm dimension) {
		this.dimension = dimension;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public boolean isWildcards() {
		return wildcards;
	}

	public void setWildcards(boolean wildcards) {
		this.wildcards = wildcards;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	@Override
	public String toString() {
		return "MatchPattern [name=" + name + ", pattern=" + pattern + ", wildcards=" + wildcards + "]";
	}

	public boolean isIgnoreLine() {
		return ignoreLine;
	}

	public void setIgnoreLine(boolean ignoreLine) {
		this.ignoreLine = ignoreLine;
	}

	public boolean isIgnoreColumn() {
		return ignoreColumn;
	}

	public void setIgnoreColumn(boolean ignoreColumn) {
		this.ignoreColumn = ignoreColumn;
	}

}
