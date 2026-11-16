package de.moinmorni.wintersport.world;

import de.moinmorni.wintersport.course.Places;
import de.moinmorni.wintersport.course.Terrain;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

/** Leere Welt (nur Himmel) mit Schnee-Biom: kalte Farben, weißer Dunst. Das Bergdorf baut das Plugin selbst. */
public final class VoidGenerator extends ChunkGenerator {

    private static final BiomeProvider SNOWY = new BiomeProvider() {
        @Override
        public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
            return Biome.SNOWY_PLAINS;
        }

        @Override
        public List<Biome> getBiomes(WorldInfo worldInfo) {
            return List.of(Biome.SNOWY_PLAINS);
        }
    };

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return SNOWY;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, Places.SPAWN[0], Terrain.FLOOR_Y + Places.SPAWN[1], Places.SPAWN[2], Places.SPAWN_YAW, 0f);
    }
}
