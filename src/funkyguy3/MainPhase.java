package funkyguy3;

import battlecode.common.*;
import battlecode.world.Trap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

public class MainPhase {

    private static int BOMBING_THRESHOLD = 12; // #of nearby ducks to blow up
    private static int STUNNING_THRESHOLD = 10;
    private static int FLAG_DISTANCE = 2;
    private static final int INF = 1000000000;
    public static FlagInfo[] nearbyAllyFlags = null;
    public static FlagInfo[] nearbyEnemyFlags = null;
    public static RobotInfo[] nearbyAllies = null;
    public static RobotInfo[] nearbyEnemies = null;
    public static MapLocation[] broadcastFlags = null;
    public static int crumbs = 0;
    public static int TRAP_COOLDOWN = 5;
    public static int BOMB_COST = 200;
    public static int STUN_COST = 100;
    public static int VISION_AREA = 60;
    public static int BOMB_AREA = 14;
    public static int STUN_AREA = 32;
    public static int BOMB_DMG = 750;
    public static int HEAL_AMT = 80;

    public static int ATTACK_AMT = 150;
    public static float STUN_ATTACK_CHANCE = 0.3f;

    public static String[] actions = new String[]{"heal", "attack", "stun", "bomb", "none"};

    public static void runMainPhase(RobotController rc) throws GameActionException {
        // ACTIONS
        if (rc.canBuyGlobal(GlobalUpgrade.ACTION)) {
            rc.buyGlobal(GlobalUpgrade.ACTION);
            ATTACK_AMT += 60;
        }
        if (rc.canBuyGlobal(GlobalUpgrade.HEALING)) {
            rc.buyGlobal(GlobalUpgrade.HEALING);
            HEAL_AMT += 50;
        }
        if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING)) rc.buyGlobal(GlobalUpgrade.CAPTURING);


        if (!RobotPlayer.spawnDuck) {
            // get crumbs
            MapLocation[] nearbyCrumbs = rc.senseNearbyCrumbs(1);
            if (nearbyCrumbs.length != 0) {
                Pathfind.moveTowards(rc, nearbyCrumbs[0]);
            }
            nearbyAllyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
            nearbyEnemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
            nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
            nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            broadcastFlags = rc.senseBroadcastFlagLocations();
            crumbs = rc.getCrumbs();
            nextAction(rc);
            nextMove(rc);
//            nextAction(rc);
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
        // attack enemy robots with flags first
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (RobotInfo robot: nearbyEnemies) {
            if (robot.hasFlag() && !rc.hasFlag()) {
                if (rc.canAttack(robot.getLocation())) rc.attack(robot.getLocation());
            }
        }
        for (RobotInfo robot: nearbyEnemies) {
            if (rc.canAttack(robot.getLocation()) && !rc.hasFlag()) {
                rc.attack(robot.getLocation());
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
                    // follow picked up enemy flag x% of time and far enough away
                    if (RobotPlayer.rng.nextInt(100) >= 30) {
                        if (rc.getLocation().distanceSquaredTo(flag.getLocation()) > FLAG_DISTANCE) {
                            flagLocs.add(flag.getLocation());
                        }
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

    private static float moveHeuristic(RobotController rc, Direction direction) throws GameActionException {
        float res = 0;
        float cAlly = -0.5f;
        float cAllyFlagHolder = -10;
        float cEnemy = 0.5f;
        float cEnemyFlagHolder = -10;
        float cNearbyAllyFlags = -10;
        float cNearbyEnemyFlags = -100;
        float cBroadcastFlags = -10;
        float cFlagHolder = -100;
        MapLocation center = rc.getLocation().add(direction);
        if (!isLocOnMap(rc, center) || rc.senseMapInfo(center).isWall()) {
            return -INF;
        }

        // go towards flag holders
        // go towards nearby allies
        // go away from nearby enemies
        for (RobotInfo ally: nearbyAllies) {
            if (ally.hasFlag()) {
                res += cAllyFlagHolder*center.distanceSquaredTo(ally.getLocation());
            }
            else {
                res += cAlly*center.distanceSquaredTo(ally.getLocation());
            }
        }
        for (RobotInfo enemy: nearbyEnemies) {
            if (enemy.hasFlag()) {
                res += cEnemyFlagHolder*center.distanceSquaredTo(enemy.getLocation());
            }
            else {
                res += cEnemy*center.distanceSquaredTo(enemy.getLocation());
            }
        }
        if (rc.hasFlag()) {
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            res += cFlagHolder*center.distanceSquaredTo(Pathfind.findClosestLocation(center, Arrays.asList(spawnLocs)));
        }
        else {
            for (FlagInfo flag : nearbyEnemyFlags) {
                res += cNearbyEnemyFlags*center.distanceSquaredTo(flag.getLocation());
            }
            if (nearbyEnemyFlags.length == 0) {
                for (MapLocation flagLoc : broadcastFlags) {
                    res += cBroadcastFlags*center.distanceSquaredTo(flagLoc);
                }
            }
        }
//        for (FlagInfo flag : nearbyAllyFlags) {
//            res += cNearbyAllyFlags*center.distanceSquaredTo(flag.getLocation());
//        }

        return res;
    }

    private static void nextMove(RobotController rc) throws GameActionException  {
        float maxHeuristic = -INF;
        Direction maxDirection = null;
        for (Direction dir : RobotPlayer.directions) {
            float newHeuristic = moveHeuristic(rc, dir);
            if (Float.compare(newHeuristic, maxHeuristic) > 0) {
                maxDirection = dir;
                maxHeuristic = newHeuristic;
            }
        }
        if (maxDirection != null) {
            Pathfind.moveTowards(rc, rc.getLocation().add(maxDirection));
        }
    }

    private static float actionHeuristic(RobotController rc, String action) {
        float res = 0;
        float cCooldown = 1;
        float cCrumbs = 1;
        float cEnemyHealth = -5;
        float cAllyHealth = 10;
        float allyHealth = 0;
        float enemyHealth = 0;
        int hCrumbs = crumbs;
        for (RobotInfo ally : nearbyAllies) {
            allyHealth += ally.getHealth();
        }
        for (RobotInfo enemy : nearbyEnemies) {
            enemyHealth += enemy.getHealth();
        }
        switch (action) {
            case "heal":
                res += cCooldown*(rc.getActionCooldownTurns() + GameConstants.HEAL_COOLDOWN - GameConstants.COOLDOWNS_PER_TURN);
                allyHealth += HEAL_AMT;
            case "attack":
                res += cCooldown*(rc.getActionCooldownTurns() + GameConstants.ATTACK_COOLDOWN - GameConstants.COOLDOWNS_PER_TURN);
                enemyHealth -= ATTACK_AMT;
            case "stun":
                res += cCooldown*(rc.getActionCooldownTurns() + TRAP_COOLDOWN - GameConstants.COOLDOWNS_PER_TURN);
                hCrumbs -= STUN_COST;
                // ~%chance of activation * estimated number of enemies stunned * x% chance of getting attacked while stunned * attack damage;
                enemyHealth -= (float) (nearbyEnemies.length/VISION_AREA)*((float) (nearbyEnemies.length/VISION_AREA)*STUN_AREA) * STUN_ATTACK_CHANCE * ATTACK_AMT;
            case "bomb":
                res += cCooldown*(rc.getActionCooldownTurns() + TRAP_COOLDOWN - GameConstants.COOLDOWNS_PER_TURN);
                // %chance of activation * estimated # of enemies it will hit * damage
                enemyHealth -= (float) (nearbyEnemies.length/VISION_AREA)*((float) (nearbyEnemies.length/VISION_AREA)*BOMB_AREA)*BOMB_DMG;
                hCrumbs -= BOMB_COST;
            case "none":
                res += cCooldown*(rc.getActionCooldownTurns() - GameConstants.COOLDOWNS_PER_TURN);
        }
        res += cCrumbs*hCrumbs + cEnemyHealth*enemyHealth + cAllyHealth*allyHealth;
        return res;
    }

    private static void nextAction(RobotController rc) throws GameActionException{
        float maxHeuristic = -INF;
        String bestAction = "";
        for (String action : actions) {
            float newHeuristic = actionHeuristic(rc, action);
            if (Float.compare(newHeuristic, maxHeuristic) > 0) {
                maxHeuristic = newHeuristic;
                bestAction = action;
            }
        }
        switch (bestAction) {
            case "attack":
                attackEnemies(rc);
                rc.setIndicatorString("Got 'eem");
            case "heal":
                healAllies(rc);
                rc.setIndicatorString("Healing eh");
            case "bomb":
                if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation())) {
                    rc.build(TrapType.EXPLOSIVE, rc.getLocation());
                }
                rc.setIndicatorString("GET DOWN!!");
            case "stun":
                if (rc.canBuild(TrapType.STUN, rc.getLocation())) {
                    rc.build(TrapType.STUN, rc.getLocation());
                }
                rc.setIndicatorString("Yung stunner");
            case "flag":
                getFlag(rc);
            case "none":
                assert true;
                rc.setIndicatorString("Vibin'");
        }
    }
    private static Boolean isLocOnMap(RobotController rc, MapLocation loc) throws GameActionException{
        return loc.x >= 0 && loc.y >= 0 && loc.x < rc.getMapWidth() && loc.y < rc.getMapHeight();
    }

    private static void getFlag(RobotController rc) throws GameActionException {

    }
}
