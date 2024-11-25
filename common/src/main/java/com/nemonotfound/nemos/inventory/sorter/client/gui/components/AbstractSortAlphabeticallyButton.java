package com.nemonotfound.nemos.inventory.sorter.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AbstractSortAlphabeticallyButton extends AbstractWidget {


    private final AbstractContainerScreen<?> containerScreen;

    public AbstractSortAlphabeticallyButton(int x, int y, int width, int height, Component message, AbstractContainerScreen<?> containerScreen) {
        super(x, y, width, height, message);
        this.containerScreen = containerScreen;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.isHovered()) {
            guiGraphics.blitSprite(RenderType::guiTextured, getButtonHoverTexture(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        } else {
            guiGraphics.blitSprite(RenderType::guiTextured, getButtonTexture(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }
    }

    protected abstract ResourceLocation getButtonHoverTexture();
    protected abstract ResourceLocation getButtonTexture();

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {

    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
        sortItems();
    }

    private void sortItems() {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractContainerMenu menu = containerScreen.getMenu();
        int containerId = menu.containerId;

        mergeAllItems(menu, containerId, minecraft);
        Map<Integer, Integer> sortedItemMap = createSortedItemMap(menu);

        swapItemsUntilSorted(sortedItemMap, minecraft, containerId);
    }

    private void mergeAllItems(AbstractContainerMenu menu, int containerId, Minecraft minecraft) {
        Map<String, List<Map.Entry<Integer, ItemStack>>> groupedItemMap = getSortedSlotItems(menu).stream()
                .collect(Collectors.groupingBy(itemMap -> itemMap.getValue().getItemName().getString()));

        groupedItemMap.forEach((key, mapEntryList) -> mergeItems(mapEntryList, menu, containerId, minecraft));
    }

    private Map<Integer, Integer> createSortedItemMap(AbstractContainerMenu menu) {
        List<Map.Entry<Integer, ItemStack>> itemMapEntries = getSortedSlotItems(menu);
        Map<Integer, Integer> sortedItemMap = new LinkedHashMap<>();

        for (int newSlot = 0; newSlot < itemMapEntries.size(); newSlot++) {
            int currentSlot = itemMapEntries.get(newSlot).getKey();
            if (currentSlot != newSlot) {
                sortedItemMap.put(currentSlot, newSlot);
            }
        }

        return sortedItemMap;
    }

    //TODO: Refactor
    private void swapItemsUntilSorted(Map<Integer, Integer> sortedItemMap, Minecraft minecraft, int containerId) {
        while (!sortedItemMap.isEmpty()) {
            Iterator<Map.Entry<Integer, Integer>> slotsIterator = sortedItemMap.entrySet().iterator();

            if (slotsIterator.hasNext()) {
                Map.Entry<Integer, Integer> slotsEntry = slotsIterator.next();
                int currentSlot = slotsEntry.getKey();
                int targetSlot = slotsEntry.getValue();

                if (currentSlot == targetSlot) {
                    slotsIterator.remove();
                    continue;
                }

                swapItems(minecraft.gameMode, containerId, currentSlot, targetSlot, minecraft.player);

                if (sortedItemMap.containsKey(targetSlot)) {
                    sortedItemMap.put(currentSlot, sortedItemMap.get(targetSlot));
                } else {
                    slotsIterator.remove();
                }

                sortedItemMap.put(targetSlot, targetSlot);
            }
        }
    }

    private @NotNull List<Map.Entry<Integer, ItemStack>> getSortedSlotItems(AbstractContainerMenu menu) {
        NonNullList<Slot> slots = menu.slots;

        return IntStream.range(0, getContainerSize(menu))
                .mapToObj(slotIndex -> Map.entry(slotIndex, slots.get(slotIndex).getItem()))
                .filter(itemStackEntry -> !itemStackEntry.getValue().is(Items.AIR))
                .sorted(compare())
                .toList();

    }

    private int getContainerSize(AbstractContainerMenu menu) {
        if (menu instanceof ChestMenu) {
            return ((ChestMenu) menu).getContainer().getContainerSize();
        } else if (menu instanceof ShulkerBoxMenu) {
            return 27;
        }

        return 0;
    }

    protected Comparator<Map.Entry<Integer, ItemStack>> compare() {
        return Comparator.comparing(entry -> entry.getValue().getItemName().getString());
    }

    private void mergeItems(List<Map.Entry<Integer, ItemStack>> mapEntryList, AbstractContainerMenu menu, int containerId, Minecraft minecraft) {
        if (mapEntryList.size() <= 1) {
            return;
        }

        int leftSlotIndex = 0;
        int rightSlotIndex = mapEntryList.size() - 1;

        while (leftSlotIndex < rightSlotIndex) {
            Map.Entry<Integer, ItemStack> leftSlotEntry = mapEntryList.get(leftSlotIndex);
            Map.Entry<Integer, ItemStack> rightSlotEntry = mapEntryList.get(rightSlotIndex);
            Slot leftSlot = menu.slots.get(leftSlotEntry.getKey());
            Slot rightSlot = menu.slots.get(rightSlotEntry.getKey());
            ItemStack leftItem = leftSlot.getItem();

            if (leftItem.getCount() < leftItem.getMaxStackSize()) {
                swapItems(minecraft.gameMode, containerId, rightSlotEntry.getKey(), leftSlotEntry.getKey(), minecraft.player);
            } else {
                leftSlotIndex++;
            }

            if (rightSlot.getItem().is(Items.AIR)) {
                rightSlotIndex--;
            }
        }
    }

    private void swapItems(MultiPlayerGameMode gameMode, int containerId, int slot, int targetSlot, LocalPlayer player) {
        gameMode.handleInventoryMouseClick(containerId, slot, 0, ClickType.PICKUP, player);
        gameMode.handleInventoryMouseClick(containerId, targetSlot, 0, ClickType.PICKUP, player);
        gameMode.handleInventoryMouseClick(containerId, slot, 0, ClickType.PICKUP, player);
    }
}
