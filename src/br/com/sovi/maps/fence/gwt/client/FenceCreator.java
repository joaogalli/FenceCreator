package br.com.sovi.maps.fence.gwt.client;

import com.gargoylesoftware.htmlunit.AlertHandler;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.Maps;
import com.google.gwt.maps.client.control.LargeMapControl;
import com.google.gwt.maps.client.event.MapClickHandler;
import com.google.gwt.maps.client.event.PolygonClickHandler;
import com.google.gwt.maps.client.event.PolygonLineUpdatedHandler;
import com.google.gwt.maps.client.event.PolygonMouseOverHandler;
import com.google.gwt.maps.client.event.PolygonClickHandler.PolygonClickEvent;
import com.google.gwt.maps.client.geocode.Geocoder;
import com.google.gwt.maps.client.geocode.LatLngCallback;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Polygon;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class FenceCreator implements EntryPoint, LatLngCallback {

	private TextArea pointsTextArea;

	private TextBox addressBox;

	private MapWidget map;

	private Polygon fencePolygon;

	// localhost:
	// ABQIAAAApHGToVGcgVbR09E5gflTsBRi_j0U6kJrkFvY4-OX2XYmEAa76BQqKGysILOktKpPv-KcEDbfSjfkdQ

	// fencecreator.appspot.com
	// ABQIAAAApHGToVGcgVbR09E5gflTsBRPF3Xbx1jAZsv-M8NS5BZCkf0-uRTvakw27pnaas2TZB4tdu_aQRrpHQ

	public void onModuleLoad() {
		Maps.loadMapsApi("ABQIAAAApHGToVGcgVbR09E5gflTsBRPF3Xbx1jAZsv-M8NS5BZCkf0-uRTvakw27pnaas2TZB4tdu_aQRrpHQ", "2",
				false, new Runnable() {
					public void run() {
						buildUi();
					}
				});
	}

	protected void buildUi() {
		HTML title = new HTML("<h1>FenceCreator <a href='v3.0.html'>v3.0</a></h1><h5>by Sovi</h5>");

		final SplitLayoutPanel dock = new SplitLayoutPanel(1);
		dock.addNorth(title, 80);
		dock.addWest(createForm(), 300);
		dock.add(createMapWidget());

		// Add the map to the HTML host page
		RootLayoutPanel.get().add(dock);
	}

	private MapWidget createMapWidget() {
		LatLng saoPauloCity = LatLng.newInstance(-23.560, -46.629);

		map = new MapWidget(saoPauloCity, 12);
		map.setSize("100%", "100%");
		// Add some controls for the zoom level
		map.addControl(new LargeMapControl());
		map.setScrollWheelZoomEnabled(true);
		map.addMapClickHandler(new MapClickHandler() {
			@Override
			public void onClick(MapClickEvent event) {
				if (fencePolygon == null) {
					createPolygon(event.getLatLng());
				}
			}
		});

		return map;
	}

	private Widget createForm() {
		VerticalPanel panel = new VerticalPanel();
		panel.setSpacing(2);

		pointsTextArea = new TextArea();
		pointsTextArea.setWidth("280px");
		pointsTextArea.setHeight("200px");

		panel.add(new HTML("Points"));
		panel.add(pointsTextArea);

		{
			HorizontalPanel buttons = new HorizontalPanel();
			Button clearButton = new Button("Clear Fence", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					clearFence();
				}
			});
			Button textToMapButton = new Button("Text 2 Map", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					updateFenceFromTextBox();
				}
			});
			Button selectAllButton = new Button("Select All", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					pointsTextArea.selectAll();
				}
			});
			Button drawButton = new Button("Draw", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					fencePolygon.setDrawingEnabled();
				}
			});

			buttons.add(clearButton);
			buttons.add(textToMapButton);
			buttons.add(selectAllButton);
			buttons.add(drawButton);
			
			panel.add(buttons);
		}

		{ // Geocode
			panel.add(new HTML("<b>Geocoding</b><br/>Type address and press Add to insert a new point"));
			HorizontalPanel geocodePanel = new HorizontalPanel();
			geocodePanel.setSpacing(2);
			addressBox = new TextBox();
			addressBox.setWidth("200px");
			Button addAddressPoint = new Button("Add", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					new Geocoder().getLatLng(addressBox.getText(), FenceCreator.this);
				}
			});
			geocodePanel.add(addressBox);
			geocodePanel.add(addAddressPoint);
			panel.add(geocodePanel);
		}

		return panel;
	}

	protected void clearAll() {
		pointsTextArea.setText("");
		clearFence();
	}

	protected void createPolygon(LatLng latLng) {
		LatLng[] ll = new LatLng[] { latLng };

		fencePolygon = new Polygon(ll);
		fencePolygon.addPolygonMouseOverHandler(new PolygonMouseOverHandler() {
			@Override
			public void onMouseOver(PolygonMouseOverEvent event) {
				fencePolygon.setEditingEnabled(true);
			}
		});

		fencePolygon.addPolygonLineUpdatedHandler(new PolygonLineUpdatedHandler() {
			@Override
			public void onUpdate(PolygonLineUpdatedEvent event) {
				updateTextBox();
			}
		});

		map.addOverlay(fencePolygon);

		fencePolygon.setDrawingEnabled();
	}

	private void updateTextBox() {
		pointsTextArea.setText("");

		if (fencePolygon != null) {
			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < fencePolygon.getVertexCount(); i++) {
				LatLng vertex = fencePolygon.getVertex(i);

				if (i > 0)
					sb.append("|");
				sb.append(vertex.toUrlValue());

			}
			pointsTextArea.setText(sb.toString());
		}
	}

	private void updateFenceFromTextBox() {
		clearFence();

		String text = pointsTextArea.getText();

		String[] points = text.split("[|]");

		String[] initialPoint = points[0].split(",");
		LatLng initialLatLng = LatLng.newInstance(Double.parseDouble(initialPoint[0]),
				Double.parseDouble(initialPoint[1]));
		createPolygon(initialLatLng);

		for (int i = 1; i < points.length; i++) {
			String[] point = points[i].split(",");
			LatLng newPoint = LatLng.newInstance(Double.parseDouble(point[0]), Double.parseDouble(point[1]));
			fencePolygon.insertVertex(i, newPoint);
		}
	}

	private void clearFence() {
		try {
			map.removeOverlay(fencePolygon);
		}
		catch (Exception e) {
		}
		map.clearOverlays();
		fencePolygon = null;
	}

	@Override
	public void onSuccess(LatLng point) {
		if (fencePolygon != null) {
			fencePolygon.insertVertex(fencePolygon.getVertexCount(), point);
			fencePolygon.setDrawingEnabled();
		}
		else {
			createPolygon(point);
		}
	}

	@Override
	public void onFailure() {
	}
}
