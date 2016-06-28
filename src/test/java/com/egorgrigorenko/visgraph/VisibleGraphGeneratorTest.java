package com.egorgrigorenko.visgraph;

import org.geotools.graph.structure.Graph;
import org.junit.Test;
import org.opengis.geometry.coordinate.Polygon;

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
}
