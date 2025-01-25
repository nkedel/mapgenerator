package us.n8l.mapgenerator;

public enum TurnType {
    LEFT_90("Left 90°"),
    LEFT_45_AHEAD("Left 45° ahead"),
    LEFT_135("Left 135°"),
    RIGHT_90("Right 90°"),
    RIGHT_45_AHEAD("Right 45° ahead"),
    RIGHT_135("Right 135°");

    private final String description;

    TurnType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
