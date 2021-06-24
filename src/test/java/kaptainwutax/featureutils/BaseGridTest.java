package kaptainwutax.featureutils;

import static org.junit.jupiter.api.Assertions.*;

import kaptainwutax.biomeutils.source.BiomeSource;
import kaptainwutax.featureutils.structure.Mansion;
import kaptainwutax.featureutils.structure.RuinedPortal;
import kaptainwutax.featureutils.structure.generator.Generator;
import kaptainwutax.featureutils.structure.generator.Generators;
import kaptainwutax.featureutils.structure.generator.piece.MansionPiece;
import kaptainwutax.featureutils.structure.generator.structure.MansionGenerator;
import kaptainwutax.mcutils.rand.ChunkRand;
import kaptainwutax.mcutils.state.Dimension;
import kaptainwutax.mcutils.util.pos.BPos;
import kaptainwutax.mcutils.util.pos.CPos;
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

}
