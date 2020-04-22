package me.nickimpact.gts.ui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nickimpact.impactor.api.gui.InventoryDimensions;
import com.nickimpact.impactor.sponge.ui.SpongeIcon;
import com.nickimpact.impactor.sponge.ui.SpongeLayout;
import com.nickimpact.impactor.sponge.ui.SpongeUI;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.nickimpact.gts.api.GTSService;
import me.nickimpact.gts.api.listings.auctions.Auction;
import me.nickimpact.gts.api.listings.direct.QuickPurchase;
import me.nickimpact.gts.api.listings.entries.Entry;
import me.nickimpact.gts.api.placeholders.PlaceholderParser;
import me.nickimpact.gts.api.text.MessageService;
import me.nickimpact.gts.api.util.groupings.Tuple;
import me.nickimpact.gts.common.config.MsgConfigKeys;
import me.nickimpact.gts.common.config.wrappers.SortConfigurationOptions;
import me.nickimpact.gts.common.plugin.GTSPlugin;
import me.nickimpact.gts.common.utils.CircularLinkedList;
import me.nickimpact.gts.common.utils.TitleLorePair;
import me.nickimpact.gts.manager.SpongeListingManager;
import me.nickimpact.gts.sponge.listings.SpongeListing;
import me.nickimpact.gts.sponge.text.SpongeMessageService;
import me.nickimpact.gts.sponge.ui.SpongeAsyncPage;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

public class SpongeMainMenu extends SpongeAsyncPage<SpongeListing> {

	private static final QuickPurchaseOnly QUICK_PURCHASE_ONLY = new QuickPurchaseOnly();
	private static final AuctionsOnly AUCTIONS_ONLY = new AuctionsOnly();

	private static final SpongeMessageService PARSER = (SpongeMessageService) GTSService.getInstance().getServiceManager().get(MessageService.class).get();

	private Class<? extends Entry> filter;

	private Task runner;

	private String searchQuery = "Testing";

	/** True = Quick Purchase, false = Auction */
	private boolean mode = false;
	private CircularLinkedList<Sorter> sorter = Sorter.QUICK_PURCHASE_ONLY.copy();

	public SpongeMainMenu(GTSPlugin plugin, Player viewer) {
		super(plugin, viewer, GTSService.getInstance().getRegistry().get(SpongeListingManager.class).fetchListings());
	}

	@Override
	protected Text getTitle() {
		return Text.of(TextColors.RED, "GTS", TextColors.GRAY, " \u00bb ", TextColors.DARK_AQUA, "Listings");
	}

	@Override
	protected Map<PageIconType, PageIcon<ItemType>> getPageIcons() {
		Map<PageIconType, PageIcon<ItemType>> options = Maps.newHashMap();
		options.put(PageIconType.PREV, new PageIcon<>(ItemTypes.ARROW, 47));
		options.put(PageIconType.NEXT, new PageIcon<>(ItemTypes.ARROW, 53));

		return options;
	}

	@Override
	protected InventoryDimensions getContentZone() {
		return new InventoryDimensions(7, 4);
	}

	@Override
	protected Tuple<Integer, Integer> getOffsets() {
		return new Tuple<>(0, 2);
	}

	@Override
	protected Tuple<Long, TimeUnit> getTimeout() {
		return new Tuple<>((long) 5, TimeUnit.SECONDS);
	}

	@Override
	protected SpongeLayout design() {
		SpongeLayout.SpongeLayoutBuilder slb = SpongeLayout.builder();
		slb.slots(SpongeIcon.BORDER, 1, 10, 19, 28, 37, 38, 39, 40, 41, 42, 42, 43, 44, 9, 36, 46, 48, 52);
		this.createBottomPanel(slb);
		this.createFilterOptions(slb);

		return slb.build();
	}

	@Override
	protected SpongeUI build(SpongeLayout layout) {
		return SpongeUI.builder()
				.title(this.title)
				.dimension(InventoryDimension.of(9, 6))
				.build()
				.define(this.layout);
	}

	@Override
	protected SpongeIcon getLoadingIcon() {
		return new SpongeIcon(ItemStack.builder()
				.itemType(ItemTypes.STAINED_GLASS_PANE)
				.add(Keys.DISPLAY_NAME, Text.of(TextColors.YELLOW, "Fetching Listings..."))
				.add(Keys.DYE_COLOR, DyeColors.YELLOW)
				.build()
		);
	}

	@Override
	protected SpongeIcon getTimeoutIcon() {
		return new SpongeIcon(ItemStack.builder()
				.itemType(ItemTypes.STAINED_GLASS_PANE)
				.add(Keys.DISPLAY_NAME, Text.of(TextColors.RED, "Fetch Timed Out"))
				.add(Keys.DYE_COLOR, DyeColors.RED)
				.add(Keys.ITEM_LORE, Lists.newArrayList(
						Text.of(TextColors.GRAY, "GTS failed to lookup the stored"),
						Text.of(TextColors.GRAY, "listings in a timely manner..."),
						Text.EMPTY,
						Text.of(TextColors.GRAY, "Please retry opening the menu!")
				))
				.build()
		);
	}

	private void createFilterOptions(SpongeLayout.SpongeLayoutBuilder layout) {
//		int size = GTSService.getInstance().getEntryRegistry().getClassifications().size();
//		if(size > 6) {
//
//		} else {
//
//		}
	}

	private void createBottomPanel(SpongeLayout.SpongeLayoutBuilder layout) {
		ItemStack quick = ItemStack.builder()
				.itemType(ItemTypes.EMERALD)
				.add(Keys.DISPLAY_NAME, Text.of(TextColors.GREEN, "Quick Purchase"))
				.add(Keys.ITEM_LORE, Lists.newArrayList(
						Text.of(TextColors.GRAY, "You are currently viewing"),
						Text.of(TextColors.AQUA, "Quick Purchase", TextColors.GRAY, " listings only!"),
						Text.EMPTY,
						Text.of(TextColors.YELLOW, "Click this to change your view"),
						Text.of(TextColors.YELLOW, "of listings to Auctions!")
				))
				.build();
		SpongeIcon qIcon = new SpongeIcon(quick);

		ItemStack auctions = ItemStack.builder()
				.itemType(ItemTypes.GOLD_INGOT)
				.add(Keys.DISPLAY_NAME, Text.of(TextColors.GREEN, "Auctions"))
				.add(Keys.ITEM_LORE, Lists.newArrayList(
						Text.of(TextColors.GRAY, "You are currently viewing"),
						Text.of(TextColors.AQUA, "Auction", TextColors.GRAY, " listings only!"),
						Text.EMPTY,
						Text.of(TextColors.YELLOW, "Click this to change your view"),
						Text.of(TextColors.YELLOW, "to Quick Purchase Listings!")
				))
				.build();
		SpongeIcon aIcon = new SpongeIcon(auctions);

		TitleLorePair pair = GTSPlugin.getInstance().getMsgConfig().get(MsgConfigKeys.MAIN_MENU_SEARCH);
		Map<String, PlaceholderParser> tokens = Maps.newHashMap();
		tokens.put("gts_search_query", placeholder -> LegacyComponentSerializer.legacy().deserialize("Testing"));
		ItemStack searcher = ItemStack.builder()
				.itemType(ItemTypes.SIGN)
				.add(Keys.DISPLAY_NAME, PARSER.getForSource(pair.getTitle(), this.getViewer()))
				.add(Keys.ITEM_LORE, PARSER.getTextListForSource(pair.getLore(), this.getViewer(), tokens))
				.build();
		SpongeIcon sIcon = new SpongeIcon(searcher);
		sIcon.addListener(clickable -> {
			throw new UnsupportedOperationException("Awaiting implementation");
		});
		layout.slot(sIcon, 49);

		SpongeIcon sorter = this.drawSorter();
		layout.slot(sorter, 51);

		qIcon.addListener(clickable -> {
			this.conditions.remove(QUICK_PURCHASE_ONLY);
			this.conditions.add(AUCTIONS_ONLY);

			this.mode = true;
			this.sorter = Sorter.AUCTION_ONLY.copy();
			this.sorter.reset();
			sorter.getDisplay().offer(Keys.ITEM_LORE, PARSER.getTextListForSource(this.craftSorterLore(GTSPlugin.getInstance().getMsgConfig().get(MsgConfigKeys.MAIN_MENU_SORT)), this.getViewer()));
			this.getView().setSlot(51, sorter);

			this.getView().setSlot(50, aIcon);
			this.apply();
		});
		aIcon.addListener(clickable -> {
			this.conditions.add(QUICK_PURCHASE_ONLY);
			this.conditions.remove(AUCTIONS_ONLY);

			this.mode = false;
			this.sorter = Sorter.QUICK_PURCHASE_ONLY.copy();
			this.sorter.reset();
			sorter.getDisplay().offer(Keys.ITEM_LORE, PARSER.getTextListForSource(this.craftSorterLore(GTSPlugin.getInstance().getMsgConfig().get(MsgConfigKeys.MAIN_MENU_SORT)), this.getViewer()));
			this.getView().setSlot(51, sorter);

			this.getView().setSlot(50, qIcon);
			this.apply();
		});

		layout.slot(qIcon, 50);
	}

	private SpongeIcon drawSorter() {
		SortConfigurationOptions options = GTSPlugin.getInstance().getMsgConfig().get(MsgConfigKeys.MAIN_MENU_SORT);

		this.sorter.next();
		ItemStack sorter = ItemStack.builder()
				.itemType(ItemTypes.HOPPER)
				.add(Keys.DISPLAY_NAME, PARSER.getForSource(options.getTitle(), this.getViewer()))
				.add(Keys.ITEM_LORE, PARSER.getTextListForSource(this.craftSorterLore(options), this.getViewer()))
				.build();
		SpongeIcon sortIcon = new SpongeIcon(sorter);
		sortIcon.addListener(clickable -> {
			this.sorter.next();
			sortIcon.getDisplay().offer(Keys.ITEM_LORE, PARSER.getTextListForSource(this.craftSorterLore(options), this.getViewer()));

			this.getView().setSlot(51, sortIcon);
		});
		return sortIcon;
	}

	private List<String> craftSorterLore(SortConfigurationOptions options) {
		List<String> lore = Lists.newArrayList();
		lore.add("");
		for(Sorter sorter : Sorter.getSortOptions(this.mode).getFramesNonCircular()) {
			if(this.sorter.getCurrent().get() == sorter) {
				lore.add(options.getSelectedColor() + "\u25b6 " + sorter.key.apply(options));
			} else {
				lore.add(options.getNonSelectedColor() + sorter.key.apply(options));
			}
		}
		lore.add("");
		lore.add("&eClick to switch sort filter");
		return lore;
	}

	@AllArgsConstructor
	private static class JustPlayer implements Predicate<SpongeListing> {

		private UUID viewer;

		@Override
		public boolean test(SpongeListing listing) {
			return listing.getLister().equals(this.viewer);
		}

	}

	private static class QuickPurchaseOnly implements Predicate<SpongeListing> {

		@Override
		public boolean test(SpongeListing listing) {
			return listing instanceof QuickPurchase;
		}

	}

	private static class AuctionsOnly implements Predicate<SpongeListing> {

		@Override
		public boolean test(SpongeListing listing) {
			return listing instanceof Auction;
		}

	}

	@Getter
	@AllArgsConstructor
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	private enum Sorter {
		QP_MOST_RECENT(SortConfigurationOptions::getQpMostRecent, new Matcher<>(Comparator.comparing(QuickPurchase::getPublishTime).reversed())),
		QP_ENDING_SOON(SortConfigurationOptions::getQpEndingSoon, new Matcher<QuickPurchase>((x, y) -> {
			if(x.getExpiration().isPresent()) {
				if(y.getExpiration().isPresent()) {
					return x.getExpiration().get().compareTo(y.getExpiration().get());
				}

				return -1;
			} else {
				return 1;
			}
		})),

		A_HIGHEST_BID(SortConfigurationOptions::getAHighest, new Matcher<>(Comparator.<Auction, Double>comparing(a -> a.getHighBid().getSecond()).reversed())),
		A_LOWEST_BID(SortConfigurationOptions::getALowest, new Matcher<Auction>(Comparator.comparing(a -> a.getHighBid().getSecond()))),
		A_ENDING_SOON(SortConfigurationOptions::getAEndingSoon, new Matcher<Auction>(Comparator.comparing(a -> a.getExpiration().get()))),
		A_MOST_BIDS(SortConfigurationOptions::getAMostBids, new Matcher<>(Comparator.<Auction, Integer>comparing(a -> a.getBids().size()).reversed()))
		;

		private Function<SortConfigurationOptions, String> key;
		private Matcher<?> comparator;

		private static final CircularLinkedList<Sorter> QUICK_PURCHASE_ONLY = CircularLinkedList.of(QP_MOST_RECENT, QP_ENDING_SOON);
		private static final CircularLinkedList<Sorter> AUCTION_ONLY = CircularLinkedList.of(A_HIGHEST_BID, A_LOWEST_BID, A_ENDING_SOON, A_MOST_BIDS);

		public static CircularLinkedList<Sorter> getSortOptions(boolean mode) {
			return mode ? AUCTION_ONLY : QUICK_PURCHASE_ONLY;
		}
	}

	@Getter
	@AllArgsConstructor
	private static class Matcher<T> {
		private Comparator<T> comparator;
	}

}