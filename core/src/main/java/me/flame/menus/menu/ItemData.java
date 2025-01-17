package me.flame.menus.menu;

import com.google.common.collect.ImmutableSet;
import me.flame.menus.items.MenuItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;

/**
 * Represents the data items of a menu
 * <p>
 * Improves the DRY principle by allowing you to add items the same way in {@link Menu} and {@link PaginatedMenu} and others.
 * <p>
 * Contains MenuItem[] and the base menu
 *
 * @since 2.0.0
 */
@SuppressWarnings("UnusedReturnValue")
public class ItemData {
    final Menu menu;
    private MenuItem[] items;
    public ItemData(@NotNull final Menu menu) {
        this.menu = menu;
        this.items = new MenuItem[menu.size];
        if (menu instanceof PaginatedMenu) {
            // check for items that should be on every page.
            PaginatedMenu pmenu = (PaginatedMenu) menu;
            pmenu.setPageItems(this);
        }
    }

    public ItemData(@NotNull final ItemData menu) {
        this.menu = menu.menu;
        this.items = menu.items;
    }

    public MenuItem[] getItems() {
        return Arrays.copyOf(items, items.length);
    }

    public boolean addItem(@NotNull final ItemStack... items) {
        final List<MenuItem> notAddedItems = new ArrayList<>(items.length);

        int slot = 0;
        boolean changed = false;
        for (final ItemStack item : items) {
            MenuItem menuItem = MenuItem.of(item);
            if (this.add(slot, menuItem, notAddedItems)) return changed;
            changed = true;
            slot++;
        }

        checkSizing(notAddedItems);
        return changed;
    }

    /**
     *
     * @param slot the slot
     * @param guiItem the item
     * @param notAddedItems the list of items to add to, if the item wasn't added
     * @return True if the item was added, false otherwise
     */
    private boolean add(int slot, @NotNull final MenuItem guiItem, @NotNull final List<MenuItem> notAddedItems) {
        try {
            while (items[slot] != null) slot++;
        } catch (ArrayIndexOutOfBoundsException ignored) {
            return false;
        }
        if (slot > menu.size) { // if the slot is out of bounds
            //if (menu.rows == 6) return true;
            notAddedItems.add(guiItem);
            return false;
        }
        items[slot] = guiItem;
        return true;
    }

    public boolean addItem(@NotNull final MenuItem... items) {
        return addItem(new ArrayList<>(), items);
    }

    public boolean addItem(final List<MenuItem> notAddedItems, @NotNull final MenuItem... items) {

        int slot = 0;
        boolean changed = false;
        boolean skip = false;
        for (final MenuItem menuItem : items) {
            if(skip) {
                notAddedItems.add(menuItem);
                continue;
            }
            if (this.add(slot, menuItem, notAddedItems)){
                changed = true;
            } else {
                skip = true;
            }
            slot++;
        }

        checkSizing(notAddedItems);
        return changed;
    }


    public boolean addItem(@NotNull final List<MenuItem> items) {
        return addItem(items.toArray(new MenuItem[0]));
    }

    private void checkSizing(List<MenuItem> notAddedItems) {
        if (menu.dynamicSizing && notAddedItems.isEmpty() && (menu.rows < 6 && menu.type == MenuType.CHEST)) {
            this.recreateInventory();
            this.addItem(notAddedItems.toArray(new MenuItem[0]));
            menu.update();
        }
    }

    void recreateInventory() {
        items = Arrays.copyOf(items, menu.size);
    }

    public void contents(MenuItem[] items) {
        this.items = items;
        menu.update();
    }

    public void setItem(int slot, MenuItem item) {
        items[slot] = item;
    }

    public MenuItem getItem(int i) {
        return items[i];
    }

    public void forEach(Consumer<? super MenuItem> action) {
        for (MenuItem item : items) action.accept(item);
    }

    public void indexed(ObjIntConsumer<? super MenuItem> action) {
        for (int index = 0; index < items.length; index++) action.accept(items[index], index);
    }

    public MenuItem findFirst(Predicate<MenuItem> action) {
        for (MenuItem item : items) if (action.test(item)) return item;
        return null;
    }

    public MenuItem removeItem(int index) {
        MenuItem oldItem = items[index];
        items[index] = null;
        return oldItem;
    }

    public boolean hasItem(int slot) {
        return items[slot] != null;
    }

    public MenuItem getItem(Predicate<MenuItem> action) {
        for (MenuItem item : items) if (action.test(item)) return item;
        return null;
    }

    public Optional<MenuItem> get(int index) {
        return Optional.ofNullable(getItem(index));
    }

    public Optional<MenuItem> get(Predicate<MenuItem> action) {
        return Optional.ofNullable(getItem(action));
    }

    public int size() {
        return items.length;
    }

    public void removeItem(Predicate<MenuItem> item) {
        for (int i = 0; i < items.length; i++) {
            if (item.test(items[i])) {
                removeItem(i);
                return;
            }
        }
    }

    public void removeItem(MenuItem @NotNull [] removingItems) {
        Set<MenuItem> items = ImmutableSet.copyOf(removingItems);
        int size = menu.size();

        for (int itemIndex = 0; itemIndex < size && items.contains(this.items[itemIndex]); itemIndex++) {
            this.items[itemIndex] = null;
        }
    }

    public void recreateItems(Inventory inventory) {
        int size = items.length;
        boolean updateStates = menu.updateStatesOnUpdate;
        for (int itemIndex = 0; itemIndex < size; itemIndex++) {
            MenuItem item = items[itemIndex];
            if (item != null && updateStates && item.hasStates()) item.updateStates();
            inventory.setItem(itemIndex, item == null ? null : item.getItemStack());
        }
    }

    public void updateItem(int slot, @NotNull ItemStack itemStack, MenuItem guiItem) {
        if (guiItem == null) {
            items[slot] = MenuItem.of(itemStack);
            return;
        }
        guiItem.setItemStack(itemStack);
        items[slot] = guiItem;
    }

    public void removeItem(ItemStack[] items) {
        indexed((item, index) -> {
            if (items[index].equals(item.getItemStack())) removeItem(index);
        });
    }
}
