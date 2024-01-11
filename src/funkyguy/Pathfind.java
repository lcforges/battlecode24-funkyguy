package funkyguy;

import battlecode.common.*;

public class Pathfind {
    static Direction direction;

    public static void moveTowards(RobotController rc, MapLocation loc) throws GameActionException {
        // moves towards location and fill in water along the way
        Direction dir = rc.getLocation().directionTo(loc);
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
        else if (rc.canFill(rc.getLocation().add(dir))) {
                rc.fill(rc.getLocation().add(dir));
        }
        else {
            // choose new direction randomly if non-water obstacle
            Direction randomDir = Direction.allDirections()[RobotPlayer.rng.nextInt(8)];
            if (rc.canMove(randomDir)) {
                rc.move(randomDir);
            }
        }
    }

    public static void explore(RobotController rc) throws GameActionException {
        // move towards crumbs, otherwise move in current direction
        MapLocation[] nearbyCrumbs = rc.senseNearbyCrumbs(-1);
        if (nearbyCrumbs.length != 0) {
            moveTowards(rc, nearbyCrumbs[0]);
        }
        if (rc.isMovementReady()) {
            if (direction != null && rc.canMove(direction)) {
                rc.move(direction);
            }
            else {
                direction = Direction.allDirections()[RobotPlayer.rng.nextInt(8)];
                if (rc.canMove(direction)) rc.move(direction);
            }
        }
    }
}
