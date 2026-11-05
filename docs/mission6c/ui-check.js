// Cloud validation copies only. No script or fixture grant is shipped in the APK.
Events.on(ClientLoadEvent,cons(function(){
 var R=Packages.mindustry.game.IndustrialResearch,strict=Planets.industrialSerpuloProgressive,free=Planets.industrialSerpulo;
 function later(n,f){Timer.schedule(run(function(){try{f();}catch(e){Log.err("M6C_UI_FAIL "+e);Core.app.exit();}}),n);}
 function shot(n){Packages.arc.util.ScreenUtils.saveScreenshot(new Packages.arc.files.Fi("/workspace/build-evidence/mission6c-ui-"+n+".png"));}
 function click(n){var b=Core.scene.find(n);if(b==null||b.isDisabled())throw "Unavailable button "+n;b.fireClick();}
 function dn(){return String(Core.scene.getDialog().getClass().getSimpleName());}
 function graph(){Vars.ui.research.show();if(!Vars.ui.research.isShown()||Vars.ui.research.lastNode.planet!=strict)throw "Strict tree missing";}
 function detail(){click("research-list-graphite-press");if(dn()!="IndustrialResearchDetailsDialog")throw "Research sheet missing";}
 function readyStrict(){if(Vars.state.getPlanet()!=strict||Team.sharded.core().items.get(Items.copper)!=100||Blocks.graphitePress.unlockedNowHost()||!Blocks.mechanicalDrill.unlockedNowHost())throw "Strict starter invalid";graph();later(2,function(){shot("strict-tree-portrait");click("research-list");later(1,function(){detail();later(1,missing);});});}
 function missing(){
  if(!Core.scene.find("industrial-research-pay").isDisabled())throw "Missing conditions did not block funding";
  shot("missing-conditions");R.discover(Items.lead,strict);R.discover(Items.coal,strict);Core.scene.getDialog().hide();
  later(1,function(){detail();later(1,function(){click("industrial-research-pay");later(1,partial);});});
 }
 function partial(){
  var node=R.tree(strict).nodes.get(Blocks.graphitePress);
  if(node.finishedRequirements[0].amount!=100||R.unlocked(Blocks.graphitePress,strict)||Team.sharded.core().items.get(Items.copper)!=0)throw "UI partial contribution failed";
  shot("partial-contribution");
  // Private test fixture supplies the remaining native bill; normal players must produce it.
  for(var i=0;i<node.requirements.length;i++){var req=node.requirements[i];Team.sharded.core().items.set(req.item,req.amount-node.finishedRequirements[i].amount);}
  if(Team.sharded.core().items.get(Items.lead)!=node.requirements[1].amount)throw "Fixture did not supply lead";
  Core.settings.put("m6c-phase1",true);Vars.control.saves.getCurrent().save();Core.settings.forceSave();
  Log.info("M6C_UI_CONFIRM_CANCEL_STRICT_START_AND_PARTIAL_PAYMENT_OK");Core.app.exit();
 }
 function legacyFirst(){
  if(Vars.state.getPlanet()!=free||!Blocks.blastDrill.unlockedNowHost())throw "Legacy free access changed";
  if(Vars.world.tile(Team.sharded.core().tileX()+5,Team.sharded.core().tileY()).block()!=Blocks.graphitePress)throw "Legacy factory missing";
  if(!R.unlocked(Blocks.mechanicalDrill,free)||R.tree(free).nodes.get(Blocks.mechanicalDrill).finishedRequirements[0].amount!=10)throw "M6B research changed";
  Core.settings.put("m6c-classic-graphite",Blocks.graphitePress.unlocked());Log.info("M6C_UI_LEGACY_M6B_FACTORY_AND_RESEARCH_OK");
  Vars.ui.planet.show(strict);Vars.ui.planet.selectSector(strict.getStartSector());Vars.ui.planet.lookAt(strict.getStartSector());
  later(2,function(){click("play-sector");later(1,function(){
   if(Vars.state.getPlanet()!=free||strict.getStartSector().hasSave())throw "Campaign started before confirmation";
   shot("start-confirmation");Core.scene.getDialog().buttons.getChildren().first().fireClick();
   later(1,function(){if(strict.getStartSector().hasSave())throw "Cancellation created save";click("play-sector");later(1,function(){Core.scene.getDialog().buttons.getChildren().peek().fireClick();later(12,readyStrict);});});
  });});
 }
 function restart(){
  var node=R.tree(strict).nodes.get(Blocks.graphitePress);
  if(node.finishedRequirements[0].amount!=100||R.unlocked(Blocks.graphitePress,strict))throw "Partial contribution lost after restart";
  for(var i=0;i<node.requirements.length;i++){var req=node.requirements[i];if(Team.sharded.core().items.get(req.item)!=req.amount-node.finishedRequirements[i].amount)throw "Reserved stock did not survive restart: "+req.item.name;}
  Log.info("M6C_UI_PARTIAL_AND_STOCK_PERSIST_AFTER_RESTART_OK");
  graph();later(2,function(){click("research-list");later(1,function(){detail();later(1,function(){click("industrial-research-pay");later(1,completed);});});});
 }
 function completed(){
  if(!R.unlocked(Blocks.graphitePress,strict)||!Blocks.graphitePress.unlockedNowHost())throw "Research did not unlock machine";
  if(Blocks.graphitePress.unlocked()!=Core.settings.getBool("m6c-classic-graphite"))throw "Classic unlock changed";
  shot("research-completed");Core.scene.getDialog().hide();later(1,function(){Core.scene.getDialog().hide();Vars.ui.research.hide();later(2,construction);});
 }
 function construction(){
  click("category-crafting");if(Core.scene.find("block-graphite-press")==null)throw "Unlocked construction button missing";
  later(1,function(){shot("construction-unlocked");Core.graphics.setWindowSize(1200,720);graph();later(2,function(){
   shot("strict-tree-landscape");click("research-catalogue");if(dn()!="IndustrialCatalogDialog")throw "Catalogue removed";
   later(1,function(){shot("catalogue-retained");Core.scene.getDialog().hide();Vars.ui.research.hide();Log.info("M6C_UI_COMPLETION_CONSTRUCTION_AND_CATALOGUE_OK");Vars.control.playSector(free.getStartSector());later(12,legacyReturn);});
  });});
 }
 function legacyReturn(){
  if(!Blocks.blastDrill.unlockedNowHost()||R.tree(free).nodes.get(Blocks.mechanicalDrill).finishedRequirements[0].amount!=10)throw "Legacy free state contaminated";
  Vars.ui.research.show();later(2,function(){shot("legacy-free-tree");Vars.ui.research.hide();Vars.control.playSector(Planets.serpulo.sectors.get(171));later(12,function(){
   if(Vars.state.rules.industrial||!Vars.state.rules.waves)throw "Classic campaign changed";
   Vars.ui.research.show();if(!Vars.ui.research.isShown()||Vars.ui.research.lastNode instanceof Packages.mindustry.game.IndustrialResearch.IndustrialNode)throw "Classic tree missing";
   later(2,function(){shot("classic-tree");Log.info("M6C_UI_LEGACY_FREE_AND_CLASSIC_RESTORED_OK");Core.app.exit();});
  });});
 }
 later(5,function(){
  Core.bundle=Packages.arc.util.I18NBundle.createBundle(Core.files.internal("bundles/bundle"),Packages.java.util.Locale.FRENCH);
  if(Core.scene.getDialog()!=null)Core.scene.getDialog().hide();
  if(!Core.settings.getBool("m6c-phase1",false)){Vars.control.playSector(free.getStartSector());later(12,legacyFirst);}
  else{Vars.control.playSector(strict.getStartSector());later(12,restart);}
 });
}));
