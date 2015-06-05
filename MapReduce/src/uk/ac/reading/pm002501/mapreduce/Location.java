package uk.ac.reading.pm002501.mapreduce;

/**
 * Store a location - latitude and longitude.
 */
public class Location {
	public String latitude;
	public String longitude;
	
	/**
	 * Construct a location fom a given latitude and longitude.
	 * @param latitude The latitude (should be double compatible but in string form).
	 * @param longitude The longitude (should be double compatible but in string form).
	 */
	public Location(String latitude, String longitude) {
		if (latitude == null)
			latitude = "";
		if (longitude == null)
			longitude = "";
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	@Override
	public String toString() {
		return "{" + latitude + ", " + longitude + "}";
	}
}
