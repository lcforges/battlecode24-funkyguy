package funkyguy3;

import battlecode.common.*;

import java.util.HashSet;
import java.util.List;

public class Pathfind {
    static Direction direction;

    private static HashSet<MapLocation> line = null;
    private static int obsStartDist = 0;
    private static MapLocation prevDest = null;
    private static int bugState = 0; // 0 = going to target, 1 = circling obstacle
    private static Direction bugDir = null;

    public static void moveTowards(RobotController rc, MapLocation loc) throws GameActionException {
        // bugNav
//        bugNav(rc, loc);
        // moves towards location and fill in water along the way
        Direction dir = rc.getLocation().directionTo(loc);

        if (rc.canMove(dir)) {
            rc.move(dir);
        }
        //fills water with x% chance
        else if (rc.canFill(rc.getLocation().add(dir)) && RobotPlayer.rng.nextInt(25) == 0) {
            rc.fill(rc.getLocation().add(dir));
        }
        else if (!rc.sensePassability(rc.getLocation().add(dir))) {
            // Go around wall
            int dirIndex = 0;
            for (int i = 0; i < 8; i++) {
                if (RobotPlayer.directions[i] == dir) {
                    dirIndex = i;
                    break;
                }
            }
            for (int i = 0; i < 8; i++) {
                dir = RobotPlayer.directions[(dirIndex+1)%8];
                if (rc.isMovementReady() && rc.canMove(dir)) {
                    rc.move(dir);
                }
                dirIndex++;
            }
        }
        else {
            //move randomly
            direction = Direction.allDirections()[RobotPlayer.rng.nextInt(8)];
            if (rc.canMove(direction)) rc.move(direction);
        }
    }

    public static void explore(RobotController rc) throws GameActionException {
        // move towards crumbs, otherwise move in current direction
        MapLocation[] nearbyCrumbs = rc.senseNearbyCrumbs(-1);
        if (nearbyCrumbs.length != 0) {
            moveTowards(rc, nearbyCrumbs[0]);
        }
        if (rc.isMovementReady()) {
            // pathfind to random location
            if (RobotPlayer.exploreLoc == null) {
                RobotPlayer.exploreLoc = getRandomLocation(rc);
            }
            else {
                if (rc.getLocation().distanceSquaredTo(RobotPlayer.exploreLoc) <= 9){
                    RobotPlayer.exploreLoc = getRandomLocation(rc);
                }
            }
            moveTowards(rc, RobotPlayer.exploreLoc);
        }
    }
    private static MapLocation getRandomLocation(RobotController rc) throws GameActionException {
        MapLocation cur = rc.getLocation();
        int px = Math.min(cur.x + RobotPlayer.rng.nextInt(RobotPlayer.EXPLORE_DIST), rc.getMapWidth()-1);
        int py = Math.min(cur.y + RobotPlayer.rng.nextInt(RobotPlayer.EXPLORE_DIST), rc.getMapHeight()-1);
        int nx = Math.max(cur.x - RobotPlayer.rng.nextInt(RobotPlayer.EXPLORE_DIST), 0);
        int ny = Math.max(cur.y - RobotPlayer.rng.nextInt(RobotPlayer.EXPLORE_DIST), 0);
        MapLocation[] locs = new MapLocation[]{new MapLocation(px, py), new MapLocation(px, ny), new MapLocation(nx, py), new MapLocation(nx, ny)};
        return locs[RobotPlayer.rng.nextInt(4)];
    }

    private static void bugNav(RobotController rc, MapLocation loc) throws GameActionException{
        if (!loc.equals(prevDest)) {
            prevDest = loc;
            line = createLine(rc.getLocation(), loc);
        }
        for (MapLocation location : line) {
            rc.setIndicatorDot(location, rc.getID()%255,rc.getID()%255, rc.getID()%255);
        }
        if (bugState == 0) {
            bugDir = rc.getLocation().directionTo(loc);
            if (rc.canMove(bugDir)) rc.move(bugDir);
            else if (rc.canFill(rc.getLocation().add(bugDir))) {
                rc.fill(rc.getLocation().add(bugDir));
                if (rc.canMove(bugDir)) rc.move(bugDir);
            }
            else {
                bugState = 1;
                obsStartDist = rc.getLocation().distanceSquaredTo(loc);
            }
        }
        else {
            if (line.contains(rc.getLocation()) && rc.getLocation().distanceSquaredTo(loc) < obsStartDist) {
                bugState = 0;
            }

            for (int i = 0; i < 8; i++) {
                if (rc.canMove(bugDir)) {
                    rc.move(bugDir);
                    bugDir = bugDir.rotateRight();
                    break;
                }
                else if (rc.canFill(rc.getLocation().add(bugDir))) {
                    rc.fill((rc.getLocation().add(bugDir)));
                    if (rc.canMove(bugDir)) {
                        rc.move(bugDir);
                        bugDir = bugDir.rotateRight();
                        break;
                    }
                }
                else {
                    bugDir = bugDir.rotateLeft();
                }
            }
        }
    }

    public static void resetBug() throws GameActionException {
        bugState = 0;
        bugDir = null;
        prevDest = null;
        obsStartDist = 0;
    }

    public static HashSet<MapLocation> createLine(MapLocation a, MapLocation b) {
        HashSet<MapLocation> locs = new HashSet<>();
        int x = a.x;
        int y = a.y;
        int dx = b.x - a.x;
        int dy = b.y - a.y;
        int sx = (int) Math.signum(dx);
        int sy = (int) Math.signum(dy);
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        int d = Math.max(dx, dy);
        int r = d/2;
        if (dx > dy) {
            for (int i = 0; i < d; i++) {
                locs.add(new MapLocation(x,y));
                x += sx;
                r += dy;
                if (r >= dx) {
                    locs.add(new MapLocation(x,y));
                    y += sy;
                    r -= dx;
                }
            }
        }
        else {
            for (int i = 0; i < d; i++) {
                locs.add(new MapLocation(x, y));
                y += sy;
                r += dx;
                if (r >= dy) {
                    locs.add(new MapLocation(x,y));
                    x += sx;
                    r -= dy;
                }
            }
        }
        locs.add(new MapLocation(x,y));
        return locs;
    }

    public static MapLocation findClosestLocation(MapLocation loc1, List<MapLocation> otherLocs) {
        MapLocation closest = null;
        int minDist = Integer.MAX_VALUE;
        for (int i = otherLocs.size() - 1; i > 0; i--){
            int dist = loc1.distanceSquaredTo(otherLocs.get(i));
            if (dist < minDist) {
                minDist = dist;
                closest = otherLocs.get(i);
            }
        }
        return closest;
    }

}
