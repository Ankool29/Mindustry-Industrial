import arc.*;
import arc.assets.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.game.IndustrialEconomyCapture.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.blocks.campaign.LaunchPad.*;
import mindustry.world.blocks.campaign.LandingPad.*;
import mindustry.world.blocks.distribution.Conveyor.*;
import mindustry.world.blocks.production.GenericCrafter.*;
import mindustry.world.blocks.production.Drill.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import org.junit.jupiter.api.*;

import java.lang.management.*;
import java.lang.reflect.*;
import java.util.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

class IndustrialCaptureTests{
    Settings previous;Fi directory;Saves saves;Planet strict;Sector source,target;
    ObjectMap<Sector,SectorInfo> infos=new ObjectMap<>();ObjectMap<Sector,Saves.SaveSlot> slots=new ObjectMap<>();
    @BeforeAll static void init(){ApplicationTests.launchApplication();}
    @BeforeEach void setup(){
        logic.reset();Time.setDeltaProvider(()->1f);previous=Core.settings;directory=previous.getDataDirectory().child("m7db-test");directory.deleteDirectory();directory.mkdirs();
        Core.settings=new Settings();Core.settings.setJson(JsonIO.json);Core.settings.setDataDirectory(directory);Core.settings.load();Core.settings.put("m7db-fixture",true);Core.settings.forceSave();
        if(Core.assets==null)Core.assets=new AssetManager();saves=new Saves();strict=Planets.industrialSerpuloProgressive;
        for(var p:new Planet[]{strict,Planets.industrialSerpulo,Planets.serpulo})for(var s:p.sectors){infos.put(s,s.info);slots.put(s,s.save);s.info=new SectorInfo();s.save=null;IndustrialCampaign.prepareInfo(s);}
        source=strict.getStartSector();target=source.near().first();base(source);base(target);
    }
    @AfterEach void cleanup(){logic.reset();infos.each((s,i)->s.info=i);slots.each((s,i)->s.save=i);Core.settings=previous;IndustrialEconomyCapture.forget();directory.deleteDirectory();}
    void base(Sector s){s.info.hasCore=true;s.info.storageCapacity=4000;s.info.waves=s.info.attack=false;s.save=saves.new SaveSlot(directory.child(s.planet.name+s.id+".msav"));}
    void arena(Sector s){logic.reset();world.loadGenerator(72,72,tiles->{tiles.fill();for(var tile:tiles)tile.setFloor(Blocks.stone.asFloor());tiles.get(6,6).setBlock(Blocks.coreShard,Team.sharded);});state.rules.sector=s;s.planet.applyRules(state.rules);state.set(State.playing);}
    void save(){SaveIO.save(state.getSector().save.file);}
    Snapshot capture(){var s=IndustrialEconomyCapture.observe(Origin.manual);assertNotNull(s);return s;}
    Node node(Snapshot s,int x,int y){return s.nodes.find(n->n.position==world.tile(x,y).pos());}
    @Test void sharedCoresAndStorageCountOneModuleOnly(){
        arena(source);world.tile(8,6).setBlock(Blocks.container,Team.sharded);world.tile(20,6).setBlock(Blocks.coreShard,Team.sharded);
        var first=world.tile(6,6).build;assertSame(first.items,world.tile(8,6).build.items);assertSame(first.items,world.tile(20,6).build.items);first.items.set(Items.copper,133);
        var s=capture();assertEquals(133,s.itemTotals[Items.copper.id]);assertEquals(1,s.itemStores.size);assertEquals(3,s.itemStores.first().members.size);assertTrue(s.stocksComplete);
        assertEquals(node(s,6,6).itemStore,node(s,20,6).itemStore);s.itemStores.first().amounts.set(0,999);assertEquals(133,first.items.get(Items.copper));
    }
    @Test void conveyorRepresentationIsCapturedButNotSummedAgain(){
        arena(source);world.tile(16,16).setBlock(Blocks.conveyor,Team.sharded);var b=(ConveyorBuild)world.build(16,16);
        b.items.add(Items.copper,2);b.len=2;b.ids[0]=b.ids[1]=Items.copper;b.xs[0]=.1f;b.xs[1]=.3f;b.ys[0]=.2f;b.ys[1]=.6f;
        var s=capture();Node n=node(s,16,16);assertEquals(2,s.itemTotals[Items.copper.id]);assertEquals(2,n.conveyorIds.length);assertEquals(.1f,n.conveyorX[0]);assertTrue(s.stocksComplete);assertFalse(s.candidate);
        b.xs[0]=.9f;assertEquals(.1f,n.conveyorX[0]);n.conveyorIds[0]=Items.lead.id;assertSame(Items.copper,b.ids[0]);
    }
    @Test void inconsistentConveyorIsReportedWithoutRepair(){
        arena(source);world.tile(16,16).setBlock(Blocks.conveyor,Team.sharded);var b=(ConveyorBuild)world.build(16,16);b.items.add(Items.copper,2);b.len=1;b.ids[0]=Items.copper;
        var s=capture();assertFalse(s.stocksComplete);assertFalse(s.complete);assertTrue(s.issues.contains(i->i.code.equals("conveyor-mismatch")));assertEquals(2,b.items.total());assertEquals(1,b.len);
    }
    @Test void distinctAndSharedLiquidModulesAreAccountedOnce(){
        arena(source);world.tile(20,20).setBlock(Blocks.liquidTank,Team.sharded);world.tile(30,20).setBlock(Blocks.liquidTank,Team.sharded);
        var a=world.build(20,20);var b=world.build(30,20);a.liquids.add(Liquids.oil,77);b.liquids=a.liquids;
        var s=capture();assertEquals(77,s.liquidTotals[Liquids.oil.id]);assertEquals(1,s.liquidStores.size);assertEquals(2,s.liquidStores.first().members.size);s.liquidStores.first().amounts.set(0,900);assertEquals(77,a.liquids.get(Liquids.oil));
    }
    @Test void batteryEnergyDoesNotIncludeConsumerCoverage(){
        arena(source);world.tile(20,20).setBlock(Blocks.batteryLarge,Team.sharded);world.tile(40,20).setBlock(Blocks.batteryLarge,Team.sharded);world.tile(30,40).setBlock(Blocks.advancedLaunchPad,Team.sharded);
        var a=world.build(20,20);var b=world.build(40,20);var consumer=world.build(30,40);a.power.status=.2f;b.power.status=.6f;consumer.power.status=1;
        var ga=new PowerGraph(true);ga.add(a);ga.add(consumer);var gb=new PowerGraph(true);gb.add(b);consumer.power.links.add(a.pos());a.power.links.add(consumer.pos());
        var s=capture();assertEquals(40000,s.energy,.01);assertEquals(2,s.circuits.size);assertTrue(s.links.contains(l->l.kind==Kind.power&&l.supported));assertEquals(0,s.powerStores.get(node(s,30,40).powerStore).capacity);
    }
    @Test void sharedBatteryModuleIsNotDuplicated(){
        arena(source);world.tile(20,20).setBlock(Blocks.batteryLarge,Team.sharded);world.tile(40,20).setBlock(Blocks.batteryLarge,Team.sharded);var a=world.build(20,20);var b=world.build(40,20);a.power.status=.5f;b.power=a.power;
        var s=capture();assertEquals(25000,s.energy);assertEquals(1,s.powerStores.size);assertEquals(2,s.powerStores.first().members.size);
    }
    @Test void stocksLiquidsAndProcessStateAreExactAndDetached(){
        arena(source);world.tile(20,20).setBlock(Blocks.graphitePress,Team.sharded);var b=(GenericCrafterBuild)world.build(20,20);b.items.add(Items.coal,6);b.progress=.4f;b.warmup=.7f;b.outputAccumulator[0]=.25f;
        var s=capture();Node n=node(s,20,20);assertEquals(6,s.itemTotals[Items.coal.id]);assertEquals(.4f,n.phase.get("progress",0));assertEquals(.25f,n.phase.get("outputRemainder0",0));
        Spec spec=s.specs.get(n.spec);assertEquals(Items.coal.id,spec.consumers.first().itemIds.first());assertEquals(2,spec.consumers.first().amounts.first());assertFalse(s.candidate);
        b.items.clear();b.progress=.9f;assertEquals(6,s.itemTotals[Items.coal.id]);assertEquals(.4f,n.phase.get("progress",0));
    }
    @Test void copperDrillAndFilteredUnloaderConfigurationAreCaptured(){
        arena(source);for(int x=20;x<22;x++)for(int y=20;y<22;y++)world.tile(x,y).setOverlay(Blocks.oreCopper);
        world.tile(20,20).setBlock(Blocks.mechanicalDrill,Team.sharded);var b=(DrillBuild)world.build(20,20);b.progress=123;b.warmup=.8f;
        var s=capture();assertEquals("copper",node(s,20,20).tags.get("ore"));assertEquals(b.dominantItems,node(s,20,20).phase.get("oreTiles",0));assertEquals(123,node(s,20,20).phase.get("progress",0));
        b.liquids.add(Liquids.water,1);assertTrue(capture().issues.contains(i->i.code.equals("boost")));assertEquals(1,b.liquids.get(Liquids.water));
        world.tile(40,40).setBlock(Blocks.unloader,Team.sharded);var u=(mindustry.world.blocks.storage.Unloader.UnloaderBuild)world.build(40,40);u.sortItem=Items.copper;u.unloadTimer=3;u.rotations=2;
        var filtered=node(capture(),40,40);assertEquals("copper",filtered.tags.get("config"));assertEquals(3,filtered.phase.get("unloadTimer",0));assertEquals(2,filtered.phase.get("cursor",0));
    }
    @Test void structuralRoutesAreDistinctFromPlainAdjacency(){
        arena(source);world.tile(9,6).setBlock(Blocks.landingPad,Team.sharded);var pad=(LandingPadBuild)world.build(9,6);pad.config=Items.copper;
        var s=capture();int a=s.nodes.indexOf(node(s,9,6),true),b=s.nodes.indexOf(node(s,6,6),true);
        assertTrue(s.links.contains(l->l.from==a&&l.to==b&&l.kind==Kind.items&&l.supported));assertTrue(s.links.contains(l->l.kind==Kind.adjacency&&!l.supported));
        assertFalse(s.links.contains(l->l.from==b&&l.to==a&&l.kind==Kind.items));
        var water=s.specs.get(node(s,9,6).spec).consumers.find(c->"water".equals(c.liquid));assertNotNull(water);assertTrue(water.known);assertFalse(water.continuous);assertEquals(1500,water.liquidAmount);assertEquals(0,water.liquidPerTick);
    }
    @Test void unsupportedEquipmentAndOpaqueStateHaveExplicitReasons(){
        arena(source);world.tile(20,20).setBlock(Blocks.microProcessor,Team.sharded);var s=capture();assertFalse(s.candidate);assertFalse(s.complete);assertTrue(s.issues.contains(i->i.blocking&&Blocks.microProcessor.name.equals(i.block)));assertFalse(node(s,20,20).stateDecoded);
    }
    @Test void simpleIdleCoreIsCandidateButNeverSimulated(){arena(source);state.rules.coreIncinerates=false;var s=capture();assertTrue(s.candidate);assertTrue(s.complete);assertEquals(0,s.itemTotals[Items.copper.id]);assertEquals(0,Team.sharded.core().items.total());}
    @Test void topologyDigestIgnoresQuantitiesButIncludesConfiguration(){
        arena(source);world.tile(20,20).setBlock(Blocks.landingPad,Team.sharded);var p=(LandingPadBuild)world.build(20,20);p.config=Items.copper;var a=capture();p.items.add(Items.copper,10);p.arrivingTimer=.4f;
        assertEquals(a.topology,capture().topology);p.config=Items.lead;assertNotEquals(a.topology,capture().topology);
    }
    @Test void automaticCheckpointAndLoadRetainOnlySessionObservations() throws Exception{
        arena(source);Team.sharded.core().items.add(Items.copper,77);save();assertEquals(Origin.checkpoint,IndustrialEconomyCapture.latest(source).origin);byte[] checkpoint=source.save.file.readBytes();
        arena(target);assertEquals(77,IndustrialEconomyCapture.latest(source).itemTotals[Items.copper.id]);assertNull(IndustrialEconomyCapture.latest(target));
        SaveIO.load(source.save.file,world.makeSectorContext(source));assertEquals(Origin.load,IndustrialEconomyCapture.latest(source).origin);assertArrayEquals(checkpoint,source.save.file.readBytes());
        Core.settings=new Settings();Core.settings.setDataDirectory(directory);assertNull(IndustrialEconomyCapture.latest(source));
    }
    @Test void observationsNeverModifyWorldSettingsJournalOrStocks(){
        arena(source);world.tile(20,20).setBlock(Blocks.graphitePress,Team.sharded);var b=(GenericCrafterBuild)world.build(20,20);b.items.add(Items.coal,10);b.progress=.2f;save();Core.settings.forceSave();
        byte[] file=source.save.file.readBytes(),settings=Core.settings.getSettingsFile().readBytes(),journal=directory.child("saves/industrial-transports-v1.bin").readBytes();String info=JsonIO.write(source.info);
        for(int i=0;i<20;i++){capture();IndustrialEconomyCapture.latest(source);}
        assertArrayEquals(file,source.save.file.readBytes());assertArrayEquals(settings,Core.settings.getSettingsFile().readBytes());assertArrayEquals(journal,directory.child("saves/industrial-transports-v1.bin").readBytes());assertEquals(info,JsonIO.write(source.info));assertEquals(10,b.items.get(Items.coal));assertEquals(.2f,b.progress);assertFalse(Core.settings.modified());
    }
    @Test void classicAndFreeNeverCaptureOrCreateGraphState(){
        for(var p:new Planet[]{Planets.serpulo,Planets.industrialSerpulo}){var s=p.getStartSector();base(s);arena(s);assertFalse(IndustrialEconomyCapture.enabled(p));assertNull(IndustrialEconomyCapture.observe(Origin.manual));save();assertNull(IndustrialEconomyCapture.latest(s));}
        assertEquals(0,IndustrialEconomyCapture.cachedCount());
    }
    @Test void legacyM7BFixtureRemainsByteIdentical() throws Exception{
        try(var in=getClass().getResourceAsStream("/industrial/m7b-sector170.msav")){source.save.file.writeBytes(in.readAllBytes());}byte[] before=source.save.file.readBytes();source.info.items.set(Items.copper,2650);
        SaveIO.load(source.save.file,world.makeSectorContext(source));var s=IndustrialEconomyCapture.latest(source);assertNotNull(s);assertEquals(2650,Team.sharded.core().items.get(Items.copper));assertEquals(2660,s.itemTotals[Items.copper.id]);assertArrayEquals(before,source.save.file.readBytes());
    }
    @Test void manifestReferencesAreDetachedAndCreditIsNotAddedToPhysicalStock(){
        arena(target);world.tile(20,20).setBlock(Blocks.landingPad,Team.sharded);var receiver=(LandingPadBuild)world.build(20,20);receiver.config=Items.copper;receiver.liquids.add(Liquids.water,3000);save();
        arena(source);source.info.destination=target;world.tile(20,20).setBlock(Blocks.advancedLaunchPad,Team.sharded);var launch=(LaunchPadBuild)world.build(20,20);launch.items.add(Items.copper,100);assertTrue(IndustrialTransports.launch(launch));save();
        var a=IndustrialEconomyCapture.latest(source);assertEquals(0,a.itemTotals[Items.copper.id]);assertEquals(100,a.manifests.first().amounts[0]);a.manifests.first().amounts[0]=999;assertEquals(100,IndustrialTransports.inspectManifests(source).first().amounts[0]);
        SaveIO.load(target.save.file,world.makeSectorContext(target));state.set(State.playing);receiver=(LandingPadBuild)world.build(20,20);receiver.efficiency=1;IndustrialTransports.receive(receiver);for(int i=0;i<160;i++)receiver.updateTile();
        var b=capture();assertEquals(100,b.itemTotals[Items.copper.id]);assertEquals("world-receipt",b.manifests.first().ownership);assertEquals(100,b.manifests.first().amounts[0]);assertEquals(100,receiver.items.total());
    }
    @Test void snapshotContainsNoNativeReferences() throws Exception{
        arena(source);world.tile(20,20).setBlock(Blocks.liquidTank,Team.sharded);world.build(20,20).liquids.add(Liquids.oil,40);var s=capture();
        for(var type:new Class<?>[]{Snapshot.class,Node.class,Spec.class,ItemStore.class,LiquidStore.class,PowerStore.class,Circuit.class,Link.class,LooseCargo.class,Issue.class,Consumer.class,IndustrialTransports.ManifestView.class}){
            for(Field field:type.getFields())assertFalse(mindustry.gen.Building.class.isAssignableFrom(field.getType())||mindustry.type.Sector.class.isAssignableFrom(field.getType())||mindustry.world.Block.class.isAssignableFrom(field.getType())||field.getType().getName().startsWith("mindustry.world.modules."),field.toString());
        }
        String json=JsonIO.write(s);var copy=JsonIO.read(Snapshot.class,json);assertEquals(s.topology,copy.topology);assertEquals(40,copy.liquidTotals[Liquids.oil.id]);assertEquals(s.nodes.size,copy.nodes.size);
    }
    @Test void sessionCacheIsBoundedAndEvictionNeverChangesAWorld(){
        arena(source);byte[] settings=Core.settings.getSettingsFile().readBytes();for(int i=0;i<105;i++){state.rules.sector=strict.sectors.get(i);capture();}
        assertEquals(100,IndustrialEconomyCapture.cachedCount());assertNull(IndustrialEconomyCapture.latest(strict.sectors.first()));assertArrayEquals(settings,Core.settings.getSettingsFile().readBytes());assertEquals(0,Team.sharded.core().items.total());
    }
    @Test void nativeRulesAreObservedAndNeverSilentlyCorrected(){
        arena(source);assertTrue(state.rules.coreIncinerates);var s=capture();assertTrue(s.coreIncinerates);assertFalse(s.candidate);assertTrue(s.issues.contains(i->i.code.equals("incineration")));assertTrue(state.rules.coreIncinerates);
    }
    @Test void unitCargoAndMiningAreReportedWithoutCreatingOutput(){
        arena(source);var unit=UnitTypes.alpha.create(Team.sharded);unit.stack=new ItemStack(Items.lead,5);unit.mineTile=world.tile(12,12);unit.mineTimer=12;unit.add();
        var foreign=UnitTypes.alpha.create(Team.crux);foreign.stack=new ItemStack(Items.lead,99);foreign.add();
        var s=capture();assertEquals(5,s.itemTotals[Items.lead.id]);assertTrue(s.issues.contains(i->i.code.equals("unit-mining")));assertTrue(s.loose.contains(a->a.kind.equals("unit-mining")&&a.progress==12));assertEquals(5,unit.stack.amount);assertEquals(12,unit.mineTimer);
    }
    @Test void captureLimitIsExplicitAndCannotBeMisreadAsACompleteInventory(){
        logic.reset();world.loadGenerator(160,160,tiles->{tiles.fill();for(var tile:tiles)tile.setFloor(Blocks.stone.asFloor());tiles.get(6,6).setBlock(Blocks.coreShard,Team.sharded);int count=0;for(int x=12;x<150&&count<10001;x++)for(int y=12;y<150&&count<10001;y++){tiles.get(x,y).setBlock(Blocks.copperWall,Team.sharded);count++;}});
        state.rules.sector=source;strict.applyRules(state.rules);state.set(State.playing);var s=capture();assertEquals(10002,s.nativeBuildings);assertEquals(10000,s.nodes.size);assertFalse(s.complete);assertFalse(s.stocksComplete);assertFalse(s.candidate);assertTrue(s.issues.contains(i->i.code.equals("limit")));assertEquals(10002,Team.sharded.data().buildings.size);
    }
    @Test void measureFiveTwentyFiftyHundredSyntheticGraphs(){
        arena(source);for(int x=12;x<62;x+=2)for(int y=12;y<28;y+=2)world.tile(x,y).setBlock(Blocks.graphitePress,Team.sharded);
        for(var b:Team.sharded.data().buildings)if(b instanceof GenericCrafterBuild c){c.items.add(Items.coal,3);c.progress=.37f;c.warmup=.6f;}
        for(int i=0;i<10;i++)capture();IndustrialEconomyCapture.forget();
        var mx=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();long thread=Thread.currentThread().getId();
        for(int count:new int[]{5,20,50,100}){
            IndustrialEconomyCapture.forget();System.gc();long heapBefore=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(),allocated=mx.getThreadAllocatedBytes(thread),cpuStart=mx.getCurrentThreadCpuTime(),start=System.nanoTime();
            for(int i=0;i<count;i++){state.rules.sector=strict.sectors.get(i);capture();}
            long nanos=System.nanoTime()-start,cpuNanos=mx.getCurrentThreadCpuTime()-cpuStart,allocatedBytes=mx.getThreadAllocatedBytes(thread)-allocated;System.gc();long heapAfter=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();long encoded=0;
            for(int i=0;i<count;i++)encoded+=JsonIO.write(IndustrialEconomyCapture.latest(strict.sectors.get(i))).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            assertEquals(count,IndustrialEconomyCapture.cachedCount());assertTrue(IndustrialEconomyCapture.cachedNodes()<=IndustrialEconomyCapture.maxCachedNodes);
            Log.info("M7DB_PERF server sectors=@ nodes-per-sector=@ links-per-sector=@ capture-total-ms=@ average-ms=@ cpu-total-ms=@ allocated-bytes=@ heap-delta-after-gc=@ json-bytes=@",count,IndustrialEconomyCapture.latest(strict.sectors.first()).nodes.size,IndustrialEconomyCapture.latest(strict.sectors.first()).links.size,nanos/1e6,nanos/(count*1e6),cpuNanos/1e6,allocatedBytes,heapAfter-heapBefore,encoded);
        }
    }
}
