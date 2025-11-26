package fr.kw.adapter.parser.type.page.settings.field;

import java.awt.Point;
import java.awt.geom.Rectangle2D;

import fr.kw.adapter.parser.type.page.settings.PointMm;

public class FieldDefinition {

	protected String name;
	protected PointMm position = new PointMm();;
	protected PointMm dimension = new PointMm();
	protected PointMm translate = new PointMm();;
	protected Page page = new Page(PageType.ANY);// par défaut
	protected String script;

	protected boolean keepSpace = false;
	protected boolean removeText = false;
	protected boolean removePicture = false;
	protected boolean removeBarcode = false;

	public FieldDefinition() {
	}

	public FieldDefinition(String name) {
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

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isKeepSpace() {
		return keepSpace;
	}

	public void setKeepSpace(boolean keepSpace) {
		this.keepSpace = keepSpace;
	}

	public boolean isRemoveText() {
		return removeText;
	}

	public void setRemoveText(boolean removeText) {
		this.removeText = removeText;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public Rectangle2D getRectPt() {
		Rectangle2D.Float rect = new Rectangle2D.Float();
		rect.setRect(PointMm.getPoints(position.getX()), PointMm.getPoints(position.getY()),
				PointMm.getPoints(dimension.getX()), PointMm.getPoints(dimension.getY()));
		return rect;

	}

	public PointMm getTranslate() {
		return translate;
	}

	public void setTranslate(PointMm translate) {
		this.translate = translate;
	}

}
