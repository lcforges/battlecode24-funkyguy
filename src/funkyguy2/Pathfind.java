package funkyguy2;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Pathfind {
    static Direction direction;
    int lefts;
    int rights;

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
            // Go around wall
            int dirIndex = 0;
            for (int i = 0; i < 8; i++) {
                if (RobotPlayer.directions[i] == dir) {
                    dirIndex = i;
                    break;
                }
            }
            dir = RobotPlayer.directions[(dirIndex+1)%8];
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
            else {
                // try to move randomly
                direction = Direction.allDirections()[RobotPlayer.rng.nextInt(8)];
                if (rc.canMove(direction)) rc.move(direction);
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
