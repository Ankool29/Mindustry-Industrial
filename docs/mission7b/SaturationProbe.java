import arc.*;
import arc.assets.*;
import arc.backend.headless.*;
import arc.files.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindustry.type.*;
import static mindustry.Vars.*;

/** Reproducible full-core native protocol in a separate temporary data directory. */
public class SaturationProbe{
 static Fi root=new Fi("/workspace/audit-sources/mission7b/saturation-data");static Saves slots;
 static void base(Sector s){s.save=slots.new SaveSlot(root.child(s.id+".msav"));s.info=new SectorInfo();s.info.hasCore=true;s.info.waves=s.info.attack=false;s.info.storageCapacity=4000;}
 static void arena(Sector s,boolean factory){
  logic.reset();world.loadGenerator(48,48,tiles->{tiles.fill();for(var t:tiles)t.setFloor(Blocks.stone.asFloor());tiles.get(6,6).setBlock(Blocks.coreShard,Team.sharded);if(factory){tiles.get(8,6).setBlock(Blocks.conveyor,Team.sharded,2);tiles.get(9,6).setBlock(Blocks.graphitePress,Team.sharded);}});
  state.rules.sector=s;s.planet.applyRules(state.rules);state.set(GameState.State.playing);
 }
 public static void main(String[]args){new HeadlessApplication(new ApplicationListener(){public void init(){try{
  Core.settings.setDataDirectory(root);headless=true;net=new Net(null);tree=new FileTree();Vars.init();content.createBaseContent();logic=new Logic();netServer=new NetServer();content.init();Core.assets=new AssetManager();slots=new Saves();
  arc.util.Time.setDeltaProvider(()->1f);Planet p=Planets.industrialSerpuloProgressive;Sector a=p.getStartSector(),b=a.near().first();base(a);base(b);arena(a,true);
  Team.sharded.core().items.set(Items.graphite,4000);world.tile(9,6).build.items.add(Items.coal,10);
  for(int i=0;i<700;i++){arc.util.Time.update();state.updateId++;Groups.build.update();a.info.update();}
  if(Team.sharded.core().items.get(Items.graphite)!=4000||world.tile(9,6).build.items.get(Items.coal)!=0)throw new AssertionError("Native full-core trial failed");
  SaveIO.save(a.save.file);float gross=a.info.rawProduction.get(Items.graphite).mean*60f;arena(b,false);
  for(int i=0;i<3;i++){universe.runTurn();String before=JsonIO.write(a.info);var row=IndustrialDiagnostics.read(p).colony(a).material(Items.graphite);if(row.stock!=4000||!row.coreSaturated||!before.equals(JsonIO.write(a.info)))throw new AssertionError("Observer changed saturation");}
  SaveIO.load(a.save.file,world.makeSectorContext(a));state.set(GameState.State.playing);
  if(Team.sharded.core().items.get(Items.graphite)!=4000)throw new AssertionError("Return changed native full stock");
  System.out.println("M7B_SATURATION active=4000 native_gross_mean_per_min="+gross+" after_3_turns=4000 returned=4000 read_only=true");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}});}
}
