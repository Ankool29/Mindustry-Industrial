// Cloud test copy only. Fixture grants never ship in the APK or touch player data.
Events.on(ClientLoadEvent,cons(function(){
 var E=Packages.mindustry.game.IndustrialExpansion,R=Packages.mindustry.game.IndustrialResearch;
 var strict=Planets.industrialSerpuloProgressive,free=Planets.industrialSerpulo,source=strict.getStartSector(),target;
 var freezeArmed=false;
 Events.run(Packages.mindustry.game.EventType.Trigger.beforeGameUpdate,run(function(){
  if(freezeArmed&&Vars.state.getSector()==source&&E.ledger(strict).pending!=null){Log.err("M6D_UI_FAIL Source simulation advanced during prepared departure");Core.app.exit();}
 }));
 function later(n,f){Timer.schedule(run(function(){try{f();}catch(e){Log.err("M6D_UI_FAIL "+e);Core.app.exit();}}),n);}
 function shot(n){Packages.arc.util.ScreenUtils.saveScreenshot(new Packages.arc.files.Fi("/workspace/build-evidence/mission6d-ui-"+n+".png"));}
 function click(n){var b=Core.scene.find(n);if(b==null||b.isDisabled())throw "Unavailable button "+n;b.fireClick();}
 function count(n){if(E.ledger(strict).established!=n)throw "Counter mismatch: "+E.ledger(strict).established+" expected "+n;}
 function stock(c,l){if(source.items().get(Items.copper)!=c||source.items().get(Items.lead)!=l)throw "Source stock mismatch "+source.items().get(Items.copper)+"/"+source.items().get(Items.lead);}
 function fill(){Team.sharded.core().items.set(Items.copper,4000);Team.sharded.core().items.set(Items.lead,4000);Vars.control.saves.getCurrent().save();Core.settings.forceSave();}
 function cargo(c,l){var q=new Packages.mindustry.type.ItemSeq();q.set(Items.copper,c);q.set(Items.lead,l);Vars.universe.updateLaunchResources(q);Core.settings.put("maxresources",false);}
 function open(next){
  Vars.ui.planet.show(strict);Vars.ui.planet.selectSector(target);Vars.ui.planet.lookAt(target);
  later(1,function(){click("play-sector");later(1,function(){if(Core.scene.find("industrial-expansion-destination")==null)throw "Expansion summary missing";next();});});
 }
 function start(){
  count(1);if(E.ledger(strict).grandfathered!=1)throw "M6C migration mismatch";
  if(!R.unlocked(Blocks.graphitePress,strict)||!Blocks.graphitePress.unlockedNowHost())throw "M6C research lost";
  if(source.near().size!=6)throw "M6C frontier changed";
  Core.settings.put("skipcoreanimation",false);fill();cargo(0,0);
  target=source.near().find(boolf(function(s){return !s.hasBase();}));Core.settings.put("m6d-first-target",String(target.id));
  open(function(){
   if(E.surcharge(target).total!=0)throw "Second colony should have no surcharge";
   shot("second-colony-free");Core.scene.getDialog().hide();Vars.ui.planet.hide();
   count(1);stock(4000,4000);if(E.ledger(strict).pending!=null)throw "Cancellation left journal";
   Team.sharded.core().items.set(Items.copper,0);Team.sharded.core().items.set(Items.lead,0);
   open(function(){
    if(!Core.scene.find("launch-sector-confirm").isDisabled())throw "Insufficient funds accepted";
    shot("insufficient-resources");Core.scene.getDialog().hide();Vars.ui.planet.hide();fill();
    open(function(){freezeArmed=true;click("launch-sector-confirm");later(14,secondLanded);});
   });
  });
 }
 function secondLanded(){
  if(Vars.state.getSector()!=target||!target.hasBase())throw "Second landing failed";
  count(2);stock(3000,3200);Log.info("M6D_UI_MIGRATION_CANCEL_INSUFFICIENT_SECOND_LANDING_OK");
  Vars.control.playSector(source);later(12,function(){
   count(2);stock(3000,3200);fill();cargo(100,50);
   target=source.near().find(boolf(function(s){return !s.hasBase();}));Core.settings.put("m6d-third-target",String(target.id));
   open(function(){
    if(E.surcharge(target).get(Items.copper)!=250||E.surcharge(target).get(Items.lead)!=200)throw "Third surcharge mismatch";
    shot("third-colony-breakdown");
    Core.scene.getDialog().buttons.getChildren().get(1).fireClick();
    var maxCargo=Vars.universe.getLaunchResources();
    if(maxCargo.get(Items.copper)!=2000||maxCargo.get(Items.lead)!=2000)throw "Native maximum cargo/cost reservation mismatch";
    if(!source.items().has(E.total(target,Vars.universe.getLastLoadout().requirements(),maxCargo)))throw "Maximum cargo exceeds source stock";
    shot("maximum-cargo");Core.scene.getDialog().hide();Vars.ui.planet.hide();count(2);stock(4000,4000);cargo(100,50);
    open(function(){freezeArmed=true;click("launch-sector-confirm");later(14,thirdLanded);});
   });
  });
 }
 function thirdLanded(){
  if(Vars.state.getSector()!=target||!target.hasBase())throw "Third landing failed";
  count(3);stock(2650,2950);
  if(Team.sharded.core().items.get(Items.copper)!=100||Team.sharded.core().items.get(Items.lead)!=50)throw "Native cargo mismatch";
  if(E.ledger(strict).pending!=null)throw "Payment not finalized";
  shot("third-colony-landed");Core.settings.put("m6d-phase1",true);Vars.control.saves.getCurrent().save();Core.settings.forceSave();
  Log.info("M6D_UI_THIRD_LANDING_SINGLE_PAYMENT_NATIVE_CARGO_OK");Core.app.exit();
 }
 function restart(){
  count(3);stock(2650,2950);if(E.ledger(strict).pending!=null)throw "Pending journal after restart";
  target=strict.sectors.get(parseInt(Core.settings.getString("m6d-third-target")));
  if(E.surcharge(target).total!=0)throw "Existing-sector visit charged";
  Vars.control.playSector(target);later(12,function(){count(3);stock(2650,2950);shot("return-free");
   Log.info("M6D_UI_RESTART_COUNTER_STOCK_AND_FREE_RETURN_OK");
   Vars.control.playSector(source);later(12,function(){
    target=source.near().find(boolf(function(s){return !s.hasBase();}));cargo(100,50);
    open(function(){
     if(E.surcharge(target).get(Items.copper)!=500)throw "Fourth surcharge mismatch";
     shot("fourth-colony-portrait");Core.scene.getDialog().hide();Vars.ui.planet.hide();count(3);stock(2650,2950);
     Log.info("M6D_UI_PORTRAIT_BREAKDOWN_CANCEL_OK");Vars.control.playSector(free.getStartSector());later(12,legacy);
    });
   });
  });
 }
 function legacy(){
  if(!Blocks.blastDrill.unlockedNowHost()||R.tree(free).nodes.get(Blocks.mechanicalDrill).finishedRequirements[0].amount!=10)throw "Legacy free campaign changed";
  if(Core.settings.has(E.key(free)))throw "Legacy expansion ledger created";
  Vars.ui.planet.show(free);target=free.getStartSector().near().find(boolf(function(s){return !s.hasBase();}));Vars.ui.planet.selectSector(target);Vars.ui.planet.lookAt(target);
  later(1,function(){click("play-sector");later(1,function(){
   if(Core.scene.find("industrial-expansion-destination")!=null)throw "Legacy launch UI changed";
   shot("legacy-native-launch");Core.scene.getDialog().hide();Vars.ui.planet.hide();Vars.control.playSector(Planets.serpulo.sectors.get(171));later(12,function(){
    if(Vars.state.rules.industrial||!Vars.state.rules.waves||Core.settings.has(E.key(Planets.serpulo)))throw "Classic campaign changed";
    Log.info("M6D_UI_FREE_AND_CLASSIC_PRESERVED_OK");Core.app.exit();
   });
  });});
 }
 later(5,function(){
  var cores=[Blocks.coreShard,Blocks.coreFoundation,Blocks.coreNucleus];
  for(var ci=0;ci<cores.length;ci++){
   var block=cores[ci],bill=new Packages.mindustry.type.ItemSeq();
   for(var bi=0;bi<block.requirements.length;bi++)bill.add(block.requirements[bi].item,block.requirements[bi].amount);
   bill.add(Items.copper,2000);bill.add(Items.lead,1600);
   for(var ri=0;ri<block.requirements.length;ri++)if(bill.get(block.requirements[ri].item)>block.itemCapacity)throw "Native core capacity exceeded: "+block.name;
  }
  Log.info("M6D_UI_ALL_NATIVE_CORES_FIT_SURCHARGE_OK");
  Core.graphics.setWindowSize(600,1000);
  Core.bundle=Packages.arc.util.I18NBundle.createBundle(Core.files.internal("bundles/bundle"),Packages.java.util.Locale.FRENCH);
  if(Core.scene.getDialog()!=null)Core.scene.getDialog().hide();Vars.control.playSector(source);
  later(12,function(){if(Core.settings.getBool("m6d-phase1",false))restart();else start();});
 });
}));
