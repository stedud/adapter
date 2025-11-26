package fr.sap.otf.parser.geometry;

public class Position {

	private long x, y;

	public Position(long x, long y) {
		super();
		this.x = x;
		this.y = y;
	}

	public Position() {
		x = 0;
		y = 0;
	}

	public long getX() {
		return x;
	}

	public void setX(long x) {
		this.x = x;
	}

	public long getY() {
		return y;
	}

	public void setY(long y) {
		this.y = y;
	}

}
