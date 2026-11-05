// Separate classic campaign check. Never packaged with the APK.
Events.on(ClientLoadEvent,cons(function(){
 function later(n,f){Timer.schedule(run(function(){try{f();}catch(e){Log.err("M5_ORIGINAL_UI_FAIL "+e);Core.app.exit();}}),n);}
 later(5,function(){
  Core.bundle=Packages.arc.util.I18NBundle.createBundle(Core.files.internal("bundles/bundle"),Packages.java.util.Locale.FRENCH);
  if(Core.scene.getDialog()!=null)Core.scene.getDialog().hide();
  const sector=Planets.serpulo.getStartSector().near().find(boolf(function(s){return s.preset==null&&!s.generateEnemyBase;}));
  if(sector==null)throw "No peaceful initial classic terrain found";
  Vars.universe.clearLoadoutInfo();Vars.control.playSector(sector);
  later(12,function(){
   if(Vars.state.getSector()!=sector||Vars.state.rules.industrial||!Vars.state.rules.waves)throw "Classic campaign rules changed";
   Blocks.duo.quietUnlock();Vars.ui.hudfrag.blockfrag.rebuild();
   const category=Core.scene.find("category-turret");if(category==null)throw "Classic turret category missing";category.fireClick();
   later(2,function(){
    const duo=Core.scene.find("block-duo");if(duo==null||duo.getWidth()<=0||duo.getHeight()<=0)throw "Classic Duo button has no layout";
    Packages.arc.util.ScreenUtils.saveScreenshot(new Packages.arc.files.Fi("/workspace/build-evidence/mission5-ui-original-visible.png"));
    Log.info("M5_ORIGINAL_UI_TURRET_LAYOUT_OK");Vars.ui.research.show();
    later(2,function(){
     if(!Vars.ui.research.isShown())throw "Classic research missing";
     Packages.arc.util.ScreenUtils.saveScreenshot(new Packages.arc.files.Fi("/workspace/build-evidence/mission5-ui-original-tree.png"));
     Log.info("M5_ORIGINAL_UI_RESEARCH_OK");Core.app.exit();
    });
   });
  });
 });
}));
