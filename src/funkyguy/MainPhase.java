package funkyguy;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MainPhase {

    public static void runMainPhase(RobotController rc) throws GameActionException {
        // ACTIONS
        // buy 750/1500 round upgrades
        if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) rc.buyGlobal(GlobalUpgrade.CAPTURING);
        if (rc.canBuyGlobal(GlobalUpgrade.ACTION)) rc.buyGlobal(GlobalUpgrade.ACTION);

        // attack enemy robots with flags
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (RobotInfo robot: nearbyEnemies) {
            if (robot.hasFlag()) {
                Pathfind.moveTowards(rc, robot.getLocation());
                if (rc.canAttack(robot.getLocation())) rc.attack(robot.getLocation());
            }
        }
        for (RobotInfo robot: nearbyEnemies) {
            if (rc.canAttack(robot.getLocation())) rc.attack(robot.getLocation());
        }

        // heal nearby ducks
        for (RobotInfo ally : rc.senseNearbyRobots(-1, rc.getTeam())){
            if (rc.canHeal(ally.getLocation())) rc.heal(ally.getLocation());
        }

        // MOVEMENT
        if (!rc.hasFlag()) {
            // move towards closest flag
            ArrayList<MapLocation> flagLocs = new ArrayList<>();
            FlagInfo[] enemyFlags = rc.senseNearbyFlags(-1,rc.getTeam().opponent());
            for (FlagInfo flag : enemyFlags) flagLocs.add(flag.getLocation());
            if (flagLocs.isEmpty()){
                MapLocation[] broadcastFlags = rc.senseBroadcastFlagLocations();
                flagLocs.addAll(Arrays.asList(broadcastFlags));
            }
            MapLocation closestFlag = findClosestLocation(rc.getLocation(), flagLocs);
            if (closestFlag != null) {
                Pathfind.moveTowards(rc, closestFlag);
                if (rc.canPickupFlag(closestFlag)) rc.pickupFlag(closestFlag);
            }
            else {
                // explore randomly if no flags nearby
                Pathfind.explore(rc);
            }
        }
        else {
            // move to closest ally spawn zone
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            MapLocation closestSpawn = findClosestLocation(rc.getLocation(), Arrays.asList(spawnLocs));
            Pathfind.moveTowards(rc, closestSpawn);
        }
    }

    public static MapLocation findClosestLocation(MapLocation loc1, List<MapLocation> otherLocs) {
        MapLocation closest = null;
        int minDist = Integer.MAX_VALUE;
        for (MapLocation loc2 : otherLocs) {
            int dist = loc1.distanceSquaredTo(loc2);
            if (dist < minDist) {
                minDist = dist;
                closest = loc2;
            }
        }
        return closest;
    }
}
