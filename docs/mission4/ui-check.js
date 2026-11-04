// Cloud-only touch-layout UI harness; never included in the APK.
Events.on(ClientLoadEvent, cons(function(){
    function fail(e){ Log.err("INDUSTRIAL_CAMPAIGN_UI_FAIL " + e); Core.app.exit(); }
    function later(seconds, action){ Timer.schedule(run(function(){try{action();}catch(e){fail(e);}}), seconds); }
    function shot(name){ Packages.arc.util.ScreenUtils.saveScreenshot(new Packages.arc.files.Fi("/workspace/build-evidence/mission4-ui-" + name + ".png")); }
    function click(table,name){ const b=table.find(name); if(b==null || b.isDisabled()) throw "Unavailable button " + name; b.fireClick(); }
    function verify(s){ if(!Vars.state.isGame() || Vars.state.getSector()!=s || !Vars.state.rules.industrial || Vars.state.rules.waves || Vars.state.rules.attackMode || Vars.state.rules.infiniteResources || Vars.state.rules.canGameOver || Vars.spawner.countSpawns()!=0) throw "Invalid industrial sector " + s; }
    later(5, function(){
        Core.bundle=Packages.arc.util.I18NBundle.createBundle(Core.files.internal("bundles/bundle"), Packages.java.util.Locale.FRENCH);
        if(Core.scene.getDialog()!=null) Core.scene.getDialog().hide();
        Core.settings.put("skipcoreanimation", true);
        Core.settings.put("maxresources", false);
        const planet=Planets.industrialSerpulo, a=planet.getStartSector(), b=a.near().first();
        Vars.ui.planet.show();
        later(1, function(){
            const chooser=Core.scene.getDialog();
            click(chooser,"industrial-campaign-choice");
            shot("campaign-choice");
            chooser.buttons.getChildren().first().fireClick();
            if(Vars.ui.planet.state.planet!=planet) throw "Campaign choice did not select industrial planet";
        });
        later(3, function(){
            shot("planet-portrait"); click(Vars.ui.planet,"play-sector");
            later(10, function(){
                verify(a);
                if(Team.sharded.core().items.get(Items.copper)!=100) throw "Starting stock is not 100 copper";
                shot("sector-a"); Log.info("INDUSTRIAL_CAMPAIGN_UI_FIRST_SECTOR_OK");
                // Fixture supplies travel costs; the actual launch dialog must debit them.
                const core=Team.sharded.core(); core.items.set(Items.copper,1500); core.items.set(Items.lead,1000);
                const tile=Vars.world.tile(core.tileX()+5,core.tileY()); tile.setBlock(Blocks.graphitePress,Team.sharded); tile.build.items.add(Items.coal,7);
                const tx=tile.x,ty=tile.y;
                Vars.ui.planet.show(); Vars.ui.planet.selectSector(b); Vars.ui.planet.lookAt(b);
                later(3, function(){
                    shot("neighbor-portrait"); click(Vars.ui.planet,"play-sector");
                    later(2, function(){
                        shot("paid-launch"); click(Vars.ui.planet.loadouts,"launch-sector-confirm");
                        later(10, function(){
                            verify(b); if(a.info.items.get(Items.copper)>=1500) throw "Travel was not paid";
                            shot("sector-b"); Log.info("INDUSTRIAL_CAMPAIGN_UI_SECOND_SECTOR_OK");
                            Core.graphics.setWindowSize(1200,720);
                            Vars.ui.planet.show(); Vars.ui.planet.selectSector(a); Vars.ui.planet.lookAt(a);
                            later(3,function(){
                                shot("planet-landscape"); click(Vars.ui.planet,"play-sector");
                                later(10,function(){
                                    verify(a); if(Vars.world.tile(tx,ty).build==null || Vars.world.tile(tx,ty).block()!=Blocks.graphitePress) throw "Factory missing after return";
                                    if(Team.sharded.core().items.get(Items.copper)!=500) throw "Source inventory did not retain paid cost";
                                    shot("returned-factory"); Log.info("INDUSTRIAL_CAMPAIGN_UI_RETURN_AND_SAVE_OK"); Core.app.exit();
                                });
                            });
                        });
                    });
                });
            });
        });
    });
}));
