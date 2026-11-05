import arc.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

class IndustrialContentTests{
    @BeforeAll
    static void init(){ ApplicationTests.launchApplication(); }

    @BeforeEach
    void reset(){ logic.reset(); Core.settings.setJson(JsonIO.json); }

    @AfterEach
    void clean(){ logic.reset(); }

    private void industrial(){
        state.rules.sector = Planets.industrialSerpulo.getStartSector();
        Planets.industrialSerpulo.applyRules(state.rules);
    }

    @Test
    void militaryMenusAreScopedToIndustrialGames(){
        industrial();
        for(Block block : content.blocks()){
            if(block instanceof Turret || block.category == Category.turret || block.category == Category.defense){
                assertFalse(IndustrialContent.visible(block), block.name);
            }
        }
        for(var block : new Block[]{Blocks.forceProjector, Blocks.shockMine, Blocks.shockwaveTower}) assertFalse(IndustrialContent.visible(block), block.name);
        assertFalse(IndustrialContent.visible(Stat.weapons));
        assertFalse(IndustrialContent.visible(Stat.ammo));
        logic.reset();
        state.rules.sector = Planets.serpulo.getStartSector();
        for(var block : content.blocks()) assertTrue(IndustrialContent.visible(block), block.name);
        for(var unit : content.units()) assertTrue(IndustrialContent.visible(unit), unit.name);
        assertTrue(IndustrialContent.visible(Stat.weapons));
        assertTrue(IndustrialContent.visible(Stat.ammo));
        assertTrue(Planets.serpulo.allowWaves);
        Seq<mindustry.ctype.UnlockableContent> choices = new Seq<>();
        Blocks.airFactory.getPlanConfigs(choices);
        assertTrue(choices.contains(UnitTypes.flare));
        assertTrue(choices.contains(UnitTypes.mono));
        logic.reset();
        Gamemode.industrial.apply(state.rules);
        assertFalse(IndustrialContent.visible(Blocks.duo));
        assertTrue(IndustrialContent.visible(Blocks.conveyor));
    }

    @Test
    void supportUnitsAndPayloadLogisticsSurviveTheFilter(){
        industrial();
        for(var unit : new UnitType[]{UnitTypes.mono, UnitTypes.poly, UnitTypes.mega, UnitTypes.quad, UnitTypes.oct, UnitTypes.alpha}){
            assertTrue(IndustrialContent.visible(unit), unit.name);
        }
        for(var unit : new UnitType[]{UnitTypes.dagger, UnitTypes.flare, UnitTypes.risso, UnitTypes.reign}) assertFalse(IndustrialContent.visible(unit), unit.name);
        assertTrue(IndustrialContent.visible(Blocks.airFactory));
        Seq<mindustry.ctype.UnlockableContent> choices = new Seq<>();
        Blocks.airFactory.getPlanConfigs(choices);
        assertTrue(choices.contains(UnitTypes.mono));
        assertFalse(choices.contains(UnitTypes.flare));
        choices.clear();
        state.rules.sector = null; // Inspect Erekir payload choices in an industrial custom game.
        Blocks.constructor.getPlanConfigs(choices);
        assertFalse(choices.contains(Blocks.tungstenWallLarge));
        assertTrue(choices.contains(Blocks.beamNode));
        industrial();
        assertTrue(IndustrialContent.visible(Blocks.groundFactory), "Nova also builds; do not remove a dual-use factory");
        assertFalse(IndustrialContent.visible(Blocks.navalFactory));
        for(var block : new Block[]{Blocks.additiveReconstructor, Blocks.multiplicativeReconstructor, Blocks.exponentialReconstructor, Blocks.tetrativeReconstructor,
            Blocks.payloadConveyor, Blocks.payloadRouter, Blocks.payloadMassDriver, Blocks.constructor, Blocks.largeConstructor, Blocks.payloadLoader, Blocks.payloadUnloader,
            Blocks.repairPoint, Blocks.mender, Blocks.overdriveDome, Blocks.massDriver, Blocks.launchPad}){
            assertTrue(IndustrialContent.visible(block), block.name);
        }
        for(var block : content.blocks()){
            if(block instanceof Reconstructor r){
                for(var pair : r.upgrades){
                    if(IndustrialContent.industrial(pair[1])){
                        assertTrue(IndustrialContent.industrial(pair[0]), "Missing prerequisite unit: " + pair[0].name);
                        assertTrue(IndustrialContent.industrial(r), r.name);
                    }
                }
            }
        }
    }

    @Test
    void everyIndustrialRecipeAndConstructionInputRemainsVisible(){
        industrial();
        for(var block : content.blocks()){
            if(block.category == Category.production || block.category == Category.crafting || block.category == Category.power ||
                block.category == Category.distribution || block.category == Category.liquid || block.category == Category.logic){
                assertTrue(IndustrialContent.industrial(block), "Industrial machine hidden: " + block.name);
            }
            if(!IndustrialContent.industrial(block)) continue;
            for(var stack : block.requirements) assertTrue(IndustrialContent.visible(stack.item), block.name + " construction " + stack.item.name);
            for(var consume : block.consumers){
                if(consume instanceof ConsumeItems items) for(var stack : items.items) assertTrue(IndustrialContent.visible(stack.item), block.name + " input " + stack.item.name);
                if(consume instanceof ConsumeLiquid liquid) assertTrue(IndustrialContent.visible(liquid.liquid), block.name + " input " + liquid.liquid.name);
                if(consume instanceof ConsumeLiquids liquids) for(var stack : liquids.liquids) assertTrue(IndustrialContent.visible(stack.liquid), block.name + " input " + stack.liquid.name);
            }
            if(block instanceof UnitFactory factory) for(var plan : factory.plans){
                if(IndustrialContent.industrial(plan.unit)) for(var stack : plan.requirements) assertTrue(IndustrialContent.visible(stack.item), stack.item.name);
            }
        }
        assertTrue(IndustrialContent.visible(Items.blastCompound), "Impact reactor fuel");
        assertTrue(IndustrialContent.visible(Items.pyratite), "Combustion fuel");
        assertTrue(IndustrialContent.visible(Blocks.impactReactor));
        assertTrue(IndustrialContent.visible(Blocks.blastMixer));
        assertTrue(IndustrialContent.visible(Blocks.pyratiteMixer));
        assertTrue(IndustrialContent.visible(Items.thorium));
        assertTrue(IndustrialContent.visible(Liquids.cryofluid));
    }

    @Test
    void policyDoesNotMutateContentOrClassicUnlocks(){
        String original = JsonIO.write(state.rules);
        boolean unlocked = Blocks.duo.unlocked();
        var visibility = Blocks.duo.buildVisibility;
        int plans = ((UnitFactory)Blocks.airFactory).plans.size;
        var requirements = Blocks.duo.requirements;
        int planets = content.planets().size;
        industrial();
        for(var block : content.blocks()) IndustrialContent.visible(block);
        assertEquals(unlocked, Blocks.duo.unlocked());
        assertSame(visibility, Blocks.duo.buildVisibility);
        assertSame(requirements, Blocks.duo.requirements);
        assertEquals(plans, ((UnitFactory)Blocks.airFactory).plans.size);
        assertEquals(planets, content.planets().size);
        logic.reset();
        assertEquals(original, JsonIO.write(state.rules));
        assertThrows(IllegalStateException.class, () -> IndustrialContent.catalog(() -> {
            assertFalse(IndustrialContent.visible(Blocks.duo));
            assertFalse(IndustrialContent.visible(UnitTypes.flare));
            throw new IllegalStateException("UI interrupted");
        }));
        assertTrue(IndustrialContent.visible(Blocks.duo), "Catalogue scope must not leak into classic UI");
        assertTrue(IndustrialContent.visible(UnitTypes.flare));
        assertEquals(original, JsonIO.write(state.rules));
    }

    @Test
    void existingMilitaryBuildingsAndInventoryRemainInSavedIndustrialWorlds(){
        world.loadSector(Planets.industrialSerpulo.getStartSector());
        logic.play();
        var core = Team.sharded.core();
        var tile = world.tile(core.tileX() + 6, core.tileY());
        tile.setBlock(Blocks.duo, Team.sharded);
        tile.build.items.add(Items.graphite, 4);
        int x = tile.x, y = tile.y;
        var save = Core.settings.getDataDirectory().child("m5-compat.msav");
        SaveIO.save(save);
        SaveIO.load(save, world.makeSectorContext(Planets.industrialSerpulo.getStartSector()));
        assertSame(Blocks.duo, world.tile(x, y).block());
        assertEquals(4, world.tile(x, y).build.items.get(Items.graphite));
        assertFalse(IndustrialContent.visible(Blocks.duo));
        assertEquals(100, Team.sharded.core().items.get(Items.copper));
        save.delete();
    }
}
