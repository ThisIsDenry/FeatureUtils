package kaptainwutax.featureutils;

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

import java.util.HashMap;
import java.util.Map;

public class MansionPerformanceTest {

	@Test
	public void performanceTest() {
		MCVersion version = MCVersion.v1_16_1;
		ChunkRand chunkRand = new ChunkRand();
		Mansion mansion = new Mansion(version);
		Generator.GeneratorFactory<?> generatorFactory = Generators.get(mansion.getClass());
		MansionGenerator mansionGenerator = (MansionGenerator) generatorFactory.create(version);

		long timeStart = System.currentTimeMillis();
		for(long worldSeed = 0; worldSeed < 100000L; worldSeed++) {
			CPos cPos = new CPos(0,0);
			BiomeSource source = BiomeSource.of(Dimension.OVERWORLD, version, worldSeed);
			TerrainGenerator terrainGenerator = TerrainGenerator.of(Dimension.OVERWORLD, source);
			mansionGenerator.generate(terrainGenerator, cPos, chunkRand);
		}
		long timeEnd = System.currentTimeMillis();
		System.out.println(timeEnd - timeStart);
	}
}
