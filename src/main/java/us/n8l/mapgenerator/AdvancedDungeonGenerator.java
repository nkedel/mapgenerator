package us.n8l.mapgenerator;

import java.util.Random;

public class AdvancedDungeonGenerator {

    private static final int MAX_ROOMS = 10; // maximum rooms to create
    private static final Random RNG = new Random();

    public Dungeon generateDungeon() {
        Dungeon dungeon = new Dungeon();

        // Starter room
        Room startRoom = new Room(RoomShape.STARTER, "20' x 20'");
        dungeon.addRoom(startRoom);

        // Expand from the starter room
        expandPassage(dungeon, startRoom, 0);

        return dungeon;
    }

    private void expandPassage(Dungeon dungeon, Room fromRoom, int depth) {
        if (dungeon.getRooms().size() >= MAX_ROOMS || depth > MAX_ROOMS * 2) {
            return; // safety check
        }
        TableIResult result = rollTableI();
        switch (result) {
            case CONTINUE_STRAIGHT -> {
                createLinearCorridor(dungeon, fromRoom, 60, "Continue straight");
                Room newRoom = getLastRoom(dungeon);
                expandPassage(dungeon, newRoom, depth + 1);
            }
            case DOOR -> {
                DoorResult doorRes = rollTableII();
                handleDoor(dungeon, fromRoom, doorRes, depth);
            }
            case SIDE_PASSAGE -> {
                SidePassageResult sideRes = rollTableIII();
                handleSidePassage(dungeon, fromRoom, sideRes, depth);
            }
            case PASSAGE_TURNS -> {
                TurnType turnType = rollTableIV();
                int width = rollPassageWidth();
                String desc = width + " ft wide, " + turnType.getDescription();
                createLinearCorridor(dungeon, fromRoom, 60, desc);
                Room newRoom = getLastRoom(dungeon);
                expandPassage(dungeon, newRoom, depth + 1);
            }
            case CHAMBER -> {
                Room newRoom = createRandomChamber();
                dungeon.addRoom(newRoom);
                Corridor corridor = new Corridor(fromRoom, newRoom, 30, "To Chamber");
                dungeon.addCorridor(corridor);
                expandPassage(dungeon, newRoom, depth + 1);
            }
            case STAIRS -> {
                StairsType stairsType = rollTableVIII();
                handleStairs(dungeon, fromRoom, stairsType, depth);
            }
            case DEAD_END -> {
                Corridor corridor = new Corridor(fromRoom, null, 10, "Dead end here");
                dungeon.addCorridor(corridor);
            }
            case TRICK_TRAP -> {
                Corridor trapCorridor = new Corridor(fromRoom, null, 30, "Trap in passage - continues");
                dungeon.addCorridor(trapCorridor);
                expandPassageTrapContinuation(dungeon, trapCorridor, depth + 1);
            }
            case WANDERING_MONSTER -> {
                Corridor corridor = new Corridor(fromRoom, null, 10, "Wandering monster encountered");
                dungeon.addCorridor(corridor);
                // Check Table I again from the same spot
                expandPassage(dungeon, fromRoom, depth + 1);
            }
        }
    }

    // ~~~~~~~~~~~ TABLE I ~~~~~~~~~~~
    private TableIResult rollTableI() {
        int roll = RNG.nextInt(20) + 1;
        if (roll <= 2) {
            return TableIResult.CONTINUE_STRAIGHT;
        } else if (roll <= 5) {
            return TableIResult.DOOR;
        } else if (roll <= 10) {
            return TableIResult.SIDE_PASSAGE;
        } else if (roll <= 13) {
            return TableIResult.PASSAGE_TURNS;
        } else if (roll <= 16) {
            return TableIResult.CHAMBER;
        } else if (roll == 17) {
            return TableIResult.STAIRS;
        } else if (roll == 18) {
            return TableIResult.DEAD_END;
        } else if (roll == 19) {
            return TableIResult.TRICK_TRAP;
        } else {
            return TableIResult.WANDERING_MONSTER;
        }
    }

    enum TableIResult {
        CONTINUE_STRAIGHT,
        DOOR,
        SIDE_PASSAGE,
        PASSAGE_TURNS,
        CHAMBER,
        STAIRS,
        DEAD_END,
        TRICK_TRAP,
        WANDERING_MONSTER
    }

    // ~~~~~~~~~~~ TABLE II ~~~~~~~~~~~
    private DoorResult rollTableII() {
        int locRoll = RNG.nextInt(20) + 1;
        DoorLocation location;
        if (locRoll <= 6) {
            location = DoorLocation.LEFT;
        } else if (locRoll <= 12) {
            location = DoorLocation.RIGHT;
        } else {
            location = DoorLocation.AHEAD;
        }

        int spaceRoll = RNG.nextInt(20) + 1;
        DoorBeyond space;
        if (spaceRoll <= 4) {
            space = DoorBeyond.PARALLEL_OR_SMALL_ROOM;
        } else if (spaceRoll <= 8) {
            space = DoorBeyond.PASSAGE_STRAIGHT;
        } else if (spaceRoll <= 10) {
            space = DoorBeyond.PASSAGE_45_OR_135;
        } else if (spaceRoll <= 18) {
            space = DoorBeyond.ROOM_TABLE_V;
        } else {
            space = DoorBeyond.CHAMBER_TABLE_V;
        }

        return new DoorResult(location, space);
    }

    record DoorResult(DoorLocation location, DoorBeyond space){}
    enum DoorLocation { LEFT, RIGHT, AHEAD }
    enum DoorBeyond {
        PARALLEL_OR_SMALL_ROOM,
        PASSAGE_STRAIGHT,
        PASSAGE_45_OR_135,
        ROOM_TABLE_V,
        CHAMBER_TABLE_V
    }

    private void handleDoor(Dungeon dungeon, Room fromRoom, DoorResult doorRes, int depth) {
        String doorDesc = "Door at " + doorRes.location();
        switch (doorRes.space()) {
            case PARALLEL_OR_SMALL_ROOM -> {
                if (RNG.nextBoolean()) {
                    // Parallel passage
                    createLinearCorridor(dungeon, fromRoom, 30, doorDesc + " -> parallel passage");
                    Room end = getLastRoom(dungeon);
                    expandPassage(dungeon, end, depth + 1);
                } else {
                    // 10'x10' Room
                    Room newRoom = new Room(RoomShape.SQUARE, "10' x 10'");
                    dungeon.addRoom(newRoom);
                    Corridor c = new Corridor(fromRoom, newRoom, 5, doorDesc + " -> small 10x10 room");
                    dungeon.addCorridor(c);
                    expandPassage(dungeon, newRoom, depth + 1);
                }
            }
            case PASSAGE_STRAIGHT -> {
                createLinearCorridor(dungeon, fromRoom, 30, doorDesc + " -> passage straight");
                Room end = getLastRoom(dungeon);
                expandPassage(dungeon, end, depth + 1);
            }
            case PASSAGE_45_OR_135 -> {
                String angle = RNG.nextBoolean() ? "45°" : "135°";
                createLinearCorridor(dungeon, fromRoom, 30, doorDesc + " -> angled " + angle + " passage");
                Room end = getLastRoom(dungeon);
                expandPassage(dungeon, end, depth + 1);
            }
            case ROOM_TABLE_V -> {
                Room newRoom = createRandomChamber();
                dungeon.addRoom(newRoom);
                Corridor c = new Corridor(fromRoom, newRoom, 10, doorDesc + " -> Room (Table V)");
                dungeon.addCorridor(c);
                expandPassage(dungeon, newRoom, depth + 1);
            }
            case CHAMBER_TABLE_V -> {
                Room newRoom = createRandomChamber();
                dungeon.addRoom(newRoom);
                Corridor c = new Corridor(fromRoom, newRoom, 10, doorDesc + " -> Chamber (Table V)");
                dungeon.addCorridor(c);
                expandPassage(dungeon, newRoom, depth + 1);
            }
        }
    }

    // ~~~~~~~~~~~ TABLE III ~~~~~~~~~~~
    private SidePassageResult rollTableIII() {
        int dirRoll = RNG.nextInt(20) + 1;
        SidePassageDirection direction;
        if (dirRoll <= 2) {
            direction = SidePassageDirection.LEFT_90;
        } else if (dirRoll <= 4) {
            direction = SidePassageDirection.RIGHT_90;
        } else if (dirRoll == 5) {
            direction = SidePassageDirection.LEFT_45;
        } else if (dirRoll == 6) {
            direction = SidePassageDirection.RIGHT_45;
        } else if (dirRoll == 7) {
            direction = SidePassageDirection.LEFT_135;
        } else if (dirRoll == 8) {
            direction = SidePassageDirection.RIGHT_135;
        } else if (dirRoll == 9) {
            direction = SidePassageDirection.LEFT_CURVE_45;
        } else if (dirRoll == 10) {
            direction = SidePassageDirection.RIGHT_CURVE_45;
        } else if (dirRoll <= 13) {
            direction = SidePassageDirection.T_INTERSECTION;
        } else if (dirRoll <= 15) {
            direction = SidePassageDirection.Y_INTERSECTION;
        } else if (dirRoll <= 19) {
            direction = SidePassageDirection.FOUR_WAY;
        } else {
            direction = SidePassageDirection.X_INTERSECTION;
        }

        int width = rollPassageWidth();
        return new SidePassageResult(direction, width);
    }

    enum SidePassageDirection {
        LEFT_90, RIGHT_90,
        LEFT_45, RIGHT_45,
        LEFT_135, RIGHT_135,
        LEFT_CURVE_45, RIGHT_CURVE_45,
        T_INTERSECTION, Y_INTERSECTION, FOUR_WAY, X_INTERSECTION
    }
    record SidePassageResult(SidePassageDirection direction, int width){}

    private void handleSidePassage(Dungeon dungeon, Room fromRoom, SidePassageResult sideRes, int depth) {
        String desc = "Side passage " + sideRes.direction()
                      + ", " + sideRes.width() + " ft wide";
        createLinearCorridor(dungeon, fromRoom, 30, desc);
        Room corridorEnd = getLastRoom(dungeon);
        expandPassage(dungeon, corridorEnd, depth + 1);
    }

    private int rollPassageWidth() {
        int roll = RNG.nextInt(20) + 1;
        if (roll <= 4) {
            return 5;
        } else if (roll <= 13) {
            return 10;
        } else if (roll <= 17) {
            return 20;
        } else if (roll == 18) {
            return 30;
        } else {
            // 19-20 => "Special" (simplify to 40)
            return 40;
        }
    }

    // ~~~~~~~~~~~ TABLE IV ~~~~~~~~~~~
    private TurnType rollTableIV() {
        int roll = RNG.nextInt(20) + 1;
        if (roll <= 8) {
            return TurnType.LEFT_90;
        } else if (roll == 9) {
            return TurnType.LEFT_45_AHEAD;
        } else if (roll == 10) {
            return TurnType.LEFT_135;
        } else if (roll <= 18) {
            return TurnType.RIGHT_90;
        } else if (roll == 19) {
            return TurnType.RIGHT_45_AHEAD;
        } else {
            return TurnType.RIGHT_135;
        }
    }

    // ~~~~~~~~~~~ TABLE V ~~~~~~~~~~~
    private Room createRandomChamber() {
        int roll = RNG.nextInt(20) + 1;
        if (roll <= 4) {
            return new Room(RoomShape.SQUARE, "20' x 20'");
        } else if (roll <= 6) {
            return new Room(RoomShape.SQUARE, "30' x 30'");
        } else if (roll <= 8) {
            return new Room(RoomShape.SQUARE, "40' x 40'");
        } else if (roll <= 10) {
            return new Room(RoomShape.RECTANGULAR, "20' x 30'");
        } else if (roll <= 13) {
            return new Room(RoomShape.RECTANGULAR, "30' x 50'");
        } else if (roll <= 15) {
            return new Room(RoomShape.RECTANGULAR, "40' x 60'");
        } else if (roll <= 17) {
            return new Room(RoomShape.CIRCULAR, "30' diameter");
        } else {
            return new Room(RoomShape.UNUSUAL, "about 500+ sq. ft");
        }
    }

    // ~~~~~~~~~~~ TABLE VIII ~~~~~~~~~~~
    private StairsType rollTableVIII() {
        int roll = RNG.nextInt(20) + 1;
        return switch (roll) {
            case 1,2,3,4,5  -> StairsType.DOWN_1;
            case 6          -> StairsType.DOWN_2;
            case 7          -> StairsType.DOWN_3;
            case 8          -> StairsType.UP_1;
            case 9          -> StairsType.UP_TO_DEAD_END;
            case 10         -> StairsType.DOWN_TO_DEAD_END;
            case 11         -> StairsType.CHIMNEY_UP_1;
            case 12         -> StairsType.CHIMNEY_UP_2;
            case 13         -> StairsType.CHIMNEY_DOWN_2;
            case 14,15,16   -> StairsType.TRAP_DOOR_DOWN_1;
            case 17         -> StairsType.TRAP_DOOR_DOWN_2;
            default         -> StairsType.UP_1_DOWN_2_CHAMBER;
        };
    }

    private void handleStairs(Dungeon dungeon, Room fromRoom, StairsType stairsType, int depth) {
        createLinearCorridor(dungeon, fromRoom, 20, "Stairs: " + stairsType.getDescription());
        Room newNode = getLastRoom(dungeon);

        // If it ends in a chamber:
        if (stairsType == StairsType.UP_1_DOWN_2_CHAMBER) {
            Room chamber = createRandomChamber();
            dungeon.addRoom(chamber);
            Corridor c = new Corridor(newNode, chamber, 10, "End of stairs -> Chamber");
            dungeon.addCorridor(c);
            expandPassage(dungeon, chamber, depth + 1);
        } else {
            // Otherwise, passage continues
            expandPassage(dungeon, newNode, depth + 1);
        }
    }

    // ~~~~~~~~~~~ Additional Helpers ~~~~~~~~~~~

    private void expandPassageTrapContinuation(Dungeon dungeon, Corridor trapCorridor, int depth) {
        if (dungeon.getRooms().size() >= MAX_ROOMS) return;

        // Create a "mini node" at corridor’s end
        Room trapEnd = new Room(RoomShape.CORRIDOR_END, "End after trap");
        dungeon.addRoom(trapEnd);

        // Connect them
        Corridor connection = new Corridor(null, trapEnd, 0, "Trap corridor ends here");
        dungeon.addCorridor(connection);

        expandPassage(dungeon, trapEnd, depth);
    }

    private void createLinearCorridor(Dungeon dungeon, Room fromRoom, int lengthFeet, String description) {
        Room corridorEnd = new Room(RoomShape.CORRIDOR_END, "N/A");
        dungeon.addRoom(corridorEnd);

        Corridor corridor = new Corridor(fromRoom, corridorEnd, lengthFeet, description);
        dungeon.addCorridor(corridor);
    }

    private Room getLastRoom(Dungeon dungeon) {
        int size = dungeon.getRooms().size();
        if (size == 0) return null;
        return dungeon.getRooms().get(size - 1);
    }
}
