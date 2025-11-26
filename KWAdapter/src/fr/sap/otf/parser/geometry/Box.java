package fr.sap.otf.parser.geometry;

public class Box {

	private Position position = new Position();
	private Dimension dimension = new Dimension();

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public Dimension getDimension() {
		return dimension;
	}

	public void setDimension(Dimension dimension) {
		this.dimension = dimension;
	}

}
