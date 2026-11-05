// Isolated cloud copy only: invokes native abandonment on copied bases, never player data.
Events.on(ClientLoadEvent,cons(function(){
 var E=Packages.mindustry.game.IndustrialExpansion,R=Packages.mindustry.game.IndustrialResearch,p=Planets.industrialSerpuloProgressive,start=p.getStartSector(),bases,index=0;
 function later(n,f){Timer.schedule(run(function(){try{f();}catch(e){Log.err("M6D_RECOVERY_FAIL "+e);Core.app.exit();}}),n);}
 function shot(n){Packages.arc.util.ScreenUtils.saveScreenshot(new Packages.arc.files.Fi("/workspace/build-evidence/mission6d-ui-"+n+".png"));}
 function click(n){var b=Core.scene.find(n);if(b==null||b.isDisabled())throw "Unavailable button "+n;b.fireClick();}
 function abandon(){
  if(index==bases.size){
   if(!E.canRecoverWithoutSource(start)||E.ledger(p).established!=3)throw "Abandonment reset counter";
   Vars.ui.planet.show(p);Vars.ui.planet.selectSector(start);Vars.ui.planet.lookAt(start);
   later(1,function(){click("play-sector");later(1,function(){
    shot("recovery-confirmation");Core.scene.getDialog().buttons.getChildren().first().fireClick();
    later(1,function(){if(E.ledger(p).established!=3||p.sectors.contains(boolf(function(s){return s.hasBase();})))throw "Recovery cancellation changed data";
     click("play-sector");later(1,function(){Core.scene.getDialog().buttons.getChildren().peek().fireClick();later(14,arrived);});
    });
   });});return;
  }
  var sector=bases.get(index++);Vars.ui.planet.abandonSectorConfirm(sector,run(function(){}));
  later(0.5,function(){Core.scene.getDialog().buttons.getChildren().peek().fireClick();later(0.8,abandon);});
 }
 function arrived(){
  if(Vars.state.getSector()!=start||!start.hasBase()||E.ledger(p).established!=4||!R.unlocked(Blocks.graphitePress,p))throw "Recovery did not preserve and increment progression";
  E.established(start);if(E.ledger(p).established!=4)throw "Recovery counted twice";
  if(Team.sharded.core().items.get(Items.copper)!=100)throw "Native recovery stock mismatch";
  shot("recovery-landed");Core.settings.put("m6d-recovery-phase1",true);Vars.control.saves.getCurrent().save();Core.settings.forceSave();
  Log.info("M6D_RECOVERY_NATIVE_ABANDON_CANCEL_CONFIRM_COUNTER_AND_RESEARCH_OK");Core.app.exit();
 }
 later(5,function(){
  Core.graphics.setWindowSize(600,1000);Core.bundle=Packages.arc.util.I18NBundle.createBundle(Core.files.internal("bundles/bundle"),Packages.java.util.Locale.FRENCH);
  if(Core.scene.getDialog()!=null)Core.scene.getDialog().hide();
  if(Core.settings.getBool("m6d-recovery-phase1",false)){
   Vars.control.playSector(start);later(12,function(){
    if(E.ledger(p).established!=4||!R.unlocked(Blocks.graphitePress,p))throw "Recovery lost after restart";
    var target=start.near().first();if(E.surcharge(target).get(Items.copper)!=750)throw "Recovery lowered next tariff";
    Log.info("M6D_RECOVERY_RESTART_AND_NEXT_TARIFF_OK");Core.app.exit();
   });
  }else{bases=p.sectors.select(boolf(function(s){return s.hasBase();}));if(bases.size!=3)throw "Unexpected copied fixture bases";abandon();}
 });
}));
