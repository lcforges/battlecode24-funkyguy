package funkyguy2v_2;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

public class MainPhase {

    private static int BOMBING_THRESHOLD = 200; // #of nearby ducks to blow up
    private static int STUNNING_THRESHOLD = 6;
    private static int FLAG_DISTANCE = 4;
    private static int SMALL_GROUP = 5;
    private static int CRUMB_CAP = 1000;
    private static int DEFENDERS = 3;
    public static void runMainPhase(RobotController rc) throws GameActionException {
        // ACTIONS
        if (rc.canBuyGlobal(GlobalUpgrade.ACTION)) rc.buyGlobal(GlobalUpgrade.ACTION);
        if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) rc.buyGlobal(GlobalUpgrade.HEALING);
        if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) rc.buyGlobal(GlobalUpgrade.CAPTURING);


        if (!RobotPlayer.spawnDuck) {
            // get crumbs
            MapLocation[] nearbyCrumbs = rc.senseNearbyCrumbs(1);
            FlagInfo[] pickableFlags = rc.senseNearbyFlags(4, rc.getTeam().opponent());
            FlagInfo[] allyFlags = rc.senseNearbyFlags(-1, rc.getTeam());

            if (nearbyCrumbs.length != 0) {
                Pathfind.moveTowards(rc, nearbyCrumbs[0]);
            }
            for (FlagInfo flag : pickableFlags) {
                if (!flag.isPickedUp()) {
                    if (rc.canPickupFlag(flag.getLocation())) rc.pickupFlag(flag.getLocation());
                }
            }
            if (rc.hasFlag()) {
                // move to closest ally spawn zone when holding flag
                MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                MapLocation closestSpawn = Pathfind.findClosestLocation(rc.getLocation(), Arrays.asList(spawnLocs));
                Pathfind.moveTowards(rc, closestSpawn);
            }
            else {
                if (allyFlags.length != 0){
                    RobotInfo[] nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
                    if (nearbyAllies.length < DEFENDERS) {
                        Pathfind.moveTowards(rc, allyFlags[0].getLocation());
                    }
                }
                attackEnemies(rc);
                healAllies(rc);
                RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                if (nearbyEnemies.length <= SMALL_GROUP) {
                    captureTheFlag(rc);
                }
            }
        }
        else {
            // only tries to add more traps if nearby bread
            if (RobotPlayer.sdArrayInd == -1) {
                for (int i = 3; i >= 1; i--) {
                    if (rc.readSharedArray(i) == 0) {
                        RobotPlayer.sdArrayInd = i;
                        break;
                    }
                }
                if (rc.canWriteSharedArray(3*RobotPlayer.sdArrayInd+1, Communication.locationToInt(rc, rc.getLocation()))) {
                    rc.writeSharedArray(3*RobotPlayer.sdArrayInd+1, Communication.locationToInt(rc, rc.getLocation()));
                    MapLocation loc = Communication.intToLocation(rc, Communication.locationToInt(rc, rc.getLocation()));
                    assert loc != null;
                }
            }
            if (rc.senseNearbyFlags(-1,rc.getTeam()).length != 0) {
                SetupPhase.trapSpawn(rc);
                RobotInfo[] spawnEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                if (rc.canWriteSharedArray(RobotPlayer.sdArrayInd, spawnEnemies.length + 1000)) {
                    rc.writeSharedArray(RobotPlayer.sdArrayInd, spawnEnemies.length + 1000);
                }
            }
            else {
                rc.writeSharedArray(RobotPlayer.sdArrayInd, 1);
            }
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
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        if (allies.length != 0) {
            for (RobotInfo ally : allies) {
                if (ally.hasFlag()){
                    if (rc.canHeal(ally.getLocation())) rc.heal(ally.getLocation());
                    else {
                        Pathfind.moveTowards(rc, ally.getLocation());
                        if (rc.canHeal(ally.getLocation())) rc.heal(ally.getLocation());
                    }
                }
            }
            PriorityQueue<Tuple> healths = new PriorityQueue<>();
            for (RobotInfo ally : allies) {
                healths.add(new Tuple(ally.getHealth(),ally));
            }

            while (!healths.isEmpty()){
                MapLocation lowestHealthLoc = healths.remove().robot.getLocation();
                if (rc.canHeal(lowestHealthLoc)) rc.heal(lowestHealthLoc);
            }
        }
    }

    private static void attackEnemies(RobotController rc) throws GameActionException {
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
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemies.length != 0) {
            for (RobotInfo enemy : enemies) {
                if (enemy.hasFlag()){
                    if (rc.canAttack(enemy.getLocation())) rc.attack(enemy.getLocation());
                    else {
                        Pathfind.moveTowards(rc, enemy.getLocation());
                        if (rc.canAttack(enemy.getLocation())) rc.attack(enemy.getLocation());
                    }
                }
            }
            PriorityQueue<Tuple> healths = new PriorityQueue<>();
            for (RobotInfo enemy : enemies) {
                healths.add(new Tuple(enemy.getHealth(),enemy));
            }
            while (!healths.isEmpty()) {
                MapLocation lowestHealthLoc = healths.remove().robot.getLocation();
                if (rc.canAttack(lowestHealthLoc)) rc.attack(lowestHealthLoc);
                else {
                    Pathfind.moveTowards(rc, lowestHealthLoc);
                    if (rc.canAttack(lowestHealthLoc)) rc.attack(lowestHealthLoc);
                }
            }
        }
        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        for (FlagInfo flag : nearbyFlags) {
            if (flag.isPickedUp()) {
                // build occasional defensive traps near captured flags
                if (RobotPlayer.rng.nextInt(25) == 0){
                    if (rc.canBuild(TrapType.STUN, rc.getLocation())){
                        rc.build(TrapType.STUN, rc.getLocation());
                    }
                }
            }
            // stun trap opponent spawn
            else {
                MapLocation[] allySpawns = rc.getAllySpawnLocations();
                Direction dir = rc.getLocation().directionTo(allySpawns[RobotPlayer.rng.nextInt(allySpawns.length)]);
                if (rc.canBuild(TrapType.STUN, rc.getLocation().add(dir)) ) {
                    rc.build(TrapType.STUN, rc.getLocation().add(dir));
                }
            }
        }
        if (enemies.length >= BOMBING_THRESHOLD){
            if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation())) {
                rc.build(TrapType.EXPLOSIVE, rc.getLocation());
            }
        }
        else if (enemies.length >= STUNNING_THRESHOLD) {
            if (rc.canBuild(TrapType.STUN, rc.getLocation())) {
                rc.build(TrapType.STUN, rc.getLocation());
            }
        }


    }

    private static void captureTheFlag(RobotController rc) throws GameActionException {
            // move towards closest flag
            ArrayList<MapLocation> flagLocs = new ArrayList<>();
            FlagInfo[] enemyFlags = rc.senseNearbyFlags(-1,rc.getTeam().opponent());
            for (FlagInfo flag : enemyFlags) {
                if (!flag.isPickedUp()){
                    flagLocs.add(flag.getLocation());
                }
                else {
                    // follow picked up enemy flag if far enough away, otherwise move away
                    if (rc.getLocation().distanceSquaredTo(flag.getLocation()) > FLAG_DISTANCE) {
                        Pathfind.moveTowards(rc, flag.getLocation());
                    }
                    else {
                        Direction dir = rc.getLocation().directionTo(flag.getLocation()).opposite();
                        Pathfind.moveTowards(rc, rc.getLocation().add(dir));
                    }
                }
            }
            boolean usingBroadcast = false;
            if (flagLocs.isEmpty()){
                MapLocation[] broadcastFlags = rc.senseBroadcastFlagLocations();
                flagLocs.addAll(Arrays.asList(broadcastFlags));
                usingBroadcast = true;
            }
            MapLocation closestFlag = Pathfind.findClosestLocation(rc.getLocation(), flagLocs);
//            if (usingBroadcast) {
//                closestFlag = flagLocs.get(RobotPlayer.rng.nextInt(flagLocs.size()));
//            }
            if (closestFlag != null) {
                if (rc.canPickupFlag(closestFlag)) {
                    rc.pickupFlag(closestFlag);
                }
                else {
                    Pathfind.moveTowards(rc, closestFlag);
                    if (rc.canPickupFlag(closestFlag)) rc.pickupFlag(closestFlag);
                }
            }
            else {
                // explore randomly if no flags nearby
                Pathfind.explore(rc);
            }

    }


}
