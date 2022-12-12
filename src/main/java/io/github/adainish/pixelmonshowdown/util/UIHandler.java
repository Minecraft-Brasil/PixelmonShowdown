package io.github.adainish.pixelmonshowdown.util;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.page.Page;
import ca.landonjw.gooeylibs2.api.template.Template;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
import com.pixelmonmod.pixelmon.api.pokemon.species.Pokedex;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.util.helpers.SpriteItemHelper;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import io.github.adainish.pixelmonshowdown.PixelmonShowdown;
import io.github.adainish.pixelmonshowdown.arenas.Arena;
import io.github.adainish.pixelmonshowdown.arenas.ArenaLocation;
import io.github.adainish.pixelmonshowdown.arenas.ArenaManager;
import io.github.adainish.pixelmonshowdown.battles.MatchMakingManager;
import io.github.adainish.pixelmonshowdown.queues.CompetitiveQueue;
import io.github.adainish.pixelmonshowdown.queues.EloLadder;
import io.github.adainish.pixelmonshowdown.queues.EloProfile;
import io.github.adainish.pixelmonshowdown.queues.QueueManager;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.text.StringTextComponent;

import java.util.*;

public class UIHandler {
    private QueueManager manager = PixelmonShowdown.getInstance().queueManager;
    private String activeQueueFormat = null;
    private Item activeQueueBall = PixelmonItems.poke_ball;
    private String activeArena = null;
    private Item activeArenaBall = PixelmonItems.poke_ball;
    private PluginContainer container = PixelmonShowdown.getContainer();
    private ServerPlayerEntity player;
    private UUID playerUUID;
    private int arenasPageNum = 1;
    private int leaderboardPageNum = 1;
    private int formatsPageNum = 1;
    private final int ELO_FLOOR = DataManager.getConfigNode().node("Elo-Management", "Elo-Range", "Elo-Floor").getInt();
    private Pokemon startingPokemon = null;

    public GooeyButton filler = GooeyButton.builder()
            .display(new ItemStack(Blocks.GRAY_STAINED_GLASS_PANE, 1))
            .build();

    public UIHandler(ServerPlayerEntity player){
        this.player = player;
        this.playerUUID = player.getUniqueID();
    }



    public Page MainPage() {
        Template template;
        ChestTemplate.Builder chestTemplate = ChestTemplate.builder(3)
                .border(0, 0, 3, 9, filler);


        activeQueueFormat = null;
        activeQueueBall = PixelmonItems.poke_ball;
        leaderboardPageNum = 1;

        //no button
        GooeyButton queue;
        if (manager.isPlayerInQueue(playerUUID)) {
            ItemStack itemLeaveQueue = new ItemStack(Items.OAK_BOAT);
            queue = GooeyButton.builder()
                    .title(StringUtil.formattedString("&6Leave Queue"))
                    .onClick(b -> {
                        manager.findPlayerInQueue(playerUUID).remPlayerInQueue(playerUUID);
                        player.sendMessage(new StringTextComponent(StringUtil.formattedString("&f[&4Pixelmon Showdown&f] &6You have left queue!")), playerUUID);
                        UIManager.closeUI(b.getPlayer());
                    })
                    .display(itemLeaveQueue)
                    .build();
            //add to template
        } else if (manager.isPlayerInPreMatch(playerUUID) || manager.isPlayerInMatch(playerUUID)) {
            //Leave Queue
            ItemStack itemInMatch = new ItemStack(Items.RED_WOOL);
            queue = GooeyButton.builder()
                    .title(StringUtil.formattedString("&6You are already in a match!"))
                    .onClick(b -> {
                        UIManager.closeUI(b.getPlayer());
                    })
                    .display(itemInMatch)
                    .build();
        } else {
            //Enter Queue
            ItemStack itemEnterQueue = new ItemStack(PixelmonItems.poke_ball);
            queue = GooeyButton.builder()
                    .title(StringUtil.formattedString("&6Enter Queue"))
                    .onClick(b -> {
                        //if (haspermission open) openQueueGUI else close UI
                        UIManager.closeUI(b.getPlayer());
                    })
                    .display(itemEnterQueue)
                    .build();
        }

        //stats

        Button itemStats = GooeyButton.builder()
                .title(StringUtil.formattedString("&c"))
                .display(new ItemStack(Items.PAPER))
                .onClick(b -> {
                    //needed perm: pixelmonshowdown.user.action.openstats
                    //openStatsGUI();
                    // else
                    UIManager.closeUI(b.getPlayer());

                })
                .lore(StringUtil.formattedArrayList(Arrays.asList("&7")))
                .build();

        Button leaderBoard = GooeyButton.builder()
                .title(StringUtil.formattedString(""))
                .display(new ItemStack(Items.MAP))
                .onClick(b -> {
                    //pixelmonshowdown.user.action.openleaderboard
                    //else no permission
                    UIManager.closeUI(b.getPlayer());
                })
                .build();

        Button itemRules = GooeyButton.builder()
                .title(StringUtil.formattedString("&6Open Rules"))
                .display(new ItemStack(Items.BOOK))
                .onClick(b -> {
                    //pixelmonshowdown.user.action.openrules
                    //else no permission
                    UIManager.closeUI(b.getPlayer());
                })
                .build();


        chestTemplate.set(1, 1, queue);
        chestTemplate.set(1, 3, itemStats);
        chestTemplate.set(1, 5, leaderBoard);
        chestTemplate.set(1, 7, itemRules);
        return GooeyPage.builder().title("Pixelmon Showdown").template(chestTemplate.build()).build();
    }
    public GooeyPage QueueGUI()
    {
        Template template;
        ChestTemplate.Builder chestTemplate = ChestTemplate.builder(3)
                .border(0, 0, 3, 9, filler);

        String title = "&6Format: Not Chosen";
        if (activeQueueFormat == null)
            title = "&6Format: " + activeQueueFormat;

        GooeyButton queueType = GooeyButton.builder()
                .title(StringUtil.formattedString(title))
                .display(new ItemStack(activeQueueBall))
                .lore(StringUtil.formattedArrayList(Arrays.asList("&aClick to see available formats")))
                .build();

        ServerPlayerEntity participant = player;
        Pokemon[] party = StorageProxy.getParty(participant).getAll();
        ArrayList<Pokemon> pokemonList = new ArrayList<>();
        for (Pokemon pokemon : party) {
            if (pokemon == null) {
                continue;
            }
            pokemonList.add(pokemon);
        }
        String pokemonItemTitle = "";
        boolean doesValidate = false;
        if(activeQueueFormat == null){
            pokemonItemTitle = "&6Team Eligible: &4Format Not Chosen";
        }
        else{
            BattleRules rules = manager.findQueue(activeQueueFormat).getFormat().getBattleRules();

            if(rules.validateTeam(pokemonList) == null){
                doesValidate = true;
            }

            if(!doesValidate) {
                pokemonItemTitle = "&4Team is not Eligible";
            }
            else{
                pokemonItemTitle = "&aTeam Eligible";
            }
        }

        GooeyButton pokemonValidation = GooeyButton.builder()
                .display(new ItemStack(Items.WRITABLE_BOOK))
                .title(StringUtil.formattedString(pokemonItemTitle))
                .build();

        GooeyButton queueButton;
        if(doesValidate) {
            queueButton = GooeyButton.builder()
                    .display(new ItemStack(Items.LIME_WOOL))
                    .title(StringUtil.formattedString("&6Confirm"))
                    .onClick(b -> {
                        CompetitiveQueue queue = manager.findQueue(activeQueueFormat);
                        if(queue != null){
                            queue.addPlayerInQueue(playerUUID);
                            MatchMakingManager.runTask();
                            player.sendMessage(new StringTextComponent(StringUtil.formattedString("&f[&6Pixelmon Showdown&f] &6You have entered queue!")), playerUUID);
                            UIManager.closeUI(b.getPlayer());
                        }
                    })
                    .build();
        }
        else{
            queueButton = GooeyButton.builder()
                    .display(new ItemStack(Items.RED_WOOL))
                    .title(StringUtil.formattedString("&6Can't Confirm! Select Format or Eligible Team"))
                    .build();
        }

        chestTemplate.set(1, 3, queueType);
        chestTemplate.set(1, 5, pokemonValidation);
        chestTemplate.set(1, 7, queueButton);

        return GooeyPage.builder().title(StringUtil.formattedString("&4Queues")).template(chestTemplate.build()).build();
    }



    public GooeyPage StatsGUI()
    {
        Template template;
        ChestTemplate.Builder chestTemplate = ChestTemplate.builder(3)
                .border(0, 0, 3, 9, filler);

        formatsPageNum = 1;
        String title = "";
        ItemStack itemQueueTypeButton = new ItemStack(activeQueueBall);
        if(activeQueueFormat == null){
            title = "&6Format: Not Chosen";
        }
        else {
            title = "&6Format: " + activeQueueFormat;
        }

        GooeyButton itemQueueType = GooeyButton.builder()
                .display(itemQueueTypeButton)
                .title(StringUtil.formattedString(title))
                .onClick(b ->
                {
                    UIManager.openUIForcefully(b.getPlayer(), openFormatList("StatsGUI"));
                })
                .lore(StringUtil.formattedArrayList(Arrays.asList("&aClick to see available formats!")))
                .build();

        ItemStack eloButtonStack = new ItemStack(activeQueueBall);
        String eloString = "";
        if(activeQueueFormat == null){
            eloString = "&6Elo: Format Not Chosen";
        }
        else {
            CompetitiveQueue queue = manager.findQueue(activeQueueFormat);
            if(queue != null) {
                if(queue.getLadder().hasPlayer(playerUUID)){
                    EloProfile playerProfile = queue.getLadder().getProfile(playerUUID);
                    if (playerProfile.getWins() == 0 && playerProfile.getLosses() == 0 && playerProfile.getElo() == ELO_FLOOR) {
                        eloString = "&6Elo: No games recorded!";
                    } else {
                        eloString = "&6Elo: " + playerProfile.getElo();
                    }
                }
                else{
                    eloString = "&6Elo: No games recorded!";
                }
            }
        }

        GooeyButton eloButton = GooeyButton.builder()
                .display(eloButtonStack)
                .title(StringUtil.formattedString(eloString))
                .build();

        ItemStack itemWinsStack = new ItemStack(Items.LIME_WOOL);
        String winsTitle = "";
        if(activeQueueFormat == null){
            winsTitle = "&6Wins: Format Not Chosen";
        }
        else {
            CompetitiveQueue queue = manager.findQueue(activeQueueFormat);
            if(queue != null) {
                if(queue.getLadder().hasPlayer(playerUUID)){
                    EloProfile playerProfile = queue.getLadder().getProfile(playerUUID);
                    if (playerProfile.getWins() == 0 && playerProfile.getLosses() == 0 && playerProfile.getElo() == ELO_FLOOR) {
                        winsTitle = "&6Wins: No games recorded!";
                    } else {
                        winsTitle = "&6Wins: " + playerProfile.getWins();
                    }
                }
                else{
                    winsTitle = "&6Wins: No games recorded!";
                }
            }
        }
        GooeyButton winsButton = GooeyButton.builder()
                .title(StringUtil.formattedString(winsTitle))
                .display(itemWinsStack)
                .build();


        ItemStack itemLosses = new ItemStack(Items.RED_WOOL);
        String lossesTitle = "";
        if(activeQueueFormat == null){
            lossesTitle = "&6Losses: Format Not Chosen";
        }
        else {
            CompetitiveQueue queue = manager.findQueue(activeQueueFormat);
            if(queue != null) {
                if(queue.getLadder().hasPlayer(playerUUID)){
                    EloProfile playerProfile = queue.getLadder().getProfile(playerUUID);
                    if (playerProfile.getWins() == 0 && playerProfile.getLosses() == 0 && playerProfile.getElo() == ELO_FLOOR) {
                        lossesTitle = "&6Losses: No games recorded!";
                    } else {
                        lossesTitle = "&6Losses: " + playerProfile.getLosses();
                    }
                }
                else{
                    lossesTitle = "&6Losses: No games recorded!";
                }
            }
        }

        GooeyButton losses = GooeyButton.builder()
                .title(StringUtil.formattedString(lossesTitle))
                .display(itemLosses)
                .build();

        ItemStack itemWinrate = new ItemStack(Items.WHITE_WOOL);
        String winRateTitle = "";
        if(activeQueueFormat == null){
            winRateTitle = "&6Winrate: Format Not Chosen";
        }
        else {
            CompetitiveQueue queue = manager.findQueue(activeQueueFormat);
            if(queue != null) {
                if(queue.getLadder().hasPlayer(playerUUID)){
                    EloProfile playerProfile = queue.getLadder().getProfile(playerUUID);
                    if (playerProfile.getWins() == 0 && playerProfile.getLosses() == 0 && playerProfile.getElo() == ELO_FLOOR) {
                        winRateTitle = "&6Winrate: No games recorded!";
                    } else {
                        winRateTitle = "&6Winrate: " + playerProfile.getWinRate();
                    }
                }
                else{
                    winRateTitle = "&6Winrate: No games recorded!";
                }
            }
        }

        GooeyButton winRateButton = GooeyButton.builder()
                .display(itemWinrate)
                .title(StringUtil.formattedString(winRateTitle))
                .build();

        ItemStack itemReset = new ItemStack(Items.WRITABLE_BOOK);
        String itemResetTitle = "&6Reset Stats";

        GooeyButton reset = GooeyButton.builder()
                .title(StringUtil.formattedString(itemResetTitle))
                .display(itemReset)
                .onClick(b -> {
                    if(PermissionUtil.checkPerm(player, "pixelmonshowdown.user.action.resetwl")) {
                        CompetitiveQueue queue = manager.findQueue(activeQueueFormat);
                        if(queue != null){
                            if(queue.getLadder().hasPlayer(playerUUID)){
                                EloLadder ladder = queue.getLadder();
                                EloProfile playerProfile = ladder.getProfile(playerUUID);
                                playerProfile.resetWL();
                                ladder.addAsActive(playerUUID);
                                UIManager.openUIForcefully(b.getPlayer(), StatsGUI());
                            }
                        }
                    }
                    else{
                        player.sendMessage(new StringTextComponent(StringUtil.formattedString("&4You do not have permission to do this!")), playerUUID);
                        UIManager.closeUI(player);
                    }
                })
                .build();

        GooeyButton back = GooeyButton.builder()
                .display(new ItemStack(PixelmonItems.red_card))
                .title(StringUtil.formattedString("&6Go Back"))
                .onClick(b -> {
                    UIManager.openUIForcefully(b.getPlayer(), MainPage());
                })
                .build();

        chestTemplate.set(1, 1, itemQueueType);
        chestTemplate.set(1, 2, eloButton);
        chestTemplate.set(1, 3, winsButton);
        chestTemplate.set(1, 4, losses);
        chestTemplate.set(1, 5, winRateButton);
        chestTemplate.set(1, 6, reset);
        chestTemplate.set(1, 7, back);

        return GooeyPage.builder().title(StringUtil.formattedString("&4Stats")).template(chestTemplate.build()).build();
    }

    public GooeyPage RulesGUI()
    {
        ChestTemplate.Builder chestTemplate = ChestTemplate.builder(3)
                .border(0, 0, 3, 9, filler);

        formatsPageNum = 1;

        ItemStack itemQueueTypeStack = new ItemStack(activeQueueBall);
        String itemQueueTitle = "";
        if(activeQueueFormat == null){
            itemQueueTitle = "&6Format: Not Chosen";
        }
        else {
            itemQueueTitle = "&6Format: " + activeQueueFormat;
        }
        GooeyButton itemQueueType = GooeyButton.builder()
                .display(itemQueueTypeStack)
                .title(StringUtil.formattedString(itemQueueTitle))
                .lore(StringUtil.formattedArrayList(Arrays.asList("&aClick to see available formats!")))
                .onClick(b -> {
                    UIManager.openUIForcefully(b.getPlayer(), openFormatList("RulesGUI"));
                })
                .build();

        ItemStack itemRulesClausesStack = new ItemStack(Items.KNOWLEDGE_BOOK);
        String itemRulesClausesTitle = "";
        List<String> clausesDisplay = new ArrayList<>();
        if(activeQueueFormat == null){
            itemRulesClausesTitle = "&6Format Not Chosen";
        }
        else {
            itemRulesClausesTitle = "&6Rules Clauses:";
            List<String> clauses = manager.findQueue(activeQueueFormat).getFormat().getStrBattleRules();
            for (String clause : clauses) {
                clausesDisplay.add("&7" + clause);
            }
        }
        GooeyButton itemRules = GooeyButton.builder()
                .lore(StringUtil.formattedArrayList(clausesDisplay))
                .title(StringUtil.formattedString(itemRulesClausesTitle))
                .display(itemRulesClausesStack)
                .build();

        ItemStack itemPokemonClausesStack = new ItemStack(Items.BOOK);
        String itemPokemonClausesTitle = "";
        List<String> pokemonClausesDisplay = new ArrayList<>();
        if(activeQueueFormat == null){
            itemPokemonClausesTitle = "&6Format Not Chosen";
        }
        else {
            itemPokemonClausesTitle = "&6Pokemon Clauses:";
            List<String> clauses = manager.findQueue(activeQueueFormat).getFormat().getStrPokemonClauses();
            for (String clause : clauses) {
                pokemonClausesDisplay.add("&7" + clause);
            }
        }
        GooeyButton pokemonClauses = GooeyButton.builder()
                .title(StringUtil.formattedString(itemPokemonClausesTitle))
                .lore(StringUtil.formattedArrayList(pokemonClausesDisplay))
                .display(itemPokemonClausesStack)
                .build();

        ItemStack itemAbilityClauses = new ItemStack(Items.BOOK);
        String itemAbilityClausesTitle = "";
        List<String> abilityClausesDisplay = new ArrayList<>();
        if(activeQueueFormat == null){
            itemAbilityClausesTitle = "&6Format Not Chosen";
        }
        else {
            itemAbilityClausesTitle = "&6Ability Clauses:";
            List<String> clauses = manager.findQueue(activeQueueFormat).getFormat().getStrAbilityClauses();

            for (String clause : clauses) {
                abilityClausesDisplay.add("&7" + clause);
            }
        }
        GooeyButton abilityClauses = GooeyButton.builder()
                .title(StringUtil.formattedString(itemAbilityClausesTitle))
                .lore(StringUtil.formattedArrayList(abilityClausesDisplay))
                .display(itemAbilityClauses)
                .build();

        ItemStack itemItemClauses = new ItemStack(Items.BOOK);
        String itemClausesTitle = "";
        List<String> itemClausesDisplay = new ArrayList<>();
        if(activeQueueFormat == null){
            itemClausesTitle = "&6Format Not Chosen";
        }
        else {
            itemClausesTitle = "&6Item Clauses:";
            List<String> clauses = manager.findQueue(activeQueueFormat).getFormat().getStrItemClauses();
            for (String clause : clauses) {
                itemClausesDisplay.add("&7" + clause);
            }

        }

        GooeyButton itemClauses = GooeyButton.builder()
                .title(StringUtil.formattedString(itemClausesTitle))
                .lore(StringUtil.formattedArrayList(itemClausesDisplay))
                .display(itemItemClauses)
                .build();

        String moveClausesTitle = "";
        ItemStack itemMoveClauses = new ItemStack(Items.BOOK);
        List<String> moveClausesDisplay = new ArrayList<>();
        if(activeQueueFormat == null){
            moveClausesTitle = "&6Format Not Chosen";
        }
        else {
            moveClausesTitle = "&6Move Clauses:";
            List<String> clauses = manager.findQueue(activeQueueFormat).getFormat().getStrMoveClauses();

            for (String clause : clauses) {
                moveClausesDisplay.add("&7" + clause);
            }
        }

        GooeyButton moveClauses = GooeyButton.builder()
                .title(StringUtil.formattedString(moveClausesTitle))
                .display(itemMoveClauses)
                .lore(StringUtil.formattedArrayList(moveClausesDisplay))
                .build();

        GooeyButton back = GooeyButton.builder()
                .title(StringUtil.formattedString("&6Go Back"))
                .display(new ItemStack(PixelmonItems.red_card))
                .onClick(b -> {
                    UIManager.openUIForcefully(b.getPlayer(), MainPage());
                })
                .build();


        chestTemplate.set(1, 1, itemQueueType);
        chestTemplate.set(1, 2, itemRules);
        chestTemplate.set(1, 3, pokemonClauses);
        chestTemplate.set(1, 4, itemClauses);
        chestTemplate.set(1, 5, abilityClauses);
        chestTemplate.set(1, 6, moveClauses);
        chestTemplate.set(1, 7, back);

        return GooeyPage.builder().title(StringUtil.formattedString("&4Rules")).template(chestTemplate.build()).build();
    }

    public List<Button> eloProfileGeneration()
    {
        List<Button> buttons = new ArrayList<>();
        if(activeQueueFormat != null) {
            CompetitiveQueue queue = manager.findQueue(activeQueueFormat);
            if (queue != null) {
                EloLadder ladder = queue.getLadder();

                for (EloProfile profile:ladder.getProfiles()) {
                    if (profile == null)
                        continue;

                    ItemStack itemPlayer1 = new ItemStack(Items.PLAYER_HEAD);
                    //add UUID to player skin

                    Button profileButton = GooeyButton.builder()
                            .display(itemPlayer1)
                            .title(StringUtil.formattedString("&6" + profile.getPlayerName()))
                            .lore(StringUtil.formattedArrayList(Arrays.asList("&aElo: " + profile.getElo())))
                            .build();
                    buttons.add(profileButton);
                }
            }
        }

        return buttons;
    }

    public LinkedPage LeaderboardGUI()
    {
        PlaceholderButton placeHolderButton = new PlaceholderButton();
        ChestTemplate.Builder chestTemplate = ChestTemplate.builder(3)
                .border(0, 0, 3, 9, filler);

        LinkedPageButton previous = LinkedPageButton.builder()
                .display(new ItemStack(PixelmonItems.trade_holder_left))
                .title("Previous Page")
                .linkType(LinkType.Previous)
                .build();

        LinkedPageButton next = LinkedPageButton.builder()
                .display(new ItemStack(PixelmonItems.trade_holder_right))
                .title("Next Page")
                .linkType(LinkType.Next)
                .build();

        ItemStack itemQueueType = new ItemStack(activeQueueBall);

        String itemQueueTypeTitle = "";
        if(activeQueueFormat == null){
            itemQueueTypeTitle = "&6Format: Not Chosen";
        }
        else {
            itemQueueTypeTitle = "&6Format: " + activeQueueFormat;
        }
        List<String> lore = new ArrayList<>();
        lore.add("&aClick to see available formats!");

        GooeyButton queueTypeButton = GooeyButton.builder()
                .display(itemQueueType)
                .title(StringUtil.formattedString(itemQueueTypeTitle))
                .lore(StringUtil.formattedArrayList(lore))
                .onClick(b -> {
                    openFormatList("LeaderboardGUI");
                })
                .build();

        GooeyButton back = GooeyButton.builder()
                .title(StringUtil.formattedString("&6Go Back"))
                .display(new ItemStack(PixelmonItems.red_card))
                .onClick(b -> {
                    UIManager.openUIForcefully(b.getPlayer(), MainPage());
                })
                .build();

        if (eloProfileGeneration().size() > 18) {
            chestTemplate = ChestTemplate.builder(5)
                    .border(0, 0, 5, 9, filler)
                    .set(0, 3, previous)
                    .set(0, 1, back)
                    .set(0, 5, next)
                    .set(0, 7, queueTypeButton)
                    .rectangle(1, 1, 3, 7, placeHolderButton);
        } else {
            chestTemplate = ChestTemplate.builder(3)
                    .border(0, 0, 3, 9, filler)
                    .set(0, 1, back)
                    .set(0, 4, queueTypeButton)
                    .row(1, placeHolderButton);
        }
        return PaginationHelper.createPagesFromPlaceholders(chestTemplate.build(), eloProfileGeneration(), LinkedPage.builder().title(StringUtil.formattedString("&4Leaderboard")).template(chestTemplate.build()));
    }

    public GooeyPage ArenasGUI()
    {
        ChestTemplate.Builder chestTemplate = ChestTemplate.builder(3)
                .border(0, 0, 3, 9, filler);


        ItemStack itemArenas = new ItemStack(activeArenaBall);
        String itemArenasTitle = "";
        List<String> lore = new ArrayList<>();
        lore.add("&aClick to see available arenas!");
        if(activeArena == null){
            itemArenasTitle = "&6Arena: Not Chosen";

        }
        else{
            itemArenasTitle = "&6Arena: " + activeArena;
        }

        GooeyButton arenas = GooeyButton.builder()
                .title(StringUtil.formattedString(itemArenasTitle))
                .lore(StringUtil.formattedArrayList(lore))
                .display(itemArenas)
                .onClick(b ->
                {
                    if (activeArena != null)
                    {
                        UIManager.openUIForcefully(b.getPlayer(), openArenasList());
                    }
                })
                .build();

        ItemStack itemLocations = new ItemStack(Items.FILLED_MAP);
        String itemLocationsTitle = "&6Location Management";

        GooeyButton locations = GooeyButton.builder()
                .title(StringUtil.formattedString(itemLocationsTitle))
                .display(itemLocations)
                .onClick(b ->
                {
                    if(activeArena != null){
                        UIManager.openUIForcefully(b.getPlayer(), LocationsGUI());
                    }
                })
                .build();

        chestTemplate.set(1, 3, arenas);
        chestTemplate.set(1, 5, locations);
        return GooeyPage.builder().title(StringUtil.formattedString("&4Arena Management")).template(chestTemplate.build()).build();
    }

    public GooeyPage LocationsGUI()
    {
        ItemStack locAStack = new ItemStack(PixelmonItems.galactic_boots);
        String locATitle = "&6Set Location A";
        GooeyButton locA = GooeyButton.builder()
                .title(StringUtil.formattedString(locATitle))
                .display(locAStack)
                .onClick(b -> {
                    ArenaManager arenaManager = PixelmonShowdown.getInstance().arenaManager;
                    Arena arena = arenaManager.getArena(activeArena);
                    if(arena != null){
                        ArenaLocation locationA = arena.getLocationA();
                        locationA.setWorld(player.getServerWorld().getDimensionKey().toString());
                        locationA.setLocation(player.getPosition());
//                        locationA.setHeadRotation(player.getHeadRotation());
                        arena.saveArena();
                        player.sendMessage(new StringTextComponent(StringUtil.formattedString("&aLocation A updated.")), playerUUID);
                    }
                })
                .build();

        GooeyButton locB = GooeyButton.builder()
                .title(StringUtil.formattedString(locATitle))
                .display(locAStack)
                .onClick(b -> {
                    ArenaManager arenaManager = PixelmonShowdown.getInstance().arenaManager;
                    Arena arena = arenaManager.getArena(activeArena);
                    if(arena != null){
                        ArenaLocation locationB = arena.getLocationB();
                        locationB.setWorld(player.getServerWorld().getDimensionKey().toString());
                        locationB.setLocation(player.getPosition());
//                        locationB.setHeadRotation(player.getHeadRotation());
                        arena.saveArena();
                        player.sendMessage(new StringTextComponent(StringUtil.formattedString("&aLocation B updated.")), playerUUID);
                    }
                })
                .build();

        GooeyButton back = GooeyButton.builder()
                .title(StringUtil.formattedString("&6Go Back"))
                .display(new ItemStack(PixelmonItems.red_card))
                .onClick(b -> {
                    UIManager.openUIForcefully(b.getPlayer(), ArenasGUI());
                })
                .build();


        ChestTemplate.Builder chestTemplate = ChestTemplate.builder(3)
                .border(0, 0, 3, 9, filler);
        chestTemplate.set(1, 3, locA);
        chestTemplate.set(1, 5, locB);
        chestTemplate.set(1, 7, back);

        return GooeyPage.builder().title(StringUtil.formattedString("&4Location Management")).template(chestTemplate.build()).build();
    }

    public List<Button> arenaViewButtons()
    {
        ArenaManager arenaManager = PixelmonShowdown.getInstance().arenaManager;
        arenaManager.sortArenas();
        ArrayList<Arena> arenas = arenaManager.getArenas();
        List<Button> buttons = new ArrayList<>();
        for (Arena arena:arenas) {
            if(arena != null){
                ItemStack ball = new ItemStack(activeArenaBall);
                Button arenaButton = GooeyButton.builder()
                        .display(ball)
                        .title(StringUtil.formattedString("&6" + arena.getName()))
                        .onClick(b -> {
                            activeArena = arena.getName();
                        })
                        .build();
                buttons.add(arenaButton);
            }
        }

        return buttons;
    }

    public LinkedPage ViewArenasGUI()
    {
        PlaceholderButton placeHolderButton = new PlaceholderButton();
        ChestTemplate.Builder chestTemplate = ChestTemplate.builder(3)
                .border(0, 0, 3, 9, filler);

        LinkedPageButton previous = LinkedPageButton.builder()
                .display(new ItemStack(PixelmonItems.trade_holder_left))
                .title("Previous Page")
                .linkType(LinkType.Previous)
                .build();

        LinkedPageButton next = LinkedPageButton.builder()
                .display(new ItemStack(PixelmonItems.trade_holder_right))
                .title("Next Page")
                .linkType(LinkType.Next)
                .build();

        ItemStack itemQueueType = new ItemStack(activeQueueBall);

        String itemQueueTypeTitle = "";
        if(activeQueueFormat == null){
            itemQueueTypeTitle = "&6Format: Not Chosen";
        }
        else {
            itemQueueTypeTitle = "&6Format: " + activeQueueFormat;
        }
        List<String> lore = new ArrayList<>();
        lore.add("&aClick to see available formats!");

        GooeyButton queueTypeButton = GooeyButton.builder()
                .display(itemQueueType)
                .title(StringUtil.formattedString(itemQueueTypeTitle))
                .lore(StringUtil.formattedArrayList(lore))
                .onClick(b -> {
                    openFormatList("LeaderboardGUI");
                })
                .build();

        GooeyButton back = GooeyButton.builder()
                .title(StringUtil.formattedString("&6Go Back"))
                .display(new ItemStack(PixelmonItems.red_card))
                .onClick(b -> {
                    UIManager.openUIForcefully(b.getPlayer(), MainPage());
                })
                .build();

        if (arenaViewButtons().size() > 18) {
            chestTemplate = ChestTemplate.builder(5)
                    .border(0, 0, 5, 9, filler)
                    .set(0, 3, previous)
                    .set(0, 1, back)
                    .set(0, 5, next)
                    .set(0, 7, queueTypeButton)
                    .rectangle(1, 1, 3, 7, placeHolderButton);
        } else {
            chestTemplate = ChestTemplate.builder(3)
                    .border(0, 0, 3, 9, filler)
                    .set(0, 1, back)
                    .set(0, 4, queueTypeButton)
                    .row(1, placeHolderButton);
        }
        return PaginationHelper.createPagesFromPlaceholders(chestTemplate.build(), arenaViewButtons(), LinkedPage.builder().title(StringUtil.formattedString("&4Arena")).template(chestTemplate.build()));
    }

    public List<Button> arenaList(ArrayList<Arena> arenas)
    {
        List<Button> buttons = new ArrayList<>();
        for (Arena arena:arenas) {
            GooeyButton button = GooeyButton.builder()
                    .title(StringUtil.formattedString(arena.getName()))
                    .display(new ItemStack(PixelmonItems.poke_ball))
                    .onClick(b ->
                    {
                        activeArena = arena.getName();
                        UIManager.openUIForcefully(b.getPlayer(), ArenasGUI());
                    })
                    .build();
            buttons.add(button);
        }

        return buttons;
    }

    public LinkedPage openArenasList()
    {
        ArenaManager arenaManager = PixelmonShowdown.getInstance().arenaManager;
        arenaManager.sortArenas();
        ArrayList<Arena> arenas = arenaManager.getArenas();

        PlaceholderButton placeHolderButton = new PlaceholderButton();
        ChestTemplate.Builder chestTemplate = ChestTemplate.builder(3)
                .border(0, 0, 3, 9, filler);

        LinkedPageButton previous = LinkedPageButton.builder()
                .display(new ItemStack(PixelmonItems.trade_holder_left))
                .title("Previous Page")
                .linkType(LinkType.Previous)
                .build();

        LinkedPageButton next = LinkedPageButton.builder()
                .display(new ItemStack(PixelmonItems.trade_holder_right))
                .title("Next Page")
                .linkType(LinkType.Next)
                .build();

        GooeyButton createArena = GooeyButton.builder()
                .title(StringUtil.formattedString("&6Create Arena"))
                .display(new ItemStack(PixelmonItems.pokemon_editor))
                .onClick(b -> {
                    arenaManager.addArena();
                    UIManager.openUIForcefully(b.getPlayer(), openArenasList());
                })
                .build();
        if (arenaList(arenas).size() > 18) {
            chestTemplate = ChestTemplate.builder(5)
                    .border(0, 0, 5, 9, filler)
                    .set(0, 1, createArena)
                    .set(0, 3, previous)
                    .set(0, 5, next)
                    .rectangle(1, 1, 3, 7, placeHolderButton);
        } else {
            chestTemplate = ChestTemplate.builder(3)
                    .border(0, 0, 3, 9, filler)
                    .set(0, 1, createArena)
                    .row(1, placeHolderButton);
        }
        return PaginationHelper.createPagesFromPlaceholders(chestTemplate.build(),
                arenaList(arenas),
                LinkedPage.builder().title(StringUtil.formattedString("&4Arenas"))
                        .template(chestTemplate.build()));
    }


    public GooeyPage openFormatList(String fromGUI)
    {
        Object[] formats = manager.getAllQueues().keySet().toArray();

        if(DataManager.getConfigNode().node("GUI-Management", "Custom-Listing-Enabled").getBoolean()){
            Object[] newFormats = new Object[formats.length];
            QueueManager queueManager = PixelmonShowdown.getInstance().queueManager;
            for(int i = 0; i < newFormats.length; i++){
                for(int k = 0; k < newFormats.length; k++){
                    String strFormatName = (String) formats[k];
                    if(queueManager.findQueue(strFormatName).getFormat().getPositionNum() == i){
                        newFormats[i] = formats[k];
                    }
                }
            }
            formats = newFormats;
        }

        ChestTemplate.Builder chestTemplate = ChestTemplate.builder(3)
                .border(0, 0, 3, 9, filler);


        for (Object obj:formats) {

        }

        for (int i = 0; i < 5; i++) {

        }

        return PaginationHelper.createPagesFromPlaceholders(chestTemplate.build(),
                ,
                LinkedPage.builder().title(StringUtil.formattedString("&4"))
                        .template(chestTemplate.build()));
    }


    public void openFormatList(String fromGUI){
        Object[] formats = manager.getAllQueues().keySet().toArray();

        if(DataManager.getConfigNode().node("GUI-Management", "Custom-Listing-Enabled").getBoolean()){
            Object[] newFormats = new Object[formats.length];
            QueueManager queueManager = PixelmonShowdown.getInstance().queueManager;
            for(int i = 0; i < newFormats.length; i++){
                for(int k = 0; k < newFormats.length; k++){
                    String strFormatName = (String) formats[k];
                    if(queueManager.findQueue(strFormatName).getFormat().getPositionNum() == i){
                        newFormats[i] = formats[k];
                    }
                }
            }
            formats = newFormats;
        }

        HashMap<Integer, Element> elements = new HashMap<>();

        ItemStack itemPrevious = ItemStack.of((ItemType) PixelmonItems.LtradeHolderLeft, 1);
        itemPrevious.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, "Previous"));
        Element previous;
        if(formatsPageNum > 1){
            Consumer<Action.Click> previousFormats = action -> {
                formatsPageNum--;
                openFormatList(fromGUI);
            };
            previous = Element.of(itemPrevious, previousFormats);
        }
        else{
            previous = Element.of(itemPrevious);
        }
        elements.put(10, previous);

        for(int i = 0; i < 5; i++){
            if(formats.length > (formatsPageNum - 1) * 5 + i){
                String format = (String) formats[(formatsPageNum - 1) * 5 + i];
                if(format != null){
                    int pokeBallIndex = ((formatsPageNum - 1) * 5 + i) % PokeBallRegistry.getAll().size();
                    ItemStack pokeBall = PokeBallRegistry.getAll().get(pokeBallIndex).getBallItem();
                    itemFormat.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, format));
                    Consumer<Action.Click> selectArena = action -> {
                        activeQueueFormat = format;
                        activeQueueBall = pokeBall.getItem();
                        switch (fromGUI) {
                            case "QueueGUI":
                                UIManager.openUIForcefully(player, QueueGUI());
                                break;
                            case "StatsGUI":
                                UIManager.openUIForcefully(player, StatsGUI());
                                break;
                            case "RulesGUI":
                                UIManager.openUIForcefully(player, RulesGUI());
                                break;
                            case "LeaderboardGUI":
                                UIManager.openUIForcefully(player, LeaderboardGUI());
                                break;
                        }
                    };
                    Element elementArena = Element.of(itemFormat, selectArena);
                    elements.put(11 + i, elementArena);
                }
            }

            ItemStack itemNext = ItemStack.of((ItemType) PixelmonItems.tradeHolderRight, 1);
            itemNext.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, "Next"));
            Element next;
            if(formatsPageNum * 5 < formats.length){
                Consumer<Action.Click> nextFormats = action -> {
                    formatsPageNum++;
                    openFormatList(fromGUI);
                };
                next = Element.of(itemNext, nextFormats);
            }
            else{
                next = Element.of(itemNext);
            }
            elements.put(16, next);
        }

        ItemStack itemBorder = ItemStack.of(ItemTypes.STAINED_GLASS_PANE,1);
        itemBorder.offer(Keys.DYE_COLOR, DyeColors.RED);
        itemBorder.offer(Keys.DISPLAY_NAME, Text.of(""));
        Element border = Element.of(itemBorder);

        Layout newLayout = Layout.builder().setAll(elements).border(border).build();

        View view = View.builder().archetype(InventoryArchetypes.CHEST)
                .property(InventoryTitle.of(Text.of(TextColors.RED, TextStyles.BOLD, "Formats"))).build(container);

        view.define(newLayout);
        view.open(player);
    }

    public void openTeamPreview(UUID opponentUUID){
        HashMap<Integer, Element> elements = new HashMap<>();

        ItemStack itemOpponent = ItemStack.of((ItemType) PixelmonItems.trainerEditor,1);

        String displayName = "null";
        Optional<UserStorageService> userStorage = Sponge.getServiceManager().provide(UserStorageService.class);
        if(userStorage.get().get(opponentUUID).isPresent()) {
            User user = userStorage.get().get(opponentUUID).get();
            displayName = user.getName();
        }
        itemOpponent.offer(Keys.DISPLAY_NAME, Text.of(TextColors.RED, displayName));
        Element elementOpponent = Element.of(itemOpponent);
        elements.put(1, elementOpponent);

        if(Sponge.getServer().getPlayer(opponentUUID).isPresent()) {
            Player opponent = Sponge.getServer().getPlayer(opponentUUID).get();
            EntityPlayerMP oppParticipant = (EntityPlayerMP) opponent;
            Pokemon[] oppParty = Pixelmon.storageManager.getParty(oppParticipant).getAll();
            ArrayList<Pokemon> oppPokemonList = new ArrayList<>();
            for (int i = 0; i < oppParty.length; ++i) {
                if (oppParty[i] == null) {
                    continue;
                }
                oppPokemonList.add(oppParty[i]);
            }

            for (int i = 0; i < oppPokemonList.size(); i++) {
                Pokemon pokemon = oppPokemonList.get(i);
                ItemStack itemPokemon = getPokemonPhoto(pokemon, pokemon.getForm());
                itemPokemon.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, pokemon.getSpecies().name));
                Element elementPokemon = Element.of(itemPokemon);
                elements.put(2 + i, elementPokemon);
            }
        }

        ServerPlayerEntity playerParticipant =  player;
        Pokemon[] playerParty = StorageProxy.getParty(playerParticipant).getAll();
        ArrayList<Pokemon> playerPokemonList = new ArrayList<>();
        for(int i = 0; i < playerParty.length; i++){
            if(playerParty[i] == null) {
                continue;
            }
            playerPokemonList.add(playerParty[i]);
        }

        for (int i = 0; i < playerPokemonList.size(); i++) {
            Pokemon pokemon = playerPokemonList.get(i);
            ItemStack itemPokemon = getPokemonPhoto(pokemon);
            itemPokemon.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, pokemon.getSpecies().name));
            int index = i;
            Consumer<Action.Click> consChangeStarter = action -> {
                this.startingPokemon = playerPokemonList.get(index);
                openTeamPreview(opponentUUID);
            };
            Element elementPokemon = Element.of(itemPokemon, consChangeStarter);
            elements.put(20 + i, elementPokemon);
        }
        if(startingPokemon == null) {
            ItemStack itemStarter = ItemStack.of((ItemType) PixelmonItemsPokeballs.pokeBall, 1);
            itemStarter.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, "Starting Pokemon: " + playerPokemonList.get(0).getSpecies().name));
            Element starter = Element.of(itemStarter);
            elements.put(19, starter);
        }
        else{
            ItemStack itemStarter = ItemStack.of((ItemType) PixelmonItemsPokeballs.pokeBall, 1);
            itemStarter.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GOLD, "Starting Pokemon: " + startingPokemon.getSpecies().name));
            Element starter = Element.of(itemStarter);
            elements.put(19, starter);
        }

        ItemStack itemBorder = ItemStack.of(ItemTypes.STAINED_GLASS_PANE,1);
        itemBorder.offer(Keys.DYE_COLOR, DyeColors.RED);
        itemBorder.offer(Keys.DISPLAY_NAME, Text.of(""));

        Element border = Element.of(itemBorder);

        Layout newLayout = Layout.builder().setAll(elements).row(border, 1).set(border, 0, 8, 18, 26).build();

        View view = View.builder().archetype(InventoryArchetypes.CHEST)
                .property(InventoryTitle.of(Text.of(TextColors.RED, TextStyles.BOLD, "Team Preview"))).build(container);

        view.define(newLayout);
        view.open(player);
    }

    public ItemStack getPokemonPhoto(Pokemon pokemon)
    {
        return SpriteItemHelper.getPhoto(pokemon);
    }

    public Pokemon getStartingPokemon(){
        return startingPokemon;
    }
}