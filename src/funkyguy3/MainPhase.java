package funkyguy3;

import battlecode.common.*;
import java.util.Arrays;
import java.util.PriorityQueue;

public class MainPhase {

    private static final int INF = 1000000000;
    public static FlagInfo[] nearbyAllyFlags = null;
    public static FlagInfo[] nearbyEnemyFlags = null;
    public static RobotInfo[] nearbyAllies = null;
    public static RobotInfo[] nearbyEnemies = null;
    public static RobotInfo[] attackableEnemies = null;
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
    public static float MAX_DISTANCE = 100000;
    public static float MAX_HEALTH = 1000;
    // action heuristic coefficients
    public static float acCooldown = -10;
    public static float acCrumbs = 100;
    public static float acEnemyHealth = -2;
    public static float acAllyHealth = 1;
    public static float acAttack = 1000;
    public static float acHeal = 209.5f;
    public static float acBuild = 100;
    public static float acNone = -100;

    // move heuristic coefficients
    public static float mcAlly = 0.2f;
    public static float mcAllyFlagHolder = -500;
    public static float mcEnemy = -2;
    public static float mcEnemyFlagHolder = -75;
    public static float mcNearbyAllyFlags = -10;
    public static float mcNearbyEnemyFlags = -100;
    public static float mcBroadcastFlags = -1;
    public static float mcFlagHolder = -1000;
    public static float mcAllyHealth = 5;
    public static float mcEnemyHealth = 1;
    public static float mcTooClose = -1000;
    public static float flagReward = 1000000;
    public static int actionCooldown = 0;
    public static int attack = 0;
    public static int build = 0;
    public static int heal = 0;
    public static int debug = -200;
    public static int BYTECODE_REMAINING = 0;
    public static int DEFENDERS = 5;
    public static int SMALL_GROUP = 3;
    public static float MAX_ACTION_COOLDOWN = 100;
    public static float MAX_ATTACK = 45;
    public static float MAX_HEAL = 70;
    public static float MAX_BUILD = 30;
    public static float MAX_CRUMBS = 10000;


    public static String[] actions = new String[]{"heal", "attack", "stun", "bomb", "flag", "none"};

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
            MAX_DISTANCE = (float) Math.pow(rc.getMapHeight(), 2)+ (float) Math.pow(rc.getMapWidth(), 2);
            if (nearbyCrumbs.length != 0) {
                Pathfind.moveTowards(rc, nearbyCrumbs[0]);
            }
            nearbyAllyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
            nearbyEnemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
            nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
            nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            broadcastFlags = rc.senseBroadcastFlagLocations();
            attackableEnemies = rc.senseNearbyRobots(4, rc.getTeam().opponent());
            crumbs = rc.getCrumbs();
            attack = rc.getExperience(SkillType.ATTACK);
            build = rc.getExperience(SkillType.BUILD);
            heal = rc.getExperience(SkillType.HEAL);
            actionCooldown = rc.getActionCooldownTurns();
            if (actionCooldown == 0) {
                nextAction(rc);
            }
            if (nearbyEnemies.length < SMALL_GROUP && !rc.hasFlag()) {
                if (nearbyEnemyFlags.length != 0){
                    Pathfind.moveTowards(rc, nearbyEnemyFlags[0].getLocation());
                } else if (broadcastFlags.length != 0) {
                    Pathfind.moveTowards(rc, broadcastFlags[0]);
                } else {
                    nextMove(rc);
                }
            }
            else if (rc.hasFlag()) {
                MapLocation dest = Pathfind.findClosestLocation(rc.getLocation(), Arrays.asList(rc.getAllySpawnLocations()));
                Pathfind.moveTowards(rc, dest);
            }
            else {
                nextMove(rc);
            }
            if (rc.getActionCooldownTurns() == 0) {
                nextAction(rc);
            }
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
        for (int i = nearbyAllies.length - 1; i >= 0; i--) {
            if (nearbyAllies[i].hasFlag()){
                if (rc.canHeal(nearbyAllies[i].getLocation())) rc.heal(nearbyAllies[i].getLocation());
                else {
                    Pathfind.moveTowards(rc, nearbyAllies[i].getLocation());
                    if (rc.canHeal(nearbyAllies[i].getLocation())) rc.heal(nearbyAllies[i].getLocation());
                }
            }
        }
        PriorityQueue<Tuple> healths = new PriorityQueue<>();
        for (int i = nearbyAllies.length - 1; i >= 0; i--) {
            healths.add(new Tuple(nearbyAllies[i].getHealth(), nearbyAllies[i]));
        }
        if (!healths.isEmpty()) {
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
        // attacks enemy flag holders and then weakest units
        for (int i = nearbyEnemies.length - 1; i >= 0; i--) {
            if (nearbyEnemies[i].hasFlag()){
                if (rc.canAttack(nearbyEnemies[i].getLocation())) rc.attack(nearbyEnemies[i].getLocation());
                else {
                    Pathfind.moveTowards(rc, nearbyEnemies[i].getLocation());
                    if (rc.canAttack(nearbyEnemies[i].getLocation())) rc.attack(nearbyEnemies[i].getLocation());
                }
            }
        }
        PriorityQueue<Tuple> healths = new PriorityQueue<>();
        for (int i = nearbyEnemies.length - 1; i >= 0; i--) {
            healths.add(new Tuple(nearbyEnemies[i].getHealth(), nearbyEnemies[i]));
        }
        if (!healths.isEmpty()) {
            MapLocation lowestHealthLoc = healths.remove().robot.getLocation();
            if (rc.canAttack(lowestHealthLoc)) rc.attack(lowestHealthLoc);
            else {
                while (!healths.isEmpty()){
                    lowestHealthLoc = healths.remove().robot.getLocation();
                    if (rc.canAttack(lowestHealthLoc)) rc.attack(lowestHealthLoc);
                }
            }
        }

    }

    private static float moveHeuristic(RobotController rc, Direction direction) throws GameActionException {
        float res = 0;
        MapLocation center = rc.getLocation().add(direction);
        if (!isLocOnMap(rc, center) || rc.senseMapInfo(center).isWall()) {
            return -INF;
        }
        if (Clock.getBytecodesLeft() < BYTECODE_REMAINING) {
            return -INF;
        }

        for (int i = nearbyAllies.length - 1; i >= 0; i--) {
            if (nearbyAllies[i].hasFlag()) {
                if (center.distanceSquaredTo(nearbyAllies[i].getLocation()) > 9){
                    res += mcAllyFlagHolder*center.distanceSquaredTo(nearbyAllies[i].getLocation())/MAX_DISTANCE;
                    res += (mcAllyFlagHolder+mcAllyHealth)*nearbyAllies[i].getHealth()/(MAX_HEALTH*(nearbyAllies.length+1));
                }
                else {
                    res += mcTooClose*MAX_DISTANCE;
                }
            }
            else {
                res += mcAlly*center.distanceSquaredTo(nearbyAllies[i].getLocation())/MAX_DISTANCE;
                res += mcAllyHealth*nearbyAllies[i].getHealth()/(MAX_HEALTH*(nearbyAllies.length+1));
            }
        }
        // include own health
        res += mcAllyHealth*rc.getHealth()/(MAX_HEALTH*(nearbyAllies.length+1));
        for (int i = nearbyEnemies.length - 1; i >= 0; i--){
            if (nearbyEnemies[i].hasFlag()) {
                res += mcEnemyFlagHolder*center.distanceSquaredTo(nearbyEnemies[i].getLocation())/MAX_DISTANCE;
                res += (mcEnemyFlagHolder+mcEnemyHealth)*nearbyEnemies[i].getHealth()/(MAX_HEALTH*nearbyEnemies.length);
            }
            else {
                res += mcEnemy*center.distanceSquaredTo(nearbyEnemies[i].getLocation())/MAX_DISTANCE;
                res += mcEnemyHealth*nearbyEnemies[i].getHealth()/(MAX_HEALTH*nearbyEnemies.length);
            }
        }
//        if (rc.hasFlag()) {
//            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
//            res += mcFlagHolder*center.distanceSquaredTo(Pathfind.findClosestLocation(center, Arrays.asList(spawnLocs)))/MAX_DISTANCE;
//        }
//        else {
//            for (int i = nearbyEnemyFlags.length - 1; i >= 0; i--) {
//                res += mcNearbyEnemyFlags*center.distanceSquaredTo(nearbyEnemyFlags[i].getLocation())/MAX_DISTANCE;
//            }
//            if (nearbyEnemyFlags.length == 0) {
//                if (nearbyEnemies.length < SMALL_GROUP) {
//                    for (int i = broadcastFlags.length - 1; i >= 0; i--) {
//                        res += mcBroadcastFlags*center.distanceSquaredTo(broadcastFlags[i])/MAX_DISTANCE/broadcastFlags.length;
//                    }
//                }
//            }
//        }
        for (FlagInfo flag : nearbyAllyFlags) {
            if (nearbyAllies.length < DEFENDERS){
                res += mcNearbyAllyFlags*center.distanceSquaredTo(flag.getLocation());
            }
        }
        return res;
    }

    private static void nextMove(RobotController rc) throws GameActionException  {
//        System.out.println("MOVE1: "+Clock.getBytecodeNum());
        float maxHeuristic = -INF;
        Direction maxDirection = null;
        for (int i = RobotPlayer.directions.length - 1; i >= 0; i--) {
            float newHeuristic = moveHeuristic(rc, RobotPlayer.directions[i]);
            if (Float.compare(newHeuristic, maxHeuristic) > 0) {
                maxDirection = RobotPlayer.directions[i];
                maxHeuristic = newHeuristic;
            }
        }
        if (maxDirection != null) {
            MapLocation nextLoc = rc.getLocation().add(maxDirection);
            Pathfind.moveTowards(rc, nextLoc);
        }
//        if (debug < 2) {
//            debug++;
//            System.out.println(maxDirection + ": " + maxHeuristic);
//        }

    }

    private static float actionHeuristic(RobotController rc, String action) {
        if (Clock.getBytecodesLeft() < BYTECODE_REMAINING) {
            return -INF;
        }
        float res = 0;
        float allyHealth = 0;
        float enemyHealth = 0;
        int hCrumbs = crumbs;
        int hAttack = attack;
        int hHeal = heal;
        int hBuild = build;
        int hActionCooldown = actionCooldown;
        int hEnemyFlagHolder = 0;
        for (int i = nearbyAllies.length - 1; i >= 0; i--) {
            allyHealth += nearbyAllies[i].getHealth()/(MAX_HEALTH*(nearbyAllies.length+1));
        }
        for (int i = nearbyEnemies.length - 1; i >= 0; i--) {
            if (nearbyEnemies[i].hasFlag()) {
                hEnemyFlagHolder = 1;
            }
            enemyHealth += nearbyEnemies[i].getHealth()/(MAX_HEALTH*nearbyEnemies.length);
        }
        // include own health
        allyHealth += rc.getHealth()/(MAX_HEALTH*(nearbyAllies.length+1));
        switch (action) {
            case "heal":
                if (nearbyAllies.length != 0 && allyHealth < MAX_HEALTH*(nearbyAllies.length+1)) {
                    hActionCooldown += GameConstants.HEAL_COOLDOWN;
                    allyHealth += HEAL_AMT/(MAX_HEALTH*(nearbyAllies.length+1));
                    hHeal += 1;
                }
            case "attack":
                if (attackableEnemies.length != 0) {
                    hActionCooldown += GameConstants.ATTACK_COOLDOWN;
                    enemyHealth -= ATTACK_AMT/(MAX_HEALTH*nearbyEnemies.length);
                    hAttack += 1;
                    res += hEnemyFlagHolder*flagReward;
                }
            case "stun":
                if (nearbyEnemies.length != 0) {
                    hActionCooldown += TRAP_COOLDOWN;
                    hCrumbs -= STUN_COST;
                    // ~%chance of activation * estimated number of enemies stunned * x% chance of getting attacked while stunned * attack damage / max health;
                    enemyHealth -= (float) (nearbyEnemies.length/VISION_AREA) * ((float) (nearbyEnemies.length/VISION_AREA)*STUN_AREA) * STUN_ATTACK_CHANCE * ATTACK_AMT / (MAX_HEALTH*nearbyEnemies.length);
                    hBuild += 1;
                }
            case "bomb":
                if (nearbyEnemies.length != 0) {
                    hActionCooldown += TRAP_COOLDOWN;
                    // %chance of activation * estimated # of enemies it will hit * damage / max health
                    enemyHealth -= (float) (nearbyEnemies.length/VISION_AREA) * ((float) (nearbyEnemies.length/VISION_AREA)*BOMB_AREA) * BOMB_DMG / (MAX_HEALTH*nearbyEnemies.length);
                    hCrumbs -= BOMB_COST;
                    hBuild += 1;
                }
            case "flag":
                if (nearbyEnemyFlags.length != 0 && rc.canPickupFlag(nearbyEnemyFlags[0].getLocation())) {
                    res += flagReward;
                }
            case "none":
                res += acNone*nearbyEnemies.length;
                assert true;
        }
        res += acCooldown*hActionCooldown/MAX_ACTION_COOLDOWN;
        res += acCrumbs*hCrumbs/MAX_CRUMBS + acEnemyHealth*enemyHealth + acAllyHealth*allyHealth;
        res += acAttack*hAttack/MAX_ATTACK + acBuild*hBuild/MAX_BUILD + acHeal*hHeal/MAX_HEAL;
//        if (debug < 2) {
//            debug++;
//            System.out.println(action + ": " + res + " Ally Health: " + allyHealth + " Enemy Health: " + enemyHealth + " Crumbs: " + (acCrumbs*hCrumbs/MAX_CRUMBS));
//        }
        return res;
    }

    private static void nextAction(RobotController rc) throws GameActionException{
//        System.out.println("ACTION1: " + Clock.getBytecodeNum());
        float maxHeuristic = -INF;
        String bestAction = "";
        for (int i = actions.length-1; i >= 0; i--) {
            float newHeuristic = actionHeuristic(rc, actions[i]);
            if (Float.compare(newHeuristic, maxHeuristic) > 0) {
                maxHeuristic = newHeuristic;
                bestAction = actions[i];
            }
        }
        if (debug < 2) {
            debug++;
            System.out.println(bestAction + ": " + maxHeuristic);
        }
        switch (bestAction) {
            case "attack":
                attackEnemies(rc);
            case "heal":
                healAllies(rc);
            case "bomb":
                if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation())) {
                    rc.build(TrapType.EXPLOSIVE, rc.getLocation());
                }
            case "stun":
                if (rc.canBuild(TrapType.STUN, rc.getLocation())) {
                    rc.build(TrapType.STUN, rc.getLocation());
                }
            case "flag":
                getFlag(rc);
            case "none":
                assert true;
        }
//        System.out.println("ACTION2: "+Clock.getBytecodeNum());
    }
    private static Boolean isLocOnMap(RobotController rc, MapLocation loc) throws GameActionException{
        return loc.x >= 0 && loc.y >= 0 && loc.x < rc.getMapWidth() && loc.y < rc.getMapHeight();
    }

    private static void getFlag(RobotController rc) throws GameActionException {
        if (nearbyEnemyFlags.length != 0 && rc.canPickupFlag(nearbyEnemyFlags[0].getLocation())) {
            rc.pickupFlag(nearbyEnemyFlags[0].getLocation());
        }
    }
}
