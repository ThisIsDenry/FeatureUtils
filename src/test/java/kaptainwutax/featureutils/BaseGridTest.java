package kaptainwutax.featureutils;

import static org.junit.jupiter.api.Assertions.*;

import kaptainwutax.biomeutils.source.BiomeSource;
import kaptainwutax.featureutils.structure.Mansion;
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
			for (long biomeSeed = 0; biomeSeed < 1L << 16; biomeSeed++) {
				long worldSeed = biomeSeed<<48|structureSeed;
				BiomeSource source = BiomeSource.of(Dimension.OVERWORLD, version, worldSeed);
				TerrainGenerator terrainGenerator = TerrainGenerator.of(Dimension.OVERWORLD, source);
				if (!mansion.canSpawn(cPos, source)) continue;
				if (!mansion.canGenerate(cPos, terrainGenerator)) continue;
				mansionGenerator.generate(terrainGenerator, cPos, chunkRand);
				System.out.println("Found valid world seed with structureSeed: " + structureSeed);
				for(MansionPiece piece :
					mansionGenerator.getPieces()) {
					if (piece.getTemplate().equals("1x2_s2")) {
						System.out.println(worldSeed);
						foundSeeds++;
					}
				}
				break;
			}

			if (foundSeeds >= 1) break;
		}
	}
}
