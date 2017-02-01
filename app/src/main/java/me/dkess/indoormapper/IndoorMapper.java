package me.dkess.indoormapper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class IndoorMapper {
    private static boolean hasSameElements(Direction[] a, Set<Direction> b) {
        for (Direction d : Direction.values()) {
            boolean inArray = false;
            for (Direction e : a) {
                if (d == e) {
                    inArray = true;
                    break;
                }
            }

            if (b.contains(d) != inArray) {
                return false;
            }
        }
        return true;
    }

    public static class IndoorMap {
        public ArrayList<LogEntry> log;
        public ArrayList<MapNode> nodes;

        public JSONObject toJSON() {
            ArrayList<JSONObject> logj = new ArrayList<>(this.log.size());
            for (LogEntry e : this.log) {
                logj.add(e.toJSON());
            }

            ArrayList<JSONObject> nodesj = new ArrayList<>(this.nodes.size());
            for (MapNode n : this.nodes) {
                nodesj.add(n.toJSON());
            }

            JSONObject retval = new JSONObject();
            try {
                retval.put("nodes", new JSONArray(nodesj));
                retval.put("log", new JSONArray(logj));
                return retval;
            } catch (JSONException e) {}
            return null;
        }

        public static IndoorMap readFromFile(File file) {
            IndoorMapper.IndoorMap map = null;
            try {
                FileInputStream stream = new FileInputStream(file);
                String jsonStr = null;
                try {
                    FileChannel fc = stream.getChannel();
                    MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                    jsonStr = Charset.defaultCharset().decode(bb).toString();
                    if (jsonStr.equals("")) {
                        throw new FileNotFoundException("file was empty");
                    }
                    map = IndoorMapper.IndoorMap.fromJSON(new JSONObject(jsonStr));
                } catch (java.io.IOException e) {
                } catch (JSONException e) {
                } finally {
                    stream.close();
                }
            } catch (FileNotFoundException e) {
                map = IndoorMapper.makeEmptyMap();
            } catch (java.io.IOException e) {
                return null;
            }
            return map;
        }

        public boolean writeToFile(File file) {
            try {
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(toJSON().toString());
                fileWriter.flush();
                fileWriter.close();
            } catch (java.io.IOException e) {
                return false;
            } catch (NullPointerException e) {
                return false;
            }
            return true;
        }

        public IndoorMap(ArrayList<LogEntry> log, ArrayList<MapNode> nodes) {
            this.log = log;
            this.nodes = nodes;
        }

        public static IndoorMap fromJSON(JSONObject json) {
            try {
                JSONArray logArray = json.getJSONArray("log");
                ArrayList<LogEntry> log = new ArrayList<>(logArray.length());
                for (int i = 0; i < logArray.length(); i++) {
                    log.add(LogEntry.fromJSON(logArray.getJSONObject(i)));
                }

                JSONArray nodesArray = json.getJSONArray("nodes");
                ArrayList<MapNode> nodes = new ArrayList<>(nodesArray.length());
                for (int i = 0; i < nodesArray.length(); i++) {
                    nodes.add(MapNode.fromJSON(nodesArray.getJSONObject(i)));
                }

                return new IndoorMap(log, nodes);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static class LogEntry {
        public final Direction afterturn;
        public final long time_enter;
        public long time_exit;
        public final int node_id;

        public LogEntry(Direction afterturn, long time_enter, long time_exit, int node_id) {
            this.afterturn = afterturn;
            this.time_enter = time_enter;
            this.time_exit = time_exit;
            this.node_id = node_id;
        }

        public JSONObject toJSON() {
            JSONObject retval = new JSONObject();
            try {
                retval.put("afterturn", afterturn.toString());
                retval.put("time_enter", time_enter);
                retval.put("time_exit", time_exit);
                retval.put("node", node_id);
                return retval;
            } catch (JSONException e) {}
            return null;
        }

        public static LogEntry fromJSON(JSONObject json) throws JSONException {
            return new LogEntry(Direction.valueOf(json.getString("afterturn")),
                    json.getLong("time_enter"),
                    json.getLong("time_exit"),
                    json.getInt("node"));
        }
    }

    private static class MapNode {
        /** -1 means unexplored, 0 or above is a node id */
        private final EnumMap<Direction, Integer> branches;
        private final String description;

        public MapNode(EnumSet<Direction> branches, String description) {
            this.branches = new EnumMap<Direction, Integer>(Direction.class);
            for (Direction d : branches) {
                this.branches.put(d, -1);
            }
            this.description = description;
        }

        private MapNode(EnumMap<Direction, Integer> branches, String description) {
            this.branches = branches;
            this.description = description;
        }

        public JSONObject toJSON() {
            JSONObject retval = new JSONObject();
            try {
                JSONObject b = new JSONObject();
                for (Map.Entry<Direction, Integer> e : branches.entrySet()) {
                    if (e.getValue() == -1) {
                        b.put(e.getKey().name(), JSONObject.NULL);
                    } else {
                        b.put(e.getKey().name(), e.getValue());
                    }
                }
                retval.put("branches", b);
                retval.put("description", description);
                return retval;
            } catch (JSONException e) {}
            return null;
        }

        public static MapNode fromJSON(JSONObject json) throws JSONException {
            EnumMap<Direction, Integer> branches = new EnumMap<Direction, Integer>(Direction.class);
            JSONObject branchesObj = json.getJSONObject("branches");
            Iterator<String> iter = branchesObj.keys();
            while (iter.hasNext()) {
                String dn = iter.next();
                if (branchesObj.isNull(dn)) {
                    branches.put(Direction.valueOf(dn), -1);
                } else {
                    branches.put(Direction.valueOf(dn), branchesObj.getInt(dn));
                }
            }

            return new MapNode(branches, json.getString("description"));
        }
    }

    public static IndoorMap makeEmptyMap() {
        ArrayList<LogEntry> log = new ArrayList<>(1);
        log.add(new LogEntry(Direction.forward, System.currentTimeMillis(), -1, 0));

        ArrayList<MapNode> nodes = new ArrayList<>(1);
        nodes.add(new MapNode(EnumSet.of(Direction.forward), "root node"));

        return new IndoorMap(log, nodes);
    }

    public static class ForkResult {
        String on;
        int on_id;
        ArrayList<IntString> choices;
    }

    IndoorMap map;
    Direction currently_facing;
    Direction behind;
    EnumSet<Direction> abs_branches;
    int last_node_id;

    public IndoorMapper(IndoorMap map) {
        this.map = map;
    }

    /**
     * The possibilities are:
     * - Something happened that triggered early termination (inconsistent nodes)
     * - We are already on a known node, and we know we are on it: all values will be filled except
     *   choices which will be null
     * - We created a new node which does not need a name: choices will be null, on will be "",
     *   and on_id will be filled
     * - We don't know which node we are on: choices will be filled, on will be null, node_id will
     *   be -1
     * @param dirs
     * @return
     */
    public ForkResult fork_part1(EnumSet<Direction> dirs) {
        currently_facing = map.log.get(map.log.size() - 1).afterturn;
        behind = currently_facing.opposite();

        abs_branches = EnumSet.noneOf(Direction.class);
        for (Direction d : dirs) {
            abs_branches.add(d.add(currently_facing));
        }
        abs_branches.add(behind);

        last_node_id = map.log.get(map.log.size() - 1).node_id;

        // check if we are definitely at an already existing node
        int node_id = -1;
        MapNode node = null;
        ForkResult forkResult = new ForkResult();
        int i = 0;
        for (MapNode mn : map.nodes) {
            if (mn.branches.containsKey(behind)
                    && mn.branches.get(behind) == last_node_id) {
                node = mn;
                node_id = i;
            }
            i++;
        }

        if (node != null) {
            // Do an extra check that this node is consistent
            if (!abs_branches.equals(node.branches.keySet())) {
                return null;
            }

            forkResult.on = node.description;
            forkResult.on_id = node_id;
        } else if (dirs.size() <= 1) {
            // create a new node
            node_id = map.nodes.size();

            node = new MapNode(abs_branches, "");
            node.branches.put(behind, last_node_id);
            map.nodes.add(node);

            forkResult.on = "";
            forkResult.on_id = node_id;
        } else {
            forkResult.choices = new ArrayList<>();
            // Generate the list of possible existing nodes we could be on
            int j = 0;
            for (MapNode mn : map.nodes) {
                if (abs_branches.equals(mn.branches.keySet())
                        && mn.branches.containsKey(behind)
                        && mn.branches.get(behind) == -1) {
                    forkResult.choices.add(new IntString(j, mn.description));
                }
                j++;
            }
        }

        return forkResult;
    }

    /**
     * Returns the absolute direction leading us to an edge that has not been
     * visited in reverse.
     */
    private Direction goTowardsNewEdge(int start) {
        HashSet<IntPair> traveled = new HashSet<>();

        for (int i = 0; i < map.log.size() - 1; i++) {
            traveled.add(new IntPair(map.log.get(i).node_id, map.log.get(i + 1).node_id));
        }

        HashMap<Integer, Integer> goalsdict = new HashMap<>();

        System.out.println("goals:");
        goals_remaining = 0;
        for (IntPair p : traveled) {
            if (!traveled.contains(p.reversed())) {
                goalsdict.put(p.b, p.a);
                System.out.println("goal: " + p.b + ", " + p.a);
                goals_remaining += 1;
            }
        }


        if (goalsdict.isEmpty()) {
            return null;
        }

        HashMap<Integer, Integer> parent = new HashMap<>();
        HashMap<Integer, Direction> parentDir = new HashMap<>();

        Queue<Integer> frontier = new ArrayDeque<>();
        parent.put(start, -1);

        frontier.add(start);

        while (!frontier.isEmpty()) {
            int current = frontier.remove();
            MapNode currentNode = map.nodes.get(current);

            // check if we have reached a goal
            Integer possible = goalsdict.get(current);
            if (possible != null) {
                // compute path back to start
                int trailer = possible;

                System.out.println("going towards node #" + trailer);

                while (current != start) {
                    trailer = current;
                    current = parent.get(current);
                }

                // get the direction to move from current to trailer
                for (Map.Entry<Direction, Integer> e : map.nodes.get(current).branches.entrySet()) {
                    if (e.getValue() == trailer) {
                        return e.getKey();
                    }
                }
            }

            // iterate through neighbors
            for (int next : currentNode.branches.values()) {
                if (!parent.containsKey(next)) {
                    parent.put(next, current);
                    frontier.add(next);
                }
            }
        }

        // we should never get to this point (because we check for an empty goalsdict at the
        // beginning), but still necessary
        return null;
    }

    /**
     * Returns the direction the user should turn in, or null if we are done.
     */
    public Direction fork_part2(int choice, String newDesc, Direction forceTurn) {
        MapNode node;
        int node_id;
        if (newDesc != null) {
            // create a new node
            node_id = map.nodes.size();
            node = new MapNode(abs_branches, newDesc);
            map.nodes.add(node);
        } else {
            node = map.nodes.get(choice);
            node_id = choice;
        }

        node.branches.put(behind, last_node_id);
        map.nodes.get(last_node_id).branches.put(currently_facing, node_id);

        // Now the node and id are saved in the node and node_id vars
        // We now need to decide which direction the user should be directed in
        // We accomplish this by doing BFS until an unexplored node is found
        HashMap<Integer, Integer> parent = new HashMap<>();
        HashMap<Integer, Direction> parentDir = new HashMap<>();

        Queue<Integer> frontier = new ArrayDeque<>();
        parent.put(node_id, -1);

        frontier.add(node_id);
        boolean found_unexplored = false;

        // we don't have to do BFS if there are no unknown nodes left, so check for that
        goals_remaining = 0;
        for (MapNode n : map.nodes) {
            for (int i : n.branches.values()) {
                if (i < 0) {
                    goals_remaining += 1;
                }
            }
        }

        if (goals_remaining == 0) {
            frontier.clear();
        }

        System.out.println("bfs");
        while (!frontier.isEmpty()) {
            int current = frontier.remove();
            System.out.println("current: " + current);
            MapNode current_node = map.nodes.get(current);
            int[] branches = new int[current_node.branches.size()];
            int i = 0;
            for (Direction d : current_node.branches.keySet()) {
                branches[i] = (d.sub(currently_facing).add(Direction.right)).ordinal();
                i++;
            }
            Arrays.sort(branches);

            for (i = branches.length - 1; i >= 0; i--) {
                Direction d = Direction.values()[branches[i]].sub(Direction.right).add(currently_facing);
            }
            for (i = branches.length - 1; i >= 0; i--) {
                Direction d = Direction.values()[branches[i]].sub(Direction.right).add(currently_facing);
                int next_id = current_node.branches.get(d);
                if (next_id == -1 || !parent.containsKey(next_id)) {
                    System.out.println("" + d + ": " + next_id);
                    parent.put(next_id, current);
                    parentDir.put(next_id, d);

                    if (next_id == -1) {
                        found_unexplored = true;
                        if (current == node_id && d.sub(currently_facing) == forceTurn) {
                            break;
                        }
                    }
                    frontier.add(next_id);
                }
            }

            if (found_unexplored) {
                break;
            }
        }

        long currentTime = System.currentTimeMillis();

        LogEntry lastNode = map.log.get(map.log.size() - 1);
        if (lastNode.time_exit < 0) {
            lastNode.time_exit = currentTime;
        }

        if (!found_unexplored) {
            // do BFS again, but this time find reverse edges
            Direction d = goTowardsNewEdge(node_id);
            if (d != null) {
                map.log.add(new LogEntry(d, currentTime, -1, node_id));
                return d.sub(currently_facing);
            }

            // if we are completely done, add this dummy log entry
            map.log.add(new LogEntry(Direction.forward, currentTime, -1, node_id));
            return null;
        } else {
            int current = -1;
            Direction d = null;
            while (current != node_id) {
                d = parentDir.get(current);
                current = parent.get(current);
            }
            map.log.add(new LogEntry(d, currentTime, -1, node_id));
            return d.sub(currently_facing);
        }
    }

    /**
     * Returns the absolute direction the user was last instructed to turn in
     */
    public Direction absolute_dir() {
        return map.log.get(map.log.size() - 1).afterturn;
    }

    public IntString currentNode() {
        int new_id = map.log.get(map.log.size() - 1).node_id;
        return new IntString(new_id, map.nodes.get(new_id).description);
    }

    /**
     * Returns the next set of directions the user should see, if they follow the last instruction.
     * Returns null if we don't know where the user will be.
     * The returned set will never contain Directoin.backward.
     */
    public Set<Direction> whatWeSee() {
        Direction abs_dir = absolute_dir();

        int current_id = map.log.get(map.log.size() - 1).node_id;
        Integer next_id = map.nodes.get(current_id).branches.get(abs_dir);
        if (next_id == null || next_id < 0) {
            return null;
        }

        EnumSet<Direction> output = EnumSet.noneOf(Direction.class);
        for (Direction d : map.nodes.get(next_id).branches.keySet()) {
            Direction relative = d.sub(abs_dir);
            if (relative != Direction.backward) {
                output.add(relative);
            }
        }
        return output;
    }


    private int goals_remaining;
    /**
     * Returns the number of goals remaining
     */
    public int goals_remaining() {
        return goals_remaining;
    }

    /** Undoes the last action and returns the node_id, desc of the updated most recent position */
    public void undo() {
        int last_node_id = map.log.get(map.log.size() - 1).node_id;
        MapNode last_node = map.nodes.get(last_node_id);
        if (last_node_id == map.nodes.size() - 1) {
            map.nodes.remove(last_node_id);
        }
        map.log.remove(map.log.size() - 1);
    }
}
