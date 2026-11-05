// Isolated M6D cloud copy only. Visual fixtures never ship in the APK.
Events.on(ClientLoadEvent,cons(function(){
 var D=Packages.mindustry.game.IndustrialDiagnostics,R=Packages.mindustry.game.IndustrialResearch;
 var p=Planets.industrialSerpuloProgressive,source=p.getStartSector(),target=p.sectors.get(171),dialog,identity,profileHash;
 var key="industrial-expansion-v1-industrial-serpulo-progressive";
 function later(n,f){Timer.schedule(run(function(){try{f();}catch(e){Log.err("M7B_UI_FAIL "+e);Core.app.exit();}}),n);}
 function shot(n){Packages.arc.util.ScreenUtils.saveScreenshot(new Packages.arc.files.Fi("/workspace/build-evidence/mission7b-ui-"+n+".png"));}
 function click(n){var b=Core.scene.find(n);if(b==null||b.isDisabled())throw "Unavailable button "+n;b.fireClick();}
 function hash(){return String(Packages.java.util.Arrays.hashCode(Core.settings.getBytes(key)));}
 function labelText(actor){
  var value="";if(actor instanceof Packages.arc.scene.ui.Label)value+=String(actor.getText())+"\n";
  if(actor instanceof Packages.arc.scene.Group){var all=actor.getChildren();for(var i=0;i<all.size;i++)value+=labelText(all.get(i));}return value;
 }
 function pane(){var s=Core.scene.find("diagnostics-scroll");if(s==null||!s.isScrollingDisabledX())throw "Horizontal scrolling was enabled";return s;}
 function open(){click("industrial-diagnostics");dialog=Core.scene.getDialog();if(String(dialog.getClass().getSimpleName())!="IndustrialDiagnosticsDialog")throw "Diagnostic dialog missing";pane();}
 function map(planet,sector){Vars.ui.planet.show(planet);Vars.ui.planet.selectSector(sector);Vars.ui.planet.lookAt(sector);}
 function rate(map,item,value){var stat=map.get(item);if(stat==null){stat=new Packages.mindustry.game.SectorInfo.ExportStat();map.put(item,stat);}stat.mean=value;}
 function menu(){
  if(D.lastSample(source)!=0)throw "M6D sample date was invented";
  profileHash=hash();map(p,source);
  later(1,function(){open();identity=String(Packages.java.lang.System.identityHashCode(dialog));
   later(1,function(){shot("colonies-portrait");click("diagnostics-sector-170");later(1,function(){
    if(labelText(dialog).indexOf("inconnu")<0)throw "Unknown age not displayed";
    shot("sector-offscreen");click("diagnostics-item-copper");later(1,function(){
     shot("material-offscreen");
     if(labelText(dialog).indexOf("NON MESURÉE")<0||labelText(dialog).replace(/[\u202f\u00a0]/g,"").indexOf("2650")<0)throw "Stock/consumption provenance missing";dialog.hide();later(1,function(){
      if(dialog.cont.getChildren().size!=0)throw "Hidden snapshot retained its widgets";
      open();if(String(Packages.java.lang.System.identityHashCode(dialog))!=identity)throw "Dialog instance not reused";
      dialog.hide();Vars.ui.planet.hide();if(hash()!=profileHash)throw "M6D ledger changed during inspection";
      Log.info("M7B_UI_M6D_STOCK_UNKNOWN_AGE_READONLY_SINGLETON_OK");Vars.control.playSector(source);later(12,active);
     });
    });
   });});
  });
 }
 function active(){
  if(!R.unlocked(Blocks.graphitePress,p)||Team.sharded.core().items.get(Items.copper)!=2650||D.lastSample(source)==0)throw "M6D world or native sample missing";
  // Native platform states for visual inspection in this throwaway copy only.
  Vars.world.tile(20,20).setBlock(Blocks.advancedLaunchPad,Team.sharded);
  Vars.world.tile(20,20).build.items.add(Items.coal,40);
  Vars.world.tile(30,20).setBlock(Blocks.landingPad,Team.sharded);
  Vars.world.tile(30,20).build.config=Items.copper;Vars.world.tile(30,20).build.items.add(Items.copper,10);
  map(p,source);
  rate(source.info.production,Items.coal,-1.25);rate(source.info.rawProduction,Items.graphite,.5);
  source.info.destination=target;target.info.destination=source;rate(target.info.export,Items.copper,2);rate(source.info.export,Items.coal,1);
  later(1,function(){open();click("diagnostics-sector-170");later(1,function(){
   if(labelText(dialog).indexOf("session")<0||labelText(dialog).indexOf("en pause")<0)throw "Active/sample origin missing";
   shot("sector-active");click("diagnostics-item-coal");later(1,function(){
    if(labelText(dialog).indexOf("75.0")<0||labelText(dialog).indexOf("NON MESURÉE")<0)throw "Net withdrawal was misreported";
    shot("material-net-withdrawal");click("diagnostics-back");
    var launch=Vars.world.tile(20,20).build;click("diagnostics-platform-"+launch.pos());
    later(1,function(){shot("launch-platform");click("diagnostics-back");var landing=Vars.world.tile(30,20).build;click("diagnostics-platform-"+landing.pos());
     later(1,function(){shot("landing-platform");pane().setScrollPercentY(1);pane().updateVisualScroll();later(1,refreshChecks);});
    });
   });
  });});
 }
 function refreshChecks(){
  shot("landing-conditions");var before=String(Packages.mindustry.io.JsonIO.write(source.info));
  var stock=Team.sharded.core().items.get(Items.copper);for(var i=0;i<10;i++)click("diagnostics-refresh");
  if(String(Packages.mindustry.io.JsonIO.write(source.info))!=before||Team.sharded.core().items.get(Items.copper)!=stock||hash()!=profileHash)throw "Refresh changed economics";
  dialog.hide();Vars.ui.planet.hide();Log.info("M7B_UI_ACTIVE_MATERIAL_PLATFORM_AND_REFRESH_READONLY_OK");
  map(Planets.industrialSerpulo,Planets.industrialSerpulo.getStartSector());later(1,function(){
   if(Core.scene.find("industrial-diagnostics")!=null)throw "Diagnostic leaked into free campaign";shot("free-map-unchanged");
   map(Planets.serpulo,Planets.serpulo.sectors.get(171));later(1,function(){
    if(Core.scene.find("industrial-diagnostics")!=null)throw "Diagnostic leaked into classic campaign";
    shot("classic-map-unchanged");Vars.ui.planet.hide();Vars.control.playSector(Planets.serpulo.sectors.get(171));later(12,function(){
     if(Vars.state.rules.industrial||!Vars.state.rules.waves)throw "Original campaign rules changed";
     Core.settings.put("m7b-ui-phase1",true);Core.settings.put("m7b-ledger-hash",profileHash);Core.settings.forceSave();
     Log.info("M7B_UI_OTHER_CAMPAIGNS_PRESERVED_OK");Core.app.exit();
    });
   });
  });
 }
 function restart(){
  if(D.lastSample(source)!=0||hash()!=Core.settings.getString("m7b-ledger-hash"))throw "Restart freshness/ledger incorrect";
  var c=D.read(p).colony(source);if(c.sampleNanos!=0||c.material(Items.copper).stock!=2650)throw "Restart invented sample or changed stock";
  map(p,source);later(1,function(){open();click("diagnostics-sector-170");later(1,function(){
   if(labelText(dialog).indexOf("inconnu")<0)throw "Restart date should remain unknown";
   shot("restart-unknown-age");Log.info("M7B_UI_RESTART_M6D_DATA_AND_UNKNOWN_AGE_OK");Core.app.exit();
  });});
 }
 later(5,function(){
  Core.graphics.setWindowSize(600,1000);Core.bundle=Packages.arc.util.I18NBundle.createBundle(Core.files.internal("bundles/bundle"),Packages.java.util.Locale.FRENCH);
  if(Core.scene.getDialog()!=null)Core.scene.getDialog().hide();
  if(Core.settings.getBool("m7b-ui-phase1",false))restart();else menu();
 });
}));
