package funkyguy2;

import battlecode.common.FlagInfo;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.TrapType;


public class SetupPhase {

    private static final int EXPLORE_ROUNDS = 150;
    public static void runSetup(RobotController rc) throws GameActionException {

        if (rc.getRoundNum() <= EXPLORE_ROUNDS){
            Pathfind.explore(rc);
        }
        else {
            // search for nearby flags to place traps
            FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam());
            FlagInfo target = null;
            for (FlagInfo flag : flags){
                if (!flag.isPickedUp()){
                    target = flag;
                    break;
                }
            }

            if (target != null) {
                Pathfind.moveTowards(rc, target.getLocation());
                if (rc.getLocation().distanceSquaredTo(target.getLocation()) < 4){
                    if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation())) {
                        rc.build(TrapType.EXPLOSIVE, rc.getLocation());
                    }
                }
            }
            else {
                Pathfind.explore(rc);
            }
        }

    }
}
