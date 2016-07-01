package com.egorgrigorenko.visgraph;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.math.Vector2D;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.graph.build.line.BasicLineGraphGenerator;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.structure.basic.BasicEdge;
import org.geotools.graph.structure.basic.BasicNode;

import java.util.*;
import java.util.stream.Collectors;

public class VisibleGraphGenerator
{
    private Graph graph;
    private ArrayList<Coordinate> allVertices;
    private ArrayList<Polygon> obstacles;
    private TreeMap<Double, Geometry> intersectedSegments;

    public VisibleGraphGenerator() {
        BasicLineGraphGenerator graphGen = new BasicLineGraphGenerator();
        graph = graphGen.getGraph();
        allVertices = new ArrayList<>();
        intersectedSegments = new TreeMap<>();
        obstacles = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public void addObstacles(Collection<Polygon> obstacles) {
        this.obstacles.addAll(obstacles);
        initObstacleVertices(obstacles);
        initGraph(obstacles);

        for (Coordinate v: allVertices) {
            Collection<Coordinate> visibleVertices = getVisibleVertices(v, obstacles);
            Node vNode = getNodeByCoordinate(v);

            for (Coordinate w: visibleVertices) {
                Node wNode = getNodeByCoordinate(w);
                Edge e = new BasicEdge(vNode, wNode);
                graph.getEdges().add(e);
            }
        }
    }

    private void initObstacleVertices(Collection<Polygon> obstacles) {
        HashSet<Coordinate> coordinates = new HashSet<>();

        for (Polygon p: obstacles) {
            coordinates.addAll(Arrays.asList(p.getCoordinates()));
        }

        allVertices.addAll(coordinates);
    }

    @SuppressWarnings("unchecked")
    private void initGraph(Collection<Polygon> obstacles) {
        Collection<Node> graphNodes = graph.getNodes();

        for (Polygon p: obstacles) {
            for (Coordinate c: allVertices) {
                BasicNode node = new BasicNode();
                node.setObject(c);
                graphNodes.add(node);
            }
        }
    }

    private Collection<Coordinate> getVisibleVertices(Coordinate v, Collection<Polygon> obstacles) {
        ArrayList<Coordinate> visibleVertices = new ArrayList<>();

        Collection<Coordinate> sortedVerticesByAngle = sortVerticesByAngle(v, allVertices);
        initIntersectedSegments(v);

        return visibleVertices;
    }

    private void initIntersectedSegments(Coordinate v) {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        Geometry vRay = geometryFactory.createLineString(
                new Coordinate[] {v, new Coordinate(v.x, Double.POSITIVE_INFINITY)});

        for (Polygon p: obstacles) {
            // process exterior
            intersectRing(geometryFactory, vRay, p.getExteriorRing());

            // process interiors
            int countInteriors = p.getNumInteriorRing();
            for (int j = 0; j < countInteriors; ++j) {
                LineString ring = p.getInteriorRingN(j);
                intersectRing(geometryFactory, vRay, ring);
            }
        }
    }

    private void intersectRing(GeometryFactory geometryFactory, Geometry ray, LineString ring) {
        Coordinate[] coords = ring.getCoordinates();
        for (int j = 0; j < coords.length - 1; ++j) {
            Geometry segment = geometryFactory.createLineString(
                    new Coordinate[] {coords[j], coords[j + 1]});
            if (ray.crosses(segment)) {
                double distance = ray.distance(segment);
                intersectedSegments.put(distance, segment);
            }
        }
    }

    protected Collection<Coordinate> sortVerticesByAngle(Coordinate v, Collection<Coordinate> vertices) {

        class AngleVertex {
            public double angle;
            public Coordinate c;

            public AngleVertex(Coordinate p, Coordinate c) {
                angle = calcAngle(p, c);
                this.c = c;
            }

            private double calcAngle(Coordinate pC, Coordinate vC) {
                Vector2D v1 = new Vector2D(pC, new Coordinate(pC.x + 1, pC.y));
                Vector2D v2 = new Vector2D(pC, vC);

                double angle = v1.angleTo(v2);
                if (angle < 0) {
                    angle += Math.PI * 2;
                }

                return angle;
            }
        };

        Comparator<AngleVertex> comparator = (w1, w2) -> {
            if (w1.angle > w2.angle) {
                return -1;
            } else if (w1.angle < w2.angle) {
                return 1;
            }

            Vector2D v1 = new Vector2D(v, w1.c);
            Vector2D v2 = new Vector2D(v, w2.c);

            return v1.length() < v2.length() ? -1 : 1;
        };

        TreeSet<AngleVertex> sortedAngleVertices = new TreeSet<>(comparator);

        for (Coordinate c: vertices) {
            if (v.equals(c)) {
                continue;
            }
            sortedAngleVertices.add(new AngleVertex(v, c));
        }

        return sortedAngleVertices.stream().map(av -> av.c)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @SuppressWarnings("unchecked")
    private Node getNodeByCoordinate(Coordinate c) {
        Collection<Node> nodes = graph.getNodes();
        for (Node n: nodes) {
            if (n.getObject().equals(c)) {
                return n;
            }
        }

        return null;
    }

    public Graph getGraph() {
        return graph;
    }
}
