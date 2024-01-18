package funkyguy3;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public strictfp class RobotPlayer {
    public static Random rng = null;

    public static boolean spawnDuck = false;
    public static MapLocation mainSpawnLoc = null;

    public static int SETUP_SPAWN = 50;

    public static final int MAX_HEALTH = 1000;
    public static final int MIN_HEALTH = 80; // min health for robot to engage in combat
    public static final int ATTACK_HEALTH = 300; // min health for an attacking robot to pursue

    public static RobotInfo[] nearbyEnemies = null;
    public static RobotInfo[] nearbyAllies = null;


    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };


    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        if (rng == null) {
            rng = new Random(rc.getID());
        }
        while (true) {
            try {
                if (rc.isSpawned()) {
                    int round = rc.getRoundNum();
                    if (round <= GameConstants.SETUP_ROUNDS) SetupPhase.runSetup(rc);
                    else MainPhase.runMainPhase(rc);
                }
                else {
                    trySpawn(rc);
                }

            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                Clock.yield();
            }
        }
    }
    public static void updateEnemyRobots(RobotController rc) throws GameActionException{
        // Sensing methods can be passed in a radius of -1 to automatically 
        // use the largest possible value.
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length != 0){
            // Save an array of locations with enemy robots in them for future use.
            MapLocation[] enemyLocations = new MapLocation[enemyRobots.length];
            for (int i = 0; i < enemyRobots.length; i++){
                enemyLocations[i] = enemyRobots[i].getLocation();
            }
            // Let the rest of our team know how many enemy robots we see!
            if (rc.canWriteSharedArray(0, enemyRobots.length)){
                rc.writeSharedArray(0, enemyRobots.length);
                int numEnemies = rc.readSharedArray(0);
            }
        }
    }

    public static void trySpawn(RobotController rc) throws GameActionException{
        if (spawnDuck && mainSpawnLoc != null){
            if (rc.canSpawn(mainSpawnLoc)) {
                rc.spawn(mainSpawnLoc);
            }
        }
        else {
            MapLocation[] spawnLocs = rc.getAllySpawnLocations();
            if (rc.getRoundNum() < SETUP_SPAWN){
                for (MapLocation loc : spawnLocs) {
                    if (rc.canSpawn(loc)) {
                        rc.spawn(loc);
                        break;
                    }
                }
            }
            else {
                List<MapLocation> queueSpawns = new ArrayList<MapLocation>(Arrays.asList(spawnLocs));
                while (!queueSpawns.isEmpty()) {
                    MapLocation loc = queueSpawns.get(rng.nextInt(queueSpawns.size()));
                    if (rc.canSpawn(loc)) {
                        rc.spawn(loc);
                        break;
                    }
                    queueSpawns.remove(loc);
                }
            }
        }
    }
}
