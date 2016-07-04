package com.egorgrigorenko.visgraph;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class VisibleGraphGenerator
{
    private Graph graph;
    private ArrayList<Node> allVertices;
    private ArrayList<Polygon> obstacles;
    private TreeMap<Double, ArrayList<Geometry>> intersectedSegments;

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
        initGraph();

        for (Node v: allVertices) {
            Collection<Node> visibleVertices = getVisibleVertices(v, obstacles);
            for (Node w: visibleVertices) {
                Edge e = new BasicEdge(v, w);
                graph.getEdges().add(e);
            }
        }
    }


    private void initObstacleVertices(Collection<Polygon> obstacles) {
        for (Polygon p: obstacles) {
            Coordinate[] extCoords = p.getExteriorRing().getCoordinates();
            for (int j = 0; j < extCoords.length - 1; ++j) {
                Node n = new BasicNode();
                n.setObject(extCoords[j]);
                allVertices.add(n);
            }

            for (int j = 0; j < p.getNumInteriorRing(); ++j) {
                Coordinate[] intCoords = p.getInteriorRingN(j).getCoordinates();
                for (int i = 0; j < intCoords.length - 1; ++i) {
                    Node n = new BasicNode();
                    n.setObject(intCoords[i]);
                    allVertices.add(n);
                }
            }
        }
    }


    @SuppressWarnings("unchecked")
    private void initGraph() {
        Collection<Node> graphNodes = graph.getNodes();
        for (Node v: allVertices) {
            graphNodes.add(v);
        }
    }

    private Collection<Node> getVisibleVertices(Node v, Collection<Polygon> obstacles) {
        ArrayList<Node> visibleVertices = new ArrayList<>();

        Collection<Node> sortedVerticesByAngle = sortVerticesByAngle(v, allVertices);
        initIntersectedSegments(v);
        for (Node w: sortedVerticesByAngle) {
            if (v.equals(w)) {
                continue;
            }

            if (isVisible(v, w)) {
                visibleVertices.add(w);
            }

            addCwSideEdges(v, w);
            removeCcwSideEdges(v, w);
        }

        return visibleVertices;
    }

    @SuppressWarnings("unchecked")
    private void removeCcwSideEdges(Node v, Node w) {
        List<Edge> edges = w.getEdges();
        for (Edge edge: edges) {
            Node n = edge.getOtherNode(w);
            if (n.equals(v)) {
                continue;
            }

            Coordinate vc = (Coordinate) v.getObject();
            Coordinate wc = (Coordinate) w.getObject();
            Coordinate nc = (Coordinate) n.getObject();

            boolean isCcw = CGAlgorithms.isCCW(new Coordinate[]{vc, wc, nc});

            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
            Geometry eg = geometryFactory.createLineString(new Coordinate[] {
                    wc,nc });

            Geometry vp = geometryFactory.createPoint(vc);
            Geometry wp = geometryFactory.createPoint(wc);
            double distance = vp.distance(wp);

            ArrayList<Geometry> cwEdges = intersectedSegments.get(distance);
            if (cwEdges != null) {
                cwEdges.removeIf((Geometry g) -> g.equals(eg));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addCwSideEdges(Node v, Node w) {
        List<Edge> edges = w.getEdges();
        for (Edge edge: edges) {
            Node n = edge.getOtherNode(w);
            if (n.equals(v)) {
                continue;
            }

            Coordinate vc = (Coordinate)v.getObject();
            Coordinate wc = (Coordinate)w.getObject();
            Coordinate nc = (Coordinate)n.getObject();

            boolean isCcw = CGAlgorithms.isCCW(new Coordinate[] { vc, wc, nc });

            if (isCcw) {
                continue;
            }

            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
            Geometry vp = geometryFactory.createPoint(vc);
            Geometry wp = geometryFactory.createPoint(wc);
            double distance = vp.distance(wp);

            Geometry eg = geometryFactory.createLineString(new Coordinate[] {
                    wc,nc });

            ArrayList<Geometry> geometries = intersectedSegments.get(distance);
            if (geometries == null) {
                ArrayList<Geometry> cwEdges = new ArrayList<>();
                cwEdges.add(eg);
                intersectedSegments.put(distance, cwEdges);
            } else {
                geometries.add(eg);
            }
        }
    }

    private void initIntersectedSegments(Node v) {
        Coordinate vCoord = (Coordinate)v.getObject();
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        Geometry vRay = geometryFactory.createLineString(
                new Coordinate[] {vCoord, new Coordinate(vCoord.x,
                        Double.POSITIVE_INFINITY)});

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

                ArrayList<Geometry> geometries = intersectedSegments.get(distance);
                if (geometries == null) {
                    geometries = new ArrayList<>();
                    geometries.add(segment);
                    intersectedSegments.put(distance, geometries);
                } else {
                    geometries.add(segment);
                }
            }
        }
    }

    protected Collection<Node> sortVerticesByAngle(Node v, Collection<Node> vertices) {
        class AngleVertex {
            public double angle;
            public Node v;

            public AngleVertex(Node p, Node w) {
                angle = calcAngle(p, w);
                v = w;
            }

            private double calcAngle(Node p, Node w) {
                Coordinate pC = (Coordinate)p.getObject();
                Coordinate wC = (Coordinate)w.getObject();
                Vector2D v1 = new Vector2D(pC, new Coordinate(pC.x + 1, pC.y));
                Vector2D v2 = new Vector2D(pC, wC);

                double angle = v1.angleTo(v2);
                if (angle < 0) {
                    angle += Math.PI * 2;
                }

                return angle;
            }
        }

        Comparator<AngleVertex> comparator = (w1, w2) -> {
            if (w1.angle > w2.angle) {
                return -1;
            } else if (w1.angle < w2.angle) {
                return 1;
            }

            Coordinate pc = (Coordinate)v.getObject();
            Vector2D v1 = new Vector2D(pc, (Coordinate)w1.v.getObject());
            Vector2D v2 = new Vector2D(pc, (Coordinate)w2.v.getObject());

            return v1.length() < v2.length() ? -1 : 1;
        };

        TreeSet<AngleVertex> sortedAngleVertices = new TreeSet<>(comparator);

        for (Node c: vertices) {
            if (v.equals(c)) {
                continue;
            }
            sortedAngleVertices.add(new AngleVertex(v, c));
        }

        return sortedAngleVertices.stream().map(av -> av.v)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Graph getGraph() {
        return graph;
    }
}
