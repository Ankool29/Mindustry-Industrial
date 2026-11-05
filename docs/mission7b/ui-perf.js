// Synthetic UI benchmark only, separate cloud copy. No world is loaded or saved.
Events.on(ClientLoadEvent,cons(function(){
 var p=Planets.industrialSerpuloProgressive,slot=p.getStartSector().save,sizes=[5,20,50,100],sizeIndex=0,times=[],allocs=[],round=0;
 var meter=Packages.java.lang.management.ManagementFactory.getThreadMXBean(),tid=Packages.java.lang.Thread.currentThread().getId();
 function later(n,f){Timer.schedule(run(function(){try{f();}catch(e){Log.err("M7B_PERF_FAIL "+e);Core.app.exit();}}),n);}
 function click(n){var b=Core.scene.find(n);if(b==null||b.isDisabled())throw "Unavailable "+n;b.fireClick();}
 function nextSize(){
  if(sizeIndex==sizes.length){Log.info("M7B_PERF_ALL_PORTRAIT_SIZES_OK");Core.app.exit();return;}
  var n=sizes[sizeIndex];
  for(var i=0;i<p.sectors.size;i++){var s=p.sectors.get(i);s.save=null;s.info=new Packages.mindustry.game.SectorInfo();}
  for(var i=0;i<n;i++){var s=p.sectors.get(i);s.save=slot;s.info.hasCore=true;s.info.waves=s.info.attack=false;s.info.storageCapacity=4000;s.info.items.set(Items.copper,1000);}
  Vars.ui.planet.show(p);Vars.ui.planet.selectSector(p.getStartSector());Vars.ui.planet.lookAt(p.getStartSector());times=[];allocs=[];round=0;
  later(.5,measure);
 }
 function measure(){
  var before=meter.getThreadAllocatedBytes(tid),start=Packages.java.lang.System.nanoTime();click("industrial-diagnostics");
  var dialog=Core.scene.getDialog();dialog.validate();
  var ms=(Packages.java.lang.System.nanoTime()-start)/1000000,bytes=meter.getThreadAllocatedBytes(tid)-before;
  var scroll=Core.scene.find("diagnostics-scroll");if(!scroll.isScrollingDisabledX())throw "Horizontal scroll enabled";
  if(round>0){times.push(ms);allocs.push(bytes/1024);}
  if(round==10){
   times.sort(function(a,b){return a-b;});allocs.sort(function(a,b){return a-b;});
   Log.info("M7B_UI_PERF N="+sizes[sizeIndex]+" create_validate_p50_ms="+times[4].toFixed(3)+" p95_ms="+times[9].toFixed(3)+" median_allocation_KiB="+allocs[4].toFixed(1));
   if(sizes[sizeIndex]==100){
    scroll.setScrollPercentY(1);scroll.updateVisualScroll();
    later(.6,function(){Packages.arc.util.ScreenUtils.saveScreenshot(new Packages.arc.files.Fi("/workspace/build-evidence/mission7b-ui-100-colonies.png"));dialog.hide();Vars.ui.planet.hide();sizeIndex++;later(.4,nextSize);});
   }else{dialog.hide();Vars.ui.planet.hide();sizeIndex++;later(.4,nextSize);}
  }else{round++;dialog.hide();later(.4,measure);}
 }
 later(5,function(){Core.graphics.setWindowSize(600,1000);Core.bundle=Packages.arc.util.I18NBundle.createBundle(Core.files.internal("bundles/bundle"),Packages.java.util.Locale.FRENCH);if(Core.scene.getDialog()!=null)Core.scene.getDialog().hide();nextSize();});
}));
