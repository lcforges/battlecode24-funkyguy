package funkyguy3;

import battlecode.common.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

public class MainPhase {

    public static void runMainPhase(RobotController rc) throws GameActionException {
        // ACTIONS
        if (rc.canBuyGlobal(GlobalUpgrade.ACTION)) rc.buyGlobal(GlobalUpgrade.ACTION);
        if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) rc.buyGlobal(GlobalUpgrade.HEALING);
        if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) rc.buyGlobal(GlobalUpgrade.CAPTURING);

        if (!RobotPlayer.spawnDuck) {
            RobotPlayer.nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());;
            RobotPlayer.nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());

            if (RobotPlayer.nearbyEnemies.length > RobotPlayer.nearbyAllies.length) {
                attackEnemies(rc);

                healAllies(rc);
            }
            else {
                healAllies(rc);

                attackEnemies(rc);
            }

            captureTheFlag(rc);

        }
        else {
            SetupPhase.trapSpawn(rc);
        }


    }

    private static void healAllies(RobotController rc) throws GameActionException {
        class Tuple implements Comparable<Tuple> {
            final int health;
            final RobotInfo robot;

            public Tuple(int health, RobotInfo robot) throws GameActionException {
                this.health = health;
                this.robot = robot;
            }

            @Override
            public int compareTo(Tuple tup) {
                return this.health - tup.health;
            }
        }
        // heals flag holders and then weakest units

        if (RobotPlayer.nearbyAllies.length != 0) {
            for (RobotInfo ally : RobotPlayer.nearbyAllies) {
                if (ally.hasFlag()){
                    if (rc.canHeal(ally.getLocation())) rc.heal(ally.getLocation());
                    else {
                        Pathfind.moveTowards(rc, ally.getLocation());
                        if (rc.canHeal(ally.getLocation())) rc.heal(ally.getLocation());
                    }
                }
            }
            PriorityQueue<Tuple> healths = new PriorityQueue<>();
            for (RobotInfo ally : RobotPlayer.nearbyAllies) {
                healths.add(new Tuple(ally.getHealth(),ally));
            }
            MapLocation lowestHealthLoc = healths.remove().robot.getLocation();
            if (rc.canHeal(lowestHealthLoc)) rc.heal(lowestHealthLoc);
            else {
                while (!healths.isEmpty()){
                    lowestHealthLoc = healths.remove().robot.getLocation();
                    if (rc.canHeal(lowestHealthLoc)) rc.heal(lowestHealthLoc);
                }
            }
        }
    }

    private static void attackEnemies(RobotController rc) throws GameActionException {
        // attack enemy robots with flags first

        for (RobotInfo robot: RobotPlayer.nearbyEnemies) {
            if (robot.hasFlag() && !rc.hasFlag()) {
                Pathfind.moveTowards(rc, robot.getLocation());
                if (rc.canAttack(robot.getLocation())) rc.attack(robot.getLocation());
            }
        }
        for (RobotInfo robot: RobotPlayer.nearbyEnemies) {
            if (!rc.hasFlag()){
                if (rc.getHealth() > RobotPlayer.MIN_HEALTH) {
                    if (rc.getHealth() > RobotPlayer.ATTACK_HEALTH) {
                        Pathfind.moveTowards(rc, robot.getLocation());
                    }
                    if (rc.canAttack(robot.getLocation()) && !rc.hasFlag()) {
                        rc.attack(robot.getLocation());
                    }
                }
                else {
                    // run away
                    Direction dir = rc.getLocation().directionTo(robot.getLocation()).opposite();
                    Pathfind.moveTowards(rc, rc.getLocation().add(dir));
                }

            }
        }
    }

    private static void captureTheFlag(RobotController rc) throws GameActionException {
        if (!rc.hasFlag()) {
            // move towards closest flag
            ArrayList<MapLocation> flagLocs = new ArrayList<>();
            FlagInfo[] enemyFlags = rc.senseNearbyFlags(-1,rc.getTeam().opponent());
            for (FlagInfo flag : enemyFlags) {
                if (!flag.isPickedUp()){
                    flagLocs.add(flag.getLocation());
                }
                else {
                    // follow picked up enemy flag x% of time
                    if (RobotPlayer.rng.nextInt(100) >= 30) {
                        flagLocs.add(flag.getLocation());
                    }
                }
            }
            if (flagLocs.isEmpty()){
                MapLocation[] broadcastFlags = rc.senseBroadcastFlagLocations();
                flagLocs.addAll(Arrays.asList(broadcastFlags));
            }
            MapLocation closestFlag = findClosestLocation(rc.getLocation(), flagLocs);
            if (closestFlag != null) {
                if (rc.canPickupFlag(closestFlag)) {
                    rc.pickupFlag(closestFlag);
                }
                else {
                    Pathfind.moveTowards(rc, closestFlag);
                    if (rc.canPickupFlag(closestFlag)) rc.pickupFlag(closestFlag);
                }
                // build occasional traps near flags
                if (rc.getLocation().distanceSquaredTo(closestFlag) < 9 && RobotPlayer.rng.nextInt(25) == 0){
                    if (rc.canBuild(TrapType.STUN, rc.getLocation())){
                        rc.build(TrapType.STUN, rc.getLocation());
                    }
                }
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
