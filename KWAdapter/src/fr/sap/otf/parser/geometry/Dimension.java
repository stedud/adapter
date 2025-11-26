package fr.sap.otf.parser.geometry;

public class Dimension {

	private long w, h;

	public Dimension(long w, long h) {
		super();
		this.w = w;
		this.h = h;
	}

	public Dimension() {
		w = 0;
		h = 0;
	}

	public long getW() {
		return w;
	}

	public void setW(long w) {
		this.w = w;
	}

	public long getH() {
		return h;
	}

	public void setH(long h) {
		this.h = h;
	}

}
