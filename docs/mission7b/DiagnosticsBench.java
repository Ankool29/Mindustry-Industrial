import arc.*;
import arc.assets.*;
import arc.backend.headless.*;
import arc.files.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.IndustrialDiagnostics.*;
import mindustry.game.SectorInfo.*;
import mindustry.io.*;
import mindustry.net.*;
import mindustry.type.*;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import static mindustry.Vars.*;

/** Synthetic read-only diagnostics benchmark, isolated from player saves. */
public class DiagnosticsBench{
 static Fi root=new Fi("/workspace/audit-sources/mission7b/bench-data/run-"+System.currentTimeMillis());
 static int generation;
 static Saves slots;
 static com.sun.management.ThreadMXBean meter=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
 static volatile Object sink;
 static void rate(arc.struct.ObjectMap<Item,ExportStat> map,Item item,float mean){ExportStat stat=new ExportStat();stat.mean=mean;map.put(item,stat);}
 static double q(long[] a,double p){long[] b=a.clone();Arrays.sort(b);return b[(int)((b.length-1)*p)]/1e6;}
 static void setup(int n){
  logic.reset();Core.settings=new Settings();Core.settings.setJson(JsonIO.json);Core.settings.setDataDirectory(root.child("n"+n+"-"+(generation++)));Core.settings.load();
  Planet p=Planets.industrialSerpuloProgressive;
  for(Sector s:p.sectors){s.info=new SectorInfo();s.save=null;}
  for(int x=0;x<n;x++){
   Sector s=p.sectors.get(x);s.save=slots.new SaveSlot(root.child("fake-"+x+".msav"));s.info.hasCore=true;s.info.waves=s.info.attack=false;s.info.storageCapacity=4000;
   s.info.destination=p.sectors.get((x+1)%n);
   for(int k=0;k<12;k++){Item i=content.item(k);s.info.items.set(i,1000);rate(s.info.production,i,.25f);rate(s.info.rawProduction,i,1);rate(s.info.export,i,.5f);rate(s.info.imports,i,.125f);}
  }
 }
 static void measure(int n,String label){
  Planet p=Planets.industrialSerpuloProgressive;
  for(int i=0;i<200;i++)sink=IndustrialDiagnostics.read(p);
  long[] times=new long[500];long tid=Thread.currentThread().getId(),bytes=meter.getThreadAllocatedBytes(tid),cpu=meter.getCurrentThreadCpuTime();
  for(int i=0;i<times.length;i++){long t=System.nanoTime();sink=IndustrialDiagnostics.read(p);times[i]=System.nanoTime()-t;}
  bytes=meter.getThreadAllocatedBytes(tid)-bytes;cpu=meter.getCurrentThreadCpuTime()-cpu;
  Reading last=(Reading)sink;int rows=0,pads=0;for(var c:last.colonies){rows+=c.materials.size;pads+=c.platforms.size;}
  boolean untouched=true;for(Sector s:p.sectors)if(s.save!=null){untouched&=s.info.importRateCache==null;for(int k=0;k<12;k++)untouched&=s.info.items.get(content.item(k))==1000;}
  System.out.printf(java.util.Locale.ROOT,"READ %s N=%d p50_ms=%.4f p95_ms=%.4f cpu_ms=%.4f allocation_KiB=%.2f rows=%d pads=%d unchanged_metadata=%s settings_modified=%s%n",label,n,q(times,.5),q(times,.95),cpu/1e6/times.length,bytes/1024.0/times.length,rows,pads,untouched,Core.settings.modified());
 }
 static void closed(){
  Sector s=Planets.industrialSerpuloProgressive.getStartSector();
  for(int i=0;i<100000;i++)IndustrialDiagnostics.measured(s);
  long tid=Thread.currentThread().getId(),bytes=meter.getThreadAllocatedBytes(tid),start=meter.getCurrentThreadCpuTime();
  for(int i=0;i<1000000;i++)IndustrialDiagnostics.measured(s);
  System.out.printf(java.util.Locale.ROOT,"CLOSED mark_cpu_ns=%.2f bytes_per_mark=%.4f settings_modified=%s%n",(meter.getCurrentThreadCpuTime()-start)/1e6,(meter.getThreadAllocatedBytes(tid)-bytes)/1e6,Core.settings.modified());
 }
 public static void main(String[]args){new HeadlessApplication(new ApplicationListener(){public void init(){try{
  Core.settings.setDataDirectory(root.child("init"));headless=true;net=new Net(null);tree=new FileTree();Vars.init();content.createBaseContent();logic=new Logic();netServer=new NetServer();content.init();Core.assets=new AssetManager();slots=new Saves();
  System.out.println("META java="+System.getProperty("java.version")+" arch="+System.getProperty("os.arch")+" items="+content.items().size);
  for(int n:new int[]{5,20,50,100}){setup(n);measure(n,"inactive");}
  setup(100);world.loadGenerator(128,128,tiles->{tiles.fill();for(var tile:tiles)tile.setFloor(Blocks.stone.asFloor());tiles.get(6,6).setBlock(Blocks.coreShard,Team.sharded);for(int x=0;x<100;x++)for(int y=20;y<70;y++)tiles.get(x+2,y).setBlock(Blocks.conveyor,Team.sharded);});
  state.rules.sector=Planets.industrialSerpuloProgressive.sectors.first();state.rules.sector.planet.applyRules(state.rules);state.set(GameState.State.playing);
  System.out.println("ACTIVE_BUILDINGS "+state.rules.defaultTeam.data().buildings.size);
  measure(100,"one_active_5000_conveyors");closed();System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}});}
}
