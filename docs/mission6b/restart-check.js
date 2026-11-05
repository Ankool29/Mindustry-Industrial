// Cloud-only compatibility/restart harness. Never included in the APK.
Events.on(ClientLoadEvent,cons(function(){
 const Research=Packages.mindustry.game.IndustrialResearch;
 function later(n,f){Timer.schedule(run(function(){try{f();}catch(e){Log.err("M6B_UI_FAIL "+e);Core.app.exit();}}),n);}
 function shot(name){Packages.arc.util.ScreenUtils.saveScreenshot(new Packages.arc.files.Fi("/workspace/build-evidence/mission6b-ui-"+name+".png"));}
 function catalogue(){
  Vars.ui.research.show();const dialog=Core.scene.getDialog();
  if(dialog==null||dialog.getClass().getSimpleName()!="IndustrialCatalogDialog")throw "Machines catalogue missing";
  if(dialog.find("industrial-catalog-blast-drill")==null||dialog.find("industrial-catalog-duo")!=null)throw "Catalogue compatibility changed";
 }
 later(5,function(){
  Core.bundle=Packages.arc.util.I18NBundle.createBundle(Core.files.internal("bundles/bundle"),Packages.java.util.Locale.FRENCH);
  if(Core.scene.getDialog()!=null)Core.scene.getDialog().hide();
  const start=Planets.industrialSerpulo.getStartSector();Vars.control.playSector(start);
  later(12,function(){
   if(Vars.state.getSector()!=start||!Vars.state.rules.industrial||Vars.state.rules.waves||!Blocks.blastDrill.unlockedNowHost())throw "Industrial gameplay no longer unrestricted";
   const core=Team.sharded.core(),node=Research.tree().nodes.get(Blocks.mechanicalDrill);
   const factory=Vars.world.tile(core.tileX()+5,core.tileY());if(factory.block()!=Blocks.graphitePress)throw "M4 factory missing";
   if(!Core.settings.getBool("m6b-ui-phase1",false)){
    if(core.items.get(Items.copper)!=500)throw "M4 inventory changed before research test";
    Core.settings.put("m6b-ui-classic-unlocked",Blocks.mechanicalDrill.unlocked());
    Core.settings.put("m6b-ui-classic-paid",new Packages.java.lang.Integer(String(Core.settings.getInt("req-mechanical-drill-copper",0))));
    Research.discover(Items.copper);core.items.set(Items.copper,4); // Isolated fixture for partial payment.
    if(Research.spend(node).get(Items.copper)!=4||node.finishedRequirements[0].amount!=4||Research.unlocked(Blocks.mechanicalDrill))throw "First partial payment failed";
    core.items.set(Items.copper,6);Core.settings.put("m6b-ui-phase1",true);
    Vars.control.saves.getCurrent().save();Core.settings.forceSave();
    catalogue();later(2,function(){shot("catalogue-first-start");Log.info("M6B_UI_PARTIAL_4_SAVED_AND_M4_COMPATIBLE_OK");Core.app.exit();});
   }else{
    if(core.items.get(Items.copper)!=6||node.finishedRequirements[0].amount!=4||!Research.discovered(Items.copper))throw "State did not survive process restart";
    if(Research.spend(node).get(Items.copper)!=6||!Research.unlocked(Blocks.mechanicalDrill)||core.items.get(Items.copper)!=0)throw "Final payment failed";
    if(Blocks.mechanicalDrill.unlocked()!=Core.settings.getBool("m6b-ui-classic-unlocked")||Core.settings.getInt("req-mechanical-drill-copper",0)!=Core.settings.getInt("m6b-ui-classic-paid"))throw "Classic research contaminated";
    Log.info("M6B_UI_RESTART_4_PLUS_6_AND_CLASSIC_ISOLATION_OK");
    catalogue();later(2,function(){
     shot("catalogue-after-restart");Core.scene.getDialog().hide();
     const classic=Planets.serpulo.getStartSector().near().find(boolf(function(s){return s.preset==null&&!s.generateEnemyBase;}));
     Vars.universe.clearLoadoutInfo();Vars.control.playSector(classic);
     later(12,function(){
      if(Vars.state.getSector()!=classic||Vars.state.rules.industrial||!Vars.state.rules.waves)throw "Classic campaign changed";
      Blocks.duo.quietUnlock();Vars.ui.hudfrag.blockfrag.rebuild();Core.scene.find("category-turret").fireClick();
      later(2,function(){
       if(Core.scene.find("block-duo")==null)throw "Classic turret missing";shot("classic-turret");Vars.ui.research.show();
       later(2,function(){if(!Vars.ui.research.isShown())throw "Classic research missing";shot("classic-tree");Log.info("M6B_UI_CLASSIC_CAMPAIGN_AND_TREE_OK");Core.app.exit();});
      });
     });
    });
   }
  });
 });
}));
