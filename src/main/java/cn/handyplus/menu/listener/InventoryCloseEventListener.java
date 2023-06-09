package cn.handyplus.menu.listener;

import cn.handyplus.lib.annotation.HandyListener;
import cn.handyplus.lib.constants.VersionCheckEnum;
import cn.handyplus.lib.core.CollUtil;
import cn.handyplus.lib.core.JsonUtil;
import cn.handyplus.lib.core.StrUtil;
import cn.handyplus.lib.inventory.HandyInventory;
import cn.handyplus.lib.util.BaseUtil;
import cn.handyplus.lib.util.ItemStackUtil;
import cn.handyplus.menu.constants.GuiTypeEnum;
import cn.handyplus.menu.constants.MenuConstants;
import cn.handyplus.menu.enter.MenuItem;
import cn.handyplus.menu.param.MenuButtonParam;
import cn.handyplus.menu.service.MenuItemService;
import cn.handyplus.menu.util.ConfigUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 关闭编辑菜单保存数据
 *
 * @author handy
 */
@HandyListener
public class InventoryCloseEventListener implements Listener {

    /**
     * 关闭gui事件
     *
     * @param event 事件
     */
    @EventHandler
    public void onEvent(InventoryCloseEvent event) throws IOException {
        // 校验
        Inventory inventory = event.getInventory();
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof HandyInventory)) {
            return;
        }
        HandyInventory handyInventory = (HandyInventory) holder;
        if (!GuiTypeEnum.CREATE.getType().equals(handyInventory.getGuiType())) {
            return;
        }
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player) humanEntity;
        int size = handyInventory.getId();

        Map<String, Map<String, Object>> createMenuItemMap = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || Material.AIR.equals(item.getType())) {
                continue;
            }
            ItemMeta itemMeta = ItemStackUtil.getItemMeta(item);
            Map<String, Object> createMenuItem = new LinkedHashMap<>();
            createMenuItem.put("index", i);
            createMenuItem.put("name", BaseUtil.getDisplayName(itemMeta.getDisplayName(), item.getType().name()));
            createMenuItem.put("material", item.getType().name());
            createMenuItem.put("lore", itemMeta.getLore());
            createMenuItem.put("isEnchant", false);
            // 材质id
            if (VersionCheckEnum.getEnum().getVersionId() > VersionCheckEnum.V_1_13.getVersionId()) {
                createMenuItem.put("custom-model-data", itemMeta.hasCustomModelData() ? itemMeta.getCustomModelData() : 0);
            }
            createMenuItem.put("hideFlag", true);
            createMenuItem.put("hideEnchant", true);
            String persistentData = ItemStackUtil.getPersistentData(item, MenuConstants.PREFIX);
            if (StrUtil.isNotEmpty(persistentData)) {
                MenuButtonParam menuButtonParam = JsonUtil.toBean(persistentData, MenuButtonParam.class);
                int money = menuButtonParam.getMoney();
                if (money > 0) {
                    createMenuItem.put("money", money);
                }
                int point = menuButtonParam.getPoint();
                if (point > 0) {
                    createMenuItem.put("point", point);
                }
                int limit = menuButtonParam.getLimit();
                if (limit > 0) {
                    createMenuItem.put("limit", limit);
                }
                List<String> commands = menuButtonParam.getCommands();
                if (CollUtil.isNotEmpty(commands)) {
                    createMenuItem.put("commands", commands);
                }
                List<String> conditions = menuButtonParam.getConditions();
                if (CollUtil.isNotEmpty(conditions)) {
                    createMenuItem.put("conditions", conditions);
                }
                String sound = menuButtonParam.getSound();
                if (StrUtil.isNotEmpty(sound)) {
                    createMenuItem.put("sound", sound);
                }
                String failSound = menuButtonParam.getFailSound();
                if (StrUtil.isNotEmpty(failSound)) {
                    createMenuItem.put("failSound", failSound);
                }
                Integer id = menuButtonParam.getId();
                if (id != null) {
                    createMenuItem.put("id", id);
                }
                Boolean isEnchant = menuButtonParam.getIsEnchant();
                if (isEnchant != null) {
                    createMenuItem.put("isEnchant", isEnchant);
                }
                Boolean hideEnchant = menuButtonParam.getHideEnchant();
                if (hideEnchant != null) {
                    createMenuItem.put("hideEnchant", hideEnchant);
                }
                Boolean hideFlag = menuButtonParam.getHideFlag();
                if (hideFlag != null) {
                    createMenuItem.put("hideFlag", hideFlag);
                }
            }
            if (createMenuItem.get("id") == null) {
                MenuItem menuItem = new MenuItem();
                menuItem.setItemStack(ItemStackUtil.itemStackSerialize(item));
                int menuItemId = MenuItemService.getInstance().add(menuItem);
                createMenuItem.put("id", menuItemId);
            }
            createMenuItemMap.put(i + "", createMenuItem);
        }
        String fileName = handyInventory.getSearchType();
        File file = new File(fileName);
        if (!file.exists()) {
            boolean mkdir = file.createNewFile();
            file = new File(fileName);
        }
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
        if (StrUtil.isEmpty(yamlConfiguration.getString("title"))) {
            yamlConfiguration.set("title", "&7Title");
        }
        if (StrUtil.isEmpty(yamlConfiguration.getString("openCommand"))) {
            yamlConfiguration.set("openCommand", "");
        }
        if (StrUtil.isEmpty(yamlConfiguration.getString("openItem"))) {
            yamlConfiguration.set("openItem", "");
        }
        yamlConfiguration.set("size", size);
        yamlConfiguration.set("menu", createMenuItemMap);
        yamlConfiguration.save(new File(fileName));
        ConfigUtil.init();
        player.sendMessage(BaseUtil.getLangMsg("createMsg"));
    }

}