package com.egorgrigorenko.visgraph;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.graph.structure.Graph;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class VisibleGraphGeneratorTest
{
    @Test
    public void addObstacles_givenNoObstacles_whenGetGraph_thenEmpty() {
        // given
        VisibleGraphGenerator graphGenerator = new VisibleGraphGenerator();
        List<Polygon> obstacles = new ArrayList<>();

        // when
        graphGenerator.addObstacles(obstacles);

        // then
        Graph graph = graphGenerator.getGraph();

        assertEquals(0, graph.getNodes().size());
        assertEquals(0, graph.getEdges().size());
    }

    @Test
    public void addObstacles_givenRectAndTriangle_whenGetGraph_thenCorrect() {
        // given
        VisibleGraphGenerator graphGenerator = new VisibleGraphGenerator();

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

        Coordinate[] coords = new Coordinate[] { new Coordinate(20.6364, -133.5453167),
                new Coordinate(21.0639833, -188.6258167),
                new Coordinate(4.7045, -117.7161),
                new Coordinate(4.3393, -134.546),
                new Coordinate(20.6364, -133.5453167)};
        LinearRing ring = geometryFactory.createLinearRing(coords);
        Polygon rect = geometryFactory.createPolygon(ring, null);

        coords = new Coordinate[] { new Coordinate(16.2092500, -102.7966),
                new Coordinate(9.6997833, -98.9757667),
                new Coordinate(9.4288833, -108.9827333),
                new Coordinate(16.2092500, -102.7966)};
        ring = geometryFactory.createLinearRing(coords);
        Polygon triangle = geometryFactory.createPolygon(ring, null);

        List<Polygon> obstacles = new ArrayList<>();
        obstacles.add(rect);
        obstacles.add(triangle);

        // when
        graphGenerator.addObstacles(obstacles);

        // then
        Graph graph = graphGenerator.getGraph();

        assertEquals(7, graph.getNodes().size());
        assertEquals(12, graph.getEdges().size());
    }
}
