package funkyguy2v_1;

import battlecode.common.*;

public class Communication {


    public static int locationToInt(RobotController rc, MapLocation loc) {
        if (loc == null)
            return 0;
        return 1 + loc.x + loc.y * rc.getMapWidth();
    }

    public static MapLocation intToLocation(RobotController rc, int m) {
        if (m == 0)
            return null;
        return new MapLocation((m - 1) % rc.getMapWidth(), (m - 1) / rc.getMapWidth());
    }
}
