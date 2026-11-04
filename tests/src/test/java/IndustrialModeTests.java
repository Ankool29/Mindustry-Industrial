import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.game.MapObjectives.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.Map;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.power.PowerGenerator.*;
import org.junit.jupiter.api.*;
import java.util.Locale;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

class IndustrialModeTests{
    @BeforeAll
    static void init(){
        ApplicationTests.launchApplication();
    }

    @BeforeEach
    void reset(){
        Time.setDeltaProvider(() -> 1f);
        logic.reset();
    }

    private void factoryWorld(){
        world.loadGenerator(64, 40, tiles -> {
            tiles.fill();
            for(var tile : tiles) tile.setFloor(Blocks.stone.asFloor());
            tiles.get(6, 6).setBlock(Blocks.coreShard, Team.sharded);
        });
        state.map = new Map(StringMap.of("name", "Industrial test"));
        Gamemode.industrial.apply(state.rules);
        logic.play();
    }

    private void tick(int ticks){
        for(int i = 0; i < ticks; i++){
            Time.update();
            Groups.build.update();
            Groups.powerGraph.update();
        }
    }

    @Test
    void visibleModeAndExistingOrdinals(){
        assertFalse(Gamemode.industrial.hidden);
        assertTrue(Gamemode.industrial.valid(ApplicationTests.testMap));
        assertEquals(0, Gamemode.survival.ordinal());
        assertEquals(1, Gamemode.sandbox.ordinal());
        assertEquals(2, Gamemode.attack.ordinal());
        assertEquals(3, Gamemode.pvp.ordinal());
        assertEquals(4, Gamemode.editor.ordinal());
        assertEquals(5, Gamemode.industrial.ordinal());
        var english = I18NBundle.createBundle(Core.files.internal("bundles/bundle"), Locale.ENGLISH);
        var french = I18NBundle.createBundle(Core.files.internal("bundles/bundle"), Locale.FRENCH);
        assertEquals("Industrial", english.get("mode.industrial.name"));
        assertEquals("Industriel", french.get("mode.industrial.name"));
        assertFalse(french.get("mode.industrial.description").isEmpty());
    }

    @Test
    void mapOverridesCannotRestoreCombatOrFreeResources(){
        Rules hostile = new Rules();
        hostile.waves = hostile.waveTimer = hostile.waveSending = true;
        hostile.attackMode = hostile.pvp = hostile.infiniteResources = true;
        hostile.instantBuild = true;
        hostile.winWave = 10;
        hostile.buildCostMultiplier = 0f;
        hostile.mission = "Destroy the enemy core";
        hostile.teams.get(Team.sharded).cheat = true;
        hostile.teams.get(Team.sharded).fillItems = true;
        hostile.teams.get(Team.sharded).infiniteResources = true;
        hostile.objectives.add(new DestroyCoreObjective());
        String original = JsonIO.write(hostile);
        Map map = new Map(StringMap.of("name", "Hostile map", "rules", original));
        Rules result = map.applyRules(Gamemode.industrial);
        assertPeaceful(result);
        assertFalse(result.infiniteResources);
        assertFalse(result.instantBuild);
        assertFalse(result.teams.get(Team.sharded).cheat);
        assertFalse(result.teams.get(Team.sharded).fillItems);
        assertFalse(result.isInfiniteResources(Team.sharded));
        assertEquals(1f, result.buildCostMultiplier);
        assertEquals(original, map.tags.get("rules"), "Source map must not be edited");

        // Control.playMap restores these fields from the original map after loading.
        result.retainContentFields(hostile);
        assertPeaceful(result);
    }

    private void assertPeaceful(Rules rules){
        assertTrue(rules.industrial);
        assertEquals(Gamemode.industrial, rules.mode());
        assertFalse(rules.waves);
        assertFalse(rules.waveTimer);
        assertFalse(rules.waveSending);
        assertFalse(rules.attackMode);
        assertFalse(rules.pvp);
        assertFalse(rules.canGameOver);
        assertTrue(rules.disableWorldProcessors);
        assertTrue(rules.spawns.isEmpty());
        assertTrue(rules.objectives.all.isEmpty());
        assertNull(rules.mission);
    }

    @Test
    void removesInitialEnemiesAndBlocksManualAndDelayedWaves(){
        factoryWorld();
        world.tile(25, 20).setBlock(Blocks.coreShard, Team.crux);
        world.tile(30, 20).setBlock(Blocks.duo, Team.crux);
        world.tile(40, 20).setOverlay(Blocks.spawn);
        Unit enemy = UnitTypes.dagger.spawn(Team.crux, 100, 100);
        Unit builder = UnitTypes.mono.spawn(Team.sharded, 80, 80);
        IndustrialMode.prepareWorld();
        assertFalse(enemy.isAdded());
        assertTrue(builder.isAdded());
        assertEquals(Blocks.air, world.tile(25, 20).block());
        assertEquals(Blocks.air, world.tile(30, 20).block());
        assertEquals(Blocks.air, world.tile(40, 20).overlay());
        assertNotNull(Team.sharded.core());
        int wave = state.wave;
        for(int i = 0; i < 100; i++) logic.runWave();
        assertEquals(wave, state.wave);
        Unit delayed = UnitTypes.dagger.spawn(Team.crux, 100, 100);
        logic.update();
        assertFalse(delayed.isAdded());
        assertFalse(state.gameOver);
        assertFalse(Groups.unit.contains(u -> u.team != Team.sharded));
    }

    @Test
    void drillsAndConveyorsDeliverOre(){
        factoryWorld();
        for(int x = 20; x <= 21; x++){
            for(int y = 10; y <= 11; y++) world.tile(x, y).setOverlay(Blocks.oreCopper);
        }
        world.tile(20, 10).setBlock(Blocks.mechanicalDrill, Team.sharded);
        world.tile(22, 10).setBlock(Blocks.conveyor, Team.sharded, 0);
        world.tile(23, 10).setBlock(Blocks.conveyor, Team.sharded, 0);
        world.tile(24, 10).setBlock(Blocks.container, Team.sharded);
        tick(3600);
        assertTrue(world.tile(24, 10).build.items.get(Items.copper) > 0, "Ore must reach storage through belts");
    }

    @Test
    void poweredMachineConsumesIngredientsAndStopsWithoutPower(){
        factoryWorld();
        world.tile(20, 20).setBlock(Blocks.siliconSmelter, Team.sharded);
        world.tile(22, 20).setBlock(Blocks.combustionGenerator, Team.sharded);
        var machine = world.tile(20, 20).build;
        var generator = world.tile(22, 20).build;
        machine.items.add(Items.coal, 10);
        machine.items.add(Items.sand, 20);
        generator.items.add(Items.coal, 10);
        tick(400);
        assertSame(machine.power.graph, generator.power.graph);
        assertTrue(machine.items.get(Items.silicon) > 0);
        assertTrue(machine.items.get(Items.coal) < 10);
        assertTrue(machine.items.get(Items.sand) < 20);
        assertTrue(((GeneratorBuild)generator).getPowerProduction() > 0f);

        generator.enabled = false;
        tick(60);
        machine.items.set(Items.silicon, 0);
        machine.items.set(Items.coal, 10);
        machine.items.set(Items.sand, 20);
        tick(200);
        assertEquals(0, machine.items.get(Items.silicon), "No free production without power");
    }

    @Test
    void constructionChargesResourcesAndWaitsWhenEmpty(){
        factoryWorld();
        var core = Team.sharded.core();
        Unit builder = UnitTypes.alpha.create(Team.sharded);
        int copper = core.items.get(Items.copper);
        Build.beginPlace(builder, Blocks.conveyor, Team.sharded, 12, 10, 0, null);
        assertTrue(world.tile(12, 10).build instanceof ConstructBuild);
        ((ConstructBuild)world.tile(12, 10).build).construct(builder, core, 1f, null);
        assertEquals(Blocks.conveyor, world.tile(12, 10).block());
        assertEquals(copper - 1, core.items.get(Items.copper));

        core.items.clear();
        Build.beginPlace(builder, Blocks.conveyor, Team.sharded, 13, 10, 0, null);
        var pending = (ConstructBuild)world.tile(13, 10).build;
        pending.construct(builder, core, 1f, null);
        assertEquals(0f, pending.progress);
        assertNotEquals(Blocks.conveyor, world.tile(13, 10).block());
    }

    @Test
    void industrialSaveRestoresModeAndFactoryInventory(){
        factoryWorld();
        world.tile(20, 10).setBlock(Blocks.graphitePress, Team.sharded);
        world.tile(20, 10).build.items.add(Items.coal, 5);
        var file = saveDirectory.child("industrial-test.msav");
        try{
            SaveIO.save(file);
            logic.reset();
            SaveIO.load(file);
            assertPeaceful(state.rules);
            assertEquals(5, world.tile(20, 10).build.items.get(Items.coal));
            assertFalse(state.rules.isInfiniteResources(Team.sharded));
        }finally{
            file.delete();
        }
    }

    @Test
    void previousSaveAndOtherModesRemainUnchanged(){
        SaveIO.load(Core.files.internal("152.msav"));
        assertFalse(state.rules.industrial);
        Rules legacy = JsonIO.read(Rules.class, "{waves:true,winWave:10}");
        assertFalse(legacy.industrial);
        assertEquals(Gamemode.survival, legacy.mode());
        assertTrue(Gamemode.survival.apply(new Rules()).waves);
        assertTrue(Gamemode.sandbox.apply(new Rules()).infiniteResources);
        assertTrue(Gamemode.attack.apply(new Rules()).attackMode);
        assertTrue(Gamemode.pvp.apply(new Rules()).pvp);
        assertTrue(Gamemode.editor.apply(new Rules()).editor);

        logic.reset();
        world.loadMap(ApplicationTests.testMap);
        int wave = state.wave;
        logic.runWave();
        assertEquals(wave + 1, state.wave, "Normal survival waves must still work");
        Unit enemy = UnitTypes.dagger.spawn(Team.crux, 100, 100);
        IndustrialMode.prepareWorld();
        IndustrialMode.removeHostileUnits();
        assertTrue(enemy.isAdded(), "Non-industrial worlds must never be cleaned");
    }
}
