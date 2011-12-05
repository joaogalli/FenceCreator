package br.com.sovi.maps.fence.gwt.client;

import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Polygon;

public class NamedPolygon extends Polygon {

	private String name;
	
	public NamedPolygon(String name, LatLng[] points) {
		super(points);
		this.setName(name);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
