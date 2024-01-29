package funkyguy2v_1;

import battlecode.common.*;

public class Communication {


    private static int locToInt(RobotController rc, MapLocation loc)  {
        if (loc == null) return 0;
        return 1 + loc.x + loc.y * rc.getMapWidth();
    }

    private static MapLocation intToLoc(RobotController rc, int num) {
        if (num == 0) return null;
        return new MapLocation((num - 1)%rc.getMapWidth(), (num - 1)/rc.getMapWidth());
    }
}
