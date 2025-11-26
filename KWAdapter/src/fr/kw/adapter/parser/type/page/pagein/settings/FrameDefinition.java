package fr.kw.adapter.parser.type.page.pagein.settings;

import java.util.ArrayList;
import java.util.List;

import fr.kw.adapter.parser.type.page.settings.PointMm;

public class FrameDefinition {

	protected String id;
	protected PointMm position ;
	protected PointMm dimension;
	protected List<FrameBlockDefinition> blocks = new ArrayList<FrameBlockDefinition>();
	
	public FrameDefinition(String id) {
		this.id = id;
		this.position = new PointMm();
		this.dimension = new PointMm();
	}


	public String getId() {
		return id;
	}


	public PointMm getPosition() {
		return position;
	}


	public PointMm getDimension() {
		return dimension;
	}


	public List<FrameBlockDefinition> getBlocks() {
		return blocks;
	}

}
