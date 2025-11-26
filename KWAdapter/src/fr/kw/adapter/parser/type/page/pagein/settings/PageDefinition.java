package fr.kw.adapter.parser.type.page.pagein.settings;

import java.util.ArrayList;
import java.util.List;

import fr.kw.adapter.parser.type.page.settings.PageDocumentDefinition;


public class PageDefinition extends PageDocumentDefinition{
	
	protected String event;
	protected List<FrameDefinition> frames = new ArrayList<FrameDefinition>();
 	
	public PageDefinition()
	{
		this.frames = new ArrayList<>();
	}
	
	public void addFrame(FrameDefinition frame)
	{
		this.frames.add(frame);
	}
	
	public FrameDefinition[] getFrames()
	{
		
		return frames.toArray(new FrameDefinition[] {});
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}
	

}
