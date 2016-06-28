package com.egorgrigorenko.visgraph;

import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.graph.build.line.BasicLineGraphGenerator;
import org.geotools.graph.structure.Graph;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.graph.structure.Node;
import org.geotools.graph.structure.basic.BasicNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class VisibleGraphGenerator
{
    private Graph graph;

    public VisibleGraphGenerator() {
        BasicLineGraphGenerator graphGen = new BasicLineGraphGenerator();
        graph = graphGen.getGraph();
    }

    public void addObstacles(Collection<Polygon> obstacles) {
        initGraph(obstacles);
    }

    @SuppressWarnings("unchecked")
    private void initGraph(Collection<Polygon> obstacles) {
        Collection<Node> graphNodes = graph.getNodes();

        for (Polygon p: obstacles) {
            Set<Coordinate> coords = new HashSet<>(Arrays.asList(p.getCoordinates()));
            for (Coordinate c: coords) {
                BasicNode node = new BasicNode();
                node.setObject(c);
                graphNodes.add(node);
            }
        }
    }

    public Graph getGraph() {
        return graph;
    }
}
