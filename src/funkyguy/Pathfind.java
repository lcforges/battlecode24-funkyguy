package funkyguy;

import battlecode.common.*;

public class Pathfind {
    static Direction direction;
    int lefts;
    int rights;

    public static void moveTowards(RobotController rc, MapLocation loc) throws GameActionException {
        // moves towards location and fill in water along the way
        Direction dir = rc.getLocation().directionTo(loc);
        Direction inDir = dir;

        if (rc.canMove(dir)) {
            rc.move(dir);
        }
        else if (rc.canFill(rc.getLocation().add(dir))) {
                rc.fill(rc.getLocation().add(dir));
        }
        else {
            // Go clockwise around a wall
            int dirIndex = 0;
            for (int i = 0; i < 8; i++){
                if (RobotPlayer.directions[i] == dir) {
                    dirIndex = i;
                    break;
                }
            }
            while (rc.senseMapInfo(rc.getLocation().add(dir)).isWall() && rc.isMovementReady()) {
                dir = Direction.allDirections()[(dirIndex+1)%8];
                dirIndex++;
            }
            if (rc.canMove(dir)) {
                rc.setIndicatorString("Moving "+dir+" Instead of "+inDir);
                rc.move(dir);
            }
            else {
                Pathfind.explore(rc);
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
