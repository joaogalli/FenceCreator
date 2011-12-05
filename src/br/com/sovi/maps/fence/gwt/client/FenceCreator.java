package br.com.sovi.maps.fence.gwt.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.Maps;
import com.google.gwt.maps.client.control.LargeMapControl;
import com.google.gwt.maps.client.event.MapClickHandler;
import com.google.gwt.maps.client.event.PolygonClickHandler;
import com.google.gwt.maps.client.event.PolygonEndLineHandler;
import com.google.gwt.maps.client.geocode.Geocoder;
import com.google.gwt.maps.client.geocode.LatLngCallback;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.PolyStyleOptions;
import com.google.gwt.maps.client.overlay.Polygon;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
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

	private List<Polygon> polygons = new ArrayList<Polygon>();

	private Polygon edPolygon;

	PolyStyleOptions editing = PolyStyleOptions.newInstance("blue");

	PolyStyleOptions normal = PolyStyleOptions.newInstance("red");

	private boolean isDrawing = false, startNewPolygon = false;
	
	private Label fenceName = new Label();

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
		HTML title = new HTML("<h1>FenceCreator <a href='versions.html'>v5</a></h1><h5>by Sovi</h5>");

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
				if (startNewPolygon) {
					startDrawingPolygon(event.getLatLng());
					startNewPolygon = false;
					isDrawing = true;
				}
			}
		});

		return map;
	}

	/**
	 * @param latLng
	 */
	protected void startDrawingPolygon(LatLng latLng) {
		LatLng[] ll = new LatLng[] { latLng };

		Polygon polygon = new Polygon(ll);
		polygon.addPolygonClickHandler(polygonClickHandler);

		polygon.addPolygonEndLineHandler(new PolygonEndLineHandler() {
			@Override
			public void onEnd(PolygonEndLineEvent event) {
				updateTextBox(event.getSender());
				isDrawing = false;
			}
		});

		map.addOverlay(polygon);

		polygon.setDrawingEnabled();
		polygons.add(polygon);
	}

	/**
	 * Create the form to manipulate the fences and the map.
	 * @return
	 */
	private Widget createForm() {
		VerticalPanel panel = new VerticalPanel();
		panel.setSpacing(2);

		pointsTextArea = new TextArea();
		pointsTextArea.setWidth("280px");
		pointsTextArea.setHeight("200px");

		// Creating new polygon
		{
			Button drawButton = new Button("Create a new fence", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					removeEditionOfCurrentEditingPolygon();
					pointsTextArea.setText("");
					startNewPolygon = true;
				}
			});
			panel.add(drawButton);
		}

		HorizontalPanel names = new HorizontalPanel();
		names.add(new HTML("Editing Fence: "));
		names.add(fenceName);
		
		panel.add(names);
		panel.add(pointsTextArea);

		{
			HorizontalPanel buttons = new HorizontalPanel();
			Button clearButton = new Button("Clear Fence", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					clearPolygons();
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

			buttons.add(clearButton);
			buttons.add(textToMapButton);
			buttons.add(selectAllButton);

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

		{ // Multiple load
			panel.add(new HTML("<b>Multiple Fence Loading</b><br/>Insert fences separated by line break <Enter>."));
			VerticalPanel multiLoadPanel = new VerticalPanel();

			HorizontalPanel textAreas = new HorizontalPanel();
			
			final TextArea namesTextArea = new TextArea();
			namesTextArea.setWidth("100px");
			namesTextArea.setHeight("120px");
			textAreas.add(namesTextArea);
			
			final TextArea multiLoadTextArea = new TextArea();
			multiLoadTextArea.setWidth("180px");
			multiLoadTextArea.setHeight("120px");
			textAreas.add(multiLoadTextArea);
			
			multiLoadPanel.add(textAreas);

			Button drawLoad = new Button("Load Fences", new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					String names = namesTextArea.getText();
					String fences = multiLoadTextArea.getText();
					loadMultipleFences(names, fences, "[\n]");
				}
			});

			multiLoadPanel.add(drawLoad);
			panel.add(multiLoadPanel);
		}

		return panel;
	}

	/**
	 * @param fences
	 * @param string
	 */
	protected void loadMultipleFences(String nameString, String fences, String separator) {
		this.clearPolygons();

		String[] names = nameString.split(separator);
		String[] fencess = fences.split(separator);

		for (int i = 0; i < fencess.length; i++) {
			String fence = fencess[i];
			String name = null;
			if (i < names.length) {
				name = names[i];
			}
			
			Polygon polygon = createPolygonFrom(name, fence);
			polygons.add(polygon);
		}
		
		for (Polygon p : polygons) {
			map.addOverlay(p);
		}
	}

	private PolygonClickHandler polygonClickHandler = new PolygonClickHandler() {
		@Override
		public void onClick(PolygonClickEvent event) {
			if (!isDrawing) {
				removeEditionOfCurrentEditingPolygon();

				
				edPolygon = event.getSender();
				edPolygon.setFillStyle(editing);
				edPolygon.setStrokeStyle(editing);
				edPolygon.setEditingEnabled(true);

				if (edPolygon instanceof NamedPolygon) {
					fenceName.setText((((NamedPolygon)edPolygon).getName()));
				}

				updateTextBox(event.getSender());
			}
		}

	};

	protected void removeEditionOfCurrentEditingPolygon() {
		// Removes edition from last polygon.
		if (edPolygon != null) {
			edPolygon.setEditingEnabled(false);
			edPolygon.setFillStyle(normal);
			edPolygon.setStrokeStyle(normal);
		}
	}

	/**
	 * Clear all polygons from the map and list.
	 */
	protected void clearPolygons() {
		for (Polygon p : polygons) {
			map.removeOverlay(p);
		}

		polygons.clear();
	}

	/**
	 * 
	 */
	protected void clearAll() {
		pointsTextArea.setText("");
		clearPolygons();
	}

	/**
	 * 
	 */
	private void updateTextBox(Polygon editingPolygon) {
		pointsTextArea.setText("");

		if (editingPolygon != null) {
			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < editingPolygon.getVertexCount(); i++) {
				LatLng vertex = editingPolygon.getVertex(i);

				if (i > 0)
					sb.append("|");
				sb.append(vertex.toUrlValue());

			}
			pointsTextArea.setText(sb.toString());
		}
	}

	private void updateFenceFromTextBox() {
		String text = pointsTextArea.getText();
		Polygon polygon = createPolygonFrom(null, text);
		polygons.add(polygon);
		map.addOverlay(polygon);
	}

	/**
	 * @param text Coordinates separated by , for Lat and Lng and separated by |
	 * for different points.
	 * @return A new polygon.
	 */
	private Polygon createPolygonFrom(String name, String text) {

		String[] points = text.split("[|]");

		LatLng[] ll = new LatLng[points.length];

		for (int i = 0; i < points.length; i++) {
			String[] point = points[i].split(",");
			LatLng newPoint = LatLng.newInstance(Double.parseDouble(point[0]), Double.parseDouble(point[1]));
			ll[i] = newPoint;
		}

		Polygon polygon = new NamedPolygon(name, ll);
		polygon.addPolygonClickHandler(polygonClickHandler);
		polygon.setFillStyle(normal);
		polygon.setStrokeStyle(normal);
		return polygon;
	}

	@Override
	public void onSuccess(LatLng point) {
		// if (editingPolygon != null) {
		// editingPolygon.insertVertex(editingPolygon.getVertexCount(), point);
		// editingPolygon.setDrawingEnabled();
		// }
		// else {
		// startPolygon(point);
		// }
	}

	@Override
	public void onFailure() {
	}
}
