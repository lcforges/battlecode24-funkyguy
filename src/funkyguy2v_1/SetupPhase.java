package funkyguy2v_1;

import battlecode.common.*;


public class SetupPhase {

    public static void runSetup(RobotController rc) throws GameActionException {

        if (RobotPlayer.spawnDuck) {
            trapSpawn(rc);
        }
        else {
            FlagInfo[] flags = rc.senseNearbyFlags(-1, rc.getTeam());
            FlagInfo target = null;
            for (FlagInfo flag : flags){
                if (!flag.isPickedUp()){
                    target = flag;
                    break;
                }
            }
            if (target != null) {
                int dist = rc.getLocation().distanceSquaredTo(target.getLocation());
                if (dist == 0){
                    RobotPlayer.mainSpawn = rc.senseMapInfo(rc.getLocation());
                    RobotPlayer.spawnDuck = true;
                }
                else {
                    Pathfind.explore(rc);
                }
            }
            else {
                Pathfind.explore(rc);
            }
        }
    }

    public static void trapSpawn(RobotController rc) throws GameActionException {
        for (int i = 0; i< 8; i++) {
            Direction dir =  RobotPlayer.directions[i];
            if (i%2 == 0) {
                if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation().add(dir))){
                    rc.build(TrapType.EXPLOSIVE, rc.getLocation().add(dir));
                }
            }
            else {
                if (rc.canBuild(TrapType.STUN, rc.getLocation().add(dir))) {
                   rc.build(TrapType.STUN, rc.getLocation().add(dir));
                }
            }
        }

    }
}
