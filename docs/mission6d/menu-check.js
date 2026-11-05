// Isolated cloud copy only. Tests a paid launch without first loading a world.
Events.on(ClientLoadEvent,cons(function(){
 var E=Packages.mindustry.game.IndustrialExpansion,p=Planets.industrialSerpuloProgressive,source=p.getStartSector(),target;
 function later(n,f){Timer.schedule(run(function(){try{f();}catch(e){Log.err("M6D_MENU_FAIL "+e);Core.app.exit();}}),n);}
 function click(n){var b=Core.scene.find(n);if(b==null||b.isDisabled())throw "Unavailable button "+n;b.fireClick();}
 later(5,function(){
  Core.graphics.setWindowSize(600,1000);Core.bundle=Packages.arc.util.I18NBundle.createBundle(Core.files.internal("bundles/bundle"),Packages.java.util.Locale.FRENCH);
  if(Core.scene.getDialog()!=null)Core.scene.getDialog().hide();
  if(!Vars.state.isMenu()||E.ledger(p).established!=3||source.items().get(Items.copper)!=2650)throw "Menu fixture invalid";
  Vars.universe.updateLaunchResources(new Packages.mindustry.type.ItemSeq());Core.settings.put("maxresources",false);
  target=source.near().find(boolf(function(s){return !s.hasBase();}));
  Vars.ui.planet.show(p);Vars.ui.planet.launchSector=source;Vars.ui.planet.selectSector(target);Vars.ui.planet.lookAt(target);
  later(1,function(){click("play-sector");later(1,function(){
   click("launch-sector-confirm");
   if(!Vars.state.isMenu()||Vars.state.gameOver)throw "Menu state changed before world load";
   later(14,function(){
    if(Vars.state.getSector()!=target||!target.hasBase()||Vars.state.gameOver||E.ledger(p).established!=4)throw "Menu departure failed";
    if(source.items().get(Items.copper)!=1150||source.items().get(Items.lead)!=1750)throw "Menu departure payment mismatch";
    Log.info("M6D_MENU_PAID_LAUNCH_STATE_AND_SINGLE_PAYMENT_OK");Core.settings.forceSave();Core.app.exit();
   });
  });});
 });
}));
