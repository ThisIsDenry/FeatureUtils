package kaptainwutax.featureutils.structure;

import kaptainwutax.biomeutils.biome.Biome;
import kaptainwutax.biomeutils.biome.Biomes;
import kaptainwutax.biomeutils.source.BiomeSource;
import kaptainwutax.mcutils.rand.ChunkRand;
import kaptainwutax.mcutils.state.Dimension;
import kaptainwutax.mcutils.util.block.BlockRotation;
import kaptainwutax.mcutils.version.MCVersion;
import kaptainwutax.mcutils.version.VersionMap;
import kaptainwutax.terrainutils.TerrainGenerator;

public class Mansion extends TriangularStructure<Mansion> {

	public static final VersionMap<RegionStructure.Config> CONFIGS = new VersionMap<RegionStructure.Config>()
		.add(MCVersion.v1_11, new RegionStructure.Config(80, 20, 10387319));

	public Mansion(MCVersion version) {
		this(CONFIGS.getAsOf(version), version);
	}

	public Mansion(RegionStructure.Config config, MCVersion version) {
		super(config, version);
	}

	public static String name() {
		return "mansion";
	}

	@Override
	public boolean canSpawn(int chunkX, int chunkZ, BiomeSource source) {
		if(!super.canSpawn(chunkX, chunkZ, source)) return false;
		return source.iterateUniqueBiomes((chunkX << 4) + 9, (chunkZ << 4) + 9, 32, this::isValidBiome);
	}

	@Override
	public Dimension getValidDimension() {
		return Dimension.OVERWORLD;
	}

	// only checked for 1.16.1
	public static int getAverageYPosition(TerrainGenerator generator, int chunkX, int chunkZ) {
		ChunkRand rand = new ChunkRand();
		rand.setCarverSeed(generator.getWorldSeed(), chunkX, chunkZ, MCVersion.v1_16_1);
		BlockRotation rotation = BlockRotation.getRandom(rand);
		int xOffset = 5;
		int zOffset = 5;
		if(rotation == BlockRotation.CLOCKWISE_90) {
			xOffset = -5;
		} else if(rotation == BlockRotation.CLOCKWISE_180) {
			xOffset = -5;
			zOffset = -5;
		} else if(rotation == BlockRotation.COUNTERCLOCKWISE_90) {
			zOffset = -5;
		}

		int posX = (chunkX << 4) + 7;
		int posZ = (chunkZ << 4) + 7;
		int center = generator.getHeightInGround(posX, posZ);
		int s = generator.getHeightInGround(posX, posZ + zOffset); // SOUTH
		int e = generator.getHeightInGround(posX + xOffset, posZ); //  EAST
		int se = generator.getHeightInGround(posX + xOffset, posZ + zOffset); // SOUTH EAST
		return Math.min(Math.min(center, s), Math.min(e, se));
	}

	@Override
	public boolean isValidBiome(Biome biome) {
		return biome == Biomes.DARK_FOREST || biome == Biomes.DARK_FOREST_HILLS;
	}

}
