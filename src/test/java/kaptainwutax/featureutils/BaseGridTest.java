package kaptainwutax.featureutils;

import static org.junit.jupiter.api.Assertions.*;

import kaptainwutax.biomeutils.source.BiomeSource;
import kaptainwutax.featureutils.structure.Mansion;
import kaptainwutax.featureutils.structure.RuinedPortal;
import kaptainwutax.featureutils.structure.generator.Generator;
import kaptainwutax.featureutils.structure.generator.Generators;
import kaptainwutax.featureutils.structure.generator.piece.MansionPiece;
import kaptainwutax.featureutils.structure.generator.structure.MansionGenerator;
import kaptainwutax.featureutils.structure.generator.structure.RuinedPortalGenerator;
import kaptainwutax.mcutils.rand.ChunkRand;
import kaptainwutax.mcutils.state.Dimension;
import kaptainwutax.mcutils.util.block.BlockBox;
import kaptainwutax.mcutils.util.block.BlockRotation;
import kaptainwutax.mcutils.util.math.DistanceMetric;
import kaptainwutax.mcutils.util.math.Vec3i;
import kaptainwutax.mcutils.util.pos.BPos;
import kaptainwutax.mcutils.util.pos.CPos;
import kaptainwutax.mcutils.util.pos.RPos;
import kaptainwutax.mcutils.version.MCVersion;
import kaptainwutax.terrainutils.TerrainGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class BaseGridTest {

	@Test
	public void findFakePortalRooms() {
		MCVersion version = MCVersion.v1_16_1;
		ChunkRand chunkRand = new ChunkRand();
		Mansion mansion = new Mansion(version);
		Generator.GeneratorFactory<?> generatorFactory = Generators.get(mansion.getClass());
		MansionGenerator mansionGenerator = (MansionGenerator) generatorFactory.create(version);

		int foundSeeds = 0;
		for(long structureSeed = 0; structureSeed < 1000L; structureSeed++) {
			CPos cPos = mansion.getInRegion(structureSeed, 0, 0, chunkRand);
			for (long biomeSeed = 0; biomeSeed < 200; biomeSeed++) {
				long worldSeed = biomeSeed<<48|structureSeed;
				BiomeSource source = BiomeSource.of(Dimension.OVERWORLD, version, worldSeed);
				TerrainGenerator terrainGenerator = TerrainGenerator.of(Dimension.OVERWORLD, source);
				if (!mansion.canSpawn(cPos, source)) continue;
				if (!mansion.canGenerate(cPos, terrainGenerator)) continue;
				mansionGenerator.generate(terrainGenerator, cPos, chunkRand);
				Map<BPos, String> roomPos = new HashMap<>();
				for(MansionPiece piece : mansionGenerator.getPieces()) {
					if (piece.getFloorNumber() == 0 && piece.getTemplate().equals("2x2_s1")) {
						roomPos.put(piece.getPos(), piece.getTemplate());
						foundSeeds++;
					} else if (piece.getTemplate().equals("1x1_a4")) {
						roomPos.put(piece.getPos(), piece.getTemplate());
					}
				}
				if (foundSeeds >= 1) {
					for (Map.Entry<BPos, String> entry : roomPos.entrySet()) {
						System.out.println(MansionGenerator.COMMON_NAMES.get(entry.getValue()) + ": " + entry.getKey().toString());
					}
					System.out.println(worldSeed);
				}
				mansionGenerator.reset();
				break;
			}

			if (foundSeeds >= 1) break;
		}
	}


	public final BlockBox BOTTOM_OBI = new BlockBox(4, 1, 4, 10, 1, 10);
	public final BlockBox TOP_OBI = new BlockBox(4, 7, 4, 10, 7, 10);

	@Test
	public void findNaturalPortal() {
		MCVersion version = MCVersion.v1_16_1;
		ChunkRand chunkRand = new ChunkRand();
		Mansion mansion = new Mansion(version);
		RuinedPortal ruinedPortal = new RuinedPortal(Dimension.OVERWORLD,version);
		Generator.GeneratorFactory<?> generatorFactory = Generators.get(mansion.getClass());
		MansionGenerator mansionGenerator = (MansionGenerator) generatorFactory.create(version);
		generatorFactory = Generators.get(ruinedPortal.getClass());
		RuinedPortalGenerator ruinedPortalGenerator = (RuinedPortalGenerator) generatorFactory.create(version);

		for(long structureSeed = 0; structureSeed < 1L << 48; structureSeed++) {
			// Rough checking
			CPos mansionCPos = mansion.getInRegion(structureSeed, 0, 0, chunkRand);
			BPos mansionBPos = mansionCPos.toBlockPos();
			BPos mansionCorner = mansionBPos.add(5 << 4, 0, 5 << 4);
			RPos rpRegion = mansionCPos.toRegionPos(40);
			CPos portalCPos = ruinedPortal.getInRegion(structureSeed, rpRegion.getX(), rpRegion.getZ(), chunkRand);
			BPos portalBPos = portalCPos.toBlockPos();
			chunkRand.setCarverSeed(structureSeed, mansionBPos.getX(), mansionBPos.getZ(), version);
			BlockRotation rotation = BlockRotation.getRandom(chunkRand);
			mansionCorner = rotation.rotate(mansionCorner, mansionBPos);
			BlockBox mansionBB = new BlockBox(mansionBPos, mansionCorner);
			if (!mansionBB.contains(portalBPos)) continue;

			int foundSeeds = 0;
			for (long biomeSeed = 0; biomeSeed < 200; biomeSeed++) {
				long worldSeed = biomeSeed<<48|structureSeed;
				BiomeSource source = BiomeSource.of(Dimension.OVERWORLD, version, worldSeed);
				TerrainGenerator terrainGenerator = TerrainGenerator.of(Dimension.OVERWORLD, source);
				if (!ruinedPortal.canSpawn(portalCPos, source)) continue;
				if (!ruinedPortal.canGenerate(portalCPos, terrainGenerator)) continue;
				if (!mansion.canSpawn(mansionCPos, source)) continue;
				if (!mansion.canGenerate(mansionCPos, terrainGenerator)) continue;
				System.out.println("Both structures spawn: " + worldSeed);

				ruinedPortalGenerator.generate(terrainGenerator, portalCPos, chunkRand);
				mansionGenerator.generate(terrainGenerator, mansionCPos, chunkRand);

				Map<BPos, String> roomPos = new HashMap<>();
				for(MansionPiece piece : mansionGenerator.getPieces()) {
					if (piece.getFloorNumber() == 0 && piece.getTemplate().equals("2x2_s1")) {
						roomPos.put(piece.getPos(), piece.getTemplate());
						System.out.println(worldSeed);
						foundSeeds++;
					}
				}

				if (foundSeeds >= 1) {
					System.out.println("Found lava room: " + worldSeed);
					for (Map.Entry<BPos, String> room : roomPos.entrySet()) {
						Vec3i portalPos = new Vec3i(room.getKey().getX(), room.getKey().getY(), room.getKey().getZ());
						if (ruinedPortalGenerator.getPos().distanceTo(portalPos, DistanceMetric.EUCLIDEAN) < 100.0) {
							System.out.println(worldSeed);
						}
					}
				}

				mansionGenerator.reset();
				break;
			}
			if (foundSeeds >= 1) break;
		}
	}
}
