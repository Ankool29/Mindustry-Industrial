// Local cloud UI checks only. Not included in the Android APK.
Events.on(ClientLoadEvent, cons(function(){
    const Policy=Packages.mindustry.game.IndustrialContent;
    function later(seconds,fn){Timer.schedule(run(function(){try{fn();}catch(e){Log.err("M5_UI_FAIL " + e);Core.app.exit();}}),seconds);}
    function shot(name){Packages.arc.util.ScreenUtils.saveScreenshot(new Packages.arc.files.Fi("/workspace/build-evidence/mission5-ui-"+name+".png"));}
    function click(name){const b=Core.scene.find(name);if(b==null||b.isDisabled())throw "Unavailable UI button "+name;b.fireClick();}
    function game(sector){if(!Vars.state.isGame()||Vars.state.getSector()!=sector)throw "Sector did not load: "+sector;}
    later(5,function(){
        Core.bundle=Packages.arc.util.I18NBundle.createBundle(Core.files.internal("bundles/bundle"),Packages.java.util.Locale.FRENCH);
        if(Core.scene.getDialog()!=null)Core.scene.getDialog().hide();
        Core.settings.put("campaignselect",true);
        Core.settings.put("skipcoreanimation",true);
        const classic=Planets.serpulo.getStartSector(), industry=Planets.industrialSerpulo.getStartSector();
        Vars.control.playSector(classic);
        later(15,function(){
            game(classic);if(Policy.active()||!Vars.state.rules.waves)throw "Classic campaign rules changed";
            if(Core.scene.getDialog()!=null)Core.scene.getDialog().hide();
            Blocks.duo.quietUnlock(); // Isolated fixture unlock, not a product change.
            Vars.ui.hudfrag.blockfrag.rebuild();
            click("category-turret");
            if(Core.scene.find("block-duo")==null)throw "Classic Duo missing";
            shot("classic-turrets");Log.info("M5_UI_CLASSIC_BUILD_OK");
            Vars.ui.research.show();
            if(!Vars.ui.research.isShown())throw "Classic research unavailable";
            later(2,function(){
                shot("classic-research");Vars.ui.research.hide();
                Vars.universe.clearLoadoutInfo();Vars.control.playSector(industry);
                later(13,function(){
                    game(industry);if(!Policy.active()||Vars.state.rules.waves)throw "Industrial campaign invalid";
                    if(!classic.hasBase())throw "Classic save missing after transition";
                    const legacy=Vars.world.tile(Team.sharded.core().tileX()+5,Team.sharded.core().tileY());
                    if(legacy.block()!=Blocks.graphitePress||Team.sharded.core().items.get(Items.copper)!=500)throw "M4 factory or stock did not survive loading";
                    Log.info("M5_UI_M4_SAVE_COMPATIBILITY_OK");
                    if(Core.scene.find("category-turret")!=null||Core.scene.find("category-defense")!=null)throw "Military categories still shown";
                    click("category-production");
                    if(Core.scene.find("block-mechanical-drill")==null)throw "Industrial drill missing";
                    shot("industrial-build");Log.info("M5_UI_INDUSTRIAL_BUILD_OK");
                    if(Packages.mindustry.ui.fragments.HintsFragment.DefaultHint.presetLaunch.show())throw "Classic conquest hint still shown";
                    Vars.ui.research.show();
                    later(2,function(){
                        const catalogue=Core.scene.getDialog();
                        if(catalogue==null||catalogue.getClass().getSimpleName()!="IndustrialCatalogDialog")throw "Industrial catalogue not shown";
                        if(catalogue.find("industrial-catalog-duo")!=null||catalogue.find("industrial-catalog-copper-wall")!=null||catalogue.find("industrial-catalog-flare")!=null)throw "Military catalogue entries shown";
                        if(catalogue.find("industrial-catalog-mechanical-drill")==null||catalogue.find("industrial-catalog-blast-compound")==null||catalogue.find("industrial-catalog-mono")==null)throw "Industrial dependencies absent from catalogue";
                        shot("catalogue-portrait");Core.graphics.setWindowSize(1200,720);
                        later(2,function(){
                            shot("catalogue-landscape");catalogue.hide();
                            const factory=Vars.world.tile(Team.sharded.core().tileX()+6,Team.sharded.core().tileY());factory.setBlock(Blocks.airFactory,Team.sharded);
                            const table=new Packages.arc.scene.ui.layout.Table();factory.build.buildConfiguration(table);
                            const choices=new Packages.arc.struct.Seq();Blocks.airFactory.getPlanConfigs(choices);
                            if(!choices.contains(UnitTypes.mono)||choices.contains(UnitTypes.flare))throw "Factory plan filter failed";
                            Vars.ui.content.show(Blocks.airFactory);
                            later(2,function(){
                                shot("industrial-factory");Vars.ui.content.hide();Log.info("M5_UI_CATALOGUE_AND_FACTORY_OK");
                                Vars.control.playSector(classic);
                                later(12,function(){
                                    game(classic);if(Policy.active()||!Vars.state.rules.waves)throw "Classic campaign failed after return";
                                    click("category-turret");if(Core.scene.find("block-duo")==null)throw "Duo not restored after return";
                                    Vars.ui.research.show();if(!Vars.ui.research.isShown())throw "Original research not restored";
                                    shot("classic-restored");Log.info("M5_UI_CLASSIC_RESTORED_OK");Core.app.exit();
                                });
                            });
                        });
                    });
                });
            });
        });
    });
}));
