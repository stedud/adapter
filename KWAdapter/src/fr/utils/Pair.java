package fr.utils;

public class Pair {

	protected Object firstValue = null;
	protected Object secondValue = null;

	public Pair() {
		// TODO Auto-generated constructor stub
	}

	public Pair(Object firstValue, Object secondValue) {
		super();
		this.firstValue = firstValue;
		this.secondValue = secondValue;
	}

	public Object getFirstValue() {
		return firstValue;
	}

	public void setFirstValue(Object firstValue) {
		this.firstValue = firstValue;
	}

	public Object getSecondValue() {
		return secondValue;
	}

	public void setSecondValue(Object secondValue) {
		this.secondValue = secondValue;
	}

	@Override
	public String toString() {
		return "Pair [firstValue=" + firstValue + ", secondValue=" + secondValue + "]";
	}

}
