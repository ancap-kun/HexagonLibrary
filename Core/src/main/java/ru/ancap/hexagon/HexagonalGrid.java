package ru.ancap.hexagon;

import org.jetbrains.annotations.NotNull;
import ru.ancap.algorithm.compact.Morton64Compactor;
import ru.ancap.algorithm.walkthrough.Walkthrough;
import ru.ancap.hexagon.common.Figure;
import ru.ancap.hexagon.common.Point;
import ru.ancap.hexagon.walkthrough.HexagonMethodApplier;
import ru.ancap.hexagon.walkthrough.HexagonalPolygonWalkthroughOperator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record HexagonalGrid(GridOrientation orientation, Point size, Point origin, Morton64Compactor morton) {
    
    public static final HexagonalGrid CLASSIC = new HexagonalGrid(GridOrientation.FLAT, new Point(100, 100), new Point(0, 0));
    
    public HexagonalGrid(HexagonalGrid hexagonalGrid) {
        this(hexagonalGrid.orientation, hexagonalGrid.size, hexagonalGrid.origin);
    }
    
    public HexagonalGrid(GridOrientation orientation, Point size, Point origin) {
        this(orientation, size, origin, new Morton64Compactor());
    }
    
    
    public Hexagon hexagon(long code) {
        int[] qr = this.morton.unpack(code);
        return new Hexagon(this, qr[0], qr[1]);
    }
    
    public Hexagon hexagon(Point point) {
        double x = (point.x() - this.origin.x()) / this.size.x();
        double y = (point.y() - this.origin.y()) / this.size.y();
        double q = this.orientation.b()[0] * x + this.orientation.b()[1] * y;
        double r = this.orientation.b()[2] * x + this.orientation.b()[3] * y;
        return this.fractionalHexagon(q, r).asStrict();
    }
    
    public FractionalHexagon fractionalHexagon(double q, double r) {
        return new FractionalHexagon(this, q, r);
    }
    
    public HexagonRegion region(Set<Hexagon> hexagons) {
        return new HexagonRegion(this, hexagons);
    }
    
    public HexagonRegion region(Figure figure) {
        return new HexagonRegion(this, new Walkthrough<>(
            this.hexagon(new Point(600, 600)),
            new HexagonalPolygonWalkthroughOperator(figure.toPolygon()),
            HexagonMethodApplier.INSTANCE
        ).walkthrough().collected());
    }
    
    public HexagonRegion regionByIntersection(Figure figure) {
        List<Point> figureVertexes = figure.vertexes();
        List<Hexagon> hexagons = new ArrayList<>();
        int len = figureVertexes.size();
        if (figureVertexes.getFirst().equals(figureVertexes.get(len - 1))) len -= 1;
        
        Hexagon hexagon = this.hexagon(figureVertexes.getFirst());
        long q1 = hexagon.q();
        long q2 = hexagon.q();
        long r1 = hexagon.r();
        long r2 = hexagon.r();
        
        for (int i = 1; i < len; i++) {
            hexagon = this.hexagon(figureVertexes.get(i));
            q1 = Math.min(q1, hexagon.q());
            q2 = Math.max(q2, hexagon.q());
            r1 = Math.min(r1, hexagon.r());
            r2 = Math.max(r2, hexagon.r());
        }
        
        q1 -= 1;
        q2 += 1;
        r1 -= 1;
        r2 += 1;
        
        for (long q = q1; q <= q2; q++) for (long r = r1; r <= r2; r++) {
            hexagon = new Hexagon(this, q, r);
            List<Point> corners = hexagon.vertexes().stream().map(HexagonVertex::position).collect(Collectors.toList());
            boolean add = false;
            
            for (int c = 0; c < 6; c++) if (pointInGeometry(figureVertexes, len, corners.get(c))) {
                add = true;
                break;
            }
                
            if (!add) for (int i = 0; i < len; i++) if (pointInGeometry(corners, 6, figureVertexes.get(i))) {
                add = true;
                break;
            }
                
            if (add) hexagons.add(hexagon);
        }
        
        return new HexagonRegion(this, new HashSet<>(hexagons));
    }
    
    private static boolean pointInGeometry(@NotNull List<Point> geometry, int len, Point point) {
        boolean contains = intersectsWithRaycast(point, geometry.get(len - 1), geometry.getFirst());
        for (int i = 1; i < len; i++) {
            if (intersectsWithRaycast(point, geometry.get(i - 1), geometry.get(i))) {
                contains = !contains;
            }
        }
        return contains;
    }
    
    private static boolean intersectsWithRaycast(Point point, Point start, Point end) {
        if (start.y() > end.y()) return intersectsWithRaycast(point, end, start);
        
        while (point.y() == start.y() || point.y() == end.y()) {
            double newY = Math.nextAfter(point.y(), Double.POSITIVE_INFINITY);
            point = new Point(point.x(), newY);
        }
        
        if (point.y() < start.y() || point.y() > end.y()) return false;
        
        if (start.x() > end.x()) {
            if (point.x() > start.x()) return false;
            if (point.x() < end.x())   return true;
        } else {
            if (point.x() > end.x())   return false;
            if (point.x() < start.x()) return true;
        }
        
        double raySlope = (point.y() - start.y()) / (point.x() - start.x());
        double diagonalSlope = (end.y() - start.y()) / (end.x() - start.x());
        
        return raySlope >= diagonalSlope;
    }
    
}