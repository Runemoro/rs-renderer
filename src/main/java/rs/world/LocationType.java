package rs.world;


public enum LocationType {
    WALL(null),
    WALL_CONNECTOR(null),
    WALL_CORNER(null),
    WALL_PILLAR(null),
    WALL_DECORATION(null),
    WALL_DECORATION_OPPOSITE(WALL_DECORATION),
    WALL_DECORATION_DIAGONAL(WALL_DECORATION),
    WALL_DECORATION_OPPOSITE_DIAGONAL(WALL_DECORATION),
    WALL_DECORATION_DOUBLE(WALL_DECORATION),
    DIAGONAL_WALL(null),
    OBJECT(null),
    OBJECT_DIAGONAL(OBJECT),
    ROOF_SLOPE(null),
    ROOF_SLOPE_DIAGONAL(null),
    ROOF_HALF_SLOPE_DIAGONAL(null),
    ROOF_SLOPE_OUTER_CONNECTOR(null),
    ROOF_SLOPE_INNER_CONNECTOR(null),
    ROOF_FLAT(null),
    ROOF_EDGE_SLOPE(null),
    ROOF_EDGE_INNER_CONNECTOR(null),
    ROOF_EDGE_OUTER_CONNECTOR_TRIANGLE(null),
    ROOF_EDGE_OUTER_CONNECTOR_SQUARE(null),
    FLOOR_DECORATION(null);

    public final LocationType baseType;

    LocationType(LocationType baseType) {
        this.baseType = baseType == null ? this : baseType;
    }
}
