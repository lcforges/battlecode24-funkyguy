package funkyguy2v_1;

import battlecode.common.*;

import java.util.*;

public class MainPhase {

    private static int BOMBING_THRESHOLD = 12; // #of nearby ducks to blow up
    private static int STUNNING_THRESHOLD = 10;
    private static int FLAG_DISTANCE = 4;
    public static void runMainPhase(RobotController rc) throws GameActionException {
        // ACTIONS
        if (rc.canBuyGlobal(GlobalUpgrade.ACTION)) rc.buyGlobal(GlobalUpgrade.ACTION);
        if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) rc.buyGlobal(GlobalUpgrade.HEALING);
        if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) rc.buyGlobal(GlobalUpgrade.CAPTURING);


        if (!RobotPlayer.spawnDuck) {
            // get crumbs
            MapLocation[] nearbyCrumbs = rc.senseNearbyCrumbs(1);
            if (nearbyCrumbs.length != 0) {
                Pathfind.moveTowards(rc, nearbyCrumbs[0]);
            }
            if (!rc.hasFlag()) {
                attackEnemies(rc);

                healAllies(rc);
            }

            captureTheFlag(rc);
        }
        else {
            // only tries to add more traps if nearby bread
            if (rc.senseNearbyFlags(-1,rc.getTeam()).length != 0) {
                SetupPhase.trapSpawn(rc);
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
            MapLocation lowestHealthLoc = healths.remove().robot.getLocation();
            if (rc.canAttack(lowestHealthLoc)) rc.attack(lowestHealthLoc);
            else {
                while (!healths.isEmpty()){
                    lowestHealthLoc = healths.remove().robot.getLocation();
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
            // stun trap opponenet spawn
            else {
                MapLocation[] allySpawns = rc.getAllySpawnLocations();
                Direction dir = rc.getLocation().directionTo(allySpawns[RobotPlayer.rng.nextInt(allySpawns.length)]);
                if (rc.canBuild(TrapType.STUN, rc.getLocation().add(dir))) rc.build(TrapType.STUN, rc.getLocation().add(dir));
            }
        }
        if (enemies.length >= BOMBING_THRESHOLD){
            if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation())) {
                System.out.println("BOMBING");
                rc.build(TrapType.EXPLOSIVE, rc.getLocation());
            }
        }
        else if (enemies.length >= STUNNING_THRESHOLD) {
            if (rc.canBuild(TrapType.STUN, rc.getLocation())) {
                System.out.println("STUNNING");
                rc.build(TrapType.STUN, rc.getLocation());
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
                    // follow picked up enemy flag if far enough away
                    if (rc.getLocation().distanceSquaredTo(flag.getLocation()) > FLAG_DISTANCE) {
                        flagLocs.add(flag.getLocation());
                    }
                }
            }
            if (flagLocs.isEmpty()){
                MapLocation[] broadcastFlags = rc.senseBroadcastFlagLocations();
                flagLocs.addAll(Arrays.asList(broadcastFlags));
            }
            MapLocation closestFlag = Pathfind.findClosestLocation(rc.getLocation(), flagLocs);
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
        else {
            // move to closest ally spawn zone
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            MapLocation closestSpawn = Pathfind.findClosestLocation(rc.getLocation(), Arrays.asList(spawnLocs));
            Pathfind.moveTowards(rc, closestSpawn);
        }
    }


}
