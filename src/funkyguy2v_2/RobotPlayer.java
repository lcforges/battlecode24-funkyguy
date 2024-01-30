package funkyguy2v_2;

import battlecode.common.*;

import java.util.*;


public strictfp class RobotPlayer {
    public static Random rng = null;

    public static boolean spawnDuck = false;
    public static MapInfo mainSpawn = null;
    public static int SETUP_SPAWN = 50;
    public static int SETUP_ROUNDS = 150;

    public static final int MAX_HEALTH = 1000;
    public static int EXPLORE_DIST;
    public static MapLocation exploreLoc = null;
    public static int sdArrayInd = -1;


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
        EXPLORE_DIST = (rc.getMapHeight() + rc.getMapWidth())/4;
        while (true) {
            try {
                if (rc.isSpawned()) {
                    int round = rc.getRoundNum();
                    if (round <= SETUP_ROUNDS) SetupPhase.runSetup(rc);
                    else MainPhase.runMainPhase(rc);
                }
                else {
//                    if (rc.readSharedArray(0) < 1){
//                        if (rc.canWriteSharedArray(0,rc.readSharedArray(0)+1)){
//                            rc.writeSharedArray(0,rc.readSharedArray(0)+1);
//                        }
                        trySpawn(rc);
//                    }
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
        if (spawnDuck && mainSpawn != null){
            if (rc.canSpawn(mainSpawn.getMapLocation())) {
                rc.spawn(mainSpawn.getMapLocation());
            }
        }
        else if (rc.readSharedArray(1) == 0 || rc.readSharedArray(2) == 0 || rc.readSharedArray(3) == 0){
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
                List<MapLocation> queueSpawns = new ArrayList<>(Arrays.asList(spawnLocs));
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
        else if (rc.readSharedArray(1) != 0 && rc.readSharedArray(2) != 0 && rc.readSharedArray(3) != 0){
            int x = rc.readSharedArray(1);
            int y = rc.readSharedArray(2);
            int z = rc.readSharedArray(3);
            List<MapLocation> queueSpawns = new ArrayList<>();
            if (x >= y && y >= z) {
                queueSpawns.addAll(getSpawnLocs((Communication.intToLocation(rc, rc.readSharedArray(4)))));
                queueSpawns.addAll(getSpawnLocs((Communication.intToLocation(rc, rc.readSharedArray(7)))));
                queueSpawns.addAll(getSpawnLocs((Communication.intToLocation(rc, rc.readSharedArray(10)))));
            }
            else if (y >= x && x >= z) {
                queueSpawns.addAll(getSpawnLocs((Communication.intToLocation(rc, rc.readSharedArray(7)))));
                queueSpawns.addAll(getSpawnLocs((Communication.intToLocation(rc, rc.readSharedArray(4)))));
                queueSpawns.addAll(getSpawnLocs((Communication.intToLocation(rc, rc.readSharedArray(10)))));
            }
            else if (z >= x && x >= y) {
                queueSpawns.addAll(getSpawnLocs((Communication.intToLocation(rc, rc.readSharedArray(10)))));
                queueSpawns.addAll(getSpawnLocs((Communication.intToLocation(rc, rc.readSharedArray(4)))));
                queueSpawns.addAll(getSpawnLocs((Communication.intToLocation(rc, rc.readSharedArray(7)))));
            }
            if (rng.nextInt(3) == 0) {
                while (!queueSpawns.isEmpty()) {
                    MapLocation loc = queueSpawns.get(0);
                    if (rc.canSpawn(loc)) {
                        rc.spawn(loc);
                        break;
                    }
                    queueSpawns.remove(loc);
                }
            }
            else {
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

    private static List<MapLocation> getSpawnLocs(MapLocation center) {
        List<MapLocation> res = new ArrayList<>();
        res.add(new MapLocation(center.x-1, center.y-1));
        res.add(new MapLocation(center.x-1, center.y));
        res.add(new MapLocation(center.x-1, center.y+1));
        res.add(new MapLocation(center.x, center.y-1));
        res.add(new MapLocation(center.x, center.y+1));
        res.add(new MapLocation(center.x+1, center.y-1));
        res.add(new MapLocation(center.x+1, center.y));
        res.add(new MapLocation(center.x+1, center.y+1));
        return res;
    }
}
