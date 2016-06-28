package com.egorgrigorenko.visgraph;

import org.geotools.graph.build.line.BasicLineGraphGenerator;
import org.geotools.graph.structure.Graph;
import org.opengis.geometry.coordinate.Polygon;

import java.util.Collection;

public class VisibleGraphGenerator
{
    private Graph graph;

    public VisibleGraphGenerator() {
        BasicLineGraphGenerator graphGen = new BasicLineGraphGenerator();
        graph = graphGen.getGraph();
    }

    public void addObstacles(Collection<Polygon> obstacles) {
    }

    public Graph getGraph() {
        return graph;
    }
}
