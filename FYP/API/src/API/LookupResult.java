package API;

public class LookupResult {
	private int result;
	private double distance;
	
	public LookupResult(int result, double distance) {
		this.result = result;
		this.distance = distance;
	}
	
	public int getResult() {
		return result;
	}
	
	public double getDistance() {
		return distance;
	}
}
