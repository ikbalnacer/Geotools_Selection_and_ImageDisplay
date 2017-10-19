package org.geotloos.bidgou;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JToolBar;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.swing.tool.CursorTool;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class Selectionlab {
	private StyleFactory style_F = CommonFactoryFinder.getStyleFactory();
    private FilterFactory2 filter_F = CommonFactoryFinder.getFilterFactory2();
    
    private enum geomtype{point,line,poly}
    
    private static final Color LINE_COLOUR = Color.BLUE;
    private static final Color FILL_COLOUR = Color.CYAN;
    private static final Color SELECTED_COLOUR = Color.YELLOW;
    private static final float OPACITY = 1.0f;
    private static final float LINE_WIDTH = 1.0f;
    private static final float POINT_SIZE = 10.0f;
    private JMapFrame mapFrame;
    private SimpleFeatureSource featureSource;

    private String geometryAttributeName;
    private geomtype geometryType;
public static void main(String [] agrs ) throws IOException{
	
	Selectionlab selec = new Selectionlab();
	
	File file = JFileDataStoreChooser.showOpenFile("shp", null);
	if(file ==null){
		return ;
	}
	selec.display(file);
}

public void display(File file) throws IOException{
	FileDataStore datastore = FileDataStoreFinder.getDataStore(file);
	featureSource = datastore.getFeatureSource();
	setGeometry();
	
	MapContent map = new MapContent();
	map.setTitle("selction ");
	Style style = createDefaultStyle();
	Layer layer = new FeatureLayer(featureSource, style);
	map.addLayer(layer);
	
	mapFrame = new JMapFrame(map);
	
	mapFrame.enableToolBar(true);
	mapFrame.enableStatusBar(true);
	
	JToolBar tool = mapFrame.getToolBar();
	JButton bouton = new JButton("select ");
	tool.addSeparator();
	tool.add(bouton);
	 bouton.addActionListener(new ActionListener() {		
		public void actionPerformed(ActionEvent arg0) {
			mapFrame.getMapPane().setCursorTool(new CursorTool() {
				 @Override
                 public void onMouseClicked(MapMouseEvent ev) {
                     selectFeatures(ev);
                 }
			});
		}
	});
	 mapFrame.setSize(600, 600);
     mapFrame.setVisible(true);
     
}
public void selectFeatures(MapMouseEvent ev) {

    System.out.println("Mouse click at: " + ev.getMapPosition());

    /*
     * Construct a 5x5 pixel rectangle centred on the mouse click position
     */
    java.awt.Point screenPos = ev.getPoint();
    Rectangle screenRect = new Rectangle(screenPos.x-2, screenPos.y-2, 5, 5);
    /*
     * Transform the screen rectangle into bounding box in the coordinate
     * reference system of our map context. Note: we are using a naive method
     * here but GeoTools also offers other, more accurate methods.
     */
    AffineTransform screenToWorld = mapFrame.getMapPane().getScreenToWorldTransform();
    Rectangle2D worldRect = screenToWorld.createTransformedShape(screenRect).getBounds2D();
    ReferencedEnvelope bbox = new ReferencedEnvelope(
            worldRect,
            mapFrame.getMapContent().getCoordinateReferenceSystem());


    /*
     * Create a Filter to select features that intersect with
     * the bounding box
     */
    
    Filter filter = filter_F.intersects(filter_F.property(geometryAttributeName), filter_F.literal(bbox));
   
 
    /*
     * Use the filter to identify the selected features
     */
    try {
        System.out.println("hehe 4");

        SimpleFeatureCollection selectedFeatures =
                featureSource.getFeatures(filter);
        
        SimpleFeatureIterator iter = selectedFeatures.features();
        System.out.println("hehe 5");
        Set<FeatureId> IDs = new HashSet<FeatureId>();
        System.out.println("hehe 6");

        try {
            while (iter.hasNext()) {
                SimpleFeature feature = iter.next();
                IDs.add(feature.getIdentifier());

                System.out.println("   " + feature.getIdentifier());
            }

        } finally {
            iter.close();
        }

        if (IDs.isEmpty()) {
            System.out.println("   no feature selected");
        }

        displaySelectedFeatures(IDs);

    } catch (Exception ex) {
        ex.printStackTrace();
        return;
    }
}
public void displaySelectedFeatures(Set<FeatureId> IDs) {
    Style style;

    if (IDs.isEmpty()) {
        style = createDefaultStyle();

    } else {
        style = createSelectedStyle(IDs);
    }

    Layer layer = mapFrame.getMapContent().layers().get(0);
    ((FeatureLayer) layer).setStyle(style);
    mapFrame.getMapPane().repaint();
}
private Style createDefaultStyle() {
    Rule rule = createRule(LINE_COLOUR, FILL_COLOUR);

    FeatureTypeStyle fts = style_F.createFeatureTypeStyle();
    fts.rules().add(rule);

    Style style = style_F.createStyle();
    style.featureTypeStyles().add(fts);
    return style;
}
private Style createSelectedStyle(Set<FeatureId> IDs) {
    Rule selectedRule = createRule(SELECTED_COLOUR, SELECTED_COLOUR);
    selectedRule.setFilter(filter_F.id(IDs));

    Rule otherRule = createRule(LINE_COLOUR, FILL_COLOUR);
    otherRule.setElseFilter(true);

    FeatureTypeStyle fts = style_F.createFeatureTypeStyle();
    fts.rules().add(selectedRule);
    fts.rules().add(otherRule);

    Style style = style_F.createStyle();
    style.featureTypeStyles().add(fts);
    return style;
}
private Rule createRule(Color outlineColor, Color fillColor) {
    Symbolizer symbolizer = null;
    Fill fill = null;
    Stroke stroke = style_F.createStroke(filter_F.literal(outlineColor), filter_F.literal(LINE_WIDTH));

    switch (geometryType) {
        case poly:
            fill = style_F.createFill(filter_F.literal(fillColor), filter_F.literal(OPACITY));
            symbolizer = style_F.createPolygonSymbolizer(stroke, fill, geometryAttributeName);
            break;

        case line:
            symbolizer = style_F.createLineSymbolizer(stroke, geometryAttributeName);
            break;

        case point:
            fill = style_F.createFill(filter_F.literal(fillColor), filter_F.literal(OPACITY));

            Mark mark = style_F.getCircleMark();
            mark.setFill(fill);
            mark.setStroke(stroke);

            Graphic graphic = style_F.createDefaultGraphic();
            graphic.graphicalSymbols().clear();
            graphic.graphicalSymbols().add(mark);
            graphic.setSize(filter_F.literal(POINT_SIZE));

            symbolizer = style_F.createPointSymbolizer(graphic, geometryAttributeName);
    }

    Rule rule = style_F.createRule();
    rule.symbolizers().add(symbolizer);
    return rule;
}
private void setGeometry() {
    GeometryDescriptor geomDesc = featureSource.getSchema().getGeometryDescriptor();
    geometryAttributeName = geomDesc.getLocalName();

    Class<?> clazz = geomDesc.getType().getBinding();

    if (Polygon.class.isAssignableFrom(clazz) ||
            MultiPolygon.class.isAssignableFrom(clazz)) {
        geometryType = geomtype.poly;

    } else if (LineString.class.isAssignableFrom(clazz) ||
            MultiLineString.class.isAssignableFrom(clazz)) {

        geometryType = geomtype.line;

    } else {
        geometryType = geomtype.point;
    }

}
}
