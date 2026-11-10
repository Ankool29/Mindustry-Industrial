import arc.*;
import arc.assets.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.game.IndustrialTransports.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.blocks.campaign.LaunchPad.*;
import mindustry.world.blocks.campaign.LandingPad.*;
import org.junit.jupiter.api.*;
import java.io.*;
import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Uses isolated canonical worlds and a separate durable journal, never player data. */
class IndustrialTransportTests{
    Settings previous;Fi directory;Saves saves;Planet strict;Sector source,target,third;
    ObjectMap<Sector,SectorInfo> infos=new ObjectMap<>();ObjectMap<Sector,Saves.SaveSlot> slots=new ObjectMap<>();
    @BeforeAll static void init(){ApplicationTests.launchApplication();}
    @BeforeEach void setup(){
        logic.reset();Time.setDeltaProvider(()->1f);previous=Core.settings;directory=previous.getDataDirectory().child("m7c-test");directory.deleteDirectory();directory.mkdirs();settings();
        if(Core.assets==null)Core.assets=new AssetManager();saves=new Saves();strict=Planets.industrialSerpuloProgressive;
        for(var p:new Planet[]{strict,Planets.industrialSerpulo,Planets.serpulo})for(var s:p.sectors){infos.put(s,s.info);slots.put(s,s.save);s.info=new SectorInfo();s.save=null;IndustrialCampaign.prepareInfo(s);}
        source=strict.getStartSector();target=source.near().first();third=strict.sectors.get(15);base(source);base(target);base(third);source.info.destination=target;
    }
    @AfterEach void cleanup(){logic.reset();infos.each((s,i)->s.info=i);slots.each((s,i)->s.save=i);Core.settings=previous;directory.deleteDirectory();}
    void settings(){Core.settings=new Settings();Core.settings.setJson(JsonIO.json);Core.settings.setDataDirectory(directory);if(!Core.settings.getSettingsFile().exists()){Core.settings.put("m7c-fixture",true);Core.settings.forceSave();}Core.settings.load();}
    void base(Sector s){s.info.hasCore=true;s.info.storageCapacity=4000;s.info.waves=s.info.attack=false;s.save=saves.new SaveSlot(directory.child(s.planet.name+s.id+".msav"));}
    void arena(Sector s){logic.reset();world.loadGenerator(48,48,tiles->{tiles.fill();for(var tile:tiles)tile.setFloor(Blocks.stone.asFloor());tiles.get(6,6).setBlock(Blocks.coreShard,Team.sharded);});state.rules.sector=s;s.planet.applyRules(state.rules);state.set(State.playing);IndustrialTransports.generated(s);}
    LaunchPadBuild ramp(int x){world.tile(x,18).setBlock(Blocks.advancedLaunchPad,Team.sharded);var p=(LaunchPadBuild)world.tile(x,18).build;p.launchCounter=1800;return p;}
    LandingPadBuild landing(){world.tile(30,30).setBlock(Blocks.landingPad,Team.sharded);var p=(LandingPadBuild)world.tile(30,30).build;p.config=Items.copper;p.liquids.add(Liquids.water,3000);p.efficiency=1;return p;}
    void save(){SaveIO.save(state.getSector().save.file);}
    void load(Sector s){SaveIO.load(s.save.file,world.makeSectorContext(s));state.set(State.playing);}
    Book book() throws Exception{try(var in=new DataInputStream(directory.child("saves/industrial-transports-v1.bin").read())){assertEquals(0x49544331,in.readInt());int n=in.readInt();in.readLong();byte[] b=new byte[n];in.readFully(b);return JsonIO.read(Book.class,new String(b,java.nio.charset.StandardCharsets.UTF_8));}}
    void send(){arena(source);var p=ramp(18);p.items.add(Items.copper,100);assertTrue(IndustrialTransports.launch(p));assertEquals(0,p.items.total());save();}
    void ready(){arena(target);landing();save();}
    LandingPadBuild receiver(){return (LandingPadBuild)world.build(30,30);}
    void begin(){receiver().efficiency=1;IndustrialTransports.receive(receiver());}
    void finish(){receiver().efficiency=1;for(int i=0;i<160;i++){state.updateId++;receiver().updateTile();}}
    @Test void checkpointDebitCreditAndRepeatedRecoveryExactlyOnce() throws Exception{
        ready();send();assertEquals(Phase.transit,book().pending.first().phase);String id=book().pending.first().id;load(target);begin();assertEquals(id,receiver().transportId);finish();assertEquals(100,receiver().items.total());assertEquals(Phase.credit,book().pending.first().phase);save();assertTrue(book().pending.isEmpty());
        for(int i=0;i<3;i++){settings();load(target);assertEquals(100,receiver().items.total());}
        assertEquals(100,book().totals.find(t->t.sector==source.id).sent);assertEquals(100,book().totals.find(t->t.sector==target.id).received);
    }
    @Test void insufficientEmptyAndInvalidDestinationsKeepStocks(){arena(source);var p=ramp(18);for(int n:new int[]{0,1,99}){p.items.set(Items.copper,n);assertFalse(IndustrialTransports.launch(p));assertEquals(n,p.items.total());}p.items.set(Items.copper,100);source.info.destination=null;assertFalse(IndustrialTransports.launch(p));assertEquals(100,p.items.total());source.info.destination=source;assertFalse(IndustrialTransports.launch(p));}
    @Test void multipleRampsDistinctIdsOneDebit() throws Exception{arena(source);for(int x:new int[]{18,25}){var p=ramp(x);p.items.add(Items.copper,100);assertTrue(IndustrialTransports.launch(p));assertFalse(IndustrialTransports.launch(p));}save();assertEquals(2,book().pending.size);assertNotEquals(book().pending.first().id,book().pending.peek().id);assertEquals(200,book().pending.sumf(Shipment::total));}
    @Test void fullDestinationWaitsWithoutOverflow() throws Exception{arena(target);landing().items.add(Items.copper,100);save();send();arena(third);IndustrialTransports.turn(strict);assertEquals(Phase.transit,book().pending.first().phase);load(target);begin();assertNull(receiver().transportId);assertEquals(100,receiver().items.total());assertEquals(100,book().pending.first().total());}
    @Test void inactiveFiniteOverlayDoesNotCreditCoreOrRepeat() throws Exception{ready();send();arena(third);IndustrialTransports.turn(strict);assertEquals(Phase.overlay,book().pending.first().phase);for(int i=0;i<10;i++)IndustrialTransports.turn(strict);assertEquals(100,book().pending.first().total());assertEquals(0,target.info.items.get(Items.copper));settings();load(target);assertEquals(100,receiver().items.total());assertEquals(1500,receiver().liquids.get(Liquids.water),.01f);String description=IndustrialTransports.describe(target);assertTrue(description.contains("En transit / attente de tampon : 0"));assertTrue(description.contains("Crédit en attente de sauvegarde : 100"));assertTrue(description.contains("tampon appliqué"));save();assertTrue(book().pending.isEmpty());settings();load(target);assertEquals(100,receiver().items.total());}
    @Test void uncheckpointedDebitRestoresBufferWithoutAnotherShipment() throws Exception{arena(source);var p=ramp(18);p.items.add(Items.copper,100);save();assertTrue(IndustrialTransports.launch(p));assertEquals(Phase.prepared,book().pending.first().phase);settings();load(source);assertEquals(100,world.build(18,18).items.total());assertTrue(book().pending.isEmpty());}
    @Test void restartBeforeCreditRetainsOneManifest() throws Exception{ready();send();settings();load(target);begin();assertNotNull(receiver().transportId);settings();load(target);assertEquals(0,receiver().items.total());begin();finish();save();assertEquals(100,receiver().items.total());assertTrue(book().pending.isEmpty());}
    @Test void restartAfterUnsavedCreditReplaysOnlyAgainstUncreditedWorld() throws Exception{ready();send();load(target);begin();finish();assertEquals(100,receiver().items.total());settings();load(target);assertEquals(0,receiver().items.total());begin();finish();save();assertEquals(100,receiver().items.total());}
    @Test void savedAnimationSurvivesSectorChange() throws Exception{ready();send();load(target);begin();save();arena(third);settings();load(target);assertNotNull(receiver().transportId);finish();save();assertEquals(100,receiver().items.total());assertTrue(book().pending.isEmpty());}
    @Test void insufficientWaterAndAbsentReceiverKeepGoods() throws Exception{arena(target);landing().liquids.clear();save();send();arena(third);IndustrialTransports.turn(strict);assertEquals(Phase.transit,book().pending.first().phase);load(target);world.tile(30,30).remove();save();arena(third);IndustrialTransports.turn(strict);assertEquals(100,book().pending.first().total());}
    @Test void mismatchedOverlayBaselineStopsWithoutDeletingWorld() throws Exception{ready();send();arena(third);IndustrialTransports.turn(strict);SaveIO.load(new java.util.zip.InflaterInputStream(target.save.file.read()),world.makeSectorContext(target));state.set(State.playing);world.tile(30,30).remove();SaveIO.write(target.save.file);settings();assertThrows(TransportException.class,()->load(target));assertTrue(target.save.file.exists());}
    @Test void corruptedJournalNeverFallsBackOrDeletesWorld() throws Exception{send();Fi f=directory.child("saves/industrial-transports-v1.bin");byte[] bytes=f.readBytes();bytes[bytes.length-1]^=1;f.writeBytes(bytes);settings();assertThrows(TransportException.class,()->load(source));assertTrue(source.save.file.exists());}
    @Test void rolledBackSourceCheckpointIsBlocked() throws Exception{arena(source);var p=ramp(18);p.items.add(Items.copper,100);save();byte[] old=source.save.file.readBytes();assertTrue(IndustrialTransports.launch(p));save();source.save.file.writeBytes(old);settings();assertThrows(TransportException.class,()->load(source));assertTrue(source.save.file.exists());}
    @Test void oldFiniteFlightsAdoptedOnce() throws Exception{arena(source);var stacks=Seq.with(new ItemStack(Items.copper,100));assertTrue(IndustrialTransports.adopt(stacks));assertTrue(stacks.isEmpty());assertTrue(IndustrialTransports.adopt(stacks));save();assertEquals(1,book().pending.size);}
    @Test void unverifiedM7BArrivalSuspendedWithoutGoodsOrWaterConsumption(){arena(target);landing().arriving=Items.copper;receiver().arrivingTimer=.9f;finish();assertEquals(0,receiver().items.total());assertEquals(3000,receiver().liquids.get(Liquids.water));assertNotNull(receiver().arriving);}
    @Test void classicAndFreeRetainNativeStatisticalReception(){for(var planet:new Planet[]{Planets.serpulo,Planets.industrialSerpulo}){var a=planet.getStartSector();var b=a.near().first();base(a);base(b);a.info.destination=b;var rate=new SectorInfo.ExportStat();rate.mean=2;a.info.export.put(Items.copper,rate);arena(b);landing().liquids.clear();receiver().liquids.add(Liquids.water,1500);for(int i=0;i<3500;i++){state.updateId++;receiver().efficiency=1;receiver().updateTile();}assertEquals(100,receiver().items.total());assertFalse(IndustrialTransports.enabled(planet));}}
    @Test void m7BPhantomReceptionRegression(){arena(target);var rate=new SectorInfo.ExportStat();rate.mean=2;source.info.export.put(Items.copper,rate);landing();for(int i=0;i<3500;i++){state.updateId++;receiver().efficiency=1;receiver().updateTile();}assertEquals(0,source.items().get(Items.copper));assertEquals(0,receiver().items.total());assertEquals(3000,receiver().liquids.get(Liquids.water));}
    @Test void departureDestinationIsFrozen() throws Exception{send();source.info.destination=third;assertEquals(target.id,book().pending.first().destination);}
    @Test void crashAfterWorldDebitWriteBeforeJournalCommitRecoversOnce() throws Exception{
        arena(source);var p=ramp(18);p.items.add(Items.copper,100);save();assertTrue(IndustrialTransports.launch(p));
        SaveIO.write(source.save.file);settings();load(source);assertEquals(0,world.build(18,18).items.total());assertEquals(Phase.transit,book().pending.first().phase);assertEquals(100,book().totals.find(t->t.sector==source.id).sent);
    }
    @Test void crashAfterWorldCreditWriteBeforeJournalCommitRecoversOnce() throws Exception{
        ready();send();load(target);begin();finish();SaveIO.write(target.save.file);settings();load(target);assertEquals(100,receiver().items.total());assertTrue(book().pending.isEmpty());assertEquals(100,book().totals.find(t->t.sector==target.id).received);
    }
    @Test void interruptedJournalReplacementIsSignalledAndPreserved() throws Exception{
        send();Fi f=directory.child("saves/industrial-transports-v1.bin.new");f.writeString("partial");settings();assertThrows(TransportException.class,()->load(source));assertTrue(f.exists());assertTrue(source.save.file.exists());
    }
    @Test void noAdditionalNativeLegacyExportPass() throws Exception{
        ready();send();arena(third);boolean old=strict.campaignRules.legacyLaunchPads;strict.campaignRules.legacyLaunchPads=true;
        try{var rate=new SectorInfo.ExportStat();rate.mean=50;source.info.export.put(Items.copper,rate);universe.runTurn();assertEquals(0,target.info.items.get(Items.copper));assertEquals(100,book().pending.first().total());}
        finally{strict.campaignRules.legacyLaunchPads=old;}
    }
    @Test void conservationAcrossTwoRampsOneFullReceiverAndRevisit() throws Exception{
        ready();arena(source);for(int x:new int[]{18,25}){var p=ramp(x);p.items.add(Items.copper,100);assertTrue(IndustrialTransports.launch(p));}save();arena(third);IndustrialTransports.turn(strict);
        load(target);assertEquals(100,receiver().items.total());assertEquals(100,book().pending.find(t->t.phase==Phase.transit).total());
        // Draining into the native core represents the local conveyor path; no new supply is introduced.
        receiver().items.clear();Team.sharded.core().items.add(Items.copper,100);receiver().cooldown=0;save();begin();finish();save();
        assertEquals(200,Team.sharded.core().items.get(Items.copper)+receiver().items.total());assertTrue(book().pending.isEmpty());assertEquals(200,book().totals.find(t->t.sector==source.id).sent);assertEquals(200,book().totals.find(t->t.sector==target.id).received);
    }
    @Test void measuredIdleAggregateCostFiveTwentyFiftyHundredColonies(){
        for(int n:new int[]{5,20,50,100}){
            for(int i=0;i<n;i++){var s=strict.sectors.get(i);base(s);arena(s);landing();save();}
            arena(third);for(int i=0;i<50;i++)IndustrialTransports.turn(strict);
            var mx=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long thread=Thread.currentThread().getId();long allocated=mx.getThreadAllocatedBytes(thread);
            long start=System.nanoTime();for(int i=0;i<1000;i++)IndustrialTransports.turn(strict);long nanos=System.nanoTime()-start;
            long bytes=directory.child("saves/industrial-transports-v1.bin").length();
            Log.info("M7C_PERF server colonies=@ turn-average-us=@ journal-bytes=@ allocated-bytes-per-turn=@",n,nanos/1000000f,bytes,(mx.getThreadAllocatedBytes(thread)-allocated)/1000f);
        }
    }
    @Test void genuineM7BFixtureLoadsWithoutConversionOrStockMutation() throws Exception{
        try(var in=getClass().getResourceAsStream("/industrial/m7b-sector170.msav")){assertNotNull(in);source.save.file.writeBytes(in.readAllBytes());}
        byte[] original=source.save.file.readBytes();source.info.items.set(Items.copper,2650);load(source);
        assertEquals(2650,Team.sharded.core().items.get(Items.copper));assertArrayEquals(original,source.save.file.readBytes());
        assertTrue(state.rules.industrial);assertFalse(state.rules.waves);assertEquals(0,Long.parseLong(state.rules.tags.get("industrial-transport-out-v1","0")));
    }
    @Test void explicitRecolonizationKeepsManifestButRejectsOldWorldGeneration() throws Exception{
        ready();send();byte[] old=source.save.file.readBytes();arena(source);ramp(18);save();assertEquals(1,book().pending.size);
        source.save.file.writeBytes(old);settings();assertThrows(TransportException.class,()->load(source));assertTrue(source.save.file.exists());
    }
    @Test void nativeLaunchPathEmitsVisualButNoSecondNativeCredit() throws Exception{
        arena(source);var p=ramp(18);p.items.add(Items.copper,100);p.updateTile();assertEquals(0,p.items.total());assertEquals(1,book().pending.size);
        for(int i=0;i<130;i++)Groups.all.update();assertEquals(0,target.info.items.get(Items.copper));assertEquals(1,book().pending.size);save();assertEquals(100,book().totals.find(t->t.sector==source.id).sent);
    }
    @Test void missingJournalWithPersistentIdentityBlocksEvenAtUncreditedDestination() throws Exception{
        ready();send();directory.child("saves/industrial-transports-v1.bin").delete();settings();assertThrows(TransportException.class,()->load(target));assertTrue(target.save.file.exists());
    }
    @Test void boundedBacklogKeepsTheNextBufferAndMeasuresDurableWriteCost() throws Exception{
        arena(source);var p=ramp(18);long start=System.nanoTime();
        for(int i=0;i<512;i++){p.items.set(Items.copper,100);assertTrue(IndustrialTransports.launch(p));}
        long nanos=System.nanoTime()-start;p.items.set(Items.copper,100);assertFalse(IndustrialTransports.launch(p));assertEquals(100,p.items.total());save();
        assertEquals(512,book().pending.size);assertEquals(51200,book().totals.find(t->t.sector==source.id).sent);
        Log.info("M7C_PERF server backlog=512 durable-admission-average-ms=@ journal-bytes=@",nanos/512000000f,directory.child("saves/industrial-transports-v1.bin").length());
    }
    @Test void saveSlotPreservesTransportExceptionInsteadOfCorruptionDeletionPath() throws Exception{
        send();directory.child("saves/industrial-transports-v1.bin").delete();settings();
        assertThrows(TransportException.class,()->source.save.load(world.makeSectorContext(source)));assertTrue(source.save.file.exists());
    }
    @Test void nativeEconomicClockReservesABWhilePlayerStaysInCWithoutLoadingB() throws Exception{
        ready();byte[] untouched=target.save.file.readBytes();send();arena(third);
        Universe previous=universe;universe=new Universe();
        try{
            for(int i=0;i<7199;i++){Time.update();universe.update();}
            assertEquals(Phase.transit,book().pending.first().phase);
            Time.update();universe.update();
            assertEquals(Phase.overlay,book().pending.first().phase);assertEquals(100,book().pending.first().total());
            assertSame(third,state.getSector());assertEquals(0,target.info.items.get(Items.copper));assertArrayEquals(untouched,target.save.file.readBytes());
            Log.info("M7C1_REPRO A->B from C: overlay=100, B core=0, B world unchanged, active=C");
        }finally{universe=previous;}
    }
    @Test void savedPartiallyArrivedFiniteManifestCompletesVirtuallyWhileBIsUnloaded() throws Exception{
        arena(third);save();ready();send();load(target);begin();for(int i=0;i<20;i++)receiver().updateTile();
        float alreadyPaid=receiver().liquidRemoved;assertTrue(alreadyPaid>0);save();byte[] untouched=target.save.file.readBytes();
        load(third);assertEquals(Phase.arriving,book().pending.first().phase);universe.runTurn();
        assertEquals(Phase.overlay,book().pending.first().phase,"Known finite arrival must not stay arriving forever offscreen");
        assertSame(third,state.getSector());assertArrayEquals(untouched,target.save.file.readBytes());
        load(target);assertEquals(100,receiver().items.total());assertEquals(1500,receiver().liquids.get(Liquids.water),.02f);
        assertNull(receiver().arriving);assertNull(receiver().transportId);save();settings();load(target);assertEquals(100,receiver().items.total());
    }
    void writeBook(Book value) throws Exception{
        writeBookJson(JsonIO.write(value));
    }
    void writeBookJson(String json) throws Exception{
        byte[] bytes=json.getBytes(java.nio.charset.StandardCharsets.UTF_8);var crc=new java.util.zip.CRC32();crc.update(bytes);
        try(var out=new DataOutputStream(directory.child("saves/industrial-transports-v1.bin").write(false))){out.writeInt(0x49544331);out.writeInt(bytes.length);out.writeLong(crc.getValue());out.write(bytes);}
    }
    void legacyM7CJournal() throws Exception{
        var root=new arc.util.serialization.JsonReader().parse(JsonIO.write(book()));
        for(var s=root.get("pending").child;s!=null;s=s.next){s.remove("waterCost");s.remove("resumedArrival");}
        for(var c=root.get("checkpoints").child;c!=null;c=c.next){
            var receivers=c.get("receivers");if(receivers==null)continue;
            for(var r=receivers.child;r!=null;r=r.next){r.remove("arrivalId");r.remove("arrivalItem");r.remove("paidWater");}
        }
        writeBookJson(root.toJson(arc.util.serialization.JsonWriter.OutputType.json));
    }
    @Test void virtualBufferIsVisibleAndReadOnlyBeforeAnyReturnToB() throws Exception{
        ready();send();arena(third);universe.runTurn();byte[] untouched=target.save.file.readBytes();byte[] journal=directory.child("saves/industrial-transports-v1.bin").readBytes();
        for(int i=0;i<5;i++){
            String summary=IndustrialTransports.describe(target);assertTrue(summary.contains("Réservés hors écran : 100"));assertTrue(summary.contains("En transit / attente de tampon : 0"));assertTrue(summary.contains("Reçus confirmés : 0"));IndustrialDiagnostics.read(strict);
        }
        var r=book().checkpoints.find(c->c.sector==target.id).receivers.first();assertEquals(100,r.amount);assertEquals(1500,r.water,.01f);
        assertEquals(0,target.info.items.get(Items.copper));assertArrayEquals(untouched,target.save.file.readBytes());assertArrayEquals(journal,directory.child("saves/industrial-transports-v1.bin").readBytes());assertSame(third,state.getSector());
    }
    @Test void migratedDestinationWithoutM7CCheckpointWaitsWithExplicitReason() throws Exception{
        ready();var b=book();b.checkpoints.removeAll(c->c.sector==target.id);writeBook(b);settings();byte[] untouched=target.save.file.readBytes();
        send();arena(third);for(int i=0;i<3;i++)universe.runTurn();assertEquals(Phase.transit,book().pending.first().phase);assertTrue(IndustrialTransports.describe(target).contains("destination non relevée depuis M7C"));assertArrayEquals(untouched,target.save.file.readBytes());assertEquals(100,book().pending.first().total());
    }
    @Test void knownArrivalSurvivesRestartInCAndUsesRemainingWaterExactlyOnce() throws Exception{
        arena(third);save();ready();send();load(target);begin();for(int i=0;i<20;i++)receiver().updateTile();save();String id=book().pending.first().id;load(third);settings();load(third);
        assertEquals(Phase.arriving,book().pending.first().phase);universe.runTurn();assertEquals(Phase.overlay,book().pending.first().phase);assertEquals(id,book().pending.first().id);assertEquals(1300,book().pending.first().waterCost,.01f);
        for(int i=0;i<5;i++)universe.runTurn();settings();load(third);universe.runTurn();assertEquals(1,book().pending.size);assertSame(third,state.getSector());
        load(target);assertEquals(100,receiver().items.total());assertEquals(1500,receiver().liquids.get(Liquids.water),.02f);save();settings();load(target);assertEquals(100,receiver().items.total());assertTrue(book().pending.isEmpty());
    }
    @Test void legacyM7CNormalOverlayStillAppliesWithoutNewFields() throws Exception{
        ready();send();arena(third);universe.runTurn();String id=book().pending.first().id;legacyM7CJournal();settings();load(target);
        assertEquals(100,receiver().items.total());assertEquals(1500,receiver().liquids.get(Liquids.water),.01f);assertEquals(id,book().pending.first().id);save();assertTrue(book().pending.isEmpty());
    }
    @Test void legacyM7CArrivalRequiresOneFreshCheckpointBeforeOfflineResume() throws Exception{
        arena(third);save();ready();send();load(target);begin();for(int i=0;i<20;i++)receiver().updateTile();save();legacyM7CJournal();settings();load(third);
        universe.runTurn();assertEquals(Phase.transit,book().pending.first().phase);assertTrue(IndustrialTransports.describe(target).contains("ancien relevé"));
        load(target);save();load(third);universe.runTurn();assertEquals(Phase.overlay,book().pending.first().phase);load(target);assertEquals(100,receiver().items.total());assertEquals(1500,receiver().liquids.get(Liquids.water),.02f);save();assertTrue(book().pending.isEmpty());
    }
    @Test void insufficientRemainingWaterBlocksOfflineArrivalWithoutInventingSupply() throws Exception{
        arena(third);save();ready();send();load(target);begin();for(int i=0;i<20;i++)receiver().updateTile();receiver().liquids.set(Liquids.water,100);save();byte[] untouched=target.save.file.readBytes();load(third);universe.runTurn();
        assertEquals(Phase.arriving,book().pending.first().phase);assertEquals(100,book().pending.first().total());assertTrue(IndustrialTransports.describe(target).contains("eau insuffisante pour terminer"));assertArrayEquals(untouched,target.save.file.readBytes());
    }
    @Test void changedSavedArrivalBindingIsQuarantinedBeforeCreditingAnyCargo() throws Exception{
        arena(third);save();ready();send();load(target);begin();receiver().updateTile();save();load(third);universe.runTurn();
        SaveIO.load(new java.util.zip.InflaterInputStream(target.save.file.read()),world.makeSectorContext(target));receiver().transportId="unrelated";SaveIO.write(target.save.file);settings();assertThrows(TransportException.class,()->load(target));assertTrue(target.save.file.exists());assertEquals(0,receiver().items.total());
    }
    @Test void twoSavedArrivalsReserveDistinctBuffersAndNeverDoubleSpendWater() throws Exception{
        arena(third);save();ready();world.tile(38,30).setBlock(Blocks.landingPad,Team.sharded);var second=(LandingPadBuild)world.build(38,30);second.config=Items.copper;second.liquids.add(Liquids.water,3000);save();
        arena(source);for(int x:new int[]{18,25}){var p=ramp(x);p.items.add(Items.copper,100);assertTrue(IndustrialTransports.launch(p));}save();load(target);second=(LandingPadBuild)world.build(38,30);second.efficiency=1;IndustrialTransports.receive(second);begin();
        for(int i=0;i<20;i++){receiver().updateTile();second.updateTile();}save();load(third);universe.runTurn();assertEquals(2,book().pending.count(s->s.phase==Phase.overlay));
        load(target);second=(LandingPadBuild)world.build(38,30);assertEquals(200,receiver().items.total()+second.items.total());assertEquals(1500,receiver().liquids.get(Liquids.water),.02f);assertEquals(1500,second.liquids.get(Liquids.water),.02f);save();assertTrue(book().pending.isEmpty());assertEquals(200,book().totals.find(t->t.sector==target.id).received);
    }
    @Test void diagnosticNeverMutatesJournal() throws Exception{send();Fi f=directory.child("saves/industrial-transports-v1.bin");byte[] old=f.readBytes();for(int i=0;i<20;i++)IndustrialDiagnostics.read(strict);assertArrayEquals(old,f.readBytes());assertEquals(0,world.build(18,18).items.total());}
}
