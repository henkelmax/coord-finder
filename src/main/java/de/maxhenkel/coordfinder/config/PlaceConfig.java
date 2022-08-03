package de.maxhenkel.coordfinder.config;

import de.maxhenkel.configbuilder.PropertyConfig;
import de.maxhenkel.coordfinder.Location;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class PlaceConfig extends PropertyConfig {

    public static final Pattern PLACE_NAME_REGEX = Pattern.compile("^[a-zA-Z_-]{1,32}$");

    private final Map<String, Location> places;

    public PlaceConfig(Path path) {
        super(path);
        Map<String, Object> entries = getEntries();
        places = new HashMap<>();
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            Location location = Location.fromString(entry.getValue().toString());
            places.put(entry.getKey(), location);
        }
    }

    @Nullable
    public Location getPlace(String name) {
        return places.get(name);
    }

    public void setPlace(String name, Location location) {
        places.put(name, location);
        set(name, location.toString());
        save();
    }

    public void removePlace(String name) {
        places.remove(name);
        properties.remove(name);
        save();
    }

    public Map<String, Location> getPlaces() {
        return places;
    }

    public static boolean isValidPlaceName(String name) {
        return PLACE_NAME_REGEX.matcher(name).matches();
    }

}

