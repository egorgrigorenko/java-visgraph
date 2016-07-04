package com.egorgrigorenko.visgraph;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.structure.basic.BasicNode;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

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
    public void addObstacles_triangle_correctGraph() {
        // given
        VisibleGraphGenerator graphGenerator = new VisibleGraphGenerator();

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

        Coordinate t1 = new Coordinate(1, 1);
        Coordinate t2 = new Coordinate(4, 4);
        Coordinate t3 = new Coordinate(6, 0);
        Coordinate[] coords = new Coordinate[] { t1, t2, t3, t1};
        LinearRing ring = geometryFactory.createLinearRing(coords);
        Polygon triangle = geometryFactory.createPolygon(ring, null);

        List<Polygon> obstacles = new ArrayList<>();
        obstacles.add(triangle);

        // when
        graphGenerator.addObstacles(obstacles);

        // then
        Graph graph = graphGenerator.getGraph();

        assertEquals(3, graph.getNodes().size());
        assertEquals(3, graph.getEdges().size());
        assertConnections(graph, t1, Arrays.asList(t2, t3));
        assertConnections(graph, t2, Arrays.asList(t1, t3));
        assertConnections(graph, t3, Arrays.asList(t1, t2));
    }

    @SuppressWarnings("unchecked")
    void assertConnections(Graph graph, Coordinate examCoord, List<Coordinate> coordinates) {
        boolean isExamNodeFound = false;
        Collection<Node> nodes = graph.getNodes();
        for (Node n: nodes) {
            Coordinate c = (Coordinate) n.getObject();
            if (c.equals(examCoord)) {
                isExamNodeFound = true;
                Collection<Edge> edges = n.getEdges();
                assertEquals(coordinates.size(), edges.size());
                ArrayList<Integer> matchedIndices = new ArrayList<>();
                for (Edge e: edges) {
                    Node otherNode = e.getOtherNode(n);
                    Coordinate otherNodeCoord = (Coordinate) otherNode.getObject();
                    int index = coordinates.indexOf(otherNode);
                    if (index == -1) {
                        String errorMsg = String.format("node (%f, %f) isn't connected with " +
                                "node (%f, %f)", examCoord.x, examCoord.y,
                                otherNodeCoord.x, otherNodeCoord.y);
                        assertTrue(errorMsg, false);
                    } else {
                        matchedIndices.add(index);
                    }
                }

                // check matched indices
                for (int j = 0; j < coordinates.size(); ++j) {
                    String errorMsg = String.format("node (%f, %f) have to be connected with " +
                                    "node (%f, %f)", examCoord.x, examCoord.y,
                            coordinates.get(j).x, coordinates.get(j).y);
                    assertTrue(errorMsg, matchedIndices.contains(j));
                }
            }
        }
        assertTrue(isExamNodeFound);
    }

    @Test
    public void sortVerticesByAngle_givenVertices_whenSort_thenSorted() {
        // given
        Coordinate vc1 = new Coordinate(1, 1);
        Coordinate vc2 = new Coordinate(4, 4);
        Coordinate vc3 = new Coordinate(6, 0);
        ArrayList<Node> vertices = new ArrayList<>();
        Node v1 = new BasicNode();
        v1.setObject(vc1);
        Node v2 = new BasicNode();
        v2.setObject(vc2);
        Node v3 = new BasicNode();
        v3.setObject(vc3);
        vertices.addAll(Arrays.asList(v1, v2, v3));

        // when
        VisibleGraphGenerator graphGenerator = new VisibleGraphGenerator();
        Collection<Node> sortedVertices1 = graphGenerator
                .sortVerticesByAngle(v1, vertices);

        Collection<Node> sortedVertices2 = graphGenerator
                .sortVerticesByAngle(v2, vertices);

        Collection<Node> sortedVertices3 = graphGenerator
                .sortVerticesByAngle(v3, vertices);

        // assert
        assertArrayEquals(Arrays.asList(v3, v2).toArray(), sortedVertices1.toArray());
        assertArrayEquals(Arrays.asList(v3, v1).toArray(), sortedVertices2.toArray());
        assertArrayEquals(Arrays.asList(v1, v2).toArray(), sortedVertices3.toArray());
    }

    @Test
    public void sortVerticesByAngle_oneVerticeOnRay_closestEarlier() {
        // given
        Coordinate vc1 = new Coordinate(1, 1);
        Coordinate vc2 = new Coordinate(4, 4);
        Coordinate vc3 = new Coordinate(6, 1);
        Coordinate vc4 = new Coordinate(7, 1);
        ArrayList<Node> vertices = new ArrayList<>();
        Node v1 = new BasicNode();
        v1.setObject(vc1);
        Node v2 = new BasicNode();
        v2.setObject(vc2);
        Node v3 = new BasicNode();
        v3.setObject(vc3);
        Node v4 = new BasicNode();
        v4.setObject(vc4);
        vertices.addAll(Arrays.asList(v1, v2, v3, v4));

        // when
        VisibleGraphGenerator graphGenerator = new VisibleGraphGenerator();
        Collection<Node> sortedVertices1 = graphGenerator
                .sortVerticesByAngle(v1, vertices);

        vertices.clear();
        vertices.addAll(Arrays.asList(v1, v2, v4, v3));
        Collection<Node> sortedVertices2 = graphGenerator
                .sortVerticesByAngle(v1, vertices);

        // then
        assertArrayEquals(Arrays.asList(v2, v3, v4).toArray(), sortedVertices1.toArray());
        assertArrayEquals(Arrays.asList(v2, v3, v4).toArray(), sortedVertices2.toArray());
    }
}
