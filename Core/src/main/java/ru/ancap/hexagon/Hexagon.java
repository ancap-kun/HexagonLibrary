package ru.ancap.hexagon;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import ru.ancap.algorithm.compact.Morton64Compactor;
import ru.ancap.commons.Pair;
import ru.ancap.hexagon.common.Point;
import ru.ancap.hexagon.common.PointsListToPolygon;

import java.awt.*;
import java.util.List;
import java.util.*;

public record Hexagon(HexagonalGrid grid, long q, long r) {
    
    public long s() {
        return -(this.q + this.r);
    }
    
    public Polygon toPolygon() {
        return PointsListToPolygon.INSTANCE.apply(this.vertexes().stream().map(HexagonVertex::position).toList());
    }
    
    public long code() {
        Morton64Compactor mort = this.grid.morton();
        return mort.pack(this.q, this.r);
    }
    
    public Point center() {
        GridOrientation gridOrientation = this.grid.orientation();
        Point size = this.grid.size();
        Point origin = this.grid.origin();
        double x = (gridOrientation.f()[0] * this.q + gridOrientation.f()[1] * this.r) * size.x() + origin.x();
        double y = (gridOrientation.f()[2] * this.q + gridOrientation.f()[3] * this.r) * size.y() + origin.y();
        return new Point(x, y);
    }
    
    public HexagonSide side(int index) {
        return new HexagonSide(this, index);
    }
    
    public Set<HexagonSide> sides() {
        Set<HexagonSide> hexagonSides = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            hexagonSides.add(this.side(i));
        }
        return hexagonSides;
    }
    
    public HexagonVertex vertex(int index) {
        return new HexagonVertex(this, index);
    }
    
    public List<HexagonVertex> vertexes() {
        List<HexagonVertex> hexagonVertexes = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            hexagonVertexes.add(this.vertex(i));
        }
        return hexagonVertexes;
    }
    
    public Hexagon neighbor(int index) {
        Pair<Integer, Integer> modifier = modifierMap.get(index);
        return new Hexagon(this.grid, this.q + modifier.key(), this.r + modifier.value());
    }
    
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private static final Map<Integer, Pair<Integer, Integer>> modifierMap = Map.of(
        0, new Pair<>( 1,  0),
        1, new Pair<>( 0,  1),
        2, new Pair<>(-1,  1),
        3, new Pair<>(-1,  0),
        4, new Pair<>( 0, -1),
        5, new Pair<>( 1, -1)
    );
    
    public Set<Hexagon> neighbors(int layers) {
        Set<Hexagon> neighbors = new HashSet<>();
        for (long q = -layers; q <= layers; q++) {
            long r1 = Math.max(-layers, -q - layers);
            long r2 = Math.min(layers, -q + layers);
            for (long r = r1; r <= r2; r++) {
                if (q == 0 && r == 0) {
                    continue;
                }
                neighbors.add(new Hexagon(this.grid, q + this.q, r + this.r));
            }
        }
        return neighbors;
    }
    
    public boolean neighborOf(Hexagon hexagon) {
        return this.neighbors(1).contains(hexagon);
    }
    
    @Override
    public String toString() {
        return this.q+";"+this.r;
    }
    
}