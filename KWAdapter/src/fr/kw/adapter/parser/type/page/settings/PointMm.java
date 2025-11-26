package fr.kw.adapter.parser.type.page.settings;

public class PointMm {

	public static final double UN_MM_EN_POINTS = 2.83465;
	public static final double UN_POINT_EN_MM = 1 / UN_MM_EN_POINTS;

	public static final PointMm ZERO = new PointMm();
	static {
		ZERO.x = 0;
		ZERO.y = 0;
	}

	protected float x = 0.0f;
	protected float y = 0.0f;

	public PointMm() {
		// TODO Auto-generated constructor stub
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public static float getPoints(float mm, Unit unit) {

		switch (unit) {
		case MM:
			return (float) (mm * PointMm.UN_MM_EN_POINTS);
		case POINT:
			return mm;
		default:
			return mm;
		}

	}

	public static float getPoints(float mm) {

		return (float) (mm * UN_MM_EN_POINTS);
	}

	public static float getMm(float pt) {

		return (float) (pt * UN_POINT_EN_MM);
	}
}
