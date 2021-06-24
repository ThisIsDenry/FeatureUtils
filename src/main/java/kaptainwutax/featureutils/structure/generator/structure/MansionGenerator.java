package kaptainwutax.featureutils.structure.generator.structure;

import kaptainwutax.featureutils.structure.Mansion;
import kaptainwutax.featureutils.structure.generator.Generator;
import kaptainwutax.featureutils.structure.generator.piece.MansionPiece;
import kaptainwutax.mcutils.rand.ChunkRand;
import kaptainwutax.mcutils.util.block.BlockDirection;
import kaptainwutax.mcutils.util.block.BlockMirror;
import kaptainwutax.mcutils.util.block.BlockRotation;
import kaptainwutax.mcutils.util.data.Pair;
import kaptainwutax.mcutils.util.math.Vec3i;
import kaptainwutax.mcutils.util.pos.BPos;
import kaptainwutax.mcutils.version.MCVersion;
import kaptainwutax.terrainutils.TerrainGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class MansionGenerator extends Generator {

	private final List<MansionPiece> globalPieces;

	public MansionGenerator(MCVersion version) {
		super(version);
		globalPieces = new ArrayList<>();

	}

	public void reset() {
		this.globalPieces.clear();
	}

	public List<MansionPiece> getPieces() {
		return globalPieces;
	}

	public void printGlobalPieces() {
		for(MansionPiece mansionPiece : globalPieces) {
			System.out.println(mansionPiece.getTemplate());
		}
	}

	public void printGlobalCommonNames() {
		for(MansionPiece mansionPiece : globalPieces) {
			System.out.println(COMMON_NAMES.get(mansionPiece.getTemplate()));
		}
	}

	@Override
	public boolean generate(TerrainGenerator generator, int chunkX, int chunkZ, ChunkRand rand) {
		if (generator == null) return false;
		int y = Mansion.getAverageYPosition(generator, chunkX, chunkZ);
		if (y < 60) return false;
		rand.setCarverSeed(generator.getWorldSeed(), chunkX, chunkZ, this.getVersion());
		BlockRotation rotation = BlockRotation.getRandom(rand);
		BPos start = new BPos(chunkX * 16 + 8, y + 1, chunkZ * 16 + 8);
		return this.start(start, rotation, this.globalPieces, rand);
	}

	public boolean start(BPos start, BlockRotation rotation, List<MansionPiece> mansionPieces, ChunkRand chunkRand) {
		Random rand = new Random(chunkRand.getSeed() ^ 0x5DEECE66DL);
		Grid grid = new Grid(rand);
		Placer placer = new Placer(rand);
		placer.createMansion(start, rotation, mansionPieces, grid);
		return true;
	}

	@Override
	public List<Pair<ILootType, BPos>> getChestsPos() {
		return null;
	}

	@Override
	public List<Pair<ILootType, BPos>> getLootPos() {
		return null;
	}

	@Override
	public ILootType[] getLootTypes() {
		return new ILootType[0];
	}

	static class SimpleGrid {
		private final int[][] grid;
		private final int width;
		private final int height;
		private final int valueIfOutside;

		public SimpleGrid(int width, int height, int valueIfOutside) {
			this.width = width;
			this.height = height;
			this.valueIfOutside = valueIfOutside;
			this.grid = new int[width][height];
		}

		public void set(int x, int z, int value) {
			if (x >= 0 && x < this.width && z >= 0 && z < this.height) {
				this.grid[x][z] = value;
			}

		}

		public void set(int minX, int minZ, int maxX, int maxZ, int value) {
			for(int i = minZ; i <= maxZ; ++i) {
				for(int j = minX; j <= maxX; ++j) {
					this.set(j, i, value);
				}
			}

		}

		public int get(int x, int z) {
			return x >= 0 && x < this.width && z >= 0 && z < this.height ? this.grid[x][z] : this.valueIfOutside;
		}

		public void setif(int x, int z, int conditionValue, int newValue) {
			if (this.get(x, z) == conditionValue) {
				this.set(x, z, newValue);
			}

		}

		public boolean adjacentTo(int x, int z, int value) {
			return this.get(x - 1, z) == value || this.get(x + 1, z) == value || this.get(x, z + 1) == value || this.get(x, z - 1) == value;
		}
	}

	static class Grid {
		private final Random random;
		private final SimpleGrid baseGrid;
		private final SimpleGrid thirdFloorGrid;
		private final SimpleGrid[] floorGrids;
		private final int entranceX;
		private final int entranceZ;

		public Grid(Random random) {
			this.random = random;
			this.entranceX = 7;
			this.entranceZ = 4;
			this.baseGrid = new SimpleGrid(11, 11, BaseRoomFlag.OUTSIDE);
			this.baseGrid.set(this.entranceX, this.entranceZ, this.entranceX + 1, this.entranceZ + 1, BaseRoomFlag.START);
			this.baseGrid.set(this.entranceX - 1, this.entranceZ, this.entranceX - 1, this.entranceZ + 1, BaseRoomFlag.ROOM);
			this.baseGrid.set(this.entranceX + 2, this.entranceZ - 2, this.entranceX + 3, this.entranceZ + 3, BaseRoomFlag.OUTSIDE);
			this.baseGrid.set(this.entranceX + 1, this.entranceZ - 2, this.entranceX + 1, this.entranceZ - 1, BaseRoomFlag.CORRIDOR);
			this.baseGrid.set(this.entranceX + 1, this.entranceZ + 2, this.entranceX + 1, this.entranceZ + 3, BaseRoomFlag.CORRIDOR);
			this.baseGrid.set(this.entranceX - 1, this.entranceZ - 1, BaseRoomFlag.CORRIDOR);
			this.baseGrid.set(this.entranceX - 1, this.entranceZ + 2, BaseRoomFlag.CORRIDOR);
			this.baseGrid.set(0, 0, 11, 1, BaseRoomFlag.OUTSIDE);
			this.baseGrid.set(0, 9, 11, 11, BaseRoomFlag.OUTSIDE);
			this.recursiveCorridor(this.baseGrid, this.entranceX, this.entranceZ - 2, Direction.WEST, 6);
			this.recursiveCorridor(this.baseGrid, this.entranceX, this.entranceZ + 3, Direction.WEST, 6);
			this.recursiveCorridor(this.baseGrid, this.entranceX - 2, this.entranceZ - 1, Direction.WEST, BaseRoomFlag.START);
			this.recursiveCorridor(this.baseGrid, this.entranceX - 2, this.entranceZ + 2, Direction.WEST, BaseRoomFlag.START);

			while(this.cleanEdges(this.baseGrid)) {
			}

			this.floorGrids = new SimpleGrid[3];
			this.floorGrids[0] = new SimpleGrid(11, 11, BaseRoomFlag.OUTSIDE);
			this.floorGrids[1] = new SimpleGrid(11, 11, BaseRoomFlag.OUTSIDE);
			this.floorGrids[2] = new SimpleGrid(11, 11, BaseRoomFlag.OUTSIDE);
			this.identifyRooms(this.baseGrid, this.floorGrids[0]);
			this.identifyRooms(this.baseGrid, this.floorGrids[1]);
			this.floorGrids[0].set(this.entranceX + 1, this.entranceZ, this.entranceX + 1, this.entranceZ + 1, 8388608);
			this.floorGrids[1].set(this.entranceX + 1, this.entranceZ, this.entranceX + 1, this.entranceZ + 1, 8388608);
			this.thirdFloorGrid = new SimpleGrid(this.baseGrid.width, this.baseGrid.height, BaseRoomFlag.OUTSIDE);
			this.setupThirdFloor();
			this.identifyRooms(this.thirdFloorGrid, this.floorGrids[2]);
		}

		public static boolean isHouse(SimpleGrid baseGrid, int gridX, int gridZ) {
			int value = baseGrid.get(gridX, gridZ);
			return value == BaseRoomFlag.CORRIDOR || value == BaseRoomFlag.ROOM || value == BaseRoomFlag.START || value == 4;
		}

		public boolean isRoomId(SimpleGrid baseGrid, int newGridX, int newGridZ, int floorRoomIndex, int value) {
			return (this.floorGrids[floorRoomIndex].get(newGridX, newGridZ) & '\uffff') == value;
		}

		@Nullable
		public Direction get1x2RoomDirection(SimpleGrid baseGrid, int gridX, int gridZ, int floorRoomIndex, int value) {
			for(Direction direction : Direction.HORIZONTALS) {
				if (this.isRoomId(baseGrid, gridX + direction.getStepX(), gridZ + direction.getStepZ(), floorRoomIndex, value)) {
					return direction;
				}
			}

			return null;
		}

		private void recursiveCorridor(SimpleGrid baseGrid, int gridX, int gridZ, Direction direction, int genDepth) {
			if (genDepth > 0) {
				baseGrid.set(gridX, gridZ, BaseRoomFlag.CORRIDOR);
				baseGrid.setif(gridX + direction.getStepX(), gridZ + direction.getStepZ(), 0, BaseRoomFlag.CORRIDOR);

				for(int i = 0; i < 8; ++i) {
					Direction newDirection = Direction.from2DDataValue(this.random.nextInt(4));
					if (newDirection != direction.getOpposite() && (newDirection != Direction.EAST || !this.random.nextBoolean())) {
						int stepX = gridX + direction.getStepX();
						int stepZ = gridZ + direction.getStepZ();
						if (baseGrid.get(stepX + newDirection.getStepX(), stepZ + newDirection.getStepZ()) == 0 && baseGrid.get(stepX + newDirection.getStepX() * 2, stepZ + newDirection.getStepZ() * 2) == 0) {
							this.recursiveCorridor(baseGrid, gridX + direction.getStepX() + newDirection.getStepX(), gridZ + direction.getStepZ() + newDirection.getStepZ(), newDirection, genDepth - 1);
							break;
						}
					}
				}

				Direction cw = direction.getClockWise();
				Direction ccw = direction.getCounterClockWise();
				baseGrid.setif(gridX + cw.getStepX(), gridZ + cw.getStepZ(), 0, 2);
				baseGrid.setif(gridX + ccw.getStepX(), gridZ + ccw.getStepZ(), 0, 2);
				baseGrid.setif(gridX + direction.getStepX() + cw.getStepX(), gridZ + direction.getStepZ() + cw.getStepZ(), 0, 2);
				baseGrid.setif(gridX + direction.getStepX() + ccw.getStepX(), gridZ + direction.getStepZ() + ccw.getStepZ(), 0, 2);
				baseGrid.setif(gridX + direction.getStepX() * 2, gridZ + direction.getStepZ() * 2, 0, 2);
				baseGrid.setif(gridX + cw.getStepX() * 2, gridZ + cw.getStepZ() * 2, 0, 2);
				baseGrid.setif(gridX + ccw.getStepX() * 2, gridZ + ccw.getStepZ() * 2, 0, 2);
			}
		}

		private boolean cleanEdges(SimpleGrid baseGrid) {
			boolean flag = false;

			for(int gridZ = 0; gridZ < baseGrid.height; ++gridZ) {
				for(int gridX = 0; gridX < baseGrid.width; ++gridX) {
					if (baseGrid.get(gridX, gridZ) == 0) {
						int adjacentRooms = 0;
						adjacentRooms = adjacentRooms + (isHouse(baseGrid, gridX + 1, gridZ) ? 1 : 0);
						adjacentRooms = adjacentRooms + (isHouse(baseGrid, gridX - 1, gridZ) ? 1 : 0);
						adjacentRooms = adjacentRooms + (isHouse(baseGrid, gridX, gridZ + 1) ? 1 : 0);
						adjacentRooms = adjacentRooms + (isHouse(baseGrid, gridX, gridZ - 1) ? 1 : 0);
						if (adjacentRooms >= 3) {
							baseGrid.set(gridX, gridZ, 2);
							flag = true;
						} else if (adjacentRooms == 2) {
							int diagonalRooms = 0;
							diagonalRooms = diagonalRooms + (isHouse(baseGrid, gridX + 1, gridZ + 1) ? 1 : 0);
							diagonalRooms = diagonalRooms + (isHouse(baseGrid, gridX - 1, gridZ + 1) ? 1 : 0);
							diagonalRooms = diagonalRooms + (isHouse(baseGrid, gridX + 1, gridZ - 1) ? 1 : 0);
							diagonalRooms = diagonalRooms + (isHouse(baseGrid, gridX - 1, gridZ - 1) ? 1 : 0);
							if (diagonalRooms <= 1) {
								baseGrid.set(gridX, gridZ, 2);
								flag = true;
							}
						}
					}
				}
			}

			return flag;
		}

		// TODO: make not ugly
		private void setupThirdFloor() {
			List<Pair<Integer, Integer>> list = new ArrayList<>();
			SimpleGrid simpleGrid = this.floorGrids[1];

			for(int i = 0; i < this.thirdFloorGrid.height; ++i) {
				for(int j = 0; j < this.thirdFloorGrid.width; ++j) {
					int k = simpleGrid.get(j, i);
					int l = k & 983040;
					if (l == 131072 && (k & 2097152) == 2097152) {
						list.add(new Pair<>(j, i));
					}
				}
			}

			if (list.isEmpty()) {
				this.thirdFloorGrid.set(0, 0, this.thirdFloorGrid.width, this.thirdFloorGrid.height, 5);
			} else {
				Pair<Integer, Integer> randomPos = list.get(this.random.nextInt(list.size()));
				int l1 = simpleGrid.get(randomPos.getFirst(), randomPos.getSecond());
				simpleGrid.set(randomPos.getFirst(), randomPos.getSecond(), l1 | 4194304);
				Direction direction1 = this.get1x2RoomDirection(this.baseGrid, randomPos.getFirst(), randomPos.getSecond(), 1, l1 & '\uffff');
				int i2 = randomPos.getFirst() + direction1.getStepX();
				int i1 = randomPos.getSecond() + direction1.getStepZ();

				for(int j1 = 0; j1 < this.thirdFloorGrid.height; ++j1) {
					for(int k1 = 0; k1 < this.thirdFloorGrid.width; ++k1) {
						if (!isHouse(this.baseGrid, k1, j1)) {
							this.thirdFloorGrid.set(k1, j1, 5);
						} else if (k1 == randomPos.getFirst() && j1 == randomPos.getSecond()) {
							this.thirdFloorGrid.set(k1, j1, 3);
						} else if (k1 == i2 && j1 == i1) {
							this.thirdFloorGrid.set(k1, j1, 3);
							this.floorGrids[2].set(k1, j1, 8388608);
						}
					}
				}

				List<Direction> list1 = new ArrayList<>();

				for(Direction direction : Direction.HORIZONTALS) {
					if (this.thirdFloorGrid.get(i2 + direction.getStepX(), i1 + direction.getStepZ()) == 0) {
						list1.add(direction);
					}
				}

				if (list1.isEmpty()) {
					this.thirdFloorGrid.set(0, 0, this.thirdFloorGrid.width, this.thirdFloorGrid.height, 5);
					simpleGrid.set(randomPos.getFirst(), randomPos.getSecond(), l1);
				} else {
					Direction direction2 = list1.get(this.random.nextInt(list1.size()));
					this.recursiveCorridor(this.thirdFloorGrid, i2 + direction2.getStepX(), i1 + direction2.getStepZ(), direction2, 4);

					while(this.cleanEdges(this.thirdFloorGrid)) {
					}

				}
			}
		}

		private void identifyRooms(SimpleGrid baseGrid, SimpleGrid floorRoom) {
			List<Pair<Integer, Integer>> rooms = new ArrayList<>();

			for(int i = 0; i < baseGrid.height; ++i) {
				for(int j = 0; j < baseGrid.width; ++j) {
					if (baseGrid.get(j, i) == BaseRoomFlag.ROOM) {
						rooms.add(new Pair<>(j, i));
					}
				}
			}

			Collections.shuffle(rooms, this.random);
			int roomCount = 10;

			for(Pair<Integer, Integer> tuple : rooms) {
				int gridX = tuple.getFirst();
				int gridZ = tuple.getSecond();
				if (floorRoom.get(gridX, gridZ) == 0) {
					int minX = gridX;
					int maxX = gridX;
					int minZ = gridZ;
					int maxZ = gridZ;
					int groupFlag = RoomGroupFlag._1x1FLAG;
					if (floorRoom.get(gridX + 1, gridZ) == 0 &&
						floorRoom.get(gridX, gridZ + 1) == 0 &&
						floorRoom.get(gridX + 1, gridZ + 1) == 0 &&
						baseGrid.get(gridX + 1, gridZ) == BaseRoomFlag.ROOM &&
						baseGrid.get(gridX, gridZ + 1) == BaseRoomFlag.ROOM &&
						baseGrid.get(gridX + 1, gridZ + 1) == BaseRoomFlag.ROOM) {
						maxX = gridX + 1;
						maxZ = gridZ + 1;
						groupFlag = RoomGroupFlag._2x2FLAG;
					} else if (floorRoom.get(gridX - 1, gridZ) == 0 &&
						floorRoom.get(gridX, gridZ + 1) == 0 &&
						floorRoom.get(gridX - 1, gridZ + 1) == 0 &&
						baseGrid.get(gridX - 1, gridZ) == BaseRoomFlag.ROOM &&
						baseGrid.get(gridX, gridZ + 1) == BaseRoomFlag.ROOM &&
						baseGrid.get(gridX - 1, gridZ + 1) == BaseRoomFlag.ROOM) {
						minX = gridX - 1;
						maxZ = gridZ + 1;
						groupFlag = RoomGroupFlag._2x2FLAG;
					} else if (floorRoom.get(gridX - 1, gridZ) == 0 &&
						floorRoom.get(gridX, gridZ - 1) == 0 &&
						floorRoom.get(gridX - 1, gridZ - 1) == 0 &&
						baseGrid.get(gridX - 1, gridZ) == BaseRoomFlag.ROOM &&
						baseGrid.get(gridX, gridZ - 1) == BaseRoomFlag.ROOM &&
						baseGrid.get(gridX - 1, gridZ - 1) == BaseRoomFlag.ROOM) {
						minX = gridX - 1;
						minZ = gridZ - 1;
						groupFlag = RoomGroupFlag._2x2FLAG;
					} else if (floorRoom.get(gridX + 1, gridZ) == 0 &&
						baseGrid.get(gridX + 1, gridZ) == BaseRoomFlag.ROOM) {
						maxX = gridX + 1;
						groupFlag = RoomGroupFlag._1x2FLAG;
					} else if (floorRoom.get(gridX, gridZ + 1) == 0 &&
						baseGrid.get(gridX, gridZ + 1) == BaseRoomFlag.ROOM) {
						maxZ = gridZ + 1;
						groupFlag = RoomGroupFlag._1x2FLAG;
					} else if (floorRoom.get(gridX - 1, gridZ) == 0 &&
						baseGrid.get(gridX - 1, gridZ) == BaseRoomFlag.ROOM) {
						minX = gridX - 1;
						groupFlag = RoomGroupFlag._1x2FLAG;
					} else if (floorRoom.get(gridX, gridZ - 1) == 0 &&
						baseGrid.get(gridX, gridZ - 1) == BaseRoomFlag.ROOM) {
						minZ = gridZ - 1;
						groupFlag = RoomGroupFlag._1x2FLAG;
					}

					int startX = this.random.nextBoolean() ? minX : maxX;
					int startZ = this.random.nextBoolean() ? minZ : maxZ;
					int secretFlag = RoomGroupFlag.SECRET;
					if (!baseGrid.adjacentTo(startX, startZ, BaseRoomFlag.CORRIDOR)) {
						startX = startX == minX ? maxX : minX;
						startZ = startZ == minZ ? maxZ : minZ;
						if (!baseGrid.adjacentTo(startX, startZ, BaseRoomFlag.CORRIDOR)) {
							startZ = startZ == minZ ? maxZ : minZ;
							if (!baseGrid.adjacentTo(startX, startZ, BaseRoomFlag.CORRIDOR)) {
								startX = startX == minX ? maxX : minX;
								startZ = startZ == minZ ? maxZ : minZ;
								if (!baseGrid.adjacentTo(startX, startZ, BaseRoomFlag.CORRIDOR)) {
									secretFlag = 0;
									startX = minX;
									startZ = minZ;
								}
							}
						}
					}

					for(int groupZ = minZ; groupZ <= maxZ; ++groupZ) {
						for(int groupX = minX; groupX <= maxX; ++groupX) {
							if (groupX == startX && groupZ == startZ) {
								floorRoom.set(groupX, groupZ, RoomGroupFlag.START | secretFlag | groupFlag | roomCount);
							} else {
								floorRoom.set(groupX, groupZ, groupFlag | roomCount);
							}
						}
					}

					++roomCount;
				}
			}

		}
	}

	static class Placer {
		private final Random random;
		private int startX;
		private int startZ;

		public Placer(Random random) {
			this.random = random;
		}

		public void createMansion(BPos start, BlockRotation rotation, List<MansionPiece> mansionPieces, Grid grid) {

			SimpleGrid baseGrid = grid.baseGrid;
			SimpleGrid thirdFloorGrid = grid.thirdFloorGrid;
			this.startX = grid.entranceX + 1;
			this.startZ = grid.entranceZ + 1;

			RoomCollection[] roomCollection = new RoomCollection[]{new FirstFloor(), new SecondFloor(), new ThirdFloor()};

			for(int floorIndex = 0; floorIndex < 3; ++floorIndex) {
				BPos bPos = start.add(0, 8 * floorIndex + (floorIndex == 2 ? 3 : 0), 0);
				SimpleGrid floorGrid = grid.floorGrids[floorIndex];
				SimpleGrid baseFloorGrid = floorIndex == 2 ? thirdFloorGrid : baseGrid;

				// Indoor Walls + Rooms
				// String s2 = floorIndex == 0 ? "indoors_wall_1" : "indoors_wall_2";
				// String s3 = floorIndex == 0 ? "indoors_door_1" : "indoors_door_2";
				List<Direction> doorways = new ArrayList<>();
				for(int gridZ = 0; gridZ < baseFloorGrid.height; ++gridZ) {
					for(int gridX = 0; gridX < baseFloorGrid.width; ++gridX) {
						boolean thirdFloorStart = floorIndex == 2 && baseFloorGrid.get(gridX, gridZ) == BaseRoomFlag.START;
						if (baseFloorGrid.get(gridX, gridZ) == BaseRoomFlag.ROOM || thirdFloorStart) {
							int gridFlag = floorGrid.get(gridX, gridZ);
							int roomSize = gridFlag & RoomGroupFlag.ROOM_SIZE;
							int roomId = gridFlag & '\uffff';
							thirdFloorStart = thirdFloorStart && (gridFlag & RoomGroupFlag.ENTRANCE) == RoomGroupFlag.ENTRANCE;
							doorways.clear();
							if ((gridFlag & RoomGroupFlag.SECRET) == RoomGroupFlag.SECRET) {
								for(Direction direction : Direction.HORIZONTALS) {
									if (baseFloorGrid.get(gridX + direction.getStepX(), gridZ + direction.getStepZ()) == BaseRoomFlag.CORRIDOR) {
										doorways.add(direction);
									}
								}
							}

							Direction doorDirection = null;
							if (!doorways.isEmpty()) {
								doorDirection = doorways.get(this.random.nextInt(doorways.size()));
							} else if ((gridFlag & RoomGroupFlag.START) == RoomGroupFlag.START) {
								doorDirection = Direction.UP;
							}

							// TODO: figure out if we need this
							BPos nextBPos = bPos.relative(rotation.rotate(BlockDirection.SOUTH), 8 + (gridZ - this.startZ) * 8);
							nextBPos = nextBPos.relative(rotation.rotate(BlockDirection.EAST), -1 + (gridX - this.startX) * 8);
							/*
							if (Grid.isHouse(baseFloorGrid, gridX - 1, gridZ) && !grid.isRoomId(baseFloorGrid, gridX - 1, gridZ, floorIndex, roomId)) {
								//pieces.add(new WoodlandMansionPieces.MansionTemplate(this.structureManager, doorDirection == Direction.WEST ? s3 : s2, nextBPos, rotation));
								String template = doorDirection == Direction.WEST ? s3 : s2;
								//System.out.println("new Mansion Template: " + template + nextBPos.toString() + rotation.toString());
							}

							if (baseFloorGrid.get(gridX + 1, gridZ) == BaseRoomFlag.CORRIDOR && !thirdFloorStart) {
								BPos doorBPos = nextBPos.relative(rotation.rotate(BlockDirection.EAST), 8);
								//pieces.add(new WoodlandMansionPieces.MansionTemplate(this.structureManager, doorDirection == Direction.EAST ? s3 : s2, doorBPos, rotation));
								String template = doorDirection == Direction.EAST ? s3 : s2;
								//System.out.println("new Mansion Template: " + template + doorBPos.toString() + rotation.toString());
							}

							if (Grid.isHouse(baseFloorGrid, gridX, gridZ + 1) && !grid.isRoomId(baseFloorGrid, gridX, gridZ + 1, floorIndex, roomId)) {
								BPos doorBPos = nextBPos.relative(rotation.rotate(BlockDirection.SOUTH), 7);
								doorBPos = doorBPos.relative(rotation.rotate(BlockDirection.EAST), 7);
								//pieces.add(new WoodlandMansionPieces.MansionTemplate(this.structureManager, doorDirection == Direction.SOUTH ? s3 : s2, doorBPos, rotation.getRotated(Rotation.CLOCKWISE_90)));
								String template = doorDirection == Direction.SOUTH ? s3 : s2;
								//System.out.println("new Mansion Template: " + template + doorBPos.toString() + rotation.toString());
							}

							if (baseFloorGrid.get(gridX, gridZ - 1) == 1 && !thirdFloorStart) {
								BPos doorBPos = nextBPos.relative(rotation.rotate(BlockDirection.NORTH), 1);
								doorBPos = doorBPos.relative(rotation.rotate(BlockDirection.EAST), 7);
								//pieces.add(new WoodlandMansionPieces.MansionTemplate(this.structureManager, doorDirection == Direction.NORTH ? s3 : s2, doorBPos, rotation.getRotated(Rotation.CLOCKWISE_90)));
								String template = doorDirection == Direction.NORTH ? s3 : s2;
								//System.out.println("new Mansion Template: " + template + doorBPos.toString() + rotation.toString());
							}

							 */

							if (roomSize == RoomGroupFlag._1x1FLAG) {
								this.addRoom1x1(mansionPieces, nextBPos, rotation, doorDirection, roomCollection[floorIndex]);
							} else if (roomSize == RoomGroupFlag._1x2FLAG && doorDirection != null) {
								Direction roomDirection = grid.get1x2RoomDirection(baseFloorGrid, gridX, gridZ, floorIndex, roomId);
								boolean flag2 = (gridFlag & RoomGroupFlag.STAIRS) == RoomGroupFlag.STAIRS;
								this.addRoom1x2(mansionPieces, nextBPos, rotation, roomDirection, doorDirection, roomCollection[floorIndex], flag2);
							} else if (roomSize == RoomGroupFlag._2x2FLAG && doorDirection != null && doorDirection != Direction.UP) {
								Direction roomDirection = doorDirection.getClockWise();
								if (!grid.isRoomId(baseFloorGrid, gridX + roomDirection.getStepX(), gridZ + roomDirection.getStepZ(), floorIndex, roomId)) {
									roomDirection = roomDirection.getOpposite();
								}
								this.addRoom2x2(mansionPieces, nextBPos, rotation, roomDirection, doorDirection, roomCollection[floorIndex]);
							} else if (roomSize == RoomGroupFlag._2x2FLAG && doorDirection == Direction.UP) {
								this.addRoom2x2Secret(mansionPieces, nextBPos, rotation, roomCollection[floorIndex]);
							}
						}
					}
				}
			}
		}

		private void addRoom1x1(List<MansionPiece> mansionPieces, BPos start, BlockRotation rotation, Direction roomDirection, RoomCollection roomCollection) {
			BlockRotation defaultRotation = BlockRotation.NONE;
			String template = roomCollection.get1x1(this.random);
			if (roomDirection != Direction.EAST) {
				if (roomDirection == Direction.NORTH) {
					defaultRotation = defaultRotation.getRotated(BlockRotation.COUNTERCLOCKWISE_90);
				} else if (roomDirection == Direction.WEST) {
					defaultRotation = defaultRotation.getRotated(BlockRotation.CLOCKWISE_180);
				} else if (roomDirection == Direction.SOUTH) {
					defaultRotation = defaultRotation.getRotated(BlockRotation.CLOCKWISE_90);
				} else {
					template = roomCollection.get1x1Secret(this.random);
				}
			}

			BPos getZeroPos = MansionPiece.getZeroPositionWithTransform(new BPos(1, 0, 0), BlockMirror.NONE, defaultRotation, 7, 7);
			defaultRotation = defaultRotation.getRotated(rotation);

			// TODO: validate this
			getZeroPos = getZeroPos.transform(BlockMirror.NONE, rotation, new BPos(0,getZeroPos.getY(),0));
			BPos finalBPos = start.add(getZeroPos.getX(), 0, getZeroPos.getZ());
			mansionPieces.add(new MansionPiece(template, finalBPos, defaultRotation, BlockMirror.NONE, roomCollection.getFloorNumber()));

		}

		private void addRoom1x2(List<MansionPiece> mansionPieces, BPos start, BlockRotation rotation, Direction roomDirection, Direction doorDirection, RoomCollection roomCollection, boolean isStairs) {
			if (doorDirection == Direction.EAST && roomDirection == Direction.SOUTH) {
				BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 1);
				mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation, BlockMirror.NONE, roomCollection.getFloorNumber()));
			} else if (doorDirection == Direction.EAST && roomDirection == Direction.NORTH) {
				BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 1);
				finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.SOUTH), 6);
				mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation, BlockMirror.LEFT_RIGHT, roomCollection.getFloorNumber()));
			} else if (doorDirection == Direction.WEST && roomDirection == Direction.NORTH) {
				BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 7);
				finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.SOUTH), 6);
				mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.CLOCKWISE_180), BlockMirror.NONE, roomCollection.getFloorNumber()));
			} else if (doorDirection == Direction.WEST && roomDirection == Direction.SOUTH) {
				BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 7);
				mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation, BlockMirror.FRONT_BACK, roomCollection.getFloorNumber()));
			} else if (doorDirection == Direction.SOUTH && roomDirection == Direction.EAST) {
				BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 1);
				mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.CLOCKWISE_90), BlockMirror.LEFT_RIGHT, roomCollection.getFloorNumber()));
			} else if (doorDirection == Direction.SOUTH && roomDirection == Direction.WEST) {
				BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 7);
				mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.CLOCKWISE_90), BlockMirror.NONE, roomCollection.getFloorNumber()));
			} else if (doorDirection == Direction.NORTH && roomDirection == Direction.WEST) {
				BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 7);
				finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.SOUTH), 6);
				mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.CLOCKWISE_90), BlockMirror.FRONT_BACK, roomCollection.getFloorNumber()));
			} else if (doorDirection == Direction.NORTH && roomDirection == Direction.EAST) {
				BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 1);
				finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.SOUTH), 6);
				mansionPieces.add(new MansionPiece(roomCollection.get1x2SideEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.COUNTERCLOCKWISE_90), BlockMirror.NONE, roomCollection.getFloorNumber()));
			} else if (doorDirection == Direction.SOUTH && roomDirection == Direction.NORTH) {
				BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 1);
				finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.NORTH), 8);
				mansionPieces.add(new MansionPiece(roomCollection.get1x2FrontEntrance(this.random, isStairs), finalBPos, rotation, BlockMirror.NONE, roomCollection.getFloorNumber()));
			} else if (doorDirection == Direction.NORTH && roomDirection == Direction.SOUTH) {
				BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 7);
				finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.SOUTH), 14);
				mansionPieces.add(new MansionPiece(roomCollection.get1x2FrontEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.CLOCKWISE_180), BlockMirror.NONE, roomCollection.getFloorNumber()));
			} else if (doorDirection == Direction.WEST && roomDirection == Direction.EAST) {
				BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 15);
				mansionPieces.add(new MansionPiece(roomCollection.get1x2FrontEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.CLOCKWISE_90), BlockMirror.NONE, roomCollection.getFloorNumber()));
			} else if (doorDirection == Direction.EAST && roomDirection == Direction.WEST) {
				BPos finalBPos = start.relative(rotation.rotate(BlockDirection.WEST), 7);
				finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.SOUTH), 6);
				mansionPieces.add(new MansionPiece(roomCollection.get1x2FrontEntrance(this.random, isStairs), finalBPos, rotation.getRotated(BlockRotation.COUNTERCLOCKWISE_90), BlockMirror.NONE, roomCollection.getFloorNumber()));
			} else if (doorDirection == Direction.UP && roomDirection == Direction.EAST) {
				BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 15);
				mansionPieces.add(new MansionPiece(roomCollection.get1x2Secret(this.random), finalBPos, rotation.getRotated(BlockRotation.CLOCKWISE_90), BlockMirror.NONE, roomCollection.getFloorNumber()));
			} else if (doorDirection == Direction.UP && roomDirection == Direction.SOUTH) {
				BPos blockpos = start.relative(rotation.rotate(BlockDirection.EAST), 1);
				blockpos = blockpos.relative(rotation.rotate(BlockDirection.NORTH), 0);
				mansionPieces.add(new MansionPiece(roomCollection.get1x2Secret(this.random), blockpos, rotation, BlockMirror.NONE, roomCollection.getFloorNumber()));
			}

		}

		private void addRoom2x2(List<MansionPiece> mansionPieces, BPos start, BlockRotation rotation, Direction roomDirection, Direction doorDirection, RoomCollection roomCollection) {
			int offsetX = 0;
			int offetZ = 0;
			BlockRotation roomRotation = rotation;
			BlockMirror mirror = BlockMirror.NONE;
			if (doorDirection == Direction.EAST && roomDirection == Direction.SOUTH) {
				offsetX = -7;
			} else if (doorDirection == Direction.EAST && roomDirection == Direction.NORTH) {
				offsetX = -7;
				offetZ = 6;
				mirror = BlockMirror.LEFT_RIGHT;
			} else if (doorDirection == Direction.NORTH && roomDirection == Direction.EAST) {
				offsetX = 1;
				offetZ = 14;
				roomRotation = rotation.getRotated(BlockRotation.COUNTERCLOCKWISE_90);
			} else if (doorDirection == Direction.NORTH && roomDirection == Direction.WEST) {
				offsetX = 7;
				offetZ = 14;
				roomRotation = rotation.getRotated(BlockRotation.COUNTERCLOCKWISE_90);
				mirror = BlockMirror.LEFT_RIGHT;
			} else if (doorDirection == Direction.SOUTH && roomDirection == Direction.WEST) {
				offsetX = 7;
				offetZ = -8;
				roomRotation = rotation.getRotated(BlockRotation.CLOCKWISE_90);
			} else if (doorDirection == Direction.SOUTH && roomDirection == Direction.EAST) {
				offsetX = 1;
				offetZ = -8;
				roomRotation = rotation.getRotated(BlockRotation.CLOCKWISE_90);
				mirror = BlockMirror.LEFT_RIGHT;
			} else if (doorDirection == Direction.WEST && roomDirection == Direction.NORTH) {
				offsetX = 15;
				offetZ = 6;
				roomRotation = rotation.getRotated(BlockRotation.CLOCKWISE_180);
			} else if (doorDirection == Direction.WEST && roomDirection == Direction.SOUTH) {
				offsetX = 15;
				mirror = BlockMirror.FRONT_BACK;
			}

			BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), offsetX);
			finalBPos = finalBPos.relative(rotation.rotate(BlockDirection.SOUTH), offetZ);
			mansionPieces.add(new MansionPiece(roomCollection.get2x2(this.random), finalBPos, roomRotation, mirror, roomCollection.getFloorNumber()));
		}

		private void addRoom2x2Secret(List<MansionPiece> mansionPieces, BPos start, BlockRotation rotation, RoomCollection roomCollection) {
			BPos finalBPos = start.relative(rotation.rotate(BlockDirection.EAST), 1);
			mansionPieces.add(new MansionPiece(roomCollection.get2x2Secret(this.random), finalBPos, rotation, BlockMirror.NONE, roomCollection.getFloorNumber()));
		}

	}

	abstract static class RoomCollection {
		private RoomCollection() {
		}

		public abstract int getFloorNumber();

		public abstract String get1x1(Random random);

		public abstract String get1x1Secret(Random random);

		public abstract String get1x2SideEntrance(Random random, boolean isStairs);

		public abstract String get1x2FrontEntrance(Random random, boolean isStairs);

		public abstract String get1x2Secret(Random random);

		public abstract String get2x2(Random random);

		public abstract String get2x2Secret(Random random);
	}

	static class FirstFloor extends RoomCollection {
		private FirstFloor() {
		}

		@Override
		public int getFloorNumber() {
			return 0;
		}

		@Override
		public String get1x1(Random random) {
			return "1x1_a" + (random.nextInt(5) + 1);
		}

		@Override
		public String get1x1Secret(Random random) {
			return "1x1_as" + (random.nextInt(4) + 1);
		}

		@Override
		public String get1x2SideEntrance(Random random, boolean isStairs) {
			return "1x2_a" + (random.nextInt(9) + 1);
		}

		@Override
		public String get1x2FrontEntrance(Random random, boolean isStairs) {
			return "1x2_b" + (random.nextInt(5) + 1);
		}

		@Override
		public String get1x2Secret(Random random) {
			return "1x2_s" + (random.nextInt(2) + 1);
		}

		@Override
		public String get2x2(Random random) {
			return "2x2_a" + (random.nextInt(4) + 1);
		}

		@Override
		public String get2x2Secret(Random random) {
			return "2x2_s1";
		}
	}

	static class SecondFloor extends RoomCollection {
		private SecondFloor() {
		}

		@Override
		public int getFloorNumber() {
			return 1;
		}

		@Override
		public String get1x1(Random random) {
			return "1x1_b" + (random.nextInt(4) + 1);
		}

		@Override
		public String get1x1Secret(Random random) {
			return "1x1_as" + (random.nextInt(4) + 1);
		}

		@Override
		public String get1x2SideEntrance(Random random, boolean isStairs) {
			return isStairs ? "1x2_c_stairs" : "1x2_c" + (random.nextInt(4) + 1);
		}

		@Override
		public String get1x2FrontEntrance(Random random, boolean isStairs) {
			return isStairs ? "1x2_d_stairs" : "1x2_d" + (random.nextInt(5) + 1);
		}

		@Override
		public String get1x2Secret(Random random) {
			return "1x2_se" + (random.nextInt(1) + 1);
		}

		@Override
		public String get2x2(Random random) {
			return "2x2_b" + (random.nextInt(5) + 1);
		}

		@Override
		public String get2x2Secret(Random random) {
			return "2x2_s1";
		}
	}

	static class ThirdFloor extends SecondFloor {
		private ThirdFloor() {
		}

		@Override
		public int getFloorNumber() {
			return 2;
		}
	}

	enum Direction {
		DOWN(Axis.Y, new Vec3i(0, -1, 0)),
		UP(Axis.Y, new Vec3i(0, 1, 0)),
		NORTH(Axis.Z, new Vec3i(0, 0, -1)), // NONE
		SOUTH(Axis.Z, new Vec3i(0, 0, 1)), // CLOCKWISE_180
		WEST(Axis.X, new Vec3i(-1, 0, 0)), // COUNTERCLOCKWISE_90
		EAST(Axis.X, new Vec3i(1, 0, 0)); // CLOCKWISE_90

		private static final Direction[] VALUES = values();
		private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		private static final Direction[] BY_2D_DATA = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};

		private static final Map<String, Direction> STRING_TO_BLOCK_DIRECTION = Arrays.stream(values()).collect(Collectors.toMap(Direction::name, o -> o));
		private final Axis axis;
		private final Vec3i vec;

		Direction(Axis axis, Vec3i vec) {
			this.axis = axis;
			this.vec = vec;
		}

		public int getStepX() {
			return this.vec.getX();
		}

		public int getStepY() {
			return this.vec.getY();
		}

		public int getStepZ() {
			return this.vec.getZ();
		}

		public static Direction from2DDataValue(int value) {
			return BY_2D_DATA[Math.abs(value % BY_2D_DATA.length)];
		}

		public Direction getOpposite() {
			switch(this) {
				case DOWN:
					return UP;
				case UP:
					return DOWN;
				case NORTH:
					return SOUTH;
				case SOUTH:
					return NORTH;
				case WEST:
					return EAST;
				case EAST:
					return WEST;
				default:
					throw new IllegalStateException("Invalid direction: " + this);
			}
		}

		public Direction getClockWise() {
			switch(this) {
				case NORTH:
					return EAST;
				case SOUTH:
					return WEST;
				case WEST:
					return NORTH;
				case EAST:
					return SOUTH;
				default:
					throw new IllegalStateException("Unable to to get CW facing of " + this);
			}
		}

		public Direction getCounterClockWise() {
			switch(this) {
				case NORTH:
					return WEST;
				case SOUTH:
					return EAST;
				case WEST:
					return SOUTH;
				case EAST:
					return NORTH;
				default:
					throw new IllegalStateException("Unable to get CCW facing of " + this);
			}
		}

		public enum Axis {
			X, Y, Z;

			public Axis get2DRotated() {
				switch (this) {
					case X:
						return Z;
					case Z:
						return X;
					default:
						return Y;
				}
			}

		}
	}

	public static final Map<String, String> COMMON_NAMES = Map.ofEntries(
		Map.entry("1x1_a1", "Flower room"),
		Map.entry("1x1_a2", "Pumpkin ring room"),
		Map.entry("1x1_a3", "Office"),
		Map.entry("1x1_a4", "Checkerboard room"),
		Map.entry("1x1_a5", "White tulip sanctuary"),
		Map.entry("1x1_as1", "X room"),
		Map.entry("1x1_as2", "Spider room"),
		Map.entry("1x1_as3", "Obsidian room"),
		Map.entry("1x1_as4", "Birch pillar room"),
		Map.entry("1x1_b1", "Birch arch room"),
		Map.entry("1x1_b2", "Small dining room"),
		Map.entry("1x1_b3", "Single bed bedroom"),
		Map.entry("1x1_b4", "Small library"),
		Map.entry("1x1_b5", "Allium room"),
		Map.entry("1x2_a1", "Gray banner room"),
		Map.entry("1x2_a2", "Wheat farm"),
		Map.entry("1x2_a3", "Forge room"),
		Map.entry("1x2_a4", "Sapling farm"),
		Map.entry("1x2_a6", "Tree-chopping room"),
		Map.entry("1x2_a7", "Mushroom farm"),
		Map.entry("1x2_a8", "Dual-staged farm"),
		Map.entry("1x2_a9", "Small storage room"),
		Map.entry("1x2_b1", "Redstone jail"),
		Map.entry("1x2_b2", "Small jail"),
		Map.entry("1x2_b3", "Wood arch hallway"),
		Map.entry("1x2_b4", "Winding stairway room"),
		Map.entry("1x2_b5", "Illager head room"),
		Map.entry("1x2_c_stairs", "Curved staircase"),
		Map.entry("1x2_c1", "Medium dining room"),
		Map.entry("1x2_c2", "Double bed bedroom"),
		Map.entry("1x2_c3", "Triple bed bedroom"),
		Map.entry("1x2_c4", "Medium library"),
		Map.entry("1x2_d_stairs", "Straight staircase"),
		Map.entry("1x2_d1", "Master bedroom"),
		Map.entry("1x2_d2", "Bedroom with loft"),
		Map.entry("1x2_d3", "Ritual room"),
		Map.entry("1x2_d4", "Cat statue room"),
		Map.entry("1x2_d5", "Chicken statue room"),
		Map.entry("1x2_s1", "Clean chest room"),
		Map.entry("1x2_s2", "Fake End portal room"),
		Map.entry("1x2_se1", "Attic room"),
		Map.entry("2x2_a1", "Large jail"),
		Map.entry("2x2_a2", "Large storage room"),
		Map.entry("2x2_a3", "Illager statue room"),
		Map.entry("2x2_a4", "Nature room"),
		Map.entry("2x2_b1", "Large dining room"),
		Map.entry("2x2_b2", "Conference room"),
		Map.entry("2x2_b3", "Large library"),
		Map.entry("2x2_b4", "Map room"),
		Map.entry("2x2_b5", "Arena room"),
		Map.entry("2x2_s1", "Lava room")
	);

	static class BaseRoomFlag {
		static final int OUTSIDE = 5;
		static final int UNSET = 0;
		static final int CORRIDOR = 1;
		static final int ROOM = 2;
		static final int START = 3;
	}

	static class RoomGroupFlag {
		static final int ROOM_ID = 0xFFFF;
		static final int ROOM_SIZE = 0xF0000;

		static final int _1x1FLAG = 0x10000;
		static final int _1x2FLAG = 0x20000;
		static final int _2x2FLAG = 0x40000;

		static final int START = 0x100000;
		static final int SECRET = 0x200000;
		static final int STAIRS = 0x400000;
		static final int ENTRANCE = 0x800000;
	}
}
