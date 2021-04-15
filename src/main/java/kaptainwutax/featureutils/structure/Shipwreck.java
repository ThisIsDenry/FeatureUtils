package kaptainwutax.featureutils.structure;


import kaptainwutax.biomeutils.Biome;
import kaptainwutax.featureutils.loot.LootContext;
import kaptainwutax.featureutils.loot.MCLootTables;
import kaptainwutax.featureutils.loot.item.ItemStack;
import kaptainwutax.seedutils.mc.ChunkRand;
import kaptainwutax.seedutils.mc.Dimension;
import kaptainwutax.seedutils.mc.MCVersion;
import kaptainwutax.seedutils.mc.VersionMap;
import kaptainwutax.seedutils.mc.pos.BPos;
import kaptainwutax.seedutils.mc.pos.CPos;
import kaptainwutax.seedutils.mc.pos.RPos;
import kaptainwutax.seedutils.mc.util.BlockBox;
import kaptainwutax.seedutils.mc.util.Mirror;
import kaptainwutax.seedutils.mc.util.Rotation;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class Shipwreck extends UniformStructure<Shipwreck> {
    private ChunkRand random = null; // this is an internal one as it will be updated on a need to know basis
    private Boolean isBeached = null;
    private Rotation rotation = null;
    private String type = null;

    public static final VersionMap<RegionStructure.Config> CONFIGS = new VersionMap<RegionStructure.Config>()
            .add(MCVersion.v1_13, new RegionStructure.Config(15, 8, 165745295))
            .add(MCVersion.v1_13_1, new RegionStructure.Config(16, 8, 165745295))
            .add(MCVersion.v1_16, new RegionStructure.Config(24, 4, 165745295));

    public Shipwreck(MCVersion version) {
        this(CONFIGS.getAsOf(version), version);
    }

    public Shipwreck(RegionStructure.Config config, MCVersion version) {
        super(config, version);
    }

    /**
     * This is a dangerous utility, we provide it on a need to know basis (don't use it)
     *
     * @return
     */
    public ChunkRand getInternalRandom() {
        return random;
    }

    /**
     * Should be called after canspawn and getRotation
     *
     * @return the type of shipwreck (useful to determine loot order)
     */
    public String getType() {
        if (isBeached == null) return null;
        if (rotation == null) return null;
        if (type == null) {
            String[] arr = isBeached ? STRUCTURE_LOCATION_BEACHED : STRUCTURE_LOCATION_OCEAN;
            type = arr[random.nextInt(arr.length)];
        }
        return type;
    }

    /**
     * This should be called before any operation related to nbt
     *
     * @param structureSeed
     * @param chunkPos
     * @param version
     * @return
     */
    public Rotation getRotation(long structureSeed, CPos chunkPos, MCVersion version) {
        // first call does the seeding the rest doesn't
        if (rotation == null) {
            random = new ChunkRand();
            random.setCarverSeed(structureSeed, chunkPos.getX(), chunkPos.getZ(), version);
            rotation = Rotation.getRandom(random);
        }
        return rotation;
    }

    @Override
    public boolean canStart(Data<Shipwreck> data, long structureSeed, ChunkRand rand) {
        return super.canStart(data, structureSeed, rand);
    }

    @Override
    public boolean isValidDimension(Dimension dimension) {
        return dimension == Dimension.OVERWORLD;
    }

    /**
     * This will only work if you call canSpawn before
     *
     * @return
     */
    public Boolean isBeached() {
        return isBeached;
    }

    @Override
    public boolean isValidBiome(Biome biome) {
        isBeached = biome == Biome.BEACH || biome == Biome.SNOWY_BEACH;
        return biome.getCategory() == Biome.Category.OCEAN || isBeached;
    }

    /**
     * WARNING You need to call canSpawn before hand, else you will get a null
     * @param start the chunkposition of the shipwreck start (obtained with getInRegion
     * @param structureSeed the structure seed (lower 48 bits of the world seed)
     * @param rand a chunkrand instance (for speed purpose)
     * @return
     */
    public HashMap<LootType, List<ItemStack>> getLoot(CPos start, long structureSeed, ChunkRand rand) {
        if (isBeached==null){
            System.err.println("Please call canspawn before");
            return null;
        }
        RPos rPos = start.toRegionPos(this.getSpacing());
        CPos validation = this.getInRegion(structureSeed, rPos.getX(), rPos.getZ(), rand);
        if (!start.equals(validation)){
            System.err.println("Provided chunkpos "+start+" was wrong, correct was "+validation);
            return null;
        }
        Rotation rotation = this.getRotation(structureSeed, start, this.getVersion());
        String type=this.getType();
        if (!STRUCTURE_SIZE.containsKey(type) || !STRUCTURE_TO_LOOT.containsKey(type)){
            System.err.println("We don't support this type yet "+type);
            return null;
        }
        int salt = 40006; //TODO make me version dependant
//        System.out.println(rotation.name()+" "+isBeached+" "+type);
        BPos size=STRUCTURE_SIZE.get(type);
        HashMap<LootType,BPos> lootPos=STRUCTURE_TO_LOOT.get(type);
        BPos anchor = start.toBlockPos(90);
        BPos pivot = new BPos(4, 0, 15); // FIXED for shipwreck
        Mirror mirror = Mirror.NONE; // FIXED for shipwreck
        BlockBox blockBox = BlockBox.getBoundingBox(anchor, rotation, pivot, mirror, size);
        BlockBox rotated=blockBox.getRotated(rotation);
//        System.out.println(blockBox+" "+rotated);
        HashMap<LootType,List<ItemStack>> result=new HashMap<>();
        HashMap<CPos,Integer> seen=new HashMap<>();
        for (LootType lootType:lootPos.keySet()){
            BPos offset=lootPos.get(lootType);
            BPos chestPos=rotated.getInside(offset,rotation);
            CPos chunkChestPos=chestPos.toChunkPos();
            rand.setDecoratorSeed(structureSeed, chunkChestPos.getX() * 16, chunkChestPos.getZ() * 16, salt, this.getVersion());
            if (this.isBeached()) {
                rand.nextInt(3);
            }
            rand.advance(lootType==LootType.SUPPLY_CHEST?2:4);
            int previousCount=seen.getOrDefault(chunkChestPos,0);
            rand.advance(lootType==LootType.SUPPLY_CHEST?previousCount :previousCount* 2L);
            seen.put(chunkChestPos,previousCount+1);
//            System.out.println(lootType.name()+" pre seed: " + rand.getSeed());
            LootContext context = new LootContext(rand.nextLong());
            List<ItemStack> loot = MCLootTables.SHIPWRECK_TREASURE_CHEST.generate(context);
            result.put(lootType,loot);
        }
        return result;
    }

    public enum LootType {
        SUPPLY_CHEST,
        TREASURE_CHEST,
        MAP_CHEST;
    }

    private static final String[] STRUCTURE_LOCATION_BEACHED = new String[] {
            "with_mast",
            "sideways_full",
            "sideways_fronthalf",
            "sideways_backhalf",
            "rightsideup_full",
            "rightsideup_fronthalf",
            "rightsideup_backhalf",
            "with_mast_degraded",
            "rightsideup_full_degraded",
            "rightsideup_fronthalf_degraded",
            "rightsideup_backhalf_degraded"
    };
    private static final String[] STRUCTURE_LOCATION_OCEAN = new String[] {
            "with_mast",
            "upsidedown_full",
            "upsidedown_fronthalf",
            "upsidedown_backhalf",
            "sideways_full",
            "sideways_fronthalf",
            "sideways_backhalf",
            "rightsideup_full",
            "rightsideup_fronthalf",
            "rightsideup_backhalf",
            "with_mast_degraded",
            "upsidedown_full_degraded",
            "upsidedown_fronthalf_degraded",
            "upsidedown_backhalf_degraded",
            "sideways_full_degraded",
            "sideways_fronthalf_degraded",
            "sideways_backhalf_degraded",
            "rightsideup_full_degraded",
            "rightsideup_fronthalf_degraded",
            "rightsideup_backhalf_degraded"
    };


    private static final HashMap<String, LinkedHashMap<LootType, BPos>> STRUCTURE_TO_LOOT = new HashMap<>();
    private static final HashMap<String, BPos> STRUCTURE_SIZE = new HashMap<>();

    static {
        // we are y+1
        STRUCTURE_TO_LOOT.put("rightsideup_backhalf", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.MAP_CHEST, new BPos(5, 3, 6));
            put(LootType.TREASURE_CHEST, new BPos(6, 5, 12));
        }});
        STRUCTURE_SIZE.put("rightsideup_backhalf", new BPos(9, 9, 16));
        STRUCTURE_TO_LOOT.put("rightsideup_backhalf_degraded", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.MAP_CHEST, new BPos(5, 3, 6));
            put(LootType.TREASURE_CHEST, new BPos(6, 5, 12));
        }});
        STRUCTURE_SIZE.put("rightsideup_backhalf_degraded", new BPos(9, 9, 16));
        STRUCTURE_TO_LOOT.put("rightsideup_fronthalf", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.SUPPLY_CHEST, new BPos(4, 3, 8));
        }});
        STRUCTURE_SIZE.put("rightsideup_fronthalf", new BPos(9, 9, 24));
        STRUCTURE_TO_LOOT.put("rightsideup_fronthalf_degraded", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.SUPPLY_CHEST, new BPos(4, 3, 8));
        }});
        STRUCTURE_SIZE.put("rightsideup_fronthalf_degraded", new BPos(9, 9, 24));
        STRUCTURE_TO_LOOT.put("rightsideup_full", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.SUPPLY_CHEST, new BPos(4, 3, 8));
            put(LootType.MAP_CHEST, new BPos(5, 3, 18));
            put(LootType.TREASURE_CHEST, new BPos(6, 5, 24));
        }});
        STRUCTURE_SIZE.put("rightsideup_full", new BPos(9, 9, 28));
        STRUCTURE_TO_LOOT.put("rightsideup_full_degraded", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.SUPPLY_CHEST, new BPos(4, 3, 8));
            put(LootType.MAP_CHEST, new BPos(5, 3, 18));
            put(LootType.TREASURE_CHEST, new BPos(6, 5, 24));
        }});
        STRUCTURE_SIZE.put("rightsideup_full_degraded", new BPos(9, 9, 28));
        STRUCTURE_TO_LOOT.put("sideways_backhalf", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.TREASURE_CHEST, new BPos(3, 3, 13));
            put(LootType.MAP_CHEST, new BPos(6, 4, 8));
        }});
        STRUCTURE_SIZE.put("sideways_backhalf", new BPos(9, 9, 17));
        STRUCTURE_TO_LOOT.put("sideways_backhalf_degraded", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.TREASURE_CHEST, new BPos(3, 3, 13));
            put(LootType.MAP_CHEST, new BPos(6, 4, 8));
        }});
        STRUCTURE_SIZE.put("sideways_backhalf_degraded", new BPos(9, 9, 17));
        STRUCTURE_TO_LOOT.put("sideways_fronthalf", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.SUPPLY_CHEST, new BPos(5, 4, 8));
        }});
        STRUCTURE_SIZE.put("sideways_fronthalf", new BPos(9, 9, 24));
        STRUCTURE_TO_LOOT.put("sideways_fronthalf_degraded", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.SUPPLY_CHEST, new BPos(5, 4, 8));
        }});
        STRUCTURE_SIZE.put("sideways_fronthalf_degraded", new BPos(9, 9, 24));
        STRUCTURE_TO_LOOT.put("sideways_full", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.TREASURE_CHEST, new BPos(3, 3, 24));
            put(LootType.SUPPLY_CHEST, new BPos(5, 4, 8));
            put(LootType.MAP_CHEST, new BPos(6, 4, 19));
        }});
        STRUCTURE_SIZE.put("sideways_full", new BPos(9, 9, 28));
        STRUCTURE_TO_LOOT.put("sideways_full_degraded", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.TREASURE_CHEST, new BPos(3, 3, 24));
            put(LootType.SUPPLY_CHEST, new BPos(5, 4, 8));
            put(LootType.MAP_CHEST, new BPos(6, 4, 19));
        }});
        STRUCTURE_SIZE.put("sideways_full_degraded", new BPos(9, 9, 28));
        STRUCTURE_TO_LOOT.put("upsidedown_backhalf", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.TREASURE_CHEST, new BPos(2, 3, 12));
            put(LootType.MAP_CHEST, new BPos(3, 6, 5));
        }});
        STRUCTURE_SIZE.put("upsidedown_backhalf", new BPos(9, 9, 16));
        STRUCTURE_TO_LOOT.put("upsidedown_backhalf_degraded", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.TREASURE_CHEST, new BPos(2, 3, 12));
            put(LootType.MAP_CHEST, new BPos(3, 6, 5));
        }});
        STRUCTURE_SIZE.put("upsidedown_backhalf_degraded", new BPos(9, 9, 16));
        STRUCTURE_TO_LOOT.put("upsidedown_fronthalf", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.MAP_CHEST, new BPos(3, 6, 17));
            put(LootType.SUPPLY_CHEST, new BPos(4, 6, 8));
        }});
        STRUCTURE_SIZE.put("upsidedown_fronthalf", new BPos(9, 9, 22));
        STRUCTURE_TO_LOOT.put("upsidedown_fronthalf_degraded", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.MAP_CHEST, new BPos(3, 6, 17));
            put(LootType.SUPPLY_CHEST, new BPos(4, 6, 8));
        }});
        STRUCTURE_SIZE.put("upsidedown_fronthalf_degraded", new BPos(9, 9, 22));
        STRUCTURE_TO_LOOT.put("upsidedown_full", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.TREASURE_CHEST, new BPos(2, 3, 24));
            put(LootType.MAP_CHEST, new BPos(3, 6, 17));
            put(LootType.SUPPLY_CHEST, new BPos(4, 6, 8));
        }});
        STRUCTURE_SIZE.put("upsidedown_full", new BPos(9, 9, 28));
        STRUCTURE_TO_LOOT.put("upsidedown_full_degraded", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.TREASURE_CHEST, new BPos(2, 3, 24));
            put(LootType.MAP_CHEST, new BPos(3, 6, 17));
            put(LootType.SUPPLY_CHEST, new BPos(4, 6, 8));
        }});
        STRUCTURE_SIZE.put("upsidedown_full_degraded", new BPos(9, 9, 28));
        STRUCTURE_TO_LOOT.put("with_mast", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.SUPPLY_CHEST, new BPos(4, 3, 9));
            put(LootType.MAP_CHEST, new BPos(5, 3, 18));
            put(LootType.TREASURE_CHEST, new BPos(6, 5, 24));
        }});
        STRUCTURE_SIZE.put("with_mast", new BPos(9, 21, 28));
        STRUCTURE_TO_LOOT.put("with_mast_degraded", new LinkedHashMap<LootType, BPos>() {{
            put(LootType.SUPPLY_CHEST, new BPos(4, 3, 9));
            put(LootType.MAP_CHEST, new BPos(5, 3, 18));
            put(LootType.TREASURE_CHEST, new BPos(6, 5, 24));
        }});
        STRUCTURE_SIZE.put("with_mast_degraded", new BPos(9, 21, 28));
    }
//import nbtlib
//from pathlib import *
//import sys
//
//p = Path(r'.').glob('**/*')
//files = [x for x in p if x.is_file()]
//for file in files:
//    print(f'STRUCTURE_TO_LOOT.put("{file.rstrip(".nbt")}", new LinkedHashMap<LootType, BPos>() {{{{')
//    nbt_file=nbtlib.load(file)
//    root=nbt_file.root
//    if "blocks" not in root.keys():
//        print(f"Missing blocks key for {file}")
//        sys.exit(1)
//    for block in root["blocks"]:
//        if "nbt" in block.keys() and "pos" in block.keys():
//            pos=block["pos"]
//            nbt=block["nbt"]
//            if "metadata" in nbt:
//                print(f'    put(LootType.{nbt["metadata"].upper()},new BPos({",".join(map(str,map(int,pos)))}));')
//    print('}});')
//    if "size" in root.keys():
//        print(f'STRUCTURE_SIZE.put("{file}",new BPos({",".join(map(str,map(int,root["size"])))}));')
//    else:
//        print(f"Missing size key for {file.rstrip(".nbt")}")
//        sys.exit(1)
}